package com.ds.browser.bean;

public class User implements Comparable{
    private String username;
    private String password;
    private int flag;

    public User(){

    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public User(String username, String password, int flag) {
        this.username = username;
        this.password = password;
        this.flag = flag;
    }

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


    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
