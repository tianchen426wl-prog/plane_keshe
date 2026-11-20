package com.airticket.service;

import com.airticket.entity.Tdjp;
import com.airticket.vo.PageResult;
import com.airticket.vo.TdjpVO;

public interface TdjpService {
    PageResult<TdjpVO> list(Integer page, Integer pageSize, Integer process);
    Tdjp getById(Integer id);
    void approve(Integer id);
}
