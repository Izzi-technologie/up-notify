import assert from "node:assert/strict"
import test from "node:test"
import { transformWebhook } from "./transform.js"

const now = new Date("2026-09-23T10:42:00.000Z")

test("transforms a Checkmate alert", () => {
  const outcome = transformWebhook(
    {
      severity: "critical",
      text: "Production API is down",
      monitor: { name: "Production API" },
    },
    { now, createId: () => "evt_test" },
  )

  assert.deepEqual(outcome, {
    action: "alert",
    message: {
      type: "alert",
      id: "evt_test",
      severity: "critical",
      title: "Production API",
      message: "Production API is down",
      timestamp: "2026-09-23T10:42:00Z",
    },
  })
})

test("keeps a provided id and timestamp, and reads message when text is absent", () => {
  const outcome = transformWebhook({
    id: "evt_123",
    severity: "warning",
    message: "Disk almost full",
    title: "Disk",
    timestamp: "2026-09-23T10:42:00Z",
  })

  assert.equal(outcome.action, "alert")
  assert.deepEqual(outcome.message, {
    type: "alert",
    id: "evt_123",
    severity: "warning",
    title: "Disk",
    message: "Disk almost full",
    timestamp: "2026-09-23T10:42:00Z",
  })
})

test("prefers monitor name, then title, then text", () => {
  const fromMonitor = transformWebhook(
    { monitor: { name: "API" }, title: "Other", text: "down", message: "ignored" },
    { createId: () => "evt_1" },
  )
  assert.equal(fromMonitor.action, "alert")
  if (fromMonitor.action !== "alert") return
  assert.equal(fromMonitor.message.title, "API")
  assert.equal(fromMonitor.message.message, "down")

  const fromTitle = transformWebhook({ title: "Payments", text: "timeout" }, { createId: () => "evt_2" })
  assert.equal(fromTitle.action, "alert")
  if (fromTitle.action !== "alert") return
  assert.equal(fromTitle.message.title, "Payments")

  const fromText = transformWebhook({ text: "timeout" }, { createId: () => "evt_3" })
  assert.equal(fromText.action, "alert")
  if (fromText.action !== "alert") return
  assert.equal(fromText.message.title, "timeout")

  const fallback = transformWebhook({}, { createId: () => "evt_4" })
  assert.equal(fallback.action, "alert")
  if (fallback.action !== "alert") return
  assert.equal(fallback.message.title, "Alert")
  assert.equal(fallback.message.severity, "info")
})

test("maps unknown severity to info", () => {
  const outcome = transformWebhook(
    { severity: "disaster", text: "hello" },
    { createId: () => "evt_5" },
  )
  assert.equal(outcome.action, "alert")
  if (outcome.action !== "alert") return
  assert.equal(outcome.message.severity, "info")
})

test("resolves from type and keeps the body id", () => {
  for (const type of ["alert_resolved", "resolved", "recovery"]) {
    const outcome = transformWebhook({ type, id: "evt_body" }, { lastAlertId: "evt_last" })
    assert.deepEqual(outcome, {
      action: "resolve",
      message: { type: "alert_resolved", id: "evt_body" },
    })
  }
})

test("resolves from status and falls back to the last alert id", () => {
  for (const status of ["up", "resolved", "online", "UP"]) {
    const outcome = transformWebhook({ status }, { lastAlertId: "evt_last" })
    assert.deepEqual(outcome, {
      action: "resolve",
      message: { type: "alert_resolved", id: "evt_last" },
    })
  }

  const fromMonitor = transformWebhook(
    { monitor: { name: "API", status: "online" } },
    { lastAlertId: "evt_monitor" },
  )
  assert.deepEqual(fromMonitor.message, { type: "alert_resolved", id: "evt_monitor" })
})

test("does not send a resolution when no id is known", () => {
  const outcome = transformWebhook({ type: "recovery" })
  assert.deepEqual(outcome, { action: "resolve", message: null })
})

test("treats a down status as an alert", () => {
  const outcome = transformWebhook(
    { status: "down", text: "API down", monitor: { name: "API", status: "down" } },
    { createId: () => "evt_down" },
  )
  assert.equal(outcome.action, "alert")
  assert.equal(outcome.message?.type, "alert")
})
