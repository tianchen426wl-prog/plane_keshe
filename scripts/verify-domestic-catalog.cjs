// Read-only verification against the running local backend. No account or writes required.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const base = process.env.CATALOG_BACKEND || 'http://127.0.0.1:18080'
async function api(endpoint, params = {}) {
  const response = await fetch(`${base}/api/${endpoint}?${new URLSearchParams(params)}`)
  assert.equal(response.status, 200)
  const body = await response.json()
  assert.equal(body.code, 200)
  return body.data
}
;(async () => {
  const airports = await api('airports')
  const calendar = await api('flights/calendar')
  const date = calendar.dates[2].serviceDate
  const flights = []
  for (let page = 1; ; page++) {
    const result = await api('flights', { date, page, pageSize: 100 })
    flights.push(...result.records)
    if (flights.length >= result.total) break
  }
  const reference = flights.filter(f => f.flightNo.startsWith('参考'))
  assert.equal(reference.length, 364)
  const pairs = new Set(reference.map(f => `${f.departureAirport}-${f.arrivalAirport}`))
  for (const f of reference) {
    assert(pairs.has(`${f.arrivalAirport}-${f.departureAirport}`))
    assert.equal((Date.parse(f.arrivalTime) - Date.parse(f.starttime)) / 60000, f.durationMinutes)
    assert(f.minTotal > 0)
  }
  for (const a of airports) assert(reference.some(f => f.departureAirport === a.code), a.code)
  assert(!pairs.has('SHA-NKG'))
  const samples = []
  for (const [start, end] of [['昆明','西双版纳'],['西双版纳','昆明'],['昆明','香格里拉'],['成都','拉萨'],['上海','长白山'],['北京','长春'],['哈尔滨','漠河']]) {
    const r = await api('flights', { start, end, date })
    assert.equal(r.exists, true)
    assert(r.records.length > 0)
    samples.push({ start, end, total: r.total, minTotal: r.records[0].minTotal })
  }
  const alias = await api('flights', { start: ' kmg ', end: '景洪', date })
  assert.equal(alias.exists, true)
  assert.equal(alias.records[0].durationMinutes, 55)
  const airportName = await api('flights', { start: '昆明长水国际机场', end: '西双版纳嘎洒国际机场', date })
  assert.equal(airportName.total, alias.total)
  const missing = await api('flights', { start: '漠河', end: '香格里拉', date })
  assert.equal(missing.exists, false)
  assert.equal(missing.total, 0)
  assert.equal(missing.records.length, 0)
  assert.equal(missing.message, '没有符合条件的航班')
  assert.equal(calendar.dates.length, 30)
  for (const day of calendar.dates) assert(day.flightCount >= 364)
  const evidence = {
    checkedAt: new Date().toISOString(), date, airports: airports.length,
    cities: new Set(airports.map(a => a.city)).size, returnPairs: pairs.size / 2,
    referenceTemplatesPerDay: reference.length, days: calendar.dates.length,
    generationRun: calendar.latestRun, allAirportsHaveReturnFlights: true,
    aliasAndAirportNameSearch: 'passed', durationArithmetic: 'passed',
    samples, missingRoute: { start: '漠河', end: '香格里拉', ...missing }
  }
  const target = path.join(__dirname, '../docs/evidence/domestic-network-verification.json')
  fs.writeFileSync(target, JSON.stringify(evidence, null, 2) + '\n')
  console.log(JSON.stringify(evidence, null, 2))
})().catch(error => { console.error(error); process.exitCode = 1 })
