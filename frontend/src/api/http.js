// Shared fetch helper with JSON handling and useful errors
export async function requestJSON(url, options = {}) {
  const { method = 'GET', body, headers = {}, timeoutMs = 8000 } = options

  const controller = new AbortController()
  const tid = setTimeout(() => controller.abort(), timeoutMs)

  try {
    const res = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    })

    const text = await res.text()
    let data = null
    if (text) {
      try {
        data = JSON.parse(text)
      } catch {
        data = { raw: text }
      }
    }

    if (!res.ok) {
      const msg = data?.error?.message || data?.message || `HTTP ${res.status}`
      const code = data?.error?.code || `HTTP_${res.status}`
      const err = new Error(msg)
      err.status = res.status
      err.code = code
      err.data = data
      throw err
    }

    return data
  } catch (e) {
    if (e.name === 'AbortError') {
      const err = new Error('Request timed out. Is the service running?')
      err.code = 'TIMEOUT'
      err.status = 503
      throw err
    }
    // Ensure code/status
    if (!e.code) e.code = 'NETWORK_ERROR'
    if (!e.status) e.status = 0
    throw e
  } finally {
    clearTimeout(tid)
  }
}

export function getEnvUrl(key, fallback) {
  const v = import.meta.env[key]
  return (v && String(v).trim()) || fallback
}
