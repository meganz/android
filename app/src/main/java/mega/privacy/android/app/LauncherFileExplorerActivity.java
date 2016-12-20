package mega.privacy.android.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
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
import android.widget.ImageButton;
import android.widget.ImageView;
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

import mega.privacy.android.app.lollipop.CloudDriveExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.IncomingSharesExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
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
import nz.mega.sdk.MegaUser;

public class LauncherFileExplorerActivity extends PinActivity implements MegaRequestListenerInterface, MegaGlobalListenerInterface, OnClickListener{	
	
	public static String ACTION_PROCESSED = "CreateLink.ACTION_PROCESSED";
	
	public static String ACTION_PICK_MOVE_FOLDER = "ACTION_PICK_MOVE_FOLDER";
	public static String ACTION_PICK_COPY_FOLDER = "ACTION_PICK_COPY_FOLDER";
	public static String ACTION_PICK_IMPORT_FOLDER = "ACTION_PICK_IMPORT_FOLDER";
	public static String ACTION_SELECT_FOLDER = "ACTION_SELECT_FOLDER";
	public static String ACTION_SELECT_FILE = "ACTION_SELECT_FILE";
	public static String ACTION_UPLOAD_SELFIE = "ACTION_UPLOAD_SELFIE";	
	public static String ACTION_CHOOSE_MEGA_FOLDER_SYNC = "ACTION_CHOOSE_MEGA_FOLDER_SYNC";
	
	private String gSession;
	String gcFTag = "";
	private String lastEmail;
	long gParentHandle;
	
	UserCredentials credentials;
	
	DisplayMetrics outMetrics;
	RelativeLayout fragmentContainer;
	
	Toolbar tB;
	ActionBar aB;

	LinearLayout loginLoggingIn;
	ProgressBar loginProgressBar;
	ProgressBar loginFetchNodesProgressBar;
	TextView generatingKeysText;
	TextView queryingSignupLinkText;
	TextView confirmingAccountText;
	TextView loggingInText;
	TextView fetchingNodesText;
	TextView prepareNodesText;
	
	private MegaApiAndroid megaApi;
	
	public static int UPLOAD = 0;
	public static int MOVE = 1;
	public static int COPY = 2;
	public static int CAMERA = 3;
	public static int IMPORT = 4;
	public static int SELECT = 5;
	public static int UPLOAD_SELFIE = 6;
	public static int SELECT_CAMERA_FOLDER = 7;
	
	private boolean folderSelected = false;
	boolean selectFile = false;
	private int mode;
	private long[] moveFromHandles;
	private long[] copyFromHandles;
	private String[] selectedContacts;
	private String imagePath;
	
	ArrayList<MegaNode> nodes;
	
	Intent intent = null;
	private Handler handler;
	ProgressDialog statusDialog;
	private AlertDialog newFolderDialog;
	
	private static int EDIT_TEXT_ID = 2;
	
	public static int CLOUD_TAB = 0;
	public static int INCOMING_TAB = 1;
	
	private CloudDriveExplorerFragmentLollipop cDriveExplorer;
	private IncomingSharesExplorerFragmentLollipop iSharesExplorer;
	private TabHost mTabHostExplorer;
	TabsAdapter mTabsAdapterExplorer;
    ViewPager viewPagerExplorer;
    private int tabShown = CLOUD_TAB;

    private List<ShareInfo> filePreparedInfos;
    
    MenuItem createFolderMenuItem;
    
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
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		log("onCreate first");
		super.onCreate(savedInstanceState);

//			DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
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

		setContentView(R.layout.activity_file_explorer);

		fragmentContainer = (RelativeLayout) findViewById(R.id.fragment_container_file_explorer);

		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_explorer);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		log("aB.setHomeAsUpIndicator_65");
		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);

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
			getSupportActionBar().hide();
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			loginLoggingIn.setVisibility(View.VISIBLE);
//				generatingKeysText.setVisibility(View.VISIBLE);
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

		aB.setTitle(getString(R.string.section_cloud_drive));

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);


	}
	
	private void afterLoginAndFetch(){
		log("afterLoginAndFetch");
		

		handler = new Handler();

		if ((intent != null) && (intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_PICK_MOVE_FOLDER)){
				log("ACTION_PICK_MOVE_FOLDER");
				mode = MOVE;
				moveFromHandles = intent.getLongArrayExtra("MOVE_FROM");

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
			}
			else if (intent.getAction().equals(ACTION_PICK_IMPORT_FOLDER)){
				log("action = ACTION_PICK_IMPORT_FOLDER");
				mode = IMPORT;
			}
			else if (intent.getAction().equals(ACTION_SELECT_FOLDER)){
				log("action = ACTION_SELECT_FOLDER");
				mode = SELECT;
				selectedContacts=intent.getStringArrayExtra("SELECTED_CONTACTS");

			}
			else if (intent.getAction().equals(ACTION_SELECT_FILE)){
				log("action = ACTION_SELECT_FILE");
				mode = SELECT;
				selectFile = true;
				selectedContacts=intent.getStringArrayExtra("SELECTED_CONTACTS");
			}
			else if(intent.getAction().equals(ACTION_UPLOAD_SELFIE)){
				log("action = ACTION_UPLOAD_SELFIE");
				mode = UPLOAD_SELFIE;
				imagePath=intent.getStringExtra("IMAGE_PATH");
			}
		}

		mTabHostExplorer = (TabHost)findViewById(R.id.tabhost_explorer);
		mTabHostExplorer.setup();
		viewPagerExplorer = (ViewPager) findViewById(R.id.explorer_tabs_pager);

		//Create tabs
		mTabHostExplorer.getTabWidget().setBackgroundColor(Color.BLACK);
		mTabHostExplorer.getTabWidget().setDividerDrawable(null);
		mTabHostExplorer.setVisibility(View.VISIBLE);


		if (mTabsAdapterExplorer == null){
			mTabsAdapterExplorer= new TabsAdapter(this, mTabHostExplorer, viewPagerExplorer);

			TabHost.TabSpec tabSpec3 = mTabHostExplorer.newTabSpec("cloudExplorerFragment");
			tabSpec3.setIndicator(getTabIndicator(mTabHostExplorer.getContext(), getString(R.string.section_cloud_drive).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout
			//tabSpec.setContent(contentID);
			//mTabHostContacts.addTab(tabSpec);
			TabHost.TabSpec tabSpec4 = mTabHostExplorer.newTabSpec("incomingExplorerFragment");
			tabSpec4.setIndicator(getTabIndicator(mTabHostExplorer.getContext(), getString(R.string.tab_incoming_shares).toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout

			Bundle b1 = new Bundle();
			b1.putInt("MODE", mode);
			if(selectFile){
				b1.putBoolean("SELECTFILE", true);
			}
			else{
				b1.putBoolean("SELECTFILE", false);
			}

			mTabsAdapterExplorer.addTab(tabSpec3, CloudDriveExplorerFragmentLollipop.class, b1);
			mTabsAdapterExplorer.addTab(tabSpec4, IncomingSharesExplorerFragmentLollipop.class, b1);
		}

		mTabHostExplorer.setOnTabChangedListener(new OnTabChangeListener(){
			@Override
			public void onTabChanged(String tabId) {
				supportInvalidateOptionsMenu();
				log("TabId :"+ tabId);
				if(tabId.equals("cloudExplorerFragment")){

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
				else if(tabId.equals("incomingExplorerFragment")){

					tabShown=INCOMING_TAB;

					String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
					iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);

					if(iSharesExplorer!=null){
						if(iSharesExplorer.deepBrowserTree==0){
							changeTitle(getString(R.string.title_incoming_shares_explorer));
						}
						else{
							changeTitle(iSharesExplorer.name);
						}
					}
				}
			 }
		});

		for (int i=0;i<mTabsAdapterExplorer.getCount();i++){
			final int index = i;
			mTabHostExplorer.getTabWidget().getChildAt(i).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					viewPagerExplorer.setCurrentItem(index);
				}
			});
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
			if (iSharesExplorer.deepBrowserTree==0){
				Intent intent = new Intent();
				setResult(RESULT_FIRST_USER, intent);
				finish();
				return;
			}
		}

		folderSelected = true;
		this.gParentHandle = handle;

		//Only UPLOAD MODE is valid, no way to enter in the other options!
		if (mode == MOVE) {
			log("MOVE option");
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
			log("COPY option");
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
			log("UPLOAD_SELFIE option");
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
			log("SELECT option");
			if(selectFile)
			{
				Intent intent = new Intent();
				intent.putExtra("SELECT", handle);
				intent.putExtra("SELECTED_CONTACTS", selectedContacts);
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
				intent.putExtra("SELECTED_CONTACTS", selectedContacts);
				setResult(RESULT_OK, intent);
				finish();
			}

		}
		else if (mode == SELECT_CAMERA_FOLDER){
			log("SELECT_CAMERA_FOLDER option");
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
	
	@Override
	public void onDestroy(){
		if(megaApi != null){	
			megaApi.removeGlobalListener(this);
		}
		
		super.onDestroy();
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart");
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
				getSupportActionBar().show();
				afterLoginAndFetch();

			}
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
	
	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
		
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
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
//			case R.id.file_explorer_new_folder:{
//				showNewFolderDialog(null);
//				break;
//			}
//			case R.id.file_explorer_back:{
//				onBackPressed();
//				break;
//			}
//			case R.id.file_explorer_window_title:{
//				if (backVisible){
//					onBackPressed();
//					break;
//				}
//			}
		}
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
	
	@Override
	public void onBackPressed() {
		log("onBackPressed: "+tabShown);		

		if(tabShown==CLOUD_TAB){
			String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
			gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
			cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);

			if(cDriveExplorer!=null){
				if (cDriveExplorer.onBackPressed() == 0){
					log("Call to super.onBackPressed");
					super.onBackPressed();
					log("Intent to Manager");
					Intent startIntent = new Intent(this, ManagerActivityLollipop.class);
//						loginIntent.setAction(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD);
					startActivity(startIntent);
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
					log("Intent to Manager");
					Intent startIntent = new Intent(this, ManagerActivityLollipop.class);
//						loginIntent.setAction(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD);
					startActivity(startIntent);
				}
			}
		}
		else{
			super.onBackPressed();
		}

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
			}
			else{
				gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
				cDriveExplorer = (CloudDriveExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
				if (cDriveExplorer != null){
					parentHandle = cDriveExplorer.getParentHandle();
				}
			}
		}
		else if (tabShown == INCOMING_TAB){
			if (iSharesExplorer != null){
				parentHandle = iSharesExplorer.getParentHandle();
			}
			else{
				gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
				iSharesExplorer = (IncomingSharesExplorerFragmentLollipop) getSupportFragmentManager().findFragmentByTag(gcFTag);
				if (iSharesExplorer != null){
					parentHandle = iSharesExplorer.getParentHandle();
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
	
	public void setParentHandle (long parentHandle){
		this.gParentHandle = parentHandle;
	}

	public void changeTitle (String title){
		aB.setTitle(title);
	}

	private View getTabIndicator(Context context, String title) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);

        TextView tv = (TextView) view.findViewById(R.id.textView);
        tv.setText(title);
        return view;
    }
	
	private String getFragmentTag(int viewPagerId, int fragmentPosition){
	     return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
	}
	
	public static void log(String message) {
		Util.log("LauncherFileExplorerActivity", message);
	}
}
