package com.mega.sdk;

public class MegaApiSWT extends MegaApiJava
{
	MegaApiSWT(String path)
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
		Display.getDefault().asyncExec(runnable);	
	}
}
