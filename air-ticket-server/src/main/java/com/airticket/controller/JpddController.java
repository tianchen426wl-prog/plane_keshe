package com.airticket.controller;

import com.airticket.dto.OrderDTO;
import com.airticket.service.JpddService;
import com.airticket.vo.JpddVO;
import com.airticket.vo.PageResult;
import com.airticket.vo.Result;
import com.airticket.vo.SeatHoldVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jpdd")
public class JpddController {

    @Autowired
    private JpddService jpddService;

    @PostMapping("/hold")
    public Result<java.util.Map<String,Object>> hold(@RequestBody OrderDTO dto) {
        Long holdId = booking.hold(dto, com.airticket.util.CurrentUser.id());
        return Result.success(java.util.Map.of("holdId",holdId,"expireTime",db.queryForObject("SELECT expire_time FROM seat_hold WHERE hold_id=?",java.sql.Timestamp.class,holdId)));
    }

    @DeleteMapping("/hold/{holdId}")
    public Result<Void> cancelHold(@PathVariable Long holdId, @RequestParam(required=false) Integer userid) {
        jpddService.cancelHold(holdId, com.airticket.util.CurrentUser.id());
        return Result.success();
    }

    @PostMapping("/create")
    public Result<Void> create(@RequestBody OrderDTO dto) {
        if (dto.getHoldId() == null) {
            throw new IllegalArgumentException("缺少占位ID，请先抢票占位");
        }
        if (!jpddService.createOrder(dto.getHoldId(), com.airticket.util.CurrentUser.id())) throw new com.airticket.config.ApiException(409,"占位已过期，库存已释放");
        return Result.success();
    }

    @GetMapping("/myOrders")
    public Result<PageResult<JpddVO>> myOrders(@RequestParam(required=false) Integer userId,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<JpddVO> result = jpddService.getMyOrders(com.airticket.util.CurrentUser.id(), status, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/myHolds")
    public Result<List<SeatHoldVO>> myHolds(@RequestParam(required=false) Integer userId) {
        List<SeatHoldVO> holds = jpddService.getMyHolds(com.airticket.util.CurrentUser.id());
        return Result.success(holds);
    }

    @PutMapping("/cancel/{ddid}")
    public Result<Void> cancel(@PathVariable Integer ddid, @RequestParam(required=false) Integer userid) {
        jpddService.cancelOrder(ddid, com.airticket.util.CurrentUser.id());
        return Result.success();
    }
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    @Autowired private com.airticket.service.BookingService booking;
}
