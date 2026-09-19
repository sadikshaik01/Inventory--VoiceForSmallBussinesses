import { useState } from 'react'
import { api, navigate } from '../services/api.js'
import { useData } from '../services/useData.js'
import { Button, Field, Heading, LinkButton, LoadState, Notice } from '../components/UI.jsx'
import { units, unitLabel } from '../services/format.js'

function Editor({ product }) {
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(event) {
    event.preventDefault(); setError(''); setBusy(true)
    const values = Object.fromEntries(new FormData(event.currentTarget))
    values.price = values.price === '' ? null : values.price
    try {
      const saved = await api(product ? `/products/${product.id}` : '/products', { method: product ? 'PUT' : 'POST', body: values })
      navigate(`/products/${saved.id}`)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <><Heading title={product ? 'Edit product' : 'Add a product'} description={product ? 'Keep your product details up to date.' : 'A few details now make stock updates easier later.'} /><form onSubmit={submit} className="panel max-w-3xl space-y-5 p-5 sm:p-8"><Notice message={error} /><div className="grid gap-5 sm:grid-cols-2"><Field label="Product name" name="name" required maxLength={160} defaultValue={product?.name} placeholder="e.g. Rice" /><Field label="Category" name="category" required maxLength={100} defaultValue={product?.category} placeholder="e.g. Grocery" /><Field label="Unit" hint={product ? 'A unit cannot change once stock history exists.' : 'Use the unit you count or sell this product in.'}><select className="input" name="unit" defaultValue={product?.unit || 'PIECES'}>{units.map(u => <option key={u} value={u}>{unitLabel(u)}</option>)}</select></Field>{!product ? <Field label="Initial stock" name="currentStock" type="number" required min="0" step="0.001" defaultValue="0" /> : <Field label="Current stock" value={product.currentStock} readOnly hint="Use Add / Remove stock to record changes in history." />}<Field label="Minimum stock" name="minimumStock" type="number" required min="0" step="0.001" defaultValue={product?.minimumStock ?? '0'} hint="We’ll flag stock at or below this level." /><Field label="Price (optional)" name="price" type="number" min="0" step="0.01" defaultValue={product?.price ?? ''} /></div><div className="flex flex-wrap gap-3 border-t border-slate-100 pt-5"><Button type="submit" disabled={busy}>{busy ? 'Saving…' : product ? 'Save changes' : 'Create product'}</Button><LinkButton to={product ? `/products/${product.id}` : '/inventory'} secondary>Cancel</LinkButton></div></form></>
}
function EditProduct({ id }) {
  const resource = useData(`/products/${id}`)
  if (!resource.data) return <LoadState {...resource} />
  if (resource.data.archived) return <Notice message="This product is archived. Its history remains available." />
  return <Editor product={resource.data} />
}
export default function ProductForm({ id }) { return id ? <EditProduct id={id} /> : <Editor /> }
