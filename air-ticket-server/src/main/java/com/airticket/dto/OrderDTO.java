package com.airticket.dto;

import javax.validation.constraints.NotNull;

public class OrderDTO {
    private String quoteId;
    private java.util.List<String> seats;
    private java.util.List<String> passengers;
    public String getQuoteId(){return quoteId;}
    public void setQuoteId(String value){quoteId=value;}
    public java.util.List<String> getSeats(){return seats;}
    public void setSeats(java.util.List<String> value){seats=value;}
    public java.util.List<String> getPassengers(){return passengers;}
    public void setPassengers(java.util.List<String> value){passengers=value;}
    private Integer hbid;
    private Integer userid;
    private Integer cabinId;
    private Long holdId;
    private Integer quantity;

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

    public Long getHoldId() {
        return holdId;
    }

    public void setHoldId(Long holdId) {
        this.holdId = holdId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

 private String requestId;
 public String getRequestId() {return requestId;}
 public void setRequestId(String value) {this.requestId=value;}
}
