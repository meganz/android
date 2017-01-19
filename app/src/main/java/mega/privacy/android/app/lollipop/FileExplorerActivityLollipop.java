package mega.privacy.android.app.lollipop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.TabsAdapter;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.adapters.CloudDrivePagerAdapter;
import mega.privacy.android.app.lollipop.adapters.FileExplorerPagerAdapter;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.Constants;
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
import nz.mega.sdk.MegaUser;

public class FileExplorerActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, MegaGlobalListenerInterface{
	
	public static String ACTION_PROCESSED = "CreateLink.ACTION_PROCESSED";
	
	public static String ACTION_PICK_MOVE_FOLDER = "ACTION_PICK_MOVE_FOLDER";
	public static String ACTION_PICK_COPY_FOLDER = "ACTION_PICK_COPY_FOLDER";
	public static String ACTION_PICK_IMPORT_FOLDER = "ACTION_PICK_IMPORT_FOLDER";
	public static String ACTION_SELECT_FOLDER = "ACTION_SELECT_FOLDER";
	public static String ACTION_SELECT_FOLDER_TO_SHARE = "ACTION_SELECT_FOLDER_TO_SHARE";
	public static String ACTION_SELECT_FILE = "ACTION_SELECT_FILE";
	public static String ACTION_UPLOAD_SELFIE = "ACTION_UPLOAD_SELFIE";	
	public static String ACTION_CHOOSE_MEGA_FOLDER_SYNC = "ACTION_CHOOSE_MEGA_FOLDER_SYNC";

	public static int UPLOAD = 0;
	public static int MOVE = 1;
	public static int COPY = 2;
	public static int CAMERA = 3;
	public static int IMPORT = 4;
	public static int SELECT = 5;
	public static int UPLOAD_SELFIE = 6;
	public static int SELECT_CAMERA_FOLDER = 7;

	public static int NO_TABS = -1;
	public static int CLOUD_TAB = 0;
	public static int INCOMING_TAB = 1;
	
	Toolbar tB;
    ActionBar aB;
	DisplayMetrics outMetrics;
    RelativeLayout fragmentContainer;
	LinearLayout loginLoggingIn;
	ProgressBar loginProgressBar;
	ProgressBar loginFetchNodesProgressBar;
	TextView generatingKeysText;
	TextView queryingSignupLinkText;
	TextView confirmingAccountText;
	TextView loggingInText;
	TextView fetchingNodesText;
	TextView prepareNodesText;
	
	MenuItem createFolderMenuItem;

	FrameLayout cloudDriveFrameLayout;

	private String gSession;
    UserCredentials credentials;
	private String lastEmail;
//	private ImageView windowBack;
//	private boolean backVisible = false;
//	private TextView windowTitle;
	
	private MegaApiAndroid megaApi;

	private int mode;
	boolean selectFile = false;
	
	private long[] moveFromHandles;
	private long[] copyFromHandles;
	private ArrayList<String> selectedContacts;
	private String imagePath;
	private boolean folderSelected = false;
	
	private Handler handler;
	
	private int tabShown = CLOUD_TAB;
	
	private CloudDriveExplorerFragmentLollipop cDriveExplorer;
	private IncomingSharesExplorerFragmentLollipop iSharesExplorer;

	private AlertDialog newFolderDialog;
	
	ProgressDialog statusDialog;
	
	private List<ShareInfo> filePreparedInfos;

	//Tabs in Cloud
	TabLayout tabLayoutExplorer;
	LinearLayout fileExplorerSectionLayout;
	FileExplorerPagerAdapter mTabsAdapterExplorer;
	ViewPager viewPagerExplorer;

	ArrayList<MegaNode> nodes;
	
	long gParentHandle;
	long parentHandleIncoming;
	long parentHandleCloud;
	int deepBrowserTree;
	String gcFTag = "";

	Intent intent = null;
	
	/*
	 * Background task to process files for uploading
	 */
	private class FilePrepareTask extends AsyncTask<Intent, Void, List<ShareInfo>> {
		Context context;
		
		FilePrepareTask(Context context){
			this.context = context;
		}
		
		@Override
		protected List<ShareInfo> doInBackground(Intent... params) {
			log("FilePrepareTask: doInBackground");
			return ShareInfo.processIntent(params[0], context);
		}

		@Override
		protected void onPostExecute(List<ShareInfo> info) {
			filePreparedInfos = info;			
			onIntentProcessed();
		}			
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
	public void onConfigurationChanged(Configuration newConfig) {
		log("onConfigurationChanged");
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
//		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//
//		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
//
//		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		log("onCreate first");
		super.onCreate(savedInstanceState);

		if(savedInstanceState!=null){
			log("Bundle is NOT NULL");
			parentHandleCloud = savedInstanceState.getLong("parentHandleCloud", -1);
			log("savedInstanceState -> parentHandleCloud: "+parentHandleCloud);
			parentHandleIncoming = savedInstanceState.getLong("parentHandleIncoming", -1);
			log("savedInstanceState -> parentHandleIncoming: "+parentHandleIncoming);
			deepBrowserTree = savedInstanceState.getInt("deepBrowserTree", deepBrowserTree);
			log("savedInstanceState -> deepBrowserTree: "+deepBrowserTree);
		}
		else{
			log("Bundle is NULL");
			parentHandleCloud = -1;
			parentHandleIncoming = -1;
			deepBrowserTree = 0;
		}
				
//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		credentials = dbH.getCredentials();
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
		if (credentials == null){
			log("User credentials NULL");
			AccountController aC = new AccountController(this);
			aC.logout(this, megaApi, false);
			
			Intent loginIntent = new Intent(this, LoginActivityLollipop.class);
			loginIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			loginIntent.setAction(Constants.ACTION_FILE_EXPLORER_UPLOAD);
			/*if (intent != null){
				if(intent.getExtras() != null)
				{
					Bundle bundle = intent.getExtras();
					Uri uri = (Uri)bundle.get(Intent.EXTRA_STREAM);
					log("URI in bundle: "+uri);
					loginIntent.putExtras(intent.getExtras());
				}
				
				if(intent.getData() != null)
				{
					log("URI: "+intent.getData());
					loginIntent.setData(intent.getData());
				}
			}
			else{
				log("intent==null");
			}*/	
			startActivity(loginIntent);
			return;
//			finish();
		}
		else{
			log("User has credentials");
		}
		
		if (savedInstanceState != null){
			folderSelected = savedInstanceState.getBoolean("folderSelected", false);
		}
	
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		
		megaApi.addGlobalListener(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
		}
		
		setContentView(R.layout.activity_file_explorer);
		
		fragmentContainer = (RelativeLayout) findViewById(R.id.fragment_container_file_explorer);
				
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_explorer);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		if(aB!=null){
			aB.hide();
		}
		else{
			log("aB is null");
		}

		//TABS
		fileExplorerSectionLayout= (LinearLayout)findViewById(R.id.tabhost_explorer);
		tabLayoutExplorer =  (TabLayout) findViewById(R.id.sliding_tabs_file_explorer);
		viewPagerExplorer = (ViewPager) findViewById(R.id.explorer_tabs_pager);
		
		//Layout for login if needed
		loginLoggingIn = (LinearLayout) findViewById(R.id.file_logging_in_layout);
		loginProgressBar = (ProgressBar) findViewById(R.id.file_login_progress_bar);
		loginFetchNodesProgressBar = (ProgressBar) findViewById(R.id.file_login_fetching_nodes_bar);
		generatingKeysText = (TextView) findViewById(R.id.file_login_generating_keys_text);
		queryingSignupLinkText = (TextView) findViewById(R.id.file_login_query_signup_link_text);
		confirmingAccountText = (TextView) findViewById(R.id.file_login_confirm_account_text);
		loggingInText = (TextView) findViewById(R.id.file_login_logging_in_text);
		fetchingNodesText = (TextView) findViewById(R.id.file_login_fetch_nodes_text);
		prepareNodesText = (TextView) findViewById(R.id.file_login_prepare_nodes_text); 
        		
		intent = getIntent();
		if (megaApi.getRootNode() == null){
			log("hide action bar");
			getSupportActionBar().hide();
			fileExplorerSectionLayout.setVisibility(View.GONE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			loginLoggingIn.setVisibility(View.VISIBLE);
//			generatingKeysText.setVisibility(View.VISIBLE);
			loginProgressBar.setVisibility(View.VISIBLE);
			loginFetchNodesProgressBar.setVisibility(View.GONE);
			loggingInText.setVisibility(View.VISIBLE);
			fetchingNodesText.setVisibility(View.GONE);
			prepareNodesText.setVisibility(View.GONE);
			gSession = credentials.getSession();
			log("SESSION: " + gSession);
			megaApi.fastLogin(gSession, this);
		}
		else{
			afterLoginAndFetch();
		}

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
	}
	
	private void afterLoginAndFetch(){
		log("afterLoginAndFetch");
		
		handler = new Handler();

		log("SHOW action bar");
		if(aB==null){
			aB=getSupportActionBar();
		}
		aB.show();
		log("aB.setHomeAsUpIndicator_65");
		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);
		aB.setTitle(getString(R.string.section_cloud_drive));

		if ((intent != null) && (intent.getAction() != null)){
			log("intent OK: "+intent.getAction());
			if (intent.getAction().equals(ACTION_SELECT_FOLDER_TO_SHARE)){
				//Just show Cloud Drive, no need of tabhost

				mode = SELECT;
				selectFile = false;
				selectedContacts=intent.getStringArrayListExtra("SELECTED_CONTACTS");

				cloudDriveFrameLayout = (FrameLayout) findViewById(R.id.cloudDriveFrameLayout);

				if(cDriveExplorer==null){
					cDriveExplorer = new CloudDriveExplorerFragmentLollipop();
				}

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.cloudDriveFrameLayout, cDriveExplorer, "cDriveExplorer");
				ft.commitNow();

				cloudDriveFrameLayout.setVisibility(View.VISIBLE);

				if(fileExplorerSectionLayout!=null){
					fileExplorerSectionLayout.setVisibility(View.GONE);
				}
				else{
					fileExplorerSectionLayout= (LinearLayout)findViewById(R.id.tabhost_explorer);
					fileExplorerSectionLayout.setVisibility(View.GONE);
				}

				tabShown=NO_TABS;

			}else{

				if (intent.getAction().equals(ACTION_PICK_MOVE_FOLDER)){
					log("ACTION_PICK_MOVE_FOLDER");
					mode = MOVE;
					moveFromHandles = intent.getLongArrayExtra("MOVE_FROM");

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

					}

					ArrayList<Long> list = new ArrayList<Long>(moveFromHandles.length);
					for (long n : moveFromHandles){
						list.add(n);
					}
					String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
					if(cDriveExplorer!=null){
						cDriveExplorer.setDisableNodes(list);
					}
				}
				else if (intent.getAction().equals(ACTION_PICK_COPY_FOLDER)){
					log("ACTION_PICK_COPY_FOLDER");
					mode = COPY;
					copyFromHandles = intent.getLongArrayExtra("COPY_FROM");

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

					}

					ArrayList<Long> list = new ArrayList<Long>(copyFromHandles.length);
					for (long n : copyFromHandles){
//					log("Disabled nodes to copy: "+n);
						list.add(n);
					}
					String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
					if(cDriveExplorer!=null){
						cDriveExplorer.setDisableNodes(list);
					}
				}
				else if (intent.getAction().equals(ACTION_CHOOSE_MEGA_FOLDER_SYNC)){
					log("action = ACTION_CHOOSE_MEGA_FOLDER_SYNC");
					mode = SELECT_CAMERA_FOLDER;

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

					}
				}
				else if (intent.getAction().equals(ACTION_PICK_IMPORT_FOLDER)){
					log("action = ACTION_PICK_IMPORT_FOLDER");
					mode = IMPORT;

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

					}
				}
				else if ((intent.getAction().equals(ACTION_SELECT_FOLDER))){
					log("action = ACTION_SELECT_FOLDER");
					mode = SELECT;
					selectedContacts=intent.getStringArrayListExtra("SELECTED_CONTACTS");

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

					}

				}
				else if (intent.getAction().equals(ACTION_SELECT_FILE)){
					log("action = ACTION_SELECT_FILE");
					mode = SELECT;
					selectFile = true;
					selectedContacts=intent.getStringArrayListExtra("SELECTED_CONTACTS");

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

					}
				}
				else if(intent.getAction().equals(ACTION_UPLOAD_SELFIE)){
					log("action = ACTION_UPLOAD_SELFIE");
					mode = UPLOAD_SELFIE;
					imagePath=intent.getStringExtra("IMAGE_PATH");

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

					}
				}
				else{
					log("action = UPLOAD");
					mode = UPLOAD;

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

					}
				}

				viewPagerExplorer.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
					public void onPageScrollStateChanged(int state) {}
					public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

					public void onPageSelected(int position) {
						log("onTabChanged TabId :"+ position);
						supportInvalidateOptionsMenu();
						if(position == 0){
							tabShown=CLOUD_TAB;
							String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
							gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
							cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);

							if(cDriveExplorer!=null){
								if(cDriveExplorer.parentHandle==-1|| cDriveExplorer.parentHandle==megaApi.getRootNode().getHandle()){
									changeTitle(getString(R.string.section_cloud_drive));
								}
								else{
									changeTitle(megaApi.getNodeByHandle(cDriveExplorer.parentHandle).getName());
								}
							}
						}
						else if(position == 1){
							tabShown=INCOMING_TAB;

							String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
							gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
							iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);

							if(iSharesExplorer!=null){
								if(iSharesExplorer.getDeepBrowserTree()==0){
									changeTitle(getString(R.string.title_incoming_shares_explorer));
								}
								else{
									changeTitle(iSharesExplorer.name);
								}
							}

						}
					}
				});
			}

		}
		else{
			log("intent error");
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.file_explorer_action, menu);
	    
	    createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder);
	    
	    if (cDriveExplorer != null){	
	    	createFolderMenuItem.setVisible(true);
	    }
//	    if(iSharesExplorer != null){
//	    	if (iSharesExplorer.deepBrowserTree==0){
//	    		createFolderMenuItem.setVisible(false);
//	    	}
//	    	else{
//	    		//Check the permissions of the folder
//	    		createFolderMenuItem.setVisible(true);
//	    	}
//	    }
	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		log("onPrepareOptionsMenuLollipop");
		
		// Inflate the menu items for use in the action bar
//	    MenuInflater inflater = getMenuInflater();
//	    inflater.inflate(R.menu.file_explorer_action, menu);
//	    
//	    createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder);
	    
	    //Check the tab shown
		if (viewPagerExplorer != null){
		    int index = viewPagerExplorer.getCurrentItem();
			if(index==0){				
				//CLOUD TAB				
				String cFTag2 = getFragmentTag(R.id.explorer_tabs_pager, 0);		
				log("Tag: "+ cFTag2);
				cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag2);
				if (cDriveExplorer != null){
					createFolderMenuItem.setVisible(true);
				}
			}
			else{
				String cFTag1 = getFragmentTag(R.id.explorer_tabs_pager, 1);		
				log("Tag: "+ cFTag1);
				iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
				if(iSharesExplorer != null){	
					log("Level deepBrowserTree: "+iSharesExplorer.getDeepBrowserTree());
			    	if (iSharesExplorer.getDeepBrowserTree()==0){
			    		createFolderMenuItem.setVisible(false);
			    	}
			    	else{		    		
			    		//Check the folder's permissions
			    		long parentH = iSharesExplorer.getParentHandle();
			    		MegaNode n = megaApi.getNodeByHandle(parentH);
						int accessLevel= megaApi.getAccess(n);
						log("Node: "+n.getName());
																				
						switch(accessLevel){
							case MegaShare.ACCESS_OWNER:
							case MegaShare.ACCESS_READWRITE:
							case MegaShare.ACCESS_FULL:{
								log("The node is: "+n.getName()+" permissions: "+accessLevel);
								createFolderMenuItem.setVisible(true);				
								break;
							}
							case MegaShare.ACCESS_READ:{
								log("The node is: "+n.getName()+" permissions: ACCESS_READ "+accessLevel);
								createFolderMenuItem.setVisible(false);
								break;
							}						
						}
			    	}
			    }
			}
		}else{
			if (cDriveExplorer != null){
				createFolderMenuItem.setVisible(true);
			}
		}
	    return super.onPrepareOptionsMenu(menu);
	}
	
	private View getTabIndicator(Context context, String title) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);

        TextView tv = (TextView) view.findViewById(R.id.textView);
        tv.setText(title);
        return view;
    }
	
//	public void setBackVisibility(boolean backVisible){
////		this.backVisible = backVisible;
////		if (windowBack != null){
////			if (!backVisible){
////				windowBack.setVisibility(View.INVISIBLE);
////			}
////			else{
////				windowBack.setVisibility(View.VISIBLE);
////			}
////		}
//		if(backVisible){
//			aB.setDisplayHomeAsUpEnabled(true);
//			aB.setDisplayShowHomeEnabled(true);
//		}
//		else{
//			aB.setDisplayHomeAsUpEnabled(false);
//			aB.setDisplayShowHomeEnabled(false);
//		}
//	}
	
	public void changeTitle (String title){
		aB.setTitle(title);	
	}
	
	private String getFragmentTag(int viewPagerId, int fragmentPosition)
	{
	     return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
	}
	
	public void finishActivity(){
		finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		log("onSaveInstanceState");
		super.onSaveInstanceState(bundle);
		bundle.putBoolean("folderSelected", folderSelected);
		if(cDriveExplorer!=null){
			parentHandleCloud = cDriveExplorer.getParentHandle();
		}
		else{
			parentHandleCloud = -1;
		}
		bundle.putLong("parentHandleCloud", parentHandleCloud);
		if(iSharesExplorer!=null){
			parentHandleIncoming = iSharesExplorer.getParentHandle();
			deepBrowserTree = iSharesExplorer.getDeepBrowserTree();
		}
		else{
			parentHandleIncoming = -1;
			deepBrowserTree = 0;
		}
		bundle.putLong("parentHandleIncoming", parentHandleIncoming);
		bundle.putInt("deepBrowserTree", deepBrowserTree);
		log("IN BUNDLE -> deepBrowserTree: "+deepBrowserTree);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (getIntent() != null){
			if (mode == UPLOAD) {
				if (folderSelected){
					if (filePreparedInfos == null){
						FilePrepareTask filePrepareTask = new FilePrepareTask(this);
						filePrepareTask.execute(getIntent());
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
				}
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		log("onBackPressed: "+tabShown);		
		
		if(tabShown==CLOUD_TAB){
			String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
			gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
			cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	
			if(cDriveExplorer!=null){
				if (cDriveExplorer.onBackPressed() == 0){
					super.onBackPressed();
					return;
				}
			}
		}
		else if(tabShown==INCOMING_TAB){
			String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
			gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
			iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		
			if(iSharesExplorer!=null){
				if (iSharesExplorer.onBackPressed() == 0){
					super.onBackPressed();
					return;
				}
			}
		}
		else if(tabShown==NO_TABS){
			cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag("cDriveExplorer");

			if(cDriveExplorer!=null){
				if (cDriveExplorer.onBackPressed() == 0){
					super.onBackPressed();
					return;
				}
			}
		}
		else{
			super.onBackPressed();
		}
	}
	
	public void onIntentProcessed() {
		List<ShareInfo> infos = filePreparedInfos;
		
		if (statusDialog != null) {
			try { 
				statusDialog.dismiss(); 
			} 
			catch(Exception ex){}
		}
		
		log("intent processed!");
		if (folderSelected) {
			if (infos == null) {
				Snackbar.make(fragmentContainer,getString(R.string.upload_can_not_open),Snackbar.LENGTH_LONG).show();
				return;
			}
			else {
				long parentHandle;
				if (cDriveExplorer != null){
					parentHandle = cDriveExplorer.getParentHandle();
				}
				else{
					parentHandle = gParentHandle;
				}
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				if(parentNode == null){
					parentNode = megaApi.getRootNode();
				}
				Snackbar.make(fragmentContainer,getString(R.string.upload_began),Snackbar.LENGTH_LONG).show();
				for (ShareInfo info : infos) {
					Intent intent = new Intent(this, UploadService.class);
					intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
					intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
					intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
					intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
					startService(intent);
				}
				filePreparedInfos = null;
				log("finish!!!");
				finish();
			}	
		}
	}
	
	public void buttonClick(long handle){
		log("buttonClick");
		
		if (tabShown == INCOMING_TAB){
			if (iSharesExplorer.getDeepBrowserTree()==0){
				Intent intent = new Intent();
				setResult(RESULT_FIRST_USER, intent);
				finish();
				return;
			}
		}
		
		folderSelected = true;
		this.gParentHandle = handle;
		
		if (mode == MOVE) {
			long parentHandle = handle;
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode == null){
				parentNode = megaApi.getRootNode();
			}
			
			Intent intent = new Intent();
			intent.putExtra("MOVE_TO", parentNode.getHandle());
			intent.putExtra("MOVE_HANDLES", moveFromHandles);
			setResult(RESULT_OK, intent);
			log("finish!");
			finish();
		}
		else if (mode == COPY){
			
			long parentHandle = handle;
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode == null){
				parentNode = megaApi.getRootNode();
			}
			
			Intent intent = new Intent();
			intent.putExtra("COPY_TO", parentNode.getHandle());
			intent.putExtra("COPY_HANDLES", copyFromHandles);
			setResult(RESULT_OK, intent);
			log("finish!");
			finish();
		}
		else if(mode == UPLOAD_SELFIE){
		
			long parentHandle = handle;
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode == null){
				parentNode = megaApi.getRootNode();
			}
		
			Intent intent = new Intent(this, UploadService.class);
			File selfie = new File(imagePath);
			intent.putExtra(UploadService.EXTRA_FILEPATH, selfie.getAbsolutePath());
			intent.putExtra(UploadService.EXTRA_NAME, selfie.getName());
			intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
			intent.putExtra(UploadService.EXTRA_SIZE, selfie.length());
			startService(intent);
			
			Intent intentResult = new Intent();
			setResult(RESULT_OK, intentResult);
			finish();
			
		}
		else if (mode == UPLOAD){
			log("mode UPLOAD");
			
			if (filePreparedInfos == null){
//				Intent prueba = getIntent();
//				Bundle bundle = prueba.getExtras();
//				Uri uri = (Uri)bundle.get(Intent.EXTRA_STREAM);
//				log("URI mode UPLOAD in bundle: "+uri);
				
				FilePrepareTask filePrepareTask = new FilePrepareTask(this);
				filePrepareTask.execute(getIntent());
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
			else{
				onIntentProcessed();
			}
			log("After UPLOAD click - back to Cloud");
			this.backToCloud(handle);
		}
		else if (mode == IMPORT){
			log("mode IMPORT");
			long parentHandle = handle;
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode == null){
				parentNode = megaApi.getRootNode();
			}
			
			Intent intent = new Intent();
			intent.putExtra("IMPORT_TO", parentNode.getHandle());
			setResult(RESULT_OK, intent);
			log("finish!");
			finish();
		}
		else if (mode == SELECT){

			if(selectFile)
			{
				Intent intent = new Intent();
				intent.putExtra("SELECT", handle);
				intent.putStringArrayListExtra("SELECTED_CONTACTS", selectedContacts);
				setResult(RESULT_OK, intent);
				finish();
			}
			else{
				long parentHandle = handle;
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				if(parentNode == null){
					parentNode = megaApi.getRootNode();
				}

				Intent intent = new Intent();
				intent.putExtra("SELECT", parentNode.getHandle());
				intent.putStringArrayListExtra("SELECTED_CONTACTS", selectedContacts);
				setResult(RESULT_OK, intent);
				finish();
			}
			
		}
		else if (mode == SELECT_CAMERA_FOLDER){

			long parentHandle = handle;
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode == null){
				parentNode = megaApi.getRootNode();
			}

			Intent intent = new Intent();
			intent.putExtra("SELECT_MEGA_FOLDER", parentNode.getHandle());			
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	public void backToCloud(long handle){
		log("backToCloud: "+handle);
		Intent startIntent = new Intent(this, ManagerActivityLollipop.class);
		if(handle!=-1){
			startIntent.setAction(Constants.ACTION_OPEN_FOLDER);
			startIntent.putExtra("PARENT_HANDLE", handle);
		}
		startActivity(startIntent);
	}

	public void showNewFolderDialog(String editText){
		
		String text;
		if (editText == null || editText.equals("")){
			text = getString(R.string.context_new_folder_name);
		}
		else{
			text = editText;
		}
		
		final EditText input = new EditText(this);
//		input.setId(EDIT_TEXT_ID);
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
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.menu_new_folder),null, input);
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

	private void createFolder(String title) {
	
		log("createFolder");
		if (!Util.isOnline(this)){
			Snackbar.make(fragmentContainer,getString(R.string.error_server_connection_problem),Snackbar.LENGTH_LONG).show();
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		long parentHandle = -1;
		if(tabShown==CLOUD_TAB){
			if (cDriveExplorer != null){
				parentHandle = cDriveExplorer.getParentHandle();
				log("1)cDriveExplorer != null: " + parentHandle);
			}
			else{
				gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
				cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
				if (cDriveExplorer != null){
					parentHandle = cDriveExplorer.getParentHandle();
					log("2)cDriveExplorer != null: " + parentHandle);
				}	
			}
		}
		else if (tabShown == INCOMING_TAB){
			if (iSharesExplorer != null){
				parentHandle = iSharesExplorer.getParentHandle();
				log("1)iSharesExplorer != null: " + parentHandle);
			}
			else{
				gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
				iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
				if (iSharesExplorer != null){
					parentHandle = iSharesExplorer.getParentHandle();
					log("2)iSharesExplorer != null: " + parentHandle);
				}	
			}
		}
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
	
	public void setParentHandle (long parentHandle){
		this.gParentHandle = parentHandle;
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
	
	public static void log(String log) {
		Util.log("FileExplorerActivityLollipop", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError error) {
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (error.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer,getString(R.string.context_folder_created),Snackbar.LENGTH_LONG).show();
				long parentHandle;
				if(tabShown==CLOUD_TAB){
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
					if (cDriveExplorer != null){
						parentHandle = cDriveExplorer.getParentHandle();
						if (megaApi.getNodeByHandle(parentHandle) != null){
							nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()));
							cDriveExplorer.setNodes(nodes);
							cDriveExplorer.getListView().invalidate();
						}
					}						
				}
				else{
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
					iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
					if (iSharesExplorer != null){
						parentHandle = iSharesExplorer.getParentHandle();
						if (megaApi.getNodeByHandle(parentHandle) != null){
							nodes = megaApi.getChildren(megaApi.getNodeByHandle(iSharesExplorer.getParentHandle()));
							iSharesExplorer.setNodes(nodes);
							iSharesExplorer.getListView().invalidate();
						}
					}	
				}
			}
		}
		if (request.getType() == MegaRequest.TYPE_LOGIN){
			if (error.getErrorCode() != MegaError.API_OK) {
				//ERROR LOGIN
				String errorMessage;
				if (error.getErrorCode() == MegaError.API_ENOENT) {
					errorMessage = getString(R.string.error_incorrect_email_or_password);
				}
				else if (error.getErrorCode() == MegaError.API_ENOENT) {
					errorMessage = getString(R.string.error_server_connection_problem);
				}
				else if (error.getErrorCode() == MegaError.API_ESID){
					errorMessage = getString(R.string.error_server_expired_session);
				}
				else{
					errorMessage = error.getErrorString();
				}
				
				//Go to the login activity
				/*
				loginLoggingIn.setVisibility(View.GONE);
				loginLogin.setVisibility(View.VISIBLE);
				loginDelimiter.setVisibility(View.VISIBLE);
				loginCreateAccount.setVisibility(View.VISIBLE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);

				Snackbar.make(scrollView,errorMessage,Snackbar.LENGTH_LONG).show();*/
				
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				dbH.clearCredentials();
				if (dbH.getPreferences() != null){
					dbH.clearPreferences();
					dbH.setFirstTime(false);
				}
			}
			else{
				//LOGIN OK
				
				loginProgressBar.setVisibility(View.VISIBLE);
				loginFetchNodesProgressBar.setVisibility(View.GONE);
				loggingInText.setVisibility(View.VISIBLE);
				fetchingNodesText.setVisibility(View.VISIBLE);
				prepareNodesText.setVisibility(View.GONE);
				
				gSession = megaApi.dumpSession();
				credentials = new UserCredentials(lastEmail, gSession);

				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				dbH.clearCredentials();
				
				log("Logged in: " + gSession);
			
				megaApi.fetchNodes(this);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			if (error.getErrorCode() == MegaError.API_OK){
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				
				gSession = megaApi.dumpSession();
				lastEmail = megaApi.getMyUser().getEmail();
				credentials = new UserCredentials(lastEmail, gSession);
				
				dbH.saveCredentials(credentials);
				
				loginLoggingIn.setVisibility(View.GONE);

				afterLoginAndFetch();
			}	
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> updatedNodes) {
		log("onNodesUpdate");
		if (cDriveExplorer != null){
			if (megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()) != null){
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()));
				cDriveExplorer.setNodes(nodes);
				cDriveExplorer.getListView().invalidate();
			}
			else{
				if (megaApi.getRootNode() != null){
					cDriveExplorer.setParentHandle(megaApi.getRootNode().getHandle());
					nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()));
					cDriveExplorer.setNodes(nodes);
					cDriveExplorer.getListView().invalidate();
				}
			}
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeGlobalListener(this);
		}
		
		super.onDestroy();
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

	public long getParentHandleCloud() {
		return parentHandleCloud;
	}

	public void setParentHandleCloud(long parentHandleCloud) {
		this.parentHandleCloud = parentHandleCloud;
	}

	public long getParentHandleIncoming() {
		return parentHandleIncoming;
	}

	public void setParentHandleIncoming(long parentHandleIncoming) {
		this.parentHandleIncoming = parentHandleIncoming;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public boolean isSelectFile() {
		return selectFile;
	}

	public void setSelectFile(boolean selectFile) {
		this.selectFile = selectFile;
	}
}
