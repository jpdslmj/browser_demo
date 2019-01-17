package com.ds.browser.util;

import android.content.Context;
import android.widget.Toast;


public class ToastUtil {

    public static void showLongToast(Context context, String content){
        Toast.makeText(context.getApplicationContext(),content,Toast.LENGTH_LONG).show();
    }
    public static void showShortToast(Context context, String content){
        Toast.makeText(context.getApplicationContext(),content,Toast.LENGTH_SHORT).show();
    }
}
