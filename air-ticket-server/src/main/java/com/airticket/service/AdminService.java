package com.airticket.service;

import com.airticket.entity.Admin;

import java.util.List;
import java.util.Map;

public interface AdminService {
    Map<String, Object> login(String username, String password);
    Admin getInfo(Integer adminId);
    List<Admin> findAll();
    Admin findById(Integer id);
    void add(Admin admin);
    void update(Admin admin);
    void delete(Integer id);
}