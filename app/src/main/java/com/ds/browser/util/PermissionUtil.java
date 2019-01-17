package com.ds.browser.util;

import android.app.Activity;

import pub.devrel.easypermissions.EasyPermissions;

public class PermissionUtil {
    /**
     *
     * @param context
     * return true: 已经获取权限
     * return false: 未获取权限，主动请求权限
     */
    // @AfterPermissionGranted 是可选的
    public static boolean checkPermission(Activity context, String[] perms) {
        return EasyPermissions.hasPermissions(context, perms);
    }
    /**
     * 请求权限
     * @param context
     */
    public static void requestPermission(Activity context, String tip, int requestCode,String[] perms) {
        EasyPermissions.requestPermissions(context, tip, requestCode, perms);
    }
}
