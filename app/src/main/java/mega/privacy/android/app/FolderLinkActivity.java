package mega.privacy.android.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.FileStorageActivity.Mode;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;


public class FolderLinkActivity extends PinActivity implements MegaRequestListenerInterface, OnItemClickListener, OnItemLongClickListener{
	
	FolderLinkActivity folderLinkActivity = this;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	
	ActionBar aB;
	
	String url;
	ListView listView;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	
	long parentHandle = -1;
	ArrayList<MegaNode> nodes;
	
	LinearLayout outSpaceLayout=null;
	LinearLayout buttonsLayout=null;
	LinearLayout getProLayout=null;
	MegaBrowserListAdapter adapterList;
	
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	private MenuItem downloadFolderMenuItem;
	private MenuItem importFolderMenuItem;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	
	private ActionMode actionMode;
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = getSelectedDocuments();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					onFileClick(handleList);
					break;
				}
			}
			
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			adapterList.setMultipleSelect(false);
			listView.setOnItemLongClickListener(folderLinkActivity);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = getSelectedDocuments();
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			boolean showLeave = false;
			
			if (selected.size() > 0) {
				showDownload = true;
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(showLeave);
			
			return false;
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenu");
		menu.clear();
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.folder_link_action, menu);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
		

		
		return super.onCreateOptionsMenu(menu);
	}
	
	public boolean onPrepareOptionsMenu(Menu menu) 
	{
		log("onPrepareOptionsMenu");
		menu.clear();

		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.folder_link_action, menu);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
		

		importFolderMenuItem.setVisible(false);
		if(megaApiFolder.getRootNode() == null)
		{
			downloadFolderMenuItem.setVisible(false);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		
		log("retryPendingConnections()");
		if (megaApi != null){
			megaApi.retryPendingConnections();
		}
		// Handle presses on the action bar items

		return false;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
    	
    	log("onCreate()");
    	
		super.onCreate(savedInstanceState);
		
		if (aB == null){
			aB = getSupportActionBar();
		}
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApiFolder = app.getMegaApiFolder();
		megaApi = app.getMegaApi();
		
		setContentView(R.layout.fragment_filebrowserlist);
		
		listView = (ListView) findViewById(R.id.file_list_view_browser);
		emptyImageView = (ImageView) findViewById(R.id.file_list_empty_image);
		emptyTextView = (TextView) findViewById(R.id.file_list_empty_text);
		
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
//
//		outSpaceLayout = (LinearLayout) findViewById(R.id.out_space);
//		outSpaceLayout.setVisibility(View.GONE);
		
		getProLayout=(LinearLayout) findViewById(R.id.get_pro_account);
		getProLayout.setVisibility(View.GONE);
		
//		buttonsLayout = (LinearLayout) findViewById(R.id.buttons_layout);
//		buttonsLayout.setVisibility(View.GONE);
		
		contentText = (TextView) findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);
		
		Intent intent = getIntent();
    	
    	if (intent != null) {
    		if (intent.getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK)){
    			if (parentHandle == -1){
    				url = intent.getDataString();
    				megaApiFolder.loginToFolder(url, this);
    			}
    		}
    	}
    	
    	aB.setTitle("MEGA - " + getString(R.string.general_loading));
    }
	
	@Override
	protected void onDestroy() {

		if (megaApiFolder != null){
			megaApiFolder.removeRequestListener(this);
			megaApiFolder.logout();
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
	
	public void onFileClick(ArrayList<Long> handleList){
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
	
	public void onFolderClick(long handle, long size){
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
	
	public void downloadTo(String parentPath, String url, long size, long [] hashes){
		log("downloadTo");
		
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
						Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, this);
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
					Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space), false, this);
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
		if (intent == null){
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
		
		if (request.getType() == MegaRequest.TYPE_LOGIN){
			if (e.getErrorCode() == MegaError.API_OK){
				megaApiFolder.fetchNodes(this);	
			}
			else{
				try{ 
					AlertDialog.Builder dialogBuilder = Util.getCustomAlertBuilder(this, getString(R.string.general_error_word), getString(R.string.general_error_folder_not_found), null);
					dialogBuilder.setPositiveButton(
						getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Intent backIntent;
								if(folderLinkActivity != null)
									backIntent = new Intent(folderLinkActivity, ManagerActivity.class);
								else
									backIntent = new Intent(FolderLinkActivity.this, ManagerActivity.class);
								
								startActivity(backIntent);
				    			finish();
							}
						});
									
					AlertDialog dialog = dialogBuilder.create();
					dialog.show(); 
					Util.brandAlertDialog(dialog);
				}
				catch(Exception ex){
					Util.showToast(this, getString(R.string.general_error_folder_not_found)); 
	    			finish();
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES)
		{
			if (e.getErrorCode() == MegaError.API_OK) {
				MegaNode rootNode = megaApiFolder.getRootNode();
				if (rootNode != null){

					if(request.getFlag()){
						log("Login into a folder with invalid decryption key");

						try{
							AlertDialog.Builder dialogBuilder = Util.getCustomAlertBuilder(this, getString(R.string.general_error_word), getString(R.string.general_error_invalid_decryption_key), null);
							dialogBuilder.setPositiveButton(
									getString(android.R.string.ok),
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											dialog.dismiss();
											Intent backIntent = new Intent(folderLinkActivity, ManagerActivity.class);
											startActivity(backIntent);
											finish();
										}
									});

							AlertDialog dialog = dialogBuilder.create();
							dialog.show();
							Util.brandAlertDialog(dialog);
						}
						catch(Exception ex){
							Util.showToast(this, getString(R.string.general_error_folder_not_found));
							finish();
						}
					}
					else {
						parentHandle = rootNode.getHandle();
						nodes = megaApiFolder.getChildren(rootNode);
						aB.setTitle(megaApiFolder.getRootNode().getName());
						supportInvalidateOptionsMenu();

						if (adapterList == null){
							adapterList = new MegaBrowserListAdapter(this, nodes, parentHandle, listView, aB, ManagerActivity.FOLDER_LINK_ADAPTER);
						}
						else{
							adapterList.setParentHandle(parentHandle);
							adapterList.setNodes(nodes);
						}

						adapterList.setPositionClicked(-1);
						adapterList.setMultipleSelect(false);

						listView.setAdapter(adapterList);
					}
				}
				else{
					try{
						AlertDialog.Builder dialogBuilder = Util.getCustomAlertBuilder(this, getString(R.string.general_error_word), getString(R.string.general_error_folder_not_found), null);;

						dialogBuilder.setPositiveButton(
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									Intent backIntent = new Intent(folderLinkActivity, ManagerActivity.class);
									startActivity(backIntent);
									finish();
								}
							});

						AlertDialog dialog = dialogBuilder.create();
						dialog.show();
						Util.brandAlertDialog(dialog);
					}
					catch(Exception ex){
						Util.showToast(this, getString(R.string.general_error_folder_not_found));
						finish();
					}
				}
			}
			else{
				try{
					AlertDialog.Builder dialogBuilder = null;

					if(e.getErrorCode() == MegaError.API_EBLOCKED){
						dialogBuilder = Util.getCustomAlertBuilder(this, getString(R.string.general_error_folder_not_found), getString(R.string.folder_link_unavaible_ToS_violation), null);
					}
					else if(e.getErrorCode() == MegaError.API_ETOOMANY){
						dialogBuilder = Util.getCustomAlertBuilder(this, getString(R.string.general_error_folder_not_found), getString(R.string.file_link_unavaible_delete_account), null);
					}
					else{
						dialogBuilder = Util.getCustomAlertBuilder(this, getString(R.string.general_error_word), getString(R.string.general_error_folder_not_found), null);
					}

					dialogBuilder.setPositiveButton(
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									Intent backIntent = new Intent(folderLinkActivity, ManagerActivity.class);
									startActivity(backIntent);
									finish();
								}
							});

					AlertDialog dialog = dialogBuilder.create();
					dialog.show();
					Util.brandAlertDialog(dialog);
				}
				catch(Exception ex){
					Util.showToast(this, getString(R.string.general_error_folder_not_found));
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
		Util.log("FolderLinkActivity", message);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (adapterList.getPositionClicked() == -1){
			clearSelections();
			actionMode = startSupportActionMode(new ActionBarCallBack());
			listView.setItemChecked(position, true);
			adapterList.setMultipleSelect(true);
			updateActionModeTitle();
			listView.setOnItemLongClickListener(null);
		}
		return true;
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				listView.setItemChecked(checkedPosition, false);
			}
		}
		updateActionModeTitle();
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null) {
			return;
		}
		List<MegaNode> documents = getSelectedDocuments();
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
			title = "";
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
	
	/*
	 * Get list of all selected documents
	 */
	private List<MegaNode> getSelectedDocuments() {
		ArrayList<MegaNode> documents = new ArrayList<MegaNode>();
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				MegaNode document = adapterList.getDocumentAt(checkedItems.keyAt(i));
				if (document != null){
					documents.add(document);
				}
			}
		}
		return documents;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (adapterList.isMultipleSelect()){
			SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
			if (checkedItems.get(position, false) == true){
				listView.setItemChecked(position, true);
			}
			else{
				listView.setItemChecked(position, false);
			}				
			updateActionModeTitle();
			adapterList.notifyDataSetChanged();
		}
		else{
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);
				
				aB.setTitle(n.getName());
//				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//				((ManagerActivity)context).supportInvalidateOptionsMenu();
				
				parentHandle = nodes.get(position).getHandle();
//				((ManagerActivity)context).setParentHandleBrowser(parentHandle);
				adapterList.setParentHandle(parentHandle);
				nodes = megaApiFolder.getChildren(nodes.get(position), orderGetChildren);
				adapterList.setNodes(nodes);
				listView.setSelection(0);
				
				//If folder has no files
				if (adapterList.getCount() == 0){
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
					intent.putExtra("adapterType", ManagerActivity.FOLDER_LINK_ADAPTER);
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
			  		if (ManagerActivity.isIntentAvailable(this, mediaIntent)){
			  			startActivity(mediaIntent);
			  		}
			  		else{
			  			Toast.makeText(this, getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
			  			adapterList.setPositionClicked(-1);
						adapterList.notifyDataSetChanged();
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						onFileClick(handleList);
			  		}					
				}
				else{
					adapterList.setPositionClicked(-1);
					adapterList.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					onFileClick(handleList);
				}
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		
		if (adapterList != null){
			parentHandle = adapterList.getParentHandle();
			
			if (adapterList.getPositionClicked() != -1){
				adapterList.setPositionClicked(-1);
				adapterList.notifyDataSetChanged();
				return;
			}
			else{
				MegaNode parentNode = megaApiFolder.getParentNode(megaApiFolder.getNodeByHandle(parentHandle));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					aB.setTitle(parentNode.getName());					
					
					supportInvalidateOptionsMenu();
					
					parentHandle = parentNode.getHandle();
					nodes = megaApiFolder.getChildren(parentNode, orderGetChildren);
					adapterList.setNodes(nodes);
					listView.setSelection(0);
					adapterList.setParentHandle(parentHandle);
					return;
				}
			}
		}
		
		super.onBackPressed();
	}
}
