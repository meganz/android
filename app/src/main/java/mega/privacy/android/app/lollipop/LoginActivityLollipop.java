package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.RelativeLayout;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.InitialTourFragmentLollipop;
import mega.privacy.android.app.LoginFragmentLollipop;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.OldPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.providers.FileProviderActivity;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;

 
public class LoginActivityLollipop extends Activity{

	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;

	RelativeLayout relativeContainer;

	//Fragments
	InitialTourFragmentLollipop tourFragment;
	LoginFragmentLollipop loginFragment;

	int visibleFragment;

	static LoginActivityLollipop loginActivity;
    private MegaApiAndroid megaApi;

	private long parentHandle = -1;
	Bundle extras = null;
	Uri uriData = null;

	String url = null;
	String action = null;

	DatabaseHandler dbH;

    Handler handler = new Handler();
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);

		loginActivity = this;

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		density  = getResources().getDisplayMetrics().density;

		scaleW = Util.getScaleW(outMetrics, density);
		scaleH = Util.getScaleH(outMetrics, density);

		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();

	    dbH = DatabaseHandler.getDbHandler(getApplicationContext());
	    
	    MegaPreferences prefs = dbH.getPreferences();

		setContentView(R.layout.activity_manager);
		relativeContainer = (RelativeLayout) findViewById(R.id.relative_container_login);

		Intent intentReceived = getIntent();
		if (intentReceived != null){
			visibleFragment = intentReceived.getIntExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
			log("There is an intent! VisibleFragment: "+visibleFragment);

			if(visibleFragment==Constants.LOGIN_FRAGMENT){
				showFragment();
				if(intentReceived.getAction()!=null){
					if (Constants.ACTION_CONFIRM.equals(intentReceived.getAction())) {
						loginFragment.handleConfirmationIntent(intentReceived);
						return;
					}
					else if (Constants.ACTION_CREATE_ACCOUNT_EXISTS.equals(intentReceived.getAction())){
						String message = getString(R.string.error_email_registered);
						Snackbar.make(relativeContainer,message,Snackbar.LENGTH_LONG).show();
						return;
					}
					else if(intentReceived.getAction().equals(Constants.ACTION_RESET_PASS)){
						String link = getIntent().getDataString();
						if(link!=null){
							log("link to resetPass: "+link);
							loginFragment.showDialogInsertMKToChangePass(link);
						}
					}
					else if(intentReceived.getAction().equals(Constants.ACTION_PASS_CHANGED)){
						int result = intentReceived.getIntExtra("RESULT",-20);
						if(result==0){
							log("Show success mesage");
							Util.showAlert(this, getString(R.string.pass_changed_alert), null);
						}
						else if(result==MegaError.API_EARGS){
							log("Incorrect arguments!");
							Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
						}
						else if(result==MegaError.API_EKEY){
							log("Incorrect MK when changing pass");
							Util.showAlert(this, getString(R.string.incorrect_MK), getString(R.string.incorrect_MK_title));
						}
						else{
							log("Error when changing pass - show error message");
							Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
						}
					}
					else if(intentReceived.getAction().equals(Constants.ACTION_PARK_ACCOUNT)){
						String link = getIntent().getDataString();
						if(link!=null){
							log("link to parkAccount: "+link);
							loginFragment.showConfirmationParkAccount(link);
						}
						else{
							log("Error when parking account - show error message");
							Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
						}
					}
				}
				else{
					log("No ACTION");
				}
			}
		}
		else{
			visibleFragment = 1;
		}

		if(visibleFragment==Constants.LOGIN_FRAGMENT){

			loginFragment = new LoginFragmentLollipop();
			UserCredentials credentials = dbH.getCredentials();
			if (credentials != null){
				log("Credentials NOT null");
				if ((intentReceived != null) && (intentReceived.getAction() != null)){
					if (intentReceived.getAction().equals(Constants.ACTION_REFRESH)){
						parentHandle = intentReceived.getLongExtra("PARENT_HANDLE", -1);
						loginFragment.startLoginInProcess();
						return;
					}
					else{
						if(intentReceived.getAction().equals(Constants.ACTION_OPEN_MEGA_FOLDER_LINK)){
							action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK;
							url = intentReceived.getDataString();
						}
						else if(intentReceived.getAction().equals(Constants.ACTION_IMPORT_LINK_FETCH_NODES)){
							action = Constants.ACTION_OPEN_MEGA_LINK;
							url = intentReceived.getDataString();
						}
						else if (intentReceived.getAction().equals(Constants.ACTION_CANCEL_UPLOAD) || intentReceived.getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD) || intentReceived.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)){
							action = intentReceived.getAction();
						}
						else if(intentReceived.getAction().equals(Constants.ACTION_CHANGE_MAIL)){
							log("intent received ACTION_CHANGE_MAIL");
							action = Constants.ACTION_CHANGE_MAIL;
							url = intentReceived.getDataString();
						}
						else if(intentReceived.getAction().equals(Constants.ACTION_CANCEL_ACCOUNT)){
							log("intent received ACTION_CANCEL_ACCOUNT");
							action = Constants.ACTION_CANCEL_ACCOUNT;
							url = intentReceived.getDataString();
						}
//					else if (intentReceived.getAction().equals(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD)){
//						action = ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD;
//						uriData = intentReceived.getData();
//						log("URI: "+uriData);
//						extras = intentReceived.getExtras();
//						url = null;
//						Snackbar.make(scrollView,getString(R.string.login_before_share),Snackbar.LENGTH_LONG).show();
//					}
						else if (intentReceived.getAction().equals(Constants.ACTION_FILE_PROVIDER)){
							action = Constants.ACTION_FILE_PROVIDER;
							uriData = intentReceived.getData();
							extras = intentReceived.getExtras();
							url = null;
						}
						else if (intentReceived.getAction().equals(Constants.ACTION_EXPORT_MASTER_KEY)){
							action = Constants.ACTION_EXPORT_MASTER_KEY;
						}
						else if (intentReceived.getAction().equals(Constants.ACTION_IPC)){
							action = Constants.ACTION_IPC;
						}

						MegaNode rootNode = megaApi.getRootNode();
						if (rootNode != null){
							Intent intent = new Intent(this, ManagerActivityLollipop.class);
							if (action != null){
//							if (action.equals(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD)){
//								intent = new Intent(this, FileExplorerActivityLollipop.class);
//								if(extras != null)
//								{
//									intent.putExtras(extras);
//								}
//								intent.setData(uriData);
//							}
								if (action.equals(Constants.ACTION_FILE_PROVIDER)){
									intent = new Intent(this, FileProviderActivity.class);
									if(extras != null)
									{
										intent.putExtras(extras);
									}
									intent.setData(uriData);
								}
								intent.setAction(action);
								if (url != null){
									intent.setData(Uri.parse(url));
								}
							}
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
							}

							handler.postDelayed(new Runnable() {

								@Override
								public void run() {
									log("Now I start the service");
									startService(new Intent(getApplicationContext(), CameraSyncService.class));
								}
							}, 5 * 60 * 1000);

							this.startActivity(intent);
							this.finish();
							return;
						}
						else{
							loginFragment.startFastLogin();
							return;
						}
					}
				}
				else{
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode != null){

						log("rootNode != null");
						Intent intent = new Intent(this, ManagerActivityLollipop.class);
						if (action != null){
//						if (action.equals(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD)){
//							intent = new Intent(this, FileExplorerActivityLollipop.class);
//							if(extras != null)
//							{
//								intent.putExtras(extras);
//							}
//							intent.setData(uriData);
//						}
							if (action.equals(Constants.ACTION_FILE_PROVIDER)){
								intent = new Intent(this, FileProviderActivity.class);
								if(extras != null)
								{
									intent.putExtras(extras);
								}
								intent.setData(uriData);
							}
							intent.setAction(action);
							if (url != null){
								intent.setData(Uri.parse(url));
							}
						}
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
						}

						prefs = dbH.getPreferences();
						if(prefs!=null)
						{
							if (prefs.getCamSyncEnabled() != null){
								if (Boolean.parseBoolean(prefs.getCamSyncEnabled())){
									log("Enciendo el servicio de la camara");
									handler.postDelayed(new Runnable() {

										@Override
										public void run() {
											log("Now I start the service");
											startService(new Intent(getApplicationContext(), CameraSyncService.class));
										}
									}, 30 * 1000);
								}
							}
						}
						this.startActivity(intent);
						this.finish();
						return;
					}
					else{
						log("rootNode == null");
						loginFragment.startFastLogin();
						return;
					}
				}
			}
			else {
				log("Credentials IS NULL");
				if ((intentReceived != null)) {
					log("INTENT NOT NULL");
					if (intentReceived.getAction() != null) {
						log("ACTION NOT NULL");
						Intent intent;
						if (intentReceived.getAction().equals(Constants.ACTION_FILE_PROVIDER)) {
							intent = new Intent(this, FileProviderActivity.class);
							if (extras != null) {
								intent.putExtras(extras);
							}
							intent.setData(uriData);

							intent.setAction(action);

							action = Constants.ACTION_FILE_PROVIDER;
						} else if (intentReceived.getAction().equals(Constants.ACTION_FILE_EXPLORER_UPLOAD)) {
							action = Constants.ACTION_FILE_EXPLORER_UPLOAD;
							//					uriData = intentReceived.getData();
							//					log("URI: "+uriData);
							//					extras = intentReceived.getExtras();
							//					url = null;
							Snackbar.make(relativeContainer, getString(R.string.login_before_share), Snackbar.LENGTH_LONG).show();
						} else if (intentReceived.getAction().equals(Constants.ACTION_EXPORT_MASTER_KEY)) {
							log("ManagerActivityLollipop.ACTION_EXPORT_MASTER_KEY");
							action = Constants.ACTION_EXPORT_MASTER_KEY;
						}
					}
				}
				if (OldPreferences.getOldCredentials(this) != null) {
					loginFragment.oldCredentialsLogin();
				}
			}
		}
	}

	public void showFragment(){
		log("showFragment");

		Fragment currentFragment = getFragmentManager().findFragmentById(R.id.fragment_container_login);
		if (currentFragment != null){
			getFragmentManager().beginTransaction().remove(currentFragment).commit();
		}

		switch (visibleFragment){
			case Constants.LOGIN_FRAGMENT:{
				log("showLoginFragment");
				if(loginFragment==null){
					loginFragment = new LoginFragmentLollipop();
				}
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, loginFragment, "loginFragment");
				ft.commit();
				break;
			}
		}
	}

	public void stopCameraSyncService(){
		log("stopCameraSyncService");
		dbH.clearPreferences();
		dbH.setFirstTime(false);
//					dbH.setPinLockEnabled(false);
//					dbH.setPinLockCode("");
//					dbH.setCamSyncEnabled(false);
		Intent stopIntent = null;
		stopIntent = new Intent(this, CameraSyncService.class);
		stopIntent.setAction(CameraSyncService.ACTION_LOGOUT);
		startService(stopIntent);
	}

	public void startCameraSyncService(boolean firstTimeCam){
		log("startCameraSyncService");
		Intent intent = null;
		if(firstTimeCam){
			intent = new Intent(this,ManagerActivityLollipop.class);
			intent.putExtra("firstTimeCam", true);
			startActivity(intent);
			finish();
		}
		else{
			log("Enciendo el servicio de la camara");
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					log("Now I start the service");
					startService(new Intent(getApplicationContext(), CameraSyncService.class));
				}
			}, 30 * 1000);
		}
	}

	@Override
	public void onBackPressed() {
		log("onBackPressed");

		int valueReturn = -1;

		switch (visibleFragment){
			case Constants.LOGIN_FRAGMENT:{
				if(loginFragment!=null){
					valueReturn = loginFragment.onBackPressed();
				}
				break;
			}
		}

		if (valueReturn == 0) {
			super.onBackPressed();
		}
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getParentHandle() {
		return parentHandle;
	}

	public void setParentHandle(long parentHandle) {
		this.parentHandle = parentHandle;
	}

	public Uri getUriData() {
		return uriData;
	}

	public void setUriData(Uri uriData) {
		this.uriData = uriData;
	}

	public Bundle getExtras() {
		return extras;
	}

	public void setExtras(Bundle extras) {
		this.extras = extras;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

//	public void onNewIntent(Intent intent){
//		if (intent != null && Constants.ACTION_CONFIRM.equals(intent.getAction())) {
//			loginFragment.handleConfirmationIntent(intent);
//		}
//	}

	public static void log(String message) {
		Util.log("LoginActivityLollipop", message);
	}

}
