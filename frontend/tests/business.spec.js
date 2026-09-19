import { expect, test } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { mkdir, writeFile, readFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import { execFileSync } from 'node:child_process'

// This local fixture changes timestamps only on the exact test user's API-created records.
async function observeSevenDays(userId, productId) {
  if (![userId, productId].every(v => /^[0-9a-f-]{36}$/.test(v))) throw new Error('Invalid fixture ID')
  const password = (await readFile(new URL('../../.run/postgres/app-password.txt', import.meta.url), 'utf8')).trim()
  const sql = `BEGIN; UPDATE products SET created_at=now()-interval '8 days' WHERE id='${productId}' AND user_id='${userId}' AND user_id IN (SELECT id FROM users WHERE email LIKE 'e2e-%@example.invalid'); WITH removals AS (SELECT id,row_number() OVER (ORDER BY created_at,id) n FROM inventory_transactions WHERE user_id='${userId}' AND product_id='${productId}' AND transaction_type='REMOVE') UPDATE inventory_transactions t SET created_at=now()-(CASE WHEN r.n=1 THEN interval '6 days' ELSE interval '1 day' END) FROM removals r WHERE t.id=r.id; COMMIT;`
  execFileSync('C:/Program Files/PostgreSQL/17/bin/psql.exe', ['-h','127.0.0.1','-p','55432','-U','voicestock_app','-d','voicestock','-v','ON_ERROR_STOP=1','-c',sql], { env: { ...process.env, PGPASSWORD: password }, stdio: 'pipe', windowsHide: true })
}

test('natural understanding and persisted business action journey', async ({ page, request }, testInfo) => {
  test.setTimeout(90000)
  const errors = []; page.on('pageerror', e => errors.push(e.message))
  const base = 'http://localhost:8081/api'
  const registration = await request.post(`${base}/auth/register`, {data:{ name:'Action Demo',businessName:'Business Test',email:`e2e-${randomUUID()}@example.invalid`,password:`Test-${randomUUID()}`,preferredLanguage:'mixed' }})
  expect(registration.status()).toBe(201)
  const session = await registration.json(), headers = {Authorization:`Bearer ${session.token}`}
  const fixture = testInfo.outputPath('fixture-user.json'); await mkdir(dirname(fixture), {recursive:true}); await writeFile(fixture, JSON.stringify({userId:session.user.id}))
  await page.goto('/'); await page.evaluate(s => sessionStorage.setItem('voicestock.session',JSON.stringify(s)),session); await page.reload()
  async function product(name,unit,stock,min) { const r=await request.post(`${base}/products`,{headers,data:{name,category:'Grocery',unit,currentStock:stock,minimumStock:min}});expect(r.status()).toBe(201);return (await r.json()).id }
  const rice=await product('Rice','BAGS',40,10)
  await product('Sugar','KG',4,5);await product('Biscuits','BOXES',0,5)
  // Verify PACKETS appears in the real product form.
  await page.goto('/#/products/new');await page.getByLabel('Product name').fill('Chips');await page.getByLabel('Category').fill('Snacks');await page.getByLabel('Unit',{exact:false}).selectOption('PACKETS');await page.getByLabel('Initial stock').fill('20');await page.getByLabel('Minimum stock',{exact:false}).fill('10');await page.getByRole('button',{name:'Create product',exact:true}).click();await expect(page.getByTestId('product-stock')).toHaveText('20 packets')
  const chips=page.url().split('/').at(-1)
  await page.goto('/#/voice')
  async function command(text) { await page.getByLabel('Type a command',{exact:true}).fill(text);await page.getByRole('button',{name:'Process',exact:true}).click() }
  async function stock(id) { return (await (await request.get(`${base}/products/${id}`,{headers})).json()).currentStock }
  async function confirm(expected) { await page.getByRole('button',{name:'Confirm stock change'}).click();await expect(page.getByRole('status')).toContainText(`Chips now has ${expected} packets`) }
  await command('Add 10 packets of chips');await expect(page.getByTestId('after-update')).toHaveText('30 packets');await confirm(30)
  await command('10 packets of chips');await expect(page.getByRole('status')).toContainText('What would you like to do?');await expect(page.getByRole('button',{name:'Confirm stock change'})).toHaveCount(0);expect(await stock(chips)).toBe(30)
  await page.getByRole('button',{name:'Add stock',exact:true}).click();await expect(page.getByTestId('after-update')).toHaveText('40 packets');await confirm(40)
  await command('Received 5 packets of chips');await expect(page.getByTestId('after-update')).toHaveText('45 packets');await confirm(45)
  await command('Sold 2 packets of chips');await expect(page.getByTestId('after-update')).toHaveText('43 packets');await confirm(43)
  await command('Add chips');await page.getByRole('button',{name:'Correct missing details'}).click();await page.getByLabel('Quantity',{exact:true}).fill('2');await page.getByLabel('Unit',{exact:true}).selectOption('PACKETS');await page.getByRole('button',{name:'Review changes'}).click();await expect(page.getByTestId('after-update')).toHaveText('45 packets');await page.getByRole('button',{name:'Cancel',exact:true}).click();expect(await stock(chips)).toBe(43)
  await product('Sunflower Oil','LITRES',10,2);await product('Groundnut Oil','LITRES',10,2)
  await command('Add 2 ltrs oil');await expect(page.getByRole('status')).toContainText('Several products match');await page.getByRole('button',{name:/Select Sunflower Oil/}).click();await expect(page.getByTestId('after-update')).toHaveText('12 litres');await page.getByRole('button',{name:'Cancel',exact:true}).click()
  await command('राइस में 5 बैग ऐड करो');await expect(page.getByTestId('after-update')).toHaveText('45 bags');await page.getByRole('button',{name:'Cancel',exact:true}).click()
  await command('राइस स्टॉक कितना है');await expect(page.getByRole('status')).toHaveText('You have 40 bags of Rice.')
  // Confirm the existing speech language selector passes all three actual locale values.
  await page.evaluate(() => { window.SpeechRecognition=class { start(){ window.__speechLocale=this.lang;this.onresult({results:[[{transcript:'Rice entha undi?'}]]});this.onend() } abort(){} } })
  for(const locale of ['en-IN','hi-IN','te-IN']) { await page.getByLabel('Speech language',{exact:false}).selectOption(locale);await page.getByRole('button',{name:'Tap to Speak'}).click();await expect(page.getByRole('status')).toHaveText('You have 40 bags of Rice.');expect(await page.evaluate(()=>window.__speechLocale)).toBe(locale) }
  for(let i=0;i<2;i++) expect((await request.post(`${base}/inventory/remove`,{headers,data:{productId:rice,quantity:14,unit:'BAGS'}})).status()).toBe(200)
  await observeSevenDays(session.user.id,rice)
  await page.goto('/#/actions')
  const insight=page.getByTestId('insight-Rice')
  await expect(insight).toContainText('RUNNING OUT SOON');await expect(insight).toContainText('4 bags/day');await expect(insight).toContainText('~3 days');await expect(insight).toContainText('Suggested order: 16 bags')
  await expect(page.getByTestId('insight-Sugar')).toContainText('Not enough recent usage data')
  await insight.getByRole('button',{name:'Approve reorder'}).click();await expect(page.getByRole('status')).toContainText('Reorder approved and saved');await page.reload();await expect(page.getByTestId('action-Rice')).toContainText('APPROVED');expect(await stock(rice)).toBe(12)
  await page.getByTestId('action-Rice').getByRole('button',{name:'Mark completed'}).click();await expect(page.getByRole('status')).toContainText('Reorder marked completed');await page.reload();await expect(page.getByTestId('action-Rice')).toContainText('COMPLETED');expect(await stock(rice)).toBe(12)
  await page.getByTestId('action-Rice').getByRole('link',{name:'Add received stock'}).click();await expect(page.getByLabel('Quantity (bags)')).toHaveValue('16');expect(await stock(rice)).toBe(12)
  await page.goto('/#/voice')
  for(const question of ['What needs my attention?','What should I reorder?','What is running out soon?','How long will rice last?','Why should I reorder rice?']) { await command(question);await expect(page.getByTestId('insight-Rice')).toContainText('~3 days') }
  await command('What actions are pending?');await expect(page.getByRole('status')).toContainText('No approved reorder actions')
  await command('What reorders did I approve?');await expect(page.getByTestId('action-Rice')).toContainText('COMPLETED')
  for(const width of [375,768,1280]) { await page.setViewportSize({width,height:900});for(const route of ['dashboard','actions','voice']) { await page.goto(`/#/${route}`);await expect(page.getByRole('heading',{level:1})).toBeVisible();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true) }await page.goto('/#/actions');await expect(page.getByTestId('action-Rice')).toBeVisible();await page.screenshot({path:testInfo.outputPath(`actions-${width}.png`),fullPage:true}) }
  const center=await (await request.get(`${base}/business`,{headers})).json();expect(center.lowStock).toBe(1);expect(center.outOfStock).toBe(1);expect(center.runningOutSoon).toBe(1)
  expect(errors).toEqual([])
})
