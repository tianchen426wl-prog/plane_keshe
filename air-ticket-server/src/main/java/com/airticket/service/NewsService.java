package com.airticket.service;

import com.airticket.entity.News;
import com.airticket.vo.PageResult;

public interface NewsService {
    PageResult<News> list(Integer page, Integer pageSize);
    News getById(Integer id);
    void add(News news);
    void update(News news);
    void delete(Integer id);
}