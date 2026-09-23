import assert from "node:assert/strict"
import test from "node:test"
import { DeviceRegistry, type DeviceSocket } from "./registry.js"

function fakeSocket(): DeviceSocket & { closed: boolean; sent: string[] } {
  const sent: string[] = []
  const socket = {
    sent,
    closed: false,
    send(data: string) {
      sent.push(data)
    },
    close() {
      socket.closed = true
    },
    readyState: 1,
  }
  return socket
}

test("replaces the previous socket for the same device", () => {
  const registry = new DeviceRegistry()
  const first = fakeSocket()
  const second = fakeSocket()

  registry.connect("tv-001", first)
  registry.connect("tv-001", second)

  assert.equal(first.closed, true)
  assert.equal(second.closed, false)
  assert.equal(registry.get("tv-001"), second)
})

test("a replaced socket closing does not drop the new connection", () => {
  const registry = new DeviceRegistry()
  const first = fakeSocket()
  const second = fakeSocket()

  registry.connect("tv-001", first)
  registry.connect("tv-001", second)
  registry.disconnect("tv-001", first)

  assert.equal(registry.get("tv-001"), second)
  registry.disconnect("tv-001", second)
  assert.equal(registry.get("tv-001"), undefined)
})

test("remembers the last alert id per device", () => {
  const registry = new DeviceRegistry()
  registry.rememberAlert("tv-001", "evt_1")
  registry.rememberAlert("tv-002", "evt_2")
  registry.rememberAlert("tv-001", "evt_3")

  assert.equal(registry.lastAlertId("tv-001"), "evt_3")
  assert.equal(registry.lastAlertId("tv-002"), "evt_2")
  assert.equal(registry.lastAlertId("tv-003"), undefined)
})
