import request from './request'

export const getOrderList = params => request.get('/admin/jpdd/list', { params })
export const getOrderLog = id => request.get(`/admin/jpdd/log/${id}`)
