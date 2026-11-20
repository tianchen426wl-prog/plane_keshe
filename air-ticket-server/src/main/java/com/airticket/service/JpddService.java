package com.airticket.service;

import com.airticket.vo.JpddVO;
import com.airticket.vo.PageResult;
import com.airticket.vo.SeatHoldVO;

import java.util.List;

public interface JpddService {
    Long holdSeat(Integer hbid, Integer cabinId, Integer userid, Integer quantity, String requestId);
    boolean createOrder(Long holdId, Integer userid);
    void cancelHold(Long holdId, Integer userid);
    void cancelOrder(Integer ddid, Integer userid);
    PageResult<JpddVO> getMyOrders(Integer userid, Integer status, Integer page, Integer pageSize);
    PageResult<JpddVO> adminQuery(String flightName, String userName, Integer page, Integer pageSize);
    List<SeatHoldVO> getMyHolds(Integer userid);
}
