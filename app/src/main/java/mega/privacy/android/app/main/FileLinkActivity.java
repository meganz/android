package mega.privacy.android.app.main;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.Nullable;

import java.io.File;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.TransfersManagementActivity;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.namecollision.NameCollisionActivity;
import mega.privacy.android.app.namecollision.data.NameCollisionType;
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase;
import mega.privacy.android.app.usecase.CopyNodeUseCase;
import mega.privacy.android.app.usecase.exception.ForeignNodeException;
import mega.privacy.android.app.usecase.exception.MegaNodeException;
import mega.privacy.android.app.usecase.exception.OverQuotaException;
import mega.privacy.android.app.usecase.exception.PreOverQuotaException;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieDialogHandler;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_CLOSE_CHAT_AFTER_IMPORT;
import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

@AndroidEntryPoint
public class FileLinkActivity extends TransfersManagementActivity implements MegaRequestListenerInterface, OnClickListener,DecryptAlertDialog.DecryptDialogListener,
		SnackbarShower {

	@Inject
	CheckNameCollisionUseCase checkNameCollisionUseCase;
	@Inject
	CopyNodeUseCase copyNodeUseCase;

	private static final String TAG_DECRYPT = "decrypt";

	FileLinkActivity fileLinkActivity = this;

	Toolbar tB;
    ActionBar aB;
	DisplayMetrics outMetrics;
	String url;
	AlertDialog statusDialog;

	File previewFile = null;
	Bitmap preview = null;

	long toHandle = 0;
	long fragmentHandle = -1;

	CoordinatorLayout fragmentContainer;
	AppBarLayout appBarLayout;
	CollapsingToolbarLayout collapsingToolbar;
	ImageView iconView;
	ImageView imageView;
	RelativeLayout iconViewLayout;
	RelativeLayout imageViewLayout;

	TextView sizeTextView;
	Button importButton;
	Button downloadButton;
	Button buttonPreviewContent;
	MegaNode document = null;
	DatabaseHandler dbH = null;

	boolean decryptionIntroduced=false;

	boolean importClicked = false;
	MegaNode target = null;

	public static final int FILE_LINK = 1;

	private String mKey;

	private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
			AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));

	private MenuItem shareMenuItem;
	private Drawable upArrow;
	private Drawable drawableShare;

	@Inject
    CookieDialogHandler cookieDialogHandler;

	@Override
	public void onDestroy(){
		if(megaApi != null)
		{
			megaApi.removeRequestListener(this);
		}

		nodeSaver.destroy();

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

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		Intent intentReceived = getIntent();
		if (intentReceived != null){
			url = intentReceived.getDataString();
		}

		if (dbH.getCredentials() != null && (megaApi == null || megaApi.getRootNode() == null)) {
			logDebug("Refresh session - sdk or karere");
			Intent intent = new Intent(this, LoginActivity.class);
			intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
			intent.setData(Uri.parse(url));
			intent.setAction(ACTION_OPEN_FILE_LINK_ROOTNODES_NULL);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		if (savedInstanceState != null) {
			nodeSaver.restoreState(savedInstanceState);
		}

		setContentView(R.layout.activity_file_link);
		fragmentContainer = findViewById(R.id.file_link_fragment_container);

		appBarLayout = findViewById(R.id.app_bar);
		collapsingToolbar = findViewById(R.id.file_link_info_collapse_toolbar);

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			collapsingToolbar.setExpandedTitleMarginBottom(scaleHeightPx(60, outMetrics));
		}else{
			collapsingToolbar.setExpandedTitleMarginBottom(scaleHeightPx(35, outMetrics));
		}
		collapsingToolbar.setExpandedTitleMarginStart((int) getResources().getDimension(R.dimen.bottom_sheet_item_divider_margin_start));
		tB = findViewById(R.id.toolbar_file_link);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		if(aB != null) {
			aB.setDisplayShowTitleEnabled(false);
			aB.setHomeButtonEnabled(true);
			aB.setDisplayHomeAsUpEnabled(true);
		}
		/*Icon & image in Toolbar*/
		iconViewLayout = findViewById(R.id.file_link_icon_layout);
		iconView = findViewById(R.id.file_link_icon);

		imageViewLayout = findViewById(R.id.file_info_image_layout);
		imageView = findViewById(R.id.file_info_toolbar_image);
		imageViewLayout.setVisibility(View.GONE);

		/*Elements*/
		sizeTextView = findViewById(R.id.file_link_size);
		buttonPreviewContent = findViewById(R.id.button_preview_content);
		buttonPreviewContent.setOnClickListener(this);
		buttonPreviewContent.setEnabled(false);

		buttonPreviewContent.setVisibility(View.GONE);

		downloadButton = findViewById(R.id.file_link_button_download);
		downloadButton.setOnClickListener(this);
		downloadButton.setVisibility(View.INVISIBLE);

		importButton = findViewById(R.id.file_link_button_import);
		importButton.setOnClickListener(this);
		importButton.setVisibility(View.GONE);

		setTransfersWidgetLayout(findViewById(R.id.transfers_widget_layout));

		try{
			statusDialog.dismiss();
		}
		catch(Exception e){
			logError(e.getMessage());
		}

		if(url!=null){
			importLink(url);
		}
		else{
			logWarning("url NULL");
		}

		fragmentContainer.post(() -> cookieDialogHandler.showDialogIfNeeded(this));
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		cookieDialogHandler.showDialogIfNeeded(this, true);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		nodeSaver.saveState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		getMenuInflater().inflate(R.menu.file_folder_link_action, menu);

		upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_arrow_back_white);
		upArrow = upArrow.mutate();

		drawableShare = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_social_share_white);
		drawableShare = drawableShare.mutate();

		shareMenuItem = menu.findItem(R.id.share_link);

		trySetupCollapsingToolbar();

		return super.onCreateOptionsMenu(menu);
	}

	@SuppressLint("NonConstantResourceId")
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

	private void trySetupCollapsingToolbar() {
		if (shareMenuItem == null || document == null) {
			return;
		}

        int statusBarColor = ColorUtils.getColorForElevation(this, getResources().getDimension(R.dimen.toolbar_elevation));
        if (isDarkMode(this)) {
            collapsingToolbar.setContentScrimColor(statusBarColor);
        }

		if (preview != null) {
			appBarLayout.addOnOffsetChangedListener((appBarLayout, offset) -> {
				if (offset == 0) {
					// Expanded
					setColorFilterWhite();
				} else {
					if (offset < 0 && Math.abs(offset) >= appBarLayout.getTotalScrollRange() / 2) {
						// Collapsed
						setColorFilterBlack();
					} else {
						setColorFilterWhite();
					}
				}
			});

			collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087));
			collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.white_alpha_087));
			collapsingToolbar.setStatusBarScrimColor(statusBarColor);
		} else {
			collapsingToolbar.setStatusBarScrimColor(statusBarColor);
			setColorFilterBlack();
		}
	}

	private void setColorFilterBlack () {
		int color = getResources().getColor(R.color.grey_087_white_087);
		upArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		getSupportActionBar().setHomeAsUpIndicator(upArrow);

		if (shareMenuItem != null) {
			drawableShare.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
			shareMenuItem.setIcon(drawableShare);
		}
	}

	private void setColorFilterWhite () {
		int color = getResources().getColor(R.color.white_alpha_087);
		upArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		getSupportActionBar().setHomeAsUpIndicator(upArrow);

		if (shareMenuItem != null) {
			drawableShare.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
			shareMenuItem.setIcon(drawableShare);
		}
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

		AlertDialog temp;
		try {
			temp = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.general_loading));
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
				logError(ex.getMessage());
			}

			if (e.getErrorCode() == MegaError.API_OK) {
				document = request.getPublicMegaNode();

				if (document == null){
					logWarning("documment==null --> Intent to ManagerActivity");
					boolean closedChat = MegaApplication.isClosedChat();
					if(closedChat){
						Intent backIntent = new Intent(this, ManagerActivity.class);
						startActivity(backIntent);
					}

	    			finish();
					return;
				}

				long handle = document.getHandle();

				logDebug("DOCUMENTNODEHANDLEPUBLIC: " + handle);
				if (dbH == null){
					dbH = DatabaseHandler.getDbHandler(getApplicationContext());
				}

				if (handle != MegaApiJava.INVALID_HANDLE) {
					dbH.setLastPublicHandle(handle);
					dbH.setLastPublicHandleTimeStamp();
					dbH.setLastPublicHandleType(MegaApiJava.AFFILIATE_TYPE_FILE_FOLDER);
				}

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
							if (MimeTypeList.typeForName(document.getName()).isVideoReproducible()
									|| MimeTypeList.typeForName(document.getName()).isAudio()
									|| MimeTypeList.typeForName(document.getName()).isPdf()
									||  MimeTypeList.typeForName(document.getName()).isOpenableTextFile(document.getSize())){
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

				trySetupCollapsingToolbar();

				if (importClicked){
					copyNode();
				}
			}
			else{
				logWarning("ERROR: " + e.getErrorCode());
				MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
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
						// Invalid Link
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
							(dialog, which) -> {
								dialog.dismiss();
								boolean closedChat = MegaApplication.isClosedChat();
								if(closedChat){
									Intent backIntent = new Intent(fileLinkActivity, ManagerActivity.class);
									startActivity(backIntent);
								}

								finish();
							});

					AlertDialog dialog = dialogBuilder.create();
					dialog.show();
				}
				catch(Exception ex){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error_file_not_found));
				}
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
							this.preview = bitmap;

							if (iconView != null) {
								trySetupCollapsingToolbar();
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
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getRequestString());
	}

	@SuppressLint("NonConstantResourceId")
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.file_link_button_download:{
				nodeSaver.saveNode(document, false, false, false, true);
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

		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER);
		startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
	}

	private void copyNode() {
		checkNameCollisionUseCase.check(document, target, NameCollisionType.COPY)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(collision ->
								startActivity(NameCollisionActivity.getIntentForSingleItem(this, collision)),
						throwable -> {
							if (throwable instanceof MegaNodeException.ParentDoesNotExistException) {
								showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.general_error), MEGACHAT_INVALID_HANDLE);
							} else if (throwable instanceof MegaNodeException.ChildDoesNotExistsException) {
								copyNodeUseCase.copy(document, target)
										.subscribeOn(Schedulers.io())
										.observeOn(AndroidSchedulers.mainThread())
										.subscribe(() ->
												showCopyResult(null), this::showCopyResult);
							}
						});
	}

	private void showCopyResult(Throwable throwable) {
		dismissAlertDialogIfExists(statusDialog);

		if (throwable == null) {
			startActivity(new Intent(this, ManagerActivity.class)
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
			finish();
		} else if (throwable instanceof OverQuotaException) {
			errorOverquota();
		} else if (throwable instanceof PreOverQuotaException) {
			errorPreOverquota();
		} else if (throwable instanceof ForeignNodeException) {
			showForeignStorageOverQuotaWarningDialog(this);
		} else {
			showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
			startActivity(new Intent(this, ManagerActivity.class)
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
			finish();
		}
	}

	public void showFile(){
		logDebug("showFile");
		String serializeString = document.serialize();
		if(MimeTypeList.typeForName(document.getName()).isImage()){
			Intent intent = ImageViewerActivity.getIntentForSingleNode(this, url);
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
				mediaIntent = getMediaIntent(this, document.getName());
				mediaIntent.putExtra("adapterType", FILE_LINK_ADAPTER);
				mediaIntent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false);
				mediaIntent.putExtra(EXTRA_SERIALIZE_STRING, serializeString);
				mediaIntent.putExtra(URL_FILE_LINK, url);
				internalIntent = true;
			}
			mediaIntent.putExtra("FILENAME", document.getName());

			if (megaApi.httpServerIsRunning() == 0) {
				megaApi.httpServerStart();
				mediaIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
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
					return;
				}
			} else {
				logWarning("ERROR: HTTP server get local link");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
				return;
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
			Intent pdfIntent = new Intent(this, PdfViewerActivity.class);
			pdfIntent.putExtra("adapterType", FILE_LINK_ADAPTER);
			pdfIntent.putExtra(EXTRA_SERIALIZE_STRING, serializeString);
			pdfIntent.putExtra("inside", true);
			pdfIntent.putExtra("FILENAME", document.getName());
			pdfIntent.putExtra(URL_FILE_LINK, url);

			if (isOnline(this)){
				if (megaApi.httpServerIsRunning() == 0) {
					megaApi.httpServerStart();
					pdfIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
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
		} else if (MimeTypeList.typeForName(document.getName()).isOpenableTextFile(document.getSize())) {
			manageTextFileIntent(this, document, FILE_LINK_ADAPTER, url);
		} else{
			logWarning("none");
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (intent == null) {
			return;
		}

		if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
			return;
		}

		if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
			if (!isOnline(this)) {
				try {
					statusDialog.dismiss();
				} catch (Exception ex) {
					logError(ex.getMessage());
				}

				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			toHandle = intent.getLongExtra("IMPORT_TO", 0);
			fragmentHandle = intent.getLongExtra("fragmentH", -1);

			target = megaApi.getNodeByHandle(toHandle);

			statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.general_importing));
			statusDialog.show();

			if (document == null) {
				importClicked = true;
			} else {
				copyNode();
			}
		}
	}

	public void showSnackbar(int type, String s){
		showSnackbar(type, fragmentContainer, s);
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

		try{
			statusDialog.dismiss();
		} catch(Exception ex){
			logError(ex.getMessage());
		}

		finish();
	}
	
	@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        nodeSaver.handleRequestPermissionsResult(requestCode);
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
}