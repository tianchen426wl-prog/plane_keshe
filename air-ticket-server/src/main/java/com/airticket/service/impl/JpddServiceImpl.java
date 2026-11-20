package com.airticket.service.impl;

import com.airticket.entity.Hbxx;
import com.airticket.entity.HbxxCabin;
import com.airticket.entity.Jpdd;
import com.airticket.entity.SeatHold;
import com.airticket.entity.Tdjp;
import com.airticket.entity.User;
import com.airticket.mapper.HbxxCabinMapper;
import com.airticket.mapper.HbxxMapper;
import com.airticket.mapper.JpddMapper;
import com.airticket.mapper.SeatHoldMapper;
import com.airticket.mapper.TdjpMapper;
import com.airticket.mapper.UserMapper;
import com.airticket.service.JpddService;
import com.airticket.vo.JpddVO;
import com.airticket.vo.PageResult;
import com.airticket.vo.SeatHoldVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JpddServiceImpl implements JpddService {

    private static final int HOLD_TTL_MINUTES = 15;

    @Autowired
    private JpddMapper jpddMapper;

    @Autowired
    private HbxxMapper hbxxMapper;

    @Autowired
    private HbxxCabinMapper hbxxCabinMapper;

    @Autowired
    private SeatHoldMapper seatHoldMapper;

    @Autowired
    private TdjpMapper tdjpMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired private com.airticket.service.BookingService booking;
    @Override public Long holdSeat(Integer flight,Integer cabin,Integer user,Integer quantity,String requestId){return booking.hold(flight,cabin,user,quantity,requestId);}
    @Override public boolean createOrder(Long id,Integer user){return booking.confirm(id,user);}
    @Override public void cancelHold(Long id,Integer user){booking.cancel(id,user);}
    @Override public void cancelOrder(Integer id,Integer user){booking.requestRefund(id,user);}

    @Override
    public PageResult<JpddVO> getMyOrders(Integer userid, Integer status, Integer page, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("userid", userid);
        params.put("status", status);
        params.put("offset", (page - 1) * pageSize);
        params.put("pageSize", pageSize);
        Long total = jpddMapper.countByUserId(params);
        return new PageResult<>(total, jpddMapper.findByUserId(params), page, pageSize);
    }

    @Override
    public PageResult<JpddVO> adminQuery(String flightName, String userName, Integer page, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("flightName", flightName);
        params.put("userName", userName);
        params.put("offset", (page - 1) * pageSize);
        params.put("pageSize", pageSize);
        Long total = jpddMapper.countAllWithDetails(params);
        return new PageResult<>(total, jpddMapper.findAllWithDetails(params), page, pageSize);
    }

    @Override
    public List<SeatHoldVO> getMyHolds(Integer userid) {
        if (userid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return seatHoldMapper.findActiveHoldsByUser(userid);
    }
}
