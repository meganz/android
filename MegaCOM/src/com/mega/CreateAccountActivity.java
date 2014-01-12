package com.mega;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class CreateAccountActivity extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_account);	
		
		TextView tos = (TextView)findViewById(R.id.tos);
		Spanned spanned = Html.fromHtml(getString(R.string.tos));
		tos.setMovementMethod(LinkMovementMethod.getInstance());
		tos.setText(spanned);
		tos.setLinkTextColor(getResources().getColor(R.color.mega));
		
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
	
	public void onLoginClick(View v){
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}
	
	public void onCreateAccountClick (View v){
		
	}
}
