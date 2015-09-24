package nz.mega.android.lollipop;

import nz.mega.android.PinActivity;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import android.content.Intent;
import android.os.Bundle;


public class OfflineActivityLollipop extends PinActivityLollipop{
	
	OfflineFragmentLollipop oFLol;
	
	boolean isListOffline = true;

	String pathNavigation = "/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);

		if (Util.isOnline(this)){
			Intent onlineIntent = new Intent(this, ManagerActivityLollipop.class);
			startActivity(onlineIntent);
			finish();
        	return;
		}		
		
		setContentView(R.layout.activity_offline);
		
		if (savedInstanceState != null){
			this.pathNavigation = "/";
		}
		
		if (oFLol == null){
			oFLol = new OfflineFragmentLollipop();
			oFLol.setIsList(isListOffline);
			oFLol.setPathNavigation(pathNavigation);
		}
		else{
			oFLol.setIsList(isListOffline);
		}
		
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_offline, oFLol, "oFLol").commit();
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
		
		if (oFLol != null){
			if (oFLol.isVisible()){
				if (oFLol.onBackPressed() == 0){
					if (Util.isOnline(this)){
						Intent onlineIntent = new Intent(this, ManagerActivityLollipop.class);
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
		Util.log("OfflineActivityLollipop", message);	
	}
}
