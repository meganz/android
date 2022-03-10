package mega.privacy.android.app.main.managerSections;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.PositionDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.fragments.homepage.EventObserver;
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel;
import mega.privacy.android.app.gallery.ui.MediaDiscoveryFragment;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.main.DrawerItem;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.PdfViewerActivity;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.sync.fileBackups.FileBackupManager;
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_MEDIA_DISCOVERY;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_ACTION_TYPE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_DIALOG_WARN;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_ITEM;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_NODE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NODE_TYPE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE;
import static mega.privacy.android.app.utils.MegaNodeUtil.allHaveOwnerAccessAndNotTakenDown;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode;
import static mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import com.jeremyliao.liveeventbus.LiveEventBus;

@AndroidEntryPoint
public class FileBrowserFragment extends RotatableFragment{

	@Inject
	SortOrderManagement sortOrderManagement;

	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	FastScroller fastScroller;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

    MegaNodeAdapter adapter;

	public int pendingTransfers = 0;
	public int totalTransfers = 0;
	public long totalSizePendingTransfer=0;
	public long totalSizeTransfered=0;

	Stack<Integer> lastPositionStack;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	float density;
	DisplayMetrics outMetrics;
	Display display;

	DatabaseHandler dbH;

	ArrayList<MegaNode> nodes;
	public ActionMode actionMode;

	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;

	String downloadLocationDefaultPath;

    private RelativeLayout transferOverQuotaBanner;
    private TextView transferOverQuotaBannerText;
    private long mediaHandle;

    // Backup warning dialog
	private AlertDialog backupWarningDialog;
	private ArrayList<Long> backupHandleList;
	private int backupDialogType = -1;
	private Long backupNodeHandle;
	private int backupNodeType;
	private int backupActionType;

	private FileBackupManager fileBackupManager;

	@Override
	protected MegaNodeAdapter getAdapter() {
		return adapter;
	}

	@Override
	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	/**
	 * Shows the Sort by panel.
	 *
	 * @param unit Unit event.
	 * @return Null.
	 */
	private Unit showSortByPanel(Unit unit) {
		((ManagerActivity) context).showNewSortByPanel(ORDER_CLOUD);
		return null;
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			logDebug("onActionItemClicked");
			List<MegaNode> documents = adapter.getSelectedNodes();
			ArrayList<Long> handleList = new ArrayList<>();
			int nodeType = BACKUP_NONE;

			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					((ManagerActivity) context).saveNodesToDevice(
							documents, false, false, false, false);

					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_rename:{
					if (documents.size()==1){
						((ManagerActivity) context).showRenameDialog(documents.get(0));
					}
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_copy:{
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_move:{
					NodeController nC = new NodeController(context);
					for (int i = 0; i < documents.size(); i++) {
						handleList.add(documents.get(i).getHandle());
					}

					if (!fileBackupManager.moveBackup(nC, handleList)) {
						nC.chooseLocationToMoveNodes(handleList);
					}

					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_share_folder:{
					//Check that all the selected options are folders
					for (int i=0;i<documents.size();i++){
						if(documents.get(i).isFolder()){
							handleList.add(documents.get(i).getHandle());
						}
					}

					NodeController nC = new NodeController(context);
					if (!fileBackupManager.shareBackupFolderInMenu(nC, handleList)){
						nC.selectContactToShareFolders(handleList);
					}

					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_share_out: {
					MegaNodeUtil.shareNodes(context, documents);
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_share_link:
				case R.id.cab_menu_edit_link:
					logDebug("Public link option");
					if (documents.get(0) == null) {
						logWarning("The selected node is NULL");
						break;
					}

					((ManagerActivity) context).showGetLinkActivity(documents);
					clearSelections();
					hideMultipleSelect();
					break;

				case R.id.cab_menu_remove_link:{

					logDebug("Remove public link option");
					if(documents.get(0)==null){
						logWarning("The selected node is NULL");
						break;
					}
					((ManagerActivity) context).showConfirmationRemovePublicLink(documents.get(0));
					clearSelections();
					hideMultipleSelect();

					break;
				}
				case R.id.cab_menu_send_to_chat:{
					logDebug("Send files to chat");
					((ManagerActivity) context).attachNodesToChats(adapter.getArrayListSelectedNodes());
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_trash:{
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					((ManagerActivity) context).askConfirmationMoveToRubbish(handleList);

					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_clear_selection:{
					clearSelections();
					hideMultipleSelect();
					break;
				}

				case R.id.cab_menu_remove_share:
					((ManagerActivity) context).showConfirmationRemoveAllSharingContacts(documents);
					break;
			}
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			logDebug("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.cloud_storage_action, menu);
			((ManagerActivity)context).hideFabButton();
			((ManagerActivity) context).showHideBottomNavigationView(true);
			checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivity)context).showFabButton();
			((ManagerActivity) context).showHideBottomNavigationView(false);
			checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			logDebug("onPrepareActionMode");
			List<MegaNode> selected = adapter.getSelectedNodes();
			if (selected.isEmpty()) {
				return false;
			}

			menu.findItem(R.id.cab_menu_share_link)
					.setTitle(StringResourcesUtils.getQuantityString(R.plurals.get_links, selected.size()));

			CloudStorageOptionControlUtil.Control control =
					new CloudStorageOptionControlUtil.Control();

			if (selected.size() == 1) {
				if (!selected.get(0).isTakenDown()) {
					if (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode()
							== MegaError.API_OK) {
						if (selected.get(0).isExported()) {
							control.manageLink().setVisible(true)
									.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

							control.removeLink().setVisible(true);
						} else {
							control.getLink().setVisible(true)
									.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
						}
					}
				}

				if (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode()
						== MegaError.API_OK) {
					control.rename().setVisible(true);
				}
			} else if (allHaveOwnerAccessAndNotTakenDown(selected)) {
				control.getLink().setVisible(true).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			boolean showSendToChat = true;
			boolean showShareFolder = true;
			boolean showTrash = true;
			boolean showRemoveShare = true;
			boolean showShareOut = true;
			boolean showCopy = true;
			boolean showDownload = true;
			int mediaCounter = 0;

			for (MegaNode node : selected) {
				if (node.isTakenDown()) {
					showShareOut = false;
					showCopy = false;
					showDownload = false;
				}

				if (!node.isFile() || node.isTakenDown()) {
					showSendToChat = false;
				} else if (node.isFile()){
					MimeTypeList nodeMime = MimeTypeList.typeForName(node.getName());
					if (nodeMime.isImage() || nodeMime.isVideo()) {
						mediaCounter++;
					}
				}
				if (node.isTakenDown() || !node.isFolder() || (MegaNodeUtil.isOutShare(node) && selected.size() > 1)) {
					showShareFolder = false;
				}
				if (megaApi.checkMove(node, megaApi.getRubbishNode()).getErrorCode()
						!= MegaError.API_OK) {
					showTrash = false;
				}

				if (node.isTakenDown() || !node.isFolder() ||  !MegaNodeUtil.isOutShare(node)) {
					showRemoveShare = false;
				}
			}

			if (showSendToChat) {
				control.sendToChat().setVisible(true)
						.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if (showShareFolder) {
				control.shareFolder().setVisible(true)
						.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if (showRemoveShare) {
				control.removeShare().setVisible(true);
			}

			control.trash().setVisible(showTrash);

			if (showShareOut) {
				control.shareOut().setVisible(true);
				if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
					control.shareOut().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
			}

			control.move().setVisible(true);
			if (selected.size() > 1
					&& control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
				control.move().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if (showCopy) {
				control.copy().setVisible(true);
				if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
					control.copy().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
			}

			if (!showDownload) {
				control.saveToDevice().setVisible(false);
			}

			control.selectAll()
					.setVisible(selected.size()
							< adapter.getItemCount() - adapter.getPlaceholderCount());

			CloudStorageOptionControlUtil.applyControl(menu, control);

			return true;
		}
	}

	public static FileBrowserFragment newInstance() {
		logDebug("newInstance");
		return new FileBrowserFragment();
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);
		downloadLocationDefaultPath = getDownloadLocation();
		lastPositionStack = new Stack<>();

		if (megaChatApi == null) {
			megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
		}

		super.onCreate(savedInstanceState);
		logDebug("After onCreate called super");
	}

	public void checkScroll() {
		if (recyclerView == null) return;

		boolean visible = (adapter != null && adapter.isMultipleSelect())
				|| MegaApplication.getTransfersManagement().isTransferOverQuotaBannerShown()
				|| (recyclerView.canScrollVertically(-1) && recyclerView.getVisibility() == View.VISIBLE);

		((ManagerActivity) context).changeAppBarElevation(visible);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");
		if (!isAdded()) {
			return null;
		}

		SortByHeaderViewModel sortByHeaderViewModel = new ViewModelProvider(this)
				.get(SortByHeaderViewModel.class);

		sortByHeaderViewModel.getShowDialogEvent().observe(getViewLifecycleOwner(),
				new EventObserver<>(this::showSortByPanel));

        LiveEventBus.get(EVENT_SHOW_MEDIA_DISCOVERY, Unit.class).observe(this, this::showMediaDiscovery);

		logDebug("Fragment ADDED");

		if (megaApi == null) {
			megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
		}

		if (aB == null) {
			aB = ((AppCompatActivity) context).getSupportActionBar();
		}

		if (megaApi.getRootNode() == null) {
			return null;
		}

		display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = getResources().getDisplayMetrics().density;

		getNodes();
		((ManagerActivity) context).setToolbarTitle();
		((ManagerActivity) context).supportInvalidateOptionsMenu();

		View v;

		if (((ManagerActivity) context).isList) {
			logDebug("isList");
			v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

			recyclerView = v.findViewById(R.id.file_list_view_browser);
			fastScroller = v.findViewById(R.id.fastscroll);
			recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);

			mLayoutManager = new LinearLayoutManager(context);
			mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setHasFixedSize(true);
			recyclerView.setItemAnimator(noChangeRecyclerViewItemAnimator());
			recyclerView.addItemDecoration(new PositionDividerItemDecoration(requireContext(), getOutMetrics()));
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = v.findViewById(R.id.file_list_empty_image);
			emptyTextView = v.findViewById(R.id.file_list_empty_text);
			emptyTextViewFirst = v.findViewById(R.id.file_list_empty_text_first);

			if (adapter == null){
				adapter = new MegaNodeAdapter(context, this, nodes,
						((ManagerActivity) context).getParentHandleBrowser(), recyclerView,
						FILE_BROWSER_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST, sortByHeaderViewModel);
			}
			else{
				adapter.setParentHandle(((ManagerActivity)context).getParentHandleBrowser());
				adapter.setListFragment(recyclerView);
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
            }

            adapter.setMultipleSelect(false);

            recyclerView.setAdapter(adapter);
            fastScroller.setRecyclerView(recyclerView);

            setNodes(nodes);

            if (adapter.getItemCount() == 0) {
				logDebug("itemCount is 0");
                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
				logDebug("itemCount is " + adapter.getItemCount());
                recyclerView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }
        } else {
			logDebug("Grid View");
            v = inflater.inflate(R.layout.fragment_filebrowsergrid,container,false);
            recyclerView = v.findViewById(R.id.file_grid_view_browser);
            fastScroller = v.findViewById(R.id.fastscroll);

            recyclerView.setPadding(0,0,0,scaleHeightPx(80,outMetrics));

            recyclerView.setClipToPadding(false);
            recyclerView.setHasFixedSize(true);

            gridLayoutManager = (CustomizedGridLayoutManager)recyclerView.getLayoutManager();
            recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

            emptyImageView = v.findViewById(R.id.file_grid_empty_image);
            emptyTextView = v.findViewById(R.id.file_grid_empty_text);
            emptyTextViewFirst = v.findViewById(R.id.file_grid_empty_text_first);

            if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes,
						((ManagerActivity) context).getParentHandleBrowser(), recyclerView,
						FILE_BROWSER_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID, sortByHeaderViewModel);
            } else {
                adapter.setParentHandle(((ManagerActivity)context).getParentHandleBrowser());
                adapter.setListFragment(recyclerView);
                adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
            }

			gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup(gridLayoutManager.getSpanCount()));

            adapter.setMultipleSelect(false);

            recyclerView.setAdapter(adapter);
            fastScroller.setRecyclerView(recyclerView);
            setNodes(nodes);

            if (adapter.getItemCount() == 0) {
                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }
        }

		transferOverQuotaBanner = v.findViewById(R.id.transfer_over_quota_banner);
		transferOverQuotaBannerText = v.findViewById(R.id.banner_content_text);
		v.findViewById(R.id.banner_dismiss_button).setOnClickListener(v1 -> hideTransferOverQuotaBanner());
		v.findViewById(R.id.banner_upgrade_button).setOnClickListener(v12 -> {
			hideTransferOverQuotaBanner();
			((ManagerActivity) context).navigateToUpgradeAccount();
		});

		setTransferOverQuotaBannerVisibility();

		return v;
    }

	@Override
	public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		observeDragSupportEvents(getViewLifecycleOwner(), recyclerView, VIEWER_FROM_FILE_BROWSER);
	}

    @Override
    public void onAttach(@NonNull Context context) {
		logDebug("onAttach");

		super.onAttach(context);
		this.context = context;
		aB = ((AppCompatActivity) context).getSupportActionBar();
		fileBackupManager = new FileBackupManager(
				requireActivity(),
				(actionType, operationType, result, handle) -> logDebug("Nothing to do for actionType = " + actionType)
		);
	}

    @Override
	public void onDestroy() {
		if (adapter != null) {
			adapter.clearTakenDownDialog();
		}

		super.onDestroy();
	}

	private void getNodes() {
		long parentHandleBrowser = ((ManagerActivity) context).getParentHandleBrowser();
		if (parentHandleBrowser == -1 || parentHandleBrowser == megaApi.getRootNode().getHandle()) {
			logWarning("After consulting... the parent keeps -1 or ROOTNODE: " + parentHandleBrowser);

			nodes = megaApi.getChildren(megaApi.getRootNode(), sortOrderManagement.getOrderCloud());
			mediaHandle = megaApi.getRootNode().getHandle();
		} else {
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
			nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
			mediaHandle = parentHandleBrowser;
		}
	}

	public void refreshNodes(){
		if (adapter != null) {
			getNodes();
			adapter.setNodes(nodes);
		}
	}

	public void openFile(MegaNode node, int position) {
		if (MimeTypeList.typeForName(node.getName()).isImage()) {
			Intent intent = ImageViewerActivity.getIntentForParentNode(
					requireContext(),
					megaApi.getParentNode(node).getHandle(),
					sortOrderManagement.getOrderCloud(),
					node.getHandle()
			);
			putThumbnailLocation(intent, recyclerView, position, VIEWER_FROM_FILE_BROWSER, adapter);
			startActivity(intent);
			((ManagerActivity) context).overridePendingTransition(0, 0);
		} else if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isAudio()) {

			String mimeType = MimeTypeList.typeForName(node.getName()).getType();

			Intent mediaIntent;
			boolean internalIntent;
			boolean opusFile = false;
			if (MimeTypeList.typeForName(node.getName()).isVideoNotSupported() || MimeTypeList.typeForName(node.getName()).isAudioNotSupported()) {
				mediaIntent = new Intent(Intent.ACTION_VIEW);
				internalIntent = false;
				String[] s = node.getName().split("\\.");
				if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) {
					opusFile = true;
				}
			} else {
				mediaIntent = getMediaIntent(context, node.getName());
				internalIntent = true;
			}
			mediaIntent.putExtra("position", position);
			mediaIntent.putExtra("placeholder", adapter.getPlaceholderCount());

			MegaNode megaNode = megaApi.getParentNode(node);
			if(megaNode != null) {
				if (megaNode.getType() == MegaNode.TYPE_ROOT) {
					mediaIntent.putExtra("parentNodeHandle", -1L);
				} else {
					mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(node).getHandle());
				}
			}

			mediaIntent.putExtra("orderGetChildren", sortOrderManagement.getOrderCloud());
			mediaIntent.putExtra("adapterType", FILE_BROWSER_ADAPTER);
			putThumbnailLocation(mediaIntent, recyclerView, position, VIEWER_FROM_FILE_BROWSER, adapter);

			mediaIntent.putExtra("FILENAME", node.getName());

			String localPath = getLocalFile(node);

			if (localPath != null) {
				File mediaFile = new File(localPath);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
					logDebug("itemClick:FileProviderOption");
					Uri mediaFileUri = FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile);
					if (mediaFileUri == null) {
						logDebug("itemClick:ERROR:NULLmediaFileUri");
						((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
					} else {
						mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
					}
				} else {
					Uri mediaFileUri = Uri.fromFile(mediaFile);
					if (mediaFileUri == null) {
						logError("itemClick:ERROR:NULLmediaFileUri");
						((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
					} else {
						mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
					}
				}
				mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			} else {
				logDebug("itemClick:localPathNULL");

				if (megaApi.httpServerIsRunning() == 0) {
					megaApi.httpServerStart();
					mediaIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
				} else {
					logWarning("itemClick:ERROR:httpServerAlreadyRunning");
				}

				ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
				ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				activityManager.getMemoryInfo(mi);

				if (mi.totalMem > BUFFER_COMP) {
					logDebug("itemClick:total mem: " + mi.totalMem + " allocate 32 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
				} else {
					logDebug("itemClick:total mem: " + mi.totalMem + " allocate 16 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
				}

				String url = megaApi.httpServerGetLocalLink(node);
				if (url != null) {
					Uri parsedUri = Uri.parse(url);
					if (parsedUri != null) {
						mediaIntent.setDataAndType(parsedUri, mimeType);
					} else {
						logError("itemClick:ERROR:httpServerGetLocalLink");
						((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
					}
				} else {
					logError("itemClick:ERROR:httpServerGetLocalLink");
					((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
				}
			}
			mediaIntent.putExtra("HANDLE", node.getHandle());
			if (opusFile) {
				mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
			}
			if (internalIntent) {
				context.startActivity(mediaIntent);
			} else {
				logDebug("itemClick:externalIntent");
				if (isIntentAvailable(context, mediaIntent)) {
					context.startActivity(mediaIntent);
				} else {
					logWarning("itemClick:noAvailableIntent");
					((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);

					((ManagerActivity) context).saveNodesToDevice(
							Collections.singletonList(node),
							true, false, false, false);
				}
			}
			((ManagerActivity) context).overridePendingTransition(0, 0);
		} else if (MimeTypeList.typeForName(node.getName()).isURL()) {
			manageURLNode(context, megaApi, node);
		} else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
			logDebug("itemClick:isFile:isPdf");

			String mimeType = MimeTypeList.typeForName(node.getName()).getType();

			Intent pdfIntent = new Intent(context, PdfViewerActivity.class);

			pdfIntent.putExtra("inside", true);
			pdfIntent.putExtra("adapterType", FILE_BROWSER_ADAPTER);

			String localPath = getLocalFile(node);

			if (localPath != null) {
				File mediaFile = new File(localPath);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
					pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(node.getName()).getType());
				} else {
					pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(node.getName()).getType());
				}
				pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			} else {
				if (megaApi.httpServerIsRunning() == 0) {
					megaApi.httpServerStart();
					pdfIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
				}

				ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
				ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				activityManager.getMemoryInfo(mi);

				if (mi.totalMem > BUFFER_COMP) {
					logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
				} else {
					logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
				}

				String url = megaApi.httpServerGetLocalLink(node);
				if (url != null) {
					Uri parsedUri = Uri.parse(url);
					if (parsedUri != null) {
						pdfIntent.setDataAndType(parsedUri, mimeType);
					} else {
						logError("itemClick:ERROR:httpServerGetLocalLink");
						((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
					}
				} else {
					logError("itemClick:ERROR:httpServerGetLocalLink");
					((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
				}
			}
			pdfIntent.putExtra("HANDLE", node.getHandle());
			putThumbnailLocation(pdfIntent, recyclerView, position, VIEWER_FROM_FILE_BROWSER, adapter);
			if (isIntentAvailable(context, pdfIntent)) {
				context.startActivity(pdfIntent);
			} else {
				Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

				((ManagerActivity) context).saveNodesToDevice(
						Collections.singletonList(node),
						true, false, false, false);
			}
			((ManagerActivity) context).overridePendingTransition(0, 0);
		} else if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
			manageTextFileIntent(context, node, FILE_BROWSER_ADAPTER);
        } else {
            logDebug("itemClick:isFile:otherOption");
            onNodeTapped(context, node, ((ManagerActivity) context)::saveNodeByTap, (ManagerActivity) context, (ManagerActivity) context);
        }
    }

    public void itemClick(int position) {
		logDebug("item click position: " + position);
        if (adapter.isMultipleSelect()) {
			logDebug("itemClick:multiselectON");
            adapter.toggleSelection(position);

            List<MegaNode> selectedNodes = adapter.getSelectedNodes();
            if (selectedNodes.size() > 0) {
                updateActionModeTitle();
            }
		}
		else{
			logDebug("itemClick:multiselectOFF");
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition;
				if(((ManagerActivity)context).isList){
					lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
					logDebug("lastFirstVisiblePosition: "+lastFirstVisiblePosition);
				}
				else{
					lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
					if(lastFirstVisiblePosition==-1){
						logDebug("Completely -1 then find just visible position");
						lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
					}
				}

				logDebug("Push to stack "+lastFirstVisiblePosition+" position");
				lastPositionStack.push(lastFirstVisiblePosition);
				setFolderInfoNavigation(n);
			}
			else{
				//Is file
				openFile(nodes.get(position), position);
			}
		}
	}

	@Override
	public void multipleItemClick(int position) {
		adapter.toggleSelection(position);
	}

	@Override
	public void reselectUnHandledSingleItem(int position) {
		adapter.filClicked(position);
	}

	public void setFolderInfoNavigation(MegaNode n){
	    mediaHandle = n.getHandle();
        ((ManagerActivity)context).setParentHandleBrowser(n.getHandle());
		((ManagerActivity)context).supportInvalidateOptionsMenu();
        ((ManagerActivity)context).setToolbarTitle();

        adapter.setParentHandle(((ManagerActivity)context).getParentHandleBrowser());
        nodes = megaApi.getChildren(n, sortOrderManagement.getOrderCloud());
        adapter.setNodes(nodes);
        recyclerView.scrollToPosition(0);

        visibilityFastScroller();

        //If folder has no files
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyImageView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.VISIBLE);

            if (megaApi.getRootNode() != null && megaApi.getRootNode().getHandle() == n.getHandle()) {

                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.empty_cloud_drive_landscape);
                } else {
                    emptyImageView.setImageResource(R.drawable.empty_cloud_drive_portrait);
                }
                String textToShow = context.getString(R.string.context_empty_cloud_drive).toUpperCase();
                try {
                    textToShow = textToShow.replace("[A]","<font color=\'"
							+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
							+ "\'>");
                    textToShow = textToShow.replace("[/A]","</font>");
                    textToShow = textToShow.replace("[B]","<font color=\'"
							+ ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
							+ "\'>");
                    textToShow = textToShow.replace("[/B]","</font>");
                } catch (Exception e) {
					logError(e.getMessage());
                }
				Spanned result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
                emptyTextViewFirst.setText(result);

            } else {
                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.empty_folder_landscape);
                } else {
                    emptyImageView.setImageResource(R.drawable.empty_folder_portrait);
                }
                String textToShow = context.getString(R.string.file_browser_empty_folder_new);
                try {
                    textToShow = textToShow.replace("[A]","<font color=\'"
							+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
							+ "\'>");
                    textToShow = textToShow.replace("[/A]","</font>");
                    textToShow = textToShow.replace("[B]","<font color=\'"
							+ ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
							+ "\'>");
                    textToShow = textToShow.replace("[/B]","</font>");
                } catch (Exception e) {
					logError(e.getMessage());
                }
                Spanned result;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextViewFirst.setText(result);
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
        }
        checkScroll();
    }

    public boolean showSelectMenuItem() {
		logDebug("showSelectMenuItem");
        if (adapter != null) {
            return adapter.isMultipleSelect();
        }

        return false;
    }

    public void selectAll() {
		logDebug("selectAll");
        if (adapter != null) {
            if (adapter.isMultipleSelect()) {
                adapter.selectAll();
            } else {
                adapter.setMultipleSelect(true);
                adapter.selectAll();

                actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
            }

			new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    /*
     * Clear all selected items
     */
    private void clearSelections() {
        if (adapter.isMultipleSelect()) {
            adapter.clearSelections();
        }
    }

    @Override
    protected void updateActionModeTitle() {
		logDebug("updateActionModeTitle");
        if (actionMode == null || getActivity() == null) {
			logWarning("RETURN: null values");
            return;
        }

        List<MegaNode> documents = adapter.getSelectedNodes();
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
        int sum = files + folders;

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
            e.printStackTrace();
			logError("Invalidate error", e);
        }

    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
		logDebug("hideMultipleSelect");
        adapter.setMultipleSelect(false);

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public int onBackPressed() {
		logDebug("onBackPressed");

        if (adapter != null) {
			logDebug("Parent Handle is: " + ((ManagerActivity)context).getParentHandleBrowser());

			if (((ManagerActivity) context).comesFromNotifications && ((ManagerActivity) context).comesFromNotificationHandle == (((ManagerActivity)context).getParentHandleBrowser())) {
				((ManagerActivity) context).comesFromNotifications = false;
				((ManagerActivity) context).comesFromNotificationHandle = -1;
				((ManagerActivity) context).selectDrawerItem(DrawerItem.NOTIFICATIONS);
				((ManagerActivity)context).setParentHandleBrowser(((ManagerActivity)context).comesFromNotificationHandleSaved);
				((ManagerActivity)context).comesFromNotificationHandleSaved = -1;
				((ManagerActivity) context).refreshCloudDrive();

				return 2;
			}
			else {
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(((ManagerActivity)context).getParentHandleBrowser()));
				if (parentNode != null) {
				    mediaHandle = parentNode.getHandle();
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);

					((ManagerActivity)context).setParentHandleBrowser(parentNode.getHandle());
					((ManagerActivity)context).supportInvalidateOptionsMenu();

					((ManagerActivity)context).setToolbarTitle();

					nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
					adapter.setNodes(nodes);

					visibilityFastScroller();

					int lastVisiblePosition = 0;
					if (!lastPositionStack.empty()) {
						lastVisiblePosition = lastPositionStack.pop();
						logDebug("Pop of the stack " + lastVisiblePosition + " position");
					}
					logDebug("Scroll to " + lastVisiblePosition + " position");

					if (lastVisiblePosition >= 0) {

						if (((ManagerActivity)context).isList) {
							mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition,0);
						} else {
							gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition,0);
						}
					}
					logDebug("return 2");
					return 2;
				} else {
					logWarning("ParentNode is NULL");
					return 0;
				}
			}
        }

        return 0;
    }

	public void scrollToFirstPosition () {
		if (((ManagerActivity)context).isList) {
			mLayoutManager.scrollToPositionWithOffset(0,0);
		}
		else {
			gridLayoutManager.scrollToPositionWithOffset(0,0);
		}
	}

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setNodes(ArrayList<MegaNode> nodes) {
		logDebug("Nodes size: " + nodes.size());

        visibilityFastScroller();
        this.nodes = nodes;

		if (adapter != null) {
			adapter.setNodes(nodes);

			if (adapter.getItemCount() == 0) {
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (megaApi.getRootNode() != null && megaApi.getRootNode().getHandle() == ((ManagerActivity)context).getParentHandleBrowser() || ((ManagerActivity)context).getParentHandleBrowser() == -1) {

					if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						emptyImageView.setImageResource(R.drawable.empty_cloud_drive_landscape);
					} else {
						emptyImageView.setImageResource(R.drawable.empty_cloud_drive_portrait);
					}
					String textToShow = context.getString(R.string.context_empty_cloud_drive).toUpperCase();
					try {
						textToShow = textToShow.replace("[A]","<font color=\'"
								+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
								+ "\'>");
						textToShow = textToShow.replace("[/A]","</font>");
						textToShow = textToShow.replace("[B]","<font color=\'"
								+ ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
								+ "\'>");
						textToShow = textToShow.replace("[/B]","</font>");
					} catch (Exception e) {
						logError(e.getMessage());
					}
					Spanned result;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);

				} else {
					if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						emptyImageView.setImageResource(R.drawable.empty_folder_landscape);
					} else {
						emptyImageView.setImageResource(R.drawable.empty_folder_portrait);
					}
					String textToShow = context.getString(R.string.file_browser_empty_folder_new);
					try {
						textToShow = textToShow.replace("[A]","<font color=\'"
								+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
								+ "\'>");
						textToShow = textToShow.replace("[/A]","</font>");
						textToShow = textToShow.replace("[B]","<font color=\'"
								+ ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
								+ "\'>");
						textToShow = textToShow.replace("[/B]","</font>");
					} catch (Exception e) {
						logError(e.getMessage());
					}
					Spanned result;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				}
			} else {
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		} else {
			logWarning("Adapter is NULL");
		}
    }

    public boolean isMultipleselect() {
        if (adapter != null) {
            return adapter.isMultipleSelect();
        }
        return false;
    }

    public int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
    }

    public void visibilityFastScroller() {
        if (adapter == null) {
            fastScroller.setVisibility(View.GONE);
        } else {
            if (adapter.getItemCount() < MIN_ITEMS_SCROLLBAR) {
                fastScroller.setVisibility(View.GONE);
            } else {
                fastScroller.setVisibility(View.VISIBLE);
            }
        }
    }

	//refresh list when item updated
	@SuppressLint("NotifyDataSetChanged")
	public void refresh(long handle) {
		if (handle == -1) {
			return;
		}
		updateNode(handle);
		adapter.notifyDataSetChanged();
	}

	private void updateNode(long handle) {
		for (int i = 0; i < nodes.size(); i++) {
			MegaNode node = nodes.get(i);
			//in grid view, we have to ignore the placholder.
			if(node == null) {
				continue;
			}
			if (node.getHandle() == handle) {
				MegaNode updated = megaApi.getNodeByHandle(handle);
				nodes.set(i, updated);
				break;
			}
		}
	}

	/**
	 * Sets the "transfer over quota" banner visibility.
	 */
	public void setTransferOverQuotaBannerVisibility() {
    	if (MegaApplication.getTransfersManagement().isTransferOverQuotaBannerShown()) {
    		transferOverQuotaBanner.setVisibility(View.VISIBLE);
    		transferOverQuotaBannerText.setText(context.getString(R.string.current_text_depleted_transfer_overquota, getHumanizedTime(megaApi.getBandwidthOverquotaDelay())));
			createAndShowCountDownTimer(R.string.current_text_depleted_transfer_overquota, transferOverQuotaBanner, transferOverQuotaBannerText);
		} else {
    		transferOverQuotaBanner.setVisibility(View.GONE);
		}
	}

	/**
	 * Hides the "transfer over quota" banner.
	 */
	private void hideTransferOverQuotaBanner() {
		MegaApplication.getTransfersManagement().setTransferOverQuotaBannerShown(false);
		setTransferOverQuotaBannerVisibility();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		backupWarningDialog = fileBackupManager.getBackupWarningDialog();
		if(backupWarningDialog != null && backupWarningDialog.isShowing()){
			backupHandleList = fileBackupManager.getBackupHandleList();
			backupNodeHandle = fileBackupManager.getBackupNodeHandle();
			backupNodeType = fileBackupManager.getBackupNodeType();
			backupActionType = fileBackupManager.getBackupActionType();
			backupDialogType = fileBackupManager.getBackupDialogType();
			if(backupHandleList != null) {
				outState.putSerializable(BACKUP_HANDLED_ITEM, backupHandleList);
			}
			outState.putLong(BACKUP_HANDLED_NODE, backupNodeHandle);
			outState.putInt(BACKUP_NODE_TYPE, backupNodeType);
			outState.putInt(BACKUP_ACTION_TYPE, backupActionType);
			outState.putInt(BACKUP_DIALOG_WARN, backupDialogType);
			backupWarningDialog.dismiss();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (savedInstanceState != null) {
			backupHandleList = (ArrayList<Long>) savedInstanceState.getSerializable(BACKUP_HANDLED_ITEM);
			backupNodeHandle = savedInstanceState.getLong(BACKUP_HANDLED_NODE, -1);
			backupNodeType = savedInstanceState.getInt(BACKUP_NODE_TYPE, -1);
			backupActionType = savedInstanceState.getInt(BACKUP_ACTION_TYPE, -1);
			backupDialogType = savedInstanceState.getInt(BACKUP_DIALOG_WARN, -1);
			if (backupDialogType == 0) {
				fileBackupManager.actWithBackupTips(backupHandleList, megaApi.getNodeByHandle(backupNodeHandle), backupNodeType, backupActionType);
			} else if (backupDialogType == 1) {
				fileBackupManager.confirmationActionForBackup(backupHandleList, megaApi.getNodeByHandle(backupNodeHandle), backupNodeType, backupActionType);
			} else {
				logDebug("Backup warning dialog is not show");
			}
		}

	}

	/**
	 * Get the nodes for operation of file backup
	 *
	 * @return the list of selected nodes
	 */
	public ArrayList<MegaNode> getNodeList() {
		return nodes;
	}

	public MediaDiscoveryFragment showMediaDiscovery(Unit u) {
		MediaDiscoveryFragment f = MediaDiscoveryFragment.getInstance(mediaHandle);
		((ManagerActivity) context).skipToMediaDiscoveryFragment(f);
		return f;
	}
}
