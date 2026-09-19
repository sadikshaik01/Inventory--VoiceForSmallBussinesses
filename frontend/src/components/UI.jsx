import { quantity, unitLabel } from '../services/format.js'

export function Icon({ name, className = 'h-5 w-5' }) {
  const paths = {
    home: 'M3 10 12 3l9 7v11h-6v-7H9v7H3Z',
    box: 'm3 7 9-4 9 4-9 4Zm0 0v10l9 4 9-4V7M12 11v10',
    mic: 'M9 6a3 3 0 0 1 6 0v6a3 3 0 0 1-6 0Zm-3 5v1a6 6 0 0 0 12 0v-1M12 18v4m-4 0h8',
    bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M9 21h6',
    history: 'M3 12a9 9 0 1 0 3-7M3 3v5h5m4 0v5l3 2',
    settings: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM5 5l2-2 3 1h4l3-1 2 2-1 3v8l1 3-2 2-3-1h-4l-3 1-2-2 1-3V8Z',
    plus: 'M12 5v14M5 12h14', minus: 'M5 12h14', logout: 'M9 3H3v18h6m5-14 5 5-5 5M8 12h11',
  }
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className={className} aria-hidden="true"><path d={paths[name] || paths.box} /></svg>
}
export function Button({ children, variant = 'primary', className = '', ...props }) {
  return <button className={`btn ${variant === 'primary' ? 'btn-primary' : variant === 'danger' ? 'btn-danger' : 'btn-secondary'} ${className}`} {...props}>{children}</button>
}
export function LinkButton({ to, children, secondary = false, className = '' }) {
  return <a href={`#${to}`} className={`btn ${secondary ? 'btn-secondary' : 'btn-primary'} ${className}`}>{children}</a>
}
export function Field({ label, hint, children, ...props }) {
  return <label className="block"><span className="mb-2 block text-sm font-semibold text-slate-700">{label}</span>{children || <input className="input" {...props} />}{hint && <span className="mt-1.5 block text-xs leading-5 text-slate-500">{hint}</span>}</label>
}
export function Notice({ message, success = false }) {
  return message ? <div role={success ? 'status' : 'alert'} className={`rounded-xl border p-4 text-sm ${success ? 'border-green-200 bg-green-50 text-green-800' : 'border-red-200 bg-red-50 text-red-800'}`}>{message}</div> : null
}
export function Status({ value }) {
  const style = { NORMAL: 'bg-emerald-50 text-emerald-700', LOW_STOCK: 'bg-amber-50 text-amber-800', OUT_OF_STOCK: 'bg-red-50 text-red-700' }
  const text = { NORMAL: 'In stock', LOW_STOCK: 'Low stock', OUT_OF_STOCK: 'Out of stock' }
  return <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold whitespace-nowrap ${style[value]}`}><span className="h-1.5 w-1.5 rounded-full bg-current" />{text[value]}</span>
}
export function Heading({ title, description, children }) {
  return <div className="mb-7 flex flex-wrap items-start justify-between gap-4"><div><h1 className="text-2xl font-bold tracking-tight text-[#173f35] sm:text-3xl">{title}</h1>{description && <p className="mt-2 text-sm leading-6 text-slate-500">{description}</p>}</div>{children}</div>
}
export function LoadState({ loading, error, reload }) {
  if (loading) return <div role="status" className="panel p-8 text-slate-500">Loading your workspace…</div>
  if (error) return <div className="space-y-4"><Notice message={error} /><Button variant="secondary" onClick={reload}>Try again</Button></div>
  return null
}
export function Empty({ title, text, children }) {
  return <div className="panel px-5 py-12 text-center"><div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-[#eef3e8] text-[#557d3b]"><Icon name="box" /></div><h2 className="text-lg font-semibold">{title}</h2><p className="mx-auto mt-2 mb-5 max-w-md text-sm leading-6 text-slate-500">{text}</p>{children}</div>
}
export function Pagination({ data, onPage }) {
  if (!data || data.totalPages <= 1) return null
  return <div className="mt-5 flex items-center justify-between gap-3"><Button variant="secondary" disabled={data.page === 0} onClick={() => onPage(data.page - 1)}>Previous</Button><span className="text-sm text-slate-500">Page {data.page + 1} of {data.totalPages}</span><Button variant="secondary" disabled={data.page + 1 >= data.totalPages} onClick={() => onPage(data.page + 1)}>Next</Button></div>
}
export function TransactionList({ items }) {
  return <div className="divide-y divide-slate-100">{items.map(t => <div key={t.id} className="flex flex-wrap items-center gap-3 py-4"><div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${t.type === 'ADD' ? 'bg-emerald-50 text-emerald-700' : 'bg-orange-50 text-orange-700'}`}><Icon name={t.type === 'ADD' ? 'plus' : 'minus'} /></div><div className="min-w-0 flex-1"><a className="font-semibold break-words hover:underline" href={`#/products/${t.productId}`}>{t.productName}</a><p className="mt-1 text-xs text-slate-500">{new Date(t.createdAt).toLocaleString()} · {t.source === 'VOICE' ? 'Voice' : 'Manual'}</p></div><div className="text-right"><p className="font-semibold tabular-nums">{t.type === 'ADD' ? '+' : '−'}{quantity(t.quantity)} {unitLabel(t.unit)}</p><p className="mt-1 text-xs text-slate-500">{t.type === 'ADD' ? 'Stock added' : 'Stock removed'}</p></div></div>)}</div>
}
