package com.airticket.mapper;

import com.airticket.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM `user` WHERE id = #{id}")
    User findById(Integer id);

    @Select("SELECT * FROM `user`")
    List<User> findAll();

    @Insert("INSERT INTO `user` (username, password, name, gender, phone, email, identity) " +
            "VALUES (#{username}, #{password}, #{name}, #{gender}, #{phone}, #{email}, #{identity})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE `user` SET name = #{name}, gender = #{gender}, phone = #{phone}, " +
            "email = #{email}, identity = #{identity} WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE `user` SET password = #{password}, auth_version=auth_version+1 WHERE id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);
}