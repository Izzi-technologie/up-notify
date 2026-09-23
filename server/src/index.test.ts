import assert from "node:assert/strict";
import { once } from "node:events";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import type { AddressInfo } from "node:net";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { serve } from "@hono/node-server";
import { createApp, isAuthorized } from "./index.js";
import type { DeviceSocket } from "./registry.js";

function fakeSocket(): DeviceSocket & { sent: string[] } {
  const sent: string[] = [];
  return {
    sent,
    send(data: string) {
      sent.push(data);
    },
    close() {},
    readyState: 1,
  };
}

async function listen(app: ReturnType<typeof createApp>) {
  const server = serve({
    fetch: app.app.fetch,
    hostname: "127.0.0.1",
    port: 0,
  });
  app.injectWebSocket(server);
  if (!server.listening) await once(server, "listening");
  const address = server.address() as AddressInfo;
  return { server, port: address.port };
}

test("health returns ok", async () => {
  const { app } = createApp();
  const response = await app.request("/health");
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { status: "ok" });
});

test("download serves the apk and stays public", async (t) => {
  const directory = await mkdtemp(path.join(tmpdir(), "webhook-alarm-apk-"));
  const filePath = path.join(directory, "WebhookAlarm-TV-release.apk");
  const bytes = Buffer.from("PK\u0003\u0004webhook-alarm");
  await writeFile(filePath, bytes);
  t.after(() => rm(directory, { recursive: true, force: true }));

  const { app } = createApp(undefined, { apkPath: filePath, token: "secret" });
  const response = await app.request("/download");
  assert.equal(response.status, 200);
  assert.equal(
    response.headers.get("content-type"),
    "application/vnd.android.package-archive",
  );
  assert.equal(
    response.headers.get("content-disposition"),
    'attachment; filename="WebhookAlarm-TV-release.apk"',
  );
  assert.equal(response.headers.get("content-length"), String(bytes.length));
  assert.deepEqual(Buffer.from(await response.arrayBuffer()), bytes);

  const missing = await createApp(undefined, {
    apkPath: path.join(directory, "missing.apk"),
    token: "secret",
  }).app.request("/download");
  assert.equal(missing.status, 404);
  assert.deepEqual(await missing.json(), { error: "APK not found" });
});

test("returns 404 when the TV is offline", async () => {
  const { app } = createApp();
  const response = await app.request("/webhook/device-001", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      severity: "critical",
      text: "Production API is down",
      monitor: { name: "Production API" },
    }),
  });
  assert.equal(response.status, 404);
  assert.deepEqual(await response.json(), { error: "Device not connected" });
});

test("rejects invalid JSON and an empty device id", async () => {
  const { app } = createApp();
  const invalid = await app.request("/webhook/device-001", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: "not-json",
  });
  assert.equal(invalid.status, 400);

  const arrayBody = await app.request("/webhook/device-001", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: "[]",
  });
  assert.equal(arrayBody.status, 400);

  const emptyId = await app.request("/webhook/%20", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: "{}",
  });
  assert.equal(emptyId.status, 400);
});

test("delivers an alert and a later resolution to the connected TV", async () => {
  const { app, registry } = createApp();
  const socket = fakeSocket();
  registry.connect("device-001", socket);

  const alert = await app.request("/webhook/device-001", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      severity: "critical",
      text: "Production API is down",
      monitor: { name: "Production API" },
      timestamp: "2026-09-23T10:42:00Z",
    }),
  });
  assert.equal(alert.status, 202);
  const sentAlert = (await alert.json()) as {
    id: string;
    type: string;
    title: string;
  };
  assert.equal(sentAlert.type, "alert");
  assert.equal(sentAlert.title, "Production API");
  assert.match(sentAlert.id, /^evt_/);
  assert.equal(socket.sent[0], JSON.stringify(sentAlert));

  const resolved = await app.request("/webhook/device-001", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ status: "up" }),
  });
  assert.equal(resolved.status, 202);
  const sentResolved = await resolved.json();
  assert.deepEqual(sentResolved, { type: "alert_resolved", id: sentAlert.id });
  assert.equal(socket.sent[1], JSON.stringify(sentResolved));
});

test("accepts a resolution with no known id and sends nothing", async () => {
  const { app, registry } = createApp();
  const socket = fakeSocket();
  registry.connect("device-001", socket);

  const response = await app.request("/webhook/device-001", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ type: "recovery" }),
  });
  assert.equal(response.status, 202);
  assert.deepEqual(await response.json(), { delivered: false });
  assert.equal(socket.sent.length, 0);
});

test("requires the bearer token only when ALARM_TOKEN is set", async (t) => {
  const previous = process.env.ALARM_TOKEN;
  t.after(() => {
    if (previous === undefined) delete process.env.ALARM_TOKEN;
    else process.env.ALARM_TOKEN = previous;
  });

  delete process.env.ALARM_TOKEN;
  assert.equal(isAuthorized(undefined), true);

  process.env.ALARM_TOKEN = "secret";
  const { app } = createApp();
  const denied = await app.request("/webhook/device-001", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: "{}",
  });
  assert.equal(denied.status, 401);

  const wrong = await app.request("/webhook/device-001", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: "Bearer nope",
    },
    body: "{}",
  });
  assert.equal(wrong.status, 401);

  const socketDenied = await app.request("/ws");
  assert.equal(socketDenied.status, 401);

  const health = await app.request("/health");
  assert.equal(health.status, 200);

  const allowed = await app.request("/webhook/device-001", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: "Bearer secret",
    },
    body: JSON.stringify({ text: "down" }),
  });
  assert.equal(allowed.status, 404);
});

test(
  "replaces the live socket when the same device reconnects",
  { timeout: 5_000 },
  async () => {
    const previous = process.env.ALARM_TOKEN;
    delete process.env.ALARM_TOKEN;
    const app = createApp();
    const { server, port } = await listen(app);

    const connect = () =>
      new Promise<WebSocket>((resolve, reject) => {
        const ws = new WebSocket(`ws://127.0.0.1:${port}/ws`);
        const timeout = setTimeout(
          () => reject(new Error("websocket register timed out")),
          2000,
        );
        ws.addEventListener("error", () => {
          clearTimeout(timeout);
          reject(new Error("websocket error"));
        });
        ws.addEventListener("open", () => {
          ws.send(JSON.stringify({ type: "register", deviceId: "device-001" }));
        });
        ws.addEventListener("message", (event) => {
          const message = JSON.parse(String(event.data)) as { type?: string };
          if (message.type === "connected") {
            clearTimeout(timeout);
            resolve(ws);
          }
        });
      });

    try {
      const first = await connect();
      const firstClosed = new Promise<void>((resolve) => {
        first.addEventListener("close", () => resolve(), { once: true });
      });
      const second = await connect();
      await firstClosed;

      const received = new Promise<string>((resolve, reject) => {
        const timeout = setTimeout(
          () => reject(new Error("alert was not delivered")),
          2000,
        );
        second.addEventListener("message", (event) => {
          clearTimeout(timeout);
          resolve(String(event.data));
        });
      });

      const response = await fetch(
        `http://127.0.0.1:${port}/webhook/device-001`,
        {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({
            severity: "critical",
            text: "Production API is down",
            monitor: { name: "Production API" },
          }),
        },
      );
      assert.equal(response.status, 202);
      const body = (await response.json()) as { type: string };
      assert.equal(body.type, "alert");
      assert.equal(JSON.parse(await received).type, "alert");
      second.close();
    } finally {
      server.close();
      if (previous === undefined) delete process.env.ALARM_TOKEN;
      else process.env.ALARM_TOKEN = previous;
    }
  },
);
