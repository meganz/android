package com.mega.android;

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
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnClickListener, MegaRequestListenerInterface{
	
	EditText et_user;
	EditText et_password;
	Button bRegister;
	Button bLogin;
	
	final String TAG = "LoginActivity";
	
	static LoginActivity loginActivity;
    private MegaApiAndroid megaApi;
    private MegaRequestListener requestListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loginActivity = this;
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		
		setContentView(R.layout.activity_login);
		
		et_user = (EditText) findViewById(R.id.emailText);
		et_password = (EditText) findViewById(R.id.passwordText);
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
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

	    float density  = getResources().getDisplayMetrics().density;
	    float dpHeight = outMetrics.heightPixels / density;
	    float dpWidth  = outMetrics.widthPixels / density;
	    
	    String cadena;
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
		String username = et_user.getText().toString();
		String password = et_password.getText().toString();
		megaApi.login(username, password, LoginActivity.this);
	}
	
	public void onRegisterClick(View v){
		Intent intent = new Intent(this, CreateAccountActivity.class);
		startActivity(intent);
		finish();
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request)
	{
		Log.d(TAG, "onRequestStart");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e)
	{
		if (request.getRequestString().equals("login")){
			Log.d(TAG, "onRequestFinish " + request.getRequestString() + " Result: " + e.getErrorCode());
			if(e.getErrorCode() != MegaError.API_OK) 
				return;
			
			megaApi.fetchNodes(LoginActivity.this);
		}
		else if (request.getRequestString().equals("fetchnodes")){
			Log.d(TAG, "onRequestFinish " + request.getRequestString() + " Result: " + e.getErrorCode());
			if(e.getErrorCode() != MegaError.API_OK) 
				return;
			
			NodeList children = megaApi.getChildren(megaApi.getRootNode());
			for(int i=0; i<children.size(); i++)
			{
				MegaNode node = children.get(i);
				Log.d(TAG, "Node: " + node.getName() + 
						(node.isFolder() ? " (folder)" : (" " + node.getSize() + " bytes")));
			}
			

			Intent intent = new Intent(this, ManagerActivity.class);
			startActivity(intent);
			finish();
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e)
	{
		Log.d(TAG, "onRequestTemporaryError");
	}

	
}
