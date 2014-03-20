package com.mega.android;

import java.util.ArrayList;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class FileExplorerActivity extends ActionBarActivity implements OnClickListener{
	
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		megaApi = ((MegaApplication)getApplication()).getMegaApi();
		
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
				Toast.makeText(this, "CREAR CARPETA", Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
	
	public static void log(String log) {
		Util.log("FileExplorerActivity", log);
	}

}
