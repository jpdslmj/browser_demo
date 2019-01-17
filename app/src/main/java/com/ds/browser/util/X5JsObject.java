package com.ds.browser.util;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.ds.browser.bean.User;
import com.ds.browser.common.biometriclib.BiometricPromptManager;
import com.ds.browser.constant.Constant;
import com.ds.browser.widget.X5WebView;
import com.google.gson.Gson;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.ValueCallback;

import java.io.File;
import java.util.ArrayList;

public class X5JsObject {

    private Context context;
    private X5WebView webView;
    private BiometricPromptManager biometricPromptManager;
    private SPHelper spHelper;
    final int version = Build.VERSION.SDK_INT;

    public X5JsObject(Context context) {
        this.context = context;
    }
    public X5JsObject(Context context, X5WebView webView, BiometricPromptManager biometricPromptManager) {
        this.context = context;
        this.webView = webView;
        this.biometricPromptManager = biometricPromptManager;
    }

    @JavascriptInterface
    public void initUserDataBySQLite() {
        BrowserDBHelper.getBrowserDBHelper(context).searchUserTable(new BrowserDBHelper.OnSearchUserTableListener(){
            @Override
            public void onResult(ArrayList<User> list) {
                if (list!=null&&list.size()>0){
                    User user = list.get(0);
                    final String username = user.getUsername();
                    final String password = user.getPassword();
                    webView.post(new Runnable() {
                        @Override
                        public void run() {
                            //webView.loadUrl("javascript:setUserData('"+username+"','"+password+"')");
                            webView.evaluateJavascript("javascript:setUserData('"+username+"','"+password+"')", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    callLoginBiometricPrompt();
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    @JavascriptInterface
    public void saveUserDataBySp(String userData) {
        // 检查是否存在，不存在或匹配，保存或重新保存
        spHelper = new SPHelper(context,"user");
        String username = (String)spHelper.getSharedPreference("username",String.class);
        String password = (String)spHelper.getSharedPreference("password",String.class);
        if (TextUtils.isEmpty(username)&&TextUtils.isEmpty(password)){
            Gson gs = new Gson();
            User user = gs.fromJson(userData, User.class);
            spHelper.put("username", user.getUsername());
            spHelper.put("password", user.getPassword());
        }
    }

    @JavascriptInterface
    public void saveUserDataBySQLite(String userData) {
        if (!TextUtils.isEmpty(userData)){

            Gson gs = new Gson();
            final User user = gs.fromJson(userData, User.class);


            // 清除cookie
            CookieManager cookieManager = CookieManager.getInstance();
            if (Constant.ANDROID_VERSION >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.removeSessionCookies(null);
                cookieManager.removeAllCookie();
                cookieManager.flush();
            } else {
                cookieManager.removeSessionCookies(null);
                cookieManager.removeAllCookie();
                CookieSyncManager.getInstance().sync();
            }


            deleteFile(new File(context.getDir("webview",0).getPath()+"/Cache"));
            deleteFile(new File(context.getDir("webview",0).getPath()+"/Local Storage"));
            deleteFile(new File(context.getDir("browser_cache",0).getPath()+"/Cache"));
            deleteFile(new File(context.getDir("browser_cache",0).getPath()+"/Local Storage"));


            BrowserDBHelper.getBrowserDBHelper(context).searchUserTable(new BrowserDBHelper.OnSearchUserTableListener() {
                @Override
                public void onResult(ArrayList<User> mUserData) {
                    if (mUserData.size()>0){
                        // 查数据是否存在，不存在提示是否，重新授权
                        String sql = "select * from "
                                + BrowserDBHelper.USERTB_NAME
                                + " where username = '"+user.getUsername()+"'"
                                + " and password = '"+user.getPassword()+"'";
                        BrowserDBHelper.getBrowserDBHelper(context).searchUserTable(sql,new BrowserDBHelper.OnSearchUserTableListener(){
                            @Override
                            public void onResult(ArrayList<User> mUserData) {

                                if (mUserData==null||mUserData.size()<1){
                                    AppUtil.createDialog(context,
                                            "授权提示",
                                            "检测到新账号，是否重新授权账号？",
                                            "确定"
                                            , new DialogInterface.OnClickListener() {// 确定
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                    // 删除就数据，并重新保存
                                                    BrowserDBHelper.getBrowserDBHelper(context).deleteTableItem(BrowserDBHelper.USERTB_NAME, null);
                                                    BrowserDBHelper.getBrowserDBHelper(context).updateUserTable(user);
                                                    /*

                                                    webView.evaluateJavascript("javascript:androidLogin()", new ValueCallback<String>() {
                                                        @Override
                                                        public void onReceiveValue(String value) {

                                                        }
                                                    });

                                                    */
                                                    if (biometricPromptManager.isBiometricPromptEnable()) {
                                                        Toast.makeText(context, "授权成功，指纹功能已经开启", Toast.LENGTH_LONG).show();
                                                    }else{
                                                        Toast.makeText(context, "授权成功，指纹功能未开启", Toast.LENGTH_LONG).show();
                                                    }
                                                    callLoginBiometricPrompt();
                                                }
                                            }, new DialogInterface.OnClickListener() {// 取消
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    AppUtil.createDialog(context,
                                                            "提示",
                                                            "请使用已授权的账号登陆，或重新授权",
                                                            "确定"
                                                            , new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {

                                                                }
                                                            }, null);
                                                }
                                            });
                                }else{
                                    /*
                                    webView.evaluateJavascript("javascript:androidLogin()", new ValueCallback<String>() {
                                        @Override public void onReceiveValue(String value) {

                                        }
                                    });
                                    */
                                    callLoginBiometricPrompt();
                                }
                            }
                        });
                    }else {// 非新账号登录或者为首次登录
                        BrowserDBHelper.getBrowserDBHelper(context).updateUserTable(user);

                        if (biometricPromptManager.isBiometricPromptEnable()) {
                            Toast.makeText(context, "授权成功，指纹功能已经开启", Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(context, "授权成功，指纹功能未开启", Toast.LENGTH_LONG).show();
                        }

                        /*
                        webView.evaluateJavascript("javascript:androidLogin()", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {

                            }
                        });
                        */
                        callLoginBiometricPrompt();
                    }
                }
            });

        }
    }

    /*
     * 调用指纹认证-登陆
     * */
    public void callLoginBiometricPrompt() {
        if (biometricPromptManager.isBiometricPromptEnable()) {
            biometricPromptManager.authenticate(new BiometricPromptManager.OnBiometricIdentifyCallback() {
                @Override
                public void onUsePassword() { }

                @Override
                public void onSucceeded() {
                    webView.evaluateJavascript("javascript:androidLogin()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {

                        }
                    });
                }

                @Override
                public void onFailed() { }


                @Override
                public void onError(int code, String reason) { }

                @Override
                public void onCancel() { }
            });
        }else{
            Toast.makeText(context, "请开启指纹识别功能", Toast.LENGTH_SHORT).show();
        }
    }
    /*
     * 调用指纹认证-普通调用
     * */
    @JavascriptInterface
    public void callBiometricPrompt() {

        if (biometricPromptManager.isBiometricPromptEnable()) {
            biometricPromptManager.authenticate(new BiometricPromptManager.OnBiometricIdentifyCallback() {
                @Override
                public void onUsePassword() {

                }

                @Override
                public void onSucceeded() {
                    //webView.loadUrl("javascript:biometricPromptReturn()");
                    webView.evaluateJavascript("javascript:biometricPromptReturn()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {

                        }
                    });
                }

                @Override
                public void onFailed() {
                    Toast.makeText(context, "指纹调用失败", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(int code, String reason) {
                    Toast.makeText(context, "指纹调用异常", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCancel() {
                    Toast.makeText(context, "您已经取消指纹签证", Toast.LENGTH_LONG).show();
                }
            });
        }else{
            Toast.makeText(context, "指纹功能为开启", Toast.LENGTH_LONG).show();
        }
    }
    private void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteFile(f);
            }
            //file.delete(); //如要保留文件夹，只删除文件，请注释这行
        } else if (file.exists()) {
            file.delete();
        }
    }
}
