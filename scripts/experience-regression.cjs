// Production UI with explicit API fixtures; never connects to a business database.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict')
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright')
const root=path.resolve(__dirname,'..'),base=path.join(root,'air-ticket-web/portal/dist'),output=path.join(root,'docs/evidence/experience')
const flight={hbid:73,start:'北京',end:'上海',flightNo:'航班0073',companyName:'云际航空',departureAirportName:'北京首都国际机场',arrivalAirportName:'上海虹桥国际机场',departureTerminal:'T2',arrivalTerminal:'T1',starttime:'2099-11-22T12:00:00+08:00',arrivalTime:'2099-11-22T14:00:00+08:00',saleStatus:1,operationalStatus:'SCHEDULED',saleOpenAt:'2020-01-01T00:00:00+08:00',saleCloseAt:'2099-11-22T11:00:00+08:00',number:10,minTotal:190,baggageKg:20,duration:'2小时',cabins:[{cabinId:7,cabinClass:1,cabinName:'经济舱',price:100,availableNumber:10,status:1}]}
const policy={baggageKg:20,description:'退订按购票规则办理'},items=[{code:'BASE',name:'基础运价',amount:200},{code:'TAX',name:'税费与燃油附加费',amount:180}]
const quote={quoteId:'fixture-quote',seats:['1B','1C'],passengers:['测试甲','测试乙'],quantity:2,policy,items,total:380,expiresAt:'2099-11-22T11:00:00+08:00'}
;(async()=>{
 fs.mkdirSync(output,{recursive:true});const browser=await chromium.launch({channel:'chrome',headless:true});const checks=[],errors=[],calls=[];let profiles=[{id:1,name:'测试甲'},{id:2,name:'测试乙'}]
 try{
  const context=await browser.newContext({viewport:{width:1440,height:1000},acceptDownloads:true})
  await context.addInitScript(()=>localStorage.setItem('portalUser',JSON.stringify({id:1,username:'体验测试',token:'fixture-user'})))
  await context.route('http://experience.test/**',async route=>{
   const request=route.request(),url=new URL(request.url()),p=url.pathname
   if(p.startsWith('/api/')){
    calls.push({path:p,query:Object.fromEntries(url.searchParams),method:request.method()});let data={}
    if(p==='/api/ai/capabilities')data={enabled:true}
    else if(p==='/api/ai/itineraries/plan')data={constraints:{date:'2099-11-22',quantity:2,budget:600},candidates:[{...flight,quantity:2,estimatedTotal:380}],message:'已找到合适航班'}
    else if(p==='/api/airports')data=[{code:'PEK',city:'北京',name:'北京首都国际机场'},{code:'SHA',city:'上海',name:'上海虹桥国际机场'}]
    else if(p.startsWith('/api/company/'))data={records:[{companyId:1,companyName:'云际航空'}]}
    else if(p==='/api/flights/calendar')data={windowStart:'2099-11-20',windowEnd:'2099-12-19',dates:[{serviceDate:'2099-11-22',minTotal:380,flightCount:1}]}
    else if(p==='/api/flights')data={records:[flight],total:1}
    else if(p==='/api/flights/73')data=flight
    else if(p.endsWith('/seats'))data={seats:['1B','1C','1D'].map(no=>({seatNo:no,seatRow:1,seatColumn:no.slice(1),seatFee:0,state:'AVAILABLE'}))}
    else if(p==='/api/user/info')data={username:'体验测试',name:'测试用户'}
    else if(p.startsWith('/api/user/passengers')){
     if(request.method()==='POST')profiles.push({id:3,name:request.postDataJSON().name})
     if(request.method()==='PUT')profiles=profiles.map(row=>row.id===Number(p.split('/').pop())?{...row,name:request.postDataJSON().name}:row)
     if(request.method()==='DELETE')profiles=profiles.filter(row=>row.id!==Number(p.split('/').pop()))
     data=profiles
    }
    else if(p==='/api/quotes'){const body=request.postDataJSON();assert.equal(body.quantity,2);assert.deepEqual(body.passengers,['测试甲','测试乙']);data=quote}
    else if(p==='/api/jpdd/hold'){assert.equal(request.postDataJSON().quantity,2);data={holdId:99}}
    else if(p==='/api/jpdd/holds/99')data={...flight,holdId:99,status:0,quantity:2,unitPrice:100,totalCost:380,priceSnapshot:quote,serverTime:new Date().toISOString(),expireTime:new Date(Date.now()+900000).toISOString()}
    else if(p==='/api/jpdd/create')data={ddid:42}
    else if(p==='/api/jpdd/orders/42/details')data={ddid:42,orderNo:'DEMO42',status:1,quantity:2,cost:380,ordertime:'2099-11-20T12:00:00+08:00',flight,flightSnapshot:{...flight,cabinName:'经济舱'},priceSnapshot:quote,passengers:[{passengerName:'测**',seatNo:'1B'},{passengerName:'测**',seatNo:'1C'}],timeline:[{action:'PAY',logTime:'2099-11-20T12:00:00+08:00',remark:'预订确认完成'}],refund:null}
    else throw Error('Unexpected fixture request '+request.method()+' '+p)
    return route.fulfill({json:{code:200,data}})
   }
   const file=path.resolve(base,'.'+decodeURIComponent(p==='/'?'/index.html':p));if(!file.startsWith(base+path.sep)||!fs.existsSync(file))return route.fulfill({status:404})
   return route.fulfill({path:file})
  })
  const page=await context.newPage();page.setDefaultTimeout(12000);page.on('pageerror',e=>errors.push(e.message))
  await page.goto('http://experience.test/#/planner');await page.locator('textarea').fill('后天北京到上海，两位成人，总预算600元');await page.getByRole('button',{name:'生成我的行程'}).click();await page.getByRole('button',{name:'查看航班与舱位'}).click()
  await page.locator('[data-testid="cabin-1"]').waitFor({state:'visible'});assert(page.url().includes('quantity=2'));checks.push('AI候选定位指定航班并保留2人和预算')
  await page.locator('[data-testid="cabin-1"]').click();await page.getByRole('button',{name:'填写旅客与选择座位'}).click()
  assert.equal(await page.locator('.passengers input').count(),2)
  await page.locator('.profile-picker .el-select').click();await page.getByRole('listitem').filter({hasText:'测试甲'}).click();await page.getByRole('listitem').filter({hasText:'测试乙'}).click();await page.keyboard.press('Escape')
  await page.locator('[data-testid="calculate-quote"]').click();await page.locator('[data-testid="quote-panel"]').waitFor();assert((await page.locator('[data-testid="quote-panel"]').innerText()).includes('380'));checks.push('常用乘机人填入、多人报价和三步预订')
  await page.setViewportSize({width:375,height:812});await page.screenshot({path:path.join(output,'mobile-booking.png'),fullPage:true})
  assert(await page.locator('[data-testid="reserve-cabin"]').isVisible());const dialog=await page.locator('.flight-booking-dialog').boundingBox();assert(dialog.width<=376);for(const title of await page.locator('.flight-booking-dialog .el-step__title').all()){assert((await title.boundingBox()).height<=36)}checks.push('375px手机端预订弹窗、步骤标题与操作按钮')
  await page.locator('[data-testid="reserve-cabin"]').click();await page.locator('[data-testid="confirm-booking"]').click();await page.getByRole('heading',{name:'预订成功'}).waitFor();checks.push('报价后锁位、进入确认页并模拟支付')
  await page.goto('http://experience.test/#/user/orders/42');await page.getByRole('heading',{name:'费用与行李'}).waitFor();await page.screenshot({path:path.join(output,'mobile-order-detail.png'),fullPage:true})
  const download=page.waitForEvent('download');await page.getByRole('button',{name:'加入日历（ICS）'}).click();const file=await download;const saved=path.join(output,'demo-itinerary.ics');await file.saveAs(saved);assert(fs.readFileSync(saved,'utf8').includes('DTSTART:20991122T040000Z'));checks.push('订单详情、业务时间线和北京时间日历导出')
  await page.setViewportSize({width:1440,height:1000});await page.screenshot({path:path.join(output,'desktop-order-detail.png'),fullPage:true})
  await page.goto('http://experience.test/#/user/center');await page.getByRole('tab',{name:'常用乘机人'}).click();await page.locator('.el-tab-pane:visible').getByPlaceholder('请输入姓名',{exact:true}).fill('测试丙');await page.getByRole('button',{name:'添加',exact:true}).click();await page.getByRole('cell',{name:'测试丙',exact:true}).waitFor();checks.push('个人中心管理常用乘机人')
  await page.goto('http://experience.test/#/flight?departure=北京&arrival=上海&date=2099-11-22&quantity=2&maxTotal=600');await page.locator('[data-testid="flight-73"]').waitFor();await page.screenshot({path:path.join(output,'desktop-search.png'),fullPage:true})
  const latest=calls.filter(c=>c.path==='/api/flights').at(-1),calendar=calls.filter(c=>c.path==='/api/flights/calendar').at(-1);assert.equal(latest.query.quantity,'2');assert.equal(calendar.query.quantity,'2');assert.equal(latest.query.maxTotal,'600');assert.equal(calendar.query.maxTotal,'600');checks.push('航班与日期价格请求使用相同人数及预算')
  await page.goto('http://experience.test/#/flight?departure=北京&arrival=上海&date=2099-11-22&quantity=3&maxTotal=600');await page.locator('.toolbar').filter({hasText:'3位成人'}).waitFor();await page.goBack();await page.locator('.toolbar').filter({hasText:'2位成人'}).waitFor();checks.push('同页面路由切换和浏览器后退恢复搜索条件')
  assert.deepEqual(errors,[]);fs.writeFileSync(path.join(output,'browser-results.json'),JSON.stringify({kind:'production UI with API fixtures; no live booking',checks,errors},null,2));console.log(JSON.stringify({checks,errors},null,2))
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1})
