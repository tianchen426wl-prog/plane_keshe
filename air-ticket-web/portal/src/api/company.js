import request from './request'

export const getCompanyList = params => request.get('/company/list', { params })

export const getCompanyDetail = id => request.get(`/company/detail/${id}`)