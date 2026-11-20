const fs=require('node:fs'),assert=require('node:assert/strict')
;(async()=>{
 const source=fs.readFileSync('air-ticket-web/portal/src/utils/itinerary.js','utf8')
 const {calendarText}=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'))
 const order={ddid:42,orderNo:'演示\nBEGIN:VEVENT;注入,测试',status:1,ordertime:'2026-11-20T12:00:00+08:00',flight:{start:'北京'.repeat(50),end:'上海',flightNo:'演示73',starttime:'2026-11-22T12:00:00+08:00',arrivalTime:'2026-11-22T14:00:00+08:00',operationalStatus:'SCHEDULED'}}
 const result=calendarText(order),lines=result.split('\r\n');assert(result.includes('DTSTART:20261122T040000Z'));assert(result.includes('DTEND:20261122T060000Z'));assert(lines.every(line=>Buffer.byteLength(line,'utf8')<=75));assert.equal(lines.filter(line=>line==='BEGIN:VEVENT').length,1);assert(result.includes('\\nBEGIN:VEVENT\\;'));assert.throws(()=>calendarText({...order,status:3}));assert.throws(()=>calendarText({...order,flight:{...order.flight,arrivalTime:null}}));console.log('日历测试通过：UTC转换、UTF-8折行、文本转义、无效与退订行程校验')
})().catch(e=>{console.error(e);process.exitCode=1})
