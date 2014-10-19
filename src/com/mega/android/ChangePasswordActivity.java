package com.mega.android;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ChangePasswordActivity extends PinActivity implements OnClickListener, MegaRequestListenerInterface{
	
	ChangePasswordActivity changePasswordActivity = this;

	private ProgressDialog progress;
	
	private MegaApiAndroid megaApi;
	
	private EditText oldPasswordView, newPassword1View, newPassword2View;
	private Button changePasswordButton;
	
	private ActionBar aB;
	
	private UserCredentials newCredentials = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_change_password);
		
		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(true);
		aB.setLogo(R.drawable.ic_action_navigation_accept);
		
		oldPasswordView = (EditText) findViewById(R.id.change_password_oldPassword);
		newPassword1View = (EditText) findViewById(R.id.change_password_newPassword1);
		newPassword2View = (EditText) findViewById(R.id.change_password_newPassword2);
		changePasswordButton = (Button) findViewById(R.id.change_password_password);
		changePasswordButton.setOnClickListener(this);
		
		progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.my_account_changing_password));
		progress.setCancelable(false);
		progress.setCanceledOnTouchOutside(false);
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
		switch(v.getId()){
			case R.id.change_password_password:{
				onChangePasswordClick();
				break;
			}
		}
	}
	
	public void onChangePasswordClick(){
		if(!Util.isOnline(this))
		{
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem),
					false, this);
			return;
		}
		
		if (!validateForm()) {
			return;
		}
		
		InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(oldPasswordView.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(newPassword1View.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(newPassword2View.getWindowToken(), 0);
		
		final String oldPassword = oldPasswordView.getText().toString();
		String newPassword1 = newPassword1View.getText().toString();
		String newPassword2 = newPassword2View.getText().toString();
		
		if (!newPassword1.equals(newPassword2)){
			log("no new password repeat");
			Util.showErrorAlertDialog(getString(R.string.my_account_change_password_error),
					false, this);
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
	private boolean validateForm() {
		String oldPasswordError = getOldPasswordError();
		String newPassword1Error = getNewPassword1Error();
		String newPassword2Error = getNewPassword2Error();

		oldPasswordView.setError(oldPasswordError);
		newPassword1View.setError(newPassword1Error);
		newPassword2View.setError(newPassword2Error);

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
		newCredentials = new UserCredentials(email);
		
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
			if (e.getErrorCode() != MegaError.API_OK){
				log("e.getErrorCode = " + e.getErrorCode() + "__ e.getErrorString = " + e.getErrorString());
				
				try{ 
					progress.dismiss();
				} catch(Exception ex) {};
				
				Util.showErrorAlertDialog(getString(R.string.my_account_change_password_error), false, changePasswordActivity);
				
			}
			else{
				//Now update the credentials
				newCredentials.setSession(megaApi.dumpSession());
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
//				DatabaseHandler dbH = new DatabaseHandler(getApplicationContext()); 
				dbH.clearCredentials();
				dbH.saveCredentials(newCredentials);
				
				try{ 
					progress.dismiss();
				} catch(Exception ex) {};
				
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
				finish();
				Toast.makeText(ChangePasswordActivity.this, R.string.my_account_change_password_OK, Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}
	
	public static void log(String message) {
		Util.log("ChangePasswordActivity", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
}
