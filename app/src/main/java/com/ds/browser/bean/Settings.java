package com.ds.browser.bean;

public class Settings {
    private int id;
    private String name;
    private String value;
    private String flag;
    public Settings() {

    }
    public Settings(int id, String name, String value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }
    public Settings(int id, String name, String value, String flag) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.flag = flag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }
}
