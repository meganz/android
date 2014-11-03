package com.mega.sdk;

class DelegateMegaLogger extends MegaLogger
{
	MegaLoggerInterface listener;

	DelegateMegaLogger(MegaLoggerInterface listener)
	{
		this.listener = listener;
	}
	
	public void log(String time, int loglevel, String source, String message)
	{
		if(listener != null)
		{
			listener.log(time, loglevel, source, message);
		}
	}
}
