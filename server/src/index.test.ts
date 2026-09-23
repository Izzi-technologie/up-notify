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

test("queues an alert and its resolution while the device is offline", async () => {
  const { app, registry } = createApp();
  const response = await app.request("/webhook/K7M2P", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      severity: "critical",
      text: "Production API is down",
      monitor: { name: "Production API" },
    }),
  });
  assert.equal(response.status, 202);
  const queued = (await response.json()) as {
    delivered: boolean;
    queued: boolean;
    type: string;
    id: string;
  };
  assert.equal(queued.delivered, false);
  assert.equal(queued.queued, true);
  assert.equal(queued.type, "alert");
  assert.match(queued.id, /^evt_/);

  const resolved = await app.request("/webhook/K7M2P", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ status: "up" }),
  });
  assert.equal(resolved.status, 202);
  assert.deepEqual(await resolved.json(), {
    delivered: false,
    queued: true,
    type: "alert_resolved",
    id: queued.id,
  });
  assert.deepEqual(
    registry.drain("K7M2P").map((message) => message.type),
    ["alert", "alert_resolved"],
  );
});

test("rejects a device id that is not a 5 character key", async () => {
  const { app } = createApp();
  const response = await app.request("/webhook/device-001", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ text: "down" }),
  });
  assert.equal(response.status, 400);
  assert.deepEqual(await response.json(), { error: "Invalid device id" });
});

test("broadcasts an alert to every connected device", async () => {
  const { app, registry } = createApp();
  const first = fakeSocket();
  const second = fakeSocket();
  registry.connect("K7M2P", first);
  registry.connect("B4NQ8", second);

  const response = await app.request("/webhook/all", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      severity: "critical",
      text: "Global incident",
      monitor: { name: "Platform" },
    }),
  });
  assert.equal(response.status, 202);
  const body = (await response.json()) as {
    broadcast: boolean;
    targets: number;
    delivered: number;
    results: Array<{ deviceId: string; type: string }>;
  };
  assert.equal(body.broadcast, true);
  assert.equal(body.targets, 2);
  assert.equal(body.delivered, 2);
  assert.equal(body.results.length, 2);
  assert.deepEqual(body.results.map((item) => item.deviceId).sort(), [
    "B4NQ8",
    "K7M2P",
  ]);
  assert.equal(first.sent.length, 1);
  assert.equal(second.sent.length, 1);
  assert.equal(JSON.parse(first.sent[0]!).type, "alert");
});

test("broadcast queues for a known offline device", async () => {
  const { app, registry } = createApp();
  registry.rememberAlert("K7M2P", "evt_old");

  const response = await app.request("/webhook/all", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      severity: "warning",
      text: "Disk is filling",
    }),
  });
  assert.equal(response.status, 202);
  const body = (await response.json()) as {
    targets: number;
    queued: number;
    results: Array<{ deviceId: string; queued: boolean }>;
  };
  assert.equal(body.targets, 1);
  assert.equal(body.queued, 1);
  assert.equal(body.results[0]?.deviceId, "K7M2P");
  assert.equal(body.results[0]?.queued, true);
  assert.equal(registry.drain("K7M2P")[0]?.type, "alert");
});

test("rejects invalid JSON and an empty device id", async () => {
  const { app } = createApp();
  const invalid = await app.request("/webhook/K7M2P", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: "not-json",
  });
  assert.equal(invalid.status, 400);

  const arrayBody = await app.request("/webhook/K7M2P", {
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
  registry.connect("K7M2P", socket);

  const alert = await app.request("/webhook/K7M2P", {
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

  const resolved = await app.request("/webhook/K7M2P", {
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
  registry.connect("K7M2P", socket);

  const response = await app.request("/webhook/K7M2P", {
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
  const denied = await app.request("/webhook/K7M2P", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: "{}",
  });
  assert.equal(denied.status, 401);

  const wrong = await app.request("/webhook/K7M2P", {
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

  const allowed = await app.request("/webhook/K7M2P", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: "Bearer secret",
    },
    body: JSON.stringify({ text: "down" }),
  });
  assert.equal(allowed.status, 202);
  const queued = (await allowed.json()) as { queued?: boolean };
  assert.equal(queued.queued, true);
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
          ws.send(JSON.stringify({ type: "register", deviceId: "K7M2P" }));
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

      const response = await fetch(`http://127.0.0.1:${port}/webhook/K7M2P`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          severity: "critical",
          text: "Production API is down",
          monitor: { name: "Production API" },
        }),
      });
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

test("queues when the live socket can no longer send", async () => {
  const { app, registry } = createApp();
  registry.connect("K7M2P", {
    send() {
      throw new Error("closed");
    },
    close() {},
  });
  const response = await app.request("/webhook/K7M2P", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ text: "down" }),
  });
  assert.equal(response.status, 202);
  const body = (await response.json()) as { queued: boolean; id: string };
  assert.equal(body.queued, true);
  assert.equal(registry.get("K7M2P"), undefined);
  assert.equal(registry.drain("K7M2P")[0]?.id, body.id);
});

test(
  "flushes queued alerts on register and answers ping",
  { timeout: 5_000 },
  async () => {
    const previous = process.env.ALARM_TOKEN;
    delete process.env.ALARM_TOKEN;
    const created = createApp();
    const { server, port } = await listen(created);
    try {
      const queuedResponse = await fetch(
        `http://127.0.0.1:${port}/webhook/K7M2P`,
        {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({
            severity: "warning",
            text: "Disk is filling",
          }),
        },
      );
      assert.equal(queuedResponse.status, 202);
      const queued = (await queuedResponse.json()) as { id: string };
      const messages: Array<{ type?: string; id?: string }> = [];
      const ws = new WebSocket(`ws://127.0.0.1:${port}/ws`);
      const waitFor = (ready: () => boolean, label: string) =>
        new Promise<void>((resolve, reject) => {
          const timeout = setTimeout(() => reject(new Error(label)), 2000);
          const check = () => {
            if (!ready()) return;
            clearTimeout(timeout);
            ws.removeEventListener("message", check);
            resolve();
          };
          ws.addEventListener("message", check);
          check();
        });

      ws.addEventListener("message", (event) => {
        messages.push(JSON.parse(String(event.data)) as { type?: string });
      });
      await new Promise<void>((resolve, reject) => {
        ws.addEventListener("open", () => {
          ws.send(JSON.stringify({ type: "register", deviceId: "K7M2P" }));
          resolve();
        });
        ws.addEventListener("error", () =>
          reject(new Error("websocket error")),
        );
      });
      await waitFor(
        () =>
          messages.some((message) => message.type === "connected") &&
          messages.some((message) => message.type === "alert"),
        "queued alert was not flushed",
      );
      assert.equal(messages[0]?.type, "connected");
      assert.equal(messages[1]?.type, "alert");
      assert.equal(messages[1]?.id, queued.id);

      ws.send(JSON.stringify({ type: "ping" }));
      await waitFor(
        () => messages.some((message) => message.type === "pong"),
        "pong was not received",
      );
      ws.close();
    } finally {
      server.close();
      if (previous === undefined) delete process.env.ALARM_TOKEN;
      else process.env.ALARM_TOKEN = previous;
    }
  },
);

test(
  "ignores a register that is not a device key",
  { timeout: 5_000 },
  async () => {
    const previous = process.env.ALARM_TOKEN;
    delete process.env.ALARM_TOKEN;
    const created = createApp();
    const { server, port } = await listen(created);
    try {
      const ws = new WebSocket(`ws://127.0.0.1:${port}/ws`);
      let connected = false;
      ws.addEventListener("message", (event) => {
        const message = JSON.parse(String(event.data)) as { type?: string };
        if (message.type === "connected") connected = true;
      });
      await new Promise<void>((resolve, reject) => {
        ws.addEventListener("open", () => {
          ws.send(JSON.stringify({ type: "register", deviceId: "device-001" }));
          resolve();
        });
        ws.addEventListener("error", () =>
          reject(new Error("websocket error")),
        );
      });
      await new Promise((resolve) => setTimeout(resolve, 200));
      assert.equal(connected, false);
      assert.equal(created.registry.get("device-001"), undefined);
      ws.close();
    } finally {
      server.close();
      if (previous === undefined) delete process.env.ALARM_TOKEN;
      else process.env.ALARM_TOKEN = previous;
    }
  },
);
