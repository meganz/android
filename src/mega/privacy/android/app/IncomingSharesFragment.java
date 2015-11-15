package mega.privacy.android.app;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.TranslateAnimation;
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


public class IncomingSharesFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener{

	Context context;
	ActionBar aB;
	ListView listView;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserListAdapter adapterList;
	MegaBrowserGridAdapter adapterGrid;
	IncomingSharesFragment incomingSharesFragment = this;
	LinearLayout buttonsLayout=null;
	TextView outSpaceText;
	Button outSpaceButton;
	int usedSpacePerc;
	Button leftNewFolder;
	Button rightUploadButton;
	TextView contentText;
	MegaApiAndroid megaApi;

	LinearLayout outSpaceLayout=null;
	LinearLayout getProLayout=null;

	long parentHandle = -1;
	int deepBrowserTree = 0;
	boolean isList = true;
	boolean overflowMenu = false;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	ArrayList<MegaNode> nodes;
	
	HashMap<Long, MegaTransfer> mTHash = null;
	
	private ActionMode actionMode;
	
	private class ActionBarCallBack implements ActionMode.Callback {
		
		boolean showRename = false;
		boolean showMove = false;
		boolean showLink = false;
		boolean showCopy = false;


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
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					break;
				}
				case R.id.cab_menu_leave_multiple_share: {
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivity) context).leaveMultipleShares(handleList);					
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
			listView.setOnItemLongClickListener(incomingSharesFragment);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = getSelectedDocuments();
					
			if (selected.size() != 0) {
				showMove = false;
				showCopy = true;
				
				// Rename
				if(selected.size() == 1) {
					
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
			
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		nodes = new ArrayList<MegaNode>();
		parentHandle=-1;
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
				
		if (isList){
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
	        
	        listView = (ListView) v.findViewById(R.id.file_list_view_browser);
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			listView.setItemsCanFocus(false);
			
			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			contentText = (TextView) v.findViewById(R.id.content_text);
			
			emptyImageView.setImageResource(R.drawable.ic_empty_shared);
			
			emptyTextView.setText(R.string.file_browser_empty_incoming_shares);
			getProLayout=(LinearLayout) v.findViewById(R.id.get_pro_account);
			getProLayout.setVisibility(View.GONE);
			
			outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space);
			outSpaceText =  (TextView) v.findViewById(R.id.out_space_text);
			outSpaceButton = (Button) v.findViewById(R.id.out_space_btn);
			
			outSpaceButton.setOnClickListener(this);
			
			usedSpacePerc=((ManagerActivity)context).getUsedPerc();
			
			if(usedSpacePerc>95){
				//Change below of ListView
				log("usedSpacePerc>95");
		
//				RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//				p.addRule(RelativeLayout.ABOVE, R.id.out_space);
//				listView.setLayoutParams(p);
				outSpaceLayout.setVisibility(View.VISIBLE);
				outSpaceLayout.bringToFront();
				
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {					
					
					@Override
					public void run() {
						log("BUTTON DISAPPEAR");
						log("altura: "+outSpaceLayout.getHeight());
						
						TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, outSpaceLayout.getHeight());
						animTop.setDuration(2000);
						animTop.setFillAfter(true);
						outSpaceLayout.setAnimation(animTop);
					
						outSpaceLayout.setVisibility(View.GONE);
						outSpaceLayout.invalidate();
//						RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//						p.addRule(RelativeLayout.ABOVE, R.id.buttons_layout);
//						listView.setLayoutParams(p);
					}
				}, 15 * 1000);
				
			}	
			else{
				outSpaceLayout.setVisibility(View.GONE);
			}			
		
			buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_layout);
			leftNewFolder = (Button) v.findViewById(R.id.btnLeft_new);
			rightUploadButton = (Button) v.findViewById(R.id.btnRight_upload);	
			leftNewFolder.setVisibility(View.GONE);
			rightUploadButton.setVisibility(View.GONE);
			buttonsLayout.setVisibility(View.GONE);
			
			if (adapterList == null){
				adapterList = new MegaBrowserListAdapter(context, nodes, parentHandle, listView, aB, ManagerActivity.INCOMING_SHARES_ADAPTER);
				if (mTHash != null){
					adapterList.setTransfers(mTHash);
				}
//				adapterList.setNodes(nodes);
			}
			else{
				adapterList.setParentHandle(parentHandle);
//				adapterList.setNodes(nodes);
			}
			
			if (parentHandle == -1){			
				((ManagerActivity)context).setParentHandleIncoming(-1);					
				findNodes();	
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByNameDescending();
				}
				else{
					sortByNameAscending();
				}
				
				if(((ManagerActivity)context).getmDrawerToggle() != null)
				{
					aB.setTitle(getString(R.string.section_shared_items));	
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
					((ManagerActivity)context).supportInvalidateOptionsMenu();
				}
				adapterList.parentHandle=-1;
			}
			else{
				adapterList.parentHandle=parentHandle;
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				((ManagerActivity)context).setParentHandleIncoming(parentHandle);

				nodes = megaApi.getChildren(parentNode, orderGetChildren);
//				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
//					sortByNameDescending();
//				}
//				else{
//					sortByNameAscending();
//				}
				
				if(((ManagerActivity)context).getmDrawerToggle() != null)
				{
					aB.setTitle(parentNode.getName());
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
					((ManagerActivity)context).supportInvalidateOptionsMenu();
				}
			}	

			if (deepBrowserTree == 0){
				contentText.setText(getInfoNode());
				if(((ManagerActivity)context).getmDrawerToggle() != null)
				{
					aB.setTitle(getString(R.string.section_shared_items));
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
				}
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				contentText.setText(getInfoFolder(infoNode));
				aB.setTitle(infoNode.getName());
			}						
			
			adapterList.setPositionClicked(-1);
			adapterList.setMultipleSelect(false);
			
			listView.setAdapter(adapterList);		
			
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}
			
			if (adapterList.getCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);	
				contentText.setVisibility(View.GONE);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				aB.setTitle(getInfoNode());
				contentText.setVisibility(View.VISIBLE);
			}	
//			setNodes(nodes);	
			
			return v;
		}
		else{
			log("Grid View");
			
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
			
			listView = (ListView) v.findViewById(R.id.file_grid_view_browser);
			listView.setOnItemClickListener(null);
			listView.setItemsCanFocus(false);
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			contentText = (TextView) v.findViewById(R.id.content_grid_text);
			
			buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_grid_layout);
			leftNewFolder = (Button) v.findViewById(R.id.btnLeft_grid_new);
			rightUploadButton = (Button) v.findViewById(R.id.btnRight_grid_upload);	
			leftNewFolder.setVisibility(View.GONE);
			rightUploadButton.setVisibility(View.GONE);
			buttonsLayout.setVisibility(View.GONE);
	        
			outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space_grid);
			outSpaceLayout.setVisibility(View.GONE);
			
			if (adapterGrid == null){
				adapterGrid = new MegaBrowserGridAdapter(context, nodes, parentHandle, listView, aB, ManagerActivity.FILE_BROWSER_ADAPTER);
				if (mTHash != null){
					adapterGrid.setTransfers(mTHash);
				}
			}
			else{
				adapterGrid.setParentHandle(parentHandle);
			}
			
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}
						
			adapterGrid.setPositionClicked(-1);
			
			listView.setAdapter(adapterGrid);			
	
			if (adapterGrid.getCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);	
				contentText.setVisibility(View.GONE);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				aB.setTitle(getInfoNode());
				contentText.setVisibility(View.VISIBLE);
			}	
					
			return v;
		}		
	}
	

	public void refresh(){
		log("refresh");
		//TODO conservar el path
		findNodes();
		if(adapterList!=null){
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}
		}
	}
	
	public void refresh (long _parentHandle){
		MegaNode n = megaApi.getNodeByHandle(_parentHandle);
		if(n == null)
		{
			refresh();
			return;
		}
		
		aB.setTitle(n.getName());
		((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
		((ManagerActivity)context).supportInvalidateOptionsMenu();
		
		parentHandle = n.getHandle();												
		contentText.setText(getInfoFolder(n));
		adapterList.setParentHandle(parentHandle);
		nodes = megaApi.getChildren(n, orderGetChildren);
		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}

		listView.setSelection(0);
		
		//If folder has no files
		if (adapterList.getCount() == 0){
			listView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			leftNewFolder.setVisibility(View.GONE);
			rightUploadButton.setVisibility(View.GONE);

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
			case R.id.out_space_btn:
				((ManagerActivity)getActivity()).upgradeAccountButton();
				break;
		}
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
	
	private String getInfoFolder(MegaNode n) {
		int numFolders = megaApi.getNumChildFolders(n);
		int numFiles = megaApi.getNumChildFiles(n);

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
					
					deepBrowserTree = deepBrowserTree+1;
					
					MegaNode n = nodes.get(position);
					
					aB.setTitle(n.getName());
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
					((ManagerActivity)context).supportInvalidateOptionsMenu();
					
					parentHandle = nodes.get(position).getHandle();
					MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
															
					contentText.setText(getInfoFolder(infoNode));
					((ManagerActivity)context).setParentHandleIncoming(parentHandle);
					adapterList.setParentHandle(parentHandle);
					nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
					if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
						sortByNameDescending();
					}
					else{
						sortByNameAscending();
					}
//					adapterList.setNodes(nodes);
					listView.setSelection(0);
					
					//If folder has no files
					if (adapterList.getCount() == 0){
						listView.setVisibility(View.GONE);
						emptyImageView.setVisibility(View.VISIBLE);
						emptyTextView.setVisibility(View.VISIBLE);
						leftNewFolder.setVisibility(View.GONE);
						rightUploadButton.setVisibility(View.GONE);

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
					if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
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
				  		if (ManagerActivity.isIntentAvailable(context, mediaIntent)){
				  			startActivity(mediaIntent);
				  		}
				  		else{
				  			Toast.makeText(context, getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
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
		else{
			MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
			contentText.setText(getInfoFolder(infoNode));
			
			if (adapterGrid.getCount() == 0){			

				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

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
	
	public void findNodes(){
		deepBrowserTree=0;
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		nodes.clear();
		for (int i=0;i<contacts.size();i++){			
			ArrayList<MegaNode> nodeContact=megaApi.getInShares(contacts.get(i));
			if(nodeContact!=null){
				if(nodeContact.size()>0){
					nodes.addAll(nodeContact);
				}
			}			
		}		
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
	public void hideMultipleSelect() {
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		log("onBackPressed "+deepBrowserTree);
		deepBrowserTree = deepBrowserTree-1;
		
		if (isList){
			
			if (adapterList == null){
				return 0;
			}
			
			if (adapterList.isMultipleSelect()){
				hideMultipleSelect();
				return 2;
			}
			
			if(deepBrowserTree==0){
				//In the beginning of the navigation
				((ManagerActivity)context).setParentHandleIncoming(-1);
				parentHandle=-1;
				aB.setTitle(getString(R.string.section_shared_items));	
				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
				findNodes();
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByNameDescending();
				}
				else{
					sortByNameAscending();
				}
//				adapterList.setNodes(nodes);
				listView.setVisibility(View.VISIBLE);
				contentText.setText(getInfoNode());
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				leftNewFolder.setVisibility(View.GONE);
				rightUploadButton.setVisibility(View.GONE);
				return 3;
			}
			else if (deepBrowserTree>0){
				parentHandle = adapterList.getParentHandle();
				//((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);			
				
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				
				contentText.setText(getInfoFolder(parentNode));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					leftNewFolder.setVisibility(View.GONE);
					rightUploadButton.setVisibility(View.GONE);

						aB.setTitle(parentNode.getName());					
						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);

					((ManagerActivity)context).supportInvalidateOptionsMenu();
					
					parentHandle = parentNode.getHandle();
					((ManagerActivity)context).setParentHandleIncoming(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					//TODO
					adapterList.setNodes(nodes);
					listView.setSelection(0);
					adapterList.setParentHandle(parentHandle);
					return 2;
				}	
				return 2;
			}
			else{
				((ManagerActivity)context).setParentHandleBrowser(megaApi.getRootNode().getHandle());
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				leftNewFolder.setVisibility(View.GONE);
				rightUploadButton.setVisibility(View.GONE);
				deepBrowserTree=0;
				return 0;
			}
			
		}
		else{
			parentHandle = adapterGrid.getParentHandle();
			((ManagerActivity)context).setParentHandleIncoming(parentHandle);
			
			if (adapterGrid.getPositionClicked() != -1){
				adapterGrid.setPositionClicked(-1);
				adapterGrid.notifyDataSetChanged();
				return 1;
			}
			else{
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
				contentText.setText(getInfoFolder(parentNode));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					leftNewFolder.setVisibility(View.GONE);
					rightUploadButton.setVisibility(View.GONE);
					
					aB.setTitle(parentNode.getName());					
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);					
					
					((ManagerActivity)context).supportInvalidateOptionsMenu();
					
					parentHandle = parentNode.getHandle();
					((ManagerActivity)context).setParentHandleIncoming(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					//TODO ?
					adapterGrid.setNodes(nodes);
					listView.setSelection(0);
					adapterGrid.setParentHandle(parentHandle);
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
	
	public void sortByNameDescending(){
		
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
		
		for (int i=0;i<folderNodes.size();i++){
			for (int j=0;j<folderNodes.size()-1;j++){
				if (folderNodes.get(j).getName().compareTo(folderNodes.get(j+1).getName()) < 0){
					MegaNode nAuxJ = folderNodes.get(j);
					MegaNode nAuxJ_1 = folderNodes.get(j+1);
					folderNodes.remove(j+1);
					folderNodes.remove(j);
					folderNodes.add(j, nAuxJ_1);
					folderNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		for (int i=0;i<fileNodes.size();i++){
			for (int j=0;j<fileNodes.size()-1;j++){
				if (fileNodes.get(j).getName().compareTo(fileNodes.get(j+1).getName()) < 0){
					MegaNode nAuxJ = fileNodes.get(j);
					MegaNode nAuxJ_1 = fileNodes.get(j+1);
					fileNodes.remove(j+1);
					fileNodes.remove(j);
					fileNodes.add(j, nAuxJ_1);
					fileNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		nodes.clear();
		nodes.addAll(folderNodes);
		nodes.addAll(fileNodes);
		
		if (isList){
			adapterList.setNodes(nodes);
		}
		else{
			adapterGrid.setNodes(nodes);
		}
	}

	
	public void sortByNameAscending(){
		log("sortByNameAscending");
		
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
		
		for (int i=0;i<folderNodes.size();i++){
			for (int j=0;j<folderNodes.size()-1;j++){
				if (folderNodes.get(j).getName().compareTo(folderNodes.get(j+1).getName()) > 0){
					MegaNode nAuxJ = folderNodes.get(j);
					MegaNode nAuxJ_1 = folderNodes.get(j+1);
					folderNodes.remove(j+1);
					folderNodes.remove(j);
					folderNodes.add(j, nAuxJ_1);
					folderNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		for (int i=0;i<fileNodes.size();i++){
			for (int j=0;j<fileNodes.size()-1;j++){
				if (fileNodes.get(j).getName().compareTo(fileNodes.get(j+1).getName()) > 0){
					MegaNode nAuxJ = fileNodes.get(j);
					MegaNode nAuxJ_1 = fileNodes.get(j+1);
					fileNodes.remove(j+1);
					fileNodes.remove(j);
					fileNodes.add(j, nAuxJ_1);
					fileNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		nodes.clear();
		nodes.addAll(folderNodes);
		nodes.addAll(fileNodes);
				
		if (isList){
			adapterList.setNodes(nodes);
		}
		else{
			adapterGrid.setNodes(nodes);
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
		Util.log("IncomingSharesFragment", log);
	}

}
