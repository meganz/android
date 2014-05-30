package com.mega.android;

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

public class ChangePasswordActivity extends ActionBarActivity implements OnClickListener, MegaRequestListenerInterface{
	
	private class HashTask extends AsyncTask<String, Void, String[]> {

		@Override
		protected String[] doInBackground(String... args) {
			String privateKey = megaApi.getBase64PwKey(args[1]);
			String publicKey = megaApi.getStringHash(privateKey, args[0]);
			String email = args[0];
			String newPassword = args[1];
			String oldPassword = args[2];
			return new String[]{new String(email), new String(newPassword), new String(oldPassword), new String(privateKey), new String(publicKey)};
		}

		
		@Override
		protected void onPostExecute(String[] key) {
			changePassword(key[0], key[1], key[2], key[3], key[4]);
		}

	}

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
		
		if (!checkPassword(oldPassword, newPassword1, newPassword2)){
			Util.showErrorAlertDialog(getString(R.string.my_account_change_password_error),
					false, this);
			return;
		}
		
		final String newPassword = newPassword1;

		
		progress.setMessage(getString(R.string.my_account_changing_password));
		progress.show();
		
		final UserCredentials oldCredentials = Preferences.getCredentials(ChangePasswordActivity.this);
		
		String currentEmail = oldCredentials.getEmail();
		
		new HashTask().execute(currentEmail, newPassword, oldPassword);
	}
	
	private boolean checkPassword (String oldPassword, String newPassword1, String newPassword2){
		log(newPassword1);
		log(newPassword2);
		if (!newPassword1.equals(newPassword2)){
			log("no new password repeat");
			return false;
		}
		
		UserCredentials cred = Preferences.getCredentials(this);
		String privateKey = megaApi.getBase64PwKey(oldPassword);
		String publicKey = megaApi.getStringHash(privateKey, cred.getEmail());
		
		if (!privateKey.equals(cred.getPrivateKey()) || !publicKey.equals(cred.getPublicKey())){
			log("no old password");
			return false;
		}
			
		return true;
	}
	
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
	
	private void changePassword (String email, String newPassword, String oldPassword, String privateKey, String publicKey){
		newCredentials = new UserCredentials(email, privateKey, publicKey);
		
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
			//Now update the credentials
			Preferences.saveCredentials(ChangePasswordActivity.this, newCredentials);
			
			try{ 
				progress.dismiss();
			} catch(Exception ex) {};
			
			if (e.getErrorCode() != MegaError.API_OK) {
				Util.showErrorAlertDialog(e, ChangePasswordActivity.this);
			}
			else{
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
