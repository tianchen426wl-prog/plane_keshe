package com.airticket.service.impl;

import com.airticket.entity.Company;
import com.airticket.mapper.CompanyMapper;
import com.airticket.service.CompanyService;
import com.airticket.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyServiceImpl implements CompanyService {
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;


    @Autowired
    private CompanyMapper companyMapper;

    @Override
    public PageResult<Company> list(Integer page, Integer pageSize, String keyword) {
        String filter=keyword==null?"":"%"+keyword+"%"; String where=filter.isEmpty()?"":" WHERE company_name LIKE ?"; Object[] args=filter.isEmpty()?new Object[]{}:new Object[]{filter};
        Long total=db.queryForObject("SELECT COUNT(*) FROM company"+where,Long.class,args);
        java.util.List<Object> values=new java.util.ArrayList<>(java.util.Arrays.asList(args));values.add(pageSize);values.add((page-1)*pageSize);
        List<Company> records=db.query("SELECT * FROM company"+where+" ORDER BY company_id DESC LIMIT ? OFFSET ?",new org.springframework.jdbc.core.BeanPropertyRowMapper<>(Company.class),values.toArray());
        return new PageResult<>(total,records,page,pageSize);
    }

    @Override
    public Company getById(Integer companyId) {
        return companyMapper.findById(companyId);
    }

    @Override
    public void add(Company company) {
        companyMapper.insert(company);
    }

    @Override
    public void update(Company company) {
        companyMapper.update(company);
    }

    @Override
    public void delete(Integer companyId) {
        if(db.queryForObject("SELECT COUNT(*) FROM hbxx WHERE company_id=?",Integer.class,companyId)>0)throw new IllegalArgumentException("公司有关联航班，不能删除");
        companyMapper.deleteById(companyId);
    }
}