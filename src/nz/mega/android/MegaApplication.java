package nz.mega.android;

import java.util.HashMap;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger.LogLevel;
import com.google.android.gms.analytics.Tracker;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;


public class MegaApplication extends Application
{
	final String TAG = "MegaApplication";
	static final String APP_KEY = "U5NE3TxD";
	static final String USER_AGENT = "MEGA Android/2.0 BETA";
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	String localIpAddress = "";
	
//	static final String GA_PROPERTY_ID = "UA-59254318-1";
//	
//	/**
//	 * Enum used to identify the tracker that needs to be used for tracking.
//	 *
//	 * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
//	 * storing them all in Application object helps ensure that they are created only once per
//	 * application instance.
//	 */
//	public enum TrackerName {
//	  APP_TRACKER/*, // Tracker used only in this app.
//	  GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
//	  ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
//	  */
//	}
//
//	HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		MegaApiAndroid.setLoggerObject(new AndroidLogger());
		MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_INFO);
		
//		initializeGA();
		
//		new MegaTest(getMegaApi()).start();
	}
	
//	private void initializeGA(){
//		// Set the log level to verbose.
//		GoogleAnalytics.getInstance(this).getLogger().setLogLevel(LogLevel.VERBOSE);
//	}
	
	public MegaApiAndroid getMegaApiFolder(){
		if (megaApiFolder == null){
			PackageManager m = getPackageManager();
			String s = getPackageName();
			PackageInfo p;
			String path = null;
			try
			{
				p = m.getPackageInfo(s, 0);
				path = p.applicationInfo.dataDir + "/";
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			
			Log.d(TAG, "Database path: " + path);
			megaApiFolder = new MegaApiAndroid(MegaApplication.APP_KEY, 
					MegaApplication.USER_AGENT, path);
		}
		
		return megaApiFolder;
	}
	
	public MegaApiAndroid getMegaApi()
	{
		if(megaApi == null)
		{
			PackageManager m = getPackageManager();
			String s = getPackageName();
			PackageInfo p;
			String path = null;
			try
			{
				p = m.getPackageInfo(s, 0);
				path = p.applicationInfo.dataDir + "/";
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			
			Log.d(TAG, "Database path: " + path);
			megaApi = new MegaApiAndroid(MegaApplication.APP_KEY, 
					MegaApplication.USER_AGENT, path);
		}
		
		return megaApi;
	}
	
//	synchronized Tracker getTracker(TrackerName trackerId) {
//		if (!mTrackers.containsKey(trackerId)) {
//
//			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
//			Tracker t = null;
//			if (trackerId == TrackerName.APP_TRACKER){
//				t = analytics.newTracker(GA_PROPERTY_ID);
//			}
////			Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(PROPERTY_ID)
////					: (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.global_tracker)
////							: analytics.newTracker(R.xml.ecommerce_tracker);
//					mTrackers.put(trackerId, t);
//					
//		}
//	
//		return mTrackers.get(trackerId);
//	}

	
	public String getLocalIpAddress(){
		return localIpAddress;
	}
	
	public void setLocalIpAddress(String ip){
		localIpAddress = ip;
	}
	
	public static void log(String message) {
		Util.log("MegaApplication", message);
	}
}
