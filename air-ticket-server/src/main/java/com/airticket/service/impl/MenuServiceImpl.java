package com.airticket.service.impl;

import com.airticket.entity.Menu;
import com.airticket.mapper.MenuMapper;
import com.airticket.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public List<Menu> findAll() {
        return menuMapper.findAll();
    }

    @Override
    public Menu getById(Integer id) {
        return menuMapper.findById(id);
    }

    @Override
    public void add(Menu menu) {
        menuMapper.insert(menu);
    }

    @Override
    public void update(Menu menu) {
        menuMapper.update(menu);
    }

    @Override
    public void delete(Integer id) {
        menuMapper.deleteById(id);
    }
}