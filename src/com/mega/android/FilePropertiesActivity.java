package com.mega.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mega.android.FileStorageActivity.Mode;
import com.mega.components.EditTextCursorWatcher;
import com.mega.components.MySwitch;
import com.mega.components.NestedListView;
import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaShare;
import com.mega.sdk.MegaUser;
import com.mega.sdk.NodeList;
import com.mega.sdk.ShareList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TableLayout;
import android.widget.TextView.OnEditorActionListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FilePropertiesActivity extends PinActivity implements OnClickListener, MegaRequestListenerInterface, OnCheckedChangeListener, MegaGlobalListenerInterface{
	
	ImageView iconView;
	TextView nameView;
	TextView availableOfflineView;
	RoundedImageView imageView;
	RelativeLayout availableOfflineLayout;
	MySwitch availableSwitchOnline;
	MySwitch availableSwitchOffline;
	ActionBar aB;
	
	TextView sizeTextView;
	TextView sizeTitleTextView;
	TextView addedTextView;
	TextView modifiedTextView;
	
	TableLayout infoTable;
	
	RelativeLayout sharedWith;
	TableLayout contactTable;	
	TableLayout sharedLayout;
	TextView contentDetailedTextView;
	TextView sharedWithTextView;
	TextView publicLinkTextView;
	TextView permissionLabel;
	TextView permissionInfo;
	ShareList sl;
	
	RelativeLayout nameLayout;
	ArrayList<MegaNode> dTreeList = null;
	
	MegaNode node;
	long handle;
	
	boolean availableOfflineBoolean = false;
	
	private MegaApiAndroid megaApi = null;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	public FilePropertiesActivity filePropertiesActivity;
	
	ProgressDialog statusDialog;
	boolean publicLink=false;
	
	private static int EDIT_TEXT_ID = 1;
	private Handler handler;
	
	private AlertDialog renameDialog;

	boolean moveToRubbish = false;
	
	public static int REQUEST_CODE_SELECT_CONTACT = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static String DB_FILE = "0";
	public static String DB_FOLDER = "1";
	
	
	MenuItem downloadMenuItem; 
	MenuItem shareFolderMenuItem;
	ImageView statusImageView;

	boolean shareIt = true;
	
	MegaSharedFolderAdapter adapter;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
	AlertDialog permissionsDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
			
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			MegaApplication app = (MegaApplication)getApplication();
			megaApi = app.getMegaApi();
		}
		
		megaApi.addGlobalListener(this);
		
		filePropertiesActivity = this;
		handler = new Handler();
		
		dbH = new DatabaseHandler(getApplicationContext());
		
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
			handle = extras.getLong("handle", -1);
			node = megaApi.getNodeByHandle(handle);
			if (node == null){  
				finish();
				return;
			}
			
			name = node.getName();
					
			setContentView(R.layout.activity_file_properties);
			iconView = (ImageView) findViewById(R.id.file_properties_icon);
			nameView = (TextView) findViewById(R.id.file_properties_name);
			imageView = (RoundedImageView) findViewById(R.id.file_properties_image);
			imageView.getLayoutParams().width = Util.px2dp((300*scaleW), outMetrics);
			imageView.getLayoutParams().height = Util.px2dp((300*scaleH), outMetrics);
			((RelativeLayout.LayoutParams) imageView.getLayoutParams()).setMargins(Util.px2dp((9*scaleW), outMetrics), Util.px2dp((9*scaleH), outMetrics), Util.px2dp((9*scaleW), outMetrics), Util.px2dp((9*scaleH), outMetrics));
							
			nameLayout = (RelativeLayout) findViewById(R.id.file_properties_name_layout);
//			((RelativeLayout.LayoutParams) imageView.getLayoutParams()).setMargins(0, 0, 0, Util.px2dp((-30*scaleH), outMetrics));
			
			availableOfflineLayout = (RelativeLayout) findViewById(R.id.file_properties_available_offline);
			availableOfflineView = (TextView) findViewById(R.id.file_properties_available_offline_text);
			availableSwitchOnline = (MySwitch) findViewById(R.id.file_properties_switch_online);
			availableSwitchOnline.setChecked(true);
			availableSwitchOffline = (MySwitch) findViewById(R.id.file_properties_switch_offline);
			availableSwitchOffline.setChecked(false);
			availableSwitchOnline.setOnCheckedChangeListener(this);			
			availableSwitchOffline.setOnCheckedChangeListener(this);			
			
			sizeTitleTextView  = (TextView) findViewById(R.id.file_properties_info_menu_size);
			sizeTextView = (TextView) findViewById(R.id.file_properties_info_data_size);
			addedTextView = (TextView) findViewById(R.id.file_properties_info_data_added);
			modifiedTextView = (TextView) findViewById(R.id.file_properties_info_data_modified);
						
			imageView.setImageResource(imageId);
			nameView.setText(name);
			nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			nameView.setSingleLine();
			nameView.setTypeface(null, Typeface.BOLD);
			
			iconView.getLayoutParams().height = Util.px2dp((20*scaleH), outMetrics);
			iconView.setImageResource(imageId);
			((RelativeLayout.LayoutParams)iconView.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((15*scaleH), outMetrics), 0, 0);
			
			sharedWith = (RelativeLayout) findViewById(R.id.contacts_shared_with_eye);
			sharedLayout= (TableLayout) findViewById(R.id.file_properties_content_table);
			sharedLayout.setOnClickListener(this);			
			
//			((RelativeLayout.LayoutParams)sharedWith.getLayoutParams()).setMargins(0, Util.px2dp((-40*scaleH), outMetrics), 0, 0);
			//sharedWithText = (TextView) findViewById(R.id.public_link);
			//((RelativeLayout.LayoutParams)sharedWithText.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((15*scaleH), outMetrics), 0, Util.px2dp((15*scaleH), outMetrics));
			//sharedWithList = (NestedListView) findViewById(R.id.file_properties_shared_folder_shared_with_list);
			
			publicLinkTextView = (TextView) findViewById(R.id.file_properties_public_link);								
			sharedWithTextView = (TextView) findViewById(R.id.shared_with_detailed);
			
			permissionLabel = (TextView) findViewById(R.id.file_properties_permission_label);				
			permissionInfo = (TextView) findViewById(R.id.file_properties_permission_info);
			permissionLabel.setVisibility(View.GONE);
			permissionInfo.setVisibility(View.GONE);
									
			infoTable = (TableLayout) findViewById(R.id.file_properties_info_table);

			File destination = null;
			File offlineFile = null;
			
			availableOfflineLayout.setVisibility(View.VISIBLE);	
			((RelativeLayout.LayoutParams)infoTable.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.file_properties_available_offline);
			
			if (node.isFile()){				
				
				sharedWith.setVisibility(View.GONE);				
				
				sizeTitleTextView.setText(getString(R.string.file_properties_info_size_file));
				
				sizeTextView.setText(Formatter.formatFileSize(this, node.getSize()));
				
				
				//Choose the button availableSwitch
				
				if(dbH.exists(node.getHandle())){
					availableOfflineBoolean = true;
					availableSwitchOffline.setVisibility(View.VISIBLE);
					availableSwitchOnline.setVisibility(View.GONE);
				}
				else{
					availableOfflineBoolean = false;
					availableSwitchOffline.setVisibility(View.GONE);
					availableSwitchOnline.setVisibility(View.VISIBLE);
				}		
				
				availableOfflineView.setPadding(Util.px2dp(30*scaleW, outMetrics), 0, Util.px2dp(40*scaleW, outMetrics), 0);
				
				if (node.getCreationTime() != 0){
					try {addedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{addedTextView.setText("");}

					if (node.getModificationTime() != 0){
						try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getModificationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}
					}
					else{
						try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}	
					}
				}
				else{
					addedTextView.setText("");
					modifiedTextView.setText("");
				}
			}
			else{ //Folder
				
				
				//Choose the button availableSwitch
				
				if(dbH.exists(node.getHandle())){
					availableOfflineBoolean = true;
					availableSwitchOffline.setVisibility(View.VISIBLE);
					availableSwitchOnline.setVisibility(View.GONE);
				}
				else{
					availableOfflineBoolean = false;
					availableSwitchOffline.setVisibility(View.GONE);
					availableSwitchOnline.setVisibility(View.VISIBLE);
				}
				
				availableOfflineView.setPadding(Util.px2dp(30*scaleW, outMetrics), 0, Util.px2dp(40*scaleW, outMetrics), 0);
				
//				availableSwitchOffline.setVisibility(View.GONE);
//				availableSwitchOnline.setVisibility(View.VISIBLE);
				
//				destination = null;
//				if (Environment.getExternalStorageDirectory() != null){
//					destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/");
//					log("DESTINATION: "+destination);
//				}
//				else{
//					destination = new File(getFilesDir(), node.getHandle()+"");
//					log("DESTINATION: "+destination);
//				}
//
//				if (destination.exists() && destination.isDirectory()){
//					log("DESTINATION OK");
//					log(node.getName());
//					offlineFile = new File(destination, node.getName());
//					
//					if (offlineFile.exists()){
//							
//						if (offlineFile.getName().equals(node.getName())){
//							availableOfflineBoolean = true;
//							availableSwitchOffline.setVisibility(View.VISIBLE);
//							availableSwitchOnline.setVisibility(View.GONE);
//						}
//					}	
//					else{						
//						availableOfflineBoolean = false;
//						availableSwitchOffline.setVisibility(View.GONE);
//						availableSwitchOnline.setVisibility(View.VISIBLE);
//						removeOffline();
//						supportInvalidateOptionsMenu();
//					}
//				}
				sl = megaApi.getOutShares(node);		

				if (sl != null){

					if (sl.size() == 0){						
						sharedWith.setVisibility(View.GONE);
						
						if (megaApi.checkAccess(node, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK){
							
							permissionLabel.setVisibility(View.GONE);
							permissionInfo.setVisibility(View.GONE);
							//permissionInfo.setText(getResources().getString(R.string.file_properties_owner));
							
						}
						else{	
							permissionLabel.setVisibility(View.VISIBLE);
							permissionInfo.setVisibility(View.VISIBLE);
							
							int accessLevel= megaApi.getAccess(node);							
							
							switch(accessLevel){
								case MegaShare.ACCESS_FULL:{
									permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_full_access));
								}
								case MegaShare.ACCESS_READ:{
									permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_only));							
								}						
								case MegaShare.ACCESS_READWRITE:{								
									permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_write));	
								}
							}
						}
						
					}
					else{		
						publicLink=false;
						for(int i=0; i<sl.size();i++){

							//Check if one of the ShareNodes is the public link

							if(sl.get(i).getUser()==null){
								//Public link + users								
								publicLink=true;
								break;

							}
						}
						if(publicLink){
							publicLinkTextView.setText(getResources().getString(R.string.file_properties_shared_folder_public_link));							

							publicLinkTextView.setVisibility(View.VISIBLE);
							sharedWith.setVisibility(View.VISIBLE);
							sharedWithTextView.setText(sl.size()-1+" "+getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
						}
						else{
							publicLinkTextView.setText(getResources().getString(R.string.file_properties_shared_folder_private_folder));		
							sharedWith.setVisibility(View.VISIBLE);
							sharedWithTextView.setText(sl.size()+" "+getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
						}

						imageView.setImageResource(imageId);
						iconView.setImageResource(imageId);
						sizeTitleTextView.setText(getString(R.string.file_properties_info_size_folder));

						sizeTextView.setText(getInfoFolder(node));
					}
					
					
					if (node.getCreationTime() != 0){
						try {addedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{addedTextView.setText("");}

						if (node.getModificationTime() != 0){
							try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getModificationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}
						}
						else{
							try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}	
						}
					}
					else{
						addedTextView.setText("");
						modifiedTextView.setText("");
					}
				}
				Bitmap thumb = null;
				Bitmap preview = null;
				//If image
				if (node.isFile()){
					if (node.hasThumbnail()){
						if (availableOfflineBoolean){
							if (offlineFile != null){

								BitmapFactory.Options options = new BitmapFactory.Options();
								options.inJustDecodeBounds = true;
								thumb = BitmapFactory.decodeFile(offlineFile.getAbsolutePath(), options);

								ExifInterface exif;
								int orientation = ExifInterface.ORIENTATION_NORMAL;
								try {
									exif = new ExifInterface(offlineFile.getAbsolutePath());
									orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
								} catch (IOException e) {}  

								// Calculate inSampleSize
								options.inSampleSize = Util.calculateInSampleSize(options, 270, 270);

								// Decode bitmap with inSampleSize set
								options.inJustDecodeBounds = false;

								thumb = BitmapFactory.decodeFile(offlineFile.getAbsolutePath(), options);
								if (thumb != null){
									thumb = Util.rotateBitmap(thumb, orientation);

									imageView.setImageBitmap(thumb);
								}
							}
						}
						else{
							thumb = ThumbnailUtils.getThumbnailFromCache(node);
							if (thumb != null){
								imageView.setImageBitmap(thumb);
							}
							else{
								thumb = ThumbnailUtils.getThumbnailFromFolder(node, this);
								if (thumb != null){
									imageView.setImageBitmap(thumb);
								}
							}
						}
					}
				}
			}

		}
	}
		
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
			case R.id.file_properties_shared_folder_shared_with_text:{
				Intent i = new Intent(this, FileContactListActivity.class);
				i.putExtra("name", node.getHandle());
				startActivity(i);
				finish();
				break;
			}
			case R.id.file_properties_content_table:{			
				Intent i = new Intent(this, FileContactListActivity.class);
				i.putExtra("name", node.getHandle());
				startActivity(i);
				finish();
				break;
			}
			
		}
	}	

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked){
			availableOfflineBoolean = false;
			availableSwitchOffline.setVisibility(View.GONE);
			availableSwitchOnline.setVisibility(View.VISIBLE);
			availableSwitchOffline.setChecked(false);			
			removeOffline();			
			supportInvalidateOptionsMenu();
		}
		else{	
									
			availableOfflineBoolean = true;
			availableSwitchOffline.setVisibility(View.VISIBLE);
			availableSwitchOnline.setVisibility(View.GONE);
			availableSwitchOnline.setChecked(true);			
			saveOffline();
			supportInvalidateOptionsMenu();
//			MegaOffline mOff = null;
//			MegaNode parentNode=megaApi.getParentNode(node);
//			if(node.isFile()){
//				mOff = new MegaOffline(Long.toString(node.getHandle()), createStringTree(), node.getName(), Long.toString(parentNode.getHandle()), DB_FILE);
//			}
//			else {
//				mOff = new MegaOffline(Long.toString(node.getHandle()), createStringTree(), node.getName(), Long.toString(parentNode.getHandle()), DB_FOLDER);
//			}
//			
//			dbH.setOfflineFile(mOff);
		}		
	}
	
	public void saveOffline (){
		log("saveOffline");

		File destination = null;
		if (Environment.getExternalStorageDirectory() != null){
			destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/"+createStringTree());
		}
		else{
			destination = getFilesDir();
		}

		if (destination.exists() && destination.isDirectory()){
			File offlineFile = new File(destination, node.getName());
			if (offlineFile.exists() && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getName())){ //This means that is already available offline
				return;
			}
		}

		destination.mkdirs();

		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(destination.getAbsolutePath());
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}

		Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
		if (node.getType() == MegaNode.TYPE_FOLDER) {
			log("saveOffline:isFolder");
			getDlList(dlFiles, node, new File(destination, new String(node.getName())));
		} else {
			log("saveOffline:isFile");
			dlFiles.put(node, destination.getAbsolutePath());			
		}

		for (MegaNode document : dlFiles.keySet()) {

			String path = dlFiles.get(document);			

			if(availableFreeSpace <document.getSize()){
				Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, this);
				continue;
			}

			String url = null;
			Intent service = new Intent(this, DownloadService.class);
			service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
			service.putExtra(DownloadService.EXTRA_URL, url);
			service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
			service.putExtra(DownloadService.EXTRA_PATH, path);
			service.putExtra(DownloadService.EXTRA_OFFLINE, true);
			startService(service);					
		}
		
		insertDB();		
	}

	public void removeOffline(){
		if (node.isFile()){
			log("removeOffline - file");
			File destination = null;
			if (Environment.getExternalStorageDirectory() != null){
				destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/");
			}
//			if (getExternalFilesDir(null) != null){
//				destination = new File (getExternalFilesDir(null), node.getHandle()+"");
//			}
			else{
				destination = new File(getFilesDir(), node.getHandle()+"");
			}
			
			try{
				File offlineFile = new File(destination, node.getHandle() + "_" + node.getName());
				Util.deleteFolderAndSubfolders(this, offlineFile);
			}
			catch(Exception e){
				log("EXCEPTION: removeOffline - file");
			};	
		}
		else if (node.isFolder()){
			log("removeOffline - folder");
			File destination = null;
			if (Environment.getExternalStorageDirectory() != null){
				destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/");
			}
//			if (getExternalFilesDir(null) != null){
//				destination = new File (getExternalFilesDir(null), node.getHandle()+"");
//			}
			else{
				destination = new File(getFilesDir(), node.getHandle()+"");
			}
			
			try{
				File offlineFile = new File(destination, node.getName());
				Util.deleteFolderAndSubfolders(this, offlineFile);
			}
			catch(Exception e){
				log("EXCEPTION: removeOffline - folder");
			};	
			
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
		    case R.id.action_file_properties_download:{
		    	if (!availableOfflineBoolean){
			    	ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(node.getHandle());
					downloadNode(handleList);
		    	}
		    	else{
		    		
		    		File destination = null;
					File offlineFile = null;
					if (Environment.getExternalStorageDirectory() != null){
						destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/");
					}
//					if (getExternalFilesDir(null) != null){
//						destination = new File (getExternalFilesDir(null), node.getHandle()+"");
//					}
					else{
						destination = new File(getFilesDir(), node.getHandle()+"");
					}
					
					if (destination.exists() && destination.isDirectory()){
						offlineFile = new File(destination, node.getHandle() + "_" + node.getName());
						if (offlineFile.exists() && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getHandle() + "_" + node.getName())){ //This means that is already available offline
							availableOfflineBoolean = true;
							availableSwitchOffline.setVisibility(View.VISIBLE);
							availableSwitchOnline.setVisibility(View.GONE);
						}
						else{
							availableOfflineBoolean = false;
							removeOffline();
							supportInvalidateOptionsMenu();
						}
					}
					else{
						availableOfflineBoolean = false;
						removeOffline();
						supportInvalidateOptionsMenu();
					}
		    		Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(offlineFile), MimeType.typeForName(offlineFile.getName()).getType());
					if (isIntentAvailable(this, intent)){
						startActivity(intent);
					}
					else{
						Toast.makeText(this, "There is not any app installed in the device to open this file", Toast.LENGTH_LONG).show();
					}
		    	}
				return true;
		    }
		    case R.id.action_file_properties_share_folder:{
		    	
		    	Intent intent = new Intent(ContactsExplorerActivity.ACTION_PICK_CONTACT_SHARE_FOLDER);
		    	intent.setClass(this, ContactsExplorerActivity.class);
		    	intent.putExtra(ContactsExplorerActivity.EXTRA_NODE_HANDLE, node.getHandle());
		    	startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
		    	break;
		    }
		    case R.id.action_file_properties_get_link:{
		    	shareIt = false;
		    	getPublicLinkAndShareIt();
		    	return true;
		    }
		    case R.id.action_file_properties_send_link:{
		    	shareIt = true;
		    	getPublicLinkAndShareIt();
		    	return true;
		    }
		    case R.id.action_file_properties_rename:{
		    	showRenameDialog();
		    	return true;
		    }
		    case R.id.action_file_properties_remove:{
		    	moveToTrash();
		    	return true;
		    }
		    case R.id.action_file_properties_move:{
		    	showMove();
		    	return true;
		    }
		    case R.id.action_file_properties_copy:{
		    	showCopy();
		    	return true;
		    }
		}	    
	    return super.onOptionsItemSelected(item);
	}
	
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}
	
	public void showCopy(){
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());
		
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}
	
	public void showMove(){
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());
		
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}

	public void downloadNode(ArrayList<Long> handleList){
		
		long size = 0;
		long[] hashes = new long[handleList.size()];
		for (int i=0;i<handleList.size();i++){
			hashes[i] = handleList.get(i);
			size += megaApi.getNodeByHandle(hashes[i]).getSize();
		}
		
		if (dbH == null){
			dbH = new DatabaseHandler(getApplicationContext());
		}
		
		boolean askMe = true;
		String downloadLocationDefaultPath = "";
		prefs = dbH.getPreferences();		
		if (prefs != null){
			if (prefs.getStorageAskAlways() != null){
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							askMe = false;
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}		
			
		if (askMe){
			Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
			intent.putExtra(FileStorageActivity.EXTRA_SIZE, size);
			intent.setClass(this, FileStorageActivity.class);
			intent.putExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES, hashes);
			startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);	
		}
		else{
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}
	}
	
	public void moveToTrash(){
		
		long handle = node.getHandle();
		moveToRubbish = false;
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		MegaNode rubbishNode = megaApi.getRubbishNode();

		//Check if the node is not yet in the rubbish bin (if so, remove it)
		MegaNode parent = megaApi.getNodeByHandle(handle);
		while (megaApi.getParentNode(parent) != null){
			parent = megaApi.getParentNode(parent);
		}
			
		if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
			moveToRubbish = true;
			megaApi.moveNode(megaApi.getNodeByHandle(handle), rubbishNode, this);
		}
		else{
			megaApi.remove(megaApi.getNodeByHandle(handle), this);
		}
		
		if (moveToRubbish){
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_move_to_trash));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
		}
		else{
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_delete_from_mega));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
		}
	}
	
	public void showRenameDialog(){
		
		final EditTextCursorWatcher input = new EditTextCursorWatcher(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setText(node.getName());

		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),
				KeyEvent.KEYCODE_ENTER);
		
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, boolean hasFocus) {
				if (hasFocus) {
					if (node.isFolder()){
						input.setSelection(0, input.getText().length());
					}
					else{
						String [] s = node.getName().split("\\.");
						if (s != null){
							int numParts = s.length;
							int lastSelectedPos = 0;
							if (numParts == 1){
								input.setSelection(0, input.getText().length());
							}
							else if (numParts > 1){
								for (int i=0; i<(numParts-1);i++){
									lastSelectedPos += s[i].length(); 
									lastSelectedPos++;
								}
								lastSelectedPos--; //The last point should not be selected)
								input.setSelection(0, lastSelectedPos);
							}
						}
						showKeyboardDelayed(v);
					}
				}
			}
		});

		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_rename) + " "	+ new String(node.getName()), null, input);
		builder.setPositiveButton(getString(R.string.context_rename),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						rename(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		renameDialog = builder.create();
		renameDialog.show();

		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					renameDialog.dismiss();
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					rename(value);
					return true;
				}
				return false;
			}
		});
	}
	
	private void rename(String newName){
		if (newName.equals(node.getName())) {
			return;
		}
		
		if(!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if (isFinishing()){
			return;
		}
		
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_renaming));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		
		log("renaming " + node.getName() + " to " + newName);
		
		megaApi.renameNode(node, newName, this);
	}
	
	public void getPublicLinkAndShareIt(){
		
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_creating_link));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		
		megaApi.exportNode(node, this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_file_properties, menu);
	   
	    downloadMenuItem = menu.findItem(R.id.action_file_properties_download);
	    shareFolderMenuItem = menu.findItem(R.id.action_file_properties_share_folder);
	    
//	    if (node.isFolder()){
	    	shareFolderMenuItem.setVisible(true);
//	    }
//	    else{
//	    	shareFolderMenuItem.setVisible(false);
//	    }
	    
	    if (availableOfflineBoolean){
	    	downloadMenuItem.setIcon(R.drawable.ic_action_collections_collection_dark);
	    }
	    else{
	    	downloadMenuItem.setIcon(R.drawable.ic_menu_download_dark);
	    }
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		
		node = megaApi.getNodeByHandle(request.getNodeHandle());
		
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_EXPORT){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				String link = request.getLink();
				ShareList sl = megaApi.getOutShares(node);
				if (filePropertiesActivity != null){
					if (shareIt){
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.setType("text/plain");
						intent.putExtra(Intent.EXTRA_TEXT, link);
						startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
					}
					else{
						if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    clipboard.setText(link);
						} else {
						    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", link);
				            clipboard.setPrimaryClip(clip);
						}
						
						Toast.makeText(this, getString(R.string.file_properties_get_link), Toast.LENGTH_LONG).show();
					}
				}
			}
			else{
				Toast.makeText(this, "Impossible to get the link", Toast.LENGTH_LONG).show();
			}
			log("export request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Correctly renamed", Toast.LENGTH_SHORT).show();
				nameView.setText(megaApi.getNodeByHandle(request.getNodeHandle()).getName());
			}			
			else{
				Toast.makeText(this, "The file has not been renamed", Toast.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, "Correctly moved to Rubbish bin", Toast.LENGTH_SHORT).show();
					finish();
				}
				else{
					Toast.makeText(this, "The file has not been removed", Toast.LENGTH_LONG).show();
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, "Correctly moved", Toast.LENGTH_SHORT).show();
					finish();
				}
				else{
					Toast.makeText(this, "The file has not been moved", Toast.LENGTH_LONG).show();
				}
				log("move nodes request finished");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			
			
			if (e.getErrorCode() == MegaError.API_OK){
				if (statusDialog.isShowing()){
					try { 
						statusDialog.dismiss();	
					} 
					catch (Exception ex) {}
					Toast.makeText(this, "Correctly deleted from MEGA", Toast.LENGTH_SHORT).show();
				}
				finish();
			}
			else{
				Toast.makeText(this, "The file has not been removed", Toast.LENGTH_LONG).show();
			}
			log("remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				if (request.getEmail() != null){
					Toast.makeText(this, "File sent to: " + request.getEmail(), Toast.LENGTH_LONG).show();
				}
				else{
					Toast.makeText(this, "Correctly copied", Toast.LENGTH_SHORT).show();
				}
			}
			else{
				Toast.makeText(this, "The file has not been copied", Toast.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		}
		if (request.getType() == MegaRequest.TYPE_SHARE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "The folder has been shared correctly", Toast.LENGTH_LONG).show();
				ShareList sl = megaApi.getOutShares(node);
			}
			else{
				Util.showErrorAlertDialog(e, this);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (intent == null) {
			return;
		}
		
		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivity.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivity.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);

			
			downloadTo (parentPath, url, size, hashes);
			Util.showToast(this, R.string.download_began);
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {
			
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);
			final int totalMoves = moveHandles.length;
			
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			moveToRubbish = false;
			
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_moving));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
			
			for(int i=0; i<moveHandles.length;i++){
				megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, this);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;
			
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_copying));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
			
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for(int i=0; i<copyHandles.length;i++){
				megaApi.copyNode(megaApi.getNodeByHandle(copyHandles[i]), parent, this);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			final ArrayList<String> emails = intent.getStringArrayListExtra(ContactsExplorerActivity.EXTRA_CONTACTS);
			final long nodeHandle = intent.getLongExtra(ContactsExplorerActivity.EXTRA_NODE_HANDLE, -1);
			
			if (node.isFolder()){
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
				dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						ProgressDialog temp = null;
						try{
							temp = new ProgressDialog(filePropertiesActivity);
							temp.setMessage(getString(R.string.context_sharing_folder));
							temp.show();
						}
						catch(Exception e){
							return;
						}
						statusDialog = temp;
						permissionsDialog.dismiss();
						
						switch(item) {
		                    case 0:{
		                    	for (int i=0;i<emails.size();i++){
		                    		megaApi.share(node, emails.get(i), MegaShare.ACCESS_READ, filePropertiesActivity);
		                    	}
		                    	break;
		                    }
		                    case 1:{
		                    	for (int i=0;i<emails.size();i++){
		                    		megaApi.share(node, emails.get(i), MegaShare.ACCESS_READWRITE, filePropertiesActivity);
		                    	}
		                        break;
		                    }
		                    case 2:{
		                    	for (int i=0;i<emails.size();i++){
		                    		megaApi.share(node, emails.get(i), MegaShare.ACCESS_FULL, filePropertiesActivity);
		                    	}		                    	
		                        break;
		                    }
		                }
					}
				});
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
				Resources resources = permissionsDialog.getContext().getResources();
				int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
				TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
		        alertTitle.setTextColor(resources.getColor(R.color.mega));
				int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
				View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
				titleDivider.setBackgroundColor(resources.getColor(R.color.mega));
			}
			else{ 
				for (int i=0;i<emails.size();i++){
					megaApi.sendFileToUser(node, emails.get(i), filePropertiesActivity);
				}
			}
			
		}
	}
	
	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
		
		if (megaApi.getRootNode() == null)
			return;
		
		folder.mkdir();
		NodeList nodeList = megaApi.getChildren(parent);
		for(int i=0; i<nodeList.size(); i++){
			MegaNode document = nodeList.get(i);
			if (document.getType() == MegaNode.TYPE_FOLDER) {
				File subfolder = new File(folder, new String(document.getName()));
				getDlList(dlFiles, document, subfolder);
			} 
			else {
				dlFiles.put(document, folder.getAbsolutePath());
			}
		}
	}
	
	/*
	 * If there is an application that can manage the Intent, returns true. Otherwise, false.
	 */
	public static boolean isIntentAvailable(Context ctx, Intent intent) {

		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	private String getInfoFolder (MegaNode n){
		NodeList nL = megaApi.getChildren(n);
		
		int numFolders = 0;
		int numFiles = 0;
		
		for (int i=0;i<nL.size();i++){
			MegaNode c = nL.get(i);
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
			info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_files, numFiles);
		}
		
		return info;
	}
	
	private String createStringTree (){
		log("createStringTree");
		dTreeList = new ArrayList<MegaNode>();
		MegaNode parentNode = null;
		MegaNode nodeTemp = node;
		StringBuilder dTree = new StringBuilder();
		String s;
		
		dTreeList.add(node);
		parentNode=megaApi.getParentNode(nodeTemp);
		
		while (parentNode.getType() != MegaNode.TYPE_ROOT){
			dTreeList.add(parentNode);
			dTree.insert(0, parentNode.getName()+"/");	
			nodeTemp=parentNode;
			parentNode=megaApi.getParentNode(nodeTemp);
			
		}		
		
		if(dTree.length()>0){
			s = dTree.toString();
		}
		else{
			s=null;
		}
							
		return s;
	}
	
	
	private void insertDB (){
		log("insertDB");
		MegaNode nodeToInsert = null;
		MegaNode parentNode = null;
		long parentHandle;		
		String path = "/";		
		
		if(dTreeList!=null){			
			
			log("dTreeList SIZE--: "+dTreeList.size());
			MegaOffline mOffParent=null;
			
			for(int i=dTreeList.size()-1; i>=0;i--){
						
				nodeToInsert = dTreeList.get(i);
				parentNode = megaApi.getParentNode(nodeToInsert);
				
				if(parentNode.getType() != MegaNode.TYPE_ROOT){
					log("PARENT NODE nooot ROOT");
					/************PARENT NODE not ROOT************/
					path = path + parentNode.getName() + "/";
					
					//Get the id of the parentHandle in the DB					
					mOffParent = dbH.findByHandle(parentNode.getHandle());
					
					//Insert the node in the DB		
					if(mOffParent!=null){
						if(nodeToInsert.isFile()){
							MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FILE);
							dbH.setOfflineFile(mOffInsert);
						}
						else{
							MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FOLDER);
							dbH.setOfflineFile(mOffInsert);
						}						
					}
					else{
						log("insertDB : mOff==null");
					}					
					
				}
				else{
					/************ PARENT NODE is ROOT************/
					log("PARENT NODE is ROOT");
					path = "/";
					if(nodeToInsert.isFile()){
						MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), -1, DB_FILE);
						dbH.setOfflineFile(mOffInsert);
					}
					else{
						MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), -1, DB_FOLDER);
						dbH.setOfflineFile(mOffInsert);
					}
				}
			}			
		}
		else{
			log("insertDB: dTreeList==null");
		}
	}
	
	@Override
	public void onUsersUpdate(MegaApiJava api) {
		log("onUsersUpdate");		
	}

	@Override
	public void onNodesUpdate(MegaApiJava api) {
		log("onNodesUpdate");
		
		if (node.isFolder()){
			int imageId = R.drawable.mime_folder;
			sl = megaApi.getOutShares(node);		

			if (sl != null){

				if (sl.size() == 0){						
					sharedWith.setVisibility(View.GONE);
					((RelativeLayout.LayoutParams)infoTable.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.file_properties_image);
					
					permissionLabel.setVisibility(View.VISIBLE);
					permissionInfo.setVisibility(View.VISIBLE);

					int accessLevel= megaApi.getAccess(node);							
					
					switch(accessLevel){
						case MegaShare.ACCESS_FULL:{
							permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_full_access));
						}
						case MegaShare.ACCESS_READ:{
							permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_only));							
						}						
						case MegaShare.ACCESS_READWRITE:{								
							permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_write));	
						}
					}
				}
				else{		
					publicLink=false;
					for(int i=0; i<sl.size();i++){

						//Check if one of the ShareNodes is the public link

						if(sl.get(i).getUser()==null){
							//Public link + users								
							publicLink=true;
							break;

						}
					}
					if(publicLink){
						publicLinkTextView.setText(getResources().getString(R.string.file_properties_shared_folder_public_link));							

						publicLinkTextView.setVisibility(View.VISIBLE);
						sharedWith.setVisibility(View.VISIBLE);
						sharedWithTextView.setText(sl.size()-1+" "+getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
						((RelativeLayout.LayoutParams)infoTable.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.contacts_shared_with_eye);
					}
					else{
						publicLinkTextView.setText(getResources().getString(R.string.file_properties_shared_folder_private_folder));		
						sharedWith.setVisibility(View.VISIBLE);
						sharedWithTextView.setText(sl.size()+" "+getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
						((RelativeLayout.LayoutParams)infoTable.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.contacts_shared_with_eye);		

					}

					imageView.setImageResource(imageId);
					iconView.setImageResource(imageId);
					sizeTitleTextView.setText(getString(R.string.file_properties_info_size_folder));

					sizeTextView.setText(getInfoFolder(node));
				}


				if (node.getCreationTime() != 0){
					try {addedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{addedTextView.setText("");}

					if (node.getModificationTime() != 0){
						try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getModificationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}
					}
					else{
						try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}	
					}
				}
				else{
					addedTextView.setText("");
					modifiedTextView.setText("");
				}
			}
			imageView.setImageResource(imageId);
			iconView.setImageResource(imageId);
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		log("onReloadNeeded");
	}
	
	@Override
	protected void onDestroy(){
    	super.onDestroy();
    	
    	megaApi.removeGlobalListener(this);
    }

	public static void log(String message) {
		Util.log("FilePropertiesActivity", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
	
	public void downloadTo(String parentPath, String url, long size, long [] hashes){
		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}
		
		
		if (hashes == null){
			if(url != null) {
				if(availableFreeSpace < size) {
					Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space), false, this);
					return;
				}
				
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				startService(service);
			}
		}
		else{
			if(hashes.length == 1){
				MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);
				if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
					log("ISFILE");
					String localPath = Util.getLocalFile(this, tempNode.getName(), tempNode.getSize(), parentPath);
					if(localPath != null){	
						try { 
							Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName())); 
						}
						catch(Exception e) {}
						
						Intent viewIntent = new Intent(Intent.ACTION_VIEW);
						viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeType.typeForName(tempNode.getName()).getType());
						if (isIntentAvailable(this, viewIntent))
							startActivity(viewIntent);
						else{
							Intent intentShare = new Intent(Intent.ACTION_SEND);
							intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeType.typeForName(tempNode.getName()).getType());
							if (isIntentAvailable(this, intentShare))
								startActivity(intentShare);
							String toastMessage = getString(R.string.already_downloaded) + ": " + localPath;
							Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
						}								
						return;
					}
				}
			}
			
			for (long hash : hashes) {
				MegaNode node = megaApi.getNodeByHandle(hash);
				if(node != null){
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					if (node.getType() == MegaNode.TYPE_FOLDER) {
						getDlList(dlFiles, node, new File(parentPath, new String(node.getName())));
					} else {
						dlFiles.put(node, parentPath);
					}
					
					for (MegaNode document : dlFiles.keySet()) {
						
						String path = dlFiles.get(document);
						
						if(availableFreeSpace < document.getSize()){
							Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, this);
							continue;
						}
						
						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
						service.putExtra(DownloadService.EXTRA_URL, url);
						service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						startService(service);
					}
				}
				else if(url != null) {
					if(availableFreeSpace < size) {
						Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space), false, this);
						continue;
					}
					
					Intent service = new Intent(this, DownloadService.class);
					service.putExtra(DownloadService.EXTRA_HASH, hash);
					service.putExtra(DownloadService.EXTRA_URL, url);
					service.putExtra(DownloadService.EXTRA_SIZE, size);
					service.putExtra(DownloadService.EXTRA_PATH, parentPath);
					startService(service);
				}
				else {
					log("node not found");
				}
			}
		}
	}
}
