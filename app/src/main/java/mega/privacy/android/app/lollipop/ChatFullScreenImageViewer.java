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
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

public class ChatFullScreenImageViewer extends PinActivityLollipop implements OnPageChangeListener, MegaRequestListenerInterface, MegaGlobalListenerInterface {


	private DisplayMetrics outMetrics;

	private boolean aBshown = true;

	ProgressDialog statusDialog;
	private android.support.v7.app.AlertDialog downloadConfirmationDialog;

	float scaleText;
	AppBarLayout appBarLayout;
	Toolbar tB;
	ActionBar aB;

	private MenuItem downloadIcon;
	private MenuItem importIcon;
	private MenuItem saveForOfflineIcon;
	private MenuItem removeIcon;

	private MegaChatFullScreenImageAdapter adapterMega;
	private int positionG;
	private ArrayList<Long> imageHandles;
	private RelativeLayout fragmentContainer;
	private TextView fileNameTextView;
	private RelativeLayout bottomLayout;
	private ExtendedViewPager viewPager;

	static ChatFullScreenImageViewer fullScreenImageViewer;
    private MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

    private ArrayList<String> paths;
	MegaNode nodeToImport;

	long [] messageIds;
	long chatId = -1;

	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;

	ArrayList<MegaChatMessage> messages;

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
	public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenu");

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_chat_full_screen_image_viewer, menu);

		downloadIcon = menu.findItem(R.id.full_image_viewer_download);
		importIcon = menu.findItem(R.id.chat_full_image_viewer_import);
		saveForOfflineIcon = menu.findItem(R.id.full_image_viewer_save_for_offline);
		removeIcon = menu.findItem(R.id.full_image_viewer_remove);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		log("onPrepareOptionsMenu");

		MegaNode node = messages.get(positionG).getMegaNodeList().get(0);

		if(megaApi.userHandleToBase64(messages.get(positionG).getUserHandle()).equals(megaApi.getMyUserHandle())){
			if((megaApi.getNodeByHandle(node.getHandle()))==null){
				log("The node is not mine");
				removeIcon.setVisible(false);
			}
			else{
				if(messages.get(positionG).isDeletable()){
					removeIcon.setVisible(true);
				}
				else{
					removeIcon.setVisible(false);
				}
			}
		}
		else{
			log("The message is not mine");
			removeIcon.setVisible(false);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		int id = item.getItemId();
		switch (id) {
			case android.R.id.home: {
				onBackPressed();
				break;
			}
			case R.id.chat_full_image_viewer_download: {
				log("download option");
				MegaNode node = messages.get(positionG).getMegaNodeList().get(0);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
					if (!hasStoragePermission) {
						ActivityCompat.requestPermissions(this,
								new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								Constants.REQUEST_WRITE_STORAGE);
						handleListM.add(node.getHandle());
					}
				}

				ChatController chatC = new ChatController(this);
				chatC.prepareForChatDownload(node);

				break;
			}

			case R.id.chat_full_image_viewer_import: {
				log("import option");
				MegaNode node = messages.get(positionG).getMegaNodeList().get(0);
				importNode(node);
				break;
			}
			case R.id.full_image_viewer_save_for_offline: {
				log("save for offline option");
				showSnackbar("Coming soon...");
				break;
			}
			case R.id.full_image_viewer_remove: {
				log("remove option");
				MegaChatMessage msg = messages.get(positionG);
				showConfirmationDeleteNode(chatId, msg);
				break;
			}

		}
		return super.onOptionsItemSelected(item);
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
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    }


		MegaApplication app = (MegaApplication)getApplication();

		megaApi = app.getMegaApi();
		megaChatApi = app.getMegaChatApi();

		megaApi.addGlobalListener(this);


		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		setContentView(R.layout.activity_chat_full_screen_image_viewer);

		fragmentContainer = (RelativeLayout) findViewById(R.id.chat_full_image_viewer_parent_layout);
		appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
		viewPager = (ExtendedViewPager) findViewById(R.id.image_viewer_pager);
		viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

			// optional
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

			// optional
			@Override
			public void onPageSelected(int position) {
				log("onPageSelected");
				supportInvalidateOptionsMenu();
			}

			// optional
			@Override
			public void onPageScrollStateChanged(int state) { }
		});

		viewPager.setPageMargin(40);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
		}

		tB = (Toolbar) findViewById(R.id.call_toolbar);
		if (tB == null) {
			log("Tb is Null");
			return;
		}

		tB.setVisibility(View.VISIBLE);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		log("aB.setHomeAsUpIndicator_1");
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		aB.setHomeButtonEnabled(true);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setTitle(" ");

		Intent intent = getIntent();
		positionG = intent.getIntExtra("position", 0);

		messageIds = intent.getLongArrayExtra("messageIds");
		chatId = intent.getLongExtra("chatId", -1);

		messages = new ArrayList<MegaChatMessage>();

		imageHandles = new ArrayList<Long>();
		paths = new ArrayList<String>();

		if(messageIds==null){
			return;
		}

		for(int j=0; j<messageIds.length; j++){
			MegaChatMessage message = megaChatApi.getMessage(chatId, messageIds[j]);
			if(message!=null){
				MegaNodeList list = message.getMegaNodeList();
				if(list.size()==1){
					MegaNode node = list.get(0);
					if(MimeTypeList.typeForName(node.getName()).isImage()){
						messages.add(message);
					}
				}
				else{
					log("Messages with more than one attachment - do not supported");
				}
			}
			else{
				log("ERROR - the message is NULL");
			}
		}

		if(messages.size() == 0)
		{
			finish();
			return;
		}

		int imageNumber = 0;
		for (int i=0;i<messages.size();i++){
			MegaNode n = messages.get(i).getMegaNodeList().get(0);
			if (MimeTypeList.typeForName(n.getName()).isImage()){
				imageHandles.add(n.getHandle());
				if (i == positionG){
					positionG = imageNumber;
				}
				imageNumber++;
			}
		}

		if(positionG >= imageHandles.size())
		{
			positionG = 0;
		}

		adapterMega = new MegaChatFullScreenImageAdapter(this, fullScreenImageViewer,messages, megaApi);

		viewPager.setAdapter(adapterMega);

		viewPager.setCurrentItem(positionG);

		viewPager.setOnPageChangeListener(this);

		bottomLayout = (RelativeLayout) findViewById(R.id.chat_image_viewer_layout_bottom);
		fileNameTextView = (TextView) findViewById(R.id.chat_full_image_viewer_file_name);
		fileNameTextView.setText(messages.get(positionG).getMegaNodeList().get(0).getName());

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
					fileNameTextView.setText(messages.get(positionG).getMegaNodeList().get(0).getName());
				}
			catch(Exception e){}
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

	public void showConfirmationDeleteNode(final long chatId, final MegaChatMessage message){
		log("showConfirmationDeleteNode");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						ChatController cC = new ChatController(fullScreenImageViewer);
						cC.deleteMessage(message, chatId);
						finish();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		android.support.v7.app.AlertDialog.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		}
		else{
			builder = new android.support.v7.app.AlertDialog.Builder(this);
		}

		builder.setMessage(R.string.confirmation_delete_one_attachment);

		builder.setPositiveButton(R.string.context_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
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
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {}
	
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

	public void touchImage() {
		log("touchImage");
		if(aB.isShowing()){
			hideActionBar();
		}else{
			showActionBar();
		}
	}

	protected void hideActionBar(){
		if (aB != null && aB.isShowing()) {
			if(tB != null) {
				tB.animate().translationY(-220).setDuration(800L)
						.withEndAction(new Runnable() {
							@Override
							public void run() {
								aB.hide();
							}
						}).start();
				bottomLayout.animate().translationY(220).setDuration(800L).start();
			} else {
				aB.hide();
			}
		}
	}
	protected void showActionBar(){
		if (aB != null && !aB.isShowing()) {
			aB.show();
			if(tB != null) {
				tB.animate().translationY(0).setDuration(800L).start();
				bottomLayout.animate().translationY(0).setDuration(800L).start();
			}

		}
	}

	@Override
	public void onBackPressed() {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
		super.onBackPressed();
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {}

	@Override
	public void onReloadNeeded(MegaApiJava api) {}

	@Override
	public void onAccountUpdate(MegaApiJava api) {}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {}
}
