package com.airticket.service.impl;

import com.airticket.entity.Hbxx;
import com.airticket.entity.Jpdd;
import com.airticket.entity.Tdjp;
import com.airticket.mapper.HbxxCabinMapper;
import com.airticket.mapper.HbxxMapper;
import com.airticket.mapper.JpddMapper;
import com.airticket.mapper.TdjpMapper;
import com.airticket.service.TdjpService;
import com.airticket.vo.PageResult;
import com.airticket.vo.TdjpVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class TdjpServiceImpl implements TdjpService {

    @Autowired
    private TdjpMapper tdjpMapper;

    @Autowired
    private JpddMapper jpddMapper;

    @Autowired
    private HbxxMapper hbxxMapper;

    @Autowired
    private HbxxCabinMapper hbxxCabinMapper;

    @Override
    public PageResult<TdjpVO> list(Integer page, Integer pageSize, Integer process) {
        Map<String, Object> params = new HashMap<>();
        params.put("process", process);
        params.put("offset", (page - 1) * pageSize);
        params.put("pageSize", pageSize);
        Long total = tdjpMapper.countAll(params);
        return new PageResult<>(total, tdjpMapper.findAll(params), page, pageSize);
    }

    @Override
    public Tdjp getById(Integer id) {
        return tdjpMapper.findById(id);
    }

    @Autowired private com.airticket.service.BookingService booking;
    @Override public void approve(Integer id){booking.review(id,true,"审核通过",com.airticket.util.CurrentUser.id());}
}
