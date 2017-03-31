package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.MegaApplication;
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
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;


public class OutgoingSharesFragmentLollipop extends Fragment implements OnClickListener{

	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserLollipopAdapter adapter;
	OutgoingSharesFragmentLollipop outgoingSharesFragment = this;

	TextView contentText;	
	RelativeLayout contentTextLayout;
	boolean downloadInProgress = false;
	ProgressBar progressBar;
	ImageView transferArrow;

	Stack<Integer> lastPositionStack;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	MegaApiAndroid megaApi;
		
	long parentHandle = -1;
	int deepBrowserTree = 0;
	boolean isList = true;
	int orderGetChildren;
	
	ArrayList<MegaNode> nodes;

	HashMap<Long, MegaTransfer> mTHash = null;
	
	public ActionMode actionMode;

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {
		
		boolean showRename = false;
		boolean showMove = false;
		boolean showLink = false;
		boolean showTrash = false;
		boolean showShare = false;

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
			List<MegaNode> documents = adapter.getSelectedNodes();
			
			switch(item.getItemId()){
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
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
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
					break;
				}
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
					break;
				}
				case R.id.cab_menu_share_link:{

					if (documents.size()==1){
						NodeController nC = new NodeController(context);
						nC.exportLink(documents.get(0));
					}
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
			List<MegaNode> selected = adapter.getSelectedNodes();
			MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);

			if (selected.size() != 0) {
				showTrash = true;
				showMove = true;
				showShare = true;

				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showTrash = false;
						showMove = false;
						break;
					}
				}

				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
					showLink=false;
					showRename=false;
				}
				else if(selected.size()==1){
					showLink=true;
					showRename=true;
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
					showLink=false;
					showRename=false;
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(true);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(true);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share).setVisible(showShare);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			return false;
		}
		
	}

	public static OutgoingSharesFragmentLollipop newInstance() {
		log("newInstance");
		OutgoingSharesFragmentLollipop fragment = new OutgoingSharesFragmentLollipop();
		return fragment;
	}
			
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		lastPositionStack = new Stack<>();
		nodes = new ArrayList<MegaNode>();
		super.onCreate(savedInstanceState);
		log("onCreate");		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
		
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
		orderGetChildren = ((ManagerActivityLollipop)context).getOrderOthers();

		if (parentHandle == -1){

			long parentHandleOutgoing = ((ManagerActivityLollipop)context).getParentHandleOutgoing();
			if(parentHandleOutgoing!=-1){
				log("After consulting... the outgoing parent is: "+parentHandleOutgoing);
				parentHandle = parentHandleOutgoing;
				deepBrowserTree = ((ManagerActivityLollipop)context).getDeepBrowserTreeOutgoing();
			}
		}
		else{
			log("The parent is not -1: "+parentHandle);
		}

		((ManagerActivityLollipop)context).showFabButton();
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (isList){
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setItemAnimator(new DefaultItemAnimator()); 
					
			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			
			progressBar = (ProgressBar) v.findViewById(R.id.file_list_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.file_list_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_text_layout);
			contentText = (TextView) v.findViewById(R.id.content_text);			

			emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
			emptyTextView.setText(R.string.file_browser_empty_outgoing_shares);			
			
			if (adapter == null){
				log("Creating the adapter: "+parentHandle);
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.OUTGOING_SHARES_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				if (mTHash != null){
					adapter.setTransfers(mTHash);
				}
			}
			else{
				log("Before passing the parent to the adapter: "+parentHandle);
				adapter.setParentHandle(parentHandle);
//				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}

			if (parentHandle == -1){
				log("ParentHandle -1");
				findNodes();
				aB.setTitle(getString(R.string.section_shared_items));
				log("aB.setHomeAsUpIndicator_333");
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}
			else{
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				log("ParentHandle: "+parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				aB.setTitle(parentNode.getName());
				log("aB.setHomeAsUpIndicator_68");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
			}
			adapter.setNodes(nodes);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);		
			
			if (adapter != null){
				adapter.setNodes(nodes);
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);	
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}	
			
			if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
				showProgressBar();
				progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
			}
			else{
				contentText.setText(MegaApiUtils.getInfoNodeOnlyFolders(nodes, context));
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

			recyclerView.setItemAnimator(new DefaultItemAnimator());         
		
			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);

			progressBar = (ProgressBar) v.findViewById(R.id.file_grid_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.file_grid_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_grid_text_layout);
			contentText = (TextView) v.findViewById(R.id.content_grid_text);			

			emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
			emptyTextView.setText(R.string.file_browser_empty_outgoing_shares);

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.OUTGOING_SHARES_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
				if (mTHash != null){
					adapter.setTransfers(mTHash);
				}
//				adapterList.setNodes(nodes);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
//				adapterList.setNodes(nodes);
			}

			if (parentHandle == -1){
				log("ParentHandle -1");
				findNodes();
			}
			else{
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				log("ParentHandle: "+parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
			}
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			if (deepBrowserTree == 0){
				
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
					progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
				}
				else{
					contentText.setText(MegaApiUtils.getInfoNodeOnlyFolders(nodes, context));
				}
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
				}
				else{					
					contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
				}				
				aB.setTitle(infoNode.getName());
			}
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);		

			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);	
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
//			setNodes(nodes);	

			return v;
		}		
	}
	
	public void refresh (long _parentHandle){
		log("refresh with parentHandle: "+parentHandle);

		parentHandle = _parentHandle;
		MegaNode parentNode=null;
		if (_parentHandle == -1) {
			log("ParentHandle -1");
			refresh();
			return;
		}
		log("parentHandle is not -1");
		MegaNode n = megaApi.getNodeByHandle(_parentHandle);
		
		aB.setTitle(n.getName());
		log("aB.setHomeAsUpIndicator_39");
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		
		if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
			showProgressBar();
		}
		else{					
			contentText.setText(MegaApiUtils.getInfoFolder(n, context));
		}
		
		adapter.setParentHandle(parentHandle);
		nodes = megaApi.getChildren(n, orderGetChildren);
		adapter.setNodes(nodes);
		
		//If folder has no files
		if (adapter.getItemCount() == 0){
			recyclerView.setVisibility(View.GONE);
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
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}	
	}

	public void refresh(){
		log("refresh");
		findNodes();
		aB.setTitle(getString(R.string.section_shared_items));
		log("aB.setHomeAsUpIndicator_1122");
		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
		if(adapter != null){
			log("adapter != null");
			adapter.setParentHandle(-1);
			adapter.setNodes(nodes);
		}
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
	}
		
	public void findNodes(){	
		log("findNodes");
		deepBrowserTree=0;
		ArrayList<MegaShare> outNodeList = megaApi.getOutShares();
		nodes.clear();
		long lastFolder=-1;		
		
		for(int k=0;k<outNodeList.size();k++){
			
			if(outNodeList.get(k).getUser()!=null){
				MegaShare mS = outNodeList.get(k);				
				MegaNode node = megaApi.getNodeByHandle(mS.getNodeHandle());
				
				if(lastFolder!=node.getHandle()){
					lastFolder=node.getHandle();
					nodes.add(node);			
				}	
			}
		}

		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}
	}

	public void showProgressBar(){
		log("showProgressBar");
		downloadInProgress = true;
		progressBar.setVisibility(View.VISIBLE);
		transferArrow.setVisibility(View.VISIBLE);
		contentText.setText(R.string.text_downloading);
		contentTextLayout.setOnClickListener(this);
	}
	
	public void hideProgressBar(){
		log("hideProgressBar");
		downloadInProgress = false;
		progressBar.setVisibility(View.GONE);	
		transferArrow.setVisibility(View.GONE);
		if (deepBrowserTree == 0){
			contentText.setText(MegaApiUtils.getInfoNodeOnlyFolders(nodes, context));
		}
		else{
			MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
			contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
		}
		contentTextLayout.setOnClickListener(null);
	}
	
	public void updateProgressBar(int progress){
		if(downloadInProgress){
			progressBar.setProgress(progress);
		}
		else{
			showProgressBar();
			progressBar.setProgress(progress);
		}
	}
		
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {
		log("onClick");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		switch(v.getId()){
		
			case R.id.content_text_layout:
			case R.id.content_grid_text_layout:{
				log("click show transfersFragment");
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					((ManagerActivityLollipop)getActivity()).selectDrawerItemLollipop(DrawerItem.TRANSFERS);
				}				
				break;
			}
		}
	}

    public void itemClick(int position) {
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
	
    	if (adapter.isMultipleSelect()){
			log("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
			}
		}
		else{
			if (nodes.get(position).isFolder()){
				
				deepBrowserTree = deepBrowserTree+1;
				log("deepBrowserTree "+deepBrowserTree);
				
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
									
				aB.setTitle(n.getName());
				log("aB.setHomeAsUpIndicator_40");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = nodes.get(position).getHandle();
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
				}
				else{					
					contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
				}

				((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
				adapter.setParentHandle(parentHandle);
				nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
				adapter.setNodes(nodes);
				recyclerView.scrollToPosition(0);
				
				//If folder has no files
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
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
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}

				((ManagerActivityLollipop) context).showFabButton();
			}
			else{
				//Is file
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", Constants.FILE_BROWSER_ADAPTER);
					intent.putExtra("isFolderLink", false);
					MyAccountInfo accountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
					if(accountInfo!=null){
						intent.putExtra("typeAccount", accountInfo.getAccountType());
					}
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
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
			  			context.startActivity(mediaIntent);
			  		}
			  		else{
						((ManagerActivityLollipop) context).showSnackbar(context.getResources().getString(R.string.intent_not_available));
			  			adapter.notifyDataSetChanged();
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList);
					}
				}
				else{
					adapter.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList);
				}
			}
		}
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
			
			updateActionModeTitle();
		}
	}
	
	public boolean showSelectMenuItem(){
		if (adapter != null){
			return adapter.isMultipleSelect();
		}

		return false;
	}

	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}
	}
	
	public void sortByNameDescending(){
		
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaNode> tempOffline = new ArrayList<MegaNode>();
		
		
		for(int k = 0; k < nodes.size() ; k++) {
			MegaNode node = nodes.get(k);
			if(node.isFolder()){
				foldersOrder.add(node.getName());
			}
			else{
				filesOrder.add(node.getName());
			}
		}
		
	
		Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.reverse(foldersOrder);
		Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.reverse(filesOrder);

		for(int k = 0; k < foldersOrder.size() ; k++) {
			for(int j = 0; j < nodes.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = nodes.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(nodes.get(j));
				}				
			}
			
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < nodes.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = nodes.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(nodes.get(j));					
				}				
			}
			
		}
		
		nodes.clear();
		nodes.addAll(tempOffline);

		adapter.setNodes(nodes);
	}

	
	public void sortByNameAscending(){
		log("sortByNameAscending");
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaNode> tempOffline = new ArrayList<MegaNode>();
				
		for(int k = 0; k < nodes.size() ; k++) {
			MegaNode node = nodes.get(k);
			if(node.isFolder()){
				foldersOrder.add(node.getName());
			}
			else{
				filesOrder.add(node.getName());
			}
		}		
	
		Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);

		for(int k = 0; k < foldersOrder.size() ; k++) {
			for(int j = 0; j < nodes.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = nodes.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(nodes.get(j));
				}				
			}			
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < nodes.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = nodes.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(nodes.get(j));
				}				
			}
			
		}
		
		nodes.clear();
		nodes.addAll(tempOffline);

		adapter.setNodes(nodes);
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
		String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));
		String foldersStr = String.format(format, folders,
				res.getQuantityString(R.plurals.general_num_folders, folders));
		String title;
		if (files == 0 && folders == 0) {
			title = foldersStr + ", " + filesStr;
		} else if (files == 0) {
			title = foldersStr;
		} else if (folders == 0) {
			title = filesStr;
		} else {
			title = foldersStr + ", " + filesStr;
		}
		actionMode.setTitle(title);
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
		// actionMode.
	}
	
	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){

		log("onBackPressed");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		log("deepBrowserTree "+deepBrowserTree);
					
		if (adapter == null){
			return 0;
		}

		deepBrowserTree = deepBrowserTree-1;
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		if(deepBrowserTree==0){
			log("deepBrowserTree==0");
			//In the beginning of the navigation
			((ManagerActivityLollipop)context).setParentHandleOutgoing(-1);
			parentHandle=-1;
			log("Shared With Me");
			aB.setTitle(getString(R.string.section_shared_items));
			log("aB.setHomeAsUpIndicator_41");
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			findNodes();

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
			
			if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
				showProgressBar();
			}
			else{
				contentText.setText(MegaApiUtils.getInfoNodeOnlyFolders(nodes, context));
			}
			
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			((ManagerActivityLollipop) context).showFabButton();
			return 3;
		}
		else if (deepBrowserTree>0){
			log("Keep navigation");
			parentHandle = adapter.getParentHandle();
			//((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);			
			
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));	
			if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
				showProgressBar();
			}
			else{					
				contentText.setText(MegaApiUtils.getInfoFolder(parentNode, context));
			}
			
			if (parentNode != null){
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				
				aB.setTitle(parentNode.getName());		
				log("aB.setHomeAsUpIndicator_42");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = parentNode.getHandle();
				((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
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
			}
			((ManagerActivityLollipop) context).showFabButton();
			return 2;
		}
		else{
			log("Back to Cloud");
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
//			((ManagerActivityLollipop)context).setParentHandleBrowser(megaApi.getRootNode().getHandle());
			deepBrowserTree=0;
			return 0;
		}
	}
	
	public long getParentHandle(){
		if (adapter != null){
			return adapter.getParentHandle();
		}
		else{
			return -1;
		}
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
	}
	
//	public void setNodes(ArrayList<MegaNode> nodes){
//		this.nodes = nodes;
//		if (isList){
//			if (adapterList != null){
//				adapterList.setNodes(nodes);
//				if (adapterList.getCount() == 0){
//					listView.setVisibility(View.GONE);
//					emptyImageView.setVisibility(View.VISIBLE);
//					emptyTextView.setVisibility(View.VISIBLE);	
//					contentText.setVisibility(View.GONE);
//				}
//				else{
//					listView.setVisibility(View.VISIBLE);
//					emptyImageView.setVisibility(View.GONE);
//					emptyTextView.setVisibility(View.GONE);
//					contentText.setText(getInfoNode());
//					contentText.setVisibility(View.VISIBLE);
//				}			
//			}	
//		}
//		else{
//			if (adapterGrid != null){
//				adapterGrid.setNodes(nodes);
//				if (adapterGrid.getCount() == 0){
//					listView.setVisibility(View.GONE);
//					emptyImageView.setVisibility(View.VISIBLE);
//					emptyTextView.setVisibility(View.VISIBLE);					
//				}
//				else{
//					listView.setVisibility(View.VISIBLE);
//					emptyImageView.setVisibility(View.GONE);
//					emptyTextView.setVisibility(View.GONE);
//
//				}			
//			}
//		}
//	}

	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	public int getItemCount(){
		if(adapter!=null){
			return adapter.getItemCount();
		}
		return 0;
	}
	
	public void setTransfers(HashMap<Long, MegaTransfer> _mTHash){
		this.mTHash = _mTHash;
		
		if (adapter != null){
			adapter.setTransfers(mTHash);
		}	
	}
	
	public void setCurrentTransfer(MegaTransfer mT){
		if (adapter != null){
			adapter.setCurrentTransfer(mT);
		}	
	}

	public boolean isMultipleselect(){
		return adapter.isMultipleSelect();
	}


	private static void log(String log) {
		Util.log("OutgoingSharesFragmentLollipop", log);
	}

	public int getDeepBrowserTree() {
		return deepBrowserTree;
	}

	public void setDeepBrowserTree(int deepBrowserTree) {
		this.deepBrowserTree = deepBrowserTree;
	}
}
