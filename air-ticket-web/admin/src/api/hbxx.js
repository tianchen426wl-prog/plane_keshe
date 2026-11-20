import request from './request'

export const searchList = params => request.get('/hbxx/search', { params })
export const addFlight = data => request.post('/admin/hbxx/add', data)
export const updateFlight = data => request.put('/admin/hbxx/update', data)
export const deleteFlight = id => request.delete(`/admin/hbxx/delete/${id}`)
export const getCompanyAll = () => request.get('/company/list', { params: { page: 1, pageSize: 100 } })

export const getCabins = hbid => request.get(`/hbxx/${hbid}/cabins`)
export const addCabin = data => request.post('/admin/hbxx/cabin/add', data)
export const updateCabin = data => request.put('/admin/hbxx/cabin/update', data)
export const deleteCabin = cabinId => request.delete(`/admin/hbxx/cabin/delete/${cabinId}`)
