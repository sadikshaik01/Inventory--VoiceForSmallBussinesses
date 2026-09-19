import { API_BASE_URL } from './healthService.js'

const SESSION_KEY = 'voicestock.session'
export function getSession() {
  try { return JSON.parse(sessionStorage.getItem(SESSION_KEY)) } catch { return null }
}
export function saveSession(session) { sessionStorage.setItem(SESSION_KEY, JSON.stringify(session)) }
export function clearSession() { sessionStorage.removeItem(SESSION_KEY) }
export function navigate(path) { window.location.hash = path }

export async function api(path, { method = 'GET', body, signal } = {}) {
  const token = path.startsWith('/auth/') ? null : getSession()?.token
  let response
  try {
    response = await fetch(`${API_BASE_URL}/api${path}`, {
      method,
      headers: { Accept: 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined,
      signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
      cache: 'no-store',
    })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new Error('Could not reach the server. Check your connection and try again.')
  }
  if (response.status === 204) return null
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/auth/')) window.dispatchEvent(new Event('voicestock:expired'))
    throw new Error(data?.message || (response.status === 401 ? 'Please sign in again.' : 'Could not complete this action. Please try again.'))
  }
  return data
}
