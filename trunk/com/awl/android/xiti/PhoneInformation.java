// Copyright 2010 Atos Worldline
//
// Inspired from Backelite bkxititag library for iPhone
// Copyright 2009 Backelite
// see http://code.google.com/p/bkxititag/
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package com.awl.android.xiti;

import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings.System;
import android.util.Log;

/**
 * Retrieves some information to be sent to Xiti and put it in 
 * an HashMap.
 * keys:
 *<ul>
 * <li>lng : locale (fr_fr, en)</li>
 * <li>mdl : model (GT-i5700, ...)</li>
 * <li>os  : name-version (android-1.5, ...)</li>
 * <li>tc  : network type (wifi/gsm)</li>
 * <li>apvr : application version (e.g. [1.0])</li>
 * <li>idclient : uniqueIdentifier from ANDROID_ID</li>
 *</ul>
 * @author Cyril Cauchois
 */
@SuppressWarnings("serial")
public class PhoneInformation extends HashMap<String,String>{
	
	/**
	 * Log tag
	 */
	private final static String LOG_TAG = "AWLXITI" ;
	
	/**
	 * Retrieves some information to be sent to Xiti and put it in 
	 * an HashMap.
	 * @param context application context (e.g. activity)
	 */
	public PhoneInformation(Context context) {
		super();
		
		// Current locale 
		try {
			this.put("lng", removeSpaces(Locale.getDefault().toString()));
		} catch ( Throwable t ) {
			Log.d(LOG_TAG, "PhoneInformation() Unable to get Locale");
		}
		
		// Device model (e.g. GT-I5700)
		try {
			this.put("mdl", removeSpaces(Build.MODEL));
		} catch ( Throwable t ) {
			Log.d(LOG_TAG, "PhoneInformation() Unable to get Model");
		}
		
		// OS name + version (e.g. android-1.5)
		try {
			this.put("os", removeSpaces("android-" + Build.VERSION.RELEASE));
		} catch ( Throwable t ) {
			Log.d(LOG_TAG, "PhoneInformation() Unable to get os version");
		}
		
		// Network connection type (Wifi/GSM)
		try {
			WifiManager wifiMgr = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
			if ( wifiMgr.isWifiEnabled() ) {
				this.put("tc", "wifi");
			} else {
				this.put("tc", "gsm");
			}
		} catch ( Throwable t ) {
			Log.d(LOG_TAG, "PhoneInformation() Unable to get wifi state");
		}
		
		// Application version
		try {
			PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			this.put("apvr", removeSpaces(pkgInfo.versionName));
		} catch ( Throwable t ) {
			Log.d(LOG_TAG, "PhoneInformation() Unable to get application version");
		}
		
		// Unique identifier
		try {
			this.put("idclient", removeSpaces(System.getString(context.getContentResolver(), System.ANDROID_ID)));
		} catch (Throwable t) {
			Log.d(LOG_TAG, "PhoneInformation() Unable to get client id");
		}
		
	}
	
	/**
	 * Remove all spaces from a string
	 * @param in the string to remove spaces from
	 * @return the lower case 'in' string without any space
	 */
	private String removeSpaces(String in) {
		
		if ( in == null ) {
			return null;
		} else if ( "".equals(in) ) {
			return in;
		}
		
		// Removes spaces	
		StringTokenizer st = new StringTokenizer(in," ",false);
		StringBuffer t = new StringBuffer() ;
		while (st.hasMoreElements()) t.append(st.nextElement());
		
		// Lower characters
		return t.toString().toLowerCase();
		
	}
	
}
