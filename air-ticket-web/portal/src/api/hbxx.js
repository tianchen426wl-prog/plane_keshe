import request from './request'

export const searchFlights = params => request.get('/flights', { params: { ...params, date: params.date || params.starttime } })

export const getFlightDetail = id => request.get(`/flights/${id}`)

export const getCabins = hbid => request.get(`/hbxx/${hbid}/cabins`)
export const getCalendar = params => request.get('/flights/calendar', { params })
export const getAirports = () => request.get('/airports')
export const getSeats = (id, cabinId) => request.get(`/flights/${id}/seats`, { params: { cabinId } })
export const getQuote = body => request.post('/quotes', body)
export const getHoldDetail = id => request.get(`/jpdd/holds/${id}`)
export const getOrderDetails = id => request.get(`/jpdd/orders/${id}/details`)
export const getRefundQuote = id => request.get(`/jpdd/orders/${id}/refund-quote`)
