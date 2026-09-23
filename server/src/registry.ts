export interface DeviceSocket {
  send(data: string): void
  close(code?: number, reason?: string): void
  readyState?: number
}

export class DeviceRegistry {
  private readonly sockets = new Map<string, DeviceSocket>()
  private readonly lastAlertIds = new Map<string, string>()

  connect(deviceId: string, socket: DeviceSocket): void {
    const previous = this.sockets.get(deviceId)
    this.sockets.set(deviceId, socket)
    if (previous && previous !== socket) {
      try {
        previous.close()
      } catch (error) {
        console.error("WebhookAlarm failed to close replaced socket", error)
      }
    }
  }

  disconnect(deviceId: string, socket: DeviceSocket): void {
    if (this.sockets.get(deviceId) === socket) {
      this.sockets.delete(deviceId)
    }
  }

  get(deviceId: string): DeviceSocket | undefined {
    return this.sockets.get(deviceId)
  }

  rememberAlert(deviceId: string, alertId: string): void {
    this.lastAlertIds.set(deviceId, alertId)
  }

  lastAlertId(deviceId: string): string | undefined {
    return this.lastAlertIds.get(deviceId)
  }
}
