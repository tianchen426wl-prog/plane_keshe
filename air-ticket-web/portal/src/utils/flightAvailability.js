export function bookingStatus(flight, quantity = 1, now = Date.now()) {
 if (flight.operationalStatus === 'CANCELLED') return 'CANCELLED'
 if (flight.starttime && new Date(flight.starttime).getTime() <= now) return 'DEPARTED'
 if (flight.saleStatus !== 1) return 'NOT_ON_SALE'
 if (flight.operationalStatus && flight.operationalStatus !== 'SCHEDULED') return 'FLIGHT_CHANGED'
 if (flight.saleOpenAt && new Date(flight.saleOpenAt).getTime() > now) return 'NOT_OPEN'
 if (flight.saleCloseAt && new Date(flight.saleCloseAt).getTime() <= now) return 'SALES_CLOSED'
 if (flight.bookingStatus) return flight.bookingStatus
 if (flight.number == null) return 'INVENTORY_UNAVAILABLE'
 if (Number(flight.number) === 0) return 'SOLD_OUT'
 if (Number(flight.number) < quantity || flight.minTotal == null) return 'INSUFFICIENT_SEATS'
 return 'AVAILABLE'
}
export const bookingLabels = {
 AVAILABLE: '可预订', SOLD_OUT: '已售罄', INSUFFICIENT_SEATS: '同行余票不足',
 INVENTORY_UNAVAILABLE: '暂不可预订', NOT_ON_SALE: '已停售', NOT_OPEN: '未开售',
 DEPARTED: '已起飞', SALES_CLOSED: '预订已截止', CANCELLED: '已取消', FLIGHT_CHANGED: '航班调整中'
}
