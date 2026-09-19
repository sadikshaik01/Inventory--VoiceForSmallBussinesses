export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')

export async function getHealth(signal) {
  const response = await fetch(`${API_BASE_URL}/api/health`, {
    signal: AbortSignal.any([signal, AbortSignal.timeout(8000)]),
    headers: { Accept: 'application/json' },
    cache: 'no-store',
  })
  if (!response.ok) throw new Error('The server could not complete the connection check.')
  const data = await response.json()
  if (data.status !== 'UP' || data.service !== 'voicestock-api') {
    throw new Error('The server returned an unexpected response.')
  }
  return data
}
