package com.mega.android.authenticator;

import com.mega.android.Util;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MegaAuthenticatorService extends Service{

	@Override
	public IBinder onBind(Intent intent) {
		
		MegaAuthenticator authenticator = new MegaAuthenticator(this);
		return authenticator.getIBinder();
	}

	public static void log(String log){
		Util.log("MegaAuthenticatorService", log);
	}
}
