package com.airticket.service.impl;

import com.airticket.entity.Hbxx;
import com.airticket.entity.HbxxCabin;
import com.airticket.mapper.HbxxCabinMapper;
import com.airticket.mapper.HbxxMapper;
import com.airticket.service.HbxxService;
import com.airticket.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class HbxxServiceImpl implements HbxxService {

    @Autowired
    private HbxxMapper hbxxMapper;

    @Autowired
    private HbxxCabinMapper hbxxCabinMapper;

    @Override
    public PageResult<Hbxx> search(String start, String end, String starttime, Integer page, Integer pageSize) {
        if(starttime!=null&&!starttime.isBlank()){try{java.time.LocalDate.parse(starttime);}catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("日期格式应为yyyy-MM-dd");}}
        Map<String, Object> params = new HashMap<>();
        params.put("publicOnly", org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()!=null && !Boolean.TRUE.equals(org.springframework.web.context.request.RequestContextHolder.getRequestAttributes().getAttribute("isAdminQuery",0)));
        params.put("start", start);
        params.put("end", end);
        params.put("starttime", starttime);
        params.put("offset", (page - 1) * pageSize);
        params.put("pageSize", pageSize);

        Long total = hbxxMapper.searchCount(params);
        return new PageResult<>(total, hbxxMapper.search(params), page, pageSize);
    }

    @Override
    public Hbxx getById(Integer hbid) {
        return hbxxMapper.findById(hbid);
    }

    @Override
    @Transactional
    public void add(Hbxx hbxx) {
        validate(hbxx);
        if (hbxx.getFlightNo()==null || hbxx.getFlightNo().isBlank()) hbxx.setFlightNo("AT"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase());
        hbxxMapper.insert(hbxx);
        // 自动初始化3个舱位：经济舱/商务舱/头等舱
        Integer hbid = hbxx.getHbid();
        BigDecimal basePrice = hbxx.getPrice();
        int totalSeats = hbxx.getNumber() == null ? 0 : hbxx.getNumber();

        // 经济舱：原价，60%座位数
        HbxxCabin economy = new HbxxCabin();
        economy.setHbid(hbid);
        economy.setCabinClass(1);
        economy.setCabinName("经济舱");
        economy.setPrice(basePrice);
        economy.setTotalNumber((int) Math.floor(totalSeats * 0.6));
        economy.setAvailableNumber((int) Math.floor(totalSeats * 0.6));
        hbxxCabinMapper.insert(economy);

        // 商务舱：1.8倍价格，30%座位数
        HbxxCabin business = new HbxxCabin();
        business.setHbid(hbid);
        business.setCabinClass(2);
        business.setCabinName("商务舱");
        business.setPrice(basePrice.multiply(new BigDecimal("1.8")).setScale(2, RoundingMode.HALF_UP));
        business.setTotalNumber((int) Math.floor(totalSeats * 0.3));
        business.setAvailableNumber((int) Math.floor(totalSeats * 0.3));
        hbxxCabinMapper.insert(business);

        // 头等舱：3倍价格，10%座位数
        HbxxCabin first = new HbxxCabin();
        first.setHbid(hbid);
        first.setCabinClass(3);
        first.setCabinName("头等舱");
        first.setPrice(basePrice.multiply(new BigDecimal("3")).setScale(2, RoundingMode.HALF_UP));
        first.setTotalNumber(totalSeats - (int)Math.floor(totalSeats*0.6) - (int)Math.floor(totalSeats*0.3));
        first.setAvailableNumber(totalSeats - (int)Math.floor(totalSeats*0.6) - (int)Math.floor(totalSeats*0.3));
        hbxxCabinMapper.insert(first);
    }

    @Override
    @Transactional
    public void update(Hbxx hbxx) {
        validate(hbxx);
        db.queryForList("SELECT hbid FROM hbxx WHERE hbid=? FOR UPDATE",hbxx.getHbid());
        if(db.queryForObject("SELECT COUNT(*) FROM hbxx WHERE hbid=? AND schedule_id IS NOT NULL",Integer.class,hbxx.getHbid())>0){
            Hbxx before=hbxxMapper.findById(hbxx.getHbid());
            if(!java.util.Objects.equals(before.getCompanyId(),hbxx.getCompanyId())||!java.util.Objects.equals(before.getStart(),hbxx.getStart())||!java.util.Objects.equals(before.getEnd(),hbxx.getEnd())||before.getPrice().compareTo(hbxx.getPrice())!=0||!before.getStarttime().equals(hbxx.getStarttime())||!java.util.Objects.equals(before.getFlightNo(),hbxx.getFlightNo())||!java.util.Objects.equals(before.getPlace(),hbxx.getPlace())||!java.util.Objects.equals(before.getDuration(),hbxx.getDuration()))throw new IllegalArgumentException("班期实例仅允许修改销售状态；价格请调整舱位报价，时刻和布局需通过版本化班期维护");
            db.update("UPDATE hbxx SET sale_status=? WHERE hbid=?",hbxx.getSaleStatus()==null?1:hbxx.getSaleStatus(),hbxx.getHbid());return;
        }
        hbxxMapper.update(hbxx);
    }

    @Override
    @Transactional
    public void delete(Integer hbid) {
        db.queryForList("SELECT hbid FROM hbxx WHERE hbid=? FOR UPDATE",hbid);
        if(db.queryForObject("SELECT (SELECT COUNT(*) FROM jpdd WHERE hbid=?)+(SELECT COUNT(*) FROM seat_hold WHERE hbid=?)",Integer.class,hbid,hbid)>0)throw new IllegalArgumentException("已有交易记录，请改为停售");
        if(db.queryForObject("SELECT COUNT(*) FROM hbxx WHERE hbid=? AND schedule_id IS NOT NULL",Integer.class,hbid)>0)throw new IllegalArgumentException("班期航班请停售，不可删除每日实例");
        // 删除无历史业务的航班
        db.queryForList("SELECT hbid FROM hbxx WHERE hbid=? FOR UPDATE",hbid);
        hbxxCabinMapper.deleteByHbid(hbid);
        hbxxMapper.deleteById(hbid);
    }
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    private void validate(Hbxx f){if(f.getStart()==null||f.getStart().isBlank()||f.getEnd()==null||f.getEnd().isBlank()||f.getStart().equals(f.getEnd())||f.getStarttime()==null||f.getPrice()==null||f.getPrice().signum()<=0||f.getNumber()==null||f.getNumber()<0)throw new IllegalArgumentException("航班地点、时间、票价及座位数不合法");}
}
