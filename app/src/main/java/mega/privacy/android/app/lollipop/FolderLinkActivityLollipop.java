package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListenerLink;
import mega.privacy.android.app.modalbottomsheet.FolderLinkBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DownloadInfo;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.SelectDownloadLocationDialog;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class FolderLinkActivityLollipop extends DownloadableActivity implements MegaRequestListenerInterface, OnClickListener{

	public static ImageView imageDrag;
	
	FolderLinkActivityLollipop folderLinkActivity = this;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	MegaChatApiAndroid megaChatApi;
	
	private AlertDialog decryptionKeyDialog;
	
	ActionBar aB;
	Toolbar tB;
	Toolbar fileLinktB;
	Handler handler;
	String url;
	String folderHandle;
	String folderKey;
	String folderSubHandle;
	RecyclerView listView;
	LinearLayoutManager mLayoutManager;
	MegaNode selectedNode;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
    RelativeLayout fragmentContainer;
	RelativeLayout fileLinkFragmentContainer;
	Button downloadButton;
	View separator;
	Button importButton;
	LinearLayout optionsBar;
	DisplayMetrics outMetrics;
	long parentHandle = -1;
	ArrayList<MegaNode> nodes;
	MegaNodeAdapter adapterList;

	ImageView fileLinkIconView;
	TextView fileLinkNameView;
	ScrollView fileLinkScrollView;
	TextView fileLinkSizeTextView;
	TextView fileLinkSizeTitleView;
	TextView fileLinkImportButton;
	TextView fileLinkDownloadButton;
	LinearLayout fileLinkOptionsBar;
	RelativeLayout fileLinkInfoLayout;

	Stack<Integer> lastPositionStack;

	long toHandle = 0;
	long fragmentHandle = -1;
	int cont = 0;
	ProgressDialog statusDialog;
	MultipleRequestListenerLink importLinkMultipleListener = null;
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	boolean decryptionIntroduced=false;
	public final static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	private ActionMode actionMode;
	
	boolean downloadCompleteFolder = false;
	FolderLinkActivityLollipop folderLinkActivityLollipop = this;

	MegaNode pN = null;
	boolean fileLinkFolderLink = false;

	String downloadLocationDefaultPath;
	public static final int FOLDER_LINK = 2;

	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapterList.isMultipleSelect()){
			adapterList.setMultipleSelect(true);
			actionMode = startSupportActionMode(new ActionBarCallBack());
		}
	}

	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = adapterList.getSelectedNodes();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						boolean hasStoragePermission = (ContextCompat.checkSelfPermission(folderLinkActivityLollipop, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
						if (!hasStoragePermission) {
							ActivityCompat.requestPermissions(folderLinkActivityLollipop,
					                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									REQUEST_WRITE_STORAGE);
							
							handleListM.clear();
							for (int i=0;i<documents.size();i++){
								handleListM.add(documents.get(i).getHandle());
							}
							
							return false;
						}
					}
					
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					onFileClick(handleList);
					clearSelections();
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					break;
				}
			}
			
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.folder_link_action, menu);
			changeStatusBarColorActionMode(getApplicationContext(), getWindow(), handler, 1);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			clearSelections();
			adapterList.setMultipleSelect(false);
			optionsBar.setVisibility(View.VISIBLE);
			separator.setVisibility(View.VISIBLE);
			changeStatusBarColorActionMode(getApplicationContext(), getWindow(), handler, 0);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapterList.getSelectedNodes();
			boolean showDownload = false;
			
			if (selected.size() != 0) {
				if (selected.size() > 0) {
					showDownload = true;
				}
				if(selected.size()==adapterList.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);			
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);	
				}	
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}			
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			
			return false;
		}
		
	}	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		logDebug("onOptionsItemSelected");
		switch (item.getItemId()) {
	    	// Respond to the action bar's Up/Home button
		    case android.R.id.home:{
		    	onBackPressed();
		    	return true;
		    }
		}
	
		return super.onOptionsItemSelected(item);
    }

	public void updateScrollPosition(int position) {
		logDebug("Position: " + position);
		if (adapterList != null && mLayoutManager != null){
			mLayoutManager.scrollToPosition(position);
		}
	}

	public ImageView getImageDrag(int position) {
		logDebug("Position: " + position);
		if (adapterList != null && mLayoutManager != null){
			View v = mLayoutManager.findViewByPosition(position);
			if (v != null){
				return (ImageView) v.findViewById(R.id.file_list_thumbnail);
			}
		}

		return null;
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
					if (adapterType == FOLDER_LINK_ADAPTER) {
						if (actionType == UPDATE_IMAGE_DRAG) {
							imageDrag = getImageDrag(position);
							if (folderLinkActivity.imageDrag != null) {
								folderLinkActivity.imageDrag.setVisibility(View.VISIBLE);
							}
							if (imageDrag != null) {
								folderLinkActivity.imageDrag = imageDrag;
								folderLinkActivity.imageDrag.setVisibility(View.GONE);
							}
						} else if (actionType == SCROLL_TO_POSITION) {
							updateScrollPosition(position);
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
					LocalBroadcastManager.getInstance(folderLinkActivity).sendBroadcast(intent1);
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate()");
    	requestWindowFeature(Window.FEATURE_NO_TITLE);	
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;

		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);

		float scaleText;
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}

		handler = new Handler();

		MegaApplication app = (MegaApplication)getApplication();
		megaApiFolder = app.getMegaApiFolder();
		megaApiFolder.httpServerStop();
		megaApi = app.getMegaApi();
		megaApi.httpServerStop();

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION));

		dbH = DatabaseHandler.getDbHandler(FolderLinkActivityLollipop.this);

		Intent intentReceived = getIntent();

		if (intentReceived != null) {
			url = intentReceived.getDataString();
		}

		if (dbH.getCredentials() != null) {
			if (megaApi == null || megaApi.getRootNode() == null) {
				logDebug("Refresh session - sdk");
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra("visibleFragment", LOGIN_FRAGMENT);
				intent.setData(Uri.parse(url));
				intent.setAction(ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return;
			}
			if (isChatEnabled()) {
				if (megaChatApi == null) {
					megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
				}

				if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
					logDebug("Refresh session - karere");
					Intent intent = new Intent(this, LoginActivityLollipop.class);
					intent.putExtra("visibleFragment", LOGIN_FRAGMENT);
					intent.setData(Uri.parse(url));
					intent.setAction(ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
					return;
				}
			}
		}
		
		folderLinkActivity = this;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
		}

		prefs = dbH.getPreferences();
		downloadLocationDefaultPath = getDownloadLocation(this);

		lastPositionStack = new Stack<>();
		
		setContentView(R.layout.activity_folder_link);	
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_folder_link);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
//		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);

		fileLinktB = (Toolbar) findViewById(R.id.toolbar_folder_link_file_link);

        fragmentContainer = (RelativeLayout) findViewById(R.id.folder_link_fragment_container);
		fileLinkFragmentContainer = (RelativeLayout) findViewById(R.id.folder_link_file_link_fragment_container);
		fileLinkFragmentContainer.setVisibility(View.GONE);

		emptyImageView = (ImageView) findViewById(R.id.folder_link_list_empty_image);
		emptyTextView = (TextView) findViewById(R.id.folder_link_list_empty_text);

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
		}else{
			emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
		}

		String textToShow = String.format(getString(R.string.file_browser_empty_folder_new));
		try{
			textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		}
		catch (Exception e){}
		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(textToShow);
		}
		emptyTextView.setText(result);
		emptyImageView.setVisibility(View.GONE);
		emptyTextView.setVisibility(View.GONE);

		listView = (RecyclerView) findViewById(R.id.folder_link_list_view_browser);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);
		listView.setItemAnimator(new DefaultItemAnimator()); 
		
		optionsBar = (LinearLayout) findViewById(R.id.options_folder_link_layout);
		separator = (View) findViewById(R.id.separator_3);

		downloadButton = (Button) findViewById(R.id.folder_link_button_download);
		downloadButton.setOnClickListener(this);

		importButton = (Button) findViewById(R.id.folder_link_import_button);
		importButton.setOnClickListener(this);

		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		if (dbH != null){
			if (dbH.getCredentials() != null){
				importButton.setVisibility(View.VISIBLE);
			}
			else{
				importButton.setVisibility(View.GONE);
			}
		}

		contentText = (TextView) findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);

		fileLinkIconView = (ImageView) findViewById(R.id.folder_link_file_link_icon);
		fileLinkIconView.getLayoutParams().width = scaleWidthPx(200, outMetrics);
		fileLinkIconView.getLayoutParams().height = scaleHeightPx(200, outMetrics);
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) fileLinkIconView.getLayoutParams();
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		fileLinkIconView.setLayoutParams(params);

		fileLinkNameView = (TextView) findViewById(R.id.folder_link_file_link_name);
		fileLinkNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		fileLinkNameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		fileLinkNameView.setSingleLine();
		fileLinkNameView.setTypeface(null, Typeface.BOLD);
		//Left margin
		RelativeLayout.LayoutParams nameViewParams = (RelativeLayout.LayoutParams)fileLinkNameView.getLayoutParams();
		nameViewParams.setMargins(scaleWidthPx(60, outMetrics), 0, 0, scaleHeightPx(20, outMetrics));
		fileLinkNameView.setLayoutParams(nameViewParams);

		fileLinkScrollView = (ScrollView) findViewById(R.id.folder_link_file_link_scroll_layout);

		fileLinkSizeTitleView = (TextView) findViewById(R.id.folder_link_file_link_info_menu_size);
		//Left margin, Top margin
		RelativeLayout.LayoutParams sizeTitleParams = (RelativeLayout.LayoutParams)fileLinkSizeTitleView.getLayoutParams();
		sizeTitleParams.setMargins(scaleWidthPx(10, outMetrics), scaleHeightPx(15, outMetrics), 0, 0);
		fileLinkSizeTitleView.setLayoutParams(sizeTitleParams);

		fileLinkSizeTextView = (TextView) findViewById(R.id.folder_link_file_link_size);
		//Bottom margin
		RelativeLayout.LayoutParams sizeTextParams = (RelativeLayout.LayoutParams)fileLinkSizeTextView.getLayoutParams();
		sizeTextParams.setMargins(scaleWidthPx(10, outMetrics), 0, 0, scaleHeightPx(15, outMetrics));
		fileLinkSizeTextView.setLayoutParams(sizeTextParams);

		fileLinkOptionsBar = (LinearLayout) findViewById(R.id.options_folder_link_file_link_layout);

		fileLinkDownloadButton = (TextView) findViewById(R.id.folder_link_file_link_button_download);
		fileLinkDownloadButton.setOnClickListener(this);
		fileLinkDownloadButton.setText(getString(R.string.general_save_to_device).toUpperCase(Locale.getDefault()));
		//Left and Right margin
		LinearLayout.LayoutParams downloadTextParams = (LinearLayout.LayoutParams)fileLinkDownloadButton.getLayoutParams();
		downloadTextParams.setMargins(scaleWidthPx(6, outMetrics), 0, scaleWidthPx(8, outMetrics), 0);
		fileLinkDownloadButton.setLayoutParams(downloadTextParams);

		fileLinkImportButton = (TextView) findViewById(R.id.folder_link_file_link_button_import);
		fileLinkImportButton.setText(getString(R.string.add_to_cloud).toUpperCase(Locale.getDefault()));
		fileLinkImportButton.setOnClickListener(this);
		//Left and Right margin
		LinearLayout.LayoutParams importTextParams = (LinearLayout.LayoutParams)fileLinkImportButton.getLayoutParams();
		importTextParams.setMargins(scaleWidthPx(6, outMetrics), 0, scaleWidthPx(8, outMetrics), 0);
		fileLinkImportButton.setLayoutParams(importTextParams);
		fileLinkImportButton.setVisibility(View.INVISIBLE);

		fileLinkInfoLayout = (RelativeLayout) findViewById(R.id.folder_link_file_link_layout);
		FrameLayout.LayoutParams infoLayoutParams = (FrameLayout.LayoutParams)fileLinkInfoLayout.getLayoutParams();
		infoLayoutParams.setMargins(0, 0, 0, scaleHeightPx(80, outMetrics));
		fileLinkInfoLayout.setLayoutParams(infoLayoutParams);

		Intent intent = getIntent();
    	
    	if (intent != null) {
    		if (intent.getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)){
    			if (parentHandle == -1){
    				url = intent.getDataString();
					if(url!=null){
						logDebug("URL: " + url);
						String [] s = url.split("!");
						logDebug("URL parts: "  + s.length);
						for (int i=0;i<s.length;i++){
							switch (i){
								case 1:{
									folderHandle = s[1];
									logDebug("URL_handle: " + folderHandle);
									break;
								}
								case 2:{
									folderKey = s[2];
									logDebug("URL_key: " + folderKey);
									break;
								}
								case 3:{
									folderSubHandle = s[3];
									logDebug("URL_subhandle: " + folderSubHandle);
									break;
								}
							}
						}
						megaApiFolder.loginToFolder(url, this);
					}
					else{
						logWarning("url NULL");
					}
//    				int counter = url.split("!").length - 1;
//    				log("Counter !: "+counter);
//    				if(counter<2){
//    					//Ask for decryption key
//    					log("Ask for decryption key");
//    					askForDecryptionKeyDialog();
//    				}
//    				else{
//    					//Decryption key included!
//
//    				}
    			}
    		}
    	}
    	
    	aB.setTitle("MEGA - " + getString(R.string.general_loading));

		
		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
    }
	
	public void askForDecryptionKeyDialog(){
		logDebug("askForDecryptionKeyDialog");
		
		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(scaleWidthPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

	    final EditText input = new EditText(this);
	    layout.addView(input, params);		
		
		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
		input.setHint(getString(R.string.alert_decryption_key));
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
					if(value.startsWith("!")){
						logDebug("Decryption key with exclamation!");
						url=url+value;
					}
					else{
						url=url+"!"+value;
					}
					logDebug("Folder link to import: " + url);
					decryptionIntroduced=true;
					megaApiFolder.loginToFolder(url, folderLinkActivity);
					decryptionKeyDialog.dismiss();
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
		builder.setTitle(getString(R.string.alert_decryption_key));
		builder.setMessage(getString(R.string.message_decryption_key));
		builder.setPositiveButton(getString(R.string.general_decryp),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							logWarning("Empty key, ask again!");
							decryptionIntroduced=false;
							askForDecryptionKeyDialog();
							return;
						}else{
							if(value.startsWith("!")){
								logDebug("Decryption key with exclamation!");
								url=url+value;
							}
							else{
								url=url+"!"+value;
							}
							logDebug("Folder link to import: " + url);
							decryptionIntroduced=true;
							megaApiFolder.loginToFolder(url, folderLinkActivity);
						}
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
			}
		});
		builder.setView(layout);
		decryptionKeyDialog = builder.create();
		decryptionKeyDialog.show();
	}
	
	private void showKeyboardDelayed(final View view) {
		logDebug("showKeyboardDelayed");
		handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {				
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	@Override
	protected void onDestroy() {

		if (megaApiFolder != null){
			megaApiFolder.removeRequestListener(this);
//			megaApiFolder.logout();
		}

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		handler.removeCallbacksAndMessages(null);

		super.onDestroy();
	}

	@Override
	protected void onPause() {
    	folderLinkActivity = null;
		logDebug("onPause");
    	super.onPause();
    }

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE){
			emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
		}else{
			emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
		}

	}


	@Override
	protected void onResume() {
		super.onResume();
    	folderLinkActivity = this;
		logDebug("onResume");
	}

	public void toSelectFolder(long [] hashes, long size, String sdRoot, String prompt) {
        Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
        intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
        intent.setClass(this, FileStorageActivityLollipop.class);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
        if(sdRoot != null) {
            intent.putExtra(FileStorageActivityLollipop.EXTRA_SD_ROOT, sdRoot);
        }
        if(prompt != null) {
            intent.putExtra(FileStorageActivityLollipop.EXTRA_PROMPT, prompt);
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
    }

    private void showSelectDownloadLocationDialog(final long [] hashes, final long size) {
        SelectDownloadLocationDialog selector = new SelectDownloadLocationDialog(this);
        selector.initDialogBuilder(new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        toSelectFolder(hashes, size, null, null);
                        break;
                    }
                    case 1: {
                        SDCardOperator sdCardOperator = null;
                        try {
                            sdCardOperator = new SDCardOperator(FolderLinkActivityLollipop.this);
                        } catch (SDCardOperator.SDCardException e) {
                            e.printStackTrace();
                            logError("Initialize SDCardOperator failed, toSelectFolder", e);
                            toSelectFolder(hashes, size, null, null);
                        }
                        if(sdCardOperator != null) {
                            String sdCardRoot = sdCardOperator.getSDCardRoot();
                            //don't use DocumentFile
                            if (sdCardOperator.canWriteWithFile(sdCardRoot)) {
                                toSelectFolder(hashes, size, sdCardRoot,null);
                            } else {
                                setDownloadInfo(new DownloadInfo(false, size, hashes));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    try {
                                        sdCardOperator.initDocumentFileRoot(dbH.getSDCardUri());
                                        toSelectFolder(hashes, size, sdCardRoot,null);
                                    } catch (SDCardOperator.SDCardException e) {
                                        e.printStackTrace();
                                        logError("SDCardOperator initDocumentFileRoot failed, requestSDCardPermission", e);
                                        //request sd card root request and write permission.
                                        SDCardOperator.requestSDCardPermission(sdCardRoot, FolderLinkActivityLollipop.this, FolderLinkActivityLollipop.this);
                                    }
                                } else {
                                    SDCardOperator.requestSDCardPermission(sdCardRoot, FolderLinkActivityLollipop.this, FolderLinkActivityLollipop.this);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        });
        selector.show();
    }

	@SuppressLint("NewApi")
    public void onFileClick(ArrayList<Long> handleList){
        long size = 0;
        long[] hashes = new long[handleList.size()];
        for (int i = 0; i < handleList.size(); i++) {
            hashes[i] = handleList.get(i);
            MegaNode n = megaApiFolder.getNodeByHandle(hashes[i]);
            if (n != null) {
                size += n.getSize();
            }
        }
        preDownload(size, hashes);
    }

    private void preDownload(long size, long[] hashes) {
        if (dbH == null) {
            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        }

        boolean askMe = Util.askMe(this);
        prefs = dbH.getPreferences();
        downloadLocationDefaultPath = getDownloadLocation(this);

        if (askMe) {
            File[] fs = getExternalFilesDirs(null);
            if (fs.length <= 1 || fs[1] == null) {
                toSelectFolder(hashes, size, null, null);
            } else {
                showSelectDownloadLocationDialog(hashes, size);
            }
        } else {
            downloadTo(downloadLocationDefaultPath, null, null, size, hashes);
        }
    }

    @SuppressLint("NewApi")
    public void onFolderClick(long handle, final long size){
		final long[] hashes = new long[1];
		hashes[0] = handle;
		preDownload(size, hashes);
	}
	
	public void downloadTo(String parentPath, String uriString, String url, final long size, final long [] hashes){

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_WRITE_STORAGE);
			    return;
			}
		}

        boolean downloadToSDCard = false;
        String downloadRoot = null;
        SDCardOperator sdCardOperator = null;
        try {
            sdCardOperator = new SDCardOperator(this);
        } catch (SDCardOperator.SDCardException e) {
            e.printStackTrace();
            logError("Initialize SDCardOperator failed, user has uninstalled the SD card", e);
            // user uninstall the sd card. but default download location is still on the sd card
            if (SDCardOperator.isSDCardPath(parentPath)) {
                logDebug("select new path as download location.");
                toSelectFolder(hashes,size, null,getString(R.string.no_external_SD_card_detected));
                return;
            }
        }
        if(sdCardOperator != null && SDCardOperator.isSDCardPath(parentPath)) {
            //user has installed another sd card.
            if(sdCardOperator.isNewSDCardPath(parentPath)) {
                logDebug("new sd card, check permission.");
                showSnackbar(Constants.SNACKBAR_TYPE,getString(R.string.old_sdcard_unavailable));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showSelectDownloadLocationDialog(hashes,size);
                    }
                }, 1500);
                return;
            }
            if (!sdCardOperator.canWriteWithFile(parentPath)) {
                logDebug("init sd card root with document file.");
                downloadToSDCard = true;
                downloadRoot = sdCardOperator.getDownloadRoot();
                try {
                    sdCardOperator.initDocumentFileRoot(dbH.getSDCardUri());
                } catch (SDCardOperator.SDCardException e) {
                    e.printStackTrace();
                    logError("SDCardOperator initDocumentFileRoot failed, requestSDCardPermission", e);
                    //don't have permission with sd card root. need to request.
                    String sdRoot = sdCardOperator.getSDCardRoot();
                    setDownloadInfo(new DownloadInfo(false, size, hashes));
                    //request SD card write permission.
                    SDCardOperator.requestSDCardPermission(sdRoot, FolderLinkActivityLollipop.this, FolderLinkActivityLollipop.this);
                    return;
                }
            }
        }

		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}

		int numberOfNodesToDownload = 0;
		int numberOfNodesAlreadyDownloaded = 0;
		int numberOfNodesPending = 0;
		for (long hash : hashes) {
			MegaNode node = megaApiFolder.getNodeByHandle(hash);
			if(node != null){
				Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
				Map<Long, String> targets = new HashMap<>();
				if (node.getType() == MegaNode.TYPE_FOLDER) {
                    if (downloadToSDCard) {
                        sdCardOperator.buildFileStructure(targets, parentPath, megaApiFolder, node);
                        getDlList(dlFiles, node, new File(downloadRoot, node.getName()));
                    } else {
                        getDlList(dlFiles, node, new File(parentPath, node.getName()));
                    }
				} else {
                    if (downloadToSDCard) {
                        targets.put(node.getHandle(), parentPath);
                        dlFiles.put(node, downloadRoot);
                    } else {
                        dlFiles.put(node, parentPath);
                    }
				}

				for (MegaNode document : dlFiles.keySet()) {
					String path = dlFiles.get(document);
                    String targetPath = targets.get(document.getHandle());
					logDebug("path of the file: "+path);
					numberOfNodesToDownload++;

					if(availableFreeSpace < document.getSize()){
						showSnackbar(NOT_SPACE_SNACKBAR_TYPE, null);
						continue;
					}

					File destDir = new File(path);
					File destFile;
					destDir.mkdirs();

					if (destDir.isDirectory()){
						destFile = new File(destDir, megaApi.escapeFsIncompatible(document.getName()));
						logDebug("destDir is Directory. destFile: " + destFile.getAbsolutePath());
					}
					else{
						logDebug("destDir is File");
						destFile = destDir;
					}

					if(destFile.exists() && (document.getSize() == destFile.length())){
						numberOfNodesAlreadyDownloaded++;
						logDebug(destFile.getAbsolutePath() + " already downloaded");
					}
					else {
						numberOfNodesPending++;
						logDebug("start service");
						logDebug("EXTRA_HASH: " + document.getHandle());
						Intent service = new Intent(this, DownloadService.class);
                        if(downloadToSDCard) {
                            service = NodeController.getDownloadToSDCardIntent(service,path, targetPath, uriString);
                        } else {
                            service.putExtra(DownloadService.EXTRA_PATH, path);
                        }
                        service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
                        service.putExtra(DownloadService.EXTRA_URL, url);
                        service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_FOLDER_LINK, true);
						startService(service);
					}
				}
			}
			else if(url != null) {
				if(availableFreeSpace < size) {
					showSnackbar(NOT_SPACE_SNACKBAR_TYPE, null);
					continue;
				}
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_HASH, hash);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				service.putExtra(DownloadService.EXTRA_FOLDER_LINK, true);
				startService(service);
			}
			else {
				logWarning("node not found");
			}
			logDebug("Total: " + numberOfNodesToDownload + " Already: " + numberOfNodesAlreadyDownloaded + " Pending: " + numberOfNodesPending);
			if (numberOfNodesAlreadyDownloaded > 0){
				String msg = getString(R.string.already_downloaded_multiple, numberOfNodesAlreadyDownloaded);
				if (numberOfNodesPending > 0){
					msg = msg + getString(R.string.pending_multiple, numberOfNodesPending);
				}
				showSnackbar(SNACKBAR_TYPE, msg);
			}
			else {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.download_began));
			}
		}
	}
	
	
	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
		
		if (megaApiFolder.getRootNode() == null)
			return;
		
		folder.mkdir();
		ArrayList<MegaNode> nodeList = megaApiFolder.getChildren(parent, orderGetChildren);
		for(int i=0; i<nodeList.size(); i++){
			MegaNode document = nodeList.get(i);
			if (document.getType() == MegaNode.TYPE_FOLDER) {
				File subfolder = new File(folder, new String(document.getName()));
				getDlList(dlFiles, document, subfolder);
			} 
			else {
				dlFiles.put(document, folder.getAbsolutePath());
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		logDebug("onActivityResult");
		if (intent == null){
			return;
		}

		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			logDebug("Local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            dbH.setStorageDownloadLocation(parentPath);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			logDebug("URL: " + url + "___SIZE: " + size);
	
			downloadTo (parentPath, null, url, size, hashes);
		} else if (requestCode == REQUEST_CODE_TREE) {
            onRequestSDCardWritePermission(intent, resultCode, null);
        }
		else if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK){

			if(!isOnline(this)) {
				try{
					statusDialog.dismiss();
				} catch(Exception ex) {}

				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			toHandle = intent.getLongExtra("IMPORT_TO", 0);
			fragmentHandle = intent.getLongExtra("fragmentH", -1);

			MegaNode target = megaApi.getNodeByHandle(toHandle);
			if(target == null){
				if (megaApi.getRootNode() != null){
					target = megaApi.getRootNode();
				}
			}

			statusDialog = new ProgressDialog(this);
			statusDialog.setMessage(getString(R.string.general_importing));
			statusDialog.show();

			if(adapterList != null && adapterList.isMultipleSelect()){
				logDebug("Is multiple select");
				List<MegaNode> nodes = adapterList.getSelectedNodes();
				if(nodes.size() != 0){
					if (target != null){
						logDebug("Target node: " + target.getHandle());
						for(MegaNode node : nodes ){
							node = megaApiFolder.authorizeNode(node);
							if(node != null){
								cont ++;
								importLinkMultipleListener = new MultipleRequestListenerLink(this, cont, cont, FOLDER_LINK);
								megaApi.copyNode(node, target, importLinkMultipleListener);
								//megaApi.copyNode(node, target, this);
							}else{
								showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
							}
						}
					}else{
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
					}
				}else{
					logWarning("No selected nodes");
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
				}
			}else{
				logDebug("No multiple select");
				if(selectedNode!=null){
					if (target != null){
						logDebug("Target node: " + target.getHandle());
						selectedNode = megaApiFolder.authorizeNode(selectedNode);
						if (selectedNode != null){
							cont ++;
							importLinkMultipleListener = new MultipleRequestListenerLink(this, cont, cont, FOLDER_LINK);
							megaApi.copyNode(selectedNode, target, importLinkMultipleListener);
						}else{
							showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
						}
					}else{
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
					}
				}else{
					logWarning("Selected Node is NULL");
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
				}
			}
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestUpdate: " + request.getRequestString());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish: " + request.getRequestString());

		if (request.getType() == MegaRequest.TYPE_LOGIN){
			if (e.getErrorCode() == MegaError.API_OK){
				megaApiFolder.fetchNodes(this);	
			}
			else{
				logWarning("Error: " + e.getErrorCode());
				if(e.getErrorCode() == MegaError.API_EINCOMPLETE){
					decryptionIntroduced=false;
					askForDecryptionKeyDialog();
					return;
				}
				else if(e.getErrorCode() == MegaError.API_EARGS){
					if(decryptionIntroduced){
						logWarning("Incorrect key, ask again!");
						decryptionIntroduced=false;
						askForDecryptionKeyDialog();
						return;
					}
					else{
						try{
							logWarning("API_EARGS - show alert dialog");
							AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
							builder.setMessage(getString(R.string.link_broken));
							builder.setTitle(getString(R.string.general_error_word));
							builder.setCancelable(false);

							builder.setPositiveButton(getString(android.R.string.ok),new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									Intent backIntent;
									boolean closedChat = MegaApplication.isClosedChat();
									if(closedChat){
										if(folderLinkActivity != null)
											backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
										else
											backIntent = new Intent(FolderLinkActivityLollipop.this, ManagerActivityLollipop.class);

										startActivity(backIntent);
									}

									finish();
								}
							});

							AlertDialog dialog = builder.create();
							dialog.show();
						}
						catch(Exception ex){
							showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error_folder_not_found));
							finish();
						}
					}
				}
				else{
					try{
						logWarning("No link - show alert dialog");
						AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
						builder.setMessage(getString(R.string.general_error_folder_not_found));
						builder.setTitle(getString(R.string.general_error_word));

						builder.setPositiveButton(getString(android.R.string.ok),new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Intent backIntent;
								boolean closedChat = MegaApplication.isClosedChat();
								if(closedChat){
									if(folderLinkActivity != null)
										backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
									else
										backIntent = new Intent(FolderLinkActivityLollipop.this, ManagerActivityLollipop.class);
									startActivity(backIntent);
								}

								finish();
							}
						});

						AlertDialog dialog = builder.create();
						dialog.show();
					}
					catch(Exception ex){
						showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error_folder_not_found));
						finish();
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			if (e.getErrorCode() != MegaError.API_OK) {
				logWarning("ERROR: " + e.getErrorString());
				if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(ACTION_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();
				}
				else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
					logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();
				}
				else{
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
				}

			}else{
				logDebug("onRequestFinish:OK");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
				clearSelections();
				hideMultipleSelect();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){

			if (e.getErrorCode() == MegaError.API_OK) {
				logDebug("DOCUMENTNODEHANDLEPUBLIC: " + request.getNodeHandle());
				if (dbH == null){
					dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				}

				dbH.setLastPublicHandle(request.getNodeHandle());
				dbH.setLastPublicHandleTimeStamp();

				MegaNode rootNode = megaApiFolder.getRootNode();
				if (rootNode != null){

					if(request.getFlag()){
						logWarning("Login into a folder with invalid decryption key");
						try{
							AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
							builder.setMessage(getString(R.string.general_error_invalid_decryption_key));
							builder.setTitle(getString(R.string.general_error_word));

							builder.setPositiveButton(
									getString(android.R.string.ok),
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											dialog.dismiss();
											boolean closedChat = MegaApplication.isClosedChat();
											if(closedChat){
												Intent backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
												startActivity(backIntent);
											}

											finish();
										}
									});

							AlertDialog dialog = builder.create();
							dialog.show();
						}
						catch(Exception ex){
							showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error_folder_not_found));
							finish();
						}
					}
					else{
						if (folderSubHandle != null){
							pN = megaApiFolder.getNodeByHandle(MegaApiAndroid.base64ToHandle(folderSubHandle));
							if (pN != null){
								if (pN.isFolder()) {
									parentHandle = MegaApiAndroid.base64ToHandle(folderSubHandle);
									nodes = megaApiFolder.getChildren(pN);
									aB.setTitle(pN.getName());
									supportInvalidateOptionsMenu();
								}
								else if (pN.isFile()){
									fileLinkFolderLink = true;
									parentHandle = MegaApiAndroid.base64ToHandle(folderSubHandle);
									setSupportActionBar(fileLinktB);
									aB = getSupportActionBar();
									aB.setDisplayHomeAsUpEnabled(true);
									aB.setDisplayShowHomeEnabled(true);
									aB.setTitle("");

									fragmentContainer.setVisibility(View.GONE);
									fileLinkFragmentContainer.setVisibility(View.VISIBLE);

									fileLinkNameView.setText(pN.getName());
									fileLinkSizeTextView.setText(Formatter.formatFileSize(this, pN.getSize()));

									fileLinkIconView.setImageResource(MimeTypeList.typeForName(pN.getName()).getIconResourceId());

									fileLinkDownloadButton.setVisibility(View.VISIBLE);
									if (dbH == null){
										dbH = DatabaseHandler.getDbHandler(getApplicationContext());
									}
									if (dbH != null){
										if (dbH.getCredentials() != null){
											fileLinkImportButton.setVisibility(View.VISIBLE);
										}
										else{
											fileLinkImportButton.setVisibility(View.INVISIBLE);
										}
									}

									Bitmap preview = null;
									preview = getPreviewFromCache(pN);
									if (preview != null){
										previewCache.put(pN.getHandle(), preview);
										fileLinkIconView.setImageBitmap(preview);
										fileLinkIconView.setOnClickListener(this);
									}
									else{
										preview = getPreviewFromFolder(pN, this);
										if (preview != null){
											previewCache.put(pN.getHandle(), preview);
											fileLinkIconView.setImageBitmap(preview);
											fileLinkIconView.setOnClickListener(this);
										}
										else{
											if (pN.hasPreview()) {
												File previewFile = new File(getPreviewFolder(this), pN.getBase64Handle() + ".jpg");
												megaApiFolder.getPreview(pN, previewFile.getAbsolutePath(), this);
											}
										}
									}

								}
								else{
									parentHandle = rootNode.getHandle();
									nodes = megaApiFolder.getChildren(rootNode);
									aB.setTitle(megaApiFolder.getRootNode().getName());
									supportInvalidateOptionsMenu();
								}
							}
							else{
								parentHandle = rootNode.getHandle();
								nodes = megaApiFolder.getChildren(rootNode);
								aB.setTitle(megaApiFolder.getRootNode().getName());
								supportInvalidateOptionsMenu();
							}
						}
						else {
							parentHandle = rootNode.getHandle();
							nodes = megaApiFolder.getChildren(rootNode);
							aB.setTitle(megaApiFolder.getRootNode().getName());
							supportInvalidateOptionsMenu();
						}

						if (adapterList == null){
							adapterList = new MegaNodeAdapter(this, null, nodes, parentHandle, listView, aB, FOLDER_LINK_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
						}
						else{
							adapterList.setParentHandle(parentHandle);
							adapterList.setNodes(nodes);
						}

						adapterList.setMultipleSelect(false);

						listView.setAdapter(adapterList);

						//If folder has not files
						if (adapterList.getItemCount() == 0){
							listView.setVisibility(View.GONE);
							emptyImageView.setVisibility(View.VISIBLE);
							emptyTextView.setVisibility(View.VISIBLE);
						}else{
							listView.setVisibility(View.VISIBLE);
							emptyImageView.setVisibility(View.GONE);
							emptyTextView.setVisibility(View.GONE);
						}


					}
				}
				else{
					try{ 
						AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			            builder.setMessage(getString(R.string.general_error_folder_not_found));
						builder.setTitle(getString(R.string.general_error_word));
						
						builder.setPositiveButton(
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									boolean closedChat = MegaApplication.isClosedChat();
									if(closedChat){
										Intent backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
										startActivity(backIntent);
									}

					    			finish();
								}
							});
										
						AlertDialog dialog = builder.create();
						dialog.show(); 
					}
					catch(Exception ex){
						showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error_folder_not_found));
		    			finish();
					}
				}
			}
			else{
				logWarning("Error: " + e.getErrorCode() + " " + e.getErrorString());
				try{
					AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);

					if(e.getErrorCode() == MegaError.API_EBLOCKED){
						builder.setMessage(getString(R.string.folder_link_unavaible_ToS_violation));
						builder.setTitle(getString(R.string.general_error_folder_not_found));
					}
					else if(e.getErrorCode() == MegaError.API_ETOOMANY){
						builder.setMessage(getString(R.string.file_link_unavaible_delete_account));
						builder.setTitle(getString(R.string.general_error_folder_not_found));
					}
					else{
						builder.setMessage(getString(R.string.general_error_folder_not_found));
						builder.setTitle(getString(R.string.general_error_word));
					}
					builder.setCancelable(false);
					builder.setPositiveButton(
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									boolean closedChat = MegaApplication.isClosedChat();
									if(closedChat){
										Intent backIntent = new Intent(folderLinkActivity, ManagerActivityLollipop.class);
										startActivity(backIntent);
									}
									finish();
								}
							});

					AlertDialog dialog = builder.create();
					dialog.show();
				}
				catch(Exception ex){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error_folder_not_found));
					finish();
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE) {
			if (e.getErrorCode() == MegaError.API_OK) {
				File previewDir = getPreviewFolder(this);
				if (pN != null) {
					File preview = new File(previewDir, pN.getBase64Handle() + ".jpg");
					if (preview.exists()) {
						if (preview.length() > 0) {
							Bitmap bitmap = getBitmapForCache(preview, this);
							previewCache.put(pN.getHandle(), bitmap);
							if (fileLinkIconView != null) {
								fileLinkIconView.setImageBitmap(bitmap);
								fileLinkIconView.setOnClickListener(this);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getRequestString());
	}

	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
		optionsBar.setVisibility(View.VISIBLE);
		separator.setVisibility(View.VISIBLE);
	}
	
	public void selectAll(){
		if (adapterList != null){
			if(adapterList.isMultipleSelect()){
				adapterList.selectAll();
			}
			else{				
				adapterList.setMultipleSelect(true);
				adapterList.selectAll();
				
				actionMode = startSupportActionMode(new ActionBarCallBack());
			}
			
			updateActionModeTitle();
		}
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if (adapterList != null && adapterList.isMultipleSelect()) {
			adapterList.clearSelections();
		}
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null) {
			return;
		}
		List<MegaNode> documents = adapterList.getSelectedNodes();
		int files = 0;
		int folders = 0;
		for (MegaNode document : documents) {
			if (document.isFile()) {
				files++;
			} else if (document.isFolder()) {
				folders++;
			}
		}
		Resources res = getResources();
		/*String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));
		String foldersStr = String.format(format, folders,
				res.getQuantityString(R.plurals.general_num_folders, folders));
		String title;
		if (files == 0 && folders == 0) {
			title = foldersStr + ", " + filesStr;
		} else if (files == 0) {
			title = foldersStr;
		} else if (folders == 0) {
			title = filesStr;
		} else {
			title = foldersStr + ", " + filesStr;
		}
		actionMode.setTitle(title);*/

		String title;
		int sum=files+folders;

		if (files == 0 && folders == 0) {
			title = Integer.toString(sum);
		} else if (files == 0) {
			title = Integer.toString(folders);
		} else if (folders == 0) {
			title = Integer.toString(files);
		} else {
			title = Integer.toString(sum);
		}
		actionMode.setTitle(title);
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			logError("Invalidate error", e);
			e.printStackTrace();
		}
	}
	
	ArrayList<Long> handleListM = new ArrayList<Long>();

	public void itemClick(int position, int[] screenPosition, ImageView imageView) {

		if (adapterList.isMultipleSelect()){
			logDebug("Multiselect ON");
			adapterList.toggleSelection(position);

			List<MegaNode> selectedNodes = adapterList.getSelectedNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
			}
		}
		else{
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;

				lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

				logDebug("Push to stack " + lastFirstVisiblePosition + " position");
				lastPositionStack.push(lastFirstVisiblePosition);

				aB.setTitle(n.getName());
				supportInvalidateOptionsMenu();
//				((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = nodes.get(position).getHandle();
//				((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
				adapterList.setParentHandle(parentHandle);
				nodes = megaApiFolder.getChildren(nodes.get(position), orderGetChildren);
				adapterList.setNodes(nodes);
				listView.scrollToPosition(0);

				//If folder has no files
				if (adapterList.getItemCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
			}
			else{
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(this, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", FOLDER_LINK_ADAPTER);
					if (megaApiFolder.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApiFolder.getParentNode(nodes.get(position)).getHandle());
					}
					intent.putExtra("orderGetChildren", orderGetChildren);
					intent.putExtra("isFolderLink", true);
					intent.putExtra("screenPosition", screenPosition);
					startActivity(intent);
					overridePendingTransition(0,0);
					imageDrag = imageView;
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isVideoReproducible() || MimeTypeList.typeForName(nodes.get(position).getName()).isAudio() ){
					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					logDebug("FILE HANDLE: " + file.getHandle());

					Intent mediaIntent;
					boolean internalIntent;
					boolean opusFile = false;
					if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()){
						mediaIntent = new Intent(Intent.ACTION_VIEW);
						internalIntent = false;
						String[] s = file.getName().split("\\.");
						if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
							opusFile = true;
						}
					}
					else {
						internalIntent = true;
						mediaIntent = new Intent(FolderLinkActivityLollipop.this, AudioVideoPlayerLollipop.class);
					}
					mediaIntent.putExtra("orderGetChildren", orderGetChildren);
					mediaIntent.putExtra("isFolderLink", true);
					mediaIntent.putExtra("HANDLE", file.getHandle());
					mediaIntent.putExtra("FILENAME", file.getName());
					mediaIntent.putExtra("screenPosition", screenPosition);
					mediaIntent.putExtra("adapterType", FOLDER_LINK_ADAPTER);
					if (megaApiFolder.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
						mediaIntent.putExtra("parentNodeHandle", -1L);
					}
					else{
						mediaIntent.putExtra("parentNodeHandle", megaApiFolder.getParentNode(nodes.get(position)).getHandle());
					}
					imageDrag = imageView;
					boolean isOnMegaDownloads = false;
					String localPath = getLocalFile(this, file.getName(), file.getSize(), downloadLocationDefaultPath);
					File f = new File(downloadLocationDefaultPath, file.getName());
					if(f.exists() && (f.length() == file.getSize())){
						isOnMegaDownloads = true;
					}
					if (localPath != null && (isOnMegaDownloads || (megaApiFolder.getFingerprint(file) != null && megaApiFolder.getFingerprint(file).equals(megaApiFolder.getFingerprint(localPath))))){
						File mediaFile = new File(localPath);
						//mediaIntent.setDataAndType(Uri.parse(localPath), mimeType);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
							mediaIntent.setDataAndType(FileProvider.getUriForFile(FolderLinkActivityLollipop.this, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						else{
							mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}
					else {
						String url;
						if (dbH.getCredentials() != null) {
							if (megaApi.httpServerIsRunning() == 0) {
								megaApi.httpServerStart();
							}

							ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
							ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
							activityManager.getMemoryInfo(mi);

							if (mi.totalMem > BUFFER_COMP) {
								logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
								megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
							}
							else {
								logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
								megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
							}

							url = megaApi.httpServerGetLocalLink(file);
						}
						else {
							if (megaApiFolder.httpServerIsRunning() == 0) {
								megaApiFolder.httpServerStart();
							}

							ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
							ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
							activityManager.getMemoryInfo(mi);

							if (mi.totalMem > BUFFER_COMP) {
								logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
								megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
							}
							else {
								logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
								megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
							}

							url = megaApiFolder.httpServerGetLocalLink(file);
						}
						if (url != null) {
							logDebug("FolderLink URL: " + url);
							mediaIntent.setDataAndType(Uri.parse(url), mimeType);
						}
					}
					if (opusFile){
						mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
					}
					if (internalIntent) {
						startActivity(mediaIntent);
					}
					else {
						if (isIntentAvailable(this, mediaIntent)){
							startActivity(mediaIntent);
						}
						else{
							showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available));
							adapterList.notifyDataSetChanged();
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(nodes.get(position).getHandle());
							onFileClick(handleList);
						}
					}
			  		overridePendingTransition(0,0);
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isPdf()){
					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					logDebug("FILE HANDLE: " + file.getHandle() + ", TYPE: " + mimeType);

					Intent pdfIntent = new Intent(FolderLinkActivityLollipop.this, PdfViewerActivityLollipop.class);
					pdfIntent.putExtra("APP", true);
					pdfIntent.putExtra("adapterType", FOLDER_LINK_ADAPTER);
					boolean isOnMegaDownloads = false;
					String localPath = getLocalFile(this, file.getName(), file.getSize(), downloadLocationDefaultPath);
					File f = new File(downloadLocationDefaultPath, file.getName());
					if(f.exists() && (f.length() == file.getSize())){
						isOnMegaDownloads = true;
					}
					if (localPath != null && (isOnMegaDownloads || (megaApiFolder.getFingerprint(file) != null && megaApiFolder.getFingerprint(file).equals(megaApiFolder.getFingerprint(localPath))))){
						File mediaFile = new File(localPath);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
							pdfIntent.setDataAndType(FileProvider.getUriForFile(FolderLinkActivityLollipop.this, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						else{
							pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}
					else {
						String url;
						if (dbH != null && dbH.getCredentials() != null) {
							if (megaApi.httpServerIsRunning() == 0) {
								megaApi.httpServerStart();
							}

							ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
							ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
							activityManager.getMemoryInfo(mi);

							if (mi.totalMem > BUFFER_COMP) {
								logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
								megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
							}
							else {
								logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
								megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
							}

							url = megaApi.httpServerGetLocalLink(file);
						}
						else {
							if (megaApiFolder.httpServerIsRunning() == 0) {
								megaApiFolder.httpServerStart();
							}

							ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
							ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
							activityManager.getMemoryInfo(mi);

							if (mi.totalMem > BUFFER_COMP) {
								logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
								megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
							}
							else {
								logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
								megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
							}

							url = megaApiFolder.httpServerGetLocalLink(file);
						}
						if (url != null) {
							logDebug("FolderLink URL: " + url);
							pdfIntent.setDataAndType(Uri.parse(url), mimeType);
						}
					}
					pdfIntent.putExtra("HANDLE", file.getHandle());
					pdfIntent.putExtra("isFolderLink", true);
					pdfIntent.putExtra("inside", true);
					pdfIntent.putExtra("screenPosition", screenPosition);
					imageDrag = imageView;
					if (isIntentAvailable(FolderLinkActivityLollipop.this, pdfIntent)){
						startActivity(pdfIntent);
					}
					else{
						Toast.makeText(FolderLinkActivityLollipop.this, FolderLinkActivityLollipop.this.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(FolderLinkActivityLollipop.this);
						nC.prepareForDownload(handleList, false);
					}
					overridePendingTransition(0,0);
				}
				else{
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
						if (!hasStoragePermission) {
							ActivityCompat.requestPermissions(this,
					                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									REQUEST_WRITE_STORAGE);
							
							handleListM.clear();
							handleListM.add(nodes.get(position).getHandle());
							
							return;
						}
					}
					adapterList.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					onFileClick(handleList);
				}
			}
		}
	}
	
	@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
        	case REQUEST_WRITE_STORAGE:{
		        boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (hasStoragePermission) {
					if (downloadCompleteFolder){
						MegaNode rootNode = null;	  
						if(megaApiFolder.getRootNode()!=null){
							rootNode = megaApiFolder.getRootNode();
						}
			        	if(rootNode!=null){
			        		onFolderClick(rootNode.getHandle(),rootNode.getSize());	
			        	}
			        	else{
							logWarning("rootNode null!!");
			        	}
					}
					else{
						onFileClick(handleListM);
					}
				}
				downloadCompleteFolder = false;
	        	break;
	        }
        }
    }
	
	@Override
	public void onBackPressed() {
		logDebug("onBackPressed");
		retryConnectionsAndSignalPresence();

		if (fileLinkFolderLink){
			fileLinkFragmentContainer.setVisibility(View.GONE);
			fragmentContainer.setVisibility(View.VISIBLE);
			setSupportActionBar(tB);
			aB = getSupportActionBar();
			aB.setDisplayHomeAsUpEnabled(true);
			aB.setDisplayShowHomeEnabled(true);
			fileLinkFolderLink = false;
			pN = null;
			MegaNode parentNode = megaApiFolder.getParentNode(megaApiFolder.getNodeByHandle(parentHandle));
			if (parentNode != null){
				logDebug("parentNode != NULL");
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				aB.setTitle(parentNode.getName());

				supportInvalidateOptionsMenu();

				parentHandle = parentNode.getHandle();
				nodes = megaApiFolder.getChildren(parentNode, orderGetChildren);
				adapterList.setNodes(nodes);
				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");

				if(lastVisiblePosition>=0){

					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);

				}
				adapterList.setParentHandle(parentHandle);
				return;
			}
			else{
				logWarning("parentNode == NULL");
				finish();
			}
		}

		if (adapterList != null){
			logDebug("adapter !=null");
			parentHandle = adapterList.getParentHandle();

			MegaNode parentNode = megaApiFolder.getParentNode(megaApiFolder.getNodeByHandle(parentHandle));
			if (parentNode != null){
				logDebug("parentNode != NULL");
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				aB.setTitle(parentNode.getName());

				supportInvalidateOptionsMenu();

				parentHandle = parentNode.getHandle();
				nodes = megaApiFolder.getChildren(parentNode, orderGetChildren);
				adapterList.setNodes(nodes);
				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");

				if(lastVisiblePosition>=0){

					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);

				}
				adapterList.setParentHandle(parentHandle);
				return;
			}
			else{
				logWarning("parentNode == NULL");
				finish();
			}
		}

		super.onBackPressed();
	}

	public void importNode(){
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);
		startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
	}

	public void downloadNode(){
		logDebug("Download option");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						REQUEST_WRITE_STORAGE);

				handleListM.clear();
				handleListM.add(selectedNode.getHandle());
				return;
			}
		}

		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(selectedNode.getHandle());
		onFileClick(handleList);
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		switch(v.getId()){
			case R.id.folder_link_file_link_button_download:
			case R.id.folder_link_button_download:{
				if (adapterList == null) {
					logWarning("No elements on list: adapterLIST is NULL");
					return;
				}

				if(adapterList.isMultipleSelect()){

					List<MegaNode> documents = adapterList.getSelectedNodes();
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
						if (!hasStoragePermission) {
							ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
							downloadCompleteFolder = false;
							handleListM.clear();
							for (int i=0;i<documents.size();i++){
								handleListM.add(documents.get(i).getHandle());
							}
							return;
						}
					}
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					onFileClick(handleList);
					clearSelections();
				} else {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
						if (!hasStoragePermission) {
							ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
							downloadCompleteFolder = true;
							return;
						}
					}

					MegaNode rootNode = null;
					if(megaApiFolder.getRootNode()!=null){
						rootNode = megaApiFolder.getRootNode();
					}
					if(rootNode!=null){
						MegaNode parentNode = megaApiFolder.getNodeByHandle(parentHandle);
						if (parentNode != null){
							onFolderClick(parentNode.getHandle(),parentNode.getSize());
						}else{
							onFolderClick(rootNode.getHandle(),rootNode.getSize());
						}
					}else{
						logWarning("rootNode null!!");
					}
				}
				break;
			}
			case R.id.folder_link_file_link_button_import:
			case R.id.folder_link_import_button:{
				if (megaApiFolder.getRootNode() != null){
					if (fileLinkFolderLink){
						if (pN != null){
							this.selectedNode = pN;
						}
					}
					else {
						this.selectedNode = megaApiFolder.getRootNode();
					}
					importNode();
				}
				break;
			}
		}			
	}



	public void showOptionsPanel(MegaNode sNode){
		logDebug("showNodeOptionsPanel-Offline");
		if(sNode!=null){
			this.selectedNode = sNode;
			FolderLinkBottomSheetDialogFragment bottomSheetDialogFragment = new FolderLinkBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void showSnackbar(int type, String s){
		logDebug("showSnackbar");
		showSnackbar(type, fragmentContainer, s);
	}

	public MegaNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(MegaNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public void errorOverquota() {
		Intent intent = new Intent(this, ManagerActivityLollipop.class);
		intent.setAction(ACTION_OVERQUOTA_STORAGE);
		startActivity(intent);
		finish();
	}

	public void errorPreOverquota() {
		Intent intent = new Intent(this, ManagerActivityLollipop.class);
		intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
		startActivity(intent);
		finish();
	}

	public void successfulCopy(){

		Intent startIntent = new Intent(this, ManagerActivityLollipop.class);
		if(toHandle!=-1){
			startIntent.setAction(ACTION_OPEN_FOLDER);
			startIntent.putExtra("PARENT_HANDLE", toHandle);
			startIntent.putExtra("offline_adapter", false);
			startIntent.putExtra("locationFileInfo", true);
			startIntent.putExtra("fragmentHandle", fragmentHandle);
		}
		startActivity(startIntent);
		clearSelections();
		hideMultipleSelect();

		try{
			statusDialog.dismiss();
		} catch(Exception ex){}

		finish();
	}
}
