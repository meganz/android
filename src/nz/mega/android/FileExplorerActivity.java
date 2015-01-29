package nz.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


public class FileExplorerActivity extends PinActivity implements OnClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface{
	
	public static String ACTION_PROCESSED = "CreateLink.ACTION_PROCESSED";
	
	public static String ACTION_PICK_MOVE_FOLDER = "ACTION_PICK_MOVE_FOLDER";
	public static String ACTION_PICK_COPY_FOLDER = "ACTION_PICK_COPY_FOLDER";
	public static String ACTION_PICK_IMPORT_FOLDER = "ACTION_PICK_IMPORT_FOLDER";
	public static String ACTION_SELECT_FOLDER = "ACTION_SELECT_FOLDER";
	public static String ACTION_UPLOAD_SELFIE = "ACTION_UPLOAD_SELFIE";	
	/*
	 * Select modes:
	 * UPLOAD - pick folder for upload
	 * MOVE - move files, folders
	 * CAMERA - pick folder for camera sync destination
	 */
	
	public static int UPLOAD = 0;
	public static int MOVE = 1;
	public static int COPY = 2;
	public static int CAMERA = 3;
	public static int IMPORT = 4;
	public static int SELECT = 5;
	public static int UPLOAD_SELFIE = 6;
	

	private TextView windowTitle;
	private ImageButton newFolderButton;
	
	private MegaApiAndroid megaApi;
	private int mode;
	
	private long[] moveFromHandles;
	private long[] copyFromHandles;
	private String[] selectedContacts;
	private String imagePath;
	String actionBarTitle;
	private boolean folderSelected = false;
	
	private Handler handler;
	
	private CloudDriveExplorerFragment cDriveExplorer;
	private IncomingSharesExplorerFragment iSharesExplorer;
	
	private static int EDIT_TEXT_ID = 2;
	
	private AlertDialog newFolderDialog;
	
	ProgressDialog statusDialog;
	
	private List<ShareInfo> filePreparedInfos;
	
	private TabHost mTabHostExplorer;
	TabsAdapter mTabsAdapterExplorer;
    ViewPager viewPagerExplorer; 
	
	ArrayList<MegaNode> nodes;
	
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
		log("onCreate first");
		super.onCreate(savedInstanceState);
		
//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		if (dbH.getCredentials() == null){
			ManagerActivity.logout(this, (MegaApplication)getApplication(), megaApi, false);
			return;
		}
		
		if (savedInstanceState != null){
			folderSelected = savedInstanceState.getBoolean("folderSelected", false);
		}
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		
		megaApi.addGlobalListener(this);
		
		setContentView(R.layout.activity_file_explorer);
        		
		Intent intent = getIntent();
		if (megaApi.getRootNode() == null){
			//TODO Mando al login con un ACTION -> que loguee, haga el fetchnodes y vuelva aquí.
			Intent loginIntent = new Intent(this, LoginActivity.class);
			loginIntent.setAction(ManagerActivity.ACTION_FILE_EXPLORER_UPLOAD);
			loginIntent.putExtras(intent.getExtras());
			loginIntent.setData(intent.getData());
			startActivity(loginIntent);
			finish();
			return;
		}
		
		handler = new Handler();		
			
		if ((intent != null) && (intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_PICK_MOVE_FOLDER)){
				mode = MOVE;
				moveFromHandles = intent.getLongArrayExtra("MOVE_FROM");
				
				ArrayList<Long> list = new ArrayList<Long>(moveFromHandles.length);
				for (long n : moveFromHandles){
					list.add(n);
				}
				String cFTag = getFragmentTag(R.id.explorer_tabs_pager, 0);		
				cDriveExplorer = (CloudDriveExplorerFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
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
				cDriveExplorer = (CloudDriveExplorerFragment) getSupportFragmentManager().findFragmentByTag(cFTag);
				if(cDriveExplorer!=null){
					cDriveExplorer.setDisableNodes(list);
				}
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
		}
		
		mTabHostExplorer = (TabHost)findViewById(R.id.tabhost_explorer);
		mTabHostExplorer.setup();
        viewPagerExplorer = (ViewPager) findViewById(R.id.explorer_tabs_pager);  
        
        //Create tabs
        mTabHostExplorer.getTabWidget().setBackgroundColor(Color.BLACK);
		
        mTabHostExplorer.setVisibility(View.VISIBLE);    			
		
		
		if (mTabsAdapterExplorer == null){
			mTabsAdapterExplorer= new TabsAdapter(this, mTabHostExplorer, viewPagerExplorer);   	
			
			TabHost.TabSpec tabSpec3 = mTabHostExplorer.newTabSpec("incomingExplorerFragment");
			tabSpec3.setIndicator(getTabIndicator(mTabHostExplorer.getContext(), getString(R.string.tab_cloud_drive_explorer))); // new function to inject our own tab layout
	        //tabSpec.setContent(contentID);
	        //mTabHostContacts.addTab(tabSpec);
	        TabHost.TabSpec tabSpec4 = mTabHostExplorer.newTabSpec("outgoingExplorerFragment");
	        tabSpec4.setIndicator(getTabIndicator(mTabHostExplorer.getContext(), getString(R.string.tab_incoming_shares_explorer))); // new function to inject our own tab layout
	                	          				
	        Bundle b1 = new Bundle();
	        b1.putInt("MODE", mode);
			mTabsAdapterExplorer.addTab(tabSpec3, CloudDriveExplorerFragment.class, b1);
			mTabsAdapterExplorer.addTab(tabSpec4, IncomingSharesExplorerFragment.class, b1);
			
		}
		
		mTabHostExplorer.setOnTabChangedListener(new OnTabChangeListener(){
            @Override
            public void onTabChanged(String tabId) {
//            	log("TabId :"+ tabId);
//                if(tabId.equals("outgoingSharesFragment")){                         	
//        			if (outSF != null){                 				
//        				if(parentHandleOutgoing!=-1){
//            				MegaNode node = megaApi.getNodeByHandle(parentHandleOutgoing);
//        					aB.setTitle(node.getName());
//    					}
//        				else{
//        					aB.setTitle(getResources().getString(R.string.section_shared_items));
//        					outSF.refresh(); 
//        				}            					   				
//        			}
//                }
//                else if(tabId.equals("incomingSharesFragment")){                        	
//                	if (inSF != null){                        		
//                		if(parentHandleIncoming!=-1){
//                			
//                			MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
//        					aB.setTitle(node.getName());	
//    					}
//        				else{
//        					
//        					aB.setTitle(getResources().getString(R.string.section_shared_items));
//        					inSF.refresh(); 
//        				}   				
//        			}                           	
//                }
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
		
		newFolderButton = (ImageButton) findViewById(R.id.file_explorer_new_folder);
		newFolderButton.setOnClickListener(this);
		
		windowTitle = (TextView) findViewById(R.id.file_explorer_window_title);
		actionBarTitle = getString(R.string.section_cloud_drive);
		windowTitle.setText(actionBarTitle);

		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
	}
	
	private View getTabIndicator(Context context, String title) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);

        TextView tv = (TextView) view.findViewById(R.id.textView);
        tv.setText(title);
        return view;
    }
	
	public void changeTitle (String title){
		windowTitle.setText(title);
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
		bundle.putBoolean("folderSelected", folderSelected);
		super.onSaveInstanceState(bundle);
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
		
		String cFTag1 = getFragmentTag(R.id.explorer_tabs_pager, 0);	
		log("Tag: "+ cFTag1);
		cDriveExplorer = (CloudDriveExplorerFragment) getSupportFragmentManager().findFragmentByTag(cFTag1);	
		
		if(cDriveExplorer!=null){
			if (cDriveExplorer.onBackPressed() == 0){
				super.onBackPressed();
				return;
			}
		}
		
//		
//		if (cDriveExplorer != null){
//			if (cDriveExplorer.isVisible()){
//				
//			}
//		}
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
				Util.showErrorAlertDialog(getString(R.string.upload_can_not_open),
						true, this);
				return;
			}
			else {
				long parentHandle = cDriveExplorer.getParentHandle();
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				if(parentNode == null){
					parentNode = megaApi.getRootNode();
				}
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
				filePreparedInfos = null;
				finish();
			}	
		}
	}
	
	public void buttonClick(long handle){
		folderSelected = true;
		
		log("buttonClick"+ mode);
		
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
			log("----------------------------------------finish!");
			finish();
			
		}
		else if (mode == UPLOAD){
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
				onIntentProcessed();
			}
		}
		else if (mode == IMPORT){
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
	
	

	@Override
	public void onClick(View v) {
		switch(v.getId()){/*
			case R.id.file_explorer_button:{
				log("button clicked!");
				folderSelected = true;
				
				if (mode == Mode.MOVE) {
					long parentHandle = cDriveExplorer.getParentHandle();
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
				else if (mode == Mode.COPY){
					
					long parentHandle = cDriveExplorer.getParentHandle();
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
				else if(mode == Mode.UPLOAD_SELFIE){
				
					long parentHandle = cDriveExplorer.getParentHandle();
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
					log("----------------------------------------finish!");
					finish();
					
				}
				else if (mode == Mode.UPLOAD){
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
						onIntentProcessed();
					}
				}
				else if (mode == Mode.IMPORT){
					long parentHandle = cDriveExplorer.getParentHandle();
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
				else if (mode == Mode.SELECT){

					long parentHandle = cDriveExplorer.getParentHandle();
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
				break;
			}*/
			case R.id.file_explorer_new_folder:{
				showNewFolderDialog(null);
				break;
			}
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

	private void createFolder(String title) {
	
	if (!Util.isOnline(this)){
		Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
		return;
	}
	
	if(isFinishing()){
		return;	
	}
	
	long parentHandle = cDriveExplorer.getParentHandle();	
	MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
	
	if (parentNode == null){
		parentNode = megaApi.getRootNode();
	}
	
	
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
		Toast.makeText(this, getString(R.string.context_folder_already_exists), Toast.LENGTH_LONG).show();
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
	
	public static void log(String log) {
		Util.log("FileExplorerActivity", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, getString(R.string.context_folder_created), Toast.LENGTH_LONG).show();
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()));
				cDriveExplorer.setNodes(nodes);
				cDriveExplorer.getListView().invalidateViews();
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
			nodes = megaApi.getChildren(megaApi.getNodeByHandle(cDriveExplorer.getParentHandle()));
			cDriveExplorer.setNodes(nodes);
			cDriveExplorer.getListView().invalidateViews();
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}

}
