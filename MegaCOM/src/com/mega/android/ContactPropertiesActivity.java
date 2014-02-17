package com.mega.android;

import com.mega.components.RoundedImageView;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ContactPropertiesActivity extends ActionBarActivity implements OnClickListener {
	
	TextView nameView;
	TextView contentTextView;
	RoundedImageView imageView;
	RelativeLayout contentLayout;
	TextView contentDetailedTextView;
	ImageView statusImageView;
	ImageButton eyeButton;
	ActionBar aB;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(true);
		aB.setDisplayShowTitleEnabled(false);
		aB.setLogo(R.drawable.ic_action_navigation_accept);

		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null){
			int imageId = extras.getInt("imageId");
			String name = extras.getString("name");
			int position = extras.getInt("position");
			
			setContentView(R.layout.activity_contact_properties);
			nameView = (TextView) findViewById(R.id.contact_properties_name);
			imageView = (RoundedImageView) findViewById(R.id.contact_properties_image);
			contentLayout = (RelativeLayout) findViewById(R.id.contact_properties_content);
			contentTextView = (TextView) findViewById(R.id.contact_properties_content_text);
			contentDetailedTextView = (TextView) findViewById(R.id.contact_properties_content_detailed);
			statusImageView = (ImageView) findViewById(R.id.contact_properties_status);
			eyeButton = (ImageButton) findViewById(R.id.contact_properties_content_eye);
			eyeButton.setOnClickListener(this);
			
			nameView.setText(name);
			imageView.setImageResource(imageId);
			contentDetailedTextView.setText("5 Folders, 10 files");
			
			if (position < 2){
				statusImageView.setImageResource(R.drawable.contact_green_dot);
			}
			else if (position == 2){
				statusImageView.setImageResource(R.drawable.contact_yellow_dot);
			}
			else if (position == 3){
				statusImageView.setImageResource(R.drawable.contact_red_dot);
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
			case R.id.contact_properties_content_eye:{
				Toast.makeText(this, "EYE BUTTON", Toast.LENGTH_LONG).show();
				break;
			}
		
		}
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

}
