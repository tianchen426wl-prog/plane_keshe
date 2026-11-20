package com.airticket.service;

import com.airticket.entity.Company;
import com.airticket.vo.PageResult;

public interface CompanyService {
    PageResult<Company> list(Integer page, Integer pageSize, String keyword);
    Company getById(Integer companyId);
    void add(Company company);
    void update(Company company);
    void delete(Integer companyId);
}