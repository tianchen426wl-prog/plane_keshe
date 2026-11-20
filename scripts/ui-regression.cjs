// Offline browser regression: production assets with explicit API fixtures, no database writes.
const { chromium } = require('playwright')
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const root = path.resolve(__dirname, '..')
const output = path.join(root, 'docs/evidence/ui-regression')
const sections = ['company', 'flight', 'news', 'menu', 'role', 'refund']
const menus = sections.map((s, i) => ({ id: i + 1, path: '/admin/' + s, title: s, icon: 'el-icon-menu' }))
const row = { id: 2, hbid: 1, companyId: 1, companyName: '测试航空', username: 'reviewer', role: 1, title: '测试资讯', path: '/admin/company', icon: 'el-icon-office-building', start: '北京', end: '上海', starttime: '2026-11-22 12:00:00', price: 600, number: 30, process: 0, status: 1 }
;(async () => {
  fs.mkdirSync(output, { recursive: true })
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  const errors = [], checks = []
  let enabled = true, statusFailure = false, planFailure = false, planCalls = 0
  try {
    const context = await browser.newContext()
    await context.route('**/*', async route => {
      const url = new URL(route.request().url()), p = url.pathname
      if (p.startsWith('/api/')) {
        let data = { records: [row], total: 1 }
        if (p === '/api/user/login') data = { token: 'fixture-user', name: '测试用户' }
        if (p === '/api/ai/capabilities') {
          if (statusFailure) return route.fulfill({ status: 503, json: { code: 503, message: '测试：服务状态不可用' } })
          data = { enabled }
        }
        if (p === '/api/ai/itineraries/plan') {
          planCalls++
          assert.equal(route.request().headers().authorization, 'Bearer fixture-user')
          await new Promise(resolve => setTimeout(resolve, 300))
          if (planFailure) return route.fulfill({ status: 503, json: { code: 503, message: '测试：规划服务暂不可用' } })
          data = { message: '已筛选候选行程', constraints: { date: '2026-11-22' }, candidates: [{ ...row, estimatedTotal: 600, quantity: 1 }] }
        }
        if (p === '/api/airports') data = []
        if (p.endsWith('/cabins')) data = [{ cabinId: 1, cabinName: '经济舱', price: 600, totalNumber: 30, availableNumber: 30, status: 1 }]
        return route.fulfill({ json: { code: 200, data } })
      }
      const app = url.hostname === 'admin.test' ? 'admin' : 'portal'
      const file = path.join(root, 'air-ticket-web', app, 'dist', p === '/' ? 'index.html' : decodeURIComponent(p))
      return fs.existsSync(file) ? route.fulfill({ path: file }) : route.fulfill({ status: 404 })
    })
    const page = await context.newPage()
    page.on('pageerror', error => { errors.push(error.message); console.log('PAGE ERROR', error.message) })
    page.on('console', message => { if (message.type() === 'error') console.log('CONSOLE', message.text()) })
    page.setDefaultTimeout(10000)
    // Follow the same guarded route and login redirect as a real user.
    await page.goto('http://portal.test/#/planner')
    await page.waitForURL('**/#/login?redirect=*')
    await page.getByPlaceholder('请输入用户名', { exact: true }).fill('fixture')
    await page.getByPlaceholder('请输入密码', { exact: true }).fill('fixture123')
    await page.getByRole('button', { name: '登录并继续' }).click()
    await page.waitForURL('**/#/planner')
    const generate = page.getByRole('button', { name: '生成我的行程' })
    await generate.waitFor()
    assert.equal(await generate.isDisabled(), true)
    await page.getByLabel('告诉我你的出行想法', { exact: true }).fill('北京到上海，11月22日，1位成人，总预算1500元')
    await generate.click()
    await page.getByRole('button', { name: '正在规划行程' }).waitFor()
    await page.locator('.itinerary-card').waitFor()
    assert.equal(planCalls, 1)
    await page.screenshot({ path: path.join(output, 'planner-success.png'), fullPage: true })
    await page.getByRole('button', { name: '查看航班与舱位' }).click()
    await page.waitForURL('**/#/flight?*')
    assert.equal(new URLSearchParams(page.url().split('?')[1]).get('departure'), '北京')
    checks.push('登录跳转、空输入提示、生成进度、鉴权、候选结果和预订跳转')
    await page.goto('http://portal.test/#/planner')
    await page.getByLabel('告诉我你的出行想法', { exact: true }).fill('北京到上海')
    planFailure = true
    await generate.click()
    await page.locator('.error-state').waitFor()
    assert.match(await page.locator('.error-state').innerText(), /规划服务暂不可用/)
    assert.equal(await generate.isEnabled(), true)
    enabled = false
    await page.reload()
    await page.getByRole('button', { name: '重新连接' }).waitFor()
    assert.equal(await generate.isDisabled(), true)
    enabled = true
    await page.getByRole('button', { name: '重新连接' }).click()
    await page.getByText('智能规划已就绪', { exact: true }).waitFor()
    statusFailure = true
    await page.reload()
    await page.locator('.service-notice').waitFor()
    assert.match(await page.locator('.service-notice').innerText(), /暂时无法连接智能规划服务/)
    statusFailure = false
    await page.getByRole('button', { name: '重新连接' }).click()
    await page.getByText('智能规划已就绪', { exact: true }).waitFor()
    checks.push('规划错误保留、禁用状态、服务异常和重试恢复')
    await page.goto('http://admin.test/#/admin/login')
    await page.evaluate(menus => {
      localStorage.setItem('adminToken', 'fixture-admin')
      localStorage.setItem('adminInfo', JSON.stringify({ id: 1, role: 0, username: 'admin' }))
      localStorage.setItem('adminMenus', JSON.stringify(menus))
    }, menus)
    await page.reload()
    for (const width of [1440, 1024, 390]) {
      await page.setViewportSize({ width, height: 1000 })
      for (const section of sections) {
        await page.goto('http://admin.test/#/admin/' + section)
        await page.reload()
        console.log('Checking', section, width)
        await page.locator('.table-actions button:visible').first().waitFor().catch(async e => { console.log('ADMIN BODY', await page.locator('body').innerText()); console.log('URL',page.url()); throw e })
        const metrics = await page.locator('.table-actions > .cell:visible').first().evaluate((cell, compact) => {
          const buttons = [...cell.querySelectorAll('button')].map(b => b.getBoundingClientRect())
          const box = cell.getBoundingClientRect()
          return { aligned: buttons.every(b => Math.abs((compact ? b.left : b.top) - (compact ? buttons[0].left : buttons[0].top)) < 1), inside: buttons.every(b => b.left >= box.left && b.right <= box.right), margin: [...cell.querySelectorAll('button')].every(b => getComputedStyle(b).marginLeft === '0px') }
        }, width <= 600)
        assert.deepEqual(metrics, { aligned: true, inside: true, margin: true }, `${section} @ ${width}`)
        if (width === 1440 || width === 390) await page.screenshot({ path: path.join(output, `${section}-${width}.png`), fullPage: true })
        checks.push(`${section} @ ${width}: 按钮对齐、无溢出、无左边距偏移`)
      }
    }
    assert.deepEqual(errors, [])
    fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify({ mode: 'offline API fixtures; no live AI call or database writes', checks, pageErrors: errors }, null, 2))
    console.log(JSON.stringify({ passed: checks.length, pageErrors: errors, output }, null, 2))
  } finally { await browser.close() }
})().catch(error => { console.error(error); process.exitCode = 1 })
