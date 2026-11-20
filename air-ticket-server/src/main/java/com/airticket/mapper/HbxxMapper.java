package com.airticket.mapper;

import com.airticket.entity.Hbxx;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface HbxxMapper {

    List<Hbxx> search(Map<String, Object> params);

    Long searchCount(Map<String, Object> params);

    @Select("SELECT h.hbid,h.company_id,h.xclb,h.start,h.`end`,h.starttime,h.price,h.duration,h.place,h.sale_status,h.flight_no, (SELECT COALESCE(SUM(available_number),0) FROM hbxx_cabin WHERE hbid=h.hbid) AS number FROM hbxx h WHERE hbid = #{hbid}")
    Hbxx findById(Integer hbid);

    @Insert("INSERT INTO hbxx (company_id, xclb, start, `end`, starttime, number, price, duration, place, sale_status, flight_no) " +
            "VALUES (#{companyId}, #{xclb}, #{start}, #{end}, #{starttime}, #{number}, #{price}, #{duration}, #{place}, COALESCE(#{saleStatus},1), #{flightNo})")
    @Options(useGeneratedKeys = true, keyProperty = "hbid")
    int insert(Hbxx hbxx);

    @Update("UPDATE hbxx SET company_id = #{companyId}, xclb = #{xclb}, start = #{start}, " +
            "`end` = #{end}, starttime = #{starttime}, price = #{price}, sale_status=COALESCE(#{saleStatus},1), flight_no=#{flightNo}, " +
            "duration = #{duration}, place = #{place} WHERE hbid = #{hbid}")
    int update(Hbxx hbxx);

    @Delete("DELETE FROM hbxx WHERE hbid = #{hbid}")
    int deleteById(Integer hbid);

    @Update("UPDATE hbxx SET number = number - #{quantity} WHERE hbid = #{hbid} AND number >= #{quantity}")
    int updateNumber(@Param("hbid") Integer hbid, @Param("quantity") Integer quantity);

    @Update("UPDATE hbxx SET number = number + #{quantity} WHERE hbid = #{hbid}")
    int increaseNumber(@Param("hbid") Integer hbid, @Param("quantity") Integer quantity);
}
