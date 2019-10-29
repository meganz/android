package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListenerLink;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class FileLinkActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, OnClickListener {
	
	FileLinkActivityLollipop fileLinkActivity = this;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	Toolbar tB;
    ActionBar aB;
	DisplayMetrics outMetrics;
	String url;
	Handler handler;
	ProgressDialog statusDialog;
	AlertDialog decryptionKeyDialog;

	MenuItem shareMenuItem;

	File previewFile = null;
	Bitmap preview = null;

	long toHandle = 0;
	long fragmentHandle = -1;
	int cont = 0;
	MultipleRequestListenerLink importLinkMultipleListener = null;

	CoordinatorLayout fragmentContainer;
	CollapsingToolbarLayout collapsingToolbar;
	ImageView iconView;
	ImageView imageView;
	RelativeLayout iconViewLayout;
	RelativeLayout imageViewLayout;

	ScrollView scrollView;
	TextView sizeTextView;
	TextView importButton;
	TextView downloadButton;
	Button buttonPreviewContent;
	LinearLayout optionsBar;
	MegaNode document = null;
	RelativeLayout infoLayout;
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	boolean decryptionIntroduced=false;

	boolean importClicked = false;
	MegaNode target = null;

	public static final int FILE_LINK = 1;

	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}
	
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
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		Intent intentReceived = getIntent();
		if (intentReceived != null){
			url = intentReceived.getDataString();
		}

		if (dbH.getCredentials() != null) {
			if (megaApi == null || megaApi.getRootNode() == null) {
				logDebug("Refresh session - sdk");
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
				intent.setData(Uri.parse(url));
				intent.setAction(ACTION_OPEN_FILE_LINK_ROOTNODES_NULL);
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
					intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
					intent.setData(Uri.parse(url));
					intent.setAction(ACTION_OPEN_FILE_LINK_ROOTNODES_NULL);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
					return;
				}
			}
		}

		setContentView(R.layout.activity_file_link);
		fragmentContainer = (CoordinatorLayout) findViewById(R.id.file_link_fragment_container);

		collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.file_link_info_collapse_toolbar);

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			collapsingToolbar.setExpandedTitleMarginBottom(scaleHeightPx(60, outMetrics));
		}else{
			collapsingToolbar.setExpandedTitleMarginBottom(scaleHeightPx(35, outMetrics));
		}
		collapsingToolbar.setExpandedTitleMarginStart((int) getResources().getDimension(R.dimen.recycler_view_separator));
		tB = (Toolbar) findViewById(R.id.toolbar_file_link);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		aB.setDisplayShowTitleEnabled(false);

		aB.setHomeButtonEnabled(true);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);

		/*Icon & image in Toolbar*/
		iconViewLayout = (RelativeLayout) findViewById(R.id.file_link_icon_layout);
		iconView = (ImageView) findViewById(R.id.file_link_icon);

		imageViewLayout = (RelativeLayout) findViewById(R.id.file_info_image_layout);
		imageView = (ImageView) findViewById(R.id.file_info_toolbar_image);
		imageViewLayout.setVisibility(View.GONE);

		/*Elements*/
		sizeTextView = (TextView) findViewById(R.id.file_link_size);
		buttonPreviewContent = (Button) findViewById(R.id.button_preview_content);
		buttonPreviewContent.setOnClickListener(this);
		buttonPreviewContent.setEnabled(false);

		buttonPreviewContent.setVisibility(View.GONE);

		downloadButton = (TextView) findViewById(R.id.file_link_button_download);
		downloadButton.setText(getString(R.string.general_save_to_device).toUpperCase(Locale.getDefault()));
		downloadButton.setOnClickListener(this);
		downloadButton.setVisibility(View.INVISIBLE);

		importButton = (TextView) findViewById(R.id.file_link_button_import);
		importButton.setText(getString(R.string.add_to_cloud).toUpperCase(Locale.getDefault()));
		importButton.setOnClickListener(this);
		importButton.setVisibility(View.GONE);

		try{
			statusDialog.dismiss();
		}
		catch(Exception e){	}

		if(url!=null){
			importLink(url);
		}
		else{
			logWarning("url NULL");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.file_link_action, menu);
		shareMenuItem = menu.findItem(R.id.share_link);
		shareMenuItem.setVisible(true);
		menu.findItem(R.id.share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));
		collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.white));
//		collapsingToolbar.setStatusBarScrimColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));

		return super.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		logDebug("onOptionsItemSelected");

		int id = item.getItemId();
		switch (id) {
			case android.R.id.home: {
				finish();
				break;
			}
			case R.id.share_link: {
				if(url!=null){
					shareLink(url);
				}else{
					logWarning("url NULL");
				}
				break;
			}
		}
		return super.onOptionsItemSelected(item);
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
		input.setHint(getString(R.string.password_text));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
					logDebug("File link to import: " + url);
					decryptionIntroduced=true;
					importLink(url);
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
							logDebug("File link to import: " + url);
							decryptionIntroduced=true;
							importLink(url);
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
	protected void onResume() {
    	super.onResume();

    	Intent intent = getIntent();
    	
    	if (intent != null){
    		if (intent.getAction() != null){
    			if (intent.getAction().equals(ACTION_IMPORT_LINK_FETCH_NODES)){
    				importNode();
    			}
    			intent.setAction(null);
    		}
    	}
    	setIntent(null);
	}
	
	private void importLink(String url) {

		if(!isOnline(this))
		{
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
			return;
		}

		if(this.isFinishing()) return;
		
		ProgressDialog temp = null;
		try {
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.general_loading));
			temp.show();
		}
		catch(Exception ex)
		{ return; }
		
		statusDialog = temp;
		
		megaApi.getPublicNode(url, this);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestUpdate: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish: " + request.getRequestString()+ " code: "+e.getErrorCode());
		if (request.getType() == MegaRequest.TYPE_GET_PUBLIC_NODE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK) {
				document = request.getPublicMegaNode();

				if (document == null){
					logWarning("documment==null --> Intent to ManagerActivityLollipop");
					boolean closedChat = MegaApplication.isClosedChat();
					if(closedChat){
						Intent backIntent = new Intent(this, ManagerActivityLollipop.class);
						startActivity(backIntent);
					}

	    			finish();
					return;
				}

				logDebug("DOCUMENTNODEHANDLEPUBLIC: " + document.getHandle());
				if (dbH == null){
					dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				}

				dbH.setLastPublicHandle(document.getHandle());
				dbH.setLastPublicHandleTimeStamp();

//				nameView.setText(document.getName());
				collapsingToolbar.setTitle(document.getName());

				sizeTextView.setText(getSizeString(document.getSize()));

				iconView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());
				iconViewLayout.setVisibility(View.VISIBLE);
				downloadButton.setVisibility(View.VISIBLE);

				if(dbH.getCredentials() == null){
					importButton.setVisibility(View.GONE);
				}
				else{
					importButton.setVisibility(View.VISIBLE);
				}

				preview = getPreviewFromCache(document);
				if (preview != null){
					previewCache.put(document.getHandle(), preview);
					imageView.setImageBitmap(preview);
					imageViewLayout.setVisibility(View.VISIBLE);
					iconViewLayout.setVisibility(View.GONE);
					buttonPreviewContent.setVisibility(View.VISIBLE);
					buttonPreviewContent.setEnabled(true);

				}else{

					preview = getPreviewFromFolder(document, this);
					if (preview != null){
						previewCache.put(document.getHandle(), preview);
						imageView.setImageBitmap(preview);
						imageViewLayout.setVisibility(View.VISIBLE);
						iconViewLayout.setVisibility(View.GONE);
						buttonPreviewContent.setVisibility(View.VISIBLE);
						buttonPreviewContent.setEnabled(true);

					}else{

						if (document.hasPreview()) {
							previewFile = new File(getPreviewFolder(this), document.getBase64Handle() + ".jpg");
							megaApi.getPreview(document, previewFile.getAbsolutePath(), this);
							buttonPreviewContent.setVisibility(View.VISIBLE);
						}else{
							if (MimeTypeList.typeForName(document.getName()).isVideoReproducible() || MimeTypeList.typeForName(document.getName()).isAudio() || MimeTypeList.typeForName(document.getName()).isPdf()){
								imageViewLayout.setVisibility(View.GONE);
								iconViewLayout.setVisibility(View.VISIBLE);

								buttonPreviewContent.setVisibility(View.VISIBLE);
								buttonPreviewContent.setEnabled(true);
							}
							else{
								buttonPreviewContent.setEnabled(false);
								buttonPreviewContent.setVisibility(View.GONE);

								imageViewLayout.setVisibility(View.GONE);
								iconViewLayout.setVisibility(View.VISIBLE);
							}
						}
					}
				}

				if (importClicked){
					if ((document != null) && (target != null)){
						megaApi.copyNode(document, target, this);
					}
				}
			}
			else{
				logWarning("ERROR: " + e.getErrorCode());
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
				dialogBuilder.setCancelable(false);
				if(e.getErrorCode() == MegaError.API_EBLOCKED){
					dialogBuilder.setMessage(getString(R.string.file_link_unavaible_ToS_violation));
					dialogBuilder.setTitle(getString(R.string.general_error_file_not_found));
				}
				else if(e.getErrorCode() == MegaError.API_EARGS){
					if(decryptionIntroduced){
						logWarning("Incorrect key, ask again!");
						decryptionIntroduced=false;
						askForDecryptionKeyDialog();
						return;
					}
					else{
						//Link no valido
						dialogBuilder.setTitle(getString(R.string.general_error_word));
						dialogBuilder.setMessage(getString(R.string.link_broken));
					}
				}
				else if(e.getErrorCode() == MegaError.API_ETOOMANY){
					dialogBuilder.setMessage(getString(R.string.file_link_unavaible_delete_account));
					dialogBuilder.setTitle(getString(R.string.general_error_file_not_found));
				}
				else if(e.getErrorCode() == MegaError.API_EINCOMPLETE){
					decryptionIntroduced=false;
					askForDecryptionKeyDialog();
					return;
				}
				else{
					dialogBuilder.setTitle(getString(R.string.general_error_word));
					dialogBuilder.setMessage(getString(R.string.general_error_file_not_found));

					if(e.getErrorCode() == MegaError.API_ETEMPUNAVAIL){
						logWarning("ERROR: " + MegaError.API_ETEMPUNAVAIL);
					}
				}

				try{
					dialogBuilder.setPositiveButton(getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									boolean closedChat = MegaApplication.isClosedChat();
									if(closedChat){
										Intent backIntent = new Intent(fileLinkActivity, ManagerActivityLollipop.class);
										startActivity(backIntent);
									}

									finish();
								}
							});

					AlertDialog dialog = dialogBuilder.create();
					dialog.show();
				}
				catch(Exception ex){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error_file_not_found));
				}

				return;
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE){
			if (e.getErrorCode() == MegaError.API_OK){
				File previewDir = getPreviewFolder(this);
				if (document != null){
					File preview = new File(previewDir, document.getBase64Handle()+".jpg");
					if (preview.exists()) {
						if (preview.length() > 0) {
							Bitmap bitmap = getBitmapForCache(preview, this);
							previewCache.put(document.getHandle(), bitmap);
							if (iconView != null) {
								imageView.setImageBitmap(bitmap);
								buttonPreviewContent.setEnabled(true);
								imageViewLayout.setVisibility(View.VISIBLE);
								iconViewLayout.setVisibility(View.GONE);
							}
						}
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			
			try{
				statusDialog.dismiss(); 
			} catch(Exception ex){};

			if (e.getErrorCode() != MegaError.API_OK) {

				logDebug("e.getErrorCode() != MegaError.API_OK");
				
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
				else
				{
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
			        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent);
					finish();
				}							
				
			}else{
				Intent intent = new Intent(this, ManagerActivityLollipop.class);
		        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent);
				finish();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getRequestString());
	}

	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.file_link_button_download:{
				NodeController nC = new NodeController(this);
				nC.downloadFileLink(document, url);
				break;
			}
			case R.id.file_link_button_import:{
				importNode();
				break;
			}
			case R.id.button_preview_content:{
				showFile();
				break;
			}
		}
	}
	
	public void importNode(){

		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);
		startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
	}

	public void showFile(){
		logDebug("showFile");
		String serializeString = document.serialize();
		if(MimeTypeList.typeForName(document.getName()).isImage()){
			logDebug("Is image");
			Intent intent = new Intent(this, FullScreenImageViewerLollipop.class);
			intent.putExtra(EXTRA_SERIALIZE_STRING, serializeString);
			intent.putExtra("position", 0);
			intent.putExtra("urlFileLink",url);
			intent.putExtra("adapterType", FILE_LINK_ADAPTER);
			intent.putExtra("parentNodeHandle", -1L);
			intent.putExtra("orderGetChildren", MegaApiJava.ORDER_DEFAULT_ASC);
			intent.putExtra("isFileLink", true);
			startActivity(intent);

		}else if (MimeTypeList.typeForName(document.getName()).isVideoReproducible() || MimeTypeList.typeForName(document.getName()).isAudio() ){
			logDebug("Is video");

			String mimeType = MimeTypeList.typeForName(document.getName()).getType();
			logDebug("NODE HANDLE: " + document.getHandle() + ", TYPE: " + mimeType);

			Intent mediaIntent;
			boolean internalIntent;
			boolean opusFile = false;
			if (MimeTypeList.typeForName(document.getName()).isVideoNotSupported() || MimeTypeList.typeForName(document.getName()).isAudioNotSupported()) {
				mediaIntent = new Intent(Intent.ACTION_VIEW);
				internalIntent = false;
				String[] s = document.getName().split("\\.");
				if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
					opusFile = true;
				}
			} else {
				logDebug("setIntentToAudioVideoPlayer");
				mediaIntent = new Intent(this, AudioVideoPlayerLollipop.class);
				mediaIntent.putExtra("adapterType", FILE_LINK_ADAPTER);
				mediaIntent.putExtra(EXTRA_SERIALIZE_STRING, serializeString);
				internalIntent = true;
			}
			mediaIntent.putExtra("FILENAME", document.getName());

			if (megaApi.httpServerIsRunning() == 0) {
				megaApi.httpServerStart();
			} else {
				logWarning("ERROR: HTTP server already running");
			}

			ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
			ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
			activityManager.getMemoryInfo(mi);

			if (mi.totalMem > BUFFER_COMP) {
				logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
				megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
			} else {
				logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
				megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
			}

			String url = megaApi.httpServerGetLocalLink(document);
			if (url != null) {
				Uri parsedUri = Uri.parse(url);
				if (parsedUri != null) {
					mediaIntent.setDataAndType(parsedUri, mimeType);
				} else {
					logWarning("ERROR: HTTP server get local link");
					showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
				}
			} else {
				logWarning("ERROR: HTTP server get local link");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
			}

			mediaIntent.putExtra("HANDLE", document.getHandle());
			if (opusFile){
				mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
			}
			if (internalIntent) {
				startActivity(mediaIntent);
			} else {
				logDebug("External Intent");
				if (isIntentAvailable(this, mediaIntent)) {
					startActivity(mediaIntent);
				} else {
					logDebug("No Available Intent");
					showSnackbar(SNACKBAR_TYPE, "NoApp available");
				}
			}

		}else if(MimeTypeList.typeForName(document.getName()).isPdf()){
			logDebug("Is pdf");

			String mimeType = MimeTypeList.typeForName(document.getName()).getType();
			logDebug("NODE HANDLE: " + document.getHandle() + ", TYPE: " + mimeType);
			Intent pdfIntent = new Intent(this, PdfViewerActivityLollipop.class);
			pdfIntent.putExtra("adapterType", FILE_LINK_ADAPTER);
			pdfIntent.putExtra(EXTRA_SERIALIZE_STRING, serializeString);
			pdfIntent.putExtra("inside", true);
			pdfIntent.putExtra("FILENAME", document.getName());

			if (isOnline(this)){
				if (megaApi.httpServerIsRunning() == 0) {
					megaApi.httpServerStart();
				}
				else{
					logWarning("ERROR: HTTP server already running");
				}
				ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
				ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
				activityManager.getMemoryInfo(mi);
				if(mi.totalMem>BUFFER_COMP){
					logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
				}
				else{
					logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
				}
				String url = megaApi.httpServerGetLocalLink(document);
				if(url!=null){
					Uri parsedUri = Uri.parse(url);
					if(parsedUri!=null){
						pdfIntent.setDataAndType(parsedUri, mimeType);
					}
					else{
						logDebug("ERROR: HTTP server get local link");
						showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
					}
				}
				else{
					logDebug("ERROR: HTTP server get local link");
					showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
				}
			}
			else {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem)+". "+ getString(R.string.no_network_connection_on_play_file));
			}

			pdfIntent.putExtra("HANDLE", document.getHandle());

			if (isIntentAvailable(this, pdfIntent)){
				startActivity(pdfIntent);
			}
			else{
				logWarning("No Available Intent");
			}
		}else{
			logWarning("none");
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (intent == null) {
			return;
		}
		
		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			logDebug("Local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			logDebug("URL: " + url + ", SIZE: " + size);

			NodeController nC = new NodeController(this);
			nC.downloadTo(document, parentPath, url);
		}
		else if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
			if (!isOnline(this)) {
				try {
					statusDialog.dismiss();
				} catch (Exception ex) {
				}
				;

				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			toHandle = intent.getLongExtra("IMPORT_TO", 0);
			fragmentHandle = intent.getLongExtra("fragmentH", -1);

			MegaNode target = megaApi.getNodeByHandle(toHandle);
			if (target == null) {
				if (megaApi.getRootNode() != null) {
					target = megaApi.getRootNode();
				}
			}

			statusDialog = new ProgressDialog(this);
			statusDialog.setMessage(getString(R.string.general_importing));
			statusDialog.show();

			if (document != null) {
				if (target != null) {
					logDebug("Target node: " + target.getHandle());
					cont++;
					importLinkMultipleListener = new MultipleRequestListenerLink(this, cont, cont, FILE_LINK);
					megaApi.copyNode(document, target, importLinkMultipleListener);
				} else {
					logWarning("TARGET == null");
				}
			} else {
				logWarning("Selected Node is NULL");
				if (target != null) {
					importClicked = true;
				}
			}

		}

	}

	public void showSnackbar(int type, String s){
		showSnackbar(type, fragmentContainer, s);
	}
	
	@SuppressLint("NewApi") 
	public void downloadWithPermissions(){
		logDebug("downloadWithPermissions");
		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}

		if (document == null) {
			return;
		}
		
		if (dbH.getCredentials() == null || dbH.getPreferences() == null){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						intentPickFolder();
					}
					else{
						Dialog downloadLocationDialog;
						String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
				        AlertDialog.Builder b=new AlertDialog.Builder(this);
	
						b.setTitle(getResources().getString(R.string.settings_storage_download_location));
						b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which){
									case 0:{
										intentPickFolder();
										break;
									}
									case 1:{
										File[] fs = getExternalFilesDirs(null);
										if (fs.length > 1){
											String path = fs[1].getAbsolutePath();
											File defaultPathF = new File(path);
											defaultPathF.mkdirs();
											Toast.makeText(getApplicationContext(), getString(R.string.general_save_to_device) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
											NodeController nC = new NodeController(fileLinkActivity);
											nC.downloadFileLink(document, url);
										}
										break;
									}
								}
							}
						});
						b.setNegativeButton(getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						});
						downloadLocationDialog = b.create();
						downloadLocationDialog.show();
					}
				}
				else{
					intentPickFolder();
				}
			}
			else{
				intentPickFolder();
			}	
			return;
		}
		
		boolean askMe = askMe(this);
		
		if (askMe){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						intentPickFolder();
					}
					else{
						Dialog downloadLocationDialog;
						String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
				        AlertDialog.Builder b=new AlertDialog.Builder(this);
	
						b.setTitle(getResources().getString(R.string.settings_storage_download_location));
						b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which){
									case 0:{
										intentPickFolder();
										break;
									}
									case 1:{
										File[] fs = getExternalFilesDirs(null);
										if (fs.length > 1){
											String path = fs[1].getAbsolutePath();
											File defaultPathF = new File(path);
											defaultPathF.mkdirs();
											Toast.makeText(getApplicationContext(), getString(R.string.general_save_to_device) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
											NodeController nC = new NodeController(getApplicationContext());
											nC.downloadFileLink(document, url);
										}
										break;
									}
								}
							}
						});
						b.setNegativeButton(getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						});
						downloadLocationDialog = b.create();
						downloadLocationDialog.show();
					}
				}
				else{
					intentPickFolder();
				}
			}
			else{
				intentPickFolder();
			}
		}
		else{
			NodeController nC = new NodeController(this);
			nC.downloadFileLink(document, url);
		}
	}

	void intentPickFolder () {
		Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
		intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
		intent.setClass(this, FileStorageActivityLollipop.class);
		intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
		intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
		startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
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

		try{
			statusDialog.dismiss();
		} catch(Exception ex){}

		finish();
	}
	
	@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
        	case REQUEST_WRITE_STORAGE:{
		        boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (hasStoragePermission) {
					downloadWithPermissions();
				}
	        	break;
	        }
        }
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

	public void shareLink(String link){
		logDebug("Link: " + link);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, link);
		startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
	}

}