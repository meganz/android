package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kotlin.Unit;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.components.ExtendedViewPager;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.components.attacher.MegaAttacher;
import mega.privacy.android.app.components.dragger.DragToExitSupport;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.lollipop.adapters.MegaFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaOfflineFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.repo.MegaNodeRepo;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
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

import static android.graphics.Color.*;
import static mega.privacy.android.app.lollipop.FileInfoActivityLollipop.*;
import static mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop.*;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LinksUtil.showGetLinkActivity;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.moveToRubbishOrRemove;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.PermissionUtils.*;
import static nz.mega.sdk.MegaApiJava.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class FullScreenImageViewerLollipop extends PasscodeActivity
		implements OnPageChangeListener, MegaRequestListenerInterface, MegaGlobalListenerInterface,
		SnackbarShower, MegaFullScreenImageAdapterLollipop.FullScreenCallback {

	int placeholderCount;

	private DisplayMetrics outMetrics;

	AlertDialog statusDialog;

	AppBarLayout appBarLayout;
	Toolbar tB;
	ActionBar aB;

	float scaleText;

	MegaNode currentDocument;
	String url;

	int positionToRemove = -1;

	private final MegaAttacher nodeAttacher = new MegaAttacher(this);
	private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
			AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));
	private final DragToExitSupport dragToExit
			= new DragToExitSupport(this, this::onDragActivated, () -> {
		finish();
		overridePendingTransition(0, android.R.anim.fade_out);

		return Unit.INSTANCE;
	});

	NodeController nC;
	boolean isFileLink;

	private MegaFullScreenImageAdapterLollipop adapterMega;
	private MegaOfflineFullScreenImageAdapterLollipop adapterOffline;

	private int positionG;
	private ArrayList<Long> imageHandles;
	private RelativeLayout fragmentContainer;
	private TextView fileNameTextView;
	private MenuItem getlinkIcon;
	private MenuItem shareIcon;
	private MenuItem downloadIcon;
	private MenuItem propertiesIcon;
	private MenuItem renameIcon;
	private MenuItem moveIcon;
	private MenuItem copyIcon;
	private MenuItem moveToTrashIcon;
	private MenuItem removeIcon;
	private MenuItem removelinkIcon;
	private MenuItem chatIcon;

	MegaOffline currentNode;

	private RelativeLayout bottomLayout;
	private ExtendedViewPager viewPager;

	private FullScreenImageViewerLollipop fullScreenImageViewer;

    private ArrayList<String> paths;
	private String offlinePathDirectory;

    int adapterType = 0;
    long[] handlesNodesSearched;

    public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;

	MegaNode node;

	int typeExport = -1;

	boolean shareIt = true;

	private Handler handler;

	int orderGetChildren = ORDER_DEFAULT_ASC;

	DatabaseHandler dbH = null;

	boolean isFolderLink = false;

	ArrayList<MegaOffline> mOffList;
	ArrayList<MegaOffline> mOffListImages;

	RelativeLayout relativeImageViewerLayout;

	ArrayList<File> zipFiles = new ArrayList<>();

	private long parentNodeHandle = INVALID_HANDLE;

	private boolean needStopHttpServer;

	@Override
	public void onDestroy(){
		if(megaApi != null){
			megaApi.removeRequestListener(this);
			megaApi.removeGlobalListener(this);
		}

		unregisterReceiver(receiverToFinish);

		nodeSaver.destroy();

		dragToExit.showPreviousHiddenThumbnail();

		if (needStopHttpServer) {
			MegaApiAndroid api = isFolderLink() ? megaApiFolder : megaApi;
			api.httpServerStop();
		}

		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		logDebug("onKeyDown");

		if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	        // do nothing
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}

	boolean isDownloaded(MegaNode node) {
		logDebug("Node Handle: " + node.getHandle());

		return getLocalFile(node) != null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		logDebug("onCreateOptionsMenu");

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_full_screen_image_viewer, menu);

		getlinkIcon = menu.findItem(R.id.full_image_viewer_get_link);
		getlinkIcon.setTitle(StringResourcesUtils.getQuantityString(R.plurals.get_links, 1));
		getlinkIcon.setVisible(false);
		removelinkIcon = menu.findItem(R.id.full_image_viewer_remove_link);
		removelinkIcon.setVisible(false);
		shareIcon = menu.findItem(R.id.full_image_viewer_share);
		propertiesIcon = menu.findItem(R.id.full_image_viewer_properties);
		downloadIcon = menu.findItem(R.id.full_image_viewer_download);
		renameIcon = menu.findItem(R.id.full_image_viewer_rename);
		moveIcon = menu.findItem(R.id.full_image_viewer_move);
		copyIcon = menu.findItem(R.id.full_image_viewer_copy);
		moveToTrashIcon = menu.findItem(R.id.full_image_viewer_move_to_trash);
		removeIcon = menu.findItem(R.id.full_image_viewer_remove);
		chatIcon = menu.findItem(R.id.full_image_viewer_chat);

		Intent intent = getIntent();
		adapterType = intent.getIntExtra("adapterType", 0);
		offlinePathDirectory = intent.getStringExtra("offlinePathDirectory");

		if (nC == null) {
			nC = new NodeController(this);
		}
		boolean fromIncoming = false;
		if (adapterType == SEARCH_ADAPTER) {
			fromIncoming = nC.nodeComesFromIncoming(megaApi.getNodeByHandle(imageHandles.get(positionG)));
		}

		if(adapterType != OFFLINE_ADAPTER) {
            shareIcon.setVisible(showShareOption(adapterType, isFolderLink, imageHandles.get(positionG)));
        }

		if (adapterType == OFFLINE_ADAPTER){
			getlinkIcon.setVisible(false);
			// In offline section, share should be always showing.
            shareIcon.setVisible(true);
			menu.findItem(R.id.full_image_viewer_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			removelinkIcon.setVisible(false);
			menu.findItem(R.id.full_image_viewer_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			propertiesIcon.setVisible(false);
			menu.findItem(R.id.full_image_viewer_properties).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			downloadIcon.setVisible(true);
			menu.findItem(R.id.full_image_viewer_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			renameIcon.setVisible(false);
			moveIcon.setVisible(false);
			copyIcon .setVisible(false);
			moveToTrashIcon.setVisible(false);
			removeIcon.setVisible(false);
			chatIcon.setVisible(false);
		} else if (adapterType == RUBBISH_BIN_ADAPTER
				|| megaApi.isInRubbish(megaApi.getNodeByHandle(imageHandles.get(positionG)))){
			renameIcon.setVisible(false);
			moveIcon.setVisible(false);
			copyIcon .setVisible(false);
			moveToTrashIcon.setVisible(false);
			removeIcon.setVisible(true);
			chatIcon.setVisible(false);
			getlinkIcon.setVisible(false);
			removelinkIcon.setVisible(false);
			propertiesIcon.setVisible(true);
			downloadIcon.setVisible(false);
			shareIcon.setVisible(false);
		} else if (adapterType == ZIP_ADAPTER){

			getlinkIcon.setVisible(false);
			menu.findItem(R.id.full_image_viewer_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			removelinkIcon.setVisible(false);
			menu.findItem(R.id.full_image_viewer_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			propertiesIcon.setVisible(false);
			menu.findItem(R.id.full_image_viewer_properties).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			downloadIcon.setVisible(false);
			menu.findItem(R.id.full_image_viewer_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			renameIcon.setVisible(false);
			moveIcon.setVisible(false);
			copyIcon .setVisible(false);
			moveToTrashIcon.setVisible(false);
			removeIcon.setVisible(false);
			chatIcon.setVisible(false);

		}else if(adapterType == FILE_LINK_ADAPTER){
			renameIcon.setVisible(false);
			moveIcon.setVisible(false);
			copyIcon .setVisible(false);
			moveToTrashIcon.setVisible(false);
			removeIcon.setVisible(false);
			chatIcon.setVisible(false);
			getlinkIcon.setVisible(false);
			removelinkIcon.setVisible(false);
			propertiesIcon.setVisible(false);
			downloadIcon.setVisible(true);
		}else if(adapterType == SEARCH_ADAPTER && !fromIncoming){
			node = megaApi.getNodeByHandle(imageHandles.get(positionG));

			if(node.isExported()){
				removelinkIcon.setVisible(true);
				menu.findItem(R.id.full_image_viewer_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				getlinkIcon.setVisible(false);
				menu.findItem(R.id.full_image_viewer_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			}else{
				removelinkIcon.setVisible(false);
				menu.findItem(R.id.full_image_viewer_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

				getlinkIcon.setVisible(true);
				menu.findItem(R.id.full_image_viewer_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			propertiesIcon.setVisible(true);
			menu.findItem(R.id.full_image_viewer_properties).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			downloadIcon.setVisible(true);
			menu.findItem(R.id.full_image_viewer_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			renameIcon.setVisible(true);
			moveIcon.setVisible(true);
			copyIcon .setVisible(true);
			chatIcon.setVisible(true);

			node = megaApi.getNodeByHandle(imageHandles.get(positionG));
			final long handle = node.getHandle();
			MegaNode parent = megaApi.getNodeByHandle(handle);
			while (megaApi.getParentNode(parent) != null){
				parent = megaApi.getParentNode(parent);
			}

			if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
				moveToTrashIcon.setVisible(true);
				removeIcon.setVisible(false);
			}
			else{
				moveToTrashIcon.setVisible(false);
				removeIcon.setVisible(true);
			}
		}
		else if (adapterType == INCOMING_SHARES_ADAPTER || fromIncoming) {
			propertiesIcon.setVisible(true);
			menu.findItem(R.id.full_image_viewer_properties).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			chatIcon.setVisible(true);
			copyIcon.setVisible(true);
			removeIcon.setVisible(false);
			getlinkIcon.setVisible(false);
			removelinkIcon.setVisible(false);
			downloadIcon.setVisible(true);
			menu.findItem(R.id.full_image_viewer_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			MegaNode node = megaApi.getNodeByHandle(imageHandles.get(positionG));
			int accessLevel = megaApi.getAccess(node);

			switch (accessLevel) {
				case MegaShare.ACCESS_FULL: {
					logDebug("Access FULL");
					renameIcon.setVisible(true);
					moveIcon.setVisible(true);
					moveToTrashIcon.setVisible(true);
					break;
				}
				case MegaShare.ACCESS_READ:
					logDebug("Access read");
				case MegaShare.ACCESS_READWRITE: {
					logDebug("Access read & write");
					renameIcon.setVisible(false);
					moveIcon.setVisible(false);
					moveToTrashIcon.setVisible(false);
					break;
				}
			}
		}
		else if (adapterType == RECENTS_ADAPTER || adapterType == RECENTS_BUCKET_ADAPTER) {
			node = megaApi.getNodeByHandle(imageHandles.get(positionG));
			getlinkIcon.setVisible(false);
			removelinkIcon.setVisible(false);
			removeIcon.setVisible(false);

			int accessLevel = megaApi.getAccess(node);
			switch (accessLevel) {
				case MegaShare.ACCESS_READWRITE:
				case MegaShare.ACCESS_READ:
				case MegaShare.ACCESS_UNKNOWN: {
					renameIcon.setVisible(false);
					moveIcon.setVisible(false);
					moveToTrashIcon.setVisible(false);
					break;
				}
				case MegaShare.ACCESS_FULL:
				case MegaShare.ACCESS_OWNER: {
					renameIcon.setVisible(true);
					moveIcon.setVisible(true);
					moveToTrashIcon.setVisible(true);
					break;
				}
			}
		}
		else {
			node = megaApi.getNodeByHandle(imageHandles.get(positionG));
			if (node == null) {
				getlinkIcon.setVisible(false);
				removelinkIcon.setVisible(false);
				shareIcon.setVisible(false);
				propertiesIcon.setVisible(false);
				downloadIcon.setVisible(false);
				renameIcon.setVisible(false);
				moveIcon.setVisible(false);
				copyIcon.setVisible(false);
				moveToTrashIcon.setVisible(false);
				removeIcon.setVisible(false);
				chatIcon.setVisible(false);

				return super.onCreateOptionsMenu(menu);
			}

			downloadIcon.setVisible(true);
			menu.findItem(R.id.full_image_viewer_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			copyIcon.setVisible(true);

			if(node.isExported()){
				getlinkIcon.setVisible(false);
				menu.findItem(R.id.full_image_viewer_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

				removelinkIcon.setVisible(true);
				menu.findItem(R.id.full_image_viewer_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			}
			else{
				if(adapterType==CONTACT_FILE_ADAPTER){

					getlinkIcon.setVisible(false);
					menu.findItem(R.id.full_image_viewer_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
					removelinkIcon.setVisible(false);
					menu.findItem(R.id.full_image_viewer_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

				}else{
					if(isFolderLink){
						getlinkIcon.setVisible(false);
						removelinkIcon.setVisible(false);

					}else{
						getlinkIcon.setVisible(true);
						menu.findItem(R.id.full_image_viewer_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
						removelinkIcon.setVisible(false);
					}
				}
			}

			if(isFolderLink){
				propertiesIcon.setVisible(false);
				menu.findItem(R.id.full_image_viewer_properties).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

				moveToTrashIcon.setVisible(false);
				removeIcon.setVisible(false);
				renameIcon.setVisible(false);
				moveIcon.setVisible(false);
				copyIcon.setVisible(false);
				chatIcon.setVisible(false);

			}
			else{

				propertiesIcon.setVisible(true);
				menu.findItem(R.id.full_image_viewer_properties).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				if(adapterType==CONTACT_FILE_ADAPTER){
					removeIcon.setVisible(false);
					node = megaApi.getNodeByHandle(imageHandles.get(positionG));
					int accessLevel = megaApi.getAccess(node);
					switch(accessLevel){

						case MegaShare.ACCESS_OWNER:
						case MegaShare.ACCESS_FULL:{
							renameIcon.setVisible(true);
							moveIcon.setVisible(true);
							moveToTrashIcon.setVisible(true);
							chatIcon.setVisible(true);
							break;
						}
						case MegaShare.ACCESS_READWRITE:
						case MegaShare.ACCESS_READ:{
							renameIcon.setVisible(false);
							moveIcon.setVisible(false);
							moveToTrashIcon.setVisible(false);
							chatIcon.setVisible(false);
							break;
						}
					}

				}else{
					chatIcon.setVisible(true);
					renameIcon.setVisible(true);
					moveIcon.setVisible(true);

					node = megaApi.getNodeByHandle(imageHandles.get(positionG));

					final long handle = node.getHandle();
					MegaNode parent = megaApi.getNodeByHandle(handle);

					while (megaApi.getParentNode(parent) != null){
						parent = megaApi.getParentNode(parent);
					}

					moveToTrashIcon.setVisible(true);
					removeIcon.setVisible(false);
				}
			}
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		logDebug("onPrepareOptionsMenu");
		return super.onPrepareOptionsMenu(menu);
	}

	@SuppressLint("NonConstantResourceId")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		logDebug("onOptionsItemSelected");

		int id = item.getItemId();
		switch (id) {
			case android.R.id.home: {
				onBackPressed();
				break;
			}
			case R.id.full_image_viewer_get_link: {
				if (adapterType == OFFLINE_ADAPTER){
					break;

				}else if (adapterType == ZIP_ADAPTER){
					break;

				}else{
					node = megaApi.getNodeByHandle(imageHandles.get(positionG));
					if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
						return false;
					}

					shareIt = false;
			    	showGetLinkActivity(this, node.getHandle());
					break;
				}

			}

			case R.id.full_image_viewer_chat:{
				nodeAttacher.attachNode(imageHandles.get(positionG));
				break;
			}

			case R.id.full_image_viewer_remove_link: {
				shareIt = false;

				node = megaApi.getNodeByHandle(imageHandles.get(positionG));
				if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
					return false;
				}
				AlertDialog removeLinkDialog;
				MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

				LayoutInflater inflater = getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
				TextView url = dialoglayout.findViewById(R.id.dialog_link_link_url);
				TextView key = dialoglayout.findViewById(R.id.dialog_link_link_key);
				TextView symbol = dialoglayout.findViewById(R.id.dialog_link_symbol);
				TextView removeText = dialoglayout.findViewById(R.id.dialog_link_text_remove);

				((RelativeLayout.LayoutParams) removeText.getLayoutParams()).setMargins(scaleWidthPx(25, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(10, outMetrics), 0);

				url.setVisibility(View.GONE);
				key.setVisibility(View.GONE);
				symbol.setVisibility(View.GONE);
				removeText.setVisibility(View.VISIBLE);

				removeText.setText(getString(R.string.context_remove_link_warning_text));

				Display display = getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics();
				display.getMetrics(outMetrics);
				float density = getResources().getDisplayMetrics().density;

				float scaleW = getScaleW(outMetrics, density);
				float scaleH = getScaleH(outMetrics, density);
				if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (10*scaleW));
				}else{
					removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (15*scaleW));

				}

				builder.setView(dialoglayout);

				builder.setPositiveButton(getString(R.string.context_remove), (dialog, which) -> {
					typeExport=TYPE_EXPORT_REMOVE;
					megaApi.disableExport(node, fullScreenImageViewer);
				});

				builder.setNegativeButton(getString(R.string.general_cancel), (dialog, which) -> {

				});

				removeLinkDialog = builder.create();
				removeLinkDialog.show();

				break;
			}
			case R.id.full_image_viewer_share: {
				logDebug("Share option");
				if (adapterType == OFFLINE_ADAPTER) {
					shareFile(this, getOfflineFile(this, mOffListImages.get(positionG)));
				} else if (adapterType == ZIP_ADAPTER) {
					shareFile(this, zipFiles.get(positionG));
				} else if (adapterType == FILE_LINK_ADAPTER) {
					shareLink(this, url);
				} else {
					shareNode(this, megaApi.getNodeByHandle(imageHandles.get(positionG)));
				}
				break;
			}
			case R.id.full_image_viewer_properties: {

				if (adapterType == OFFLINE_ADAPTER){
					break;

				}else if (adapterType == ZIP_ADAPTER){
					break;

				}else{
					node = megaApi.getNodeByHandle(imageHandles.get(positionG));
					Intent i = new Intent(this, FileInfoActivityLollipop.class);
					i.putExtra("handle", node.getHandle());
					i.putExtra(NAME, node.getName());
					if (nC == null) {
						nC = new NodeController(this);
					}
					boolean fromIncoming = false;

					if (adapterType == SEARCH_ADAPTER || adapterType == RECENTS_ADAPTER || adapterType == RECENTS_BUCKET_ADAPTER) {
						fromIncoming = nC.nodeComesFromIncoming(node);
					}
					if (adapterType == INCOMING_SHARES_ADAPTER || fromIncoming) {
						i.putExtra("from", FROM_INCOMING_SHARES);
						i.putExtra("firstLevel", false);
					}
					else if(adapterType == INBOX_ADAPTER){
						i.putExtra("from", FROM_INBOX);
					}
					startActivity(i);
					break;
				}
			}
			case R.id.full_image_viewer_download: {
				switch (adapterType) {
					case OFFLINE_ADAPTER:
						nodeSaver.saveOfflineNode(mOffListImages.get(positionG), true);
						break;
					case ZIP_ADAPTER:
						// don't have this option
						break;
					case FILE_LINK_ADAPTER:
						nodeSaver.saveNode(currentDocument, false, false, true, true);
						break;
					default:
						nodeSaver.saveNode(node, false, isFolderLink, true, false);
						break;
				}

				break;
			}
			case R.id.full_image_viewer_rename: {
				showRenameNodeDialog(this, megaApi.getNodeByHandle(imageHandles.get(positionG)),
						this, null);
				break;
			}
			case R.id.full_image_viewer_move: {
				showMove();
				break;
			}
			case R.id.full_image_viewer_copy: {
				showCopy();
				break;
			}
			case R.id.full_image_viewer_move_to_trash:
			case R.id.full_image_viewer_remove: {
				positionToRemove = positionG;
				moveToRubbishOrRemove(imageHandles.get(positionG), this, this);
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean shouldSetStatusBarTextColor() {
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate");

		Window window = getWindow();
		window.setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
		window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onCreate(savedInstanceState);
		setContentView(dragToExit.wrapContentView(R.layout.activity_full_screen_image_viewer));

		relativeImageViewerLayout = findViewById(R.id.full_image_viewer_layout);

		handler = new Handler();
		fullScreenImageViewer = this;

		registerReceiver(receiverToFinish, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN));

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = getResources().getDisplayMetrics().density;

		appBarLayout = findViewById(R.id.app_bar);

		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);
		scaleText = Math.min(scaleH, scaleW);

		viewPager = findViewById(R.id.image_viewer_pager);
		viewPager.setPageMargin(40);

		fragmentContainer = findViewById(R.id.full_image_viewer_parent_layout);

		Intent intent = getIntent();

		dragToExit.setViewerFrom(intent.getIntExtra(INTENT_EXTRA_KEY_VIEWER_FROM, INVALID_VALUE));
		dragToExit.observeThumbnailLocation(this);

		positionG = intent.getIntExtra("position", 0);
		//If inserted a placehoder in MegaNodeAdapter,here the position need to be remove the placeholder.
        placeholderCount = intent.getIntExtra("placeholder",0 );
        orderGetChildren = intent.getIntExtra("orderGetChildren", ORDER_DEFAULT_ASC);
        isFolderLink = intent.getBooleanExtra("isFolderLink", false);
        isFileLink = intent.getBooleanExtra("isFileLink",false);

        adapterType = intent.getIntExtra("adapterType", 0);
        if(adapterType == RUBBISH_BIN_ADAPTER || adapterType == INBOX_ADAPTER ||
				adapterType == INCOMING_SHARES_ADAPTER|| adapterType == OUTGOING_SHARES_ADAPTER ||
				adapterType == SEARCH_ADAPTER || adapterType == FILE_BROWSER_ADAPTER ||
				adapterType == PHOTO_SYNC_ADAPTER || adapterType == SEARCH_BY_ADAPTER ||
				adapterType == LINKS_ADAPTER || adapterType == PHOTOS_BROWSE_ADAPTER
				|| adapterType == PHOTOS_SEARCH_ADAPTER ) {
            // only for the first time
            if(savedInstanceState == null) {
                positionG -= placeholderCount;
            }
        }

		if (isFolderLink) {
			megaApi = megaApiFolder;
		}

		if(isOnline(this) && !isFileLink) {
			if (shouldRefreshSessionDueToSDK()) {
				return;
			}
		}

		if (shouldRefreshSessionDueToKarere()) {
			return;
		}

		dbH = DatabaseHandler.getDbHandler(this);
		handler = new Handler();

		tB = findViewById(R.id.call_toolbar);
		if (tB == null) {
			logWarning("Tb is Null");
			return;
		}

		tB.setVisibility(View.VISIBLE);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		if (aB != null) {
			aB.setHomeButtonEnabled(true);
			aB.setDisplayHomeAsUpEnabled(true);
			aB.setTitle(" ");
		}

		imageHandles = new ArrayList<>();
		paths = new ArrayList<>();
		parentNodeHandle = intent.getLongExtra("parentNodeHandle", INVALID_HANDLE);
		MegaNode parentNode;
		bottomLayout = findViewById(R.id.image_viewer_layout_bottom);

		fileNameTextView = findViewById(R.id.full_image_viewer_file_name);
		fileNameTextView.setMaxWidth(scaleWidthPx(300, outMetrics));

		megaApi.addGlobalListener(this);

		if (adapterType == OFFLINE_ADAPTER){
			//OFFLINE
			mOffList = intent.getParcelableArrayListExtra(INTENT_EXTRA_KEY_ARRAY_OFFLINE);
			logDebug ("mOffList.size() = " + mOffList.size());

			for(int i=0; i<mOffList.size();i++){
				MegaOffline checkOffline = mOffList.get(i);
				File offlineFile = getOfflineFile(this, checkOffline);
				if(!isFileAvailable(offlineFile)){
					mOffList.remove(i);
					i--;
				}
			}

			if (mOffList.size() > 0){

				mOffListImages = new ArrayList<>();
				int positionImage = -1;
				for (int i=0;i<mOffList.size();i++){
					if (MimeTypeList.typeForName(mOffList.get(i).getName()).isImage()){
						mOffListImages.add(mOffList.get(i));
						positionImage++;
						if (i == positionG && savedInstanceState == null){
							positionG = positionImage;
						}
					}
				}

				if (positionG >= mOffListImages.size()) positionG = 0;

				adapterOffline = new MegaOfflineFullScreenImageAdapterLollipop(this, fullScreenImageViewer, mOffListImages);
			}

			currentNode = mOffListImages.get(positionG);
			fileNameTextView.setText(currentNode.getName());
		}else if (adapterType == FILE_LINK_ADAPTER){
			url = intent.getStringExtra(URL_FILE_LINK);
			String serialize = intent.getStringExtra(EXTRA_SERIALIZE_STRING);
			if(serialize!=null){
				currentDocument = MegaNode.unserialize(serialize);
				if(currentDocument != null){
					long hash = currentDocument.getHandle();
					logDebug("Handle: " + hash);
					imageHandles.add(hash);

					adapterMega = new MegaFullScreenImageAdapterLollipop(this,
							fullScreenImageViewer, imageHandles, megaApi, this);
					viewPager.setAdapter(adapterMega);
					viewPager.setCurrentItem(positionG);
					viewPager.addOnPageChangeListener(this);

					fileNameTextView = findViewById(R.id.full_image_viewer_file_name);
					if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						fileNameTextView.setMaxWidth(scaleWidthPx(300, outMetrics));
					}else{
						fileNameTextView.setMaxWidth(scaleWidthPx(300, outMetrics));
					}
					fileNameTextView.setText(currentDocument.getName());

				}else{
					logWarning("Node is NULL after unserialize");
				}
			} else {
				logWarning("serialize == NULL");
			}

		}else if (adapterType == ZIP_ADAPTER){
			offlinePathDirectory = intent.getStringExtra("offlinePathDirectory");
			File currentImage = new File(offlinePathDirectory);
			File offlineDirectory = new File(currentImage.getParent());

			paths.clear();
			int imageNumber = 0;
			int index = 0;
			File[] fList = offlineDirectory.listFiles();
			if(fList == null)
			{
				//Nothing to show (folder deleted?)
				//Close the image viewer
				finish();
				return;
			}
			for (int i=0; i<fList.length; i++) {
				zipFiles.add(fList[i]);
			}
			Collections.sort(zipFiles, (z1, z2) -> {
				String name1 = z1.getName();
				String name2 = z2.getName();
				int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
				if (res == 0) {
					res = name1.compareTo(name2);
				}
				return res;
			});

			logDebug("SIZE: " + zipFiles.size());
			for (File f : zipFiles){
				logDebug("F: " + f.getAbsolutePath());
				if (MimeTypeList.typeForName(f.getName()).isImage()){
					paths.add(f.getAbsolutePath());
					if (index == positionG && savedInstanceState == null){
						positionG = imageNumber;
					}
					imageNumber++;
				}
				index++;
			}

			if(paths.size() == 0) finish();

			if(positionG >= paths.size()) positionG = 0;

			adapterOffline = new MegaOfflineFullScreenImageAdapterLollipop(this, fullScreenImageViewer, paths, true);
			fileNameTextView.setText(new File(paths.get(positionG)).getName());
		}
		else if(adapterType == SEARCH_ADAPTER){
			ArrayList<String> handles = intent.getStringArrayListExtra(ARRAY_SEARCH);
			getImageHandles(getSearchedNodes(handles), savedInstanceState);
		}else if(adapterType == SEARCH_BY_ADAPTER || adapterType == PHOTOS_SEARCH_ADAPTER){
			handlesNodesSearched = intent.getLongArrayExtra("handlesNodesSearch");

			ArrayList<MegaNode> nodes = new ArrayList<>();
			for(Long handle:handlesNodesSearched){
				nodes.add(megaApi.getNodeByHandle(handle));
			}
			getImageHandles(nodes,savedInstanceState);
		}
		else if (adapterType == RECENTS_ADAPTER || adapterType == RECENTS_BUCKET_ADAPTER) {
			long handle = intent.getLongExtra(HANDLE, -1);
			if (handle == -1) finish();

			long[] nodeHandles = intent.getLongArrayExtra(NODE_HANDLES);
			if (nodeHandles != null && nodeHandles.length > 0) {
				for (int i = 0; i < nodeHandles.length; i++) {
					if (nodeHandles[i] != -1) {
						imageHandles.add(nodeHandles[i]);
						if (nodeHandles[i] == handle) {
							positionG = i;
						}
					}
				}

			}

			if (imageHandles.isEmpty()) {
				imageHandles.add(handle);
				positionG = 0;
			}

			fileNameTextView.setText(megaApi.getNodeByHandle(imageHandles.get(positionG)).getName());
			adapterMega = new MegaFullScreenImageAdapterLollipop(this, fullScreenImageViewer,
					imageHandles, megaApi, this);
		} else if (isInRootLinksLevel(adapterType, parentNodeHandle)) {
			getImageHandles(megaApi.getPublicLinks(orderGetChildren), savedInstanceState);
		} else if (adapterType == PHOTOS_BROWSE_ADAPTER) {
			// TODO: use constants
			getImageHandles(megaApi.searchByType(orderGetChildren, FILE_TYPE_PHOTO, SEARCH_TARGET_ROOTNODE), savedInstanceState);
		} else if (adapterType == PHOTO_SYNC_ADAPTER) {
			getImageHandles(new MegaNodeRepo(megaApi, dbH).getCuChildren(orderGetChildren), savedInstanceState, true);
		} else {
			if (parentNodeHandle == INVALID_HANDLE){
				switch(adapterType){
					case RUBBISH_BIN_ADAPTER:{
						parentNode = megaApi.getRubbishNode();
						break;
					}
					case SHARED_WITH_ME_ADAPTER:{
						parentNode = megaApi.getInboxNode();
						break;
					}

					default:{
						parentNode = megaApi.getRootNode();
						break;
					}
				}

			}
			else{
				parentNode = megaApi.getNodeByHandle(parentNodeHandle);
			}

			ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
			getImageHandles(nodes, savedInstanceState);
		}

		if (adapterType == OFFLINE_ADAPTER) {
			viewPager.setAdapter(adapterOffline);
		}
		else {
			viewPager.setAdapter(adapterMega);
		}
		viewPager.setCurrentItem(positionG);
		viewPager.addOnPageChangeListener(this);

		if (savedInstanceState == null) {
			dragToExit.runEnterAnimation(intent, viewPager, animationStart -> {
				if (animationStart) {
					updateViewForAnimation();
				} else if (!isFinishing()) {
					showActionBar();

					fragmentContainer.setBackgroundColor(BLACK);
					relativeImageViewerLayout.setBackgroundColor(BLACK);
					appBarLayout.setBackgroundColor(BLACK);

					long currentHandle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE,
							INVALID_HANDLE);
					if (currentHandle != INVALID_HANDLE) {
						dragToExit.nodeChanged(currentHandle);
					}
				}

				return null;
			});
		} else {
			nodeAttacher.restoreState(savedInstanceState);
			nodeSaver.restoreState(savedInstanceState);
		}
	}

	/**
	 * Gets a list of the searched MegaNode from the handles list received as param.
	 *
	 * @param handles List of searched node handles.
	 * @return The list of the searched MegaNodes.
	 */
	private ArrayList<MegaNode> getSearchedNodes(ArrayList<String> handles) {
		ArrayList<MegaNode> nodes = new ArrayList<>();

		for (String handle : handles) {
			MegaNode node = megaApi.getNodeByHandle(Long.parseLong(handle));
			if (node != null) {
				nodes.add(node);
			}
		}

		return nodes;
	}

	/**
	 * Gets all the image handles to preview.
	 *
	 * @param nodes				 List of all nodes where search.
	 * @param savedInstanceState Saved instance state if exists, null otherwise.
	 */
	private void getImageHandles(List<MegaNode> nodes, Bundle savedInstanceState) {
		getImageHandles(nodes, savedInstanceState, false);
	}

	/**
	 * Gets all the handles to preview.
	 *
	 * @param nodes				 List of all nodes where search.
	 * @param savedInstanceState Saved instance state if exists, null otherwise.
	 * @param includeVideos		 True if should get video and image handles, false otherwise.
	 */
	private void getImageHandles(List<MegaNode> nodes, Bundle savedInstanceState, boolean includeVideos) {
		int imageNumber = 0;
		for (int i = 0; i < nodes.size(); i++) {
			MegaNode n = nodes.get(i);
			MimeTypeList mime = MimeTypeList.typeForName(n.getName());
			boolean isImageHandle = includeVideos
					? mime.isImage() || mime.isVideoReproducible()
					: mime.isImage();

			if (isImageHandle) {
				imageHandles.add(n.getHandle());
				if (i == positionG && savedInstanceState == null) {
					positionG = imageNumber;
				}
				imageNumber++;
			}
		}

		if (imageHandles.isEmpty()) finish();

		if (positionG >= imageHandles.size()) positionG = 0;

		fileNameTextView.setText(megaApi.getNodeByHandle(imageHandles.get(positionG)).getName());

		adapterMega = new MegaFullScreenImageAdapterLollipop(this, fullScreenImageViewer,
				imageHandles, megaApi, this);
	}

	private final BroadcastReceiver receiverToFinish = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				finish();
			}
		}
	};

	public void updateScrollPosition(){
		logDebug("updateScrollPosition");
	    if (adapterType == OFFLINE_ADAPTER){
	        String name = mOffListImages.get(positionG).getName();

            for (int i=0; i<mOffList.size(); i++){
				logDebug("Name: " + name + " mOfflist name: " + mOffList.get(i).getName());
                if (mOffList.get(i).getName().equals(name)) {
                	dragToExit.nodeChanged(Long.parseLong(mOffList.get(i).getHandle()));
                    break;
                }
            }
		} else if (adapterType == PHOTO_SYNC_ADAPTER || adapterType == SEARCH_BY_ADAPTER
				|| adapterType == SEARCH_ADAPTER || adapterType == PHOTOS_BROWSE_ADAPTER
				|| adapterType == PHOTOS_SEARCH_ADAPTER || adapterType == RECENTS_BUCKET_ADAPTER) {
			dragToExit.nodeChanged(adapterMega.getImageHandle(positionG));
		}
		else if (adapterType == ZIP_ADAPTER) {
			String name = new File(paths.get(positionG)).getName();
			for (int i = 0; i< zipFiles.size(); i++) {
				if (zipFiles.get(i).getName().equals(name)) {
					dragToExit.nodeChanged(name.hashCode());
				}
			}
		}
        else {
            Long handle = adapterMega.getImageHandle(positionG);
			ArrayList<MegaNode> listNodes;
			if (isInRootLinksLevel(adapterType, parentNodeHandle)) {
				listNodes = megaApi.getPublicLinks(orderGetChildren);
			} else {
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(handle));
				listNodes = megaApi.getChildren(parentNode, orderGetChildren);
			}

            for (int i=0; i<listNodes.size(); i++){
                if (listNodes.get(i).getHandle() == handle){
					dragToExit.nodeChanged(handle);
                    break;
                }
            }
        }
	}

	@Override
	public void onPageSelected(int position) {
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		logDebug("State: " + state);

		supportInvalidateOptionsMenu();
		if (state == ViewPager.SCROLL_STATE_IDLE){
			if (viewPager.getCurrentItem() != positionG){
				int oldPosition = positionG;
				int newPosition = viewPager.getCurrentItem();
				positionG = newPosition;
				if ((adapterType == OFFLINE_ADAPTER)){
					fileNameTextView.setText(mOffListImages.get(positionG).getName());
				}
				else if(adapterType == ZIP_ADAPTER){
					fileNameTextView.setText(new File(paths.get(positionG)).getName());
				}
				else{
					try {
						TouchImageView tIV = (TouchImageView) adapterMega.getVisibleImage(oldPosition);
						if (tIV != null) {
							tIV.setZoom(1);
						}
					}
					catch (Exception e) {
						logError(e.getMessage());
					}
					fileNameTextView.setText(megaApi.getNodeByHandle(imageHandles.get(positionG)).getName());
				}
//				title.setText(names.get(positionG));
				updateScrollPosition();
			}
		}
	}

	@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		logDebug("onRequestPermissionsResult");
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		nodeSaver.handleRequestPermissionsResult(requestCode);
    }

	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		logDebug("onSaveInstanceState");
		super.onSaveInstanceState(savedInstanceState);
		if (getIntent() != null) {
			getIntent().putExtra("position", positionG);
			if (adapterType == RECENTS_ADAPTER || adapterType == RECENTS_BUCKET_ADAPTER) {
				getIntent().putExtra(HANDLE, imageHandles.get(positionG));
			}
		}
		savedInstanceState.putInt("adapterType", adapterType);
		if (adapterType != OFFLINE_ADAPTER && adapterType != ZIP_ADAPTER) {
			savedInstanceState.putBoolean("aBshown", adapterMega.isaBshown());
			savedInstanceState.putBoolean("overflowVisible", adapterMega.isMenuVisible());
		}

		nodeAttacher.saveState(savedInstanceState);
		nodeSaver.saveState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState (Bundle savedInstanceState){
		logDebug("onRestoreInstanceState");
		super.onRestoreInstanceState(savedInstanceState);

		adapterType = savedInstanceState.getInt("adapterType");

		if (adapterType != OFFLINE_ADAPTER && adapterType != ZIP_ADAPTER){
			boolean aBshown = savedInstanceState.getBoolean("aBshown");
			adapterMega.setaBshown(aBshown);
		}
	}

	public void showMove(){
		logDebug("showMove");

		node = megaApi.getNodeByHandle(imageHandles.get(positionG));

		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());

		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}

	public void showCopy(){
		logDebug("showCopy");

		node = megaApi.getNodeByHandle(imageHandles.get(positionG));

		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());

		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getRequestString());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {

		node = megaApi.getNodeByHandle(request.getNodeHandle());

		logDebug("onRequestFinish");
        if (request.getType() == MegaRequest.TYPE_MOVE) {
            logDebug("Move nodes request finished");

            if (e.getErrorCode() == MegaError.API_OK) {
                showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_correctly_moved), -1);
                finish();
            } else if (e.getErrorCode() == MegaError.API_EOVERQUOTA && api.isForeignNode(request.getParentHandle())) {
				showForeignStorageOverQuotaWarningDialog(this);
			} else {
                showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_no_moved), -1);
            }
        } else if (request.getType() == MegaRequest.TYPE_COPY) {
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {
				logError(ex.getMessage());
			}

			if (e.getErrorCode() == MegaError.API_OK){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied), -1);
			}
			else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
				if (api.isForeignNode(request.getParentHandle())) {
					showForeignStorageOverQuotaWarningDialog(this);
					return;
				}

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
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
			}
			logDebug("Copy nodes request finished");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getRequestString());
	}

	public void openAdvancedDevices (long handleToDownload, boolean highPriority){
		logDebug("handleToDownload: " + handleToDownload + ", highPriority: " + highPriority);
//		handleToDownload = handle;
		String externalPath = getExternalCardPath();

		if(externalPath!=null){
			logDebug("ExternalPath for advancedDevices: " + externalPath);
			MegaNode node = megaApi.getNodeByHandle(handleToDownload);
			if(node!=null){

//				File newFile =  new File(externalPath+"/"+node.getName());
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		logDebug("onActivityResult");

		if (nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this)) {
			return;
		}

		if (nodeSaver.handleActivityResult(requestCode, resultCode, intent)) {
			return;
		}

		if (requestCode == WRITE_SD_CARD_REQUEST_CODE && resultCode == RESULT_OK) {
			if (intent == null) {
				logWarning("Intent is null");
				return;
			}

			boolean hasStoragePermission = hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (!hasStoragePermission) {
				requestPermission(this,
						REQUEST_WRITE_STORAGE,
						Manifest.permission.WRITE_EXTERNAL_STORAGE);
			}

			if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
				showOverDiskQuotaPaywallWarning();
				return;
			}

			Uri treeUri = intent.getData();
			logDebug("Create the node : " + treeUri);
			long handleToDownload = intent.getLongExtra("handleToDownload", -1);
			boolean highPriority = intent.getBooleanExtra(HIGH_PRIORITY_TRANSFER, false);
			logDebug("The recovered handle is: " + handleToDownload);
			//Now, call to the DownloadService

			if(handleToDownload!=0 && handleToDownload!=-1){
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_HASH, handleToDownload);
				service.putExtra(DownloadService.EXTRA_CONTENT_URI, treeUri.toString());
				String path = getCacheFolder(this, TEMPORAL_FOLDER).getAbsolutePath();
				service.putExtra(DownloadService.EXTRA_PATH, path);
				service.putExtra("fromMV", true);
				service.putExtra(HIGH_PRIORITY_TRANSFER, highPriority);
				startService(service);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {
			if (intent == null) {
				logWarning("Intent is null");
				return;
			}

			if(!isOnline(this)){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return;
			}

			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);
			final int totalMoves = moveHandles.length;

			MegaNode parent = megaApi.getNodeByHandle(toHandle);

			AlertDialog temp;
			try{
				temp = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.context_moving));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;

			for (long moveHandle : moveHandles) {
				megaApi.moveNode(megaApi.getNodeByHandle(moveHandle), parent, this);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
			if (intent == null) {
				logWarning("Intent is null");
				return;
			}

			if(!isOnline(this)){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return;
			}

			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;

			AlertDialog temp;
			try{
				temp = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.context_copying));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;

			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for(int i=0; i<copyHandles.length;i++){
				MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
				if (cN != null){
					logDebug("cN != null, i = " + i + " of " + copyHandles.length);
					megaApi.copyNode(cN, parent, this);
				}
				else{
					logDebug("cN == null, i = " + i + " of " + copyHandles.length);
					try {
						statusDialog.dismiss();
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
					}
					catch (Exception ex) {
						logError(ex.getMessage());
					}
				}
			}
		}
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {}

	@Override
	public void onTouchImage() {
		if (aB.isShowing()) {
			hideActionBar();
		} else {
			showActionBar();
		}
	}

	@Override
	public void onPlayVideo() {
		MegaNode video = megaApi.getNodeByHandle(imageHandles.get(positionG));

		Intent mediaIntent;
		if (MimeTypeList.typeForName(video.getName()).isVideoNotSupported()) {
			mediaIntent = new Intent(Intent.ACTION_VIEW);
		} else {
			mediaIntent = getMediaIntent(this, node.getName());
		}

		mediaIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, video.getHandle())
				.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTO_SYNC_ADAPTER)
				.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false);

		mediaIntent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.getName());

		boolean paramsSetSuccessfully;
		String localPath = null;

		try {
			localPath = findVideoLocalPath(this, node);
		} catch (Exception e) {
			logWarning(e.getMessage());
		}

		if (localPath != null && checkFingerprint(megaApi, node, localPath)) {
			paramsSetSuccessfully = setLocalIntentParams(this, node, mediaIntent, localPath,
					false, this);
		} else {
			paramsSetSuccessfully = setStreamingIntentParams(this, node, megaApi,
					mediaIntent, this);
		}

		if (!isIntentAvailable(this, mediaIntent)) {
			showSnackbar(SNACKBAR_TYPE,
					StringResourcesUtils.getString(R.string.intent_not_available), MEGACHAT_INVALID_HANDLE);

			paramsSetSuccessfully = false;
		}

		if (paramsSetSuccessfully) {
			startActivity(mediaIntent);
			overridePendingTransition(0, 0);
		}
	}

	@Override
	public void onStartHttpServer() {
		needStopHttpServer = true;
	}

	protected void hideActionBar(){
		logDebug("hideActionBar");

		if (aB != null && aB.isShowing()) {
			if(tB != null) {
				tB.animate().translationY(-220).setDuration(ANIMATION_DURATION)
						.withEndAction(() -> aB.hide()).start();
				bottomLayout.animate().translationY(220).setDuration(ANIMATION_DURATION).start();
			} else {
				aB.hide();
			}
		}
	}
	protected void showActionBar(){
		logDebug("showActionBar");

		if (aB != null && !aB.isShowing()) {
			aB.show();
			if(tB != null) {
				tB.animate().translationY(0).setDuration(ANIMATION_DURATION).start();
				bottomLayout.animate().translationY(0).setDuration(ANIMATION_DURATION).start();
			}
		}
	}

	@Override
	protected void onResume(){
		logDebug("onResume");
		super.onResume();
		if (adapterType != OFFLINE_ADAPTER && adapterType != FILE_LINK_ADAPTER && adapterType != ZIP_ADAPTER){
			if (imageHandles.get(positionG) != -1){
				logDebug("Node updated");
				node = megaApi.getNodeByHandle(imageHandles.get(positionG));
			}

			if (node == null){
				finish();
			}
		}
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {

	}

	@Override
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		logDebug("onUserAlertsUpdate");
	}

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {

		logDebug("onNodesUpdate");

		if ((adapterType != OFFLINE_ADAPTER) && adapterType != FILE_LINK_ADAPTER && adapterType != ZIP_ADAPTER) {
			boolean thisNode = false;
			if(nodes==null){
				return;
			}


			for (MegaNode n : nodes) {
				if (n != null && positionG < imageHandles.size() && n.getHandle() == imageHandles.get(positionG)) {
					thisNode = true;
				}
			}

			if (!thisNode){
				logWarning("Not related to this node");
				return;
			}

			if (positionG < imageHandles.size() && imageHandles.get(positionG) != -1){
				logDebug("Node updated");
				node = megaApi.getNodeByHandle(imageHandles.get(positionG));
			}

			if (node == null){
				return;
			}

			fileNameTextView.setText(node.getName());
			supportInvalidateOptionsMenu();
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {}

	@Override
	public void onAccountUpdate(MegaApiJava api) {}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {}

	public void setNormalizedScale(float normalizedScale) {
		dragToExit.setNormalizedScale(normalizedScale);
	}

	public void setDraggable(boolean draggable) {
		dragToExit.setDraggable(draggable);
	}

	private Unit onDragActivated(boolean activated) {
		logDebug("activated: " + activated);

		if (activated) {
			updateViewForAnimation();

			if (adapterType == OFFLINE_ADAPTER){
				dragToExit.setCurrentView(adapterOffline.getVisibleImage(positionG));
			} else if (adapterType == ZIP_ADAPTER) {
				dragToExit.setCurrentView(adapterOffline.getVisibleImage(positionG));
			} else {
				dragToExit.setCurrentView(adapterMega.getVisibleImage(positionG));
			}
		} else {
			handler.postDelayed(() -> {
				fragmentContainer.setBackgroundColor(BLACK);
				relativeImageViewerLayout.setBackgroundColor(BLACK);
				appBarLayout.setBackgroundColor(BLACK);
			}, 300);
		}

		return null;
	}

	private void updateViewForAnimation() {
		if (aB != null && aB.isShowing()) {
			if(tB != null) {
				tB.animate().translationY(-220).setDuration(0)
						.withEndAction(() -> aB.hide()).start();
				bottomLayout.animate().translationY(220).setDuration(0).start();
			} else {
				aB.hide();
			}
		}

		fragmentContainer.setBackgroundColor(TRANSPARENT);
		relativeImageViewerLayout.setBackgroundColor(TRANSPARENT);
		appBarLayout.setBackgroundColor(TRANSPARENT);

		fragmentContainer.setElevation(0);
		relativeImageViewerLayout.setElevation(0);
		appBarLayout.setElevation(0);
	}

	public boolean isFolderLink () {
		return isFolderLink;
	}

	public boolean isFileLink() {
		return isFileLink;
	}

	public MegaNode getCurrentDocument() {
		return currentDocument;
	}

	@Override
	public void showSnackbar(int type, String content, long chatId) {
		showSnackbar(type, fragmentContainer, content, chatId);
	}
}
