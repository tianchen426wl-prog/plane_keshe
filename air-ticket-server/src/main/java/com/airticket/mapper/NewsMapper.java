package com.airticket.mapper;

import com.airticket.entity.News;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NewsMapper {

    @Select("SELECT * FROM `system`")
    List<News> findAll();

    @Select("SELECT * FROM `system` WHERE id = #{id}")
    News findById(Integer id);

    @Insert("INSERT INTO `system` (title, profile, picture, time) VALUES (#{title}, #{profile}, #{picture}, #{time})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(News news);

    @Update("UPDATE `system` SET title = #{title}, profile = #{profile}, picture = #{picture} WHERE id = #{id}")
    int update(News news);

    @Delete("DELETE FROM `system` WHERE id = #{id}")
    int deleteById(Integer id);
}