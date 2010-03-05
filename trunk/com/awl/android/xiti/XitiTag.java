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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.util.Log;

/**
 * Helper class to send Xiti stats.
 *
 * HOWTO use XitiTag:
 *
 *<ul>
 *  <li>XitiTag must be initialized before use XitiTag.init(context, subdomain_of_xiti_tag, your_website_xiti_id, your_subsite_xiti_id)</li>
 *  <li>then you can call XitiTag.tagPage(name_of_the_page or XitiTag.tagAction(action_name, action_type)</li>
 *  <li>At the end of your application life cycle, call XitiTag.terminate()</li>
 *</ul>
 *
 * When tagAction or tagPage is called, a XitiTagOperation is created and queued in the operationQueue of XitiTag.
 * The operation queue is set to launch one operation at a time.
 * A call to Xiti is then made during the operation with the given parameters + information about the device running the application (see PhoneInformation for more information about what is sent).
 *
 * @see com.awl.android.xiti.PhoneInformation
 * @author Cyril Cauchois
 */
public class XitiTag implements Runnable{
	
	/**
	 * Special operation used to stop thread
	 */
	private final static String END_SIGNAL = "END_SIGNAL";
	
	/**
	 * Log Tag
	 */
	private final static String LOG_TAG = "AWLXITI" ;

	/**
	 * Enum used by tagAction to send actions to Xiti.
	 * Actions are defined with the url parameter "clic"
	 */
	public enum XitiTagActionType { 
		XitiTagActionTypeAction,
		XitiTagActionTypeExit,
		XitiTagActionTypeNavigation,
		XitiTagActionTypeDownload 
	};
	
		
	/**
	 * subdomain of xiti tag	
	 */
	private String subdomain;
	
	/**
	 * website xiti id
	 */
	private String siteId;
	
	/**
	 * subsite xiti id
	 */
	private String subsiteId;
	
	/**
	 * Phone information
	 */
	private PhoneInformation phoneInfo;
	
	/**
	 * Operations queue
	 */
	private BlockingQueue<XitiTagOperation> operationQueue;
	
	/**
	 * XitiTag is a singleton.
	 */
	private static XitiTag instance = null;
	
	/**
	 * Build the single instance
	 * @param context application context
	 * @param subdomain subdomain of xiti tag
	 * @param siteId website xiti id
	 * @param subsiteId subsite xiti id
	 */
	private XitiTag(Context context, String subdomain, String siteId, String subsiteId) {
		
		this.subdomain = subdomain;
		this.siteId = siteId;
		this.subsiteId = subsiteId;
		
		phoneInfo = new PhoneInformation(context);
		operationQueue = new LinkedBlockingQueue<XitiTagOperation>();
	}
	
	/** 
	 * This method must be called to configure XitiTag before any other call.
	 * @param context application or activity context
	 * @param subdomain xiti subdomain. Identifies the server to call for Xiti stats. e.g: http://subd1.xiti.com , subdomain should be "subd1"
	 * @param siteId id of the site
	 * @param subsiteId id of the subsite. can be null (optionnal)
	 */
	public static XitiTag init(Context context, String subdomain, String siteId, String subsiteId) {
		if ( instance == null ) {
			instance = new XitiTag(context, subdomain, siteId, subsiteId);
			// start the thread in charge of XitiTagOperations in operationQueue.
			new Thread(instance).start(); 
		}
		
		return instance;
	}
	
	/**
	 * This method must be called at the end of the life cycle of your activity or application
	 * in order to stop the thread in charge of XitiTagOperations in operationQueue.
	 */
	public static void terminate() {
		if ( instance == null ) {
			throw new IllegalStateException("Xiti tag must be initialized before use.");
		}
		
		instance.launchRequest(END_SIGNAL);
	}
	
	/**
	 * Call this method to tag a page.
	 * @param page name of the page to tag. 'page' will appear in your xiti tag page.
	 */
	public static void tagPage(String page) {
		
		if ( instance == null ) {
			throw new IllegalStateException("Xiti tag must be initialized before use.");
		}
		
		instance.launchRequest("p="+page);
		
	}
	
	/**
	 * Call this method to tag an action.
	 * @param action name of the action to tag. 'action' will appear in your xiti tag page.
	 */
	public static void tagAction(String action, XitiTagActionType actionType) {
		
		if ( instance == null ) {
			throw new IllegalStateException("Xiti tag must be initialized before use.");
		}
		
		instance.launchRequest("p="+action+"&clic='"+stringForActionType(actionType)+"'");
		
	}
	
	/**
	 * Get the Xiti action string for a given action type
	 * @param actionType the action type
	 * @return Xiti action string for the given action type (e.g. XitiTagActionTypeAction => "A")
	 */
	private static String stringForActionType (XitiTagActionType actionType) {
		String result = "N";
		
		switch ( actionType ) {
		case XitiTagActionTypeAction : result = "A"; break;
		case XitiTagActionTypeDownload : result = "T"; break;
		case XitiTagActionTypeExit : result = "S"; break;
		case XitiTagActionTypeNavigation : result = "N"; break;
		}
		
		return result;
	}
	
	/**
	 * put the request into the operationQueue
	 * @param toAppend additional parameters
	 */
	private void launchRequest(String toAppend) {
		
		operationQueue.offer(new XitiTagOperation(buildUrl(toAppend)));
		
	}
	
	/**
	 * Build the url with the given xiti parameters
	 * @param toAppend other parameters to append
	 * @return the url ready to be requested at Xiti servers
	 */
	private String buildUrl(String toAppend) {
		
		//TODO some part of the url could be built once
		
		StringBuffer buf = new StringBuffer("http://");
		
		buf.append(subdomain)
		   .append(".xiti.com/hit.xiti?s=").append(siteId)
		   .append("&").append(URLEncoder.encode(toAppend));
		
		if ( subsiteId != null ) {
			buf.append("&s2=").append(subsiteId);
		}
		
		for (String key : phoneInfo.keySet()) {
			if ( phoneInfo.get(key) != null ) {
				buf.append("&").append(key).append("=").append(URLEncoder.encode(phoneInfo.get(key)));
			}
		}
		
		buf.append("&na=").append(System.currentTimeMillis());
		
		return buf.toString();
		
	}

	/**
	 * Do not use this method.
	 * XitiTag use it for you.
	 */
	@Override
	public void run() {
		
		XitiTagOperation operation = null;
		
		while ( true ) {
			
			try {
				operation = operationQueue.take(); // take a XitiTagOperaion from the operationQueue
				
				if ( END_SIGNAL.equals(operation.getOperation()) ) {
					break; // end of thread
				}
				
				doRequest(operation.getOperation());
				
			} catch (InterruptedException e) {} 
			
		}
	}
	
	/**
	 * Do the http request
	 * @param url url of the request
	 */
	private static void doRequest(String url) {
		
		final HttpClient httpClient  = new DefaultHttpClient();
		final HttpGet httpGet = new HttpGet(url);

		
		try {
			httpClient.execute(httpGet);
		} catch (ClientProtocolException e) {
			Log.d(LOG_TAG, "XitiTag.doRequest("+url+") failed ! ", e);
		} catch (IOException e) {
			Log.d(LOG_TAG, "XitiTag.doRequest("+url+") failed ! ", e);
		}
			

	}
}
