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
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import com.google.android.material.appbar.AppBarLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.ExtendedViewPager;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.components.dragger.DraggableView;
import mega.privacy.android.app.components.dragger.ExitViewAnimator;
import mega.privacy.android.app.fragments.homepage.photos.PhotosFragment;
import mega.privacy.android.app.fragments.managerFragments.LinksFragment;
import mega.privacy.android.app.fragments.offline.OfflineFragment;
import mega.privacy.android.app.fragments.managerFragments.cu.CameraUploadsFragment;
import mega.privacy.android.app.lollipop.adapters.MegaFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaOfflineFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.InboxFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.IncomingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OutgoingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment;
import mega.privacy.android.app.lollipop.managerSections.RubbishBinFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;
import mega.privacy.android.app.utils.DraggingThumbnailCallback;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
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
import static mega.privacy.android.app.SearchNodesTask.*;
import static mega.privacy.android.app.lollipop.FileInfoActivityLollipop.*;
import static mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop.*;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static nz.mega.sdk.MegaApiJava.*;
import static mega.privacy.android.app.utils.Util.*;

public class FullScreenImageViewerLollipop extends DownloadableActivity implements OnPageChangeListener, MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaChatRequestListenerInterface, DraggableView.DraggableListener{

	private static final Map<Class<?>, DraggingThumbnailCallback> DRAGGING_THUMBNAIL_CALLBACKS
			= new HashMap<>(DraggingThumbnailCallback.DRAGGING_THUMBNAIL_CALLBACKS_SIZE);

	int[] screenPosition;
	int mLeftDelta;
	int mTopDelta;
	float mWidthScale;
	float mHeightScale;
	int placeholderCount;

	private DisplayMetrics outMetrics;

	private boolean aBshown = true;

	ProgressDialog statusDialog;

	AppBarLayout appBarLayout;
	Toolbar tB;
	ActionBar aB;

	private boolean isGetLink = false;
	float scaleText;

	Context context;
	MegaNode currentDocument;
	String url;

	int positionToRemove = -1;
	String regex = "[*|\\?:\"<>\\\\\\\\/]";

	NodeController nC;
	boolean isFileLink;

	private MegaFullScreenImageAdapterLollipop adapterMega;
	private MegaOfflineFullScreenImageAdapterLollipop adapterOffline;

	private int positionG;
	private ArrayList<Long> imageHandles;
	private boolean fromShared = false;
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

	private androidx.appcompat.app.AlertDialog downloadConfirmationDialog;

	MegaOffline currentNode;

	private RelativeLayout bottomLayout;
	private ExtendedViewPager viewPager;

	static FullScreenImageViewerLollipop fullScreenImageViewer;
    private MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

    private ArrayList<String> paths;
	private String offlinePathDirectory;

    int adapterType = 0;
    long[] handlesNodesSearched;

	int countChat = 0;
	int errorSent = 0;
	int successSent = 0;

    public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;


	MegaNode node;

	int typeExport = -1;

	boolean shareIt = true;
	boolean moveToRubbish = false;

	private static int EDIT_TEXT_ID = 1;
	private Handler handler;

	private androidx.appcompat.app.AlertDialog renameDialog;

	int orderGetChildren = ORDER_DEFAULT_ASC;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	boolean isFolderLink = false;

	ArrayList<Long> handleListM = new ArrayList<Long>();

	ArrayList<MegaOffline> mOffList;
	ArrayList<MegaOffline> mOffListImages;
	String pathImage;


	public DraggableView draggableView;
	public static int screenHeight;
	int screenWidth;
	RelativeLayout relativeImageViewerLayout;
	ImageView ivShadow;

	ArrayList<File> zipFiles = new ArrayList<>();

	private long parentNodeHandle = INVALID_HANDLE;

	public static void addDraggingThumbnailCallback(Class<?> clazz, DraggingThumbnailCallback cb) {
		DRAGGING_THUMBNAIL_CALLBACKS.put(clazz, cb);
	}

	public static void removeDraggingThumbnailCallback(Class<?> clazz) {
		DRAGGING_THUMBNAIL_CALLBACKS.remove(clazz);
	}

	@Override
	public void onDestroy(){

		setImageDragVisibility(View.VISIBLE);

		if(megaApi != null){
			megaApi.removeRequestListener(this);
			megaApi.removeGlobalListener(this);
		}

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverToFinish);

		DRAGGING_THUMBNAIL_CALLBACKS.clear();

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

		return getLocalFile(this, node.getName(), node.getSize()) != null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		logDebug("onCreateOptionsMenu");

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_full_screen_image_viewer, menu);

		getlinkIcon = menu.findItem(R.id.full_image_viewer_get_link);
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
		chatIcon.setIcon(mutateIconSecondary(this, R.drawable.ic_send_to_contact, R.color.white));

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

			downloadIcon.setVisible(false);
			menu.findItem(R.id.full_image_viewer_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			renameIcon.setVisible(false);
			moveIcon.setVisible(false);
			copyIcon .setVisible(false);
			moveToTrashIcon.setVisible(false);
			removeIcon.setVisible(false);
			chatIcon.setVisible(false);

		}else if (adapterType == ZIP_ADAPTER){

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
		else if (adapterType == RECENTS_ADAPTER) {
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

					if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){

						moveToTrashIcon.setVisible(true);
						removeIcon.setVisible(false);

					}else{

						moveToTrashIcon.setVisible(false);
						removeIcon.setVisible(true);
						getlinkIcon.setVisible(false);
						removelinkIcon.setVisible(false);
					}
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
					if (showTakenDownNodeActionNotAvailableDialog(node, context)) {
						return false;
					}
					shareIt = false;
			    	showGetLinkActivity(node.getHandle());
					break;
				}

			}

			case R.id.full_image_viewer_chat:{
				if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
					showOverDiskQuotaPaywallWarning();
					break;
				}

//				node = megaApi.getNodeByHandle(imageHandles.get(positionG));

//				ArrayList<Long> handleList = new ArrayList<Long>();
//				handleList.add(node.getHandle());
//
//				long[] longArray = new long[handleList.size()];
//				for (int i=0; i<handleList.size(); i++){
//					longArray[i] = handleList.get(i);
//				}

				long[] longArray = new long[1];
				longArray[0] = imageHandles.get(positionG);

				if(nC ==null){
					nC = new NodeController(this, isFolderLink);
				}

				MegaNode attachNode = megaApi.getNodeByHandle(longArray[0]);
				if (attachNode != null) {
					nC.checkIfNodeIsMineAndSelectChatsToSendNode(attachNode);
				}

				break;
			}

			case R.id.full_image_viewer_remove_link: {
				shareIt = false;

				node = megaApi.getNodeByHandle(imageHandles.get(positionG));
				if (showTakenDownNodeActionNotAvailableDialog(node, context)) {
					return false;
				}
				androidx.appcompat.app.AlertDialog removeLinkDialog;
				androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);

				LayoutInflater inflater = getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
				TextView url = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_url);
				TextView key = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_key);
				TextView symbol = (TextView) dialoglayout.findViewById(R.id.dialog_link_symbol);
				TextView removeText = (TextView) dialoglayout.findViewById(R.id.dialog_link_text_remove);

				((RelativeLayout.LayoutParams) removeText.getLayoutParams()).setMargins(scaleWidthPx(25, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(10, outMetrics), 0);

				url.setVisibility(View.GONE);
				key.setVisibility(View.GONE);
				symbol.setVisibility(View.GONE);
				removeText.setVisibility(View.VISIBLE);

				removeText.setText(getString(R.string.context_remove_link_warning_text));

				Display display = getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics();
				display.getMetrics(outMetrics);
				screenHeight = outMetrics.heightPixels;
				screenWidth = outMetrics.widthPixels;
				float density = getResources().getDisplayMetrics().density;

				float scaleW = getScaleW(outMetrics, density);
				float scaleH = getScaleH(outMetrics, density);
				if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (10*scaleW));
				}else{
					removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (15*scaleW));

				}

				builder.setView(dialoglayout);

				builder.setPositiveButton(getString(R.string.context_remove), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						typeExport=TYPE_EXPORT_REMOVE;
						megaApi.disableExport(node, fullScreenImageViewer);
					}
				});

				builder.setNegativeButton(getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
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

					if (adapterType == SEARCH_ADAPTER || adapterType == RECENTS_ADAPTER) {
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

				if (adapterType == OFFLINE_ADAPTER){
					break;

				}else if (adapterType == ZIP_ADAPTER){
					break;

				}else if (adapterType == FILE_LINK_ADAPTER){
					logDebug("Click download");
					if (nC == null) {
						nC = new NodeController(this);
					}
					nC.downloadFileLink(currentDocument, url);
					break;
				}else{
					node = megaApi.getNodeByHandle(imageHandles.get(positionG));
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
						if (!hasStoragePermission) {
							ActivityCompat.requestPermissions(this,
									new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									REQUEST_WRITE_STORAGE);

							handleListM.add(node.getHandle());

							break;
						}
					}
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(node.getHandle());
					if(nC==null){
						nC = new NodeController(this, isFolderLink);
					}
					nC.prepareForDownload(handleList, false);

					break;
				}
			}
			case R.id.full_image_viewer_rename: {
				showRenameDialog();
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
			case R.id.full_image_viewer_move_to_trash: {
				positionToRemove = positionG;
				moveToTrash();
				break;
			}
			case R.id.full_image_viewer_remove: {
				moveToTrash();
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_screen_image_viewer);

		relativeImageViewerLayout = findViewById(R.id.full_image_viewer_layout);

		draggableView.setViewAnimator(new ExitViewAnimator<>());

		handler = new Handler();
		fullScreenImageViewer = this;

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG));
		LocalBroadcastManager.getInstance(this).registerReceiver(receiverToFinish, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN));

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = getResources().getDisplayMetrics().density;


		appBarLayout = findViewById(R.id.app_bar);

		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}

		viewPager = findViewById(R.id.image_viewer_pager);
		viewPager.setPageMargin(40);

		fragmentContainer = findViewById(R.id.full_image_viewer_parent_layout);

		Intent intent = getIntent();
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
        MegaApplication app = (MegaApplication)getApplication();
		if (isFolderLink ){
			megaApi = app.getMegaApiFolder();
		}else{
			megaApi = app.getMegaApi();
		}

		if(isOnline(this) && !isFileLink) {
			if (megaApi == null || megaApi.getRootNode() == null) {
				logDebug("Refresh session - sdk");
				Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
				intentLogin.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
				intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intentLogin);
				finish();
				return;
			}
		}

		if (megaChatApi == null) {
			megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
		}
		if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
			Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
			intentLogin.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
			intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intentLogin);
			finish();
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
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		aB.setHomeButtonEnabled(true);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setTitle(" ");

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		imageHandles = new ArrayList<>();
		paths = new ArrayList<>();
		parentNodeHandle = intent.getLongExtra("parentNodeHandle", INVALID_HANDLE);
		fromShared = intent.getBooleanExtra("fromShared", false);
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

					adapterMega = new MegaFullScreenImageAdapterLollipop(this, fullScreenImageViewer, imageHandles, megaApi);
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
			Collections.sort(zipFiles, new Comparator<File>(){

				public int compare(File z1, File z2) {
					String name1 = z1.getName();
					String name2 = z2.getName();
					int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
					if (res == 0) {
						res = name1.compareTo(name2);
					}
					return res;
				}
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
		else if (adapterType == RECENTS_ADAPTER) {
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
			adapterMega = new MegaFullScreenImageAdapterLollipop(this, fullScreenImageViewer, imageHandles, megaApi);
		} else if (isInRootLinksLevel(adapterType, parentNodeHandle)) {
			getImageHandles(megaApi.getPublicLinks(orderGetChildren), savedInstanceState);
		} else if (adapterType == PHOTOS_BROWSE_ADAPTER) {
			// TODO: use constants
			getImageHandles(megaApi.searchByType(null, null, null, true, orderGetChildren, 1, 3), savedInstanceState);
		} else {
			if (parentNodeHandle == INVALID_HANDLE){
				switch(adapterType){
					case FILE_BROWSER_ADAPTER:{
						parentNode = megaApi.getRootNode();
						break;
					}
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
			ViewTreeObserver observer = viewPager.getViewTreeObserver();
			observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@Override
				public boolean onPreDraw() {

					viewPager.getViewTreeObserver().removeOnPreDrawListener(this);
					int[] location = new int[2];
					viewPager.getLocationOnScreen(location);
					int[] getlocation = new int[2];
					getLocationOnScreen(getlocation);
					if (screenPosition != null){
						mLeftDelta = getlocation[0] - location[0];
						mTopDelta = getlocation[1] - location[1];

						mWidthScale = (float) screenPosition[2] / viewPager.getWidth();
						mHeightScale = (float) screenPosition[3] / viewPager.getHeight();
					}
					else {
						mLeftDelta = (screenWidth/2) - location[0];
						mTopDelta = (screenHeight/2) - location[1];

						mWidthScale = (float) (screenWidth/4) / viewPager.getWidth();
						mHeightScale = (float) (screenHeight/4) / viewPager.getHeight();
					}

					runEnterAnimation();

					return true;
				}
			});
		}
	}

	private void getImageHandles(ArrayList<MegaNode> nodes, Bundle savedInstanceState) {
		int imageNumber = 0;
		for (int i = 0; i < nodes.size(); i++) {
			MegaNode n = nodes.get(i);
			if (MimeTypeList.typeForName(n.getName()).isImage()) {
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

		adapterMega = new MegaFullScreenImageAdapterLollipop(this, fullScreenImageViewer, imageHandles, megaApi);
	}

	public void setImageDragVisibility(int visibility){
		logDebug("Visibility: " + visibility);
		if (adapterType == RUBBISH_BIN_ADAPTER){
			if (RubbishBinFragmentLollipop.imageDrag != null){
				RubbishBinFragmentLollipop.imageDrag.setVisibility(visibility);
			}
		}
		else if (adapterType == INBOX_ADAPTER){
			if (InboxFragmentLollipop.imageDrag != null){
				InboxFragmentLollipop.imageDrag.setVisibility(visibility);
			}
		}
		else if (adapterType == INCOMING_SHARES_ADAPTER){
			if (IncomingSharesFragmentLollipop.imageDrag != null) {
				IncomingSharesFragmentLollipop.imageDrag.setVisibility(visibility);
			}
		}
		else if (adapterType == OUTGOING_SHARES_ADAPTER){
			if (OutgoingSharesFragmentLollipop.imageDrag != null){
				OutgoingSharesFragmentLollipop.imageDrag.setVisibility(visibility);
			}
		}
		else if (adapterType == CONTACT_FILE_ADAPTER){
			if (ContactFileListFragmentLollipop.imageDrag != null){
				ContactFileListFragmentLollipop.imageDrag.setVisibility(visibility);
			}
		}
		else if (adapterType == FOLDER_LINK_ADAPTER){
			if (FolderLinkActivityLollipop.imageDrag != null){
				FolderLinkActivityLollipop.imageDrag.setVisibility(visibility);
			}
		}
		else if (adapterType == SEARCH_ADAPTER){
			if (SearchFragmentLollipop.imageDrag != null){
				SearchFragmentLollipop.imageDrag.setVisibility(visibility);
			}
		}
		else if (adapterType == FILE_BROWSER_ADAPTER){
			if (FileBrowserFragmentLollipop.imageDrag != null){
				FileBrowserFragmentLollipop.imageDrag.setVisibility(visibility);
			}
		}
		else if (adapterType == PHOTO_SYNC_ADAPTER ||adapterType == SEARCH_BY_ADAPTER) {
			DraggingThumbnailCallback callback
					= DRAGGING_THUMBNAIL_CALLBACKS.get(CameraUploadsFragment.class);
			if (callback != null) {
				callback.setVisibility(visibility);
			}
		}
		else if (adapterType == OFFLINE_ADAPTER) {
			OfflineFragment.setDraggingThumbnailVisibility(visibility);
		}
		else if (adapterType == ZIP_ADAPTER) {
			if (ZipBrowserActivityLollipop.imageDrag != null) {
				ZipBrowserActivityLollipop.imageDrag.setVisibility(visibility);
			}
		} else if (adapterType == LINKS_ADAPTER) {
			if (LinksFragment.imageDrag != null) {
				LinksFragment.imageDrag.setVisibility(visibility);
			}
		} else if (adapterType == RECENTS_ADAPTER && RecentsFragment.imageDrag != null) {
			RecentsFragment.imageDrag.setVisibility(visibility);
		} else if (adapterType == PHOTOS_BROWSE_ADAPTER || adapterType == PHOTOS_SEARCH_ADAPTER) {
			DraggingThumbnailCallback callback
					= DRAGGING_THUMBNAIL_CALLBACKS.get(PhotosFragment.class);
			if (callback != null) {
				callback.setVisibility(visibility);
			}
		}
	}

	void getLocationOnScreen(int[] location){
		logDebug("getLocationOnScreen");
		if (adapterType == RUBBISH_BIN_ADAPTER){
			if (RubbishBinFragmentLollipop.imageDrag != null) {
				RubbishBinFragmentLollipop.imageDrag.getLocationOnScreen(location);
			}
		}
		else if (adapterType == INBOX_ADAPTER){
			if (InboxFragmentLollipop.imageDrag != null){
				InboxFragmentLollipop.imageDrag.getLocationOnScreen(location);
			}
		}
		else if (adapterType == INCOMING_SHARES_ADAPTER){
			if (IncomingSharesFragmentLollipop.imageDrag != null) {
				IncomingSharesFragmentLollipop.imageDrag.getLocationOnScreen(location);
			}
		}
		else if (adapterType == OUTGOING_SHARES_ADAPTER){
			if (OutgoingSharesFragmentLollipop.imageDrag != null) {
				OutgoingSharesFragmentLollipop.imageDrag.getLocationOnScreen(location);
			}
		}
		else if (adapterType == CONTACT_FILE_ADAPTER){
			if (ContactFileListFragmentLollipop.imageDrag != null) {
				ContactFileListFragmentLollipop.imageDrag.getLocationOnScreen(location);
			}
		}
		else if (adapterType == FOLDER_LINK_ADAPTER){
			if (FolderLinkActivityLollipop.imageDrag != null) {
				FolderLinkActivityLollipop.imageDrag.getLocationOnScreen(location);
			}
		}
		else if (adapterType == SEARCH_ADAPTER){
			if (SearchFragmentLollipop.imageDrag != null){
				SearchFragmentLollipop.imageDrag.getLocationOnScreen(location);
			}
		}
		else if (adapterType == FILE_BROWSER_ADAPTER){
			if (FileBrowserFragmentLollipop.imageDrag != null){
				FileBrowserFragmentLollipop.imageDrag.getLocationOnScreen(location);
			}
		}
		else if (adapterType == PHOTO_SYNC_ADAPTER || adapterType == SEARCH_BY_ADAPTER) {
			DraggingThumbnailCallback callback
					= DRAGGING_THUMBNAIL_CALLBACKS.get(CameraUploadsFragment.class);
			if (callback != null) {
				callback.getLocationOnScreen(location);
			}
		}
		else if (adapterType == OFFLINE_ADAPTER){
			OfflineFragment.getDraggingThumbnailLocationOnScreen(location);
		}
		else if (adapterType == ZIP_ADAPTER) {
			if (ZipBrowserActivityLollipop.imageDrag != null) {
				ZipBrowserActivityLollipop.imageDrag.getLocationOnScreen(location);
			}
		} else if (adapterType == LINKS_ADAPTER) {
			if (LinksFragment.imageDrag != null) {
				LinksFragment.imageDrag.getLocationOnScreen(location);
			}
		} else if (adapterType == RECENTS_ADAPTER && RecentsFragment.imageDrag != null) {
			RecentsFragment.imageDrag.getLocationOnScreen(location);
		} else if (adapterType == PHOTOS_BROWSE_ADAPTER || adapterType == PHOTOS_SEARCH_ADAPTER) {
			DraggingThumbnailCallback callback
					= DRAGGING_THUMBNAIL_CALLBACKS.get(PhotosFragment.class);
			if (callback != null) {
				callback.getLocationOnScreen(location);
			}
		}
	}

	public void runEnterAnimation() {
		logDebug("runEnterAnimation");
		final long duration = 400;
		if (aB != null && aB.isShowing()) {
			if(tB != null) {
				tB.animate().translationY(-220).setDuration(0)
						.withEndAction(new Runnable() {
							@Override
							public void run() {
								aB.hide();
							}
						}).start();
				bottomLayout.animate().translationY(220).setDuration(0).start();
			} else {
				aB.hide();
			}
		}

		fragmentContainer.setBackgroundColor(TRANSPARENT);
		relativeImageViewerLayout.setBackgroundColor(TRANSPARENT);
		appBarLayout.setBackgroundColor(TRANSPARENT);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			fragmentContainer.setElevation(0);
			relativeImageViewerLayout.setElevation(0);
			appBarLayout.setElevation(0);

		}

		viewPager.setPivotX(0);
		viewPager.setPivotY(0);
		viewPager.setScaleX(mWidthScale);
		viewPager.setScaleY(mHeightScale);
		viewPager.setTranslationX(mLeftDelta);
		viewPager.setTranslationY(mTopDelta);

		ivShadow.setImageAlpha(0);

		viewPager.animate().setDuration(duration).scaleX(1).scaleY(1).translationX(0).translationY(0).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
			@Override
			public void run() {
				showActionBar();
				fragmentContainer.setBackgroundColor(BLACK);
				relativeImageViewerLayout.setBackgroundColor(BLACK);
				appBarLayout.setBackgroundColor(BLACK);
			}
		});

		ivShadow.animate().setDuration(duration).alpha(1);
	}

	public void updateCurrentImage(){
	    if (adapterType == OFFLINE_ADAPTER){
	        String name = mOffListImages.get(positionG).getName();
            for (int i=0; i<mOffList.size(); i++){
				logDebug("Name: " + name + " mOfflist name: " + mOffList.get(i).getName());
                if (mOffList.get(i).getName().equals(name)){
                    getImageView(i, Long.parseLong(mOffList.get(i).getHandle()));
                    break;
                }
            }
		} else if (adapterType == PHOTO_SYNC_ADAPTER || adapterType == SEARCH_BY_ADAPTER
				|| adapterType == SEARCH_ADAPTER || adapterType == PHOTOS_BROWSE_ADAPTER
				|| adapterType == PHOTOS_SEARCH_ADAPTER) {
			Long handle = adapterMega.getImageHandle(positionG);
			getImageView(0, handle);
		}
		else if (adapterType == ZIP_ADAPTER) {
			String name = new File(paths.get(positionG)).getName();
			for (int i = 0; i< zipFiles.size(); i++) {
				if (zipFiles.get(i).getName().equals(name)) {
					getImageView(i, -1);
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
                    getImageView(i, handle);
                    break;
                }
            }
        }
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null){
				screenPosition = intent.getIntArrayExtra("screenPosition");
				draggableView.setScreenPosition(screenPosition);
			}
		}
	};

	private BroadcastReceiver receiverToFinish = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				finish();
			}
		}
	};

	public void getImageView (int i, long handle) {
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION);
		intent.putExtra("position", i);
		intent.putExtra("actionType", UPDATE_IMAGE_DRAG);
		intent.putExtra("adapterType", adapterType);
        intent.putExtra("placeholder",placeholderCount);
		intent.putExtra("handle", handle);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	public void updateScrollPosition(){
		logDebug("updateScrollPosition");
	    if (adapterType == OFFLINE_ADAPTER){
	        String name = mOffListImages.get(positionG).getName();

            for (int i=0; i<mOffList.size(); i++){
				logDebug("Name: " + name + " mOfflist name: " + mOffList.get(i).getName());
                if (mOffList.get(i).getName().equals(name)){
                    scrollToPosition(i, Long.parseLong(mOffList.get(i).getHandle()));
                    break;
                }
            }
		} else if (adapterType == PHOTO_SYNC_ADAPTER || adapterType == SEARCH_BY_ADAPTER
				|| adapterType == SEARCH_ADAPTER || adapterType == PHOTOS_BROWSE_ADAPTER
				|| adapterType == PHOTOS_SEARCH_ADAPTER) {
			Long handle = adapterMega.getImageHandle(positionG);
			scrollToPosition(0, handle);
		}
		else if (adapterType == ZIP_ADAPTER) {
			String name = new File(paths.get(positionG)).getName();
			for (int i = 0; i< zipFiles.size(); i++) {
				if (zipFiles.get(i).getName().equals(name)) {
					scrollToPosition(i, -1);
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
                    scrollToPosition(i, handle);
                    break;
                }
            }
        }
	}

	void scrollToPosition (int i, long handle) {
		getImageView(i, handle);
		Intent intent = new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION);
		intent.putExtra("position", i);
		intent.putExtra("actionType", SCROLL_TO_POSITION);
		intent.putExtra("adapterType", adapterType);
		intent.putExtra("handle", handle);
        intent.putExtra("placeholder",placeholderCount );
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
					catch (Exception e) {}
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
        switch(requestCode){
        	case REQUEST_WRITE_STORAGE:{
		        boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (hasStoragePermission) {
					if(nC==null){
						nC = new NodeController(this, isFolderLink);
					}
					if (adapterType == FILE_LINK_ADAPTER) {
						if (nC == null) {
							nC = new NodeController(this);
						}
						nC.downloadFileLink(currentDocument, url);
					}else{
						nC.prepareForDownload(handleListM, false);
					}
				}
	        	break;
	        }
        }
    }

	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		logDebug("onSaveInstanceState");
		super.onSaveInstanceState(savedInstanceState);
		if (getIntent() != null) {
			getIntent().putExtra("position", positionG);
			if (adapterType == RECENTS_ADAPTER) {
				getIntent().putExtra(HANDLE, imageHandles.get(positionG));
			}
		}
		savedInstanceState.putInt("adapterType", adapterType);
		if ((adapterType == OFFLINE_ADAPTER) || (adapterType == ZIP_ADAPTER)){

		}
		else{
			savedInstanceState.putBoolean("aBshown", adapterMega.isaBshown());
			savedInstanceState.putBoolean("overflowVisible", adapterMega.isMenuVisible());
		}
	}

	@Override
	public void onRestoreInstanceState (Bundle savedInstanceState){
		logDebug("onRestoreInstanceState");
		super.onRestoreInstanceState(savedInstanceState);

		adapterType = savedInstanceState.getInt("adapterType");

		if ((adapterType == OFFLINE_ADAPTER) || (adapterType == ZIP_ADAPTER)){

		}
		else{
			aBshown = savedInstanceState.getBoolean("aBshown");
			adapterMega.setaBshown(aBshown);
		}

	}

	public void showRenameDialog(){
		logDebug("showRenameDialog");
		node = megaApi.getNodeByHandle(imageHandles.get(positionG));

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);
	//	    layout.setLayoutParams(params);

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, node.isFolder());
		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),EditorInfo.IME_ACTION_DONE);
		input.setText(node.getName());


		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(final View v, boolean hasFocus) {
					if (hasFocus) {
						if (node.isFolder()){
							input.setSelection(0, input.getText().length());
						}
						else{
							String [] s = node.getName().split("\\.");
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

		final RelativeLayout error_layout = new RelativeLayout(FullScreenImageViewerLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(FullScreenImageViewerLollipop.this);
		error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout.addView(error_icon);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();

		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon.setLayoutParams(params_icon);

		error_icon.setColorFilter(ContextCompat.getColor(FullScreenImageViewerLollipop.this, R.color.login_warning));

		final TextView textError = new TextView(FullScreenImageViewerLollipop.this);
		error_layout.addView(textError);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(FullScreenImageViewerLollipop.this, R.color.login_warning));

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
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(fullScreenImageViewer, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
										  KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {

					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(fullScreenImageViewer, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_string));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();

					}else{
						boolean result=matches(regex, value);
						if(result){
							input.getBackground().mutate().setColorFilter(ContextCompat.getColor(fullScreenImageViewer, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
							textError.setText(getString(R.string.invalid_characters));
							error_layout.setVisibility(View.VISIBLE);
							input.requestFocus();

						}else{
	//						nC.renameNode(node, value);
							renameDialog.dismiss();
							rename(value);
						}
					}
					return true;
				}
				return false;
			}
		});

		androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.context_rename) + " "	+ new String(node.getName()));
		builder.setPositiveButton(getString(R.string.context_rename),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
							if (value.length() == 0) {
								return;
							}
							rename(value);
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
		renameDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String value = input.getText().toString().trim();

				if (value.length() == 0) {
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(fullScreenImageViewer, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError.setText(getString(R.string.invalid_string));
					error_layout.setVisibility(View.VISIBLE);
					input.requestFocus();
				}
				else{
					boolean result=matches(regex, value);
					if(result){
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(fullScreenImageViewer, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_characters));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();

					}else{
						//nC.renameNode(node, value);
						renameDialog.dismiss();
						rename(value);
					}
				}
			}
		});
	}

	public static boolean matches(String regex, CharSequence input) {
		logDebug("matches");

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(input);
		return m.find();
	}

	private void rename(String newName){
		logDebug("rename");

		if (newName.equals(node.getName())) {
			return;
		}

		if(!isOnline(this)){
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
			return;
		}

		if (isFinishing()){
			return;
		}

		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_renaming));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;

		logDebug("Renaming " + node.getName() + " to " + newName);

		megaApi.renameNode(node, newName, this);
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

	public void moveToTrash(){
		logDebug("moveToTrash");

		node = megaApi.getNodeByHandle(imageHandles.get(positionG));

		final long handle = node.getHandle();
		moveToRubbish = false;
		if (!isOnline(this)){
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
			return;
		}

		if(isFinishing()){
			return;
		}

		final MegaNode rubbishNode = megaApi.getRubbishNode();

		MegaNode parent = megaApi.getNodeByHandle(handle);
		while (megaApi.getParentNode(parent) != null){
			parent = megaApi.getParentNode(parent);
		}

		if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
			moveToRubbish = true;
		}
		else{
			moveToRubbish = false;
		}

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		    		//Check if the node is not yet in the rubbish bin (if so, remove it)

		    		if (moveToRubbish){
		    			megaApi.moveNode(megaApi.getNodeByHandle(handle), rubbishNode, fullScreenImageViewer);
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(fullScreenImageViewer);
		    				temp.setMessage(getString(R.string.context_move_to_trash));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}
		    		else{
		    			megaApi.remove(megaApi.getNodeByHandle(handle), fullScreenImageViewer);
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(fullScreenImageViewer);
		    				temp.setMessage(getString(R.string.context_delete_from_mega));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}


		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};

		if (moveToRubbish){
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_move_to_rubbish);
			builder.setMessage(message).setPositiveButton(R.string.general_move, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
		else{
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_delete_from_mega);
			builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
	}

	public void showGetLinkActivity(long handle){
		logDebug("Handle: " + handle);
		Intent linkIntent = new Intent(this, GetLinkActivityLollipop.class);
		linkIntent.putExtra("handle", handle);
		startActivity(linkIntent);
	}

	public void setIsGetLink(boolean value){
		logDebug("Value: " + value);

		this.isGetLink = value;
	}


	//Display keyboard
	private void showKeyboardDelayed(final View view) {
		logDebug("showKeyboardDelayed");

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
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
		if (request.getType() == MegaRequest.TYPE_RENAME){

			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_renamed), -1);
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_renamed), -1);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					if(positionToRemove!=-1){
						logDebug("Position to remove: " + positionToRemove);
						logDebug("Position in: " + positionG);
						imageHandles.remove(positionToRemove);
						if(imageHandles.size()==0){
							finish();
						}
						else{
							adapterMega.refreshImageHandles(imageHandles);
							viewPager.setAdapter(adapterMega);
							if(positionG>imageHandles.size()-1){
								logDebug("Last item deleted, go to new last position");
								positionG=imageHandles.size()-1;
							}
							viewPager.setCurrentItem(positionG);
							positionToRemove=-1;
							supportInvalidateOptionsMenu();

						}
					}
				}
				else{
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
				}
				moveToRubbish = false;
				logDebug("Move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved), -1);
					finish();
				}
				else{
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
				}
				logDebug("Move nodes request finished");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){


			if (e.getErrorCode() == MegaError.API_OK){
				if (statusDialog.isShowing()){
					try {
						statusDialog.dismiss();
					}
					catch (Exception ex) {}
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_removed), -1);
				}
				finish();
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_removed), -1);
			}
			logDebug("Remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied), -1);
			}
			else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
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
		logDebug("onActivityResult");

		if (intent == null) {
			return;
		}

		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			logDebug("Local folder selected");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if(adapterType == FILE_LINK_ADAPTER){
				if (nC == null) {
					nC = new NodeController(this);
				}
				nC.downloadTo(currentDocument, parentPath, url);
			}
			else{
				String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
				long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
				long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
				boolean highPriority = intent.getBooleanExtra(HIGH_PRIORITY_TRANSFER, false);

				if(nC==null){
					nC = new NodeController(this, isFolderLink);
				}
				nC.checkSizeBeforeDownload(parentPath,url, size, hashes, highPriority);
			}
        } else if (requestCode == REQUEST_CODE_TREE) {
            onRequestSDCardWritePermission(intent, resultCode, false, nC);
        }
		else if (requestCode == WRITE_SD_CARD_REQUEST_CODE && resultCode == RESULT_OK) {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (!hasStoragePermission) {
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							REQUEST_WRITE_STORAGE);
				}
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
				String path = getCacheFolder(context, TEMPORAL_FOLDER).getAbsolutePath();
				service.putExtra(DownloadService.EXTRA_PATH, path);
				service.putExtra("fromMV", true);
				service.putExtra(HIGH_PRIORITY_TRANSFER, highPriority);
				startService(service);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {

			if(!isOnline(this)){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return;
			}

			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);
			final int totalMoves = moveHandles.length;

			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			moveToRubbish = false;

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
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
			if(!isOnline(this)){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return;
			}

			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;

			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_copying));
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
					catch (Exception ex) {}
				}
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK){
			long[] chatHandles = intent.getLongArrayExtra(SELECTED_CHATS);
			long[] contactHandles = intent.getLongArrayExtra(SELECTED_USERS);
			long[] nodeHandles = intent.getLongArrayExtra(NODE_HANDLES);

			if ((chatHandles != null && chatHandles.length > 0) || (contactHandles != null && contactHandles.length > 0)) {
				if (contactHandles != null && contactHandles.length > 0) {
					ArrayList<MegaChatRoom> chats = new ArrayList<>();
					ArrayList<MegaUser> users = new ArrayList<>();

					for (int i=0; i<contactHandles.length; i++) {
						MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]));
						if (user != null) {
							users.add(user);
						}
					}

					if (chatHandles != null) {
						for (int i = 0; i < chatHandles.length; i++) {
							MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandles[i]);
							if (chatRoom != null) {
								chats.add(chatRoom);
							}
						}
					}

					if(nodeHandles!=null){
						CreateChatListener listener = new CreateChatListener(chats, users, nodeHandles[0], this, CreateChatListener.SEND_FILE);
						for (MegaUser user : users) {
							MegaChatPeerList peers = MegaChatPeerList.createInstance();
							peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
							megaChatApi.createChat(false, peers, listener);
						}
					}
					else{
						logWarning("Error on sending to chat");
					}
				}
				else {
					countChat = chatHandles.length;
					for (int i = 0; i < chatHandles.length; i++) {
						megaChatApi.attachNode(chatHandles[i], nodeHandles[0], this);
					}
				}
			}

		}
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {}

	public void showSnackbar(int type, String s, long idChat){
		showSnackbar(type, fragmentContainer, s, idChat);
	}

	public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes, final boolean highPriority){
        logDebug("askSizeConfirmationBeforeDownload");

		final String parentPathC = parentPath;
		final String urlC = url;
		final long [] hashesC = hashes;
		final long sizeC=size;

		androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));

		builder.setMessage(getString(R.string.alert_larger_file, getSizeString(sizeC)));
		builder.setPositiveButton(getString(R.string.general_save_to_device),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskSizeDownload("false");
						}
						if(nC==null){
							nC = new NodeController(fullScreenImageViewer, isFolderLink);
						}
						nC.checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
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

	public void askConfirmationNoAppInstaledBeforeDownload (String parentPath, String url, long size, long [] hashes, String nodeToDownload, final boolean highPriority){
        logDebug("askConfirmationNoAppInstaledBeforeDownload");

		final String parentPathC = parentPath;
		final String urlC = url;
		final long [] hashesC = hashes;
		final long sizeC=size;

		androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));
		builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
		builder.setPositiveButton(getString(R.string.general_save_to_device),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskNoAppDownload("false");
						}
						if(nC==null){
							nC = new NodeController(fullScreenImageViewer, isFolderLink);
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

	public void touchImage() {
		logDebug("touchImage");
		if(aB.isShowing()){
			hideActionBar();
		}else{
			showActionBar();
		}
	}

	protected void hideActionBar(){
		logDebug("hideActionBar");

		if (aB != null && aB.isShowing()) {
			if(tB != null) {
				tB.animate().translationY(-220).setDuration(400L)
						.withEndAction(new Runnable() {
							@Override
							public void run() {
								aB.hide();
							}
						}).start();
				bottomLayout.animate().translationY(220).setDuration(400L).start();
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
				tB.animate().translationY(0).setDuration(400L).start();
				bottomLayout.animate().translationY(0).setDuration(400L).start();
				getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
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
	public void onBackPressed() {
		logDebug("onBackPressed");
		setImageDragVisibility(View.VISIBLE);
		super.onBackPressed();
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


			Iterator<MegaNode> it = nodes.iterator();
			while (it.hasNext()){
				MegaNode n = it.next();
				if (n != null && positionG < imageHandles.size() && n.getHandle() == imageHandles.get(positionG)){
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


	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		logDebug("onRequestFinish");
		if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){

			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("File sent correctly");
				successSent++;
			}else{
				logWarning("File NOT sent: " + e.getErrorCode() + "___" + e.getErrorString());
				errorSent++;
			}

			if(countChat==errorSent+successSent){
				if(successSent==countChat){
					if(countChat==1){
						showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.sent_as_message), request.getChatHandle());
					}
					else{
						showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.sent_as_message), -1);
					}
				}
				else if(errorSent==countChat){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.error_attaching_node_from_cloud), -1);
				}
				else{
					showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.error_sent_as_message), -1);
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {}

	@Override
	public void onViewPositionChanged(float fractionScreen) {
		ivShadow.setAlpha(1 - fractionScreen);
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(getContainer());
		View view = LayoutInflater.from(this).inflate(layoutResID, null);
		draggableView.addView(view);
	}

	private View getContainer() {
		RelativeLayout container = new RelativeLayout(this);
		draggableView = new DraggableView(this);
		if (getIntent() != null) {
			screenPosition = getIntent().getIntArrayExtra("screenPosition");
			draggableView.setScreenPosition(screenPosition);
		}
		draggableView.setDraggableListener(this);
		ivShadow = new ImageView(this);
		ivShadow.setBackgroundColor(ContextCompat.getColor(this, R.color.black_p50));
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		container.addView(ivShadow, params);
		container.addView(draggableView);
		return container;
	}

	@Override
	public void onDragActivated(boolean activated) {
		logDebug("activated: " + activated);

		if (activated) {
			updateCurrentImage();
			if (aB != null && aB.isShowing()) {
				if(tB != null) {
					tB.animate().translationY(-220).setDuration(0)
							.withEndAction(new Runnable() {
								@Override
								public void run() {
									aB.hide();
								}
							}).start();
					bottomLayout.animate().translationY(220).setDuration(0).start();
				} else {
					aB.hide();
				}
			}
			fragmentContainer.setBackgroundColor(TRANSPARENT);
			relativeImageViewerLayout.setBackgroundColor(TRANSPARENT);
			appBarLayout.setBackgroundColor(TRANSPARENT);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				fragmentContainer.setElevation(0);
				relativeImageViewerLayout.setElevation(0);
				appBarLayout.setElevation(0);

			}

			if (adapterType == OFFLINE_ADAPTER){
				draggableView.setCurrentView(adapterOffline.getVisibleImage(positionG));
			}
			else if (adapterType == ZIP_ADAPTER) {
				draggableView.setCurrentView(adapterOffline.getVisibleImage(positionG));
			}
			else {
				draggableView.setCurrentView(adapterMega.getVisibleImage(positionG));
			}
		}
		else {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
//					showActionBar();
					fragmentContainer.setBackgroundColor(BLACK);
					relativeImageViewerLayout.setBackgroundColor(BLACK);
					appBarLayout.setBackgroundColor(BLACK);
				}
			}, 300);
		}
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
}
