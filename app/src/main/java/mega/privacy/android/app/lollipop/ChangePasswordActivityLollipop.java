package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;


@SuppressLint("NewApi")
public class ChangePasswordActivityLollipop extends PinActivityLollipop implements OnClickListener, MegaRequestListenerInterface{
	
	ChangePasswordActivityLollipop changePasswordActivity = this;

	private ProgressDialog progress;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	private MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	boolean changePassword = true;
	
	private EditText oldPasswordView, newPassword1View, newPassword2View;
	private RelativeLayout oldPasswordErrorView, newPassword1ErrorView, newPassword2ErrorView;
	private TextView oldPasswordErrorText, newPassword1ErrorText, newPassword2ErrorText;
	private Button changePasswordButton;
	private Button cancelChangePasswordButton;
	private ImageView loginThreeDots;
	private SwitchCompat loginSwitch;
	private TextView loginABC;
    private RelativeLayout fragmentContainer;
	private TextView title;
	private String linkToReset;
	private String mk;

	private ActionBar aB;
	Toolbar tB;

	private Drawable oldPassword_background;
	private Drawable newPassword_background;
	private Drawable newPassword2_background;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_change_password);
		
        fragmentContainer = (RelativeLayout) findViewById(R.id.fragment_container_change_pass);
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		if(megaApi==null||megaApi.getRootNode()==null){
			log("Refresh session - sdk");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		if(Util.isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
			}

			if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
				log("Refresh session - karere");
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return;
			}
		}

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);

		title = (TextView) findViewById(R.id.title_change_pass);
		LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams)title.getLayoutParams();
		titleParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0,  0, Util.scaleHeightPx(30, outMetrics));
		title.setLayoutParams(titleParams);
		
		oldPasswordView = (EditText) findViewById(R.id.change_password_oldPassword);
		android.view.ViewGroup.LayoutParams paramsb1 = oldPasswordView.getLayoutParams();		
		paramsb1.width = Util.scaleWidthPx(280, outMetrics);		
		oldPasswordView.setLayoutParams(paramsb1);

		//Left margin
		LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)oldPasswordView.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		oldPasswordView.setLayoutParams(textParams);

		oldPasswordView.getBackground().mutate().clearColorFilter();

		oldPasswordErrorView = (RelativeLayout) findViewById(R.id.login_oldPassword_text_error);
		paramsb1 = oldPasswordErrorView.getLayoutParams();
		paramsb1.width = Util.scaleWidthPx(280, outMetrics);
		oldPasswordErrorView.setLayoutParams(paramsb1);
		//Left margin
		textParams = (LinearLayout.LayoutParams)oldPasswordErrorView.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		oldPasswordErrorView.setLayoutParams(textParams);
		oldPasswordErrorView.setPadding(Util.scaleWidthPx(3, outMetrics), 0, 0, 0);

		oldPasswordErrorText = (TextView) findViewById(R.id.login_oldPassword_text_error_text);

		oldPassword_background = oldPasswordView.getBackground().mutate().getConstantState().newDrawable();

		oldPasswordView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				quitError(oldPasswordView);
			}
		});
		
		newPassword1View = (EditText) findViewById(R.id.change_password_newPassword1);
		paramsb1 = newPassword1View.getLayoutParams();
		paramsb1.width = Util.scaleWidthPx(280, outMetrics);
		newPassword1View.setLayoutParams(paramsb1);
		textParams = (LinearLayout.LayoutParams) newPassword1View.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		newPassword1View.setLayoutParams(textParams);

		newPassword1View.getBackground().clearColorFilter();

		newPassword_background = newPassword1View.getBackground().mutate().getConstantState().newDrawable();

		newPassword1ErrorView = (RelativeLayout) findViewById(R.id.login_newPassword1_text_error);
		paramsb1 = newPassword1ErrorView.getLayoutParams();
		paramsb1.width = Util.scaleWidthPx(280, outMetrics);
		newPassword1ErrorView.setLayoutParams(paramsb1);
		//Left margin
		textParams = (LinearLayout.LayoutParams)newPassword1ErrorView.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		newPassword1ErrorView.setLayoutParams(textParams);
		newPassword1ErrorView.setPadding(Util.scaleWidthPx(3, outMetrics), 0, 0, 0);

		newPassword1ErrorText = (TextView) findViewById(R.id.login_newPassword1_text_error_text);

		newPassword1View.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				quitError(newPassword1View);
			}
		});
		
		newPassword2View = (EditText) findViewById(R.id.change_password_newPassword2);
		paramsb1 = newPassword2View.getLayoutParams();
		paramsb1.width = Util.scaleWidthPx(280, outMetrics);
		newPassword2View.setLayoutParams(paramsb1);
		textParams = (LinearLayout.LayoutParams) newPassword2View.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		newPassword2View.setLayoutParams(textParams);

		newPassword2View.getBackground().clearColorFilter();

		newPassword2ErrorView = (RelativeLayout) findViewById(R.id.login_newPassword2_text_error);
		paramsb1 = newPassword2ErrorView.getLayoutParams();
		paramsb1.width = Util.scaleWidthPx(280, outMetrics);
		newPassword2ErrorView.setLayoutParams(paramsb1);
		//Left margin
		textParams = (LinearLayout.LayoutParams)newPassword2ErrorView.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
		newPassword2ErrorView.setLayoutParams(textParams);
		newPassword2ErrorView.setPadding(Util.scaleWidthPx(3, outMetrics), 0, 0, 0);

		newPassword2ErrorText = (TextView) findViewById(R.id.login_newPassword2_text_error_text);

		newPassword2_background = newPassword2View.getBackground().mutate().getConstantState().newDrawable();

		newPassword2View.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				quitError(newPassword2View);
			}
		});
		
		loginThreeDots = (ImageView) findViewById(R.id.change_pass_three_dots);
		LinearLayout.LayoutParams textThreeDots = (LinearLayout.LayoutParams)loginThreeDots.getLayoutParams();
		textThreeDots.setMargins(Util.scaleWidthPx(0, outMetrics), 0, Util.scaleWidthPx(10, outMetrics), 0); 
		loginThreeDots.setLayoutParams(textThreeDots);
		
		loginABC = (TextView) findViewById(R.id.ABC_change_pass);
		
		loginSwitch = (SwitchCompat) findViewById(R.id.switch_change_pass);
		LinearLayout.LayoutParams switchParams = (LinearLayout.LayoutParams)loginSwitch.getLayoutParams();
		switchParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0); 
		loginSwitch.setLayoutParams(switchParams);
		loginSwitch.setChecked(false);

		
		loginSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(!isChecked){
					oldPasswordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					oldPasswordView.setTypeface(Typeface.SANS_SERIF,Typeface.NORMAL);
					oldPasswordView.setSelection(oldPasswordView.getText().length());
					
					newPassword1View.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					newPassword1View.setTypeface(Typeface.SANS_SERIF,Typeface.NORMAL);
					newPassword1View.setSelection(newPassword1View.getText().length());
					
					newPassword2View.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					newPassword2View.setTypeface(Typeface.SANS_SERIF,Typeface.NORMAL);
					newPassword2View.setSelection(newPassword2View.getText().length());
				}else{
					oldPasswordView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					oldPasswordView.setSelection(oldPasswordView.getText().length());
					
					newPassword1View.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					newPassword1View.setSelection(newPassword1View.getText().length());
					
					newPassword2View.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					newPassword2View.setSelection(newPassword2View.getText().length());
			    }				
			}
		});		
				
		changePasswordButton = (Button) findViewById(R.id.action_change_password);

		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)changePasswordButton.getLayoutParams();
		if(this.getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			log("onCreate: Landscape configuration");
			params.setMargins(0, 0, Util.scaleWidthPx(14, outMetrics), 0);
		}
		else {
			params.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0);
		}
		changePasswordButton.setLayoutParams(params);
		changePasswordButton.setOnClickListener(this);

		cancelChangePasswordButton = (Button) findViewById(R.id.cancel_change_password);
		cancelChangePasswordButton.setOnClickListener(this);
		
		progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.my_account_changing_password));
		progress.setCancelable(false);
		progress.setCanceledOnTouchOutside(false);

		Intent intentReceived = getIntent();
		if (intentReceived != null) {
			log("There is an intent!");
			if(intentReceived.getAction()!=null){
				if (getIntent().getAction().equals(Constants.ACTION_RESET_PASS_FROM_LINK)) {
					log("ACTION_RESET_PASS_FROM_LINK");
					changePassword=false;
					linkToReset = getIntent().getDataString();
					if (linkToReset == null) {
						log("link is NULL - close activity");
						finish();
					}
					mk = getIntent().getStringExtra("MK");
					if(mk==null){
						log("MK is NULL - close activity");
						Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
					}
					oldPasswordView.setVisibility(View.GONE);
					title.setText(getString(R.string.title_enter_new_password));
				}
				if (getIntent().getAction().equals(Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT)) {
					changePassword=false;
					log("ACTION_RESET_PASS_FROM_PARK_ACCOUNT");
					linkToReset = getIntent().getDataString();
					if (linkToReset == null) {
						log("link is NULL - close activity");
						Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
					}
					mk = null;
					oldPasswordView.setVisibility(View.GONE);
					title.setText(getString(R.string.title_enter_new_password));
				}
			}
		}
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
		    case android.R.id.home:{
		    	finish();
		    	return true;
		    }
		}	    
	    return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
		switch(v.getId()){
			case R.id.action_change_password: {
				if (changePassword) {
					log("ok proceed to change");
					onChangePasswordClick();
				} else {
					log("reset pass on click");
					if (linkToReset == null) {
						log("link is NULL");
						Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
					} else {
						if (mk == null) {
							log("procced to park account");
							onResetPasswordClick(false);
						} else {
							log("ok proceed to reset");
							onResetPasswordClick(true);
						}
					}
				}
				break;
			}
			case R.id.cancel_change_password:{
				changePasswordActivity.finish();
				break;
			}
		}
	}

	public void onResetPasswordClick(boolean hasMk){
		log("onResetPasswordClick");

		if(!Util.isOnline(this))
		{
			Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
			return;
		}

		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		if (!validateForm(false)) {
			return;
		}

		InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(newPassword1View.getWindowToken(), 0);
		inputMethodManager.hideSoftInputFromWindow(newPassword2View.getWindowToken(), 0);

		final String oldPassword = oldPasswordView.getText().toString();
		String newPassword1 = newPassword1View.getText().toString();
		String newPassword2 = newPassword2View.getText().toString();

		if(oldPassword.equals(newPassword1)){
			log("old password and new password are equals");
			setError(newPassword1View, getString(R.string.old_and_new_passwords_equals));
			return;
		}

		if (!newPassword1.equals(newPassword2)){
			log("no new password repeat");
//			newPassword2View.setError(getString(R.string.my_account_change_password_dont_match));
			setError(newPassword2View, getString(R.string.my_account_change_password_dont_match));
			return;
		}

		final String newPassword = newPassword1;

		progress.setMessage(getString(R.string.my_account_changing_password));
		progress.show();

		if(hasMk){
			log("reset with mk");
			megaApi.confirmResetPassword(linkToReset, newPassword, mk, this);
		}
		else{
			megaApi.confirmResetPassword(linkToReset, newPassword, null, this);
		}

	}
	
	public void onChangePasswordClick(){
		log("onChangePasswordClick");
		if(!Util.isOnline(this))
		{
			Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
			return;
		}
		
		if (!validateForm(true)) {
			return;
		}
		
		InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(oldPasswordView.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(newPassword1View.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(newPassword2View.getWindowToken(), 0);
		
		final String oldPassword = oldPasswordView.getText().toString();
		String newPassword1 = newPassword1View.getText().toString();
		String newPassword2 = newPassword2View.getText().toString();

		if(oldPassword.equals(newPassword1)){
			log("old password and new password are equals");
			setError(newPassword1View, getString(R.string.old_and_new_passwords_equals));
			return;
		}

		if (!newPassword1.equals(newPassword2)){
			log("no new password repeat");
//			newPassword2View.setError(getString(R.string.my_account_change_password_dont_match));
			setError(newPassword2View, getString(R.string.my_account_change_password_dont_match));
			return;
		}
		
//		if (!checkPassword(oldPassword, newPassword1, newPassword2)){
//
//		}
		
		final String newPassword = newPassword1;

		
		progress.setMessage(getString(R.string.my_account_changing_password));
		progress.show();
		
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
		final UserCredentials oldCredentials = dbH.getCredentials();
		
		String currentEmail = oldCredentials.getEmail();
		
//		new HashTask().execute(currentEmail, newPassword, oldPassword);
		changePassword(currentEmail, newPassword, oldPassword);
	}
	
//	private boolean checkPassword (String oldPassword, String newPassword1, String newPassword2){
//		log(newPassword1);
//		log(newPassword2);
//		if (!newPassword1.equals(newPassword2)){
//			log("no new password repeat");
//			return false;
//		}
//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext()); 
//		UserCredentials cred = dbH.getCredentials();
//		String email = cred.getEmail();
//		new CheckTask();
//		String privateKey = megaApi.getBase64PwKey(oldPassword);
//		String publicKey = megaApi.getStringHash(privateKey, cred.getEmail());
//		
//		if (!privateKey.equals(cred.getPrivateKey()) || !publicKey.equals(cred.getPublicKey())){
//			log("no old password");
//			return false;
//		}
//			
//		return true;
//	}
	
	/*
	 * Validate old password and new passwords 
	 */
	private boolean validateForm(boolean withOldPass) {
		if(withOldPass){
			String oldPasswordError = getOldPasswordError();
			String newPassword1Error = getNewPassword1Error();
			String newPassword2Error = getNewPassword2Error();

//			oldPasswordView.setError(oldPasswordError);
			setError(oldPasswordView, oldPasswordError);
//			newPassword1View.setError(newPassword1Error);
			setError(newPassword1View, newPassword1Error);
//			newPassword2View.setError(newPassword2Error);
			setError(newPassword2View, newPassword2Error);

			if (oldPasswordError != null) {
				oldPasswordView.requestFocus();
				return false;
			}
			else if(newPassword1Error != null) {
				newPassword1View.requestFocus();
				return false;
			}
			else if(newPassword2Error != null) {
				newPassword2View.requestFocus();
				return false;
			}
		}
		else{
			String newPassword1Error = getNewPassword1Error();
			String newPassword2Error = getNewPassword2Error();

//			newPassword1View.setError(newPassword1Error);
			setError(newPassword1View, newPassword1Error);
//			newPassword2View.setError(newPassword2Error);
			setError(newPassword2View, newPassword2Error);

			if(newPassword1Error != null) {
				newPassword1View.requestFocus();
				return false;
			}
			else if(newPassword2Error != null) {
				newPassword2View.requestFocus();
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Validate old password
	 */
	private String getOldPasswordError() {
		String value = oldPasswordView.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		}
		return null;
	}
	
	/*
	 * Validate new password1
	 */
	private String getNewPassword1Error() {
		String value = newPassword1View.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		}
		return null;
	}
	
	/*
	 * Validate new password2
	 */
	private String getNewPassword2Error() {
		String value = newPassword2View.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		}
		return null;
	}
	
	private void changePassword (String email, String newPassword, String oldPassword){
		megaApi.changePassword(oldPassword, newPassword, this);
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish");
		
		if (request.getType() == MegaRequest.TYPE_CHANGE_PW){
			log("TYPE_CHANGE_PW");
			if (e.getErrorCode() != MegaError.API_OK){
				log("e.getErrorCode = " + e.getErrorCode() + "__ e.getErrorString = " + e.getErrorString());
				
				try{ 
					progress.dismiss();
				} catch(Exception ex) {};

				//Intent to MyAccount
				Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
				resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
				if(e.getErrorCode()==MegaError.API_EARGS){
					log("Error, the old pass is not correct");
					resetPassIntent.putExtra("RESULT", MegaError.API_EARGS);
				}
				else{
					log("General Error");
					resetPassIntent.putExtra("RESULT", -1);
				}

				startActivity(resetPassIntent);
				finish();
			}
			else{
				log("pass changed OK");
				try{ 
					progress.dismiss();
				} catch(Exception ex) {};
				
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
				//Intent to MyAccount
				Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
				resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
				resetPassIntent.putExtra("RESULT", 0);
				startActivity(resetPassIntent);
				finish();
			}
		}
		else if(request.getType() == MegaRequest.TYPE_CONFIRM_RECOVERY_LINK){
			log("TYPE_CONFIRM_RECOVERY_LINK");
			if(megaApi.getRootNode()==null) {
				log("Not logged in");
				if (e.getErrorCode() != MegaError.API_OK){
					log("e.getErrorCode = " + e.getErrorCode() + "__ e.getErrorString = " + e.getErrorString());

					try{
						progress.dismiss();
					} catch(Exception ex) {};

					//Intent to Login
					Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
					resetPassIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
					resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);

					if(e.getErrorCode()==MegaError.API_EARGS){
						resetPassIntent.putExtra("RESULT", MegaError.API_EARGS);
					}
					else if(e.getErrorCode()==MegaError.API_EKEY){
						resetPassIntent.putExtra("RESULT", MegaError.API_EKEY);
					}
					else{
						resetPassIntent.putExtra("RESULT", -1);
					}

					startActivity(resetPassIntent);
					finish();
				}
				else{
					log("pass changed");
					try{
						progress.dismiss();
					} catch(Exception ex) {};

					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
					//Intent to Login
					Intent resetPassIntent = new Intent(this, LoginActivityLollipop.class);
					resetPassIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
					resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
					resetPassIntent.putExtra("RESULT", 0);
					startActivity(resetPassIntent);
					finish();
				}
			}
			else {
				log("Logged IN");

				if (e.getErrorCode() != MegaError.API_OK){
					log("e.getErrorCode = " + e.getErrorCode() + "__ e.getErrorString = " + e.getErrorString());

					try{
						progress.dismiss();
					} catch(Exception ex) {};

					//Intent to Login
					Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
					resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
					resetPassIntent.putExtra("RESULT", -1);
					startActivity(resetPassIntent);
					finish();
				}
				else{
					log("pass changed");
					try{
						progress.dismiss();
					} catch(Exception ex) {};

					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
					//Intent to Login
					Intent resetPassIntent = new Intent(this, ManagerActivityLollipop.class);
					resetPassIntent.setAction(Constants.ACTION_PASS_CHANGED);
					resetPassIntent.putExtra("RESULT", 0);
					startActivity(resetPassIntent);
					finish();
				}

			}
		}
	}

	private void setError(final EditText editText, String error){
		log("setError");
		if(error == null || error.equals("")){
			return;
		}
		switch (editText.getId()){
			case R.id.change_password_oldPassword:{
				oldPasswordErrorView.setVisibility(View.VISIBLE);
				oldPasswordErrorText.setText(error);
				PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
				Drawable background = oldPassword_background.mutate().getConstantState().newDrawable();
				background.setColorFilter(porterDuffColorFilter);
				if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					oldPasswordView.setBackgroundDrawable(background);
				} else{
					oldPasswordView.setBackground(background);
				}
				LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams)oldPasswordView.getLayoutParams();
				textParamsEditText.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, 0);
				oldPasswordView.setLayoutParams(textParamsEditText);
			}
			break;
			case R.id.change_password_newPassword1:{
				newPassword1ErrorView.setVisibility(View.VISIBLE);
				newPassword1ErrorText.setText(error);
				PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
				Drawable background = newPassword_background.mutate().getConstantState().newDrawable();
				background.setColorFilter(porterDuffColorFilter);
				if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					newPassword1View.setBackgroundDrawable(background);
				} else{
					newPassword1View.setBackground(background);
				}
				LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) newPassword1View.getLayoutParams();
				textParamsEditText.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, 0);
				newPassword1View.setLayoutParams(textParamsEditText);
			}
			break;
			case R.id.change_password_newPassword2:{
				newPassword2ErrorView.setVisibility(View.VISIBLE);
				newPassword2ErrorText.setText(error);
				PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
//                et_user.getBackground().mutate().setColorFilter(porterDuffColorFilter);
				Drawable background = newPassword2_background.mutate().getConstantState().newDrawable();
				background.setColorFilter(porterDuffColorFilter);
				if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					newPassword2View.setBackgroundDrawable(background);
				} else{
					newPassword2View.setBackground(background);
				}
				LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) newPassword2View.getLayoutParams();
				textParamsEditText.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, 0);
				newPassword2View.setLayoutParams(textParamsEditText);
			}
			break;
		}
	}

	private void quitError(EditText editText){
		switch (editText.getId()){
			case R.id.change_password_oldPassword:{
				if(oldPasswordErrorView.getVisibility() != View.GONE){
					oldPasswordErrorView.setVisibility(View.GONE);
					if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
						oldPasswordView.setBackgroundDrawable(oldPassword_background);
					} else{
						oldPasswordView.setBackground(oldPassword_background);
					}
					LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams)oldPasswordView.getLayoutParams();
					textParamsEditText.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleWidthPx(10, outMetrics));
					oldPasswordView.setLayoutParams(textParamsEditText);
				}
			}
			break;
			case R.id.change_password_newPassword1:{
				if(newPassword1ErrorView.getVisibility() != View.GONE){
					newPassword1ErrorView.setVisibility(View.GONE);
					if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
						newPassword1View.setBackgroundDrawable(newPassword_background);
					} else{
						newPassword1View.setBackground(newPassword_background);
					}
					LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) newPassword1View.getLayoutParams();
					textParamsEditText.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleWidthPx(10, outMetrics));
					newPassword1View.setLayoutParams(textParamsEditText);
				}
			}
			break;
			case R.id.change_password_newPassword2:{
				if(newPassword2ErrorView.getVisibility() != View.GONE){
					newPassword2ErrorView.setVisibility(View.GONE);
					if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
						newPassword2View.setBackgroundDrawable(newPassword2_background);
					} else{
						newPassword2View.setBackground(newPassword2_background);
					}
					LinearLayout.LayoutParams textParamsEditText = (LinearLayout.LayoutParams) newPassword2View.getLayoutParams();
					textParamsEditText.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleWidthPx(10, outMetrics));
					newPassword2View.setLayoutParams(textParamsEditText);
				}
			}
			break;
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}
	
	public static void log(String message) {
		Util.log("ChangePasswordActivityLollipop", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}
}
