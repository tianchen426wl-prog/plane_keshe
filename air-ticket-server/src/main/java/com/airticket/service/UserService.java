package com.airticket.service;

import com.airticket.dto.RegisterDTO;
import com.airticket.entity.User;
import com.airticket.vo.UserVO;

import java.util.List;

public interface UserService {
    UserVO register(RegisterDTO dto);
    UserVO login(String username, String password);
    UserVO getUserInfo(Integer userId);
    void update(User user);
    void updatePassword(Integer userId, String oldPassword, String newPassword);
    List<User> findAll();
}