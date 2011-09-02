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
import java.util.Vector;
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
	
	
	/** Index du 1er caractere accentué **/
    private static final int MIN = 192;
    /** Index du dernier caractere accentué **/
    private static final int MAX = 255;
	
    /** Vecteur de correspondance entre accent / sans accent **/
    private static final Vector<String> map = initMap();
    
    /** Initialisation du tableau de correspondance entre les caractéres accentués
       * et leur homologues non accentués 
       */
    private static Vector<String> initMap()
    {  Vector<String> result         = new Vector<String>();
       java.lang.String car  = null;
      
       car = new java.lang.String("A");
       result.add( car );            /* '\u00C0'   À   alt-0192  */ 
       result.add( car );            /* '\u00C1'   Á   alt-0193  */
       result.add( car );            /* '\u00C2'   Â   alt-0194  */
       result.add( car );            /* '\u00C3'   Ã   alt-0195  */
       result.add( car );            /* '\u00C4'   Ä   alt-0196  */
       result.add( car );            /* '\u00C5'   Å   alt-0197  */
       car = new java.lang.String("AE");
       result.add( car );            /* '\u00C6'   Æ   alt-0198  */
       car = new java.lang.String("C");
       result.add( car );            /* '\u00C7'   Ç   alt-0199  */
       car = new java.lang.String("E");
       result.add( car );            /* '\u00C8'   È   alt-0200  */
       result.add( car );            /* '\u00C9'   É   alt-0201  */
       result.add( car );            /* '\u00CA'   Ê   alt-0202  */
       result.add( car );            /* '\u00CB'   Ë   alt-0203  */
       car = new java.lang.String("I");
       result.add( car );            /* '\u00CC'   Ì   alt-0204  */
       result.add( car );            /* '\u00CD'   Í   alt-0205  */
       result.add( car );            /* '\u00CE'   Î   alt-0206  */
       result.add( car );            /* '\u00CF'   Ï   alt-0207  */
       car = new java.lang.String("D");
       result.add( car );            /* '\u00D0'   Ð   alt-0208  */
       car = new java.lang.String("N");
       result.add( car );            /* '\u00D1'   Ñ   alt-0209  */
       car = new java.lang.String("O");
       result.add( car );            /* '\u00D2'   Ò   alt-0210  */
       result.add( car );            /* '\u00D3'   Ó   alt-0211  */
       result.add( car );            /* '\u00D4'   Ô   alt-0212  */
       result.add( car );            /* '\u00D5'   Õ   alt-0213  */
       result.add( car );            /* '\u00D6'   Ö   alt-0214  */
       car = new java.lang.String("*");
       result.add( car );            /* '\u00D7'   ×   alt-0215  */
       car = new java.lang.String("0");
       result.add( car );            /* '\u00D8'   Ø   alt-0216  */
       car = new java.lang.String("U");
       result.add( car );            /* '\u00D9'   Ù   alt-0217  */
       result.add( car );            /* '\u00DA'   Ú   alt-0218  */
       result.add( car );            /* '\u00DB'   Û   alt-0219  */
       result.add( car );            /* '\u00DC'   Ü   alt-0220  */
       car = new java.lang.String("Y");
       result.add( car );            /* '\u00DD'   Ý   alt-0221  */
       car = new java.lang.String("Þ");
       result.add( car );            /* '\u00DE'   Þ   alt-0222  */
       car = new java.lang.String("B");
       result.add( car );            /* '\u00DF'   ß   alt-0223  */
       car = new java.lang.String("a");
       result.add( car );            /* '\u00E0'   à   alt-0224  */
       result.add( car );            /* '\u00E1'   á   alt-0225  */
       result.add( car );            /* '\u00E2'   â   alt-0226  */
       result.add( car );            /* '\u00E3'   ã   alt-0227  */
       result.add( car );            /* '\u00E4'   ä   alt-0228  */
       result.add( car );            /* '\u00E5'   å   alt-0229  */
       car = new java.lang.String("ae");
       result.add( car );            /* '\u00E6'   æ   alt-0230  */
       car = new java.lang.String("c");
       result.add( car );            /* '\u00E7'   ç   alt-0231  */
       car = new java.lang.String("e");
       result.add( car );            /* '\u00E8'   è   alt-0232  */
       result.add( car );            /* '\u00E9'   é   alt-0233  */
       result.add( car );            /* '\u00EA'   ê   alt-0234  */
       result.add( car );            /* '\u00EB'   ë   alt-0235  */
       car = new java.lang.String("i");
       result.add( car );            /* '\u00EC'   ì   alt-0236  */
       result.add( car );            /* '\u00ED'   í   alt-0237  */
       result.add( car );            /* '\u00EE'   î   alt-0238  */
       result.add( car );            /* '\u00EF'   ï   alt-0239  */
       car = new java.lang.String("d");
       result.add( car );            /* '\u00F0'   ð   alt-0240  */
       car = new java.lang.String("n");
       result.add( car );            /* '\u00F1'   ñ   alt-0241  */
       car = new java.lang.String("o");
       result.add( car );            /* '\u00F2'   ò   alt-0242  */
       result.add( car );            /* '\u00F3'   ó   alt-0243  */
       result.add( car );            /* '\u00F4'   ô   alt-0244  */
       result.add( car );            /* '\u00F5'   õ   alt-0245  */
       result.add( car );            /* '\u00F6'   ö   alt-0246  */
       car = new java.lang.String("/");
       result.add( car );            /* '\u00F7'   ÷   alt-0247  */
       car = new java.lang.String("0");
       result.add( car );            /* '\u00F8'   ø   alt-0248  */
       car = new java.lang.String("u");
       result.add( car );            /* '\u00F9'   ù   alt-0249  */
       result.add( car );            /* '\u00FA'   ú   alt-0250  */
       result.add( car );            /* '\u00FB'   û   alt-0251  */
       result.add( car );            /* '\u00FC'   ü   alt-0252  */
       car = new java.lang.String("y");
       result.add( car );            /* '\u00FD'   ý   alt-0253  */
       car = new java.lang.String("þ");
       result.add( car );            /* '\u00FE'   þ   alt-0254  */
       car = new java.lang.String("y");
       result.add( car );            /* '\u00FF'   ÿ   alt-0255  */
       result.add( car );            /* '\u00FF'       alt-0255  */
      
       return result;
    }

    /** Transforme une chaine pouvant contenir des accents dans une version sans accent
     *  @param chaine Chaine a convertir sans accent
     *  @return Chaine dont les accents ont été supprimé
     **/
    public static String sansAccent(String chaine)
    {  StringBuffer res  = new StringBuffer(chaine);
      
       for(int bcl = 0 ; bcl < res.length() ; bcl++)
       {   int carVal = chaine.charAt(bcl);
           if( carVal >= MIN && carVal <= MAX )
           {  // Remplacement
              java.lang.String newVal = (java.lang.String)map.get( carVal - MIN );
              res.replace(bcl, bcl+1,newVal);
           }   
       }
       return res.toString();
   }
	
	
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
	 * This method must be called to configure XitiTag before any other call.
	 * @param context application or activity context
	 * @param resource id of subdomain xiti subdomain. Identifies the server to call for Xiti stats. e.g: http://subd1.xiti.com , subdomain should be "subd1"
	 * @param resource id of siteId id of the site
	 * @param resource id of subsiteId id of the subsite.
	 */
	public static XitiTag init(Context context, int subdomainResId, int siteIdResId, int subsiteIdResId) {
		
		return init(context, 
				context.getResources().getString(subdomainResId),
				context.getResources().getString(siteIdResId),
				context.getResources().getString(subsiteIdResId));
		
	}
	
	/** 
	 * This method must be called to configure XitiTag before any other call.
	 * @param context application or activity context
	 * @param resource id of subdomain xiti subdomain. Identifies the server to call for Xiti stats. e.g: http://subd1.xiti.com , subdomain should be "subd1"
	 * @param resource id of siteId id of the site
	 */
	public static XitiTag init(Context context, int subdomainResId, int siteIdResId) {
			
			return init(context, 
					context.getResources().getString(subdomainResId),
					context.getResources().getString(siteIdResId),
					null);
			
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
		
		instance.launchRequest("p='"+page+"'");
		
	}
	
	/**
	 * Call this method to tag a page with a specific subsiteId
	 * @param page name of the page to tag. 'page' will appear in your xiti tag page.
	 * @param subsiteId id of the subsite to use
	 */
	public static void tagPage(String page, String subsiteId) 
	{
		if ( instance == null ) {
			throw new IllegalStateException("Xiti tag must be initialized before use.");
		}
		
		instance.launchRequest("p='"+page+"'", subsiteId);
	}
	
	/**
	 * Call this method to tag an action.
	 * @param action name of the action to tag. 'action' will appear in your xiti tag page.
	 */
	public static void tagAction(String action, XitiTagActionType actionType) {
		
		if ( instance == null ) {
			throw new IllegalStateException("Xiti tag must be initialized before use.");
		}
		
		instance.launchRequest("p='"+action+"'&clic='"+stringForActionType(actionType)+"'");
	}
	
	/**
	 * Call this method to tag an action.
	 * @param action name of the action to tag. 'action' will appear in your xiti tag page.
	 * @param subsiteId id of the subsite to use
	 */
	public static void tagAction(String action, XitiTagActionType actionType, String subsiteId) {
		
		if ( instance == null ) {
			throw new IllegalStateException("Xiti tag must be initialized before use.");
		}
		
		instance.launchRequest("p='"+action+"'&clic='"+stringForActionType(actionType)+"'", subsiteId);
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
		
		operationQueue.offer(new XitiTagOperation(buildUrl(toAppend, this.subsiteId)));
		
	}
	
	/**
	 * put the request into the operationQueue
	 * @param toAppend additional parameters
	 */
	private void launchRequest(String toAppend, String subsiteId) {
		
		operationQueue.offer(new XitiTagOperation(buildUrl(toAppend, subsiteId)));
	}
	
	/**
	 * Build the url with the given xiti parameters
	 * @param toAppend other parameters to append
	 * @return the url ready to be requested at Xiti servers
	 */
	private String buildUrl(String toAppend, String subsiteId) {
		
		//TODO some part of the url could be built once
		
		StringBuffer buf = new StringBuffer("http://");
		
		buf.append(subdomain)
		   .append(".xiti.com/hit.xiti?s=").append(this.siteId)
		   .append("&").append(toAppend);
		
		if ( subsiteId != null ) {
			buf.append("&s2=").append(subsiteId);
		}
		
		for (String key : phoneInfo.keySet()) {
			if ( phoneInfo.get(key) != null ) {
				buf.append("&").append(key).append("=").append(URLEncoder.encode(phoneInfo.get(key)));
			}
		}
		
		buf.append("&na=").append(System.currentTimeMillis());
		
		Log.d(LOG_TAG, "XitiTag.BuildUrl returns : "+buf.toString());
		
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
				
			} catch (Throwable t) {} 
			
		}
	}
	
	/**
	 * Do the http request
	 * @param url url of the request
	 */
	private static void doRequest(String url) {
		
		try {
			final HttpClient httpClient  = new DefaultHttpClient();
			final HttpGet httpGet = new HttpGet(url);
			httpClient.execute(httpGet);
		} catch (Throwable t) {
			Log.d(LOG_TAG, "XitiTag.doRequest("+url+") failed ! ", t);
		}
			

	}
	
	public static String escapePageName(String dynamicPageName) {
		
		return sansAccent(dynamicPageName
				.replace(" ", "")
				.replace(" ", "")
				.replace("-", "")
				.replace("Â ", "")
				.replace("'", "")
				.replace("â€™", "")
				.replace("’", "")
				.replace("?", ""));

	}
}
