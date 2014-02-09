package com.mega.android;

import com.mega.components.MySwitch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnClickListener{
	
	EditText et_password;
	Button bRegister;
	Button bLogin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
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
	    cadena = "Density: " + density + "_Width: " + dpWidth + "_Height: " + dpHeight + "_";
	    Toast.makeText(this, cadena, Toast.LENGTH_LONG).show();
		
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
		Intent intent = new Intent(this, ManagerActivity.class);
		startActivity(intent);
		finish();
	}
	
	public void onRegisterClick(View v){
		Intent intent = new Intent(this, CreateAccountActivity.class);
		startActivity(intent);
		finish();
	}

	
}
