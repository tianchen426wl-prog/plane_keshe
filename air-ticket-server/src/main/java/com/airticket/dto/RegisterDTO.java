package com.airticket.dto;

import javax.validation.constraints.NotBlank;

public class RegisterDTO {
    @NotBlank
    @javax.validation.constraints.Pattern(regexp="[A-Za-z0-9_]{3,50}")
    private String username;
    @NotBlank
    @javax.validation.constraints.Size(min=6,max=64)
    private String password;
    @NotBlank
    @javax.validation.constraints.Size(max=50)
    private String name;
    @javax.validation.constraints.Min(0) @javax.validation.constraints.Max(1)
    private Integer gender;
    @javax.validation.constraints.Pattern(regexp="^$|1[0-9]{10}")
    private String phone;
    @javax.validation.constraints.Email
    private String email;
    private String identity;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
