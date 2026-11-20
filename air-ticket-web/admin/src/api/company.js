import request from './request'

export const getList = params => request.get('/company/list', { params })
export const addCompany = data => request.post('/admin/company/add', data)
export const updateCompany = data => request.put('/admin/company/update', data)
export const deleteCompany = id => request.delete(`/admin/company/delete/${id}`)
