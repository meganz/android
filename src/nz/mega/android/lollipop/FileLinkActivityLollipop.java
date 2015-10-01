package nz.mega.android.lollipop;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import nz.mega.android.DatabaseHandler;
import nz.mega.android.FileStorageActivity;
import nz.mega.android.FileStorageActivity.Mode;
import nz.mega.android.DownloadService;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaPreferences;
import nz.mega.android.MimeTypeList;
import nz.mega.android.MimeTypeMime;
import nz.mega.android.R;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutCompat.LayoutParams;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class FileLinkActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, OnClickListener {
	
	FileLinkActivityLollipop fileLinkActivity = this;
	MegaApiAndroid megaApi;
	
	Toolbar tB;
    ActionBar aB;
	
	String url;
	
	ProgressDialog statusDialog;
	
	ImageView iconView;
	TextView nameView;
	RelativeLayout nameLayout;
	TextView sizeTextView;
	TextView sizeTitleView;
	TextView importButton;
	TextView downloadButton;
	LinearLayout optionsBar;
	MegaNode document = null;
	RelativeLayout infoLayout;
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate()");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);	    
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		if(megaApi==null){
			log("Disconnected");
		}
		
		setContentView(R.layout.activity_file_link);
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_file_link);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
//		aB.setLogo(R.drawable.ic_arrow_back_black);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);
		aB.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
		aB.setDisplayShowTitleEnabled(false);
		
		infoLayout = (RelativeLayout) findViewById(R.id.file_link_layout);
		
		RelativeLayout.LayoutParams infoLayoutParams = (RelativeLayout.LayoutParams)infoLayout.getLayoutParams();
		infoLayoutParams.setMargins(0, 0, 0, Util.scaleHeightPx(80, outMetrics)); 		
		infoLayout.setLayoutParams(infoLayoutParams);
		
		iconView = (ImageView) findViewById(R.id.file_link_icon);		
		iconView.getLayoutParams().width = Util.scaleWidthPx(200, outMetrics);
		iconView.getLayoutParams().height = Util.scaleHeightPx(200, outMetrics);		
		
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) iconView.getLayoutParams();
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		iconView.setLayoutParams(params);

//		((RelativeLayout.LayoutParams)iconView.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((15*scaleH), outMetrics), 0, 0);
		
//		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//		lp.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(20, outMetrics));
//		iconView.setLayoutParams(lp);
		
		float scaleText;
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}
			
		nameView = (TextView) findViewById(R.id.file_link_name);
		nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		nameView.setSingleLine();
		nameView.setTypeface(null, Typeface.BOLD);
		//Left margin
		RelativeLayout.LayoutParams nameViewParams = (RelativeLayout.LayoutParams)nameView.getLayoutParams();
		nameViewParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleHeightPx(20, outMetrics)); 		
		nameView.setLayoutParams(nameViewParams);
		
//		lp.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(20, outMetrics));
//		nameView.setLayoutParams(lp);
		
		nameLayout = (RelativeLayout) findViewById(R.id.file_link_name_layout);
		sizeTitleView = (TextView) findViewById(R.id.file_link_info_menu_size);
		//Left margin, Top margin
		RelativeLayout.LayoutParams sizeTitleParams = (RelativeLayout.LayoutParams)sizeTitleView.getLayoutParams();
		sizeTitleParams.setMargins(Util.scaleWidthPx(10, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, 0); 		
		sizeTitleView.setLayoutParams(sizeTitleParams);
				
		sizeTextView = (TextView) findViewById(R.id.file_link_size);
		//Bottom margin
		RelativeLayout.LayoutParams sizeTextParams = (RelativeLayout.LayoutParams)sizeTextView.getLayoutParams();
		sizeTextParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(15, outMetrics)); 		
		sizeTextView.setLayoutParams(sizeTextParams);		
		
		optionsBar = (LinearLayout) findViewById(R.id.options_file_link_layout);
		downloadButton = (TextView) findViewById(R.id.file_link_button_download);
		downloadButton.setOnClickListener(this);
		downloadButton.setText(getString(R.string.general_download).toUpperCase(Locale.getDefault()));
		android.view.ViewGroup.LayoutParams paramsb1 = downloadButton.getLayoutParams();		
		paramsb1.height = Util.scaleHeightPx(48, outMetrics);
		paramsb1.width = Util.scaleWidthPx(83, outMetrics);
		downloadButton.setLayoutParams(paramsb1);
		//Left and Right margin
		LinearLayout.LayoutParams cancelTextParams = (LinearLayout.LayoutParams)downloadButton.getLayoutParams();
		cancelTextParams.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(8, outMetrics), 0); 
		downloadButton.setLayoutParams(cancelTextParams);
		
		importButton = (TextView) findViewById(R.id.file_link_button_import);
		importButton.setText(getString(R.string.general_import).toUpperCase(Locale.getDefault()));	
		importButton.setOnClickListener(this);
		android.view.ViewGroup.LayoutParams paramsb2 = importButton.getLayoutParams();		
		paramsb2.height = Util.scaleHeightPx(48, outMetrics);
		paramsb2.width = Util.scaleWidthPx(73, outMetrics);
		importButton.setLayoutParams(paramsb2);
		//Left and Right margin
		LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams)importButton.getLayoutParams();
		optionTextParams.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(8, outMetrics), 0); 
		importButton.setLayoutParams(optionTextParams);
				
//		iconView.getLayoutParams().height = Util.px2dp((20*scaleH), outMetrics);
//		((LayoutParams)iconView.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((15*scaleH), outMetrics), 0, 0);
//		
//		((LayoutParams) sizeTextView.getLayoutParams()).setMargins(Util.px2dp((75*scaleW), outMetrics), 0, 0, 0);
		
		dbH = DatabaseHandler.getDbHandler(getApplicationContext()); 
    	if(dbH.getCredentials() == null){	
    		importButton.setVisibility(View.INVISIBLE);
    	}
		
		Intent intent = getIntent();
		if (intent != null){
			url = intent.getDataString();		
		}
		
		if (url != null && (url.matches("^https://mega.co.nz/#!.*!.*$") || url.matches("^https://mega.nz/#!.*!.*$"))) {
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
    			if (intent.getAction().equals(ManagerActivityLollipop.ACTION_IMPORT_LINK_FETCH_NODES)){
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
		if (!url.matches("^https://mega.co.nz/#!.*!.*$") && !url.matches("^https://mega.nz/#!.*!.*$")) {
			return null;
		}
		String[] parts = url.split("!");
		if(parts.length != 3) return null;
		return new String[] { parts[1], parts[2] };
	}
	
	public static void log(String message) {
		Util.log("FileLinkActivityLollipop", message);
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
					Intent backIntent = new Intent(this, ManagerActivityLollipop.class);
	    			startActivity(backIntent);
	    			finish();
					return;
				}

				nameView.setText(document.getName());
				sizeTextView.setText(Formatter.formatFileSize(this, document.getSize()));

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
								Intent backIntent = new Intent(fileLinkActivity, ManagerActivityLollipop.class);
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
				
				log("e.getErrorCode() != MegaError.API_OK");
				
				if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					log("OVERQUOTA ERROR: "+e.getErrorCode());
					Toast.makeText(this, e.getErrorCode()+"", Toast.LENGTH_LONG).show();
					
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(ManagerActivityLollipop.ACTION_OVERQUOTA_ALERT);
					startActivity(intent);
					finish();

				}
				else
				{
					Util.showErrorAlertDialog(e, this);
					Toast.makeText(this, getString(R.string.context_no_copied), Toast.LENGTH_LONG).show();
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
			        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent);
					finish();
				}							
				
			}else{
				Intent intent = new Intent(this, ManagerActivityLollipop.class);
		        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent);
				finish();
			}		
			
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
			Intent intent = new Intent(this, ManagerActivityLollipop.class);
			intent.setAction(ManagerActivityLollipop.ACTION_IMPORT_LINK_FETCH_NODES);
			intent.setData(Uri.parse(url));
			startActivity(intent);
			finish();
		}
		else{
			Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
			intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);
			startActivityForResult(intent, ManagerActivityLollipop.REQUEST_CODE_SELECT_IMPORT_FOLDER);	
		}		
	}
	
	void downloadNode(){
		
		if (document == null){
			try{ 
				AlertDialog.Builder dialogBuilder = Util.getCustomAlertBuilder(this, getString(R.string.general_error_word), getString(R.string.general_error_file_not_found), null);
				dialogBuilder.setPositiveButton(
					getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							Intent backIntent = new Intent(fileLinkActivity, ManagerActivityLollipop.class);
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
			startActivityForResult(intent, ManagerActivityLollipop.REQUEST_CODE_SELECT_LOCAL_FOLDER);	
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
		
		if (requestCode == ManagerActivityLollipop.REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivity.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivity.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);

			
			downloadTo (parentPath, url, size, hashes);
			Util.showToast(this, R.string.download_began);
		}
		else if (requestCode == ManagerActivityLollipop.REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK){
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
						if (ManagerActivityLollipop.isIntentAvailable(this, viewIntent))
							startActivity(viewIntent);
						else{
							Intent intentShare = new Intent(Intent.ACTION_SEND);
							intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
							if (ManagerActivityLollipop.isIntentAvailable(this, intentShare))
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