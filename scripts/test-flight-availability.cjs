const fs=require('node:fs'),assert=require('node:assert/strict')
;(async()=>{
 const source=fs.readFileSync('air-ticket-web/portal/src/utils/flightAvailability.js','utf8')
 const {bookingStatus,bookingLabels}=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'))
 const now=Date.parse('2026-11-20T12:00:00+08:00'),base={saleStatus:1,operationalStatus:'SCHEDULED',number:10,minTotal:500,starttime:'2026-11-22T12:00:00+08:00'}
 for(const [extra,quantity,status] of [[{},1,'AVAILABLE'],[{number:0,minTotal:null},1,'SOLD_OUT'],[{number:2,minTotal:null},3,'INSUFFICIENT_SEATS'],[{number:null,minTotal:null},1,'INVENTORY_UNAVAILABLE'],[{saleOpenAt:'2026-11-21T00:00:00+08:00'},1,'NOT_OPEN'],[{saleCloseAt:'2026-11-20T11:00:00+08:00'},1,'SALES_CLOSED'],[{operationalStatus:'CANCELLED'},1,'CANCELLED'],[{saleStatus:0},1,'NOT_ON_SALE'],[{bookingStatus:'INVENTORY_UNAVAILABLE'},1,'INVENTORY_UNAVAILABLE']]){assert.equal(bookingStatus({...base,...extra},quantity,now),status);assert(bookingLabels[status])}
 for(const file of ['FlightSearch.vue','OrderDetail.vue','Booking.vue','PassengerProfiles.vue']){
  const folder=file==='PassengerProfiles.vue'?'components':'views';assert(!/课程|演示|非实时库存/.test(fs.readFileSync(`air-ticket-web/portal/src/${folder}/${file}`,'utf8')))
 }
 console.log('Passed: availability states, group inventory and customer-facing copy')
})().catch(e=>{console.error(e);process.exitCode=1})
