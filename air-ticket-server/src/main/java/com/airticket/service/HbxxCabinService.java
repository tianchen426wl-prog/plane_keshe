package com.airticket.service;

import com.airticket.entity.HbxxCabin;

import java.util.List;

public interface HbxxCabinService {
    List<HbxxCabin> getCabinsByHbid(Integer hbid);
    HbxxCabin getById(Integer cabinId);
    void add(HbxxCabin cabin);
    void update(HbxxCabin cabin);
    void deleteByHbid(Integer hbid);
    void deleteById(Integer cabinId);
}
