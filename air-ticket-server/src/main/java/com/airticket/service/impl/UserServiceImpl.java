package com.airticket.service.impl;

import com.airticket.dto.RegisterDTO;
import com.airticket.entity.User;
import com.airticket.mapper.UserMapper;
import com.airticket.service.UserService;
import com.airticket.util.PasswordUtil;
import com.airticket.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordUtil passwordUtil;
    @Autowired private com.airticket.util.JwtUtil jwt;

    @Override
    public UserVO register(RegisterDTO dto) {
        User existUser = userMapper.findByUsername(dto.getUsername());
        if (existUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordUtil.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setGender(dto.getGender());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setIdentity(dto.getIdentity());
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public UserVO login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!passwordUtil.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) throw new IllegalArgumentException("账号已停用");
        UserVO vo=toVO(user);vo.setToken(jwt.generateToken(java.util.Map.of("id",user.getId(),"kind","USER","version",user.getAuthVersion())));return vo;
    }
    @Override
    public UserVO getUserInfo(Integer userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return toVO(user);
    }

    @Override
    public void update(User user) {
        if(user.getName()==null||user.getName().isBlank()||user.getName().length()>50)throw new IllegalArgumentException("姓名不能为空且不超过50字");
        if(user.getPhone()!=null&&!user.getPhone().isBlank()&&!user.getPhone().matches("1[0-9]{10}"))throw new IllegalArgumentException("手机号码不合法");
        if(user.getEmail()!=null&&!user.getEmail().isBlank()&&!user.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))throw new IllegalArgumentException("邮箱不合法");
        userMapper.update(user);
    }

    @Override
    public void updatePassword(Integer userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!passwordUtil.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        if(newPassword==null||newPassword.length()<6||newPassword.length()>64)throw new IllegalArgumentException("新密码长度应为6～64位");
        userMapper.updatePassword(userId, passwordUtil.encode(newPassword));
    }

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setIdentity(user.getIdentity());
        return vo;
    }
}