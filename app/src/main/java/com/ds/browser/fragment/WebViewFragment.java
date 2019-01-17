package com.ds.browser.fragment;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.ds.browser.R;
import com.ds.browser.adapter.PopupMenuAdapter;
import com.ds.browser.common.biometriclib.BiometricPromptManager;
import com.ds.browser.task.ResolveDownloadUrlTask;
import com.ds.browser.util.AppUtil;
import com.ds.browser.util.BrowserDBHelper;
import com.ds.browser.util.SPHelper;
import com.ds.browser.util.X5JsObject;
import com.ds.browser.widget.X5WebView;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.util.ArrayList;
import java.util.List;



public class WebViewFragment extends android.support.v4.app.Fragment{
    public final static int LOAD_IN_NEW_WINDOW = 0;
    public final static int LOAD_IN_BACKGROUND = 1;
    public final static int FREE_REPLICATION = 2;
    public final static int COPY_LINK = 3;
    public final static int DOWNLOAD_IMAGE = 4;

    private Bundle bundle;
    private OnWebViewListener wl;
    private X5WebView webView;
    private View cache;
    private FrameLayout webViewContainer;
    private int touchPointX, touchPointY, mHeight, mWidth;
    private PopupWindow quickAction;
    private RecyclerView popupMenuList;
    private String extra;
    private String url;

    private BiometricPromptManager biometricPromptManager;
    private SPHelper spHelper;


    private String cookie;

    public WebViewFragment() {
    }

    @SuppressLint("ValidFragment")
    public WebViewFragment(Bundle savedInstanceState, OnWebViewListener onWebViewListener) {
        this(savedInstanceState, onWebViewListener, null);
    }

    @SuppressLint("ValidFragment")
    public WebViewFragment(Bundle savedInstanceState, OnWebViewListener onWebViewListener, String url) {
        bundle = savedInstanceState;  //为空表示是用户手动添加标签页
        this.wl = onWebViewListener;
        if (url!=null) {
            this.url = url;
        }
        Log.d(WebViewFragment.class.getSimpleName(),this.url);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
        Log.d("WP", "Fragment onSavedInstance");
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("WP", "调用onCreateView cache :" + cache);

        biometricPromptManager = BiometricPromptManager.from(getActivity());

        if (cache == null) {
            cache = inflater.inflate(R.layout.webview_fragment, container, false);
            webView = cache.findViewById(R.id.web_view);   //TBS WebView必须在布局中创建，否则网页视频无法全屏
            WebSettings setting = webView.getSettings();
            setSettings(setting);
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onReceivedTitle(WebView view, String title) {
                    if (!title.equals("") && !title.contains("https") && !title.contains("http")) {
                        BrowserDBHelper.getBrowserDBHelper(getActivity()).updateHistoryTable(view.getUrl(), title);
                        Log.d("web_view", title + " " + view.getUrl());
                    }
                    super.onReceivedTitle(view, title);
                }


                @Override
                public void onProgressChanged(WebView webView, int i) {
                    if (wl != null) wl.onProgressChanged(webView, i);
                    super.onProgressChanged(webView, i);
                }

            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Log.d("WP", "调用shouldOverrideUrlLoading :" + url);
                    if (!URLUtil.isValidUrl(url))
                        return true;
                    /*
                    if(url.contains("/download/")){
                        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            Toast.makeText(getActivity(), "请检查手机SD卡", Toast.LENGTH_SHORT).show();
                        } else {
                            cookie =  CookieManager.getInstance().getCookie(url);
                            new ResolveDownloadUrlTask(getActivity(), cache).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, cookie);
                        }
                        return true;
                    }
                    */
                    return false;
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {
                    Log.d("WP", "调用shouldOverrideUrlLoading webResourceRequest:" + webResourceRequest.toString());
                    return super.shouldOverrideUrlLoading(webView, webResourceRequest);
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    Log.d("appo", "onPageStarted" + url);
                    if (wl != null) wl.onPageStarted(view, url, favicon);
                    super.onPageStarted(view, url, favicon);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    blockAds(view);//过滤
                    Log.d("appo", "onPageFinished" + url);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    if (wl != null) wl.onReceivedError(view, errorCode, description, failingUrl);
                    super.onReceivedError(view, errorCode, description, failingUrl);
                }
            });
            View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    WebView.HitTestResult result = webView.getHitTestResult();
                    int resultType = result.getType();
                    extra = result.getExtra();
                    Log.d("Ming", resultType + "");
                    boolean showCustomPopup=false;

                    //int xOff, yOff;
                    switch (resultType) {
                        case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                            initLoadingWebQuickAction();
                            showCustomPopup=true;
                            break;
                        case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                        case WebView.HitTestResult.IMAGE_TYPE:
                            initImageQuickAction();
                            showCustomPopup=true;
                            break;
                    }
                    if (showCustomPopup){
                        Point point=new Point();
                        getActivity().getWindowManager().getDefaultDisplay().getSize(point);
                        int xOff = (touchPointX + mWidth)>point.x?touchPointX-mWidth:touchPointX;
                        int yOff = (touchPointY + mHeight) > point.y ? touchPointY - mHeight : touchPointY;
                        quickAction.showAtLocation(v, Gravity.TOP|Gravity.START, xOff,yOff);
                        return true;
                    }
                    return false;
                }
            };

            webView.setOnLongClickListener(onLongClickListener);
            webView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    touchPointX = (int) event.getRawX();
                    touchPointY = (int) event.getRawY();
                    return false;
                }
            });


            webView.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                    Log.d("DownloadListener", "onDownloadStart url:" + url +",contentDisposition:"+contentDisposition+",contentLength:"+contentLength);
                    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        Toast.makeText(getActivity(), "请检查手机SD卡", Toast.LENGTH_SHORT).show();
                    } else {
                        cookie =  CookieManager.getInstance().getCookie(url);
                        new ResolveDownloadUrlTask(getActivity(), cache , cookie).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
                    }

                }

            });
            webView.addJavascriptInterface(new X5JsObject(getContext(),webView,biometricPromptManager),"android");



            if (bundle != null)
                webView.restoreState(bundle);
            else if (savedInstanceState != null)
                webView.restoreState(savedInstanceState);
            else
                webView.loadUrl(url);

            if (wl != null)
                wl.onGetWebView(webView);  //新添加的fragment
            webViewContainer = cache.findViewById(R.id.frame_layout);

        }
        return cache;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        //BrowserApplication.getRefWatcher(getActivity()).watch(this);
    }


    @SuppressLint("SetJavaScriptEnabled")
    private void setSettings(WebSettings webSettings) {
        //noinspection deprecation

        //webSettings.setDefaultTextEncodingName("UTF-8"); //设置默认的字符编码集，默认”UTF-8”
        webSettings.setAllowFileAccess(true); // 允许访问文件
        webSettings.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        webSettings.setJavaScriptEnabled(true); // 开启JavaScript支持
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);   //允许放大缩小
        webSettings.setDisplayZoomControls(false);    //去掉放大缩小框

        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setSupportMultipleWindows(false);

        // 是否允许通过file url加载的Javascript读取本地文件，默认值 false
        webSettings.setAllowFileAccessFromFileURLs(true);
        // 是否允许通过file url加载的Javascript读取全部资源(包括文件,http,https)，默认值 false
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webSettings.setGeolocationEnabled(true); //允许启用地理定位
        webSettings.setGeolocationDatabasePath(getActivity().getDir("geolocation", 0).getPath());// 定位数据保存路径
        webSettings.setSaveFormData(true); //支持保存自动填充的表单数据
        webSettings.setDomStorageEnabled(true); //DOM存储API是否可用，默认false


        webSettings.setDatabaseEnabled(true); //数据库存储API是否可用，默认值false
        webSettings.setDatabasePath(getActivity().getDir("databases", 0).getPath());

        // 设置缓存
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        //webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(getActivity().getDir("browser_cache", 0).getPath());
        webSettings.setAppCacheMaxSize(Long.MAX_VALUE);

        // 设置渲染优先级
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        webSettings.setPluginState(WebSettings.PluginState.ON_DEMAND);
        // 全屏显示
        webSettings.setUseWideViewPort(true);
        webSettings.setTextZoom(Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("text_size", "100")));

    }

    private void blockAds(WebView view) {
        String tags = view.getUrl();
        //noinspection MismatchedQueryAndUpdateOfStringBuilder
        StringBuilder sb = new StringBuilder();
        sb.append("javascript: ");
        String[] allTag = tags.split(",");
        for (String tag : allTag) {
            String adTag = tag;
            if (adTag.trim().length() > 0) {
                adTag = adTag.trim();
                if (adTag.contains("#")) {
                    adTag = adTag.substring(adTag.indexOf("#") + 1);
                    sb.append("document.getElementById(\'").append(adTag).append("\').remove();");
                } else if (adTag.contains(".")) {
                    adTag = adTag.substring(adTag.indexOf(".") + 1);
                    sb.append("var esc=document.getElementsByClassName(\'").append(adTag).append("\');for (var i = esc.length - 1; i >= 0; i--){esc[i].remove();};");

                } else {
                    sb.append("var esc=document.getElementsByTagName(\'").append(adTag).append("\');for (var i = esc.length - 1; i >= 0; i--){esc[i].remove();};");
                }
            }
        }
    }

    public X5WebView getInnerWebView() {
        return webView;
    }

    public FrameLayout getInnerContainer() {
        return webViewContainer;
    }


    public interface OnWebViewListener {
        void onGetWebView(X5WebView webView);

        void onPageStarted(WebView view, String url, Bitmap favicon);

        void onReceivedError(WebView view, int errorCode, String description, String failingUrl);

        void onProgressChanged(WebView webView, int i);

        void onQuickActionClick(WebView webView, int itemId, String extra);
    }

    private void initLoadingWebQuickAction() {
        @SuppressLint("InflateParams")
        View pop_layout = LayoutInflater.from(getActivity()).inflate(R.layout.popup_menu, null);
        List<String> data=new ArrayList<>();
        data.add("新窗口打开");
        data.add("后台打开");
        data.add("自由复制");
        data.add("复制链接");
        popupMenuList=pop_layout.findViewById(R.id.popup_menu_list);
        PopupMenuAdapter adapter=new PopupMenuAdapter(getActivity(),data);
        adapter.setOnItemClickListener(new PopupMenuAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                quickAction.dismiss();
                switch (position){
                    case 0:
                        wl.onQuickActionClick(webView, LOAD_IN_NEW_WINDOW, extra);
                        break;
                    case 1:
                        wl.onQuickActionClick(webView, LOAD_IN_BACKGROUND, extra);
                        break;
                    case 2:
                        wl.onQuickActionClick(webView, FREE_REPLICATION, extra);
                        break;
                    case 3:
                        wl.onQuickActionClick(webView, COPY_LINK, extra);
                        break;
                }
            }
        });
        popupMenuList.setAdapter(adapter);
        quickAction = new PopupWindow(AppUtil.dip2px(getActivity(), 150), ViewGroup.LayoutParams.WRAP_CONTENT);
        quickAction.setContentView(pop_layout);
        quickAction.setFocusable(true);
        quickAction.setOutsideTouchable(true);
        quickAction.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        pop_layout.measure(w, h);
        //获取PopWindow宽和高
        mHeight = pop_layout.getMeasuredHeight();
        mWidth = pop_layout.getMeasuredWidth();
    }

    private void initImageQuickAction() {
        @SuppressLint("InflateParams")
        View pop_layout = LayoutInflater.from(getActivity()).inflate(R.layout.popup_menu, null);
        List<String> data=new ArrayList<>();
        data.add("保存图片");
        data.add("复制链接");
        popupMenuList=pop_layout.findViewById(R.id.popup_menu_list);
        PopupMenuAdapter adapter=new PopupMenuAdapter(getActivity(),data);
        adapter.setOnItemClickListener(new PopupMenuAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                quickAction.dismiss();
                switch (position){
                    case 0:
                        wl.onQuickActionClick(webView, DOWNLOAD_IMAGE, extra);
                        break;
                    case 1:
                        wl.onQuickActionClick(webView,COPY_LINK, extra);
                        break;
                }
            }
        });
        popupMenuList.setAdapter(adapter);
        quickAction = new PopupWindow(AppUtil.dip2px(getActivity(), 150), ViewGroup.LayoutParams.WRAP_CONTENT);
        quickAction.setContentView(pop_layout);
        quickAction.setFocusable(true);
        quickAction.setOutsideTouchable(true);
        quickAction.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        pop_layout.measure(w, h);
        //获取PopWindow宽和高
        mHeight = pop_layout.getMeasuredHeight();
        mWidth = pop_layout.getMeasuredWidth();
    }
}

