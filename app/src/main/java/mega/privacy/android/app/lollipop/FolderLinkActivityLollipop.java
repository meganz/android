package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MegaStreamingService;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.modalbottomsheet.FolderLinkBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class FolderLinkActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, OnClickListener{
	
	FolderLinkActivityLollipop folderLinkActivity = this;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	
	private AlertDialog decryptionKeyDialog;
	
	ActionBar aB;
	Toolbar tB;
	Handler handler;
	String url;
	RecyclerView listView;
	private RecyclerView.LayoutManager mLayoutManager;
	MegaNode selectedNode;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
    RelativeLayout fragmentContainer;
	Button downloadButton;
	View separator;
	Button importButton;
	LinearLayout optionsBar;
	DisplayMetrics outMetrics;
	long parentHandle = -1;
	ArrayList<MegaNode> nodes;
	MegaBrowserLollipopAdapter adapterList;
	
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	boolean decryptionIntroduced=false;

	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	
	private ActionMode actionMode;
	
	boolean downloadCompleteFolder = false;
	FolderLinkActivityLollipop folderLinkActivityLollipop = this;

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapterList.isMultipleSelect()){
			adapterList.setMultipleSelect(true);
			actionMode = startSupportActionMode(new ActionBarCallBack());
		}
	}

	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = adapterList.getSelectedNodes();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						boolean hasStoragePermission = (ContextCompat.checkSelfPermission(folderLinkActivityLollipop, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
						if (!hasStoragePermission) {
							ActivityCompat.requestPermissions(folderLinkActivityLollipop,
					                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									Constants.REQUEST_WRITE_STORAGE);
							
							handleListM.clear();
							for (int i=0;i<documents.size();i++){
								handleListM.add(documents.get(i).getHandle());
							}
							
							return false;
						}
					}
					
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					onFileClick(handleList);
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					break;
				}
			}
			
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.folder_link_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			clearSelections();
			adapterList.setMultipleSelect(false);
			optionsBar.setVisibility(View.VISIBLE);
			separator.setVisibility(View.VISIBLE);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapterList.getSelectedNodes();
			boolean showDownload = false;
			
			if (selected.size() != 0) {
				if (selected.size() > 0) {
					showDownload = true;
				}
				if(selected.size()==adapterList.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);			
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);	
				}	
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}			
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			
			return false;
		}
		
	}	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");
		switch (item.getItemId()) {
	    	// Respond to the action bar's Up/Home button
		    case android.R.id.home:{
		    	onBackPressed();
		    	return true;
		    }
		}
	
		return super.onOptionsItemSelected(item);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
    	
    	log("onCreate()");
    	requestWindowFeature(Window.FEATURE_NO_TITLE);	
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;

		MegaApplication app = (MegaApplication)getApplication();
		megaApiFolder = app.getMegaApiFolder();
		megaApi = app.getMegaApi();
		
		folderLinkActivity = this;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
		}
		
		setContentView(R.layout.activity_folder_link);	
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_folder_link);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
//		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);
		
        fragmentContainer = (RelativeLayout) findViewById(R.id.folder_link_fragment_container);

		emptyImageView = (ImageView) findViewById(R.id.folder_link_list_empty_image);
		emptyTextView = (TextView) findViewById(R.id.folder_link_list_empty_text);
		
		listView = (RecyclerView) findViewById(R.id.folder_link_list_view_browser);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new MegaLinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);
		listView.setItemAnimator(new DefaultItemAnimator()); 
		
		optionsBar = (LinearLayout) findViewById(R.id.options_folder_link_layout);
		separator = (View) findViewById(R.id.separator_3);

		downloadButton = (Button) findViewById(R.id.folder_link_button_download);
		downloadButton.setOnClickListener(this);

		importButton = (Button) findViewById(R.id.folder_link_import_button);
		importButton.setOnClickListener(this);

		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		if (dbH != null){
			if (dbH.getCredentials() != null){
				importButton.setVisibility(View.VISIBLE);
			}
			else{
				importButton.setVisibility(View.INVISIBLE);
			}
		}

		contentText = (TextView) findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);
		
		Intent intent = getIntent();
    	
    	if (intent != null) {
    		if (intent.getAction().equals(Constants.ACTION_OPEN_MEGA_FOLDER_LINK)){
    			if (parentHandle == -1){
    				url = intent.getDataString();
					if(url!=null){
						megaApiFolder.loginToFolder(url, this);
					}
					else{
						log("url NULL");
					}
//    				int counter = url.split("!").length - 1;
//    				log("Counter !: "+counter);
//    				if(counter<2){
//    					//Ask for decryption key
//    					log("Ask for decryption key");
//    					askForDecryptionKeyDialog();
//    				}
//    				else{
//    					//Decryption key included!
//
//    				}
    			}
    		}
    	}
    	
    	aB.setTitle("MEGA - " + getString(R.string.general_loading));

		
		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
    }
	
	public void askForDecryptionKeyDialog(){
		log("askForDecryptionKeyDialog");
		
		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

	    final EditText input = new EditText(this);
	    layout.addView(input, params);		
		
		input.setSingleLine();
		input.setTextColor(getResources().getColor(R.color.text_secondary));
		input.setHint(getString(R.string.alert_decryption_key));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					if(value.startsWith("!")){
						log("Decryption key with exclamation!");
						url=url+value;
					}
					else{
						url=url+"!"+value;
					}
					log("Folder link to import: "+url);
					decryptionIntroduced=true;
					megaApiFolder.loginToFolder(url, folderLinkActivity);
					decryptionKeyDialog.dismiss();
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.cam_sync_ok),EditorInfo.IME_ACTION_DONE);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.alert_decryption_key));
		builder.setMessage(getString(R.string.message_decryption_key));
		builder.setPositiveButton(getString(R.string.general_decryp),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						if(value.startsWith("!")){
							log("Decryption key with exclamation!");
							url=url+value;
						}
						else{
							url=url+"!"+value;
						}
						log("Folder link to import: "+url);
						decryptionIntroduced=true;
						megaApiFolder.loginToFolder(url, folderLinkActivity);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
			}
		});
		builder.setView(layout);
		decryptionKeyDialog = builder.create();
		decryptionKeyDialog.show();
	}
	
	private void showKeyboardDelayed(final View view) {
		log("showKeyboardDelayed");
		handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {				
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	@Override
	protected void onDestroy() {

		if (megaApiFolder != null){
			megaApiFolder.removeRequestListener(this);
//			megaApiFolder.logout();
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
    	folderLinkActivity = null;
    	log("onPause");
    	super.onPause();
    }
	
	@Override
	protected void onResume() {
    	super.onResume();
    	folderLinkActivity = this;
    	
    	log("onResume");
	}
	
	@SuppressLint("NewApi") public void onFileClick(ArrayList<Long> handleList){
		long size = 0;
		long[] hashes = new long[handleList.size()];
		for (int i=0;i<handleList.size();i++){
			hashes[i] = handleList.get(i);
			MegaNode n = megaApiFolder.getNodeByHandle(hashes[i]);
			if(n != null)
			{
				size += n.getSize();
			}
		}
		
		if (dbH == null){
//			dbH = new DatabaseHandler(getApplicationContext());
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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
						intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
						intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
						intent.setClass(this, FileStorageActivityLollipop.class);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
						startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
					}
					else{
						Dialog downloadLocationDialog;
						String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
				        AlertDialog.Builder b=new AlertDialog.Builder(this);
	
						b.setTitle(getResources().getString(R.string.settings_storage_download_location));
						final long sizeFinal = size;
						final long[] hashesFinal = new long[hashes.length];
						for (int i=0; i< hashes.length; i++){
							hashesFinal[i] = hashes[i];
						}
						
						b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which){
									case 0:{
										Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
										intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
										intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, sizeFinal);
										intent.setClass(getApplicationContext(), FileStorageActivityLollipop.class);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashesFinal);
										startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
										break;
									}
									case 1:{
										File[] fs = getExternalFilesDirs(null);
										if (fs.length > 1){
											String path = fs[1].getAbsolutePath();
											File defaultPathF = new File(path);
											defaultPathF.mkdirs();
											Toast.makeText(getApplicationContext(), getString(R.string.general_download) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
											downloadTo(path, null, sizeFinal, hashesFinal);
										}
										break;
									}
								}
							}
						});
						b.setNegativeButton(getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						});
						downloadLocationDialog = b.create();
						downloadLocationDialog.show();
					}
				}
				else{
					Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
					intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
					intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
					intent.setClass(this, FileStorageActivityLollipop.class);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
					startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
				}
			}
			else{
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
				intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
				intent.setClass(this, FileStorageActivityLollipop.class);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
				startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}
		}
		else{
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}
	}
	
	@SuppressLint("NewApi") public void onFolderClick(long handle, long size){
		log("onFolderClick");
		
		long[] hashes = new long[1];
		
		hashes[0] = handle;		
		
		if (dbH == null){
//			dbH = new DatabaseHandler(getApplicationContext());
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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
						intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
						intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
						intent.setClass(this, FileStorageActivityLollipop.class);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
						startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
					}
					else{
						Dialog downloadLocationDialog;
						String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
				        AlertDialog.Builder b=new AlertDialog.Builder(this);
	
						b.setTitle(getResources().getString(R.string.settings_storage_download_location));
						final long sizeFinal = size;
						final long[] hashesFinal = new long[hashes.length];
						for (int i=0; i< hashes.length; i++){
							hashesFinal[i] = hashes[i];
						}
						
						b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which){
									case 0:{
										Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
										intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
										intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, sizeFinal);
										intent.setClass(getApplicationContext(), FileStorageActivityLollipop.class);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashesFinal);
										startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
										break;
									}
									case 1:{
										File[] fs = getExternalFilesDirs(null);
										if (fs.length > 1){
											String path = fs[1].getAbsolutePath();
											File defaultPathF = new File(path);
											defaultPathF.mkdirs();
											Toast.makeText(getApplicationContext(), getString(R.string.general_download) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
											downloadTo(path, null, sizeFinal, hashesFinal);
										}
										break;
									}
								}
							}
						});
						b.setNegativeButton(getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						});
						downloadLocationDialog = b.create();
						downloadLocationDialog.show();
					}
				}
				else{
					Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
					intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
					intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
					intent.setClass(this, FileStorageActivityLollipop.class);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
					startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
				}
			}
			else{
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
				intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
				intent.setClass(this, FileStorageActivityLollipop.class);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
				startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}
		}
		else{
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}
	}
	
	public void downloadTo(String parentPath, String url, long size, long [] hashes){
		log("downloadTo");
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}
		
		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}		
			
		for (long hash : hashes) {
			MegaNode node = megaApiFolder.getNodeByHandle(hash);
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
						Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
						continue;
					}

					log("EXTRA_HASH: " + document.getHandle());
					Intent service = new Intent(this, DownloadService.class);
					service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
					service.putExtra(DownloadService.EXTRA_URL, url);
					service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
					service.putExtra(DownloadService.EXTRA_PATH, path);
					service.putExtra(DownloadService.EXTRA_FOLDER_LINK, true);
					startService(service);
				}
			}
			else if(url != null) {
				if(availableFreeSpace < size) {
					Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
					continue;
				}
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_HASH, hash);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				service.putExtra(DownloadService.EXTRA_FOLDER_LINK, true);
				startService(service);
			}
			else {
				log("node not found");
			}
		}
	}
	
	
	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
		
		if (megaApiFolder.getRootNode() == null)
			return;
		
		folder.mkdir();
		ArrayList<MegaNode> nodeList = megaApiFolder.getChildren(parent, orderGetChildren);
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		log("onActivityResult");
		if (intent == null){
			return;
		}
		
		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);
	
			downloadTo (parentPath, url, size, hashes);
			Snackbar.make(fragmentContainer, getResources().getString(R.string.download_began), Snackbar.LENGTH_LONG).show();
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK){
			log("REQUEST_CODE_SELECT_IMPORT_FOLDER");
			if(!Util.isOnline(this)){
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
				return;
			}
			
			final long toHandle = intent.getLongExtra("IMPORT_TO", 0);
			
			MegaNode target = megaApi.getNodeByHandle(toHandle);
			if(target == null){
				if (megaApi.getRootNode() != null){
					target = megaApi.getRootNode();
				}
			}
			
			if(selectedNode!=null){
				if (target != null){
					log("Target node: "+target.getName());
					selectedNode = megaApiFolder.authorizeNode(selectedNode);
					megaApi.copyNode(selectedNode, target, this);
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
				}
			}
			else{
				log("selected Node is NULL");
				Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
			}
			
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: " + request.getRequestString());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		
		if (request.getType() == MegaRequest.TYPE_LOGIN){
			if (e.getErrorCode() == MegaError.API_OK){
				megaApiFolder.fetchNodes(this);	
			}
			else{
				log("Error: "+e.getErrorCode());
				if(e.getErrorCode() == MegaError.API_EINCOMPLETE){
					decryptionIntroduced=false;
					askForDecryptionKeyDialog();
					return;
				}
				else if(e.getErrorCode() == MegaError.API_EARGS){
					if(decryptionIntroduced){
						log("incorrect key, ask again!");
						decryptionIntroduced=false;
						askForDecryptionKeyDialog();
						return;
					}
					else{
						try{
							log("API_EARGS - show alert dialog");
							AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
							builder.setMessage(getString(R.string.general_error_folder_not_found));
							builder.setTitle(getString(R.string.general_error_word));

							builder.setPositiveButton(getString(android.R.string.ok),new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									Intent backIntent;
									if(folderLinkActivity != null)
										backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
									else
										backIntent = new Intent(FolderLinkActivityLollipop.this, ManagerActivityLollipop.class);

									startActivity(backIntent);
									finish();
								}
							});

							AlertDialog dialog = builder.create();
							dialog.show();
						}
						catch(Exception ex){
							Snackbar.make(fragmentContainer, getResources().getString(R.string.general_error_folder_not_found), Snackbar.LENGTH_LONG).show();
							finish();
						}
					}
				}
				else{
					try{
						log("no link - show alert dialog");
						AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
						builder.setMessage(getString(R.string.general_error_folder_not_found));
						builder.setTitle(getString(R.string.general_error_word));

						builder.setPositiveButton(getString(android.R.string.ok),new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Intent backIntent;
								if(folderLinkActivity != null)
									backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
								else
									backIntent = new Intent(FolderLinkActivityLollipop.this, ManagerActivityLollipop.class);

								startActivity(backIntent);
								finish();
							}
						});

						AlertDialog dialog = builder.create();
						dialog.show();
					}
					catch(Exception ex){
						Snackbar.make(fragmentContainer, getResources().getString(R.string.general_error_folder_not_found), Snackbar.LENGTH_LONG).show();
						finish();
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("TYPE_COPY");

			if (e.getErrorCode() != MegaError.API_OK) {
				
				log("ERROR: "+e.getErrorString());
				
				if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					log("OVERQUOTA ERROR: "+e.getErrorCode());
					
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(Constants.ACTION_OVERQUOTA_ALERT);
					startActivity(intent);
					finish();
				}
				else
				{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
//					Intent intent = new Intent(this, ManagerActivityLollipop.class);
//			        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
//			        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//					startActivity(intent);
//					finish();
				}							
				
			}else{
				log("OK");
				Snackbar.make(fragmentContainer, getString(R.string.context_correctly_copied), Snackbar.LENGTH_LONG).show();
//				Intent intent = new Intent(this, ManagerActivityLollipop.class);
//		        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
//		        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//				startActivity(intent);
//				finish();
			}			
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){

			if (e.getErrorCode() == MegaError.API_OK) {
				MegaNode rootNode = megaApiFolder.getRootNode();
				if (rootNode != null){

					if(request.getFlag()){
						log("Login into a folder with invalid decryption key");
						try{
							AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
							builder.setMessage(getString(R.string.general_error_invalid_decryption_key));
							builder.setTitle(getString(R.string.general_error_word));

							builder.setPositiveButton(
									getString(android.R.string.ok),
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											dialog.dismiss();
											Intent backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
											startActivity(backIntent);
											finish();
										}
									});

							AlertDialog dialog = builder.create();
							dialog.show();
						}
						catch(Exception ex){
							Snackbar.make(fragmentContainer, getResources().getString(R.string.general_error_folder_not_found), Snackbar.LENGTH_LONG).show();
							finish();
						}
					}
					else{
						parentHandle = rootNode.getHandle();
						nodes = megaApiFolder.getChildren(rootNode);
						aB.setTitle(megaApiFolder.getRootNode().getName());
						supportInvalidateOptionsMenu();

						if (adapterList == null){
							adapterList = new MegaBrowserLollipopAdapter(this, null, nodes, parentHandle, listView, aB, Constants.FOLDER_LINK_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
						}
						else{
							adapterList.setParentHandle(parentHandle);
							adapterList.setNodes(nodes);
						}

						adapterList.setMultipleSelect(false);

						listView.setAdapter(adapterList);
					}
				}
				else{
					try{ 
						AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			            builder.setMessage(getString(R.string.general_error_folder_not_found));
						builder.setTitle(getString(R.string.general_error_word));
						
						builder.setPositiveButton(
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									Intent backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
					    			startActivity(backIntent);
					    			finish();
								}
							});
										
						AlertDialog dialog = builder.create();
						dialog.show(); 
					}
					catch(Exception ex){
						Snackbar.make(fragmentContainer, getResources().getString(R.string.general_error_folder_not_found), Snackbar.LENGTH_LONG).show();
		    			finish();
					}
				}
			}
			else{
				log("Error: "+e.getErrorCode()+" "+e.getErrorString());
				try{
					AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);

					if(e.getErrorCode() == MegaError.API_EBLOCKED){
						builder.setMessage(getString(R.string.folder_link_unavaible_ToS_violation));
						builder.setTitle(getString(R.string.general_error_folder_not_found));
					}
					else if(e.getErrorCode() == MegaError.API_ETOOMANY){
						builder.setMessage(getString(R.string.file_link_unavaible_delete_account));
						builder.setTitle(getString(R.string.general_error_folder_not_found));
					}
					else{
						builder.setMessage(getString(R.string.general_error_folder_not_found));
						builder.setTitle(getString(R.string.general_error_word));
					}

					builder.setPositiveButton(
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									Intent backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
									startActivity(backIntent);
									finish();
								}
							});

					AlertDialog dialog = builder.create();
					dialog.show();
				}
				catch(Exception ex){
					Snackbar.make(fragmentContainer, getResources().getString(R.string.general_error_folder_not_found), Snackbar.LENGTH_LONG).show();
					finish();
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}
	
	public static void log(String message) {
		Util.log("FolderLinkActivityLollipop", message);
	}

	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
		optionsBar.setVisibility(View.VISIBLE);
		separator.setVisibility(View.VISIBLE);
	}
	
	public void selectAll(){
		if (adapterList != null){
			if(adapterList.isMultipleSelect()){
				adapterList.selectAll();
			}
			else{				
				adapterList.setMultipleSelect(true);
				adapterList.selectAll();
				
				actionMode = startSupportActionMode(new ActionBarCallBack());
			}
			
			updateActionModeTitle();
		}
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapterList.isMultipleSelect()){
			adapterList.clearSelections();
		}
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null) {
			return;
		}
		List<MegaNode> documents = adapterList.getSelectedNodes();
		int files = 0;
		int folders = 0;
		for (MegaNode document : documents) {
			if (document.isFile()) {
				files++;
			} else if (document.isFolder()) {
				folders++;
			}
		}
		Resources res = getResources();
		String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));
		String foldersStr = String.format(format, folders,
				res.getQuantityString(R.plurals.general_num_folders, folders));
		String title;
		if (files == 0 && folders == 0) {
			title = foldersStr + ", " + filesStr;
		} else if (files == 0) {
			title = foldersStr;
		} else if (folders == 0) {
			title = filesStr;
		} else {
			title = foldersStr + ", " + filesStr;
		}
		actionMode.setTitle(title);
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
		// actionMode.
	}
	
	ArrayList<Long> handleListM = new ArrayList<Long>();

	public void itemClick(int position) {
		if (adapterList.isMultipleSelect()){
			log("multiselect ON");
			adapterList.toggleSelection(position);

			List<MegaNode> selectedNodes = adapterList.getSelectedNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
			}
		}
		else{
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);
				
				aB.setTitle(n.getName());
				supportInvalidateOptionsMenu();
//				((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = nodes.get(position).getHandle();
//				((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
				adapterList.setParentHandle(parentHandle);
				nodes = megaApiFolder.getChildren(nodes.get(position), orderGetChildren);
				adapterList.setNodes(nodes);
				listView.scrollToPosition(0);
				
				//If folder has no files
				if (adapterList.getItemCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApiFolder.getRootNode().getHandle()==n.getHandle()) {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_empty_folder);
						emptyTextView.setText(R.string.file_browser_empty_folder);
					}
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
			}
			else{
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(this, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", Constants.FOLDER_LINK_ADAPTER);
					if (megaApiFolder.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApiFolder.getParentNode(nodes.get(position)).getHandle());
					}
					intent.putExtra("orderGetChildren", orderGetChildren);
					intent.putExtra("isFolderLink", true);
					startActivity(intent);
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isVideo() || MimeTypeList.typeForName(nodes.get(position).getName()).isAudio() ){
					MegaNode file = nodes.get(position);
					Intent service = new Intent(this, MegaStreamingService.class);
			  		startService(service);
			  		String fileName = file.getName();
					try {
						fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
					} 
					catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					
			  		String url = "http://127.0.0.1:4443/" + file.getBase64Handle() + "/" + fileName;
			  		String mimeType = MimeTypeList.typeForName(file.getName()).getType();
			  		System.out.println("FILENAME: " + fileName);
			  		
			  		Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
			  		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
			  		if (MegaApiUtils.isIntentAvailable(this, mediaIntent)){
			  			startActivity(mediaIntent);
			  		}
			  		else{
			  			Snackbar.make(fragmentContainer, getResources().getString(R.string.intent_not_available), Snackbar.LENGTH_SHORT).show();
						adapterList.notifyDataSetChanged();
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						onFileClick(handleList);
			  		}					
				}
				else{
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
						if (!hasStoragePermission) {
							ActivityCompat.requestPermissions(this,
					                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									Constants.REQUEST_WRITE_STORAGE);
							
							handleListM.clear();
							handleListM.add(nodes.get(position).getHandle());
							
							return;
						}
					}
					adapterList.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					onFileClick(handleList);
				}
			}
		}
	}
	
	@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
        	case Constants.REQUEST_WRITE_STORAGE:{
		        boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (hasStoragePermission) {
					if (downloadCompleteFolder){
						MegaNode rootNode = null;	  
						if(megaApiFolder.getRootNode()!=null){
							rootNode = megaApiFolder.getRootNode();
						}
			        	if(rootNode!=null){
			        		onFolderClick(rootNode.getHandle(),rootNode.getSize());	
			        	}
			        	else{
			        		log("rootNode null!!");
			        	}
					}
					else{
						onFileClick(handleListM);
					}
				}
				downloadCompleteFolder = false;
	        	break;
	        }
        }
    }
	
	@Override
	public void onBackPressed() {
		log("onBackPressed");

		if (adapterList != null){
			log("onBackPressed: adapter !=null");
			parentHandle = adapterList.getParentHandle();
			

			MegaNode parentNode = megaApiFolder.getParentNode(megaApiFolder.getNodeByHandle(parentHandle));
			if (parentNode != null){
				log("onBackPressed: parentNode != NULL");
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				aB.setTitle(parentNode.getName());

				supportInvalidateOptionsMenu();

				parentHandle = parentNode.getHandle();
				nodes = megaApiFolder.getChildren(parentNode, orderGetChildren);
				adapterList.setNodes(nodes);
				listView.post(new Runnable()
				{
					@Override
					public void run()
					{
						listView.scrollToPosition(0);
						View v = listView.getChildAt(0);
						if (v != null)
						{
							v.requestFocus();
						}
					}
				});
				adapterList.setParentHandle(parentHandle);
				return;
			}
			else{
				log("onBackPressed: parentNode == NULL");
				finish();
			}
		}
		
		super.onBackPressed();
	}

	public void importNode(){
		log("importNode");
//		if (megaApi.getRootNode() == null){
//			log("megaApi bad fetch nodes");
//			Snackbar.make(fragmentContainer, getString(R.string.session_problem), Snackbar.LENGTH_LONG).show();
//		}
//		else{
			Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
			intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);
			startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER);
//		}		
	}

	public void downloadNode(){
		log("Download option");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);

				handleListM.clear();
				handleListM.add(selectedNode.getHandle());
				return;
			}
		}

		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(selectedNode.getHandle());
		onFileClick(handleList);
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		
		switch(v.getId()){
			case R.id.folder_link_button_download:{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
					if (!hasStoragePermission) {
						ActivityCompat.requestPermissions(this,
				                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								Constants.REQUEST_WRITE_STORAGE);
						
						downloadCompleteFolder = true;
						
						return;
					}
				}
				
				MegaNode rootNode = null;	  
				if(megaApiFolder.getRootNode()!=null){
					rootNode = megaApiFolder.getRootNode();
				}
	        	if(rootNode!=null){
					MegaNode parentNode = megaApiFolder.getNodeByHandle(parentHandle);
					if (parentNode != null){
						onFolderClick(parentNode.getHandle(),parentNode.getSize());
					}
					else{
						onFolderClick(rootNode.getHandle(),rootNode.getSize());
					}
	        	}
	        	else{
	        		log("rootNode null!!");
	        	}
				break;
			}
			case R.id.folder_link_import_button:{
				if (megaApiFolder.getRootNode() != null){
					this.selectedNode = megaApiFolder.getRootNode();
					importNode();
				}
				break;
			}
		}			
	}

	public void showOptionsPanel(MegaNode sNode){
		log("showNodeOptionsPanel-Offline");
		if(sNode!=null){
			this.selectedNode = sNode;
			FolderLinkBottomSheetDialogFragment bottomSheetDialogFragment = new FolderLinkBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public MegaNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(MegaNode selectedNode) {
		this.selectedNode = selectedNode;
	}
}
