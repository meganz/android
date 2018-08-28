package mega.privacy.android.app.lollipop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.adapters.FileExplorerPagerAdapter;
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithTitle;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerFragment;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.ChatUploadService;
import mega.privacy.android.app.lollipop.megachat.PendingMessage;
import mega.privacy.android.app.lollipop.megachat.PendingNodeAttachment;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

public class FileExplorerActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaChatRequestListenerInterface, View.OnClickListener {
	
	public static String ACTION_PROCESSED = "CreateLink.ACTION_PROCESSED";
	
	public static String ACTION_PICK_MOVE_FOLDER = "ACTION_PICK_MOVE_FOLDER";
	public static String ACTION_PICK_COPY_FOLDER = "ACTION_PICK_COPY_FOLDER";
	public static String ACTION_PICK_IMPORT_FOLDER = "ACTION_PICK_IMPORT_FOLDER";
	public static String ACTION_SELECT_FOLDER = "ACTION_SELECT_FOLDER";
	public static String ACTION_SELECT_FOLDER_TO_SHARE = "ACTION_SELECT_FOLDER_TO_SHARE";
	public static String ACTION_SELECT_FILE = "ACTION_SELECT_FILE";
	public static String ACTION_CHOOSE_MEGA_FOLDER_SYNC = "ACTION_CHOOSE_MEGA_FOLDER_SYNC";
	public static String ACTION_MULTISELECT_FILE = "ACTION_MULTISELECT_FILE";
	public static String ACTION_UPLOAD_TO_CLOUD = "ACTION_UPLOAD_TO_CLOUD";

	public static int UPLOAD = 0;
	public static int MOVE = 1;
	public static int COPY = 2;
	public static int CAMERA = 3;
	public static int IMPORT = 4;
	public static int SELECT = 5;
	public static int SELECT_CAMERA_FOLDER = 7;
	public static int SHARE_LINK = 8;

	public static int NO_TABS = -1;
	public static int CLOUD_TAB = 0;
	public static int INCOMING_TAB = 1;
	public static int CHAT_TAB = 2;
	boolean isChatFirst = false;

	boolean sendOriginalAttachments = false;

	DatabaseHandler dbH = null;

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

	FloatingActionButton fabButton;

	MegaNode parentMoveCopy;
    ArrayList<Long> nodeHandleMoveCopy;

	MenuItem createFolderMenuItem;
	MenuItem newChatMenuItem;

	FrameLayout cloudDriveFrameLayout;
	private long fragmentHandle  = -1;

	private String gSession;
    UserCredentials credentials;
	private String lastEmail;
//	private ImageView windowBack;
//	private boolean backVisible = false;
//	private TextView windowTitle;
	
	private MegaApiAndroid megaApi;
	private MegaChatApiAndroid megaChatApi;

	private int mode;
	public boolean multiselect = false;
	boolean selectFile = false;
	
	private long[] moveFromHandles;
	private long[] copyFromHandles;
	private long[] importChatHandles;
	private ArrayList<String> selectedContacts;
	private String imagePath;
	private boolean folderSelected = false;
	
	private Handler handler;

	ChatSettings chatSettings;
	
	private int tabShown = CLOUD_TAB;

	ArrayList<MegaChatListItem> chatListItems;

	private CloudDriveExplorerFragmentLollipop cDriveExplorer;
	private IncomingSharesExplorerFragmentLollipop iSharesExplorer;
	private ChatExplorerFragment chatExplorer;

	private AlertDialog newFolderDialog;
	
	ProgressDialog statusDialog;
	
	private List<ShareInfo> filePreparedInfos;

	//Tabs in Cloud
	TabLayout tabLayoutExplorer;
	LinearLayout fileExplorerSectionLayout;
	FileExplorerPagerAdapter mTabsAdapterExplorer;
	ViewPager viewPagerExplorer;

	ArrayList<MegaNode> nodes;

	String regex = "[*|\\?:\"<>\\\\\\\\/]";

	//	long gParentHandle;
	long parentHandleIncoming;
	long parentHandleCloud;
	int deepBrowserTree = 0;

	Intent intent = null;

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		log("onRequestFinish(CHAT)");

		if (request.getType() == MegaChatRequest.TYPE_CONNECT){
			MegaApplication.setLoggingIn(false);
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Connected to chat!");
			}
			else{
				log("ERROR WHEN CONNECTING " + e.getErrorString());
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
			log("Create chat request finish.");
			onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle());
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	/*
	 * Background task to process files for uploading
	 */
	private class OwnFilePrepareTask extends AsyncTask<Intent, Void, List<ShareInfo>> {
		Context context;
		
		OwnFilePrepareTask(Context context){
			this.context = context;
		}
		
		@Override
		protected List<ShareInfo> doInBackground(Intent... params) {
			log("OwnFilePrepareTask: doInBackground");
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
			deepBrowserTree = savedInstanceState.getInt("deepBrowserTree", 0);
			log("savedInstanceState -> deepBrowserTree: "+deepBrowserTree);
		}
		else{
			log("Bundle is NULL");
			parentHandleCloud = -1;
			parentHandleIncoming = -1;
			deepBrowserTree = 0;
		}
				
		dbH = DatabaseHandler.getDbHandler(this);
		credentials = dbH.getCredentials();
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
		if (credentials == null){

			log("User credentials NULL");
//			megaApi.localLogout();
//			AccountController aC = new AccountController(this);
//			aC.logout(this, megaApi, megaChatApi, false);
			
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
		}
		else{
			log("User has credentials");
		}
		
		if (savedInstanceState != null){
			folderSelected = savedInstanceState.getBoolean("folderSelected", false);
		}
	
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		megaApi.addGlobalListener(this);

		if (Util.isChatEnabled()) {
			if (megaChatApi == null) {
				megaChatApi = ((MegaApplication)getApplication()).getMegaChatApi();
			}
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

		fabButton = (FloatingActionButton) findViewById(R.id.fab_file_explorer);
		fabButton.setOnClickListener(this);
		fabButton.setVisibility(View.GONE);
		//TABS
		fileExplorerSectionLayout= (LinearLayout)findViewById(R.id.tabhost_explorer);
		tabLayoutExplorer =  (TabLayout) findViewById(R.id.sliding_tabs_file_explorer);
		viewPagerExplorer = (ViewPager) findViewById(R.id.explorer_tabs_pager);
		viewPagerExplorer.setOffscreenPageLimit(3);
		
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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				Window window = this.getWindow();
				window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				window.setStatusBarColor(ContextCompat.getColor(this, R.color.transparent_black));
			}

			log("hide action bar");
			if (!MegaApplication.isLoggingIn()) {

				MegaApplication.setLoggingIn(true);

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

				if(Util.isChatEnabled()){
					log("onCreate: Chat is ENABLED");

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
			else{
				log("Another login is proccessing");
			}
		}
		else{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				Window window = this.getWindow();
				window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));

			}

			afterLoginAndFetch();
			((MegaApplication) getApplication()).sendSignalPresenceActivity();
		}

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

	}
	
	private void afterLoginAndFetch(){
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

		if ((intent != null) && (intent.getAction() != null)){
			log("intent OK: "+intent.getAction());
			if (intent.getAction().equals(ACTION_SELECT_FOLDER_TO_SHARE)){
				log("action = ACTION_SELECT_FOLDER_TO_SHARE");
				//Just show Cloud Drive, no INCOMING tab , no need of tabhost

				mode = SELECT;
				selectFile = false;
				selectedContacts=intent.getStringArrayListExtra("SELECTED_CONTACTS");

				aB.setTitle(getString(R.string.title_share_folder_explorer));

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

			}
			else if (intent.getAction().equals(ACTION_SELECT_FILE)){
				log("action = ACTION_SELECT_FILE");
				//Just show Cloud Drive, no INCOMING tab , no need of tabhost

				mode = SELECT;
				String title = getResources().getQuantityString(R.plurals.plural_select_file, 1);
				aB.setTitle(title);

				selectFile = true;
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
			}
			else if (intent.getAction().equals(ACTION_MULTISELECT_FILE)){
				log("action = ACTION_MULTISELECT_FILE");
				//Just show Cloud Drive, no INCOMING tab , no need of tabhost

				mode = SELECT;
				selectFile = true;
				multiselect = true;

				String title = getResources().getQuantityString(R.plurals.plural_select_file, 10);
				aB.setTitle(title);

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
			}
			else{

				if (intent.getAction().equals(ACTION_PICK_MOVE_FOLDER)){
					log("ACTION_PICK_MOVE_FOLDER");
					mode = MOVE;
					moveFromHandles = intent.getLongArrayExtra("MOVE_FROM");

					aB.setTitle(getString(R.string.title_share_folder_explorer));

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

						if (mTabsAdapterExplorer != null) {
							if (mTabsAdapterExplorer.getCount() > 2) {
								tabLayoutExplorer.removeTabAt(2);
							}
						}
					}
					else{
						log("mTabsAdapterExplorer != null");
					}

					ArrayList<Long> list = new ArrayList<Long>(moveFromHandles.length);
                    nodeHandleMoveCopy = new ArrayList<Long>(moveFromHandles.length);
					MegaNode p;
					for (long n : moveFromHandles) {
						list.add(n);
                        nodeHandleMoveCopy.add(n);
						p = megaApi.getNodeByHandle(n);
						p = megaApi.getParentNode(p);
                        parentMoveCopy = p;
                    }

					String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
					if(cDriveExplorer!=null){
						cDriveExplorer.setDisableNodes(list);
					}
				}
				else if (intent.getAction().equals(ACTION_PICK_COPY_FOLDER)){
					log("ACTION_PICK_COPY_FOLDER");
					mode = COPY;
					copyFromHandles = intent.getLongArrayExtra("COPY_FROM");

					aB.setTitle(getString(R.string.title_share_folder_explorer));

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

						if (mTabsAdapterExplorer != null) {
							if (mTabsAdapterExplorer.getCount() > 2) {
								tabLayoutExplorer.removeTabAt(2);
							}
						}
					}

					MegaNode p;
                    nodeHandleMoveCopy = new ArrayList<Long>(copyFromHandles.length);
					ArrayList<Long> list = new ArrayList<Long>(copyFromHandles.length);
					for (long n : copyFromHandles){
						list.add(n);
                        nodeHandleMoveCopy.add(n);
						p = megaApi.getNodeByHandle(n);
						p = megaApi.getParentNode(p);
                        parentMoveCopy = p;
					}
				}
				else if (intent.getAction().equals(ACTION_CHOOSE_MEGA_FOLDER_SYNC)){
					log("action = ACTION_CHOOSE_MEGA_FOLDER_SYNC");
					mode = SELECT_CAMERA_FOLDER;

					aB.setTitle(getString(R.string.title_share_folder_explorer));

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

						if (mTabsAdapterExplorer != null) {
							if (mTabsAdapterExplorer.getCount() > 2) {
								tabLayoutExplorer.removeTabAt(2);
							}
						}
					}
				}
				else if (intent.getAction().equals(ACTION_PICK_IMPORT_FOLDER)){
					mode = IMPORT;

					importChatHandles = intent.getLongArrayExtra("HANDLES_IMPORT_CHAT");

					aB.setTitle(getString(R.string.title_share_folder_explorer));

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

						if (mTabsAdapterExplorer != null) {
							if (mTabsAdapterExplorer.getCount() > 2) {
								tabLayoutExplorer.removeTabAt(2);
							}
						}
					}
				}
				else if ((intent.getAction().equals(ACTION_SELECT_FOLDER))){
					log("action = ACTION_SELECT_FOLDER");
					mode = SELECT;
					selectedContacts=intent.getStringArrayListExtra("SELECTED_CONTACTS");

					aB.setTitle(getString(R.string.title_share_folder_explorer));

					if (mTabsAdapterExplorer == null){
						fileExplorerSectionLayout.setVisibility(View.VISIBLE);
						viewPagerExplorer.setVisibility(View.VISIBLE);
						mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
						viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
						tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

						if (mTabsAdapterExplorer != null) {
							if (mTabsAdapterExplorer.getCount() > 2) {
								tabLayoutExplorer.removeTabAt(2);
							}
						}
					}
				}
				else if ((intent.getAction().equals(ACTION_UPLOAD_TO_CLOUD))){
					log("action = UPLOAD to Cloud Drive");
					mode = UPLOAD;
					selectFile = false;

					aB.setTitle(getString(R.string.title_cloud_explorer));

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
				}
				else{
					log("action = UPLOAD");
					mode = UPLOAD;
					aB.setTitle(getString(R.string.title_cloud_explorer));

					if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
						if ("text/plain".equals(intent.getType())) {
							log("Handle intent of text plain");
							Bundle extras = intent.getExtras();
							if(extras!=null) {
								if (!extras.containsKey(Intent.EXTRA_STREAM)) {
									isChatFirst = true;
								}
							}
						}
					}

					if(isChatFirst){
						String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

						if (URLUtil.isHttpsUrl(sharedText) || URLUtil.isHttpUrl(sharedText)) {
							if (mTabsAdapterExplorer == null){
								fileExplorerSectionLayout.setVisibility(View.VISIBLE);
								viewPagerExplorer.setVisibility(View.VISIBLE);
								mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this, true);
								viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
								tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

							}
						}
					}
					else{
						if (mTabsAdapterExplorer == null){
							fileExplorerSectionLayout.setVisibility(View.VISIBLE);
							viewPagerExplorer.setVisibility(View.VISIBLE);
							mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
							viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
							tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

						}
					}
				}

				viewPagerExplorer.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
					public void onPageScrollStateChanged(int state) {}
					public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

					public void onPageSelected(int position) {
						log("onTabChanged TabId :"+ position);
						supportInvalidateOptionsMenu();
						changeTitle();
					}
				});
			}

		}
		else{
			log("intent error");
		}
	}

	public void showFabButton(boolean show){
		if(show){
			fabButton.setVisibility(View.VISIBLE);
		}
		else{
			fabButton.setVisibility(View.GONE);
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.file_explorer_action, menu);
	    
	    createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder);
	    newChatMenuItem = menu.findItem(R.id.cab_menu_new_chat);

		createFolderMenuItem.setVisible(false);

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

	    //Check the tab shown
		if (viewPagerExplorer != null){

			int index = viewPagerExplorer.getCurrentItem();

			if(index==0){
				if(isChatFirst){
					createFolderMenuItem.setVisible(false);
					newChatMenuItem.setVisible(true);
				}
				else{
					//CLOUD TAB
					if(intent.getAction().equals(ACTION_MULTISELECT_FILE)||intent.getAction().equals(ACTION_SELECT_FILE)){
						createFolderMenuItem.setVisible(false);
					}
					else{
						createFolderMenuItem.setVisible(true);
					}
					newChatMenuItem.setVisible(false);
				}

			}
			else if(index==1){
				if(isChatFirst){
					//CLOUD TAB
					if(intent.getAction().equals(ACTION_MULTISELECT_FILE)||intent.getAction().equals(ACTION_SELECT_FILE)){
						createFolderMenuItem.setVisible(false);
					}
					else{
						createFolderMenuItem.setVisible(true);
					}
					newChatMenuItem.setVisible(false);
				}
				else{
					String cFTag1 = getFragmentTag(R.id.explorer_tabs_pager, 1);
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
					newChatMenuItem.setVisible(false);
				}
			}
			else if(index==2){
				if(isChatFirst){
					//INCOMING TAB
					String cFTag1 = getFragmentTag(R.id.explorer_tabs_pager, 2);
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
					newChatMenuItem.setVisible(false);
				}
				else{
					createFolderMenuItem.setVisible(false);
					newChatMenuItem.setVisible(true);
				}
			}

		}else{
			if (cDriveExplorer != null){
				createFolderMenuItem.setVisible(true);
			}
			newChatMenuItem.setVisible(false);
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
	

	public void setRootTitle(){
		log("setRootTitle");

		if(mode==SELECT){
			if(selectFile){
				if(multiselect){
					String title = getResources().getQuantityString(R.plurals.plural_select_file, 10);
					aB.setTitle(title);
				}
				else{
					String title = getResources().getQuantityString(R.plurals.plural_select_file, 1);
					aB.setTitle(title);
				}
			}
			else{
				aB.setTitle(getString(R.string.title_share_folder_explorer));
			}
		}
		else if(mode == MOVE || mode == COPY || mode == SELECT_CAMERA_FOLDER || mode == IMPORT){
			aB.setTitle(getString(R.string.title_share_folder_explorer));
		}
		else if(mode==UPLOAD){
			aB.setTitle(getString(R.string.title_cloud_explorer));
		}
	}

	public void changeTitle (){
		log("changeTitle");

		if(tabShown==NO_TABS){
			cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag("cDriveExplorer");

			if(cDriveExplorer!=null){
				if(cDriveExplorer.parentHandle==-1|| cDriveExplorer.parentHandle==megaApi.getRootNode().getHandle()){
					setRootTitle();
				}
				else{
					aB.setTitle(megaApi.getNodeByHandle(cDriveExplorer.parentHandle).getName());
				}
			}
			fabButton.setVisibility(View.GONE);
		}
		else{
			int position = viewPagerExplorer.getCurrentItem();
			if(position == 0){

				String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);

				Fragment f = (Fragment) getSupportFragmentManager().findFragmentByTag(cFTag);
				if(f!=null){
					if(f instanceof ChatExplorerFragment){

						if(tabShown!=NO_TABS){
							tabShown=CHAT_TAB;
						}

						if(((ChatExplorerFragment)f)!=null){
							aB.setTitle(getString(R.string.title_chat_explorer));
						}

						if(((ChatExplorerFragment)f).getSelectedChats().size() > 0){
							fabButton.setVisibility(View.VISIBLE);
						}
						else{
							fabButton.setVisibility(View.GONE);
						}
					}
					else if(f instanceof CloudDriveExplorerFragmentLollipop){

						if(tabShown!=NO_TABS){
							tabShown=CLOUD_TAB;
						}

						if(((CloudDriveExplorerFragmentLollipop)f).parentHandle==-1|| ((CloudDriveExplorerFragmentLollipop)f).parentHandle==megaApi.getRootNode().getHandle()){
							setRootTitle();
						}
						else{
							aB.setTitle(megaApi.getNodeByHandle(((CloudDriveExplorerFragmentLollipop)f).parentHandle).getName());
						}

						fabButton.setVisibility(View.GONE);
					}
				}
			}
			else if(position == 1){

				String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);

				Fragment f = (Fragment) getSupportFragmentManager().findFragmentByTag(cFTag);
				if(f!=null){
					if(f instanceof IncomingSharesExplorerFragmentLollipop){

						if(tabShown!=NO_TABS){
							tabShown=INCOMING_TAB;
						}

						if(((IncomingSharesExplorerFragmentLollipop)f).getDeepBrowserTree()==0){
							setRootTitle();
						}
						else{
							aB.setTitle(megaApi.getNodeByHandle(((IncomingSharesExplorerFragmentLollipop)f).parentHandle).getName());
						}
					}
					else if(f instanceof CloudDriveExplorerFragmentLollipop){

						if(tabShown!=NO_TABS){
							tabShown=CLOUD_TAB;
						}

						if(((CloudDriveExplorerFragmentLollipop)f).parentHandle==-1|| ((CloudDriveExplorerFragmentLollipop)f).parentHandle==megaApi.getRootNode().getHandle()){
							setRootTitle();
						}
						else{
							aB.setTitle(megaApi.getNodeByHandle(((CloudDriveExplorerFragmentLollipop)f).parentHandle).getName());
						}
					}
				}
				fabButton.setVisibility(View.GONE);
			}
			else if(position == 2){

				String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 2);

				Fragment f = (Fragment) getSupportFragmentManager().findFragmentByTag(cFTag);
				if(f!=null){
					if(f instanceof ChatExplorerFragment){

						if(tabShown!=NO_TABS){
							tabShown=CHAT_TAB;
						}

						if(((ChatExplorerFragment)f)!=null){
							aB.setTitle(getString(R.string.title_chat_explorer));
						}

						if(((ChatExplorerFragment)f).getSelectedChats().size() > 0){
							fabButton.setVisibility(View.VISIBLE);
						}
						else{
							fabButton.setVisibility(View.GONE);
						}
					}
					else if(f instanceof IncomingSharesExplorerFragmentLollipop){

						if(tabShown!=NO_TABS){
							tabShown=INCOMING_TAB;
						}

						if(((IncomingSharesExplorerFragmentLollipop)f).getDeepBrowserTree()==0){
							setRootTitle();
						}
						else{
							aB.setTitle(megaApi.getNodeByHandle(((IncomingSharesExplorerFragmentLollipop)f).parentHandle).getName());
						}

						fabButton.setVisibility(View.GONE);
					}
				}
			}
		}
	}
	
	private String getFragmentTag(int viewPagerId, int fragmentPosition)
	{
	     return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
	}

	public void finishActivity(){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			super.finishAndRemoveTask();
		}
		else {
			super.finish();
		}
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
		String cFTag1;
		if(isChatFirst){
			cFTag1 = getFragmentTag(R.id.explorer_tabs_pager, 2);
		}
		else{
			cFTag1 = getFragmentTag(R.id.explorer_tabs_pager, 1);
		}

		iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
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
						OwnFilePrepareTask ownFilePrepareTask = new OwnFilePrepareTask(this);
						ownFilePrepareTask.execute(getIntent());
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
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}
	
	@Override
	public void onBackPressed() {
		log("onBackPressed: "+tabShown);
		String cFTag;
		if(tabShown==CLOUD_TAB){
			if(isChatFirst){
				cFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
			}
			else{
				cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
			}

			cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	
			if(cDriveExplorer!=null){
				if (cDriveExplorer.onBackPressed() == 0){
//					super.onBackPressed();
					finishActivity();
				}
			}
		}
		else if(tabShown==INCOMING_TAB){
			if(isChatFirst){
				cFTag = getFragmentTag(R.id.explorer_tabs_pager, 2);
			}
			else{
				cFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
			}
			iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
			if(iSharesExplorer!=null){
				if (iSharesExplorer.onBackPressed() == 0){
//					super.onBackPressed();
					finishActivity();
				}
			}
		}
		else if(tabShown==NO_TABS){
			cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag("cDriveExplorer");

			if(cDriveExplorer!=null){
				if (cDriveExplorer.onBackPressed() == 0){
//					super.onBackPressed();
					finishActivity();
				}
			}
		}
		else{
			super.onBackPressed();
		}
	}

	/*
	 * Handle processed upload intent
	 */
	public void onIntentChatProcessed(List<ShareInfo> infos) {
		log("onIntentChatProcessed");

		if (infos == null) {
			Snackbar.make(fragmentContainer, getString(R.string.upload_can_not_open), Snackbar.LENGTH_LONG).show();
		}
		else {
			log("Launch chat upload with files "+infos.size());
			for (ShareInfo info : infos) {
				Intent intent = new Intent(this, ChatUploadService.class);

				long timestamp = System.currentTimeMillis()/1000;

				if(chatListItems!=null){
					for(int i=0; i < chatListItems.size();i++){
						MegaChatListItem item = chatListItems.get(i);
						long idChat = item.getChatId();
						log("Id chat: "+idChat);
						long idPendingMsg = dbH.setPendingMessage(idChat+"", Long.toString(timestamp));
						if(idPendingMsg!=-1){
							intent.putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idPendingMsg);
							log("name of the file: "+info.getTitle());
							log("size of the file: "+info.getSize());
							PendingNodeAttachment nodeAttachment = null;

							if (MimeTypeList.typeForName(info.getFileAbsolutePath()).isImage()) {

								if(sendOriginalAttachments){
									String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());

									//Add node to db
									long idNode = dbH.setNodeAttachment(info.getFileAbsolutePath(), info.getTitle(), fingerprint);

									dbH.setMsgNode(idPendingMsg, idNode);

									nodeAttachment = new PendingNodeAttachment(info.getFileAbsolutePath(), fingerprint, info.getTitle());
								}
								else{
									File previewDir = PreviewUtils.getPreviewFolder(this);
									String nameFilePreview = info.getTitle();
									File preview = new File(previewDir, nameFilePreview);

									boolean isPreview = megaApi.createPreview(info.getFileAbsolutePath(), preview.getAbsolutePath());

									if(isPreview){
										log("Preview: "+preview.getAbsolutePath());
										String fingerprint = megaApi.getFingerprint(preview.getAbsolutePath());

										//Add node to db
										long idNode = dbH.setNodeAttachment(preview.getAbsolutePath(), info.getTitle(), fingerprint);

										dbH.setMsgNode(idPendingMsg, idNode);

										nodeAttachment = new PendingNodeAttachment(preview.getAbsolutePath(), fingerprint, info.getTitle());
									}
									else{
										log("No preview");
										String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());

										//Add node to db
										long idNode = dbH.setNodeAttachment(info.getFileAbsolutePath(), info.getTitle(), fingerprint);

										dbH.setMsgNode(idPendingMsg, idNode);

										nodeAttachment = new PendingNodeAttachment(info.getFileAbsolutePath(), fingerprint, info.getTitle());
									}
								}
							}
							else{
								String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());

								//Add node to db
								long idNode = dbH.setNodeAttachment(info.getFileAbsolutePath(), info.getTitle(), fingerprint);

								dbH.setMsgNode(idPendingMsg, idNode);

								nodeAttachment = new PendingNodeAttachment(info.getFileAbsolutePath(), fingerprint, info.getTitle());
							}

							PendingMessage newPendingMsg = new PendingMessage(idPendingMsg, idChat, nodeAttachment, timestamp, PendingMessage.STATE_SENDING);
//							AndroidMegaChatMessage newNodeAttachmentMsg = new AndroidMegaChatMessage(newPendingMsg, true);
//							sendMessageUploading(newNodeAttachmentMsg);

							intent.putExtra(ChatUploadService.EXTRA_FILEPATH, newPendingMsg.getFilePath());
							intent.putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat);

							startService(intent);
						}
						else{
							log("Error when adding pending msg to the database");
						}
					}
				}
				else{
					filePreparedInfos = null;
					log("ERROR null files to upload");
					finishActivity();
				}

			}

			if (statusDialog != null) {
				try {
					statusDialog.dismiss();
				}
				catch(Exception ex){}
			}

			if(chatListItems.size()==1){
				MegaChatListItem chatItem = chatListItems.get(0);
				long idChat = chatItem.getChatId();
				if(chatItem!=null){
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
					intent.putExtra("CHAT_ID", idChat);
					startActivity(intent);
				}
			}
			else{
				Intent chatIntent = new Intent(this, ManagerActivityLollipop.class);
				chatIntent.setAction(Constants.ACTION_CHAT_SUMMARY);
				startActivity(chatIntent);
			}

			filePreparedInfos = null;
			log("finish!!!");
			finishActivity();
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
					parentHandle = parentHandleCloud;
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
				finishActivity();
			}	
		}
	}

    public void buttonClick(long[] handles){
        log("buttonClick handles");

        Intent intent = new Intent();
        intent.putExtra("NODE_HANDLES", handles);
        intent.putStringArrayListExtra("SELECTED_CONTACTS", selectedContacts);
        setResult(RESULT_OK, intent);
		finishActivity();
    }
	
	public void buttonClick(long handle){
		log("buttonClick");
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		if (tabShown == INCOMING_TAB){
			if (iSharesExplorer.getDeepBrowserTree()==0){
				Intent intent = new Intent();
				setResult(RESULT_FIRST_USER, intent);
				finishActivity();
				return;
			}
		}
		
		folderSelected = true;
		this.parentHandleCloud = handle;
		
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
			finishActivity();
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
			finishActivity();
		}
		else if (mode == UPLOAD){

			log("mode UPLOAD");

			if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
				if ("text/plain".equals(intent.getType())) {
					log("Handle intent of text plain");

					Bundle extras = intent.getExtras();
					if(extras!=null){
						if (!extras.containsKey(Intent.EXTRA_STREAM)) {
							StringBuilder body = new StringBuilder();
							String sharedText2 = intent.getStringExtra(Intent.EXTRA_SUBJECT);
							if (sharedText2 != null) {
								body.append(getString(R.string.new_file_subject_when_uploading)+": ");
								body.append(sharedText2);
							}
							String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
							if (sharedText != null) {
								body.append("\n");
								body.append(getString(R.string.new_file_content_when_uploading)+": ");
								body.append(sharedText);
							}
							String sharedText3 = intent.getStringExtra(Intent.EXTRA_EMAIL);
							if (sharedText3 != null) {
								body.append("\n");
								body.append(getString(R.string.new_file_email_when_uploading)+": ");
								body.append(sharedText3);
							}

							long parentHandle = handle;
							MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
							if(parentNode == null){
								parentNode = megaApi.getRootNode();
							}

							showNewFileDialog(parentNode,body.toString());
							return;
						}
					}
				}
			}

			if (filePreparedInfos == null){
//				Intent prueba = getIntent();
//				Bundle bundle = prueba.getExtras();
//				Uri uri = (Uri)bundle.get(Intent.EXTRA_STREAM);
//				log("URI mode UPLOAD in bundle: "+uri);
				
				OwnFilePrepareTask ownFilePrepareTask = new OwnFilePrepareTask(this);
				ownFilePrepareTask.execute(getIntent());
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
			long parentHandle = handle;
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode == null){
				parentNode = megaApi.getRootNode();
			}

			if(tabShown==CLOUD_TAB){
				fragmentHandle= megaApi.getRootNode().getHandle();
			}else if(tabShown == INCOMING_TAB){
				fragmentHandle = -1;
			}
			
			Intent intent = new Intent();
			intent.putExtra("IMPORT_TO", parentNode.getHandle());
			intent.putExtra("fragmentH",fragmentHandle);


			if(importChatHandles!=null){
				intent.putExtra("HANDLES_IMPORT_CHAT", importChatHandles);
			}

			setResult(RESULT_OK, intent);
			log("finish!");
			finishActivity();
		}
		else if (mode == SELECT){

			if(selectFile)
			{
				Intent intent = new Intent();
				intent.putExtra("SELECT", handle);
				intent.putStringArrayListExtra("SELECTED_CONTACTS", selectedContacts);
				setResult(RESULT_OK, intent);
				finishActivity();
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
				finishActivity();
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
			finishActivity();
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

    public void showSnackbar(String s){
        log("showSnackbar: "+s);
        Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    private void createFile(String name, String data, MegaNode parentNode){

		File file = Util.createTemporalTextFile(name, data);
		if(file!=null){
			Snackbar.make(fragmentContainer,getString(R.string.upload_began),Snackbar.LENGTH_LONG).show();

			Intent intent = new Intent(this, UploadService.class);
			intent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
			intent.putExtra(UploadService.EXTRA_NAME, file.getName());
			intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
			intent.putExtra(UploadService.EXTRA_SIZE, file.getTotalSpace());
			startService(intent);

			log("After UPLOAD click - back to Cloud");
			this.backToCloud(parentNode.getHandle());
			finishActivity();
		}
		else{
			Snackbar.make(fragmentContainer,getString(R.string.email_verification_text_error),Snackbar.LENGTH_LONG).show();
		}
	}

	private void createFolder(String title) {
	
		log("createFolder");
		if (!Util.isOnline(this)){
            showSnackbar(getString(R.string.error_server_connection_problem));
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
				String gcFTag;
				if(isChatFirst){
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
				}
				else{
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
				}
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
				String gcFTag;
				if(isChatFirst){
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 2);
				}
				else{
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
				}
				iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
				if (iSharesExplorer != null){
					parentHandle = iSharesExplorer.getParentHandle();
					log("2)iSharesExplorer != null: " + parentHandle);
				}	
			}
		}
		else if (tabShown == NO_TABS){
			if (cDriveExplorer != null){
				parentHandle = cDriveExplorer.getParentHandle();
				log("1)cDriveExplorer != null: " + parentHandle);
			}
			else{
				String gcFTag;
				if(isChatFirst){
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
				}
				else{
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
				}
				cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
				if (cDriveExplorer != null){
					parentHandle = cDriveExplorer.getParentHandle();
					log("2)cDriveExplorer != null: " + parentHandle);
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
		this.parentHandleCloud = parentHandle;
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

				if(tabShown==CLOUD_TAB){
					String gcFTag;
					if(isChatFirst){
						gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
					}
					else{
						gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					}
					cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
					if (cDriveExplorer != null){
						cDriveExplorer.navigateToFolder(request.getNodeHandle());
						parentHandleCloud = request.getNodeHandle();
						log("The handle of the created folder is: "+parentHandleCloud);
					}						
				}
				else if (tabShown == NO_TABS){
					if (cDriveExplorer != null){
						cDriveExplorer.navigateToFolder(request.getNodeHandle());
						parentHandleCloud = request.getNodeHandle();
					}
					else{
						String gcFTag;
						if(isChatFirst){
							gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
						}
						else{
							gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
						}
						cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
						if (cDriveExplorer != null){
							cDriveExplorer.navigateToFolder(request.getNodeHandle());
							parentHandleCloud = request.getNodeHandle();
						}
					}
					log("The handle of the created folder is: "+parentHandleCloud);
				}
				else{

					String gcFTag;
					if(isChatFirst){
						gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 2);
					}
					else{
						gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
					}
					iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
					if (iSharesExplorer != null){
						iSharesExplorer.navigateToFolder(request.getNodeHandle());
						parentHandleIncoming = request.getNodeHandle();
					}
				}
			}
		}
		if (request.getType() == MegaRequest.TYPE_LOGIN){

			if (error.getErrorCode() != MegaError.API_OK) {

				MegaApplication.setLoggingIn(false);

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
				credentials = new UserCredentials(lastEmail, gSession, "", "", "");

				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				dbH.clearCredentials();
				
				log("Logged in with session");

				megaApi.fetchNodes(this);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){

			if (error.getErrorCode() == MegaError.API_OK){

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Window window = this.getWindow();
					window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
					window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
					window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));

				}
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				
				gSession = megaApi.dumpSession();
				MegaUser myUser = megaApi.getMyUser();
				String myUserHandle = "";
				if(myUser!=null){
					lastEmail = megaApi.getMyUser().getEmail();
					myUserHandle = megaApi.getMyUser().getHandle()+"";
				}

				credentials = new UserCredentials(lastEmail, gSession, "", "", myUserHandle);
				
				dbH.saveCredentials(credentials);
				
				loginLoggingIn.setVisibility(View.GONE);

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
						afterLoginAndFetch();
					}
					else{

						log("Chat NOT enabled - readyToManager");
						MegaApplication.setLoggingIn(false);
						afterLoginAndFetch();
					}
				}
				else{

					log("chatSettings NULL - readyToManager");
					MegaApplication.setLoggingIn(false);
					afterLoginAndFetch();

				}
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
	public void onEvent(MegaApiJava api, MegaEvent event) {

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
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
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
			case R.id.cab_menu_new_chat:{

				if(megaApi!=null && megaApi.getRootNode()!=null){
					ArrayList<MegaUser> contacts = megaApi.getContacts();
					if(contacts==null){
						showSnackbar("You have no MEGA contacts. Please, invite friends from the Contacts section");
					}
					else {
						if(contacts.isEmpty()){
							showSnackbar("You have no MEGA contacts. Please, invite friends from the Contacts section");
						}
						else{
							Intent in = new Intent(this, AddContactActivityLollipop.class);
							in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
							startActivityForResult(in, Constants.REQUEST_CREATE_CHAT);
						}
					}
				}
				else{
					log("Online but not megaApi");
					Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				}
			}
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		log("-------------------onActivityResult " + requestCode + "____" + resultCode);

		if (requestCode == Constants.REQUEST_CREATE_CHAT && resultCode == RESULT_OK) {
			log("onActivityResult REQUEST_CREATE_CHAT OK");

			if (intent == null) {
				log("Return.....");
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);

			if (contactsData != null){
				if(contactsData.size()==1){
					MegaUser user = megaApi.getContact(contactsData.get(0));
					if(user!=null){
						log("Chat with contact: "+contactsData.size());
						startOneToOneChat(user);
					}
				}
				else{
					MegaChatPeerList peers = MegaChatPeerList.createInstance();
					for (int i=0; i<contactsData.size(); i++){
						MegaUser user = megaApi.getContact(contactsData.get(i));
						if(user!=null){
							peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
						}
					}
					log("create group chat with participants: "+peers.size());
					final String chatTitle = intent.getStringExtra(AddContactActivityLollipop.EXTRA_CHAT_TITLE);
					if(chatTitle!=null){
						CreateGroupChatWithTitle listener = new CreateGroupChatWithTitle(this, chatTitle);
						megaChatApi.createChat(true, peers, listener);
					}
					else{
						megaChatApi.createChat(true, peers, this);
					}
				}
			}
		}
	}

	public void onRequestFinishCreateChat(int errorCode, long chatHandle){
		log("onRequestFinishCreateChat");

		if(errorCode==MegaChatError.ERROR_OK){
			log("Chat CREATED.");
			//Update chat view
			String chatTag1;

			if(isChatFirst){
				chatTag1 = getFragmentTag(R.id.explorer_tabs_pager, 0);
			}
			else{
				chatTag1 = getFragmentTag(R.id.explorer_tabs_pager, 2);
			}
			chatExplorer = (ChatExplorerFragment) getSupportFragmentManager().findFragmentByTag(chatTag1);
			if(chatExplorer!=null && chatExplorer.isAdded()){
				chatExplorer.setChats();
			}
		}
		else{
			log("EEEERRRRROR WHEN CREATING CHAT " + errorCode);
			showSnackbar(getString(R.string.create_chat_error));
		}
	}

	public void startOneToOneChat(MegaUser user){
		log("startOneToOneChat");
		MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
		MegaChatPeerList peers = MegaChatPeerList.createInstance();
		if(chat==null){
			log("No chat, create it!");
			peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
			megaChatApi.createChat(false, peers, this);
		}
		else{
			log("There is already a chat, open it!");
			showSnackbar(getString(R.string.chat_already_exists));
		}
	}

	public void showNewFolderDialog(){
		log("showNewFolderDialog");
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(17, outMetrics), 0);

		final RelativeLayout error_layout = new RelativeLayout(FileExplorerActivityLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(FileExplorerActivityLollipop.this);
		error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout.addView(error_icon);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();


		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon.setLayoutParams(params_icon);

		error_icon.setColorFilter(ContextCompat.getColor(FileExplorerActivityLollipop.this, R.color.login_warning));

		final TextView textError = new TextView(FileExplorerActivityLollipop.this);
		error_layout.addView(textError);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(Util.scaleWidthPx(3, outMetrics), 0,0,0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(FileExplorerActivityLollipop.this, R.color.login_warning));
		error_layout.setVisibility(View.GONE);

		input.getBackground().mutate().clearColorFilter();
		input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		input.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if(error_layout.getVisibility() == View.VISIBLE){
					error_layout.setVisibility(View.GONE);
					input.getBackground().mutate().clearColorFilter();
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
		input.setHint(getString(R.string.context_new_folder_name));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();

					if (value.length() == 0) {
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_string));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();

					}else{
						boolean result=matches(regex, value);
						if(result){
							input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
							textError.setText(getString(R.string.invalid_characters));
							error_layout.setVisibility(View.VISIBLE);
							input.requestFocus();

						}else{
							createFolder(value);
							newFolderDialog.dismiss();
						}
					}
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
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				input.getBackground().clearColorFilter();
			}
		});
		builder.setView(layout);
		newFolderDialog = builder.create();
		newFolderDialog.show();
		newFolderDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String value = input.getText().toString().trim();
				if (value.length() == 0) {
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError.setText(getString(R.string.invalid_string));
					error_layout.setVisibility(View.VISIBLE);
					input.requestFocus();
				}else{
					boolean result=matches(regex, value);
					if(result){
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_characters));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();
					}else{
						createFolder(value);
						newFolderDialog.dismiss();
					}
				}
			}
		});
	}

	@Override
	public void onClick(View v) {
		log("onClick");

		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		switch(v.getId()) {
			case R.id.fab_file_explorer: {
				String chatTag1;

				if(isChatFirst){
					chatTag1 = getFragmentTag(R.id.explorer_tabs_pager, 0);
				}
				else{
					chatTag1 = getFragmentTag(R.id.explorer_tabs_pager, 2);
				}
				chatExplorer = (ChatExplorerFragment) getSupportFragmentManager().findFragmentByTag(chatTag1);
				if(chatExplorer!=null && chatExplorer.isAdded()){
					if(chatExplorer.getSelectedChats()!=null){
						sendToChats(chatExplorer.getSelectedChats());
					}
				}
				break;
			}
		}
	}

	public void sendToChats(ArrayList<MegaChatListItem> chatListItems){
		log("sendToChats");

		this.chatListItems = chatListItems;

		if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
			if ("text/plain".equals(intent.getType())) {
				log("Handle intent of text plain");
				Bundle extras = intent.getExtras();
				if (extras != null) {
					if (!extras.containsKey(Intent.EXTRA_STREAM)) {
						StringBuilder body = new StringBuilder();
						String sharedText2 = intent.getStringExtra(Intent.EXTRA_SUBJECT);
						if (sharedText2 != null) {
							body.append(getString(R.string.new_file_subject_when_uploading) + ": ");
							body.append(sharedText2);
						}
						String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
						if (sharedText != null) {
							body.append("\n");
							body.append(getString(R.string.new_file_content_when_uploading) + ": ");
							body.append(sharedText);
						}
						String sharedText3 = intent.getStringExtra(Intent.EXTRA_EMAIL);
						if (sharedText3 != null) {
							body.append("\n");
							body.append(getString(R.string.new_file_email_when_uploading) + ": ");
							body.append(sharedText3);
						}

						for(int i=0; i < chatListItems.size();i++){
							megaChatApi.sendMessage(chatListItems.get(i).getChatId(), body.toString());
						}

						if(chatListItems.size()==1){
							MegaChatListItem chatItem = chatListItems.get(0);
							long idChat = chatItem.getChatId();
							if(chatItem!=null){
								Intent intent = new Intent(this, ManagerActivityLollipop.class);
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
								intent.putExtra("CHAT_ID", idChat);
								startActivity(intent);
							}
						}
						else{
							Intent chatIntent = new Intent(this, ManagerActivityLollipop.class);
							chatIntent.setAction(Constants.ACTION_CHAT_SUMMARY);
							startActivity(chatIntent);
						}
					}
				}
			}
			else{
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
				else{
//			onIntentProcessed();
				}
			}
		}
		else{
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
			else{
//			onIntentProcessed();
			}
		}
	}

	public void showNewFileDialog(final MegaNode parentNode, final String data){
		log("showNewFileDialog");
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(17, outMetrics), 0);

		final RelativeLayout error_layout = new RelativeLayout(FileExplorerActivityLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(FileExplorerActivityLollipop.this);
		error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout.addView(error_icon);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();

		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon.setLayoutParams(params_icon);

		error_icon.setColorFilter(ContextCompat.getColor(FileExplorerActivityLollipop.this, R.color.login_warning));

		final TextView textError = new TextView(FileExplorerActivityLollipop.this);
		error_layout.addView(textError);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(Util.scaleWidthPx(3, outMetrics), 0,0,0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(FileExplorerActivityLollipop.this, R.color.login_warning));
		error_layout.setVisibility(View.GONE);

		input.getBackground().mutate().clearColorFilter();
		input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		input.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if(error_layout.getVisibility() == View.VISIBLE){
					error_layout.setVisibility(View.GONE);
					input.getBackground().mutate().clearColorFilter();
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.text_secondary));
		input.setHint(getString(R.string.context_new_file_name));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);


		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_string));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();
					}else{
						boolean result=matches(regex, value);
						if(result){
							input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
							textError.setText(getString(R.string.invalid_characters));
							error_layout.setVisibility(View.VISIBLE);
							input.requestFocus();

						}else{
							createFile(value, data, parentNode);
							newFolderDialog.dismiss();
						}
					}
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
		builder.setTitle(getString(R.string.dialog_title_new_file));
		builder.setPositiveButton(getString(R.string.general_create),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						createFile(value, data, parentNode);
					}
				});

		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				input.getBackground().clearColorFilter();
			}
		});
		builder.setView(layout);
		newFolderDialog = builder.create();
		newFolderDialog.show();

		newFolderDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				String value = input.getText().toString().trim();
				if (value.length() == 0) {
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError.setText(getString(R.string.invalid_string));
					error_layout.setVisibility(View.VISIBLE);
					input.requestFocus();
				}else{
					boolean result=matches(regex, value);
					if(result){
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_characters));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();
					}else{
						createFile(value, data, parentNode);
						newFolderDialog.dismiss();
					}
				}
			}
		});
	}

	public static boolean matches(String regex, CharSequence input) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(input);
		return m.find();
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

	public MegaNode parentMoveCopy(){
			return parentMoveCopy;

	}

    public ArrayList<Long> getNodeHandleMoveCopy() {
        return nodeHandleMoveCopy;
    }

	public int getDeepBrowserTree() {
		return deepBrowserTree;
	}

	public void setDeepBrowserTree(int deep) {
		deepBrowserTree=deep;
	}

	public void increaseDeepBrowserTree() {
		deepBrowserTree++;
	}

	public void decreaseDeepBrowserTree() {
		deepBrowserTree--;
	}

}
