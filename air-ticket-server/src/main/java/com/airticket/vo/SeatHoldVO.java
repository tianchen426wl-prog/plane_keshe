package com.airticket.vo;

import java.math.BigDecimal;
import java.util.Date;

public class SeatHoldVO {
    private Long holdId;
    private Integer hbid;
    private Integer userid;
    private Integer cabinId;
    private Integer quantity;
    private Date createTime;
    private Date expireTime;
    private Integer status;
    // 航班信息
    private String start;
    private String end;
    private Date starttime;
    private String place;
    private String duration;
    private String xclb;
    private String companyName;
    // 舱位信息
    private String cabinName;
    private Integer cabinClass;
    private BigDecimal cabinPrice;
    // 计算字段：总费用
    private BigDecimal totalCost;

    public Long getHoldId() {
        return holdId;
    }

    public void setHoldId(Long holdId) {
        this.holdId = holdId;
    }

    public Integer getHbid() {
        return hbid;
    }

    public void setHbid(Integer hbid) {
        this.hbid = hbid;
    }

    public Integer getUserid() {
        return userid;
    }

    public void setUserid(Integer userid) {
        this.userid = userid;
    }

    public Integer getCabinId() {
        return cabinId;
    }

    public void setCabinId(Integer cabinId) {
        this.cabinId = cabinId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public Date getStarttime() {
        return starttime;
    }

    public void setStarttime(Date starttime) {
        this.starttime = starttime;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getXclb() {
        return xclb;
    }

    public void setXclb(String xclb) {
        this.xclb = xclb;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCabinName() {
        return cabinName;
    }

    public void setCabinName(String cabinName) {
        this.cabinName = cabinName;
    }

    public Integer getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(Integer cabinClass) {
        this.cabinClass = cabinClass;
    }

    public BigDecimal getCabinPrice() {
        return cabinPrice;
    }

    public void setCabinPrice(BigDecimal cabinPrice) {
        this.cabinPrice = cabinPrice;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
}
