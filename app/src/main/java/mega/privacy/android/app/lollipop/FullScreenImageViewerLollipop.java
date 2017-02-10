package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.ExtendedViewPager;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;

public class FullScreenImageViewerLollipop extends PinActivityLollipop implements OnPageChangeListener, OnClickListener, MegaRequestListenerInterface, OnItemClickListener, DatePickerDialog.OnDateSetListener{
	
	private DisplayMetrics outMetrics;

	private boolean aBshown = true;
	
	ProgressDialog statusDialog;

	int accountType;

	private android.support.v7.app.AlertDialog getLinkDialog;
	Button expiryDateButton;
	SwitchCompat switchGetLink;
	private boolean isExpiredDateLink = false;
	private boolean isGetLink = false;

	float scaleText;
	
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
	
	ArrayList<Long> handleListM = new ArrayList<Long>();
	
	ArrayList<MegaOffline> mOffList;
	ArrayList<MegaOffline> mOffListImages;
	
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
		log("onCreate");
		super.onCreate(savedInstanceState);
		
		handler = new Handler();
		fullScreenImageViewer = this;

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}
		
		dbH = DatabaseHandler.getDbHandler(this);
		
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

		accountType = intent.getIntExtra("typeAccount", MegaAccountDetails.ACCOUNT_TYPE_FREE);

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
		if (adapterType == Constants.OFFLINE_ADAPTER){
			mOffList = new ArrayList<MegaOffline>();
			
			String pathNavigation = intent.getStringExtra("pathNavigation");
			int orderGetChildren = intent.getIntExtra("orderGetChildren", MegaApiJava.ORDER_DEFAULT_ASC);
			log("PATHNAVIGATION: " + pathNavigation);
			mOffList=dbH.findByPath(pathNavigation);
			log ("mOffList.size() = " + mOffList.size());
			
			for(int i=0; i<mOffList.size();i++){
				MegaOffline checkOffline = mOffList.get(i);
				
				if(!checkOffline.isIncoming()){				
					log("NOT isIncomingOffline");
					File offlineDirectory = null;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + checkOffline.getPath()+checkOffline.getName());
					}
					else{
						offlineDirectory = getFilesDir();
					}	
					
					if (!offlineDirectory.exists()){
						log("Path to remove A: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
						//dbH.removeById(mOffList.get(i).getId());
						mOffList.remove(i);		
						i--;
					}	
				}
				else{
					log("isIncomingOffline");
					File offlineDirectory = null;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +checkOffline.getHandleIncoming() + "/" + checkOffline.getPath()+checkOffline.getName());
						log("offlineDirectory: "+offlineDirectory);
					}
					else{
						offlineDirectory = getFilesDir();
					}	
					
					if (!offlineDirectory.exists()){
						log("Path to remove B: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
						//dbH.removeById(mOffList.get(i).getId());
						mOffList.remove(i);
						i--;
					}						
				}
			}
			
			if (mOffList != null){
				if(!mOffList.isEmpty()) {
					MegaOffline lastItem = mOffList.get(mOffList.size()-1);
					if(!(lastItem.getHandle().equals("0"))){
						String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.oldMKFile;
						log("Export in: "+path);
						File file= new File(path);
						if(file.exists()){
							MegaOffline masterKeyFile = new MegaOffline("0", path, "MEGARecoveryKey.txt", 0, "0", false, "0");
							mOffList.add(masterKeyFile);
						}
					}	
				}
				else{
					String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.oldMKFile;
					log("Export in: "+path);
					File file= new File(path);
					if(file.exists()){
						MegaOffline masterKeyFile = new MegaOffline("0", path, "MEGARecoveryKey.txt", 0, "0", false, "0");
						mOffList.add(masterKeyFile);
					}
				}
			}
			
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}
			
			if (mOffList.size() > 0){
				
				mOffListImages = new ArrayList<MegaOffline>();
				int positionImage = -1;
				for (int i=0;i<mOffList.size();i++){
					if (MimeTypeList.typeForName(mOffList.get(i).getName()).isImage()){
						mOffListImages.add(mOffList.get(i));
						positionImage++;
						if (i == positionG){
							positionG = positionImage;
						}
					}
				}
				
				if (positionG >= mOffListImages.size()){
					positionG = 0;
				}
				
				adapterOffline = new MegaOfflineFullScreenImageAdapterLollipop(fullScreenImageViewer, mOffListImages);
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
				shareIcon.setVisibility(View.GONE);
				
				bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
			    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
			}			
		}				
		else if (adapterType == Constants.ZIP_ADAPTER){
			String offlinePathDirectory = intent.getStringExtra("offlinePathDirectory");
		
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
			
			if(adapterType == Constants.ZIP_ADAPTER){
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
			shareIcon.setVisibility(View.GONE);
			
			bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
		    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
		}
		else if(adapterType == Constants.SEARCH_ADAPTER){
			ArrayList<MegaNode> nodes = null;
			if (parentNodeHandle == -1){
				String query = intent.getStringExtra("searchQuery");
				nodes = megaApi.search(query);
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
					case Constants.FILE_BROWSER_ADAPTER:{
						parentNode = megaApi.getRootNode();
						break;
					}
					case Constants.RUBBISH_BIN_ADAPTER:{
						parentNode = megaApi.getRubbishNode();
						break;
					}
					case Constants.SHARED_WITH_ME_ADAPTER:{
						parentNode = megaApi.getInboxNode();
						break;
					}
					case Constants.FOLDER_LINK_ADAPTER:{
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

			if(adapterType==Constants.CONTACT_FILE_ADAPTER){
				shareIcon.setVisibility(View.GONE);
			}
			else{
				if(fromShared){
					shareIcon.setVisibility(View.GONE);
				}
				else{
					if (!isFolderLink){
						shareIcon.setVisibility(View.VISIBLE);
						shareIcon.setOnClickListener(this);
					}
					else{
						shareIcon.setVisibility(View.GONE);
					}
				}
			}
			
			downloadIcon = (ImageView) findViewById(R.id.full_image_viewer_download);
			downloadIcon.setVisibility(View.VISIBLE);
			downloadIcon.setOnClickListener(this);
			
			propertiesIcon = (ImageView) findViewById(R.id.full_image_viewer_properties);
			if (!isFolderLink){
				propertiesIcon.setVisibility(View.VISIBLE);
			}
			else{
				propertiesIcon.setVisibility(View.GONE);
			}
			propertiesIcon.setOnClickListener(this);
			
			linkIcon = (ImageView) findViewById(R.id.full_image_viewer_get_link);
			if (!isFolderLink){
				linkIcon.setVisibility(View.VISIBLE);
				linkIcon.setOnClickListener(this);
				if(adapterType==Constants.CONTACT_FILE_ADAPTER){
					linkIcon.setVisibility(View.GONE);
				}
				else{
					if(fromShared){
						linkIcon.setVisibility(View.GONE);
					}
					else{
						shareIcon.setVisibility(View.VISIBLE);
						shareIcon.setOnClickListener(this);
					}
				}
			}
			else{
				linkIcon.setVisibility(View.GONE);
			}

			ArrayAdapter<String> arrayAdapter;

			if(fromShared){
				node = megaApi.getNodeByHandle(imageHandles.get(positionG));

				int accessLevel = megaApi.getAccess(node);

				if(accessLevel== MegaShare.ACCESS_FULL){
					String menuOptions[] = new String[4];
					menuOptions[0] = getString(R.string.context_rename);
					menuOptions[1] = getString(R.string.context_move);
					menuOptions[2] = getString(R.string.context_copy);
					menuOptions[3] = getString(R.string.context_remove);
					overflowMenuList = (ListView) findViewById(R.id.image_viewer_overflow_menu_list);
					arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
					overflowMenuList.setAdapter(arrayAdapter);
				}
				else{
					String menuOptions[] = new String[1];
					menuOptions[0] = getString(R.string.context_copy);
					overflowMenuList = (ListView) findViewById(R.id.image_viewer_overflow_menu_list);
					arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
					overflowMenuList.setAdapter(arrayAdapter);
				}
			}
			else{
				String menuOptions[] = new String[4];
				menuOptions[0] = getString(R.string.context_rename);
				menuOptions[1] = getString(R.string.context_move);
				menuOptions[2] = getString(R.string.context_copy);
				menuOptions[3] = getString(R.string.context_move_to_trash);
				overflowMenuList = (ListView) findViewById(R.id.image_viewer_overflow_menu_list);
				arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
				overflowMenuList.setAdapter(arrayAdapter);
			}

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
	
	public void sortByNameDescending(){
		
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();
		
		
		for(int k = 0; k < mOffList.size() ; k++) {
			MegaOffline node = mOffList.get(k);
			if(node.getType().equals("1")){
				foldersOrder.add(node.getName());
			}
			else{
				filesOrder.add(node.getName());
			}
		}
		
	
		Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.reverse(foldersOrder);
		Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.reverse(filesOrder);

		for(int k = 0; k < foldersOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));
				}				
			}
			
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));					
				}				
			}
			
		}
		
		mOffList.clear();
		mOffList.addAll(tempOffline);
	}

	
	public void sortByNameAscending(){
		log("sortByNameAscending");
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();
				
		for(int k = 0; k < mOffList.size() ; k++) {
			MegaOffline node = mOffList.get(k);
			if(node.getType().equals("1")){
				foldersOrder.add(node.getName());
			}
			else{
				filesOrder.add(node.getName());
			}
		}		
	
		Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);

		for(int k = 0; k < foldersOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));
				}				
			}			
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));
				}				
			}
			
		}
		
		mOffList.clear();
		mOffList.addAll(tempOffline);
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
					if ((adapterType == Constants.OFFLINE_ADAPTER) || (adapterType == Constants.ZIP_ADAPTER)){
						
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
		
		if (adapterType == Constants.OFFLINE_ADAPTER){
			switch (v.getId()){
				case R.id.full_image_viewer_icon:{
					finish();
					break;
				}
				case R.id.full_image_viewer_share:{
					String offlineDirectory;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR;
					}
					else{
						offlineDirectory = getFilesDir().getPath();
					}	
					
					String fileName = offlineDirectory + mOffListImages.get(positionG).getPath() + mOffListImages.get(positionG).getName();
					File previewFile = new File(fileName);
					
					if (previewFile.exists()){
						Intent share = new Intent(android.content.Intent.ACTION_SEND);
						share.setType("image/*");
						share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + previewFile));
						startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
					}
					else{
						Snackbar.make(fragmentContainer, fileName + ": "  + getString(R.string.full_image_viewer_not_preview), Snackbar.LENGTH_LONG).show();
					}
					
					break;
				}
			}
		}
		else if (adapterType == Constants.ZIP_ADAPTER){
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
					
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
						if (!hasStoragePermission) {
							ActivityCompat.requestPermissions(this,
					                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									Constants.REQUEST_WRITE_STORAGE);
							
							handleListM.add(node.getHandle());
							
							return;
						}
					}
					
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
	
	@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
        	case Constants.REQUEST_WRITE_STORAGE:{
		        boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (hasStoragePermission) {
					downloadNode(handleListM);
				}
	        	break;
	        }
        }
    }
	
	@SuppressLint("NewApi") 
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
	
	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putInt("adapterType", adapterType);
		if ((adapterType == Constants.OFFLINE_ADAPTER) || (adapterType == Constants.ZIP_ADAPTER)){
			
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
		
		if ((adapterType == Constants.OFFLINE_ADAPTER) || (adapterType == Constants.ZIP_ADAPTER)){
			
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

		if(node.isExported()){
			log("node is already exported: "+node.getName());
			log("node link: "+node.getPublicLink());
			showGetLinkPanel(node.getPublicLink(), node.getExpirationTime());
		}
		else{
			NodeController nC = new NodeController(fullScreenImageViewer);
			MegaNode node = megaApi.getNodeByHandle(imageHandles.get(positionG));
			nC.exportLink(node);
		}
	}
	
	public void showRenameDialog(){
		
		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, node.isFolder());
//		input.setId(EDIT_TEXT_ID);
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
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_move_to_rubbish);
			builder.setMessage(message).setPositiveButton(R.string.general_move, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
		else{
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_delete_from_mega);
			builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
	}

	public void showGetLinkPanel(final String link, long expirationTimestamp){
		log("showGetLinkPanel: "+link);

		final Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);

		final DatePickerDialog datePickerDialog = new DatePickerDialog(fullScreenImageViewer, this, year, month, day);
		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);

		builder.setTitle(getString(R.string.context_get_link_menu));

		LayoutInflater inflater = getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.panel_get_link, null);

		final CheckedTextView linkWithoutKeyCheck = (CheckedTextView) dialoglayout.findViewById(R.id.link_without_key);
		linkWithoutKeyCheck.setChecked(true);
		linkWithoutKeyCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		linkWithoutKeyCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams linkWOK = (ViewGroup.MarginLayoutParams) linkWithoutKeyCheck.getLayoutParams();
		linkWOK.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(14, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		final CheckedTextView linkDecryptionKeyCheck = (CheckedTextView) dialoglayout.findViewById(R.id.link_decryption_key);
		linkDecryptionKeyCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		linkDecryptionKeyCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams linkDecry = (ViewGroup.MarginLayoutParams) linkDecryptionKeyCheck.getLayoutParams();
		linkDecry.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		final CheckedTextView linkWithKeyCheck = (CheckedTextView) dialoglayout.findViewById(R.id.link_with_key);
		linkWithKeyCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		linkWithKeyCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams linkWK = (ViewGroup.MarginLayoutParams) linkWithKeyCheck.getLayoutParams();
		linkWK.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		RelativeLayout expiryDateLayout = (RelativeLayout) dialoglayout.findViewById(R.id.expiry_date_layout);
		LinearLayout.LayoutParams paramsDateLayout = (LinearLayout.LayoutParams)expiryDateLayout.getLayoutParams();
		paramsDateLayout.setMargins(Util.scaleWidthPx(26, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, 0);
		expiryDateLayout.setLayoutParams(paramsDateLayout);

		TextView expiryDateTitle = (TextView) dialoglayout.findViewById(R.id.title_set_expiry_date);
		expiryDateTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));

		TextView expiryDateSubtitle = (TextView) dialoglayout.findViewById(R.id.subtitle_set_expiry_date);
		expiryDateSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleText));

		expiryDateButton = (Button) dialoglayout.findViewById(R.id.expiry_date);
		LinearLayout.LayoutParams paramsExpiryDate = (LinearLayout.LayoutParams)expiryDateButton.getLayoutParams();
		paramsExpiryDate.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, 0);
		expiryDateButton.setLayoutParams(paramsExpiryDate);

		final TextView linkText = (TextView) dialoglayout.findViewById(R.id.link);
		linkText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleText));
		LinearLayout.LayoutParams paramsLink = (LinearLayout.LayoutParams)linkText.getLayoutParams();
		paramsLink.setMargins(Util.scaleWidthPx(26, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(16, outMetrics), Util.scaleHeightPx(6, outMetrics));
		linkText.setLayoutParams(paramsLink);

		switchGetLink = (SwitchCompat) dialoglayout.findViewById(R.id.switch_set_expiry_date);
		RelativeLayout.LayoutParams paramsSwitch = (RelativeLayout.LayoutParams)switchGetLink.getLayoutParams();
		paramsSwitch.setMargins(0, 0, Util.scaleWidthPx(16, outMetrics), 0);
		switchGetLink.setLayoutParams(paramsSwitch);

		linkWithoutKeyCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				linkWithoutKeyCheck.setChecked(true);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(false);
				String urlString="";
				String [] s = link.split("!");
				if (s.length == 3){
					urlString = s[0] + "!" + s[1];
				}
				linkText.setText(urlString);
			}
		});

		linkDecryptionKeyCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(true);
				linkWithKeyCheck.setChecked(false);
				String keyString="!";
				String [] s = link.split("!");
				if (s.length == 3){
					keyString = keyString+s[2];
				}
				linkText.setText(keyString);
			}
		});

		linkWithKeyCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(true);
				linkText.setText(link);
			}
		});

		datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_NEGATIVE) {
					log("Negative button of DatePicker clicked");
					switchGetLink.setChecked(false);
					expiryDateButton.setVisibility(View.INVISIBLE);
				}
			}
		});
		//Set by default, link without key
		String urlString="";
		String [] s = link.split("!");
		if (s.length == 3){
			urlString = s[0] + "!" + s[1];
		}
		linkText.setText(urlString);
		linkWithoutKeyCheck.setChecked(true);

		builder.setView(dialoglayout);
//
		builder.setPositiveButton(getString(R.string.context_send), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, linkText.getText());
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
					android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", linkText.getText());
					clipboard.setPrimaryClip(clip);
				}
				Snackbar.make(fragmentContainer, getString(R.string.file_properties_get_link), Snackbar.LENGTH_LONG).show();
			}
		});

		getLinkDialog = builder.create();

		expiryDateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				datePickerDialog.show();
			}
		});

		if(accountType>MegaAccountDetails.ACCOUNT_TYPE_FREE){
			log("The user is PRO - enable expiration date");

			if(expirationTimestamp<=0){
				switchGetLink.setChecked(false);
				expiryDateButton.setVisibility(View.INVISIBLE);
			}
			else{
				switchGetLink.setChecked(true);
				java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
				Calendar cal = Util.calculateDateFromTimestamp(expirationTimestamp);
				TimeZone tz = cal.getTimeZone();
				df.setTimeZone(tz);
				Date date = cal.getTime();
				String formattedDate = df.format(date);
				expiryDateButton.setText(formattedDate);
				expiryDateButton.setVisibility(View.VISIBLE);
			}

			switchGetLink.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(switchGetLink.isChecked()){
						datePickerDialog.show();
					}
					else{
						isExpiredDateLink=true;
						NodeController nC = new NodeController(fullScreenImageViewer);
						MegaNode node = megaApi.getNodeByHandle(imageHandles.get(positionG));
						nC.exportLink(node);
					}
				}
			});
		}
		else{
			log("The is user is not PRO");
			switchGetLink.setEnabled(false);
			expiryDateButton.setVisibility(View.INVISIBLE);
		}

		log("show getLinkDialog");
		getLinkDialog.show();
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		log("onDateSet: "+year+monthOfYear+dayOfMonth);

		Calendar cal = Calendar.getInstance();
		cal.set(year, monthOfYear, dayOfMonth);
		Date date = cal.getTime();
		SimpleDateFormat dfTimestamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
		String dateString = dfTimestamp.format(date);
		dateString = dateString + "2359";
		log("the date string is: "+dateString);
		int timestamp = (int) Util.calculateTimestamp(dateString);
		log("the TIMESTAMP is: "+timestamp);
		isExpiredDateLink=true;
		NodeController nC = new NodeController(this);
		MegaNode node = megaApi.getNodeByHandle(imageHandles.get(positionG));
		nC.exportLinkTimestamp(node, timestamp);
	}


	public void setIsGetLink(boolean value){
		this.isGetLink = value;
	}

	public void setExpiredDateLink(boolean expiredDateLink) {
		isExpiredDateLink = expiredDateLink;
	}

	public boolean isExpiredDateLink() {
		return isExpiredDateLink;
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

				if (isGetLink){
					final String link = request.getLink();
					MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
					log("EXPIRATION DATE: "+node.getExpirationTime());
					if(isExpiredDateLink){
						log("change the expiration date");

						if(node.getExpirationTime()<=0){
							switchGetLink.setChecked(false);
							expiryDateButton.setVisibility(View.INVISIBLE);
						}
						else{
							switchGetLink.setChecked(true);
							java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
							Calendar cal = Util.calculateDateFromTimestamp(node.getExpirationTime());
							TimeZone tz = cal.getTimeZone();
							df.setTimeZone(tz);
							Date date = cal.getTime();
							String formattedDate = df.format(date);
							expiryDateButton.setText(formattedDate);
							expiryDateButton.setVisibility(View.VISIBLE);
						}
					}
					else{
						showGetLinkPanel(link, node.getExpirationTime());
					}
				}
				log("link: "+request.getLink());
			}
			else{
				log("Error: "+e.getErrorString());
				showSnackbar(getString(R.string.context_no_link));
			}
			isGetLink=false;
			isExpiredDateLink=false;
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
		Util.log("FullScreenImageViewerLollipop", message);
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
						if (MegaApiUtils.isIntentAvailable(this, viewIntent))
							startActivity(viewIntent);
						else{
							Intent intentShare = new Intent(Intent.ACTION_SEND);
							intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
							if (MegaApiUtils.isIntentAvailable(this, intentShare))
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

	public void showSnackbar(String s){
		log("showSnackbar");
		Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
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
