import request from './request'

export const getNewsList = params => request.get('/news/list', { params })

export const getNewsDetail = id => request.get(`/news/detail/${id}`)