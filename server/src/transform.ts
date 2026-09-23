import { randomUUID } from "node:crypto"

export type Severity = "info" | "warning" | "critical"

export type AlertPayload = {
  type: "alert"
  id: string
  severity: Severity
  title: string
  message: string
  timestamp: string
}

export type ResolvedPayload = {
  type: "alert_resolved"
  id: string
}

export type OutgoingMessage = AlertPayload | ResolvedPayload

export type TransformOutcome =
  | { action: "alert"; message: AlertPayload }
  | { action: "resolve"; message: ResolvedPayload | null }

const RESOLVED_TYPES = new Set(["alert_resolved", "resolved", "recovery"])
const UP_STATUSES = new Set(["up", "resolved", "online"])

export type TransformOptions = {
  lastAlertId?: string | null
  now?: Date
  createId?: () => string
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null
  return value as Record<string, unknown>
}

function asText(value: unknown): string | null {
  if (typeof value !== "string") return null
  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : null
}

function utcTimestamp(date: Date): string {
  return date.toISOString().replace(/\.\d{3}Z$/, "Z")
}

export function isResolution(body: Record<string, unknown>): boolean {
  const type = asText(body.type)?.toLowerCase()
  if (type && RESOLVED_TYPES.has(type)) return true

  const status = asText(body.status)?.toLowerCase()
  if (status && UP_STATUSES.has(status)) return true

  const monitor = asRecord(body.monitor)
  const monitorStatus = monitor ? asText(monitor.status)?.toLowerCase() : null
  return Boolean(monitorStatus && UP_STATUSES.has(monitorStatus))
}

export function transformWebhook(
  body: Record<string, unknown>,
  options: TransformOptions = {},
): TransformOutcome {
  if (isResolution(body)) {
    const id = asText(body.id) ?? options.lastAlertId ?? null
    if (!id) return { action: "resolve", message: null }
    return { action: "resolve", message: { type: "alert_resolved", id } }
  }

  const monitor = asRecord(body.monitor)
  const message = asText(body.text) ?? asText(body.message) ?? ""
  const title =
    (monitor ? asText(monitor.name) : null) ??
    asText(body.title) ??
    (message || "Alert")
  const severityRaw = asText(body.severity)?.toLowerCase()
  const severity: Severity =
    severityRaw === "warning" || severityRaw === "critical" ? severityRaw : "info"
  const id = asText(body.id) ?? (options.createId ?? (() => `evt_${randomUUID()}`))()
  const timestamp = asText(body.timestamp) ?? utcTimestamp(options.now ?? new Date())

  return {
    action: "alert",
    message: { type: "alert", id, severity, title, message, timestamp },
  }
}
