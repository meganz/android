package mega.privacy.android.app.lollipop.managerSections;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MegaStreamingService;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.DrawerItem;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.adapters.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;


public class FileBrowserFragmentLollipop extends Fragment implements OnClickListener{

	private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserLollipopAdapter adapter;
	FileBrowserFragmentLollipop fileBrowserFragment = this;
	TextView contentText;
	RelativeLayout contentTextLayout;
	ProgressBar progressBar;

	public int pendingTransfers = 0;
	public int totalTransfers = 0;
	public long totalSizePendingTransfer=0;
	public long totalSizeTransfered=0;

	Stack<Integer> lastPositionStack;

	MegaApiAndroid megaApi;

	RelativeLayout transfersOverViewLayout;
	TextView transfersTitleText;
	TextView transfersNumberText;
	ImageButton playButton;
	ImageButton dotsOptionsTransfers;

	long parentHandle = -1;
	boolean isList = true;
	int orderGetChildren;

	float density;
	DisplayMetrics outMetrics;
	Display display;

	DatabaseHandler dbH;
	MegaPreferences prefs;

	ArrayList<MegaNode> nodes;

	public ActionMode actionMode;

//    FloatingActionButton fabButton;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;
	MegaNode selectedNode = null;

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			List<MegaNode> documents = adapter.getSelectedNodes();
			((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

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
					nC.prepareForDownload(handleList);
					break;
				}
				case R.id.cab_menu_rename:{

					if (documents.size()==1){
						((ManagerActivityLollipop) context).showRenameDialog(documents.get(0), documents.get(0).getName());
					}
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
				case R.id.cab_menu_send_file:{
					//Check that all the selected options are files
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						if(documents.get(i).isFile()){
							handleList.add(documents.get(i).getHandle());
						}
					}

					log("sendToInbox no of files: "+handleList.size());
					NodeController nC = new NodeController(context);
					nC.selectContactToSendNodes(handleList);

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
					break;
				}
				case R.id.cab_menu_share_link_remove:{

					log("Remove public link option");
					if(documents.get(0)==null){
						log("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(documents.get(0));

					break;
				}
				case R.id.cab_menu_edit_link:{

					log("Edit link option");
					if(documents.get(0)==null){
						log("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showGetLinkActivity(documents.get(0).getHandle());
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
					((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);
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
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaNode> selected = adapter.getSelectedNodes();

			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showEditLink = false;
			boolean showRemoveLink = false;
			boolean showTrash = false;
			boolean showShare = false;

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

				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showTrash = false;
						//showMove = false;
						break;
					}
					//if(showShare){
						if(selected.get(i).isFile()){
							showShare = false;
						}else{
							if(selected.size()==1){
								showShare=true;
							}

						}
					//}
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
			if(showShare){
				if(selected.size()==1){
					menu.findItem(R.id.cab_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				}else{
					menu.findItem(R.id.cab_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				}
			}else{
				menu.findItem(R.id.cab_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			}

			menu.findItem(R.id.cab_menu_send_file).setVisible(true);

			for(int i=0;i<selected.size();i++){
				MegaNode n = selected.get(i);
				if(n.isFolder()){
					menu.findItem(R.id.cab_menu_send_file).setVisible(false);
					break;
				}
			}

			return false;
		}

	}

//	private class ScrollToPositionRunnable implements Runnable{
//
//		int position;
//
//		public ScrollToPositionRunnable(int position) {
//			this.position = position;
//		}
//
//		@Override
//		public void run()
//		{
//			log("ScrollToPositionRunnable: run ->scroll to position: "+position);
//			recyclerView.scrollToPosition(position);
//			View v = recyclerView.getChildAt(position);
//			if (v != null)
//			{
//				v.requestFocus();
//			}
//		}
//	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		log("onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, recyclerView.getLayoutManager().onSaveInstanceState());
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
		prefs = dbH.getPreferences();
		lastPositionStack = new Stack<>();

		super.onCreate(savedInstanceState);
		log("after onCreate called super");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");

		if(!isAdded()){
			return null;
		}

		log("fragment ADDED");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}

		if (megaApi.getRootNode() == null){
			return null;
		}

		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;

	    isList = ((ManagerActivityLollipop)context).isList();
		orderGetChildren = ((ManagerActivityLollipop)context).getOrderCloud();

		if (parentHandle == -1){

			long parentHandleBrowser = ((ManagerActivityLollipop)context).getParentHandleBrowser();
			if(parentHandleBrowser!=-1){
				log("After consulting... the parent is: "+parentHandleBrowser);
				parentHandle = parentHandleBrowser;
			}
		}

		if (parentHandle == -1||parentHandle==megaApi.getRootNode().getHandle()){
			log("After consulting... the parent keeps -1 or ROOTNODE: "+parentHandle);

			if(aB!=null){
				aB.setTitle(getString(R.string.section_cloud_drive));
				log("indicator_menu_white_435");
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}

			nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

			if(parentNode!=null){
				log("The parent node is: "+parentNode.getName());

				if(aB!=null){
					aB.setTitle(parentNode.getName());
					log("indicator_arrow_back_035");
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				}
				else {
					log("AB still is NULL");
				}
			}
			nodes = megaApi.getChildren(parentNode, orderGetChildren);

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		}

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (isList){
			log("isList");
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setHasFixedSize(true);
			recyclerView.setItemAnimator(new DefaultItemAnimator()); 

			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_text_layout);
			contentText = (TextView) v.findViewById(R.id.content_text);

			transfersOverViewLayout = (RelativeLayout) v.findViewById(R.id.transfers_overview_item_layout);
			transfersTitleText = (TextView) v.findViewById(R.id.transfers_overview_title);
			transfersNumberText = (TextView) v.findViewById(R.id.transfers_overview_number);
			playButton = (ImageButton) v.findViewById(R.id.transfers_overview_button);
			dotsOptionsTransfers = (ImageButton) v.findViewById(R.id.transfers_overview_three_dots);
			progressBar = (ProgressBar) v.findViewById(R.id.transfers_overview_progress_bar);

			transfersOverViewLayout.setOnClickListener(this);

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.FILE_BROWSER_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
//				adapter.setNodes(nodes);
		}

			setOverviewLayout();
			
			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);			
			
			setNodes(nodes);
			
			if (adapter.getItemCount() == 0){				
				log("itemCount is 0");
				recyclerView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				log("itemCount is " + adapter.getItemCount());
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}

			return v;
		}
		else{
			log("Grid View");
			
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);

			recyclerView = (CustomizedGridRecyclerView) v.findViewById(R.id.file_grid_view_browser);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
			recyclerView.setClipToPadding(false);

			recyclerView.setHasFixedSize(true);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
//			gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//				@Override
//			      public int getSpanSize(int position) {
//					return 1;
//				}
//			});
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			
			progressBar = (ProgressBar) v.findViewById(R.id.file_grid_download_progress_bar);

			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);			
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_grid_text_layout);

			contentText = (TextView) v.findViewById(R.id.content_grid_text);			

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.FILE_BROWSER_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
				adapter.setNodes(nodes);
			}

			if (parentHandle == megaApi.getRootNode().getHandle()){
				MegaNode infoNode = megaApi.getRootNode();
				contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
			}						

			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);			
			
			setNodes(nodes);
			
			if (adapter.getItemCount() == 0){				
				
				recyclerView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}

			return v;
		}		
	}

	public void setOverviewLayout(){
		log("setOverviewLayout");
		if(isList){

			//Check transfers in progress
			pendingTransfers = megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();
			totalTransfers = megaApi.getTotalDownloads() + megaApi.getTotalUploads();

			totalSizePendingTransfer = megaApi.getTotalDownloadBytes() + megaApi.getTotalUploadBytes();
			totalSizeTransfered = megaApi.getTotalDownloadedBytes() + megaApi.getTotalUploadedBytes();

			if(pendingTransfers>0){
				log("Transfers in progress");
				contentTextLayout.setVisibility(View.GONE);
				transfersOverViewLayout.setVisibility(View.VISIBLE);
				dotsOptionsTransfers.setOnClickListener(this);
				playButton.setOnClickListener(this);

				updateTransferButton();

				int progressPercent = (int) Math.round((double) totalSizeTransfered / totalSizePendingTransfer * 100);
				progressBar.setProgress(progressPercent);
				log("Progress Percent: "+progressPercent);

				long delay = megaApi.getBandwidthOverquotaDelay();
				if(delay==0){
					transfersTitleText.setText(getString(R.string.section_transfers));
				}
				else{
					log("Overquota delay activated until: "+delay);
					transfersTitleText.setText(getString(R.string.title_depleted_transfer_overquota));
				}

				int inProgress = totalTransfers - pendingTransfers + 1;
				String progressText = getResources().getQuantityString(R.plurals.text_number_transfers, totalTransfers, inProgress, totalTransfers);
				transfersNumberText.setText(progressText);

				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recyclerView.getLayoutParams();
				params.addRule(RelativeLayout.BELOW, transfersOverViewLayout.getId());
				recyclerView.setLayoutParams(params);
			}
			else{
				log("NO TRANSFERS in progress");
				contentTextLayout.setVisibility(View.VISIBLE);
				transfersOverViewLayout.setVisibility(View.GONE);
				dotsOptionsTransfers.setOnClickListener(null);
				playButton.setOnClickListener(null);

				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recyclerView.getLayoutParams();
				params.addRule(RelativeLayout.BELOW, contentTextLayout.getId());
				recyclerView.setLayoutParams(params);

				setContentText();
			}
		}
		else{

			setContentText();
		}

	}

	@Override
    public void onAttach(Activity activity) {
		log("onAttach");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }
		
	@Override
	public void onClick(View v) {
		log("onClick");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		switch(v.getId()) {

			case R.id.transfers_overview_three_dots:{
				log("click show options");
				((ManagerActivityLollipop) getActivity()).showTransfersPanel();
				break;
			}
			case R.id.transfers_overview_button:{
				log("click play/pause");

				((ManagerActivityLollipop) getActivity()).changeTransfersStatus();
				break;
			}
			case R.id.transfers_overview_item_layout:{
				log("click transfers layout");

				((ManagerActivityLollipop) getActivity()).selectDrawerItemTransfers();
				((ManagerActivityLollipop) getActivity()).invalidateOptionsMenu();
				break;
			}
		}
	}

	public void updateTransferButton(){
		log("updateTransferButton");
		if(megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)||megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)){
			log("show PLAY button");
			playButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_play));
			transfersTitleText.setText(getString(R.string.paused_transfers_title));
		}
		else{
			log("show PAUSE button");
			playButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pause));
			transfersTitleText.setText(getString(R.string.section_transfers));
		}
	}

    public void itemClick(int position) {
		log("item click position: " + position);
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		if (adapter.isMultipleSelect()){
			log("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
				((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);
			}
//			else{
//				hideMultipleSelect();
//			}
		}
		else{
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;
				if(isList){
					lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
				}
				else{
					lastFirstVisiblePosition = ((CustomizedGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
					if(lastFirstVisiblePosition==-1){
						log("Completely -1 then find just visible position");
						lastFirstVisiblePosition = ((CustomizedGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
					}
				}

				log("Push to stack "+lastFirstVisiblePosition+" position");
				lastPositionStack.push(lastFirstVisiblePosition);
				setFolderInfoNavigation(n);
			}
			else{
				//Is file
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", Constants.FILE_BROWSER_ADAPTER);
					intent.putExtra("isFolderLink", false);
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}
					MyAccountInfo accountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
					if(accountInfo!=null){
						intent.putExtra("typeAccount", accountInfo.getAccountType());
					}
					intent.putExtra("orderGetChildren", orderGetChildren);
					startActivity(intent);

				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isVideo() || MimeTypeList.typeForName(nodes.get(position).getName()).isAudio() ){
					MegaNode file = nodes.get(position);
					Intent service = new Intent(context, MegaStreamingService.class);
					context.startService(service);
					String fileName = file.getName();
					try {
						fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
					}
					catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}

					String url = "http://127.0.0.1:4443/" + file.getBase64Handle() + "/" + fileName;
					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					System.out.println("FILENAME: " + fileName);

					Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
					mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
						startActivity(mediaIntent);
					}
					else{
						Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList);
					}
				}
				else{
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList);
				}
			}
		}
	}

	public void setFolderInfoNavigation(MegaNode n){
		log("setFolderInfoNavigation");
		String cameraSyncHandle = null;
//					if ((n.getName().compareTo(CameraSyncService.CAMERA_UPLOADS) == 0) && (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT)){
//						((ManagerActivityLollipop)context).cameraUploadsClicked();
//						return;
//					}
		//Check if the item is the Camera Uploads folder
		if(dbH.getPreferences()!=null){
			prefs = dbH.getPreferences();
			if(prefs.getCamSyncHandle()!=null){
				cameraSyncHandle = prefs.getCamSyncHandle();
			}
			else{
				cameraSyncHandle = null;
			}
		}
		else{
			prefs=null;
		}

		if(cameraSyncHandle!=null){
			if(!(cameraSyncHandle.equals("")))
			{
				if ((n.getHandle()==Long.parseLong(cameraSyncHandle))){
					((ManagerActivityLollipop)context).cameraUploadsClicked();
					return;
				}
			}
			else{
				if(n.getName().equals("Camera Uploads")){
					if (prefs != null){
						prefs.setCamSyncHandle(String.valueOf(n.getHandle()));
					}
					dbH.setCamSyncHandle(n.getHandle());
					log("FOUND Camera Uploads!!----> "+n.getHandle());
					((ManagerActivityLollipop)context).cameraUploadsClicked();
					return;
				}
			}
		}
		else{
			if(n.getName().equals("Camera Uploads")){
				if (prefs != null){
					prefs.setCamSyncHandle(String.valueOf(n.getHandle()));
				}
				dbH.setCamSyncHandle(n.getHandle());
				log("FOUND Camera Uploads!!: "+n.getHandle());
				((ManagerActivityLollipop)context).cameraUploadsClicked();
				return;
			}
		}

		//Check if the item is the Media Uploads folder

		String secondaryMediaHandle = null;

		if(prefs!=null){
			if(prefs.getMegaHandleSecondaryFolder()!=null){
				secondaryMediaHandle =prefs.getMegaHandleSecondaryFolder();
			}
			else{
				secondaryMediaHandle = null;
			}
		}

		if(secondaryMediaHandle!=null){
			if(!(secondaryMediaHandle.equals("")))
			{
				if ((n.getHandle()==Long.parseLong(secondaryMediaHandle))){
					log("Click on Media Uploads");
					((ManagerActivityLollipop)context).secondaryMediaUploadsClicked();
					log("aB.setTitle "+n.getName());
					if(aB==null){
						aB = ((AppCompatActivity)context).getSupportActionBar();
					}
					if(aB==null){
						log("AB still is NULL");
					}

					aB.setTitle(n.getName());
					log("indicator_arrow_back_003");
					return;
				}
			}
		}
		else{
			if(n.getName().equals(CameraSyncService.SECONDARY_UPLOADS)){
				if (prefs != null){
					prefs.setMegaHandleSecondaryFolder(String.valueOf(n.getHandle()));
				}
				dbH.setSecondaryFolderHandle(n.getHandle());
				log("FOUND Media Uploads!!: "+n.getHandle());
				((ManagerActivityLollipop)context).secondaryMediaUploadsClicked();
				log("aB.setTitle "+n.getName());
				if(aB==null){
					aB = ((AppCompatActivity)context).getSupportActionBar();
				}
				if(aB==null){
					log("AB still is NULL");
				}

				aB.setTitle(n.getName());
				log("indicator_arrow_back_696");
				return;
			}
		}
		log("aB.setTitle "+n.getName());
		if(aB==null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}
		if(aB==null){
			log("AB still is NULL");
		}

		aB.setTitle(n.getName());
		log("indicator_arrow_back_036");
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		parentHandle = n.getHandle();
		MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);

		contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));

		((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
		adapter.setParentHandle(parentHandle);
		nodes = megaApi.getChildren(n, orderGetChildren);
		adapter.setNodes(nodes);
		recyclerView.scrollToPosition(0);

		//If folder has no files
		if (adapter.getItemCount() == 0){
			recyclerView.setVisibility(View.GONE);
			contentTextLayout.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);

			if (megaApi.getRootNode().getHandle()==n.getHandle()) {
				emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
				emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
			} else {
				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
				emptyTextView.setText(R.string.file_browser_empty_folder);
			}
		}
		else{
			recyclerView.setVisibility(View.VISIBLE);
			contentTextLayout.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
	}
	
	public boolean showSelectMenuItem(){
		log("showSelectMenuItem");
		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}
	
	public void selectAll(){
		log("selectAll");
		if (adapter != null){
			if(adapter.isMultipleSelect()){
				adapter.selectAll();
			}
			else{				
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
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
	}
	
	private void updateActionModeTitle() {
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
			log("oninvalidate error");
		}

	}
		
	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		log("hideMultipleSelect");
		adapter.setMultipleSelect(false);
		((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_TRANSPARENT_BLACK);

		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		log("onBackPressed");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (adapter != null){
			parentHandle = adapter.getParentHandle();
			((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);

			log("parentHandle is: "+parentHandle);
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
			if (parentNode != null){
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
					log("Parent is ROOT");
					aB.setTitle(getString(R.string.section_cloud_drive));
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				}
				else{
					aB.setTitle(parentNode.getName());
					log("indicator_arrow_back_033");
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				}

				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

				parentHandle = parentNode.getHandle();
				((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				adapter.setNodes(nodes);

				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					log("Pop of the stack "+lastVisiblePosition+" position");
				}
				log("Scroll to "+lastVisiblePosition+" position");

				if(lastVisiblePosition>=0){

					if(isList){
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
					else{
						gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
				}

				adapter.setParentHandle(parentHandle);

				contentText.setText(MegaApiUtils.getInfoFolder(parentNode, context));
				log("return 2");
				return 2;
			}
			else{
				log("ParentNode is NULL");
				return 0;
			}

		}
		
		return 0;
	}
	
	public long getParentHandle(){
		if (isList){
			if (adapter != null){
				return adapter.getParentHandle();
			}
			else{
				return -1;
			}
		}
		else{
			if (adapter != null){
				return adapter.getParentHandle();
			}
			else{
				return -1;
			}
		}
	}
	
	public void setParentHandle(long parentHandle){
		log("setParentHandle: "+parentHandle);
		this.parentHandle = parentHandle;

		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}

	}

	public RecyclerView getRecyclerView(){
		return recyclerView;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		log("setNodes: "+nodes.size());
		this.nodes = nodes;
		if (isList){
			if (adapter != null){
				adapter.setNodes(nodes);
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);

					if (megaApi.getRootNode().getHandle()==parentHandle||parentHandle==-1) {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_empty_folder);
						emptyTextView.setText(R.string.file_browser_empty_folder);
					}
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
				log("Now the parentHandle of the adapter is; "+adapter.getParentHandle());
			}
			else{
				log("adapter is NULL----------------");
			}
		}
		else{
			if (adapter != null){
				adapter.setNodes(nodes);
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);

					if (megaApi.getRootNode().getHandle()==parentHandle) {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_empty_folder);
						emptyTextView.setText(R.string.file_browser_empty_folder);
					}
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}
			else{
				log("grid adapter is NULL----------------");
			}
		}
	}
	
//	public void notifyDataSetChanged(){
//		log("notifyDataSetChanged");
//		if (adapter != null){
//			adapter.notifyDataSetChanged();
//		}
//	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
//	public boolean getIsList(){
//		return isList;
//	}
	
	public void setOrder(int orderGetChildren){
		log("setOrder:Cloud");
		this.orderGetChildren = orderGetChildren;
	}

	private static void log(String log) {
		Util.log("FileBrowserFragmentLollipop", log);
	}
	
	public void setContentText(){
		log("setContentText");
		
		if (megaApi.getRootNode() != null){
			if (parentHandle == megaApi.getRootNode().getHandle()||parentHandle==-1){
				log("in ROOT node");
				MegaNode infoNode = megaApi.getRootNode();
				if (infoNode !=  null){
					contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
	//				aB.setTitle(getString(R.string.section_cloud_drive));
				}
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				if (infoNode !=  null){
					contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
	//				aB.setTitle(infoNode.getName());
				}
			}
			log("contentText: "+contentText.getText());
		}
	}

	public boolean isMultipleselect(){
		return adapter.isMultipleSelect();
	}

	public int getItemCount(){
		if(adapter!=null){
			return adapter.getItemCount();
		}
		return 0;
	}



}
