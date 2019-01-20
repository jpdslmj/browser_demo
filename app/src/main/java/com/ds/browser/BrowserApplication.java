package com.ds.browser;

import android.app.Application;
import android.content.Intent;

import com.ds.browser.service.AdvanceLoadX5Service;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsDownloader;


public class BrowserApplication extends Application{

    private static BrowserApplication instance;
    public static BrowserApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preInitX5WebCore();

        // 预加载x5内核
        Intent intent = new Intent(this, AdvanceLoadX5Service.class);
        startService(intent);
    }

    private void preInitX5WebCore() {
        TbsDownloader.needDownload(getApplicationContext(), true);
        QbSdk.setDownloadWithoutWifi(true);
        if (!QbSdk.isTbsCoreInited()) {
            QbSdk.preInit(getApplicationContext(), new QbSdk.PreInitCallback() {
                @Override
                public void onCoreInitFinished() {

                }

                @Override
                public void onViewInitFinished(boolean b) {

                }
            });// 设置X5初始化完成的回调接口
        }
    }


}
