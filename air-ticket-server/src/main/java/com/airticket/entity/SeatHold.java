package com.airticket.entity;

import java.util.Date;

public class SeatHold {
    private Long holdId;
    private Integer hbid;
    private Integer cabinId;
    private Integer userid;
    private Integer quantity;
    private Date createTime;
    private Date expireTime;
    private Integer status;

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

    public Integer getCabinId() {
        return cabinId;
    }

    public void setCabinId(Integer cabinId) {
        this.cabinId = cabinId;
    }

    public Integer getUserid() {
        return userid;
    }

    public void setUserid(Integer userid) {
        this.userid = userid;
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

 private java.math.BigDecimal unitPrice;
 public java.math.BigDecimal getUnitPrice() {return unitPrice;}
 public void setUnitPrice(java.math.BigDecimal value) {this.unitPrice=value;}

 private String requestId;
 public String getRequestId() {return requestId;}
 public void setRequestId(String value) {this.requestId=value;}
}
