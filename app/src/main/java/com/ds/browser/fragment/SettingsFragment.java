package com.ds.browser.fragment;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.ds.browser.R;
import com.ds.browser.task.ClearCacheTask;
import com.ds.browser.util.SPHelper;
import com.ds.browser.widget.PreferenceHead;


public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_settings);

        ((PreferenceHead)findPreference("configHead")).setOnBackButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsFragment.this.getActivity().finish();
            }
        });

        findPreference("clear_cache").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                new ClearCacheTask(getActivity()).execute(newValue.toString());
                return false;
            }
        });

        final EditTextPreference serverAddr = (EditTextPreference)findPreference("preference_addr");
        final SPHelper spHelper = new SPHelper(getActivity(),"custom_setting");
        String addr = (String)spHelper.getSharedPreference("preference_addr", "http://192.168.1.29:8080/index");
        serverAddr.setSummary(addr);
        serverAddr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                serverAddr.setSummary(newValue.toString());
                serverAddr.setDefaultValue(newValue);
                spHelper.put("preference_addr",newValue);
                return true;
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        assert view != null;
        view.setBackgroundColor(Color.WHITE);
        /*
        去除preference两边的空白,可修改子preference分割线
         */
        ListView listView=view.findViewById(android.R.id.list);
        listView.setPadding(0,listView.getPaddingTop(),0,listView.getPaddingBottom());
        return view;
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference!=null){
            if (preference.getKey().equals("restore_default")){
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putString("text_size","100");
                editor.putString("theme_color","#474747");
                editor.putStringSet("clear_cache",null);
                editor.apply();
                getActivity().finish();
                return true;
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
