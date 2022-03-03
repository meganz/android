package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
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
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.PositionDividerItemDecoration;
import mega.privacy.android.app.fragments.homepage.EventObserver;
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.lollipop.DrawerItem;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.lollipop.ManagerActivity;
import mega.privacy.android.app.lollipop.PdfViewerActivity;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode;
import static mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped;
import static mega.privacy.android.app.utils.Util.*;

@AndroidEntryPoint
public class InboxFragment extends RotatableFragment{

	@Inject
	SortOrderManagement sortOrderManagement;
	
	Context context;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;
	MegaNodeAdapter adapter;
	MegaNode inboxNode;

	ArrayList<MegaNode> nodes;
	
	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;
	Stack<Integer> lastPositionStack;
	
	MegaApiAndroid megaApi;
	String downloadLocationDefaultPath;
	
	private ActionMode actionMode;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	DatabaseHandler dbH;
	MegaPreferences prefs;

	@Override
	protected RotatableAdapter getAdapter() {
		return adapter;
	}

	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	@Override
	public void multipleItemClick(int position) {
		adapter.toggleSelection(position);
	}

	@Override
	public void reselectUnHandledSingleItem(int position) {
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

			List<MegaNode> documents = adapter.getSelectedNodes();
			
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
					ArrayList<Long> handleList = new ArrayList<Long>();
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
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);

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
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					((ManagerActivity) context).askConfirmationMoveToRubbish(handleList);

					clearSelections();
					hideMultipleSelect();
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
			inflater.inflate(R.menu.file_browser_action, menu);
            checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
            checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();

			menu.findItem(R.id.cab_menu_share_link)
					.setTitle(StringResourcesUtils.getQuantityString(R.plurals.get_links, selected.size()));

			boolean areAllNotTakenDown = MegaNodeUtil.areAllNotTakenDown(selected);
			boolean showDownload = false;
			boolean showSendToChat = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;

			MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);

			if (selected.size() != 0) {

				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else if(selected.size()==1){
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}

				if(selected.size()==1){
					showRename = true;
				}
				else{
					showRename = false;
				}

				showDownload = areAllNotTakenDown;
				showTrash = true;
				showMove = true;
				showCopy = areAllNotTakenDown;
				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getInboxNode()).getErrorCode() != MegaError.API_OK)	{
						showTrash = false;
						showMove = false;
						break;
					}
				}
				//showSendToChat
				showSendToChat = areAllFileNodesAndNotTakenDown(selected);
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}

			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			if(showDownload){
				menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			menu.findItem(R.id.cab_menu_send_to_chat).setVisible(showSendToChat);
			if(showSendToChat) {
				menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);

			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			if(showCopy){
				menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			if(showMove){
				menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			if (showTrash){
				menu.findItem(R.id.cab_menu_trash).setTitle(context.getString(R.string.context_move_to_trash));
			}

			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			
			return false;
		}		
	}
	
	public boolean showSelectMenuItem(){
		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}

	public void selectAll(){
		if (adapter != null){
			if(adapter.isMultipleSelect()){
				adapter.selectAll();
			}
			else{					
				adapter.setMultipleSelect(true);
				adapter.selectAll();
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
			}

			new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
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
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		logDebug("onCreate");

		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		downloadLocationDefaultPath = getDownloadLocation();

		lastPositionStack = new Stack<>();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}

	public void checkScroll () {
		if (recyclerView != null) {
			if ((recyclerView.canScrollVertically(-1) && recyclerView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect())) {
				((ManagerActivity) context).changeAppBarElevation(true);
			}
			else {
				((ManagerActivity) context).changeAppBarElevation(false);
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		SortByHeaderViewModel sortByHeaderViewModel = new ViewModelProvider(this)
				.get(SortByHeaderViewModel.class);

		sortByHeaderViewModel.getShowDialogEvent().observe(getViewLifecycleOwner(),
				new EventObserver<>(this::showSortByPanel));

		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;

		if (((ManagerActivity) context).getParentHandleInbox() == -1||((ManagerActivity) context).getParentHandleInbox()==megaApi.getInboxNode().getHandle()) {
			logWarning("Parent Handle == -1");

			if (megaApi.getInboxNode() != null){
				logDebug("InboxNode != null");
				inboxNode = megaApi.getInboxNode();
				nodes = megaApi.getChildren(inboxNode, sortOrderManagement.getOrderCloud());
			}
		}
		else{
			logDebug("Parent Handle: " + ((ManagerActivity) context).getParentHandleInbox());
			MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivity) context).getParentHandleInbox());

			if(parentNode!=null){
				logDebug("Parent Node Handle: " + parentNode.getHandle());
				nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
			}

		}
		((ManagerActivity)context).supportInvalidateOptionsMenu();
        ((ManagerActivity) context).setToolbarTitle();
	    
		if (((ManagerActivity) context).isList){
			View v = inflater.inflate(R.layout.fragment_inboxlist, container, false);

			recyclerView = (RecyclerView) v.findViewById(R.id.inbox_list_view);
			mLayoutManager = new LinearLayoutManager(context);
			//Add bottom padding for recyclerView like in other fragments.
			recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setItemAnimator(noChangeRecyclerViewItemAnimator());
			recyclerView.addItemDecoration(new PositionDividerItemDecoration(requireContext(), getOutMetrics()));
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = (ImageView) v.findViewById(R.id.inbox_list_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.inbox_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.inbox_list_empty_text_first);

			if (adapter == null){
				adapter = new MegaNodeAdapter(context, this, nodes,
						((ManagerActivity) context).getParentHandleInbox(),
						recyclerView, INBOX_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST, sortByHeaderViewModel);
			}
			else{
				adapter.setParentHandle(((ManagerActivity) context).getParentHandleInbox());
                adapter.setListFragment(recyclerView);
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}	

			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);

			setNodes(nodes);
			return v;
		}
		else{
			logDebug("Grid View");
			View v = inflater.inflate(R.layout.fragment_inboxgrid, container, false);
			
			recyclerView = (NewGridRecyclerView) v.findViewById(R.id.inbox_grid_view);
			recyclerView.setHasFixedSize(true);
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();

			recyclerView.setItemAnimator(new DefaultItemAnimator());

			emptyImageView = (ImageView) v.findViewById(R.id.inbox_grid_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.inbox_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.inbox_grid_empty_text_first);

			if (adapter == null){
				adapter = new MegaNodeAdapter(context, this, nodes,
						((ManagerActivity) context).getParentHandleInbox(), recyclerView,
						INBOX_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID, sortByHeaderViewModel);
			}
			else{
				adapter.setParentHandle(((ManagerActivity) context).getParentHandleInbox());
				adapter.setListFragment(recyclerView);
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}

			gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup(gridLayoutManager.getSpanCount()));

			recyclerView.setAdapter(adapter);

			setNodes(nodes);

			setContentText();

			return v;
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		observeDragSupportEvents(getViewLifecycleOwner(), recyclerView, VIEWER_FROM_INBOX);
	}

	public void refresh(){
		logDebug("refresh");
		if(inboxNode != null && (((ManagerActivity) context).getParentHandleInbox()==-1||((ManagerActivity) context).getParentHandleInbox()==inboxNode.getHandle())){
			nodes = megaApi.getChildren(inboxNode, sortOrderManagement.getOrderCloud());
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivity) context).getParentHandleInbox());
			if(parentNode!=null){
				logDebug("Parent Node Handle: " + parentNode.getHandle());
				nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
			}
		}

		setNodes(nodes);
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

	public void openFile(MegaNode node, int position) {
		if (MimeTypeList.typeForName(node.getName()).isImage()) {
			Intent intent = ImageViewerActivity.getIntentForParentNode(
					requireContext(),
					megaApi.getParentNode(node).getHandle(),
					sortOrderManagement.getOrderCloud(),
					node.getHandle()
			);
			putThumbnailLocation(intent, recyclerView, position, VIEWER_FROM_INBOX, adapter);
			startActivity(intent);
			((ManagerActivity) context).overridePendingTransition(0, 0);
		} else if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isAudio()) {
			MegaNode file = node;

			String mimeType = MimeTypeList.typeForName(file.getName()).getType();

			Intent mediaIntent;
			boolean internalIntent;
			boolean opusFile = false;
			if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()) {
				mediaIntent = new Intent(Intent.ACTION_VIEW);
				internalIntent = false;
				String[] s = file.getName().split("\\.");
				if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) {
					opusFile = true;
				}
			} else {
				internalIntent = true;
				mediaIntent = getMediaIntent(context, node.getName());
			}
			mediaIntent.putExtra("position", position);
			if (megaApi.getParentNode(node).getType() == MegaNode.TYPE_INCOMING) {
				mediaIntent.putExtra("parentNodeHandle", -1L);
			} else {
				mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(node).getHandle());
			}

			mediaIntent.putExtra("orderGetChildren", sortOrderManagement.getOrderCloud());
			putThumbnailLocation(mediaIntent, recyclerView, position, VIEWER_FROM_INBOX, adapter);
			mediaIntent.putExtra("placeholder", adapter.getPlaceholderCount());
			mediaIntent.putExtra("HANDLE", file.getHandle());
			mediaIntent.putExtra("FILENAME", file.getName());
			mediaIntent.putExtra("adapterType", INBOX_ADAPTER);

			String localPath = getLocalFile(file);

			if (localPath != null) {
				File mediaFile = new File(localPath);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
					mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
				} else {
					mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
				}
				mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			} else {
				if (megaApi.httpServerIsRunning() == 0) {
					megaApi.httpServerStart();
					mediaIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
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

				String url = megaApi.httpServerGetLocalLink(file);
				mediaIntent.setDataAndType(Uri.parse(url), mimeType);
			}

			if (opusFile) {
				mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
			}
			if (internalIntent) {
				context.startActivity(mediaIntent);
			} else {
				if (isIntentAvailable(context, mediaIntent)) {
					context.startActivity(mediaIntent);
				} else {
					((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);
					adapter.notifyDataSetChanged();
					((ManagerActivity) context).saveNodesToDevice(
							Collections.singletonList(node),
							true, false, false, false);
				}
			}
			((ManagerActivity) context).overridePendingTransition(0, 0);
		} else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
			MegaNode file = node;

			String mimeType = MimeTypeList.typeForName(file.getName()).getType();

			Intent pdfIntent = new Intent(context, PdfViewerActivity.class);

			pdfIntent.putExtra("inside", true);
			pdfIntent.putExtra("adapterType", INBOX_ADAPTER);

			String localPath = getLocalFile(file);

			if (localPath != null) {
				File mediaFile = new File(localPath);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
					pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
				} else {
					pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
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

				String url = megaApi.httpServerGetLocalLink(file);
				pdfIntent.setDataAndType(Uri.parse(url), mimeType);
			}
			pdfIntent.putExtra("HANDLE", file.getHandle());
			putThumbnailLocation(pdfIntent, recyclerView, position, VIEWER_FROM_INBOX, adapter);
			if (isIntentAvailable(context, pdfIntent)) {
				startActivity(pdfIntent);
			} else {
				Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

				((ManagerActivity) context).saveNodesToDevice(
						Collections.singletonList(node),
						true, false, false, false);
			}
			((ManagerActivity) context).overridePendingTransition(0, 0);
		} else if (MimeTypeList.typeForName(node.getName()).isURL()) {
			manageURLNode(context, megaApi, node);
		} else if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
			manageTextFileIntent(requireContext(), node, INBOX_ADAPTER);
		} else {
			adapter.notifyDataSetChanged();
			onNodeTapped(context, node, ((ManagerActivity) context)::saveNodeByTap, (ManagerActivity) context, (ManagerActivity) context);
		}
	}

	public void itemClick(int position) {
		logDebug("itemClick");

		if (adapter.isMultipleSelect()) {
			logDebug("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0) {
				updateActionModeTitle();
			}
		} else {
			if (nodes.get(position).isFolder()) {
				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;
				if (((ManagerActivity) context).isList) {
					lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
				} else {
					lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
					if (lastFirstVisiblePosition == -1) {
						logDebug("Completely -1 then find just visible position");
						lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
					}
				}

				logDebug("Push to stack " + lastFirstVisiblePosition + " position");
				lastPositionStack.push(lastFirstVisiblePosition);

				((ManagerActivity) context).setParentHandleInbox(nodes.get(position).getHandle());

				((ManagerActivity) context).supportInvalidateOptionsMenu();
				((ManagerActivity) context).setToolbarTitle();

				nodes = megaApi.getChildren(nodes.get(position), sortOrderManagement.getOrderCloud());
				adapter.setNodes(nodes);

				setContentText();

				recyclerView.scrollToPosition(0);
				checkScroll();
			} else {
				openFile(nodes.get(position), position);
			}
		}
	}

    @Override
	protected void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
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
		Resources res = getActivity().getResources();

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
			e.printStackTrace();
			logError("Invalidate error", e);
		}
	}

	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
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

	public static InboxFragment newInstance() {
		logDebug("newInstance");
		InboxFragment fragment = new InboxFragment();
		return fragment;
	}
	
	public int onBackPressed(){
		logDebug("onBackPressed");

		if (adapter == null){
			return 0;
		}

		if (((ManagerActivity) context).comesFromNotifications && ((ManagerActivity) context).comesFromNotificationHandle == (((ManagerActivity)context).getParentHandleInbox())) {
			((ManagerActivity) context).comesFromNotifications = false;
			((ManagerActivity) context).comesFromNotificationHandle = -1;
			((ManagerActivity) context).selectDrawerItem(DrawerItem.NOTIFICATIONS);
			((ManagerActivity)context).setParentHandleInbox(((ManagerActivity)context).comesFromNotificationHandleSaved);
			((ManagerActivity)context).comesFromNotificationHandleSaved = -1;

			return 2;
		}
		else {
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(((ManagerActivity) context).getParentHandleInbox()));
			if (parentNode != null) {
				logDebug("Parent Node Handle: " + parentNode.getHandle());

				((ManagerActivity)context).supportInvalidateOptionsMenu();

				((ManagerActivity) context).setParentHandleInbox(parentNode.getHandle());
				((ManagerActivity) context).setToolbarTitle();

				nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
				setNodes(nodes);

				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");

				if(lastVisiblePosition>=0){

					if(((ManagerActivity) context).isList){
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
					else{
						gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
				}
				return 2;
			}
			else{
				return 0;
			}
		}
	}

	public boolean getIsList(){
		return ((ManagerActivity) context).isList;
	}
	
	public long getParentHandle(){
		return ((ManagerActivity) context).getParentHandleInbox();
	}

	public RecyclerView getRecyclerView(){
		return recyclerView;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		logDebug("setNodes");
		this.nodes = nodes;
		if (adapter != null){
			adapter.setNodes(nodes);
			setContentText();
		}	
	}

	public void setContentText(){
		logDebug("setContentText");

		if (adapter.getItemCount() == 0){

			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);

			if (megaApi.getInboxNode().getHandle()==((ManagerActivity)context).getParentHandleInbox()||((ManagerActivity)context).getParentHandleInbox()==-1) {
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.inbox_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.inbox_empty);
				}

				String textToShow = StringResourcesUtils.getString(R.string.context_empty_inbox);
				try{
					textToShow = textToShow.replace(
							"[A]", "<font color=\'"
									+ ColorUtils.getColorHexString(requireContext(), R.color.grey_900_grey_100)
									+ "\'>"
					).replace("[/A]", "</font>").replace(
							"[B]", "<font color=\'"
									+ ColorUtils.getColorHexString(requireContext(), R.color.grey_300_grey_600)
									+ "\'>"
					).replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);

			} else {
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.empty_folder_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.empty_folder_portrait);
				}
				String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
				try{
					textToShow = textToShow.replace(
							"[A]", "<font color=\'"
									+ ColorUtils.getColorHexString(requireContext(), R.color.grey_900_grey_100)
									+ "\'>"
					).replace("[/A]", "</font>").replace(
							"[B]", "<font color=\'"
									+ ColorUtils.getColorHexString(requireContext(), R.color.grey_300_grey_600)
									+ "\'>"
					).replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);

			}
		}
		else{
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
	}

	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}

	public int getItemCount(){
		if(adapter != null){
			return adapter.getItemCount();
		}
		return 0;
	}
}
