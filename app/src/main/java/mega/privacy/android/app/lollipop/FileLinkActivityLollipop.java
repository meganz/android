package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListenerLink;
import mega.privacy.android.app.snackbarListeners.SnackbarNavigateOption;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

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

	long toHandle = 0;
	long fragmentHandle = -1;
	int cont = 0;
	MultipleRequestListenerLink importLinkMultipleListener = null;

	RelativeLayout fragmentContainer;
	ImageView iconView;
	TextView nameView;
	ScrollView scrollView;
	TextView sizeTextView;
	TextView sizeTitleView;
	TextView importButton;
	TextView downloadButton;
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
		log("onCreate()");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);	    
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		Intent intentReceived = getIntent();
		if (intentReceived != null){
			url = intentReceived.getDataString();
		}

		if (dbH.getCredentials() != null) {
			if (megaApi == null || megaApi.getRootNode() == null) {
				log("Refresh session - sdk");
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
				intent.setData(Uri.parse(url));
				intent.setAction(Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return;
			}
			if (Util.isChatEnabled()) {
				if (megaChatApi == null) {
					megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
				}

				if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
					log("Refresh session - karere");
					Intent intent = new Intent(this, LoginActivityLollipop.class);
					intent.putExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
					intent.setData(Uri.parse(url));
					intent.setAction(Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
					return;
				}
			}
		}

		setContentView(R.layout.activity_file_link);
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_file_link);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		if(aB!=null){
			//		aB.setLogo(R.drawable.ic_arrow_back_black);
			aB.setDisplayHomeAsUpEnabled(true);
			aB.setDisplayShowHomeEnabled(true);
			aB.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
			aB.setDisplayShowTitleEnabled(false);
		}

		fragmentContainer = (RelativeLayout) findViewById(R.id.file_link_fragment_container);
		
		infoLayout = (RelativeLayout) findViewById(R.id.file_link_layout);

		scrollView = (ScrollView) findViewById(R.id.file_link_scroll_layout);

		FrameLayout.LayoutParams infoLayoutParams = (FrameLayout.LayoutParams)infoLayout.getLayoutParams();
		infoLayoutParams.setMargins(0, 0, 0, Util.scaleHeightPx(80, outMetrics)); 		
		infoLayout.setLayoutParams(infoLayoutParams);
		
		iconView = (ImageView) findViewById(R.id.file_link_icon);		
		iconView.getLayoutParams().width = Util.scaleWidthPx(200, outMetrics);
		iconView.getLayoutParams().height = Util.scaleHeightPx(200, outMetrics);		
		
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) iconView.getLayoutParams();
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		iconView.setLayoutParams(params);

//		((RelativeLayout.LayoutParams)iconView.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((15*scaleH), outMetrics), 0, 0);
		
//		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//		lp.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(20, outMetrics));
//		iconView.setLayoutParams(lp);
		
		float scaleText;
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}
			
		nameView = (TextView) findViewById(R.id.file_link_name);
		nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleText));
		nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		nameView.setSingleLine();
		nameView.setTypeface(null, Typeface.BOLD);
		//Left margin
		RelativeLayout.LayoutParams nameViewParams = (RelativeLayout.LayoutParams)nameView.getLayoutParams();
		nameViewParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleHeightPx(20, outMetrics)); 		
		nameView.setLayoutParams(nameViewParams);
		
//		lp.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(20, outMetrics));
//		nameView.setLayoutParams(lp);
		sizeTitleView = (TextView) findViewById(R.id.file_link_info_menu_size);
		//Left margin, Top margin
		RelativeLayout.LayoutParams sizeTitleParams = (RelativeLayout.LayoutParams)sizeTitleView.getLayoutParams();
		sizeTitleParams.setMargins(Util.scaleWidthPx(10, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, 0); 		
		sizeTitleView.setLayoutParams(sizeTitleParams);
				
		sizeTextView = (TextView) findViewById(R.id.file_link_size);
		//Bottom margin
		RelativeLayout.LayoutParams sizeTextParams = (RelativeLayout.LayoutParams)sizeTextView.getLayoutParams();
		sizeTextParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, Util.scaleHeightPx(15, outMetrics)); 		
		sizeTextView.setLayoutParams(sizeTextParams);		
		
		optionsBar = (LinearLayout) findViewById(R.id.options_file_link_layout);

		downloadButton = (TextView) findViewById(R.id.file_link_button_download);
		downloadButton.setOnClickListener(this);
		downloadButton.setText(getString(R.string.general_download).toUpperCase(Locale.getDefault()));
//		android.view.ViewGroup.LayoutParams paramsb1 = downloadButton.getLayoutParams();
//		paramsb1.height = Util.scaleHeightPx(48, outMetrics);
//		paramsb1.width = Util.scaleWidthPx(83, outMetrics);
//		downloadButton.setLayoutParams(paramsb1);
		//Left and Right margin
		LinearLayout.LayoutParams cancelTextParams = (LinearLayout.LayoutParams)downloadButton.getLayoutParams();
		cancelTextParams.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(8, outMetrics), 0); 
		downloadButton.setLayoutParams(cancelTextParams);
		
		importButton = (TextView) findViewById(R.id.file_link_button_import);
		importButton.setText(getString(R.string.general_import).toUpperCase(Locale.getDefault()));	
		importButton.setOnClickListener(this);
//		android.view.ViewGroup.LayoutParams paramsb2 = importButton.getLayoutParams();
//		paramsb2.height = Util.scaleHeightPx(48, outMetrics);
//		paramsb2.width = Util.scaleWidthPx(73, outMetrics);
//		importButton.setLayoutParams(paramsb2);
		//Left and Right margin
		LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams)importButton.getLayoutParams();
		optionTextParams.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(8, outMetrics), 0); 
		importButton.setLayoutParams(optionTextParams);

//		RelativeLayout.LayoutParams paramsScroll = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
//		paramsScroll.height =  fragmentContainer.getHeight()-tB.getHeight()-optionsBar.getHeight();
//		scrollView.setLayoutParams(paramsScroll);
				
//		iconView.getLayoutParams().height = Util.px2dp((20*scaleH), outMetrics);
//		((LayoutParams)iconView.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((15*scaleH), outMetrics), 0, 0);
//		
//		((LayoutParams) sizeTextView.getLayoutParams()).setMargins(Util.px2dp((75*scaleW), outMetrics), 0, 0, 0);
		
		importButton.setVisibility(View.INVISIBLE);

		try{
			statusDialog.dismiss();
		}
		catch(Exception e){	}

		if(url!=null){
			importLink(url);
		}
		else{
			log("url NULL");
		}

		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}

	public void askForDecryptionKeyDialog(){
		log("askForDecryptionKeyDialog");

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

		input.setSingleLine();
		input.setTextColor(getResources().getColor(R.color.text_secondary));
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
						log("Decryption key with exclamation!");
						url=url+value;
					}
					else{
						url=url+"!"+value;
					}
					log("File link to import: "+url);
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
							return;
						}
						if(value.startsWith("!")){
							log("Decryption key with exclamation!");
							url=url+value;
						}
						else{
							url=url+"!"+value;
						}
						log("File link to import: "+url);
						decryptionIntroduced=true;
						importLink(url);
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
		log("showKeyboardDelayed");
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

		((MegaApplication) getApplication()).sendSignalPresenceActivity();
    	
    	Intent intent = getIntent();
    	
    	if (intent != null){
    		if (intent.getAction() != null){
    			if (intent.getAction().equals(Constants.ACTION_IMPORT_LINK_FETCH_NODES)){
    				importNode();
    			}
    			intent.setAction(null);
    		}
    	}
    	setIntent(null);

		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}	
	
	private void importLink(String url) {

		if(!Util.isOnline(this))
		{
			Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
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

	public static void log(String message) {
		Util.log("FileLinkActivityLollipop", message);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_GET_PUBLIC_NODE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK) {
				document = request.getPublicMegaNode();
				
				if (document == null){
					log("documment==null --> Intent to ManagerActivityLollipop");
					Intent backIntent = new Intent(this, ManagerActivityLollipop.class);
	    			startActivity(backIntent);
	    			finish();
					return;
				}

				nameView.setText(document.getName());
				sizeTextView.setText(Formatter.formatFileSize(this, document.getSize()));

				iconView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());

				downloadButton.setVisibility(View.VISIBLE);

				if(dbH.getCredentials() == null){
					importButton.setVisibility(View.INVISIBLE);
				}
				else{
					importButton.setVisibility(View.VISIBLE);
				}

				Bitmap preview = null;
				preview = PreviewUtils.getPreviewFromCache(document);
				if (preview != null){
					PreviewUtils.previewCache.put(document.getHandle(), preview);
					iconView.setImageBitmap(preview);
					iconView.setOnClickListener(this);
				}
				else{
					preview = PreviewUtils.getPreviewFromFolder(document, this);
					if (preview != null){
						PreviewUtils.previewCache.put(document.getHandle(), preview);
						iconView.setImageBitmap(preview);
						iconView.setOnClickListener(this);
					}
					else{
						if (document.hasPreview()) {
							File previewFile = new File(PreviewUtils.getPreviewFolder(this), document.getBase64Handle() + ".jpg");
							megaApi.getPreview(document, previewFile.getAbsolutePath(), this);
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
				log("ERROR: " + e.getErrorCode());
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
				if(e.getErrorCode() == MegaError.API_EBLOCKED){
					dialogBuilder.setMessage(getString(R.string.file_link_unavaible_ToS_violation));
					dialogBuilder.setTitle(getString(R.string.general_error_file_not_found));
				}
				else if(e.getErrorCode() == MegaError.API_EARGS){
					if(decryptionIntroduced){
						log("incorrect key, ask again!");
						decryptionIntroduced=false;
						askForDecryptionKeyDialog();
						return;
					}
					else{
						//Link no valido
						dialogBuilder.setTitle(getString(R.string.general_error_word));
						dialogBuilder.setMessage(getString(R.string.general_error_file_not_found));
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
						log("ERROR: "+MegaError.API_ETEMPUNAVAIL);
					}
				}

				try{
					dialogBuilder.setPositiveButton(getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									Intent backIntent = new Intent(fileLinkActivity, ManagerActivityLollipop.class);
									startActivity(backIntent);
									finish();
								}
							});

					AlertDialog dialog = dialogBuilder.create();
					dialog.show();
				}
				catch(Exception ex){
					Snackbar.make(fragmentContainer, getString(R.string.general_error_file_not_found), Snackbar.LENGTH_LONG).show();
				}

				return;
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE){
			if (e.getErrorCode() == MegaError.API_OK){
				File previewDir = PreviewUtils.getPreviewFolder(this);
				if (document != null){
					File preview = new File(previewDir, document.getBase64Handle()+".jpg");
					if (preview.exists()) {
						if (preview.length() > 0) {
							Bitmap bitmap = PreviewUtils.getBitmapForCache(preview, this);
							PreviewUtils.previewCache.put(document.getHandle(), bitmap);
							if (iconView != null) {
								iconView.setImageBitmap(bitmap);
								iconView.setOnClickListener(this);
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
				
				log("e.getErrorCode() != MegaError.API_OK");
				
				if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					log("OVERQUOTA ERROR: "+e.getErrorCode());					
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();

				}
				else
				{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
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
		log("onRequestTemporaryError: " + request.getRequestString());
	}

	@Override
	public void onClick(View v) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
		switch(v.getId()){
			case R.id.file_link_button_download:{
				downloadNode();
				break;
			}
			case R.id.file_link_button_import:{
				importNode();
				break;
			}
			case R.id.file_link_icon:{

				break;
			}
		}
	}
	
	void importNode(){

		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);
		startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER);
	}
	
	String urlM;
	long sizeM;
	long [] hashesM;
	MegaNode documentM;
	
	@SuppressLint("NewApi") 
	void downloadNode(){
		
		if (document == null){
			try{ 
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
				dialogBuilder.setTitle(getString(R.string.general_error_word));
				dialogBuilder.setMessage(getString(R.string.general_error_file_not_found));				
				dialogBuilder.setPositiveButton(
					getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							Intent backIntent = new Intent(fileLinkActivity, ManagerActivityLollipop.class);
			    			startActivity(backIntent);
			    			finish();
						}
					});
								
				AlertDialog dialog = dialogBuilder.create();
				dialog.show(); 
			}
			catch(Exception ex){
				Snackbar.make(fragmentContainer, getString(R.string.general_error_file_not_found), Snackbar.LENGTH_LONG).show();
			}
			
			return;
		}
		
		long[] hashes = new long[1];
		hashes[0]=document.getHandle();
		long size = document.getSize();
		
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
				this.urlM = url;
				this.sizeM = size;
				this.hashesM = new long[hashes.length];
				for (int i=0; i< hashes.length; i++){
					this.hashesM[i] = hashes[i];
				}
				this.documentM = document;

				return;
			}
		}
		
		
		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		
		if (dbH.getCredentials() == null || dbH.getPreferences() == null){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
						intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
						intent.setClass(this, FileStorageActivityLollipop.class);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
						startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
					}
					else{
						Dialog downloadLocationDialog;
						String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
				        AlertDialog.Builder b=new AlertDialog.Builder(this);
	
						b.setTitle(getResources().getString(R.string.settings_storage_download_location));
						final long sizeFinal = size;
						final long[] hashesFinal = new long[hashes.length];
						for (int i=0; i< hashes.length; i++){
							hashesFinal[i] = hashes[i];
						}
						
						b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which){
									case 0:{
										Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
										intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
										intent.setClass(getApplicationContext(), FileStorageActivityLollipop.class);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
										startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
										break;
									}
									case 1:{
										File[] fs = getExternalFilesDirs(null);
										if (fs.length > 1){
											String path = fs[1].getAbsolutePath();
											File defaultPathF = new File(path);
											defaultPathF.mkdirs();
											Toast.makeText(getApplicationContext(), getString(R.string.general_download) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
											downloadTo(path, url, sizeFinal, hashesFinal);
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
					Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
					intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
					intent.setClass(this, FileStorageActivityLollipop.class);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
					startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
				}
			}
			else{
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
				intent.setClass(this, FileStorageActivityLollipop.class);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
				startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}	
			return;
		}
		
		boolean askMe = true;
		String downloadLocationDefaultPath = "";
		prefs = dbH.getPreferences();		
		if (prefs != null){
			if (prefs.getStorageAskAlways() != null){
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							askMe = false;
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}
		
		if (askMe){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
						intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
						intent.setClass(this, FileStorageActivityLollipop.class);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
						startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
					}
					else{
						Dialog downloadLocationDialog;
						String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
				        AlertDialog.Builder b=new AlertDialog.Builder(this);
	
						b.setTitle(getResources().getString(R.string.settings_storage_download_location));
						final long sizeFinal = size;
						final long[] hashesFinal = new long[hashes.length];
						for (int i=0; i< hashes.length; i++){
							hashesFinal[i] = hashes[i];
						}
						
						b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which){
									case 0:{
										Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
										intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
										intent.setClass(getApplicationContext(), FileStorageActivityLollipop.class);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
										startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
										break;
									}
									case 1:{
										File[] fs = getExternalFilesDirs(null);
										if (fs.length > 1){
											String path = fs[1].getAbsolutePath();
											File defaultPathF = new File(path);
											defaultPathF.mkdirs();
											Toast.makeText(getApplicationContext(), getString(R.string.general_download) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
											downloadTo(path, url, sizeFinal, hashesFinal);
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
					Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
					intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
					intent.setClass(this, FileStorageActivityLollipop.class);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
					startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
				}
			}
			else{
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
				intent.setClass(this, FileStorageActivityLollipop.class);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
				startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}
		}
		else{
			downloadTo(downloadLocationDefaultPath, url, size, hashes);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    	// Respond to the action bar's Up/Home button
		    case android.R.id.home:{
		    	finish();
		    	return true;
		    }
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		
		if (intent == null) {
			return;
		}
		
		if (requestCode == Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);
			
			downloadTo (parentPath, url, size, hashes);
//			Snackbar.make(fragmentContainer, getString(R.string.download_began), Snackbar.LENGTH_LONG).show();
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
			if (!Util.isOnline(this)) {
				try {
					statusDialog.dismiss();
				} catch (Exception ex) {
				}
				;

				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
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
					log("Target node: " + target.getName());
					cont++;
					importLinkMultipleListener = new MultipleRequestListenerLink(this, cont, cont, FILE_LINK);
					megaApi.copyNode(document, target, importLinkMultipleListener);
				} else {
					log("TARGET == null");
				}
			} else {
				log("selected Node is NULL");
				if (target != null) {
					importClicked = true;
				}
			}

		}

	}

//	int numberOfNodesToDownload = 0;
//	int numberOfNodesAlreadyDownloaded = 0;
//	int numberOfNodesPending = 0;
//
//	public void downloadTo(String parentPath, String url, long size, long [] hashes){
//		log("downloadTo");
//		double availableFreeSpace = Double.MAX_VALUE;
//		try{
//			StatFs stat = new StatFs(parentPath);
//			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
//		}
//		catch(Exception ex){}
//
//		if (documentM != null){
//			return;
//		}
//		else if (document != null){
//			if(document.getType() == MegaNode.TYPE_FILE){
//				log("ISFILE");
//				String localPath = Util.getLocalFile(this, document.getName(), document.getSize(), parentPath);
//				if(localPath != null){
//					try {
//						Util.copyFile(new File(localPath), new File(parentPath, document.getName()));
//					}
//					catch(Exception e) {}
//
//					try {
//						Intent viewIntent = new Intent(Intent.ACTION_VIEW);
//						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//							viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(document.getName()).getType());
//						} else {
//							viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(document.getName()).getType());
//						}
//						viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//						if (MegaApiUtils.isIntentAvailable(this, viewIntent))
//							startActivity(viewIntent);
//						else {
//							Intent intentShare = new Intent(Intent.ACTION_SEND);
//							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//								intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(document.getName()).getType());
//							} else {
//								intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(document.getName()).getType());
//							}
//							intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//							if (MegaApiUtils.isIntentAvailable(this, intentShare))
//								startActivity(intentShare);
//							String toastMessage = getString(R.string.general_already_downloaded) + ": " + localPath;
//							Snackbar.make(fragmentContainer, toastMessage, Snackbar.LENGTH_LONG).show();
//						}
//					}
//					catch (Exception e){
//						String toastMessage = getString(R.string.general_already_downloaded) + ": " + localPath;
//						Snackbar.make(fragmentContainer, toastMessage, Snackbar.LENGTH_LONG).show();
//					}
//					log("Finish");
//					finish();
//					return;
//				}
//				else{
//					log("LocalPath is NULL");
//				}
//
//				log("path of the file: "+parentPath);
//				numberOfNodesToDownload++;
//
//				File destDir = new File(parentPath);
//				File destFile;
//				destDir.mkdirs();
//				if (destDir.isDirectory()){
//					destFile = new File(destDir, megaApi.escapeFsIncompatible(document.getName()));
//					log("destDir is Directory. destFile: " + destFile.getAbsolutePath());
//				}
//				else{
//					log("destDir is File");
//					destFile = destDir;
//				}
//
//				if(destFile.exists() && (document.getSize() == destFile.length())){
//					numberOfNodesAlreadyDownloaded++;
//					log(destFile.getAbsolutePath() + " already downloaded");
//				}
//				else {
//					numberOfNodesPending++;
//					log("start service");
//					Intent service = new Intent(this, DownloadService.class);
//					service.putExtra(DownloadService.EXTRA_HASH, hash);
//					service.putExtra(DownloadService.EXTRA_URL, url);
//					service.putExtra(DownloadService.EXTRA_SIZE, size);
//					service.putExtra(DownloadService.EXTRA_PATH, parentPath);
//					startService(service);
//
//					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//						log("Build.VERSION_CODES.LOLLIPOP --> Finish this!!!");
//						finish();
//					}
//				}
//
//				log("Total: " + numberOfNodesToDownload + " Already: " + numberOfNodesAlreadyDownloaded + " Pending: " + numberOfNodesPending);
//				if (numberOfNodesAlreadyDownloaded > 0){
//					String msg = getString(R.string.already_downloaded_multiple, numberOfNodesAlreadyDownloaded);
//					if (numberOfNodesPending > 0){
//						msg = msg + getString(R.string.pending_multiple, numberOfNodesPending);
//					}
//					showSnackbar(msg);
//				}
//			}
//		}
//	}

	public void downloadTo(String parentPath, String url, long size, long [] hashes){
		log("downloadTo");
		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}


		if (hashes == null){
			if(url != null) {
				if(availableFreeSpace < size) {
					showSnackbarNotSpace();
					return;
				}

				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				startService(service);
			}
		}
		else{
			if(hashes.length == 1){
				MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);
				if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
					log("ISFILE");
					String localPath = Util.getLocalFile(this, tempNode.getName(), tempNode.getSize(), parentPath);
					if(localPath != null){
						try {
							Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName()));
						}
						catch(Exception e) {}

						try {
							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
								viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
							} else {
								viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
							}
							viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							if (MegaApiUtils.isIntentAvailable(this, viewIntent))
								startActivity(viewIntent);
							else {
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
									intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
								} else {
									intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
								}
								intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
								if (MegaApiUtils.isIntentAvailable(this, intentShare))
									startActivity(intentShare);
								String toastMessage = getString(R.string.general_already_downloaded) + ": " + localPath;
								Snackbar.make(fragmentContainer, toastMessage, Snackbar.LENGTH_LONG).show();
							}
						}
						catch (Exception e){
							String toastMessage = getString(R.string.general_already_downloaded) + ": " + localPath;
							Snackbar.make(fragmentContainer, toastMessage, Snackbar.LENGTH_LONG).show();
						}
						log("Finish");
						finish();
						return;
					}
					else{
						log("LocalPath is NULL");
					}
				}
			}

			for (long hash : hashes) {
				MegaNode node = megaApi.getNodeByHandle(hash);
				if(node != null){
					log("Node!=null: "+node.getName());
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					dlFiles.put(node, parentPath);

					for (MegaNode document : dlFiles.keySet()) {

						String path = dlFiles.get(document);

						if(availableFreeSpace < document.getSize()){
							showSnackbarNotSpace();
							continue;
						}

						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
						service.putExtra(DownloadService.EXTRA_URL, url);
						service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						log("intent to DownloadService");
						startService(service);
					}
				}
				else if(url != null) {
					if(availableFreeSpace < size) {
						showSnackbarNotSpace();
						continue;
					}

					Intent service = new Intent(this, DownloadService.class);
					service.putExtra(DownloadService.EXTRA_HASH, hash);
					service.putExtra(DownloadService.EXTRA_URL, url);
					service.putExtra(DownloadService.EXTRA_SIZE, size);
					service.putExtra(DownloadService.EXTRA_PATH, parentPath);
					startService(service);
				}
				else {
					log("node not found. Let's try the document");
				}
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			log("Build.VERSION_CODES.LOLLIPOP --> Finish this!!!");
			finish();
		}
	}

	public void showSnackbar(String s){
		log("showSnackbar");
		Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}

	public void showSnackbarNotSpace(){
		log("showSnackbarNotSpace");
		Snackbar mySnackbar = Snackbar.make(fragmentContainer, R.string.error_not_enough_free_space, Snackbar.LENGTH_LONG);
		mySnackbar.setAction("Settings", new SnackbarNavigateOption(this));
		mySnackbar.show();
	}
	
	@SuppressLint("NewApi") 
	public void downloadWithPermissions(){
		log("downloadWithPermissions");
		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		
		if (dbH.getCredentials() == null || dbH.getPreferences() == null){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
						intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
						intent.setClass(this, FileStorageActivityLollipop.class);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, urlM);
						if (documentM != null){
							intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, documentM.getSize());
						}
						else if (document != null){
							intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
						}
						startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
					}
					else{
						Dialog downloadLocationDialog;
						String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
				        AlertDialog.Builder b=new AlertDialog.Builder(this);
	
						b.setTitle(getResources().getString(R.string.settings_storage_download_location));
						if (hashesM == null){
							if (document != null) {
								hashesM = new long[1];
								hashesM[0] = document.getHandle();
								sizeM = document.getSize();
								documentM = document;
							}
							else{
								return;
							}
						}

						final long sizeFinal = sizeM;
						final long[] hashesFinal = new long[hashesM.length];
						for (int i=0; i< hashesM.length; i++){
							hashesFinal[i] = hashesM[i];
						}
						
						b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which){
									case 0:{
										Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
										intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
										intent.setClass(getApplicationContext(), FileStorageActivityLollipop.class);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, urlM);
										if (documentM != null){
											intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, documentM.getSize());
										}
										else if (document != null){
											intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
										}
										startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
										break;
									}
									case 1:{
										File[] fs = getExternalFilesDirs(null);
										if (fs.length > 1){
											String path = fs[1].getAbsolutePath();
											File defaultPathF = new File(path);
											defaultPathF.mkdirs();
											Toast.makeText(getApplicationContext(), getString(R.string.general_download) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
											downloadTo(path, urlM, sizeFinal, hashesFinal);
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
					Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
					intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
					intent.setClass(this, FileStorageActivityLollipop.class);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, urlM);
					if (documentM != null){
						intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, documentM.getSize());
					}
					else if (document != null){
						intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
					}
					startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
				}
			}
			else{
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
				intent.setClass(this, FileStorageActivityLollipop.class);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, urlM);
				if (documentM != null){
					intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, documentM.getSize());
				}
				else if (document != null){
					intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
				}
				startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}	
			return;
		}
		
		boolean askMe = true;
		String downloadLocationDefaultPath = "";
		prefs = dbH.getPreferences();		
		if (prefs != null){
			if (prefs.getStorageAskAlways() != null){
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							askMe = false;
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}
		
		if (askMe){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
						intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
						intent.setClass(this, FileStorageActivityLollipop.class);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, urlM);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
						startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
					}
					else{
						Dialog downloadLocationDialog;
						String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
				        AlertDialog.Builder b=new AlertDialog.Builder(this);
	
						b.setTitle(getResources().getString(R.string.settings_storage_download_location));
						final long sizeFinal = sizeM;
						final long[] hashesFinal = new long[hashesM.length];
						for (int i=0; i< hashesM.length; i++){
							hashesFinal[i] = hashesM[i];
						}
						
						b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which){
									case 0:{
										Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
										intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
										intent.setClass(getApplicationContext(), FileStorageActivityLollipop.class);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, urlM);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
										startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
										break;
									}
									case 1:{
										File[] fs = getExternalFilesDirs(null);
										if (fs.length > 1){
											String path = fs[1].getAbsolutePath();
											File defaultPathF = new File(path);
											defaultPathF.mkdirs();
											Toast.makeText(getApplicationContext(), getString(R.string.general_download) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
											downloadTo(path, url, sizeFinal, hashesFinal);
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
					Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
					intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
					intent.setClass(this, FileStorageActivityLollipop.class);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, urlM);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
					startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
				}
			}
			else{
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
				intent.setClass(this, FileStorageActivityLollipop.class);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, urlM);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
				startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}
		}
		else{
			downloadTo(downloadLocationDefaultPath, urlM, sizeM, hashesM);
		}
	}

	public void successfulCopy(){
		Intent startIntent = new Intent(this, ManagerActivityLollipop.class);
		if(toHandle!=-1){
			startIntent.setAction(Constants.ACTION_OPEN_FOLDER);
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
        	case Constants.REQUEST_WRITE_STORAGE:{
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
		intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);
		startActivity(intent);
		finish();
	}

}