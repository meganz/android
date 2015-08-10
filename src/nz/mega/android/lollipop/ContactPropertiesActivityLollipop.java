package nz.mega.android.lollipop;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.mega.android.ContactFileListFragment;
import nz.mega.android.DatabaseHandler;
import nz.mega.android.DownloadService;
import nz.mega.android.FileStorageActivity;
import nz.mega.android.FileStorageActivity.Mode;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaPreferences;
import nz.mega.android.MimeTypeList;
import nz.mega.android.PinActivity;
import nz.mega.android.R;
import nz.mega.android.ShareInfo;
import nz.mega.android.UploadService;
import nz.mega.android.ZipBrowserActivity;
import nz.mega.android.utils.FixedCenterCrop;
import nz.mega.android.utils.Util;
import nz.mega.components.EditTextCursorWatcher;
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v7.app.ActionBar;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


public class ContactPropertiesActivityLollipop extends PinActivity implements MegaGlobalListenerInterface, MegaTransferListenerInterface, MegaRequestListenerInterface {

	TextView nameView;
	TextView contentTextView;
	FixedCenterCrop imageView;
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
	AlertDialog permissionsDialog;

	ContactPropertiesFragmentLollipop cpF;
	ContactFileListFragment cflF;

	MenuItem shareMenuItem;
	MenuItem viewSharedItem;

	public static final int CONTACT_PROPERTIES = 1000;
	public static final int CONTACT_FILE_LIST = 1001;

	public static int REQUEST_CODE_GET = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_GET_LOCAL = 1003;
	public static final int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_SELECT_FOLDER = 1008;

	static ContactPropertiesActivityLollipop contactPropertiesMainActivity;

	private static int EDIT_TEXT_ID = 2;

	long parentHandle = -1;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	private AlertDialog renameDialog;
	ProgressDialog statusDialog;

	ArrayList<MegaTransfer> tL;
	long lastTimeOnTransferUpdate = -1;

	private List<ShareInfo> filePreparedInfos;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		megaApi.addGlobalListener(this);
		megaApi.addTransferListener(this);

		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(true);
		aB.setDisplayShowTitleEnabled(true);
		aB.setLogo(R.drawable.ic_action_navigation_accept_white);

		contactPropertiesMainActivity=this;

		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		Bundle extras = getIntent().getExtras();
		if (extras != null){
			userEmail = extras.getString("name");
			aB.setTitle(userEmail);
			setContentView(R.layout.activity_main_contact_properties);

			int currentFragment = CONTACT_PROPERTIES;
			selectContactFragment(currentFragment);
		}
	}

	@Override
	protected void onResume() {
		log("onResume");
		super.onResume();

		Intent intent = getIntent(); 

		if (intent != null) { 
			if (intent.getAction() != null){ 
				if(getIntent().getAction().equals(ManagerActivityLollipop.ACTION_EXPLORE_ZIP)){  

					String pathZip=intent.getExtras().getString(ManagerActivityLollipop.EXTRA_PATH_ZIP);    				

					log("Path: "+pathZip);

					//Lanzar nueva activity ZipBrowserActivity

					Intent intentZip = new Intent(this, ZipBrowserActivity.class);    				
					intentZip.putExtra(ZipBrowserActivity.EXTRA_PATH_ZIP, pathZip);
					startActivity(intentZip);


				}
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

	public void selectContactFragment(int currentFragment){
		switch(currentFragment){
			case CONTACT_PROPERTIES:{
				if (cpF == null){
					cpF = new ContactPropertiesFragmentLollipop();
				}
				cpF.setUserEmail(userEmail);
	
				if (aB != null){
					aB.setTitle(userEmail);
				}
	
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_contact_properties, cpF, "cpF").commit();
	
				break;
			}
			case CONTACT_FILE_LIST:{
				if (cflF == null){
					cflF = new ContactFileListFragment();
				}
				cflF.setUserEmail(userEmail);
	
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_contact_properties, cflF, "cflF").commit();
	
				break;
			}
		}
	}
	
	public void leaveIncomingShare (MegaNode n){
		log("leaveIncomingShare");
		//TODO
//		ProgressDialog temp = null;
//		try{
//			temp = new ProgressDialog(this);
//			temp.setMessage(getString(R.string.leave_incoming_share)); 
//			temp.show();
//		}
//		catch(Exception e){
//			return;
//		}
//		statusDialog = temp;
		megaApi.remove(n);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
		case android.R.id.home: {
			onBackPressed();
			return true;
		}
		case R.id.action_contact_file_list_share: {
			
    		cpF = (ContactPropertiesFragmentLollipop) getSupportFragmentManager().findFragmentByTag("cpF");
        	if (cpF != null){
        		MegaUser user = megaApi.getContact(cpF.getUserEmail());
    			if(user == null)
    			{
    				return true;
    			}
    			
    			this.pickFolderToShare(user);
        	}
        	else{
        		MegaUser user = megaApi.getContact(cflF.getUserEmail());
    			if(user == null)
    			{
    				return true;
    			}
    			
    			this.pickFolderToShare(user);   	}
			return true;
		}
		case R.id.action_contact_file_list_view_share: {
			if (userEmail.compareTo(this.userEmail) == 0){
				selectContactFragment(CONTACT_FILE_LIST);
			}
			return true;
		}			
		default: {
			return super.onOptionsItemSelected(item);
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

	public void onContentClick(String userEmail){
		if (userEmail.compareTo(this.userEmail) == 0){
			selectContactFragment(CONTACT_FILE_LIST);
		}
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

		//		 if(contactPropertiesVisible){				
		//				log("Es visible ContactProperties--------------");
		//				
		//		}
		//		else{
		//			log("Es visible ContacFileListProperties-----------------");
		//			
		//		}

		if(cflF!=null){
			if(cflF.isVisible()){
				log("Es visible ContacFileListProperties");
				shareMenuItem.setVisible(true);		
				viewSharedItem.setVisible(false);
				//					shareMenuItem.setVisible(true);		
				//					viewSharedItem.setVisible(false);
			}
		}		
		if(cpF!=null){
			if(cpF.isVisible()){
				log("Es visible ContactProperties");
				shareMenuItem.setVisible(true);		
				viewSharedItem.setVisible(true);
				//					shareMenuItem.setVisible(true);		
				//					viewSharedItem.setVisible(true);
			}
		}	

		super.onPrepareOptionsMenu(menu);
		return true;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenu-----------------------------------");		

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_contact_file_list, menu);
		shareMenuItem = menu.findItem(R.id.action_contact_file_list_share);
		viewSharedItem = menu.findItem(R.id.action_contact_file_list_view_share);		

		if(cflF!=null){
			if(cflF.isVisible()){
				shareMenuItem.setVisible(true);		
				viewSharedItem.setVisible(false);
			}
		}		
		if(cpF!=null){
			if(cpF.isVisible()){
				shareMenuItem.setVisible(true);		
				viewSharedItem.setVisible(true);
			}
		}	
		return super.onCreateOptionsMenu(menu);
	}

	public void setParentHandle(long parentHandle) {
		this.parentHandle = parentHandle;
	}

	public void pickFolderToShare(MegaUser user){

		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_SELECT_FOLDER);
		String[] longArray = new String[1];		
		longArray[0] = user.getEmail();		
		intent.putExtra("SELECTED_CONTACTS", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER);

	}



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
			Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX,
					getString(R.string.context_download_to));
			intent.putExtra(FileStorageActivity.EXTRA_SIZE, size);
			intent.setClass(this, FileStorageActivity.class);
			intent.putExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES, hashes);
			startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
		} else {
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}
	}

	public void downloadTo(String parentPath, String url, long size,
			long[] hashes) {
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
						if(MimeTypeList.typeForName(tempNode.getName()).isZip()){

							File zipFile = new File(localPath);

							Intent intentZip = new Intent();
							intentZip.setClass(this, ZipBrowserActivity.class);
							intentZip.putExtra(ZipBrowserActivity.EXTRA_PATH_ZIP, zipFile.getAbsolutePath());
							intentZip.putExtra(ZipBrowserActivity.EXTRA_HANDLE_ZIP, tempNode.getHandle());

							this.startActivity(intentZip);

						}
						else{

							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							viewIntent.setDataAndType(Uri.fromFile(new File(localPath)),
									MimeTypeList.typeForName(tempNode.getName()).getType());
							if (ManagerActivityLollipop.isIntentAvailable(this, viewIntent))
								startActivity(viewIntent);
							else {
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								intentShare.setDataAndType(Uri.fromFile(new File(localPath)),
										MimeTypeList.typeForName(tempNode.getName()).getType());
								if (ManagerActivityLollipop.isIntentAvailable(this, intentShare))
									startActivity(intentShare);
								String toastMessage = getString(R.string.general_already_downloaded) + ": " + localPath;
								Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
							}
						}
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

	public void moveToTrash(ArrayList<Long> handleList) {

		if (!Util.isOnline(this)) {
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}

		if (isFinishing()) {
			return;
		}

		MegaNode rubbishNode = megaApi.getRubbishNode();

		for (int i = 0; i < handleList.size(); i++) {
			// Check if the node is not yet in the rubbish bin (if so, remove
			// it)
			MegaNode parent = megaApi.getNodeByHandle(handleList.get(i));
			while (megaApi.getParentNode(parent) != null) {
				parent = megaApi.getParentNode(parent);
			}

			if (parent.getHandle() != megaApi.getRubbishNode().getHandle()) {
				megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)), rubbishNode, this);
			} 
		}

		ProgressDialog temp = null;
		try {
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_move_to_trash));
			temp.show();
		} catch (Exception e) {
			return;
		}
		statusDialog = temp;
	}

	public void showRenameDialog(final MegaNode document, String text) {

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this);
		input.setId(EDIT_TEXT_ID);
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
			Util.showErrorAlertDialog(
					getString(R.string.error_server_connection_problem), false,
					this);
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

	public void showMove(ArrayList<Long> handleList){

		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}

	public void showCopy(ArrayList<Long> handleList) {

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
					.getStringExtra(FileStorageActivity.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivity.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivity.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);

			downloadTo(parentPath, url, size, hashes);
			Util.showToast(this, R.string.download_began);
		} 
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER	&& resultCode == RESULT_OK) {
			if (!Util.isOnline(this)) {
				Util.showErrorAlertDialog(
						getString(R.string.error_server_connection_problem),
						false, this);
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

			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for (int i = 0; i < copyHandles.length; i++) {
				log("NODO A COPIAR: " + megaApi.getNodeByHandle(copyHandles[i]).getName());
				log("DONDE: " + parent.getName());
				log("NODOS: " + copyHandles[i] + "_" + parent.getHandle());
				megaApi.copyNode(megaApi.getNodeByHandle(copyHandles[i]), parent, this);
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

			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}

			final String[] selectedContacts = intent.getStringArrayExtra("SELECTED_CONTACTS");
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
							for (int i=0;i<selectedContacts.length;i++){
								MegaUser user= megaApi.getContact(selectedContacts[i]);
								megaApi.share(parent, user, MegaShare.ACCESS_READ,contactPropertiesMainActivity);
							}
							break;
						}
						case 1:{	                    	
							for (int i=0;i<selectedContacts.length;i++){
								MegaUser user= megaApi.getContact(selectedContacts[i]);
								megaApi.share(parent, user, MegaShare.ACCESS_READWRITE,contactPropertiesMainActivity);
							}
							break;
						}
						case 2:{                   	
							for (int i=0;i<selectedContacts.length;i++){
								MegaUser user= megaApi.getContact(selectedContacts[i]);		                    		
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
				int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
				View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
				titleDivider.setBackgroundColor(resources.getColor(R.color.mega));
			}
		}		

		else if (requestCode == REQUEST_CODE_GET_LOCAL && resultCode == RESULT_OK) {

			String folderPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			ArrayList<String> paths = intent.getStringArrayListExtra(FileStorageActivity.EXTRA_FILES);

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

				startService(uploadServiceIntent);
				i++;
			}

		}
	}

	/*
	 * Background task to process files for uploading
	 */
	private class FilePrepareTask extends
	AsyncTask<Intent, Void, List<ShareInfo>> {
		Context context;

		FilePrepareTask(Context context) {
			this.context = context;
		}

		@Override
		protected List<ShareInfo> doInBackground(Intent... params) {
			return ShareInfo.processIntent(params[0], context);
		}

		@Override
		protected void onPostExecute(List<ShareInfo> info) {
			filePreparedInfos = info;
			onIntentProcessed();
		}
	}

	public void onIntentProcessed() {
		List<ShareInfo> infos = filePreparedInfos;
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
			Toast.makeText(getApplicationContext(),
					getString(R.string.upload_began), Toast.LENGTH_SHORT).show();
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
					selectContactFragment(CONTACT_PROPERTIES);
					return;
				}
			}
		}

		if (cpF != null){
			if (cpF.isVisible()){
				super.onBackPressed();
				return;
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
		Util.log("ContactPropertiesMainActivity", log);
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

	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate");		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish");

		if (request.getType() == MegaRequest.TYPE_RENAME){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, getString(R.string.context_correctly_renamed), Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(this, getString(R.string.context_no_renamed), Toast.LENGTH_LONG).show();
			}
			log("rename nodes request finished");			
		}
		else if (request.getType() == MegaRequest.TYPE_COPY) {
			try {
				statusDialog.dismiss();
			} catch (Exception ex) {
			}

			if (e.getErrorCode() == MegaError.API_OK) {
				Toast.makeText(this, getString(R.string.context_correctly_copied), Toast.LENGTH_SHORT).show();				
			} else {
				Toast.makeText(this, getString(R.string.context_no_copied), Toast.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, getString(R.string.context_correctly_moved), Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(this, getString(R.string.context_no_moved), Toast.LENGTH_LONG).show();
			}
			log("move to rubbish request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_SHARE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
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


}
