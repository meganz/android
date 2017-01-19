package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

 
public class LoginActivityLollipop extends AppCompatActivity {

	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;

	RelativeLayout relativeContainer;

	//Fragments
	TourFragmentLollipop tourFragment;
	LoginFragmentLollipop loginFragment;
	ChooseAccountFragmentLollipop chooseAccountFragment;
	CreateAccountFragmentLollipop createAccountFragment;
	ConfirmEmailFragmentLollipop confirmEmailFragment;

	ActionBar aB;
	int visibleFragment;

	static LoginActivityLollipop loginActivity;

	Intent intentReceived = null;

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

		aB = getSupportActionBar();
		if(aB!=null){
			aB.hide();
		}

		scaleW = Util.getScaleW(outMetrics, density);
		scaleH = Util.getScaleH(outMetrics, density);

	    dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		setContentView(R.layout.activity_login);
		relativeContainer = (RelativeLayout) findViewById(R.id.relative_container_login);

		intentReceived = getIntent();
		if (intentReceived != null){
			visibleFragment = intentReceived.getIntExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
			log("There is an intent! VisibleFragment: "+visibleFragment);
		}
		else{
			visibleFragment = 1;
		}

//		visibleFragment = Constants.CHOOSE_ACCOUNT_FRAGMENT;
		showFragment(visibleFragment);
	}

	public void showSnackbar(String message){
		Snackbar snackbar = Snackbar.make(relativeContainer,message,Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}

	public void showFragment(int visibleFragment){
		log("showFragment");
		this.visibleFragment = visibleFragment;
		switch (visibleFragment){
			case Constants.LOGIN_FRAGMENT:{
				log("showLoginFragment");
				if(loginFragment==null){
					loginFragment = new LoginFragmentLollipop();
				}
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_login, loginFragment);
				ft.commitNow();

//
//				getFragmentManager()
//						.beginTransaction()
//						.attach(loginFragment)
//						.commit();
				break;
			}
			case Constants.CHOOSE_ACCOUNT_FRAGMENT:{

				if(chooseAccountFragment==null){
					chooseAccountFragment = new ChooseAccountFragmentLollipop();
				}

				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_login, chooseAccountFragment);
				ft.commitNow();
				break;
			}
			case Constants.CREATE_ACCOUNT_FRAGMENT:{

				if(createAccountFragment==null){
					createAccountFragment = new CreateAccountFragmentLollipop();
				}

				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_login, createAccountFragment);
				ft.commitNow();
				break;
			}
			case Constants.TOUR_FRAGMENT:{

				if(tourFragment==null){
					tourFragment = new TourFragmentLollipop();
				}

				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_login, tourFragment);
				ft.commitNow();
				break;
			}
			case Constants.CONFIRM_EMAIL_FRAGMENT:{

				if(confirmEmailFragment==null){
					confirmEmailFragment = new ConfirmEmailFragmentLollipop();
				}

				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_login, confirmEmailFragment);
				ft.commitNow();
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.executePendingTransactions();
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

	public void startCameraSyncService(boolean firstTimeCam, int time){
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
			}, time);
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
			case Constants.CREATE_ACCOUNT_FRAGMENT:{
				showFragment(Constants.TOUR_FRAGMENT);
				break;
			}
			case Constants.TOUR_FRAGMENT:{
				valueReturn=0;
				break;
			}
			case Constants.CONFIRM_EMAIL_FRAGMENT:{
				valueReturn=0;
				break;
			}
			case Constants.CHOOSE_ACCOUNT_FRAGMENT:{
				//nothing to do
				break;
			}
		}

		if (valueReturn == 0) {
			super.onBackPressed();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		Intent intent = getIntent();

		if (intent != null){
			if (intent.getAction() != null){
				if (intent.getAction().equals(Constants.ACTION_CANCEL_UPLOAD) || intent.getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD) || intent.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)){
					log("ACTION_CANCEL_UPLOAD or ACTION_CANCEL_DOWNLOAD or ACTION_CANCEL_CAM_SYNC");
					Intent tempIntent = null;
					String title = null;
					String text = null;
					if(intent.getAction().equals(Constants.ACTION_CANCEL_UPLOAD)){
						tempIntent = new Intent(this, UploadService.class);
						tempIntent.setAction(UploadService.ACTION_CANCEL);
						title = getString(R.string.upload_uploading);
						text = getString(R.string.upload_cancel_uploading);
					}
					else if (intent.getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD)){
						tempIntent = new Intent(this, DownloadService.class);
						tempIntent.setAction(DownloadService.ACTION_CANCEL);
						title = getString(R.string.download_downloading);
						text = getString(R.string.download_cancel_downloading);
					}
					else if (intent.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)){
						tempIntent = new Intent(this, CameraSyncService.class);
						tempIntent.setAction(CameraSyncService.ACTION_CANCEL);
						title = getString(R.string.cam_sync_syncing);
						text = getString(R.string.cam_sync_cancel_sync);
					}

					final Intent cancelIntent = tempIntent;
					AlertDialog.Builder builder = Util.getCustomAlertBuilder(this,
							title, text, null);
					builder.setPositiveButton(getString(R.string.general_yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									startService(cancelIntent);
								}
							});
					builder.setNegativeButton(getString(R.string.general_no), null);
					final AlertDialog dialog = builder.create();
					try {
						dialog.show();
					}
					catch(Exception ex)	{
						startService(cancelIntent);
					}
				}
				intent.setAction(null);
			}
		}

		setIntent(null);
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
