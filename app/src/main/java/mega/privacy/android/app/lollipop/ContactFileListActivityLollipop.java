package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.modalbottomsheet.ContactFileListBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
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

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class ContactFileListActivityLollipop extends DownloadableActivity implements MegaGlobalListenerInterface, MegaRequestListenerInterface, ContactFileListBottomSheetDialogFragment.CustomHeight {

	FrameLayout fragmentContainer;
    
    String userEmail;
	MegaUser contact;
	String fullName = "";

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	AlertDialog permissionsDialog;

	ContactFileListFragmentLollipop cflF;

	NodeController nC;
	private android.support.v7.app.AlertDialog downloadConfirmationDialog;

	CoordinatorLayout coordinatorLayout;
	Handler handler;

	MenuItem shareMenuItem;
	MenuItem viewSharedItem;

	boolean moveToRubbish=false;

	public static int REQUEST_CODE_GET = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_GET_LOCAL = 1003;
	public static int REQUEST_CODE_SELECT_FOLDER = 1008;

	static ContactFileListActivityLollipop contactPropertiesMainActivity;

	long parentHandle = -1;

	DatabaseHandler dbH;
	MegaPreferences prefs;

	MenuItem createFolderMenuItem;
	MenuItem startConversation;
	private AlertDialog newFolderDialog;
	DisplayMetrics outMetrics;

	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	private android.support.v7.app.AlertDialog renameDialog;
	ProgressDialog statusDialog;

	long lastTimeOnTransferUpdate = -1;

	private List<ShareInfo> filePreparedInfos;

	MegaNode selectedNode = null;

	Toolbar tB;
	ActionBar aB;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		logDebug("onCreateOptionsMenuLollipop");

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.file_explorer_action, menu);

		menu.findItem(R.id.cab_menu_sort).setVisible(false);
		menu.findItem(R.id.cab_menu_grid_list).setVisible(false);
		createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder);
		startConversation = menu.findItem(R.id.cab_menu_new_chat);
		startConversation.setVisible(false);

		if (cflF != null && cflF.isVisible()){
			if(cflF.getFabVisibility()==View.VISIBLE){
				createFolderMenuItem.setVisible(true);
			}
			else{
				createFolderMenuItem.setVisible(false);
			}
		}
		else{
			createFolderMenuItem.setVisible(false);
		}
		return super.onCreateOptionsMenu(menu);
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
		}
		return true;
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

		final RelativeLayout error_layout = new RelativeLayout(ContactFileListActivityLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(ContactFileListActivityLollipop.this);
		error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout.addView(error_icon);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();


		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon.setLayoutParams(params_icon);

		error_icon.setColorFilter(ContextCompat.getColor(ContactFileListActivityLollipop.this, R.color.login_warning));

		final TextView textError = new TextView(ContactFileListActivityLollipop.this);
		error_layout.addView(textError);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(ContactFileListActivityLollipop.this, R.color.login_warning));

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
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(contactPropertiesMainActivity, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(contactPropertiesMainActivity, R.color.text_secondary));
		input.setHint(getString(R.string.context_new_folder_name));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(contactPropertiesMainActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_string));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();
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
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(contactPropertiesMainActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError.setText(getString(R.string.invalid_string));
					error_layout.setVisibility(View.VISIBLE);
					input.requestFocus();
				}
				else{
					createFolder(value);
					newFolderDialog.dismiss();
				}
			}
		});
	}

	private void createFolder(String title) {

		logDebug("createFolder");
		if (!isOnline(this)) {
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
			return;
		}

		if(isFinishing()){
			return;
		}

		long parentHandle = cflF.getParentHandle();

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
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_already_exists));
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
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_already_exists));
				}
			}
			else{
				return;
			}
		}
	}

	private void showKeyboardDelayed(final View view) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int position;
			int adapterType;
			int actionType;
			ImageView imageDrag = null;

			if (intent != null) {
				position = intent.getIntExtra("position", -1);
				adapterType = intent.getIntExtra("adapterType", 0);
				actionType = intent.getIntExtra("actionType", -1);

				if (position != -1) {
					if (adapterType == CONTACT_FILE_ADAPTER) {
						if (cflF != null && cflF.isAdded()) {
							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = cflF.getImageDrag(position);
								if (cflF.imageDrag != null) {
									cflF.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null) {
									cflF.imageDrag = imageDrag;
									cflF.imageDrag.setVisibility(View.GONE);
								}
							} else if (actionType == SCROLL_TO_POSITION) {
								cflF.updateScrollPosition(position);
							}
						}
					}
				}

				if (imageDrag != null){
					int[] positionDrag = new int[2];
					int[] screenPosition = new int[4];
					imageDrag.getLocationOnScreen(positionDrag);

					screenPosition[0] = (imageDrag.getWidth() / 2) + positionDrag[0];
					screenPosition[1] = (imageDrag.getHeight() / 2) + positionDrag[1];
					screenPosition[2] = imageDrag.getWidth();
					screenPosition[3] = imageDrag.getHeight();

					Intent intent1 =  new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG);
					intent1.putExtra("screenPosition", screenPosition);
					LocalBroadcastManager.getInstance(contactPropertiesMainActivity).sendBroadcast(intent1);
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		logDebug("onCreate first");
		super.onCreate(savedInstanceState);

		getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_search));

		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		if(megaApi==null||megaApi.getRootNode()==null){
			logDebug("Refresh session - sdk");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		if(isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
			}

			if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
				logDebug("Refresh session - karere");
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return;
			}
		}

		megaApi.addGlobalListener(this);

		contactPropertiesMainActivity=this;

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION));

		handler = new Handler();
		dbH = DatabaseHandler.getDbHandler(this);

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = getResources().getDisplayMetrics().density;

		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);

		Bundle extras = getIntent().getExtras();
		if (extras != null){
			userEmail = extras.getString("name");
            int currNodePosition = extras.getInt("node_position", -1);
			
			setContentView(R.layout.activity_main_contact_properties);

			coordinatorLayout = (CoordinatorLayout) findViewById(R.id.contact_properties_main_activity_layout);
			coordinatorLayout.setFitsSystemWindows(false);

			//Set toolbar
			tB = (Toolbar) findViewById(R.id.toolbar_main_contact_properties);
			if(tB==null){
				logWarning("Toolbar is NULL");
			}

			setSupportActionBar(tB);
			aB = getSupportActionBar();

			contact = megaApi.getContact(userEmail);
			if(contact == null)
			{
				finish();
			}

			ContactController cC = new ContactController(this);
			fullName =  cC.getContactFullName(contact.getHandle());

			if(aB!=null){
				aB.setDisplayHomeAsUpEnabled(true);
				aB.setDisplayShowHomeEnabled(true);
				setTitleActionBar(null);
			}
			else{
				logWarning("aB is NULL!!!!");
			}

			fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_contact_properties);

			logDebug("Shared Folders are:");
			coordinatorLayout.setFitsSystemWindows(true);

			if (cflF == null){
				cflF = new ContactFileListFragmentLollipop();
			}
			cflF.setUserEmail(userEmail);
			cflF.setCurrNodePosition(currNodePosition);

			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_contact_properties, cflF, "cflF").commitNow();
			coordinatorLayout.invalidate();
		}
	}

	public void showUploadPanel() {
		logDebug("showUploadPanel");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						REQUEST_WRITE_STORAGE);
			}
		}

		UploadBottomSheetDialogFragment bottomSheetDialogFragment = new UploadBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	@Override
	protected void onResume() {
		logDebug("onResume");
		super.onResume();

		Intent intent = getIntent(); 

		if (intent != null) { 
			if (intent.getAction() != null){ 
//				if(getIntent().getAction().equals(ManagerActivityLollipop.ACTION_EXPLORE_ZIP)){  
//
//					String pathZip=intent.getExtras().getString(ManagerActivityLollipop.EXTRA_PATH_ZIP);    				
//
//					log("Path: "+pathZip);
//
//					//Lanzar nueva activity ZipBrowserActivity
//
//					Intent intentZip = new Intent(this, ZipBrowserActivityLollipop.class);    				
//					intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, pathZip);
//					startActivity(intentZip);
//
//
//				}
//				else if(getIntent().getAction().equals(ManagerActivityLollipop.ACTION_OPEN_PDF)){ 
//					String pathPdf=intent.getExtras().getString(ManagerActivityLollipop.EXTRA_PATH_PDF);
//
//					File pdfFile = new File(pathPdf);
//
//					Intent intentPdf = new Intent();
//					intentPdf.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
//					intentPdf.setClass(this, OpenPDFActivity.class);
//					intentPdf.setAction("android.intent.action.VIEW");
//					this.startActivity(intentPdf);
//				}
			}
			intent.setAction(null);
			setIntent(null);
		}
	}

	@Override
	protected void onNewIntent(Intent intent){
		logDebug("onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent); 
	}

//	public void selectContactFragment(int currentFragment){
//		log("selectContactFragment: "+currentFragment);
//		switch(currentFragment){
//			case CONTACT_PROPERTIES:{
//				if (cpF == null){
//					cpF = new ContactPropertiesFragmentLollipop();
//				}
//				cpF.setUserEmail(userEmail);
//				coordinatorLayout.setFitsSystemWindows(false);
//				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_contact_properties, cpF, "cpF").commit();
//
//				break;
//			}
//			case CONTACT_FILE_LIST:{
//
//				break;
//			}
//		}
//	}

	public void showConfirmationLeaveIncomingShare (final MegaNode n){
		logDebug("showConfirmationLeaveIncomingShare");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						//TODO remove the incoming shares
						megaApi.remove(n);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationLeaveIncomingShare (final ArrayList<Long> handleList){
		logDebug("showConfirmationLeaveIncomingShare");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						//TODO remove the incoming shares
						contactPropertiesMainActivity.leaveMultipleShares(handleList);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}
	
	public void leaveMultipleShares (ArrayList<Long> handleList){
		
		for (int i=0; i<handleList.size(); i++){
			MegaNode node = megaApi.getNodeByHandle(handleList.get(i));
			megaApi.remove(node);
		}
	}
	
	public String getDescription(ArrayList<MegaNode> nodes){
		int numFolders = 0;
		int numFiles = 0;

		for (int i=0;i<nodes.size();i++){
			MegaNode c = nodes.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}

		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			if (numFiles == 0){
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			}
			else{
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}

		return info;
	}

	@Override
	protected void onDestroy(){
		logDebug("onDestroy()");

		super.onDestroy();    	    	

		if(megaApi != null)
		{
			megaApi.removeGlobalListener(this);	
			megaApi.removeRequestListener(this);
		}

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		logDebug("onPrepareOptionsMenu----------------------------------");

		if(cflF!=null){
			if(cflF.isVisible()){
				logDebug("visible ContacFileListProperties");
				if(shareMenuItem!=null){
					shareMenuItem.setVisible(true);
					viewSharedItem.setVisible(false);
				}
			}
		}

		super.onPrepareOptionsMenu(menu);
		return true;

	}

	public void setParentHandle(long parentHandle) {
		this.parentHandle = parentHandle;
	}

	@SuppressLint("NewApi")
	public void onFileClick(ArrayList<Long> handleList) {

		if(nC==null){
			nC = new NodeController(this);
		}
		nC.prepareForDownload(handleList, true);
	}

	public void moveToTrash(final ArrayList<Long> handleList){
		logDebug("moveToTrash: ");
		moveToRubbish=true;
		if (!isOnline(this)) {
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
			return;
		}

		MultipleRequestListener moveMultipleListener = null;
		MegaNode parent;
		//Check if the node is not yet in the rubbish bin (if so, remove it)
		if(handleList!=null){
			if(handleList.size()>1){
				logDebug("MOVE multiple: " + handleList.size());
				moveMultipleListener = new MultipleRequestListener(MULTIPLE_SEND_RUBBISH, this);
				for (int i=0;i<handleList.size();i++){
					megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)), megaApi.getRubbishNode(), moveMultipleListener);
				}
			}
			else{
				logDebug("MOVE single");
				megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(0)), megaApi.getRubbishNode(), this);

			}
		}
		else{
			logWarning("handleList NULL");
			return;
		}
	}

	public void showRenameDialog(final MegaNode document, String text){
		logDebug("showRenameDialog");

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);
//	    layout.setLayoutParams(params);

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, document.isFolder());
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
					if (document.isFolder()){
						input.setSelection(0, input.getText().length());
					}
					else{
						String [] s = document.getName().split("\\.");
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

		final RelativeLayout error_layout = new RelativeLayout(ContactFileListActivityLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(ContactFileListActivityLollipop.this);
		error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout.addView(error_icon);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();


		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon.setLayoutParams(params_icon);

		error_icon.setColorFilter(ContextCompat.getColor(ContactFileListActivityLollipop.this, R.color.login_warning));

		final TextView textError = new TextView(ContactFileListActivityLollipop.this);
		error_layout.addView(textError);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(ContactFileListActivityLollipop.this, R.color.login_warning));

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
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(contactPropertiesMainActivity, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
										  KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					logDebug("actionId is IME_ACTION_DONE");
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(contactPropertiesMainActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_string));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();
					}
					else{
						rename(document, value);
						renameDialog.dismiss();
					}
					return true;
				}
				return false;
			}
		});

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.context_rename) + " "	+ new String(document.getName()));
		builder.setPositiveButton(getString(R.string.context_rename),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						rename(document, value);
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
		renameDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String value = input.getText().toString().trim();
				if (value.length() == 0) {
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(contactPropertiesMainActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError.setText(getString(R.string.invalid_string));
					error_layout.setVisibility(View.VISIBLE);
					input.requestFocus();
				}
				else{
					rename(document, value);
					renameDialog.dismiss();
				}
			}
		});
	}

	private void rename(MegaNode document, String newName) {
		if (newName.equals(document.getName())) {
			return;
		}

		if (!isOnline(this)) {
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
			return;
		}

		if (isFinishing()) {
			return;
		}

		ProgressDialog temp = null;
		try {
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_renaming));
			temp.show();
		} catch (Exception e) {
			return;
		}
		statusDialog = temp;

		logDebug("Renaming " + document.getName() + " to " + newName);

		megaApi.renameNode(document, newName, this);
	}

	public void showMoveLollipop(ArrayList<Long> handleList){
		moveToRubbish=false;
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}

	public void showCopyLollipop(ArrayList<Long> handleList) {

		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i = 0; i < handleList.size(); i++) {
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (intent == null) {
			return;
		}

		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER	&& resultCode == RESULT_OK) {
			logDebug("Local folder selected");
			String parentPath = intent
					.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            dbH.setStorageDownloadLocation(parentPath);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			logDebug("URL: " + url + "___SIZE: " + size);

			if(nC==null){
				nC = new NodeController(this);
			}
			nC.checkSizeBeforeDownload(parentPath,url, size, hashes, false);
        } else if (requestCode == REQUEST_CODE_TREE) {
            onRequestSDCardWritePermission(intent, resultCode, nC);
        }
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER	&& resultCode == RESULT_OK) {
			if (!isOnline(this)) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			ProgressDialog temp = null;
			try {
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_copying));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;

			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;

			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for (int i = 0; i < copyHandles.length; i++) {
				logDebug("NODE TO COPY: " + megaApi.getNodeByHandle(copyHandles[i]).getName());
				logDebug("WHERE: " + parent.getName());
				logDebug("NODES: " + copyHandles[i] + "_" + parent.getHandle());
				MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
				if (cN != null){
					logDebug("cN != null");
					megaApi.copyNode(cN, parent, this);
				}
				else{
					logWarning("cN == null");
					try {
						statusDialog.dismiss();
						if(cflF!=null && cflF.isVisible()){
							showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_sent_node));
						}
					} catch (Exception ex) {
					}
				}
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {

			if (!isOnline(this)) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}
			
			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);
			moveToRubbish=false;
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_moving));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
			
			for(int i=0; i<moveHandles.length;i++){
				megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, this);
			}
		}		
		else if (requestCode == REQUEST_CODE_GET && resultCode == RESULT_OK) {
			Uri uri = intent.getData();
			intent.setAction(Intent.ACTION_GET_CONTENT);
			FilePrepareTask filePrepareTask = new FilePrepareTask(this);
			filePrepareTask.execute(intent);
			ProgressDialog temp = null;
			try {
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.upload_prepare));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;
		} 
		else if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {

			if (!isOnline(this)) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			final ArrayList<String> selectedContacts = intent.getStringArrayListExtra("SELECTED_CONTACTS");
			final long folderHandle = intent.getLongExtra("SELECT", 0);			

			final MegaNode parent = megaApi.getNodeByHandle(folderHandle);

			if (parent.isFolder()){
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleAddContacts);
				dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {

						ProgressDialog temp = null;
						try{
							temp = new ProgressDialog(contactPropertiesMainActivity);
							temp.setMessage(getString(R.string.context_sharing_folder));
							temp.show();
						}
						catch(Exception e){
							return;
						}
						statusDialog = temp;
						permissionsDialog.dismiss();

						logDebug("item "+item);

						switch(item) {
						case 0:{
							for (int i=0;i<selectedContacts.size();i++){
								MegaUser user= megaApi.getContact(selectedContacts.get(i));
								logDebug("user: " + user);
								logDebug("useremail: " + userEmail);
								logDebug("parentNode: " + parent.getName() + "_" + parent.getHandle());
								megaApi.share(parent, user, MegaShare.ACCESS_READ,contactPropertiesMainActivity);
							}
							break;
						}
						case 1:{	                    	
							for (int i=0;i<selectedContacts.size();i++){
								MegaUser user= megaApi.getContact(selectedContacts.get(i));
								megaApi.share(parent, user, MegaShare.ACCESS_READWRITE,contactPropertiesMainActivity);
							}
							break;
						}
						case 2:{                   	
							for (int i=0;i<selectedContacts.size();i++){
								MegaUser user= megaApi.getContact(selectedContacts.get(i));
								megaApi.share(parent, user, MegaShare.ACCESS_FULL,contactPropertiesMainActivity);
							}
							break;
						}
						}
					}
				});
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
				Resources resources = permissionsDialog.getContext().getResources();
				int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
				TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
				alertTitle.setTextColor(ContextCompat.getColor(this, R.color.black));
			}
		}		

		else if (requestCode == REQUEST_CODE_GET_LOCAL && resultCode == RESULT_OK) {

			String folderPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			ArrayList<String> paths = intent.getStringArrayListExtra(FileStorageActivityLollipop.EXTRA_FILES);

			int i = 0;

			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode == null) {
				parentNode = megaApi.getRootNode();
			}

			for (String path : paths) {
				Intent uploadServiceIntent = new Intent(this, UploadService.class);
				File file = new File(path);
				if (file.isDirectory()) {
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, file.getName());
					logDebug("FOLDER: EXTRA_FILEPATH: " + file.getAbsolutePath());
					logDebug("FOLDER: EXTRA_NAME: " + file.getName());
				} else {
					ShareInfo info = ShareInfo.infoFromFile(file);
					if (info == null) {
						continue;
					}
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
					uploadServiceIntent.putExtra(UploadService.EXTRA_SIZE, info.getSize());

					logDebug("FILE: EXTRA_FILEPATH: " + info.getFileAbsolutePath());
					logDebug("FILE: EXTRA_NAME: " + info.getTitle());
					logDebug("FILE: EXTRA_SIZE: " + info.getSize());
				}

				uploadServiceIntent.putExtra(UploadService.EXTRA_FOLDERPATH, folderPath);
				uploadServiceIntent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				logDebug("PARENTNODE: " + parentNode.getHandle() + "___" + parentNode.getName());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.upload_began));
				startService(uploadServiceIntent);
				i++;
			}
		}
	}

	public void onIntentProcessed(List<ShareInfo> infos) {
		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			} catch (Exception ex) {
			}
		}

		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
		if (parentNode == null) {
			showErrorAlertDialog(
					getString(R.string.error_temporary_unavaible), false, this);
			return;
		}

		if (infos == null) {
			showErrorAlertDialog(getString(R.string.upload_can_not_open),
					false, this);
		} else {
			showSnackbar(SNACKBAR_TYPE, getString(R.string.upload_began));
			for (ShareInfo info : infos) {
				Intent intent = new Intent(this, UploadService.class);
				intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
				intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
				intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
				startService(intent);
			}
		}
	}

	@Override
	public void onBackPressed() {
		retryConnectionsAndSignalPresence();

		if (cflF != null && cflF.isVisible() && cflF.onBackPressed() == 0){
			super.onBackPressed();
		}
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
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		if (cflF != null){
			if (cflF.isVisible()){
				cflF.setNodes(parentHandle);
			}
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_MOVE) {
			logDebug("Move request start");
		} 
		else if (request.getType() == MegaRequest.TYPE_REMOVE) {
			logDebug("Remove request start");
		} 
		else if (request.getType() == MegaRequest.TYPE_EXPORT) {
			logDebug("Export request start");
		} 
		else if (request.getType() == MegaRequest.TYPE_RENAME) {
			logDebug("Rename request start");
		} 
		else if (request.getType() == MegaRequest.TYPE_COPY) {
			logDebug("Copy request start");
		}
		else if (request.getType() == MegaRequest.TYPE_SHARE) {
			logDebug("Share request start");
		}
	}

	public void askConfirmationMoveToRubbish(final ArrayList<Long> handleList){
		logDebug("askConfirmationMoveToRubbish");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						moveToTrash(handleList);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		if(handleList!=null){

			if (handleList.size() > 0){
				android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//				builder.setTitle(getResources().getString(R.string.section_rubbish_bin));
				if (handleList.size() > 1){
					builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish_plural));
				}
				else{
					builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish));
				}
				builder.setPositiveButton(R.string.general_move, dialogClickListener);
				builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
				builder.show();
			}
		}
		else{
			logWarning("handleList NULL");
			return;
		}
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestUpdate");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish");

		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				if(cflF!=null && cflF.isVisible()){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_created));
					cflF.setNodes();
				}
			}
			else{
				if(cflF!=null && cflF.isVisible()){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_no_created));
					cflF.setNodes();
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){

			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				if(cflF!=null && cflF.isVisible()){
					cflF.clearSelections();
					cflF.hideMultipleSelect();
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_renamed));
				}
			}
			else{
				if(cflF!=null && cflF.isVisible()){
					cflF.clearSelections();
					cflF.hideMultipleSelect();
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_renamed));
				}
			}
			logDebug("Rename nodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY) {
			try {
				statusDialog.dismiss();
			} catch (Exception ex) {
			}

			if (e.getErrorCode() == MegaError.API_OK){
				if(cflF!=null && cflF.isVisible()){
					cflF.clearSelections();
					cflF.hideMultipleSelect();
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied));
				}
			}
			else{
				if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(ACTION_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();
				}
				else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
					logWarning("PRE OVERQUOTA ERROR: " + e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();
				}
				else{
					if(cflF!=null && cflF.isVisible()){
						cflF.clearSelections();
						cflF.hideMultipleSelect();
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
					}
				}
			}

			logDebug("Copy nodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}

			if(moveToRubbish){
				logDebug("Finish move to Rubbish!");
				if (e.getErrorCode() == MegaError.API_OK){
					if(cflF!=null && cflF.isVisible()){
						cflF.clearSelections();
						cflF.hideMultipleSelect();
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved_to_rubbish));
					}
				}
				else{
					if(cflF!=null && cflF.isVisible()){
						cflF.clearSelections();
						cflF.hideMultipleSelect();
					}
				}
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					if(cflF!=null && cflF.isVisible()){
						cflF.clearSelections();
						cflF.hideMultipleSelect();
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved));
					}
				}
				else{
					if(cflF!=null && cflF.isVisible()){
						cflF.clearSelections();
						cflF.hideMultipleSelect();
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved));
					}
				}
			}
			moveToRubbish=false;
			logDebug("Move request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_SHARE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				cflF.clearSelections();
				cflF.hideMultipleSelect();
				logDebug("Shared folder correctly: " + request.getNodeHandle());
				Toast.makeText(this, getString(R.string.context_correctly_shared), Toast.LENGTH_SHORT).show();
			}
			else{
				cflF.clearSelections();
				cflF.hideMultipleSelect();
				Toast.makeText(this, getString(R.string.context_no_shared), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logDebug("onRequestTemporaryError");
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

	public void showOptionsPanel(MegaNode node){
		logDebug("showOptionsPanel");
		if(node!=null){
			this.selectedNode = node;
			ContactFileListBottomSheetDialogFragment bottomSheetDialogFragment = new ContactFileListBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void showSnackbar(int type, String s){
		CoordinatorLayout coordinatorFragment = (CoordinatorLayout) findViewById(R.id.contact_file_list_coordinator_layout);
		cflF = (ContactFileListFragmentLollipop) getSupportFragmentManager().findFragmentByTag("cflF");
		if(cflF!=null && cflF.isVisible()){
			if(coordinatorFragment!=null){
				showSnackbar(type, coordinatorFragment, s);
			}
			else{
				showSnackbar(type, fragmentContainer, s);
			}
		}
	}

	public MegaNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(MegaNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public boolean isEmptyParentHandleStack() {
		if(cflF!=null){
			return cflF.isEmptyParentHandleStack();
		}
		logDebug("Fragment NULL");
		return true;
	}

	public void setTitleActionBar(String title){
		if (aB != null){
			if(title == null){
				logDebug("Reset title and subtitle");
				aB.setTitle(R.string.title_incoming_shares_with_explorer);
				aB.setSubtitle(fullName);

			}
			else{
				aB.setTitle(title);
				aB.setSubtitle(null);
			}
		}
	}

	public long getParentHandle() {

		if(cflF!=null){
			return cflF.getParentHandle();
		}
		return -1;
	}

	public void refreshAfterMovingToRubbish(){
		if(cflF!=null && cflF.isVisible()){
			cflF.clearSelections();
			cflF.hideMultipleSelect();
		}
	}

	@Override
	public int getHeightToPanel(BottomSheetDialogFragment dialog) {
			if(dialog instanceof ContactFileListBottomSheetDialogFragment){
				if(fragmentContainer != null && aB != null){
					final Rect r = new Rect();
					fragmentContainer.getWindowVisibleDisplayFrame(r);
					return (r.height() - aB.getHeight());
				}
			}
		return -1;
	}

	public void openAdvancedDevices (long handleToDownload, boolean highPriority){
		logDebug("handleToDownload: " + handleToDownload + ", highPriority: " + highPriority);
		String externalPath = getExternalCardPath();

		if(externalPath!=null){
			logDebug("ExternalPath for advancedDevices: " + externalPath);
			MegaNode node = megaApi.getNodeByHandle(handleToDownload);
			if(node!=null){

				File newFile =  new File(node.getName());
				logDebug("File: " + newFile.getPath());
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

				// Filter to only show results that can be "opened", such as
				// a file (as opposed to a list of contacts or timezones).
				intent.addCategory(Intent.CATEGORY_OPENABLE);

				// Create a file with the requested MIME type.
				String mimeType = MimeTypeList.getMimeType(newFile);
				logDebug("Mimetype: " + mimeType);
				intent.setType(mimeType);
				intent.putExtra(Intent.EXTRA_TITLE, node.getName());
				intent.putExtra("handleToDownload", handleToDownload);
				intent.putExtra(HIGH_PRIORITY_TRANSFER, highPriority);
				try{
					startActivityForResult(intent, WRITE_SD_CARD_REQUEST_CODE);
				}
				catch(Exception e){
					logError("Exception in External SDCARD", e);
					Environment.getExternalStorageDirectory();
					Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
					toast.show();
				}
			}
		}
		else{
			logWarning("No external SD card");
			Environment.getExternalStorageDirectory();
			Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
			toast.show();
		}
	}

	public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes, final boolean highPriority){
        logDebug("askSizeConfirmationBeforeDownload");

		final String parentPathC = parentPath;
		final String urlC = url;
		final long [] hashesC = hashes;
		final long sizeC=size;

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

		builder.setMessage(getString(R.string.alert_larger_file, getSizeString(sizeC)));
		builder.setPositiveButton(getString(R.string.general_save_to_device),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskSizeDownload("false");
						}
						if(nC==null){
							nC = new NodeController(contactPropertiesMainActivity);
						}
						nC.checkInstalledAppBeforeDownload(parentPathC,urlC, sizeC, hashesC, highPriority);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if(dontShowAgain.isChecked()){
					dbH.setAttrAskSizeDownload("false");
				}
			}
		});

		downloadConfirmationDialog = builder.create();
		downloadConfirmationDialog.show();
	}

	public void askConfirmationNoAppInstaledBeforeDownload (String parentPath,String url, long size, long [] hashes, String nodeToDownload, final boolean highPriority){
        logDebug("askConfirmationNoAppInstaledBeforeDownload");

		final String parentPathC = parentPath;
		final String urlC = url;
		final long [] hashesC = hashes;
		final long sizeC=size;

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

		builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
		builder.setPositiveButton(getString(R.string.general_save_to_device),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskNoAppDownload("false");
						}
						if(nC==null){
							nC = new NodeController(contactPropertiesMainActivity);
						}
						nC.download(parentPathC,urlC, sizeC, hashesC, highPriority);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if(dontShowAgain.isChecked()){
					dbH.setAttrAskNoAppDownload("false");
				}
			}
		});
		downloadConfirmationDialog = builder.create();
		downloadConfirmationDialog.show();
	}
}
