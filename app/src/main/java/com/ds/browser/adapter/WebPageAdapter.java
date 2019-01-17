package com.ds.browser.adapter;

import android.support.v4.app.FragmentManager;

import com.ds.browser.util.WebPageHelper;
import com.ds.browser.fragment.WebViewFragment;


public class WebPageAdapter extends FragmentPagerAdapter {
    public WebPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public WebViewFragment getItem(int position) {
        return WebPageHelper.webpagelist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return WebPageHelper.webpagelist.get(position).hashCode();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return WebPageHelper.webpagelist.size();
    }

}
