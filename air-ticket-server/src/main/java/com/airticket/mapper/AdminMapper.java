package com.airticket.mapper;

import com.airticket.entity.Admin;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminMapper {

    @Select("SELECT * FROM admin WHERE username = #{username}")
    Admin findByUsername(String username);

    @Select("SELECT * FROM admin WHERE status=1")
    List<Admin> findAll();

    @Select("SELECT * FROM admin WHERE id = #{id}")
    Admin findById(Integer id);

    @Insert("INSERT INTO admin (username, password, role, menu_id) VALUES (#{username}, #{password}, #{role}, #{menuId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Admin admin);

    @Update("UPDATE admin SET username = #{username}, role = #{role}, menu_id = #{menuId}, auth_version=auth_version+1 WHERE id = #{id}")
    int update(Admin admin);

    @Update("UPDATE admin SET username = #{username}, password = #{password}, role = #{role}, menu_id = #{menuId}, auth_version=auth_version+1 WHERE id = #{id}")
    int updateWithPassword(Admin admin);

    @Delete("DELETE FROM admin WHERE id = #{id}")
    int deleteById(Integer id);
}
