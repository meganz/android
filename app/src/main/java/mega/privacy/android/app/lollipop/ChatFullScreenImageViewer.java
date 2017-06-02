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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.ExtendedViewPager;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.lollipop.adapters.MegaChatFullScreenImageAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaOfflineFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;

public class ChatFullScreenImageViewer extends PinActivityLollipop implements OnPageChangeListener, OnClickListener, MegaRequestListenerInterface, OnItemClickListener{

	private DisplayMetrics outMetrics;

	private boolean aBshown = true;

	ProgressDialog statusDialog;
	private android.support.v7.app.AlertDialog downloadConfirmationDialog;

	float scaleText;

	private MegaChatFullScreenImageAdapter adapterMega;
	private int positionG;
	private ArrayList<Long> imageHandles;
	private RelativeLayout fragmentContainer;
	private TextView fileNameTextView;
	private ImageView actionBarIcon;
	private ImageView overflowIcon;
	private ImageView downloadIcon;
	private ImageView importIcon;
	private ListView overflowMenuList;
	private boolean overflowVisible = false;

	private RelativeLayout bottomLayout;
    private RelativeLayout topLayout;
	private ExtendedViewPager viewPager;

	String menuOptions[];

	static ChatFullScreenImageViewer fullScreenImageViewer;
    private MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

    private ArrayList<String> paths;
	MegaNode nodeToImport;

	long [] messageIds;
	long chatId = -1;

	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;

	ArrayList<MegaNode> nodes;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	ArrayList<Long> handleListM = new ArrayList<Long>();

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
		log("onCreate");
		super.onCreate(savedInstanceState);

		fullScreenImageViewer = this;

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}

		dbH = DatabaseHandler.getDbHandler(this);

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
		    requestWindowFeature(Window.FEATURE_NO_TITLE);
		    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		setContentView(R.layout.activity_chat_full_screen_image_viewer);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
		    ActionBar actionBar = getSupportActionBar();
		    if (actionBar != null){
		    	actionBar.hide();
		    }
		}

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    }


		MegaApplication app = (MegaApplication)getApplication();

		megaApi = app.getMegaApi();
		megaChatApi = app.getMegaChatApi();

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		viewPager = (ExtendedViewPager) findViewById(R.id.image_viewer_pager);
		viewPager.setPageMargin(40);

		fragmentContainer = (RelativeLayout) findViewById(R.id.chat_full_image_viewer_parent_layout);

		Intent intent = getIntent();
		positionG = intent.getIntExtra("position", 0);

		messageIds = intent.getLongArrayExtra("messageIds");
		chatId = intent.getLongExtra("chatId", -1);

		nodes = new ArrayList<MegaNode>();

		imageHandles = new ArrayList<Long>();
		paths = new ArrayList<String>();

		if(messageIds.length==1){
			log("One message");
			MegaChatMessage message = megaChatApi.getMessage(chatId, messageIds[0]);
			if(message!=null){
				MegaNodeList list = message.getMegaNodeList();
				if(list.size()==1){
					MegaNode node = list.get(0);
					nodes.add(node);
				}
				else{
					for(int i=0; i<list.size(); i++){
						MegaNode node = list.get(i);
						nodes.add(node);
					}
				}

				if(message.getUserHandle()==megaChatApi.getMyUserHandle()){
					menuOptions = new String[2];
					menuOptions[0] = getString(R.string.save_for_offline);
					menuOptions[1] = getString(R.string.general_revoke);
				}
				else{
					menuOptions = new String[1];
					menuOptions[0] = getString(R.string.save_for_offline);
				}

			}
			else{
				log("ERROR - the message is NULL");
			}
		}
		else{
			log("Several messages");
		}

		int imageNumber = 0;
		for (int i=0;i<nodes.size();i++){
			MegaNode n = nodes.get(i);
			if (MimeTypeList.typeForName(n.getName()).isImage()){
				imageHandles.add(n.getHandle());
				if (i == positionG){
					positionG = imageNumber;
				}
				imageNumber++;
			}
		}
//			Toast.makeText(this, ""+parentNode.getName() + "_" + imageHandles.size(), Toast.LENGTH_LONG).show();

		if(nodes.size() == 0)
		{
			finish();
			return;
		}

		if(positionG >= imageHandles.size())
		{
			positionG = 0;
		}

		adapterMega = new MegaChatFullScreenImageAdapter(fullScreenImageViewer,nodes, megaApi);

		viewPager.setAdapter(adapterMega);

		viewPager.setCurrentItem(positionG);

		viewPager.setOnPageChangeListener(this);

		actionBarIcon = (ImageView) findViewById(R.id.chat_full_image_viewer_icon);
		actionBarIcon.setOnClickListener(this);

		overflowIcon = (ImageView) findViewById(R.id.chat_full_image_viewer_overflow);
		overflowIcon.setVisibility(View.VISIBLE);
		overflowIcon.setOnClickListener(this);

		downloadIcon = (ImageView) findViewById(R.id.chat_full_image_viewer_download);
		downloadIcon.setVisibility(View.VISIBLE);
		downloadIcon.setOnClickListener(this);

		importIcon = (ImageView) findViewById(R.id.chat_full_image_viewer_import);
		importIcon.setVisibility(View.VISIBLE);
		importIcon.setOnClickListener(this);

		ArrayAdapter<String> arrayAdapter;

		overflowMenuList = (ListView) findViewById(R.id.chat_image_viewer_overflow_menu_list);
		arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
		overflowMenuList.setAdapter(arrayAdapter);

		overflowMenuList.setOnItemClickListener(this);
		if (overflowVisible){
			overflowMenuList.setVisibility(View.VISIBLE);
		}
		else{
			overflowMenuList.setVisibility(View.GONE);
		}

		bottomLayout = (RelativeLayout) findViewById(R.id.chat_image_viewer_layout_bottom);
		topLayout = (RelativeLayout) findViewById(R.id.chat_image_viewer_layout_top);

		fileNameTextView = (TextView) findViewById(R.id.chat_full_image_viewer_file_name);
		fileNameTextView.setText(nodes.get(positionG).getName());

		((MegaApplication) getApplication()).sendSignalPresenceActivity();

	}

	@Override
	public void onPageSelected(int position) {
		return;
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		return;
	}

	@Override
	public void onPageScrollStateChanged(int state) {

		if (state == ViewPager.SCROLL_STATE_IDLE){
			if (viewPager.getCurrentItem() != positionG){
				int oldPosition = positionG;
				int newPosition = viewPager.getCurrentItem();
				positionG = newPosition;

				try{
					TouchImageView tIV = adapterMega.getVisibleImage(oldPosition);
					if (tIV != null){
						tIV.setZoom(1);
					}
					fileNameTextView.setText(nodes.get(positionG).getName());
				}
				catch(Exception e){}
//				title.setText(names.get(positionG));
			}
		}
	}

	@Override
	public void onClick(View v) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		MegaNode node = nodes.get(positionG);
		switch (v.getId()){
			case R.id.chat_full_image_viewer_icon:{
				finish();
				break;
			}
			case R.id.chat_full_image_viewer_overflow:{
				log("show overflow menu option");
				if (adapterMega.isaBshown()){
					overflowVisible = adapterMega.isMenuVisible();
					if (overflowVisible){
						overflowMenuList.setVisibility(View.GONE);
						overflowVisible = false;
					}
					else{
						overflowMenuList.setVisibility(View.VISIBLE);
						overflowVisible = true;
					}
					adapterMega.setMenuVisible(overflowVisible);
				}
				break;
			}
			case R.id.chat_full_image_viewer_import:{

				log("import option");

				overflowMenuList.setVisibility(View.GONE);
				overflowVisible = false;
				adapterMega.setMenuVisible(overflowVisible);

				importNode(node);

				break;
			}
			case R.id.chat_full_image_viewer_download:{

				log("download option");

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
					if (!hasStoragePermission) {
						ActivityCompat.requestPermissions(this,
								new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								Constants.REQUEST_WRITE_STORAGE);

						handleListM.add(node.getHandle());

						return;
					}
				}

				overflowMenuList.setVisibility(View.GONE);
				overflowVisible = false;
				adapterMega.setMenuVisible(overflowVisible);

				ChatController chatC = new ChatController(this);
				chatC.prepareForChatDownload(node);

				break;
			}
		}

	}

	public void askSizeConfirmationBeforeChatDownload(String parentPath, ArrayList<MegaNode> nodeList, long size){
		log("askSizeConfirmationBeforeChatDownload");

		final String parentPathC = parentPath;
		final ArrayList<MegaNode> nodeListC = nodeList;
		final long sizeC = size;
		final ChatController chatC = new ChatController(this);

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(getResources().getColor(R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));

		builder.setMessage(getString(R.string.alert_larger_file, Util.getSizeString(sizeC)));
		builder.setPositiveButton(getString(R.string.general_download),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskSizeDownload("false");
						}
						chatC.download(parentPathC, nodeListC);
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
	

	public void importNode(MegaNode node){
		log("importNode");

		nodeToImport = node;
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);
		startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER);

	}

	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putBoolean("aBshown", adapterMega.isaBshown());
		savedInstanceState.putBoolean("overflowVisible", adapterMega.isMenuVisible());

	}
	
	@Override
	public void onRestoreInstanceState (Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		

		aBshown = savedInstanceState.getBoolean("aBshown");
		adapterMega.setaBshown(aBshown);
		overflowVisible = savedInstanceState.getBoolean("overflowVisible");
		adapterMega.setMenuVisible(overflowVisible);

		
		if (!aBshown){
			TranslateAnimation animBottom = new TranslateAnimation(0, 0, 0, Util.px2dp(48, outMetrics));
			animBottom.setDuration(0);
			animBottom.setFillAfter( true );
			bottomLayout.setAnimation(animBottom);
			
			TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(-48, outMetrics));
			animTop.setDuration(0);
			animTop.setFillAfter( true );
			topLayout.setAnimation(animTop);
		}
		
		if(overflowMenuList != null)
		{
			if (overflowVisible){
				overflowMenuList.setVisibility(View.VISIBLE);
			}
			else{
				overflowMenuList.setVisibility(View.GONE);
			}
		}
	}

	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

		log("onRequestFinish");
		if(request.getType() == MegaRequest.TYPE_COPY){
			if (e.getErrorCode() != MegaError.API_OK) {

				log("e.getErrorCode() != MegaError.API_OK");

				if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					log("OVERQUOTA ERROR: "+e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(Constants.ACTION_OVERQUOTA_ALERT);
					startActivity(intent);
					finish();
				}
				else
				{
					Snackbar.make(fragmentContainer, getString(R.string.import_success_error), Snackbar.LENGTH_LONG).show();
				}

			}else{
				Snackbar.make(fragmentContainer, getString(R.string.import_success_message), Snackbar.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());	
	}
	
	public static void log(String message) {
		Util.log("ChatFullScreenImageViewer", message);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		overflowMenuList.setVisibility(View.GONE);
		overflowVisible = false;
		adapterMega.setMenuVisible(overflowVisible);
		
		switch(position){
			case 0:{
				showSnackbar("Coming soon...");
				break;
			}
			case 1:{
				showSnackbar("Coming soon...");
				break;
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (intent == null) {
			return;
		}
		
		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);
			
			downloadTo (parentPath, url, size, hashes);
			Snackbar.make(fragmentContainer, getString(R.string.download_began), Snackbar.LENGTH_LONG).show();
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
			log("onActivityResult REQUEST_CODE_SELECT_IMPORT_FOLDER OK");

			if(!Util.isOnline(this)) {
				try{
					statusDialog.dismiss();
				} catch(Exception ex) {};

				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
				return;
			}

			final long toHandle = intent.getLongExtra("IMPORT_TO", 0);

			MegaNode target = null;
			target = megaApi.getNodeByHandle(toHandle);
			if(target == null){
				target = megaApi.getRootNode();
			}
			log("TARGET: " + target.getName() + "and handle: " + target.getHandle());
			//Temporal until we had preview of several messages

			if (nodeToImport != null) {
				log("DOCUMENT: " + nodeToImport.getName() + "_" + nodeToImport.getHandle());
				if (target != null) {
					megaApi.copyNode(nodeToImport, target, this);
				} else {
					log("TARGET: null");
					Snackbar.make(fragmentContainer, getString(R.string.import_success_error), Snackbar.LENGTH_LONG).show();
				}
			}
			else{
				log("DOCUMENT: null");
				Snackbar.make(fragmentContainer, getString(R.string.import_success_error), Snackbar.LENGTH_LONG).show();
			}

		}
	}
	

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		
	}
	
	public void downloadTo(String parentPath, String url, long size, long [] hashes){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}
		
		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}
		
		
		if (hashes == null){
			if(url != null) {
				if(availableFreeSpace < size) {
					Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
					return;
				}
				
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				service.putExtra(DownloadService.EXTRA_FOLDER_LINK, false);
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
								String message = getString(R.string.general_already_downloaded) + ": " + localPath;
								Snackbar.make(fragmentContainer, message, Snackbar.LENGTH_LONG).show();
							}
						}
						catch (Exception e){
							String message = getString(R.string.general_already_downloaded) + ": " + localPath;
							Snackbar.make(fragmentContainer, message, Snackbar.LENGTH_LONG).show();
						}
						return;
					}
				}
			}
			
			for (long hash : hashes) {
				MegaNode node = megaApi.getNodeByHandle(hash);
				if(node != null){
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					if (node.getType() == MegaNode.TYPE_FOLDER) {
//						getDlList(dlFiles, node, new File(parentPath, new String(node.getName())));
					} else {
						dlFiles.put(node, parentPath);
					}
					
					for (MegaNode document : dlFiles.keySet()) {
						
						String path = dlFiles.get(document);
						
						if(availableFreeSpace < document.getSize()){
							Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
							continue;
						}
						
						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
						service.putExtra(DownloadService.EXTRA_URL, url);
						service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						service.putExtra(DownloadService.EXTRA_FOLDER_LINK, false);
						startService(service);
					}
				}
				else if(url != null) {
					if(availableFreeSpace < size) {
						Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
						continue;
					}
					
					Intent service = new Intent(this, DownloadService.class);
					service.putExtra(DownloadService.EXTRA_HASH, hash);
					service.putExtra(DownloadService.EXTRA_URL, url);
					service.putExtra(DownloadService.EXTRA_SIZE, size);
					service.putExtra(DownloadService.EXTRA_PATH, parentPath);
					service.putExtra(DownloadService.EXTRA_FOLDER_LINK, false);
					startService(service);
				}
				else {
					log("node not found");
				}
			}
		}
	}

	public void showSnackbar(String s){
		log("showSnackbar");
		Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}
	
	@Override
	public void onBackPressed() {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		if (overflowMenuList != null){
			if (overflowMenuList.getVisibility() == View.VISIBLE){
				overflowMenuList.setVisibility(View.GONE);
				return;
			}
		}
		super.onBackPressed();
	}
}
