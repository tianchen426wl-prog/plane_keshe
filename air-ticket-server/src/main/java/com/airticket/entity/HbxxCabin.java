package com.airticket.entity;

import java.math.BigDecimal;

public class HbxxCabin {
    private Integer cabinId;
    private Integer hbid;
    private Integer cabinClass;
    private String cabinName;
    private BigDecimal price;
    private Integer totalNumber;
    private Integer availableNumber;

    public Integer getCabinId() {
        return cabinId;
    }

    public void setCabinId(Integer cabinId) {
        this.cabinId = cabinId;
    }

    public Integer getHbid() {
        return hbid;
    }

    public void setHbid(Integer hbid) {
        this.hbid = hbid;
    }

    public Integer getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(Integer cabinClass) {
        this.cabinClass = cabinClass;
    }

    public String getCabinName() {
        return cabinName;
    }

    public void setCabinName(String cabinName) {
        this.cabinName = cabinName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getTotalNumber() {
        return totalNumber;
    }

    public void setTotalNumber(Integer totalNumber) {
        this.totalNumber = totalNumber;
    }

    public Integer getAvailableNumber() {
        return availableNumber;
    }

    public void setAvailableNumber(Integer availableNumber) {
        this.availableNumber = availableNumber;
    }

 private Integer status;
 public Integer getStatus() {return status;}
 public void setStatus(Integer value) {this.status=value;}
}
