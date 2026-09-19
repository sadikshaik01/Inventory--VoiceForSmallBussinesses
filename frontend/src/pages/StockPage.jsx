import { useState } from 'react'
import { api } from '../services/api.js'
import { useData } from '../services/useData.js'
import { Button, Field, Heading, LinkButton, Notice } from '../components/UI.jsx'
import { quantity, unitLabel } from '../services/format.js'

export default function StockPage({ params }) {
  const [mode, setMode] = useState(params.get('mode') === 'remove' ? 'remove' : 'add')
  const [productId, setProductId] = useState(params.get('productId') || '')
  const [search, setSearch] = useState('')
  const [amount, setAmount] = useState(params.get('quantity') || '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const choices = useData(`/products?size=100&search=${encodeURIComponent(search)}`)
  const selected = useData(productId ? `/products/${productId}` : null)
  const p = productId ? selected.data : null
  const options = [...(choices.data?.items || [])]
  if (p && !options.some(item => item.id === p.id)) options.unshift(p)
  async function submit(event) {
    event.preventDefault()
    if (!p) return
    setBusy(true); setError(''); setSuccess('')
    try {
      const updated = await api(`/inventory/${mode}`, { method: 'POST', body: { productId, quantity: amount, unit: p.unit } })
      setSuccess(`${mode === 'add' ? 'Stock added' : 'Stock removed'}. ${updated.name} now has ${quantity(updated.currentStock)} ${unitLabel(updated.unit)}.`)
      setAmount(''); selected.reload(); choices.reload()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <><Heading title="Update stock" description="Choose a product, enter a quantity, and confirm." /><form onSubmit={submit} className="panel max-w-2xl space-y-5 p-5 sm:p-8"><div className="grid grid-cols-2 gap-3"><Button type="button" variant={mode === 'add' ? 'primary' : 'secondary'} aria-pressed={mode === 'add'} onClick={() => { setMode('add'); setError(''); setSuccess('') }}>+ Add stock</Button><Button type="button" variant={mode === 'remove' ? 'danger' : 'secondary'} aria-pressed={mode === 'remove'} onClick={() => { setMode('remove'); setError(''); setSuccess('') }}>− Remove stock</Button></div><Notice message={error || choices.error || (productId && selected.error)} /><Notice message={success} success /><Field label="Find a product" value={search} onChange={e => setSearch(e.target.value)} placeholder="Type a name or category to narrow the list" /><Field label="Product"><select className="input" value={productId} required onChange={e => { setProductId(e.target.value); setError(''); setSuccess('') }}><option value="">Choose a product</option>{options.map(item => <option key={item.id} value={item.id}>{item.name} · {quantity(item.currentStock)} {unitLabel(item.unit)}</option>)}</select></Field>{p && <div className="rounded-xl bg-[#f2f5ec] p-4"><p className="text-xs text-slate-500">Available stock</p><p className="mt-1 text-xl font-semibold">{quantity(p.currentStock)} {unitLabel(p.unit)}</p></div>}<Field label={p ? `Quantity (${unitLabel(p.unit)})` : 'Quantity'} type="number" min="0.001" step="0.001" required value={amount} onChange={e => setAmount(e.target.value)} placeholder="0" inputMode="decimal" /><p className="text-sm text-slate-500">{p && amount ? `${mode === 'add' ? 'Add' : 'Remove'} ${amount} ${unitLabel(p.unit)} ${mode === 'add' ? 'to' : 'from'} ${p.name}?` : 'Every confirmed update is saved to your transaction history.'}</p><div className="flex flex-wrap gap-3"><Button disabled={busy || !p || p.archived || !amount} type="submit" variant={mode === 'remove' ? 'danger' : 'primary'}>{busy ? 'Saving…' : mode === 'add' ? 'Confirm add stock' : 'Confirm remove stock'}</Button><LinkButton secondary to={productId ? `/products/${productId}` : '/inventory'}>Back to inventory</LinkButton></div></form></>
}

