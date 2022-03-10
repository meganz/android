package mega.privacy.android.app.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.jeremyliao.liveeventbus.LiveEventBus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.TransfersManagementActivity;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.domain.entity.NameCollision;
import mega.privacy.android.app.namecollision.NameCollisionActivity;
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase;
import mega.privacy.android.app.usecase.exception.MegaNodeException;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.generalusecase.FilePrepareUseCase;
import mega.privacy.android.app.interfaces.ActionNodeCallback;
import mega.privacy.android.app.components.CustomViewPager;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.listeners.CreateFolderListener;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.main.adapters.FileExplorerPagerAdapter;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink;
import mega.privacy.android.app.main.megachat.ChatExplorerFragment;
import mega.privacy.android.app.main.megachat.ChatExplorerListItem;
import mega.privacy.android.app.main.megachat.ChatUploadService;
import mega.privacy.android.app.main.megachat.PendingMessageSingle;
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.Util;
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
import static mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_VIEW_MODE;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.ChatUtil.createAttachmentPendingMessage;
import static mega.privacy.android.app.utils.ColorUtils.tintIcon;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showNewFileDialog;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showNewFolderDialog;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showNewURLFileDialog;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

import javax.inject.Inject;

@AndroidEntryPoint
public class FileExplorerActivity extends TransfersManagementActivity
		implements MegaRequestListenerInterface, MegaGlobalListenerInterface,
		MegaChatRequestListenerInterface, View.OnClickListener, MegaChatListenerInterface,
		ActionNodeCallback, SnackbarShower {

	private final static String SHOULD_RESTART_SEARCH = "SHOULD_RESTART_SEARCH";
	private final static String QUERY_AFTER_SEARCH = "QUERY_AFTER_SEARCH";
	private final static String CURRENT_ACTION = "CURRENT_ACTION";

	public final static int CLOUD_FRAGMENT = 0;
	public final static int INCOMING_FRAGMENT = 1;
	public final static int CHAT_FRAGMENT = 3;
	public final static int IMPORT_FRAGMENT = 4;
    public static final String EXTRA_SHARE_INFOS = "share_infos";
    public static final String EXTRA_SHARE_ACTION = "share_action";
    public static final String EXTRA_SHARE_TYPE = "share_type";
    public static final String EXTRA_PARENT_HANDLE = "parent_handle";
    public static final String EXTRA_SELECTED_FOLDER = "selected_folder";

	public static String ACTION_PROCESSED = "CreateLink.ACTION_PROCESSED";
	
	public static String ACTION_PICK_MOVE_FOLDER = "ACTION_PICK_MOVE_FOLDER";
	public static String ACTION_PICK_COPY_FOLDER = "ACTION_PICK_COPY_FOLDER";
	public static String ACTION_PICK_IMPORT_FOLDER = "ACTION_PICK_IMPORT_FOLDER";
	public static String ACTION_SELECT_FOLDER_TO_SHARE = "ACTION_SELECT_FOLDER_TO_SHARE";
	public static String ACTION_CHOOSE_MEGA_FOLDER_SYNC = "ACTION_CHOOSE_MEGA_FOLDER_SYNC";
	public static String ACTION_MULTISELECT_FILE = "ACTION_MULTISELECT_FILE";
	public static String ACTION_UPLOAD_TO_CLOUD = "ACTION_UPLOAD_TO_CLOUD";
	public static String ACTION_UPLOAD_TO_CHAT = "ACTION_UPLOAD_TO_CHAT";
	public static String ACTION_SAVE_TO_CLOUD = "ACTION_SAVE_TO_CLOUD";

	public static final int UPLOAD = 0;
	public static final int MOVE = 1;
	public static final int COPY = 2;
	public static final int CAMERA = 3;
	public static final int IMPORT = 4;
	public static final int SELECT = 5;
	public static final int SELECT_CAMERA_FOLDER = 7;
	public static final int SHARE_LINK = 8;
	public static final int SAVE = 9;

	private static final int NO_TABS = -1;
	private static final int CLOUD_TAB = 0;
	private static final int INCOMING_TAB = 1;
	private static final int CHAT_TAB = 2;
	private static final int SHOW_TABS = 3;
	private boolean isChatFirst;
	private static final int DEFAULT_TAB_TO_REMOVE = -1;

	@Inject
	FilePrepareUseCase filePrepareUseCase;
	@Inject
	CheckNameCollisionUseCase checkNameCollisionUseCase;

	private DatabaseHandler dbH;
	private MegaPreferences prefs;
	private AppBarLayout abL;
	private MaterialToolbar tB;
	private ActionBar aB;
	private DisplayMetrics outMetrics;
	private RelativeLayout fragmentContainer;
	private LinearLayout loginLoggingIn;
	private ProgressBar loginProgressBar;
	private ProgressBar loginFetchNodesProgressBar;
	private TextView generatingKeysText;
	private TextView queryingSignupLinkText;
	private TextView confirmingAccountText;
	private TextView loggingInText;
	private TextView fetchingNodesText;
	private TextView prepareNodesText;

	private FloatingActionButton fabButton;

	private MegaNode parentMoveCopy;

	private MenuItem createFolderMenuItem;
	private MenuItem newChatMenuItem;
	private MenuItem searchMenuItem;
	private boolean isList = true;

	private FrameLayout cloudDriveFrameLayout;
	private long fragmentHandle  = -1;

	private String gSession;
	private UserCredentials credentials;
	private String lastEmail;
	
	private MegaApiAndroid megaApi;
	private MegaChatApiAndroid megaChatApi;

	private int mode;
	private boolean multiselect;
	private boolean selectFile;
	
	private long[] moveFromHandles;
	private long[] copyFromHandles;
	private long[] importChatHandles;
	private ArrayList<String> selectedContacts;
	private boolean folderSelected;
	
	private Handler handler;

	private int tabShown = CLOUD_TAB;

	private ArrayList<MegaChatRoom> chatListItems = new ArrayList<>();

	private CloudDriveExplorerFragment cDriveExplorer;
	private IncomingSharesExplorerFragment iSharesExplorer;
	private ChatExplorerFragment chatExplorer;
	private ImportFilesFragment importFileFragment;

	private AlertDialog statusDialog;

	private List<ShareInfo> filePreparedInfos;

	//Tabs in Cloud
	private TabLayout tabLayoutExplorer;
	private FileExplorerPagerAdapter mTabsAdapterExplorer;
	private CustomViewPager viewPagerExplorer;

	private ArrayList<MegaNode> nodes;

	private long parentHandleIncoming;
	private long parentHandleCloud;
	private int deepBrowserTree;

	private Intent intent;
	private boolean importFileF;
	private int importFragmentSelected = -1;
	private String action;
	private HashMap<String, String> nameFiles = new HashMap<>();

	private MegaNode myChatFilesNode;
	private ArrayList<MegaNode> attachNodes = new ArrayList<>();
	private ArrayList<ShareInfo> uploadInfos = new ArrayList<>();
	private int filesChecked;

	private SearchView searchView;

    private boolean needLogin;

	private FileExplorerActivity fileExplorerActivity;

	private String querySearch = "";
	private boolean isSearchExpanded;
	private boolean collapsedByClick;
	private boolean pendingToOpenSearchView;
	private int pendingToAttach;
	private int totalAttached;
	private int totalErrors;

	private boolean shouldRestartSearch;
	private String queryAfterSearch;
	private String currentAction;

	private BottomSheetDialogFragment bottomSheetDialogFragment;

	private FileExplorerActivityViewModel mViewModel;

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		logDebug("onRequestFinish(CHAT)");

        if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
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
					Intent intent = new Intent(this, ManagerActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.setAction(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE);
					if (chatListItems.size() == 1) {
						intent.putExtra(CHAT_ID, chatListItems.get(0).getChatId());
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

	@Override
	public void showSnackbar(int type, @Nullable String content, long chatId) {
		showSnackbar(type, fragmentContainer, content, chatId);
	}

	private void onProcessAsyncInfo(List<ShareInfo> info) {
		if (info == null || info.isEmpty()) {
			logWarning("Selected items list is null or empty.");
			finishFileExplorer();
			return;
		}

		filePreparedInfos = info;
		if(needLogin) {
			Intent loginIntent = new Intent(FileExplorerActivity.this, LoginActivity.class);
			loginIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
			loginIntent.putExtra(EXTRA_SHARE_ACTION, getIntent().getAction());
			loginIntent.putExtra(EXTRA_SHARE_TYPE, getIntent().getType());
			loginIntent.putExtra(EXTRA_SHARE_INFOS,new ArrayList<>(info));
			// close previous login page
			loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			loginIntent.setAction(ACTION_FILE_EXPLORER_UPLOAD);
			needLogin = false;
			startActivity(loginIntent);
			finish();
			return;
		}
		if (action != null && getIntent() != null) {
			getIntent().setAction(action);
		}
		if (importFileF) {
			if (importFragmentSelected != -1) {
				chooseFragment(importFragmentSelected);
			} else if (ACTION_UPLOAD_TO_CHAT.equals(action)) {
				chooseFragment(CHAT_FRAGMENT);
			} else {
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	        // do nothing
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		logDebug("onCreate first");
		super.onCreate(savedInstanceState);

		mViewModel = new ViewModelProvider(this).get(FileExplorerActivityViewModel.class);
		mViewModel.info.observe(this, this::onProcessAsyncInfo);

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
			shouldRestartSearch = savedInstanceState.getBoolean(SHOULD_RESTART_SEARCH, false);
			queryAfterSearch = savedInstanceState.getString(QUERY_AFTER_SEARCH, null);
			currentAction = savedInstanceState.getString(CURRENT_ACTION, null);

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

		fileExplorerActivity = this;
				
		dbH = DatabaseHandler.getDbHandler(this);
		prefs = dbH.getPreferences();
		if (prefs == null || prefs.getPreferredViewList() == null) {
			isList = true;
		}
		else {
			isList = Boolean.parseBoolean(prefs.getPreferredViewList());
		}
		credentials = dbH.getCredentials();
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
		
		if (credentials == null){
			logWarning("User credentials NULL");

            if (isChatFirst()) {
				startActivity(new Intent(this, LoginActivity.class)
						.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
						.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(Intent.EXTRA_TEXT))
						.putExtra(Intent.EXTRA_SUBJECT, getIntent().getStringExtra(Intent.EXTRA_SUBJECT))
						.putExtra(Intent.EXTRA_EMAIL, getIntent().getStringExtra(Intent.EXTRA_EMAIL))
						.setAction(ACTION_FILE_EXPLORER_UPLOAD)
						.setType(TYPE_TEXT_PLAIN)
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

				finish();
			} else {
				needLogin = true;

				mViewModel.ownFilePrepareTask(this,getIntent());
				createAndShowProgressDialog(false, getQuantityString(R.plurals.upload_prepare, 1));
			}

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

		if (megaChatApi == null) {
			megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
		}
		
		setContentView(R.layout.activity_file_explorer);
		
		fragmentContainer = findViewById(R.id.fragment_container_file_explorer);

		abL = findViewById(R.id.app_bar_layout_explorer);
		//Set toolbar
		tB = findViewById(R.id.toolbar_explorer);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		if(aB!=null){
			aB.hide();
		}
		else{
			logWarning("aB is null");
		}

		fabButton = findViewById(R.id.fab_file_explorer);
		fabButton.setOnClickListener(this);
		showFabButton(false);
		//TABS
		tabLayoutExplorer =  findViewById(R.id.sliding_tabs_file_explorer);
		viewPagerExplorer = findViewById(R.id.explorer_tabs_pager);
		viewPagerExplorer.setOffscreenPageLimit(3);

		//Layout for login if needed
		loginLoggingIn = findViewById(R.id.file_logging_in_layout);
		loginProgressBar = findViewById(R.id.file_login_progress_bar);
		loginFetchNodesProgressBar = findViewById(R.id.file_login_fetching_nodes_bar);
		generatingKeysText = findViewById(R.id.file_login_generating_keys_text);
		queryingSignupLinkText = findViewById(R.id.file_login_query_signup_link_text);
		confirmingAccountText =findViewById(R.id.file_login_confirm_account_text);
		loggingInText = findViewById(R.id.file_login_logging_in_text);
		fetchingNodesText = findViewById(R.id.file_login_fetch_nodes_text);
		prepareNodesText = findViewById(R.id.file_login_prepare_nodes_text);

		intent = getIntent();
		if (megaApi.getRootNode() == null){
			logDebug("hide action bar");
			if (!MegaApplication.isLoggingIn()) {

				MegaApplication.setLoggingIn(true);

				getSupportActionBar().hide();
				tabLayoutExplorer.setVisibility(View.GONE);
				viewPagerExplorer.setVisibility(View.GONE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				loginLoggingIn.setVisibility(View.VISIBLE);
				loginProgressBar.setVisibility(View.VISIBLE);
				loginFetchNodesProgressBar.setVisibility(View.GONE);
				loggingInText.setVisibility(View.VISIBLE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				gSession = credentials.getSession();

				ChatUtil.initMegaChatApi(gSession, this);

				megaApi.fastLogin(gSession, this);
			}
			else{
				logWarning("Another login is proccessing");
			}
		}
		else{
			afterLoginAndFetch();
		}

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

		LiveEventBus.get(EVENT_UPDATE_VIEW_MODE, Boolean.class).observe(this, this::refreshViewNodes);
	}
	
	private void afterLoginAndFetch(){
		handler = new Handler();
		logDebug("SHOW action bar");
		if(aB==null){
			aB=getSupportActionBar();
		}
		aB.show();
		logDebug("aB.setHomeAsUpIndicator");
		aB.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_arrow_back_white));
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);

		if ((intent != null) && (intent.getAction() != null)){
			selectedContacts = intent.getStringArrayListExtra(SELECTED_CONTACTS);
			logDebug("intent OK: " + intent.getAction());
			currentAction = intent.getAction();
			if (intent.getAction().equals(ACTION_SELECT_FOLDER_TO_SHARE)){
				logDebug("action = ACTION_SELECT_FOLDER_TO_SHARE");
				//Just show Cloud Drive, no INCOMING tab , no need of tabhost
				mode = SELECT;

				aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());
				setView(CLOUD_TAB, false, -1);
				tabShown = NO_TABS;

			} else if (intent.getAction().equals(ACTION_MULTISELECT_FILE)) {
				logDebug("action = ACTION_MULTISELECT_FILE");
				//Just show Cloud Drive, no INCOMING tab , no need of tabhost
				mode = SELECT;
				selectFile = true;
				multiselect = true;

				aB.setTitle(getResources().getQuantityString(R.plurals.plural_select_file, 10).toUpperCase());
				setView(SHOW_TABS, false, CHAT_TAB);
			}
			else if (intent.getAction().equals(ACTION_PICK_MOVE_FOLDER)){
				logDebug("ACTION_PICK_MOVE_FOLDER");
				mode = MOVE;
				moveFromHandles = intent.getLongArrayExtra("MOVE_FROM");

				if (moveFromHandles != null && moveFromHandles.length > 0) {
					MegaNode moveNode = megaApi.getNodeByHandle(moveFromHandles[0]);
					parentMoveCopy = megaApi.getParentNode(moveNode);
				}

				aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());
				setView(SHOW_TABS, false, CHAT_TAB);
			}
			else if (intent.getAction().equals(ACTION_PICK_COPY_FOLDER)){
				logDebug("ACTION_PICK_COPY_FOLDER");
				mode = COPY;
				copyFromHandles = intent.getLongArrayExtra("COPY_FROM");

				if (copyFromHandles != null) {
					MegaNode copyNode = megaApi.getNodeByHandle(copyFromHandles[0]);
					parentMoveCopy = megaApi.getParentNode(copyNode);
				}

				aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());
				setView(SHOW_TABS, false, CHAT_TAB);
			}
			else if (intent.getAction().equals(ACTION_CHOOSE_MEGA_FOLDER_SYNC)){
				logDebug("action = ACTION_CHOOSE_MEGA_FOLDER_SYNC");
				mode = SELECT_CAMERA_FOLDER;

				aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());
				setView(SHOW_TABS, false, CHAT_TAB);
			}
			else if (intent.getAction().equals(ACTION_PICK_IMPORT_FOLDER)){
				mode = IMPORT;

				importChatHandles = intent.getLongArrayExtra("HANDLES_IMPORT_CHAT");

				aB.setTitle(getString(R.string.title_share_folder_explorer).toUpperCase());
				setView(SHOW_TABS, false, CHAT_TAB);
			}
			else if ((intent.getAction().equals(ACTION_UPLOAD_TO_CLOUD))){
				logDebug("action = UPLOAD to Cloud Drive");
				mode = UPLOAD;
				selectFile = false;

				aB.setTitle(getString(R.string.title_cloud_explorer).toUpperCase());
				setView(CLOUD_TAB, false, -1);
				tabShown=NO_TABS;
			}
			else if ((intent.getAction().equals(ACTION_SAVE_TO_CLOUD))){
				logDebug("action = SAVE to Cloud Drive");
				mode = SAVE;
				selectFile = false;
				parentHandleCloud = intent.getLongExtra(EXTRA_PARENT_HANDLE, INVALID_HANDLE);

				aB.setTitle(StringResourcesUtils.getString(R.string.section_cloud_drive));
				aB.setSubtitle(StringResourcesUtils.getString(R.string.cloud_drive_select_destination));
				setView(CLOUD_TAB, false, -1);
				tabShown=NO_TABS;
			}
			else{
				logDebug("action = UPLOAD");
				mode = UPLOAD;
				isChatFirst = isChatFirst();

				if(isChatFirst){
					aB.setTitle(getString(R.string.title_file_explorer_send_link).toUpperCase());
					setView(SHOW_TABS, true, INCOMING_TAB);
				}
				else{
					aB.setTitle(getString(R.string.title_upload_explorer).toUpperCase());
					importFileF = true;
					action = intent.getAction();

					cloudDriveFrameLayout = (FrameLayout) findViewById(R.id.cloudDriveFrameLayout);

					mViewModel.ownFilePrepareTask(this,getIntent());
					createAndShowProgressDialog(false, getQuantityString(R.plurals.upload_prepare, 1));

					cloudDriveFrameLayout.setVisibility(View.VISIBLE);

					tabLayoutExplorer.setVisibility(View.GONE);
					viewPagerExplorer.setVisibility(View.GONE);
					tabShown=NO_TABS;
				}
			}
		}
		else{
			logError("intent error");
		}
	}

	/**
	 * Checks if should show first the chat tab.
	 * If the action of the intent is ACTION_SEND and the type of the intent is TYPE_TEXT_PLAIN,
	 * the chat tab should be shown first.
	 *
	 * @return True if should show first the chat tab, false otherwise.
	 */
	private boolean isChatFirst() {
		if (Intent.ACTION_SEND.equals(getIntent().getAction())
				&& TYPE_TEXT_PLAIN.equals(getIntent().getType())) {
			Bundle extras = getIntent().getExtras();
			return extras != null && !extras.containsKey(Intent.EXTRA_STREAM);
		}

		return false;
	}

	private void updateAdapterExplorer(boolean isChatFirst, int tabToRemove) {
		tabLayoutExplorer.setVisibility(View.VISIBLE);
		viewPagerExplorer.setVisibility(View.VISIBLE);

		int position = mTabsAdapterExplorer != null ? viewPagerExplorer.getCurrentItem() : 0;
		if (isChatFirst) {
			mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(), this, true);
		} else {
			mTabsAdapterExplorer = new FileExplorerPagerAdapter(getSupportFragmentManager(), this);
		}
		viewPagerExplorer.setAdapter(mTabsAdapterExplorer);
		viewPagerExplorer.setCurrentItem(position);
		tabLayoutExplorer.setupWithViewPager(viewPagerExplorer);

		if (mTabsAdapterExplorer != null && mTabsAdapterExplorer.getCount() > 2
				&& ((!isChatFirst && tabToRemove == CHAT_TAB) || (isChatFirst && tabToRemove == INCOMING_TAB))) {
			mTabsAdapterExplorer.setTabRemoved(true);
			tabLayoutExplorer.removeTabAt(2);
			mTabsAdapterExplorer.notifyDataSetChanged();
		}
	}

	private void setView(int tab, boolean isChatFirst, int tabToRemove) {
		logDebug("setView "+tab);
		switch (tab) {
			case CLOUD_TAB:{
				cloudDriveFrameLayout = (FrameLayout) findViewById(R.id.cloudDriveFrameLayout);
				if(cDriveExplorer==null){
					cDriveExplorer = new CloudDriveExplorerFragment();
				}

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.cloudDriveFrameLayout, cDriveExplorer, "cDriveExplorer");
				ft.commitNowAllowingStateLoss();

				cloudDriveFrameLayout.setVisibility(View.VISIBLE);
				tabLayoutExplorer.setVisibility(View.GONE);
				viewPagerExplorer.setVisibility(View.GONE);
				break;
			}
			case SHOW_TABS:{
				if (mTabsAdapterExplorer == null) {
					updateAdapterExplorer(isChatFirst, tabToRemove);
				}

				viewPagerExplorer.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
					public void onPageScrollStateChanged(int state) {}
					public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

					public void onPageSelected(int position) {
						logDebug("Position:"+ position);
						supportInvalidateOptionsMenu();
						changeTitle();

						checkFragmentScroll(position);

						if (!multiselect) {
							return;
						}

						if (isSearchExpanded && !pendingToOpenSearchView) {
							clearQuerySearch();
							collapseSearchView();
						}

						if (position == 0) {
							if (iSharesExplorer != null ) {
								iSharesExplorer.hideMultipleSelect();
							}
						}
						else if (position == 1) {
							if (cDriveExplorer != null) {
								cDriveExplorer.hideMultipleSelect();
							}
						}
					}
				});
			}
		}
	}

	private void checkFragmentScroll(int position) {
		CheckScrollInterface fragment = (CheckScrollInterface)mTabsAdapterExplorer.getItem(position);
		fragment.checkScroll();
	}

	public void chooseFragment (int fragment) {
		importFragmentSelected = fragment;
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if (fragment == CLOUD_FRAGMENT) {
			if(cDriveExplorer==null){
				cDriveExplorer = new CloudDriveExplorerFragment();
			}
			ft.replace(R.id.cloudDriveFrameLayout, cDriveExplorer, "cDriveExplorer");
		}
		else if (fragment == INCOMING_FRAGMENT) {
			if(iSharesExplorer==null){
				iSharesExplorer = new IncomingSharesExplorerFragment();
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
		fabButton.setVisibility(show ? View.VISIBLE : View.GONE);
	}

    public void changeActionBarElevation(boolean elevate, int fragmentIndex) {
        if (!isCurrentFragment(fragmentIndex)) return;

		ColorUtils.changeStatusBarColorForElevation(this, elevate);

        float elevation = getResources().getDimension(R.dimen.toolbar_elevation);

        if (fragmentIndex == CHAT_FRAGMENT) {
            if (Util.isDarkMode(this)) {
                if (tabShown == NO_TABS) {
                    if (elevate) {
                        int toolbarElevationColor = ColorUtils.getColorForElevation(this, elevation);
                        tB.setBackgroundColor(toolbarElevationColor);
                    } else {
                        tB.setBackgroundColor(android.R.color.transparent);
                    }
                } else {
                    if (elevate){
                        tB.setBackgroundColor(android.R.color.transparent);
                        abL.setElevation(elevation);
                    } else {
                        tB.setBackgroundColor(android.R.color.transparent);
                        abL.setElevation(0);
                    }
                }
            }
        } else {
            abL.setElevation(elevate ? elevation : 0);
        }
    }

	private boolean isCurrentFragment(int index) {
		if (tabShown == NO_TABS) return true;  // only one fragment

		// No need to care ImportFilesFragment as it would never be shown in SHOW_TABS mode
		switch(index) {
			case CLOUD_FRAGMENT:
				return tabShown == CLOUD_TAB;
			case CHAT_FRAGMENT:
				return tabShown == CHAT_TAB;
			case INCOMING_FRAGMENT:
				return tabShown == INCOMING_TAB;
			default:
				return false;
		}
	}

	private boolean isSearchMultiselect() {
		if (multiselect) {
			cDriveExplorer = getCloudExplorerFragment();
			iSharesExplorer = getIncomingExplorerFragment();
			return isCloudVisible() || isIncomingVisible();
		}
		return false;
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		logDebug("onCreateOptionsMenu");
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.file_explorer_action, menu);

	    searchMenuItem = menu.findItem(R.id.cab_menu_search);
	    createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder);
	    newChatMenuItem = menu.findItem(R.id.cab_menu_new_chat);

	    searchMenuItem.setVisible(false);
		createFolderMenuItem.setVisible(false);
		newChatMenuItem.setVisible(false);

		searchView = (SearchView) searchMenuItem.getActionView();

		SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
		searchAutoComplete.setHint(getString(R.string.hint_action_search));
		View v = searchView.findViewById(androidx.appcompat.R.id.search_plate);
		v.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

		if (searchView != null){
			searchView.setIconifiedByDefault(true);
		}

		searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				isSearchExpanded = true;

				if (isSearchMultiselect()) {
					hideTabs(true, isCloudVisible() ? CLOUD_FRAGMENT : INCOMING_FRAGMENT);
				} else {
					hideTabs(true, CHAT_FRAGMENT);
					chatExplorer = getChatExplorerFragment();

					if (chatExplorer != null && chatExplorer.isVisible()) {
						chatExplorer.enableSearch(true);
					}
				}

				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				isSearchExpanded = false;

				if (isSearchMultiselect()) {
					if (isCloudVisible()) {
						hideTabs(false, CLOUD_FRAGMENT);
						cDriveExplorer.closeSearch(collapsedByClick);
					} else if (isIncomingVisible()) {
						hideTabs(false, INCOMING_FRAGMENT);
						iSharesExplorer.closeSearch(collapsedByClick);
					}

					supportInvalidateOptionsMenu();
				} else {
					hideTabs(false, CHAT_FRAGMENT);
					chatExplorer = getChatExplorerFragment();

					if (chatExplorer != null && chatExplorer.isVisible()) {
						chatExplorer.enableSearch(false);
					}
				}

				return true;
			}
		});

		searchView.setMaxWidth(Integer.MAX_VALUE);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				logDebug("Query: " + query);
				hideKeyboard(fileExplorerActivity, 0);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (!collapsedByClick) {
					querySearch = newText;
				} else {
					collapsedByClick = false;
				}
				if (isSearchMultiselect()) {
					if (isCloudVisible()) {
						cDriveExplorer.search(newText);
					}
					else if (isIncomingVisible()) {
						iSharesExplorer.search(newText);
					}
				}
				else {
					chatExplorer = getChatExplorerFragment();
					if (chatExplorer != null && chatExplorer.isVisible()) {
						chatExplorer.search(newText);
					}
				}
				return true;
			}
		});

		if (isSearchMultiselect()) {
			isPendingToOpenSearchView();
		}
	    
	    return super.onCreateOptionsMenu(menu);
	}

	public void isPendingToOpenSearchView () {
		if (pendingToOpenSearchView && searchMenuItem != null) {
			openSearchView(querySearch);
			pendingToOpenSearchView = false;
		}
	}

	private void openSearchView(String search) {
	    if (searchMenuItem == null) return;

        searchMenuItem.expandActionView();
        searchView.setQuery(search, false);
    }

	private void setCreateFolderVisibility() {
		if (intent == null) {
			return;
		}

		createFolderMenuItem.setVisible(!intent.getAction().equals(ACTION_MULTISELECT_FILE));
	}

	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		logDebug("onPrepareOptionsMenu");

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
					setCreateFolderVisibility();
					newChatMenuItem.setVisible(false);
					if (multiselect) {
						cDriveExplorer = getCloudExplorerFragment();
						searchMenuItem.setVisible(cDriveExplorer != null && !cDriveExplorer.isFolderEmpty());
					}
				}

			}
			else if(index==1){
				if(isChatFirst){
					//CLOUD TAB
					setCreateFolderVisibility();
					newChatMenuItem.setVisible(false);
				}
				else{
					iSharesExplorer = getIncomingExplorerFragment();
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
									setCreateFolderVisibility();
									break;

								case MegaShare.ACCESS_READ:
									createFolderMenuItem.setVisible(false);
									break;
							}
						}
					}
					newChatMenuItem.setVisible(false);
					if (multiselect) {
						searchMenuItem.setVisible(iSharesExplorer != null && !iSharesExplorer.isFolderEmpty());
					}
				}
			}
			else if(index==2){
				if(isChatFirst){
					//INCOMING TAB
					iSharesExplorer = getIncomingExplorerFragment();
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
				setCreateFolderVisibility();
			}
			else if (importFileF) {
				if (importFragmentSelected != -1 ) {
					switch (importFragmentSelected) {
						case CLOUD_FRAGMENT: {
							createFolderMenuItem.setVisible(true);
							break;
						}
						case INCOMING_FRAGMENT:{
							iSharesExplorer = getIncomingExplorerFragment();
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

	private void setRootTitle(){
		logDebug("setRootTitle");

		if(mode == SELECT){
			if(selectFile){
				if(multiselect){
					aB.setTitle(getResources().getQuantityString(R.plurals.plural_select_file, 10).toUpperCase());
				}
				else{
					aB.setTitle(getResources().getQuantityString(R.plurals.plural_select_file, 1).toUpperCase());
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
			aB.setTitle(getString(R.string.title_file_explorer_send_link).toUpperCase());
		}
		else if(mode == SAVE){
			aB.setTitle(StringResourcesUtils.getString(R.string.section_cloud_drive).toUpperCase());
		}
		else if (mode == UPLOAD && importFileF) {
			if (importFragmentSelected == -1) {
				return;
			}
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

	public void changeTitle (){
		logDebug("changeTitle");

		cDriveExplorer = getCloudExplorerFragment();
		iSharesExplorer = getIncomingExplorerFragment();

		if (mode == SAVE) {
			aB.setSubtitle(StringResourcesUtils.getString(R.string.cloud_drive_select_destination));
		} else {
			aB.setSubtitle(null);
		}

		if (tabShown == NO_TABS || mTabsAdapterExplorer == null) {
			if (importFileF) {
				if (importFragmentSelected != -1) {
					switch (importFragmentSelected) {
						case CLOUD_FRAGMENT: {
							if(cDriveExplorer!=null){
								if (cDriveExplorer.getParentHandle() == INVALID_HANDLE
										|| cDriveExplorer.getParentHandle() == megaApi.getRootNode().getHandle()) {
									setRootTitle();
									aB.setSubtitle(R.string.general_select_to_download);
								} else {
									aB.setTitle(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()).getName());
								}
							}
							break;
						}
						case CHAT_FRAGMENT:
						case IMPORT_FRAGMENT:
							setRootTitle();
							break;
					}
				}
			}
			else {
				if(cDriveExplorer!=null){
					if(cDriveExplorer.getParentHandle()==-1|| cDriveExplorer.getParentHandle()==megaApi.getRootNode().getHandle()){
						setRootTitle();
					}
					else{
						aB.setTitle(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()).getName());
					}
				}
				showFabButton(false);
			}
		}
		else{
			int position = viewPagerExplorer.getCurrentItem();
			Fragment f = (Fragment) mTabsAdapterExplorer.instantiateItem(viewPagerExplorer, position);
			if (f == null) {
				return;
			}
			if(position == 0){
				if(f instanceof ChatExplorerFragment){
					if(tabShown!=NO_TABS){
						tabShown=CHAT_TAB;
					}

					aB.setTitle(getString(R.string.title_file_explorer_send_link).toUpperCase());
				}
				else if(f instanceof CloudDriveExplorerFragment){
					if(tabShown!=NO_TABS){
						tabShown=CLOUD_TAB;
					}

					if(((CloudDriveExplorerFragment)f).getParentHandle()==-1|| ((CloudDriveExplorerFragment)f).getParentHandle()==megaApi.getRootNode().getHandle()){
						setRootTitle();
					}
					else{
						aB.setTitle(megaApi.getNodeByHandle(((CloudDriveExplorerFragment)f).getParentHandle()).getName());
					}

					showFabButton(false);
				}
			}
			else if(position == 1){
				if(f instanceof IncomingSharesExplorerFragment){
					if(tabShown!=NO_TABS){
						tabShown=INCOMING_TAB;
					}

					if(deepBrowserTree==0){
						setRootTitle();
					}
					else{
						aB.setTitle(megaApi.getNodeByHandle(((IncomingSharesExplorerFragment)f).getParentHandle()).getName());
					}
				}
				else if(f instanceof CloudDriveExplorerFragment){
					if(tabShown!=NO_TABS){
						tabShown=CLOUD_TAB;
					}

					if(((CloudDriveExplorerFragment)f).getParentHandle()==-1|| ((CloudDriveExplorerFragment)f).getParentHandle()==megaApi.getRootNode().getHandle()){
						setRootTitle();
					}
					else{
						aB.setTitle(megaApi.getNodeByHandle(((CloudDriveExplorerFragment)f).getParentHandle()).getName());
					}
				}
				showFabButton(false);
			}
			else if(position == 2){
				if(f instanceof ChatExplorerFragment){
					if(tabShown!=NO_TABS){
						tabShown=CHAT_TAB;
					}

					aB.setTitle(getString(R.string.title_chat_explorer).toUpperCase());
				}
				else if(f instanceof IncomingSharesExplorerFragment){
					if(tabShown!=NO_TABS){
						tabShown=INCOMING_TAB;
					}

					if(deepBrowserTree==0){
						setRootTitle();
					}
					else{
						aB.setTitle(megaApi.getNodeByHandle(((IncomingSharesExplorerFragment)f).getParentHandle()).getName());
					}

					showFabButton(false);
				}
			}
		}
		supportInvalidateOptionsMenu();
	}
	
	public String getFragmentTag(int viewPagerId, int fragmentPosition)
	{
	     return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
	}

	public void finishActivity(){
		finishAndRemoveTask();
	}

	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		logDebug("onSaveInstanceState");
		super.onSaveInstanceState(bundle);
		bundle.putBoolean("folderSelected", folderSelected);
		cDriveExplorer = getCloudExplorerFragment();
		if(cDriveExplorer!=null){
			parentHandleCloud = cDriveExplorer.getParentHandle();
		}
		else{
			parentHandleCloud = -1;
		}
		bundle.putLong("parentHandleCloud", parentHandleCloud);

		iSharesExplorer = getIncomingExplorerFragment();
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
		bundle.putBoolean(SHOULD_RESTART_SEARCH, shouldRestartSearch);
		bundle.putString(QUERY_AFTER_SEARCH, queryAfterSearch);
		bundle.putString(CURRENT_ACTION, currentAction);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (getIntent() != null && mode == UPLOAD && folderSelected && filePreparedInfos == null) {
			mViewModel.ownFilePrepareTask(this,getIntent());
			createAndShowProgressDialog(false, getQuantityString(R.plurals.upload_prepare, 1));
		}
	}
	
	@Override
	public void onBackPressed() {
		logDebug("tabShown: " + tabShown);
		if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
		retryConnectionsAndSignalPresence();

		cDriveExplorer = getCloudExplorerFragment();
		iSharesExplorer = getIncomingExplorerFragment();

		if (importFileF) {
			switch (importFragmentSelected) {
				case CLOUD_FRAGMENT: {
					if(cDriveExplorer!=null && cDriveExplorer.onBackPressed() == 0){
						chooseFragment(IMPORT_FRAGMENT);
					}
					break;
				}
				case INCOMING_FRAGMENT:{
					if(iSharesExplorer!=null && iSharesExplorer.onBackPressed() == 0){
						iSharesExplorer = null;
						chooseFragment(IMPORT_FRAGMENT);
					}
					break;
				}
				case CHAT_FRAGMENT:{
                    if (ACTION_UPLOAD_TO_CHAT.equals(action)) {
                        finishActivity();
                    } else {
                        chatExplorer = getChatExplorerFragment();
                        if (chatExplorer != null) {
                            chatExplorer.clearSelections();
                            showFabButton(false);
                            chooseFragment(IMPORT_FRAGMENT);
                        }
                    }
					break;
				}
				case IMPORT_FRAGMENT:{
					finishActivity();
					break;
				}
			}
		}
		else if (isCloudVisible()) {
			if (cDriveExplorer.onBackPressed() == 0) {
				finishActivity();
			}
		}
		else if (isIncomingVisible()) {
			if (iSharesExplorer.onBackPressed() == 0) {
				finishActivity();
			}
		}
		else {
			super.onBackPressed();
		}
	}

	private boolean isCloudVisible() {
		return cDriveExplorer != null && cDriveExplorer.isVisible()
				&& ((tabShown == CLOUD_TAB || tabShown == NO_TABS) && !importFileF)
				|| (importFileF && importFragmentSelected == CLOUD_FRAGMENT);
	}

	private boolean isIncomingVisible() {
		return iSharesExplorer != null && iSharesExplorer.isVisible()
				&& ((tabShown == INCOMING_TAB && !importFileF)
				|| (importFileF && importFragmentSelected == INCOMING_FRAGMENT));
	}

	/**
	 * Checks if should start ChatUploadService to share the content or only attach it.
	 * If the ChatUploadService has to start, it also checks if the content is already
	 * available on Cloud to avoid start upload existing files.
	 */
	private void startChatUploadService() {
		if (chatListItems == null || chatListItems.isEmpty()) {
			logWarning("ERROR null chats to upload");
			filePreparedInfos = null;
			openManagerAndFinish();
			return;
		}

		logDebug("Launch chat upload with files " + filePreparedInfos.size());

		boolean notEmptyAttachedNodes = attachNodes != null && !attachNodes.isEmpty();
		boolean notEmptyUploadInfo = uploadInfos != null && !uploadInfos.isEmpty();
		filesChecked = 0;

		if (notEmptyAttachedNodes && !notEmptyUploadInfo) {
			// All files exists, not necessary start ChatUploadService
			pendingToAttach = attachNodes.size() * chatListItems.size();
			for (MegaNode node : attachNodes) {
				for (MegaChatRoom item : chatListItems) {
					megaChatApi.attachNode(item.getChatId(), node.getHandle(), this);
				}
			}

			return;
		}

		Intent intent = new Intent(this, ChatUploadService.class);

		if (notEmptyAttachedNodes) {
			// There are exist files and files for upload
			long[] attachNodeHandles = new long[attachNodes.size()];

			for (int i = 0; i < attachNodes.size(); i++) {
				attachNodeHandles[i] = attachNodes.get(i).getHandle();
			}

			intent.putExtra(ChatUploadService.EXTRA_ATTACH_FILES, attachNodeHandles);
		}

		long[] attachIdChats = new long[chatListItems.size()];
		for (int i = 0; i < chatListItems.size(); i++) {
			attachIdChats[i] = chatListItems.get(i).getChatId();
		}
		intent.putExtra(ChatUploadService.EXTRA_ATTACH_CHAT_IDS, attachIdChats);

		List<ShareInfo> infoToShare = notEmptyUploadInfo ? uploadInfos : filePreparedInfos;
		long[] idPendMsgs = new long[uploadInfos.size() * chatListItems.size()];
		HashMap<String, String> filesToUploadFingerPrint = new HashMap<>();
		int pos = 0;

		for (ShareInfo info : infoToShare) {
			long timestamp = System.currentTimeMillis() / 1000;
			String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());
			if (fingerprint == null) {
				logWarning("Error, fingerprint == NULL is not possible to access file for some reason");
				continue;
			}

			filesToUploadFingerPrint.put(fingerprint, info.getFileAbsolutePath());

			for (MegaChatRoom item : chatListItems) {
				PendingMessageSingle pendingMsg = createAttachmentPendingMessage(item.getChatId(),
						info.getFileAbsolutePath(), info.getTitle(), true);

				idPendMsgs[pos] = pendingMsg.getId();
				pos++;
			}
		}

		intent.putExtra(ChatUploadService.EXTRA_NAME_EDITED, nameFiles);
		intent.putExtra(ChatUploadService.EXTRA_UPLOAD_FILES_FINGERPRINTS, filesToUploadFingerPrint);
		intent.putExtra(ChatUploadService.EXTRA_PEND_MSG_IDS, idPendMsgs);
		intent.putExtra(ChatUploadService.EXTRA_COMES_FROM_FILE_EXPLORER, true);
		intent.putExtra(ChatUploadService.EXTRA_PARENT_NODE, myChatFilesNode.serialize());
		startService(intent);

		openManagerAndFinish();
	}

	private void openManagerAndFinish() {
		Intent intent = new Intent(this, ManagerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

		finish();
	}

	private void finishFileExplorer () {
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

	public void checkIfFilesExistsInMEGA () {
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

	/**
	 * Handle processed upload intent.
	 *
	 * @param infos List<ShareInfo> containing all the upload info.
	 */
	private void onIntentProcessed(List<ShareInfo> infos) {
		logDebug("onIntentChatProcessed");

		if (getIntent() != null && getIntent().getAction() != ACTION_PROCESSED) {
			getIntent().setAction(ACTION_PROCESSED);
		}

		if (infos == null) {
			statusDialog.dismiss();
			showSnackbar(getString(R.string.upload_can_not_open));
		} else if (existsMyChatFilesFolder()) {
			setMyChatFilesFolder(getMyChatFilesFolder());
			checkIfFilesExistsInMEGA();
		} else {
			megaApi.getMyChatFilesFolder(new GetAttrUserListener(this));
		}
	}

	private void onIntentProcessed() {
		List<ShareInfo> infos = filePreparedInfos;

		if (getIntent() != null && getIntent().getAction() != ACTION_PROCESSED) {
			getIntent().setAction(ACTION_PROCESSED);
		}


		logDebug("intent processed!");
		if (folderSelected) {
			if (infos == null) {
				dismissAlertDialogIfExists(statusDialog);
				showSnackbar(StringResourcesUtils.getString(R.string.upload_can_not_open));
				return;
			}

			if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
				dismissAlertDialogIfExists(statusDialog);
				showOverDiskQuotaPaywallWarning();
				return;
			}

			long parentHandle = cDriveExplorer != null
					? cDriveExplorer.getParentHandle()
					: parentHandleCloud;

			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode == null) {
				parentNode = megaApi.getRootNode();
			}

			MegaNode finalParentNode = parentNode;
			checkNameCollisionUseCase.check(infos, parentNode)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe((result, throwable) -> {
						dismissAlertDialogIfExists(statusDialog);

						if (throwable != null) {
							showSnackbar(StringResourcesUtils.getString(R.string.error_temporary_unavaible));
						} else {
							ArrayList<NameCollision> collisions = result.getFirst();
							List<ShareInfo> withoutCollisions = result.getSecond();

							if (!collisions.isEmpty()) {
								startActivity(NameCollisionActivity.getIntentForList(this, collisions));
							}

							if (!withoutCollisions.isEmpty()) {
								showSnackbar(StringResourcesUtils.getQuantityString(R.plurals.upload_began, withoutCollisions.size(), withoutCollisions.size()));

								backToCloud(finalParentNode.getHandle(), infos.size());

								for (ShareInfo info : infos) {
									if (transfersManagement.shouldBreakTransfersProcessing()) {
										break;
									}

									Intent intent = new Intent(this, UploadService.class);
									intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
									intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
									if (nameFiles != null && nameFiles.get(info.getTitle()) != null
											&& !nameFiles.get(info.getTitle()).equals(info.getTitle())) {
										intent.putExtra(UploadService.EXTRA_NAME_EDITED, nameFiles.get(info.getTitle()));
									}
									intent.putExtra(UploadService.EXTRA_PARENT_HASH, finalParentNode.getHandle());
									intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
									startService(intent);
								}

								filePreparedInfos = null;
								logDebug("finish!!!");
								finishActivity();
							}
						}
					});
		}
	}

    public void buttonClick(long[] handles){
		logDebug("handles: " + handles.length);

        Intent intent = new Intent();
        intent.putExtra(NODE_HANDLES, handles);
        intent.putStringArrayListExtra(SELECTED_CONTACTS, selectedContacts);
        setResult(RESULT_OK, intent);
		finishActivity();
    }

	/**
	 * Method to create and show a progress dialog
	 * @param cancelable Flag to set if the progress dialog is cancelable or not
	 * @param message Message to display into the progress dialog
	 */
	private void createAndShowProgressDialog(boolean cancelable, String message) {
		AlertDialog temp;
		try {
			temp = MegaProgressDialogUtil.createProgressDialog(this, message);
			temp.setCancelable(cancelable);
			temp.setCanceledOnTouchOutside(cancelable);
			temp.show();
		} catch (Exception e) {
			logWarning("Error creating and showing progress dialog.", e);
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
		else if (mode == UPLOAD || mode == SAVE){

			logDebug("mode UPLOAD");

			if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
				if (TYPE_TEXT_PLAIN.equals(intent.getType())) {
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
								body.append(sharedText3);
							}

							MegaNode parentNode = megaApi.getNodeByHandle(handle);
							if(parentNode == null){
								parentNode = megaApi.getRootNode();
							}

							if (isURL) {
								showNewURLFileDialog(this, parentNode, body.toString(), sharedText2);
							} else {
								showNewFileDialog(this, parentNode, body.toString());
							}

							return;
						}
					}
				}
			}

			if (filePreparedInfos == null){
				mViewModel.ownFilePrepareTask(this,getIntent());
				createAndShowProgressDialog(false, getQuantityString(R.plurals.upload_prepare, 1));
			}
			else{
				onIntentProcessed();
			}
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
				intent.putExtra(EXTRA_SELECTED_FOLDER, handle);
				intent.putStringArrayListExtra(SELECTED_CONTACTS, selectedContacts);
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
				intent.putExtra(EXTRA_SELECTED_FOLDER, parentNode.getHandle());
				intent.putStringArrayListExtra(SELECTED_CONTACTS, selectedContacts);
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

	private void backToCloud(long handle, int numberUploads){
		logDebug("handle: " + handle);

		Intent startIntent = new Intent(this, ManagerActivity.class)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
				.putExtra(SHOW_MESSAGE_UPLOAD_STARTED, true)
				.putExtra(NUMBER_UPLOADS, numberUploads);
		if(handle!=-1){
			startIntent.setAction(ACTION_OPEN_FOLDER);
			startIntent.putExtra("PARENT_HANDLE", handle);
		}
		startActivity(startIntent);
	}

    public void showSnackbar(String s){
		showSnackbar(fragmentContainer, s);
    }

    public void createFile(String name, String data, MegaNode parentNode, boolean isURL){
		if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
			showOverDiskQuotaPaywallWarning();
			return;
		}

		File file;
		if (isURL) {
			file = createTemporalURLFile(this, name, data);
		} else {
			file = createTemporalTextFile(this, name, data);
		}

		if (file == null) {
			showSnackbar(StringResourcesUtils.getString(R.string.general_text_error));
			return;
		}

		checkNameCollisionUseCase.check(file.getName(), parentNode)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(handle -> {
							NameCollision collision = NameCollision.Upload
									.getUploadCollision(handle, file, parentNode.getParentHandle());

							startActivity(NameCollisionActivity.getIntentForSingleItem(this, collision));
						},
						throwable -> {
							if (throwable instanceof MegaNodeException.ParentDoesNotExistException) {
								showSnackbar(StringResourcesUtils.getString(R.string.general_text_error));
							} else if (throwable instanceof MegaNodeException.ChildDoesNotExistsException) {
								Intent intent = new Intent(this, UploadService.class);
								intent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
								intent.putExtra(UploadService.EXTRA_NAME, file.getName());
								intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
								intent.putExtra(UploadService.EXTRA_SIZE, file.getTotalSpace());
								startService(intent);

								logDebug("After UPLOAD click - back to Cloud");
								this.backToCloud(parentNode.getHandle(), 1);
								finishActivity();
							}
						});
	}

	@Override
	public void finishRenameActionWithSuccess(@NonNull String newName) {
		//No action needed
	}

	@Override
	public void actionConfirmed() {
		//No update needed
	}

	@Override
	public void createFolder(@NotNull String title) {

		logDebug("createFolder");
		if (!isOnline(this)){
            showSnackbar(getString(R.string.error_server_connection_problem));
			return;
		}

		if(isFinishing()){
			return;
		}

		long parentHandle = -1;

		cDriveExplorer = getCloudExplorerFragment();
		iSharesExplorer = getIncomingExplorerFragment();

		if (isCloudVisible()) {
			parentHandle = cDriveExplorer.getParentHandle();
		}
		else if (isIncomingVisible()) {
			parentHandle = iSharesExplorer.getParentHandle();
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
					statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.context_creating_folder));
					statusDialog.show();
				}
				catch(Exception e){
					return;
				}

				megaApi.createFolder(title, parentNode, new CreateFolderListener(this));
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
						statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.context_creating_folder));
						statusDialog.show();
					}
					catch(Exception e){
						return;
					}

					megaApi.createFolder(title, parentNode, new CreateFolderListener(this));
				}
				else{
					showSnackbar(getString(R.string.context_folder_already_exists));
				}
			}
		}
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
		if (request.getType() == MegaRequest.TYPE_LOGIN){

			if (error.getErrorCode() != MegaError.API_OK) {
                logWarning("Login failed with error code: " + error.getErrorCode());
				MegaApplication.setLoggingIn(false);
			}
			else{
				loginProgressBar.setVisibility(View.VISIBLE);
				loginFetchNodesProgressBar.setVisibility(View.GONE);
				loggingInText.setVisibility(View.VISIBLE);
				fetchingNodesText.setVisibility(View.VISIBLE);
				prepareNodesText.setVisibility(View.GONE);

                gSession = megaApi.dumpSession();
                credentials = new UserCredentials(lastEmail, gSession, "", "", "");
                dbH.saveCredentials(credentials);
				logDebug("Logged in with session");
				logDebug("Setting account auth token for folder links.");
				megaApiFolder.setAccountAuth(megaApi.getAccountAuth());
				megaApi.fetchNodes(this);

                // Get cookies settings after login.
                MegaApplication.getInstance().checkEnabledCookies();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){

			if (error.getErrorCode() == MegaError.API_OK){
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

				MegaApplication.setLoggingIn(false);
				afterLoginAndFetch();
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
		if (getCloudExplorerFragment() != null){
			if (megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()) != null){
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()));
				cDriveExplorer.setNodes(nodes);
				cDriveExplorer.getRecyclerView().invalidate();
			}
			else{
				if (megaApi.getRootNode() != null){
					cDriveExplorer.setParentHandle(megaApi.getRootNode().getHandle());
					nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()));
					cDriveExplorer.setNodes(nodes);
					cDriveExplorer.getRecyclerView().invalidate();
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
		if (megaApi != null) {
			megaApi.removeGlobalListener(this);
		}

		File childThumbDir = new File(getThumbFolder(this), ImportFilesFragment.THUMB_FOLDER);
		if (isFileAvailable(childThumbDir)){
			try {
				deleteFile(childThumbDir);
			} catch (IOException e) {
				logWarning("IOException deleting childThumbDir.", e);
			}
		}
		mViewModel.shutdownExecutorService();
		super.onDestroy();
	}

	private void deleteFile(File file) throws IOException {
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
	        	showNewFolderDialog(this, this);
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
							Intent in = new Intent(this, AddContactActivity.class);
							in.putExtra("contactType", CONTACT_TYPE_MEGA);
							startActivityForResult(in, REQUEST_CREATE_CHAT);
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
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		logDebug("Request code: " + requestCode + ", Result code: " + resultCode);

		if (requestCode == REQUEST_CREATE_CHAT && resultCode == RESULT_OK) {
			logDebug("REQUEST_CREATE_CHAT OK");

			if (intent == null) {
				logWarning("Return.....");
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);

			if (contactsData != null) {
				if (contactsData.size() == 1) {
					MegaUser user = megaApi.getContact(contactsData.get(0));
					if (user != null) {
						logDebug("Chat with contact: " + contactsData.size());
						startOneToOneChat(user);
					}
				} else {
					logDebug("Create GROUP chat");
					MegaChatPeerList peers = MegaChatPeerList.createInstance();
					for (int i = 0; i < contactsData.size(); i++) {
						MegaUser user = megaApi.getContact(contactsData.get(i));
						if (user != null) {
							peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
						}
					}
					logDebug("create group chat with participants: " + peers.size());

					final String chatTitle = intent.getStringExtra(AddContactActivity.EXTRA_CHAT_TITLE);
					final boolean isEKR = intent.getBooleanExtra(AddContactActivity.EXTRA_EKR, false);
					if (isEKR) {
						megaChatApi.createChat(true, peers, chatTitle, this);
					} else {
						final boolean chatLink = intent.getBooleanExtra(AddContactActivity.EXTRA_CHAT_LINK, false);

						if (chatLink) {
							if (chatTitle != null && !chatTitle.isEmpty()) {
								CreateGroupChatWithPublicLink listener = new CreateGroupChatWithPublicLink(this, chatTitle);
								megaChatApi.createPublicChat(peers, chatTitle, listener);
							} else {
								showAlert(this, getString(R.string.message_error_set_title_get_link), null);
							}
						} else {
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

	private void startOneToOneChat(MegaUser user){
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

	private void getChatAdded (ArrayList<ChatExplorerListItem> listItems) {
		ArrayList<MegaChatRoom> chats = new ArrayList<>();
		ArrayList<MegaUser> users = new ArrayList<>();

		createAndShowProgressDialog(true, StringResourcesUtils.getString(R.string.preparing_chats));

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
			CreateChatListener listener = new CreateChatListener(
					CreateChatListener.SEND_FILE_EXPLORER_CONTENT, chats, users, this, this,
					this::sendToChats);

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
							Intent intent = new Intent(this, AddContactActivity.class);
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

	private Unit sendToChats(List<? extends MegaChatRoom> chats){

		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			}
			catch(Exception ex){}
		}

		chatListItems.addAll(chats);

		if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
			Bundle extras = intent.getExtras();
			if (TYPE_TEXT_PLAIN.equals(intent.getType()) && extras != null && !extras.containsKey(Intent.EXTRA_STREAM)) {
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
						Intent intent = new Intent(this, ManagerActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
						intent.putExtra(CHAT_ID, idChat);
						startActivity(intent);
					}
				}
				else{
					Intent chatIntent = new Intent(this, ManagerActivity.class);
					chatIntent.setAction(ACTION_CHAT_SUMMARY);
					startActivity(chatIntent);
				}

				return Unit.INSTANCE;
			}
		}

		if (filePreparedInfos == null) {
			createAndShowProgressDialog(false, getQuantityString(R.plurals.upload_prepare, 1));
			filePrepareUseCase.prepareFiles(intent)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe((shareInfo, throwable) -> {
						if (throwable == null) {
							onIntentProcessed(shareInfo);
						}
					});
		} else {
			onIntentProcessed(filePreparedInfos);
		}

		return Unit.INSTANCE;
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

	private ChatExplorerFragment getChatExplorerFragment () {

		ChatExplorerFragment c;

		if (importFileF) {
			return (ChatExplorerFragment) getSupportFragmentManager().findFragmentByTag("chatExplorer");
		}

		if (mTabsAdapterExplorer == null) return null;

		if(isChatFirst){
			c = (ChatExplorerFragment) mTabsAdapterExplorer.instantiateItem(viewPagerExplorer, 0);
		}
		else{
			c = (ChatExplorerFragment) mTabsAdapterExplorer.instantiateItem(viewPagerExplorer, 2);
		}

		if (c.isAdded()) {
			return c;
		}

		return null;
	}

	private IncomingSharesExplorerFragment getIncomingExplorerFragment() {
		if (mTabsAdapterExplorer == null) return null;

		if (!isChatFirst) {
			IncomingSharesExplorerFragment iS =
					(IncomingSharesExplorerFragment) mTabsAdapterExplorer.instantiateItem(viewPagerExplorer, 1);

			if (iS.isAdded()) {
				return iS;
			}
		}

		return null;
	}

	private CloudDriveExplorerFragment getCloudExplorerFragment () {
		CloudDriveExplorerFragment cD;

		if (importFileF || tabShown == NO_TABS) {
			return (CloudDriveExplorerFragment) getSupportFragmentManager().findFragmentByTag("cDriveExplorer");
		}

		if (mTabsAdapterExplorer == null) return null;

		if (isChatFirst) {
			cD = (CloudDriveExplorerFragment) mTabsAdapterExplorer.instantiateItem(viewPagerExplorer, 1);
		}
		else {
			cD = (CloudDriveExplorerFragment) mTabsAdapterExplorer.instantiateItem(viewPagerExplorer, 0);
		}

		if (cD.isAdded()) {
			return cD;
		}

		return null;
	}

	public void refreshOrderNodes (int order) {
		cDriveExplorer = getCloudExplorerFragment();
		if (cDriveExplorer != null) {
			cDriveExplorer.orderNodes(order);
		}

		iSharesExplorer = getIncomingExplorerFragment();
		if (iSharesExplorer != null) {
			iSharesExplorer.orderNodes(order);
		}
	}

	private void refreshViewNodes (boolean isList) {
		this.isList = isList;

		cDriveExplorer =  getCloudExplorerFragment();
		iSharesExplorer = getIncomingExplorerFragment();

		if (cDriveExplorer != null) {
			getSupportFragmentManager().beginTransaction().detach(cDriveExplorer).commitNowAllowingStateLoss();
			getSupportFragmentManager().beginTransaction().attach(cDriveExplorer).commitNowAllowingStateLoss();
		}

		if (iSharesExplorer != null) {
			getSupportFragmentManager().beginTransaction().detach(iSharesExplorer).commitNowAllowingStateLoss();
			getSupportFragmentManager().beginTransaction().attach(iSharesExplorer).commitNowAllowingStateLoss();
		}
	}

	public void collapseSearchView () {
		if (searchMenuItem == null) {
			return;
		}
		collapsedByClick = true;
		searchMenuItem.collapseActionView();
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

	public MegaNode parentMoveCopy() {
		return parentMoveCopy;
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

	public DrawerItem getCurrentItem() {
		if (viewPagerExplorer != null) {
			if (viewPagerExplorer.getCurrentItem() == 0) {
				cDriveExplorer = getCloudExplorerFragment();
				if (cDriveExplorer != null) {
					return DrawerItem.CLOUD_DRIVE;
				}
			}
			else {
				iSharesExplorer = getIncomingExplorerFragment();
				if (iSharesExplorer != null) {
					return DrawerItem.SHARED_ITEMS;
				}
			}
		}
		return null;
	}

	public boolean isList () {
		return isList;
	}

	public int getItemType() {
		if (isList) {
			return MegaNodeAdapter.ITEM_VIEW_TYPE_LIST;
		}
		else {
			return MegaNodeAdapter.ITEM_VIEW_TYPE_GRID;
		}
	}

	public boolean isMultiselect() {
		return multiselect;
	}

	public void setMyChatFilesFolder(MegaNode myChatFilesNode) {
		this.myChatFilesNode = myChatFilesNode;
	}

	public void finishCreateFolder(boolean success, long handle) {
		try {
			statusDialog.dismiss();
		}
		catch (Exception ex) {}

		if (success){
			cDriveExplorer = getCloudExplorerFragment();
			iSharesExplorer = getIncomingExplorerFragment();

			if (isCloudVisible()){
				cDriveExplorer.navigateToFolder(handle);
				parentHandleCloud = handle;
				hideTabs(true, CLOUD_TAB);
			}
			else if (isIncomingVisible()){
				iSharesExplorer.navigateToFolder(handle);
				parentHandleIncoming = handle;
				hideTabs(true, INCOMING_TAB);
			}
		}
	}

	public void setShouldRestartSearch(boolean shouldRestartSearch) {
		this.shouldRestartSearch = shouldRestartSearch;
	}

	public boolean shouldRestartSearch() {
		return shouldRestartSearch;
	}

	public String getQuerySearch() {
		return querySearch;
	}

	public void clearQuerySearch() {
		querySearch = null;
	}

    public void setQueryAfterSearch() {
	    this.queryAfterSearch = querySearch;
    }

    public boolean shouldReopenSearch() {
	    if (queryAfterSearch == null) return false;

        openSearchView(queryAfterSearch);
	    queryAfterSearch = null;
	    return true;
    }

	/**
	 * Hides or shows tabs of a section depending on the navigation level
	 * and if select mode is enabled or not.
	 *
	 * @param hide       If true, hides the tabs, else shows them.
	 * @param currentTab The current tab where the action happens.
	 */
	public void hideTabs(boolean hide, int currentTab) {
		if (!hide && (queryAfterSearch != null || isSearchExpanded || pendingToOpenSearchView)) {
			return;
		}

		switch (currentTab) {
			case CLOUD_FRAGMENT:
				if (getCloudExplorerFragment() == null
						|| (!hide && parentHandleCloud != getCloudRootHandle() && parentHandleCloud != INVALID_HANDLE)) {
					return;
				}

				break;

			case INCOMING_FRAGMENT:
				if (getIncomingExplorerFragment() == null
						|| (!hide && parentHandleIncoming != INVALID_HANDLE)) {
					return;
				}

				break;

			case CHAT_FRAGMENT:
				if (getChatExplorerFragment() == null) {
					return;
				}

				break;
		}

		viewPagerExplorer.disableSwipe(hide);

		// If no tab should be shown, keep hide.
		tabLayoutExplorer.setVisibility(hide || (tabShown == NO_TABS) ? View.GONE : View.VISIBLE);
	}

	public void showSortByPanel() {
		if (isBottomSheetDialogShown(bottomSheetDialogFragment)) {
			return;
		}

		if (getIncomingExplorerFragment() != null && deepBrowserTree == 0
				&& viewPagerExplorer != null && viewPagerExplorer.getCurrentItem() == INCOMING_TAB) {
			bottomSheetDialogFragment = SortByBottomSheetDialogFragment.newInstance(ORDER_OTHERS);
		} else {
			bottomSheetDialogFragment = SortByBottomSheetDialogFragment.newInstance(ORDER_CLOUD);
		}

		bottomSheetDialogFragment.show(getSupportFragmentManager(),
				bottomSheetDialogFragment.getTag());
	}
}
