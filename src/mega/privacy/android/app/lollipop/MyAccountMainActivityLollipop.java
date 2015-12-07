package mega.privacy.android.app.lollipop;

import java.util.BitSet;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.FrameLayout;


public class MyAccountMainActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface {

    FrameLayout fragmentContainer;
    
	MegaApiAndroid megaApi;

	MyAccountFragmentLollipop maF;
	UpgradeAccountFragmentLollipop upAF;
	
	public static final int MY_ACCOUNT_FRAGMENT = 1000;
	public static final int UPGRADE_ACCOUNT_FRAGMENT = 1001;
	public static final int PAYMENT_FRAGMENT = 1002;

	static MyAccountMainActivityLollipop myAccountMainActivityLollipop;

	BitSet paymentBitSet = null;
	int currentFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		myAccountMainActivityLollipop=this;

		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		setContentView(R.layout.activity_main_myaccount);
		
		fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_myaccount);
		
		currentFragment = MY_ACCOUNT_FRAGMENT;
		selectMyAccountFragment(currentFragment);
	}

	public void selectMyAccountFragment(int currentFragment){
		switch(currentFragment){
			case MY_ACCOUNT_FRAGMENT:{
				if (maF == null){
					maF = new MyAccountFragmentLollipop();
				}
				maF.setMyEmail(megaApi.getMyEmail());
				
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_myaccount, maF, "maF").commit();
				
				break;
			}
			case UPGRADE_ACCOUNT_FRAGMENT:{
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				if(upAF == null){
					upAF = new UpgradeAccountFragmentLollipop();
					upAF.setInfo(paymentBitSet);
					ft.replace(R.id.fragment_container_myaccount, upAF, "upAF");
					ft.commit();
				}
				else{
					upAF.setInfo(paymentBitSet);
					ft.replace(R.id.fragment_container_myaccount, upAF, "upAF");
					ft.commit();
				}
				
				break;
			}
		}
		/*switch(currentFragment){
			case MY_ACCOUNT_FRAGMENT:{
				if (maF == null){
					maF = new MyAccountFragmentLollipop();
				}
//				maF.setUserEmail(userEmail);
//				maF.setToolbar(contactPropertiesImage, initialLetter, collapsingToolbarLayout);
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_myaccount, maF, "maF").commit();
	
				break;
			}
			case UPGRADE_ACCOUNT_FRAGMENT:{
				if (cflF == null){
					cflF = new ContactFileListFragmentLollipop();
				}
				cflF.setUserEmail(userEmail);
	
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_contact_properties, cflF, "cflF").commit();
	
				break;
			}
			case PAYMENT_FRAGMENT:{
				break;
			}
		}*/
	}
	
	public void showUpAF(BitSet paymentBitSet){
		
		if (paymentBitSet == null){
			if (this.paymentBitSet != null){
				paymentBitSet = this.paymentBitSet;
			}
		}
		
		currentFragment = UPGRADE_ACCOUNT_FRAGMENT;
		selectMyAccountFragment(currentFragment);
	}
	
	@SuppressLint("NewApi") 
	void showAlert(String message, String title) {
		AlertDialog.Builder bld;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {	
			bld = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		}
		else{
			bld = new AlertDialog.Builder(this);
		}
        bld.setMessage(message);
        bld.setTitle(title);
//        bld.setNeutralButton("OK", null);
        bld.setPositiveButton("OK",null);
        log("Showing alert dialog: " + message);
        bld.create().show();
    }

	/*@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		log("onPrepareOptionsMenu----------------------------------");

		if(maF != null){
			if(maF.isVisible()){
				logoutFromAllDevicesMenuItem.setVisible(true);
				changePasswordMenuItem.setVisible(true);
				helpMenuItem.setVisible(true);
				upgradeAccountMenuItem.setVisible(true);
				logoutMenuItem.setVisible(true);
				
				String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
    			log("Export in: "+path);
    			File file= new File(path);
    			if(file.exists()){
    				exportMasterKeyMenuItem.setVisible(false); 
	    			removeMasterKeyMenuItem.setVisible(true); 
    			}
    			else{
    				exportMasterKeyMenuItem.setVisible(true); 
	    			removeMasterKeyMenuItem.setVisible(false); 		
    			}
			}
		}

		return super.onPrepareOptionsMenu(menu);

	}*/

	@Override
	public void onBackPressed() {
		/*if (cflF != null){
			if (cflF.isVisible()){
				if (cflF.onBackPressed() == 0){
					selectContactFragment(CONTACT_PROPERTIES);
					return;
				}
			}
		}

		if (cpF != null){
			if (cpF.isVisible()){
				super.onBackPressed();
				return;
			}
		}*/
		
		super.onBackPressed();
	}

	public static void log(String log) {
		Util.log("MyAccountMainActivity", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: " + request.getRequestString());		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		
		if (request.getType() == MegaRequest.TYPE_KILL_SESSION){
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.success_kill_all_sessions), Snackbar.LENGTH_LONG).show();
			}
			else
			{
				log("error when killing sessions: "+e.getErrorString());
				Snackbar.make(fragmentContainer, getString(R.string.error_kill_all_sessions), Snackbar.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}
}
