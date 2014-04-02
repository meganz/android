package com.mega.android;

public class ItemFileBrowser {

	private long nodeHandle;

	//Esto lo tengo que quitar
	private int imageId;
	private String name;
	//HASTA AQUI

	public ItemFileBrowser(long _nodeHandle){
		nodeHandle = _nodeHandle;
	}

	public long getNodeHandle(){
		return nodeHandle;
	}

	public void setNodeHandle(long _nodeHandle){
		nodeHandle = _nodeHandle;
	}

	//Esto lo tengo que quitar
	public ItemFileBrowser(int _imageId, String _name){
		imageId = _imageId;
		name = _name;
	}

	public int getImageId(){
		return imageId;
	}

	public void setImageId(int _imageId){
		imageId = _imageId;
	}

	public String getName() {
		return name;
	}

	public void setName(String _name) {
		name = _name;
	}
	//HASTA AQUI
}