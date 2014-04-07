package com.mega.android;

import java.io.File;

import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaUser;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ContactPropertiesActivity extends ActionBarActivity implements OnClickListener, MegaRequestListenerInterface {
	
	TextView nameView;
	TextView contentTextView;
	RoundedImageView imageView;
	RelativeLayout contentLayout;
	TextView contentDetailedTextView;
	TextView infoEmail;
	TextView infoAdded;
	ImageView statusImageView;
	ImageButton eyeButton;
	TableLayout contentTable;
	ActionBar aB;
	
	String userEmail;
	
	MegaApiAndroid megaApi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}
		
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
			userEmail = extras.getString("name");
			
			setContentView(R.layout.activity_contact_properties);
			nameView = (TextView) findViewById(R.id.contact_properties_name);
			imageView = (RoundedImageView) findViewById(R.id.contact_properties_image);
			imageView.getLayoutParams().width = Util.px2dp((270*scaleW), outMetrics);
			imageView.getLayoutParams().height = Util.px2dp((270*scaleW), outMetrics);
			contentLayout = (RelativeLayout) findViewById(R.id.contact_properties_content);
			contentTextView = (TextView) findViewById(R.id.contact_properties_content_text);
			contentDetailedTextView = (TextView) findViewById(R.id.contact_properties_content_detailed);
			statusImageView = (ImageView) findViewById(R.id.contact_properties_status);
			eyeButton = (ImageButton) findViewById(R.id.contact_properties_content_eye);
			contentTable = (TableLayout) findViewById(R.id.contact_properties_content_table);
//			eyeButton.setOnClickListener(this);
			contentTable.setOnClickListener(this);
			
			infoEmail = (TextView) findViewById(R.id.contact_properties_info_data_email);
			infoAdded = (TextView) findViewById(R.id.contact_properties_info_data_added);
			
			nameView.setText(userEmail);

			MegaUser contact = megaApi.getContact(userEmail);
			contentDetailedTextView.setText(getDescription(megaApi.getInShares(contact)));
			
			File avatar = new File(getCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
			Bitmap imBitmap = null;
			if (avatar.exists()){
				if (avatar.length() > 0){
					BitmapFactory.Options bOpts = new BitmapFactory.Options();
					bOpts.inPurgeable = true;
					bOpts.inInputShareable = true;
					imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
					if (imBitmap == null) {
						avatar.delete();
						megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail(), this);
					}
					else{
						imageView.setImageBitmap(imBitmap);
					}
				}
			}
			
			infoEmail.setText(userEmail);
			infoAdded.setText(contact.getTimestamp()+"");
		
//			if (position < 2){
//				statusImageView.setImageResource(R.drawable.contact_green_dot);
//			}
//			else if (position == 2){
//				statusImageView.setImageResource(R.drawable.contact_yellow_dot);
//			}
//			else if (position == 3){
//				statusImageView.setImageResource(R.drawable.contact_red_dot);
//			}
		}
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
//			case R.id.contact_properties_content_eye:{
//				Toast.makeText(this, "EYE BUTTON", Toast.LENGTH_LONG).show();
//				break;
//			}
			case R.id.contact_properties_content_table:{
				Intent i = new Intent(this, ContactFileListActivity.class);
				i.putExtra("name", userEmail);
				startActivity(i);
				finish();
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
	
	public String getDescription(NodeList nodes){
		int numFolders = 0;
		int numFiles = 0;
		
		for (int i=0;i<nodes.size();i++){
			MegaNode c = nodes.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}
		
		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			if (numFiles == 0){
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			}
			else{
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		
		return info;
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart()");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			if (e.getErrorCode() == MegaError.API_OK){
				File avatar = new File(getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				Bitmap imBitmap = null;
				if (avatar.exists()){
					if (avatar.length() > 0){
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bOpts.inPurgeable = true;
						bOpts.inInputShareable = true;
						imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
						if (imBitmap == null) {
							avatar.delete();
						}
						else{
							imageView.setImageBitmap(imBitmap);
						}
					}
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}
	
	private static void log(String log) {
		Util.log("ContactPropertiesActivity", log);
	}

}
