package com.airticket.service;

import com.airticket.entity.Menu;

import java.util.List;

public interface MenuService {
    List<Menu> findAll();
    Menu getById(Integer id);
    void add(Menu menu);
    void update(Menu menu);
    void delete(Integer id);
}