package com.airticket.entity;

import java.math.BigDecimal;
import java.util.Date;

public class Hbxx {
    private Integer hbid;
    private Integer companyId;
    private String companyName;
    private String xclb;
    private String start;
    private String end;
    private Date starttime;
    private Integer number;
    private BigDecimal price;
    private String duration;
    private String place;

    public Integer getHbid() {
        return hbid;
    }

    public void setHbid(Integer hbid) {
        this.hbid = hbid;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getXclb() {
        return xclb;
    }

    public void setXclb(String xclb) {
        this.xclb = xclb;
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

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

 private Integer saleStatus;
 public Integer getSaleStatus() {return saleStatus;}
 public void setSaleStatus(Integer value) {this.saleStatus=value;}

 private String flightNo;
 public String getFlightNo() {return flightNo;}
 public void setFlightNo(String value) {this.flightNo=value;}
}