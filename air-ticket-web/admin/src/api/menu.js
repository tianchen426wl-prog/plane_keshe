import request from './request'

export const getMenuList = () => request.get('/admin/menu/list')
export const addMenu = data => request.post('/admin/menu/add', data)
export const updateMenu = data => request.put('/admin/menu/update', data)
export const deleteMenu = id => request.delete(`/admin/menu/delete/${id}`)