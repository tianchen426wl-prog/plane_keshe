package com.airticket.service;

import com.airticket.entity.Hbxx;
import com.airticket.vo.PageResult;

public interface HbxxService {
    PageResult<Hbxx> search(String start, String end, String starttime, Integer page, Integer pageSize);
    Hbxx getById(Integer hbid);
    void add(Hbxx hbxx);
    void update(Hbxx hbxx);
    void delete(Integer hbid);
}