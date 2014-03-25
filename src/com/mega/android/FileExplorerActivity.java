package com.mega.android;

import java.util.ArrayList;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.NodeList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class FileExplorerActivity extends ActionBarActivity implements OnClickListener, MegaRequestListenerInterface{
	
	public static String ACTION_PROCESSED = "CreateLink.ACTION_PROCESSED";
	
	public static String ACTION_PICK_MOVE_FOLDER = "ACTION_PICK_MOVE_FOLDER";
	public static String ACTION_PICK_COPY_FOLDER = "ACTION_PICK_COPY_FOLDER";
	
	/*
	 * Select modes:
	 * UPLOAD - pick folder for upload
	 * MOVE - move files, folders
	 * CAMERA - pick folder for camera sync destination
	 */
	private enum Mode {
		UPLOAD, MOVE, COPY, CAMERA;
	}
	
	private Button uploadButton;
	private TextView windowTitle;
	private ImageButton newFolderButton;
	
	private FileExplorerFragment fe;
	
	private MegaApiAndroid megaApi;
	private Mode mode;
	
	private long[] moveFromHandles;
	private long[] copyFromHandles;
	
	private boolean folderSelected = false;
	
	private Handler handler;
	
	private static int EDIT_TEXT_ID = 2;
	
	private AlertDialog newFolderDialog;
	
	ProgressDialog statusDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		
		handler = new Handler();
		
		setContentView(R.layout.activity_file_explorer);
		
		fe = (FileExplorerFragment) getSupportFragmentManager().findFragmentById(R.id.file_explorer_fragment);
		
		mode = Mode.UPLOAD;
		Intent intent = getIntent();
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
		}
		
		uploadButton = (Button) findViewById(R.id.file_explorer_button);
		uploadButton.setOnClickListener(this);
		
		newFolderButton = (ImageButton) findViewById(R.id.file_explorer_new_folder);
		newFolderButton.setOnClickListener(this);
		
		windowTitle = (TextView) findViewById(R.id.file_explorer_window_title);
		String actionBarTitle = getString(R.string.manager_activity);
		windowTitle.setText(actionBarTitle);
		
		if (mode == Mode.MOVE) {
			uploadButton.setText(getString(R.string.general_move_to) + " " + actionBarTitle );
		}
		else if (mode == Mode.COPY){
			uploadButton.setText(getString(R.string.general_copy_to) + " " + actionBarTitle );
		}
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
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
	
	statusDialog = null;
	try {
		statusDialog = new ProgressDialog(this);
		statusDialog.setMessage(getString(R.string.context_creating_folder));
		statusDialog.show();
	}
	catch(Exception e){
		return;
	}
	
	long parentHandle = fe.getParentHandle();	
	MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
	
	if (parentNode == null){
		parentNode = megaApi.getRootNode();
	}
	
	megaApi.createFolder(title, parentNode, this);
	
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		if (request.getType() == MegaRequest.TYPE_MKDIR){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Folder created", Toast.LENGTH_LONG).show();
				NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fe.getParentHandle()));
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

}
