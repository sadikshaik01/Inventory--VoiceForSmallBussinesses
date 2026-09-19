import { useState } from 'react'
import { api, getSession, saveSession } from '../services/api.js'
import { Button, Field, Heading, Notice } from '../components/UI.jsx'
import { languages } from '../services/format.js'

export default function Settings({ user, onUpdate }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(''); setSuccess('')
    try {
      const updated = await api('/users/profile', { method: 'PUT', body: Object.fromEntries(new FormData(event.currentTarget)) })
      saveSession({ ...getSession(), user: updated }); onUpdate(updated); setSuccess('Your profile has been saved.')
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <><Heading title="Settings & profile" description="Your business details and language preference." /><form onSubmit={submit} className="panel max-w-2xl space-y-5 p-5 sm:p-8"><Notice message={error} /><Notice message={success} success /><Field label="Your name" name="name" required maxLength={120} defaultValue={user.name} /><Field label="Business name" name="businessName" required maxLength={160} defaultValue={user.businessName} /><Field label="Email address" value={user.email} readOnly /><Field label="Preferred language" hint="Saved for the upcoming voice integration. The current interface uses English."><select className="input" name="preferredLanguage" defaultValue={user.preferredLanguage}>{languages.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field><Button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Save profile'}</Button></form></>
}
