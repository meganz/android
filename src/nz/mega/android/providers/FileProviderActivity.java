package nz.mega.android.providers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import nz.mega.android.CameraSyncService;
import nz.mega.android.DatabaseHandler;
import nz.mega.android.DownloadService;
import nz.mega.android.MegaApplication;
import nz.mega.android.PinActivity;
import nz.mega.android.R;
import nz.mega.android.TabsAdapter;
import nz.mega.android.UserCredentials;
import nz.mega.android.utils.Util;
import nz.mega.components.MySwitch;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


public class FileProviderActivity extends PinActivity implements OnClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaTransferListenerInterface{
	
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
	private String gSession;
	private String gPublicKey;
	private String gPrivateKey;
	UserCredentials credentials;
	
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
	Display display;
	EditText et_user;
	EditText et_password;
	Button bRegister;
	Button bLogin;
	ImageView loginThreeDots;
	TextView loginABC;
	MySwitch loginSwitch;	
	
	public static int CLOUD_TAB = 0;
	public static int INCOMING_TAB = 1;
	
	String SD_CACHE_PATH = "/Android/data/nz.mega.android/cache/files";

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
	
	private int tabShown = CLOUD_TAB;
	
	private CloudDriveProviderFragment cDriveExplorer;
	private IncomingSharesProviderFragment iSharesProvider;

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
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
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
		credentials = dbH.getCredentials();
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
			loginCreateAccount.setVisibility(View.VISIBLE);
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
				gSession = credentials.getSession();
				
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
				loginCreateAccount.setVisibility(View.VISIBLE);
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
				setContentView(R.layout.activity_file_explorer);
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
					String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
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
					String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
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
			
			mTabHostProvider = (TabHost)findViewById(R.id.tabhost_explorer);
			mTabHostProvider.setup();
	        viewPagerProvider = (ViewPager) findViewById(R.id.explorer_tabs_pager);  
	        
	        //Create tabs
	        mTabHostProvider.getTabWidget().setBackgroundColor(Color.BLACK);
			
	        mTabHostProvider.setVisibility(View.VISIBLE);    			
			
			
			if (mTabsAdapterProvider == null){
				mTabsAdapterProvider= new TabsAdapter(this, mTabHostProvider, viewPagerProvider);   	
				
				TabHost.TabSpec tabSpec3 = mTabHostProvider.newTabSpec("cloudProviderFragment");
				tabSpec3.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.tab_cloud_drive_explorer))); // new function to inject our own tab layout
		        //tabSpec.setContent(contentID);
		        //mTabHostContacts.addTab(tabSpec);
		        TabHost.TabSpec tabSpec4 = mTabHostProvider.newTabSpec("incomingProviderFragment");
		        tabSpec4.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.tab_incoming_shares_explorer))); // new function to inject our own tab layout
		                	          				
				mTabsAdapterProvider.addTab(tabSpec3, CloudDriveProviderFragment.class, null);
				mTabsAdapterProvider.addTab(tabSpec4, IncomingSharesProviderFragment.class, null);
				
			}
			
			mTabHostProvider.setOnTabChangedListener(new OnTabChangeListener(){
	            @Override
	            public void onTabChanged(String tabId) {
	            	log("TabId :"+ tabId);
	                if(tabId.equals("cloudProviderFragment")){                     	
	
	     				tabShown=CLOUD_TAB;
	    				String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
	    				gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
	    				cDriveExplorer = (CloudDriveProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
	
	    				if(cDriveExplorer!=null){
	    					if(cDriveExplorer.getParentHandle()==-1|| cDriveExplorer.getParentHandle()==megaApi.getRootNode().getHandle()){
	    						changeTitle(getString(R.string.section_cloud_drive));
	    						changeBackVisibility(false);
	    					}
	    					else{
	    						changeTitle(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()).getName());
	    						changeBackVisibility(true);
	    					}    					
	    				}	
	                }
	                else if(tabId.equals("incomingProviderFragment")){                     	
	
	            		tabShown=INCOMING_TAB;
	            		
	            		String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
	            		gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
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
	
	private View getTabIndicator(Context context, String title) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);

        TextView tv = (TextView) view.findViewById(R.id.textView);
        tv.setText(title);
        return view;
    }
	
	public void changeTitle (String title){
		if (windowTitle != null){
			windowTitle.setText(title);
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
							Uri contentUri = FileProvider.getUriForFile(this, "nz.mega.android.providers.fileprovider", fileToShare);
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
		
		if(tabShown==CLOUD_TAB){
			String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
			gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
			cDriveExplorer = (CloudDriveProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
	
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
	
	public void onIntentProcessed() {
//		List<ShareInfo> infos = filePreparedInfos;
//		
//		if (statusDialog != null) {
//			try { 
//				statusDialog.dismiss(); 
//			} 
//			catch(Exception ex){}
//		}
//		
//		log("intent processed!");
//		if (folderSelected) {
//			if (infos == null) {
//				Util.showErrorAlertDialog(getString(R.string.upload_can_not_open),
//						true, this);
//				return;
//			}
//			else {
//				long parentHandle;
//				if (cDriveExplorer != null){
//					parentHandle = cDriveExplorer.getParentHandle();
//				}
//				else{
//					parentHandle = gParentHandle;
//				}
//				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
//				if(parentNode == null){
//					parentNode = megaApi.getRootNode();
//				}
//				Toast.makeText(getApplicationContext(), getString(R.string.upload_began),
//						Toast.LENGTH_SHORT).show();
//				for (ShareInfo info : infos) {
//					Intent intent = new Intent(this, UploadService.class);
//					intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
//					intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
//					intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
//					intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
//					startService(intent);
//				}
//				filePreparedInfos = null;
//				finish();
//			}	
//		}
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
			loginCreateAccount.setVisibility(View.VISIBLE);
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
			loginCreateAccount.setVisibility(View.VISIBLE);
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
	
	public static void log(String log) {
		Util.log("FileProviderActivity", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart");
	}

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
				loginCreateAccount.setVisibility(View.VISIBLE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				
				Util.showErrorAlertDialog(errorMessage, false, this);
				
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				dbH.clearCredentials();
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
				credentials = new UserCredentials(lastEmail, gSession);
				
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				dbH.clearCredentials();
				dbH.saveCredentials(credentials);
				
				log("Logged in: " + gSession);
				
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
				loginCreateAccount.setVisibility(View.VISIBLE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				prepareNodesText.setVisibility(View.GONE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				
				Util.showErrorAlertDialog(errorMessage, false, this);
			}
			else{
				setContentView(R.layout.activity_file_explorer);
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
					String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
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
					String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
					gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
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
			
			mTabHostProvider = (TabHost)findViewById(R.id.tabhost_explorer);
			mTabHostProvider.setup();
	        viewPagerProvider = (ViewPager) findViewById(R.id.explorer_tabs_pager);  
	        
	        //Create tabs
	        mTabHostProvider.getTabWidget().setBackgroundColor(Color.BLACK);
			
	        mTabHostProvider.setVisibility(View.VISIBLE);    			
			
			
			if (mTabsAdapterProvider == null){
				mTabsAdapterProvider= new TabsAdapter(this, mTabHostProvider, viewPagerProvider);   	
				
				TabHost.TabSpec tabSpec3 = mTabHostProvider.newTabSpec("cloudProviderFragment");
				tabSpec3.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.tab_cloud_drive_explorer))); // new function to inject our own tab layout
		        //tabSpec.setContent(contentID);
		        //mTabHostContacts.addTab(tabSpec);
		        TabHost.TabSpec tabSpec4 = mTabHostProvider.newTabSpec("incomingProviderFragment");
		        tabSpec4.setIndicator(getTabIndicator(mTabHostProvider.getContext(), getString(R.string.tab_incoming_shares_explorer))); // new function to inject our own tab layout
		                	          				
				mTabsAdapterProvider.addTab(tabSpec3, CloudDriveProviderFragment.class, null);
				mTabsAdapterProvider.addTab(tabSpec4, IncomingSharesProviderFragment.class, null);
				
			}
			
			mTabHostProvider.setOnTabChangedListener(new OnTabChangeListener(){
	            @Override
	            public void onTabChanged(String tabId) {
	            	log("TabId :"+ tabId);
	                if(tabId.equals("cloudProviderFragment")){                     	
	
	     				tabShown=CLOUD_TAB;
	    				String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
	    				gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);
	    				cDriveExplorer = (CloudDriveProviderFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
	
	    				if(cDriveExplorer!=null){
	    					if(cDriveExplorer.getParentHandle()==-1|| cDriveExplorer.getParentHandle()==megaApi.getRootNode().getHandle()){
	    						changeTitle(getString(R.string.section_cloud_drive));
	    						changeBackVisibility(false);
	    					}
	    					else{
	    						changeTitle(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()).getName());
	    						changeBackVisibility(true);
	    					}    					
	    				}	
	                }
	                else if(tabId.equals("incomingProviderFragment")){                     	
	
	            		tabShown=INCOMING_TAB;
	            		
	            		String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
	            		gcFTag = getFragmentTag(R.id.explorer_tabs_pager, 1);
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
		if (cDriveExplorer != null){
			if (megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()) != null){
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()));
				cDriveExplorer.setNodes(nodes);
				cDriveExplorer.getListView().invalidateViews();
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
		Uri contentUri = FileProvider.getUriForFile(this, "nz.mega.android.providers.fileprovider", fileToShare);
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
}
