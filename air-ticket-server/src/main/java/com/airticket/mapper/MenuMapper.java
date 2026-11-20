package com.airticket.mapper;

import com.airticket.entity.Menu;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MenuMapper {

    @Select("SELECT * FROM menu")
    List<Menu> findAll();

    @Select("SELECT * FROM menu WHERE id = #{id}")
    Menu findById(Integer id);

    @Insert("INSERT INTO menu (path, title, icon) VALUES (#{path}, #{title}, #{icon})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Menu menu);

    @Update("UPDATE menu SET path = #{path}, title = #{title}, icon = #{icon} WHERE id = #{id}")
    int update(Menu menu);

    @Delete("DELETE FROM menu WHERE id = #{id}")
    int deleteById(Integer id);
}