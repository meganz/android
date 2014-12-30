package nz.mega.android;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.mega.android.FileStorageActivity.Mode;
import nz.mega.android.utils.Util;
import nz.mega.components.RoundedImageView;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class FileLinkActivity extends PinActivity implements MegaRequestListenerInterface, OnClickListener {
	
	FileLinkActivity fileLinkActivity = this;
	MegaApiAndroid megaApi;
	
	ActionBar aB;
	
	String url;
	
	ProgressDialog statusDialog;
	
	ImageView iconView;
	ImageView imageView;
	TextView nameView;
	RelativeLayout nameLayout;
	TextView sizeTextView;
	Button importButton;
	Button downloadButton;
	
	MegaNode document = null;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate()");
    	
		super.onCreate(savedInstanceState);
		
		if (aB == null){
			aB = getSupportActionBar();
		}
		aB.setHomeButtonEnabled(true);
		aB.setDisplayShowTitleEnabled(false);
		aB.setLogo(R.drawable.ic_action_navigation_accept);
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		setContentView(R.layout.activity_file_link);
		
		iconView = (ImageView) findViewById(R.id.file_link_icon);
		nameView = (TextView) findViewById(R.id.file_link_name);
		imageView = (RoundedImageView) findViewById(R.id.file_link_image);
		nameLayout = (RelativeLayout) findViewById(R.id.file_link_name_layout);
		sizeTextView = (TextView) findViewById(R.id.file_link_size);
		importButton = (Button) findViewById(R.id.file_link_button_import);
		importButton.setOnClickListener(this);
		downloadButton = (Button) findViewById(R.id.file_link_button_download);
		downloadButton.setOnClickListener(this);
		
		nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		nameView.setSingleLine();
		nameView.setTypeface(null, Typeface.BOLD);
		
		iconView.getLayoutParams().height = Util.px2dp((20*scaleH), outMetrics);
		((RelativeLayout.LayoutParams)iconView.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((15*scaleH), outMetrics), 0, 0);
		
		imageView.getLayoutParams().width = Util.px2dp((300*scaleW), outMetrics);
		imageView.getLayoutParams().height = Util.px2dp((300*scaleH), outMetrics);
		((RelativeLayout.LayoutParams) imageView.getLayoutParams()).setMargins(Util.px2dp((9*scaleW), outMetrics), Util.px2dp((9*scaleH), outMetrics), Util.px2dp((9*scaleW), outMetrics), Util.px2dp((9*scaleH), outMetrics));
		
		((RelativeLayout.LayoutParams) sizeTextView.getLayoutParams()).setMargins(Util.px2dp((75*scaleW), outMetrics), 0, 0, 0);
		
		dbH = DatabaseHandler.getDbHandler(getApplicationContext()); 
    	if(dbH.getCredentials() == null){	
    		importButton.setVisibility(View.INVISIBLE);
    	}
		
		Intent intent = getIntent();
		if (intent != null){
			url = intent.getDataString();		
		}
		
		if (url != null && url.matches("^https://mega.co.nz/#!.*!.*$")) {
			try{
				statusDialog.dismiss();
			}
			catch(Exception e){	}
			
			importLink(url);
		}	
	}
	
	@Override
	protected void onResume() {
    	super.onResume();
    	
    	Intent intent = getIntent();
    	
    	if (intent != null){
    		if (intent.getAction() != null){
    			if (intent.getAction().equals(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES)){
    				importNode();
    			}
    			intent.setAction(null);
    		}
    	}
    	setIntent(null);
	}	
	
	private void importLink(String url) {
		String[] parts = parseDownloadUrl(url);
		
		if (parts == null) {
			Util.showErrorAlertDialog(getString(R.string.manager_download_from_link_incorrect), false, this);
			return;
		}
		
		if(!Util.isOnline(this))
		{
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem),
					false, this);
			return;
		}

		if(this.isFinishing()) return;
		
		ProgressDialog temp = null;
		try {
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.general_loading));
			temp.show();
		}
		catch(Exception ex)
		{ return; }
		
		statusDialog = temp;
		
		megaApi.getPublicNode(url, this);
	}
	
	/*
	 * Check MEGA url and parse if valid
	 */
	private String[] parseDownloadUrl(String url) {
		if (url == null) {
			return null;
		}
		if (!url.matches("^https://mega.co.nz/#!.*!.*$")) {
			return null;
		}
		String[] parts = url.split("!");
		if(parts.length != 3) return null;
		return new String[] { parts[1], parts[2] };
	}
	
	public static void log(String message) {
		Util.log("FileLinkActivity", message);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_GET_PUBLIC_NODE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK) {
				document = request.getPublicMegaNode();
				
				if (document == null){
					Util.showErrorAlertDialog(MegaError.API_ETEMPUNAVAIL, this);
					Intent backIntent = new Intent(this, ManagerActivity.class);
	    			startActivity(backIntent);
	    			finish();
					return;
				}

				nameView.setText(document.getName());
				sizeTextView.setText(Formatter.formatFileSize(this, document.getSize()));
				
				imageView.setImageResource(MimeTypeMime.typeForName(document.getName()).getIconResourceId());
				iconView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());
			}
			else{
				
				try{ 
					AlertDialog.Builder dialogBuilder = Util.getCustomAlertBuilder(this, getString(R.string.general_error_word), getString(R.string.general_error_file_not_found), null);
					dialogBuilder.setPositiveButton(
						getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Intent backIntent = new Intent(fileLinkActivity, ManagerActivity.class);
				    			startActivity(backIntent);
				    			finish();
							}
						});
									
					AlertDialog dialog = dialogBuilder.create();
					dialog.show(); 
					Util.brandAlertDialog(dialog);
				}
				catch(Exception ex){
					Util.showToast(this, getString(R.string.general_error_file_not_found)); 
				}
				
    			return;
			}
		 }
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try{
				statusDialog.dismiss(); 
			} catch(Exception ex){};

			if (e.getErrorCode() != MegaError.API_OK) {
				Util.showErrorAlertDialog(e, this);
			}
			
			Intent intent = new Intent(this, ManagerActivity.class);
	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
			finish();
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}

	@Override
	public void onClick(View v) {
		
		switch(v.getId()){
			case R.id.file_link_button_download:{
				downloadNode();
				break;
			}
			case R.id.file_link_button_import:{
				importNode();
				break;
			}
		}
	}
	
	void importNode(){
		
		if (megaApi.getRootNode() == null){
			Intent intent = new Intent(this, ManagerActivity.class);
			intent.setAction(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES);
			intent.setData(Uri.parse(url));
			startActivity(intent);
			finish();
		}
		else{
			Intent intent = new Intent(this, FileExplorerActivity.class);
			intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER);
			startActivityForResult(intent, ManagerActivity.REQUEST_CODE_SELECT_IMPORT_FOLDER);	
		}		
	}
	
	void downloadNode(){
		long[] hashes = new long[1];
		hashes[0]=document.getHandle();
		long size = document.getSize();
		
		if (dbH == null){
//			dbH = new DatabaseHandler(this);
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
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
			intent.setClass(this, FileStorageActivity.class);
			intent.putExtra(FileStorageActivity.EXTRA_URL, url);
			intent.putExtra(FileStorageActivity.EXTRA_SIZE, document.getSize());
			startActivityForResult(intent, ManagerActivity.REQUEST_CODE_SELECT_LOCAL_FOLDER);	
		}
		else{
			downloadTo(downloadLocationDefaultPath, url, size, hashes);
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
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		
		if (intent == null) {
			return;
		}
		
		if (requestCode == ManagerActivity.REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivity.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivity.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);

			
			downloadTo (parentPath, url, size, hashes);
			Util.showToast(this, R.string.download_began);
		}
		else if (requestCode == ManagerActivity.REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK){
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			final long toHandle = intent.getLongExtra("IMPORT_TO", 0);
			
			statusDialog = new ProgressDialog(this);
			statusDialog.setMessage(getString(R.string.general_importing));
			statusDialog.show();
			
			if(!Util.isOnline(this)) {	
				try{ 
					statusDialog.dismiss(); 
				} catch(Exception ex) {};
				
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			MegaNode target = megaApi.getNodeByHandle(toHandle);
			if(target == null){
				target = megaApi.getRootNode();
			}
			megaApi.copyNode(document, target, this);
		}
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
						viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
						if (ManagerActivity.isIntentAvailable(this, viewIntent))
							startActivity(viewIntent);
						else{
							Intent intentShare = new Intent(Intent.ACTION_SEND);
							intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
							if (ManagerActivity.isIntentAvailable(this, intentShare))
								startActivity(intentShare);
							String toastMessage = getString(R.string.general_already_downloaded) + ": " + localPath;
							Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
						}
						finish();
						return;
					}
				}
			}
			
			for (long hash : hashes) {
				MegaNode node = megaApi.getNodeByHandle(hash);
				if(node != null){
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					dlFiles.put(node, parentPath);
					
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
		
		finish();
	}
}