package com.ds.browser.util;

import com.ds.browser.bean.User;

import java.util.List;

public interface IWebViewJS {

	void initUserDataBySQLite();
	void saveUserDataBySp(String userData);
	void saveUserDataBySQLite(String userData);
	void callBiometricPrompt();
}
