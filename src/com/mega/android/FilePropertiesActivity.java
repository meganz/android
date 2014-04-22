package com.mega.android;

import com.mega.components.MySwitch;
import com.mega.components.RoundedImageView;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FilePropertiesActivity extends ActionBarActivity implements OnClickListener {
	
	TextView nameView;
	TextView availableOfflineView;
	RoundedImageView imageView;
	RelativeLayout availableOfflineLayout;
	MySwitch availableSwitch;	
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
			
			setContentView(R.layout.activity_file_properties);
			nameView = (TextView) findViewById(R.id.file_properties_name);
			imageView = (RoundedImageView) findViewById(R.id.file_properties_image);
			imageView.getLayoutParams().width = Util.px2dp((270*scaleW), outMetrics);
			imageView.getLayoutParams().height = Util.px2dp((270*scaleW), outMetrics);
			availableOfflineLayout = (RelativeLayout) findViewById(R.id.file_properties_available_offline);
			availableOfflineView = (TextView) findViewById(R.id.file_properties_available_offline_text);
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
			
			availableOfflineView.setPadding(Util.px2dp(30*scaleW, outMetrics), 0, Util.px2dp(40*scaleW, outMetrics), 0);
			
			nameView.setText(name);
			imageView.setImageResource(imageId);
		}
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_file_properties, menu);
	   
	    
		return super.onCreateOptionsMenu(menu);
	}

}
