package com.example.david.sharing;

/**
 * Created by david on 28-12-2015.
 */
public class File{
    private String name;
    private String link;
    private String ownerMail;

    public String getLink() {
        return link;
    }

    public String getOwnerMail() {
        return ownerMail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setOwnerMail(String ownerMail) {
        this.ownerMail = ownerMail;
    }

    public File(String link, String name, String ownerMail) {
        this.name = name;
        this.link = link;
        this.ownerMail = ownerMail;
    }

    public File(String link, String name) {
        this.name = name;
        this.link = link;
    }
}