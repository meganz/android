package com.mega.sdk;

import android.os.Handler;
import android.os.Looper;

public class MegaApiAndroid extends MegaApiJava
{
	static Handler handler = new Handler(Looper.getMainLooper());
	
	public MegaApiAndroid(String path)
	{
		super(path);
	}

	@Override
	boolean isRunCallbackThreaded()
	{
		return true;
	}
	
	@Override
	void runCallback(Runnable runnable)
	{
		handler.post(runnable);
	}
}
