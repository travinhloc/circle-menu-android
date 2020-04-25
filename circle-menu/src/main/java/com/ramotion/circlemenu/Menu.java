package com.ramotion.circlemenu;

public class Menu {
    int res;
    String title;
    int badge;

    public Menu(int res, String title, int badge) {
        this.res = res;
        this.title = title;
        this.badge = badge;
    }

    public int getRes() {
        return res;
    }

    public void setRes(int res) {
        this.res = res;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getBadge() {
        return badge;
    }

    public void setBadge(int badge) {
        this.badge = badge;
    }
}
