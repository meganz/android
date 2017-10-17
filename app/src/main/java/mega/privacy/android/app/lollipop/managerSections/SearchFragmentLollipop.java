package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;


public class SearchFragmentLollipop extends Fragment implements OnClickListener{

	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserLollipopAdapter adapter;
	SearchFragmentLollipop searchFragment = this;
	TextView contentText;
	RelativeLayout contentTextLayout;
	ProgressBar progressBar;
	MegaApiAndroid megaApi;
	RelativeLayout transfersOverViewLayout;

	Stack<Integer> lastPositionStack;

    private MenuItem trashIcon;


    long parentHandle = -1;
	boolean isList = true;
	int levels = -1;
	int orderGetChildren;
	
	ArrayList<MegaNode> nodes;
//	ArrayList<MegaNode> searchNodes;
	String searchQuery = null;
		
	private ActionMode actionMode;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

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
					break;
				}				
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
            trashIcon = menu.findItem(R.id.cab_menu_trash);

			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();
			MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);

			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			boolean itemsSelected = false;

			// Rename
			if((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)) {
				showRename = true;
			}
			
			// Link
			if ((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {
				showLink = true;
			}


			if (selected.size() != 0) {
				showDownload = true;
				showTrash = true;
				showMove = true;
				showCopy = true;

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
				}
				else if(selected.size()==1){

                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);

					if (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK) {
						showLink = true;
					}

					final long handle = selected.get(0).getHandle();
					MegaNode parent = megaApi.getNodeByHandle(handle);
					while (megaApi.getParentNode(parent) != null){
						parent = megaApi.getParentNode(parent);
					}

					if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
						trashIcon.setTitle(getString(R.string.context_move_to_trash));
					}else{
						trashIcon.setTitle(getString(R.string.context_remove));
					}
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);

					for(MegaNode i:selected){

						final long handle = i.getHandle();
						MegaNode parent = megaApi.getNodeByHandle(handle);
						while (megaApi.getParentNode(parent) != null){
							parent = megaApi.getParentNode(parent);
						}
						if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
							itemsSelected=true;
						}
					}

					if(!itemsSelected){
						trashIcon.setTitle(getString(R.string.context_remove));
					}else{
						trashIcon.setTitle(getString(R.string.context_move_to_trash));
					}
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);

			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			
			return false;
		}
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		lastPositionStack = new Stack<>();
		super.onCreate(savedInstanceState);
		log("onCreate");		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
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
			nodes = megaApi.search(searchQuery);

			aB.setTitle(getString(R.string.action_search)+": "+searchQuery);
			log("aB.setHomeAsUpIndicator_49");
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		}
		else{
			MegaNode n = megaApi.getNodeByHandle(parentHandle);

			aB.setTitle(n.getName());	
			log("aB.setHomeAsUpIndicator_50");
			aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
			
			((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
			nodes = megaApi.getChildren(n, orderGetChildren);

		}

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (isList){
			
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setHasFixedSize(true);
			recyclerView.setItemAnimator(new DefaultItemAnimator());

			progressBar = (ProgressBar) v.findViewById(R.id.transfers_overview_progress_bar);
			progressBar.setVisibility(View.GONE);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_text_layout);
			contentText = (TextView) v.findViewById(R.id.content_text);			

			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);

			transfersOverViewLayout = (RelativeLayout) v.findViewById(R.id.transfers_overview_item_layout);
			transfersOverViewLayout.setVisibility(View.GONE);

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.SEARCH_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				adapter.setNodes(nodes);
			}

			adapter.setMultipleSelect(false);
	
			recyclerView.setAdapter(adapter);

			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recyclerView.getLayoutParams();
			params.addRule(RelativeLayout.BELOW, contentTextLayout.getId());
			recyclerView.setLayoutParams(params);
			
			setNodes(nodes);

			return v;
		}
		else{
			log("Grid View");
			
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.file_grid_view_browser);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
			recyclerView.setClipToPadding(false);

			recyclerView.setHasFixedSize(true);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			
			progressBar = (ProgressBar) v.findViewById(R.id.file_grid_download_progress_bar);

			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);			
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_grid_text_layout);

			contentText = (TextView) v.findViewById(R.id.content_grid_text);			

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.SEARCH_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
				adapter.setNodes(nodes);
			}
			
			setNodes(nodes);
			contentText.setText(MegaApiUtils.getInfoNode(nodes, (ManagerActivityLollipop)context));
			
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);
			
			return v;
		}
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

	@Override
	public void onAttach(Context context) {
		log("onAttach");
		super.onAttach(context);
		this.context = context;
		aB = ((AppCompatActivity)context).getSupportActionBar();
	}
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){

		}
	}
	
    public void itemClick(int position) {
		log("itemClick: "+position);
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (adapter.isMultipleSelect()){
			log("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
				((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);

			}
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
				
				contentText.setText(MegaApiUtils.getInfoFolder(n, context));
				
				aB.setTitle(n.getName());
				log("aB.setHomeAsUpIndicator_51");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			
				parentHandle = nodes.get(position).getHandle();
				((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
				adapter.setParentHandle(parentHandle);
				nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
				adapter.setNodes(nodes);
				recyclerView.scrollToPosition(0);
				
				levels++;
				
				//If folder has no files
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					contentText.setVisibility(View.GONE);
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
					contentText.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}

				((ManagerActivityLollipop) context).showFabButton();
			}
			else{
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					intent.putExtra("searchQuery", searchQuery);
					intent.putExtra("adapterType", Constants.SEARCH_ADAPTER);
					if (parentHandle == -1){
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
			  		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
			  		if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
			  			context.startActivity(mediaIntent);
			  		}
			  		else{
			  			Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
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
		// actionMode.
	}
	
	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		adapter.setMultipleSelect(false);
		((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_TRANSPARENT_BLACK);

		if (actionMode != null) {
			actionMode.finish();
		}
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
	
	public boolean showSelectMenuItem(){

		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}
	
	public int onBackPressed(){
		log("onBackPressed");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		
		parentHandle = adapter.getParentHandle();
		((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);

		if (levels > 0){
			log("levels > 0");
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
			if (parentNode != null){
				contentText.setText(MegaApiUtils.getInfoFolder(parentNode, context));
				recyclerView.setVisibility(View.VISIBLE);
				contentText.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
					aB.setTitle(getString(R.string.section_cloud_drive));
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					log("aB.setHomeAsUpIndicator_52");
					((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				}
				else{
					aB.setTitle(parentNode.getName());
					log("aB.setHomeAsUpIndicator_53");
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				}

				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

				parentHandle = parentNode.getHandle();
				((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				if(nodes!=null){
					log("nodes.size: "+nodes.size());
					if(nodes.size()>0){
						recyclerView.setVisibility(View.VISIBLE);
						contentText.setVisibility(View.VISIBLE);
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);
					}
					else{
						recyclerView.setVisibility(View.GONE);
						contentText.setVisibility(View.GONE);
						emptyImageView.setVisibility(View.VISIBLE);
						emptyTextView.setVisibility(View.VISIBLE);
					}
				}
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
				levels--;
				((ManagerActivityLollipop) context).showFabButton();
				return 2;
			}
			else{
				((ManagerActivityLollipop) context).showFabButton();
				return 0;
			}
		}
		else if (levels == -1){
			log("levels == -1");
			((ManagerActivityLollipop) context).showFabButton();
			return 0;
		}
		else{
			log("levels searchQuery: "+levels);
			parentHandle = -1;
			((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
			nodes = megaApi.search(searchQuery);
			adapter.setNodes(nodes);
			setNodes(nodes);
			contentText.setText(MegaApiUtils.getInfoNode(nodes, (ManagerActivityLollipop)context));
			if(nodes!=null){
				log("nodes.size: "+nodes.size());
				if(nodes.size()>0){
					contentText.setVisibility(View.VISIBLE);
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
				else{
					contentText.setVisibility(View.GONE);
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
				}
			}
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
			levels--;
			aB.setTitle(getString(R.string.action_search)+": "+searchQuery);
			log("aB.setHomeAsUpIndicator_54");
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			((ManagerActivityLollipop) context).showFabButton();
			return 3;
		}
	}

	
	public long getParentHandle(){
		return adapter.getParentHandle();

	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
	}

	public void refresh(){
		log("refresh");
		if(parentHandle==-1){
			nodes = megaApi.search(searchQuery);
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode!=null){
				log("parentNode: "+parentNode.getName());
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				contentText.setText(MegaApiUtils.getInfoFolder(parentNode, context));
			}
		}
		setNodes(nodes);

		if(adapter != null){
			adapter.notifyDataSetChanged();
		}
	}
	
	public int getLevels(){
		return levels;
	}
	
	public void setLevels(int levels){
		this.levels = levels;
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
	}
	
	public void setSearchQuery(String searchQuery){
		this.searchQuery = searchQuery;
	}

	public ArrayList<MegaNode> getNodes(){
		return nodes;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		log("setNodes");
		this.nodes = nodes;
		if (adapter != null){
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
				log("no results");
				recyclerView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if(parentHandle==-1){
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.no_results_found);
				}
				else if (megaApi.getRootNode().getHandle()==parentHandle) {
					emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
				}
				else {
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
			}
			else{
				contentTextLayout.setVisibility(View.VISIBLE);
				contentText.setText(MegaApiUtils.getInfoNode(nodes, context));
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}			
		}
	}

	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	private static void log(String log) {
		Util.log("SearchFragmentLollipop", log);
	}

}
