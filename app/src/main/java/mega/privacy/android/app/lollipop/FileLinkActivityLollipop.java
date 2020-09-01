package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
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
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.io.File;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.TransfersManagementActivity;
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

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_CLOSE_CHAT_AFTER_IMPORT;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class FileLinkActivityLollipop extends TransfersManagementActivity implements MegaRequestListenerInterface, OnClickListener,DecryptAlertDialog.DecryptDialogListener {

	private static final String TAG_DECRYPT = "decrypt";

	FileLinkActivityLollipop fileLinkActivity = this;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	Toolbar tB;
    ActionBar aB;
	DisplayMetrics outMetrics;
	String url;
	Handler handler;
	ProgressDialog statusDialog;

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

	private String mKey;

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

		setTransfersWidgetLayout(findViewById(R.id.transfers_widget_layout));

		registerTransfersReceiver();

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
		getMenuInflater().inflate(R.menu.file_folder_link_action, menu);

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
				shareLink(this, url);
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	public void askForDecryptionKeyDialog(){
		logDebug("askForDecryptionKeyDialog");

		DecryptAlertDialog.Builder builder = new DecryptAlertDialog.Builder();
		builder.setListener(this).setTitle(getString(R.string.alert_decryption_key))
				.setPosText(R.string.general_decryp).setNegText(R.string.general_cancel)
				.setMessage(getString(R.string.message_decryption_key))
				.setErrorMessage(R.string.invalid_decryption_key).setKey(mKey)
				.build().show(getSupportFragmentManager(), TAG_DECRYPT);
	}

	private void decrypt() {
		if (TextUtils.isEmpty(mKey)) return;
		String urlWithKey = "";

		if (url.contains("#!")) {
			// old folder link format
			if (mKey.startsWith("!")) {
				logDebug("Decryption key with exclamation!");
				urlWithKey = url + mKey;
			} else {
				urlWithKey = url + "!" + mKey;
			}
		} else if (url.contains(SEPARATOR + "file" + SEPARATOR)) {
			// new folder link format
			if (mKey.startsWith("#")) {
				logDebug("Decryption key with hash!");
				urlWithKey = url + mKey;
			} else {
				urlWithKey = url + "#" + mKey;
			}
		}

		logDebug("File link to import: " + urlWithKey);
		decryptionIntroduced = true;
		importLink(urlWithKey);
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
			} catch (Exception ex) {
			}

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

				if (document.getHandle() != MegaApiJava.INVALID_HANDLE) {
					dbH.setLastPublicHandle(document.getHandle());
					dbH.setLastPublicHandleTimeStamp();
					dbH.setLastPublicHandleType(MegaApiJava.AFFILIATE_TYPE_FILE_FOLDER);
				}

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
			intent.putExtra(URL_FILE_LINK, url);
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
				mediaIntent.putExtra(URL_FILE_LINK, url);
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
			pdfIntent.putExtra(URL_FILE_LINK, url);

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
        } else if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
			if (!isOnline(this)) {
				try {
					statusDialog.dismiss();
				} catch (Exception ex) {
				}

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
        NodeController nC = new NodeController(this);
        nC.downloadFileLink(document, url);
	}

	public void successfulCopy(){
		if (getIntent() != null && getIntent().getBooleanExtra(OPENED_FROM_CHAT, false)) {
			sendBroadcast(new Intent(ACTION_CLOSE_CHAT_AFTER_IMPORT));
		}

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

	@Override
	public void onDialogPositiveClick(String key) {
		mKey = key;
		decrypt();
	}

	@Override
	public void onDialogNegativeClick() {
		finish();
	}
}