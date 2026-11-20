package com.airticket.entity;

public class Admin {
    private Integer id;
    private String username;
    private String password;
    private Integer role;
    private String menuId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

 private Integer status;
 public Integer getStatus() {return status;}
 public void setStatus(Integer value) {this.status=value;}

 private Integer authVersion;
 public Integer getAuthVersion() {return authVersion;}
 public void setAuthVersion(Integer value) {this.authVersion=value;}
}