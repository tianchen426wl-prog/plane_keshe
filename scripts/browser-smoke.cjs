/* End-to-end checks against a deliberately started air_ticket_test backend only. */
const http = require('node:http'),
  fs = require('node:fs'),
  path = require('node:path'),
  assert = require('node:assert/strict')
const { chromium } = require('playwright')
if (process.env.E2E_CONFIRM_TEST_DB !== 'air_ticket_test')
  throw Error(
    'Start backend against air_ticket_test and explicitly confirm E2E_CONFIRM_TEST_DB'
  )
const root = path.resolve(__dirname, '..'),
  backend = new URL(process.env.E2E_BACKEND || 'http://127.0.0.1:18085')
const evidence = path.join(root, 'docs', 'evidence')
fs.mkdirSync(evidence, { recursive: true })
function serve(app, port) {
  const base = path.join(root, 'air-ticket-web', app, 'dist')
  const server = http.createServer((req, res) => {
    if (req.url.startsWith('/api/')) {
      const proxy = http.request(
        {
          hostname: backend.hostname,
          port: backend.port,
          path: req.url,
          method: req.method,
          headers: { ...req.headers, host: backend.host },
        },
        (r) => {
          res.writeHead(r.statusCode, r.headers)
          r.pipe(res)
        }
      )
      proxy.on('error', () => {
        res.statusCode = 502
        res.end('Backend unavailable')
      })
      req.pipe(proxy)
      return
    }
    const file = path.resolve(
      base,
      '.' +
        decodeURIComponent(
          req.url.split('?')[0] === '/' ? '/index.html' : req.url.split('?')[0]
        )
    )
    if (
      !file.startsWith(base + path.sep) ||
      !fs.existsSync(file) ||
      !fs.statSync(file).isFile()
    ) {
      res.statusCode = 404
      res.end()
      return
    }
    const types = {
      '.js': 'application/javascript',
      '.css': 'text/css',
      '.html': 'text/html; charset=utf-8',
      '.png': 'image/png',
      '.jpg': 'image/jpeg',
      '.woff': 'font/woff',
      '.ttf': 'font/ttf',
    }
    res.setHeader(
      'Content-Type',
      types[path.extname(file).toLowerCase()] || 'application/octet-stream'
    )
    fs.createReadStream(file).pipe(res)
  })
  return new Promise((resolve) =>
    server.listen(port, '127.0.0.1', () => resolve(server))
  )
}
;(async () => {
  let browser
  const servers = []
  const errors = []
  try {
    servers.push(await serve('portal', 18081), await serve('admin', 18082))
    browser = await chromium.launch({ channel: 'chrome', headless: true })
    const context = await browser.newContext({
      viewport: { width: 1440, height: 1000 },
    })
    await context.route('**/*', (route) => {
      const u = new URL(route.request().url())
      return ['127.0.0.1', 'localhost'].includes(u.hostname) ||
        u.protocol === 'data:'
        ? route.continue()
        : route.abort()
    })
    const tag = 'e2e' + Date.now()
    const futureDate = new Date(Date.now()+2*86400000).toLocaleDateString('en-CA',{timeZone:'Asia/Shanghai'})
    const flightUrl='http://127.0.0.1:18081/#/flight?departure='+encodeURIComponent('北京')+'&arrival='+encodeURIComponent('上海')+'&date='+futureDate
    const registration = await context.request.post(
      'http://127.0.0.1:18081/api/user/register',
      {
        data: {
          username: tag,
          password: 'browser123456',
          name: tag,
          gender: 0,
        },
      }
    )
    assert.equal(registration.status(), 200)
    const page = await context.newPage()
    page.on('pageerror', (e) => errors.push(e.message))
    page.on('response', (r) => {
      if (r.url().includes('/api/'))
        console.log(r.status(), new URL(r.url()).pathname)
    })
    await page.goto('http://127.0.0.1:18081/#/login')
    await page.getByPlaceholder('请输入用户名', { exact: true }).fill(tag)
    await page
      .getByPlaceholder('请输入密码', { exact: true })
      .fill('browser123456')
    await page.locator('.login-card button').click()
    await page.waitForURL('**/#/')
    await page.goto(flightUrl)
    await page.getByPlaceholder('请输入出发城市').fill('北京')
    await page.getByPlaceholder('请输入到达城市').fill('上海')
    await page.locator('.search-form button.el-button--primary').click()
    await page.locator('.toolbar .el-select').first().click()
    await page.locator('.el-select-dropdown:visible').getByText('吉祥航空',{exact:true}).waitFor()
    assert.equal(await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').count(),14)
    await page.locator('.el-select-dropdown:visible').getByText('中国国际航空',{exact:true}).click()
    await page.locator('[data-testid=choose-flight]:not([disabled])').first().waitFor()
    await page.locator('[data-testid=choose-flight]:not([disabled])').first().click()
    await page.locator('[data-testid=cabin-1]').click()
    await page.getByPlaceholder('请输入旅客1姓名').fill('浏览器测试旅客')
    await page.locator('[data-testid=seat-5A]').click()
    await page.locator('[data-testid=calculate-quote]').click()
    await page.locator('[data-testid=quote-panel]').waitFor()
    await page.screenshot({path:path.join(evidence,'feature-seat-map-'+Date.now()+'.png'),fullPage:true})
    await page.locator('[data-testid=reserve-cabin]').click()
    await page.waitForURL('**/#/booking/*')
    await page.locator('[data-testid=confirm-booking]').waitFor()
    await page.reload()
    await page.locator('[data-testid=confirm-booking]').waitFor()
    await page.screenshot({
      path: path.join(evidence, '01-reserved-cabin.png'),
      fullPage: true,
      animations: 'disabled',
    })
    await page.locator('[data-testid=confirm-booking]').click()
    await page.getByText('模拟购买成功', { exact: true }).waitFor()
    await page.goto('http://127.0.0.1:18081/#/user/orders')
    await page.getByRole('tab', { name: '已购票', exact: true }).click()
    await page.locator('[data-testid=refund-order]').first().click()
    await page.locator('.el-message-box').getByRole('button').last().click()
    await page.getByRole('tab', { name: '退订中', exact: true }).click()
    await page.getByText('北京 → 上海', { exact: true }).first().waitFor()
    await page.screenshot({
      path: path.join(evidence, '02-refund-requested.png'),
      fullPage: true,
      animations: 'disabled',
    })
    const admin = await context.newPage()
    admin.on('pageerror', (e) => errors.push(e.message))
    await admin.goto('http://127.0.0.1:18082/#/admin/login')
    await admin.getByPlaceholder('请输入用户名', { exact: true }).fill('admin')
    await admin.getByPlaceholder('请输入密码', { exact: true }).fill('admin123')
    await admin.locator('.login-card button').click()
    await admin.waitForURL('**/#/admin/dashboard')
    await admin.goto('http://127.0.0.1:18082/#/admin/flight')
    await admin.locator('.el-collapse-item__header').filter({hasText:'每日航班、班期与票价规则'}).click()
    await admin.getByText('SUCCESS',{exact:true}).first().waitFor()
    await admin.getByRole('tab',{name:'产品规则',exact:true}).click()
    await admin.getByText('标准成人演示产品 · DEMO_STANDARD_V1',{exact:true}).waitFor()
    await admin.goto('http://127.0.0.1:18082/#/admin/refund')
    const row = admin
      .locator('.el-table__fixed-right tr')
      .filter({ hasText: tag })
    await row.getByRole('button', { name: '审核通过', exact: true }).click()
    await admin
      .locator('.el-message-box')
      .getByRole('button', { name: '确定通过', exact: true })
      .click()
    await admin.getByRole('tab', { name: '已通过', exact: true }).click()
    await admin.getByText(tag, { exact: true }).first().waitFor()
    await admin.screenshot({
      path: path.join(evidence, '03-refund-approved.png'),
      fullPage: true,
      animations: 'disabled',
    })
    await page.reload()
    await page.getByRole('tab', { name: '已退订', exact: true }).click()
    await page.getByText('北京 → 上海', { exact: true }).first().waitFor()
    await admin.goto('http://127.0.0.1:18082/#/admin/user')
    await admin.locator('.el-table__body-wrapper').first().waitFor()
    await admin.goto('http://127.0.0.1:18082/#/admin/order')
    await admin
      .locator('.el-table__fixed-right')
      .getByRole('button', { name: '日志', exact: true })
      .first()
      .click()
    await admin.getByText('订单生命周期日志', { exact: true }).waitFor()
    await admin.screenshot({
      path: path.join(evidence, '04-order-history.png'),
      fullPage: true,
      animations: 'disabled',
    })
    const responsive = []
    for (const width of [1440, 390]) {
      await page.setViewportSize({ width, height: 900 })
      await admin.setViewportSize({ width, height: 900 })
      for (const route of [
        '/',
        '/flight',
        '/user/orders',
        '/user/center',
        '/company',
        '/news',
        '/planner',
      ]) {
        await page.goto('http://127.0.0.1:18081/#' + route)
        await page.locator('.portal-footer').waitFor()
        await page.waitForTimeout(200)
        assert.ok(
          await page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth + 1
          ),
          route + ' overflow ' + width
        )
        responsive.push({ route, width })
      }
      for (const route of [
        '/admin/dashboard',
        '/admin/flight',
        '/admin/order',
        '/admin/refund',
        '/admin/user',
        '/admin/role',
      ]) {
        await admin.goto('http://127.0.0.1:18082/#' + route)
        await admin.locator('.admin-main').waitFor()
        await admin.waitForTimeout(200)
        assert.ok(
          await admin.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth + 1
          ),
          route + ' overflow ' + width
        )
        responsive.push({ route, width })
      }
    }
    await page.goto(flightUrl)
    await page.locator('[data-testid=choose-flight]').first().waitFor()
    await page.screenshot({
      path: path.join(evidence, 'ui-live-mobile.png'),
      fullPage: true,
      animations: 'disabled',
    })
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.goto('http://127.0.0.1:18081/#/')
    await page.locator('.route-card').first().waitFor()
    await page.screenshot({
      path: path.join(evidence, 'ui-live-home.png'),
      fullPage: true,
      animations: 'disabled',
    })
    await page.setViewportSize({ width: 390, height: 900 })
    await page.goto(flightUrl)
    await page.locator('[data-testid=choose-flight]:not([disabled])').first().click()
    await page.locator('[data-testid=cabin-1]').click()
    await page.getByPlaceholder('请输入旅客1姓名').fill('浏览器测试旅客')
    await page.locator('[data-testid=seat-5A]').click()
    await page.locator('[data-testid=calculate-quote]').click()
    await page.locator('[data-testid=quote-panel]').waitFor()
    await page.screenshot({path:path.join(evidence,'feature-seat-map-'+Date.now()+'.png'),fullPage:true})
    await page.locator('[data-testid=reserve-cabin]').click()
    await page.waitForURL('**/#/booking/*')
    await page.locator('[data-testid=confirm-booking]').waitFor()
    assert.ok(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1
      ),
      'mobile booking overflow'
    )
    await page.screenshot({
      path: path.join(evidence, 'ui-live-mobile-booking.png'),
      fullPage: true,
      animations: 'disabled',
    })
    await page
      .getByRole('button', { name: '取消占位并释放', exact: true })
      .click()
    await page.locator('.el-message-box').getByRole('button').last().click()
    await page.waitForURL(url => url.hash.startsWith('#/flight'))
    await page.route('**/api/flights?*', (route) =>
      route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ code: 503, message: '测试查询失败' }),
      })
    )
    await page.locator('[data-testid=search-flights]').click()
    await page.locator('.ui-error').waitFor()
    assert.equal(await page.locator('.el-empty').count(), 0)
    await page.unroute('**/api/flights?*')
    await page.locator('[data-testid=search-flights]').click()
    await page.locator('[data-testid=choose-flight]').first().waitFor()
    const oldToken = await page.evaluate(
      () => JSON.parse(localStorage.getItem('portalUser')).token
    )
    await page.locator('.mobile-menu').click()
    await page.locator('.account-button').click()
    await page.getByText('退出登录', { exact: true }).click()
    await page.waitForURL('**/#/')
    const revoked = await context.request.get(
      'http://127.0.0.1:18081/api/user/info',
      { headers: { Authorization: 'Bearer ' + oldToken } }
    )
    assert.equal(revoked.status(), 401)
    assert.deepEqual(errors, [])
    const result = {
      passed: true,
      database: 'air_ticket_test',
      testUser: tag,
      steps: [
        'browser user login',
        'search future flights',
        'select cabin and paid seat',
        'calculate itemized server quote',
        'reserve with server expiry',
        'simulate payment',
        'request refund',
        'browser admin login',
        'approve refund',
        'verify refunded order',
        'users pagination',
        'order history',
        '14 airline filter choices',
        'generation log and fare policy management',
      ],
      pageErrors: errors,
      responsive,
    }
    fs.writeFileSync(
      path.join(evidence, 'browser-smoke.json'),
      JSON.stringify(result, null, 2)
    )
    console.log(JSON.stringify(result))
  } catch (e) {
    if (browser) {
      for (const c of browser.contexts())
        for (const p of c.pages())
          await p
            .screenshot({
              path: path.join(
                evidence,
                'browser-failure-' + Date.now() + '.png'
              ),
              fullPage: true,
            })
            .catch(() => {})
    }
    throw e
  } finally {
    if (browser) await browser.close()
    await Promise.all(
      servers.map((s) => new Promise((resolve) => s.close(resolve)))
    )
  }
})().catch((e) => {
  console.error(e)
  process.exitCode = 1
})
