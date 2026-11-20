package com.airticket.controller;

import com.airticket.dto.LoginDTO;
import com.airticket.dto.RegisterDTO;
import com.airticket.entity.User;
import com.airticket.service.UserService;
import com.airticket.vo.Result;
import com.airticket.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        UserVO userVO = userService.register(dto);
        return Result.success(userVO);
    }

    @PostMapping("/login")
    public Result<UserVO> login(@Valid @RequestBody LoginDTO dto) {
        UserVO userVO = userService.login(dto.getUsername(), dto.getPassword());
        return Result.success(userVO);
    }

    @GetMapping("/info")
    public Result<UserVO> info() {
        UserVO userVO = userService.getUserInfo(com.airticket.util.CurrentUser.id());
        return Result.success(userVO);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody User user) {
        user.setId(com.airticket.util.CurrentUser.id());
        userService.update(user);
        return Result.success();
    }

    @PutMapping("/updatePassword")
    public Result<Void> updatePassword(@RequestBody Map<String, Object> params) {
        Integer userId = com.airticket.util.CurrentUser.id();
        String oldPassword = (String) params.get("oldPassword");
        String newPassword = (String) params.get("newPassword");
        userService.updatePassword(userId, oldPassword, newPassword);
        return Result.success();
    }
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    @PostMapping("/logout")
    public Result<Void> logout(){db.update("UPDATE `user` SET auth_version=auth_version+1 WHERE id=?",com.airticket.util.CurrentUser.id());return Result.success();}
}