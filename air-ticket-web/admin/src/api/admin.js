import request from './request'

export const login = data => request.post('/admin/login', data)
export const getInfo = () => request.get('/admin/info')
export const getUserList = params => request.get('/admin/user/list', { params })
export const getAdminList = params => request.get('/admin/role/list', { params })
export const addAdmin = data => request.post('/admin/role/add', data)
export const updateAdmin = data => request.put('/admin/role/update', data)
export const deleteAdmin = id => request.delete(`/admin/role/delete/${id}`)