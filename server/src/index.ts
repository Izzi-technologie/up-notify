import { createHash, timingSafeEqual } from "node:crypto";
import { createReadStream } from "node:fs";
import { readdirSync } from "node:fs";
import { stat } from "node:fs/promises";
import path from "node:path";
import { Readable } from "node:stream";
import { fileURLToPath, pathToFileURL } from "node:url";
import { serve } from "@hono/node-server";
import { createNodeWebSocket } from "@hono/node-ws";
import { Hono } from "hono";
import { bodyLimit } from "hono/body-limit";
import { isDeviceKey, isReservedWebhookDeviceId } from "./device-key.js";
import { DeviceRegistry } from "./registry.js";
import { transformWebhook, type OutgoingMessage } from "./transform.js";

const MAX_BODY_BYTES = 64 * 1024;

function alarmToken(): string {
  return process.env.ALARM_TOKEN?.trim() ?? "";
}

export function isAuthorized(
  header: string | undefined,
  token = alarmToken(),
): boolean {
  if (!token) return true;
  if (!header?.startsWith("Bearer ")) return false;
  const provided = header.slice("Bearer ".length);
  const actual = createHash("sha256").update(provided).digest();
  const expected = createHash("sha256").update(token).digest();
  return timingSafeEqual(actual, expected);
}

function readText(data: unknown): string {
  if (typeof data === "string") return data;
  if (Buffer.isBuffer(data)) return data.toString("utf8");
  if (data instanceof ArrayBuffer) return Buffer.from(data).toString("utf8");
  if (ArrayBuffer.isView(data)) {
    return Buffer.from(data.buffer, data.byteOffset, data.byteLength).toString(
      "utf8",
    );
  }
  return "";
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null;
  return value as Record<string, unknown>;
}

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function serverRoot(): string {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
}

const VERSIONED_APK_PREFIXES = [
  "IzziWebhookAlarm-v",
  "WebhookAlarm-TV-v",
] as const;

function isVersionedApk(name: string): boolean {
  return (
    name.endsWith(".apk") &&
    VERSIONED_APK_PREFIXES.some((prefix) => name.startsWith(prefix))
  );
}

function versionedApks(directory: string): string[] {
  try {
    return readdirSync(directory)
      .filter(isVersionedApk)
      .sort()
      .reverse()
      .map((name) => path.join(directory, name));
  } catch {
    return [];
  }
}

export function apkCandidates(): string[] {
  const configured = process.env.APK_PATH?.trim();
  if (configured) return [configured];
  const root = serverRoot();
  const apkDir = path.join(root, "apk");
  const releaseDir = path.resolve(root, "../app/build/outputs/apk/release");
  const debugDir = path.resolve(root, "../app/build/outputs/apk/debug");
  return [
    ...versionedApks(apkDir),
    path.join(apkDir, "IzziWebhookAlarm-release.apk"),
    path.join(apkDir, "WebhookAlarm-TV-release.apk"),
    ...versionedApks(releaseDir),
    path.join(releaseDir, "IzziWebhookAlarm-release.apk"),
    path.join(releaseDir, "WebhookAlarm-TV-release.apk"),
    path.join(debugDir, "IzziWebhookAlarm-debug.apk"),
    path.join(debugDir, "WebhookAlarm-TV-debug.apk"),
  ];
}

async function resolveApkPath(
  override?: string,
): Promise<{ filePath: string; size: number } | null> {
  const candidates = override ? [override] : apkCandidates();
  for (const candidate of candidates) {
    try {
      const info = await stat(candidate);
      if (info.isFile()) return { filePath: candidate, size: info.size };
    } catch {
      // Try the next location.
    }
  }
  return null;
}

function apkFilename(filePath: string): string {
  const base = path.basename(filePath).replace(/["\r\n]/g, "");
  return base.length > 0 ? base : "IzziWebhookAlarm.apk";
}

const webhookBodyLimit = bodyLimit({
  maxSize: MAX_BODY_BYTES,
  onError: (c) => c.json({ error: "Payload too large" }, 413),
});

type DeviceDelivery =
  | { status: "skipped" }
  | { status: "sent"; message: OutgoingMessage }
  | { status: "queued"; message: OutgoingMessage };

function deliverToDevice(
  registry: DeviceRegistry,
  deviceId: string,
  body: Record<string, unknown>,
): DeviceDelivery {
  const outcome = transformWebhook(body, {
    lastAlertId: registry.lastAlertId(deviceId),
  });

  if (!outcome.message) return { status: "skipped" };

  const message = outcome.message;
  const socket = registry.get(deviceId);
  if (socket) {
    try {
      socket.send(JSON.stringify(message));
      if (message.type === "alert")
        registry.rememberAlert(deviceId, message.id);
      console.log(
        `WebhookAlarm webhook ${deviceId} ${message.type} ${message.id}`,
      );
      return { status: "sent", message };
    } catch (error) {
      console.error("WebhookAlarm send failed", error);
      registry.disconnect(deviceId, socket);
    }
  }

  registry.enqueue(deviceId, message);
  console.log(
    `WebhookAlarm webhook ${deviceId} queued ${message.type} ${message.id}`,
  );
  return { status: "queued", message };
}

function jsonForDelivery(delivery: DeviceDelivery): Record<string, unknown> {
  if (delivery.status === "skipped") return { delivered: false };
  if (delivery.status === "sent")
    return delivery.message as Record<string, unknown>;
  return {
    delivered: false,
    queued: true,
    type: delivery.message.type,
    id: delivery.message.id,
  };
}

async function parseWebhookJson(c: {
  req: { json(): Promise<unknown> };
}): Promise<{ ok: true; body: Record<string, unknown> } | { ok: false }> {
  let payload: unknown;
  try {
    payload = await c.req.json();
  } catch {
    return { ok: false };
  }
  const body = asRecord(payload);
  if (!body) return { ok: false };
  return { ok: true, body };
}

type AppOptions = {
  apkPath?: string;
  token?: string;
};

export function createApp(
  registry = new DeviceRegistry(),
  options: AppOptions = {},
) {
  const token = () => options.token ?? alarmToken();
  const app = new Hono();
  const { injectWebSocket, upgradeWebSocket } = createNodeWebSocket({ app });

  app.onError((error, c) => {
    console.error("WebhookAlarm", error);
    return c.json({ error: "Internal Server Error" }, 500);
  });

  app.notFound((c) => c.json({ error: "Not Found" }, 404));

  app.get("/health", (c) => c.json({ status: "ok" }));

  app.get("/download", async (c) => {
    const apk = await resolveApkPath(options.apkPath);
    if (!apk) return c.json({ error: "APK not found" }, 404);

    const filename = apkFilename(apk.filePath);
    console.log(`WebhookAlarm download ${filename}`);
    return c.body(
      Readable.toWeb(createReadStream(apk.filePath)) as ReadableStream,
      200,
      {
        "Content-Type": "application/vnd.android.package-archive",
        "Content-Disposition": `attachment; filename="${filename}"`,
        "Content-Length": String(apk.size),
        "Cache-Control": "no-store",
      },
    );
  });

  app.get(
    "/ws",
    (c, next) => {
      if (!isAuthorized(c.req.header("Authorization"), token())) {
        return c.json({ error: "Unauthorized" }, 401);
      }
      return next();
    },
    upgradeWebSocket(() => {
      let deviceId: string | undefined;
      return {
        onMessage(event, ws) {
          const body = asRecord(parseJson(readText(event.data)));
          if (!body) {
            console.log("WebhookAlarm ignored invalid websocket message");
            return;
          }
          const type = typeof body.type === "string" ? body.type : "";
          if (type === "ping") {
            ws.send(JSON.stringify({ type: "pong" }));
            return;
          }
          if (type === "register") {
            const nextId =
              typeof body.deviceId === "string" ? body.deviceId.trim() : "";
            if (!isDeviceKey(nextId)) {
              console.log(
                "WebhookAlarm ignored register with invalid deviceId",
              );
              return;
            }
            if (deviceId && deviceId !== nextId)
              registry.disconnect(deviceId, ws);
            deviceId = nextId;
            registry.connect(nextId, ws);
            ws.send(JSON.stringify({ type: "connected", deviceId: nextId }));
            console.log(`WebhookAlarm connected ${nextId}`);
            flushQueue(registry, nextId, ws);
            return;
          }
          if (type === "acknowledge") {
            const alertId =
              typeof body.alertId === "string" ? body.alertId : "";
            const ackDevice =
              typeof body.deviceId === "string"
                ? body.deviceId
                : (deviceId ?? "");
            console.log(
              `WebhookAlarm acknowledge alertId=${alertId} deviceId=${ackDevice}`,
            );
          }
        },
        onClose(_event, ws) {
          if (!deviceId) return;
          registry.disconnect(deviceId, ws);
          console.log(`WebhookAlarm disconnected ${deviceId}`);
        },
        onError(event) {
          console.error("WebhookAlarm websocket error", event);
        },
      };
    }),
  );

  app.post("/webhook/all", webhookBodyLimit, async (c) => {
    if (!isAuthorized(c.req.header("Authorization"), token())) {
      return c.json({ error: "Unauthorized" }, 401);
    }

    const parsed = await parseWebhookJson(c);
    if (!parsed.ok) return c.json({ error: "Invalid JSON" }, 400);

    const deviceIds = registry.targetDeviceIds();
    let delivered = 0;
    let queued = 0;
    let skipped = 0;
    const results: Array<Record<string, unknown>> = [];

    for (const deviceId of deviceIds) {
      const delivery = deliverToDevice(registry, deviceId, parsed.body);
      if (delivery.status === "skipped") {
        skipped += 1;
        results.push({ deviceId, delivered: false });
        continue;
      }
      if (delivery.status === "sent") delivered += 1;
      else queued += 1;
      results.push({ deviceId, ...jsonForDelivery(delivery) });
    }

    console.log(
      `WebhookAlarm webhook all targets=${deviceIds.length} delivered=${delivered} queued=${queued} skipped=${skipped}`,
    );

    return c.json(
      {
        broadcast: true,
        targets: deviceIds.length,
        delivered,
        queued,
        skipped,
        results,
      },
      202,
    );
  });

  app.post("/webhook/:deviceId", webhookBodyLimit, async (c) => {
    if (!isAuthorized(c.req.header("Authorization"), token())) {
      return c.json({ error: "Unauthorized" }, 401);
    }

    const deviceId = c.req.param("deviceId").trim();
    if (isReservedWebhookDeviceId(deviceId)) {
      return c.json({ error: "Invalid device id" }, 400);
    }
    if (!isDeviceKey(deviceId))
      return c.json({ error: "Invalid device id" }, 400);

    const parsed = await parseWebhookJson(c);
    if (!parsed.ok) return c.json({ error: "Invalid JSON" }, 400);

    const delivery = deliverToDevice(registry, deviceId, parsed.body);
    if (delivery.status === "skipped") {
      console.log(`WebhookAlarm webhook ${deviceId} resolve skipped`);
      return c.json({ delivered: false }, 202);
    }

    return c.json(jsonForDelivery(delivery), 202);
  });

  return { app, registry, injectWebSocket };
}

function flushQueue(
  registry: DeviceRegistry,
  deviceId: string,
  ws: { send(data: string): void },
): void {
  const pending = registry.drain(deviceId);
  for (let index = 0; index < pending.length; index++) {
    const message: OutgoingMessage = pending[index];
    try {
      ws.send(JSON.stringify(message));
    } catch (error) {
      console.error("WebhookAlarm flush failed", error);
      for (const remaining of pending.slice(index)) {
        registry.enqueue(deviceId, remaining);
      }
      return;
    }
  }
}

function isDirectRun(): boolean {
  const entry = process.argv[1];
  if (!entry) return false;
  return import.meta.url === pathToFileURL(entry).href;
}

export function start(): ReturnType<typeof serve> {
  const port = Number(process.env.PORT) || 3000;
  const { app, injectWebSocket } = createApp();
  const httpServer = serve(
    { fetch: app.fetch, hostname: "0.0.0.0", port },
    (info) => {
      console.log(`WebhookAlarm listening on :${info.port}`);
    },
  );
  injectWebSocket(httpServer);
  return httpServer;
}

if (isDirectRun()) {
  const httpServer = start();
  const shutdown = () => {
    httpServer.close(() => process.exit(0));
  };
  process.on("SIGTERM", shutdown);
  process.on("SIGINT", shutdown);
}
