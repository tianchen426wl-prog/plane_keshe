package com.airticket.service.impl;
import com.airticket.entity.HbxxCabin;
import com.airticket.service.HbxxCabinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service public class HbxxCabinServiceImpl implements HbxxCabinService {
 @Autowired private JdbcTemplate db;
 public List<HbxxCabin> getCabinsByHbid(Integer id){return db.query("SELECT * FROM hbxx_cabin WHERE hbid=? ORDER BY cabin_class",new BeanPropertyRowMapper<>(HbxxCabin.class),id);}
 public HbxxCabin getById(Integer id){List<HbxxCabin> rows=db.query("SELECT * FROM hbxx_cabin WHERE cabin_id=?",new BeanPropertyRowMapper<>(HbxxCabin.class),id);if(rows.isEmpty())throw new IllegalArgumentException("舱位不存在");return rows.get(0);}
 private void validate(HbxxCabin c){if(c.getPrice()==null||c.getPrice().signum()<=0||c.getTotalNumber()==null||c.getTotalNumber()<0||c.getCabinClass()==null||c.getCabinClass()<1||c.getCabinClass()>3||c.getCabinName()==null||c.getCabinName().isBlank())throw new IllegalArgumentException("舱位价格、容量、类型或名称不合法");}
 @Transactional public void add(HbxxCabin c){validate(c);db.queryForList("SELECT hbid FROM hbxx WHERE hbid=? FOR UPDATE",c.getHbid());db.update("INSERT INTO hbxx_cabin(hbid,cabin_class,cabin_name,price,total_number,available_number)VALUES(?,?,?,?,?,?)",c.getHbid(),c.getCabinClass(),c.getCabinName(),c.getPrice(),c.getTotalNumber(),c.getTotalNumber());}
 @Transactional public void update(HbxxCabin c){
  validate(c);HbxxCabin before=getById(c.getCabinId());
  db.queryForList("SELECT hbid FROM hbxx WHERE hbid=? FOR UPDATE",before.getHbid());
  HbxxCabin current=db.queryForObject("SELECT * FROM hbxx_cabin WHERE cabin_id=? FOR UPDATE",new BeanPropertyRowMapper<>(HbxxCabin.class),c.getCabinId());
  if(db.queryForObject("SELECT COUNT(*) FROM flight_seat WHERE cabin_id=?",Integer.class,c.getCabinId())>0 && (!current.getTotalNumber().equals(c.getTotalNumber()) || !current.getCabinClass().equals(c.getCabinClass())))throw new IllegalArgumentException("已绑定客舱布局，不能直接修改容量或舱位类型");
  int available=current.getAvailableNumber()+c.getTotalNumber()-current.getTotalNumber();
  if(available<0)throw new IllegalArgumentException("新容量不能小于已售和已占位数量");
  db.update("UPDATE hbxx_cabin SET cabin_name=?,price=?,total_number=?,available_number=?,status=? WHERE cabin_id=?",c.getCabinName(),c.getPrice(),c.getTotalNumber(),available,c.getStatus()==null?1:c.getStatus(),c.getCabinId());
 }
 @Transactional public void deleteById(Integer id){HbxxCabin c=getById(id);db.queryForList("SELECT hbid FROM hbxx WHERE hbid=? FOR UPDATE",c.getHbid());
  if(db.queryForObject("SELECT (SELECT COUNT(*) FROM jpdd WHERE cabin_id=?)+(SELECT COUNT(*) FROM seat_hold WHERE cabin_id=?)",Integer.class,id,id)>0)throw new IllegalArgumentException("舱位有交易记录，请改为停用");
  if(db.queryForObject("SELECT COUNT(*) FROM flight_seat WHERE cabin_id=?",Integer.class,id)>0)throw new IllegalArgumentException("已绑定布局，请停用舱位");
  db.update("DELETE FROM hbxx_cabin WHERE cabin_id=?",id);
 }
 @Transactional public void deleteByHbid(Integer id){for(HbxxCabin c:getCabinsByHbid(id))deleteById(c.getCabinId());}
}
