package com.mega.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class CreateAccountActivity extends Activity implements OnClickListener{
	
	Button bRegister;
	Button bLogin;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_account);	
		
		TextView tos = (TextView)findViewById(R.id.tos);
		Spanned spanned = Html.fromHtml(getString(R.string.tos));
		tos.setMovementMethod(LinkMovementMethod.getInstance());
		tos.setText(spanned);
		tos.setLinkTextColor(getResources().getColor(R.color.mega));
		
		bRegister = (Button) findViewById(R.id.button_create_account_create);
		bLogin = (Button) findViewById(R.id.button_login_create);
		bRegister.setOnClickListener(this);
		bLogin.setOnClickListener(this);
		
//		TextView termsView = (TextView) findViewById(R.id.terms);
//		termsView.setText(R.string.tos2);
//		termsView.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				String url = "http://g.static.mega.co.nz/pages/terms.html";
//				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//				startActivity(intent);
//			}
//		});
	}
	
	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.button_create_account_create:
			onCreateAccountClick(v);
			break;
			
		case R.id.button_login_create:
			onLoginClick(v);
			break;
		}		
	}
	
	public void onLoginClick(View v){
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}
	
	public void onCreateAccountClick (View v){
		
	}
}
