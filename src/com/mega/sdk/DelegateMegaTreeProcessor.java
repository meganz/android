package com.mega.sdk;

public class DelegateMegaTreeProcessor extends MegaTreeProcessor
{
	MegaApiJava megaApi;
	MegaTreeProcessorInterface listener;

	DelegateMegaTreeProcessor(MegaApiJava megaApi, MegaTreeProcessorInterface listener)
	{
		this.megaApi = megaApi;
		this.listener = listener;
	}
	
	public boolean processMegaNode(MegaNode node)
	{
		if(listener != null) 
			return listener.processMegaNode(megaApi, node);
		return false;
	}
}
