import { useState } from 'react'
import { api, navigate } from '../services/api.js'
import { useData } from '../services/useData.js'
import { Button, Empty, Heading, LinkButton, LoadState, Notice, Status, TransactionList } from '../components/UI.jsx'
import { quantity, unitLabel } from '../services/format.js'

export default function ProductDetails({ id }) {
  const product = useData(`/products/${id}`)
  const history = useData(`/transactions?productId=${id}&size=5`)
  const [confirm, setConfirm] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function archive() {
    setBusy(true); setError('')
    try { await api(`/products/${id}`, { method: 'DELETE' }); navigate('/inventory') } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  if (!product.data) return <LoadState {...product} />
  const p = product.data
  return <><Heading title={p.name} description={p.category}>{!p.archived && <LinkButton to={`/products/${id}/edit`} secondary>Edit product</LinkButton>}</Heading><Notice message={error} /><section className="panel mb-6 p-6 sm:p-8"><div className="flex flex-wrap items-start justify-between gap-5"><div><p className="text-sm text-slate-500">Current stock</p><p className="mt-3 text-4xl font-semibold tabular-nums" data-testid="product-stock">{quantity(p.currentStock)} <span className="text-lg font-normal text-slate-500">{unitLabel(p.unit)}</span></p></div>{p.archived ? <span className="rounded-full bg-slate-100 px-3 py-1 text-sm">Archived</span> : <Status value={p.status} />}</div><dl className="mt-7 grid gap-5 border-t border-slate-100 pt-6 sm:grid-cols-3"><div><dt className="text-xs text-slate-500">Minimum stock</dt><dd className="mt-2 font-semibold">{quantity(p.minimumStock)} {unitLabel(p.unit)}</dd></div><div><dt className="text-xs text-slate-500">Price</dt><dd className="mt-2 font-semibold">{p.price == null ? 'Not set' : quantity(p.price)}</dd></div><div><dt className="text-xs text-slate-500">Suggested reorder</dt><dd className="mt-2 font-semibold">{quantity(p.reorderQuantity)} {unitLabel(p.unit)}</dd></div></dl>{!p.archived && <div className="mt-7 flex flex-wrap gap-3"><LinkButton to={`/stock?mode=add&productId=${id}`}>+ Add stock</LinkButton><LinkButton to={`/stock?mode=remove&productId=${id}`} secondary>− Remove stock</LinkButton></div>}</section><section className="panel mb-6 p-5 sm:p-6"><div className="mb-3 flex items-center justify-between gap-3"><h2 className="text-lg font-semibold">Recent transactions</h2><a className="text-sm font-semibold hover:underline" href={`#/transactions?productId=${id}`}>View all</a></div>{history.loading || history.error ? <LoadState {...history} /> : history.data?.items.length ? <TransactionList items={history.data.items} /> : <Empty title="No stock changes yet" text="Add stock to start this product’s history." />}</section>{!p.archived && <section className="rounded-xl border border-slate-200 p-5"><h2 className="font-semibold">Archive product</h2><p className="mt-2 mb-4 text-sm text-slate-500">Remove it from active inventory. Its stock and transaction history are preserved.</p>{confirm ? <div className="space-y-3"><p className="text-sm font-semibold">Archive {p.name}?</p><div className="flex gap-3"><Button variant="danger" disabled={busy} onClick={archive}>{busy ? 'Archiving…' : 'Confirm archive'}</Button><Button variant="secondary" onClick={() => setConfirm(false)}>Cancel</Button></div></div> : <Button variant="secondary" onClick={() => setConfirm(true)}>Archive product</Button>}</section>}</>
}
