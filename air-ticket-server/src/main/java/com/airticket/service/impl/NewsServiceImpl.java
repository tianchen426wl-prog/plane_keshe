package com.airticket.service.impl;

import com.airticket.entity.News;
import com.airticket.mapper.NewsMapper;
import com.airticket.service.NewsService;
import com.airticket.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NewsServiceImpl implements NewsService {
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;


    @Autowired
    private NewsMapper newsMapper;

    @Override
    public PageResult<News> list(Integer page, Integer pageSize) {
        String where=""; Object[] args=new Object[]{};
        Long total=db.queryForObject("SELECT COUNT(*) FROM `system`"+where,Long.class,args);
        java.util.List<Object> values=new java.util.ArrayList<>(java.util.Arrays.asList(args));values.add(pageSize);values.add((page-1)*pageSize);
        List<News> records=db.query("SELECT * FROM `system`"+where+" ORDER BY id DESC LIMIT ? OFFSET ?",new org.springframework.jdbc.core.BeanPropertyRowMapper<>(News.class),values.toArray());
        return new PageResult<>(total,records,page,pageSize);
    }

    @Override
    public News getById(Integer id) {
        return newsMapper.findById(id);
    }

    @Override
    public void add(News news) {
        news.setTime(new Date());
        newsMapper.insert(news);
    }

    @Override
    public void update(News news) {
        newsMapper.update(news);
    }

    @Override
    public void delete(Integer id) {
        newsMapper.deleteById(id);
    }
}