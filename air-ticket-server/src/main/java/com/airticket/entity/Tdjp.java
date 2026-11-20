package com.airticket.entity;

import java.util.Date;

public class Tdjp {
    private Integer id;
    private Integer ddid;
    private Date canceltime;
    private Integer process;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDdid() {
        return ddid;
    }

    public void setDdid(Integer ddid) {
        this.ddid = ddid;
    }

    public Date getCanceltime() {
        return canceltime;
    }

    public void setCanceltime(Date canceltime) {
        this.canceltime = canceltime;
    }

    public Integer getProcess() {
        return process;
    }

    public void setProcess(Integer process) {
        this.process = process;
    }

 private String reviewReason;
 public String getReviewReason() {return reviewReason;}
 public void setReviewReason(String value) {this.reviewReason=value;}

 private Integer reviewerId;
 public Integer getReviewerId() {return reviewerId;}
 public void setReviewerId(Integer value) {this.reviewerId=value;}

 private java.util.Date reviewTime;
 public java.util.Date getReviewTime() {return reviewTime;}
 public void setReviewTime(java.util.Date value) {this.reviewTime=value;}
}