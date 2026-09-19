import { useEffect, useState } from 'react'
import { api } from './api.js'

export function useData(path) {
  const [state, setState] = useState({ data: null, loading: true, error: '' })
  const [version, setVersion] = useState(0)
  useEffect(() => {
    if (!path) return
    const controller = new AbortController()
    // Reset only when the external resource changes; completed requests update the result.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setState({ data: null, loading: true, error: '' })
    api(path, { signal: controller.signal }).then(data => setState({ data, loading: false, error: '' }))
      .catch(error => { if (!controller.signal.aborted) setState({ data: null, loading: false, error: error.message }) })
    return () => controller.abort()
  }, [path, version])
  return { ...state, reload: () => setVersion(v => v + 1) }
}
