package com.mega.sdk;

public class MegaApiSwing extends MegaApiJava
{
	MegaApiSwing(String path)
	{
		super(path);
	}
	
	@Override
	void runCallback(Runnable runnable)
	{
		SwingUtilities.invokeLater(runnable);		
	}
}
