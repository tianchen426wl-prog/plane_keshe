import request from './request'

export const getRefundList = params => request.get('/admin/tdjp/list', { params })
export const approveRefund = id => request.put(`/admin/tdjp/approve/${id}`)
export const rejectRefund = (id, reason) => request.put(`/admin/tdjp/reject/${id}`, { reason })
