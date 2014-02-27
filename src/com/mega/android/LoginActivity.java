package com.mega.android;

import java.util.Locale;

import com.mega.components.MySwitch;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListener;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Credentials;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnClickListener, MegaRequestListenerInterface{
	
	EditText et_user;
	EditText et_password;
	Button bRegister;
	Button bLogin;
	
	private ProgressDialog progress;
	
	private String lastEmail;
	private String lastPassword;
	
	private String confirmLink;
	
	static LoginActivity loginActivity;
    private MegaApiAndroid megaApi;
    private MegaRequestListener requestListener;
    UserCredentials credentials;
	
	/*
	 * Task to process email and password
	 */
	private class HashTask extends AsyncTask<String, Void, String[]> {

		@Override
		protected String[] doInBackground(String... args) {
			String privateKey = megaApi.getBase64PwKey(args[1]);
			String publicKey = megaApi.getStringHash(privateKey, args[0]);
			return new String[]{new String(privateKey), new String(publicKey)}; 
		}

		
		@Override
		protected void onPostExecute(String[] key) {
			onKeysGenerated(key[0], key[1]);
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

	    float density  = getResources().getDisplayMetrics().density;
	    float dpHeight = outMetrics.heightPixels / density;
	    float dpWidth  = outMetrics.widthPixels / density;
		
		if (Preferences.getCredentials(this) != null){
			Intent intent = new Intent(this, ManagerActivity.class);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			}
			this.startActivity(intent);
			this.finish();
			return;
		}
		
		loginActivity = this;
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		setContentView(R.layout.activity_login);
		
		et_user = (EditText) findViewById(R.id.emailText);
		et_password = (EditText) findViewById(R.id.passwordText);
		et_password.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					submitForm();
					return true;
				}
				return false;
			}
		});
		
		bRegister = (Button) findViewById(R.id.button_create_account_login);
		bLogin = (Button) findViewById(R.id.button_login_login);
		bRegister.setOnClickListener(this);		
		bLogin.setOnClickListener(this);
		
		MySwitch loginSwitch = (MySwitch) findViewById(R.id.switch_login);
		loginSwitch.setChecked(true);
		
		loginSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
						et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
						et_password.setSelection(et_password.getText().length());
				}else{
						et_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
						et_password.setSelection(et_password.getText().length());
			    }				
			}
		});
		
		progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.login_logging_in));
		progress.setCancelable(false);
		progress.setCanceledOnTouchOutside(false);
	}
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.button_login_login:
				onLoginClick(v);
				break;
			case R.id.button_create_account_login:
				onRegisterClick(v);
				break;
		}
	}
	
	public void onLoginClick(View v){
		submitForm();
//		String username = et_user.getText().toString();
//		String password = et_password.getText().toString();
//		megaApi.login(username, password, LoginActivity.this);
	}
	
	public void onRegisterClick(View v){
		Intent intent = new Intent(this, CreateAccountActivity.class);
		startActivity(intent);
		finish();
	}
	
	/*
	 * Log in form submit
	 */
	private void submitForm() {
		if (!validateForm()) {
			return;
		}
		
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(et_user.getWindowToken(), 0);
		
		if(!Util.isOnline(this))
		{
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem),false, this);
			return;
		}
		
		progress.setMessage(getString(R.string.login_generating_key));
		progress.show();
		
		lastEmail = et_user.getText().toString().toLowerCase(Locale.ENGLISH).trim();
		lastPassword = et_password.getText().toString();
		
		log("generating keys");
		
		new HashTask().execute(lastEmail, lastPassword);
	}
	
	private void onKeysGenerated(final String privateKey, final String publicKey) {
		log("key generation finished");

		if (confirmLink == null) {
			onKeysGeneratedLogin(privateKey, publicKey);
		} 
	}
	
	private void onKeysGeneratedLogin(final String privateKey, final String publicKey) {
		
		if(!Util.isOnline(this)){
			try{ progress.dismiss(); } catch(Exception ex) {};
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		progress.setMessage(getString(R.string.login_connecting_to_server));
		
		credentials = new UserCredentials(lastEmail,privateKey, publicKey);
		
		megaApi.fastLogin(lastEmail, publicKey, privateKey, this);
	}
	
	/*
	 * Validate email and password
	 */
	private boolean validateForm() {
		String emailError = getEmailError();
		String passwordError = getPasswordError();

		et_user.setError(emailError);
		et_password.setError(passwordError);

		if (emailError != null) {
			et_user.requestFocus();
			return false;
		} else if (passwordError != null) {
			et_password.requestFocus();
			return false;
		}
		return true;
	}
	
	/*
	 * Validate email
	 */
	private String getEmailError() {
		String value = et_user.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_email);
		}
		if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
			return getString(R.string.error_invalid_email);
		}
		return null;
	}
	
	/*
	 * Validate password
	 */
	private String getPasswordError() {
		String value = et_password.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		}
		return null;
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request)
	{
		log("onRequestStart");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError error) {
		
		if (request.getType() == MegaRequest.TYPE_FAST_LOGIN){
			if (error.getErrorCode()!=MegaError.API_OK) {
				String errorMessage;
				if (error.getErrorCode() == MegaError.API_ENOENT) {
					errorMessage = getString(R.string.error_incorrect_email_or_password);
				}
				else if (error.getErrorCode() == MegaError.API_ENOENT) {
					errorMessage = getString(R.string.error_server_connection_problem);
				}
				else {
					errorMessage = new String(error.getErrorString());
				}
				try{ progress.dismiss(); } catch (Exception e){};
				Util.showErrorAlertDialog(errorMessage, false, loginActivity);
			}
			else{
				try{ 
					progress.setMessage(getString(R.string.download_updating_filelist));
				} 
				catch (Exception e){};
				
				Preferences.saveCredentials(loginActivity, credentials);
				Intent intent = new Intent(loginActivity,ManagerActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
			}
		}
		
//		if (request.getRequestString().equals("login")){
//			log("onRequestFinish " + request.getRequestString() + " Result: " + e.getErrorCode());
//			if(e.getErrorCode() != MegaError.API_OK) 
//				return;
//			
//			megaApi.fetchNodes(LoginActivity.this);
//		}
//		else if (request.getRequestString().equals("fetchnodes")){
//			log("onRequestFinish " + request.getRequestString() + " Result: " + e.getErrorCode());
//			if(e.getErrorCode() != MegaError.API_OK) 
//				return;
//			
//			NodeList children = megaApi.getChildren(megaApi.getRootNode());
//			for(int i=0; i<children.size(); i++)
//			{
//				MegaNode node = children.get(i);
//				log("Node: " + node.getName() + 
//						(node.isFolder() ? " (folder)" : (" " + node.getSize() + " bytes")));
//			}
//			
//
//			Intent intent = new Intent(this, ManagerActivity.class);
//			startActivity(intent);
//			finish();
//		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e)
	{
		log("onRequestTemporaryError");
	}
	
	
	
	public static void log(String message) {
		Util.log("LoginActivity", message);
	}	
}
