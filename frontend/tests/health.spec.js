import { expect, test } from '@playwright/test'

const apiUrl = process.env.E2E_API_URL || 'http://localhost:8081'
test('backend health reflects the real database and unauthenticated inventory is protected', async ({ request, page }) => {
  const health = await request.get(apiUrl + '/api/health')
  expect(health.ok()).toBeTruthy()
  expect(await health.json()).toMatchObject({ status: 'UP', database: 'UP' })
  expect((await request.get(apiUrl + '/api/products')).status()).toBe(401)
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible()
})
test('login reports network failures and recovers to normal credential validation', async ({ page }) => {
  await page.goto('/#/login')
  await page.getByLabel('Email address').fill('nobody@example.invalid')
  await page.getByLabel('Password').fill('incorrect-password')
  await page.route('**/api/auth/login', route => route.abort('failed'))
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('Could not reach the server')
  await page.unroute('**/api/auth/login')
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('Email or password is incorrect')
})
