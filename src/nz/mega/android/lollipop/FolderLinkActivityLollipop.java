package nz.mega.android.lollipop;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.mega.android.DatabaseHandler;
import nz.mega.android.DownloadService;
import nz.mega.android.FileStorageActivity;
import nz.mega.android.FileStorageActivity.Mode;
import nz.mega.android.FullScreenImageViewer;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaPreferences;
import nz.mega.android.MegaStreamingService;
import nz.mega.android.MimeTypeList;
import nz.mega.android.MimeTypeMime;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.components.SlidingUpPanelLayout;
import nz.mega.components.SlidingUpPanelLayout.PanelState;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FolderLinkActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener {
	
	FolderLinkActivityLollipop folderLinkActivity = this;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	
	ActionBar aB;
	Toolbar tB;
	
	String url;
	RecyclerView listView;
	GestureDetectorCompat detector;
	private RecyclerView.LayoutManager mLayoutManager;
	MegaNode selectedNode;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
    RelativeLayout fragmentContainer;
	
	long parentHandle = -1;
	ArrayList<MegaNode> nodes;
	
	LinearLayout outSpaceLayout=null;
	LinearLayout buttonsLayout=null;
	LinearLayout getProLayout=null;
	MegaBrowserLollipopAdapter adapterList;
	
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	private MenuItem downloadFolderMenuItem;
	private MenuItem importFolderMenuItem;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
	
	//OPTIONS PANEL
	private SlidingUpPanelLayout slidingOptionsPanel;
	public FrameLayout optionsOutLayout;
	public LinearLayout optionsLayout;
	public LinearLayout optionDownload;
	public LinearLayout optionProperties;
	public TextView propertiesText;
	////
	
	
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	
	private ActionMode actionMode;
	
	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

//		@Override
//	    public boolean onSingleTapConfirmed(MotionEvent e) {
//	        View view = listView.findChildViewUnder(e.getX(), e.getY());
//	        int position = listView.getChildPosition(view);
//
//	        // handle single tap
//	        itemClick(view, position);
//
//	        return super.onSingleTapConfirmed(e);
//	    }

	    public void onLongPress(MotionEvent e) {
	        View view = listView.findChildViewUnder(e.getX(), e.getY());
	        int position = listView.getChildPosition(view);

	        // handle long press
			if (adapterList.getPositionClicked() == -1){
				adapterList.setMultipleSelect(true);
			
				actionMode = startSupportActionMode(new ActionBarCallBack());			

		        itemClick(position);
			}  
	        super.onLongPress(e);
	    }
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = adapterList.getSelectedNodes();
			
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
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapterList.getSelectedNodes();
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			
			if (selected.size() > 0) {
				showDownload = true;
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			
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
		
		downloadFolderMenuItem =menu.findItem(R.id.action_download_folder);
		importFolderMenuItem = menu.findItem(R.id.action_import_folder);
		importFolderMenuItem.setVisible(false);
		if(megaApiFolder.getRootNode() == null)
		{
			downloadFolderMenuItem.setVisible(false);
		}
		
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
		
		downloadFolderMenuItem =menu.findItem(R.id.action_download_folder);
		importFolderMenuItem = menu.findItem(R.id.action_import_folder);
		
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
	    switch (item.getItemId()) {
		    case android.R.id.home:{
				finish();
			}
		    case R.id.action_import_folder:{
		    	//TODO
		    	return true;
		    }
	        case R.id.action_download_folder:{
	        	//TODO        	
	        	
	        	MegaNode rootNode = megaApiFolder.getRootNode();	        	
	        	onFolderClick(rootNode.getHandle(),rootNode.getSize());	        	
	        	return true;
	        
	        }
	        default:{
	            return super.onOptionsItemSelected(item);
            }
	    }
//		return false;
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
		megaApiFolder = app.getMegaApiFolder();
		megaApi = app.getMegaApi();
		
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
		
		detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());
		
		listView = (RecyclerView) findViewById(R.id.folder_link_list_view_browser);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnItemTouchListener(this);
		listView.setItemAnimator(new DefaultItemAnimator()); 

		outSpaceLayout = (LinearLayout) findViewById(R.id.out_space);
		outSpaceLayout.setVisibility(View.GONE);
		
		getProLayout=(LinearLayout) findViewById(R.id.get_pro_account);
		getProLayout.setVisibility(View.GONE);
		
		buttonsLayout = (LinearLayout) findViewById(R.id.buttons_layout);
		buttonsLayout.setVisibility(View.GONE);	
		
		contentText = (TextView) findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);
		
		Intent intent = getIntent();
    	
    	if (intent != null) {
    		if (intent.getAction().equals(ManagerActivityLollipop.ACTION_OPEN_MEGA_FOLDER_LINK)){
    			if (parentHandle == -1){
    				url = intent.getDataString();
    				megaApiFolder.loginToFolder(url, this);
    			}
    		}
    	}
    	
    	aB.setTitle("MEGA - " + getString(R.string.general_loading));
    	
    	slidingOptionsPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
		optionsLayout = (LinearLayout) findViewById(R.id.folder_link_list_options);
		optionsOutLayout = (FrameLayout) findViewById(R.id.folder_link_list_out_options);
		optionDownload = (LinearLayout) findViewById(R.id.folder_link_list_option_download_layout);
		optionProperties = (LinearLayout) findViewById(R.id.folder_link_list_option_properties_layout);
		propertiesText = (TextView) findViewById(R.id.folder_link_list_option_properties_text);	
		
		optionDownload.setOnClickListener(this);
		optionProperties.setOnClickListener(this);
		optionsOutLayout.setOnClickListener(this);
		
		slidingOptionsPanel.setVisibility(View.INVISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.HIDDEN);		
		
		slidingOptionsPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            	log("onPanelSlide, offset " + slideOffset);
            	if(slideOffset==0){
            		hideOptionsPanel();
            	}
            }

            @Override
            public void onPanelExpanded(View panel) {
            	log("onPanelExpanded");

            }

            @Override
            public void onPanelCollapsed(View panel) {
            	log("onPanelCollapsed");            	

            }

            @Override
            public void onPanelAnchored(View panel) {
            	log("onPanelAnchored");
            }

            @Override
            public void onPanelHidden(View panel) {
                log("onPanelHidden");                
            }
        });			
    }
	
	public void showOptionsPanel(MegaNode sNode){
		log("showOptionsPanel");
		
		this.selectedNode = sNode;
		
		if (selectedNode.isFolder()) {
			propertiesText.setText(R.string.general_folder_info);
		}else{
			propertiesText.setText(R.string.general_file_info);
		}
		
		optionDownload.setVisibility(View.VISIBLE);
		optionProperties.setVisibility(View.VISIBLE);				
					
		slidingOptionsPanel.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.COLLAPSED);
		log("Show the slidingPanel");
	}
	
	public void hideOptionsPanel(){
		log("hideOptionsPanel");
				
		adapterList.setPositionClicked(-1);
		slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
		slidingOptionsPanel.setVisibility(View.GONE);
	}
	
	public PanelState getPanelState ()
	{
		log("getPanelState: "+slidingOptionsPanel.getPanelState());
		return slidingOptionsPanel.getPanelState();
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
			Snackbar.make(fragmentContainer, getResources().getString(R.string.download_began), Snackbar.LENGTH_LONG).show();
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
									backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
								else
									backIntent = new Intent(FolderLinkActivityLollipop.this, ManagerActivityLollipop.class);
								
								startActivity(backIntent);
				    			finish();
							}
						});
									
					AlertDialog dialog = dialogBuilder.create();
					dialog.show(); 
					Util.brandAlertDialog(dialog);
				}
				catch(Exception ex){
					Snackbar.make(fragmentContainer, getResources().getString(R.string.general_error_folder_not_found), Snackbar.LENGTH_LONG).show();
	    			finish();
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES)
		{		
			MegaNode rootNode = megaApiFolder.getRootNode();
			if (rootNode != null){
				parentHandle = rootNode.getHandle();
				nodes = megaApiFolder.getChildren(rootNode);
				aB.setTitle(megaApiFolder.getRootNode().getName());
				supportInvalidateOptionsMenu();
				
				if (adapterList == null){
					adapterList = new MegaBrowserLollipopAdapter(this, null, nodes, parentHandle, listView, aB, ManagerActivityLollipop.FOLDER_LINK_ADAPTER);
				}
				else{
					adapterList.setParentHandle(parentHandle);
					adapterList.setNodes(nodes);
				}
				
				adapterList.setPositionClicked(-1);
				adapterList.setMultipleSelect(false);
				
				listView.setAdapter(adapterList);
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
								Intent backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
				    			startActivity(backIntent);
				    			finish();
							}
						});
									
					AlertDialog dialog = dialogBuilder.create();
					dialog.show(); 
					Util.brandAlertDialog(dialog);
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
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapterList.isMultipleSelect()){
			adapterList.clearSelections();
		}

		updateActionModeTitle();
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

	public void itemClick(int position) {
		if (adapterList.isMultipleSelect()){
			adapterList.toggleSelection(position);
			updateActionModeTitle();
			adapterList.notifyDataSetChanged();
		}
		else{
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);
				
				aB.setTitle(n.getName());
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
					Intent intent = new Intent(this, FullScreenImageViewer.class);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", ManagerActivityLollipop.FOLDER_LINK_ADAPTER);
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
			  		if (ManagerActivityLollipop.isIntentAvailable(this, mediaIntent)){
			  			startActivity(mediaIntent);
			  		}
			  		else{
			  			Snackbar.make(fragmentContainer, getResources().getString(R.string.intent_not_available), Snackbar.LENGTH_SHORT).show();
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
		
		PanelState pS=slidingOptionsPanel.getPanelState();
		
		if(pS==null){
			log("NULLL");
		}
		else{
			if(pS==PanelState.HIDDEN){
				log("Hidden");
			}
			else if(pS==PanelState.COLLAPSED){
				log("Collapsed");
			}
			else{
				log("ps: "+pS);
			}
		}		
		
		if(slidingOptionsPanel.getPanelState()!=PanelState.HIDDEN){
			log("getPanelState()!=PanelState.HIDDEN");
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
			slidingOptionsPanel.setVisibility(View.GONE);
			adapterList.setPositionClicked(-1);
			adapterList.notifyDataSetChanged();
			return;
		}
		
		log("Sliding not shown");
		
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
			}
		}
		
		super.onBackPressed();
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
		detector.onTouchEvent(e);
		return false;
	}

	@Override
	public void onRequestDisallowInterceptTouchEvent(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTouchEvent(RecyclerView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		
		switch(v.getId()){
			case R.id.folder_link_list_out_options:{
				log("Out Panel");
				hideOptionsPanel();
				break;				
			}
			case R.id.folder_link_list_option_download_layout: {
				log("Download option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				adapterList.setPositionClicked(-1);
				adapterList.notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());
				onFileClick(handleList);
				break;
			}
			case R.id.folder_link_list_option_properties_layout: {
				log("Properties option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				adapterList.setPositionClicked(-1);
				adapterList.notifyDataSetChanged();
				Intent i = new Intent(this, FilePropertiesActivityLollipop.class);
				i.putExtra("handle", selectedNode.getHandle());
				
				if (selectedNode.isFolder()) {
					if (megaApi.isShared(selectedNode)){
						i.putExtra("imageId", R.drawable.folder_shared_mime);	
					}
					else{
						i.putExtra("imageId", R.drawable.folder_mime);
					}
				} 
				else {
					i.putExtra("imageId", MimeTypeMime.typeForName(selectedNode.getName()).getIconResourceId());
				}
				i.putExtra("name", selectedNode.getName());
				this.startActivity(i);

				break;
			}
		}			
	}
}
