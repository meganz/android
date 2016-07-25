package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.RelativeLayout;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.CreateAccountFragmentLollipop;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.InitialTourFragmentLollipop;
import mega.privacy.android.app.LoginFragmentLollipop;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

 
public class LoginActivityLollipop extends Activity{

	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;

	RelativeLayout relativeContainer;

	//Fragments
	InitialTourFragmentLollipop tourFragment;
	LoginFragmentLollipop loginFragment;
	ChooseAccountFragmentLollipop chooseAccountFragment;
	CreateAccountFragmentLollipop createAccountFragment;

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

		showFragment(visibleFragment);
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
				ft.commit();

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
				ft.commit();
				break;
			}
			case Constants.CREATE_ACCOUNT_FRAGMENT:{

				if(createAccountFragment==null){
					createAccountFragment = new CreateAccountFragmentLollipop();
				}

				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_login, createAccountFragment);
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
		}

		if (valueReturn == 0) {
			super.onBackPressed();
		}
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
