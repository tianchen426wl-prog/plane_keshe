package com.airticket.entity;

import java.math.BigDecimal;
import java.util.Date;

public class Jpdd {
    private Integer ddid;
    private Integer hbid;
    private Integer userid;
    private Integer cabinId;
    private Integer quantity;
    private BigDecimal cost;
    private BigDecimal unitPrice;
    private Date ordertime;
    private Integer status;

    public Integer getDdid() {
        return ddid;
    }

    public void setDdid(Integer ddid) {
        this.ddid = ddid;
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

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Date getOrdertime() {
        return ordertime;
    }

    public void setOrdertime(Date ordertime) {
        this.ordertime = ordertime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

 private Long holdId;
 public Long getHoldId() {return holdId;}
 public void setHoldId(Long value) {this.holdId=value;}

 private String orderNo;
 public String getOrderNo() {return orderNo;}
 public void setOrderNo(String value) {this.orderNo=value;}

 private String flightSnapshot;
 public String getFlightSnapshot() {return flightSnapshot;}
 public void setFlightSnapshot(String value) {this.flightSnapshot=value;}
}
