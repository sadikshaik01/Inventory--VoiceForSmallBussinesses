import { InsightCard, ActionCard } from '../components/BusinessCards.jsx'
import { useEffect, useRef, useState } from 'react'
import { Button, Field, Heading, Icon, LinkButton, Notice, Status } from '../components/UI.jsx'
import { api } from '../services/api.js'
import { quantity, unitLabel, units } from '../services/format.js'

export default function VoiceAssistant() {
  const [text, setText] = useState(''), [transcript, setTranscript] = useState('')
  const [state, setState] = useState('READY'), [error, setError] = useState('')
  const [reply, setReply] = useState(null), [updated, setUpdated] = useState(null)
  const [edit, setEdit] = useState(null), [locale, setLocale] = useState('en-IN')
  const recognition = useRef(null)
  const busy = state === 'PROCESSING' || state === 'LISTENING'
  useEffect(() => () => { if (recognition.current) { recognition.current.onend = null; recognition.current.onerror = null; recognition.current.onresult = null; recognition.current.abort() } }, [])
  async function discard() {
    if (reply?.confirmationId) await api(`/assistant/confirm/${reply.confirmationId}`, { method: 'DELETE' })
  }
  async function process(input = text) {
    if (!input.trim()) return
    setState('PROCESSING'); setError(''); setUpdated(null); setEdit(null); setTranscript(input)
    try {
      await discard(); setReply(null)
      const result = await api('/assistant/interpret', { method: 'POST', body: { text: input } })
      setReply(result); setState(result.confirmationId ? 'NEEDS CONFIRMATION' : result.command.missingFields?.length ? 'NEEDS CLARIFICATION' : 'SUCCESS')
    } catch (e) { setError(e.message); setState('ERROR') }
  }
  async function clarify(patch) {
    setState('PROCESSING'); setError(''); setEdit(null)
    try {
      const result = await api('/assistant/preview', { method: 'POST', body: { ...reply.command, ...patch } })
      setReply(result); setState(result.confirmationId ? 'NEEDS CONFIRMATION' : 'NEEDS CLARIFICATION')
    } catch (e) { setError(e.message); setState('ERROR') }
  }
  function listen() {
    const Speech = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!Speech) { setError('Speech recognition is unavailable in this browser. Type a command below.'); setState('ERROR'); return }
    const r = new Speech(); recognition.current = r
    r.lang = locale; r.continuous = false; r.interimResults = false
    let received = false
    r.onresult = event => { received = true; const value = event.results[0][0].transcript; setText(value); process(value) }
    r.onerror = event => { received = true; setState('ERROR'); setError(event.error === 'not-allowed' ? 'Microphone permission was denied. Allow microphone access or type a command below.' : 'Speech recognition could not finish. Please try again or type your command.') }
    r.onend = () => { recognition.current = null; if (!received) { setState('READY'); setError('No speech detected. Try again or type your command.') } }
    setState('LISTENING'); setError('')
    try { r.start() } catch { setState('ERROR'); setError('Microphone could not start. Type your command below.') }
  }
  async function confirm() {
    setState('PROCESSING'); setError('')
    try { const p = await api(`/assistant/confirm/${reply.confirmationId}`, { method: 'POST' }); setUpdated(p); setReply(null); setState('SUCCESS') }
    catch (e) { setError(e.message); setState('ERROR') }
  }
  async function cancel() {
    setState('PROCESSING'); setError('')
    try { await discard(); setReply(null); setEdit(null); setState('READY') }
    catch (e) { setError(e.message); setState('ERROR') }
  }
  async function startEdit() {
    try { await discard(); setEdit({ ...reply.command }); setReply({ ...reply, confirmationId: null }); setState('READY') }
    catch (e) { setError(e.message); setState('ERROR') }
  }
  async function review(event) {
    event.preventDefault(); setState('PROCESSING'); setError('')
    try { const result = await api('/assistant/preview', { method: 'POST', body: { ...edit, quantity: edit.quantity === '' || edit.quantity == null ? null : Number(edit.quantity) } }); setReply(result); if (result.confirmationId) setEdit(null); setState(result.confirmationId ? 'NEEDS CONFIRMATION' : 'ERROR') }
    catch (e) { setError(e.message); setState('ERROR') }
  }
  return <><Heading title="Voice assistant" description="Speak or type. Review every stock change before it happens." />
    <div className="mx-auto max-w-3xl space-y-5">
      <section className="panel p-5 sm:p-8">
        <div className="flex flex-wrap justify-between gap-3"><h2 className="text-sm font-semibold text-slate-500">1. LISTEN</h2><span aria-live="polite" className="text-xs font-bold text-[#55763e]">{state}</span></div>
        <button onClick={listen} disabled={busy} className="mx-auto my-6 flex min-h-36 w-40 flex-col items-center justify-center gap-3 rounded-3xl bg-[#173f35] p-5 font-semibold text-white disabled:opacity-50"><Icon name="mic" className="h-10 w-10" />{state === 'LISTENING' ? 'Listening…' : 'Tap to Speak'}</button>
        {state === 'LISTENING' && <Button variant="secondary" onClick={() => recognition.current?.stop()}>Stop listening</Button>}
        <Field label="Speech language"><select className="input" value={locale} onChange={e => setLocale(e.target.value)} disabled={busy}><option value="en-IN">English / mixed</option><option value="te-IN">Telugu</option><option value="hi-IN">Hindi</option></select></Field>
        <form className="mt-5 space-y-3" onSubmit={e => { e.preventDefault(); process() }}><Field label="Type a command"><textarea aria-label="Type a command" placeholder="Type a command..." className="input min-h-24" maxLength={500} value={text} onChange={e => setText(e.target.value)} disabled={busy} /></Field><Button className="w-full" disabled={busy || !text.trim()}>Process</Button></form>
        <p className="mt-4 text-xs leading-5 text-slate-500">Speech support depends on your browser and microphone permissions. Your browser may send audio to its speech provider. Typed commands use the same assistant.</p>
      </section>
      <Notice message={error} />
      {transcript && <section className="panel p-5"><h2 className="mb-3 text-sm font-semibold text-slate-500">2. YOU SAID</h2><p className="break-words">“{transcript}”</p></section>}
      {reply && <section className="panel space-y-4 p-5 sm:p-7"><h2 className="text-sm font-semibold text-slate-500">3. I UNDERSTOOD</h2><p className="font-bold">{reply.command.intent.replaceAll('_', ' ')}</p><p role="status">{reply.message}</p>
        {reply.confirmationId && <><h3 className="text-xl font-semibold">{reply.product.name}</h3><h2 className="text-sm font-semibold text-slate-500">4. CONFIRM</h2><dl className="space-y-3 rounded-xl bg-slate-50 p-4">{[['Current stock', reply.product.currentStock], [reply.command.intent === 'ADD_STOCK' ? 'Adding' : 'Removing', reply.command.quantity], ['After update', reply.afterStock]].map(([label, value]) => <div key={label} className="flex flex-wrap justify-between gap-2"><dt>{label}</dt><dd data-testid={label.toLowerCase().replaceAll(' ', '-')} className="font-bold">{quantity(value)} {unitLabel(reply.product.unit)}</dd></div>)}</dl>{reply.command.price != null && <p className="text-sm">Mentioned price: {reply.command.price}. This stock operation does not change the product price.</p>}<Button className="min-h-14 w-full" disabled={busy} onClick={confirm}>Confirm stock change</Button><div className="grid grid-cols-2 gap-3"><Button variant="secondary" disabled={busy} onClick={startEdit}>Edit</Button><Button variant="secondary" disabled={busy} onClick={cancel}>Cancel</Button></div></>}
        {!reply.confirmationId && reply.command.missingFields?.length > 0 && <div className="space-y-3 rounded-xl bg-slate-50 p-4"><p><strong>Product:</strong> {reply.command.product || 'Needed'} · <strong>Quantity:</strong> {reply.command.quantity ?? 'Needed'} · <strong>Unit:</strong> {reply.command.unit ? unitLabel(reply.command.unit) : 'Needed'}</p>{reply.candidates?.length > 0 && <div className="grid gap-2">{reply.candidates.map(p => <Button key={p.id} variant="secondary" disabled={busy} onClick={() => clarify({ productId: p.id, product: p.name })}>Select {p.name} · {unitLabel(p.unit)} · {quantity(p.currentStock)} available</Button>)}</div>}{reply.command.intent === 'UNKNOWN' && reply.product && <div className="flex flex-wrap gap-3"><Button disabled={busy} onClick={() => clarify({ intent: 'ADD_STOCK' })}>Add stock</Button><Button variant="secondary" disabled={busy} onClick={() => clarify({ intent: 'REMOVE_STOCK' })}>Remove stock</Button></div>}<div className="flex flex-wrap gap-3"><Button variant="secondary" disabled={busy} onClick={startEdit}>Correct missing details</Button><Button variant="secondary" disabled={busy} onClick={cancel}>Cancel</Button></div></div>}
        {reply.insights?.map(i => <InsightCard key={i.product.id} insight={i} />)}
        {reply.actions?.map(a => <ActionCard key={a.id} action={a} />)}
        {(reply.insights?.length > 0 || reply.actions?.length > 0) && <LinkButton to="/actions" secondary>Open Action Center</LinkButton>}
        {(!reply.insights?.length ? reply.products : []).map(p => <article key={p.id} className="rounded-xl border border-slate-200 p-4"><h3 className="mb-2 font-semibold">{p.name}</h3><Status value={p.status} /><p className="mt-3">Current: {quantity(p.currentStock)} {unitLabel(p.unit)} · Minimum: {quantity(p.minimumStock)} {unitLabel(p.unit)}</p>{reply.command.intent === 'REORDER' && <p className="mt-2 font-semibold">Suggested order: {quantity(p.reorderQuantity)} {unitLabel(p.unit)}</p>}</article>)}
      </section>}
      {edit && <form className="panel space-y-4 p-5" onSubmit={review}><h2 className="font-semibold">Edit command</h2><Field label="Action"><select aria-label="Action" className="input" value={edit.intent} onChange={e => setEdit({ ...edit, intent: e.target.value })}><option value="UNKNOWN" disabled>Choose an action</option><option value="ADD_STOCK">Add stock</option><option value="REMOVE_STOCK">Remove stock</option></select></Field><Field label="Product name" value={edit.product || ''} required maxLength={160} onChange={e => setEdit({ ...edit, product: e.target.value, productId: null })} /><Field label="Quantity" type="number" min="0.001" step="0.001" required value={edit.quantity ?? ''} onChange={e => setEdit({ ...edit, quantity: e.target.value })} /><Field label="Unit"><select aria-label="Unit" className="input" value={edit.unit || ''} required onChange={e => setEdit({ ...edit, unit: e.target.value })}><option value="">Choose a unit</option>{units.map(u => <option key={u} value={u}>{unitLabel(u)}</option>)}</select></Field><Button disabled={busy}>Review changes</Button><Button type="button" variant="secondary" disabled={busy} onClick={cancel}>Cancel edit</Button></form>}
      {updated && <section className="panel p-6"><h2 className="mb-3 text-sm font-semibold text-[#55763e]">5. UPDATED</h2><Notice success message={`${updated.name} now has ${quantity(updated.currentStock)} ${unitLabel(updated.unit)}. Recorded in transaction history as Voice.`} /></section>}
      <section className="panel p-5"><h2 className="mb-3 font-semibold">Try saying</h2><div className="flex flex-col gap-2">{['Add 10 bags of rice', 'Rice 5 bags add cheyyi', 'Rice mein se 2 bags hatao', 'How much rice do I have?', 'What needs to be ordered?'].map(example => <button key={example} disabled={busy} onClick={() => setText(example)} className="rounded-lg bg-slate-50 p-3 text-left text-sm hover:bg-slate-100">{example}</button>)}</div></section>
      <div className="grid gap-3 sm:grid-cols-2"><LinkButton to="/stock?mode=add">+ Add stock manually</LinkButton><LinkButton to="/stock?mode=remove" secondary>− Remove stock manually</LinkButton></div>
    </div></>
}



