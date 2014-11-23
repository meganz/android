package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
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
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaUser;

public class FileExplorerActivity extends PinActivity implements OnClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface{
	
	public static String ACTION_PROCESSED = "CreateLink.ACTION_PROCESSED";
	
	public static String ACTION_PICK_MOVE_FOLDER = "ACTION_PICK_MOVE_FOLDER";
	public static String ACTION_PICK_COPY_FOLDER = "ACTION_PICK_COPY_FOLDER";
	public static String ACTION_PICK_IMPORT_FOLDER = "ACTION_PICK_IMPORT_FOLDER";
	public static String ACTION_SELECT_FOLDER = "ACTION_SELECT_FOLDER";
		
	/*
	 * Select modes:
	 * UPLOAD - pick folder for upload
	 * MOVE - move files, folders
	 * CAMERA - pick folder for camera sync destination
	 */
	private enum Mode {
		UPLOAD, MOVE, COPY, CAMERA, IMPORT, SELECT;
	}
	
	private Button uploadButton;
	private TextView windowTitle;
	private ImageButton newFolderButton;
	
	private FileExplorerFragment fe;
	
	private MegaApiAndroid megaApi;
	private Mode mode;
	
	private long[] moveFromHandles;
	private long[] copyFromHandles;
	private String[] selectedContacts;
	
	private boolean folderSelected = false;
	
	private Handler handler;
	
	private static int EDIT_TEXT_ID = 2;
	
	private AlertDialog newFolderDialog;
	
	ProgressDialog statusDialog;
	
	private List<ShareInfo> filePreparedInfos;
	
	ArrayList<MegaNode> nodes;
	RelativeLayout menuOverflowLayout;
	
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
		
		setContentView(R.layout.activity_file_explorer);
		
		fe = (FileExplorerFragment) getSupportFragmentManager().findFragmentById(R.id.file_explorer_fragment);
		
		mode = Mode.UPLOAD;
		
		if ((intent != null) && (intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_PICK_MOVE_FOLDER)){
				mode = Mode.MOVE;
				moveFromHandles = intent.getLongArrayExtra("MOVE_FROM");
				
				ArrayList<Long> list = new ArrayList<Long>(moveFromHandles.length);
				for (long n : moveFromHandles){
					list.add(n);
				}
				fe.setDisableNodes(list);
			}
			else if (intent.getAction().equals(ACTION_PICK_COPY_FOLDER)){
				mode = Mode.COPY;
				copyFromHandles = intent.getLongArrayExtra("COPY_FROM");
				
				ArrayList<Long> list = new ArrayList<Long>(copyFromHandles.length);
				for (long n : copyFromHandles){
					list.add(n);
				}
				fe.setDisableNodes(list);
			}
			else if (intent.getAction().equals(ACTION_PICK_IMPORT_FOLDER)){
				mode = Mode.IMPORT;
			}
			else if (intent.getAction().equals(ACTION_SELECT_FOLDER)){
				mode = Mode.SELECT;
				selectedContacts=intent.getStringArrayExtra("SELECTED_CONTACTS");			
				
			}
		}
		
		uploadButton = (Button) findViewById(R.id.file_explorer_button);
		uploadButton.setOnClickListener(this);
		
		menuOverflowLayout = (RelativeLayout) findViewById(R.id.file_browser_overflow_menu);
		menuOverflowLayout.setVisibility(View.GONE);
		
		newFolderButton = (ImageButton) findViewById(R.id.file_explorer_new_folder);
		newFolderButton.setOnClickListener(this);
		
		windowTitle = (TextView) findViewById(R.id.file_explorer_window_title);
		String actionBarTitle = getString(R.string.main_folder);
		windowTitle.setText(actionBarTitle);
		
		if (mode == Mode.MOVE) {
			uploadButton.setText(getString(R.string.general_move_to) + " " + actionBarTitle );
		}
		else if (mode == Mode.COPY){
			uploadButton.setText(getString(R.string.general_copy_to) + " " + actionBarTitle );
		}
		else if (mode == Mode.UPLOAD){
			uploadButton.setText(getString(R.string.action_upload));
		}
		else if (mode == Mode.IMPORT){
			uploadButton.setText(getString(R.string.general_import_to) + " " + actionBarTitle );
		}
		else if (mode == Mode.SELECT){
			uploadButton.setText(getString(R.string.general_select) + " " + actionBarTitle );
		}
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
	}
	
	public void changeNavigationTitle(String folder){
		
		windowTitle.setText(folder);
		
		if (mode == Mode.MOVE) {
			uploadButton.setText(getString(R.string.general_move_to) + " " + folder);
		}
		else if (mode == Mode.COPY){
			uploadButton.setText(getString(R.string.general_copy_to) + " " + folder);
		}
		else if (mode == Mode.UPLOAD){
			uploadButton.setText(getString(R.string.action_upload));
		}
		else if (mode == Mode.IMPORT){
			uploadButton.setText(getString(R.string.general_import_to) + " " + folder);
		}
		else if (mode == Mode.SELECT){
			uploadButton.setText(getString(R.string.general_select) + " " + folder);
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
		if (getIntent() != null){
			if (mode == Mode.UPLOAD) {
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
		if (fe != null){
			if (fe.isVisible()){
				if (fe.onBackPressed() == 0){
					super.onBackPressed();
					return;
				}
			}
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
				Util.showErrorAlertDialog(getString(R.string.upload_can_not_open),
						true, this);
				return;
			}
			else {
				long parentHandle = fe.getParentHandle();
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

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.file_explorer_button:{
				log("button clicked!");
				folderSelected = true;
				
				if (mode == Mode.MOVE) {
					long parentHandle = fe.getParentHandle();
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
					
					long parentHandle = fe.getParentHandle();
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
					long parentHandle = fe.getParentHandle();
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

					long parentHandle = fe.getParentHandle();
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
			}
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
	
	long parentHandle = fe.getParentHandle();	
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
		Toast.makeText(this, "Folder already exists", Toast.LENGTH_LONG).show();
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
				Toast.makeText(this, "Folder created", Toast.LENGTH_LONG).show();
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(fe.getParentHandle()));
				fe.setNodes(nodes);
				fe.getListView().invalidateViews();
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
		if (fe != null){
			nodes = megaApi.getChildren(megaApi.getNodeByHandle(fe.getParentHandle()));
			fe.setNodes(nodes);
			fe.getListView().invalidateViews();
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}

}
