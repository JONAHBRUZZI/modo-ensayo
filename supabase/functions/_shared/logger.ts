export function logInfo(event: string, data: Record<string, unknown> = {}) {
  console.log(JSON.stringify({ level: "INFO", event, timestamp: new Date().toISOString(), ...data }));
}
export function logWarn(event: string, message: string, data: Record<string, unknown> = {}) {
  console.warn(JSON.stringify({ level: "WARN", event, message, timestamp: new Date().toISOString(), ...data }));
}
export function logError(event: string, error: unknown, data: Record<string, unknown> = {}) {
  console.error(JSON.stringify({
    level: "ERROR", event, timestamp: new Date().toISOString(),
    error: error instanceof Error ? error.message : String(error),
    stack: error instanceof Error ? error.stack : undefined,
    ...data,
  }));
}
