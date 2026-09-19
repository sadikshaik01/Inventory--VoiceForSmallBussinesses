import { useState } from 'react'
import { useData } from '../services/useData.js'
import { Empty, Heading, LoadState, Pagination, TransactionList } from '../components/UI.jsx'

export default function Transactions({ params }) {
  const [page, setPage] = useState(0)
  const productId = params.get('productId')
  const resource = useData(`/transactions?page=${page}${productId ? `&productId=${encodeURIComponent(productId)}` : ''}`)
  return <><Heading title="Transaction history" description="Every stock update, newest first. Opening stock is recorded as an addition." />{!resource.data ? <LoadState {...resource} /> : resource.data.items.length ? <><div className="panel p-5 sm:p-6"><TransactionList items={resource.data.items} /></div><Pagination data={resource.data} onPage={setPage} /></> : <Empty title="No transactions yet" text="Your stock additions and removals will appear here." />}</>
}
