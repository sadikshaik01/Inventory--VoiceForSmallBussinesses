import { useEffect, useState } from 'react'
import { api, clearSession, getSession, navigate } from './services/api.js'
import { Icon } from './components/UI.jsx'
import AuthPage from './pages/AuthPage.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Inventory from './pages/Inventory.jsx'
import ProductForm from './pages/ProductForm.jsx'
import ProductDetails from './pages/ProductDetails.jsx'
import StockPage from './pages/StockPage.jsx'
import Transactions from './pages/Transactions.jsx'
import Settings from './pages/Settings.jsx'
import VoiceAssistant from './pages/VoiceAssistant.jsx'
import ActionCenter from './pages/ActionCenter.jsx'

const navigation = [
  ['/dashboard', 'Overview', 'home'], ['/inventory', 'Inventory', 'box'],
  ['/voice', 'Voice assistant', 'mic'], ['/alerts', 'Stock alerts', 'bell'],
  ['/actions', 'Action Center', 'bell'], ['/transactions', 'Transactions', 'history'], ['/settings', 'Settings', 'settings'],
]
export default function App() {
  const [user, setUser] = useState(() => getSession()?.user || null)
  const [route, setRoute] = useState(() => window.location.hash.slice(1) || '/dashboard')
  useEffect(() => {
    const changed = () => setRoute(window.location.hash.slice(1) || '/dashboard')
    const expired = () => { clearSession(); setUser(null); navigate('/login') }
    window.addEventListener('hashchange', changed)
    window.addEventListener('voicestock:expired', expired)
    const initialToken = getSession()?.token
    if (initialToken) api('/users/profile').then(profile => {
      if (getSession()?.token === initialToken) setUser(profile)
    }).catch(() => {})
    return () => { window.removeEventListener('hashchange', changed); window.removeEventListener('voicestock:expired', expired) }
  }, [])
  function logout() { clearSession(); setUser(null); navigate('/login') }
  const [path, query = ''] = route.split('?')
  if (!user) return <AuthPage key={path} register={path === '/register'} onLogin={setUser} />
  const params = new URLSearchParams(query)
  const segments = path.split('/').filter(Boolean)
  let content
  if (path === '/inventory') content = <Inventory />
  else if (path === '/alerts') content = <Inventory alerts />
  else if (path === '/products/new') content = <ProductForm />
  else if (segments[0] === 'products' && segments[1]) content = segments[2] === 'edit' ? <ProductForm id={segments[1]} /> : <ProductDetails id={segments[1]} />
  else if (path === '/stock') content = <StockPage params={params} />
  else if (path === '/transactions') content = <Transactions params={params} />
  else if (path === '/settings') content = <Settings user={user} onUpdate={setUser} />
  else if (path === '/actions') content = <ActionCenter />
  else if (path === '/voice') content = <VoiceAssistant />
  else content = <Dashboard user={user} />
  const active = to => path === to || (to === '/inventory' && (path.startsWith('/products') || path === '/stock'))
  return <div className="min-h-screen bg-[#f6f7f2] lg:pl-60">
    <a href="#main-content" className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded-lg focus:bg-white focus:p-3" onClick={e => { e.preventDefault(); document.getElementById('main-content')?.focus() }}>Skip to content</a>
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-60 flex-col bg-[#173f35] px-5 py-8 text-white lg:flex">
      <a href="#/dashboard" className="mb-12 flex items-center gap-2.5 px-2 text-2xl font-bold tracking-tight"><img src="/favicon.svg" width="37" height="37" alt="" />VoiceStock</a>
      <p className="mb-4 px-4 text-[10px] font-semibold uppercase tracking-[.18em] text-white/40">Workspace</p>
      <nav aria-label="Main navigation" className="space-y-2">{navigation.map(([to, label, icon]) => <a key={to} href={'#' + to} aria-current={active(to) ? 'page' : undefined} className={'flex min-h-12 items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-colors ' + (active(to) ? 'bg-white/10 text-[#d8efad]' : 'text-white/65 hover:bg-white/5 hover:text-white')}><Icon name={icon} />{label}</a>)}</nav>
      <div className="mt-auto border-t border-white/10 pt-6"><p className="px-3 text-sm font-semibold break-words">{user.businessName}</p><p className="mt-1 px-3 text-xs text-white/45">Your inventory, connected.</p><button onClick={logout} className="mt-4 flex min-h-11 w-full items-center gap-3 rounded-lg px-3 text-sm text-white/70 hover:bg-white/5"><Icon name="logout" />Log out</button></div>
    </aside>
    <header className="sticky top-0 z-20 flex min-h-20 items-center justify-between gap-3 border-b border-[#e5e9df] bg-[#f6f7f2]/95 px-5 backdrop-blur-sm sm:px-8 lg:px-10">
      <a href="#/dashboard" className="flex items-center gap-2 text-xl font-bold lg:hidden"><img src="/favicon.svg" width="32" height="32" alt="" />VoiceStock</a>
      <p className="hidden text-sm text-slate-500 lg:block">{user.businessName} <span className="mx-3 text-slate-300">/</span> Workspace</p>
      <div className="flex items-center gap-3"><a href="#/voice" aria-label="Open voice assistant" className="flex h-11 w-11 items-center justify-center rounded-full bg-[#e8efdc] text-[#456533]"><Icon name="mic" /></a><a href="#/settings" aria-label="Open profile" className="flex h-10 w-10 items-center justify-center rounded-full border border-[#dce4d0] bg-white text-sm font-bold">{user.name.slice(0, 1).toUpperCase()}</a><details className="relative lg:hidden"><summary className="flex min-h-11 cursor-pointer items-center px-2 text-sm font-semibold">Menu</summary><div className="absolute right-0 mt-3 w-52 rounded-xl border border-slate-200 bg-white p-2 shadow-lg">{navigation.map(([to, label]) => <a key={to} href={'#' + to} onClick={e => e.currentTarget.closest('details').removeAttribute('open')} className="block rounded-lg px-4 py-3 text-sm hover:bg-slate-50">{label}</a>)}<button onClick={logout} className="w-full rounded-lg px-4 py-3 text-left text-sm text-red-700">Log out</button></div></details></div>
    </header>
    <main id="main-content" tabIndex={-1} className="mx-auto max-w-7xl px-5 py-7 pb-28 outline-none sm:px-8 lg:px-10 lg:py-10"><div key={route}>{content}</div></main>
    <nav aria-label="Mobile navigation" className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-4 border-t border-[#e0e6d8] bg-white px-2 pb-[env(safe-area-inset-bottom)] lg:hidden">{[['/dashboard','Home','home'],['/inventory','Inventory','box'],['/voice','Voice','mic'],['/alerts','Alerts','bell']].map(([to,label,icon]) => <a href={'#' + to} key={to} aria-current={active(to) ? 'page' : undefined} className={'flex min-h-18 flex-col items-center justify-center gap-1 text-[11px] font-medium ' + (active(to) ? 'text-[#3d682a]' : 'text-slate-500')}><Icon name={icon} />{label}</a>)}</nav>
  </div>
}

