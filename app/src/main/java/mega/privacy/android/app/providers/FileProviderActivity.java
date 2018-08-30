package mega.privacy.android.app.providers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.components.MySwitch;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.providers.CloudDriveProviderFragmentLollipop;
import mega.privacy.android.app.lollipop.providers.IncomingSharesProviderFragmentLollipop;
import mega.privacy.android.app.lollipop.providers.ProviderPageAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;


@SuppressLint("NewApi") 
public class FileProviderActivity extends PinFileProviderActivity implements OnClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaTransferListenerInterface, MegaChatRequestListenerInterface {
	
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


	private MenuItem searchMenuItem;

	CountDownTimer timer;
	
	Toolbar tB;
    ActionBar aB;
	
	ScrollView scrollView;
	TextView newToMega;
	LinearLayout loginLogin;
	LinearLayout loginCreateAccount;
	LinearLayout loginLoggingIn;
	TextView queryingSignupLinkText;
	TextView confirmingAccountText;
	ProgressBar loginProgressBar;
	ProgressBar loginFetchNodesProgressBar;
	TextView loggingInText;
	TextView fetchingNodesText;
	TextView prepareNodesText;
	TextView serversBusyText;
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
	SwitchCompat loginSwitchLol;
	MySwitch loginSwitch;
	Button bRegister;
	Button bLogin;
	RelativeLayout relativeLayout;
	
	public static int CLOUD_TAB = 0;
	public static int INCOMING_TAB = 1;
	
	String SD_CACHE_PATH = "/Android/data/mega.privacy.android.app/cache/files";

	private MegaApiAndroid megaApi;
	private MegaChatApiAndroid megaChatApi;
	MegaApplication app;
//	private int mode;

	ChatSettings chatSettings;

	private boolean folderSelected = false;

	private int tabShown = -1;

	private CloudDriveProviderFragmentLollipop cDriveProviderLol;
	private IncomingSharesProviderFragmentLollipop iSharesProviderLol;

	ProgressDialog statusDialog;

	LinearLayout optionsBar;
	Button cancelButton;
	Button attachButton;

	TabLayout tabLayoutProvider;
	LinearLayout providerSectionLayout;
	ProviderPageAdapter mTabsAdapterProvider;
	ViewPager viewPagerProvider;

	ArrayList<MegaNode> nodes;
	int incomingDeepBrowserTree = -1;
	long gParentHandle=-1;
	long incParentHandle=-1;
	String gcFTag = "";


    List<MegaNode> selectedNodes;
    int totalTransfers = 0;
	int progressTransfersFinish = 0;
	ClipData clipDataTransfers;
	ArrayList<Uri> contentUris = new ArrayList<>();
	
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
		relativeLayout = (RelativeLayout) findViewById(R.id.provider_container);

		if (savedInstanceState != null){
			folderSelected = savedInstanceState.getBoolean("folderSelected", false);
			incParentHandle = savedInstanceState.getLong("incParentHandle", -1);
			gParentHandle = savedInstanceState.getLong("parentHandle", -1);
			incomingDeepBrowserTree = savedInstanceState.getInt("deepBrowserTree", -1);
			tabShown = savedInstanceState.getInt("tabShown", CLOUD_TAB);
		}

		try{
			app = (MegaApplication) getApplication();
		}
		catch(Exception ex){
			finish();
		}

		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		megaChatApi = ((MegaApplication)getApplication()).getMegaChatApi();

		megaApi.addGlobalListener(this);
		megaApi.addTransferListener(this);

//		Intent intent = getIntent();
		checkLogin();
		UserCredentials credentials = dbH.getCredentials();
		if (credentials == null){
			loginLogin.setVisibility(View.VISIBLE);
			if(scrollView!=null){
				scrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_create_account));
			}
			loginCreateAccount.setVisibility(View.INVISIBLE);
			loginLoggingIn.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			loggingInText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			prepareNodesText.setVisibility(View.GONE);
			if(serversBusyText!=null){
				serversBusyText.setVisibility(View.GONE);
			}
			loginProgressBar.setVisibility(View.GONE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
		}
		else{

			log("dbH.getCredentials() NOT null");

			if (megaApi.getRootNode() == null){

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Window window = this.getWindow();
					window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
					window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
					window.setStatusBarColor(ContextCompat.getColor(this, R.color.transparent_black));
				}

				log("megaApi.getRootNode() == null");

				lastEmail = credentials.getEmail();
				String gSession = credentials.getSession();

				if (!MegaApplication.isLoggingIn()){
					MegaApplication.setLoggingIn(true);
					loginLogin.setVisibility(View.GONE);
					loginCreateAccount.setVisibility(View.GONE);
					queryingSignupLinkText.setVisibility(View.GONE);
					confirmingAccountText.setVisibility(View.GONE);
					loginLoggingIn.setVisibility(View.VISIBLE);
					if(scrollView!=null){
						scrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
					}
					loginProgressBar.setVisibility(View.VISIBLE);
					loginFetchNodesProgressBar.setVisibility(View.GONE);
					loggingInText.setVisibility(View.VISIBLE);
					fetchingNodesText.setVisibility(View.GONE);
					prepareNodesText.setVisibility(View.GONE);
					if(serversBusyText!=null){
						serversBusyText.setVisibility(View.GONE);

					}

					if(Util.isChatEnabled()){
						log("onCreate: Chat is ENABLED");

						if (megaChatApi == null){
							megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
						}

						int ret = megaChatApi.getInitState();

						if(ret==0||ret==MegaChatApi.INIT_ERROR){
							ret = megaChatApi.init(gSession);
							log("onCreate: result of init ---> "+ret);
							chatSettings = dbH.getChatSettings();
							if (ret == MegaChatApi.INIT_NO_CACHE)
							{
								log("onCreate: condition ret == MegaChatApi.INIT_NO_CACHE");
							}
							else if (ret == MegaChatApi.INIT_ERROR)
							{
								log("onCreate: condition ret == MegaChatApi.INIT_ERROR");
								if(chatSettings==null) {
									log("1 - onCreate: ERROR----> Switch OFF chat");
									chatSettings = new ChatSettings();
									chatSettings.setEnabled(false+"");
									dbH.setChatSettings(chatSettings);
								}
								else{
									log("2 - onCreate: ERROR----> Switch OFF chat");
									dbH.setEnabledChat(false + "");
								}
								megaChatApi.logout(this);
							}
							else{
								log("onCreate: Chat correctly initialized");
							}
						}
					}

					megaApi.fastLogin(gSession, this);
				}

			}
			else{

				setContentView(R.layout.activity_file_provider);

				log("megaApi.getRootNode() NOT null");

				//Set toolbar
				tB = (Toolbar) findViewById(R.id.toolbar_provider);
				setSupportActionBar(tB);
				aB = getSupportActionBar();
//				aB.setLogo(R.drawable.ic_action_navigation_accept_white);
				aB.setDisplayHomeAsUpEnabled(true);
				aB.setDisplayShowHomeEnabled(true);

				Display display = getWindowManager().getDefaultDisplay();

				DisplayMetrics metrics = new DisplayMetrics();
				display.getMetrics(metrics);

				optionsBar = (LinearLayout) findViewById(R.id.options_provider_layout);

				cancelButton = (Button) findViewById(R.id.cancel_button);
				cancelButton.setOnClickListener(this);
				cancelButton.setText(getString(R.string.general_cancel));
				//Left and Right margin
				LinearLayout.LayoutParams cancelButtonParams = (LinearLayout.LayoutParams)cancelButton.getLayoutParams();
				cancelButtonParams.setMargins(Util.scaleWidthPx(10, metrics), 0, 0, 0);
				cancelButton.setLayoutParams(cancelButtonParams);

				attachButton = (Button) findViewById(R.id.attach_button);
				attachButton.setOnClickListener(this);
				attachButton.setText(getString(R.string.general_attach));
				activateButton(false);

				//TABS section
				providerSectionLayout= (LinearLayout)findViewById(R.id.tabhost_provider);
				tabLayoutProvider =  (TabLayout) findViewById(R.id.sliding_tabs_provider);
				viewPagerProvider = (ViewPager) findViewById(R.id.provider_tabs_pager);

				//Create tabs
				providerSectionLayout.setVisibility(View.VISIBLE);

				if (mTabsAdapterProvider == null){

					log("mTabsAdapterProvider == null");
					log("tabShown: "+tabShown);
					log("parentHandle INCOMING: "+incParentHandle);
					log("parentHandle CLOUD: " +gParentHandle);
					viewPagerProvider.setCurrentItem(tabShown);
					if(tabShown==-1){
						tabShown=CLOUD_TAB;
					}
					mTabsAdapterProvider = new ProviderPageAdapter(getSupportFragmentManager(),this);
					viewPagerProvider.setAdapter(mTabsAdapterProvider);
					tabLayoutProvider.setupWithViewPager(viewPagerProvider);
					viewPagerProvider.setCurrentItem(tabShown);
				}
				else{

					log("mTabsAdapterProvider NOOOT null");
					log("tabShown: "+tabShown);
					log("parentHandle INCOMING: "+incParentHandle);
					log("parentHandle CLOUD: " +gParentHandle);
					viewPagerProvider.setCurrentItem(tabShown);
					if(tabShown==-1){
						tabShown=CLOUD_TAB;
					}
				}

				viewPagerProvider.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
					public void onPageScrollStateChanged(int state) {}
					public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

					public void onPageSelected(int position) {
						log("onTabChanged TabId :"+ position);
						if(position == 0){
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
						else if(position == 1){
							tabShown=INCOMING_TAB;

							String cFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
							gcFTag = getFragmentTag(R.id.provider_tabs_pager, 1);
							iSharesProviderLol = (IncomingSharesProviderFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);

							if(iSharesProviderLol!=null){
								if(iSharesProviderLol.getDeepBrowserTree()==0){
									aB.setTitle(getString(R.string.title_incoming_shares_explorer));
								}
								else{
									aB.setTitle(megaApi.getNodeByHandle(iSharesProviderLol.getParentHandle()).getName());

								}
							}
						}
					}
				});

				getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Window window = this.getWindow();
					window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
					window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
					window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
				}
			}
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelectedLollipop");
	
		int id = item.getItemId();
		switch(id){
			case android.R.id.home:{				
				this.onBackPressed();
			}
		}
	    return super.onOptionsItemSelected(item);
	}

	@SuppressLint("NewApi")
	public void checkLogin(){
		setContentView(R.layout.fragment_login);
		
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
//		loginThreeDots = (ImageView) findViewById(R.id.login_three_dots);
//		LinearLayout.LayoutParams textThreeDots = (LinearLayout.LayoutParams)loginThreeDots.getLayoutParams();
//		textThreeDots.setMargins(Util.scaleWidthPx(30, outMetrics), 0, Util.scaleWidthPx(10, outMetrics), 0);
//		loginThreeDots.setLayoutParams(textThreeDots);
//
//		loginABC = (TextView) findViewById(R.id.ABC);
//
//		loginSwitchLol = (SwitchCompat) findViewById(R.id.switch_login);
//		LinearLayout.LayoutParams switchParams = (LinearLayout.LayoutParams)loginSwitchLol.getLayoutParams();
//		switchParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0);
//		loginSwitchLol.setLayoutParams(switchParams);
//		loginSwitchLol.setChecked(false);
//
//		loginSwitchLol.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//				if(!isChecked){
//						et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//						et_password.setSelection(et_password.getText().length());
//				}else{
//						et_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
//						et_password.setSelection(et_password.getText().length());
//			    }
//			}
//		});
		
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
		serversBusyText = (TextView) findViewById(R.id.login_servers_busy_text);

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

	public void changeTitle (String title){
		if (aB != null){
			aB.setTitle(title);
		}
	}

	private String getFragmentTag(int viewPagerId, int fragmentPosition)
	{
	     return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
	}

	public void downloadAndAttach(long size, long [] hashes){
		
		log("downloadAndAttach");
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}

		File destination = null;	
		
		destination=getCacheDir();
		String pathToDownload = destination.getPath();
				
		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(destination.getPath());
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}
		Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
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
							if(totalTransfers == 0) {
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
							else{
								contentUris.add(contentUri);
								progressTransfersFinish++;
								//Send it
								if (progressTransfersFinish == totalTransfers) {
									Intent result = new Intent();
									result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
									if (clipDataTransfers == null){
										clipDataTransfers = ClipData.newUri(getContentResolver(), "", contentUris.get(0));
									}
									else{
										clipDataTransfers.addItem(new ClipData.Item(contentUris.get(0)));
									}
									if (contentUris.size() >= 0) {
										for (int i = 1; i < contentUris.size(); i++) {
											clipDataTransfers.addItem(new ClipData.Item(contentUris.get(i)));
										}
									}
									result.setClipData(clipDataTransfers);
									result.setAction(Intent.ACTION_GET_CONTENT);

									if (getParent() == null) {
										setResult(Activity.RESULT_OK, result);
									} else {
										getParent().setResult(Activity.RESULT_OK, result);
									}
									totalTransfers = 0;
									finish();
								}
							}
						}

					}
					catch(Exception e) {
						finish();
					}
				}
				if(tempNode != null){
					dlFiles.put(tempNode, pathToDownload);
				}
			}
		}
		if(dlFiles.size() >0){
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

	
	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		bundle.putBoolean("folderSelected", folderSelected);
		bundle.putInt("tabShown", tabShown);
		bundle.putInt("deepBrowserTree", incomingDeepBrowserTree);
		bundle.putLong("parentHandle", gParentHandle);
		bundle.putLong("incParentHandle", incParentHandle);
		super.onSaveInstanceState(bundle);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

	}
	
	@Override
	public void onBackPressed() {
		log("onBackPressed: "+tabShown);

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
			case R.id.cancel_button:{
				finish();
				break;
			}
			case R.id.attach_button:{
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

				progressTransfersFinish = 0;
				clipDataTransfers = null;
                long[] hashes = new long[selectedNodes.size()];
				ArrayList<Long> totalHashes = new ArrayList<>();

                for (int i=0; i<selectedNodes.size(); i++){
                    hashes[i] = selectedNodes.get(i).getHandle();
					getTotalTransfers(selectedNodes.get(i), totalHashes);
                }

				hashes = new long[totalTransfers];
				for (int i=0; i<totalHashes.size(); i++){
					hashes[i] = totalHashes.get(i);
				}
                downloadAndAttach(selectedNodes.size(), hashes);
				break;
			}
		}
	}

	public void getTotalTransfers (MegaNode n, ArrayList<Long> totalHashes){
		int total = 0;
		if(n.isFile()){
			totalTransfers++;
			totalHashes.add(n.getHandle());
		}
		else{
			ArrayList<MegaNode> nodes = megaApi.getChildren(n);
			for (int i=0; i<nodes.size(); i++){
				getTotalTransfers(nodes.get(i), totalHashes);
			}
			totalTransfers += total;
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
			if(scrollView!=null){
				scrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_create_account));
			}
			loginCreateAccount.setVisibility(View.INVISIBLE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			loggingInText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			prepareNodesText.setVisibility(View.GONE);
			if(serversBusyText!=null){
				serversBusyText.setVisibility(View.GONE);
			}
			
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem),false, this);
			return;
		}
		
		loginLogin.setVisibility(View.GONE);
		loginCreateAccount.setVisibility(View.GONE);
		loginLoggingIn.setVisibility(View.VISIBLE);
		if(scrollView!=null){
			scrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
		}
		generatingKeysText.setVisibility(View.VISIBLE);
		loginProgressBar.setVisibility(View.VISIBLE);
		loginFetchNodesProgressBar.setVisibility(View.GONE);
		queryingSignupLinkText.setVisibility(View.GONE);
		confirmingAccountText.setVisibility(View.GONE);
		
		lastEmail = et_user.getText().toString().toLowerCase(Locale.ENGLISH).trim();
		lastPassword = et_password.getText().toString();
		
		log("generating keys");

		onKeysGenerated(lastEmail, lastPassword);
	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		if (request.getType() == MegaChatRequest.TYPE_CONNECT){
			MegaApplication.setLoggingIn(false);

			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Connected to chat!");
			}
			else{
				log("ERROR WHEN CONNECTING " + e.getErrorString());
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	private void onKeysGenerated(String email, String password) {
		log("key generation finished");

		this.lastEmail = email;
		this.lastPassword = password;

		onKeysGeneratedLogin(email, password);
	}
	
	private void onKeysGeneratedLogin(final String email, final String password) {

		if(!Util.isOnline(this)){
			loginLoggingIn.setVisibility(View.GONE);
			loginLogin.setVisibility(View.VISIBLE);
			if(scrollView!=null){
				scrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_create_account));
			}
			loginCreateAccount.setVisibility(View.INVISIBLE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			loggingInText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			prepareNodesText.setVisibility(View.GONE);
			if(serversBusyText!=null){
				serversBusyText.setVisibility(View.GONE);
			}

			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}

		if (!MegaApplication.isLoggingIn()) {
			MegaApplication.setLoggingIn(true);
			loggingInText.setVisibility(View.VISIBLE);
			fetchingNodesText.setVisibility(View.GONE);
			prepareNodesText.setVisibility(View.GONE);
			if (serversBusyText != null) {
				serversBusyText.setVisibility(View.GONE);
			}
			log("fastLogin con publicKey y privateKey");
			if (Util.isChatEnabled()) {
				log("onKeysGeneratedLogin: Chat is ENABLED");
				if (megaChatApi == null) {
					megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
				}
				int ret = megaChatApi.init(null);
				log("onKeysGeneratedLogin: result of init ---> " + ret);
				if (ret == MegaChatApi.INIT_WAITING_NEW_SESSION) {
					log("startFastLogin: condition ret == MegaChatApi.INIT_WAITING_NEW_SESSION");
					megaApi.login(lastEmail, lastPassword, this);
				} else {
					log("ERROR INIT CHAT: " + ret);
					megaChatApi.logout(this);
				}
			} else {
				log("onKeysGeneratedLogin: Chat is NOT ENABLED");
				megaApi.login(lastEmail, lastPassword, this);
			}
		}
	}
	
	public void setParentHandle (long parentHandle){
		this.gParentHandle = parentHandle;
	}

	public long getParentHandle() {
		return gParentHandle;
	}

	public long getIncParentHandle() {
		return incParentHandle;
	}

	public void setIncParentHandle(long incParentHandle) {
		this.incParentHandle = incParentHandle;
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

		log("Timer cancel");
		try{
			if(timer!=null){
				timer.cancel();
				if(serversBusyText!=null){
					serversBusyText.setVisibility(View.GONE);
				}
			}
		}
		catch(Exception ex){
			log("TIMER EXCEPTION");
			log(ex.getMessage());
		}

		if (request.getType() == MegaRequest.TYPE_LOGIN){
			log("REQUEST LOGIN");

			try {
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() != MegaError.API_OK) {

				MegaApplication.setLoggingIn(false);

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
				if(scrollView!=null){
					scrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_create_account));
				}
				loginCreateAccount.setVisibility(View.INVISIBLE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				if(serversBusyText!=null){
					serversBusyText.setVisibility(View.GONE);
				}
				
				Util.showErrorAlertDialog(errorMessage, false, this);
				
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				if (dbH.getPreferences() != null){
					dbH.clearPreferences();
					dbH.setFirstTime(false);
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
						Intent stopIntent = null;
						stopIntent = new Intent(this, CameraSyncService.class);
						stopIntent.setAction(CameraSyncService.ACTION_LOGOUT);
						startService(stopIntent);
					}
				}
			}
			else{
				loginProgressBar.setVisibility(View.VISIBLE);
				loginFetchNodesProgressBar.setVisibility(View.GONE);
				loggingInText.setVisibility(View.VISIBLE);
				fetchingNodesText.setVisibility(View.VISIBLE);
				prepareNodesText.setVisibility(View.GONE);
				if(serversBusyText!=null){
					serversBusyText.setVisibility(View.GONE);
				}
				
				gSession = megaApi.dumpSession();

//				if (lastEmail != null){
//					credentials = new UserCredentials(lastEmail, gSession, "", "");
//				}
				
//				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
//				dbH.clearCredentials();
//				dbH.saveCredentials(credentials);
				
				log("Logged in");
				
				megaApi.fetchNodes(this);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){

			if (e.getErrorCode() != MegaError.API_OK) {

				loginLoggingIn.setVisibility(View.GONE);
				loginLogin.setVisibility(View.VISIBLE);
				if(scrollView!=null){
					scrollView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_create_account));
				}
				loginCreateAccount.setVisibility(View.INVISIBLE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				if(serversBusyText!=null){
					serversBusyText.setVisibility(View.GONE);
				}
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);

				String errorMessage;
				if (e.getErrorCode() == MegaError.API_ESID){
					errorMessage = getString(R.string.error_server_expired_session);
				}
				else if (e.getErrorCode() == MegaError.API_ETOOMANY){
					errorMessage = getString(R.string.too_many_attempts_login);
				}
				else if (e.getErrorCode() == MegaError.API_EINCOMPLETE){
					errorMessage = getString(R.string.account_not_validated_login);
				}
				else if (e.getErrorCode() == MegaError.API_EBLOCKED){
					errorMessage = getString(R.string.error_account_suspended);
				}
				else{
					errorMessage = e.getErrorString();
				}
				showSnackbar(errorMessage);
			}
			else{

				UserCredentials credentials = new UserCredentials(lastEmail, gSession, "", "", megaApi.getMyUserHandle());
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				dbH.saveCredentials(credentials);
				
				setContentView(R.layout.activity_file_provider);
				tabShown = CLOUD_TAB;
				log("megaApi.getRootNode() NOT null");

				chatSettings = dbH.getChatSettings();
				if(chatSettings!=null) {
					boolean chatEnabled = Boolean.parseBoolean(chatSettings.getEnabled());
					if(chatEnabled){
						log("Chat enabled-->connect");
						if((megaChatApi.getInitState()!=MegaChatApi.INIT_ERROR)){
							log("Connection goes!!!");
							megaChatApi.connect(this);
						}
						else{
							log("Not launch connect: "+megaChatApi.getInitState());
						}

						MegaApplication.setLoggingIn(false);
                        afterFetchNodes();

						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							Window window = this.getWindow();
							window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
							window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
							window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
						}
					}
					else{
						log("Chat NOT enabled - readyToManager");
						MegaApplication.setLoggingIn(false);
						afterFetchNodes();
					}
				}
				else{
					log("chatSettings NULL - readyToManager");
					MegaApplication.setLoggingIn(false);
					afterFetchNodes();
				}
			}
		}
	}


	public void showSnackbar(String message) {
		if(scrollView!=null){
			Snackbar snackbar = Snackbar.make(scrollView, message, Snackbar.LENGTH_LONG);
			TextView snackbarTextView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
			snackbarTextView.setMaxLines(5);
			snackbar.show();
		}
	}

	public void afterFetchNodes(){
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_provider);

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tB.getLayoutParams();
		params.setMargins(0, 0, 0, 0);

		setSupportActionBar(tB);
		aB = getSupportActionBar();
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);

		Display display = getWindowManager().getDefaultDisplay();

		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);

		optionsBar = (LinearLayout) findViewById(R.id.options_provider_layout);

		cancelButton = (Button) findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(this);

		attachButton = (Button) findViewById(R.id.attach_button);
		attachButton.setOnClickListener(this);
		activateButton(false);

		//TABS section
		providerSectionLayout= (LinearLayout)findViewById(R.id.tabhost_provider);
		tabLayoutProvider =  (TabLayout) findViewById(R.id.sliding_tabs_provider);
		viewPagerProvider = (ViewPager) findViewById(R.id.provider_tabs_pager);

		//Create tabs
		providerSectionLayout.setVisibility(View.VISIBLE);

		if (mTabsAdapterProvider == null){
			mTabsAdapterProvider = new ProviderPageAdapter(getSupportFragmentManager(),this);
			viewPagerProvider.setAdapter(mTabsAdapterProvider);
			tabLayoutProvider.setupWithViewPager(viewPagerProvider);
		}

		viewPagerProvider.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			public void onPageScrollStateChanged(int state) {}
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

			public void onPageSelected(int position) {
				log("onTabChanged TabId :"+ position);
				if(position == 0){
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
				else if(position == 1){
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

	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());

		log("Start timer");
		try {
			timer = new CountDownTimer(10000, 2000) {

				public void onTick(long millisUntilFinished) {
					log("TemporaryError one more");
				}

				public void onFinish() {
					log("the timer finished, message shown");
					if (serversBusyText != null) {
						serversBusyText.setVisibility(View.VISIBLE);
					}
				}
			}.start();
		} catch (Exception exception) {
			log(exception.getMessage());
			log("EXCEPTION when starting count");
		}
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate");

		log("Cancel timer");
		try {
			if (timer != null) {
				timer.cancel();
				if (serversBusyText != null) {
					serversBusyText.setVisibility(View.GONE);
				}
			}
		} catch (Exception e) {
			log("TIMER EXCEPTION");
			log(e.getMessage());
		}
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> updatedNodes) {
		log("onNodesUpdate");
		if (cDriveProviderLol != null){
			if (megaApi.getNodeByHandle(cDriveProviderLol.getParentHandle()) != null){
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveProviderLol.getParentHandle()));
				cDriveProviderLol.setNodes(nodes);
				cDriveProviderLol.getListView().invalidate();
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
		if(transfer.isStreamingTransfer()){
			return;
		}

		try {
			//Get the URI of the file
			File fileToShare = new File(transfer.getPath());
			//		File newFile = new File(fileToShare, "default_image.jpg");
			Uri contentUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", fileToShare);
			grantUriPermission("*", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
		
			if(totalTransfers == 0) {
				log("CONTENT URI: " + contentUri);
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
			else {
				contentUris.add(contentUri);
				progressTransfersFinish++;
				if(progressTransfersFinish == totalTransfers) {
					Intent result = new Intent();
					result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					if (clipDataTransfers == null){
						clipDataTransfers = ClipData.newUri(getContentResolver(), "", contentUris.get(0));
					}
					else{
						clipDataTransfers.addItem(new ClipData.Item(contentUris.get(0)));
					}
					if (contentUris.size() >= 0) {
						for (int i = 1; i < contentUris.size(); i++) {
							clipDataTransfers.addItem(new ClipData.Item(contentUris.get(i)));
						}
					}
					result.setClipData(clipDataTransfers);
					result.setAction(Intent.ACTION_GET_CONTENT);

					if (getParent() == null) {
						setResult(Activity.RESULT_OK, result);
					} else {
						Toast.makeText(this, "ENTROOO parent no null", Toast.LENGTH_LONG).show();
						getParent().setResult(Activity.RESULT_OK, result);
					}
					totalTransfers = 0;
					finish();
				}
			}
		}
		catch (Exception exception){
			finish();
		}
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

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	public void activateButton (Boolean show){
		attachButton.setEnabled(show);
		if(show){
			attachButton.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
		}
		else{
			attachButton.setTextColor(ContextCompat.getColor(this, R.color.invite_button_deactivated));
		}
	}

	public void attachFiles (List<MegaNode> nodes){
        this.selectedNodes = nodes;

    }

	public int getIncomingDeepBrowserTree() {
		return incomingDeepBrowserTree;
	}

	public void setIncomingDeepBrowserTree(int incomingDeepBrowserTree) {
		this.incomingDeepBrowserTree = incomingDeepBrowserTree;
	}

	public int getTabShown() {
		return tabShown;
	}

}
