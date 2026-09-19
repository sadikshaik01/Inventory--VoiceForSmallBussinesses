import { useState } from 'react'
import { api, navigate, saveSession } from '../services/api.js'
import { Button, Field, Icon, Notice } from '../components/UI.jsx'
import { languages } from '../services/format.js'

export default function AuthPage({ register, onLogin }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('')
    const body = Object.fromEntries(new FormData(event.currentTarget))
    try {
      const session = await api(`/auth/${register ? 'register' : 'login'}`, { method: 'POST', body })
      saveSession(session); onLogin(session.user); navigate('/dashboard')
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <div className="min-h-screen lg:grid lg:grid-cols-2"><aside className="bg-[#173f35] px-7 py-8 text-white lg:flex lg:min-h-screen lg:flex-col lg:justify-between lg:p-14"><a href="#/login" className="flex items-center gap-3 text-2xl font-bold"><img src="/favicon.svg" alt="" width="42" height="42" />VoiceStock</a><div className="my-8 max-w-lg lg:my-16"><span className="text-xs font-semibold uppercase tracking-[.2em] text-[#d8efad]">Made for your everyday business</span><h1 className="mt-5 text-4xl leading-tight font-semibold tracking-tight lg:text-5xl">Know your stock.<br />Keep your day simple.</h1><p className="mt-5 max-w-md text-base leading-7 text-white/70">Add products, update stock, and see what needs ordering. All your inventory, in one place.</p><div className="mt-8 hidden items-center gap-3 text-[#d8efad] lg:flex"><Icon name="box" /><span>Less paperwork. More time for your shop.</span></div></div><p className="hidden text-sm text-white/50 lg:block">Built for small businesses. Ready for your voice.</p></aside><main className="flex items-center justify-center px-5 py-10 sm:px-10"><div className="w-full max-w-md"><p className="text-xs font-semibold uppercase tracking-widest text-[#67824a]">Your business workspace</p><h2 className="mt-3 text-3xl font-bold text-[#173f35]">{register ? 'Create your account' : 'Welcome back'}</h2><p className="mt-3 mb-7 text-sm text-slate-500">{register ? 'Start with your business details. Add your first product next.' : 'Sign in to manage your inventory.'}</p><form onSubmit={submit} className="space-y-4"><Notice message={error} />{register && <><Field label="Your name" name="name" required maxLength={120} autoComplete="name" /><Field label="Business name" name="businessName" required maxLength={160} autoComplete="organization" /></>}<Field label="Email address" name="email" type="email" required maxLength={254} autoComplete="email" /><Field label="Password" name="password" type="password" required minLength={register ? 8 : undefined} maxLength={72} autoComplete={register ? 'new-password' : 'current-password'} hint={register ? 'Use at least 8 characters.' : undefined} />{register && <Field label="Preferred language"><select className="input" name="preferredLanguage" defaultValue="en">{languages.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>}<Button disabled={busy} className="mt-2 w-full" type="submit">{busy ? 'Please wait…' : register ? 'Create account' : 'Sign in'}</Button></form><p className="mt-6 text-center text-sm text-slate-500">{register ? 'Already have an account?' : 'New to VoiceStock?'} <a className="font-semibold text-[#173f35] underline underline-offset-4" href={register ? '#/login' : '#/register'}>{register ? 'Sign in' : 'Create an account'}</a></p></div></main></div>
}
