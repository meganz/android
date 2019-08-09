package mega.privacy.android.app.lollipop.managerSections;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.NewHeaderItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.FileUtils.*;

public class FileBrowserFragmentLollipop extends RotatableFragment implements OnClickListener{

	public static ImageView imageDrag;

	Context context;
	ActionBar aB;
	LinearLayout linearLayoutRecycler;
	RecyclerView recyclerView;
	FastScroller fastScroller;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

    MegaNodeAdapter adapter;

	ProgressBar progressBar;

	public int pendingTransfers = 0;
	public int totalTransfers = 0;
	public long totalSizePendingTransfer=0;
	public long totalSizeTransfered=0;

	Stack<Integer> lastPositionStack;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	RelativeLayout transfersOverViewLayout;
	TextView transfersTitleText;
	TextView transfersNumberText;
	ImageView playButton;
	RelativeLayout actionLayout;
	RelativeLayout dotsOptionsTransfersLayout;

	public NewHeaderItemDecoration headerItemDecoration;

	float density;
	DisplayMetrics outMetrics;
	Display display;

	DatabaseHandler dbH;
	MegaPreferences prefs;

	ArrayList<MegaNode> nodes;
	public ActionMode actionMode;

	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;

	boolean allFiles = true;
	String downloadLocationDefaultPath;
    
    private int placeholderCount;

	RelativeLayout callInProgressLayout;
	Chronometer callInProgressChrono;

	@Override
	protected MegaNodeAdapter getAdapter() {
		return adapter;
	}

	@Override
	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	public void updateScrollPosition(int position) {
		log("updateScrollPosition");
		if (adapter != null) {
			if (adapter.getAdapterType() == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
				mLayoutManager.scrollToPosition(position);
			}
			else if (gridLayoutManager != null) {
				gridLayoutManager.scrollToPosition(position);
			}
		}
	}

	public ImageView getImageDrag(int position) {
		log("getImageDrag");
		if (adapter != null) {
			if (adapter.getAdapterType() == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
				View v = mLayoutManager.findViewByPosition(position);
				if (v != null) {
					return (ImageView) v.findViewById(R.id.file_list_thumbnail);
				}
			}
			else if (gridLayoutManager != null) {
				View v = gridLayoutManager.findViewByPosition(position);
				if (v != null) {
					return (ImageView) v.findViewById(R.id.file_grid_thumbnail);
				}
			}
		}

		return null;
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			List<MegaNode> documents = adapter.getSelectedNodes();
			switch(item.getItemId()){
				case R.id.action_mode_close_button:{
					log("on close button");
				}
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList, false);
					break;
				}
				case R.id.cab_menu_rename:{

					if (documents.size()==1){
						((ManagerActivityLollipop) context).showRenameDialog(documents.get(0), documents.get(0).getName());
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
				case R.id.cab_menu_share:{
					//Check that all the selected options are folders
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						if(documents.get(i).isFolder()){
							handleList.add(documents.get(i).getHandle());
						}
					}

					NodeController nC = new NodeController(context);
					nC.selectContactToShareFolders(handleList);

					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_share_link:{

					log("Public link option");
					if(documents.get(0)==null){
						log("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showGetLinkActivity(documents.get(0).getHandle());
					clearSelections();
					hideMultipleSelect();

					break;
				}
				case R.id.cab_menu_share_link_remove:{

					log("Remove public link option");
					if(documents.get(0)==null){
						log("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(documents.get(0));
					clearSelections();
					hideMultipleSelect();

					break;
				}
				case R.id.cab_menu_edit_link:{

					log("Edit link option");
					if(documents.get(0)==null){
						log("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showGetLinkActivity(documents.get(0).getHandle());
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_send_to_chat:{
					log("Send files to chat");
					ArrayList<MegaNode> nodesSelected = adapter.getArrayListSelectedNodes();
					NodeController nC = new NodeController(context);
					nC.checkIfNodesAreMineAndSelectChatsToSendNodes(nodesSelected);
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_trash:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);

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
			log("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			((ManagerActivityLollipop)context).hideFabButton();
			((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
            ((ManagerActivityLollipop) context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_ACCENT);
			checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
			((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
			((ManagerActivityLollipop) context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_ZERO_DELAY);
			checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaNode> selected = adapter.getSelectedNodes();

			boolean showDownload = false;
			boolean showSendToChat = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showEditLink = false;
			boolean showRemoveLink = false;
			boolean showTrash = false;
			boolean showShare = false;

			menu.findItem(R.id.cab_menu_send_to_chat).setIcon(Util.mutateIconSecondary(context, R.drawable.ic_send_to_contact, R.color.white));

			// Rename
			if((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)) {
				showRename = true;
			}

			// Link
			if ((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {

				if(selected.get(0).isExported()){
					//Node has public link
					showRemoveLink=true;
					showLink=false;
					showEditLink = true;

				}
				else{
					showRemoveLink=false;
					showLink=true;
					showEditLink = false;
				}

			}


			if (selected.size() != 0) {
				showDownload = true;
				showTrash = true;
				showMove = true;
				showCopy = true;
				showShare = true;
				allFiles = true;

				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showTrash = false;
						break;
					}
					if(selected.get(i).isFile()){
						showShare = false;
					}else{
						if(selected.size()==1){
							showShare=true;
						}
					}
				}

				//showSendToChat
				for(int i=0; i<selected.size();i++)	{
					if(!selected.get(i).isFile()){
						allFiles = false;
					}
				}

				if(allFiles){
					if (Util.isChatEnabled()) {
						showSendToChat = true;
					}
					else {
						showSendToChat = false;
					}
				}else{
					showSendToChat = false;
				}

				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}


			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			menu.findItem(R.id.cab_menu_send_to_chat).setVisible(showSendToChat);
			menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);

			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			if(showCopy){
				if(selected.size()==1){
					menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

				}else{
					menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				}
			}

			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			if(showMove){
				if(selected.size()==1){
					menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

				}else{
					menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				}
			}


			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			if(showLink){
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}else{
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			}

			menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);
			if(showRemoveLink){
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}else{
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			}
			menu.findItem(R.id.cab_menu_edit_link).setVisible(showEditLink);

			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

			menu.findItem(R.id.cab_menu_share).setVisible(showShare);
			menu.findItem(R.id.cab_menu_share).setTitle(context.getResources().getQuantityString(R.plurals.context_share_folders, selected.size()));

			if(showShare){
				if(selected.size()==1){
					menu.findItem(R.id.cab_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				}else{
					menu.findItem(R.id.cab_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				}


			}else{
				menu.findItem(R.id.cab_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			}

			return false;
		}
	}

	public static FileBrowserFragmentLollipop newInstance() {
		log("newInstance");
		FileBrowserFragmentLollipop fragment = new FileBrowserFragmentLollipop();
		return fragment;
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		log("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);
		downloadLocationDefaultPath = getDownloadLocation(context);
		lastPositionStack = new Stack<>();

		if(Util.isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
			}
		}else{
			log("Chat not enabled!");
		}



		super.onCreate(savedInstanceState);
		log("after onCreate called super");
	}

	public void checkScroll() {
		if (recyclerView != null) {
			if ((recyclerView.canScrollVertically(-1) && recyclerView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect())) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			}
			else if (!isMultipleselect()) {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		if (!isAdded()) {
			return null;
		}

		log("fragment ADDED");

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

		if (((ManagerActivityLollipop) context).parentHandleBrowser == -1 || ((ManagerActivityLollipop) context).parentHandleBrowser == megaApi.getRootNode().getHandle()) {
			log("After consulting... the parent keeps -1 or ROOTNODE: " + ((ManagerActivityLollipop) context).parentHandleBrowser);

			nodes = megaApi.getChildren(megaApi.getRootNode(), ((ManagerActivityLollipop) context).orderCloud);
		} else {
			MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop) context).parentHandleBrowser);

			nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop) context).orderCloud);
		}
		((ManagerActivityLollipop) context).setToolbarTitle();
		((ManagerActivityLollipop) context).supportInvalidateOptionsMenu();

		if (((ManagerActivityLollipop) context).isList) {
			log("FileBrowserFragmentLollipop isList");

			log("isList");
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

			linearLayoutRecycler = v.findViewById(R.id.linear_layout_recycler);
			recyclerView = v.findViewById(R.id.file_list_view_browser);
			fastScroller = v.findViewById(R.id.fastscroll);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);

			mLayoutManager = new LinearLayoutManager(context);
			mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setHasFixedSize(true);
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = v.findViewById(R.id.file_list_empty_image);
			emptyTextView = v.findViewById(R.id.file_list_empty_text);
			emptyTextViewFirst = v.findViewById(R.id.file_list_empty_text_first);

			callInProgressLayout = v.findViewById(R.id.call_in_progress_layout);
			callInProgressLayout.setOnClickListener(this);
			callInProgressChrono = v.findViewById(R.id.call_in_progress_chrono);
			callInProgressLayout.setVisibility(View.GONE);
			callInProgressChrono.setVisibility(View.GONE);


			transfersOverViewLayout = v.findViewById(R.id.transfers_overview_item_layout);
			transfersTitleText = v.findViewById(R.id.transfers_overview_title);
			transfersNumberText = v.findViewById(R.id.transfers_overview_number);
			playButton = v.findViewById(R.id.transfers_overview_button);
			actionLayout = v.findViewById(R.id.transfers_overview_action_layout);
			dotsOptionsTransfersLayout = v.findViewById(R.id.transfers_overview_three_dots_layout);
			progressBar = v.findViewById(R.id.transfers_overview_progress_bar);

			transfersOverViewLayout.setOnClickListener(this);

			if (adapter == null) {
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop) context).parentHandleBrowser, recyclerView, aB, Constants.FILE_BROWSER_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			} else {
				adapter.setParentHandle(((ManagerActivityLollipop) context).parentHandleBrowser);
				adapter.setListFragment(recyclerView);
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
            }

            adapter.setMultipleSelect(false);

            recyclerView.setAdapter(adapter);
            fastScroller.setRecyclerView(recyclerView);

            setNodes(nodes);

            if (adapter.getItemCount() == 0) {
                log("itemCount is 0");
                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                log("itemCount is " + adapter.getItemCount());
                recyclerView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }

            setOverviewLayout();
			showCallLayout();
			return v;

        } else {
            log("Grid View");
            log("FileBrowserFragmentLollipop isGrid");
            View v = inflater.inflate(R.layout.fragment_filebrowsergrid,container,false);
			linearLayoutRecycler = (LinearLayout) v.findViewById(R.id.linear_layout_recycler);
            recyclerView = (NewGridRecyclerView)v.findViewById(R.id.file_grid_view_browser);
            fastScroller = (FastScroller)v.findViewById(R.id.fastscroll);
            
            recyclerView.setPadding(0,0,0,Util.scaleHeightPx(80,outMetrics));
            
            recyclerView.setClipToPadding(false);
            recyclerView.setHasFixedSize(true);
            
            gridLayoutManager = (CustomizedGridLayoutManager)recyclerView.getLayoutManager();
            recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});
			progressBar = v.findViewById(R.id.file_grid_download_progress_bar);

			emptyImageView = v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = v.findViewById(R.id.file_grid_empty_text);
			emptyTextViewFirst = v.findViewById(R.id.file_grid_empty_text_first);

			transfersOverViewLayout = v.findViewById(R.id.transfers_overview_item_layout);
			transfersTitleText = v.findViewById(R.id.transfers_overview_title);
			transfersNumberText = v.findViewById(R.id.transfers_overview_number);
			playButton = v.findViewById(R.id.transfers_overview_button);
			actionLayout = v.findViewById(R.id.transfers_overview_action_layout);
			dotsOptionsTransfersLayout = v.findViewById(R.id.transfers_overview_three_dots_layout);
			progressBar = v.findViewById(R.id.transfers_overview_progress_bar);

			transfersOverViewLayout.setOnClickListener(this);

            if (adapter == null) {
                adapter = new MegaNodeAdapter(context,this,nodes,((ManagerActivityLollipop)context).parentHandleBrowser,recyclerView,aB,Constants.FILE_BROWSER_ADAPTER,MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
            } else {
                adapter.setParentHandle(((ManagerActivityLollipop)context).parentHandleBrowser);
                adapter.setListFragment(recyclerView);
                adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
            }

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
            
            setOverviewLayout();
            
            return v;
        }
    }

	public void setOverviewLayout() {
		log("setOverviewLayout");


		//Check transfers in progress
		pendingTransfers = megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();
		totalTransfers = megaApi.getTotalDownloads() + megaApi.getTotalUploads();
		
		totalSizePendingTransfer = megaApi.getTotalDownloadBytes() + megaApi.getTotalUploadBytes();
		totalSizeTransfered = megaApi.getTotalDownloadedBytes() + megaApi.getTotalUploadedBytes();

		if (pendingTransfers > 0) {
			log("Transfers in progress");
			transfersOverViewLayout.setVisibility(View.VISIBLE);
			dotsOptionsTransfersLayout.setOnClickListener(this);
			actionLayout.setOnClickListener(this);
			
			updateTransferButton();
            long delay = megaApi.getBandwidthOverquotaDelay();

            if (delay == 0) {
			} else {
                log("Overquota delay activated until: " + delay);
                transfersTitleText.setText(getString(R.string.title_depleted_transfer_overquota));
            }

            //this number could be negative - totalTransfers has been reset to 0, however pendingTransfers has not been reset yet due to the wait time of async response
            int progressPercent = (int)Math.round((double)totalSizeTransfered / totalSizePendingTransfer * 100);
            int inProgress = totalTransfers - pendingTransfers + 1;
            String progressText;
            if(inProgress > 0){
                progressText = getResources().getQuantityString(R.plurals.text_number_transfers,totalTransfers,inProgress,totalTransfers);
            }else{
                progressText = getString(R.string.label_process_finishing);
                progressPercent = 100;
            }
            progressBar.setProgress(progressPercent);
            log("Progress Percent: " + progressPercent);

            transfersNumberText.setText(progressText);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)linearLayoutRecycler.getLayoutParams();
			params.addRule(RelativeLayout.BELOW,transfersOverViewLayout.getId());
			linearLayoutRecycler.setLayoutParams(params);
		} else {
			log("NO TRANSFERS in progress");

			if (transfersOverViewLayout != null) {
				transfersOverViewLayout.setVisibility(View.GONE);
			}

			dotsOptionsTransfersLayout.setOnClickListener(null);
			actionLayout.setOnClickListener(null);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)linearLayoutRecycler.getLayoutParams();
			linearLayoutRecycler.setLayoutParams(params);
		}
	}
    
    @Override
    public void onAttach(Activity activity) {
        log("onAttach1");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }
    
    @Override
    public void onAttach(Context context) {
        log("onAttach2");
        
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)context).getSupportActionBar();
    }
    
    @Override
    public void onClick(View v) {
        log("onClick");
        switch (v.getId()) {
            
            case R.id.transfers_overview_three_dots_layout: {
                log("click show options");
                ((ManagerActivityLollipop)getActivity()).showTransfersPanel();
                break;
            }
            case R.id.transfers_overview_action_layout: {
                log("click play/pause");
                
                ((ManagerActivityLollipop)getActivity()).changeTransfersStatus();
                break;
            }
            case R.id.transfers_overview_item_layout: {
                log("click transfers layout");
                
                ((ManagerActivityLollipop)getActivity()).selectDrawerItemTransfers();
                ((ManagerActivityLollipop)getActivity()).invalidateOptionsMenu();
                break;
            }
			case R.id.call_in_progress_layout:{
				log("onClick:call_in_progress_layout");
				if(checkPermissionsCall()){
					ChatUtil.returnCall(context, megaChatApi);
				}
				break;
			}
        }
    }
    
    public void updateTransferButton() {
        log("updateTransferButton");
        
        if (transfersOverViewLayout.getVisibility() == View.VISIBLE) {
            if (megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD) || megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
                log("show PLAY button");
                playButton.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_play));
                transfersTitleText.setText(getString(R.string.paused_transfers_title));
            } else {
                log("show PAUSE button");
                playButton.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_pause));
                transfersTitleText.setText(getString(R.string.section_transfers));
            }
        } else {
            log("Transfer panel not visible");
        }
    }

    public void itemClick(int position,int[] screenPosition,ImageView imageView) {
        log("item click position: " + position);
        if (adapter.isMultipleSelect()) {
            log("itemClick:multiselectON");
            adapter.toggleSelection(position);

            List<MegaNode> selectedNodes = adapter.getSelectedNodes();
            if (selectedNodes.size() > 0) {
                updateActionModeTitle();
            }
		}
		else{
			log("itemClick:multiselectOFF");
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;
				if(((ManagerActivityLollipop)context).isList){
					lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
					log("lastFirstVisiblePosition: "+lastFirstVisiblePosition);

				}
				else{
					lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
					if(lastFirstVisiblePosition==-1){
						log("Completely -1 then find just visible position");
						lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
					}
				}

				log("Push to stack "+lastFirstVisiblePosition+" position");
				lastPositionStack.push(lastFirstVisiblePosition);
				setFolderInfoNavigation(n);
			}
			else{
				//Is file
				log("itemClick:isFile");
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					log("itemClick:isFile:isImage");
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
                    //Put flag to notify FullScreenImageViewerLollipop.
					intent.putExtra("placeholder", placeholderCount);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", Constants.FILE_BROWSER_ADAPTER);
					intent.putExtra("isFolderLink", false);
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}

					intent.putExtra("orderGetChildren", ((ManagerActivityLollipop)context).orderCloud);
					intent.putExtra("screenPosition", screenPosition);
					context.startActivity(intent);
					((ManagerActivityLollipop) context).overridePendingTransition(0,0);
					imageDrag = imageView;
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isVideoReproducible() || MimeTypeList.typeForName(nodes.get(position).getName()).isAudio() ){
					log("itemClick:isFile:isVideoReproducibleOrIsAudio");

					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					log("itemClick:FILENAME: " + file.getName() + " TYPE: "+mimeType);

					Intent mediaIntent;
					boolean internalIntent;
					boolean opusFile = false;
					if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()){
						mediaIntent = new Intent(Intent.ACTION_VIEW);
						internalIntent=false;
						String[] s = file.getName().split("\\.");
						if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
							opusFile = true;
						}
					}
					else {
						log("itemClick:setIntentToAudioVideoPlayer");
						mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
						internalIntent=true;
					}
					mediaIntent.putExtra("position", position);
                    mediaIntent.putExtra("placeholder", placeholderCount);
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
						mediaIntent.putExtra("parentNodeHandle", -1L);
					}
					else{
						mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}
					mediaIntent.putExtra("orderGetChildren", ((ManagerActivityLollipop)context).orderCloud);
					mediaIntent.putExtra("adapterType", Constants.FILE_BROWSER_ADAPTER);
					mediaIntent.putExtra("screenPosition", screenPosition);

					mediaIntent.putExtra("FILENAME", file.getName());
					boolean isOnMegaDownloads = false;
					String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
					File f = new File(downloadLocationDefaultPath, file.getName());
					if(f.exists() && (f.length() == file.getSize())){
						isOnMegaDownloads = true;
					}
					if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))){
						File mediaFile = new File(localPath);
						//mediaIntent.setDataAndType(Uri.parse(localPath), mimeType);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
							log("itemClick:FileProviderOption");
							Uri mediaFileUri = FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile);
							if(mediaFileUri==null){
								log("itemClick:ERROR:NULLmediaFileUri");
								((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
							}
							else{
								mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(file.getName()).getType());
							}
						}
						else{
							Uri mediaFileUri = Uri.fromFile(mediaFile);
							if(mediaFileUri==null){
								log("itemClick:ERROR:NULLmediaFileUri");
								((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
							}
							else{
								mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(file.getName()).getType());
							}
						}
						mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}
					else {
						log("itemClick:localPathNULL");

						if (megaApi.httpServerIsRunning() == 0) {
							megaApi.httpServerStart();
						}
						else{
							log("itemClick:ERROR:httpServerAlreadyRunning");
						}

						ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
						ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
						activityManager.getMemoryInfo(mi);

						if(mi.totalMem>Constants.BUFFER_COMP){
							log("itemClick:total mem: "+mi.totalMem+" allocate 32 MB");
							megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
						}
						else{
							log("itemClick:total mem: "+mi.totalMem+" allocate 16 MB");
							megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
						}

						String url = megaApi.httpServerGetLocalLink(file);
						if(url!=null){
							Uri parsedUri = Uri.parse(url);
							if(parsedUri!=null){
								mediaIntent.setDataAndType(parsedUri, mimeType);
							}
							else{
								log("itemClick:ERROR:httpServerGetLocalLink");
								((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
							}
						}
						else{
							log("itemClick:ERROR:httpServerGetLocalLink");
							((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
						}
					}
					mediaIntent.putExtra("HANDLE", file.getHandle());
					if (opusFile){
						mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
					}
					imageDrag = imageView;
					if(internalIntent){
						context.startActivity(mediaIntent);
					}
					else{
						log("itemClick:externalIntent");
						if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
							context.startActivity(mediaIntent);
						}
						else{
							log("itemClick:noAvailableIntent");
							((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);

							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(nodes.get(position).getHandle());
							NodeController nC = new NodeController(context);
							nC.prepareForDownload(handleList, true);
						}
					}
					((ManagerActivityLollipop) context).overridePendingTransition(0,0);
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isURL()){
					log("Is URL file");
					MegaNode file = nodes.get(position);

					boolean isOnMegaDownloads = false;
					String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
					File f = new File(downloadLocationDefaultPath, file.getName());
					if(f.exists() && (f.length() == file.getSize())){
						isOnMegaDownloads = true;
					}
					log("isOnMegaDownloads: "+isOnMegaDownloads);
					if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))){
						File mediaFile = new File(localPath);
						InputStream instream = null;

						try {
							// open the file for reading
							instream = new FileInputStream(f.getAbsolutePath());

							// if file the available for reading
							if (instream != null) {
								// prepare the file for reading
								InputStreamReader inputreader = new InputStreamReader(instream);
								BufferedReader buffreader = new BufferedReader(inputreader);

								String line1 = buffreader.readLine();
								if(line1!=null){
									String line2= buffreader.readLine();

									String url = line2.replace("URL=","");

									log("Is URL - launch browser intent");
									Intent i = new Intent(Intent.ACTION_VIEW);
									i.setData(Uri.parse(url));
									startActivity(i);
								}
								else{
									log("Not expected format: Exception on processing url file");
									Intent intent = new Intent(Intent.ACTION_VIEW);
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
										intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", f), "text/plain");
									} else {
										intent.setDataAndType(Uri.fromFile(f), "text/plain");
									}
									intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

									if (MegaApiUtils.isIntentAvailable(context, intent)){
										startActivity(intent);
									}
									else{
										ArrayList<Long> handleList = new ArrayList<Long>();
										handleList.add(nodes.get(position).getHandle());
										NodeController nC = new NodeController(context);
										nC.prepareForDownload(handleList, true);
									}
								}
							}
						} catch (Exception ex) {

							Intent intent = new Intent(Intent.ACTION_VIEW);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
								intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", f), "text/plain");
							} else {
								intent.setDataAndType(Uri.fromFile(f), "text/plain");
							}
							intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

							if (MegaApiUtils.isIntentAvailable(context, intent)){
								startActivity(intent);
							}
							else{
								ArrayList<Long> handleList = new ArrayList<Long>();
								handleList.add(nodes.get(position).getHandle());
								NodeController nC = new NodeController(context);
								nC.prepareForDownload(handleList, true);
							}

						} finally {
							// close the file.
							try {
								instream.close();
							} catch (IOException e) {
								log("EXCEPTION closing InputStream");
							}
						}
					}
					else {
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList, true);
					}
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isPdf()){
					log("itemClick:isFile:isPdf");
					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					log("FILENAME: " + file.getName() + " TYPE: "+mimeType);

					Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);

					pdfIntent.putExtra("inside", true);
					pdfIntent.putExtra("adapterType", Constants.FILE_BROWSER_ADAPTER);
					boolean isOnMegaDownloads = false;
					String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
					File f = new File(downloadLocationDefaultPath, file.getName());
					if(f.exists() && (f.length() == file.getSize())){
						isOnMegaDownloads = true;
					}
					log("isOnMegaDownloads: "+isOnMegaDownloads);
					if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))){
						File mediaFile = new File(localPath);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
							pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						else{
							pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}
					else {
						if (megaApi.httpServerIsRunning() == 0) {
							megaApi.httpServerStart();
						}

						ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
						ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
						activityManager.getMemoryInfo(mi);

						if(mi.totalMem>Constants.BUFFER_COMP){
							log("Total mem: "+mi.totalMem+" allocate 32 MB");
							megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
						}
						else{
							log("Total mem: "+mi.totalMem+" allocate 16 MB");
							megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
						}

						String url = megaApi.httpServerGetLocalLink(file);
						pdfIntent.setDataAndType(Uri.parse(url), mimeType);
					}
					pdfIntent.putExtra("HANDLE", file.getHandle());
					pdfIntent.putExtra("screenPosition", screenPosition);
					imageDrag = imageView;
					if (MegaApiUtils.isIntentAvailable(context, pdfIntent)){
						context.startActivity(pdfIntent);
					}
					else{
						Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList, true);
					}
					((ManagerActivityLollipop) context).overridePendingTransition(0,0);
				}
				else{
					log("itemClick:isFile:otherOption");
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList, true);
				}
			}
		}
	}

	@Override
	public void multipleItemClick(int position) {
		adapter.toggleSelection(position);
	}

	public void setFolderInfoNavigation(MegaNode n){
		log("setFolderInfoNavigation");
		String cameraSyncHandle = null;
        //Check if the item is the Camera Uploads folder
        if (dbH.getPreferences() != null) {
            prefs = dbH.getPreferences();
            if (prefs.getCamSyncHandle() != null) {
                cameraSyncHandle = prefs.getCamSyncHandle();
            } else {
                cameraSyncHandle = null;
            }
        } else {
            prefs = null;
        }
        
        if (cameraSyncHandle != null) {
            if (!(cameraSyncHandle.equals(""))) {
                if ((n.getHandle() == Long.parseLong(cameraSyncHandle))) {
                    ((ManagerActivityLollipop)context).cameraUploadsClicked();
                    return;
                }
            } else {
                if (n.getName().equals("Camera Uploads")) {
                    if (prefs != null) {
                        prefs.setCamSyncHandle(String.valueOf(n.getHandle()));
                    }
                    dbH.setCamSyncHandle(n.getHandle());
                    log("FOUND Camera Uploads!!----> " + n.getHandle());
                    ((ManagerActivityLollipop)context).cameraUploadsClicked();
                    return;
                }
            }
            
        } else {
            if (n.getName().equals("Camera Uploads")) {
                
                if (prefs != null) {
                    prefs.setCamSyncHandle(String.valueOf(n.getHandle()));
                }
                dbH.setCamSyncHandle(n.getHandle());
                log("FOUND Camera Uploads!!: " + n.getHandle());
                ((ManagerActivityLollipop)context).cameraUploadsClicked();
                return;
            }
        }
        
        //Check if the item is the Media Uploads folder
        
        String secondaryMediaHandle = null;
        
        if (prefs != null) {
            if (prefs.getMegaHandleSecondaryFolder() != null) {
                secondaryMediaHandle = prefs.getMegaHandleSecondaryFolder();
            } else {
                secondaryMediaHandle = null;
            }
        }
        
        if (secondaryMediaHandle != null) {
            if (!(secondaryMediaHandle.equals(""))) {
                if ((n.getHandle() == Long.parseLong(secondaryMediaHandle))) {
                    log("Click on Media Uploads");
                    ((ManagerActivityLollipop)context).secondaryMediaUploadsClicked();
                    return;
                }
            }
        } else {
            if (n.getName().equals(CameraUploadsService.SECONDARY_UPLOADS)) {
                if (prefs != null) {
                    prefs.setMegaHandleSecondaryFolder(String.valueOf(n.getHandle()));
                }
                dbH.setSecondaryFolderHandle(n.getHandle());
                log("FOUND Media Uploads!!: " + n.getHandle());
                ((ManagerActivityLollipop)context).secondaryMediaUploadsClicked();
                return;
            }
        }
        
        ((ManagerActivityLollipop)context).parentHandleBrowser = n.getHandle();
        
        ((ManagerActivityLollipop)context).setToolbarTitle();
        
        adapter.setParentHandle(((ManagerActivityLollipop)context).parentHandleBrowser);
        nodes = megaApi.getChildren(n,((ManagerActivityLollipop)context).orderCloud);
        addSectionTitle(nodes,adapter.getAdapterType());
        adapter.setNodes(nodes);
        recyclerView.scrollToPosition(0);
        
        visibilityFastScroller();
        
        //If folder has no files
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyImageView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.VISIBLE);
            
            if (megaApi.getRootNode().getHandle() == n.getHandle()) {
                
                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
                } else {
                    emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
                }
                String textToShow = String.format(context.getString(R.string.context_empty_cloud_drive));
                try {
                    textToShow = textToShow.replace("[A]","<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]","</font>");
                    textToShow = textToShow.replace("[B]","<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]","</font>");
                } catch (Exception e) {
                }
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextViewFirst.setText(result);
                
            } else {
                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
                } else {
                    emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
                }
                String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
                try {
                    textToShow = textToShow.replace("[A]","<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]","</font>");
                    textToShow = textToShow.replace("[B]","<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]","</font>");
                } catch (Exception e) {
                }
                Spanned result = null;
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
        setOverviewLayout();
    }
    
    public boolean showSelectMenuItem() {
        log("showSelectMenuItem");
        if (adapter != null) {
            return adapter.isMultipleSelect();
        }
        
        return false;
    }
    
    public void selectAll() {
        log("selectAll");
        if (adapter != null) {
            if (adapter.isMultipleSelect()) {
                adapter.selectAll();
            } else {
                adapter.setMultipleSelect(true);
                adapter.selectAll();
                
                actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
            }
            
            updateActionModeTitle();
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
        log("updateActionModeTitle");
        if (actionMode == null || getActivity() == null) {
            log("RETURN");
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
            log("oninvalidate error");
        }
        
    }
    
    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        log("hideMultipleSelect");
        adapter.setMultipleSelect(false);
        
        if (actionMode != null) {
            actionMode.finish();
        }
    }
    
    public int onBackPressed() {
        log("onBackPressed");
        
        if (adapter != null) {
            log("parentHandle is: " + ((ManagerActivityLollipop)context).parentHandleBrowser);

			if (((ManagerActivityLollipop) context).comesFromNotifications && ((ManagerActivityLollipop) context).comesFromNotificationHandle == (((ManagerActivityLollipop)context).parentHandleBrowser)) {
				((ManagerActivityLollipop) context).comesFromNotifications = false;
				((ManagerActivityLollipop) context).comesFromNotificationHandle = -1;
				((ManagerActivityLollipop) context).selectDrawerItemLollipop(ManagerActivityLollipop.DrawerItem.NOTIFICATIONS);
				((ManagerActivityLollipop)context).parentHandleBrowser = ((ManagerActivityLollipop)context).comesFromNotificationHandleSaved;
				((ManagerActivityLollipop)context).comesFromNotificationHandleSaved = -1;

				return 2;
			}
			else {
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleBrowser));
				if (parentNode != null) {
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);

					((ManagerActivityLollipop)context).parentHandleBrowser = parentNode.getHandle();
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

					((ManagerActivityLollipop)context).setToolbarTitle();

					nodes = megaApi.getChildren(parentNode,((ManagerActivityLollipop)context).orderCloud);
					addSectionTitle(nodes,adapter.getAdapterType());
					adapter.setNodes(nodes);

					visibilityFastScroller();

					setOverviewLayout();

					int lastVisiblePosition = 0;
					if (!lastPositionStack.empty()) {
						lastVisiblePosition = lastPositionStack.pop();
						log("Pop of the stack " + lastVisiblePosition + " position");
					}
					log("Scroll to " + lastVisiblePosition + " position");

					if (lastVisiblePosition >= 0) {

						if (((ManagerActivityLollipop)context).isList) {
							mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition,0);
						} else {
							gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition,0);
						}
					}
					log("return 2");
					return 2;
				} else {
					log("ParentNode is NULL");
					return 0;
				}
			}
        }

        return 0;
    }

	public void scrollToFirstPosition () {
		if (((ManagerActivityLollipop)context).isList) {
			mLayoutManager.scrollToPositionWithOffset(0,0);
		}
		else {
			gridLayoutManager.scrollToPositionWithOffset(0,0);
		}
	}
    
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
    
    public void addSectionTitle(List<MegaNode> nodes,int type) {
        Map<Integer, String> sections = new HashMap<>();
        int folderCount = 0;
        int fileCount = 0;
        for (MegaNode node : nodes) {
            if(node == null) {
                continue;
            }
            if (node.isFolder()) {
                folderCount++;
            }
            if (node.isFile()) {
                fileCount++;
            }
        }

        if (type == MegaNodeAdapter.ITEM_VIEW_TYPE_GRID) {
            int spanCount = 2;
            if (recyclerView instanceof NewGridRecyclerView) {
                spanCount = ((NewGridRecyclerView)recyclerView).getSpanCount();
            }
            if(folderCount > 0) {
                for (int i = 0;i < spanCount;i++) {
                    sections.put(i,getString(R.string.general_folders));
                }
            }
            
            if(fileCount > 0 ) {
                placeholderCount =  (folderCount % spanCount) == 0 ? 0 : spanCount - (folderCount % spanCount);
                if (placeholderCount == 0) {
                    for (int i = 0;i < spanCount;i++) {
                        sections.put(folderCount + i,getString(R.string.general_files));
                    }
                } else {
                    for (int i = 0;i < spanCount;i++) {
                        sections.put(folderCount + placeholderCount + i,getString(R.string.general_files));
                    }
                }
            }
        } else {
            placeholderCount = 0;
            sections.put(0,getString(R.string.general_folders));
            sections.put(folderCount,getString(R.string.general_files));
        }

		if (headerItemDecoration == null) {
			log("create new decoration");
			headerItemDecoration = new NewHeaderItemDecoration(context);
		} else {
		    log("Remove old decoration");
		    recyclerView.removeItemDecoration(headerItemDecoration);
        }
		headerItemDecoration.setType(type);
		headerItemDecoration.setKeys(sections);
        recyclerView.addItemDecoration(headerItemDecoration);
    }
    
    public void setNodes(ArrayList<MegaNode> nodes) {
        log("setNodes: " + nodes.size());
        
        visibilityFastScroller();
        this.nodes = nodes;
		if (((ManagerActivityLollipop)context).isList) {
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
		}
		else {
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
		}

		if (adapter != null) {
			adapter.setNodes(nodes);

			if (adapter.getItemCount() == 0) {
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (megaApi.getRootNode() != null && megaApi.getRootNode().getHandle() == ((ManagerActivityLollipop)context).parentHandleBrowser || ((ManagerActivityLollipop)context).parentHandleBrowser == -1) {

					if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_cloud_drive));
					try {
						textToShow = textToShow.replace("[A]","<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]","</font>");
						textToShow = textToShow.replace("[B]","<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]","</font>");
					} catch (Exception e) {
					}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);

				} else {
					if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
					}
					String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
					try {
						textToShow = textToShow.replace("[A]","<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]","</font>");
						textToShow = textToShow.replace("[B]","<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]","</font>");
					} catch (Exception e) {
					}
					Spanned result = null;
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
			log("adapter is NULL----------------");
		}
        
        setOverviewLayout();
    }

    private static void log(String log) {
        Util.log("FileBrowserFragmentLollipop",log);
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
            if (adapter.getItemCount() < Constants.MIN_ITEMS_SCROLLBAR) {
                fastScroller.setVisibility(View.GONE);
            } else {
                fastScroller.setVisibility(View.VISIBLE);
            }
        }
    }
	public boolean checkPermissionsCall(){
		log("checkPermissionsCall() ");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

			boolean hasCameraPermission = (ContextCompat.checkSelfPermission(((ManagerActivityLollipop) context), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
			if (!hasCameraPermission) {
				ActivityCompat.requestPermissions(((ManagerActivityLollipop) context), new String[]{Manifest.permission.CAMERA}, Constants.REQUEST_CAMERA);
				return false;
			}

			boolean hasRecordAudioPermission = (ContextCompat.checkSelfPermission(((ManagerActivityLollipop) context), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
			if (!hasRecordAudioPermission) {
				ActivityCompat.requestPermissions(((ManagerActivityLollipop) context), new String[]{Manifest.permission.RECORD_AUDIO}, Constants.RECORD_AUDIO);
				return false;
			}

			return true;
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		log("onRequestPermissionsResult");
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case Constants.REQUEST_CAMERA: {
				log("REQUEST_CAMERA");
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if(checkPermissionsCall()){
						log("REQUEST_CAMERA -> returnCall");
						ChatUtil.returnCall(context, megaChatApi);
					}
				}
				break;
			}
			case Constants.RECORD_AUDIO: {
				log("RECORD_AUDIO");
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if(checkPermissionsCall()){
						log("RECORD_AUDIO -> returnCall");
						ChatUtil.returnCall(context, megaChatApi);
					}
				}
				break;
			}

		}
	}

	public void showCallLayout() {
		ChatUtil.showCallLayout(context, megaChatApi, callInProgressLayout, callInProgressChrono);
	}

	//refresh list when item updated
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
}
