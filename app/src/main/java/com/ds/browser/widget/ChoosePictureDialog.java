package com.ds.browser.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.ds.browser.R;

public class ChoosePictureDialog extends AlertDialog implements View.OnClickListener {

    public ChoosePictureDialog(Context context) {

        super(context);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle();
        initView();
    }

    /**
     * 加载视图
     */
    private void initView() {
        View rootView = View.inflate(getContext(), R.layout.dialog_choose_picture, null);
        rootView.findViewById(R.id.dialog_camera).setOnClickListener(this);
        rootView.findViewById(R.id.dialog_photo_album).setOnClickListener(this);
        rootView.findViewById(R.id.dialog_choose_cancle).setOnClickListener(this);
        this.setContentView(rootView);
    }

    /**
     * 设置特征
     */
    private void setStyle() {
        WindowManager.LayoutParams layoutParams = this.getWindow().getAttributes();
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        layoutParams.width = getPhoneSize().widthPixels * 3 / 4;
        layoutParams.y = 40;
        this.setCanceledOnTouchOutside(false);
    }
    private DisplayMetrics getPhoneSize() {
        return getContext().getResources().getDisplayMetrics();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dialog_choose_cancle:
                this.dismiss();
                break;
            case R.id.dialog_camera:
                this.dismiss();
                if (getResultListener() != null) {
                    getResultListener().camera();
                }
                break;
            case R.id.dialog_photo_album:
                this.dismiss();
                if (getResultListener() != null) {
                    getResultListener().photoAlbum();
                }
                break;
            default:

                break;
        }
    }

    private ResultListener resultListener;

    public ResultListener getResultListener() {
        return resultListener;
    }

    public void setResultListener(ResultListener resultListener) {
        this.resultListener = resultListener;
    }

    public interface ResultListener {
        /**
         * 拍照动作
         */
        void camera();

        /**
         * 图库动作
         */
        void photoAlbum();
    }

}
