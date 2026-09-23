import type { OutgoingMessage } from "./transform.js";

export interface DeviceSocket {
  send(data: string): void;
  close(code?: number, reason?: string): void;
  readyState?: number;
}

type QueuedMessage = {
  message: OutgoingMessage;
  enqueuedAt: number;
};

const MAX_QUEUE = 32;
const QUEUE_TTL_MS = 10 * 60 * 1000;

export class DeviceRegistry {
  private readonly sockets = new Map<string, DeviceSocket>();
  private readonly lastAlertIds = new Map<string, string>();
  private readonly queues = new Map<string, QueuedMessage[]>();

  connect(deviceId: string, socket: DeviceSocket): void {
    const previous = this.sockets.get(deviceId);
    this.sockets.set(deviceId, socket);
    if (previous && previous !== socket) {
      try {
        previous.close();
      } catch (error) {
        console.error("WebhookAlarm failed to close replaced socket", error);
      }
    }
  }

  disconnect(deviceId: string, socket: DeviceSocket): void {
    if (this.sockets.get(deviceId) === socket) {
      this.sockets.delete(deviceId);
    }
  }

  get(deviceId: string): DeviceSocket | undefined {
    return this.sockets.get(deviceId);
  }

  /** Connected devices and any device with a queue or prior alert (for broadcast). */
  targetDeviceIds(): string[] {
    const ids = new Set<string>();
    for (const deviceId of this.sockets.keys()) ids.add(deviceId);
    for (const deviceId of this.queues.keys()) ids.add(deviceId);
    for (const deviceId of this.lastAlertIds.keys()) ids.add(deviceId);
    return [...ids];
  }

  rememberAlert(deviceId: string, alertId: string): void {
    this.lastAlertIds.set(deviceId, alertId);
  }

  lastAlertId(deviceId: string): string | undefined {
    return this.lastAlertIds.get(deviceId);
  }

  enqueue(deviceId: string, message: OutgoingMessage, now = Date.now()): void {
    const queue = this.fresh(deviceId, now);
    queue.push({ message, enqueuedAt: now });
    while (queue.length > MAX_QUEUE) queue.shift();
    this.queues.set(deviceId, queue);
    if (message.type === "alert") this.rememberAlert(deviceId, message.id);
  }

  drain(deviceId: string, now = Date.now()): OutgoingMessage[] {
    const queue = this.fresh(deviceId, now);
    this.queues.delete(deviceId);
    return queue.map((item) => item.message);
  }

  private fresh(deviceId: string, now: number): QueuedMessage[] {
    const queue = this.queues.get(deviceId) ?? [];
    return queue.filter((item) => now - item.enqueuedAt < QUEUE_TTL_MS);
  }
}
