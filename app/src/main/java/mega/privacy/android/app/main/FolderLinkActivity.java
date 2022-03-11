package mega.privacy.android.app.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.TransfersManagementActivity;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieDialogHandler;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.main.listeners.MultipleRequestListenerLink;
import mega.privacy.android.app.modalbottomsheet.FolderLinkBottomSheetDialogFragment;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_CLOSE_CHAT_AFTER_IMPORT;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.permission.PermissionUtils.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.Util.*;

@AndroidEntryPoint
public class FolderLinkActivity extends TransfersManagementActivity implements MegaRequestListenerInterface, OnClickListener, DecryptAlertDialog.DecryptDialogListener,
		SnackbarShower {

	private static final String TAG_DECRYPT = "decrypt";

	FolderLinkActivity folderLinkActivity = this;

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
	AlertDialog statusDialog;
	MultipleRequestListenerLink importLinkMultipleListener = null;
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	boolean decryptionIntroduced=false;
	private ActionMode actionMode;
	
	MegaNode pN = null;
	boolean fileLinkFolderLink = false;

	public static final int FOLDER_LINK = 2;

	private FolderLinkBottomSheetDialogFragment bottomSheetDialogFragment;

	private String mKey;

	private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
			AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));

	@Inject
	CookieDialogHandler cookieDialogHandler;

	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapterList.isMultipleSelect()){
			adapterList.setMultipleSelect(true);
			actionMode = startSupportActionMode(new ActionBarCallBack());
		}
	}

	private void decrypt() {
		if (TextUtils.isEmpty(mKey)) return;
		String urlWithKey = "";

		if (url.contains("#F!")) {
			// old folder link format
			if (mKey.startsWith("!")) {
				logDebug("Decryption key with exclamation!");
				urlWithKey = url + mKey;
			} else {
				urlWithKey = url + "!" + mKey;
			}
		} else if (url.contains(SEPARATOR + "folder" + SEPARATOR)) {
			// new folder link format
			if (mKey.startsWith("#")) {
				logDebug("Decryption key with hash!");
				urlWithKey = url + mKey;
			} else {
				urlWithKey = url + "#" + mKey;
			}
		}

		logDebug("Folder link to import: " + urlWithKey);
		decryptionIntroduced=true;
		megaApiFolder.loginToFolder(urlWithKey, folderLinkActivity);
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

	@Override
	public void showSnackbar(int type, @Nullable String content, long chatId) {
		showSnackbar(type, fragmentContainer, content, chatId);
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					downloadNodes(adapterList.getSelectedNodes());
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

            ColorUtils.changeStatusBarColorForElevation(FolderLinkActivity.this, true);
            // No App bar in this activity, control tool bar instead.
            tB.setElevation(getResources().getDimension(R.dimen.toolbar_elevation));

			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			clearSelections();
			adapterList.setMultipleSelect(false);
			optionsBar.setVisibility(View.VISIBLE);
			separator.setVisibility(View.VISIBLE);

			// No App bar in this activity, control tool bar instead.
			boolean withElevation = listView.canScrollVertically(-1);
            ColorUtils.changeStatusBarColorForElevation(FolderLinkActivity.this, withElevation);
            if(!withElevation) {
                tB.setElevation(0);
            }
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
			case android.R.id.home:
				onBackPressed();
				break;

			case R.id.share_link:
				shareLink(this, url);
				break;

			case R.id.action_more:
				showOptionsPanel(megaApiFolder.getNodeByHandle(parentHandle));
				break;
		}

		return super.onOptionsItemSelected(item);
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

		float scaleText;
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}

		handler = new Handler();

		registerTransfersReceiver();

		dbH = DatabaseHandler.getDbHandler(FolderLinkActivity.this);

		Intent intentReceived = getIntent();

		if (intentReceived != null) {
			url = intentReceived.getDataString();
		}

		if (dbH.getCredentials() != null && (megaApi == null || megaApi.getRootNode() == null)) {
			logDebug("Refresh session - sdk or karere");
			Intent intent = new Intent(this, LoginActivity.class);
			intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
			intent.setData(Uri.parse(url));
			intent.setAction(ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		if (savedInstanceState != null) {
			nodeSaver.restoreState(savedInstanceState);
		}
		
		folderLinkActivity = this;

		prefs = dbH.getPreferences();

		lastPositionStack = new Stack<>();
		
		setContentView(R.layout.activity_folder_link);	
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_folder_link);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);

		fileLinktB = (Toolbar) findViewById(R.id.toolbar_folder_link_file_link);

        fragmentContainer = (RelativeLayout) findViewById(R.id.folder_link_fragment_container);
		fileLinkFragmentContainer = (RelativeLayout) findViewById(R.id.folder_link_file_link_fragment_container);
		fileLinkFragmentContainer.setVisibility(View.GONE);

		emptyImageView = (ImageView) findViewById(R.id.folder_link_list_empty_image);
		emptyTextView = (TextView) findViewById(R.id.folder_link_list_empty_text);

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			emptyImageView.setImageResource(R.drawable.empty_folder_landscape);
		}else{
			emptyImageView.setImageResource(R.drawable.empty_folder_portrait);
		}

		String textToShow = getString(R.string.file_browser_empty_folder_new);
		try{
			textToShow = textToShow.replace("[A]", "<font color=\'"
					+ ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
					+ "\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'"
					+ ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
					+ "\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		}
		catch (Exception e){}
		Spanned result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
		emptyTextView.setText(result);
		emptyImageView.setVisibility(View.GONE);
		emptyTextView.setVisibility(View.GONE);

		listView = (RecyclerView) findViewById(R.id.folder_link_list_view_browser);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);
		listView.setItemAnimator(noChangeRecyclerViewItemAnimator());

		listView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });
		
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
		//Left and Right margin
		LinearLayout.LayoutParams downloadTextParams = (LinearLayout.LayoutParams)fileLinkDownloadButton.getLayoutParams();
		downloadTextParams.setMargins(scaleWidthPx(6, outMetrics), 0, scaleWidthPx(8, outMetrics), 0);
		fileLinkDownloadButton.setLayoutParams(downloadTextParams);

		fileLinkImportButton = (TextView) findViewById(R.id.folder_link_file_link_button_import);
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
    			}
    		}
    	}
    	
    	aB.setTitle("MEGA - " + getString(R.string.general_loading));

		setTransfersWidgetLayout(findViewById(R.id.transfers_widget_layout));
		
		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}

		observeDragSupportEvents(this, listView, VIEWER_FROM_FOLDER_LINK);

		fragmentContainer.post(() -> cookieDialogHandler.showDialogIfNeeded(this));
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		nodeSaver.saveState(outState);
	}

    public void checkScroll() {
        if (listView == null) return;

        boolean canScroll = listView.canScrollVertically(-1);
        Util.changeToolBarElevation(this, tB, canScroll || adapterList.isMultipleSelect());
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

	@Override
	protected void onDestroy() {

		if (megaApiFolder != null){
			megaApiFolder.removeRequestListener(this);
		}

		handler.removeCallbacksAndMessages(null);

		nodeSaver.destroy();

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
			emptyImageView.setImageResource(R.drawable.empty_folder_landscape);
		}else{
			emptyImageView.setImageResource(R.drawable.empty_folder_portrait);
		}

		cookieDialogHandler.showDialogIfNeeded(this, true);
	}

	@Override
	protected void onResume() {
		super.onResume();
    	folderLinkActivity = this;
		logDebug("onResume");
	}

    public void downloadNodes(List<MegaNode> nodes) {
		nodeSaver.saveNodes(nodes, false, true, false, false);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		logDebug("onActivityResult");
		if (intent == null){
			return;
		}

		if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
			return;
		}

		if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
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

			statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.general_importing));
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

                // Get cookies settings after login.
                MegaApplication.getInstance().checkEnabledCookies();
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
							MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
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
										backIntent = new Intent(
												Objects.requireNonNullElse(
														folderLinkActivity,
														FolderLinkActivity.this
												),
												ManagerActivity.class);

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
						MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
						builder.setMessage(getString(R.string.general_error_folder_not_found));
						builder.setTitle(getString(R.string.general_error_word));

						builder.setPositiveButton(getString(android.R.string.ok),new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Intent backIntent;
								boolean closedChat = MegaApplication.isClosedChat();
								if(closedChat){
									backIntent = new Intent(
											Objects.requireNonNullElse(
													folderLinkActivity,
													FolderLinkActivity.this
											),
											ManagerActivity.class);
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
					if (api.isForeignNode(request.getParentHandle())) {
						showForeignStorageOverQuotaWarningDialog(this);
						return;
					}

					logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivity.class);
					intent.setAction(ACTION_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();
				}
				else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
					logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivity.class);
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

				if (request.getNodeHandle() != MegaApiJava.INVALID_HANDLE) {
					dbH.setLastPublicHandle(request.getNodeHandle());
					dbH.setLastPublicHandleTimeStamp();
					dbH.setLastPublicHandleType(MegaApiJava.AFFILIATE_TYPE_FILE_FOLDER);
				}

				MegaNode rootNode = megaApiFolder.getRootNode();
				if (rootNode != null){

					if(request.getFlag()){
						logWarning("Login into a folder with invalid decryption key");
						try{
							MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
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
												Intent backIntent;
												backIntent = new Intent(
														Objects.requireNonNullElse(
																folderLinkActivity,
																FolderLinkActivity.this
														),
														ManagerActivity.class
												);
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
									fileLinkSizeTextView.setText(getSizeString(pN.getSize()));

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
							adapterList = new MegaNodeAdapter(this, null, nodes,
									parentHandle, listView, FOLDER_LINK_ADAPTER,
									MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
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
						MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
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
										Intent backIntent;
										backIntent = new Intent(
												Objects.requireNonNullElse(
														folderLinkActivity,
														FolderLinkActivity.this
												),
												ManagerActivity.class
										);
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
					MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

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
										Intent backIntent;
										backIntent = new Intent(
												Objects.requireNonNullElse(
														folderLinkActivity,
														FolderLinkActivity.this
												),
												ManagerActivity.class
										);
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
		if (adapterList != null) {
			adapterList.setMultipleSelect(false);
		}

		if (actionMode != null) {
			actionMode.finish();
		}

		if (optionsBar != null) {
			optionsBar.setVisibility(View.VISIBLE);
		}

		if (separator != null) {
			separator.setVisibility(View.VISIBLE);
		}
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

			new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
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

	public void itemClick(int position) {

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

				parentHandle = nodes.get(position).getHandle();
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
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()) {
					long[] children = nodes.stream().mapToLong(MegaNode::getHandle).toArray();
					Intent intent = ImageViewerActivity.getIntentForChildren(
							this,
							children,
							nodes.get(position).getHandle()
					);
					putThumbnailLocation(intent, listView, position, VIEWER_FROM_FOLDER_LINK, adapterList);
					startActivity(intent);
					overridePendingTransition(0,0);
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
						mediaIntent = getMediaIntent(this, nodes.get(position).getName());
					}
					mediaIntent.putExtra("orderGetChildren", orderGetChildren);
					mediaIntent.putExtra("isFolderLink", true);
					mediaIntent.putExtra("HANDLE", file.getHandle());
					mediaIntent.putExtra("FILENAME", file.getName());
					putThumbnailLocation(mediaIntent, listView, position, VIEWER_FROM_FOLDER_LINK, adapterList);
					mediaIntent.putExtra("adapterType", FOLDER_LINK_ADAPTER);

					MegaNode parentNode = megaApiFolder.getParentNode(nodes.get(position));

					//Null check validation.
					if (parentNode == null) {
						LogUtil.logError(nodes.get(position).getName() + "'s parent node is null");
						return;
					}

					if (parentNode.getType() == MegaNode.TYPE_ROOT){
						mediaIntent.putExtra("parentNodeHandle", -1L);
					}
					else{
						mediaIntent.putExtra("parentNodeHandle", megaApiFolder.getParentNode(nodes.get(position)).getHandle());
					}

					String localPath = getLocalFile(file);

					MegaApiAndroid api = dbH.getCredentials() != null ? megaApi : megaApiFolder;

					boolean paramsSetSuccessfully;
					if (isLocalFile(file, megaApiFolder, localPath)) {
						paramsSetSuccessfully = setLocalIntentParams(this, file, mediaIntent,
								localPath, false, this);
					} else {
						paramsSetSuccessfully = setStreamingIntentParams(this, file, api,
								mediaIntent, this);
					}
					if (!paramsSetSuccessfully) {
						return;
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
							downloadNodes(Collections.singletonList(nodes.get(position)));
						}
					}
			  		overridePendingTransition(0,0);
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isPdf()){
					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					logDebug("FILE HANDLE: " + file.getHandle() + ", TYPE: " + mimeType);

					Intent pdfIntent = new Intent(FolderLinkActivity.this, PdfViewerActivity.class);
					pdfIntent.putExtra("APP", true);
					pdfIntent.putExtra("adapterType", FOLDER_LINK_ADAPTER);

					String localPath = getLocalFile(file);

					MegaApiAndroid api = dbH.getCredentials() != null ? megaApi : megaApiFolder;

					boolean paramsSetSuccessfully;
					if (isLocalFile(file, megaApiFolder, localPath)) {
						paramsSetSuccessfully = setLocalIntentParams(this, file, pdfIntent,
								localPath, false, this);
					} else {
						paramsSetSuccessfully = setStreamingIntentParams(this, file, api,
								pdfIntent, this);
					}
					if (!paramsSetSuccessfully) {
						return;
					}

					pdfIntent.putExtra("HANDLE", file.getHandle());
					pdfIntent.putExtra("isFolderLink", true);
					pdfIntent.putExtra("inside", true);
					putThumbnailLocation(pdfIntent, listView, position, VIEWER_FROM_FOLDER_LINK, adapterList);
					if (isIntentAvailable(FolderLinkActivity.this, pdfIntent)){
						startActivity(pdfIntent);
					}
					else{
						Toast.makeText(FolderLinkActivity.this, FolderLinkActivity.this.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

						downloadNodes(Collections.singletonList(nodes.get(position)));
					}
					overridePendingTransition(0,0);
				} else if (MimeTypeList.typeForName(nodes.get(position).getName()).isOpenableTextFile(nodes.get(position).getSize())) {
					manageTextFileIntent(this, nodes.get(position), FOLDER_LINK_ADAPTER);
				} else{
					boolean hasStoragePermission = hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
					if (!hasStoragePermission) {
						requestPermission(this,
								REQUEST_WRITE_STORAGE,
								Manifest.permission.WRITE_EXTERNAL_STORAGE);

						handleListM.clear();
						handleListM.add(nodes.get(position).getHandle());

						return;
					}

					adapterList.notifyDataSetChanged();
					downloadNodes(Collections.singletonList(nodes.get(position)));
				}
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		nodeSaver.handleRequestPermissionsResult(requestCode);
	}

	@Override
	public void onBackPressed() {
		logDebug("onBackPressed");
		if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
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
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER);
		startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
	}

	public void downloadNode(){
		logDebug("Download option");
		downloadNodes(Collections.singletonList(selectedNode));
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
					downloadNodes(adapterList.getSelectedNodes());
					clearSelections();
				} else {
					MegaNode rootNode = null;
					if(megaApiFolder.getRootNode()!=null){
						rootNode = megaApiFolder.getRootNode();
					}
					if(rootNode!=null){
						MegaNode parentNode = megaApiFolder.getNodeByHandle(parentHandle);
						if (parentNode != null){
							downloadNodes(Collections.singletonList(parentNode));
						}else{
							downloadNodes(Collections.singletonList(rootNode));
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

		if (sNode == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedNode = sNode;
		bottomSheetDialogFragment = new FolderLinkBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
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
		Intent intent = new Intent(this, ManagerActivity.class);
		intent.setAction(ACTION_OVERQUOTA_STORAGE);
		startActivity(intent);
		finish();
	}

	public void errorPreOverquota() {
		Intent intent = new Intent(this, ManagerActivity.class);
		intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
		startActivity(intent);
		finish();
	}

	public void successfulCopy(){
		if (getIntent() != null && getIntent().getBooleanExtra(OPENED_FROM_CHAT, false)) {
			sendBroadcast(new Intent(ACTION_CLOSE_CHAT_AFTER_IMPORT));
		}

		Intent startIntent = new Intent(this, ManagerActivity.class);
		if(toHandle!=-1){
			startIntent.setAction(ACTION_OPEN_FOLDER);
			startIntent.putExtra("PARENT_HANDLE", toHandle);
			startIntent.putExtra("offline_adapter", false);
			startIntent.putExtra(INTENT_EXTRA_KEY_LOCATION_FILE_INFO, true);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.file_folder_link_action, menu);
		menu.findItem(R.id.action_more).setVisible(true);
		return super.onCreateOptionsMenu(menu);
	}
}
