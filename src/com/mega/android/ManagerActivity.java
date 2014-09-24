package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mega.android.FileStorageActivity.Mode;
import com.mega.components.EditTextCursorWatcher;
import com.mega.sdk.MegaAccountDetails;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaTransferListenerInterface;
import com.mega.sdk.MegaUser;
import com.mega.sdk.NodeList;
import com.mega.sdk.TransferList;
import com.mega.sdk.UserList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.os.UserManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils.TruncateAt;
import android.text.format.Formatter;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class ManagerActivity extends PinActivity implements OnItemClickListener, OnClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaTransferListenerInterface {
			
	public enum DrawerItem {
		CLOUD_DRIVE, SAVED_FOR_OFFLINE, SHARED_WITH_ME, RUBBISH_BIN, CONTACTS, PHOTO_SYNC, TRANSFERS, ACCOUNT, SEARCH;

		public String getTitle(Context context) {
			switch(this)
			{
				case CLOUD_DRIVE: return context.getString(R.string.section_cloud_drive);
				case SAVED_FOR_OFFLINE: return context.getString(R.string.section_saved_for_offline);
				case SHARED_WITH_ME: return context.getString(R.string.section_shared_with_me);
				case RUBBISH_BIN: return context.getString(R.string.section_rubbish_bin);
				case CONTACTS: return context.getString(R.string.section_contacts);
				case PHOTO_SYNC: return context.getString(R.string.section_image_viewer);
				case TRANSFERS: return context.getString(R.string.section_transfers);
				case ACCOUNT: return context.getString(R.string.section_account);
				case SEARCH: return context.getString(R.string.search_files_and_folders);
			}
			return null;
		}
	}
	
	public static int REQUEST_CODE_GET = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_GET_LOCAL = 1003;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_REFRESH = 1005;
	public static int REQUEST_CODE_SORT_BY = 1006;
	public static int REQUEST_CODE_SELECT_IMPORT_FOLDER = 1007;
	
	public static String ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD";
	public static String ACTION_CANCEL_UPLOAD = "CANCEL_UPLOAD";
	public static String ACTION_CANCEL_CAM_SYNC = "CANCEL_CAM_SYNC";
	public static String ACTION_OPEN_MEGA_LINK = "OPEN_MEGA_LINK";
	public static String ACTION_OPEN_MEGA_FOLDER_LINK = "OPEN_MEGA_FOLDER_LINK";
	public static String ACTION_IMPORT_LINK_FETCH_NODES = "IMPORT_LINK_FETCH_NODES";
	public static String ACTION_FILE_EXPLORER_UPLOAD = "FILE_EXPLORER_UPLOAD";
	public static String ACTION_REFRESH_PARENTHANDLE_BROWSER = "REFRESH_PARENTHANDLE_BROWSER";
	
	public static String EXTRA_OPEN_FOLDER = "EXTRA_OPEN_FOLER";
	
	final public static int FILE_BROWSER_ADAPTER = 2000;
	final public static int CONTACT_FILE_ADAPTER = 2001;
	final public static int RUBBISH_BIN_ADAPTER = 2002;
	final public static int SHARED_WITH_ME_ADAPTER = 2003;
	final public static int OFFLINE_ADAPTER = 2004;
	final public static int FOLDER_LINK_ADAPTER = 2005;
	final public static int SEARCH_ADAPTER = 2006;
	final public static int PHOTO_SYNC_ADAPTER = 2007;
	
	private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    
   	private SearchView mSearchView;    
	private MenuItem searchMenuItem;
	
	private MenuItem createFolderMenuItem;
	private MenuItem rubbishBinMenuItem;
	private MenuItem addMenuItem;
	private MenuItem refreshMenuItem;
	private MenuItem sortByMenuItem;
	private MenuItem helpMenuItem;
	private MenuItem upgradeAccountMenuItem;
	private MenuItem settingsMenuItem;
	private MenuItem logoutMenuItem;
	
	private static DrawerItem drawerItem;
	
	private TableLayout topControlBar;
	private TableLayout bottomControlBar;
	private ImageView imageProfile;
	private TextView userName;
	private TextView userEmail;
	private TextView usedSpaceText;
	private TextView usedSpace;
	
	ProgressBar usedSpaceBar;
	
	MegaUser contact = null;
	
	ImageButton customListGrid;
	LinearLayout customSearch;

	private boolean firstTime = true;
	
	long parentHandleBrowser;
	long parentHandleRubbish;
	long parentHandleSharedWithMe;
	long parentHandleSearch;
	private boolean isListCloudDrive = true;
	private boolean isListContacts = true;
	private boolean isListRubbishBin = true;
	private boolean isListSharedWithMe = true;
	private boolean isListOffline = true;
	private boolean isListPhotoSync = true;
	private FileBrowserFragment fbF;
	private ContactsFragment cF;
	private RubbishBinFragment rbF;
	private SharedWithMeFragment swmF;
    private TransfersFragment tF; 
    private MyAccountFragment maF;
    private OfflineFragment oF;
    private SearchFragment sF;
    private PhotoSyncFragment psF;
    
    static ManagerActivity managerActivity;
    private MegaApiAndroid megaApi;
    
    private static int EDIT_TEXT_ID = 1;
    
    private AlertDialog renameDialog;
    private AlertDialog newFolderDialog;
    private AlertDialog addContactDialog;
    private AlertDialog clearRubbishBinDialog;
    
    private Handler handler;
    
    private boolean moveToRubbish = false;
    
    private boolean isClearRubbishBin = false;
    
    ProgressDialog statusDialog;
    
	public UploadHereDialog uploadDialog;
	
	private List<ShareInfo> filePreparedInfos;
	
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	ActionBar aB;
	
	String urlLink = "";
		
	SparseArray<TransfersHolder> transfersListArray = null;
	
	boolean downloadPlay = true;
	
	boolean pauseIconVisible = false;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	MegaAttributes attr = null;
	
	TransferList tL;
	
	String searchQuery = null;
	NodeList searchNodes;
	int levelsSearch = -1;
	
	private boolean openLink = false;
	
	MegaApplication app;
	
	NavigationDrawerAdapter nDA;
	
	String pathNavigation = "/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");

//	    dbH = new DatabaseHandler(getApplicationContext());
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		if (Util.isOnline(this)){
			dbH.setAttrOnline(true);
		}
		else{
			dbH.setAttrOnline(false);
		}
		
    	log("onCreate()");
    	
		super.onCreate(savedInstanceState);
		managerActivity = this;
		if (aB == null){
			aB = getSupportActionBar();
		}

		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
	    
		if (dbH.getCredentials() == null){
			Intent newIntent = getIntent();
		    
		    if (newIntent != null){
		    	if (newIntent.getAction() != null){
		    		if (newIntent.getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_LINK) || newIntent.getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK)){
		    			openLink = true;
		    		}
		    		else if (newIntent.getAction().equals(ACTION_CANCEL_UPLOAD) || newIntent.getAction().equals(ACTION_CANCEL_DOWNLOAD) || newIntent.getAction().equals(ACTION_CANCEL_CAM_SYNC)){
		    			Intent cancelTourIntent = new Intent(this, TourActivity.class);
		    			cancelTourIntent.setAction(newIntent.getAction());
		    			startActivity(cancelTourIntent);
		    			finish();
		    			return;		    			
		    		}
		    	}
		    }
		    
		    if (!openLink){
		    	logout(this, (MegaApplication)getApplication(), megaApi, false);
		    }
		    
	    	return;
		}
				
		prefs = dbH.getPreferences();
		if (prefs == null){
			firstTime = true;
		}
		else{
			if (prefs.getFirstTime() == null){
				firstTime = true;
			}
			else{
				firstTime = Boolean.parseBoolean(prefs.getFirstTime());
			}
		}
				
		getOverflowMenu();
		
		handler = new Handler();
		
		setContentView(R.layout.activity_manager);

		imageProfile = (ImageView) findViewById(R.id.profile_photo);
		userEmail = (TextView) findViewById(R.id.profile_user_email);
		userEmail.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		userEmail.getLayoutParams().width = Util.px2dp((235*scaleW), outMetrics);
		userEmail.setSingleLine();
		userEmail.setEllipsize(TruncateAt.END);
		userName = (TextView) findViewById(R.id.profile_user_name);
		userName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		userName.getLayoutParams().width = Util.px2dp((235*scaleW), outMetrics);
		userName.setSingleLine();
		userName.setEllipsize(TruncateAt.END);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer_list);
        topControlBar = (TableLayout) findViewById(R.id.top_control_bar);
        topControlBar.setOnClickListener(this);
        bottomControlBar = (TableLayout) findViewById(R.id.bottom_control_bar);
        bottomControlBar.setOnClickListener(this);
        usedSpace = (TextView) findViewById(R.id.used_space);
        usedSpaceText = (TextView) findViewById(R.id.used_space_text);
        usedSpaceBar = (ProgressBar) findViewById(R.id.manager_used_space_bar);
        
        usedSpaceBar.setProgress(0);
//        barFill = (ImageView) findViewById(R.id.bar_fill);
//        barStructure = (ImageView) findViewById(R.id.bar_structure);
        
        if (!Util.isOnline(this)){
        	
        	Intent offlineIntent = new Intent(this, OfflineActivity.class);
			startActivity(offlineIntent);
			finish();
        	return;
        	/*dbH.setAttrOnline(false);
        	
        	userName.setVisibility(View.INVISIBLE);
        	userEmail.setVisibility(View.INVISIBLE);
        	bottomControlBar.setVisibility(View.INVISIBLE);
        	topControlBar.setVisibility(View.INVISIBLE);
        	
        	List<String> items = new ArrayList<String>();
			for (DrawerItem item : DrawerItem.values()) {
				if (!item.equals(DrawerItem.RUBBISH_BIN) && (!item.equals(DrawerItem.SEARCH))){
					items.add(item.getTitle(this));
				}
			}
	        
			nDA = new NavigationDrawerAdapter(getApplicationContext(), items);
			nDA.setPositionClicked(1);
			mDrawerList.setAdapter(nDA);
			
	        
	        getSupportActionBar().setIcon(R.drawable.ic_launcher);
	        getSupportActionBar().setHomeButtonEnabled(true);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	        
	        mDrawerToggle = new ActionBarDrawerToggle(
	                this,                  // host Activity 
	                mDrawerLayout,         // DrawerLayout object 
	                R.drawable.ic_drawer,  // nav drawer image to replace 'Up' caret 
	                R.string.app_name,  // "open drawer" description for accessibility 
	                R.string.app_name  // "close drawer" description for accessibility 
	                ) {
	            public void onDrawerClosed(View view) {
	                supportInvalidateOptionsMenu();	// creates call to onPrepareOptionsMenu()
	            }
	
	            public void onDrawerOpened(View drawerView) {
	                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
	            }
	        };
	        mDrawerToggle.setDrawerIndicatorEnabled(true);
	        mDrawerLayout.setDrawerListener(mDrawerToggle);
	        if (savedInstanceState == null){
	        	mDrawerLayout.openDrawer(Gravity.LEFT);
	        }
	        else{
				mDrawerLayout.closeDrawer(Gravity.LEFT);
	        }
	        
	        mDrawerLayout.setVisibility(View.GONE);
	        
	        //Create the actionBar Menu
	        getSupportActionBar().setDisplayShowCustomEnabled(true);
	        getSupportActionBar().setCustomView(R.layout.custom_action_bar_top);
	        
	        customSearch = (LinearLayout) getSupportActionBar().getCustomView().findViewById(R.id.custom_search);
	        customSearch.setVisibility(View.INVISIBLE);
			
			customListGrid = (ImageButton) getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
			customListGrid.setOnClickListener(this);
			
			drawerItem = DrawerItem.SAVED_FOR_OFFLINE;
			mDrawerLayout.closeDrawer(Gravity.LEFT);
			
			//INITIAL FRAGMENT
			selectDrawerItem(drawerItem);
			
        	return;*/
        }
        
        dbH.setAttrOnline(true);
        this.setPathNavigationOffline(pathNavigation);
        
        MegaNode rootNode = megaApi.getRootNode();
		if (rootNode == null){
			if (getIntent() != null){
				if (getIntent().getAction() != null){
					if (getIntent().getAction().equals(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES)){
						Intent intent = new Intent(managerActivity, LoginActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();	
						return;
					}
					else if (getIntent().getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_LINK)){
						Intent intent = new Intent(managerActivity, FileLinkActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();	
						return;
					}
					else if (getIntent().getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK)){
						Intent intent = new Intent(managerActivity, LoginActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();	
						return;
					}
					else if (getIntent().getAction().equals(ACTION_CANCEL_UPLOAD) || getIntent().getAction().equals(ACTION_CANCEL_DOWNLOAD) || getIntent().getAction().equals(ACTION_CANCEL_CAM_SYNC)){
						Intent intent = new Intent(managerActivity, LoginActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(getIntent().getAction());
						startActivity(intent);
						finish();
						return;
					}
				}
			}
			Intent intent = new Intent(managerActivity, LoginActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		else{
			log("rootNode != null");		
			megaApi.addGlobalListener(this);
			megaApi.addTransferListener(this);
			UserList contacts = megaApi.getContacts();
			for (int i=0; i < contacts.size(); i++){
				if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_ME){
					contact = contacts.get(i);
				}
			}
			
			if (contact != null){
				userEmail.setVisibility(View.VISIBLE);
				userEmail.setText(contact.getEmail());
				String userNameString = contact.getEmail();
				String [] sp = userNameString.split("@");
				if (sp.length != 0){
					userNameString = sp[0];
					userName.setVisibility(View.VISIBLE);
					userName.setText(userNameString);
				}
				
				File avatar = null;
				if (getExternalCacheDir() != null){
					avatar = new File(getExternalCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
				}
				else{
					avatar = new File(getCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
				}
				Bitmap imBitmap = null;
				if (avatar.exists()){
					if (avatar.length() > 0){
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bOpts.inPurgeable = true;
						bOpts.inInputShareable = true;
						imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
						if (imBitmap == null) {
							avatar.delete();
							if (getExternalCacheDir() != null){
								megaApi.getUserAvatar(contact, getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
							}
							else{
								megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
							}
						}
						else{
							Bitmap circleBitmap = Bitmap.createBitmap(imBitmap.getWidth(), imBitmap.getHeight(), Bitmap.Config.ARGB_8888);
							
							BitmapShader shader = new BitmapShader (imBitmap,  TileMode.CLAMP, TileMode.CLAMP);
					        Paint paint = new Paint();
					        paint.setShader(shader);
					
					        Canvas c = new Canvas(circleBitmap);
					        int radius; 
					        if (imBitmap.getWidth() < imBitmap.getHeight())
					        	radius = imBitmap.getWidth()/2;
					        else
					        	radius = imBitmap.getHeight()/2;
					        
						    c.drawCircle(imBitmap.getWidth()/2, imBitmap.getHeight()/2, radius, paint);
					        imageProfile.setImageBitmap(circleBitmap);
						}
					}
					else{
						if (getExternalCacheDir() != null){
							megaApi.getUserAvatar(contact, getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
						}
						else{
							megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
						}
					}
				}
				else{
					if (getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
					}
					else{
						megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
					}
				}
			}
			
			bottomControlBar.setVisibility(View.GONE);
	        
	        megaApi.getAccountDetails(this);
	        
	        List<String> items = new ArrayList<String>();
			for (DrawerItem item : DrawerItem.values()) {
				if (!item.equals(DrawerItem.RUBBISH_BIN) && (!item.equals(DrawerItem.SEARCH))){
					items.add(item.getTitle(this));
				}
			}
	        
			nDA = new NavigationDrawerAdapter(getApplicationContext(), items);
			mDrawerList.setAdapter(nDA);
	        
	        mDrawerList.setOnItemClickListener(this);
	        
	        getSupportActionBar().setIcon(R.drawable.ic_launcher);
	        getSupportActionBar().setHomeButtonEnabled(true);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	        
	        mDrawerToggle = new ActionBarDrawerToggle(
	                this,                  /* host Activity */
	                mDrawerLayout,         /* DrawerLayout object */
	                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
	                R.string.app_name,  /* "open drawer" description for accessibility */
	                R.string.app_name  /* "close drawer" description for accessibility */
	                ) {
	            public void onDrawerClosed(View view) {
	                supportInvalidateOptionsMenu();	// creates call to onPrepareOptionsMenu()
	            }
	
	            public void onDrawerOpened(View drawerView) {
	                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
	            }
	        };
	        mDrawerToggle.setDrawerIndicatorEnabled(true);
	        mDrawerLayout.setDrawerListener(mDrawerToggle);
	        if (savedInstanceState == null){
	        	mDrawerLayout.openDrawer(Gravity.LEFT);
	        }
	        else{
				mDrawerLayout.closeDrawer(Gravity.LEFT);
	        }
	        
	        mDrawerLayout.setVisibility(View.VISIBLE);
	        
	        //Create the actionBar Menu
	        getSupportActionBar().setDisplayShowCustomEnabled(true);
	        getSupportActionBar().setCustomView(R.layout.custom_action_bar_top);
	        
	        customSearch = (LinearLayout) getSupportActionBar().getCustomView().findViewById(R.id.custom_search);
	        customSearch.setOnClickListener(this);
			
			customListGrid = (ImageButton) getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
			customListGrid.setOnClickListener(this);
			
			parentHandleBrowser = -1;
			parentHandleRubbish = -1;
			parentHandleSharedWithMe = -1;
			parentHandleSearch = -1;
			orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
			if (savedInstanceState != null){
				firstTime = false;
				int visibleFragment = savedInstanceState.getInt("visibleFragment");
				orderGetChildren = savedInstanceState.getInt("orderGetChildren");
				parentHandleBrowser = savedInstanceState.getLong("parentHandleBrowser");
				parentHandleRubbish = savedInstanceState.getLong("parentHandleRubbish");
				parentHandleSharedWithMe = savedInstanceState.getLong("parentHandleSharedWithMe");
				parentHandleSearch = savedInstanceState.getLong("parentHandleSearch");
				switch (visibleFragment){
					case 1:{
						drawerItem = DrawerItem.CLOUD_DRIVE;
						isListCloudDrive = true;
						break;
					}
					case 2:{
						drawerItem = DrawerItem.CLOUD_DRIVE;
						isListCloudDrive = false;
						break;
					}
					case 3:{
						drawerItem = DrawerItem.CONTACTS;
						isListContacts = true;
						break;
					}
					case 4:{
						drawerItem = DrawerItem.CONTACTS;
						isListContacts = false;
						break;
					}
					case 5:{
						drawerItem = DrawerItem.RUBBISH_BIN;
						isListRubbishBin = true;
						break;
					}
					case 6:{
						drawerItem = DrawerItem.RUBBISH_BIN;
						isListRubbishBin = false;
						break;
					}
					case 7:{
						drawerItem = DrawerItem.TRANSFERS;
						downloadPlay = savedInstanceState.getBoolean("downloadPlay", true);
						pauseIconVisible = savedInstanceState.getBoolean("pauseIconVisible", false);
						break;
					}
					case 8:{
						drawerItem = DrawerItem.SHARED_WITH_ME;
						isListSharedWithMe = true;
						break;
					}
					case 9:{
						drawerItem = DrawerItem.SHARED_WITH_ME;
						isListSharedWithMe = false;
						break;
					}
					case 10:{
						drawerItem = DrawerItem.ACCOUNT;
						break;
					}
					case 11:{
						drawerItem = DrawerItem.SEARCH;
						searchQuery = savedInstanceState.getString("searchQuery");
						levelsSearch = savedInstanceState.getInt("levels");
						break;
					}
					case 12:{
						drawerItem = DrawerItem.PHOTO_SYNC;
						isListPhotoSync = true;
						break;
					}
					case 13:{
						drawerItem = DrawerItem.PHOTO_SYNC;
						isListPhotoSync = false;
						break;
					}
					
				}
			}
			
			if (drawerItem == null) {
				drawerItem = DrawerItem.CLOUD_DRIVE;
			}
			else{
				mDrawerLayout.closeDrawer(Gravity.LEFT);
			}
	
			//INITIAL FRAGMENT
			selectDrawerItem(drawerItem);
		}
	}
    
    @Override
	protected void onSaveInstanceState(Bundle outState) {
    	log("onSaveInstaceState");
    	super.onSaveInstanceState(outState);
    	
    	long pHBrowser = -1;
    	long pHRubbish = -1;
    	long pHSharedWithMe = -1;
    	long pHSearch = -1;
    	int visibleFragment = -1;
    	String pathOffline = this.pathNavigation;
    	
    	int order = this.orderGetChildren;
    	if (fbF != null){
    		pHBrowser = fbF.getParentHandle();
    		if (fbF.isVisible()){
    			if (isListCloudDrive){
    				visibleFragment = 1;
    			}
    			else{
    				visibleFragment = 2;
    			}
    		}
    	}
    	if (cF != null){
    		if (cF.isVisible()){
    			if (isListContacts){
    				visibleFragment = 3;
    			}
    			else{
    				visibleFragment = 4;
    			}
    		}
    	}
    	
    	if (rbF != null){
    		pHRubbish = rbF.getParentHandle();
    		if (rbF.isVisible()){
    			if (isListRubbishBin){
    				visibleFragment = 5;
    			}
    			else{
    				visibleFragment = 6;
    			}
    		}
    	}
    	
    	if (swmF != null){
    		pHSharedWithMe = swmF.getParentHandle();
    		if (swmF.isVisible()){
    			if (isListSharedWithMe){
    				visibleFragment = 8;
    			}
    			else{
    				visibleFragment = 9;
    			}
    		}
    	}
    	
    	if (tF != null){
	    	if (tF.isVisible()){
	    		visibleFragment = 7;
	    		outState.putBoolean("pauseIconVisible", pauseIconVisible);
	    		outState.putBoolean("downloadPlay", downloadPlay);
	    	}
    	}
    	
    	if (maF != null){
    		if (maF.isVisible()){
    			visibleFragment = 10;
    		}
    	}
    	
    	if (sF != null){
    		if (sF.isVisible()){
    			pHSearch = sF.getParentHandle();
    			visibleFragment = 11;
    			outState.putString("searchQuery", searchQuery);
    			outState.putInt("levels", sF.getLevels());
    		}
    	}
    	
    	if (psF != null){
    		if (psF.isVisible()){
    			if (isListPhotoSync){
    				visibleFragment = 12;
    			}
    			else{
    				visibleFragment = 13;
    			}
    			
    		}
    	}
    	
    	outState.putInt("orderGetChildren", order);
    	outState.putInt("visibleFragment", visibleFragment);
    	outState.putLong("parentHandleBrowser", pHBrowser);
    	outState.putLong("parentHandleRubbish", pHRubbish);
    	outState.putLong("parentHandleSharedWithMe", pHSharedWithMe);
    	outState.putLong("parentHandleSearch", pHSearch);
    }
    
    @Override
    protected void onDestroy(){
    	log("onDestroy()");

    	super.onDestroy();
    	    	
    	if (megaApi.getRootNode() != null){
    		megaApi.removeGlobalListener(this);
    	
//    		startService(new Intent(getApplicationContext(), CameraSyncService.class));
    		megaApi.removeTransferListener(this);
    	} 
    }
    
    @Override
	protected void onNewIntent(Intent intent){
    	log("onNewIntent");
    	if ((intent != null) && Intent.ACTION_SEARCH.equals(intent.getAction())){
    		searchQuery = intent.getStringExtra(SearchManager.QUERY);
    		parentHandleSearch = -1;
    		
    		selectDrawerItem(DrawerItem.SEARCH);
    		
    		if (searchMenuItem != null) {
    			MenuItemCompat.collapseActionView(searchMenuItem);
			}
    		return;
    	}
    	
    	super.onNewIntent(intent);
    	setIntent(intent); 
    	return;
	}
    
    @Override
	protected void onPause() {
    	log("onPause");
    	managerActivity = null;
    	super.onPause();
    }
    
    @Override
	protected void onResume() {
    	log("onResume");
    	super.onResume();
    	managerActivity = this;
    	
    	Intent intent = getIntent(); 
    	
//    	dbH = new DatabaseHandler(getApplicationContext());
    	dbH = DatabaseHandler.getDbHandler(getApplicationContext());
    	if(dbH.getCredentials() == null){	
    		if (!openLink){
    			logout(this, (MegaApplication)getApplication(), megaApi, false);
    			return;
    		}			
		}
    	   	
    	if (intent != null) {    		
    		// Open folder from the intent
			if (intent.hasExtra(EXTRA_OPEN_FOLDER)) {
				parentHandleBrowser = intent.getLongExtra(EXTRA_OPEN_FOLDER, -1);
				intent.removeExtra(EXTRA_OPEN_FOLDER);
				setIntent(null);
			}
    					
    		if (intent.getAction() != null){    			
    			if (getIntent().getAction().equals(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES)){
					Intent loginIntent = new Intent(managerActivity, LoginActivity.class);
					loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					loginIntent.setAction(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES);
					loginIntent.setData(Uri.parse(getIntent().getDataString()));
					startActivity(loginIntent);
					finish();	
					return;
				}
				else if (getIntent().getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_LINK)){
					Intent fileLinkIntent = new Intent(managerActivity, FileLinkActivity.class);
					fileLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					fileLinkIntent.setAction(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES);
					fileLinkIntent.setData(Uri.parse(getIntent().getDataString()));
					startActivity(fileLinkIntent);
					finish();	
					return;
				}
    			else if (intent.getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)){
    				Intent intentFolderLink = new Intent(managerActivity, FolderLinkActivity.class);
    				intentFolderLink.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    				intentFolderLink.setAction(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK);
    				intentFolderLink.setData(Uri.parse(getIntent().getDataString()));
					startActivity(intentFolderLink);
					finish();
    			}
    			else if (intent.getAction().equals(ACTION_REFRESH_PARENTHANDLE_BROWSER)){
    				
    				parentHandleBrowser = intent.getLongExtra("parentHandle", -1);    				
    				intent.removeExtra("parentHandle");
    				setParentHandleBrowser(parentHandleBrowser);
    				
    				if (fbF != null){
						fbF.setParentHandle(parentHandleBrowser);
    					fbF.setIsList(isListCloudDrive);
    					fbF.setOrder(orderGetChildren);
    					NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderGetChildren);
    					fbF.setNodes(nodes);
    					if (!fbF.isVisible()){
    						getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fbF, "fbF").commit();
    					}
    				}	
    				else{
    					fbF = new FileBrowserFragment();
    					fbF.setParentHandle(parentHandleBrowser);
    					fbF.setIsList(isListCloudDrive);
    					fbF.setOrder(orderGetChildren);
    					NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderGetChildren);
    					fbF.setNodes(nodes);
    					if (!fbF.isVisible()){
    						getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fbF, "fbF").commit();
    					}
    				}
    			}
    			else if (intent.getAction().equals(ACTION_CANCEL_UPLOAD) || intent.getAction().equals(ACTION_CANCEL_DOWNLOAD) || intent.getAction().equals(ACTION_CANCEL_CAM_SYNC)){
    				log("ACTION_CANCEL_UPLOAD or ACTION_CANCEL_DOWNLOAD or ACTION_CANCEL_CAM_SYNC");
					Intent tempIntent = null;
					String title = null;
					String text = null;
					if(intent.getAction().equals(ACTION_CANCEL_UPLOAD)){
						tempIntent = new Intent(this, UploadService.class);
						tempIntent.setAction(UploadService.ACTION_CANCEL);
						title = getString(R.string.upload_uploading);
						text = getString(R.string.upload_cancel_uploading);
					} 
					else if (intent.getAction().equals(ACTION_CANCEL_DOWNLOAD)){
						tempIntent = new Intent(this, DownloadService.class);
						tempIntent.setAction(DownloadService.ACTION_CANCEL);
						title = getString(R.string.download_downloading);
						text = getString(R.string.download_cancel_downloading);
					}
					else if (intent.getAction().equals(ACTION_CANCEL_CAM_SYNC)){
						tempIntent = new Intent(this, CameraSyncService.class);
						tempIntent.setAction(CameraSyncService.ACTION_CANCEL);
						title = getString(R.string.cam_sync_syncing);
						text = getString(R.string.cam_sync_cancel_sync);
					}
					
					final Intent cancelIntent = tempIntent;
					AlertDialog.Builder builder = Util.getCustomAlertBuilder(this,
							title, text, null);
					builder.setPositiveButton(getString(R.string.general_yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									if (tF != null){
										if (tF.isVisible()){
											tF.setNoActiveTransfers();
											downloadPlay = true;
										}
									}	
									startService(cancelIntent);						
								}
							});
					builder.setNegativeButton(getString(R.string.general_no), null);
					final AlertDialog dialog = builder.create();
					try {
						dialog.show(); 
					}
					catch(Exception ex)	{ 
						startService(cancelIntent); 
					}
				}
    			intent.setAction(null);
				setIntent(null);
    		}
    	}
    }
    
    /*
	 * Open MEGA link from intent from another app
	 */
	private void handleOpenLinkIntent(Intent intent) {
		log("handleOpenLinkIntent");
		final String url = intent.getDataString();
		log("url: " + url);

		if (url != null && url.matches("^https://mega.co.nz/#!.*!.*$")) {
			importLink(url);
			intent.setData(null);
		}
	}
	
	/*
	 * Show Import Dialog
	 */
	private void importLink(final String url) {
		log("importLink");
		this.urlLink = url;
		String[] parts = parseDownloadUrl(url);
		if (parts == null) {
			if (url != null && url.matches("^https://mega.co.nz/#F!.+$")){
				Util.showErrorAlertDialog("importLink: Folder links not yet implemented", false, this);
			}
			else{
				Util.showErrorAlertDialog(getString(R.string.manager_download_from_link_incorrect), false, this);
			}
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
		log("parseDownloadUrl");
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
    
    public void selectDrawerItem(DrawerItem item){
    	log("selectDrawerItem");
    	switch (item){
    		case CLOUD_DRIVE:{
				if (fbF == null){
					fbF = new FileBrowserFragment();
					if (parentHandleBrowser == -1){
						fbF.setParentHandle(megaApi.getRootNode().getHandle());
						parentHandleBrowser = megaApi.getRootNode().getHandle();
					}
					else{
						fbF.setParentHandle(parentHandleBrowser);
					}
					fbF.setIsList(isListCloudDrive);
					fbF.setOrder(orderGetChildren);
					NodeList nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
					fbF.setNodes(nodes);
				}
				else{
					fbF.setIsList(isListCloudDrive);
					fbF.setParentHandle(parentHandleBrowser);
					fbF.setOrder(orderGetChildren);
					NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderGetChildren);
					fbF.setNodes(nodes);
				}
				
				
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fbF, "fbF").commit();
				if (isListCloudDrive){					
					customListGrid.setImageResource(R.drawable.ic_menu_gridview);
				}
				else{
    				customListGrid.setImageResource(R.drawable.ic_menu_listview);
    			}
    			    			
    			if (!firstTime){
    				mDrawerLayout.closeDrawer(Gravity.LEFT);
    			}
    			else{
    				firstTime = false;
    			}
    			
    			customListGrid.setVisibility(View.VISIBLE);
    			customSearch.setVisibility(View.VISIBLE);
    			

    			if (createFolderMenuItem != null){
	    			createFolderMenuItem.setVisible(true);
	    			rubbishBinMenuItem.setVisible(true);
	    			addMenuItem.setVisible(true);
	    			refreshMenuItem.setVisible(true);
	    			sortByMenuItem.setVisible(true);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(true);
	    			settingsMenuItem.setVisible(true);
	    			logoutMenuItem.setVisible(true);
	    			
	    			createFolderMenuItem.setIcon(R.drawable.ic_menu_new_folder_dark);
	    			rubbishBinMenuItem.setIcon(R.drawable.ic_menu_rubbish);
	    			rubbishBinMenuItem.setEnabled(true);
	    			addMenuItem.setIcon(R.drawable.ic_menu_add);
	    			addMenuItem.setEnabled(true);
    			}
    			
    			break;
    		}
    		case CONTACTS:{
    			
    			if (cF == null){
    				cF = new ContactsFragment();
    				cF.setIsList(isListContacts);
    			}
    			
    			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, cF, "cF").commit();
    			if (isListContacts){
    				customListGrid.setImageResource(R.drawable.ic_menu_gridview);
				}
				else{
    				customListGrid.setImageResource(R.drawable.ic_menu_listview);
    			}
    			
    			customListGrid.setVisibility(View.VISIBLE);
    			customSearch.setVisibility(View.VISIBLE);
    			
    			mDrawerLayout.closeDrawer(Gravity.LEFT);

    			if (createFolderMenuItem != null){
    				createFolderMenuItem.setVisible(true);
    				rubbishBinMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(true);
	    			sortByMenuItem.setVisible(true);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(true);
	    			settingsMenuItem.setVisible(true);
	    			logoutMenuItem.setVisible(true);
	    			
	    			createFolderMenuItem.setIcon(R.drawable.ic_action_social_add_person);
	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			rubbishBinMenuItem.setEnabled(false);
	    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			addMenuItem.setEnabled(false);
    			}
    			break;
    		}
    		case RUBBISH_BIN:{
    			
    			if (rbF == null){
    				rbF = new RubbishBinFragment();
    				rbF.setParentHandle(megaApi.getRubbishNode().getHandle());
    				parentHandleRubbish = megaApi.getRubbishNode().getHandle();
    				rbF.setIsList(isListRubbishBin);
    				rbF.setOrder(orderGetChildren);
    				NodeList nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
    				rbF.setNodes(nodes);
    			}
    			else{
    				rbF.setIsList(isListRubbishBin);
    				rbF.setParentHandle(parentHandleRubbish);
    				rbF.setOrder(orderGetChildren);
    				NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleRubbish), orderGetChildren);
    				rbF.setNodes(nodes);
    			}
    			
    			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, rbF, "rbF").commit();
    			if (isListRubbishBin){
    				customListGrid.setImageResource(R.drawable.ic_menu_gridview);
				}
				else{
    				customListGrid.setImageResource(R.drawable.ic_menu_listview);
    			}
    			
    			customListGrid.setVisibility(View.VISIBLE);
    			customSearch.setVisibility(View.VISIBLE);
    			mDrawerLayout.closeDrawer(Gravity.LEFT);
    			
    			if (createFolderMenuItem != null){
    				createFolderMenuItem.setVisible(true);
    				rubbishBinMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(true);
	    			sortByMenuItem.setVisible(true);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(true);
	    			settingsMenuItem.setVisible(true);
	    			logoutMenuItem.setVisible(true);
	    			
	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			rubbishBinMenuItem.setEnabled(false);
	    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setIcon(R.drawable.ic_menu_discard_dark);
	    		}

    			break;
    		}
    		case SHARED_WITH_ME:{
    			
    			if (swmF == null){
    				swmF = new SharedWithMeFragment();
    				swmF.setParentHandle(megaApi.getInboxNode().getHandle());
    				parentHandleSharedWithMe = megaApi.getInboxNode().getHandle();
    				swmF.setIsList(isListSharedWithMe);
    				swmF.setOrder(orderGetChildren);
    				NodeList nodes = megaApi.getChildren(megaApi.getInboxNode(), orderGetChildren);
    				swmF.setNodes(nodes);
    			}
    			else{
    				swmF.setIsList(isListSharedWithMe);
    				swmF.setParentHandle(parentHandleSharedWithMe);
    				swmF.setOrder(orderGetChildren);
    				NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleSharedWithMe), orderGetChildren);
    				swmF.setNodes(nodes);
    			}
    			
    			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, swmF, "swmF").commit();
    			if (isListSharedWithMe){
    				customListGrid.setImageResource(R.drawable.ic_menu_gridview);
				}
				else{
    				customListGrid.setImageResource(R.drawable.ic_menu_listview);
    			}
    			
    			customListGrid.setVisibility(View.VISIBLE);
    			customSearch.setVisibility(View.VISIBLE);
    			mDrawerLayout.closeDrawer(Gravity.LEFT);
    			
    			if (createFolderMenuItem != null){
    				createFolderMenuItem.setVisible(false);
    				rubbishBinMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(true);
	    			sortByMenuItem.setVisible(true);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(true);
	    			settingsMenuItem.setVisible(true);
	    			logoutMenuItem.setVisible(true);
	    			
	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			rubbishBinMenuItem.setEnabled(false);
	    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			createFolderMenuItem.setEnabled(false);
	    		}
    			
    			break;
    		}
    		case ACCOUNT:{
    			
    			if (maF == null){
    				maF = new MyAccountFragment();
    			}
    			
    			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, maF, "maF").commit();
    			customListGrid.setVisibility(View.GONE);
    			customSearch.setVisibility(View.GONE);
    			
    			mDrawerLayout.closeDrawer(Gravity.LEFT);
    			
    			if (createFolderMenuItem != null){
    				createFolderMenuItem.setVisible(false);
    				rubbishBinMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(true);
	    			settingsMenuItem.setVisible(true);
	    			logoutMenuItem.setVisible(true);
	    			
	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			rubbishBinMenuItem.setEnabled(false);
	    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			createFolderMenuItem.setEnabled(false);
	    		}
    			
    			break;
    		}
    		case TRANSFERS:{
    			
    			if (tF == null){
    				tF = new TransfersFragment();
    			}
    			tF.setTransfers(megaApi.getTransfers());
    			tF.setPause(!downloadPlay);
    			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, tF).commit();
    			customListGrid.setVisibility(View.GONE);
    			customSearch.setVisibility(View.GONE);

    			mDrawerLayout.closeDrawer(Gravity.LEFT);
    			
    			if (createFolderMenuItem != null){
    				createFolderMenuItem.setVisible(true);
    				rubbishBinMenuItem.setVisible(false);
	    			addMenuItem.setVisible(true);
	    			refreshMenuItem.setVisible(true);
	    			sortByMenuItem.setVisible(true);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(true);
	    			settingsMenuItem.setVisible(true);
	    			logoutMenuItem.setVisible(true);
	    			
	    			if (downloadPlay){
	    				addMenuItem.setIcon(R.drawable.ic_menu_pause);
	    			}
	    			else{
	    				addMenuItem.setIcon(R.drawable.ic_menu_play);
	    			}
	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			rubbishBinMenuItem.setEnabled(false);
	    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			createFolderMenuItem.setEnabled(false);
	    			
	    			if (megaApi.getTransfers().size() == 0){
	    				createFolderMenuItem.setVisible(false);
	    				rubbishBinMenuItem.setVisible(false);
	    				addMenuItem.setVisible(false);
	    				downloadPlay = true;
	    			}
    			}

//    			if (createFolderMenuItem != null){
//    				createFolderMenuItem.setVisible(false);
//    				rubbishBinMenuItem.setVisible(false);
//	    			addMenuItem.setVisible(false);
//	    			refreshMenuItem.setVisible(false);
//	    			sortByMenuItem.setVisible(false);
//	    			helpMenuItem.setVisible(true);
//	    			upgradeAccountMenuItem.setVisible(true);
//	    			logoutMenuItem.setVisible(true);
//	    			
//	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
//	    			rubbishBinMenuItem.setEnabled(false);
//	    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
//	    			addMenuItem.setEnabled(false);
//	    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
//	    			createFolderMenuItem.setEnabled(false);
//	    		}
    			
    			break;
    		}
    		case SAVED_FOR_OFFLINE:{
    			if (oF == null){
    				oF = new OfflineFragment();
    				oF.setIsList(isListOffline);
    				oF.setPathNavigation("/");
    			}
    			else{
    				oF.setPathNavigation("/");
    				oF.setIsList(isListOffline);
    			}
    			
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, oF, "oF").commit();
				if (isListCloudDrive){					
					customListGrid.setImageResource(R.drawable.ic_menu_gridview);
				}
				else{
    				customListGrid.setImageResource(R.drawable.ic_menu_listview);
    			}
    			    			
    			mDrawerLayout.closeDrawer(Gravity.LEFT);
   			
    			customListGrid.setVisibility(View.VISIBLE);
    			customSearch.setVisibility(View.VISIBLE);
    			

    			if (createFolderMenuItem != null){
	    			createFolderMenuItem.setVisible(false);
	    			rubbishBinMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(true);
	    			settingsMenuItem.setVisible(true);
	    			logoutMenuItem.setVisible(true);
	    			
	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			rubbishBinMenuItem.setEnabled(false);
	    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			createFolderMenuItem.setEnabled(false);
    			}
    			
    			break;
    		}
    		case SEARCH:{
    			if (sF == null){
        			sF = new SearchFragment();
        		}
    			
    			searchNodes = megaApi.search(megaApi.getRootNode(), searchQuery, true);
    			
    			sF.setSearchNodes(searchNodes);
    			sF.setNodes(searchNodes);
    			sF.setSearchQuery(searchQuery);
    			sF.setParentHandle(parentHandleSearch);
    			sF.setLevels(levelsSearch);
    			
    			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, sF, "sF").commit();
    			customListGrid.setVisibility(View.GONE);
    			customSearch.setVisibility(View.GONE);
    			

    			if (createFolderMenuItem != null){
        			createFolderMenuItem.setVisible(false);
        			rubbishBinMenuItem.setVisible(false);
        			addMenuItem.setVisible(false);
        			refreshMenuItem.setVisible(false);
        			sortByMenuItem.setVisible(false);
        			helpMenuItem.setVisible(true);
        			upgradeAccountMenuItem.setVisible(true);
        			settingsMenuItem.setVisible(true);
        			logoutMenuItem.setVisible(true);
        			
        			createFolderMenuItem.setIcon(R.drawable.ic_menu_new_folder_dark);
        			rubbishBinMenuItem.setIcon(R.drawable.ic_menu_rubbish);
        			rubbishBinMenuItem.setEnabled(true);
        			addMenuItem.setIcon(R.drawable.ic_menu_add);
        			addMenuItem.setEnabled(true);
    			}
    			break;
    		}
    		case PHOTO_SYNC:{
   			
    			if (psF == null){
    				psF = new PhotoSyncFragment();
    				psF.setIsList(isListPhotoSync);
				}
				else{
					psF.setIsList(isListPhotoSync);
				}
				
				
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, psF, "psF").commit();
				if (isListPhotoSync){					
					customListGrid.setImageResource(R.drawable.ic_menu_gridview);
				}
				else{
    				customListGrid.setImageResource(R.drawable.ic_menu_listview);
    			}
    			    			
   				mDrawerLayout.closeDrawer(Gravity.LEFT);
    			
    			customListGrid.setVisibility(View.VISIBLE);
    			customSearch.setVisibility(View.VISIBLE);
    			
    			if (createFolderMenuItem != null){
	    			createFolderMenuItem.setVisible(false);
	    			rubbishBinMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(true);
	    			settingsMenuItem.setVisible(true);
	    			logoutMenuItem.setVisible(true);
	    			
	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			rubbishBinMenuItem.setEnabled(false);
	    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			createFolderMenuItem.setEnabled(false);
    			}
      			break;
    		}
			default:{
				break;
			}
    	}
    }
	
	@Override
	public void onBackPressed() {
		log("onBackPressed");
		try { 
			statusDialog.dismiss();	
		} 
		catch (Exception ex) {}
		
		if (fbF != null){
			if (fbF.isVisible()){
				if (fbF.onBackPressed() == 0){
					super.onBackPressed();
					return;
				}
			}
		}
		
		if (cF != null){
			if (cF.isVisible()){
				if (cF.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItem(drawerItem);
					if(nDA!=null){
						nDA.setPositionClicked(0);
					}
					return;
				}
			}
		}
		
		if (rbF != null){
			if (rbF.isVisible()){
				if (rbF.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItem(drawerItem);
					if(nDA!=null){
						nDA.setPositionClicked(0);
					}
					return;
				}
			}
		}
		
		if (swmF != null){
			if (swmF.isVisible()){
				if (swmF.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItem(drawerItem);
					if(nDA!=null){
						nDA.setPositionClicked(0);
					}
					return;
				}
			}
		}
		
		if (tF != null){
			if (tF.isVisible()){
				if (tF.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItem(drawerItem);
					if(nDA!=null){
						nDA.setPositionClicked(0);
					}
					return;
				}
			}
		}
		
		if (maF != null){
			if (maF.isVisible()){
				if (maF.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItem(drawerItem);
					if(nDA!=null){
						nDA.setPositionClicked(0);
					}
					return;
				}
			}
		}
		
		if (oF != null){
			if (oF.isVisible()){
				if (oF.onBackPressed() == 0){
					attr = dbH.getAttributes();
					if (attr != null){
						if (attr.getOnline() != null){
							if (!Boolean.parseBoolean(attr.getOnline())){
								super.onBackPressed();
								return;
							}
						}
					}
					
					if (fbF != null){
						drawerItem = DrawerItem.CLOUD_DRIVE;
						selectDrawerItem(drawerItem);
						if(nDA!=null){
							nDA.setPositionClicked(0);
						}
					}
					else{
						super.onBackPressed();
					}
					return;
				}
			}
		}
		
		if (sF != null){
			if (sF.isVisible()){
				if (sF.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItem(drawerItem);
					if(nDA!=null){
						nDA.setPositionClicked(0);
					}
					return;
				}
			}
		}
		
		if (psF != null){
			if (psF.isVisible()){
				if (psF.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItem(drawerItem);
					if(nDA!=null){
						nDA.setPositionClicked(0);
					}
					return;
				}
			}
		}
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState){
		log("onPostCreate");
		super.onPostCreate(savedInstanceState);
		if (!openLink){
			mDrawerToggle.syncState();
		}
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenu");
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_manager, menu);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
	    
	    final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchMenuItem = menu.findItem(R.id.action_search);
		searchMenuItem.setVisible(false);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		
		if (searchView != null){
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setIconifiedByDefault(true);
		}
		
		rubbishBinMenuItem = menu.findItem(R.id.action_rubbish_bin);
		addMenuItem = menu.findItem(R.id.action_add);
		createFolderMenuItem = menu.findItem(R.id.action_new_folder);
		
		refreshMenuItem = menu.findItem(R.id.action_menu_refresh);
		sortByMenuItem = menu.findItem(R.id.action_menu_sort_by);
		helpMenuItem = menu.findItem(R.id.action_menu_help);
		upgradeAccountMenuItem = menu.findItem(R.id.action_menu_upgrade_account);
		settingsMenuItem = menu.findItem(R.id.action_menu_settings);
		logoutMenuItem = menu.findItem(R.id.action_menu_logout);
		
		if (fbF != null){
			if (fbF.isVisible()){
    			createFolderMenuItem.setVisible(true);
    			rubbishBinMenuItem.setVisible(true);
    			addMenuItem.setVisible(true);
    			refreshMenuItem.setVisible(true);
    			sortByMenuItem.setVisible(true);
    			helpMenuItem.setVisible(true);
    			upgradeAccountMenuItem.setVisible(true);
    			settingsMenuItem.setVisible(true);
    			logoutMenuItem.setVisible(true);   			
    			
    			createFolderMenuItem.setIcon(R.drawable.ic_menu_new_folder_dark);
    			rubbishBinMenuItem.setIcon(R.drawable.ic_menu_rubbish);
    			rubbishBinMenuItem.setEnabled(true);
    			addMenuItem.setIcon(R.drawable.ic_menu_add);
    			addMenuItem.setEnabled(true);
			}
		}
		
		if (cF != null){
			if (cF.isVisible()){
				createFolderMenuItem.setVisible(true);
				rubbishBinMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(true);
    			sortByMenuItem.setVisible(true);
    			helpMenuItem.setVisible(true);
    			upgradeAccountMenuItem.setVisible(true);
    			settingsMenuItem.setVisible(true);
    			logoutMenuItem.setVisible(true);
    			
    			createFolderMenuItem.setIcon(R.drawable.ic_action_social_add_person);
    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			rubbishBinMenuItem.setEnabled(false);
    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			addMenuItem.setEnabled(false);
			}
		}
		
		if (rbF != null){
			if (rbF.isVisible()){
				createFolderMenuItem.setVisible(true);
				rubbishBinMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(true);
    			sortByMenuItem.setVisible(true);
    			helpMenuItem.setVisible(true);
    			upgradeAccountMenuItem.setVisible(true);
    			settingsMenuItem.setVisible(true);
    			logoutMenuItem.setVisible(true);
    			
    			createFolderMenuItem.setIcon(R.drawable.ic_menu_discard_dark);
    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			rubbishBinMenuItem.setEnabled(false);
    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			addMenuItem.setEnabled(false);
			}
		}
		
		if (swmF != null){
			if (swmF.isVisible()){
				createFolderMenuItem.setVisible(false);
				rubbishBinMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(true);
    			sortByMenuItem.setVisible(true);
    			helpMenuItem.setVisible(true);
    			upgradeAccountMenuItem.setVisible(true);
    			settingsMenuItem.setVisible(true);
    			logoutMenuItem.setVisible(true);
    			
    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			rubbishBinMenuItem.setEnabled(false);
    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			addMenuItem.setEnabled(false);
    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			createFolderMenuItem.setEnabled(false);
    		}
		}
		
		if (maF != null){
			if (maF.isVisible()){
				createFolderMenuItem.setVisible(false);
				rubbishBinMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(false);
    			sortByMenuItem.setVisible(false);
    			helpMenuItem.setVisible(true);
    			upgradeAccountMenuItem.setVisible(true);
    			settingsMenuItem.setVisible(true);
    			logoutMenuItem.setVisible(true);
    			
    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			rubbishBinMenuItem.setEnabled(false);
    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			addMenuItem.setEnabled(false);
    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			createFolderMenuItem.setEnabled(false);
			}
		}
		
		if (tF != null){
			if (tF.isVisible()){
				createFolderMenuItem.setVisible(true);
				rubbishBinMenuItem.setVisible(false);
    			addMenuItem.setVisible(true);
    			refreshMenuItem.setVisible(true);
    			sortByMenuItem.setVisible(true);
    			helpMenuItem.setVisible(true);
    			upgradeAccountMenuItem.setVisible(true);
    			settingsMenuItem.setVisible(true);
    			logoutMenuItem.setVisible(true);
    			
    			if (downloadPlay){
    				addMenuItem.setIcon(R.drawable.ic_menu_pause);
    			}
    			else{
    				addMenuItem.setIcon(R.drawable.ic_menu_play);
    			}
    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			rubbishBinMenuItem.setEnabled(false);
    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			createFolderMenuItem.setEnabled(false);
    			
    			if (megaApi.getTransfers().size() == 0){
    				createFolderMenuItem.setVisible(false);
    				rubbishBinMenuItem.setVisible(false);
    				addMenuItem.setVisible(false);
    				downloadPlay = true;
    			}
			}
		}
		
		if (oF != null){
			if (oF.isVisible()){
				createFolderMenuItem.setVisible(false);
				rubbishBinMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(false);
    			sortByMenuItem.setVisible(false);
    			helpMenuItem.setVisible(true);
    			upgradeAccountMenuItem.setVisible(true);
    			settingsMenuItem.setVisible(true);
    			logoutMenuItem.setVisible(true);
    			
    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			rubbishBinMenuItem.setEnabled(false);
    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			addMenuItem.setEnabled(false);
    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			createFolderMenuItem.setEnabled(false);
			}
		}
		
		if (sF != null){
			if (sF.isVisible()){
				if (createFolderMenuItem != null){
	    			createFolderMenuItem.setVisible(false);
	    			rubbishBinMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(true);
	    			settingsMenuItem.setVisible(true);
	    			logoutMenuItem.setVisible(true);
	    			
	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			rubbishBinMenuItem.setEnabled(false);
	    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			createFolderMenuItem.setEnabled(false);
				}
			}
		}
		
		if (psF != null){
			if (psF.isVisible()){
				createFolderMenuItem.setVisible(false);
				rubbishBinMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(false);
    			sortByMenuItem.setVisible(false);
    			helpMenuItem.setVisible(true);
    			upgradeAccountMenuItem.setVisible(true);
    			settingsMenuItem.setVisible(true);
    			logoutMenuItem.setVisible(true);
    			
    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			rubbishBinMenuItem.setEnabled(false);
    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			addMenuItem.setEnabled(false);
    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			createFolderMenuItem.setEnabled(false);
			}
		}
	    	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");
		// Handle presses on the action bar items
	    switch (item.getItemId()) {
		    case android.R.id.home:{
//		    case R.id.home:
//		    case R.id.homeAsUp:
	    	//case 16908332: //Algo pasa con la CyanogenMod
		    	if (mDrawerToggle.isDrawerIndicatorEnabled()) {
					mDrawerToggle.onOptionsItemSelected(item);
				}
		    	else {
		    		if (fbF != null){
		    			if (fbF.isVisible()){
		    				fbF.onBackPressed();
		    				return true;
		    			}
		    		}
		    		if (rbF != null){
		    			if (rbF.isVisible()){
		    				rbF.onBackPressed();
		    				return true;
		    			}
		    		}
		    		if (swmF != null){
		    			if (swmF.isVisible()){
		    				swmF.onBackPressed();
		    		    	return true;
		    			}
		    		}
		    		if (sF != null){
		    			if (sF.isVisible()){
		    				sF.onBackPressed();
		    				return true;
		    			}
		    		}
				}
		    	return true;
		    }
	        case R.id.action_search:{
	        	mSearchView.setIconified(false);
	        	return true;
	        }
	        case R.id.action_new_folder:{
	        	if (drawerItem == DrawerItem.CLOUD_DRIVE){
	        		showNewFolderDialog(null);
	        	}
	        	else if (drawerItem == DrawerItem.CONTACTS){
	        		showNewContactDialog(null);
	        	}
	        	
	        	else if (drawerItem == DrawerItem.RUBBISH_BIN){
	        		showClearRubbishBinDialog(null);
	        	}
	        	return true;
	        }
	        case R.id.action_add:{
	        	if (drawerItem == DrawerItem.TRANSFERS){	    			
	    			if (downloadPlay){
	    				downloadPlay = false;
	    			}
	    			else{
	    				downloadPlay = true;
	    			}
	    			megaApi.pauseTransfers(!downloadPlay, this);
	        	}
	        	else{
		        	uploadDialog = new UploadHereDialog();
					uploadDialog.show(getSupportFragmentManager(), "fragment_upload");
	        	}
	        	return true;     	
	        }
	        case R.id.action_rubbish_bin:{
	        	drawerItem = DrawerItem.RUBBISH_BIN;
				selectDrawerItem(drawerItem);
				return true;
	        }
	        case R.id.action_menu_refresh:{
	        	switch(drawerItem){
		        	case CLOUD_DRIVE:{
		        		Intent intent = new Intent(managerActivity, LoginActivity.class);
			    		intent.setAction(LoginActivity.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
		        		break;
		        	}
		        	case CONTACTS:{
		        		Intent intent = new Intent(managerActivity, LoginActivity.class);
			    		intent.setAction(LoginActivity.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
			    		break;
		        	}
		        	case RUBBISH_BIN:{
		        		Intent intent = new Intent(managerActivity, LoginActivity.class);
			    		intent.setAction(LoginActivity.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleRubbish);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
			    		break;
		        	}
		        	case SHARED_WITH_ME:{
		        		Intent intent = new Intent(managerActivity, LoginActivity.class);
			    		intent.setAction(LoginActivity.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleSharedWithMe);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
			    		break;
		        	}
	        	}
	        	return true;
	        }
	        case R.id.action_menu_sort_by:{
	        	switch(drawerItem){
		        	case CONTACTS:{
		        		Toast.makeText(managerActivity, "Sort by (in contacts) not yet implemented (it's implemented on CLOUD_DRIVE)", Toast.LENGTH_LONG).show();
			    		break;
		        	}
		        	default:{
		        		Intent intent = new Intent(managerActivity, SortByDialogActivity.class);
			    		intent.setAction(SortByDialogActivity.ACTION_SORT_BY);
			    		startActivityForResult(intent, REQUEST_CODE_SORT_BY);
			    		break;
		        	}
	        	}
	        	return true;
	        }
	        case R.id.action_menu_help:{
	        	Toast.makeText(managerActivity, "Help not yet implemented (refresh, sort by and logout are implemented)", Toast.LENGTH_SHORT).show();
	    		return true;
	    	}
	        case R.id.action_menu_upgrade_account:{
	        	Intent intent = new Intent(managerActivity, UpgradeActivity.class);
				startActivity(intent);
				return true;
	        }
	        case R.id.action_menu_settings:{
//				if (Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB) {
				    startActivity(new Intent(this, SettingsActivity.class));
//				}
//				else {
//					startActivity(new Intent(this, SettingsActivityHC.class));
//				}
	        	return true;
	        }
	        case R.id.action_menu_logout:{
	        	logout(managerActivity, (MegaApplication)getApplication(), megaApi, false);
	        	return true;
	        }
            default:{
	            return super.onOptionsItemSelected(item);
            }
	    }
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		log("onItemClick");
		if (nDA != null){
			nDA.setPositionClicked(position);
		}
		
		if (position >= 3){
			position++;
		}
		drawerItem = DrawerItem.values()[position];
		selectDrawerItem(drawerItem);
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		switch(v.getId()){
			case R.id.menu_action_bar_grid:{
				
				if (fbF != null){
					if (fbF.isVisible()){
						Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("fbF");
						FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.detach(currentFragment);
						fragTransaction.commit();
						
						isListCloudDrive = !isListCloudDrive;
						fbF.setIsList(isListCloudDrive);
						fbF.setParentHandle(parentHandleBrowser);
						
						fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.attach(currentFragment);
						fragTransaction.commit();
						
						if (isListCloudDrive){
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_gridview);
						}
						else{
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_listview);
						}
					}
				}
				
				if (cF != null){
					if (cF.isVisible()){
						Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("cF");
						FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.detach(currentFragment);
						fragTransaction.commit();
						
						isListContacts = !isListContacts;
						cF.setIsList(isListContacts);
						
						fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.attach(currentFragment);
						fragTransaction.commit();
						
						if (isListContacts){
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_gridview);
						}
						else{
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_listview);
						}
					}
				}
				
				if (rbF != null){
					if (rbF.isVisible()){
						Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("rbF");
						FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.detach(currentFragment);
						fragTransaction.commit();
						
						isListRubbishBin = !isListRubbishBin;
						rbF.setIsList(isListRubbishBin);
						rbF.setParentHandle(parentHandleRubbish);
						
						fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.attach(currentFragment);
						fragTransaction.commit();
						
						if (isListRubbishBin){
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_gridview);
						}
						else{
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_listview);
						}
					}
				}
				
				if (swmF != null){
					if (swmF.isVisible()){
						Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("swmF");
						FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.detach(currentFragment);
						fragTransaction.commit();
						
						isListSharedWithMe = !isListSharedWithMe;
						swmF.setIsList(isListSharedWithMe);
						swmF.setParentHandle(parentHandleSharedWithMe);
						
						fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.attach(currentFragment);
						fragTransaction.commit();
						
						if (isListSharedWithMe){
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_gridview);
						}
						else{
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_listview);
						}
					}
				}
				
				if (oF != null){
					if (oF.isVisible()){
						Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("oF");
						FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.detach(currentFragment);
						fragTransaction.commit();
						
						isListOffline = !isListOffline;
						oF.setIsList(isListOffline);						
						oF.setPathNavigation(pathNavigation);
						//oF.setGridNavigation(false);
						//oF.setParentHandle(parentHandleSharedWithMe);
						
						fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.attach(currentFragment);
						fragTransaction.commit();
						
						if (isListOffline){
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_gridview);
						}
						else{
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_listview);
						}
					}
				}
				
				if (psF != null){
					if (psF.isVisible()){
						Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("psF");
						FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.detach(currentFragment);
						fragTransaction.commit();
						
						isListPhotoSync = !isListPhotoSync;
						psF.setIsList(isListPhotoSync);
						
						fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.attach(currentFragment);
						fragTransaction.commit();
						
						if (isListPhotoSync){
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_gridview);
						}
						else{
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_listview);
						}
					}
				}
				break;
			}
			case R.id.custom_search:{
				if (searchMenuItem != null) {
					MenuItemCompat.expandActionView(searchMenuItem);
				}
				else{
					Toast.makeText(this, "searchMenuItem == null", Toast.LENGTH_LONG).show();
				}
				break;
			}
			case R.id.top_control_bar:{
				drawerItem = DrawerItem.ACCOUNT;
				selectDrawerItem(drawerItem);
				break;
			}
			case R.id.bottom_control_bar:{
				drawerItem = DrawerItem.ACCOUNT;
				selectDrawerItem(drawerItem);
				break;
			}
		}
	}
	
	 /*
	 * Logout user
	 */
	static public void logout(Context context, MegaApplication app, MegaApiAndroid megaApi, boolean confirmAccount) {
//		context.stopService(new Intent(context, BackgroundService.class));
		log("logout");
//		context.stopService(new Intent(context, CameraSyncService.class));
		
		File offlineDirectory = null;
		if (Environment.getExternalStorageDirectory() != null){
			offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);
		}
//		if (context.getExternalFilesDir(null) != null){
//			offlineDirectory = context.getExternalFilesDir(null);
//		}
		else{
			offlineDirectory = context.getFilesDir();
		}
		
		try {
			Util.deleteFolderAndSubfolders(context, offlineDirectory);
		} catch (IOException e) {}

//		DatabaseHandler dbH = new DatabaseHandler(context);
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
		dbH.clearCredentials();
		if (dbH.getPreferences() != null){
			dbH.clearPreferences();
			dbH.setFirstTime(false);
//			dbH.setPinLockEnabled(false);
//			dbH.setPinLockCode("");
//			dbH.setCamSyncEnabled(false);
//			dbH.setStorageAskAlways(true);
			Intent stopIntent = null;
			stopIntent = new Intent(context, CameraSyncService.class);
			stopIntent.setAction(CameraSyncService.ACTION_LOGOUT);
			context.startService(stopIntent);
		}
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		megaApi.logout();
		drawerItem = null;
		
		if (!confirmAccount){		
			if(managerActivity != null)	{
				Intent intent = new Intent(managerActivity, TourActivity.class);
		        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				managerActivity.startActivity(intent);
				managerActivity.finish();
				managerActivity = null;
			}
			else{
				Intent intent = new Intent (context, TourActivity.class);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				context.startActivity(intent);
				((Activity)context).finish();
				context = null;
			}
		}
		else{
			if (managerActivity != null){
				managerActivity.finish();
			}
			else{
				((Activity)context).finish();
			}
		}
	}	
	

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart");
		if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
			log("account_details request start");
		}
		else if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout request start");
		}	
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fecthnodes request start");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			log("move request start");
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			log("remove request start");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			log("export request start");
		}
		else if(request.getType() == MegaRequest.TYPE_RENAME){
			log("rename request start");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("copy request start");
		}
		else if (request.getType() == MegaRequest.TYPE_MKDIR){
			log("create folder start");
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			log("pause transfers start");
		}
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
			log ("account_details request");
			if (e.getErrorCode() == MegaError.API_OK){
				
				MegaAccountDetails accountInfo = request.getMegaAccountDetails();
				
				long totalStorage = accountInfo.getStorageMax();
				long usedStorage = accountInfo.getStorageUsed();
				
				totalStorage = ((totalStorage / 1024) / 1024) / 1024;
				String total = "";
				if (totalStorage >= 1024){
					totalStorage = totalStorage / 1024;
					total = total + totalStorage + " TB";
				}
				else{
					 total = total + totalStorage + " GB";
				}

				usedStorage = ((usedStorage / 1024) / 1024) / 1024;
				String used = "";
				if (usedStorage >= 1024){
					usedStorage = usedStorage / 1024;
					used = used + usedStorage + " TB";
				}
				else{
					used = used + usedStorage + " GB";
				}
				
		        String usedSpaceString = getString(R.string.used_space, used, total);
		        usedSpace.setText(usedSpaceString);
		        Spannable wordtoSpan = new SpannableString(usedSpaceString);
		        
		        bottomControlBar.setVisibility(View.VISIBLE);
		        int usedPerc = 0;
		        if (totalStorage != 0){
		        	usedPerc = (int)((100 * usedStorage) / totalStorage);
		        }
		        if (usedPerc < 90){
		        	usedSpaceBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_ok));
		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_ok)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        }
		        else if ((usedPerc >= 90) && (usedPerc <= 95)){
		        	usedSpaceBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_warning));
		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_warning)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        }
		        else{
		        	if (usedPerc > 100){
			        	usedPerc = 100;
			        }
		        	usedSpaceBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_exceed));    
		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_exceed)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        }
		        usedSpaceBar.setProgress(usedPerc);
		        
		                
				
		        
		        wordtoSpan.setSpan(new RelativeSizeSpan(1.5f), 0, used.length() - 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.navigation_drawer_mail)), used.length() + 1, used.length() + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        wordtoSpan.setSpan(new RelativeSizeSpan(1.5f), used.length() + 4, used.length() + 4 + total.length() - 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        usedSpace.setText(wordtoSpan);
		        
			}
		}
		else if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout finished");
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fecthnodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, "Correctly moved to Rubbish bin", Toast.LENGTH_SHORT).show();
					if (fbF != null){
						if (fbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
							fbF.setNodes(nodes);
							fbF.getListView().invalidateViews();
						}
					}
					if (rbF != null){
						if (rbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbF.getParentHandle()), orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
						}
					}
					
					if (swmF != null){
						if (swmF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(swmF.getParentHandle()), orderGetChildren);
							swmF.setNodes(nodes);
							swmF.getListView().invalidateViews();
						}
					}
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
					if (fbF != null){
						if (fbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
							fbF.setNodes(nodes);
							fbF.getListView().invalidateViews();
						}
					}
					if (rbF != null){
						if (rbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbF.getParentHandle()), orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
						}
					}
					if (swmF != null){
						if (swmF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(swmF.getParentHandle()), orderGetChildren);
							swmF.setNodes(nodes);
							swmF.getListView().invalidateViews();
						}
					}
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
				
				if (fbF != null){
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
					}
				}
				if (rbF != null){
					if (rbF.isVisible()){
						if (isClearRubbishBin){
							isClearRubbishBin = false;
							parentHandleRubbish = megaApi.getRubbishNode().getHandle();
							rbF.setParentHandle(megaApi.getRubbishNode().getHandle());
							NodeList nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
							aB.setTitle(getString(R.string.section_rubbish_bin));	
							getmDrawerToggle().setDrawerIndicatorEnabled(true);
						}
						else{
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbF.getParentHandle()), orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
						}
					}
				}
				if (swmF != null){
					if (swmF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(swmF.getParentHandle()), orderGetChildren);
						swmF.setNodes(nodes);
						swmF.getListView().invalidateViews();
					}
				}
			}
			else{
				Toast.makeText(this, "The file has not been removed", Toast.LENGTH_LONG).show();
			}
			log("remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				String link = request.getLink();
				if (managerActivity != null){
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_TEXT, link);
					startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
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
				if (fbF != null){
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
					}
				}
				if (rbF != null){
					if (rbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbF.getParentHandle()), orderGetChildren);
						rbF.setNodes(nodes);
						rbF.getListView().invalidateViews();
					}
				}
				if (swmF != null){
					if (swmF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(swmF.getParentHandle()), orderGetChildren);
						swmF.setNodes(nodes);
						swmF.getListView().invalidateViews();
					}
				}
			}
			else{
				Toast.makeText(this, "The file has not been renamed", Toast.LENGTH_LONG).show();
			}
		} 
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Correctly copied", Toast.LENGTH_SHORT).show();
				if (fbF != null){
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
					}
				}
				if (rbF != null){
					if (rbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbF.getParentHandle()), orderGetChildren);
						rbF.setNodes(nodes);
						rbF.getListView().invalidateViews();
					}
				}
				if (swmF != null){
					if (swmF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(swmF.getParentHandle()), orderGetChildren);
						swmF.setNodes(nodes);
						swmF.getListView().invalidateViews();
					}
				}
			}
			else{
				Toast.makeText(this, "The file has not been copied", Toast.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_MKDIR){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Folder created", Toast.LENGTH_LONG).show();
				if (fbF != null){
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			if (e.getErrorCode() == MegaError.API_OK){
				
				File avatar = null;
				if (getExternalCacheDir() != null){
					avatar = new File(getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				}
				else{
					avatar = new File(getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				}
				Bitmap imBitmap = null;
				if (avatar.exists()){
					if (avatar.length() > 0){
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bOpts.inPurgeable = true;
						bOpts.inInputShareable = true;
						imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
						if (imBitmap == null) {
							avatar.delete();
						}
						else{
							Bitmap circleBitmap = Bitmap.createBitmap(imBitmap.getWidth(), imBitmap.getHeight(), Bitmap.Config.ARGB_8888);
							
							BitmapShader shader = new BitmapShader (imBitmap,  TileMode.CLAMP, TileMode.CLAMP);
					        Paint paint = new Paint();
					        paint.setShader(shader);
					
					        Canvas c = new Canvas(circleBitmap);
					        int radius; 
					        if (imBitmap.getWidth() < imBitmap.getHeight())
					        	radius = imBitmap.getWidth()/2;
					        else
					        	radius = imBitmap.getHeight()/2;
					        
						    c.drawCircle(imBitmap.getWidth()/2, imBitmap.getHeight()/2, radius, paint);
					        imageProfile.setImageBitmap(circleBitmap);
						}
					}
				}
			}
			log("avatar user downloaded");
		}
		else if (request.getType() == MegaRequest.TYPE_ADD_CONTACT){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Contact added", Toast.LENGTH_LONG).show();
				if (cF.isVisible()){	
					UserList contacts = megaApi.getContacts();
					cF.setContacts(contacts);
					cF.getListView().invalidateViews();
				}
			}
			log("add contact");
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			if (e.getErrorCode() == MegaError.API_OK) {
				if (tF != null){
					if (tF.isVisible()){
						if (!downloadPlay){
							addMenuItem.setIcon(R.drawable.ic_menu_play);
							tF.setPause(true);
						}
						else{
							addMenuItem.setIcon(R.drawable.ic_menu_pause);
							tF.setPause(false);
						}		
					}
				}				
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFER){
			if (e.getErrorCode() == MegaError.API_OK){
				if (tF != null){
					if (tF.isVisible()){
						Intent cancelOneIntent = new Intent(this, DownloadService.class);
						cancelOneIntent.setAction(DownloadService.ACTION_CANCEL_ONE_DOWNLOAD);				
						startService(cancelOneIntent);
						tF.setTransfers(megaApi.getTransfers());
					}
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,MegaError e) {
		log("onRequestTemporaryError");
		if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout temporary error");
		}	
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fetchnodes temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			log("move temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			log("remove temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			log("export temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			log("rename temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("copy temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_MKDIR){
			log("create folder temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			log("get user attribute temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_ADD_CONTACT){
			log("add contact temporary error");
		}
	}
	
	public ActionBarDrawerToggle getmDrawerToggle() {
		log("getmDrawerToggle");
		return mDrawerToggle;
	}

	public void setmDrawerToggle(ActionBarDrawerToggle mDrawerToggle) {
		log("setmDrawerToggle");
		this.mDrawerToggle = mDrawerToggle;
	}
	
	File destination;
	
	public void onFileClick(ArrayList<Long> handleList){
		log("onFileClick");
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
		String downloadLocationDefaultPath = Util.downloadDIR;
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
			File defaultPathF = new File(downloadLocationDefaultPath);
			defaultPathF.mkdirs();
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}		
	}
	
	public void moveToTrash(ArrayList<Long> handleList){
		log("moveToTrash");
		isClearRubbishBin = false;
		
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		MegaNode rubbishNode = megaApi.getRubbishNode();

		for (int i=0;i<handleList.size();i++){
			//Check if the node is not yet in the rubbish bin (if so, remove it)
			MegaNode parent = megaApi.getNodeByHandle(handleList.get(i));
			while (megaApi.getParentNode(parent) != null){
				parent = megaApi.getParentNode(parent);
			}
				
			if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
				moveToRubbish = true;
				megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)), rubbishNode, this);
			}
			else{
				megaApi.remove(megaApi.getNodeByHandle(handleList.get(i)), this);
			}
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
	
	public void getPublicLinkAndShareIt(MegaNode document){
		log("getPublicLinkAndShareIt");
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
		
		megaApi.exportNode(document, this);
	}
	
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		log("showKeyboardDelayed");
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (fbF != null){
					if (!fbF.isVisible()){
						return;
					}
				}
				if (rbF != null){
					if (rbF.isVisible()){
						return;
					}
				}
				if (cF != null){
					if (cF.isVisible()){
						return;
					}
				}
				if (swmF != null){
					if (swmF.isVisible()){
						return;
					}
				}
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	public void showClearRubbishBinDialog(String editText){
		log("showClearRubbishBinDialog");
		if (rbF.isVisible()){
			rbF.setPositionClicked(-1);
			rbF.notifyDataSetChanged();
		}
		
		String text;
		if ((editText == null) || editText.equals("")){
			text = getString(R.string.context_clear_rubbish);
		}
		else{
			text = editText;
		}
		
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_clear_rubbish), null, null);
		builder.setPositiveButton(getString(R.string.general_empty),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						clearRubbishBin();
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		clearRubbishBinDialog = builder.create();
		clearRubbishBinDialog.show();
	}
	
	public void showNewContactDialog(String editText){
		log("showNewContactDialog");
		if (cF.isVisible()){
			cF.setPositionClicked(-1);
			cF.notifyDataSetChanged();
		}
		
		String text;
		if ((editText == null) || editText.equals("")){
			text = getString(R.string.context_new_contact_name);
		}
		else{
			text = editText;
		}
		
		final EditText input = new EditText(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					addContact(value);
					addContactDialog.dismiss();
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_add),
				KeyEvent.KEYCODE_ENTER);
		input.setText(text);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.menu_add_contact),
				null, input);
		builder.setPositiveButton(getString(R.string.general_add),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						addContact(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		addContactDialog = builder.create();
		addContactDialog.show();
	}
	
	public void showNewFolderDialog(String editText){
		log("showNewFolderDialog");
		if (fbF.isVisible()){
			fbF.setPositionClicked(-1);
			fbF.notifyDataSetChanged();
		}
		
		String text;
		if (editText == null || editText.equals("")){
			text = getString(R.string.context_new_folder_name);
		}
		else{
			text = editText;
		}
		
		final EditText input = new EditText(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
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
		input.setImeActionLabel(getString(R.string.general_create),
				KeyEvent.KEYCODE_ENTER);
		input.setText(text);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.menu_new_folder),
				null, input);
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
		newFolderDialog = builder.create();
		newFolderDialog.show();
	}
	
	private void clearRubbishBin(){
		log("clearRubbishBin");
		if (rbF != null){
			NodeList rubbishNodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
			
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
			
			isClearRubbishBin = true;
			for (int i=0; i<rubbishNodes.size(); i++){
				megaApi.remove(rubbishNodes.get(i), this);
			}
		}
	}
	
	private void addContact(String contactEmail){
		log("addContact");
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		statusDialog = null;
		try {
			statusDialog = new ProgressDialog(this);
			statusDialog.setMessage(getString(R.string.context_adding_contact));
			statusDialog.show();
		}
		catch(Exception e){
			return;
		}
		
		megaApi.addContact(contactEmail, this);
	}
	
	private void createFolder(String title) {
		log("createFolder");
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		statusDialog = null;
		try {
			statusDialog = new ProgressDialog(this);
			statusDialog.setMessage(getString(R.string.context_creating_folder));
			statusDialog.show();
		}
		catch(Exception e){
			return;
		}
		
		long parentHandle;
		if (fbF.isVisible()){
			parentHandle = fbF.getParentHandle();
		}
		else{
			return;
		}
		
		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
		
		if (parentNode == null){
			parentNode = megaApi.getRootNode();
		}
		
		megaApi.createFolder(title, parentNode, this);
		
	}
	
	public void showRenameDialog(final MegaNode document, String text){
		log("showRenameDialog");
		final EditTextCursorWatcher input = new EditTextCursorWatcher(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),
				KeyEvent.KEYCODE_ENTER);
		input.setText(text);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, boolean hasFocus) {
				if (hasFocus) {
					if (document.isFolder()){
						input.setSelection(0, input.getText().length());
					}
					else{
						String [] s = document.getName().split("\\.");
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

		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_rename) + " "	+ new String(document.getName()), null, input);
		builder.setPositiveButton(getString(R.string.context_rename),
				new DialogInterface.OnClickListener() {
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
	
	private void rename(MegaNode document, String newName){
		log("rename");
		if (newName.equals(document.getName())) {
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
		
		log("renaming " + document.getName() + " to " + newName);
		
		megaApi.renameNode(document, newName, this);
	}
	
	public void showMove(ArrayList<Long> handleList){
		log("showMove");
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}
	
	public void showCopy(ArrayList<Long> handleList){
		log("showCopy");
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}
	
	/*
	 * If there is an application that can manage the Intent, returns true. Otherwise, false.
	 */
	public static boolean isIntentAvailable(Context ctx, Intent intent) {
		log("isIntentAvailable");
		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		log("onActivityResult");
		if (intent == null) {
			return;
		}
		if (requestCode == REQUEST_CODE_GET && resultCode == RESULT_OK) {
			Uri uri = intent.getData();
			intent.setAction(Intent.ACTION_GET_CONTENT);
			FilePrepareTask filePrepareTask = new FilePrepareTask(this);
			filePrepareTask.execute(intent);
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.upload_prepare));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
		} 
		else if (requestCode == REQUEST_CODE_GET_LOCAL && resultCode == RESULT_OK) {
			
			String folderPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			ArrayList<String> paths = intent.getStringArrayListExtra(FileStorageActivity.EXTRA_FILES);
			
			int i = 0;
			long parentHandle;
			if (fbF.isVisible()){
				parentHandle = fbF.getParentHandle();
			}
			else{
				return;
			}
			
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode == null){
				parentNode = megaApi.getRootNode();
			}
			
			for (String path : paths) {
				Intent uploadServiceIntent = new Intent (this, UploadService.class);
				File file = new File (path);
				if (file.isDirectory()){
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, file.getName());
					log("EXTRA_FILE_PATH_dir:" + file.getAbsolutePath());
				}
				else{
					ShareInfo info = ShareInfo.infoFromFile(file);
					if (info == null){
						continue;
					}
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
					uploadServiceIntent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
					log("EXTRA_FILE_PATH_file:" + info.getFileAbsolutePath());
				}
				
				log("EXTRA_FOLDER_PATH:" + folderPath);
				uploadServiceIntent.putExtra(UploadService.EXTRA_FOLDERPATH, folderPath);
				uploadServiceIntent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				startService(uploadServiceIntent);				
				
				i++;
			}
			
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
		else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivity.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivity.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);

			
			downloadTo (parentPath, url, size, hashes);
			Util.showToast(this, R.string.download_began);
		}
		else if (requestCode == REQUEST_CODE_REFRESH && resultCode == RESULT_OK) {
			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				parentHandleBrowser = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){
					if (fbF != null){
						if (fbF.isVisible()){
							NodeList nodes = megaApi.getChildren(parentNode, orderGetChildren);
							fbF.setNodes(nodes);
							fbF.getListView().invalidateViews();
						}
					}
				}
				else{
					if (fbF != null){
						if (fbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
							fbF.setNodes(nodes);
							fbF.getListView().invalidateViews();
						}
					}
				}
			}
			else if (drawerItem == DrawerItem.RUBBISH_BIN){
				parentHandleRubbish = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleRubbish);
				if (parentNode != null){
					if (rbF != null){
						if (rbF.isVisible()){
							NodeList nodes = megaApi.getChildren(parentNode, orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
						}
					}
				}
				else{
					if (rbF != null){
						if (rbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
						}
					}
				}
			}
			else if (drawerItem == DrawerItem.SHARED_WITH_ME){
				parentHandleSharedWithMe = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleSharedWithMe);
				if (parentNode != null){
					if (swmF != null){
						if (swmF.isVisible()){
							NodeList nodes = megaApi.getChildren(parentNode, orderGetChildren);
							swmF.setNodes(nodes);
							swmF.getListView().invalidateViews();
						}
					}
				}
				else{
					if (swmF != null){
						if (swmF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getInboxNode(), orderGetChildren);
							swmF.setNodes(nodes);
							swmF.getListView().invalidateViews();
						}
					}
				}
			}
		}
		else if (requestCode == REQUEST_CODE_SORT_BY && resultCode == RESULT_OK){
			orderGetChildren = intent.getIntExtra("ORDER_GET_CHILDREN", 1);
			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){
					if (fbF != null){
						if (fbF.isVisible()){
							NodeList nodes = megaApi.getChildren(parentNode, orderGetChildren);
							fbF.setOrder(orderGetChildren);
							fbF.setNodes(nodes);
							fbF.getListView().invalidateViews();
						}
					}
				}
				else{
					if (fbF != null){
						if (fbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
							fbF.setOrder(orderGetChildren);
							fbF.setNodes(nodes);
							fbF.getListView().invalidateViews();
						}
					}
				}
			}
			else if (drawerItem == DrawerItem.RUBBISH_BIN){
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleRubbish);
				if (parentNode != null){
					if (rbF != null){
						if (rbF.isVisible()){
							NodeList nodes = megaApi.getChildren(parentNode, orderGetChildren);
							rbF.setOrder(orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
						}
					}
				}
				else{
					if (rbF != null){
						if (rbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
							rbF.setOrder(orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
						}
					}
				}
			}
			else if (drawerItem == DrawerItem.SHARED_WITH_ME){
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleSharedWithMe);
				if (parentNode != null){
					if (swmF != null){
						if (swmF.isVisible()){
							NodeList nodes = megaApi.getChildren(parentNode, orderGetChildren);
							swmF.setOrder(orderGetChildren);
							swmF.setNodes(nodes);
							swmF.getListView().invalidateViews();
						}
					}
				}
				else{
					if (swmF != null){
						if (swmF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getInboxNode(), orderGetChildren);
							swmF.setOrder(orderGetChildren);
							swmF.setNodes(nodes);
							swmF.getListView().invalidateViews();
						}
					}
				}
			}
		}
	}
	
	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
		log("getDlList");
		if (megaApi.getRootNode() == null)
			return;
		
		folder.mkdir();
		NodeList nodeList = megaApi.getChildren(parent, orderGetChildren);
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
	 * Background task to process files for uploading
	 */
	private class FilePrepareTask extends AsyncTask<Intent, Void, List<ShareInfo>> {
		Context context;
		
		FilePrepareTask(Context context){
			log("FilePrepareTask::FilePrepareTask");
			this.context = context;
		}
		
		@Override
		protected List<ShareInfo> doInBackground(Intent... params) {
			log("FilePrepareTask::doInBackGround");
			return ShareInfo.processIntent(params[0], context);
		}

		@Override
		protected void onPostExecute(List<ShareInfo> info) {
			log("FilePrepareTask::onPostExecute");
			filePreparedInfos = info;
			onIntentProcessed();
		}
	}
	
	/*
	 * Handle processed upload intent
	 */
	public void onIntentProcessed() {
		log("onIntentProcessed");
		List<ShareInfo> infos = filePreparedInfos;
		if (statusDialog != null) {
			try { 
				statusDialog.dismiss(); 
			} 
			catch(Exception ex){}
		}
		
		long parentHandle = -1;
		if (fbF.isVisible()){
			parentHandle = fbF.getParentHandle();
		}
		
		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle); 
		if(parentNode == null){
			Util.showErrorAlertDialog(getString(R.string.error_temporary_unavaible), false, this);
			return;
		}
			
		if (infos == null) {
			Util.showErrorAlertDialog(getString(R.string.upload_can_not_open),
					false, this);
		} 
		else {
			Toast.makeText(getApplicationContext(), getString(R.string.upload_began),
					Toast.LENGTH_SHORT).show();
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
	public void onUsersUpdate(MegaApiJava api) {
		log("onUsersUpdate");
		if (cF != null){
			if (cF.isVisible()){	
				UserList contacts = megaApi.getContacts();
				cF.setContacts(contacts);
				cF.getListView().invalidateViews();
			}
		}
	}

	@Override
	public void onNodesUpdate(MegaApiJava api) {
		log("onNodesUpdate");
		try { 
			statusDialog.dismiss();	
		} 
		catch (Exception ex) {}
		
		if (fbF != null){
			if (fbF.isVisible()){
				NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
				fbF.setNodes(nodes);
				fbF.getListView().invalidateViews();
			}
		}
		if (rbF != null){
			if (rbF.isVisible()){
				if (isClearRubbishBin){
					isClearRubbishBin = false;
					parentHandleRubbish = megaApi.getRubbishNode().getHandle();
					rbF.setParentHandle(megaApi.getRubbishNode().getHandle());
					NodeList nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
					rbF.setNodes(nodes);
					rbF.getListView().invalidateViews();
					aB.setTitle(getString(R.string.section_rubbish_bin));	
					getmDrawerToggle().setDrawerIndicatorEnabled(true);
				}
				else{
					NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbF.getParentHandle()), orderGetChildren);
					rbF.setNodes(nodes);
					rbF.getListView().invalidateViews();
				}				
			}
		}
		if (swmF != null){
			if (swmF.isVisible()){
				NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(swmF.getParentHandle()), orderGetChildren);
				swmF.setNodes(nodes);
				swmF.getListView().invalidateViews();
			}
		}
		if (psF != null){
			if (psF.isVisible()){
				long photoSyncHandle = psF.getPhotoSyncHandle();
				MegaNode nps = megaApi.getNodeByHandle(photoSyncHandle);
				log("photoSyncHandle: " + photoSyncHandle);
				if (nps != null){
					log("nps != null");
					NodeList nodes = megaApi.getChildren(nps, MegaApiJava.ORDER_MODIFICATION_DESC);
					psF.setNodes(nodes);
					psF.getListView().invalidateViews();
				}
			}
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		log("onReloadNeeded");
		// TODO Fetch nodes from MEGA		
	}	
	
	public void setParentHandleBrowser(long parentHandleBrowser){
		log("setParentHandleBrowser");
		
		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		
		//ArrayList<MegaTransfer> mTList = new ArrayList<MegaTransfer> ();
		
		this.parentHandleBrowser = parentHandleBrowser;
		
		tL = megaApi.getTransfers();
		//Actualizar la lista de transferencias...
		
		for(int i=0; i<tL.size(); i++){
			
			MegaTransfer tempT = tL.get(i);
			long handleT = tempT.getNodeHandle();
			MegaNode nodeT = megaApi.getNodeByHandle(handleT);
			MegaNode parentT = megaApi.getParentNode(nodeT);
			
			if(parentT.getHandle() == this.parentHandleBrowser){
				
				mTHash.put(handleT,tempT);				
			}			
		}			
		
		fbF.setTransfers(mTHash);		
		
	}
	
	public void setParentHandleRubbish(long parentHandleRubbish){
		log("setParentHandleRubbish");
		this.parentHandleRubbish = parentHandleRubbish;
	}
	
	public void setParentHandleSharedWithMe(long parentHandleSharedWithMe){
		log("setParentHandleSharedWithMe");
		this.parentHandleSharedWithMe = parentHandleSharedWithMe;
	}
	
	public void setParentHandleSearch(long parentHandleSearch){
		log("setParentHandleSearch");
		this.parentHandleSearch = parentHandleSearch;
	}
	
	public void setPathNavigationOffline(String pathNavigation){
		this.pathNavigation = pathNavigation;
	}
	
	public void setPauseIconVisible(boolean visible){
		log("setPauseIconVisible");
		pauseIconVisible = visible;
		if (addMenuItem != null){
			addMenuItem.setVisible(visible);
		}
	}
	
	public void setTransfers(TransferList transfersList){
		log("setTransfers");
		if (tF != null){
			tF.setTransfers(transfersList);
		}
	}
	
	private void getOverflowMenu() {
		log("getOverflowMenu");
	     try {
	        ViewConfiguration config = ViewConfiguration.get(this);
	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public void setDownloadPlay(boolean downloadPlay){
		log("setDownloadPlay");
		this.downloadPlay = downloadPlay;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		log("onKeyUp");
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferStart");
		

		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		if (tF == null){
			tF = new TransfersFragment();
		}
		
		//ArrayList<MegaTransfer> mTList = new ArrayList<MegaTransfer> ();	
		tL = megaApi.getTransfers();
		//Actualizar la lista de transferencias...
		tF.setTransfers(tL);
		
		for(int i=0; i<tL.size(); i++){
			
			MegaTransfer tempT = tL.get(i).copy();
			long handleT = tempT.getNodeHandle();
			MegaNode nodeT = megaApi.getNodeByHandle(handleT);
			MegaNode parentT = megaApi.getParentNode(nodeT);
			
			if(parentT.getHandle() == this.parentHandleBrowser){
				
				mTHash.put(handleT,tempT);
				
			}			
		}			
		
		fbF.setTransfers(mTHash);	
		
		log("onTransferStart: " + transfer.getFileName() + " - " + transfer.getTag());

	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		log("onTransferFinish");
		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		if (tF == null){
			tF = new TransfersFragment();
		}
		
		//ArrayList<MegaTransfer> mTList = new ArrayList<MegaTransfer> ();	
		tL = megaApi.getTransfers();
		//Actualizar la lista de transferencias...
		tF.setTransfers(tL);
		
		for(int i=0; i<tL.size(); i++){
			
			MegaTransfer tempT = tL.get(i).copy();
			long handleT = tempT.getNodeHandle();
			MegaNode nodeT = megaApi.getNodeByHandle(handleT);
			MegaNode parentT = megaApi.getParentNode(nodeT);
			
			if(parentT.getHandle() == this.parentHandleBrowser){
				
				mTHash.put(handleT,tempT);
				
			}			
		}			
		
		fbF.setTransfers(mTHash);	

		log("onTransferFinish: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferUpdate: " + transfer.getFileName() + " - " + transfer.getTag());
		//HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		if (tF == null){
			tF = new TransfersFragment();
		}
		
		//ArrayList<MegaTransfer> mTList = new ArrayList<MegaTransfer> ();	
		tL = megaApi.getTransfers();
		//Actualizar la lista de transferencias...
		tF.setTransfers(tL);
		fbF.setCurrentTransfer(transfer);	
		
//		for(int i=0; i<tL.size(); i++){
//			
//			MegaTransfer tempT = tL.get(i);
//			long handleT = tempT.getNodeHandle();
//			MegaNode nodeT = megaApi.getNodeByHandle(handleT);
//			MegaNode parentT = megaApi.getParentNode(nodeT);
//			
//			if(parentT.getHandle() == this.parentHandleBrowser){
//				
//				mTHash.put(handleT,tempT);
//				
//			}			
//		}			
		//fbF.getParentFragment()
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		
		log("onTransferTemporaryError: " + transfer.getFileName() + " - " + transfer.getTag());
	}
	
	public static void log(String message) {
		Util.log("ManagerActivity", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate");
		// TODO Auto-generated method stub
		
	}
	
	public void downloadTo(String parentPath, String url, long size, long [] hashes){
		log("downloadTo");
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

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		log("onTransferData");
		return true;
	}
}
