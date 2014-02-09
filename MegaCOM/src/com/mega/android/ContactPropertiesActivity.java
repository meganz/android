package com.mega.android;

import com.mega.components.MySwitch;
import com.mega.components.RoundedImageView;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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

}
