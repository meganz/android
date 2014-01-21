package com.mega;

import com.mega.components.MySwitch;
import com.mega.components.RoundedImageView;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FilePropertiesActivity extends ActionBarActivity implements OnClickListener {
	
	TextView nameView;
	RoundedImageView imageView;
	RelativeLayout availableOfflineLayout;
	MySwitch availableSwitch;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null){
			int imageId = extras.getInt("imageId");
			String name = extras.getString("name");
			
			setContentView(R.layout.activity_file_properties);
			nameView = (TextView) findViewById(R.id.file_properties_name);
			imageView = (RoundedImageView) findViewById(R.id.file_properties_image);
			availableOfflineLayout = (RelativeLayout) findViewById(R.id.file_properties_available_offline);
			availableSwitch = (MySwitch) findViewById(R.id.file_properties_switch);
			availableSwitch.setChecked(true);
			availableSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
						Toast.makeText(getApplicationContext(), "HA PASADO A OFF", Toast.LENGTH_LONG).show();
					}
					else{
						Toast.makeText(getApplicationContext(), "HA PASADO A ON", Toast.LENGTH_LONG).show();
					}			
				}
			});
			
			nameView.setText(name);
			imageView.setImageResource(imageId);
		}
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		}
	}

}
