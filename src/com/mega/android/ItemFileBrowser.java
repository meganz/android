package com.mega.android;

public class ItemFileBrowser {
	
	private int imageId;
	private String name;
	
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
}
