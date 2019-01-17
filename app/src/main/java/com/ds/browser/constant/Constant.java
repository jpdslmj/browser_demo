package com.ds.browser.constant;

import android.Manifest;
import android.os.Build;

public class Constant {
    public static final int ANDROID_VERSION = Build.VERSION.SDK_INT;


    //public static final String APP_ADDR = "http://192.168.1.33:8080/login";
    public static final String APP_ADDR = "http://www.baidu.com";
    /**
     * 文件
     */
    public static final int REQUEST_FILE_PICKER = 129;
    /**
     * 相机，图库的请求code
     */
    public final static int REQUEST_CODE_IMAGE_CAPTURE = 120;
    public final static int REQUEST_CODE_GALLERY = 121;
    public static final int REQUEST_CODE_CHOOSE_MATISSE = 0x7258;
    public static final int REQUEST_CODE_CHOOSE_MULTIPLE_IMG = 0x258;

    public static final int CAMERA_PERMISSION_CODE = 110;
    public static final String[] CAMERA_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

}
