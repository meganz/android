package com.mega.sdk;

public class MegaApiSWT extends MegaApiJava
{
	MegaApiSWT(String path)
	{
		super(path);
	}
	
	@Override
	void runCallback(Runnable runnable)
	{
		Display.getDefault().asyncExec(runnable);	
	}
}
