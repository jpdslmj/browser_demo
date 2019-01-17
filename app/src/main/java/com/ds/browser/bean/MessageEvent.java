package com.ds.browser.bean;


public class MessageEvent {
    private int viewTop;
    public MessageEvent(int top){
        viewTop=top;
    }
    public int getViewTop(){
        return viewTop;
    }
}
