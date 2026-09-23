import assert from "node:assert/strict";
import test from "node:test";
import { DeviceRegistry, type DeviceSocket } from "./registry.js";
import type { AlertPayload } from "./transform.js";

function fakeSocket(): DeviceSocket & { closed: boolean; sent: string[] } {
  const sent: string[] = [];
  const socket = {
    sent,
    closed: false,
    send(data: string) {
      sent.push(data);
    },
    close() {
      socket.closed = true;
    },
    readyState: 1,
  };
  return socket;
}

test("replaces the previous socket for the same device", () => {
  const registry = new DeviceRegistry();
  const first = fakeSocket();
  const second = fakeSocket();

  registry.connect("device-001", first);
  registry.connect("device-001", second);

  assert.equal(first.closed, true);
  assert.equal(second.closed, false);
  assert.equal(registry.get("device-001"), second);
});

test("a replaced socket closing does not drop the new connection", () => {
  const registry = new DeviceRegistry();
  const first = fakeSocket();
  const second = fakeSocket();

  registry.connect("device-001", first);
  registry.connect("device-001", second);
  registry.disconnect("device-001", first);

  assert.equal(registry.get("device-001"), second);
  registry.disconnect("device-001", second);
  assert.equal(registry.get("device-001"), undefined);
});

test("lists broadcast targets from sockets, queues, and last alert ids", () => {
  const registry = new DeviceRegistry();
  registry.connect("K7M2P", fakeSocket());
  registry.enqueue("B4NQ8", alert("evt_q"), 0);
  registry.rememberAlert("R3T6W", "evt_r");

  assert.deepEqual(registry.targetDeviceIds().sort(), [
    "B4NQ8",
    "K7M2P",
    "R3T6W",
  ]);
});

test("remembers the last alert id per device", () => {
  const registry = new DeviceRegistry();
  registry.rememberAlert("device-001", "evt_1");
  registry.rememberAlert("device-002", "evt_2");
  registry.rememberAlert("device-001", "evt_3");

  assert.equal(registry.lastAlertId("device-001"), "evt_3");
  assert.equal(registry.lastAlertId("device-002"), "evt_2");
  assert.equal(registry.lastAlertId("device-003"), undefined);
});

function alert(id: string): AlertPayload {
  return {
    type: "alert",
    id,
    severity: "critical",
    title: "API",
    message: "down",
    timestamp: "2026-09-23T10:42:00Z",
  };
}

test("drops the oldest queued message past 32 and expires after 10 minutes", () => {
  const registry = new DeviceRegistry();
  for (let index = 0; index < 33; index++) {
    registry.enqueue("K7M2P", alert(`evt_${index}`), index);
  }
  const capped = registry.drain("K7M2P", 32);
  assert.equal(capped.length, 32);
  assert.equal(capped[0]?.id, "evt_1");
  assert.equal(capped.at(-1)?.id, "evt_32");
  assert.equal(registry.lastAlertId("K7M2P"), "evt_32");

  registry.enqueue("K7M2P", alert("evt_old"), 0);
  registry.enqueue("K7M2P", alert("evt_new"), 1_000);
  const fresh = registry.drain("K7M2P", 10 * 60 * 1000);
  assert.deepEqual(
    fresh.map((message) => message.id),
    ["evt_new"],
  );
  assert.deepEqual(registry.drain("K7M2P", 10 * 60 * 1000), []);
});
