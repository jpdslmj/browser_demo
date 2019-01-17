package com.ds.browser.widget;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ds.browser.R;
public class MPreferenceCategory extends PreferenceCategory {
    private Context context;

    @SuppressWarnings("unused")
    public MPreferenceCategory(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context=context;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        return LayoutInflater.from(context).inflate(R.layout.contacts_category,parent,false);
    }

}
