package com.airticket.mapper;

import com.airticket.entity.Jpdd;
import com.airticket.vo.JpddVO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface JpddMapper {

    List<JpddVO> findByUserId(Map<String, Object> params);

    Long countByUserId(Map<String, Object> params);

    @Select("SELECT * FROM jpdd WHERE ddid = #{ddid}")
    Jpdd findById(Integer ddid);

    @Insert("INSERT INTO jpdd (hbid, userid, cabin_id, quantity, cost, unit_price, ordertime, status) " +
            "VALUES (#{hbid}, #{userid}, #{cabinId}, #{quantity}, #{cost}, #{unitPrice}, #{ordertime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "ddid")
    int insert(Jpdd jpdd);

    @Update("UPDATE jpdd SET status = #{status} WHERE ddid = #{ddid}")
    int updateStatus(@Param("ddid") Integer ddid, @Param("status") Integer status);

    List<JpddVO> findAllWithDetails(Map<String, Object> params);

    Long countAllWithDetails(Map<String, Object> params);
}
