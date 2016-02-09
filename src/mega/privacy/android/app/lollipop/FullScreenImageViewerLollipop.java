package mega.privacy.android.app.lollipop;

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
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.ExtendedViewPager;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class FullScreenImageViewerLollipop extends PinActivityLollipop implements OnPageChangeListener, OnClickListener, MegaRequestListenerInterface, OnItemClickListener{
	
	private Display display;
	private DisplayMetrics outMetrics;
	private float density;
	private float scaleW;
	private float scaleH;
	
	private boolean aBshown = true;
	
	ProgressDialog statusDialog;
	
	private MegaFullScreenImageAdapterLollipop adapterMega;
	private MegaOfflineFullScreenImageAdapterLollipop adapterOffline;
	private int positionG;
	private ArrayList<Long> imageHandles;
	private boolean fromShared = false;
	private RelativeLayout fragmentContainer;
	private TextView fileNameTextView;
	private ImageView actionBarIcon;
	private ImageView overflowIcon;
	private ImageView shareIcon;
	private ImageView downloadIcon;
	private ImageView propertiesIcon;
	private ImageView linkIcon;
	private ListView overflowMenuList;
	private boolean overflowVisible = false; 
	
	private RelativeLayout bottomLayout;
    private RelativeLayout topLayout;
	private ExtendedViewPager viewPager;	
	
	static FullScreenImageViewerLollipop fullScreenImageViewer;
    private MegaApiAndroid megaApi;

    private ArrayList<String> paths;
    
    int adapterType = 0;
    
    public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	
	MegaNode node;
	
	boolean shareIt = true;
	boolean moveToRubbish = false;
	
	private static int EDIT_TEXT_ID = 1;
	private Handler handler;
	
	private AlertDialog renameDialog;
	
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
	boolean isFolderLink = false;
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	        // do nothing
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		handler = new Handler();
		fullScreenImageViewer = this;
		
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
		    requestWindowFeature(Window.FEATURE_NO_TITLE); 
		    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		setContentView(R.layout.activity_full_screen_image_viewer);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
		    ActionBar actionBar = getSupportActionBar();
		    if (actionBar != null){
		    	actionBar.hide();
		    }
		}
		
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    }
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
		
		viewPager = (ExtendedViewPager) findViewById(R.id.image_viewer_pager);
		viewPager.setPageMargin(40);
		
		fragmentContainer = (RelativeLayout) findViewById(R.id.full_image_viewer_parent_layout);
		
		Intent intent = getIntent();
		positionG = intent.getIntExtra("position", 0);
		orderGetChildren = intent.getIntExtra("orderGetChildren", MegaApiJava.ORDER_DEFAULT_ASC);
		isFolderLink = intent.getBooleanExtra("isFolderLink", false);
		
		MegaApplication app = (MegaApplication)getApplication();
		if (isFolderLink){
			megaApi = app.getMegaApiFolder();
		}
		else{
			megaApi = app.getMegaApi();
		}
		
		imageHandles = new ArrayList<Long>();
		paths = new ArrayList<String>();
		long parentNodeHandle = intent.getLongExtra("parentNodeHandle", -1);
		fromShared = intent.getBooleanExtra("fromShared", false);
		MegaNode parentNode;		
		
		adapterType = intent.getIntExtra("adapterType", 0);
		if ((adapterType == ManagerActivityLollipop.OFFLINE_ADAPTER) || (adapterType == ManagerActivityLollipop.ZIP_ADAPTER)){
			String offlinePathDirectory = intent.getStringExtra("offlinePathDirectory");
			log("OFFLINEPATHDIRECTORY: "  + offlinePathDirectory);
		
			File offlineDirectory = new File(offlinePathDirectory);
//			if (Environment.getExternalStorageDirectory() != null){
//				offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);
//			}
//			else{
//				offlineDirectory = getFilesDir();
//			}
			
			paths.clear();			
			int imageNumber = 0;
			int index = 0;
			File[] fList = offlineDirectory.listFiles();
			if(fList == null)
			{
				//Nothing to show (folder deleted?)
				//Close the image viewer
				finish();
				return;
			}
			
			log("SIZE: " + fList.length);
			for (File f : fList){
				log("F: " + f.getAbsolutePath());
				if (MimeTypeList.typeForName(f.getName()).isImage()){
					paths.add(f.getAbsolutePath());
					if (index == positionG){
						positionG = imageNumber; 
					}
					imageNumber++;
				}
				index++;				
			}
			
			if(paths.size() == 0)
			{
				//No images to show (images deleted?)
				//Close the image viewer
				finish();
				return;
			}
			
			if(positionG >= paths.size())
			{
				//Invalid index. Show the first image
				positionG = 0;
			}
			
			if (adapterType == ManagerActivityLollipop.OFFLINE_ADAPTER){
				adapterOffline = new MegaOfflineFullScreenImageAdapterLollipop(fullScreenImageViewer, paths);
			}
			else if(adapterType == ManagerActivityLollipop.ZIP_ADAPTER){
				adapterOffline = new MegaOfflineFullScreenImageAdapterLollipop(fullScreenImageViewer, paths, true);
			}
			
			viewPager.setAdapter(adapterOffline);
			
			viewPager.setCurrentItem(positionG);
	
			viewPager.setOnPageChangeListener(this);
			
			actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
			actionBarIcon.setOnClickListener(this);
			
			overflowIcon = (ImageView) findViewById(R.id.full_image_viewer_overflow);
			overflowIcon.setVisibility(View.INVISIBLE);
			
			downloadIcon = (ImageView) findViewById(R.id.full_image_viewer_download);
			downloadIcon.setVisibility(View.GONE);
			
			propertiesIcon = (ImageView) findViewById(R.id.full_image_viewer_properties);
			propertiesIcon.setVisibility(View.GONE);
			
			linkIcon = (ImageView) findViewById(R.id.full_image_viewer_get_link);
			linkIcon.setVisibility(View.GONE);
			
			shareIcon = (ImageView) findViewById(R.id.full_image_viewer_share);
			shareIcon.setOnClickListener(this);
			shareIcon.setVisibility(View.VISIBLE);
			
			bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
		    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
		}
		else if(adapterType == ManagerActivityLollipop.SEARCH_ADAPTER){
			ArrayList<MegaNode> nodes = null;
			if (parentNodeHandle == -1){
				String query = intent.getStringExtra("searchQuery");
				nodes = megaApi.search(megaApi.getRootNode(), query, true);
			}
			else{
				parentNode =  megaApi.getNodeByHandle(parentNodeHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
			}
			
			int imageNumber = 0;
			for (int i=0;i<nodes.size();i++){
				MegaNode n = nodes.get(i);
				if (MimeTypeList.typeForName(n.getName()).isImage()){
					imageHandles.add(n.getHandle());
					if (i == positionG){
						positionG = imageNumber; 
					}
					imageNumber++;
				}
			}

			if(imageHandles.size() == 0)
			{
				finish();
				return;
			}
			
			if(positionG >= imageHandles.size())
			{
				positionG = 0;
			}
			
			adapterMega = new MegaFullScreenImageAdapterLollipop(fullScreenImageViewer,imageHandles, megaApi);
			
			viewPager.setAdapter(adapterMega);
			
			viewPager.setCurrentItem(positionG);
	
			viewPager.setOnPageChangeListener(this);
			
			actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
			actionBarIcon.setOnClickListener(this);
			
			overflowIcon = (ImageView) findViewById(R.id.full_image_viewer_overflow);
			if (!isFolderLink){
				overflowIcon.setVisibility(View.VISIBLE);
				overflowIcon.setOnClickListener(this);
			}
			else{
				overflowIcon.setVisibility(View.INVISIBLE);
			}
			
			shareIcon = (ImageView) findViewById(R.id.full_image_viewer_share);
			shareIcon.setVisibility(View.VISIBLE);
			shareIcon.setOnClickListener(this);
			
			downloadIcon = (ImageView) findViewById(R.id.full_image_viewer_download);
			downloadIcon.setVisibility(View.VISIBLE);
			downloadIcon.setOnClickListener(this);
			
			propertiesIcon = (ImageView) findViewById(R.id.full_image_viewer_properties);
			propertiesIcon.setVisibility(View.VISIBLE);
			propertiesIcon.setOnClickListener(this);
			
			linkIcon = (ImageView) findViewById(R.id.full_image_viewer_get_link);
			linkIcon.setVisibility(View.VISIBLE);
			linkIcon.setOnClickListener(this);
			
			String menuOptions[] = new String[4];
//			menuOptions[0] = getString(R.string.context_get_link_menu);
			menuOptions[0] = getString(R.string.context_rename);
			menuOptions[1] = getString(R.string.context_move);
			menuOptions[2] = getString(R.string.context_copy);
			menuOptions[3] = getString(R.string.context_remove);
			
			overflowMenuList = (ListView) findViewById(R.id.image_viewer_overflow_menu_list);
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
			overflowMenuList.setAdapter(arrayAdapter);
			overflowMenuList.setOnItemClickListener(this);
			if (overflowVisible){
				overflowMenuList.setVisibility(View.VISIBLE);	
			}
			else{
				overflowMenuList.setVisibility(View.GONE);
			}
			
			bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
		    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
		    
		    fileNameTextView = (TextView) findViewById(R.id.full_image_viewer_file_name);
		    fileNameTextView.setText(megaApi.getNodeByHandle(imageHandles.get(positionG)).getName());
		}
		else{

			if (parentNodeHandle == -1){
	
				switch(adapterType){
					case ManagerActivityLollipop.FILE_BROWSER_ADAPTER:{
						parentNode = megaApi.getRootNode();
						break;
					}
					case ManagerActivityLollipop.RUBBISH_BIN_ADAPTER:{
						parentNode = megaApi.getRubbishNode();
						break;
					}
					case ManagerActivityLollipop.SHARED_WITH_ME_ADAPTER:{
						parentNode = megaApi.getInboxNode();
						break;
					}
					case ManagerActivityLollipop.FOLDER_LINK_ADAPTER:{
						parentNode = megaApi.getRootNode();
						break;
					}
					default:{
						parentNode = megaApi.getRootNode();
						break;
					}
				} 
				
			}
			else{
				parentNode = megaApi.getNodeByHandle(parentNodeHandle);
			}
			
			ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
			if (fromShared){
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					nodes = sortByNameDescending(nodes);
				}
				else{
					nodes = sortByNameAscending(nodes);
				}
			}
			int imageNumber = 0;
			for (int i=0;i<nodes.size();i++){
				MegaNode n = nodes.get(i);
				if (MimeTypeList.typeForName(n.getName()).isImage()){
					imageHandles.add(n.getHandle());
					if (i == positionG){
						positionG = imageNumber; 
					}
					imageNumber++;
				}
			}
//			Toast.makeText(this, ""+parentNode.getName() + "_" + imageHandles.size(), Toast.LENGTH_LONG).show();
				
			if(imageHandles.size() == 0)
			{
				finish();
				return;
			}
			
			if(positionG >= imageHandles.size())
			{
				positionG = 0;
			}
			
			adapterMega = new MegaFullScreenImageAdapterLollipop(fullScreenImageViewer,imageHandles, megaApi);
			
			viewPager.setAdapter(adapterMega);
			
			viewPager.setCurrentItem(positionG);
	
			viewPager.setOnPageChangeListener(this);
			
			actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
			actionBarIcon.setOnClickListener(this);
			
			overflowIcon = (ImageView) findViewById(R.id.full_image_viewer_overflow);
			if (!isFolderLink){
				overflowIcon.setVisibility(View.VISIBLE);
				overflowIcon.setOnClickListener(this);
			}
			else{
				overflowIcon.setVisibility(View.INVISIBLE);
			}
			
			shareIcon = (ImageView) findViewById(R.id.full_image_viewer_share);
			shareIcon.setVisibility(View.VISIBLE);
			shareIcon.setOnClickListener(this);
			
			downloadIcon = (ImageView) findViewById(R.id.full_image_viewer_download);
			downloadIcon.setVisibility(View.VISIBLE);
			downloadIcon.setOnClickListener(this);
			
			propertiesIcon = (ImageView) findViewById(R.id.full_image_viewer_properties);
			propertiesIcon.setVisibility(View.VISIBLE);
			propertiesIcon.setOnClickListener(this);
			
			linkIcon = (ImageView) findViewById(R.id.full_image_viewer_get_link);
			linkIcon.setVisibility(View.VISIBLE);
			linkIcon.setOnClickListener(this);
			
			String menuOptions[] = new String[4];
//			menuOptions[0] = getString(R.string.context_get_link_menu);
			menuOptions[0] = getString(R.string.context_rename);
			menuOptions[1] = getString(R.string.context_move);
			menuOptions[2] = getString(R.string.context_copy);
			menuOptions[3] = getString(R.string.context_remove);
			
			overflowMenuList = (ListView) findViewById(R.id.image_viewer_overflow_menu_list);
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
			overflowMenuList.setAdapter(arrayAdapter);
			overflowMenuList.setOnItemClickListener(this);
			if (overflowVisible){
				overflowMenuList.setVisibility(View.VISIBLE);	
			}
			else{
				overflowMenuList.setVisibility(View.GONE);
			}
			
			bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
		    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
		    
		    fileNameTextView = (TextView) findViewById(R.id.full_image_viewer_file_name);
		    fileNameTextView.setText(megaApi.getNodeByHandle(imageHandles.get(positionG)).getName());
		}
	}
	
	public ArrayList<MegaNode> sortByNameAscending(ArrayList<MegaNode> nodes){
		log("sortByNameAscending");
		
		ArrayList<MegaNode> folderNodes = new ArrayList<MegaNode>();
		ArrayList<MegaNode> fileNodes = new ArrayList<MegaNode>();
		
		for (int i=0;i<nodes.size();i++){
			if (nodes.get(i).isFolder()){
				folderNodes.add(nodes.get(i));
			}
			else{
				fileNodes.add(nodes.get(i));
			}
		}
		
		for (int i=0;i<folderNodes.size();i++){
			for (int j=0;j<folderNodes.size()-1;j++){
				if (folderNodes.get(j).getName().compareTo(folderNodes.get(j+1).getName()) > 0){
					MegaNode nAuxJ = folderNodes.get(j);
					MegaNode nAuxJ_1 = folderNodes.get(j+1);
					folderNodes.remove(j+1);
					folderNodes.remove(j);
					folderNodes.add(j, nAuxJ_1);
					folderNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		for (int i=0;i<fileNodes.size();i++){
			for (int j=0;j<fileNodes.size()-1;j++){
				if (fileNodes.get(j).getName().compareTo(fileNodes.get(j+1).getName()) > 0){
					MegaNode nAuxJ = fileNodes.get(j);
					MegaNode nAuxJ_1 = fileNodes.get(j+1);
					fileNodes.remove(j+1);
					fileNodes.remove(j);
					fileNodes.add(j, nAuxJ_1);
					fileNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		nodes.clear();
		nodes.addAll(folderNodes);
		nodes.addAll(fileNodes);
		
		return nodes;
	}
	
	public ArrayList<MegaNode> sortByNameDescending(ArrayList<MegaNode> nodes){
		
		ArrayList<MegaNode> folderNodes = new ArrayList<MegaNode>();
		ArrayList<MegaNode> fileNodes = new ArrayList<MegaNode>();
		
		for (int i=0;i<nodes.size();i++){
			if (nodes.get(i).isFolder()){
				folderNodes.add(nodes.get(i));
			}
			else{
				fileNodes.add(nodes.get(i));
			}
		}
		
		for (int i=0;i<folderNodes.size();i++){
			for (int j=0;j<folderNodes.size()-1;j++){
				if (folderNodes.get(j).getName().compareTo(folderNodes.get(j+1).getName()) < 0){
					MegaNode nAuxJ = folderNodes.get(j);
					MegaNode nAuxJ_1 = folderNodes.get(j+1);
					folderNodes.remove(j+1);
					folderNodes.remove(j);
					folderNodes.add(j, nAuxJ_1);
					folderNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		for (int i=0;i<fileNodes.size();i++){
			for (int j=0;j<fileNodes.size()-1;j++){
				if (fileNodes.get(j).getName().compareTo(fileNodes.get(j+1).getName()) < 0){
					MegaNode nAuxJ = fileNodes.get(j);
					MegaNode nAuxJ_1 = fileNodes.get(j+1);
					fileNodes.remove(j+1);
					fileNodes.remove(j);
					fileNodes.add(j, nAuxJ_1);
					fileNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		nodes.clear();
		nodes.addAll(folderNodes);
		nodes.addAll(fileNodes);
		
		return nodes;
	}

	@Override
	public void onPageSelected(int position) {
		return;
	}
	
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		return;
	}
	
	@Override
	public void onPageScrollStateChanged(int state) {

		if (state == ViewPager.SCROLL_STATE_IDLE){
			if (viewPager.getCurrentItem() != positionG){
				int oldPosition = positionG;
				int newPosition = viewPager.getCurrentItem();
				positionG = newPosition;
				
				try{
					if ((adapterType == ManagerActivityLollipop.OFFLINE_ADAPTER) || (adapterType == ManagerActivityLollipop.ZIP_ADAPTER)){
						
					}
					else{
						TouchImageView tIV = adapterMega.getVisibleImage(oldPosition);
						if (tIV != null){
							tIV.setZoom(1);
						}
						fileNameTextView.setText(megaApi.getNodeByHandle(imageHandles.get(positionG)).getName());
					}
				}
				catch(Exception e){}
//				title.setText(names.get(positionG));
			}
		}
	}

	@Override
	public void onClick(View v) {
		
		if ((adapterType == ManagerActivityLollipop.OFFLINE_ADAPTER) || (adapterType == ManagerActivityLollipop.ZIP_ADAPTER)){
			switch (v.getId()){
				case R.id.full_image_viewer_icon:{
					finish();
					break;
				}
				case R.id.full_image_viewer_share:{
					
					String fileName = paths.get(positionG);
					File previewFile = new File(fileName);
					
					if (previewFile.exists()){
						Intent share = new Intent(android.content.Intent.ACTION_SEND);
						share.setType("image/*");
						share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + previewFile));
						startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
					}
					else{
						Snackbar.make(fragmentContainer, getString(R.string.full_image_viewer_not_preview), Snackbar.LENGTH_LONG).show();
					}
					
					break;
				}
			}
		}
		else{
			node = megaApi.getNodeByHandle(imageHandles.get(positionG));
			switch (v.getId()){
				case R.id.full_image_viewer_icon:{
					finish();
					break;
				}
				case R.id.full_image_viewer_get_link:{
					shareIt = false;
			    	getPublicLinkAndShareIt();
					break;
				}
				case R.id.full_image_viewer_properties:{
					Intent i = new Intent(this, FilePropertiesActivityLollipop.class);
					i.putExtra("handle", node.getHandle());
					i.putExtra("imageId", MimeTypeMime.typeForName(node.getName()).getIconResourceId());
					i.putExtra("name", node.getName());
					startActivity(i);
					break;
				}
				case R.id.full_image_viewer_overflow:{
					if (adapterMega.isaBshown()){
						overflowVisible = adapterMega.isMenuVisible();
						if (overflowVisible){
							overflowMenuList.setVisibility(View.GONE);
							overflowVisible = false;
						}
						else{
							overflowMenuList.setVisibility(View.VISIBLE);
							overflowVisible = true;
						}
						adapterMega.setMenuVisible(overflowVisible);
					}
					break;
				}
				case R.id.full_image_viewer_share:{
					
					overflowMenuList.setVisibility(View.GONE);
					overflowVisible = false;
					adapterMega.setMenuVisible(overflowVisible);
									
					File previewFolder = PreviewUtils.getPreviewFolder(this);
					File previewFile = new File(previewFolder, node.getBase64Handle() + ".jpg");
					
					if (previewFile.exists()){
						Intent share = new Intent(android.content.Intent.ACTION_SEND);
						share.setType("image/*");
						share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + previewFile));
						startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
					}
					else{
						Snackbar.make(fragmentContainer, getString(R.string.full_image_viewer_not_preview), Snackbar.LENGTH_LONG).show();
					}
					
					break;
				}
				case R.id.full_image_viewer_download:{
					
					overflowMenuList.setVisibility(View.GONE);
					overflowVisible = false;
					adapterMega.setMenuVisible(overflowVisible);
					
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(node.getHandle());
					downloadNode(handleList);
					break;
				}
			}
		}
	}
	
	public void downloadNode(ArrayList<Long> handleList){
		
		long size = 0;
		long[] hashes = new long[handleList.size()];
		for (int i=0;i<handleList.size();i++){
			hashes[i] = handleList.get(i);
			size += megaApi.getNodeByHandle(hashes[i]).getSize();
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
			intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS,false);
			intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
			intent.setClass(this, FileStorageActivityLollipop.class);
			intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
			startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);	
		}
		else{
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}
	}
	
	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putInt("adapterType", adapterType);
		if ((adapterType == ManagerActivityLollipop.OFFLINE_ADAPTER) || (adapterType == ManagerActivityLollipop.ZIP_ADAPTER)){
			
		}
		else{
			savedInstanceState.putBoolean("aBshown", adapterMega.isaBshown());
			savedInstanceState.putBoolean("overflowVisible", adapterMega.isMenuVisible());
		}
	}
	
	@Override
	public void onRestoreInstanceState (Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		
		adapterType = savedInstanceState.getInt("adapterType");
		
		if ((adapterType == ManagerActivityLollipop.OFFLINE_ADAPTER) || (adapterType == ManagerActivityLollipop.ZIP_ADAPTER)){
			
		}
		else{
			aBshown = savedInstanceState.getBoolean("aBshown");
			adapterMega.setaBshown(aBshown);
			overflowVisible = savedInstanceState.getBoolean("overflowVisible");
			adapterMega.setMenuVisible(overflowVisible);
		}
		
		if (!aBshown){
			TranslateAnimation animBottom = new TranslateAnimation(0, 0, 0, Util.px2dp(48, outMetrics));
			animBottom.setDuration(0);
			animBottom.setFillAfter( true );
			bottomLayout.setAnimation(animBottom);
			
			TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(-48, outMetrics));
			animTop.setDuration(0);
			animTop.setFillAfter( true );
			topLayout.setAnimation(animTop);
		}
		
		if(overflowMenuList != null)
		{
			if (overflowVisible){
				overflowMenuList.setVisibility(View.VISIBLE);
			}
			else{
				overflowMenuList.setVisibility(View.GONE);
			}
		}
	}
	
	public void getPublicLinkAndShareIt(){
		
		if (!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
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
	
	public void showRenameDialog(){
		
		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, node.isFolder());
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
	
	private void rename(String newName){
		if (newName.equals(node.getName())) {
			return;
		}
		
		if(!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
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
	
	public void showMove(){
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());
		
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}
	
	public void showCopy(){
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());
		
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}
	
	public void moveToTrash(){
log("moveToTrash");
		
		final long handle = node.getHandle();
		moveToRubbish = false;
		if (!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		final MegaNode rubbishNode = megaApi.getRubbishNode();
		
		MegaNode parent = megaApi.getNodeByHandle(handle);
		while (megaApi.getParentNode(parent) != null){
			parent = megaApi.getParentNode(parent);
		}
		
		if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
			moveToRubbish = true;			
		}
		else{
			moveToRubbish = false;
		}
		
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	//TODO remove the outgoing shares
		    		//Check if the node is not yet in the rubbish bin (if so, remove it)			
		    		
		    		if (moveToRubbish){
		    			megaApi.moveNode(megaApi.getNodeByHandle(handle), rubbishNode, fullScreenImageViewer);
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(fullScreenImageViewer);
		    				temp.setMessage(getString(R.string.context_move_to_trash));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}
		    		else{
		    			megaApi.remove(megaApi.getNodeByHandle(handle), fullScreenImageViewer);
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(fullScreenImageViewer);
		    				temp.setMessage(getString(R.string.context_delete_from_mega));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}
		        	
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};
		
		if (moveToRubbish){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.section_rubbish_bin));
			String message= getResources().getString(R.string.confirmation_move_to_rubbish);
			builder.setMessage(message).setPositiveButton(R.string.general_yes, dialogClickListener)
		    	.setNegativeButton(R.string.general_no, dialogClickListener).show();
		}
		else{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.title_delete_from_mega));
			String message= getResources().getString(R.string.confirmation_delete_from_mega);
			builder.setMessage(message).setPositiveButton(R.string.general_yes, dialogClickListener)
		    	.setNegativeButton(R.string.general_no, dialogClickListener).show();
		}
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
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

				final String link = request.getLink();					
				
				AlertDialog getLinkDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);					
//	            builder.setMessage(link);
				builder.setTitle(getString(R.string.context_get_link_menu));
				
				// Create TextView
				final TextView input = new TextView (this);
				input.setGravity(Gravity.CENTER);
				
				final CharSequence[] items = {getString(R.string.option_full_link), getString(R.string.option_link_without_key), getString(R.string.option_decryption_key)};

				android.content.DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						
						switch(item) {
		                    case 0:{
		                    	input.setText(link);
		                    	break;
		                    }
		                    case 1:{
		                    	String urlString="";			    					
		    					String [] s = link.split("!");
		    					if (s.length == 3){
		    						urlString = s[0] + "!" + s[1];			    						
		    					}
		                    	input.setText(urlString);
		                        break;
		                    }
		                    case 2:{
		                    	String keyString="";
		    					String [] s = link.split("!");
		    					if (s.length == 3){
		    						keyString = s[2];
		    					}
		                    	input.setText(keyString);
		                        break;
		                    }
		                }
					}
				};
				
				builder.setSingleChoiceItems(items, 0, dialogListener);
//				
				builder.setPositiveButton(getString(R.string.context_send), new android.content.DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.setType("text/plain");
						intent.putExtra(Intent.EXTRA_TEXT, input.getText());
						startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
					}
				});
				
				builder.setNegativeButton(getString(R.string.context_copy), new android.content.DialogInterface.OnClickListener() {
					
					@SuppressLint("NewApi") 
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    clipboard.setText(link);
						} else {
						    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", input.getText());
				            clipboard.setPrimaryClip(clip);
						}
						Snackbar.make(fragmentContainer, getString(R.string.file_properties_get_link), Snackbar.LENGTH_LONG).show();
					}
				});	
				
				input.setText(link);
				builder.setView(input);
				
				getLinkDialog = builder.create();
				getLinkDialog.create();
				FrameLayout.LayoutParams lpPL = new FrameLayout.LayoutParams(input.getLayoutParams());
				lpPL.setMargins(Util.scaleWidthPx(15, outMetrics), 0, Util.scaleWidthPx(15, outMetrics), 0);
				input.setLayoutParams(lpPL);
				getLinkDialog.show();
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_link), Snackbar.LENGTH_LONG).show();
			}
			log("export request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.context_correctly_renamed), Snackbar.LENGTH_LONG).show();
			}			
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_renamed), Snackbar.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_moved), Snackbar.LENGTH_LONG).show();
					finish();
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_moved), Snackbar.LENGTH_LONG).show();
					finish();
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
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
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_removed), Snackbar.LENGTH_LONG).show();
				}
				finish();
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_removed), Snackbar.LENGTH_LONG).show();
			}
			log("remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.context_correctly_copied), Snackbar.LENGTH_LONG).show();
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());	
	}
	
	public static void log(String message) {
		Util.log("FullScreenImageViewer", message);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		overflowMenuList.setVisibility(View.GONE);
		overflowVisible = false;
		adapterMega.setMenuVisible(overflowVisible);
		
		switch(position){
//			case 0:{
//				shareIt = false;
//		    	getPublicLinkAndShareIt();
//				break;
//			}
			case 0:{
				showRenameDialog();
				break;
			}
			case 1:{
				showMove();
				break;
			}
			case 2:{
				showCopy();
				break;
			}
			case 3:{
				moveToTrash();
				break;
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (intent == null) {
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
			Snackbar.make(fragmentContainer, getString(R.string.download_began), Snackbar.LENGTH_LONG).show();
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {
			
			if(!Util.isOnline(this)){
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
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
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
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
	}
	
	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
		
		if (megaApi.getRootNode() == null)
			return;
		
		folder.mkdir();
		ArrayList<MegaNode> nodeList = megaApi.getChildren(parent, orderGetChildren);
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
					Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
					return;
				}
				
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
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
							String message = getString(R.string.general_already_downloaded) + ": " + localPath;
							Snackbar.make(fragmentContainer, message, Snackbar.LENGTH_LONG).show();
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
							Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
							continue;
						}
						
						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
						service.putExtra(DownloadService.EXTRA_URL, url);
						service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
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
					service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
					startService(service);
				}
				else {
					log("node not found");
				}
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		if (overflowMenuList != null){
			if (overflowMenuList.getVisibility() == View.VISIBLE){
				overflowMenuList.setVisibility(View.GONE);
				return;
			}
		}
		super.onBackPressed();
	}
}
