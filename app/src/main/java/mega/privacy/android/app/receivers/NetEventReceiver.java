package mega.privacy.android.app.receivers;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.utils.Util;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;


public class NetEventReceiver extends BroadcastReceiver {
	
	Handler handler = new Handler();
	
	public NetEventReceiver() {}

	@Override
	public void onReceive(Context context, Intent intent){
//		Cursor cursor = context.getContentResolver().query(intent.getData(), null,null, null, null);
//	    cursor.moveToFirst();
//	    String image_path = cursor.getString(cursor.getColumnIndex("_data"));
//	    log("New Photo is Saved as : -" + image_path);
	    
		log("NetEventReceiver");
		final Context c = context;
		
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
		MegaAttributes attr = dbH.getAttributes();
		
		if (attr != null){
			if (attr.getOnline() != null){
				if (Boolean.parseBoolean(attr.getOnline())){
					if (!Util.isOnline(context)){
//						Toast.makeText(context, "REFRESCAR, porque antes era online y ahora offline", Toast.LENGTH_LONG).show();
					}
				}
				else{
					if (Util.isOnline(context)){
//						Toast.makeText(context, "REFRESCAR, porque antes era offline y ahora online", Toast.LENGTH_LONG).show();
					}
				}
			}
		}
		
		if (Util.isOnline(context)){
			dbH.setAttrOnline(true);
		}
		else{
			dbH.setAttrOnline(false);
		}
		
		String ipAddress = getLocalIpAddress();
		
		log("IPADDRESS: " + ipAddress);
		
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				log("Now I start the service");
				c.startService(new Intent(c, CameraSyncService.class));		
			}
		}, 2 * 1000);
	}
	
	public String getLocalIpAddress(){
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} 
		catch (Exception ex) {
			log(ex.toString());
		}
		
		return null;
	}
	
	public static void log(String message) {
		Util.log("NetEventReceiver", message);
	}
}