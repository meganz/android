package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
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

public class RubbishBinFragmentLollipop extends Fragment {

	public static int GRID_WIDTH =400;
	
	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;
	MegaBrowserLollipopAdapter adapter;
	public RubbishBinFragmentLollipop rubbishBinFragment = this;
		
	boolean isList = true;
	long parentHandle = -1;
	int orderGetChildren;
	
	ArrayList<MegaNode> nodes;
	MegaNode selectedNode = null;
	
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	RelativeLayout contentTextLayout;

	MegaApiAndroid megaApi;
	
	public ActionMode actionMode;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	Stack<Integer> lastPositionStack;

	DatabaseHandler dbH;
	MegaPreferences prefs;
	////

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
			List<MegaNode> documents = adapter.getSelectedNodes();
			
			switch(item.getItemId()){

				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
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
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			showRename = false;
			showLink = false;

			if (selected.size() != 0) {
				showTrash = true;
				showMove = true;
                showCopy = true;

//				for(int i=0; i<selected.size();i++)	{
//					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
//						showTrash = false;
//						showMove = false;
//						break;
//					}
//				}

				if(selected.size()==1){
					showRename=true;
				}
				else{
					showRename=false;
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
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			if(showMove){
				menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);

			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			if (showTrash){
				menu.findItem(R.id.cab_menu_trash).setTitle(context.getString(R.string.context_remove));
			}
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			if(showTrash){
				menu.findItem(R.id.cab_menu_trash).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
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
			
			updateActionModeTitle();
		}
	}

	public static RubbishBinFragmentLollipop newInstance() {
		log("newInstance");
		RubbishBinFragmentLollipop fragment = new RubbishBinFragmentLollipop();
		return fragment;
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		log("onCreate");

		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();

		lastPositionStack = new Stack<>();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
		
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

		orderGetChildren = ((ManagerActivityLollipop)context).getOrderCloud();
		isList = ((ManagerActivityLollipop)context).isList();

		if (parentHandle == -1){

			long parentHandleRubbish = ((ManagerActivityLollipop)context).getParentHandleRubbish();
			if(parentHandleRubbish!=-1){
				log("After consulting... the parentRubbish is: "+parentHandleRubbish);
				parentHandle = parentHandleRubbish;
			}
		}

		if (parentHandle == -1||parentHandle==megaApi.getRubbishNode().getHandle()){
			log("Parent is the Rubbish: "+parentHandle);

			nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

			if (parentNode != null){
				log("The parent node is: "+parentNode.getName());
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
			
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

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
			log("isList View");
			View v = inflater.inflate(R.layout.fragment_rubbishbinlist, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.rubbishbin_list_view);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			
			emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.rubbishbin_list_empty_text);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.rubbishbin_content_text_layout);
			contentText = (TextView) v.findViewById(R.id.rubbishbin_list_content_text);			

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.RUBBISH_BIN_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{


				adapter.setParentHandle(parentHandle);
				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}

			if(megaApi.getRubbishNode()!=null){
				log("setContent of the Rubbish Bin");
				if (parentHandle == megaApi.getRubbishNode().getHandle()||parentHandle==-1){
					contentText.setText(MegaApiUtils.getInfoFolder(megaApi.getRubbishNode(), context));

				}
				else{
					MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
					contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
				}
			}

			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);

			if (adapter.getItemCount() == 0){
				
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (megaApi.getRubbishNode().getHandle()==parentHandle||parentHandle==-1) {
					emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
					emptyTextView.setText(R.string.empty_rubbish_bin);
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
			
			return v;
		}
		else{
			log("isGrid View");
			View v = inflater.inflate(R.layout.fragment_rubbishbingrid, container, false);

			recyclerView = (RecyclerView) v.findViewById(R.id.rubbishbin_grid_view);
			recyclerView.setHasFixedSize(true);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
//			gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//				@Override
//			      public int getSpanSize(int position) {
//					return 1;
//				}
//			});
			
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			
			emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.rubbishbin_grid_empty_text);
			emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
			emptyTextView.setText(R.string.file_browser_empty_folder);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.rubbishbin_grid_content_text_layout);
			contentText = (TextView) v.findViewById(R.id.rubbishbin_grid_content_text);			

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.RUBBISH_BIN_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}

			if(megaApi.getRubbishNode()!=null){
				if (parentHandle == megaApi.getRubbishNode().getHandle()||parentHandle==-1){
					contentText.setText(MegaApiUtils.getInfoFolder(megaApi.getRubbishNode(), context));
				}
				else{
					MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
					contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
				}
			}

			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);
			
			setNodes(nodes);
			
			if (adapter.getItemCount() == 0){
				
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (megaApi.getRubbishNode().getHandle()==parentHandle||parentHandle==-1) {
					emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
					emptyTextView.setText(R.string.empty_rubbish_bin);
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
			return v;
		}
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
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
				
				aB.setTitle(n.getName());
				log("indicator_arrow_back_190");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = nodes.get(position).getHandle();
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
				((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
				adapter.setParentHandle(parentHandle);
				nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
				adapter.setNodes(nodes);
				recyclerView.scrollToPosition(0);
				
				//If folder has no files
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApi.getRubbishNode().getHandle()==parentHandle||parentHandle==-1) {
						emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
						emptyTextView.setText(R.string.empty_rubbish_bin);
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
			else{
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", Constants.RUBBISH_BIN_ADAPTER);
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_RUBBISH){
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
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		parentHandle = adapter.getParentHandle();
		((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
		
		if (adapter == null){
			return 0;
		}

		MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
		if (parentNode != null){
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()){
				aB.setTitle(getString(R.string.section_rubbish_bin));
				log("aB.setHomeAsUpIndicator_47");

				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}
			else{
				aB.setTitle(parentNode.getName());
				log("indicator_arrow_back_191");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
			}

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

			parentHandle = parentNode.getHandle();
			((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
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
			return 2;
		}
		else{
			return 0;
		}
	}
	
	public void setContentText(){
		log("setContentText");
		MegaNode rN = megaApi.getRubbishNode();
		if(rN!=null){
			if (parentHandle == rN.getHandle()||parentHandle==-1){
				contentText.setText(MegaApiUtils.getInfoFolder(rN, context));

			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				if (infoNode !=  null){
					contentText.setText(MegaApiUtils.getInfoFolder(infoNode, context));
				}
				else{
					log("INFO NODE null");
				}
			}
		}
		else{
			log("INFO NODE null");
		}
	}
	
	public void setIsList(boolean isList){
		log("setIsList");
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	public long getParentHandle(){
		return adapter.getParentHandle();
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
	
	public void setNodes(ArrayList<MegaNode> nodes){
		log("setNodes");
		this.nodes = nodes;
		
		if(megaApi.getRubbishNode()==null){
			log("megaApi.getRubbishNode() is NULL");
			return;
		}
		
		if (adapter != null){
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRubbishNode().getHandle()==parentHandle||parentHandle==-1) {
					emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
					emptyTextView.setText(R.string.empty_rubbish_bin);
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

		setContentText();
	}

	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}
	
	public void setOrder(int orderGetChildren){
		log("setOrder:Rubbish");
		this.orderGetChildren = orderGetChildren;
	}

	public boolean isMultipleselect(){
		return adapter.isMultipleSelect();
	}

	public int getItemCount(){
		return adapter.getItemCount();
	}
	
	private static void log(String log) {
		Util.log("RubbishBinFragmentLollipop", log);
	}
}
