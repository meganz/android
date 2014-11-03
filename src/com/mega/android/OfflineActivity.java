package com.mega.android;

import android.content.Intent;
import android.os.Bundle;

import com.mega.android.utils.Util;

public class OfflineActivity extends PinActivity{
	
	OfflineFragment oF;
	
	boolean isListOffline = true;

	String pathNavigation = "/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		
		if (Util.isOnline(this)){
			Intent onlineIntent = new Intent(this, ManagerActivity.class);
			startActivity(onlineIntent);
			finish();
        	return;
		}
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_offline);
		
		if (savedInstanceState != null){
			this.pathNavigation = "/";
		}
		
		if (oF == null){
			oF = new OfflineFragment();
			oF.setIsList(isListOffline);
			oF.setPathNavigation(pathNavigation);
		}
		else{
			oF.setIsList(isListOffline);
		}
		
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, oF, "oF").commit();
//		if (isListOffline){					
//			customListGrid.setImageResource(R.drawable.ic_menu_gridview);
//		}
//		else{
//			customListGrid.setImageResource(R.drawable.ic_menu_listview);
//		}
//		    			
//		customListGrid.setVisibility(View.VISIBLE);
//		customSearch.setVisibility(View.VISIBLE);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
    	log("onSaveInstaceState");
    	super.onSaveInstanceState(outState);
    	
    	String pathOffline = this.pathNavigation;
    	
    	outState.putString("pathOffline", pathOffline);
	}
	
	@Override
	public void onBackPressed() {
		log("onBackPressed");
		
		if (oF != null){
			if (oF.isVisible()){
				if (oF.onBackPressed() == 0){
					if (Util.isOnline(this)){
						Intent onlineIntent = new Intent(this, ManagerActivity.class);
						startActivity(onlineIntent);
						finish();
						return;
					}
					else{
						super.onBackPressed();
					}
				}
			}
		}
	}
	
	public void setPathNavigationOffline(String pathNavigation){
		this.pathNavigation = pathNavigation;
	}
	
	public static void log(String message) {
		Util.log("OfflineActivity", message);	
	}
}
