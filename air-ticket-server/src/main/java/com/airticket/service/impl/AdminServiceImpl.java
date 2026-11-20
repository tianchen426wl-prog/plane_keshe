package com.airticket.service.impl;

import com.airticket.entity.Admin;
import com.airticket.entity.Menu;
import com.airticket.mapper.AdminMapper;
import com.airticket.mapper.MenuMapper;
import com.airticket.service.AdminService;
import com.airticket.util.JwtUtil;
import com.airticket.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;

    @Override
    public Map<String, Object> login(String username, String password) {
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!passwordUtil.matches(password, admin.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if(!Integer.valueOf(1).equals(admin.getStatus()))throw new IllegalArgumentException("账号已停用");
        Map<String,Object> claims=new HashMap<>();
        claims.put("id",admin.getId());claims.put("kind","ADMIN");claims.put("version",admin.getAuthVersion());
        claims.put("adminId", admin.getId());
        claims.put("username", admin.getUsername());
        claims.put("role", admin.getRole());
        claims.put("menuId", admin.getMenuId() != null ? admin.getMenuId() : "");
        String token = jwtUtil.generateToken(claims);

        List<Menu> menuList;
        if (admin.getRole() == 0) {
            menuList = menuMapper.findAll();
        } else {
            menuList=db.query("SELECT m.* FROM menu m JOIN admin_menu am ON am.menu_id=m.id WHERE am.admin_id=? ORDER BY m.id",new org.springframework.jdbc.core.BeanPropertyRowMapper<>(Menu.class),admin.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);

        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("id", admin.getId());
        adminInfo.put("username", admin.getUsername());
        adminInfo.put("role", admin.getRole());
        adminInfo.put("menuId", admin.getMenuId());
        result.put("admin", adminInfo);

        result.put("menus", menuList);
        return result;
    }

    @Override
    public Admin getInfo(Integer adminId) {
        return adminMapper.findById(adminId);
    }

    @Override
    public List<Admin> findAll() {
        return adminMapper.findAll();
    }

    @Override
    public Admin findById(Integer id) {
        return adminMapper.findById(id);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void add(Admin admin) {
        Admin existAdmin = adminMapper.findByUsername(admin.getUsername());
        if (existAdmin != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if(admin.getPassword()==null||admin.getPassword().length()<6||admin.getPassword().length()>64)throw new IllegalArgumentException("密码长度应为6～64位");
        admin.setPassword(passwordUtil.encode(admin.getPassword()));
        validate(admin);
        adminMapper.insert(admin);saveMenus(admin);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void update(Admin admin) {
        validate(admin);if(admin.getId()==null||adminMapper.findById(admin.getId())==null)throw new IllegalArgumentException("管理员不存在");protectLastSuper(admin.getId(),admin.getRole());
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            if (admin.getPassword().length()<6 || admin.getPassword().length()>64) throw new IllegalArgumentException("密码长度应为6～64位");
            admin.setPassword(passwordUtil.encode(admin.getPassword()));
            adminMapper.updateWithPassword(admin);
        } else {
            adminMapper.update(admin);
        }
        saveMenus(admin);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void delete(Integer id) {
        protectLastSuper(id,1);
        db.update("UPDATE admin SET status=0,auth_version=auth_version+1 WHERE id=?",id);
    }
    private void validate(Admin a){
        if(a.getUsername()==null||!a.getUsername().matches("[A-Za-z0-9_]{3,50}"))throw new IllegalArgumentException("用户名应为3～50位字母数字下划线");
        if(a.getRole()==null||(a.getRole()!=0&&a.getRole()!=1))throw new IllegalArgumentException("角色不合法");
    }
    private void protectLastSuper(Integer id,Integer role){
        List<Map<String,Object>> all=db.queryForList("SELECT id FROM admin WHERE role=0 AND status=1 ORDER BY id FOR UPDATE");
        if(role!=0&&all.size()==1&&((Number)all.get(0).get("id")).intValue()==id)throw new IllegalArgumentException("必须保留至少一个有效超级管理员");
    }
    private void saveMenus(Admin a){
        db.update("DELETE FROM admin_menu WHERE admin_id=?",a.getId());
        if(a.getRole()==1&&a.getMenuId()!=null&&!a.getMenuId().isBlank())for(String raw:a.getMenuId().split(",")){
            int id=Integer.parseInt(raw.trim());
            if(db.queryForObject("SELECT COUNT(*) FROM menu WHERE id=? AND path NOT IN ('/admin/menu','/admin/role')",Integer.class,id)!=1)throw new IllegalArgumentException("菜单不存在或不可分配");
            db.update("INSERT IGNORE INTO admin_menu(admin_id,menu_id)VALUES(?,?)",a.getId(),id);
        }
    }
}
