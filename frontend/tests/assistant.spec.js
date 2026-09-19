import { expect, test } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'

for (const width of [375, 768, 1280]) {
  test(`assistant exact demo and safety at ${width}px`, async ({ page, request }, testInfo) => {
    test.setTimeout(90000)
    await page.setViewportSize({ width, height: 900 })
    const errors = []; page.on('pageerror', e => errors.push(e.message))
    const base = 'http://localhost:8081/api'
    const response = await request.post(`${base}/auth/register`, { data: { name: 'Voice Demo', businessName: 'Demo Store', email: `e2e-${randomUUID()}@example.invalid`, password: `Test-${randomUUID()}`, preferredLanguage: 'mixed' } })
    expect(response.status()).toBe(201)
    const session = await response.json()
    const fixture = testInfo.outputPath('fixture-user.json'); await mkdir(dirname(fixture), { recursive: true }); await writeFile(fixture, JSON.stringify({ userId: session.user.id }))
    const headers = { Authorization: `Bearer ${session.token}` }
    await page.goto('/')
    await page.evaluate(s => sessionStorage.setItem('voicestock.session', JSON.stringify(s)), session)
    await page.reload()
    async function create(name, unit, stock, minimum) {
      await page.goto('/#/products/new')
      await page.getByLabel('Product name').fill(name); await page.getByLabel('Category').fill('Grocery')
      await page.getByLabel('Unit', { exact: false }).selectOption(unit)
      await page.getByLabel('Initial stock').fill(String(stock)); await page.getByLabel('Minimum stock', { exact: false }).fill(String(minimum))
      await page.getByRole('button', { name: 'Create product', exact: true }).click()
      await expect(page.getByRole('heading', { name, exact: true })).toBeVisible()
      return page.url().split('#')[1].split('/').at(-1)
    }
    const rice = await create('Rice', 'BAGS', 50, 10)
    await page.getByRole('link', { name: '+ Add stock', exact: true }).click()
    await page.getByLabel('Quantity (bags)').fill('20'); await page.getByRole('button', { name: 'Confirm add stock' }).click()
    await expect(page.getByRole('status')).toContainText('70 bags')
    // Use the product details link to preserve the existing stock route contract.
    await page.goto(`/#/products/${rice}`); await page.getByRole('link', { name: '− Remove stock', exact: true }).click()
    await page.getByLabel('Quantity (bags)').fill('5'); await page.getByRole('button', { name: 'Confirm remove stock' }).click()
    await expect(page.getByRole('status')).toContainText('65 bags')
    await create('Sugar', 'KG', 4, 5); await create('Biscuits', 'BOXES', 0, 5)
    await page.goto('/#/voice')
    async function command(text) { await page.getByLabel('Type a command', { exact: true }).fill(text); await page.getByRole('button', { name: 'Process', exact: true }).click() }
    async function stock() { return (await (await request.get(`${base}/products/${rice}`, { headers })).json()).currentStock }
    for (const [text, before, after] of [['Add 20 bags of rice',65,85],['Rice 5 bags add cheyyi',85,90],['Rice mein se 5 bags hatao',90,85]]) {
      await command(text)
      await expect(page.getByTestId('current-stock')).toHaveText(`${before} bags`)
      await expect(page.getByTestId('after-update')).toHaveText(`${after} bags`)
      expect(await stock()).toBe(before)
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
      await page.screenshot({ path: testInfo.outputPath(`confirmation-${before}.png`), fullPage: true })
      await page.getByRole('button', { name: 'Confirm stock change' }).click()
      await expect(page.getByRole('status')).toContainText(`Rice now has ${after} bags`)
      expect(await stock()).toBe(after)
    }
    await command('How much rice do I have?'); await expect(page.getByRole('status')).toHaveText('You have 85 bags of Rice.')
    await command('Which products are running low?'); await expect(page.locator('article')).toHaveCount(1); await expect(page.locator('article')).toContainText('Sugar')
    await command('Which products are out of stock?'); await expect(page.locator('article')).toHaveCount(1); await expect(page.locator('article')).toContainText('Biscuits')
    await command('What needs to be ordered?'); await expect(page.locator('article').filter({hasText:'Sugar'})).toContainText('Suggested order: 6 kg'); await expect(page.locator('article').filter({hasText:'Biscuits'})).toContainText('Suggested order: 10 boxes')
    await command('Remove 500 bags of rice'); await expect(page.getByRole('status')).toContainText('Only 85 bags available'); await expect(page.getByRole('button', { name: 'Confirm stock change' })).toHaveCount(0)
    await command('Add rice'); await expect(page.getByRole('status')).toContainText('quantity and unit')
    await command('Add 100 bags of rice'); await page.getByRole('button', {name:'Cancel',exact:true}).click(); await expect(page.getByRole('button', {name:'Confirm stock change'})).toHaveCount(0); expect(await stock()).toBe(85)
    await command('Add 10 bags of rice'); await page.getByRole('button', {name:'Edit',exact:true}).click()
    await page.getByLabel('Action', {exact:true}).selectOption('REMOVE_STOCK'); await page.getByLabel('Quantity', {exact:true}).fill('2')
    await page.getByLabel('Product name', {exact:true}).fill('Sugar'); await page.getByLabel('Unit', {exact:true}).selectOption('KG')
    await page.getByRole('button',{name:'Review changes'}).click(); await expect(page.getByTestId('after-update')).toHaveText('2 kg'); await page.getByRole('button', {name:'Cancel',exact:true}).click(); expect(await stock()).toBe(85)
    await page.evaluate(() => { window.SpeechRecognition = undefined; window.webkitSpeechRecognition = undefined })
    await page.getByRole('button', {name:'Tap to Speak'}).click(); await expect(page.getByRole('alert')).toContainText('unavailable')
    await page.evaluate(() => { window.SpeechRecognition = class { start() { this.onerror({error:'not-allowed'}); this.onend() } abort() {} } })
    await page.getByRole('button', {name:'Tap to Speak'}).click(); await expect(page.getByRole('alert')).toContainText('permission was denied')
    await page.evaluate(() => { window.SpeechRecognition = class { start() { this.onresult({results:[[{transcript:'How much rice do I have?'}]]}); this.onend() } abort() {} } })
    await page.getByRole('button', {name:'Tap to Speak'}).click(); await expect(page.getByRole('status')).toHaveText('You have 85 bags of Rice.')
    await page.route('**/api/assistant/interpret', route => route.abort())
    await command('Add 1 bags rice'); await expect(page.getByRole('alert')).toContainText('Could not reach the server'); await page.unroute('**/api/assistant/interpret')
    await command('Rice entha undi?'); await expect(page.getByRole('status')).toHaveText('You have 85 bags of Rice.')
    await page.goto(`/#/products/${rice}`); await page.reload(); await expect(page.getByTestId('product-stock')).toHaveText('85 bags')
    const history = await (await request.get(`${base}/transactions`, {headers})).json()
    expect(history.items.filter(t => t.source === 'VOICE')).toHaveLength(3)
    await page.goto('/#/transactions'); await expect(page.getByText('+20 bags', {exact:true})).toHaveCount(2)
    for (const route of ['dashboard','inventory','alerts','voice']) { await page.goto(`/#/${route}`); await expect(page.getByRole('heading',{level:1})).toBeVisible(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true) }
    expect(errors).toEqual([])
  })
}

