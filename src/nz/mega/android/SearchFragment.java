package nz.mega.android;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class SearchFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener{

	Context context;
	ActionBar aB;
	ListView listView;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserListAdapter adapterList;
	LinearLayout buttonsLayout;
	LinearLayout outSpaceLayout=null;
	SearchFragment searchFragment = this;
	TextView contentText;
	
	MegaApiAndroid megaApi;
		
	long parentHandle = -1;
	int levels = -1;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	ArrayList<MegaNode> nodes;
	ArrayList<MegaNode> searchNodes;
	String searchQuery = null;
		
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
			listView.setOnItemLongClickListener(searchFragment);
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
					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
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
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		if (megaApi.getRootNode() == null){
			return null;
		}
		
		if (parentHandle == -1){
			nodes = megaApi.search(megaApi.getRootNode(), searchQuery, true);
			searchNodes = megaApi.search(megaApi.getRootNode(), searchQuery, true);
			
			if(((ManagerActivity)context).getmDrawerToggle() != null)
			{
				aB.setTitle(getString(R.string.action_search)+": "+searchQuery);
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
				((ManagerActivity)context).supportInvalidateOptionsMenu();
			}
		}
		else{
			MegaNode n = megaApi.getNodeByHandle(parentHandle);
						
			if(((ManagerActivity)context).getmDrawerToggle() != null)
			{
				aB.setTitle(n.getName());
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
				((ManagerActivity)context).supportInvalidateOptionsMenu();
			}
			
			((ManagerActivity)context).setParentHandleSearch(parentHandle);
			nodes = megaApi.getChildren(n, orderGetChildren);
		}
		
		View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
		
		listView = (ListView) v.findViewById(R.id.file_list_view_browser);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_layout);
		buttonsLayout.setVisibility(View.GONE);
		
		outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space);
		outSpaceLayout.setVisibility(View.GONE);
				
		listView.setItemsCanFocus(false);
		contentText = (TextView) v.findViewById(R.id.content_text);
		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
		
		if (adapterList == null){
			adapterList = new MegaBrowserListAdapter(context, nodes, parentHandle, listView, aB, ManagerActivity.SEARCH_ADAPTER);
		}
		else{
			adapterList.setParentHandle(parentHandle);
			adapterList.setNodes(nodes);
		}
		
		adapterList.setPositionClicked(-1);
		adapterList.setMultipleSelect(false);

		listView.setAdapter(adapterList);
		
		setNodes(nodes);
		
		contentText.setText(getInfoNode());
		
		
		return v;
	}
	
	private String getInfoNode() {
		int numFolders = nodes.size();
		
		String info = "";
		if (numFolders > 0) {
			info = numFolders
					+ " "
					+ context.getResources().getQuantityString(
							R.plurals.general_num_folders, numFolders);
			
		} 
		return info;			
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
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
		
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
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);
				
				aB.setTitle(n.getName());
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
				((ManagerActivity)context).supportInvalidateOptionsMenu();
				
				parentHandle = nodes.get(position).getHandle();
				((ManagerActivity)context).setParentHandleSearch(parentHandle);
				adapterList.setParentHandle(parentHandle);
				nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
				adapterList.setNodes(nodes);
				listView.setSelection(0);
				
				levels++;
				
				//If folder has no files
				if (adapterList.getCount() == 0){
					listView.setVisibility(View.GONE);
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
					listView.setVisibility(View.VISIBLE);
					contentText.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
			}
			else{
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewer.class);
					intent.putExtra("position", position);
					intent.putExtra("searchQuery", searchQuery);
					intent.putExtra("adapterType", ManagerActivity.SEARCH_ADAPTER);
					if (parentHandle == -1){
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
			  		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
			  		if (ManagerActivity.isIntentAvailable(context, mediaIntent)){
			  			context.startActivity(mediaIntent);
			  		}
			  		else{
			  			Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
			  			adapterList.setPositionClicked(-1);
						adapterList.notifyDataSetChanged();
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						((ManagerActivity) context).onFileClick(handleList);
			  		}						
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
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		
		parentHandle = adapterList.getParentHandle();
		((ManagerActivity)context).setParentHandleSearch(parentHandle);
		
		if (adapterList.isMultipleSelect()){
			hideMultipleSelect();
			return 3;
		}
		
		if (adapterList.getPositionClicked() != -1){
			adapterList.setPositionClicked(-1);
			adapterList.notifyDataSetChanged();
			return 1;
		}
		else{
			if (levels > 0){
				
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
						aB.setTitle(getString(R.string.section_cloud_drive));	
						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
					}
					else{
						aB.setTitle(parentNode.getName());					
						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
					}
					
					((ManagerActivity)context).supportInvalidateOptionsMenu();
					
					parentHandle = parentNode.getHandle();
					((ManagerActivity)context).setParentHandleSearch(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					adapterList.setNodes(nodes);
					listView.setSelection(0);
					adapterList.setParentHandle(parentHandle);
					levels--;
					return 2;
				}
				else{
					return 0;
				}
			}
			else if (levels == -1){
				return 0;
			}
			else{
				parentHandle = -1;
				((ManagerActivity)context).setParentHandleSearch(parentHandle);
				nodes = megaApi.search(megaApi.getRootNode(), searchQuery, true);
				adapterList.setNodes(nodes);
				listView.setSelection(0);
				adapterList.setParentHandle(parentHandle);
				levels--;
				aB.setTitle(getString(R.string.action_search)+": "+searchQuery);	
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
				((ManagerActivity)context).supportInvalidateOptionsMenu();
				return 3;
			}
		}
	}
	
	public long getParentHandle(){
		return adapterList.getParentHandle();

	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapterList != null){
			adapterList.setParentHandle(parentHandle);
		}
	}
	
	public int getLevels(){
		return levels;
	}
	
	public void setLevels(int levels){
		this.levels = levels;
	}
	
	public ListView getListView(){
		return listView;
	}
	
	public void setSearchQuery(String searchQuery){
		this.searchQuery = searchQuery;
	}
	
	public void setSearchNodes (ArrayList<MegaNode> searchNodes){
		this.searchNodes = searchNodes;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		
		if (adapterList != null){
			adapterList.setNodes(nodes);
			if (adapterList.getCount() == 0){
				listView.setVisibility(View.GONE);
				contentText.setVisibility(View.GONE);
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
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}			
		}
	}
	
	public void setPositionClicked(int positionClicked){
		if (adapterList != null){
			adapterList.setPositionClicked(positionClicked);
		}	
	}
	
	public void notifyDataSetChanged(){
		if (adapterList != null){
			adapterList.notifyDataSetChanged();
		}
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
		
		if (adapterList != null){
			adapterList.setOrder(orderGetChildren);
		}
	}
	
	private static void log(String log) {
		Util.log("SearchFragment", log);
	}
}
