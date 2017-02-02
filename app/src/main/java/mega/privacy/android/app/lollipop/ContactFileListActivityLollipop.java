package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.modalbottomsheet.ContactFileListBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;


public class ContactFileListActivityLollipop extends PinActivityLollipop implements MegaGlobalListenerInterface, MegaTransferListenerInterface, MegaRequestListenerInterface {

	FrameLayout fragmentContainer;
    
    String userEmail;

	MegaApiAndroid megaApi;
	AlertDialog permissionsDialog;

	ContactFileListFragmentLollipop cflF;

	CoordinatorLayout coordinatorLayout;
	Handler handler;

	MenuItem shareMenuItem;
	MenuItem viewSharedItem;

	boolean sendToInbox=false;
	boolean moveToRubbish=false;

	public static int REQUEST_CODE_GET = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_GET_LOCAL = 1003;
	public static final int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_SELECT_FOLDER = 1008;

	static ContactFileListActivityLollipop contactPropertiesMainActivity;

	long parentHandle = -1;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	MenuItem createFolderMenuItem;
	private AlertDialog newFolderDialog;
	DisplayMetrics outMetrics;

	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	private AlertDialog renameDialog;
	ProgressDialog statusDialog;

	ArrayList<MegaTransfer> tL;
	long lastTimeOnTransferUpdate = -1;

	private List<ShareInfo> filePreparedInfos;

	MegaNode selectedNode = null;

	Toolbar tB;
	ActionBar aB;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.file_explorer_action, menu);

		createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder);

		if (cflF != null && cflF.isVisible()){
			if(cflF.getFabVisibility()==View.VISIBLE){
				createFolderMenuItem.setVisible(true);
			}
			else{
				createFolderMenuItem.setVisible(false);
			}
		}
		else{
			createFolderMenuItem.setVisible(false);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");
		int id = item.getItemId();
		switch(id){
			case android.R.id.home:{
				onBackPressed();
				break;
			}
			case R.id.cab_menu_create_folder:{
				showNewFolderDialog();
				break;
			}
		}
		return true;
	}

	public void showNewFolderDialog(){
		log("showNewFolderDialog");

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setTextColor(getResources().getColor(R.color.text_secondary));
		input.setHint(getString(R.string.context_new_folder_name));
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
					createFolder(value);
					newFolderDialog.dismiss();
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_create),EditorInfo.IME_ACTION_DONE);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.menu_new_folder));
		builder.setPositiveButton(getString(R.string.general_create),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						createFolder(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);
		newFolderDialog = builder.create();
		newFolderDialog.show();
	}

	private void createFolder(String title) {

		log("createFolder");
		if (!Util.isOnline(this)) {
			CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
			if(coordinatorFragment!=null){
				showSnackbar(getString(R.string.error_server_connection_problem), coordinatorFragment);
			}
			else{
				showSnackbar(getString(R.string.error_server_connection_problem), fragmentContainer);
			}
			return;
		}

		if(isFinishing()){
			return;
		}

		long parentHandle = cflF.getParentHandle();

		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

		if (parentNode != null){
			log("parentNode != null: " + parentNode.getName());
			boolean exists = false;
			ArrayList<MegaNode> nL = megaApi.getChildren(parentNode);
			for (int i=0;i<nL.size();i++){
				if (title.compareTo(nL.get(i).getName()) == 0){
					exists = true;
				}
			}

			if (!exists){
				statusDialog = null;
				try {
					statusDialog = new ProgressDialog(this);
					statusDialog.setMessage(getString(R.string.context_creating_folder));
					statusDialog.show();
				}
				catch(Exception e){
					return;
				}

				megaApi.createFolder(title, parentNode, this);
			}
			else{
				Snackbar.make(fragmentContainer,getString(R.string.context_folder_already_exists),Snackbar.LENGTH_LONG).show();
			}
		}
		else{
			log("parentNode == null: " + parentHandle);
			parentNode = megaApi.getRootNode();
			if (parentNode != null){
				log("megaApi.getRootNode() != null");
				boolean exists = false;
				ArrayList<MegaNode> nL = megaApi.getChildren(parentNode);
				for (int i=0;i<nL.size();i++){
					if (title.compareTo(nL.get(i).getName()) == 0){
						exists = true;
					}
				}

				if (!exists){
					statusDialog = null;
					try {
						statusDialog = new ProgressDialog(this);
						statusDialog.setMessage(getString(R.string.context_creating_folder));
						statusDialog.show();
					}
					catch(Exception e){
						return;
					}

					megaApi.createFolder(title, parentNode, this);
				}
				else{
					Snackbar.make(fragmentContainer,getString(R.string.context_folder_already_exists),Snackbar.LENGTH_LONG).show();
				}
			}
			else{
				return;
			}
		}
	}

	private void showKeyboardDelayed(final View view) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		log("onCreate first");
		super.onCreate(savedInstanceState);

		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		megaApi.addGlobalListener(this);
		megaApi.addTransferListener(this);

		contactPropertiesMainActivity=this;

		handler = new Handler();

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		Bundle extras = getIntent().getExtras();
		if (extras != null){
			userEmail = extras.getString("name");

			setContentView(R.layout.activity_main_contact_properties);

			coordinatorLayout = (CoordinatorLayout) findViewById(R.id.contact_properties_main_activity_layout);
			coordinatorLayout.setFitsSystemWindows(false);

			//Set toolbar
			tB = (Toolbar) findViewById(R.id.toolbar_main_contact_properties);
			if(tB==null){
				log("Toolbar is NULL");
			}
//			tB.setPadding(0,getStatusBarHeight(),0,0);
			tB.setTitle(getString(R.string.contact_shared_files));
			setSupportActionBar(tB);
			aB = getSupportActionBar();
			if(aB!=null){
				aB.setDisplayHomeAsUpEnabled(true);
				aB.setDisplayShowHomeEnabled(true);
				aB.hide();
			}
			else{
				log("aB is NULL!!!!");
			}

			fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_contact_properties);

			log("Shared Folders are:");
			coordinatorLayout.setFitsSystemWindows(true);
			aB.show();

			if (cflF == null){
				cflF = new ContactFileListFragmentLollipop();
			}
			cflF.setUserEmail(userEmail);

			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_contact_properties, cflF, "cflF").commitNow();
			coordinatorLayout.invalidate();
		}
		
	}

	public void showUploadPanel() {
		log("showUploadPanel");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}

		UploadBottomSheetDialogFragment bottomSheetDialogFragment = new UploadBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	@Override
	protected void onResume() {
		log("onResume");
		super.onResume();

		Intent intent = getIntent(); 

		if (intent != null) { 
			if (intent.getAction() != null){ 
//				if(getIntent().getAction().equals(ManagerActivityLollipop.ACTION_EXPLORE_ZIP)){  
//
//					String pathZip=intent.getExtras().getString(ManagerActivityLollipop.EXTRA_PATH_ZIP);    				
//
//					log("Path: "+pathZip);
//
//					//Lanzar nueva activity ZipBrowserActivity
//
//					Intent intentZip = new Intent(this, ZipBrowserActivityLollipop.class);    				
//					intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, pathZip);
//					startActivity(intentZip);
//
//
//				}
//				else if(getIntent().getAction().equals(ManagerActivityLollipop.ACTION_OPEN_PDF)){ 
//					String pathPdf=intent.getExtras().getString(ManagerActivityLollipop.EXTRA_PATH_PDF);
//
//					File pdfFile = new File(pathPdf);
//
//					Intent intentPdf = new Intent();
//					intentPdf.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
//					intentPdf.setClass(this, OpenPDFActivity.class);
//					intentPdf.setAction("android.intent.action.VIEW");
//					this.startActivity(intentPdf);
//				}
			}
			intent.setAction(null);
			setIntent(null);
		}    	
	}

	@Override
	protected void onNewIntent(Intent intent){
		log("onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent); 
	}

//	public void selectContactFragment(int currentFragment){
//		log("selectContactFragment: "+currentFragment);
//		switch(currentFragment){
//			case CONTACT_PROPERTIES:{
//				if (cpF == null){
//					cpF = new ContactPropertiesFragmentLollipop();
//				}
//				cpF.setUserEmail(userEmail);
//				coordinatorLayout.setFitsSystemWindows(false);
//				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_contact_properties, cpF, "cpF").commit();
//
//				break;
//			}
//			case CONTACT_FILE_LIST:{
//
//				break;
//			}
//		}
//	}

	public void showConfirmationLeaveIncomingShare (final MegaNode n){
		log("showConfirmationLeaveIncomingShare");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						//TODO remove the incoming shares
						megaApi.remove(n);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationLeaveIncomingShare (final ArrayList<Long> handleList){
		log("showConfirmationLeaveIncomingShare");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						//TODO remove the incoming shares
						contactPropertiesMainActivity.leaveMultipleShares(handleList);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}
	
	public void leaveMultipleShares (ArrayList<Long> handleList){
		
		for (int i=0; i<handleList.size(); i++){
			MegaNode node = megaApi.getNodeByHandle(handleList.get(i));
			megaApi.remove(node);
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
	protected void onDestroy(){
		log("onDestroy()");

		super.onDestroy();    	    	

		if(megaApi != null)
		{
			megaApi.removeGlobalListener(this);	
			megaApi.removeTransferListener(this);
			megaApi.removeRequestListener(this);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		log("onPrepareOptionsMenu----------------------------------");

		if(cflF!=null){
			if(cflF.isVisible()){
				log("visible ContacFileListProperties");
				if(shareMenuItem!=null){
					shareMenuItem.setVisible(true);
					viewSharedItem.setVisible(false);
				}
			}
		}

		super.onPrepareOptionsMenu(menu);
		return true;

	}

	public void setParentHandle(long parentHandle) {
		this.parentHandle = parentHandle;
	}

	public void pickFolderToShare(String email){

//		MegaUser user = megaApi.getContact(email);
		if (email != null){
			Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
			intent.setAction(FileExplorerActivityLollipop.ACTION_SELECT_FOLDER_TO_SHARE);
			ArrayList<String> contacts = new ArrayList<String>();
//			String[] longArray = new String[1];
//			longArray[0] = email;
			contacts.add(email);
			intent.putExtra("SELECTED_CONTACTS", contacts);
			startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER);
		}
		else{
			CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
			if(coordinatorFragment!=null){
				showSnackbar(getString(R.string.error_sharing_folder), coordinatorFragment);
			}
			else{
				showSnackbar(getString(R.string.error_sharing_folder), fragmentContainer);
			}
			log("Error sharing folder");
		}
	}

	@SuppressLint("NewApi")
	public void onFileClick(ArrayList<Long> handleList) {
		long size = 0;
		long[] hashes = new long[handleList.size()];
		for (int i = 0; i < handleList.size(); i++) {
			hashes[i] = handleList.get(i);
			size += megaApi.getNodeByHandle(hashes[i]).getSize();
		}

		if (dbH == null) {
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
			//			dbH = new DatabaseHandler(getApplicationContext());
		}

		boolean askMe = true;
		String downloadLocationDefaultPath = "";
		prefs = dbH.getPreferences();
		if (prefs != null) {
			if (prefs.getStorageAskAlways() != null) {
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())) {
					if (prefs.getStorageDownloadLocation() != null) {
						if (prefs.getStorageDownloadLocation().compareTo("") != 0) {
							askMe = false;
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}

		if (askMe) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
						intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX,getString(R.string.context_download_to));
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
										intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX,getString(R.string.context_download_to));
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
											CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
											if(coordinatorFragment!=null){
												showSnackbar(getString(R.string.general_download), coordinatorFragment);
											}
											else{
												showSnackbar(getString(R.string.general_download), fragmentContainer);
											}
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
					intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX,getString(R.string.context_download_to));
					intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
					intent.setClass(this, FileStorageActivityLollipop.class);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
					startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
				}
			}
			else{
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX,getString(R.string.context_download_to));
				intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
				intent.setClass(this, FileStorageActivityLollipop.class);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
				startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}
		} else {
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}
	}

	public void downloadTo(String parentPath, String url, long size,
			long[] hashes) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}
		
		double availableFreeSpace = Double.MAX_VALUE;
		try {
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double) stat.getAvailableBlocks()
					* (double) stat.getBlockSize();
		} catch (Exception ex) {
		}

		if (hashes == null) {
			if (url != null) {
				if (availableFreeSpace < size) {
					Util.showErrorAlertDialog(
							getString(R.string.error_not_enough_free_space),
							false, this);
					return;
				}

				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				service.putExtra(DownloadService.EXTRA_CONTACT_ACTIVITY, true);
				startService(service);
			}
		} else {
			if (hashes.length == 1) {
				MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);
				if ((tempNode != null)
						&& tempNode.getType() == MegaNode.TYPE_FILE) {
					log("ISFILE");
					String localPath = Util.getLocalFile(this,tempNode.getName(), tempNode.getSize(), parentPath);
					if (localPath != null) {
						try {
							Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName()));
						} catch (Exception e) {
						}

//						if(MimeType.typeForName(tempNode.getName()).isPdf()){
//
//							File pdfFile = new File(localPath);
//
//							Intent intentPdf = new Intent();
//							intentPdf.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
//							intentPdf.setClass(this, OpenPDFActivity.class);
//							intentPdf.setAction("android.intent.action.VIEW");
//							this.startActivity(intentPdf);
//
//						}
//						else 
//						if(MimeTypeList.typeForName(tempNode.getName()).isZip()){
//
//							File zipFile = new File(localPath);
//
//							Intent intentZip = new Intent();
//							intentZip.setClass(this, ZipBrowserActivityLollipop.class);
//							intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, zipFile.getAbsolutePath());
//							intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, tempNode.getHandle());
//
//							this.startActivity(intentZip);
//
//						}
//						else{

							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							viewIntent.setDataAndType(Uri.fromFile(new File(localPath)),
									MimeTypeList.typeForName(tempNode.getName()).getType());
							if (MegaApiUtils.isIntentAvailable(this, viewIntent))
								startActivity(viewIntent);
							else {
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								intentShare.setDataAndType(Uri.fromFile(new File(localPath)),
										MimeTypeList.typeForName(tempNode.getName()).getType());
								if (MegaApiUtils.isIntentAvailable(this, intentShare))
									startActivity(intentShare);
								String toastMessage = getString(R.string.general_already_downloaded) + ": " + localPath;
								CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
								if(coordinatorFragment!=null){
									showSnackbar(toastMessage, coordinatorFragment);
								}
								else{
									showSnackbar(toastMessage, fragmentContainer);
								}
							}
//						}
						return;
					}
				}
			}

			for (long hash : hashes) {
				MegaNode node = megaApi.getNodeByHandle(hash);
				if (node != null) {
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					if (node.getType() == MegaNode.TYPE_FOLDER) {
						getDlList(dlFiles, node, new File(parentPath, new String(node.getName())));
					} else {
						dlFiles.put(node, parentPath);
					}

					for (MegaNode document : dlFiles.keySet()) {

						String path = dlFiles.get(document);

						if (availableFreeSpace < document.getSize()) {
							Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space)	+ " (" + new String(document.getName()) + ")", false, this);
							continue;
						}

						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
						service.putExtra(DownloadService.EXTRA_URL, url);
						service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						service.putExtra(DownloadService.EXTRA_CONTACT_ACTIVITY, true);
						startService(service);
					}
				} else if (url != null) {
					if (availableFreeSpace < size) {
						Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space), false, this);
						continue;
					}

					Intent service = new Intent(this, DownloadService.class);
					service.putExtra(DownloadService.EXTRA_HASH, hash);
					service.putExtra(DownloadService.EXTRA_URL, url);
					service.putExtra(DownloadService.EXTRA_SIZE, size);
					service.putExtra(DownloadService.EXTRA_PATH, parentPath);
					service.putExtra(DownloadService.EXTRA_CONTACT_ACTIVITY, true);
					startService(service);
				} else {
					log("node not found");
				}
			}
		}
	}

	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent,
			File folder) {

		if (megaApi.getRootNode() == null)
			return;

		folder.mkdir();
		ArrayList<MegaNode> nodeList = megaApi.getChildren(parent, orderGetChildren);
		for (int i = 0; i < nodeList.size(); i++) {
			MegaNode document = nodeList.get(i);
			if (document.getType() == MegaNode.TYPE_FOLDER) {
				File subfolder = new File(folder,
						new String(document.getName()));
				getDlList(dlFiles, document, subfolder);
			} else {
				dlFiles.put(document, folder.getAbsolutePath());
			}
		}
	}

	public void moveToTrash(final ArrayList<Long> handleList){
		log("moveToTrash: ");
		moveToRubbish=true;
		if (!Util.isOnline(this)) {
			CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
			if(coordinatorFragment!=null){
				showSnackbar(getString(R.string.error_server_connection_problem), coordinatorFragment);
			}
			else{
				showSnackbar(getString(R.string.error_server_connection_problem), fragmentContainer);
			}
			return;
		}

		MultipleRequestListener moveMultipleListener = null;
		MegaNode parent;
		//Check if the node is not yet in the rubbish bin (if so, remove it)
		if(handleList!=null){
			if(handleList.size()>1){
				log("MOVE multiple: "+handleList.size());
				moveMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_SEND_RUBBISH, this);
				for (int i=0;i<handleList.size();i++){
					megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)), megaApi.getRubbishNode(), moveMultipleListener);
				}
			}
			else{
				log("MOVE single");
				megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(0)), megaApi.getRubbishNode(), this);
			}
		}
		else{
			log("handleList NULL");
			return;
		}
	}

	public void showRenameDialog(final MegaNode document, String text) {

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, document.isFolder());
		input.setSingleLine();
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename), KeyEvent.KEYCODE_ENTER);
		input.setText(text);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, boolean hasFocus) {
				if (hasFocus) {
					if (document.isFolder()) {
						input.setSelection(0, input.getText().length());
					} else {
						String[] s = document.getName().split("\\.");
						if (s != null) {
							int numParts = s.length;
							int lastSelectedPos = 0;
							if (numParts == 1) {
								input.setSelection(0, input.getText().length());
							} else if (numParts > 1) {
								for (int i = 0; i < (numParts - 1); i++) {
									lastSelectedPos += s[i].length();
									lastSelectedPos++;
								}
								lastSelectedPos--; // The last point should not
								// be selected)
								input.setSelection(0, lastSelectedPos);
							}
						}
						// showKeyboardDelayed(v);
					}
				}
			}
		});

		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_rename) + " " + new String(document.getName()), null, input);
		builder.setPositiveButton(getString(R.string.context_rename), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString().trim();
				if (value.length() == 0) {
					return;
				}
				rename(document, value);
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
					rename(document, value);
					return true;
				}
				return false;
			}
		});
	}

	private void rename(MegaNode document, String newName) {
		if (newName.equals(document.getName())) {
			return;
		}

		if (!Util.isOnline(this)) {
			CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
			if(coordinatorFragment!=null){
				showSnackbar(getString(R.string.error_server_connection_problem), coordinatorFragment);
			}
			else{
				showSnackbar(getString(R.string.error_server_connection_problem), fragmentContainer);
			}
			return;
		}

		if (isFinishing()) {
			return;
		}

		ProgressDialog temp = null;
		try {
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_renaming));
			temp.show();
		} catch (Exception e) {
			return;
		}
		statusDialog = temp;

		log("renaming " + document.getName() + " to " + newName);

		megaApi.renameNode(document, newName, this);
	}

	public void showMoveLollipop(ArrayList<Long> handleList){
		moveToRubbish=false;
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}

	public void showCopyLollipop(ArrayList<Long> handleList) {

		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i = 0; i < handleList.size(); i++) {
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (intent == null) {
			return;
		}

		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER	&& resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent
					.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);

			downloadTo(parentPath, url, size, hashes);

			CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
			if(coordinatorFragment!=null){
				showSnackbar(getString(R.string.download_began), coordinatorFragment);
			}
			else{
				showSnackbar(getString(R.string.download_began), fragmentContainer);
			}
		} 
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER	&& resultCode == RESULT_OK) {
			if (!Util.isOnline(this)) {
				CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
				if(coordinatorFragment!=null){
					showSnackbar(getString(R.string.error_server_connection_problem), coordinatorFragment);
				}
				else{
					showSnackbar(getString(R.string.error_server_connection_problem), fragmentContainer);
				}
				return;
			}

			ProgressDialog temp = null;
			try {
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_copying));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;

			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;

			sendToInbox=false;

			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for (int i = 0; i < copyHandles.length; i++) {
				log("NODO A COPIAR: " + megaApi.getNodeByHandle(copyHandles[i]).getName());
				log("DONDE: " + parent.getName());
				log("NODOS: " + copyHandles[i] + "_" + parent.getHandle());
				megaApi.copyNode(megaApi.getNodeByHandle(copyHandles[i]), parent, this);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {

			if (!Util.isOnline(this)) {
				CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
				if(coordinatorFragment!=null){
					showSnackbar(getString(R.string.error_server_connection_problem), coordinatorFragment);
				}
				else{
					showSnackbar(getString(R.string.error_server_connection_problem), fragmentContainer);
				}
				return;
			}
			
			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);
//			final int totalMoves = moveHandles.length;
			moveToRubbish=false;
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			
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
		else if (requestCode == REQUEST_CODE_GET && resultCode == RESULT_OK) {
			Uri uri = intent.getData();
			intent.setAction(Intent.ACTION_GET_CONTENT);
			FilePrepareTask filePrepareTask = new FilePrepareTask(this);
			filePrepareTask.execute(intent);
			ProgressDialog temp = null;
			try {
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.upload_prepare));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;
		} 
		else if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {

			if (!Util.isOnline(this)) {
				CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
				if(coordinatorFragment!=null){
					showSnackbar(getString(R.string.error_server_connection_problem), coordinatorFragment);
				}
				else{
					showSnackbar(getString(R.string.error_server_connection_problem), fragmentContainer);
				}
				return;
			}

			final ArrayList<String> selectedContacts = intent.getStringArrayListExtra("SELECTED_CONTACTS");
			final long folderHandle = intent.getLongExtra("SELECT", 0);			

			final MegaNode parent = megaApi.getNodeByHandle(folderHandle);

			if (parent.isFolder()){
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
				dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {

						ProgressDialog temp = null;
						try{
							temp = new ProgressDialog(contactPropertiesMainActivity);
							temp.setMessage(getString(R.string.context_sharing_folder));
							temp.show();
						}
						catch(Exception e){
							return;
						}
						statusDialog = temp;
						permissionsDialog.dismiss();

						log("item "+item);

						switch(item) {
						case 0:{
							for (int i=0;i<selectedContacts.size();i++){
								MegaUser user= megaApi.getContact(selectedContacts.get(i));
								log("user: "+user);
								log("useremail: "+userEmail);
								log("parentNode: "+parent.getName()+"_"+parent.getHandle());
								megaApi.share(parent, user, MegaShare.ACCESS_READ,contactPropertiesMainActivity);
							}
							break;
						}
						case 1:{	                    	
							for (int i=0;i<selectedContacts.size();i++){
								MegaUser user= megaApi.getContact(selectedContacts.get(i));
								megaApi.share(parent, user, MegaShare.ACCESS_READWRITE,contactPropertiesMainActivity);
							}
							break;
						}
						case 2:{                   	
							for (int i=0;i<selectedContacts.size();i++){
								MegaUser user= megaApi.getContact(selectedContacts.get(i));
								megaApi.share(parent, user, MegaShare.ACCESS_FULL,contactPropertiesMainActivity);
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
				/*int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
				View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
				titleDivider.setBackgroundColor(resources.getColor(R.color.mega));*/
			}
		}		

		else if (requestCode == REQUEST_CODE_GET_LOCAL && resultCode == RESULT_OK) {

			String folderPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			ArrayList<String> paths = intent.getStringArrayListExtra(FileStorageActivityLollipop.EXTRA_FILES);

			int i = 0;

			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode == null) {
				parentNode = megaApi.getRootNode();
			}

			for (String path : paths) {
				Intent uploadServiceIntent = new Intent(this, UploadService.class);
				File file = new File(path);
				if (file.isDirectory()) {
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, file.getName());
					log("FOLDER: EXTRA_FILEPATH: " + file.getAbsolutePath());
					log("FOLDER: EXTRA_NAME: " + file.getName());
				} else {
					ShareInfo info = ShareInfo.infoFromFile(file);
					if (info == null) {
						continue;
					}
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
					uploadServiceIntent.putExtra(UploadService.EXTRA_SIZE, info.getSize());

					log("FILE: EXTRA_FILEPATH: " + info.getFileAbsolutePath());
					log("FILE: EXTRA_NAME: " + info.getTitle());
					log("FILE: EXTRA_SIZE: " + info.getSize());
				}

				uploadServiceIntent.putExtra(UploadService.EXTRA_FOLDERPATH, folderPath);
				uploadServiceIntent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				log("PARENTNODE: " + parentNode.getHandle() + "___" + parentNode.getName());
				CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
				if(coordinatorFragment!=null){
					showSnackbar(getString(R.string.upload_began), coordinatorFragment);
				}
				else{
					showSnackbar(getString(R.string.upload_began), fragmentContainer);
				}
				startService(uploadServiceIntent);
				i++;
			}
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK) {
			log("onActivityResult REQUEST_CODE_SELECT_CONTACT OK");


			final ArrayList<String> contactsData = intent.getStringArrayListExtra(ContactsExplorerActivityLollipop.EXTRA_CONTACTS);
			final int multiselectIntent = intent.getIntExtra("MULTISELECT", -1);

			if(multiselectIntent==0){
				//Send one file to one contact
				final long nodeHandle = intent.getLongExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, -1);
				sendToInbox(nodeHandle, contactsData);
			}
			else{
				//Send multiple files to one contact
				final long[] nodeHandles = intent.getLongArrayExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE);
				sendToInbox(nodeHandles, contactsData);
			}
		}

	}

	public void sendToInbox(long[] nodeHandles, ArrayList<String> selectedContacts) {

		sendToInbox=true;

		if (!Util.isOnline(this)) {
			CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
			if(coordinatorFragment!=null){
				showSnackbar(getString(R.string.error_server_connection_problem), coordinatorFragment);
			}
			else{
				showSnackbar(getString(R.string.error_server_connection_problem), fragmentContainer);
			}
			return;
		}

		if(nodeHandles!=null){
			MultipleRequestListener sendMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_FILES_SEND_INBOX, this);
			MegaUser u = megaApi.getContact(selectedContacts.get(0));
			if(nodeHandles.length>1){
				log("many files to one contact");
				for(int j=0; j<nodeHandles.length;j++){

					final MegaNode node = megaApi.getNodeByHandle(nodeHandles[j]);

					if(u!=null){
						log("Send: "+ node.getName() + " to "+ u.getEmail());
						megaApi.sendFileToUser(node, u, sendMultipleListener);
					}
					else{
						log("Send File to a NON contact! ");
						megaApi.sendFileToUser(node, selectedContacts.get(0), sendMultipleListener);
					}
				}
			}
			else{
				log("one file to many contacts");

				final MegaNode node = megaApi.getNodeByHandle(nodeHandles[0]);
				if(u!=null){
					log("Send: "+ node.getName() + " to "+ u.getEmail());
					megaApi.sendFileToUser(node, u, this);
				}
				else{
					log("Send File to a NON contact! ");
					megaApi.sendFileToUser(node, selectedContacts.get(0), this);
				}
			}
		}
	}

	public void sendToInbox(long fileHandle, ArrayList<String> selectedContacts){

		sendToInbox=true;

		if (!Util.isOnline(this)) {
			CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
			if(coordinatorFragment!=null){
				showSnackbar(getString(R.string.error_server_connection_problem), coordinatorFragment);
			}
			else{
				showSnackbar(getString(R.string.error_server_connection_problem), fragmentContainer);
			}
			return;
		}

		MultipleRequestListener sendMultipleListener = null;
		MegaNode node = megaApi.getNodeByHandle(fileHandle);
		if(node!=null)
		{
			log("File to send: "+node.getName());
			if(selectedContacts.size()>1){
				log("File to multiple contacts");
				sendMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_CONTACTS_SEND_INBOX, this);
				for (int i=0;i<selectedContacts.size();i++){
					MegaUser user= megaApi.getContact(selectedContacts.get(i));

					if(user!=null){
						log("Send File to contact: "+user.getEmail());
						megaApi.sendFileToUser(node, user, sendMultipleListener);
					}
					else{
						log("Send File to a NON contact! ");
						megaApi.sendFileToUser(node, selectedContacts.get(i), sendMultipleListener);
					}
				}
			}
			else{
				log("File to a single contact");
				MegaUser user= megaApi.getContact(selectedContacts.get(0));
				if(user!=null){
					log("Send File to contact: "+user.getEmail());
					megaApi.sendFileToUser(node, user, this);
				}
				else{
					log("Send File to a NON contact! ");
					megaApi.sendFileToUser(node, selectedContacts.get(0), this);
				}
			}
		}
	}

	public void onIntentProcessed(List<ShareInfo> infos) {
//		List<ShareInfo> infos = filePreparedInfos;
		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			} catch (Exception ex) {
			}
		}

		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
		if (parentNode == null) {
			Util.showErrorAlertDialog(
					getString(R.string.error_temporary_unavaible), false, this);
			return;
		}

		if (infos == null) {
			Util.showErrorAlertDialog(getString(R.string.upload_can_not_open),
					false, this);
		} else {
			CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
			if(coordinatorFragment!=null){
				showSnackbar(getString(R.string.upload_began), coordinatorFragment);
			}
			else{
				showSnackbar(getString(R.string.upload_began), fragmentContainer);
			}
			for (ShareInfo info : infos) {
				Intent intent = new Intent(this, UploadService.class);
				intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
				intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
				intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
				startService(intent);
			}
		}
	}

	@Override
	public void onBackPressed() {

		if (cflF != null){
			if (cflF.isVisible()){
				if (cflF.onBackPressed() == 0){
					log("onBackPressed == 0");
					finish();
					return;
				}
			}
		}
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferStart");

		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		tL = megaApi.getTransfers();

		if (cflF != null){
			for(int i=0; i<tL.size(); i++){

				MegaTransfer tempT = tL.get(i);
				if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
					long handleT = tempT.getNodeHandle();
					MegaNode nodeT = megaApi.getNodeByHandle(handleT);
					MegaNode parentT = megaApi.getParentNode(nodeT);

					if (parentT != null){
						if(parentT.getHandle() == this.parentHandle){	
							mTHash.put(handleT,tempT);						
						}
					}
				}
			}

			cflF.setTransfers(mTHash);
		}

		log("onTransferStart: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		log("onTransferFinish");

		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		tL = megaApi.getTransfers();

		if (cflF != null){
			for(int i=0; i<tL.size(); i++){

				MegaTransfer tempT = tL.get(i);
				if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
					long handleT = tempT.getNodeHandle();
					MegaNode nodeT = megaApi.getNodeByHandle(handleT);
					MegaNode parentT = megaApi.getParentNode(nodeT);

					if (parentT != null){
						if(parentT.getHandle() == this.parentHandle){	
							mTHash.put(handleT,tempT);						
						}
					}
				}
			}

			cflF.setTransfers(mTHash);
		}

		log("onTransferFinish: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferUpdate: " + transfer.getFileName() + " - " + transfer.getTag());

		if (cflF != null){
			if (cflF.isVisible()){
				if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD){
					Time now = new Time();
					now.setToNow();
					long nowMillis = now.toMillis(false);
					if (lastTimeOnTransferUpdate < 0){
						lastTimeOnTransferUpdate = now.toMillis(false);
						cflF.setCurrentTransfer(transfer);
					}
					else if ((nowMillis - lastTimeOnTransferUpdate) > Util.ONTRANSFERUPDATE_REFRESH_MILLIS){
						lastTimeOnTransferUpdate = nowMillis;
						cflF.setCurrentTransfer(transfer);
					}			
				}		
			}
		}
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer,
			byte[] buffer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		if (cflF != null){
			if (cflF.isVisible()){
				cflF.setNodes(parentHandle);
			}
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub

	}

	public static void log(String log) {
		Util.log("ContactFileListActivityLollipop", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_MOVE) {
			log("move request start");
		} 
		else if (request.getType() == MegaRequest.TYPE_REMOVE) {
			log("remove request start");
		} 
		else if (request.getType() == MegaRequest.TYPE_EXPORT) {
			log("export request start");
		} 
		else if (request.getType() == MegaRequest.TYPE_RENAME) {
			log("rename request start");
		} 
		else if (request.getType() == MegaRequest.TYPE_COPY) {
			log("copy request start");
		}
		else if (request.getType() == MegaRequest.TYPE_SHARE) {
			log("share request start");
		}
	}

	public void askConfirmationMoveToRubbish(final ArrayList<Long> handleList){
		log("askConfirmationMoveToRubbish");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						moveToTrash(handleList);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		if(handleList!=null){

			if (handleList.size() > 0){
				android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//				builder.setTitle(getResources().getString(R.string.section_rubbish_bin));
				if (handleList.size() > 1){
					builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish_plural));
				}
				else{
					builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish));
				}
				builder.setPositiveButton(R.string.general_move, dialogClickListener);
				builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
				builder.show();
			}
		}
		else{
			log("handleList NULL");
			return;
		}
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate");		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");

		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
				if(cflF!=null && cflF.isVisible()){
					if(coordinatorFragment!=null){
						showSnackbar(getString(R.string.context_folder_created), coordinatorFragment);
					}
					else{
						showSnackbar(getString(R.string.context_folder_created), fragmentContainer);
					}
					cflF.setNodes();
				}
			}
			else{
				CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
				if(cflF!=null && cflF.isVisible()){
					if(coordinatorFragment!=null){
						showSnackbar(getString(R.string.context_folder_no_created), coordinatorFragment);
					}
					else{
						showSnackbar(getString(R.string.context_folder_no_created), fragmentContainer);
					}
					cflF.setNodes();
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){

				CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
				if(cflF!=null && cflF.isVisible()){
					if(coordinatorFragment!=null){
						showSnackbar(getString(R.string.context_correctly_renamed), coordinatorFragment);
					}
					else{
						showSnackbar(getString(R.string.context_correctly_renamed), fragmentContainer);
					}
				}
			}
			else{
				CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
				if(cflF!=null && cflF.isVisible()){
					if(coordinatorFragment!=null){
						showSnackbar(getString(R.string.context_no_renamed), coordinatorFragment);
					}
					else{
						showSnackbar(getString(R.string.context_no_renamed), fragmentContainer);
					}
				}
			}
			log("rename nodes request finished");			
		}
		else if (request.getType() == MegaRequest.TYPE_COPY) {
			try {
				statusDialog.dismiss();
			} catch (Exception ex) {
			}

			if(sendToInbox) {
				log("sendToInbox: " + e.getErrorCode() + " " + e.getErrorString());
				if (e.getErrorCode() == MegaError.API_OK){

					CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
					if(cflF!=null && cflF.isVisible()){
						if(coordinatorFragment!=null){
							showSnackbar(getString(R.string.context_correctly_sent_node), coordinatorFragment);
						}
						else{
							showSnackbar(getString(R.string.context_correctly_sent_node), fragmentContainer);
						}
					}
				}
				else{
					CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
					if(cflF!=null && cflF.isVisible()){
						if(coordinatorFragment!=null){
							showSnackbar(getString(R.string.context_no_sent_node), coordinatorFragment);
						}
						else{
							showSnackbar(getString(R.string.context_no_sent_node), fragmentContainer);
						}
					}
				}
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){

					CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
					if(cflF!=null && cflF.isVisible()){
						if(coordinatorFragment!=null){
							showSnackbar(getString(R.string.context_correctly_copied), coordinatorFragment);
						}
						else{
							showSnackbar(getString(R.string.context_correctly_copied), fragmentContainer);
						}
					}
				}
				else{
					CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
					if(cflF!=null && cflF.isVisible()){
						if(coordinatorFragment!=null){
							showSnackbar(getString(R.string.context_no_copied), coordinatorFragment);
						}
						else{
							showSnackbar(getString(R.string.context_no_copied), fragmentContainer);
						}
					}
				}
			}
			sendToInbox=false;
			log("copy nodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}

			if(moveToRubbish){
				log("Finish move to Rubbish!");
				if (e.getErrorCode() == MegaError.API_OK){

					CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
					if(cflF!=null && cflF.isVisible()){
						if(coordinatorFragment!=null){
							showSnackbar(getString(R.string.context_correctly_moved_to_rubbish), coordinatorFragment);
						}
						else{
							showSnackbar(getString(R.string.context_correctly_moved_to_rubbish), fragmentContainer);
						}
					}
				}
				else{
					CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
					if(cflF!=null && cflF.isVisible()){
						if(coordinatorFragment!=null){
							showSnackbar(getString(R.string.context_no_moved), coordinatorFragment);
						}
						else{
							showSnackbar(getString(R.string.context_no_moved), fragmentContainer);
						}
					}
				}
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){

					CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
					if(cflF!=null && cflF.isVisible()){
						if(coordinatorFragment!=null){
							showSnackbar(getString(R.string.context_correctly_moved), coordinatorFragment);
						}
						else{
							showSnackbar(getString(R.string.context_correctly_moved), fragmentContainer);
						}
					}
				}
				else{
					CoordinatorLayout coordinatorFragment = (CoordinatorLayout) fragmentContainer.findViewById(R.id.contact_file_list_coordinator_layout);
					if(cflF!=null && cflF.isVisible()){
						if(coordinatorFragment!=null){
							showSnackbar(getString(R.string.context_no_moved), coordinatorFragment);
						}
						else{
							showSnackbar(getString(R.string.context_no_moved), fragmentContainer);
						}
					}
				}
			}
			moveToRubbish=false;

			log("move request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_SHARE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				log("Shared folder correctly: "+request.getNodeHandle());
				Toast.makeText(this, getString(R.string.context_correctly_shared), Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(this, getString(R.string.context_no_shared), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api,
			ArrayList<MegaContactRequest> requests) {
		// TODO Auto-generated method stub
		
	}

	public void showOptionsPanel(MegaNode node){
		log("showOptionsPanel");
		if(node!=null){
			this.selectedNode = node;
			ContactFileListBottomSheetDialogFragment bottomSheetDialogFragment = new ContactFileListBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void showSnackbar(String s, View view){
		log("showSnackbar");
		Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}

	public MegaNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(MegaNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public boolean isEmptyParentHandleStack() {
		if(cflF!=null){
			return cflF.isEmptyParentHandleStack();
		}
		log("Fragment NULL");
		return true;
	}

	public long getParentHandle() {

		if(cflF!=null){
			return cflF.getParentHandle();
		}
		return -1;
	}
}
