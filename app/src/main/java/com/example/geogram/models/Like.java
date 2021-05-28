package com.example.geogram.models;

public class Like {

    /**
     * user id niye kaj korbo caz jodi kno user tar username change kore
     * tobe tar name joto gula namme ase sob gula show krbe tai
     * user id niye e kaj krbo
     *
     */

    private String user_id;

    public Like(String user_id) {
        this.user_id = user_id;
    }

    public Like() {
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    @Override
    public String toString() {
        return "Like{" +
                "user_id='" + user_id + '\'' +
                '}';
    }
}
