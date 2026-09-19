import { Button, LinkButton } from './UI.jsx'
import { quantity, unitLabel } from '../services/format.js'

export function InsightCard({ insight: i, onApprove, approved, busy }) {
  const p = i.product, unit = unitLabel(p.unit)
  return <article className="panel space-y-4 p-5" data-testid={`insight-${p.name}`}>
    <div className="flex flex-wrap items-center justify-between gap-3"><h3 className="text-lg font-semibold break-words">{p.name}</h3><span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-bold text-amber-800">{i.status.replaceAll('_', ' ')}</span></div>
    <p className="text-xs font-semibold text-slate-500">DETECTED → WHY → RECOMMENDED ACTION</p>
    <dl className="grid grid-cols-2 gap-3 text-sm"><div><dt>Current stock</dt><dd className="mt-1 font-bold">{quantity(p.currentStock)} {unit}</dd></div><div><dt>Minimum stock</dt><dd className="mt-1 font-bold">{quantity(p.minimumStock)} {unit}</dd></div><div><dt>Recent usage</dt><dd className="mt-1 font-bold">{i.averageDailyUsage == null ? 'Unavailable' : `${quantity(i.averageDailyUsage)} ${unit}/day`}</dd></div><div><dt>Estimated remaining</dt><dd className="mt-1 font-bold">{i.daysRemaining == null ? 'Unavailable' : `~${quantity(i.daysRemaining)} days`}</dd></div></dl>
    <p className="text-sm leading-6 text-slate-600">{i.reason}</p>
    {i.suggestedReorderQuantity > 0 && <p className="font-semibold">Suggested order: {quantity(i.suggestedReorderQuantity)} {unit}</p>}
    <p className="text-xs text-slate-500">Supplier: Not configured</p>
    {onApprove && (approved ? <p className="font-bold text-[#55763e]">APPROVED — tracked below</p> : <Button className="min-h-12 w-full" disabled={busy} onClick={onApprove}>Approve reorder</Button>)}
  </article>
}

export function ActionCard({ action: a, onComplete, onCancel, busy }) {
  return <article className="panel space-y-3 p-5" data-testid={`action-${a.productName}`}><div className="flex flex-wrap justify-between gap-2"><h3 className="font-semibold">{a.productName}</h3><span className="text-sm font-bold">{a.status}</span></div><p>Recommended: {quantity(a.recommendedQuantity)} {unitLabel(a.unit)}</p><p className="text-sm leading-6 text-slate-600">{a.reason}</p><p className="text-xs text-slate-500">Approved: {a.approvedAt ? new Date(a.approvedAt).toLocaleString() : '—'}{a.completedAt && ` · Completed: ${new Date(a.completedAt).toLocaleString()}`}</p>{a.status === 'APPROVED' && onComplete && <div className="flex flex-wrap gap-3"><Button disabled={busy} onClick={onComplete}>Mark completed</Button><Button variant="secondary" disabled={busy} onClick={onCancel}>Cancel reorder</Button></div>}{a.status === 'COMPLETED' && !a.productArchived && <LinkButton secondary to={`/stock?mode=add&productId=${a.productId}&quantity=${a.recommendedQuantity}`}>Add received stock</LinkButton>}</article>
}
