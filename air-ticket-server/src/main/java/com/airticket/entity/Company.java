package com.airticket.entity;

public class Company {
    private String airlineKey,logoPath,sourceUrl;
    private java.sql.Date verifiedAt;
    public String getAirlineKey(){return airlineKey;} public void setAirlineKey(String value){airlineKey=value;}
    public String getLogoPath(){return logoPath;} public void setLogoPath(String value){logoPath=value;}
    public String getSourceUrl(){return sourceUrl;} public void setSourceUrl(String value){sourceUrl=value;}
    public java.sql.Date getVerifiedAt(){return verifiedAt;} public void setVerifiedAt(java.sql.Date value){verifiedAt=value;}

    private Integer companyId;
    private String companyName;
    private String companyProfile;
    private String companyPicture;
    private String companyCulture;
    private String companyPhone;
    private String companyPurport;

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

    public String getCompanyProfile() {
        return companyProfile;
    }

    public void setCompanyProfile(String companyProfile) {
        this.companyProfile = companyProfile;
    }

    public String getCompanyPicture() {
        return companyPicture;
    }

    public void setCompanyPicture(String companyPicture) {
        this.companyPicture = companyPicture;
    }

    public String getCompanyCulture() {
        return companyCulture;
    }

    public void setCompanyCulture(String companyCulture) {
        this.companyCulture = companyCulture;
    }

    public String getCompanyPhone() {
        return companyPhone;
    }

    public void setCompanyPhone(String companyPhone) {
        this.companyPhone = companyPhone;
    }

    public String getCompanyPurport() {
        return companyPurport;
    }

    public void setCompanyPurport(String companyPurport) {
        this.companyPurport = companyPurport;
    }
}