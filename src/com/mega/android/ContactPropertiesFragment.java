package com.mega.android;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.mega.android.utils.Util;
import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaUser;

public class ContactPropertiesFragment extends Fragment implements OnClickListener, MegaRequestListenerInterface {

	RoundedImageView imageView;
	RelativeLayout contentLayout;
	TextView userNameTextView;
	TextView infoEmail;
	TableLayout contentTable;
	Button sharedFoldersButton;	
	String userEmail;	
	Context context;
	ActionBar aB;
	//	private ListView overflowMenuList;
	private boolean overflowVisible = false; 
	MegaApiAndroid megaApi;
	MegaUser contact;

	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
		log("onCreate");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}

		aB.setTitle(R.string.contact_properties_activity);
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = null;

		if (userEmail != null){
			v = inflater.inflate(R.layout.fragment_contact_properties, container, false);

			//			layoutTop = (RelativeLayout) v.findViewById(R.id.contact_properties_layout_top);
			//			closeImage = (ImageView) v.findViewById(R.id.contact_properties_close_icon);
			//			overflowImage = (ImageView) v.findViewById(R.id.contact_properties_overflow);
			//			
			//			overflowImage.setOnClickListener(this);
			//			closeImage.setOnClickListener(this);
			//			
			imageView = (RoundedImageView) v.findViewById(R.id.contact_properties_image);
			imageView.getLayoutParams().width = Util.px2dp((270*scaleW), outMetrics);
			imageView.getLayoutParams().height = Util.px2dp((270*scaleW), outMetrics);
			contentTable = (TableLayout) v.findViewById(R.id.contact_properties_content_table);
			userNameTextView = (TextView) v.findViewById(R.id.contact_properties_name);
			infoEmail = (TextView) v.findViewById(R.id.contact_properties_email);
			//			contentLayout = (RelativeLayout) v.findViewById(R.id.contact_properties_content);
			//			contentTextView = (TextView) v.findViewById(R.id.contact_properties_content_text);
			//			contentTextView = (TextView) v.findViewById(R.id.contact_properties_content_detailed);
			//			eyeButton = (ImageButton) v.findViewById(R.id.contact_properties_content_eye);

			//			eyeButton.setOnClickListener(this);
			sharedFoldersButton = (Button) v.findViewById(R.id.shared_folders_button);
			sharedFoldersButton.setOnClickListener(this);


			infoEmail.setText(userEmail);
			userNameTextView.setText(userEmail);

			//			infoAdded = (TextView) v.findViewById(R.id.contact_properties_info_data_added);

			contact = megaApi.getContact(userEmail);
			//			contentTextView.setText(getDescription(megaApi.getInShares(contact)));
			sharedFoldersButton.setText(getDescription(megaApi.getInShares(contact)));


			String menuOptions[] = new String[2];
			menuOptions[0] = getString(R.string.context_share_folder);
			menuOptions[1] = getString(R.string.context_view_shared_folders);

			//			overflowMenuList = (ListView) v.findViewById(R.id.contact_properties_overflow_menu_list);
			//			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, menuOptions);
			//			overflowMenuList.setAdapter(arrayAdapter);
			//			overflowMenuList.setOnItemClickListener(this);
			//			if (overflowVisible){
			//				overflowMenuList.setVisibility(View.VISIBLE);	
			//			}
			//			else{
			//				overflowMenuList.setVisibility(View.GONE);
			//			}			

			File avatar = null;
			if (context.getExternalCacheDir() != null){
				avatar = new File(context.getExternalCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
			}
			else{
				avatar = new File(context.getCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
			}

			Bitmap imBitmap = null;
			if (avatar.exists()){
				if (avatar.length() > 0){
					BitmapFactory.Options bOpts = new BitmapFactory.Options();
					bOpts.inPurgeable = true;
					bOpts.inInputShareable = true;
					imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
					if (imBitmap == null) {
						avatar.delete();
						if (context.getExternalCacheDir() != null){
							megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail(), this);
						}
						else{
							megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail(), this);
						}
					}
					else{
						imageView.setImageBitmap(imBitmap);
					}
				}
			}
			//infoAdded.setText(contact.getTimestamp()+"");
		}

		return v;
	}

	public void setUserEmail(String userEmail){
		this.userEmail = userEmail;
	}

	public String getUserEmail(){
		return this.userEmail;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((ActionBarActivity)activity).getSupportActionBar();
	}	

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.shared_folders_button:{
			((ContactPropertiesMainActivity)context).onContentClick(userEmail);
			break;
		}
		}
	}

	public String getDescription(ArrayList<MegaNode> nodes){
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
			info = numFolders +  " " + getResources().getQuantityString(R.plurals.general_num_shared_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + getResources().getQuantityString(R.plurals.general_num_shared_folders, numFiles);
			}
		}
		else {
			if (numFiles == 0){
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_shared_folders, numFolders);
			}
			else{
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_shared_folders, numFiles);
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
				File avatar = null;
				if (context.getExternalCacheDir() != null){
					avatar = new File(context.getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				}
				else{
					avatar = new File(context.getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				}
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

	public static void log(String log) {
		Util.log("ContactPropertiesFragment", log);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}
}
