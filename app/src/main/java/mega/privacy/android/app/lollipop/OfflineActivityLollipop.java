package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.modalbottomsheet.OfflineOptionsBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;


public class OfflineActivityLollipop extends PinActivityLollipop{
	
	OfflineFragmentLollipop oFLol;
    Toolbar tB;
    ActionBar aB;

	boolean isListOffline = true;
	private MenuItem thumbViewMenuItem;
	String pathNavigation = "/";
	MegaOffline selectedNode;

	static OfflineActivityLollipop offlineActivity = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);

		offlineActivity = this;

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
			oFLol.setIsList(isListOffline);
		}
		else{
			oFLol.setPathNavigation(pathNavigation);
			oFLol.setIsList(isListOffline);
		}

		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_offline, oFLol, "oFLol").commitNow();
	}

	public void showOptionsPanel(MegaOffline sNode){
		log("showNodeOptionsPanel-Offline");
		if(sNode!=null){
			this.selectedNode = sNode;
			OfflineOptionsBottomSheetDialogFragment bottomSheetDialogFragment = new OfflineOptionsBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
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
					fragTransaction.commitNow();

					oFLol.setIsList(isListOffline);						
					oFLol.setPathNavigation(pathNavigation);
					//oFLol.setGridNavigation(false);
					//oFLol.setParentHandle(parentHandleSharedWithMe);

					fragTransaction = getSupportFragmentManager().beginTransaction();
					fragTransaction.attach(currentFragment);
					fragTransaction.commitNow();
					
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

	public void updateOfflineView(MegaOffline mOff){
		log("updateOfflineView");
		if(oFLol!=null){
			if(mOff==null){
				oFLol.refresh();
			}
			else{
				oFLol.refreshPaths(mOff);
			}
		}
	}

	public void showConfirmationRemoveFromOffline(){
		log("showConfirmationRemoveFromOffline");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						String pathNavigation = getPathNavigation();
						MegaOffline mOff = getSelectedNode();
						NodeController nC = new NodeController(offlineActivity);
						nC.deleteOffline(mOff, pathNavigation);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE: {
						//No button clicked
						break;
					}
				}
			}
		};

		AlertDialog.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		}
		else{
			builder = new AlertDialog.Builder(this);
		}
		builder.setMessage(R.string.confirmation_delete_from_save_for_offline).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}
	
	public void setPathNavigationOffline(String pathNavigation){
		this.pathNavigation = pathNavigation;
	}

	public String getPathNavigation() {
		return pathNavigation;
	}

	public MegaOffline getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(MegaOffline selectedNode) {
		this.selectedNode = selectedNode;
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
