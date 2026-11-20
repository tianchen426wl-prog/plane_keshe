package com.airticket.dto;

import java.math.BigDecimal;

/** Shared by search and date prices so pagination and calendar use identical constraints. */
public class FlightFilter {
 private int quantity=1;
 private Integer companyId,cabinClass,departureFrom,departureTo,minBaggage;
 private BigDecimal maxTotal;
 private boolean onlyAvailable=false;
 public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;}
 public Integer getCompanyId(){return companyId;} public void setCompanyId(Integer v){companyId=v;}
 public Integer getCabinClass(){return cabinClass;} public void setCabinClass(Integer v){cabinClass=v;}
 public Integer getDepartureFrom(){return departureFrom;} public void setDepartureFrom(Integer v){departureFrom=v;}
 public Integer getDepartureTo(){return departureTo;} public void setDepartureTo(Integer v){departureTo=v;}
 public Integer getMinBaggage(){return minBaggage;} public void setMinBaggage(Integer v){minBaggage=v;}
 public BigDecimal getMaxTotal(){return maxTotal;} public void setMaxTotal(BigDecimal v){maxTotal=v;}
 public boolean isOnlyAvailable(){return onlyAvailable;} public void setOnlyAvailable(boolean v){onlyAvailable=v;}
 public void validate(){
  if(quantity<1||quantity>5||(cabinClass!=null&&(cabinClass<1||cabinClass>3))||(departureFrom!=null&&(departureFrom<0||departureFrom>23))||(departureTo!=null&&(departureTo<1||departureTo>24))||(departureFrom!=null&&departureTo!=null&&departureFrom>=departureTo)||(minBaggage!=null&&(minBaggage<0||minBaggage>100))||(maxTotal!=null&&(maxTotal.signum()<0||maxTotal.compareTo(new BigDecimal("1000000"))>0)))throw new IllegalArgumentException("人数、预算或筛选条件不合法");
 }
}
