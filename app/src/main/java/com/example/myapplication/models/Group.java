package com.example.myapplication.models;

import java.io.Serializable;
import java.util.Date;

public class Group implements Serializable {
    public String name,image,second_image,token,id,lastMessage;
    public Date lastDateMessage;
    public Long user_count;
}
