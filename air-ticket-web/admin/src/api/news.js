import request from './request'

export const getNewsList = params => request.get('/admin/news/list', { params })
export const addNews = data => request.post('/admin/news/add', data)
export const updateNews = data => request.put('/admin/news/update', data)
export const deleteNews = id => request.delete(`/admin/news/delete/${id}`)