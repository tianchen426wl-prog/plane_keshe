package com.airticket.mapper;

import com.airticket.entity.HbxxCabin;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface HbxxCabinMapper {

    @Select("SELECT * FROM hbxx_cabin WHERE hbid = #{hbid} ORDER BY cabin_class")
    List<HbxxCabin> findByHbid(Integer hbid);

    @Select("SELECT * FROM hbxx_cabin WHERE cabin_id = #{cabinId}")
    HbxxCabin findById(Integer cabinId);

    @Insert("INSERT INTO hbxx_cabin (hbid, cabin_class, cabin_name, price, total_number, available_number) " +
            "VALUES (#{hbid}, #{cabinClass}, #{cabinName}, #{price}, #{totalNumber}, #{availableNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "cabinId")
    int insert(HbxxCabin cabin);

    @Update("UPDATE hbxx_cabin SET cabin_class = #{cabinClass}, cabin_name = #{cabinName}, " +
            "price = #{price}, total_number = #{totalNumber}, available_number = #{availableNumber} " +
            "WHERE cabin_id = #{cabinId}")
    int update(HbxxCabin cabin);

    @Delete("DELETE FROM hbxx_cabin WHERE hbid = #{hbid}")
    int deleteByHbid(Integer hbid);

    @Delete("DELETE FROM hbxx_cabin WHERE cabin_id = #{cabinId}")
    int deleteById(Integer cabinId);

    @Update("UPDATE hbxx_cabin SET available_number = available_number - #{quantity} " +
            "WHERE cabin_id = #{cabinId} AND available_number >= #{quantity}")
    int decreaseAvailable(@Param("cabinId") Integer cabinId, @Param("quantity") Integer quantity);

    @Update("UPDATE hbxx_cabin SET available_number = available_number + #{quantity} " +
            "WHERE cabin_id = #{cabinId}")
    int increaseAvailable(@Param("cabinId") Integer cabinId, @Param("quantity") Integer quantity);
}
