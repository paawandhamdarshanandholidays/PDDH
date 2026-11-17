export function getApiBaseUrl(): string {
  const configuredUrl = (globalThis as typeof globalThis & { PDDH_API_URL?: string }).PDDH_API_URL;
  return configuredUrl?.replace(/\/+$/, '') || 'http://localhost:9091';
}
