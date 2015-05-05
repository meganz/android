package nz.mega.android;

import java.util.ArrayList;
import java.util.List;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class InboxFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener{

	public static int GRID_WIDTH =400;
	
	Context context;
	ActionBar aB;
	ListView listView;
	MegaBrowserListAdapter adapterList;
	MegaBrowserNewGridAdapter adapterGrid;
	public InboxFragment inboxFragment = this;
	MegaNode inboxNode;
	boolean isList = true;
	long parentHandle = -1;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	ArrayList<MegaNode> nodes;
	
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	
	MegaApiAndroid megaApi;
	
	private ActionMode actionMode;
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = getSelectedDocuments();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivity) context).onFileClick(handleList);
					break;
				}
				case R.id.cab_menu_rename:{
					clearSelections();
					hideMultipleSelect();
					if (documents.size()==1){
						((ManagerActivity) context).showRenameDialog(documents.get(0), documents.get(0).getName());
					}
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivity) context).showCopy(handleList);
					break;
				}	
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivity) context).showMove(handleList);
					break;
				}
				case R.id.cab_menu_share_link:{
					clearSelections();
					hideMultipleSelect();
					if (documents.size()==1){
						((ManagerActivity) context).getPublicLinkAndShareIt(documents.get(0));
					}
					break;
				}
				case R.id.cab_menu_trash:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivity) context).moveToTrash(handleList);
					break;
				}
				case R.id.cab_menu_select_all:{
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
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			adapterList.setMultipleSelect(false);
			listView.setOnItemLongClickListener(inboxFragment);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = getSelectedDocuments();
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			
			// Rename
			if((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)) {
				showRename = true;
			}
			
			// Link
			if ((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {
				showLink = true;
			}
			
			if (selected.size() > 0) {
				showDownload = true;
				showTrash = true;
				showMove = true;
				showCopy = true;
				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getInboxNode()).getErrorCode() != MegaError.API_OK)	{
						showTrash = false;
						showMove = false;
						break;
					}
				}
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			if (showTrash){
				menu.findItem(R.id.cab_menu_trash).setTitle(context.getString(R.string.context_remove));
			}
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			
			return false;
		}
		
	}
	
	public boolean showSelectMenuItem(){
		if (isList){
			if (adapterList != null){
				return adapterList.isMultipleSelect();
			}
		}
		else{
			if (adapterGrid != null){
				return adapterGrid.isMultipleSelect();
			}
		}
		
		return false;
	}
	
	public void selectAll(){
		if (isList){
			actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
	
			adapterList.setMultipleSelect(true);
			for ( int i=0; i< adapterList.getCount(); i++ ) {
				listView.setItemChecked(i, true);
			}
			updateActionModeTitle();
			listView.setOnItemLongClickListener(null);
		}
		else{
			if (adapterGrid != null){
				adapterGrid.selectAll();
			}
		}
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		log("onCreateView");
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		

		parentHandle = megaApi.getInboxNode().getHandle();
		inboxNode = megaApi.getInboxNode();		
//		((ManagerActivity)context).setParentHandleRubbish(parentHandle);
		nodes = megaApi.getChildren(inboxNode, orderGetChildren);
		
		if(((ManagerActivity)context).getmDrawerToggle() != null)
		{
			aB.setTitle(getString(R.string.section_inbox));	
			((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
			((ManagerActivity)context).supportInvalidateOptionsMenu();
		}

		if (isList){
			View v = inflater.inflate(R.layout.fragment_inboxlist, container, false);
	        
	        listView = (ListView) v.findViewById(R.id.inbox_list_view);
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			listView.setItemsCanFocus(false);
			
			emptyImageView = (ImageView) v.findViewById(R.id.inbox_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.inbox_list_empty_text);
			emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
			emptyTextView.setText(R.string.file_browser_empty_folder);
			contentText = (TextView) v.findViewById(R.id.inbox_list_content_text);
			if (adapterList == null){
				adapterList = new MegaBrowserListAdapter(context, nodes, parentHandle, listView, aB, ManagerActivity.INBOX_ADAPTER);
			}
			else{
				adapterList.setParentHandle(parentHandle);
				adapterList.setNodes(nodes);
			}			

			adapterList.setPositionClicked(-1);
			adapterList.setMultipleSelect(false);

			listView.setAdapter(adapterList);
			contentText.setText(getInfoFolder(inboxNode));
			setNodes(nodes);
			
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_inboxgrid, container, false);
			
			Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
			DisplayMetrics outMetrics = new DisplayMetrics ();
		    display.getMetrics(outMetrics);
		    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
			
		    float scaleW = Util.getScaleW(outMetrics, density);
		    float scaleH = Util.getScaleH(outMetrics, density);
		    
		    int totalWidth = outMetrics.widthPixels;
		    int totalHeight = outMetrics.heightPixels;
		    
		    int numberOfCells = totalWidth / GRID_WIDTH;
		    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
		    	if (numberOfCells < 3){
					numberOfCells = 3;
				}	
		    }
		    else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
		    	if (numberOfCells < 2){
					numberOfCells = 2;
				}	
		    }
				        
	        listView = (ListView) v.findViewById(R.id.inbox_grid_view);
	        listView.setOnItemClickListener(null);
	        listView.setItemsCanFocus(false);

	        emptyImageView = (ImageView) v.findViewById(R.id.inbox_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.inbox_grid_empty_text);
			emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
			emptyTextView.setText(R.string.file_browser_empty_folder);
			contentText = (TextView) v.findViewById(R.id.inbox_content_grid_text);
			
			if (adapterGrid == null){
				adapterGrid = new MegaBrowserNewGridAdapter(context, nodes, parentHandle, listView, aB, numberOfCells, ManagerActivity.INBOX_ADAPTER, orderGetChildren, emptyImageView, emptyTextView, null, null, contentText);
			}
			else{
				adapterGrid.setParentHandle(parentHandle);
				adapterGrid.setNodes(nodes);
			}			
			
			adapterGrid.setPositionClicked(-1);
			listView.setAdapter(adapterGrid);
			contentText.setText(getInfoFolder(inboxNode));
			setNodes(nodes);
			
			return v;	
		}
	}
	
	public void refresh(){
		log("refresh");
		nodes = megaApi.getChildren(inboxNode, orderGetChildren);
		setNodes(nodes);
		if (isList){
			if(adapterList!=null){				
				adapterList.notifyDataSetChanged();
			}
		}
		else{
			if(adapterGrid!=null){
				adapterGrid.notifyDataSetChanged();
			}
		}
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){

		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (adapterList.getPositionClicked() == -1){
			clearSelections();
			actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
			listView.setItemChecked(position, true);
			adapterList.setMultipleSelect(true);
			updateActionModeTitle();
			listView.setOnItemLongClickListener(null);
		}
		return true;
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
		
		if (isList){
			if (adapterList.isMultipleSelect()){
				SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
				if (checkedItems.get(position, false) == true){
					listView.setItemChecked(position, true);
				}
				else{
					listView.setItemChecked(position, false);
				}				
				updateActionModeTitle();
				adapterList.notifyDataSetChanged();
			}
			else{

				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewer.class);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", ManagerActivity.RUBBISH_BIN_ADAPTER);
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_RUBBISH){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}
					intent.putExtra("orderGetChildren", orderGetChildren);
					startActivity(intent);
				}
				else{
					adapterList.setPositionClicked(-1);
					adapterList.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					((ManagerActivity) context).onFileClick(handleList);
				}
				
			}
		}
    }
	
	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaNode> documents = getSelectedDocuments();
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
			title = "";
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
	 * Get list of all selected documents
	 */
	private List<MegaNode> getSelectedDocuments() {
		ArrayList<MegaNode> documents = new ArrayList<MegaNode>();
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				MegaNode document = adapterList.getDocumentAt(checkedItems.keyAt(i));
				if (document != null){
					documents.add(document);
				}
			}
		}
		return documents;
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				listView.setItemChecked(checkedPosition, false);
			}
		}
		updateActionModeTitle();
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		
		if (isList){
			
			if (adapterList == null){
				return 0;
			}
			
			if (adapterList.isMultipleSelect()){
				hideMultipleSelect();
				return 2;
			}
			
			if (adapterList.getPositionClicked() != -1){
				adapterList.setPositionClicked(-1);
				adapterList.notifyDataSetChanged();
				return 1;
			}
			else{
					return 0;
			}
		}
		else{
			
			if (adapterGrid == null){
				return 0;
			}
			
			if (adapterGrid.isMultipleSelect()){
				adapterGrid.hideMultipleSelect();
				return 2;
			}
			
			if (adapterGrid.getPositionClicked() != -1){
				adapterGrid.setPositionClicked(-1);
				adapterGrid.notifyDataSetChanged();
				return 1;
			}
			else{				
				return 0;				
			}
		}
	}
	
	private String getInfoFolder(MegaNode n) {
		int numFiles = megaApi.getNumChildFiles(n);

		String info = "";
		
		info = numFiles
				+ " "
				+ context.getResources().getQuantityString(
						R.plurals.general_num_files, numFiles);
		

		return info;
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	public long getParentHandle(){
		if (isList){
			return adapterList.getParentHandle();
		}
		else{
			return adapterGrid.getParentHandle();
		}
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (isList){
			if (adapterGrid != null){
				adapterGrid.setParentHandle(parentHandle);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setParentHandle(parentHandle);
			}
		}
	}
	
	public ListView getListView(){
		return listView;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if (isList){
			if (adapterList != null){
				adapterList.setNodes(nodes);
				if (adapterList.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);					
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}	
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setNodes(nodes);
				if (adapterGrid.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}
		}
	}
	
	public void setPositionClicked(int positionClicked){
		if (isList){
			if (adapterList != null){
				adapterList.setPositionClicked(positionClicked);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setPositionClicked(positionClicked);
			}	
		}		
	}
	
	public void notifyDataSetChanged(){
		if (isList){
			if (adapterList != null){
				adapterList.notifyDataSetChanged();
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.notifyDataSetChanged();
			}
		}
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
		if (isList){
			if (adapterList != null){
				adapterList.setOrder(orderGetChildren);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setOrder(orderGetChildren);
			}
		}
	}
	
	private static void log(String log) {
		Util.log("ContactsFragment", log);
	}
}
