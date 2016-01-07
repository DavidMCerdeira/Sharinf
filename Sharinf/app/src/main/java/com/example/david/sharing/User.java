package com.example.david.sharing;

/**
 * Created by david on 12-12-2015.
 */
public class User {
    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    String mail;

    User(String mail){
        this.mail = mail;
    }
}
