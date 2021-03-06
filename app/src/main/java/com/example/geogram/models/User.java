package com.example.geogram.models;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {

    private String user_id;
    //private long phone_number;
    private String email;
    private String username;

    public User(String user_id, String email, String username) {
        this.user_id = user_id;
        //this.phone_number = phone_number;
        this.email = email;
        this.username = username;
    }

    public User() {
    }

    protected User(Parcel in) {
        user_id = in.readString();
        //phone_number = in.readLong();
        email = in.readString();
        username = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    /*public Long getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(Long phone_number) {
        this.phone_number = phone_number;
    }*/

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "User{" +
                "user_id='" + user_id + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                '}';
        //", phone_number=" + phone_number +
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(user_id);
        //parcel.writeLong(phone_number);
        parcel.writeString(email);
        parcel.writeString(username);
    }
}
