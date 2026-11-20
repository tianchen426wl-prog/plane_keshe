import request from './request'

export const holdSeat = data => request.post('/jpdd/hold', data)

export const cancelHold = (holdId, userid) => request.delete(`/jpdd/hold/${holdId}`, {})

export const createOrder = data => request.post('/jpdd/create', data)

export const getMyOrders = ({ status, page, pageSize }) => request.get('/jpdd/myOrders', { params: { status, page, pageSize } })

export const getMyHolds = userId => request.get('/jpdd/myHolds', {})

export const cancelOrder = (ddid, userid) => request.put(`/jpdd/cancel/${ddid}`, null, {})
