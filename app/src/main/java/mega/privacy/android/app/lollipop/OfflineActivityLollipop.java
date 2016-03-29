package mega.privacy.android.app.lollipop;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.PinActivity;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class OfflineActivityLollipop extends PinActivityLollipop{
	
	OfflineFragmentLollipop oFLol;
    Toolbar tB;
    ActionBar aB;
	
	boolean isListOffline = true;
	private MenuItem thumbViewMenuItem;
	String pathNavigation = "/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);

		if (Util.isOnline(this)){
			log("Network then intent to ManagerActivityLollipop");
			Intent onlineIntent = new Intent(this, ManagerActivityLollipop.class);
			startActivity(onlineIntent);
			finish();
        	return;
		}		
		
		setContentView(R.layout.activity_offline);
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_activity_offline);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		log("aB.setHomeAsUpIndicator_29");
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
		
		if (savedInstanceState != null){
			this.pathNavigation = "/";
		}
		
		if (oFLol == null){
			oFLol = new OfflineFragmentLollipop();			
			oFLol.setPathNavigation(pathNavigation);
		}

//		isListOffline = oFLol.getIsList();
//		log("IsListOffline: "+isListOffline);
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_offline, oFLol, "oFLol").commit();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.offline_activity_action, menu);
	    getSupportActionBar().setDisplayShowCustomEnabled(true);
	    
	    thumbViewMenuItem= menu.findItem(R.id.action_grid);
	    thumbViewMenuItem.setVisible(true);
	    
		if (isListOffline){	
			thumbViewMenuItem.setTitle(getString(R.string.action_grid));
		}
		else{
			thumbViewMenuItem.setTitle(getString(R.string.action_list));
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
    	log("onSaveInstaceState");
    	super.onSaveInstanceState(outState);
    	
    	String pathOffline = this.pathNavigation;
    	
    	outState.putString("pathOffline", pathOffline);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");
		int id = item.getItemId();
		switch(id){
			case android.R.id.home:{
				onBackPressed();
				break;
			}
			case R.id.action_grid:{	
		    	log("action_grid selected");
		    	isListOffline = !isListOffline;
		    	oFLol.setIsListDB(isListOffline);
				if (isListOffline){	
					thumbViewMenuItem.setTitle(getString(R.string.action_grid));
				}
				else{
					thumbViewMenuItem.setTitle(getString(R.string.action_list));
				}
				if (oFLol != null){        			
					Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("oFLol");
					FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
					fragTransaction.detach(currentFragment);
					fragTransaction.commit();

					oFLol.setIsList(isListOffline);						
					oFLol.setPathNavigation(pathNavigation);
					//oFLol.setGridNavigation(false);
					//oFLol.setParentHandle(parentHandleSharedWithMe);

					fragTransaction = getSupportFragmentManager().beginTransaction();
					fragTransaction.attach(currentFragment);
					fragTransaction.commit();
					
				}
				break;
			}
		}
		return true;
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
				else{
					
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

	public boolean isListOffline() {
		return isListOffline;
	}

	public void setListOffline(boolean isListOffline) {
		this.isListOffline = isListOffline;
	}
}
