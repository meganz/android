package mega.privacy.android.app.lollipop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.lollipop.adapters.FileExplorerPagerAdapter;
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink;
import mega.privacy.android.app.lollipop.listeners.CreateChatToPerformActionListener;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerFragment;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerListItem;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.ChatUploadService;
import mega.privacy.android.app.lollipop.megachat.PendingMessageSingle;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatPresenceConfig;
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
import nz.mega.sdk.MegaUserAlert;

import static android.webkit.URLUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class FileExplorerActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaChatRequestListenerInterface, View.OnClickListener, MegaChatListenerInterface {

	public final static int CLOUD_FRAGMENT = 0;
	public final static int INCOMING_FRAGMENT = 1;
	public final static int CHAT_FRAGMENT = 3;
	public final static int IMPORT_FRAGMENT = 4;

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

	AppBarLayout abL;
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
	MenuItem searchMenuItem;

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

	ArrayList<MegaChatRoom> chatListItems;

	private CloudDriveExplorerFragmentLollipop cDriveExplorer;
	private IncomingSharesExplorerFragmentLollipop iSharesExplorer;
	private ChatExplorerFragment chatExplorer;
	private ImportFilesFragment importFileFragment;

	private AlertDialog newFolderDialog;
	
	ProgressDialog statusDialog;

	private List<ShareInfo> filePreparedInfos;

	//Tabs in Cloud
	TabLayout tabLayoutExplorer;
	FileExplorerPagerAdapter mTabsAdapterExplorer;
	ViewPager viewPagerExplorer;

	ArrayList<MegaNode> nodes;

	String regex = "[*|\\?:\"<>\\\\\\\\/]";

	//	long gParentHandle;
	long parentHandleIncoming;
	long parentHandleCloud;
	int deepBrowserTree = 0;

	Intent intent = null;
	boolean importFileF = false;
	int importFragmentSelected = -1;
	String action = null;
    private android.support.v7.app.AlertDialog renameDialog;
	HashMap<String, String> nameFiles = new HashMap<>();

	MegaNode myChatFilesNode;
	ArrayList<MegaNode> attachNodes = new ArrayList<>();
	ArrayList<ShareInfo> uploadInfos = new ArrayList<>();
	int filesChecked = 0;

	SearchView searchView;

	FileExplorerActivityLollipop fileExplorerActivityLollipop;

	private String querySearch = "";
	private boolean isSearchExpanded = false;
	private boolean pendingToOpenSearchView = false;
	private int pendingToAttach = 0;
	private int totalAttached = 0;
	private int totalErrors = 0;

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		logDebug("onRequestFinish(CHAT)");

		if (request.getType() == MegaChatRequest.TYPE_CONNECT){
			MegaApplication.setLoggingIn(false);
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("Connected to chat!");
			}
			else{
				logWarning("ERROR WHEN CONNECTING " + e.getErrorString());
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
			logDebug("Create chat request finish.");
			onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), false);
		}
		else if (request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){
			logDebug("Attach file request finish.");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				totalAttached++;
			}
			else{
				totalErrors++;
			}
			if (totalAttached+totalErrors == pendingToAttach) {
				if (totalErrors == 0 || totalAttached > 0) {
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.setAction(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE);
					if (chatListItems.size() == 1) {
						intent.putExtra("CHAT_ID", chatListItems.get(0).getChatId());
					}
					startActivity(intent);
				}
				else {
					showSnackbar(getString(R.string.files_send_to_chat_error));
				}
				finishFileExplorer();
			}
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
			logDebug("OwnFilePrepareTask: doInBackground");
			return ShareInfo.processIntent(params[0], context);
		}

		@Override
		protected void onPostExecute(List<ShareInfo> info) {
			filePreparedInfos = info;
			if (action != null && getIntent() != null) {
				getIntent().setAction(action);
			}
			if (importFileF) {
				if (importFragmentSelected != -1) {
					chooseFragment(importFragmentSelected);
				}
				else {
					chooseFragment(IMPORT_FRAGMENT);
				}

				if (statusDialog != null) {
					try {
						statusDialog.dismiss();
					}
					catch(Exception ex){}
				}
			}
			else {
				onIntentProcessed();
			}
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
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		logDebug("onCreate first");
		super.onCreate(savedInstanceState);

		if(savedInstanceState!=null){
			logDebug("Bundle is NOT NULL");
			parentHandleCloud = savedInstanceState.getLong("parentHandleCloud", -1);
			logDebug("savedInstanceState -> parentHandleCloud: " + parentHandleCloud);
			parentHandleIncoming = savedInstanceState.getLong("parentHandleIncoming", -1);
			logDebug("savedInstanceState -> parentHandleIncoming: " + parentHandleIncoming);
			deepBrowserTree = savedInstanceState.getInt("deepBrowserTree", 0);
			logDebug("savedInstanceState -> deepBrowserTree: " + deepBrowserTree);
			importFileF = savedInstanceState.getBoolean("importFileF", false);
			importFragmentSelected = savedInstanceState.getInt("importFragmentSelected", -1);
			action = savedInstanceState.getString("action", null);
			nameFiles = (HashMap<String, String>) savedInstanceState.getSerializable("nameFiles");
			chatExplorer = (ChatExplorerFragment) getSupportFragmentManager().getFragment(savedInstanceState, "chatExplorerFragment");
			querySearch = savedInstanceState.getString("querySearch", "");
			isSearchExpanded = savedInstanceState.getBoolean("isSearchExpanded", isSearchExpanded);
			pendingToAttach = savedInstanceState.getInt("pendingToAttach", 0);
			totalAttached = savedInstanceState.getInt("totalAttached", 0);
			totalErrors = savedInstanceState.getInt("totalErrors", 0);

			if (isSearchExpanded) {
				pendingToOpenSearchView = true;
			}
		}
		else{
			logDebug("Bundle is NULL");
			parentHandleCloud = -1;
			parentHandleIncoming = -1;
			deepBrowserTree = 0;
			importFileF = false;
			importFragmentSelected = -1;
			action = null;
			pendingToAttach = 0;
			totalAttached = 0;
			totalErrors = 0;
		}

		fileExplorerActivityLollipop = this;
				
		dbH = DatabaseHandler.getDbHandler(this);
		credentials = dbH.getCredentials();
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
		if (credentials == null){

			logWarning("User credentials NULL");
//			megaApi.localLogout();
//			AccountController aC = new AccountController(this);
//			aC.logout(this, megaApi, megaChatApi, false);
			
			Intent loginIntent = new Intent(this, LoginActivityLollipop.class);
			loginIntent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
			loginIntent.setAction(ACTION_FILE_EXPLORER_UPLOAD);
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
			finish();
			return;
		}
		else{
			logDebug("User has credentials");
		}
		
		if (savedInstanceState != null){
			folderSelected = savedInstanceState.getBoolean("folderSelected", false);
		}
	
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		megaApi.addGlobalListener(this);

		if (isChatEnabled()) {
			if (megaChatApi == null) {
				megaChatApi = ((MegaApplication)getApplication()).getMegaChatApi();
			}
		}
		
		setContentView(R.layout.activity_file_explorer);
		
		fragmentContainer = (RelativeLayout) findViewById(R.id.fragment_container_file_explorer);

		abL = (AppBarLayout) findViewById(R.id.app_bar_layout_explorer);
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_explorer);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		if(aB!=null){
			aB.hide();
		}
		else{
			logWarning("aB is null");
		}

		fabButton = (FloatingActionButton) findViewById(R.id.fab_file_explorer);
		fabButton.setOnClickListener(this);
		showFabButton(false);
		//TABS
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
			getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.transparent_black));

			logDebug("hide action bar");
			if (!MegaApplication.isLoggingIn()) {

				MegaApplication.setLoggingIn(true);

				getSupportActionBar().hide();
				tabLayoutExplorer.setVisibility(View.GONE);
				viewPagerExplorer.setVisibility(View.GONE);
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

				if(isChatEnabled()){
					logDebug("Chat is ENABLED");

					int ret = megaChatApi.getInitState();

					if(ret==MegaChatApi.INIT_NOT_DONE||ret==MegaChatApi.INIT_ERROR){
						ret = megaChatApi.init(gSession);
						logDebug("Result of init ---> " + ret);
						chatSettings = dbH.getChatSettings();
						if (ret == MegaChatApi.INIT_NO_CACHE) {
							logDebug("Condition ret == MegaChatApi.INIT_NO_CACHE");
						}
						else if (ret == MegaChatApi.INIT_ERROR) {
							logDebug("Condition ret == MegaChatApi.INIT_ERROR");
							if(chatSettings == null) {
								logWarning("ERROR----> Switch OFF chat");
								chatSettings = new ChatSettings();
								chatSettings.setEnabled(false+"");
								dbH.setChatSettings(chatSettings);
							} else{
								logWarning("ERROR----> Switch OFF chat");
								dbH.setEnabledChat(false + "");
							}
							megaChatApi.logout(this);
						}
						else{
							logDebug("onCreate: Chat correctly initialized");
						}
					}
				}

				megaApi.fastLogin(gSession, this);
			}
			else{
				logWarning("Another login is proccessing");
			}
		}
		else{
			getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

			afterLoginAndFetch();
		}

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

	}
	
	private void afterLoginAndFetch(){
		handler = new Handler();

		logDebug("SHOW action bar");
		if(aB==null){
			aB=getSupportActionBar();
		}
		aB.show();
		logDebug("aB.setHomeAsUpIndicator");
		aB.setHomeAsUpIndicator(mutateIcon(this, R.drawable.ic_arrow_back_white, R.color.black));
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);

		if ((intent != null) && (intent.getAction() != null)){
			logDebug("intent OK: " + intent.getAction());
			if (intent.getAction().equals(ACTION_SELECT_FOLDER_TO_SHARE)){
				logDebug("action = ACTION_SELECT_FOLDER_TO_SHARE");
				//Just show Cloud Drive, no INCOMING tab , no need of tabhost

				mode = SELECT;
				selectFile = false;
				selectedContacts=intent.getStringArrayListExtra("SELECTED_CONTACTS");

				aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());

				cloudDriveFrameLayout = (FrameLayout) findViewById(R.id.cloudDriveFrameLayout);

				if(cDriveExplorer==null){
					cDriveExplorer = new CloudDriveExplorerFragmentLollipop();
				}

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.cloudDriveFrameLayout, cDriveExplorer, "cDriveExplorer");
				ft.commitNowAllowingStateLoss();

				cloudDriveFrameLayout.setVisibility(View.VISIBLE);

				tabLayoutExplorer.setVisibility(View.GONE);
				viewPagerExplorer.setVisibility(View.GONE);

				tabShown=NO_TABS;

			}
			else if (intent.getAction().equals(ACTION_SELECT_FILE)){
				logDebug("action = ACTION_SELECT_FILE");
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
				ft.commitNowAllowingStateLoss();

				cloudDriveFrameLayout.setVisibility(View.VISIBLE);

				tabLayoutExplorer.setVisibility(View.GONE);
				viewPagerExplorer.setVisibility(View.GONE);

				tabShown=NO_TABS;
			}
			else if (intent.getAction().equals(ACTION_MULTISELECT_FILE)){
				logDebug("action = ACTION_MULTISELECT_FILE");
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
				ft.commitNowAllowingStateLoss();

				cloudDriveFrameLayout.setVisibility(View.VISIBLE);

				tabLayoutExplorer.setVisibility(View.GONE);
				viewPagerExplorer.setVisibility(View.GONE);

				tabShown=NO_TABS;
			}
			else{

				if (intent.getAction().equals(ACTION_PICK_MOVE_FOLDER)){
					logDebug("ACTION_PICK_MOVE_FOLDER");
					mode = MOVE;
					moveFromHandles = intent.getLongArrayExtra("MOVE_FROM");

					aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());

					if (mTabsAdapterExplorer == null){
						tabLayoutExplorer.setVisibility(View.VISIBLE);
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
						logDebug("mTabsAdapterExplorer != null");
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
					logDebug("ACTION_PICK_COPY_FOLDER");
					mode = COPY;
					copyFromHandles = intent.getLongArrayExtra("COPY_FROM");

					aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());

					if (mTabsAdapterExplorer == null){
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
					logDebug("action = ACTION_CHOOSE_MEGA_FOLDER_SYNC");
					mode = SELECT_CAMERA_FOLDER;

					aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());

					if (mTabsAdapterExplorer == null){
						tabLayoutExplorer.setVisibility(View.VISIBLE);
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

					aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());

					if (mTabsAdapterExplorer == null){
						tabLayoutExplorer.setVisibility(View.VISIBLE);
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
					logDebug("action = ACTION_SELECT_FOLDER");
					mode = SELECT;
					selectedContacts=intent.getStringArrayListExtra("SELECTED_CONTACTS");

					aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());

					if (mTabsAdapterExplorer == null){
						tabLayoutExplorer.setVisibility(View.VISIBLE);
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
					logDebug("action = UPLOAD to Cloud Drive");
					mode = UPLOAD;
					selectFile = false;

					aB.setTitle(getString(R.string.title_cloud_explorer).toUpperCase());

					cloudDriveFrameLayout = (FrameLayout) findViewById(R.id.cloudDriveFrameLayout);

					if(cDriveExplorer==null){
						cDriveExplorer = new CloudDriveExplorerFragmentLollipop();
					}

					FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
					ft.replace(R.id.cloudDriveFrameLayout, cDriveExplorer, "cDriveExplorer");
					ft.commitNowAllowingStateLoss();

					cloudDriveFrameLayout.setVisibility(View.VISIBLE);

					tabLayoutExplorer.setVisibility(View.GONE);
					viewPagerExplorer.setVisibility(View.GONE);

					tabShown=NO_TABS;
				}
				else{
					logDebug("action = UPLOAD");
					mode = UPLOAD;

					if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
						if ("text/plain".equals(intent.getType())) {
							logDebug("Handle intent of text plain");
							Bundle extras = intent.getExtras();
							if(extras!=null) {
								if (!extras.containsKey(Intent.EXTRA_STREAM)) {
									isChatFirst = true;
								}
							}
						}
					}

					if(isChatFirst){
						aB.setTitle(getString(R.string.title_chat_explorer).toUpperCase());
						if (mTabsAdapterExplorer == null){
							tabLayoutExplorer.setVisibility(View.VISIBLE);
							viewPagerExplorer.setVisibility(View.VISIBLE);
                            if (isChatEnabled()) {
                                mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this, true);
                            }
                            else {
                            	isChatFirst = false;
                                mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(),this);
                            }
							viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
							tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

							if (!isChatEnabled() && mTabsAdapterExplorer != null && mTabsAdapterExplorer.getCount() > 2) {
                                tabLayoutExplorer.removeTabAt(2);
                            }
						}
					}
					else{
						aB.setTitle(getString(R.string.title_upload_explorer).toUpperCase());
						importFileF = true;
						action = intent.getAction();
						cloudDriveFrameLayout = (FrameLayout) findViewById(R.id.cloudDriveFrameLayout);
						OwnFilePrepareTask ownFilePrepareTask = new OwnFilePrepareTask(this);
						ownFilePrepareTask.execute(getIntent());
						createAndShowProgressDialog(false, R.string.upload_prepare);

						cloudDriveFrameLayout.setVisibility(View.VISIBLE);

						tabLayoutExplorer.setVisibility(View.GONE);
						viewPagerExplorer.setVisibility(View.GONE);
						tabShown=NO_TABS;
					}
				}

				viewPagerExplorer.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
					public void onPageScrollStateChanged(int state) {}
					public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

					public void onPageSelected(int position) {
						logDebug("Position:"+ position);
						supportInvalidateOptionsMenu();
						changeTitle();
					}
				});
			}

		}
		else{
			logWarning("intent error");
		}
	}

	public void chooseFragment (int fragment) {
		importFragmentSelected = fragment;
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if (fragment == CLOUD_FRAGMENT) {
			if(cDriveExplorer==null){
				cDriveExplorer = new CloudDriveExplorerFragmentLollipop();
			}
			ft.replace(R.id.cloudDriveFrameLayout, cDriveExplorer, "cDriveExplorer");
		}
		else if (fragment == INCOMING_FRAGMENT) {
			if(iSharesExplorer==null){
				iSharesExplorer = new IncomingSharesExplorerFragmentLollipop();
			}
			ft.replace(R.id.cloudDriveFrameLayout, iSharesExplorer, "iSharesExplorer");
		}
		else if (fragment == CHAT_FRAGMENT) {
			if(chatExplorer==null){
				chatExplorer = new ChatExplorerFragment();
			}
			ft.replace(R.id.cloudDriveFrameLayout, chatExplorer, "chatExplorer");
		}
		else if (fragment == IMPORT_FRAGMENT){
			if(importFileFragment==null){
				importFileFragment = new ImportFilesFragment();
			}
			ft.replace(R.id.cloudDriveFrameLayout, importFileFragment, "importFileFragment");
		}
		ft.commitNowAllowingStateLoss();
		supportInvalidateOptionsMenu();
		changeTitle();
	}

	public void showFabButton(boolean show){
		if(show){
			fabButton.setVisibility(View.VISIBLE);
		}
		else{
			fabButton.setVisibility(View.GONE);
		}
	}

	public void changeActionBarElevation(boolean whitElevation) {
		chatExplorer = getChatExplorerFragment();
		if (chatExplorer == null || chatExplorer.isHidden()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				if (whitElevation) {
					abL.setElevation(px2dp(4, outMetrics));
				}
				else {
					abL.setElevation(0);
				}
			}
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		logDebug("onCreateOptionsMenuLollipop");
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.file_explorer_action, menu);

	    searchMenuItem = menu.findItem(R.id.cab_menu_search);
	    searchMenuItem.setIcon(mutateIconSecondary(this, R.drawable.ic_menu_search, R.color.black));
	    createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder);
	    newChatMenuItem = menu.findItem(R.id.cab_menu_new_chat);

	    searchMenuItem.setVisible(false);
		createFolderMenuItem.setVisible(false);
		newChatMenuItem.setVisible(false);

		searchView = (SearchView) searchMenuItem.getActionView();

		SearchView.SearchAutoComplete searchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
		searchAutoComplete.setTextColor(ContextCompat.getColor(this, R.color.black));
		searchAutoComplete.setHintTextColor(ContextCompat.getColor(this, R.color.status_bar_login));
		searchAutoComplete.setHint(getString(R.string.hint_action_search));
		View v = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
		v.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

		if (searchView != null){
			searchView.setIconifiedByDefault(true);
		}

		searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				isSearchExpanded = true;
				chatExplorer = getChatExplorerFragment();
				if (chatExplorer != null) {
					chatExplorer.enableSearch(true);
				}
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				isSearchExpanded = false;
				chatExplorer = getChatExplorerFragment();
				if (chatExplorer != null) {
					chatExplorer.enableSearch(false);
				}
				return true;
			}
		});

		searchView.setMaxWidth(Integer.MAX_VALUE);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				logDebug("Query: " + query);
				hideKeyboard(fileExplorerActivityLollipop, 0);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				querySearch = newText;
				chatExplorer = getChatExplorerFragment();
				if (chatExplorer != null) {
					chatExplorer.search(newText);
				}
				return true;
			}
		});

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

	public void isPendingToOpenSearchView () {
		if (pendingToOpenSearchView) {
			String query = querySearch;
			searchMenuItem.expandActionView();
			searchView.setQuery(query, false);
			pendingToOpenSearchView = false;
		}
	}

	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		logDebug("onPrepareOptionsMenuLollipop");

	    //Check the tab shown
		if (viewPagerExplorer != null && tabShown != NO_TABS){

			int index = viewPagerExplorer.getCurrentItem();

			if(index==0){
				if(isChatFirst){
					searchMenuItem.setVisible(true);
					createFolderMenuItem.setVisible(false);
					newChatMenuItem.setVisible(false);
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
						logDebug("Level deepBrowserTree: " + deepBrowserTree);
						if (deepBrowserTree==0){
							createFolderMenuItem.setVisible(false);
						}
						else{
							//Check the folder's permissions
							long parentH = iSharesExplorer.getParentHandle();
							MegaNode n = megaApi.getNodeByHandle(parentH);
							int accessLevel= megaApi.getAccess(n);
							logDebug("Node: " + n.getHandle() + ", Permissions: " + accessLevel);

							switch(accessLevel){
								case MegaShare.ACCESS_OWNER:
								case MegaShare.ACCESS_READWRITE:
								case MegaShare.ACCESS_FULL:
									createFolderMenuItem.setVisible(true);
									break;
								case MegaShare.ACCESS_READ:
									createFolderMenuItem.setVisible(false);
									break;
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
						logDebug("Level deepBrowserTree: " + deepBrowserTree);
						if (deepBrowserTree==0){
							createFolderMenuItem.setVisible(false);
						}
						else{
							//Check the folder's permissions
							long parentH = iSharesExplorer.getParentHandle();
							MegaNode n = megaApi.getNodeByHandle(parentH);
							int accessLevel= megaApi.getAccess(n);
							logDebug("Node: " + n.getHandle() + ", Permissions: " + accessLevel);

							switch(accessLevel){
								case MegaShare.ACCESS_OWNER:
								case MegaShare.ACCESS_READWRITE:
								case MegaShare.ACCESS_FULL:
									createFolderMenuItem.setVisible(true);
									break;
								case MegaShare.ACCESS_READ:
									createFolderMenuItem.setVisible(false);
									break;
							}
						}
					}
					newChatMenuItem.setVisible(false);
				}
				else{
					searchMenuItem.setVisible(true);
					createFolderMenuItem.setVisible(false);
					newChatMenuItem.setVisible(false);
				}
			}

		}
		else{
			if (cDriveExplorer != null && !importFileF){
				createFolderMenuItem.setVisible(true);
			}
			else if (importFileF) {
				if (importFragmentSelected != -1 ) {
					switch (importFragmentSelected) {
						case CLOUD_FRAGMENT: {
							createFolderMenuItem.setVisible(true);
							break;
						}
						case INCOMING_FRAGMENT:{
							iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag("iSharesExplorer");
							if(iSharesExplorer != null){
								if (deepBrowserTree > 0) {
									//Check the folder's permissions
									long parentH = iSharesExplorer.getParentHandle();
									MegaNode n = megaApi.getNodeByHandle(parentH);
									int accessLevel= megaApi.getAccess(n);

									switch(accessLevel){
										case MegaShare.ACCESS_OWNER:
										case MegaShare.ACCESS_READWRITE:
										case MegaShare.ACCESS_FULL:{
											createFolderMenuItem.setVisible(true);
											break;
										}
										case MegaShare.ACCESS_READ:{
											createFolderMenuItem.setVisible(false);
											break;
										}
									}
								}
							}
							break;
						}
						case CHAT_FRAGMENT:{
							newChatMenuItem.setVisible(false);
							searchMenuItem.setVisible(true);
							break;
						}
					}
				}
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
	

	public void setRootTitle(){
		logDebug("setRootTitle");

		if(mode == SELECT){
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
				aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());
			}
		}
		else if(mode == MOVE || mode == COPY || mode == SELECT_CAMERA_FOLDER || mode == IMPORT){
			aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());
		}
		else if(mode == UPLOAD && !importFileF){
			aB.setTitle(getString(R.string.title_cloud_explorer).toUpperCase());
		}
		else if (mode == UPLOAD && importFileF) {
			if (importFragmentSelected != -1) {
				switch (importFragmentSelected) {
					case CLOUD_FRAGMENT: {
						aB.setTitle(getString(R.string.section_cloud_drive).toUpperCase());
						break;
					}
					case INCOMING_FRAGMENT:{
						aB.setTitle(getString(R.string.title_incoming_shares_explorer).toUpperCase());
						break;
					}
					case CHAT_FRAGMENT:{
						aB.setTitle(getString(R.string.title_chat_explorer).toUpperCase());
						break;
					}
					case IMPORT_FRAGMENT:{
						aB.setTitle(getString(R.string.title_upload_explorer).toUpperCase());
						break;
					}
				}
			}
		}
	}

	public void changeTitle (){
		logDebug("changeTitle");

		if(tabShown==NO_TABS){
			if (importFileF) {
				if (importFragmentSelected != -1) {
					switch (importFragmentSelected) {
						case CLOUD_FRAGMENT: {
							cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag("cDriveExplorer");

							if(cDriveExplorer!=null){
								if(cDriveExplorer.parentHandle==-1|| cDriveExplorer.parentHandle==megaApi.getRootNode().getHandle()){
									setRootTitle();
								}
								else{
									aB.setTitle(megaApi.getNodeByHandle(cDriveExplorer.parentHandle).getName());
								}
							}
							break;
						}
						case INCOMING_FRAGMENT:{
							iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag("iSharesExplorer");

							if(iSharesExplorer!=null){
								if(deepBrowserTree==0){
									setRootTitle();
								}
								else{
									aB.setTitle(megaApi.getNodeByHandle(iSharesExplorer.parentHandle).getName());
								}
							}
							break;
						}
						case CHAT_FRAGMENT:{
							setRootTitle();
							break;
						}
						case IMPORT_FRAGMENT:{
							setRootTitle();
							break;
						}
					}
				}
			}
			else {
				cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag("cDriveExplorer");

				if(cDriveExplorer!=null){
					if(cDriveExplorer.parentHandle==-1|| cDriveExplorer.parentHandle==megaApi.getRootNode().getHandle()){
						setRootTitle();
					}
					else{
						aB.setTitle(megaApi.getNodeByHandle(cDriveExplorer.parentHandle).getName());
					}
				}
				showFabButton(false);
			}
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
							aB.setTitle(getString(R.string.title_chat_explorer).toUpperCase());
						}

//						if(((ChatExplorerFragment)f).getSelectedChats().size() > 0){
//							showFabButton(true);
//						}
//						else{
//							showFabButton(false);
//						}
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

						showFabButton(false);
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

						if(deepBrowserTree==0){
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
				showFabButton(false);
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
							aB.setTitle(getString(R.string.title_chat_explorer).toUpperCase());
						}

//						if(((ChatExplorerFragment)f).getSelectedChats().size() > 0){
//							showFabButton(true);
//						}
//						else{
//							showFabButton(false);
//						}
					}
					else if(f instanceof IncomingSharesExplorerFragmentLollipop){

						if(tabShown!=NO_TABS){
							tabShown=INCOMING_TAB;
						}

						if(deepBrowserTree==0){
							setRootTitle();
						}
						else{
							aB.setTitle(megaApi.getNodeByHandle(((IncomingSharesExplorerFragmentLollipop)f).parentHandle).getName());
						}

						showFabButton(false);
					}
				}
			}
		}
		supportInvalidateOptionsMenu();
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
		logDebug("onSaveInstanceState");
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
		if (importFileF) {
			cFTag1 = "iSharesExplorer";
		}
		else {
			if (isChatFirst) {
				cFTag1 = getFragmentTag(R.id.explorer_tabs_pager, 2);
			}
			else {
				cFTag1 = getFragmentTag(R.id.explorer_tabs_pager, 1);
			}
		}

		iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
		if(iSharesExplorer!=null){
			parentHandleIncoming = iSharesExplorer.getParentHandle();
		}
		else{
			parentHandleIncoming = -1;
		}
		bundle.putLong("parentHandleIncoming", parentHandleIncoming);
		bundle.putInt("deepBrowserTree", deepBrowserTree);
		logDebug("IN BUNDLE -> deepBrowserTree: " + deepBrowserTree);

		bundle.putBoolean("importFileF", importFileF);
		bundle.putInt("importFragmentSelected", importFragmentSelected);
		bundle.putString("action", action);
		bundle.putSerializable("nameFiles", nameFiles);

		if (getChatExplorerFragment() != null) {
			getSupportFragmentManager().putFragment(bundle, "chatExplorerFragment", getChatExplorerFragment());
		}

		bundle.putString("querySearch", querySearch);
		bundle.putBoolean("isSearchExpanded", isSearchExpanded);
		bundle.putInt("pendingToAttach", pendingToAttach);
		bundle.putInt("totalAttached", totalAttached);
		bundle.putInt("totalErrors", totalErrors);
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
						createAndShowProgressDialog(false, R.string.upload_prepare);
					}
				}
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		logDebug("tabShown: " + tabShown);
		retryConnectionsAndSignalPresence();

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
			importFileFragment = (ImportFilesFragment) getSupportFragmentManager().findFragmentByTag("importFileFragment");

			if (importFileF) {
				switch (importFragmentSelected) {
					case CLOUD_FRAGMENT: {
						if(cDriveExplorer!=null){
							if (cDriveExplorer.onBackPressed() == 0){
								chooseFragment(IMPORT_FRAGMENT);
							}
						}
						break;
					}
					case INCOMING_FRAGMENT:{
						if(iSharesExplorer!=null && iSharesExplorer.isAdded()){
							if (iSharesExplorer.onBackPressed() == 0){
								iSharesExplorer = null;
								chooseFragment(IMPORT_FRAGMENT);
							}
						}
						break;
					}
					case CHAT_FRAGMENT:{
						chatExplorer = getChatExplorerFragment();
						if(chatExplorer!=null){
							showFabButton(false);
//							chatExplorer.clearSelections();
							chooseFragment(IMPORT_FRAGMENT);
						}
						break;
					}
					case IMPORT_FRAGMENT:{
						finishActivity();
						break;
					}
				}
			}
			else if(cDriveExplorer!=null){
				if (cDriveExplorer.onBackPressed() == 0){
					finishActivity();
				}
			}
		}
		else{
			super.onBackPressed();
		}

		setToolbarSubtitle(null);
	}

	long createPendingMessageDBH (long idChat, long timestamp, String fingerprint, ShareInfo info) {
		logDebug("Chat ID: "+ idChat +", Fingerprint: " + fingerprint);
		PendingMessageSingle pMsgSingle = new PendingMessageSingle();
		pMsgSingle.setChatId(idChat);
		pMsgSingle.setUploadTimestamp(timestamp);
		pMsgSingle.setFilePath(info.getFileAbsolutePath());
		pMsgSingle.setName(info.getTitle());
		pMsgSingle.setFingerprint(fingerprint);
		long idMessage = dbH.addPendingMessageFromExplorer(pMsgSingle);
		pMsgSingle.setId(idMessage);

		if(idMessage!=-1){
			logDebug("File: " + info.getTitle() + ", Size: " + info.getSize());
		}
		else{
			logWarning("Error when adding pending msg to the database");
		}
		return idMessage;
	}

	void startChatUploadService () {
		logDebug("Launch chat upload with files " + filePreparedInfos.size());
		filesChecked = 0;
		long[] attachNodeHandles;
		ArrayList<Long> pendMsgArray = new ArrayList<>();
		Intent intent = new Intent(this, ChatUploadService.class);

		if (chatListItems != null && !chatListItems.isEmpty()) {
			long[] idPendMsgs = new long[uploadInfos.size() * chatListItems.size()];
			HashMap<String, String> filesToUploadFingerPrint= new HashMap<>();
			if (attachNodes != null && !attachNodes.isEmpty()) {
//			There are exists files
				if (uploadInfos != null && uploadInfos.size() > 0) {
//					There are exist files and files for upload
					attachNodeHandles = new long[attachNodes.size()];
					for (int i=0; i<attachNodes.size(); i++) {
						attachNodeHandles[i] = attachNodes.get(i).getHandle();
					}

					long[] attachIdChats = new long[chatListItems.size()];
					for (int i=0; i<chatListItems.size(); i++) {
						attachIdChats[i] = chatListItems.get(i).getChatId();
					}
					intent.putExtra(ChatUploadService.EXTRA_ATTACH_CHAT_IDS, attachIdChats);
					intent.putExtra(ChatUploadService.EXTRA_ATTACH_FILES, attachNodeHandles);

					int pos = 0;
					for (ShareInfo info : uploadInfos) {
						long timestamp = System.currentTimeMillis()/1000;
						String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());
						filesToUploadFingerPrint.put(fingerprint, info.getFileAbsolutePath());
						for(MegaChatRoom item : chatListItems){
							idPendMsgs[pos] = createPendingMessageDBH(item.getChatId(), timestamp, fingerprint, info);
							pos++;
						}
					}
					intent.putExtra(ChatUploadService.EXTRA_UPLOAD_FILES_FINGERPRINTS, filesToUploadFingerPrint);
					intent.putExtra(ChatUploadService.EXTRA_PEND_MSG_IDS, idPendMsgs);
					intent.putExtra(ChatUploadService.EXTRA_COMES_FROM_FILE_EXPLORER, true);
					startService(intent);

					finishFileExplorer();
				}
				else {
//					All files exists, not necessary start ChatUploadService
					pendingToAttach = attachNodes.size() * chatListItems.size();
					for (MegaNode node : attachNodes) {
						for (MegaChatRoom item : chatListItems) {
							megaChatApi.attachNode(item.getChatId(), node.getHandle(), this);
						}
					}
				}
			}
			else {
//			All files for upload
				int pos = 0;
				for (ShareInfo info : filePreparedInfos) {
					long timestamp = System.currentTimeMillis()/1000;
					String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());
					if (fingerprint == null) {
						logWarning("Error, fingerprint == NULL is not possible to access file for some reason");
						continue;
					}
					filesToUploadFingerPrint.put(fingerprint, info.getFileAbsolutePath());
					for(MegaChatRoom item : chatListItems){
						idPendMsgs[pos] = createPendingMessageDBH(item.getChatId(), timestamp, fingerprint, info);
						pos++;
					}
				}
				intent.putExtra(ChatUploadService.EXTRA_UPLOAD_FILES_FINGERPRINTS, filesToUploadFingerPrint);
				intent.putExtra(ChatUploadService.EXTRA_PEND_MSG_IDS, idPendMsgs);
				intent.putExtra(ChatUploadService.EXTRA_COMES_FROM_FILE_EXPLORER, true);
				startService(intent);

				finishFileExplorer();
			}
		}
		else{
			filePreparedInfos = null;
			logWarning("ERROR null files to upload");
			finishActivity();
		}
	}

	void finishFileExplorer () {
		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			}
			catch(Exception ex){}
		}

		filePreparedInfos = null;
		logDebug("finish!!!");
		finishActivity();
	}

	void checkIfFilesExistsInMEGA () {
		for (ShareInfo info : filePreparedInfos) {
			String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());
			MegaNode node = megaApi.getNodeByFingerprint(fingerprint);
			if (node != null) {
				if (node.getParentHandle() == myChatFilesNode.getHandle()) {
//					File is in My Chat Files --> Add to attach
					attachNodes.add(node);
					filesChecked++;
				}
				else {
//					File is in Cloud --> Copy in My Chat Files
					megaApi.copyNode(node, myChatFilesNode, this);
				}
			}
			else {
				uploadInfos.add(info);
				filesChecked++;
			}
		}
		if (filesChecked == filePreparedInfos.size()) {
			startChatUploadService();
		}
	}

	/*
	 * Handle processed upload intent
	 */
	public void onIntentChatProcessed(List<ShareInfo> infos) {
		logDebug("onIntentChatProcessed");

		if (getIntent() != null && getIntent().getAction() != ACTION_PROCESSED) {
			getIntent().setAction(ACTION_PROCESSED);
		}

		if (infos == null) {
		    showSnackbar(getString(R.string.upload_can_not_open));
		}
		else {
			myChatFilesNode = megaApi.getNodeByPath("/"+CHAT_FOLDER);
			if(myChatFilesNode == null){
				logDebug("Create folder: " + CHAT_FOLDER);
				megaApi.createFolder(CHAT_FOLDER, megaApi.getRootNode(), this);
			}
			else {
				checkIfFilesExistsInMEGA();
			}
		}
	}
	
	public void onIntentProcessed() {
		List<ShareInfo> infos = filePreparedInfos;

		if (getIntent() != null && getIntent().getAction() != ACTION_PROCESSED) {
			getIntent().setAction(ACTION_PROCESSED);
		}

		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			}
			catch(Exception ex){}
		}

		logDebug("intent processed!");
		if (folderSelected) {
			if (infos == null) {
				showSnackbar(getString(R.string.upload_can_not_open));
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
				showSnackbar(getString(R.string.upload_began));
				for (ShareInfo info : infos) {
					Intent intent = new Intent(this, UploadService.class);
					intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
					intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
					if (nameFiles != null && nameFiles.get(info.getTitle()) != null && !nameFiles.get(info.getTitle()).equals(info.getTitle())) {
						intent.putExtra(UploadService.EXTRA_NAME_EDITED, nameFiles.get(info.getTitle()));
					}
					intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
					intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
					startService(intent);
				}
				filePreparedInfos = null;
				logDebug("finish!!!");
				finishActivity();
			}	
		}
	}

    public void buttonClick(long[] handles){
		logDebug("handles: " + handles.length);

        Intent intent = new Intent();
        intent.putExtra("NODE_HANDLES", handles);
        intent.putStringArrayListExtra("SELECTED_CONTACTS", selectedContacts);
        setResult(RESULT_OK, intent);
		finishActivity();
    }

    void createAndShowProgressDialog (boolean cancelable, int string) {
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(string));
			temp.setCancelable(cancelable);
			temp.setCanceledOnTouchOutside(cancelable);
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
	}
	
	public void buttonClick(long handle){
		logDebug("handle: " + handle);

		if (tabShown == INCOMING_TAB){
			if (deepBrowserTree==0){
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
			logDebug("finish!");
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
			logDebug("finish!");
			finishActivity();
		}
		else if (mode == UPLOAD){

			logDebug("mode UPLOAD");

			if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
				if ("text/plain".equals(intent.getType())) {
					logDebug("Handle intent of text plain");

					Bundle extras = intent.getExtras();
					if(extras!=null){
						if (!extras.containsKey(Intent.EXTRA_STREAM)) {
							boolean isURL = false;
							StringBuilder body = new StringBuilder();
							String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
							if (sharedText != null) {
								if (isHttpsUrl(sharedText) || isHttpUrl(sharedText)) {
									isURL = true;
									String header = "[InternetShortcut]\n";
									body.append(header);

									body.append("URL=");
								}
//								body.append(getString(R.string.new_file_content_when_uploading)+": ");
								body.append(sharedText);
							}

							String sharedText2 = intent.getStringExtra(Intent.EXTRA_SUBJECT);
							if (sharedText2 != null) {
								body.append("\nsubject=");
								body.append(sharedText2);
							}

							String sharedText3 = intent.getStringExtra(Intent.EXTRA_EMAIL);
							if (sharedText3 != null) {
								body.append("\nemail=");
//								body.append(getString(R.string.new_file_email_when_uploading)+": ");
								body.append(sharedText3);
							}

							long parentHandle = handle;
							MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
							if(parentNode == null){
								parentNode = megaApi.getRootNode();
							}

							showNewFileDialog(parentNode,body.toString(), isURL);
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
				createAndShowProgressDialog(false, R.string.upload_prepare);
			}
			else{
				onIntentProcessed();
			}
			logDebug("After UPLOAD click - back to Cloud");
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
			logDebug("finish!");
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
		logDebug("handle: " + handle);
		Intent startIntent = new Intent(this, ManagerActivityLollipop.class);
		if(handle!=-1){
			startIntent.setAction(ACTION_OPEN_FOLDER);
			startIntent.putExtra("PARENT_HANDLE", handle);
		}
		startActivity(startIntent);
	}

    public void showSnackbar(String s){
		showSnackbar(fragmentContainer, s);
    }

    private void createFile(String name, String data, MegaNode parentNode, boolean isURL){
		File file;
		if (isURL){
			file = createTemporalURLFile(this, name, data);
		}
		else {
			file = createTemporalTextFile(this, name, data);
		}
		if(file!=null){
			showSnackbar(getString(R.string.upload_began));
			Intent intent = new Intent(this, UploadService.class);
			intent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
			intent.putExtra(UploadService.EXTRA_NAME, file.getName());
			intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
			intent.putExtra(UploadService.EXTRA_SIZE, file.getTotalSpace());
			startService(intent);

			logDebug("After UPLOAD click - back to Cloud");
			this.backToCloud(parentNode.getHandle());
			finishActivity();
		}
		else{
			showSnackbar(getString(R.string.general_text_error));
		}
	}

	private void createFolder(String title) {

		logDebug("createFolder");
		if (!isOnline(this)){
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
				logDebug("cDriveExplorer != null: " + parentHandle);
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
					logDebug("cDriveExplorer != null: " + parentHandle);
				}	
			}
		}
		else if (tabShown == INCOMING_TAB){
			if (iSharesExplorer != null){
				parentHandle = iSharesExplorer.getParentHandle();
				logDebug("iSharesExplorer != null: " + parentHandle);
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
					logDebug("iSharesExplorer != null: " + parentHandle);
				}	
			}
		}
		else if (tabShown == NO_TABS){
			if (importFileF && importFragmentSelected != -1) {
				switch (importFragmentSelected) {
					case CLOUD_FRAGMENT: {
						if (cDriveExplorer != null && cDriveExplorer.isAdded()) {
							parentHandle = cDriveExplorer.getParentHandle();
						}
						break;
					}
					case INCOMING_FRAGMENT: {
						if (iSharesExplorer != null && iSharesExplorer.isAdded()) {
							parentHandle = iSharesExplorer.getParentHandle();
						}
						break;
					}
				}
			}
			else if (cDriveExplorer != null){
				parentHandle = cDriveExplorer.getParentHandle();
				logDebug("cDriveExplorer != null: " + parentHandle);
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
					logDebug("cDriveExplorer != null: " + parentHandle);
				}
			}
		}

		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
		
		if (parentNode != null){
			logDebug("parentNode != null: " + parentNode.getName());
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
				showSnackbar(getString(R.string.context_folder_already_exists));
			}
		}
		else{
			logWarning("parentNode == null: " + parentHandle);
			parentNode = megaApi.getRootNode();
			if (parentNode != null){
				logDebug("megaApi.getRootNode() != null");
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
					showSnackbar(getString(R.string.context_folder_already_exists));
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

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError error) {
		logDebug("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			myChatFilesNode = megaApi.getNodeByPath("/"+CHAT_FOLDER);
			if (myChatFilesNode != null && myChatFilesNode.getHandle() == request.getNodeHandle()) {
				checkIfFilesExistsInMEGA();
			}
			else {
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
							logDebug("The handle of the created folder is: " + parentHandleCloud);
						}
					}
					else if (tabShown == NO_TABS){
						if (importFileF && importFragmentSelected != -1) {
							switch (importFragmentSelected) {
								case CLOUD_FRAGMENT: {
									if (cDriveExplorer != null && cDriveExplorer.isAdded()) {
										cDriveExplorer.navigateToFolder(request.getNodeHandle());
									}
									break;
								}
								case INCOMING_FRAGMENT: {
									if (iSharesExplorer != null && iSharesExplorer.isAdded()) {
										iSharesExplorer.navigateToFolder(request.getNodeHandle());
									}
									break;
								}
							}
						}
						else if (cDriveExplorer != null){
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
						logDebug("The handle of the created folder is: " + parentHandleCloud);
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
				prepareNodesText.setVisibility(View.GONE);*/
				
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

				logDebug("Logged in with session");

				megaApi.fetchNodes(this);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){

			if (error.getErrorCode() == MegaError.API_OK){

				getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

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

						logDebug("Chat enabled-->connect");
						if((megaChatApi.getInitState()!=MegaChatApi.INIT_ERROR)){
							logDebug("Connection goes!!!");
							megaChatApi.connect(this);
						} else{
							logWarning("Not launch connect: " + megaChatApi.getInitState());
						}
						MegaApplication.setLoggingIn(false);
						afterLoginAndFetch();
					} else{
						logWarning("Chat NOT enabled - readyToManager");
						MegaApplication.setLoggingIn(false);
						afterLoginAndFetch();
					}
				} else{
					logWarning("chatSettings NULL - readyToManager");
					MegaApplication.setLoggingIn(false);
					afterLoginAndFetch();

				}
			}	
		}
		else if (request.getType() == MegaRequest.TYPE_COPY) {
			filesChecked++;
			if (error.getErrorCode() == MegaError.API_OK) {
				MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
				if (node != null) {
					attachNodes.add(node);
				}
			} else {
				logWarning("Error copying node into My Chat Files");
			}
			if (filesChecked == filePreparedInfos.size()) {
				startChatUploadService();
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
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		logDebug("onUserAlertsUpdate");
	}


	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> updatedNodes) {
		logDebug("onNodesUpdate");
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

		File childThumbDir = new File(getThumbFolder(this), ImportFilesFragment.THUMB_FOLDER);
		if (childThumbDir != null){
			if (childThumbDir.exists()){
				try {
					deleteFile(childThumbDir);
				} catch (IOException e) {}
			}
		}
		
		super.onDestroy();
	}

	public void deleteFile(File file) throws IOException {
		if (file.isDirectory()) {
			if (file.list().length == 0) {
				file.delete();
			} else {
				String[] files = file.list();
				for (String temp : files) {
					File deleteFile = new File(file, temp);
					deleteFile(deleteFile);
				}
				if (file.list().length == 0) {
					file.delete();
				}
			}
		} else {
			file.delete();
		}
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

	public void setToolbarSubtitle(String s) {
		if(aB != null) {
			aB.setSubtitle(s);
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		logDebug("onOptionsItemSelected");

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
						showSnackbar(getString(R.string.no_contacts_invite));
					}
					else {
						if(contacts.isEmpty()){
							showSnackbar(getString(R.string.no_contacts_invite));
						}
						else{
							Intent in = new Intent(this, AddContactActivityLollipop.class);
							in.putExtra("contactType", CONTACT_TYPE_MEGA);
							startActivityForResult(in, REQUEST_CREATE_CHAT);
						}
					}
				}
				else{
					logWarning("Online but not megaApi");
					showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				}
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		logDebug("Request code: " + requestCode + ", Result code: " + resultCode);

		if (requestCode == REQUEST_CREATE_CHAT && resultCode == RESULT_OK) {
			logDebug("REQUEST_CREATE_CHAT OK");

			if (intent == null) {
				logWarning("Return.....");
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);

			if (contactsData != null){
				if(contactsData.size()==1){
					MegaUser user = megaApi.getContact(contactsData.get(0));
					if(user!=null){
						logDebug("Chat with contact: " + contactsData.size());
						startOneToOneChat(user);
					}
				}
				else{
					logDebug("Create GROUP chat");
					MegaChatPeerList peers = MegaChatPeerList.createInstance();
					for (int i=0; i<contactsData.size(); i++){
						MegaUser user = megaApi.getContact(contactsData.get(i));
						if(user!=null){
							peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
						}
					}
					logDebug("create group chat with participants: " + peers.size());

					final String chatTitle = intent.getStringExtra(AddContactActivityLollipop.EXTRA_CHAT_TITLE);
					final boolean isEKR = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_EKR, false);
					if (isEKR) {
						megaChatApi.createChat(true, peers, chatTitle, this);
					}
					else {
						final boolean chatLink = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_CHAT_LINK, false);

						if(chatLink){
							if(chatTitle!=null && !chatTitle.isEmpty()){
								CreateGroupChatWithPublicLink listener = new CreateGroupChatWithPublicLink(this, chatTitle);
								megaChatApi.createPublicChat(peers, chatTitle, listener);
							}
							else{
								showAlert(this, getString(R.string.message_error_set_title_get_link), null);
							}
						}
						else{
							megaChatApi.createPublicChat(peers, chatTitle, this);
						}
					}
				}
			}
		}
	}

	public void onRequestFinishCreateChat(int errorCode, long chatHandle, boolean publicLink){
		logDebug("onRequestFinishCreateChat");

		if(errorCode==MegaChatError.ERROR_OK){
			logDebug("Chat CREATED.");
			//Update chat view
			chatExplorer = getChatExplorerFragment();
			if(chatExplorer!=null){
				chatExplorer.setChats();
			}
			showSnackbar(getString(R.string.new_group_chat_created));
		}
		else{
			logWarning("ERROR WHEN CREATING CHAT " + errorCode);
			showSnackbar(getString(R.string.create_chat_error));
		}
	}

	public void startOneToOneChat(MegaUser user){
		logDebug("User: " + user.getHandle());
		MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
		MegaChatPeerList peers = MegaChatPeerList.createInstance();
		if(chat==null){
			logDebug("No chat, create it!");
			peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
			megaChatApi.createChat(false, peers, this);
		}
		else{
			logDebug("There is already a chat, open it!");
			showSnackbar(getString(R.string.chat_already_exists));
		}
	}

	public void showNewFolderDialog(){
		logDebug("showNewFolderDialog");
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

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
		params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
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

	void getChatAdded (ArrayList<ChatExplorerListItem> listItems) {
		ArrayList<MegaChatRoom> chats = new ArrayList<>();
		ArrayList<MegaUser> users = new ArrayList<>();

		createAndShowProgressDialog(true, R.string.preparing_chats);

		for (ChatExplorerListItem item : listItems) {
			if (item.getChat() != null) {
				MegaChatRoom chatRoom = megaChatApi.getChatRoom(item.getChat().getChatId());
				if (chatRoom != null) {
					chats.add(chatRoom);
				}
			}
			else if (item.getContact() != null && item.getContact().getMegaUser() != null) {
				users.add(item.getContact().getMegaUser());
			}
		}

		if (!users.isEmpty()) {
			CreateChatToPerformActionListener listener = new CreateChatToPerformActionListener(chats, users, -1, this, CreateChatToPerformActionListener.SEND_FILE_EXPLORER_CONTENT);

			for (MegaUser user : users) {
				MegaChatPeerList peers = MegaChatPeerList.createInstance();
				peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
				megaChatApi.createChat(false, peers, listener);
			}
		}
		else {
			sendToChats(chats);
		}
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		switch(v.getId()) {
			case R.id.fab_file_explorer: {
				chatExplorer = getChatExplorerFragment();
				if(chatExplorer!=null){
					if(chatExplorer.getAddedChats()!=null){
//						sendToChats(chatExplorer.getAddedChats());
						getChatAdded(chatExplorer.getAddedChats());
					}
				}
				break;
			}
			case R.id.new_group_button: {
				if(megaApi!=null && megaApi.getRootNode()!=null){
					ArrayList<MegaUser> contacts = megaApi.getContacts();
					if(contacts==null){
						showSnackbar(getString(R.string.no_contacts_invite));
					}
					else {
						if(contacts.isEmpty()){
							showSnackbar(getString(R.string.no_contacts_invite));
						}
						else{
							Intent intent = new Intent(this, AddContactActivityLollipop.class);
							intent.putExtra("contactType", CONTACT_TYPE_MEGA);
							intent.putExtra("onlyCreateGroup", true);
							startActivityForResult(intent, REQUEST_CREATE_CHAT);
						}
					}
				}
				else{
					logWarning("Online but not megaApi");
					showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				}
				break;
			}
		}
	}

	public void sendToChats(ArrayList<MegaChatRoom> chatListItems){

		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			}
			catch(Exception ex){}
		}

		this.chatListItems = chatListItems;

		if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
			Bundle extras = intent.getExtras();
			if ("text/plain".equals(intent.getType()) && extras != null && !extras.containsKey(Intent.EXTRA_STREAM)) {
				logDebug("Handle intent of text plain");
				StringBuilder body = new StringBuilder();
				String sharedText2 = intent.getStringExtra(Intent.EXTRA_SUBJECT);
				if (sharedText2 != null) {
					body.append(getString(R.string.new_file_subject_when_uploading) + ": ");
					body.append(sharedText2);
				}
				String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
				if (sharedText != null) {
					if(body.length()>0){
						body.append("\n");
					}
					body.append(getString(R.string.new_file_content_when_uploading) + ": ");
					body.append(sharedText);
				}
				String sharedText3 = intent.getStringExtra(Intent.EXTRA_EMAIL);
				if (sharedText3 != null) {
					if(body.length()>0){
						body.append("\n");
					}
					body.append(getString(R.string.new_file_email_when_uploading) + ": ");
					body.append(sharedText3);
				}

				for(int i=0; i < chatListItems.size();i++){
					megaChatApi.sendMessage(chatListItems.get(i).getChatId(), body.toString());
				}

				if(chatListItems.size()==1){
					MegaChatRoom chatItem = chatListItems.get(0);
					long idChat = chatItem.getChatId();
					if(chatItem!=null){
						Intent intent = new Intent(this, ManagerActivityLollipop.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
						intent.putExtra("CHAT_ID", idChat);
						startActivity(intent);
					}
				}
				else{
					Intent chatIntent = new Intent(this, ManagerActivityLollipop.class);
					chatIntent.setAction(ACTION_CHAT_SUMMARY);
					startActivity(chatIntent);
				}
			}
			else{
				if (filePreparedInfos == null){
					FilePrepareTask filePrepareTask = new FilePrepareTask(this);
					filePrepareTask.execute(getIntent());
					createAndShowProgressDialog(false, R.string.upload_prepare);
				}
				else{
                    onIntentChatProcessed(filePreparedInfos);
				}
			}
		}
		else{
			if (filePreparedInfos == null){
				FilePrepareTask filePrepareTask = new FilePrepareTask(this);
				filePrepareTask.execute(getIntent());
				createAndShowProgressDialog(false, R.string.upload_prepare);
			}
			else{
				onIntentChatProcessed(filePreparedInfos);
			}
		}
	}

	public void showNewFileDialog(final MegaNode parentNode, final String data, final boolean isURL){
		logDebug("showNewFileDialog");

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

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
		params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
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
		if (isURL) {
			input.setHint(getString(R.string.context_new_link_name));
		}
		else {
			input.setHint(getString(R.string.context_new_file_name_hint));
		}
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
							createFile(value, data, parentNode, isURL);
							newFolderDialog.dismiss();
						}
					}
					return true;
				}
				return false;
			}
		});

		input.setImeActionLabel(getString(R.string.cam_sync_ok),EditorInfo.IME_ACTION_DONE);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		if (isURL) {
			builder.setTitle(getString(R.string.dialog_title_new_link));
		}
		else {
			builder.setTitle(getString(R.string.context_new_file_name));
		}
		builder.setPositiveButton(getString(R.string.cam_sync_ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						createFile(value, data, parentNode, isURL);
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
						createFile(value, data, parentNode, isURL);
						newFolderDialog.dismiss();
					}
				}
			}
		});
	}

	public void changeName (String name, String rename) {
		String[] params = {name, rename};
		new ChangeNameTask().execute(params);
    }

    public class ChangeNameTask extends AsyncTask<String, Void, Void> {

		HashMap<String, String> temp = new HashMap<>();

		@Override
		protected Void doInBackground(String... strings) {
			String name = strings[0];
			String rename = strings[1];

			if (importFileFragment != null && importFileFragment.isAdded()) {
				HashMap<String, String> names = importFileFragment.getNameFiles();
				Iterator it = names.entrySet().iterator();

				while (it.hasNext()) {
					Map.Entry entry = (Map.Entry) it.next();
					if (entry.getKey().equals(name)) {
						temp.put((String)entry.getKey(), rename);
					}
					else {
						temp.put((String)entry.getKey(), (String)entry.getValue());
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			nameFiles = temp;
			importFileFragment.setNameFiles(temp);
			if (importFileFragment.adapter != null) {
				importFileFragment.adapter.setImportNameFiles(temp);
			}
		}
	}

    public void showRenameDialog(final File document, final String text){
		logDebug("showRenameDialog");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);
//	    layout.setLayoutParams(params);

        final EditTextCursorWatcher input = new EditTextCursorWatcher(this, document.isDirectory());
//		input.setId(EDIT_TEXT_ID);
        input.setSingleLine();
        input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
//		input.setHint(getString(R.string.context_new_folder_name));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        input.setImeActionLabel(getString(R.string.context_rename),EditorInfo.IME_ACTION_DONE);
        input.setText(text);
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, boolean hasFocus) {
                if (hasFocus) {
                    if (document.isDirectory()){
                        input.setSelection(0, input.getText().length());
                    }
                    else{
                        String [] s = text.split("\\.");
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

        layout.addView(input, params);

        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

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
        params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
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
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(FileExplorerActivityLollipop.this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

        input.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    String value = v.getText().toString().trim();
                    if (value.length() == 0) {
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(FileExplorerActivityLollipop.this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                        textError.setText(getString(R.string.invalid_string));
                        error_layout.setVisibility(View.VISIBLE);
                        input.requestFocus();

                    }else{
                        boolean result=matches(regex, value);
                        if(result){
                            input.getBackground().mutate().setColorFilter(ContextCompat.getColor(FileExplorerActivityLollipop.this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                            textError.setText(getString(R.string.invalid_characters));
                            error_layout.setVisibility(View.VISIBLE);
                            input.requestFocus();

                        }
                        else{
                            changeName(document.getName(), value);
//                            nC.renameNode(document, value);
                            renameDialog.dismiss();
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.context_rename) + " "	+ new String(document.getName()));
        builder.setPositiveButton(getString(R.string.context_rename),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                input.getBackground().clearColorFilter();
            }
        });
        builder.setView(layout);
        renameDialog = builder.create();
        renameDialog.show();
        renameDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = input.getText().toString().trim();

                if (value.length() == 0) {
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(FileExplorerActivityLollipop.this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                    textError.setText(getString(R.string.invalid_string));
                    error_layout.setVisibility(View.VISIBLE);
                    input.requestFocus();
                }
                else{
                    boolean result=matches(regex, value);
                    if(result){
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(FileExplorerActivityLollipop.this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                        textError.setText(getString(R.string.invalid_characters));
                        error_layout.setVisibility(View.VISIBLE);
                        input.requestFocus();

                    }
                    else{
                        changeName(document.getName(), value);
//                        nC.renameNode(document, value);
                        renameDialog.dismiss();
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

	@Override
	public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

	}

	@Override
	public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

	}

	@Override
	public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {

	}

	@Override
	public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {

	}

	@Override
	public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

	}

	@Override
	public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
		int state = megaChatApi.getUserOnlineStatus(userhandle);
		if(state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID) {
			String formattedDate = lastGreenDate(this, lastGreen);
			if (userhandle != megaChatApi.getMyUserHandle()) {
				chatExplorer = getChatExplorerFragment();
				if (chatExplorer != null) {
					chatExplorer.updateLastGreenContact(userhandle, formattedDate);
				}
			}
		}
	}

	ChatExplorerFragment getChatExplorerFragment () {

		if (!isChatEnabled()) {
			return null;
		}

		String chatTag1;
		if (importFileF) {
			chatTag1  ="chatExplorer";
		}
		else {
			if(isChatFirst){
				chatTag1 = getFragmentTag(R.id.explorer_tabs_pager, 0);
			}
			else{
				chatTag1 = getFragmentTag(R.id.explorer_tabs_pager, 2);
			}
		}
		return (ChatExplorerFragment) getSupportFragmentManager().findFragmentByTag(chatTag1);
	}

	public void collapseSearchView () {
		if (searchMenuItem != null) {
			searchMenuItem.collapseActionView();
		}
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

	public List<ShareInfo> getFilePreparedInfos() {
		return filePreparedInfos;
	}

	public void setNameFiles (HashMap<String, String> nameFiles) {
		this.nameFiles = nameFiles;
	}

	public HashMap<String, String> getNameFiles () {
		return nameFiles;
	}
}
