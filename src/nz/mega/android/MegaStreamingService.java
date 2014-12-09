package nz.mega.android;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

public class MegaStreamingService extends Service implements Runnable {

	MegaApplication application;
    Handler guiHandler;
	Thread thread;
    boolean started = false;
    MegaProxyServer proxyServer;
    
	 @Override
     public int onStartCommand(Intent intent, int flags, int startId) {
          if(started)
        	  return START_STICKY;
          
          started = true;
	      application = (MegaApplication)getApplication();          
          guiHandler = new Handler();
          thread = new Thread(this);
          thread.start();
          return START_STICKY;
       }


	public void run() 
	{
		try {
			proxyServer = new MegaProxyServer(4443, application.getMegaApi(), application.getMegaApiFolder(), application, guiHandler);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}	
	}


	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}
