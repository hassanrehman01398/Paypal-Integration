package com.example.ecommerce.Model;

public class Users
{
    private String username, phone, password, image, address;
    public  Users()
    {

    }

    public Users(String username, String phone, String password, String image, String address) {
        this.username = username;
        this.phone = phone;
        this.password = password;
        this.image = image;
        this.address = address;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

