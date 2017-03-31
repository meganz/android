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
import android.widget.Toast;

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


public class IncomingSharesFragmentLollipop extends Fragment implements OnClickListener{

	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserLollipopAdapter adapter;
	IncomingSharesFragmentLollipop incomingSharesFragment = this;

	Stack<Integer> lastPositionStack;

	MegaApiAndroid megaApi;
	
	TextView contentText;	
	RelativeLayout contentTextLayout;
	boolean downloadInProgress = false;
	ProgressBar progressBar;
	ImageView transferArrow;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	long parentHandle = -1;
	int deepBrowserTree = 0;
	boolean isList = true;
	int orderGetChildren;
	
	ArrayList<MegaNode> nodes;
	MegaNode selectedNode;
	
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
		boolean showCopy = false;


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
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_leave_multiple_share: {
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					((ManagerActivityLollipop) context).showConfirmationLeaveMultipleShares(handleList);
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
				showMove = false;
				showCopy = true;

				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
					showRename = false;
					showMove = false;
					showLink = false;
				}
				else if(selected.size()==1){
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);

					if((megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)){
						showMove = true;
						showRename = true;
						showLink = true;
					}
					else if(megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK){
						showMove = false;
						showRename = true;
						showLink = true;
					}
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
					showRename = false;
					showMove = false;
					showLink = false;
				}

				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showMove = false;
						break;
					}
				}

			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(true);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(true);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(true);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(false);
			menu.findItem(R.id.cab_menu_trash).setVisible(false);
			
			return false;
		}
		
	}

	public static IncomingSharesFragmentLollipop newInstance() {
		log("newInstance");
		IncomingSharesFragmentLollipop fragment = new IncomingSharesFragmentLollipop();
		return fragment;
	}
			
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		nodes = new ArrayList<MegaNode>();
		parentHandle=-1;
		lastPositionStack = new Stack<>();

		super.onCreate(savedInstanceState);
		log("onCreate");		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView: parentHandle is: "+parentHandle);
		
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

			long parentHandleIncoming = ((ManagerActivityLollipop)context).getParentHandleIncoming();
			if(parentHandleIncoming!=-1){
				log("After consulting... the INCOMING parent is: "+parentHandleIncoming);
				parentHandle = parentHandleIncoming;
				deepBrowserTree = ((ManagerActivityLollipop)context).getDeepBrowserTreeIncoming();
				log("AND deepBrowserTree: "+deepBrowserTree);
			}
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
			
			emptyImageView.setImageResource(R.drawable.incoming_shares_empty);			
			emptyTextView.setText(R.string.file_browser_empty_incoming_shares);

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.INCOMING_SHARES_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				if (mTHash != null){
					adapter.setTransfers(mTHash);
				}
//				adapterList.setNodes(nodes);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
//				adapterList.setNodes(nodes);
			}

			if (parentHandle == -1){
				log("ParentHandle -1");
				findNodes();
				adapter.setParentHandle(-1);
				aB.setTitle(getString(R.string.section_shared_items));
				log("aB.setHomeAsUpIndicator_333");
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}
			else{
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				log("ParentHandle to find children: "+parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				adapter.setNodes(nodes);
				aB.setTitle(parentNode.getName());
				log("ic_arrow_back_white_68");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
			}
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

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

			emptyImageView.setImageResource(R.drawable.incoming_shares_empty);			
			emptyTextView.setText(R.string.file_browser_empty_incoming_shares);

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.INCOMING_SHARES_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
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
				adapter.setParentHandle(-1);
			}
			else{
				adapter.setParentHandle(parentHandle);
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
//				aB.setTitle(getString(R.string.section_shared_items));
//				log("aB.setHomeAsUpIndicator_59");
//				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
//				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
				}
				else{					
					contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
				};
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

//	public void refresh(){
//		log("refresh");
//		//TODO conservar el path
//		findNodes();
//	}
	
	public void refresh (long _parentHandle){
		log("refresh");

		parentHandle = _parentHandle;
		MegaNode parentNode=null;
		if (_parentHandle == -1){

			log("ParentHandle -1");
			findNodes();
			adapter.setParentHandle(-1);

			aB.setTitle(getString(R.string.section_shared_items));
			log("aB.setHomeAsUpIndicator_112");
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
		}
		else{
			if (megaApi.getNodeByHandle(parentHandle) == null){
				findNodes();

				parentHandle = -1;
				adapter.setParentHandle(-1);

				aB.setTitle(getString(R.string.section_shared_items));
				log("aB.setHomeAsUpIndicator_111");
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}
			else {
				adapter.setParentHandle(_parentHandle);
				parentNode = megaApi.getNodeByHandle(_parentHandle);
				log("ParentHandle: " + _parentHandle);

				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				adapter.setNodes(nodes);

				aB.setTitle(parentNode.getName());
				log("aB.setHomeAsUpIndicator_60");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop) context).setFirstNavigationLevel(false);
			}
		}
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
			showProgressBar();
		}
		else{
			if(deepBrowserTree==0){
				contentText.setText(MegaApiUtils.getInfoNodeOnlyFolders(nodes, context));
			}
			else{
				if(parentNode!=null){
					contentText.setText(MegaApiUtils.getInfoFolder(parentNode, context));
				}

			}
		}

		//If folder has no files
		if (adapter.getItemCount() == 0){
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
			emptyTextView.setText(R.string.file_browser_empty_incoming_shares);

		}
		else{
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
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
		log("onclick");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		switch(v.getId()){

			case R.id.content_text_layout:
			case R.id.content_grid_text_layout:{
				log("click show transfersFragment");
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					((ManagerActivityLollipop)getActivity()).selectDrawerItemLollipop(ManagerActivityLollipop.DrawerItem.TRANSFERS);
				}
				break;
			}
		}
	}

    public void itemClick(int position) {
    	log("itemClick");
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
				log("Is folder");
				deepBrowserTree = deepBrowserTree+1;
				
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
				log("aB.setHomeAsUpIndicator_61");
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
				((ManagerActivityLollipop)context).setParentHandleIncoming(parentHandle);
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
					intent.putExtra("fromShared", true);
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
			  			Toast.makeText(context, getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
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

	public void findNodes(){
		log("findNodes");
//		deepBrowserTree=0;
//		ArrayList<MegaUser> contacts = megaApi.getContacts();
//		nodes.clear();
//		for (int i=0;i<contacts.size();i++){
//			ArrayList<MegaNode> nodeContact=megaApi.getInShares(contacts.get(i));
//			if(nodeContact!=null){
//				if(nodeContact.size()>0){
//					nodes.addAll(nodeContact);
//				}
//			}
//		}
		nodes=megaApi.getInShares();
		for(int i=0;i<nodes.size();i++){
			log("NODE: "+nodes.get(i).getName());
		}

		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByMailDescending();
		}

		adapter.setNodes(nodes);

		if (adapter.getItemCount() == 0){
			log("adapter.getItemCount() = 0");
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
		}
		else{
			log("adapter.getItemCount() != 0");
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}

		if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
			showProgressBar();
			progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
		}
		else{
			contentText.setText(MegaApiUtils.getInfoNodeOnlyFolders(nodes, context));
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
		log("hideMultipleSelect");
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		log("onBackPressed deepBrowserTree:"+deepBrowserTree);
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (adapter == null){
			return 0;
		}

		log("deepBrowserTree-1");
		deepBrowserTree = deepBrowserTree-1;
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		if(deepBrowserTree==0){
			//In the beginning of the navigation
			log("deepTree==0");
			((ManagerActivityLollipop)context).setParentHandleIncoming(-1);
			parentHandle=-1;
			aB.setTitle(getString(R.string.section_shared_items));	
			log("aB.setHomeAsUpIndicator_62");
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			findNodes();
//				adapterList.setNodes(nodes);
			recyclerView.setVisibility(View.VISIBLE);
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
			((ManagerActivityLollipop) context).showFabButton();

			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			return 3;
		}
		else if (deepBrowserTree>0){
			log("deepTree>0");
			parentHandle = adapter.getParentHandle();
			//((ManagerActivityLollipop)context).setParentHandleSharedWithMe(parentHandle);	

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
				log("aB.setHomeAsUpIndicator_63");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = parentNode.getHandle();
				((ManagerActivityLollipop)context).setParentHandleIncoming(parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				//TODO
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
			log("ELSE deepTree");
//			((ManagerActivityLollipop)context).setParentHandleBrowser(megaApi.getRootNode().getHandle());
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
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
//					aB.setTitle(getInfoNode());
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
	
	public void setNodes(ArrayList<MegaNode> nodes){
		log("setNodes");
		this.nodes = nodes;
		adapter.setNodes(nodes);
	}
	
	public void sortByMailDescending(){
		log("sortByNameDescending");
		ArrayList<MegaNode> folderNodes = new ArrayList<MegaNode>();
		ArrayList<MegaNode> fileNodes = new ArrayList<MegaNode>();

		for (int i=0;i<nodes.size();i++){
			if (nodes.get(i).isFolder()){
				folderNodes.add(nodes.get(i));
			}
			else{
				fileNodes.add(nodes.get(i));
			}
		}

		Collections.reverse(folderNodes);
		Collections.reverse(fileNodes);

		nodes.clear();
		nodes.addAll(folderNodes);
		nodes.addAll(fileNodes);
	}

	
//	public void sortByMailAscending(){
//		log("sortByNameAscending");
//
//		ArrayList<MegaNode> folderNodes = new ArrayList<MegaNode>();
//		ArrayList<MegaNode> fileNodes = new ArrayList<MegaNode>();
//
//		for (int i=0;i<nodes.size();i++){
//			if (nodes.get(i).isFolder()){
//				folderNodes.add(nodes.get(i));
//			}
//			else{
//				fileNodes.add(nodes.get(i));
//			}
//		}
//
//		Collections.reverse(folderNodes);
//		Collections.reverse(fileNodes);
//
//		nodes.clear();
//		nodes.addAll(folderNodes);
//		nodes.addAll(fileNodes);
//
//		adapter.setNodes(nodes);
//	}
//
	public int getItemCount(){
		if(adapter != null){
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
		Util.log("IncomingSharesFragmentLollipop", log);
	}

	public int getDeepBrowserTree() {
		log("getDeepBrowserTree:" + deepBrowserTree);
		return deepBrowserTree;
	}

	public void setDeepBrowserTree(int deepBrowserTree) {
		log("setDeepBrowserTree:" + deepBrowserTree);
		this.deepBrowserTree = deepBrowserTree;
	}
}
