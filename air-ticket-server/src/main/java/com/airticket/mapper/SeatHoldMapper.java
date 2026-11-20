package com.airticket.mapper;

import com.airticket.entity.SeatHold;
import com.airticket.vo.SeatHoldVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SeatHoldMapper {

    @Insert("INSERT INTO seat_hold (hbid, cabin_id, userid, quantity, create_time, expire_time, status) " +
            "VALUES (#{hbid}, #{cabinId}, #{userid}, #{quantity}, #{createTime}, #{expireTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "holdId")
    int insert(SeatHold seatHold);

    @Select("SELECT * FROM seat_hold WHERE hold_id = #{holdId}")
    SeatHold findById(Long holdId);

    @Select("SELECT * FROM seat_hold WHERE hold_id = #{holdId} AND userid = #{userid} AND status = 0 FOR UPDATE")
    SeatHold findByIdAndUserForUpdate(@Param("holdId") Long holdId, @Param("userid") Integer userid);

    @Update("UPDATE seat_hold SET status = #{status} WHERE hold_id = #{holdId}")
    int updateStatus(@Param("holdId") Long holdId, @Param("status") Integer status);

    @Select("SELECT * FROM seat_hold WHERE status = 0 AND expire_time < NOW()")
    List<SeatHold> findExpired();

    List<SeatHoldVO> findActiveHoldsByUser(@Param("userid") Integer userid);
}
