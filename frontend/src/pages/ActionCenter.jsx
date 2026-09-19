import { useState } from 'react'
import { api } from '../services/api.js'
import { useData } from '../services/useData.js'
import { Empty, Heading, LinkButton, LoadState, Notice } from '../components/UI.jsx'
import { ActionCard, InsightCard } from '../components/BusinessCards.jsx'

export function BusinessSummary() {
  const resource = useData('/business')
  if (!resource.data) return <div className="mb-7"><LoadState {...resource} /></div>
  const d = resource.data
  return <section className="panel mb-7 flex flex-wrap items-center justify-between gap-5 p-5"><div><p className="text-xs font-bold text-[#55763e]">BUSINESS ACTIONS</p><h2 className="mt-2 text-xl font-semibold">{d.insights.length} products need attention</h2><p className="mt-2 text-sm text-slate-500">{d.lowStock} low stock · {d.outOfStock} out of stock · {d.runningOutSoon} may run out soon</p></div><LinkButton to="/actions">View actions</LinkButton></section>
}

export default function ActionCenter() {
  const resource = useData('/business')
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('')
  async function act(path, message) {
    setBusy(true); setError(''); setSuccess('')
    try { await api(path, { method: 'POST' }); setSuccess(message); resource.reload() }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  if (!resource.data) return <LoadState {...resource} />
  const d = resource.data
  return <><Heading title="Action Center" description="Detect → Explain → Recommend → Approve → Track" /><div className="mb-5 space-y-3"><Notice message={error || resource.error} /><Notice message={success} success /><p className="text-sm leading-6 text-slate-600">Plan a reorder here. Approval and completion only track the task; record physical receipts separately using Add stock.</p></div><h2 className="mb-4 text-lg font-semibold">Products requiring attention</h2><div className="grid gap-4 xl:grid-cols-2">{d.insights.map(i => <InsightCard key={i.product.id} insight={i} approved={d.actions.some(a => a.productId === i.product.id && a.status === 'APPROVED')} busy={busy} onApprove={() => act(`/business/approve/${i.product.id}`, 'Reorder approved and saved. Inventory is unchanged.')} />)}</div>{!d.insights.length && <Empty title="No products need attention" text="Current stock and recorded usage show no active alerts." />}<section className="mt-8"><h2 className="mb-2 text-lg font-semibold">Track reorder actions</h2><p className="mb-4 text-sm text-slate-500">Most recent 100 actions. Completed tasks remain in history.</p><div className="grid gap-4 xl:grid-cols-2">{d.actions.map(a => <ActionCard key={a.id} action={a} busy={busy} onComplete={() => act(`/business/actions/${a.id}/complete`, 'Reorder marked completed. Inventory is unchanged. Record received stock separately.')} onCancel={() => act(`/business/actions/${a.id}/cancel`, 'Reorder cancelled. Inventory is unchanged.')} />)}</div>{!d.actions.length && <p className="panel p-5 text-sm text-slate-500">No reorder actions have been approved yet.</p>}</section></>
}
