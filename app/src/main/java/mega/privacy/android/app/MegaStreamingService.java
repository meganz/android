package mega.privacy.android.app;

import java.io.IOException;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaTransfer;

public class MegaStreamingService extends Service implements Runnable {

	public static String ACTION_OVERQUOTA_ERROR = "OVERQUOTA_ERROR";

	MegaApplication application;
    Handler guiHandler;
	Thread thread;
    boolean started = false;
    MegaProxyServer proxyServer;

	private NotificationCompat.Builder mBuilderCompat;
	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;
	DatabaseHandler dbH = null;
    
	 @Override
	 public int onStartCommand(Intent intent, int flags, int startId) {

		 if(intent!=null){
			 if (intent.getAction() != null){
				 if (intent.getAction().equals(ACTION_OVERQUOTA_ERROR)){
					 log("Overquota intent");
					 showOverquotaNotification();
					 return START_NOT_STICKY;
				 }
			 }
		 }

		  if(started)
			  return START_STICKY;

		  started = true;
		  application = (MegaApplication)getApplication();
		  guiHandler = new Handler();
		  thread = new Thread(this);
		  thread.start();
		  return START_STICKY;
	 }

	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate");

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			mBuilder = new Notification.Builder(MegaStreamingService.this);
		}
		mBuilderCompat = new NotificationCompat.Builder(getApplicationContext());

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}


	public void run() 
	{
		try {
			proxyServer = new MegaProxyServer(4443, application.getMegaApi(), application.getMegaApiFolder(), application, guiHandler, application.getApplicationContext());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}	
	}

	private void showOverquotaNotification(){
		log("showOverquotaNotification");

		Intent intent;
		PendingIntent pendingIntent = null;

		String info = "Streaming";
		Notification notification = null;

		String contentText = getString(R.string.download_show_info);
		String message = getString(R.string.title_depleted_transfer_overquota);

		MegaApiAndroid megaApi = application.getMegaApi();

		if(megaApi!=null){
			if(megaApi.isLoggedIn()==0 || dbH.getCredentials()==null){
				log("Intent to login");
				dbH.clearEphemeral();
				intent = new Intent(MegaStreamingService.this, LoginActivityLollipop.class);
				intent.setAction(Constants.ACTION_OVERQUOTA_TRANSFER);
				pendingIntent = PendingIntent.getActivity(MegaStreamingService.this, 0, intent, 0);
			}
			else{
				log("Intent to Manager");
				intent = new Intent(MegaStreamingService.this, ManagerActivityLollipop.class);
				intent.setAction(Constants.ACTION_OVERQUOTA_TRANSFER);
				pendingIntent = PendingIntent.getActivity(MegaStreamingService.this, 0, intent, 0);
			}
		}

		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_notify_download)
					.setOngoing(false).setContentTitle(message).setSubText(info)
					.setContentText(contentText)
					.setOnlyAlertOnce(true);

			if(pendingIntent!=null){
				mBuilder.setContentIntent(pendingIntent);
			}
			notification = mBuilder.build();
		}
		else if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_notify_download)
					.setOngoing(false).setContentTitle(message).setContentInfo(info)
					.setContentText(contentText)
					.setOnlyAlertOnce(true);

			if(pendingIntent!=null){
				mBuilder.setContentIntent(pendingIntent);
			}
			notification = mBuilder.getNotification();
		}
		else
		{
			notification = new Notification(R.drawable.ic_stat_notify_download, null, 1);
			notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
			if(pendingIntent!=null){
				notification.contentIntent = pendingIntent;
			}
			notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_notify_download);
			notification.contentView.setTextViewText(R.id.status_text, message);
			notification.contentView.setTextViewText(R.id.progress_text, info);
		}

		mNotificationManager.notify(Constants.NOTIFICATION_STREAMING_OVERQUOTA, notification);
	}


	public static void log(String log){
        Util.log("MegaStreamingService", log);
    }

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}
