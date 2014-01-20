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

public class FilePropertiesFragment extends Fragment implements OnClickListener {

	Context context;
	ActionBar aB;
	
	TextView nameView;
	RoundedImageView imageView;
	RelativeLayout availableOfflineLayout;
	MySwitch availableSwitch;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		Bundle bundle = this.getArguments();
		int imageId = bundle.getInt("imageId");
		String name = bundle.getString("name");
		
		View v = inflater.inflate(R.layout.fragment_fileproperties, container, false);
		nameView = (TextView) v.findViewById(R.id.file_properties_name);
		imageView = (RoundedImageView) v.findViewById(R.id.file_properties_image);
		availableOfflineLayout = (RelativeLayout) v.findViewById(R.id.file_properties_available_offline);
		availableSwitch = (MySwitch) v.findViewById(R.id.file_properties_switch);
		availableSwitch.setChecked(true);
		availableSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					Toast.makeText(context, "HA PASADO A OFF", Toast.LENGTH_LONG).show();
				}
				else{
					Toast.makeText(context, "HA PASADO A ON", Toast.LENGTH_LONG).show();
				}			
			}
		});
		
		nameView.setText(name);
		imageView.setImageResource(imageId);
		
		return v;
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		}
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }

}
