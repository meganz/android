package mega.privacy.android.app.providers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.TabsAdapter;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.components.MySwitch;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.providers.CloudDriveProviderFragmentLollipop;
import mega.privacy.android.app.lollipop.providers.IncomingSharesProviderFragmentLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;


@SuppressLint("NewApi") 
public class FileProviderActivity extends PinFileProviderActivity implements OnClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaTransferListenerInterface{
	
//	public static String ACTION_PROCESSED = "CreateLink.ACTION_PROCESSED";
//	
//	public static String ACTION_PICK_MOVE_FOLDER = "ACTION_PICK_MOVE_FOLDER";
//	public static String ACTION_PICK_COPY_FOLDER = "ACTION_PICK_COPY_FOLDER";
//	public static String ACTION_PICK_IMPORT_FOLDER = "ACTION_PICK_IMPORT_FOLDER";
//	public static String ACTION_SELECT_FOLDER = "ACTION_SELECT_FOLDER";
//	public static String ACTION_UPLOAD_SELFIE = "ACTION_UPLOAD_SELFIE";	
//	public static String ACTION_CHOOSE_MEGA_FOLDER_SYNC = "ACTION_CHOOSE_MEGA_FOLDER_SYNC";
	/*
	 * Select modes:
	 * UPLOAD - pick folder for upload
	 * MOVE - move files, folders
	 * CAMERA - pick folder for camera sync destination
	 */
	
//	public static int UPLOAD = 0;
//	public static int MOVE = 1;
//	public static int COPY = 2;
//	public static int CAMERA = 3;
//	public static int IMPORT = 4;
//	public static int SELECT = 5;
//	public static int UPLOAD_SELFIE = 6;
//	public static int SELECT_CAMERA_FOLDER = 7;
	
	private String lastEmail;
	private String lastPassword;
	private String gPublicKey;
	private String gPrivateKey;
	
	private MenuItem searchMenuItem;
	
	Toolbar tB;
    ActionBar aB;
	
	ScrollView scrollView;
	TextView newToMega;
	LinearLayout loginLogin;
	View loginDelimiter;
	LinearLayout loginCreateAccount;
	LinearLayout loginLoggingIn;
	TextView queryingSignupLinkText;
	TextView confirmingAccountText;
	ProgressBar loginProgressBar;
	ProgressBar loginFetchNodesProgressBar;
	TextView loggingInText;
	TextView fetchingNodesText;
	TextView prepareNodesText;
	TextView loginTitle;
	TextView generatingKeysText;
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
    float scaleText;
	Display display;
	EditText et_user;
	EditText et_password;
	TextView bRegisterLol;
	TextView bLoginLol;
	ImageView loginThreeDots;
	TextView loginABC;
	Switch loginSwitchLol;	
	MySwitch loginSwitch;
	Button bRegister;
	Button bLogin;
	
	public static int CLOUD_TAB = 0;
	public static int INCOMING_TAB = 1;
	
	String SD_CACHE_PATH = "/Android/data/mega.privacy.android.app/cache/files";

	private ImageView windowBack;
	private boolean backVisible = false;
	private TextView windowTitle;
	private ImageButton newFolderButton;
	
	private MegaApiAndroid megaApi;
	MegaApplication app;
//	private int mode;
	
	String actionBarTitle;
	private boolean folderSelected = false;
	
	private Handler handler;
	
	private int tabShown = -1;
	
	private CloudDriveProviderFragment cDriveProvider;
	private IncomingSharesProviderFragment iSharesProvider;
	
	private CloudDriveProviderFragmentLollipop cDriveProviderLol;
	private IncomingSharesProviderFragmentLollipop iSharesProviderLol;

	ProgressDialog statusDialog;
	
	private TabHost mTabHostProvider;
	TabsAdapter mTabsAdapterProvider;
    ViewPager viewPagerProvider; 
	
	ArrayList<MegaNode> nodes;
	
	long gParentHandle;
	String gcFTag = "";
	
	
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
		
		log("onCreate first");	
		
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {		
			onCreateLollipop(savedInstanceState);
		}
		else
		{
			onCreateOlder(savedInstanceState);
		}
	}
	
	protected void onCreateLollipop(Bundle savedInstanceState) {		
		
		log("onCreateLollipop");
	
		requestWindowFeature(Window.FEATURE_NO_TITLE);	

		super.onCreate(savedInstanceState);
			
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);

	    if (scaleH < scaleW){
	    	scaleText = scaleH;
	    }
	    else{
	    	scaleText = scaleW;
	    }
		
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		
		if (savedInstanceState != null){
			folderSelected = savedInstanceState.getBoolean("folderSelected", false);
		}		
		
		try{
			app = (MegaApplication) getApplication();
		}
		catch(Exception ex){
			finish();
		}
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		
		megaApi.addGlobalListener(this);
		megaApi.addTransferListener(this);
		
//		Intent intent = getIntent();
		checkLogin();
		UserCredentials credentials = dbH.getCredentials();
		if (credentials == null){
			
//			loginLogin.setVisibility(View.VISIBLE);
//			loginCreateAccount.setVisibility(View.VISIBLE);
//			loginDelimiter.setVisibility(View.VISIBLE);
//			loginLoggingIn.setVisibility(View.GONE);
//			generatingKeysText.setVisibility(View.GONE);
//			loggingInText.setVisibility(View.GONE);
//			fetchingNodesText.setVisibility(View.GONE);
//			prepareNodesText.setVisibility(View.GONE);
//			loginProgressBar.setVisibility(View.GONE);
//			queryingSignupLinkText.setVisibility(View.GONE);
//			confirmingAccountText.setVisibility(View.GONE);			
			
			loginLogin.setVisibility(View.VISIBLE);
			loginCreateAccount.setVisibility(View.INVISIBLE);
			loginDelimiter.setVisibility(View.VISIBLE);
			loginLoggingIn.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			loggingInText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			prepareNodesText.setVisibility(View.GONE);
			loginProgressBar.setVisibility(View.GONE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);				
		
		}
		else{
			log("dbH.getCredentials() NOT null");
			if (megaApi.getRootNode() == null){
				log("megaApi.getRootNode() == null");
				
				lastEmail = credentials.getEmail();
				String gSession = credentials.getSession();
				
				
				
				loginLogin.setVisibility(View.GONE);
				loginDelimiter.setVisibility(View.GONE);
				loginCreateAccount.setVisibility(View.GONE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				loginLoggingIn.setVisibility(View.VISIBLE);
//				generatingKeysText.setVisibility(View.VISIBLE);
				loginProgressBar.setVisibility(View.VISIBLE);
				loginFetchNodesProgressBar.setVisibility(View.GONE);
				loggingInText.setVisibility(View.VISIBLE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				megaApi.fastLogin(gSession, this);

			}
			else{
				setContentView(R.layout.activity_file_provider);
				tabShown = CLOUD_TAB;
				log("megaApi.getRootNode() NOT null");

				handler = new Handler();				

				//Set toolbar
				tB = (Toolbar) findViewById(R.id.toolbar_provider);
				setSupportActionBar(tB);
				aB = getSupportActionBar();
//				aB.setLogo(R.drawable.ic_action_navigation_accept_white);
				aB.setDisplayHomeAsUpEnabled(true);
				aB.setDisplayShowHomeEnabled(true);
				
				mTabHostProvider = (TabHost)findViewById(R.id.tabhost_provider);
				mTabHostProvider.setup();
				viewPagerProvider = (ViewPager) findViewById(R.id.provider_tabs_pager);  

				//Create tabs
				mTabHostProvider.setVisibility(View.VISIBLE);
				mTabHostProvider.getTabWidget().setDividerDrawable(null);

				if (mTabsAdapterProvider == null){
					mTabsAdapterProvider= new TabsAdapter(this, mTabHostProvider, viewPagerProvider);   	

					TabHost.TabSpec tabSpec3 = mTabHostProvider.newTabSpec("cloudProviderFragment");
					tabSpec3.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.section_cloud_drive).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout
					//tabSpec.setContent(contentID);
					//mTabHostContacts.addTab(tabSpec);
					TabHost.TabSpec tabSpec4 = mTabHostProvider.newTabSpec("incomingProviderFragment");
					tabSpec4.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.tab_incoming_shares).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout

					mTabsAdapterProvider.addTab(tabSpec3, CloudDriveProviderFragmentLollipop.class, null);
					mTabsAdapterProvider.addTab(tabSpec4, IncomingSharesProviderFragmentLollipop.class, null);
				}				
			
				mTabHostProvider.setOnTabChangedListener(new OnTabChangeListener(){
					@Override
					public void onTabChanged(String tabId) {
						log("TabId :"+ tabId);
						if(tabId.equals("cloudProviderFragment")){                     	
	
							tabShown=CLOUD_TAB;
							String cFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
							gcFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
							cDriveProviderLol = (CloudDriveProviderFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	
							if(cDriveProviderLol!=null){
								if(cDriveProviderLol.getParentHandle()==-1|| cDriveProviderLol.getParentHandle()==megaApi.getRootNode().getHandle()){
									aB.setTitle(getString(R.string.section_cloud_drive));											
								}
								else{
									aB.setTitle(megaApi.getNodeByHandle(cDriveProviderLol.getParentHandle()).getName());											
								}    					
							}	
						}
						else if(tabId.equals("incomingProviderFragment")){                     	
	
							tabShown=INCOMING_TAB;
	
							String cFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
							gcFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
							iSharesProviderLol = (IncomingSharesProviderFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	
							if(iSharesProviderLol!=null){
								if(iSharesProviderLol.getDeepBrowserTree()==0){
									aB.setTitle(getString(R.string.title_incoming_shares_explorer));
								}
								else{
									aB.setTitle(iSharesProviderLol.name);
								}    					
							}        			                      	
						}
					}
				});
	
				for (int i=0;i<mTabsAdapterProvider.getCount();i++){
					final int index = i;
					mTabHostProvider.getTabWidget().getChildAt(i).setOnClickListener(new OnClickListener() {
	
						@Override
						public void onClick(View v) {
							viewPagerProvider.setCurrentItem(index);							
						}
					});
				}			
	
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
			}
		}			
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelectedLollipop");
	
		int id = item.getItemId();
		switch(id){
			case android.R.id.home:{				
				this.onBackPressed();			}
		}
	    return super.onOptionsItemSelected(item);
	}

	protected void onCreateOlder(Bundle savedInstanceState) {
		log("onCreate first");
		super.onCreate(savedInstanceState);
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
		
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		
		if (savedInstanceState != null){
			folderSelected = savedInstanceState.getBoolean("folderSelected", false);
		}
		
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		try{
			app = (MegaApplication) getApplication();
		}
		catch(Exception ex){
			finish();
		}
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		
		megaApi.addGlobalListener(this);
		megaApi.addTransferListener(this);
		
		Intent intent = getIntent();		
		UserCredentials credentials = dbH.getCredentials();
		if (credentials == null){
			setContentView(R.layout.activity_login_returning);
			log("dbH.getCredentials() == null");
			
			loginTitle = (TextView) findViewById(R.id.login_text_view);
			loginLogin = (LinearLayout) findViewById(R.id.login_login_layout);
			loginLoggingIn = (LinearLayout) findViewById(R.id.login_logging_in_layout);
			loginCreateAccount = (LinearLayout) findViewById(R.id.login_create_account_layout);
			loginDelimiter = (View) findViewById(R.id.login_delimiter);
			loginProgressBar = (ProgressBar) findViewById(R.id.login_progress_bar);
			loginFetchNodesProgressBar = (ProgressBar) findViewById(R.id.login_fetching_nodes_bar);
			generatingKeysText = (TextView) findViewById(R.id.login_generating_keys_text);
			queryingSignupLinkText = (TextView) findViewById(R.id.login_query_signup_link_text);
			confirmingAccountText = (TextView) findViewById(R.id.login_confirm_account_text);
			loggingInText = (TextView) findViewById(R.id.login_logging_in_text);
			fetchingNodesText = (TextView) findViewById(R.id.login_fetch_nodes_text);
			prepareNodesText = (TextView) findViewById(R.id.login_prepare_nodes_text);
			
			loginTitle.setText(R.string.login_text);
			loginTitle.setTextSize(28*scaleH);
			
			loginLogin.setVisibility(View.VISIBLE);
			loginCreateAccount.setVisibility(View.INVISIBLE);
			loginDelimiter.setVisibility(View.VISIBLE);
			loginLoggingIn.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			loggingInText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			prepareNodesText.setVisibility(View.GONE);
			loginProgressBar.setVisibility(View.GONE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			
			et_user = (EditText) findViewById(R.id.login_email_text);
			et_password = (EditText) findViewById(R.id.login_password_text);
			et_password.setOnEditorActionListener(new OnEditorActionListener() {
				
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						submitForm();
						return true;
					}
					return false;
				}
			});		
			
			bLogin = (Button) findViewById(R.id.button_login_login);
			
			bLogin.setOnClickListener(this);
			
			loginLogin.setPadding(0, Util.px2dp((40*scaleH), outMetrics), 0, Util.px2dp((40*scaleH), outMetrics));
//			
//			bRegister = (Button) findViewById(R.id.button_create_account_login);			
//	
//			bRegister.setVisibility(View.GONE);
			
			((LinearLayout.LayoutParams)bLogin.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((3*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), Util.px2dp((5*scaleH), outMetrics));
			
			loginThreeDots = (ImageView) findViewById(R.id.login_three_dots);
			
			loginThreeDots.setPadding(0, Util.px2dp((20*scaleH), outMetrics), Util.px2dp((4*scaleW), outMetrics), Util.px2dp((3*scaleH), outMetrics));
			
			loginABC = (TextView) findViewById(R.id.ABC);
			
			((TableRow.LayoutParams)loginABC.getLayoutParams()).setMargins(0, 0, 0, Util.px2dp((5*scaleH), outMetrics));
			
			loginSwitch = (MySwitch) findViewById(R.id.switch_login);
			loginSwitch.setChecked(true);
			
			loginSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
							et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
							et_password.setSelection(et_password.getText().length());
					}else{
							et_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
							et_password.setSelection(et_password.getText().length());
				    }				
				}
			});
			
			((TableRow.LayoutParams)loginSwitch.getLayoutParams()).setMargins(Util.px2dp((1*scaleH), outMetrics), Util.px2dp((8*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0);
			
		}
		else{
			log("dbH.getCredentials() NOT null");
			if (megaApi.getRootNode() == null){
				setContentView(R.layout.activity_login_returning);
				log("megaApi.getRootNode() == null");
				lastEmail = credentials.getEmail();
				String gSession = credentials.getSession();
				
				loginTitle = (TextView) findViewById(R.id.login_text_view);
				loginLogin = (LinearLayout) findViewById(R.id.login_login_layout);
				loginLoggingIn = (LinearLayout) findViewById(R.id.login_logging_in_layout);
				loginCreateAccount = (LinearLayout) findViewById(R.id.login_create_account_layout);
				loginDelimiter = (View) findViewById(R.id.login_delimiter);
				loginProgressBar = (ProgressBar) findViewById(R.id.login_progress_bar);
				loginFetchNodesProgressBar = (ProgressBar) findViewById(R.id.login_fetching_nodes_bar);
				generatingKeysText = (TextView) findViewById(R.id.login_generating_keys_text);
				queryingSignupLinkText = (TextView) findViewById(R.id.login_query_signup_link_text);
				confirmingAccountText = (TextView) findViewById(R.id.login_confirm_account_text);
				loggingInText = (TextView) findViewById(R.id.login_logging_in_text);
				fetchingNodesText = (TextView) findViewById(R.id.login_fetch_nodes_text);
				prepareNodesText = (TextView) findViewById(R.id.login_prepare_nodes_text);
				
				loginTitle.setText(R.string.login_text);
				loginTitle.setTextSize(28*scaleH);
				
				loginLogin.setVisibility(View.VISIBLE);
				loginCreateAccount.setVisibility(View.INVISIBLE);
				loginDelimiter.setVisibility(View.VISIBLE);
				loginLoggingIn.setVisibility(View.GONE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				loginProgressBar.setVisibility(View.GONE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				
				log("session: " + gSession);
				loginLogin.setVisibility(View.GONE);
				loginDelimiter.setVisibility(View.GONE);
				loginCreateAccount.setVisibility(View.GONE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				loginLoggingIn.setVisibility(View.VISIBLE);
//				generatingKeysText.setVisibility(View.VISIBLE);
				loginProgressBar.setVisibility(View.VISIBLE);
				loginFetchNodesProgressBar.setVisibility(View.GONE);
				loggingInText.setVisibility(View.VISIBLE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				megaApi.fastLogin(gSession, this);
				
				
//				Intent loginIntent = new Intent(this, LoginActivity.class);
//				loginIntent.setAction(ManagerActivity.ACTION_FILE_PROVIDER);
//				if (intent != null){
//					if(intent.getExtras() != null)
//					{
//						loginIntent.putExtras(intent.getExtras());
//					}
//					
//					if(intent.getData() != null)
//					{
//						loginIntent.setData(intent.getData());
//					}
//				}
//				startActivity(loginIntent);
//				finish();
//				return;
			}
			else{
				setContentView(R.layout.activity_file_provider);
				tabShown = CLOUD_TAB;
				log("megaApi.getRootNode() NOT null");
			
		
				handler = new Handler();		
				/*	
				if ((intent != null) && (intent.getAction() != null)){
					if (intent.getAction().equals(ACTION_PICK_MOVE_FOLDER)){
						mode = MOVE;
						moveFromHandles = intent.getLongArrayExtra("MOVE_FROM");
						
						ArrayList<Long> list = new ArrayList<Long>(moveFromHandles.length);
						for (long n : moveFromHandles){
							list.add(n);
						}
						String cFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
						gcFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
						cDriveExplorer = (CloudDriveProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
						if(cDriveExplorer!=null){
							cDriveExplorer.setDisableNodes(list);
						}				
					}					
					else if (intent.getAction().equals(ACTION_PICK_COPY_FOLDER)){
						mode = COPY;
						copyFromHandles = intent.getLongArrayExtra("COPY_FROM");
						
						ArrayList<Long> list = new ArrayList<Long>(copyFromHandles.length);
						for (long n : copyFromHandles){
							list.add(n);
						}
						String cFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
						gcFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
						cDriveExplorer = (CloudDriveProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
						if(cDriveExplorer!=null){
							cDriveExplorer.setDisableNodes(list);
						}
					}
					else if (intent.getAction().equals(ACTION_CHOOSE_MEGA_FOLDER_SYNC)){
						log("action = ACTION_CHOOSE_MEGA_FOLDER_SYNC");
						mode = SELECT_CAMERA_FOLDER;
					}	
					else if (intent.getAction().equals(ACTION_PICK_IMPORT_FOLDER)){
						mode = IMPORT;
					}
					else if (intent.getAction().equals(ACTION_SELECT_FOLDER)){
						mode = SELECT;
						selectedContacts=intent.getStringArrayExtra("SELECTED_CONTACTS");			
						
					}
					else if(intent.getAction().equals(ACTION_UPLOAD_SELFIE)){
						mode = UPLOAD_SELFIE;
						imagePath=intent.getStringExtra("IMAGE_PATH");
					}
				}*/
			
				mTabHostProvider = (TabHost)findViewById(R.id.tabhost_provider);
				mTabHostProvider.setup();
		        viewPagerProvider = (ViewPager) findViewById(R.id.provider_tabs_pager);  
		        
		        //Create tabs
		        mTabHostProvider.getTabWidget().setBackgroundColor(Color.BLACK);
				
		        mTabHostProvider.setVisibility(View.VISIBLE);    			
				
				
				if (mTabsAdapterProvider == null){
					mTabsAdapterProvider= new TabsAdapter(this, mTabHostProvider, viewPagerProvider);   	
					
					TabHost.TabSpec tabSpec3 = mTabHostProvider.newTabSpec("cloudProviderFragment");
					tabSpec3.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.section_cloud_drive).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout
			        //tabSpec.setContent(contentID);
			        //mTabHostContacts.addTab(tabSpec);
			        TabHost.TabSpec tabSpec4 = mTabHostProvider.newTabSpec("incomingProviderFragment");
			        tabSpec4.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.tab_incoming_shares).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout
			                	          				
					mTabsAdapterProvider.addTab(tabSpec3, CloudDriveProviderFragment.class, null);
					mTabsAdapterProvider.addTab(tabSpec4, IncomingSharesProviderFragment.class, null);
					
				}
			
				mTabHostProvider.setOnTabChangedListener(new OnTabChangeListener(){
		            @Override
		            public void onTabChanged(String tabId) {
		            	log("TabId :"+ tabId);
		                if(tabId.equals("cloudProviderFragment")){                     	
		
		     				tabShown=CLOUD_TAB;
		    				String cFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
		    				gcFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
		    				cDriveProvider = (CloudDriveProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
		
		    				if(cDriveProvider!=null){
		    					if(cDriveProvider.getParentHandle()==-1|| cDriveProvider.getParentHandle()==megaApi.getRootNode().getHandle()){
		    						changeTitle(getString(R.string.section_cloud_drive));
		    						changeBackVisibility(false);
		    					}
		    					else{
		    						changeTitle(megaApi.getNodeByHandle(cDriveProvider.getParentHandle()).getName());
		    						changeBackVisibility(true);
		    					}    					
		    				}	
		                }
		                else if(tabId.equals("incomingProviderFragment")){                     	
		
		            		tabShown=INCOMING_TAB;
		            		
		            		String cFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
		            		gcFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
		    				iSharesProvider = (IncomingSharesProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
		    		
		    				if(iSharesProvider!=null){
		    					if(iSharesProvider.getDeepBrowserTree()==0){
		    						changeTitle(getString(R.string.title_incoming_shares_explorer));
		    						changeBackVisibility(false);
		    					}
		    					else{
		    						changeTitle(iSharesProvider.name);
		    						changeBackVisibility(true);
		    					}    					
		    				}        			                      	
		                }
		             }
				});
				
				for (int i=0;i<mTabsAdapterProvider.getCount();i++){
					final int index = i;
					mTabHostProvider.getTabWidget().getChildAt(i).setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							viewPagerProvider.setCurrentItem(index);							
						}
					});
				}
				
				newFolderButton = (ImageButton) findViewById(R.id.file_explorer_new_folder);
				newFolderButton.setVisibility(View.GONE);
		//		newFolderButton.setOnClickListener(this);
				
				windowTitle = (TextView) findViewById(R.id.file_explorer_window_title);
				actionBarTitle = getString(R.string.section_cloud_drive);
				windowTitle.setText(actionBarTitle);
				
				windowBack = (ImageView) findViewById(R.id.file_explorer_back);
				windowBack.setOnClickListener(this);
				windowTitle.setOnClickListener(this);	
				
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
			}			
		}	
	}
	
	@SuppressLint("NewApi")
	public void checkLogin(){
		setContentView(R.layout.activity_login);
		
		scrollView = (ScrollView) findViewById(R.id.scroll_view_login);		
		
	    loginTitle = (TextView) findViewById(R.id.login_text_view);
		//Left margin
		LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)loginTitle.getLayoutParams();
		textParams.setMargins(Util.scaleHeightPx(30, outMetrics), Util.scaleHeightPx(40, outMetrics), 0, Util.scaleHeightPx(20, outMetrics)); 
		loginTitle.setLayoutParams(textParams);
		
		loginTitle.setText(R.string.login_text);
		loginTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (22*scaleText));
		
		et_user = (EditText) findViewById(R.id.login_email_text);
		android.view.ViewGroup.LayoutParams paramsb1 = et_user.getLayoutParams();		
		paramsb1.width = Util.scaleWidthPx(280, outMetrics);		
		et_user.setLayoutParams(paramsb1);
		//Left margin
		textParams = (LinearLayout.LayoutParams)et_user.getLayoutParams();
		textParams.setMargins(Util.scaleWidthPx(30, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics)); 
		et_user.setLayoutParams(textParams);		
		
		et_password = (EditText) findViewById(R.id.login_password_text);	
		et_password.setLayoutParams(paramsb1);
		et_password.setLayoutParams(textParams);	
		
		et_password.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					submitForm();
					return true;
				}
				return false;
			}
		});			
		loginThreeDots = (ImageView) findViewById(R.id.login_three_dots);
		LinearLayout.LayoutParams textThreeDots = (LinearLayout.LayoutParams)loginThreeDots.getLayoutParams();
		textThreeDots.setMargins(Util.scaleWidthPx(30, outMetrics), 0, Util.scaleWidthPx(10, outMetrics), 0); 
		loginThreeDots.setLayoutParams(textThreeDots);
		
		loginABC = (TextView) findViewById(R.id.ABC);
		
		loginSwitchLol = (Switch) findViewById(R.id.switch_login);
		LinearLayout.LayoutParams switchParams = (LinearLayout.LayoutParams)loginSwitchLol.getLayoutParams();
		switchParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0); 
		loginSwitchLol.setLayoutParams(switchParams);
		loginSwitchLol.setChecked(false);
		
		loginSwitchLol.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(!isChecked){
						et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
						et_password.setSelection(et_password.getText().length());
				}else{
						et_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
						et_password.setSelection(et_password.getText().length());
			    }				
			}
		});
		
		bLoginLol = (TextView) findViewById(R.id.button_login_login);
		bLoginLol.setText(getString(R.string.login_text).toUpperCase(Locale.getDefault()));
//		android.view.ViewGroup.LayoutParams paramsbLogin = bLoginLol.getLayoutParams();		
//		paramsbLogin.height = Util.scaleHeightPx(48, outMetrics);
//		paramsbLogin.width = Util.scaleWidthPx(63, outMetrics);
//		bLoginLol.setLayoutParams(paramsbLogin);
		//Margin
		LinearLayout.LayoutParams textParamsLogin = (LinearLayout.LayoutParams)bLoginLol.getLayoutParams();
		textParamsLogin.setMargins(Util.scaleWidthPx(35, outMetrics), Util.scaleHeightPx(40, outMetrics), 0, Util.scaleHeightPx(80, outMetrics)); 
		bLoginLol.setLayoutParams(textParamsLogin);
		
		bLoginLol.setOnClickListener(this);
		
		loginDelimiter = (View) findViewById(R.id.login_delimiter);
		loginCreateAccount = (LinearLayout) findViewById(R.id.login_create_account_layout);
		loginCreateAccount.setVisibility(View.INVISIBLE);
		
	    newToMega = (TextView) findViewById(R.id.text_newToMega);
		//Margins (left, top, right, bottom)
		LinearLayout.LayoutParams textnewToMega = (LinearLayout.LayoutParams)newToMega.getLayoutParams();
		textnewToMega.setMargins(Util.scaleHeightPx(30, outMetrics), Util.scaleHeightPx(20, outMetrics), 0, Util.scaleHeightPx(30, outMetrics)); 
		newToMega.setLayoutParams(textnewToMega);	
		newToMega.setTextSize(TypedValue.COMPLEX_UNIT_SP, (22*scaleText));
		
	    bRegisterLol = (TextView) findViewById(R.id.button_create_account_login);
	    
	    bRegisterLol.setText(getString(R.string.create_account).toUpperCase(Locale.getDefault()));
//		android.view.ViewGroup.LayoutParams paramsb2 = bRegisterLol.getLayoutParams();		
//		paramsb2.height = Util.scaleHeightPx(48, outMetrics);
//		paramsb2.width = Util.scaleWidthPx(144, outMetrics);
//		bRegisterLol.setLayoutParams(paramsb2);
		//Margin
		LinearLayout.LayoutParams textParamsRegister = (LinearLayout.LayoutParams)bRegisterLol.getLayoutParams();
		textParamsRegister.setMargins(Util.scaleWidthPx(35, outMetrics), 0, 0, 0); 
		bRegisterLol.setLayoutParams(textParamsRegister);
	    
	    bRegisterLol.setOnClickListener(this);
		
		loginLogin = (LinearLayout) findViewById(R.id.login_login_layout);
		loginLoggingIn = (LinearLayout) findViewById(R.id.login_logging_in_layout);
		loginProgressBar = (ProgressBar) findViewById(R.id.login_progress_bar);
		loginFetchNodesProgressBar = (ProgressBar) findViewById(R.id.login_fetching_nodes_bar);
		generatingKeysText = (TextView) findViewById(R.id.login_generating_keys_text);
		queryingSignupLinkText = (TextView) findViewById(R.id.login_query_signup_link_text);
		confirmingAccountText = (TextView) findViewById(R.id.login_confirm_account_text);
		loggingInText = (TextView) findViewById(R.id.login_logging_in_text);
		fetchingNodesText = (TextView) findViewById(R.id.login_fetch_nodes_text);
		prepareNodesText = (TextView) findViewById(R.id.login_prepare_nodes_text);	
		
	}
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_fileprovider, menu);
	    getSupportActionBar().setDisplayShowCustomEnabled(true);
	    
	    final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchMenuItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		
		if (searchView != null){
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setIconifiedByDefault(true);
		}
		
		return super.onCreateOptionsMenu(menu);
	}
	
	private View getTabIndicator(Context context, String title) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);

        TextView tv = (TextView) view.findViewById(R.id.textView);
        tv.setText(title);
        return view;
    }
	
	public void changeTitle (String title){
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
			if (aB != null){
				aB.setTitle(title);
			}
		}
		else
		{
			if (windowTitle != null){
				windowTitle.setText(title);
			}
		}
	}
	
	public void changeBackVisibility(boolean backVisible){
		this.backVisible = backVisible;
		if (windowBack != null){
			if (!backVisible){
				windowBack.setVisibility(View.INVISIBLE);
			}
			else{
				windowBack.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private String getFragmentTag(int viewPagerId, int fragmentPosition)
	{
	     return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
	}
	
	public void finishActivity(){
		finish();
	}
	
	public void downloadTo(long size, long [] hashes){
		
		log("downloadTo");
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
		                ManagerActivityLollipop.REQUEST_WRITE_STORAGE);
			}
		}
		
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_preparing_provider));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;

		File destination = null;	
		
		destination=getCacheDir();
		String pathToDownload = destination.getPath();
				
		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(destination.getPath());
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}		

		if (hashes != null&&hashes.length>0){			

			for (long hash : hashes) {

				MegaNode tempNode = megaApi.getNodeByHandle(hash);

				String localPath = Util.getLocalFile(this, tempNode.getName(), tempNode.getSize(), pathToDownload);

				if(localPath != null){	
					try { 
						log("COPY_FILE");
						File fileToShare = new File(pathToDownload, tempNode.getName());
						Util.copyFile(new File(localPath), fileToShare); 
						
						if(fileToShare.exists()){
							Uri contentUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", fileToShare);
							grantUriPermission("*", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
							log("CONTENT URI: "+contentUri);
							//Send it
							Intent result = new Intent();
							result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							result.setData(contentUri);
							result.setAction(Intent.ACTION_GET_CONTENT);
							
							
							if (getParent() == null) {
							    setResult(Activity.RESULT_OK, result);
							} else {
							    getParent().setResult(Activity.RESULT_OK, result);
							}

							finish();	
						}
					
					}
					catch(Exception e) {}
				}


				if(tempNode != null){
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					dlFiles.put(tempNode, pathToDownload);

					for (MegaNode document : dlFiles.keySet()) {

						String path = dlFiles.get(document);

						if(availableFreeSpace < document.getSize()){
							Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, this);
							continue;
						}

						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
						service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						service.putExtra(DownloadService.EXTRA_OPEN_FILE, false);
						startService(service);
					}
				}
			}
//			}
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		bundle.putBoolean("folderSelected", folderSelected);
		super.onSaveInstanceState(bundle);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

	}
	
	@Override
	public void onBackPressed() {
		log("onBackPressed: "+tabShown);
		
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {	
			onBackPressedLollipop();
		}
		else{
		
			if(tabShown==CLOUD_TAB){
				String cFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
				gcFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
				cDriveProvider = (CloudDriveProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
		
				if(cDriveProvider!=null){
					if (cDriveProvider.onBackPressed() == 0){
						super.onBackPressed();
						return;
					}
				}
			}
			else if(tabShown==INCOMING_TAB){
				String cFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
				gcFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
				iSharesProvider = (IncomingSharesProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
			
				if(iSharesProvider!=null){
					if (iSharesProvider.onBackPressed() == 0){
						super.onBackPressed();
						return;
					}
				}
			}
			else{
				super.onBackPressed();
			}
		}
	}
	
	public void onBackPressedLollipop() {
		log("onBackPressedLollipop: "+tabShown);

		if(tabShown==CLOUD_TAB){
			String cFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
			gcFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
			cDriveProviderLol = (CloudDriveProviderFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	
			if(cDriveProviderLol!=null){
				if (cDriveProviderLol.onBackPressed() == 0){
					super.onBackPressed();
					return;
				}
			}
		}
		else if(tabShown==INCOMING_TAB){
			String cFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
			gcFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
			iSharesProviderLol = (IncomingSharesProviderFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		
			if(iSharesProviderLol!=null){
				if (iSharesProviderLol.onBackPressed() == 0){
					super.onBackPressed();
					return;
				}
			}
		}
		else{
			super.onBackPressed();
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.button_login_login:{
//				loginClicked = true;
				onLoginClick(v);
				break;
			}
			case R.id.file_explorer_back:{
				onBackPressed();
				break;
			}
			case R.id.file_explorer_window_title:{
				if (backVisible){
					onBackPressed();
					break;
				}
			}
		}
	}
	
	public void onLoginClick(View v){
		submitForm();
	}
	
	/*
	 * Validate email
	 */
	private String getEmailError() {
		String value = et_user.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_email);
		}
		if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
			return getString(R.string.error_invalid_email);
		}
		return null;
	}
	
	/*
	 * Validate password
	 */
	private String getPasswordError() {
		String value = et_password.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		}
		return null;
	}
	
	private boolean validateForm() {
		String emailError = getEmailError();
		String passwordError = getPasswordError();

		et_user.setError(emailError);
		et_password.setError(passwordError);

		if (emailError != null) {
			et_user.requestFocus();
			return false;
		} else if (passwordError != null) {
			et_password.requestFocus();
			return false;
		}
		return true;
	}
	
	private void submitForm() {
		if (!validateForm()) {
			return;
		}
		
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(et_user.getWindowToken(), 0);
		
		if(!Util.isOnline(this))
		{
			loginLoggingIn.setVisibility(View.GONE);
			loginLogin.setVisibility(View.VISIBLE);
			loginDelimiter.setVisibility(View.VISIBLE);
			loginCreateAccount.setVisibility(View.INVISIBLE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			loggingInText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			prepareNodesText.setVisibility(View.GONE);
			
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem),false, this);
			return;
		}
		
		loginLogin.setVisibility(View.GONE);
		loginDelimiter.setVisibility(View.GONE);
		loginCreateAccount.setVisibility(View.GONE);
		loginLoggingIn.setVisibility(View.VISIBLE);
		generatingKeysText.setVisibility(View.VISIBLE);
		loginProgressBar.setVisibility(View.VISIBLE);
		loginFetchNodesProgressBar.setVisibility(View.GONE);
		queryingSignupLinkText.setVisibility(View.GONE);
		confirmingAccountText.setVisibility(View.GONE);
		
		lastEmail = et_user.getText().toString().toLowerCase(Locale.ENGLISH).trim();
		lastPassword = et_password.getText().toString();
		
		log("generating keys");
		
		new HashTask().execute(lastEmail, lastPassword);
	}
	
	/*
	 * Task to process email and password
	 */
	private class HashTask extends AsyncTask<String, Void, String[]> {

		@Override
		protected String[] doInBackground(String... args) {
			String privateKey = megaApi.getBase64PwKey(args[1]);
			String publicKey = megaApi.getStringHash(privateKey, args[0]);
			return new String[]{new String(privateKey), new String(publicKey)}; 
		}

		
		@Override
		protected void onPostExecute(String[] key) {
			onKeysGenerated(key[0], key[1]);
		}

	}
	
	private void onKeysGenerated(String privateKey, String publicKey) {
		log("key generation finished");

		this.gPrivateKey = privateKey;
		this.gPublicKey = publicKey;		

		onKeysGeneratedLogin(privateKey, publicKey);
	}
	
	private void onKeysGeneratedLogin(final String privateKey, final String publicKey) {
		
		if(!Util.isOnline(this)){
			loginLoggingIn.setVisibility(View.GONE);
			loginLogin.setVisibility(View.VISIBLE);
			loginDelimiter.setVisibility(View.VISIBLE);
			loginCreateAccount.setVisibility(View.INVISIBLE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			loggingInText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			prepareNodesText.setVisibility(View.GONE);
			
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		loggingInText.setVisibility(View.VISIBLE);
		fetchingNodesText.setVisibility(View.GONE);
		prepareNodesText.setVisibility(View.GONE);
		
		log("fastLogin con publicKey y privateKey");
		megaApi.fastLogin(lastEmail, publicKey, privateKey, this);
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
	
	public int getStatusBarHeight() { 
	      int result = 0;
	      int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
	      if (resourceId > 0) {
	          result = getResources().getDimensionPixelSize(resourceId);
	      } 
	      return result;
	}
	
	public static void log(String log) {
		Util.log("FileProviderActivity", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart");
	}
	
	String gSession = null;
	UserCredentials credentials = null;

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: "+request.getFile());
		if (request.getType() == MegaRequest.TYPE_LOGIN){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() != MegaError.API_OK) {
				String errorMessage;
				if (e.getErrorCode() == MegaError.API_ENOENT) {
					errorMessage = getString(R.string.error_incorrect_email_or_password);
				}
				else if (e.getErrorCode() == MegaError.API_ENOENT) {
					errorMessage = getString(R.string.error_server_connection_problem);
				}
				else if (e.getErrorCode() == MegaError.API_ESID){
					errorMessage = getString(R.string.error_server_expired_session);
				}
				else{
					errorMessage = e.getErrorString();
				}
				loginLoggingIn.setVisibility(View.GONE);
				loginLogin.setVisibility(View.VISIBLE);
				loginDelimiter.setVisibility(View.VISIBLE);
				loginCreateAccount.setVisibility(View.INVISIBLE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				
				Util.showErrorAlertDialog(errorMessage, false, this);
				
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				if (dbH.getPreferences() != null){
					dbH.clearPreferences();
					dbH.setFirstTime(false);
					Intent stopIntent = null;
					stopIntent = new Intent(this, CameraSyncService.class);
					stopIntent.setAction(CameraSyncService.ACTION_LOGOUT);
					startService(stopIntent);
				}
			}
			else{

				loginProgressBar.setVisibility(View.VISIBLE);
				loginFetchNodesProgressBar.setVisibility(View.GONE);
				loggingInText.setVisibility(View.VISIBLE);
				fetchingNodesText.setVisibility(View.VISIBLE);
				prepareNodesText.setVisibility(View.GONE);
				
				gSession = megaApi.dumpSession();
				if (lastEmail != null){
					credentials = new UserCredentials(lastEmail, gSession);
				}
				
//				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
//				dbH.clearCredentials();
//				dbH.saveCredentials(credentials);
				
				log("Logged in");
				
				megaApi.fetchNodes(this);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			if (e.getErrorCode() != MegaError.API_OK) {
				String errorMessage;
				errorMessage = e.getErrorString();
				loginLoggingIn.setVisibility(View.GONE);
				loginLogin.setVisibility(View.VISIBLE);
				loginDelimiter.setVisibility(View.VISIBLE);
				loginCreateAccount.setVisibility(View.INVISIBLE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);

				Util.showErrorAlertDialog(errorMessage, false, this);
			}
			else{
				
				if (credentials != null){
					DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
					dbH.clearCredentials();
					dbH.saveCredentials(credentials);
				}
				
				setContentView(R.layout.activity_file_provider);
				tabShown = CLOUD_TAB;
				log("megaApi.getRootNode() NOT null");

				mTabHostProvider = (TabHost)findViewById(R.id.tabhost_provider);
				mTabHostProvider.setup();
				viewPagerProvider = (ViewPager) findViewById(R.id.provider_tabs_pager);  

				//Create tabs
				mTabHostProvider.getTabWidget().setBackgroundColor(Color.BLACK);

				mTabHostProvider.setVisibility(View.VISIBLE);    			

				if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {

					//Set toolbar
					tB = (Toolbar) findViewById(R.id.toolbar_provider);
					
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tB.getLayoutParams();
					params.setMargins(0, getStatusBarHeight(), 0, 0);
					
					setSupportActionBar(tB);
					aB = getSupportActionBar();
					aB.setDisplayHomeAsUpEnabled(true);
					aB.setDisplayShowHomeEnabled(true);
					
					//Lollipop Version
					if (mTabsAdapterProvider == null){
						mTabsAdapterProvider= new TabsAdapter(this, mTabHostProvider, viewPagerProvider);   	

						TabHost.TabSpec tabSpec3 = mTabHostProvider.newTabSpec("cloudProviderFragment");
						tabSpec3.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.section_cloud_drive).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout
						//tabSpec.setContent(contentID);
						//mTabHostContacts.addTab(tabSpec);
						TabHost.TabSpec tabSpec4 = mTabHostProvider.newTabSpec("incomingProviderFragment");
						tabSpec4.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.tab_incoming_shares).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout

						mTabsAdapterProvider.addTab(tabSpec3, CloudDriveProviderFragmentLollipop.class, null);
						mTabsAdapterProvider.addTab(tabSpec4, IncomingSharesProviderFragmentLollipop.class, null);

					}

					mTabHostProvider.setOnTabChangedListener(new OnTabChangeListener(){
						@Override
						public void onTabChanged(String tabId) {
							log("TabId :"+ tabId);
							if(tabId.equals("cloudProviderFragment")){                     	

								tabShown=CLOUD_TAB;
								String cFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
								gcFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
								cDriveProviderLol = (CloudDriveProviderFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);

								if(cDriveProviderLol!=null){
									if(cDriveProviderLol.getParentHandle()==-1|| cDriveProviderLol.getParentHandle()==megaApi.getRootNode().getHandle()){
										aB.setTitle(getString(R.string.section_cloud_drive));
									}
									else{
										aB.setTitle(megaApi.getNodeByHandle(cDriveProviderLol.getParentHandle()).getName());
									}    					
								}	
							}
							else if(tabId.equals("incomingProviderFragment")){                     	

								tabShown=INCOMING_TAB;

								String cFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
								gcFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
								iSharesProviderLol = (IncomingSharesProviderFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);

								if(iSharesProviderLol!=null){
									if(iSharesProviderLol.getDeepBrowserTree()==0){
										aB.setTitle(getString(R.string.title_incoming_shares_explorer));
									}
									else{
										aB.setTitle(iSharesProviderLol.name);
									}    					
								}        			                      	
							}
						}
					});
					
					getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
					getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
				}
				else{
					//NOT Lollipop
					if (mTabsAdapterProvider == null){
						mTabsAdapterProvider= new TabsAdapter(this, mTabHostProvider, viewPagerProvider);   	

						TabHost.TabSpec tabSpec3 = mTabHostProvider.newTabSpec("cloudProviderFragment");
						tabSpec3.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.section_cloud_drive).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout
						//tabSpec.setContent(contentID);
						//mTabHostContacts.addTab(tabSpec);
						TabHost.TabSpec tabSpec4 = mTabHostProvider.newTabSpec("incomingProviderFragment");
						tabSpec4.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.tab_incoming_shares).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout

						mTabsAdapterProvider.addTab(tabSpec3, CloudDriveProviderFragment.class, null);
						mTabsAdapterProvider.addTab(tabSpec4, IncomingSharesProviderFragment.class, null);

					}

					mTabHostProvider.setOnTabChangedListener(new OnTabChangeListener(){
						@Override
						public void onTabChanged(String tabId) {
							log("TabId :"+ tabId);
							if(tabId.equals("cloudProviderFragment")){                     	

								tabShown=CLOUD_TAB;
								String cFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
								gcFTag = getFragmentTag(R.id.provider_tabs_pager, 0);
								cDriveProvider = (CloudDriveProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);

								if(cDriveProvider!=null){
									if(cDriveProvider.getParentHandle()==-1|| cDriveProvider.getParentHandle()==megaApi.getRootNode().getHandle()){
										changeTitle(getString(R.string.section_cloud_drive));
										changeBackVisibility(false);
									}
									else{
										changeTitle(megaApi.getNodeByHandle(cDriveProvider.getParentHandle()).getName());
										changeBackVisibility(true);
									}    					
								}	
							}
							else if(tabId.equals("incomingProviderFragment")){                     	

								tabShown=INCOMING_TAB;

								String cFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
								gcFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
								iSharesProvider = (IncomingSharesProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);

								if(iSharesProvider!=null){
									if(iSharesProvider.getDeepBrowserTree()==0){
										changeTitle(getString(R.string.title_incoming_shares_explorer));
//										changeTitle(getString(R.string.title_incoming_shares_explorer));
										changeBackVisibility(false);
									}
									else{
										changeTitle(iSharesProvider.name);
//										changeTitle(iSharesProvider.name);
										changeBackVisibility(true);
									}    					
								}        			                      	
							}
						}
					});
				}


				for (int i=0;i<mTabsAdapterProvider.getCount();i++){
					final int index = i;
					mTabHostProvider.getTabWidget().getChildAt(i).setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							viewPagerProvider.setCurrentItem(index);							
						}
					});
				}

				newFolderButton = (ImageButton) findViewById(R.id.file_explorer_new_folder);
				if (newFolderButton != null){
					newFolderButton.setVisibility(View.GONE);
				}
				//		newFolderButton.setOnClickListener(this);

				windowTitle = (TextView) findViewById(R.id.file_explorer_window_title);
				if (windowTitle != null){
					windowTitle.setText(actionBarTitle);
				}
				
				if (actionBarTitle != null){
					actionBarTitle = getString(R.string.section_cloud_drive);
				}
				
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
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
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {	
			if (cDriveProviderLol != null){
				if (megaApi.getNodeByHandle(cDriveProviderLol.getParentHandle()) != null){
					nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveProviderLol.getParentHandle()));
					cDriveProviderLol.setNodes(nodes);
					cDriveProviderLol.getListView().invalidate();
				}
			}
		}
		else{
			if (cDriveProvider != null){
				if (megaApi.getNodeByHandle(cDriveProvider.getParentHandle()) != null){
					nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveProvider.getParentHandle()));
					cDriveProvider.setNodes(nodes);
					cDriveProvider.getListView().invalidateViews();
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
			megaApi.removeRequestListener(this);
			megaApi.removeTransferListener(this);
			megaApi.removeGlobalListener(this);
		}
		
		super.onDestroy();
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		log("onTransferFinish: "+transfer.getPath());
		
		//Get the URI of the file
		File fileToShare = new File(transfer.getPath());
//		File newFile = new File(fileToShare, "default_image.jpg");
		Uri contentUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", fileToShare);
		grantUriPermission("*", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
		log("CONTENT URI: "+contentUri);
		//Send it
		Intent result = new Intent();
		result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		result.setData(contentUri);
		result.setAction(Intent.ACTION_GET_CONTENT);
		
		
		if (getParent() == null) {
		    setResult(Activity.RESULT_OK, result);
		} else {
			Toast.makeText(this, "ENTROOO parent no null", Toast.LENGTH_LONG).show();
		    getParent().setResult(Activity.RESULT_OK, result);
		}

		finish();		
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {

		//Answer to the Intent GET_CONTENT with null
		
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer,
			byte[] buffer) {
		// TODO Auto-generated method stub
		return false;
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
