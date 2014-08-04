package com.mega.android;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaShare;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.format.DateUtils;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PhotoSyncFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener, MegaRequestListenerInterface{

	Context context;
	ActionBar aB;
	ListView listView;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaPhotoSyncListAdapter adapterList;
	MegaBrowserGridAdapter adapterGrid;
	PhotoSyncFragment fileBrowserFragment = this;
	
	MegaApiAndroid megaApi;
		
//	long parentHandle = -1;
	boolean isList = true;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	NodeList nodes;
	ArrayList<PhotoSyncHolder> nodesArray = new ArrayList<PhotoSyncFragment.PhotoSyncHolder>();
	
	private ActionMode actionMode;
	
	ProgressDialog statusDialog;
	long photosyncHandle = -1;
	
	public class PhotoSyncHolder{
		boolean isNode;
		long handle;
		String monthYear;
	}
	
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
			listView.setOnItemLongClickListener(fileBrowserFragment);
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
		
		aB.setTitle(getString(R.string.section_photo_sync));	
		((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
		((ManagerActivity)context).supportInvalidateOptionsMenu();
		
		View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
		
		listView = (ListView) v.findViewById(R.id.file_list_view_browser);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
		
		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
		
		emptyImageView.setImageResource(R.drawable.ic_empty_folder);
		emptyTextView.setText(R.string.file_browser_empty_folder);
		
		emptyImageView.setVisibility(View.VISIBLE);
		emptyTextView.setVisibility(View.VISIBLE);
		listView.setVisibility(View.GONE);
		
		DatabaseHandler dbH = new DatabaseHandler(context);
		Preferences prefs = dbH.getPreferences();
		
		
		if (prefs == null){
			photosyncHandle = -1;
		}
		else{
			//The "PhotoSync" folder exists?
			if (prefs.getCamSyncHandle() == null){
				photosyncHandle = -1;
			}
			else{
				photosyncHandle = Long.parseLong(prefs.getCamSyncHandle());
				if (megaApi.getNodeByHandle(photosyncHandle) == null){
					photosyncHandle = -1;
				}
				else{
					if (megaApi.getParentNode(megaApi.getNodeByHandle(photosyncHandle)).getHandle() != megaApi.getRootNode().getHandle()){
						photosyncHandle = -1;
					}
				}
			}
		}
		
		if (photosyncHandle == -1){
			NodeList nl = megaApi.getChildren(megaApi.getRootNode());
			for (int i=0;i<nl.size();i++){
				if ((CameraSyncService.PHOTO_SYNC.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
					photosyncHandle = nl.get(i).getHandle();
					dbH.setCamSyncHandle(photosyncHandle);
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					break;
				}
			}
			
			if (photosyncHandle == -1){
				log("must create the folder");
				statusDialog = null;
				try {
					statusDialog = new ProgressDialog(context);
					statusDialog.setMessage(getString(R.string.context_creating_folder));
					statusDialog.show();
				}
				catch(Exception e){}
				
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
				
				megaApi.createFolder(CameraSyncService.PHOTO_SYNC, megaApi.getRootNode(), this);
			}
		}
		else{
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			nodes = megaApi.getChildren(megaApi.getNodeByHandle(photosyncHandle), MegaApiJava.ORDER_MODIFICATION_DESC);
			int month = 0;
			int year = 0;
			for (int i=0;i<nodes.size();i++){
				PhotoSyncHolder psh = new PhotoSyncHolder();
				Date d = new Date(nodes.get(i).getModificationTime()*1000);
				if ((month == d.getMonth()) && (year == d.getYear())){
					psh.isNode = true;
					psh.handle = nodes.get(i).getHandle();
					nodesArray.add(psh);
				}
				else{
					month = d.getMonth();
					year = d.getYear();
					psh.isNode = false;
					psh.monthYear = getImageDateString(month, year);
					nodesArray.add(psh);
					psh = new PhotoSyncHolder();
					psh.isNode = true;
					psh.handle = nodes.get(i).getHandle();
					nodesArray.add(psh);
					log("MONTH: " + d.getMonth() + "YEAR: " + d.getYear());
				}
			}			
			
			if (adapterList == null){
				adapterList = new MegaPhotoSyncListAdapter(context, nodesArray, photosyncHandle, listView, emptyImageView, emptyTextView, aB);
			}
			else{
				adapterList.setNodes(nodesArray);
			}
			
			adapterList.setPositionClicked(-1);
			adapterList.setMultipleSelect(false);

			listView.setAdapter(adapterList);
		}
		
		return v;
		
		
		
		
		
		
		
		
//		if (parentHandle == -1){
//			parentHandle = megaApi.getRootNode().getHandle();
//			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//			nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
//			aB.setTitle(getString(R.string.section_cloud_drive));	
//			((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//			((ManagerActivity)context).supportInvalidateOptionsMenu();
//		}
//		else{
//			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
//			nodes = megaApi.getChildren(parentNode, orderGetChildren);
//			
//			if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
//				aB.setTitle(getString(R.string.section_cloud_drive));	
//				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//			}
//			else{
//				aB.setTitle(parentNode.getName());					
//				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//			}
//			((ManagerActivity)context).supportInvalidateOptionsMenu();
//		}	
//		
//		if (isList){
//			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
//	        
//	        listView = (ListView) v.findViewById(R.id.file_list_view_browser);
//			listView.setOnItemClickListener(this);
//			listView.setOnItemLongClickListener(this);
//			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
//			listView.setItemsCanFocus(false);
//			
//			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
//			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
//			
//			if (adapterList == null){
//				adapterList = new MegaBrowserListAdapter(context, nodes, parentHandle, listView, emptyImageView, emptyTextView, aB, ManagerActivity.FILE_BROWSER_ADAPTER);
//			}
//			else{
//				adapterList.setParentHandle(parentHandle);
//				adapterList.setNodes(nodes);
//			}
//			
//			if (parentHandle == megaApi.getRootNode().getHandle()){
//				aB.setTitle(getString(R.string.section_cloud_drive));
//			}
//			else{
//				aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
//			}
//			
//			adapterList.setPositionClicked(-1);
//			adapterList.setMultipleSelect(false);
//
//			listView.setAdapter(adapterList);
//			
//			setNodes(nodes);
//			
//			return v;
//		}
//		else{
//			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
//			
//			listView = (ListView) v.findViewById(R.id.file_grid_view_browser);
//			listView.setOnItemClickListener(null);
//			listView.setItemsCanFocus(false);
//	        
//	        emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
//			emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);
//	        
//			if (adapterGrid == null){
//				adapterGrid = new MegaBrowserGridAdapter(context, nodes, parentHandle, listView, emptyImageView, emptyTextView, aB, ManagerActivity.FILE_BROWSER_ADAPTER);
//			}
//			else{
//				adapterGrid.setParentHandle(parentHandle);
//				adapterGrid.setNodes(nodes);
//			}
//			
//			if (parentHandle == megaApi.getRootNode().getHandle()){
//				aB.setTitle(getString(R.string.section_cloud_drive));
//			}
//			else{
//				aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
//			}
//			
//			adapterGrid.setPositionClicked(-1);
//			
//			listView.setAdapter(adapterGrid);
//			
//			setNodes(nodes);
//			
//			return v;
//		}		
	}
	
	public String getImageDateString(int month, int year){
		String ret = "";
		year = year + 1900;
	
		switch(month){
			case 0:{
				ret = context.getString(R.string.january) + " " + year;
				break;
			}
			case 1:{
				ret = context.getString(R.string.february) + " " + year;
				break;
			}
			case 2:{
				ret = context.getString(R.string.march) + " " + year;
				break;
			}
			case 3:{
				ret = context.getString(R.string.april) + " " + year;
				break;
			}
			case 4:{
				ret = context.getString(R.string.may) + " " + year;
				break;
			}
			case 5:{
				ret = context.getString(R.string.june) + " " + year;
				break;
			}
			case 6:{
				ret = context.getString(R.string.july) + " " + year;
				break;
			}
			case 7:{
				ret = context.getString(R.string.august) + " " + year;
				break;
			}
			case 8:{
				ret = context.getString(R.string.september) + " " + year;
				break;
			}
			case 9:{
				ret = context.getString(R.string.october) + " " + year;
				break;
			}
			case 10:{
				ret = context.getString(R.string.november) + " " + year;
				break;
			}
			case 11:{
				ret = context.getString(R.string.december) + " " + year;
				break;
			}
		}
		return ret;
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
				if (nodes.get(position).isFolder()){
//					MegaNode n = nodes.get(position);
//					
//					aB.setTitle(n.getName());
//					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					((ManagerActivity)context).supportInvalidateOptionsMenu();
//					
//					parentHandle = nodes.get(position).getHandle();
//					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//					adapterList.setParentHandle(parentHandle);
//					nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
//					adapterList.setNodes(nodes);
//					listView.setSelection(0);
//					
//					//If folder has no files
//					if (adapterList.getCount() == 0){
//						listView.setVisibility(View.GONE);
//						emptyImageView.setVisibility(View.VISIBLE);
//						emptyTextView.setVisibility(View.VISIBLE);
//						if (megaApi.getRootNode().getHandle()==n.getHandle()) {
//							emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
//							emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
//						} else {
//							emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//							emptyTextView.setText(R.string.file_browser_empty_folder);
//						}
//					}
//					else{
//						listView.setVisibility(View.VISIBLE);
//						emptyImageView.setVisibility(View.GONE);
//						emptyTextView.setVisibility(View.GONE);
//					}
				}
				else{
					if (MimeType.typeForName(nodes.get(position).getName()).isImage()){
						Intent intent = new Intent(context, FullScreenImageViewer.class);
						intent.putExtra("position", position);
						intent.putExtra("adapterType", ManagerActivity.FILE_BROWSER_ADAPTER);
						if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT){
							intent.putExtra("parentNodeHandle", -1L);
						}
						else{
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
						}
						intent.putExtra("orderGetChildren", orderGetChildren);
						startActivity(intent);
					}
					else if (MimeType.typeForName(nodes.get(position).getName()).isVideo() || MimeType.typeForName(nodes.get(position).getName()).isAudio() ){
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
				  		String mimeType = MimeType.typeForName(file.getName()).getType();
				  		System.out.println("FILENAME: " + fileName);
				  		
				  		Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
				  		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
				  		try
				  		{
				  			startActivity(mediaIntent);
				  		}
				  		catch(Exception e)
				  		{
				  			Toast.makeText(context, "NOOOOOOOO", Toast.LENGTH_LONG).show();
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
//		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
//		for (int i = 0; i < checkedItems.size(); i++) {
//			if (checkedItems.valueAt(i) == true) {
//				
//				MegaNode document = adapterList.getDocumentAt(checkedItems.keyAt(i));
//				if (document != null){
//					documents.add(document);
//				}
//			}
//		}
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
		
		return 0;

//		if (isList){
//			parentHandle = adapterList.getParentHandle();
//			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//			
//			if (adapterList.getPositionClicked() != -1){
//				adapterList.setPositionClicked(-1);
//				adapterList.notifyDataSetChanged();
//				return 1;
//			}
//			else{
//				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
//				if (parentNode != null){
//					listView.setVisibility(View.VISIBLE);
//					emptyImageView.setVisibility(View.GONE);
//					emptyTextView.setVisibility(View.GONE);
//					if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
//						aB.setTitle(getString(R.string.section_cloud_drive));	
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//					}
//					else{
//						aB.setTitle(parentNode.getName());					
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					}
//					
//					((ManagerActivity)context).supportInvalidateOptionsMenu();
//					
//					parentHandle = parentNode.getHandle();
//					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//					nodes = megaApi.getChildren(parentNode, orderGetChildren);
//					adapterList.setNodes(nodes);
//					listView.setSelection(0);
//					adapterList.setParentHandle(parentHandle);
//					return 2;
//				}
//				else{
//					return 0;
//				}
//			}
//		}
//		else{
//			parentHandle = adapterGrid.getParentHandle();
//			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//			
//			if (adapterGrid.getPositionClicked() != -1){
//				adapterGrid.setPositionClicked(-1);
//				adapterGrid.notifyDataSetChanged();
//				return 1;
//			}
//			else{
//				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
//				if (parentNode != null){
//					listView.setVisibility(View.VISIBLE);
//					emptyImageView.setVisibility(View.GONE);
//					emptyTextView.setVisibility(View.GONE);
//					if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
//						aB.setTitle(getString(R.string.section_cloud_drive));	
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//					}
//					else{
//						aB.setTitle(parentNode.getName());					
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					}
//					
//					((ManagerActivity)context).supportInvalidateOptionsMenu();
//					
//					parentHandle = parentNode.getHandle();
//					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//					nodes = megaApi.getChildren(parentNode, orderGetChildren);
//					adapterGrid.setNodes(nodes);
//					listView.setSelection(0);
//					adapterGrid.setParentHandle(parentHandle);
//					return 2;
//				}
//				else{
//					return 0;
//				}
//			}
//		}
	}
	
	public long getPhotoSyncHandle(){
		if (isList){
			if (adapterList != null){
				return adapterList.getPhotoSyncHandle();
			}
			else{
				return -1;
			}
		}
		else{
			return adapterGrid.getParentHandle();
		}
	}
	
	
	public ListView getListView(){
		return listView;
	}
	
	public void setNodes(NodeList nodes){
		this.nodes = nodes;
		this.nodesArray.clear();
		int month = 0;
		int year = 0;
		for (int i=0;i<nodes.size();i++){
			PhotoSyncHolder psh = new PhotoSyncHolder();
			Date d = new Date(nodes.get(i).getModificationTime()*1000);
			if ((month == d.getMonth()) && (year == d.getYear())){
				psh.isNode = true;
				psh.handle = nodes.get(i).getHandle();
				nodesArray.add(psh);
			}
			else{
				month = d.getMonth();
				year = d.getYear();
				psh.isNode = false;
				psh.monthYear = getImageDateString(month, year);
				nodesArray.add(psh);
				psh = new PhotoSyncHolder();
				psh.isNode = true;
				psh.handle = nodes.get(i).getHandle();
				nodesArray.add(psh);
				log("MONTH: " + d.getMonth() + "YEAR: " + d.getYear());
			}
		}
		if (isList){
			if (adapterList != null){
				adapterList.setNodes(nodesArray);
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
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
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
		Util.log("PhotoSyncFragment", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_MKDIR){
			log("create folder start");
		}		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		if (request.getType() == MegaRequest.TYPE_MKDIR){
			log("create folder finished");
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(context, "PhotoSync Folder created", Toast.LENGTH_LONG).show();
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}
}
