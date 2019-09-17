package mega.privacy.android.app.receivers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import static mega.privacy.android.app.utils.LogUtil.*;

/*
*  A simple Broadcast Receiver to receive an INSTALL_REFERRER
*  intent and pass it to other receivers, including
*  the Google Analytics receiver.
*/
public class CustomInstallReferrerReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

        logDebug("onReceive()");
		try {
            final Bundle extras = intent.getExtras();
            if (extras != null) {
                extras.containsKey(null);
            }
        }
        catch (final Exception e) {
            return;
        }
		
		Map<String, String> referralParams = new HashMap<String, String>();
		
		// Return if this is not the right intent.
        if (! intent.getAction().equals("com.android.vending.INSTALL_REFERRER")) { //$NON-NLS-1$
            return;
        }
        
        String referrer = intent.getStringExtra("referrer"); //$NON-NLS-1$
        if( referrer == null || referrer.length() == 0) {
            return;
        }

        try
        {    // Remove any url encoding
        	referrer = URLDecoder.decode(referrer, "UTF-8");
        }
        catch (UnsupportedEncodingException e) { return; }

        // Parse the query string, extracting the relevant data
        String[] params = referrer.split("&"); // $NON-NLS-1$
        for (String param : params)
        {
            String[] pair = param.split("="); // $NON-NLS-1$
            if (pair.length == 1){
            	referralParams.put(pair[0], "");	
            }
            else if (pair.length == 2){
            	referralParams.put(pair[0], pair[1]);
            }
            else if (pair.length > 2){
            	referralParams.put(pair[0], pair[1]);
            }
        }

//        CustomInstallReferrerReceiver.storeReferralParams(context, referralParams);
        
        for(String key : CustomInstallReferrerReceiver.EXPECTED_PARAMETERS)
        {
            String value = referralParams.get(key);
            if(value != null)
            {
                logDebug("KEY: "  + key + "; VALUE: " + value);
            	Log.d("MEGAInstallReferrerReceiver", key + " = " + value);
            }
        }

		// 	Pass the intent to other receivers.

		// When you're done, pass the intent to the Google Analytics receiver.
//		new CampaignTrackingReceiver().onReceive(context, intent);
	}
	
	private final static String[] EXPECTED_PARAMETERS = {
        "utm_source",
        "utm_medium",
        "utm_term",
        "utm_content",
        "utm_campaign"
    };
	
	private final static String PREFS_FILE_NAME = "ReferralParamsFile";
	
	public static void storeReferralParams(Context context, Map<String, String> params)
    {
        SharedPreferences storage = context.getSharedPreferences(CustomInstallReferrerReceiver.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = storage.edit();

        for(String key : CustomInstallReferrerReceiver.EXPECTED_PARAMETERS)
        {
            String value = params.get(key);
            if(value != null)
            {
                editor.putString(key, value);
            }
        }

        editor.commit();
    }
	
	public static Map<String, String> retrieveReferralParams(Context context)
    {
        HashMap<String, String> params = new HashMap<String, String>();
        SharedPreferences storage = context.getSharedPreferences(CustomInstallReferrerReceiver.PREFS_FILE_NAME, Context.MODE_PRIVATE);

        for(String key : CustomInstallReferrerReceiver.EXPECTED_PARAMETERS)
        {
            String value = storage.getString(key, null);
            if(value != null)
            {
                params.put(key, value);
            }
        }
        return params;
    }
}