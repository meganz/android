package com.mega.android;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaShare;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaUser;

public class FileBrowserFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener{

	public static int GRID_WIDTH =400;
	
	Context context;
	ActionBar aB;
	ListView listView;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserListAdapter adapterList;
	MegaBrowserNewGridAdapter adapterGrid;
	FileBrowserFragment fileBrowserFragment = this;
	LinearLayout buttonsLayout=null;
	Button leftNewFolder;
	Button rightUploadButton;
	TextView contentText;
	RelativeLayout menuOverflowLayout;
	ListView menuOverflowList;
	TextView titleOverflowList;
	
	MegaApiAndroid megaApi;
		
	long parentHandle = -1;
	boolean isList = true;
	boolean overflowMenu = false;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	ArrayList<MegaNode> nodes;
	
	HashMap<Long, MegaTransfer> mTHash = null;
	
	private ActionMode actionMode;
	
	private class ActionBarCallBack implements ActionMode.Callback {
		
		boolean showDownload = false;
		boolean showRename = false;
		boolean showCopy = false;
		boolean showMove = false;
		boolean showLink = false;
		boolean showTrash = false;

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
			listView.setOnItemLongClickListener(fileBrowserFragment);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = getSelectedDocuments();
		
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
				
				if(selected.size()==adapterList.getCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);			
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);	
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
		
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
			parentHandle = megaApi.getRootNode().getHandle();
			((ManagerActivity)context).setParentHandleBrowser(parentHandle);

			nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
			aB.setTitle(getString(R.string.section_cloud_drive));	
			((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
			((ManagerActivity)context).supportInvalidateOptionsMenu();
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			((ManagerActivity)context).setParentHandleBrowser(parentHandle);

			nodes = megaApi.getChildren(parentNode, orderGetChildren);
			
			if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
				aB.setTitle(getString(R.string.section_cloud_drive));	
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
			}
			else{
				aB.setTitle(parentNode.getName());					
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
			}
			((ManagerActivity)context).supportInvalidateOptionsMenu();
		}	
		
				
		if (isList){
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
	        
	        listView = (ListView) v.findViewById(R.id.file_list_view_browser);
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			listView.setItemsCanFocus(false);
			
			//Menu overflow three dots
			menuOverflowLayout = (RelativeLayout) v.findViewById(R.id.file_browser_overflow_menu);
			menuOverflowList = (ListView) v.findViewById(R.id.file_browser_overflow_menu_list);	
			titleOverflowList = (TextView) v.findViewById(R.id.file_browser_overflow_title);	

			if (overflowMenu){
				
				listView.setVisibility(View.GONE);
				String menuOptions[] = new String[4];
				menuOptions[0] = getString(R.string.context_rename);
				menuOptions[1] = getString(R.string.context_move);
				menuOptions[2] = getString(R.string.context_copy);
				menuOptions[3] = getString(R.string.context_send_link);
				
				ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, menuOptions);
//				ArrayAdapter<String> arrayAdapter = new ArrayAdapter
				menuOverflowList.setAdapter(arrayAdapter);
				menuOverflowList.setOnItemClickListener(this);
				menuOverflowLayout.setVisibility(View.VISIBLE);	
				menuOverflowList.setVisibility(View.VISIBLE);	
				titleOverflowList.setVisibility(View.VISIBLE);	
			}
			else{
				menuOverflowLayout.setVisibility(View.GONE);	
				menuOverflowList.setVisibility(View.GONE);
				titleOverflowList.setVisibility(View.GONE);	
			}
					
			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			contentText = (TextView) v.findViewById(R.id.content_text);
			
			buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_layout);
			leftNewFolder = (Button) v.findViewById(R.id.btnLeft_new);
			rightUploadButton = (Button) v.findViewById(R.id.btnRight_upload);			
			
			leftNewFolder.setOnClickListener(this);
			rightUploadButton.setOnClickListener(this);
			
			if (adapterList == null){
				adapterList = new MegaBrowserListAdapter(context, nodes, parentHandle, listView, aB, ManagerActivity.FILE_BROWSER_ADAPTER);
				if (mTHash != null){
					adapterList.setTransfers(mTHash);
				}
			}
			else{
				adapterList.setParentHandle(parentHandle);
				adapterList.setNodes(nodes);
			}
			
			if (parentHandle == megaApi.getRootNode().getHandle()){
				MegaNode infoNode = megaApi.getRootNode();
				contentText.setText(getInfoFolder(infoNode));
				aB.setTitle(getString(R.string.section_cloud_drive));
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				contentText.setText(getInfoFolder(infoNode));
				aB.setTitle(infoNode.getName());
			}						
			
			adapterList.setPositionClicked(-1);
			adapterList.setMultipleSelect(false);

			listView.setAdapter(adapterList);			
			
			setNodes(nodes);
			
			if (adapterList.getCount() == 0){				
				
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				leftNewFolder.setVisibility(View.VISIBLE);
				rightUploadButton.setVisibility(View.VISIBLE);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				leftNewFolder.setVisibility(View.GONE);
				rightUploadButton.setVisibility(View.GONE);
			}					
			
			return v;
		}
		else{
			log("Grid View");
			
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
			
			Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
			DisplayMetrics outMetrics = new DisplayMetrics ();
		    display.getMetrics(outMetrics);
		    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
			
		    float scaleW = Util.getScaleW(outMetrics, density);
		    float scaleH = Util.getScaleH(outMetrics, density);
		    
		    int totalWidth = outMetrics.widthPixels;
		    int totalHeight = outMetrics.heightPixels;
		    
		    int numberOfCells = totalWidth / GRID_WIDTH;
		    if (numberOfCells < 2){
				numberOfCells = 2;
			}
		    
		    listView = (ListView) v.findViewById(R.id.file_grid_view_browser);
			listView.setOnItemClickListener(null);
			listView.setItemsCanFocus(false);
		    
			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);
			contentText = (TextView) v.findViewById(R.id.content_grid_text);
			
			buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_grid_layout);
			leftNewFolder = (Button) v.findViewById(R.id.btnLeft_grid_new);
			rightUploadButton = (Button) v.findViewById(R.id.btnRight_grid_upload);			
			
			leftNewFolder.setOnClickListener(this);
			rightUploadButton.setOnClickListener(this);
			
			
//		    Toast.makeText(context, totalWidth + "x" + totalHeight + "= " + numberOfCells, Toast.LENGTH_LONG).show();
			
		    if (adapterGrid == null){
				adapterGrid = new MegaBrowserNewGridAdapter(context, nodes, parentHandle, listView, aB, numberOfCells, ManagerActivity.FILE_BROWSER_ADAPTER, orderGetChildren, emptyImageView, emptyTextView, leftNewFolder, rightUploadButton, contentText);
				if (mTHash != null){
					adapterGrid.setTransfers(mTHash);
				}
			}
			else{
				adapterGrid.setParentHandle(parentHandle);
				adapterGrid.setNodes(nodes);
			}
		    
			if (parentHandle == megaApi.getRootNode().getHandle()){
				MegaNode infoNode = megaApi.getRootNode();
				contentText.setText(getInfoFolder(infoNode));
				aB.setTitle(getString(R.string.section_cloud_drive));
			}
			else{
				MegaNode infoNode = megaApi.getRootNode();
				contentText.setText(getInfoFolder(infoNode));
				aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
			}
			
			adapterGrid.setPositionClicked(-1);
			
			listView.setAdapter(adapterGrid);
			
			setNodes(nodes);
			
			if (adapterGrid.getCount() == 0){				
				
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				leftNewFolder.setVisibility(View.VISIBLE);
				rightUploadButton.setVisibility(View.VISIBLE);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				contentText.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				leftNewFolder.setVisibility(View.GONE);
				rightUploadButton.setVisibility(View.GONE);
			}				
			
			return v;
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
			case R.id.btnLeft_new:
				((ManagerActivity)getActivity()).showNewFolderDialog(null);				
				break;
				
			case R.id.btnRight_upload:
				((ManagerActivity)getActivity()).uploadFile();
				break;
			case R.id.btnLeft_grid_new:
				((ManagerActivity)getActivity()).showNewFolderDialog(null);				
				break;
				
			case R.id.btnRight_grid_upload:
				((ManagerActivity)getActivity()).uploadFile();
				break;
		}
	}
	
	private String getInfoFolder(MegaNode n) {
		ArrayList<MegaNode> nL = megaApi.getChildren(n);

		int numFolders = 0;
		int numFiles = 0;

		for (int i = 0; i < nL.size(); i++) {
			MegaNode c = nL.get(i);
			if (c.isFolder()) {
				numFolders++;
			} else {
				numFiles++;
			}
		}

		String info = "";
		if (numFolders > 0) {
			info = numFolders
					+ " "
					+ context.getResources().getQuantityString(
							R.plurals.general_num_folders, numFolders);
			if (numFiles > 0) {
				info = info
						+ ", "
						+ numFiles
						+ " "
						+ context.getResources().getQuantityString(
								R.plurals.general_num_files, numFiles);
			}
		} else {
			info = numFiles
					+ " "
					+ context.getResources().getQuantityString(
							R.plurals.general_num_files, numFiles);
		}

		return info;
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		if(overflowMenu){
			overflowMenu=false;
		}
	
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
					MegaNode n = nodes.get(position);
					
					if ((n.getName().compareTo(CameraSyncService.CAMERA_UPLOADS) == 0) && (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT)){
						((ManagerActivity)context).cameraUploadsClicked();
						return;
					}
					
					aB.setTitle(n.getName());
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
					((ManagerActivity)context).supportInvalidateOptionsMenu();
					
					parentHandle = nodes.get(position).getHandle();
					MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
					contentText.setText(getInfoFolder(infoNode));
					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
					adapterList.setParentHandle(parentHandle);
					nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
					adapterList.setNodes(nodes);
					listView.setSelection(0);
					
					//If folder has no files
					if (adapterList.getCount() == 0){
						listView.setVisibility(View.GONE);
						emptyImageView.setVisibility(View.VISIBLE);
						emptyTextView.setVisibility(View.VISIBLE);
						leftNewFolder.setVisibility(View.VISIBLE);
						rightUploadButton.setVisibility(View.VISIBLE);

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
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);
						leftNewFolder.setVisibility(View.GONE);
						rightUploadButton.setVisibility(View.GONE);
					}
				}
				else{
					//Is file
					if (MimeType.typeForName(nodes.get(position).getName()).isImage()){
						Intent intent = new Intent(context, FullScreenImageViewer.class);
						intent.putExtra("position", position);
						intent.putExtra("adapterType", ManagerActivity.FILE_BROWSER_ADAPTER);
						intent.putExtra("isFolderLink", false);
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
		else{
			MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
			contentText.setText(getInfoFolder(infoNode));
			
			if (adapterGrid.getCount() == 0){				

				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				leftNewFolder.setVisibility(View.VISIBLE);
				rightUploadButton.setVisibility(View.VISIBLE);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				leftNewFolder.setVisibility(View.GONE);
				rightUploadButton.setVisibility(View.GONE);
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
	
	public void selectAll(){
		actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());

		adapterList.setMultipleSelect(true);
		for ( int i=0; i< adapterList.getCount(); i++ ) {
			listView.setItemChecked(i, true);
		}
		updateActionModeTitle();
		listView.setOnItemLongClickListener(null);
	}
	
	public void setOverFlowMenu(final MegaNode n){
		log("setOverFlowMenu");
		
		if (overflowMenu){

			//listView.setVisibility(View.GONE);
			String menuOptions[] = new String[5];
			menuOptions[0] = getString(R.string.context_share_folder);
			menuOptions[1] = getString(R.string.context_rename);
			menuOptions[2] = getString(R.string.context_move);
			menuOptions[3] = getString(R.string.context_copy);
			menuOptions[4] = getString(R.string.context_send_link);
			
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, menuOptions);
			menuOverflowList.setAdapter(arrayAdapter);
			menuOverflowList.setOnItemClickListener(
					new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
							log("onItemClick");
							log("id: "+v.getId());
							switch (position) {	
								case 0: {
									//Rename
									setPositionClicked(-1);
									notifyDataSetChanged();
									menuOverflowLayout.setVisibility(View.GONE);	
									menuOverflowList.setVisibility(View.GONE);	
									titleOverflowList.setVisibility(View.GONE);										
									((ManagerActivity) context).shareFolder(n);
									break;
								}
								case 1: {
									//Rename
									setPositionClicked(-1);
									notifyDataSetChanged();
									menuOverflowLayout.setVisibility(View.GONE);	
									menuOverflowList.setVisibility(View.GONE);	
									titleOverflowList.setVisibility(View.GONE);	
									((ManagerActivity) context).showRenameDialog(n, n.getName());	
									break;
								}
								case 2: {
									//Move
									setPositionClicked(-1);
									notifyDataSetChanged();
									menuOverflowLayout.setVisibility(View.GONE);	
									menuOverflowList.setVisibility(View.GONE);
									titleOverflowList.setVisibility(View.GONE);		
									ArrayList<Long> handleList = new ArrayList<Long>();
									handleList.add(n.getHandle());									
									((ManagerActivity) context).showMove(handleList);									
									break;
									
								}
								case 3: {
									//Copy
									setPositionClicked(-1);
									notifyDataSetChanged();
									menuOverflowLayout.setVisibility(View.GONE);	
									menuOverflowList.setVisibility(View.GONE);
									titleOverflowList.setVisibility(View.GONE);		
									ArrayList<Long> handleList = new ArrayList<Long>();
									handleList.add(n.getHandle());									
									((ManagerActivity) context).showCopy(handleList);									
									break;
								}
								case 4: {
									//Send link
									
									menuOverflowLayout.setVisibility(View.GONE);	
									menuOverflowList.setVisibility(View.GONE);
									titleOverflowList.setVisibility(View.GONE);		
									break;
								}
							}							
						}
					});		
			menuOverflowLayout.setVisibility(View.VISIBLE);	
			menuOverflowList.setVisibility(View.VISIBLE);	
			titleOverflowList.setVisibility(View.VISIBLE);		
		
		}
		else{
			menuOverflowLayout.setVisibility(View.GONE);	
			menuOverflowList.setVisibility(View.GONE);
			titleOverflowList.setVisibility(View.GONE);	
		}
		
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

		if (isList){
			parentHandle = adapterList.getParentHandle();
			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
			
			if(overflowMenu){
				menuOverflowLayout.setVisibility(View.GONE);	
				menuOverflowList.setVisibility(View.GONE);
				titleOverflowList.setVisibility(View.GONE);	
				overflowMenu=false;
				return 1;
			}
			
			if (adapterList.getPositionClicked() != -1){
				adapterList.setPositionClicked(-1);
				adapterList.notifyDataSetChanged();
				return 1;
			}
			else{
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					leftNewFolder.setVisibility(View.GONE);
					rightUploadButton.setVisibility(View.GONE);
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
					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					adapterList.setNodes(nodes);
					listView.setSelection(0);
					adapterList.setParentHandle(parentHandle);
					contentText.setText(getInfoFolder(parentNode));
					return 2;
				}
				else{
					return 0;
				}
			}
		}
		else{
			parentHandle = adapterGrid.getParentHandle();
			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
			
			if (adapterGrid.getPositionClicked() != -1){
				adapterGrid.setPositionClicked(-1);
				adapterGrid.notifyDataSetChanged();
				return 1;
			}
			else{
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					leftNewFolder.setVisibility(View.GONE);
					rightUploadButton.setVisibility(View.GONE);
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
					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					adapterGrid.setNodes(nodes);
					listView.setSelection(0);
					adapterGrid.setParentHandle(parentHandle);
					contentText.setText(getInfoFolder(parentNode));
					return 2;
				}
				else{
					return 0;
				}
			}
		}
	}
	
	public long getParentHandle(){
		if (isList){
			if (adapterList != null){
				return adapterList.getParentHandle();
			}
			else{
				return -1;
			}
		}
		else{
			if (adapterGrid != null){
				return adapterGrid.getParentHandle();
			}
			else{
				return -1;
			}
		}
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (isList){
			if (adapterList != null){
				adapterList.setParentHandle(parentHandle);
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
					leftNewFolder.setVisibility(View.VISIBLE);
					rightUploadButton.setVisibility(View.VISIBLE);
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
					leftNewFolder.setVisibility(View.GONE);
					rightUploadButton.setVisibility(View.GONE);
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
					leftNewFolder.setVisibility(View.VISIBLE);
					rightUploadButton.setVisibility(View.VISIBLE);
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
					leftNewFolder.setVisibility(View.GONE);
					rightUploadButton.setVisibility(View.GONE);
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
	
	public void setTransfers(HashMap<Long, MegaTransfer> _mTHash){
		this.mTHash = _mTHash;
		
		if (isList){
			if (adapterList != null){
				adapterList.setTransfers(mTHash);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setTransfers(mTHash);
			}
		}	
	
	}
	
	public void setCurrentTransfer(MegaTransfer mT){
		if (isList){
			if (adapterList != null){
				adapterList.setCurrentTransfer(mT);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setCurrentTransfer(mT);
			}
		}	
		
		
	}
	
	private static void log(String log) {
		Util.log("FileBrowserFragment", log);
	}
	
	public void setContentText(){
		
		if (parentHandle == megaApi.getRootNode().getHandle()){
			MegaNode infoNode = megaApi.getRootNode();
			if (infoNode !=  null){
				contentText.setText(getInfoFolder(infoNode));
				aB.setTitle(getString(R.string.section_cloud_drive));
			}
		}
		else{
			MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
			if (infoNode !=  null){
				contentText.setText(getInfoFolder(infoNode));
				aB.setTitle(infoNode.getName());
			}
		}
	}
}
