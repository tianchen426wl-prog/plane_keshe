package com.airticket.service.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service public class SeatHoldServiceImpl implements com.airticket.service.SeatHoldService {
 @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
 @Autowired private com.airticket.service.BookingService booking;
 public void releaseExpiredHolds(){for(Long id:db.queryForList("SELECT hold_id FROM seat_hold WHERE status=0 AND expire_time<NOW() ORDER BY hold_id LIMIT 100",Long.class)){
  try{booking.expire(id);}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Failed to expire hold {}",id,e);}
 }}
}