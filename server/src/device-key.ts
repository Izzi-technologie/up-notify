export const DEVICE_KEY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

/** Reserved path segment for POST /webhook/all (broadcast). */
export const RESERVED_WEBHOOK_DEVICE_ID = "all";

const DEVICE_KEY = new RegExp(`^[${DEVICE_KEY_ALPHABET}]{5}$`);

export function isDeviceKey(value: string): boolean {
  return DEVICE_KEY.test(value);
}

export function isReservedWebhookDeviceId(value: string): boolean {
  return value.trim().toLowerCase() === RESERVED_WEBHOOK_DEVICE_ID;
}
