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

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaShare;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaUser;

public class SharedWithMeFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener{

	public static int MODE_IN = 0;
	public static int MODE_OUT = 1;
	Context context;
	ActionBar aB;
	ListView listView;
	MegaShareInOutAdapter adapterList;
	MegaBrowserGridAdapter adapterGrid;
	public SharedWithMeFragment sharedWithMeFragment = this;
	MegaUser owner = null;
	long initialParentHandle;
	
	ArrayList<MegaShare> outNodeList;
	ArrayList<MegaUser> uL;
	
	boolean isList = true;
	long parentHandle = -1;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	//NodeList nodes;
	
	int modeShare = MODE_IN; 
	
	HashMap<Long, MegaTransfer> mTHash = null;
	
	ArrayList<MegaShareAndroidElement> megaShareInList = null;
	ArrayList<MegaShareAndroidElement> megaShareOutList = null;
	
	ImageView emptyImageView;
	TextView emptyTextView;
	
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
//			adapterList.setMultipleSelect(false);
			listView.setOnItemLongClickListener(sharedWithMeFragment);
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
				showLink = false;
			}
			
			if (selected.size() > 0) {
				showDownload = true;
				showTrash = true;
				showMove = false;
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
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			
			return false;
		}
		
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		megaShareInList = new ArrayList<MegaShareAndroidElement> ();
		megaShareOutList = new ArrayList<MegaShareAndroidElement> ();
		
		owner=null;
		parentHandle = -1;
		initialParentHandle = -1;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		if(parentHandle==-1){
			aB.setTitle(getString(R.string.section_shared_with_me));			
		}
 
		
		if(modeShare==MODE_IN){
			if (adapterList != null){
				adapterList.setType(MODE_IN);
			}
			
			uL = megaApi.getContacts();
			megaShareInList.clear();
			megaShareOutList.clear();
			for(int i=0; i<uL.size();i++){
				MegaUser user=uL.get(i);
				log("USER: " + user.getEmail());
				ArrayList<MegaNode> inNodeList=megaApi.getInShares(user);
				if(inNodeList.size()>0){
					MegaShareAndroidElement mSIn = new MegaShareAndroidElement(user, null);
					megaShareInList.add(mSIn);
					for(int j=0; j<inNodeList.size();j++){
						MegaNode node = inNodeList.get(j);
						mSIn = new MegaShareAndroidElement(user, node);
						log("node.getName() = " + node.getName());
						megaShareInList.add(mSIn);
					}					
				}				
			}
		}
		else{

			if (adapterList != null){
				adapterList.setType(MODE_OUT);
			}
			
			outNodeList = megaApi.getOutShares();
			//outNodeList = megaApi.getOutShares();
			log("outNodeList.size:" +outNodeList.size());
			
			long lastFolder=-1;
			megaShareInList.clear();
			megaShareOutList.clear();
			
			for(int k=0;k<outNodeList.size();k++){
				
				
				MegaShare mS = outNodeList.get(k);				
				MegaNode node = megaApi.getNodeByHandle(mS.getNodeHandle());
				
				log("Node: "+node.getHandle());
				
				if(lastFolder!=node.getHandle()){
					log("Entro por distinto");
					if(mS.getUser()!=null){						
						MegaUser user=megaApi.getContact(mS.getUser());					
						MegaShareAndroidElement mSOut = new MegaShareAndroidElement(user, node);					
						megaShareOutList.add(mSOut);	
								
					}	
					else{
						
						//Ojo el node que es public link
						log("Public link");
						
						MegaUser user=null;					
						MegaShareAndroidElement mSOut = new MegaShareAndroidElement(user, node);
						megaShareOutList.add(mSOut);

					}
					lastFolder=node.getHandle();
				}
				else{
					log("Entro por igual");
					if(mS.getUser()!=null){
						
						MegaUser user=megaApi.getContact(mS.getUser());					
						MegaShareAndroidElement mSOut = new MegaShareAndroidElement(user, node);
						mSOut.setRepeat(true);
						megaShareOutList.add(mSOut);	
						lastFolder=node.getHandle();						
						
					}	
					else{						
						//Ojo el node que es public link
						log("Public link");						
						MegaUser user=null;					
						MegaShareAndroidElement mSOut = new MegaShareAndroidElement(user, node);
						mSOut.setRepeat(true);
						megaShareOutList.add(mSOut);

					}
				}
			}
		}
			
		//outNodeList.get(0).
						
//			//parentHandle = megaApi.getInboxNode().getHandle();
//			((ManagerActivity)context).setParentHandleSharedWithMe(-1);
//			//nodes = megaApi.getChildren(megaApi.getInboxNode(), orderGetChildren);
//			aB.setTitle(getString(R.string.section_shared_with_me));	
//			((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//			((ManagerActivity)context).supportInvalidateOptionsMenu();
//		}
//		else{
//			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
//			
//			if (parentNode == null){
//				parentNode = megaApi.getInboxNode();
//				parentHandle = parentNode.getHandle();
//				((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);
//			}
//			nodes = megaApi.getChildren(parentNode, orderGetChildren);
//			
//			if (parentNode.getHandle() == megaApi.getInboxNode().getHandle()){
//				aB.setTitle(getString(R.string.section_shared_with_me));	
//				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//			}
//			else{
//				aB.setTitle(parentNode.getName());					
//				((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//			}
//			((ManagerActivity)context).supportInvalidateOptionsMenu();
//		}

		if (isList){
			View v = inflater.inflate(R.layout.fragment_sharedwithmelist, container, false);
	        
	        listView = (ListView) v.findViewById(R.id.sharedwithme_list_view);
			listView.setOnItemClickListener(this);
			//listView.setOnItemLongClickListener(this);
			//listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			listView.setItemsCanFocus(false);
			
			emptyImageView = (ImageView) v.findViewById(R.id.sharedwithme_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.sharedwithme_list_empty_text);
			log("Llego aqui---------------");
			
			if(modeShare==MODE_IN){		
				log("Creo adapter in");
				if (adapterList == null){
					adapterList = new MegaShareInOutAdapter(context, megaShareInList, parentHandle, listView, emptyImageView, emptyTextView, aB, MODE_IN);
					//adapterList = new MegaBrowserListAdapter(context, inNodeList, parentHandle, listView, emptyImageView, emptyTextView, aB, ManagerActivity.SHARED_WITH_ME_ADAPTER);
					if (mTHash != null){
						adapterList.setTransfers(mTHash);
					}
				}
				else{
					adapterList.setParentHandle(parentHandle);
					adapterList.setNodes(megaShareInList);
				}
				
				adapterList.setPositionClicked(-1);
//				adapterList.setMultipleSelect(false);

				listView.setAdapter(adapterList);
				
				setNodes(megaShareInList);

			}
			else{
				//MODE OUT
				log("Creo adapter OUT");
				if (adapterList == null){
					
					adapterList = new MegaShareInOutAdapter(context, megaShareOutList, parentHandle, listView, emptyImageView, emptyTextView, aB, MODE_OUT);
					//adapterList = new MegaBrowserListAdapter(context, inNodeList, parentHandle, listView, emptyImageView, emptyTextView, aB, ManagerActivity.SHARED_WITH_ME_ADAPTER);
					if (mTHash != null){
						adapterList.setTransfers(mTHash);
					}
				}
				else{
					adapterList.setParentHandle(parentHandle);
					adapterList.setNodes(megaShareOutList);
				}
				
				adapterList.setPositionClicked(-1);
//				adapterList.setMultipleSelect(false);

				listView.setAdapter(adapterList);
				
				setNodes(megaShareOutList);
			}
			
	
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_sharedwithmegrid, container, false);
				        
	        listView = (ListView) v.findViewById(R.id.sharedwithme_grid_view);
	        listView.setOnItemClickListener(null);
	        listView.setItemsCanFocus(false);

	        emptyImageView = (ImageView) v.findViewById(R.id.sharedwithme_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.sharedwithme_grid_empty_text);
			
			/*
			if (adapterGrid == null){
				adapterGrid = new MegaBrowserGridAdapter(context, nodes, parentHandle, listView, emptyImageView, emptyTextView, aB, ManagerActivity.SHARED_WITH_ME_ADAPTER);
				if (mTHash != null){
					adapterGrid.setTransfers(mTHash);
				}
			}
			else{
				adapterGrid.setParentHandle(parentHandle);
				adapterGrid.setNodes(nodes);
			}
			*/			
		        
			adapterGrid.setPositionClicked(-1);
			listView.setAdapter(adapterGrid);
			
			setNodes(megaShareInList);
			
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

		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (adapterList.getPositionClicked() == -1){
			clearSelections();
			actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
			listView.setItemChecked(position, true);
//			adapterList.setMultipleSelect(true);
			updateActionModeTitle();
			listView.setOnItemLongClickListener(null);
		}
		return true;
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
		
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
				 if(modeShare==MODE_IN){
					if(megaShareInList.get(position).getNode()!=null){
						if (megaShareInList.get(position).getNode().isFolder()){
							MegaNode parentNode = megaShareInList.get(position).getNode();
							MegaUser user= megaShareInList.get(position).getUser();
							owner=user;
							ArrayList<MegaNode> childrenNodes;
							
							if(parentHandle==-1){						
								initialParentHandle=megaShareInList.get(position).getNode().getHandle();
								log("------------------Initial Parent Handle: "+initialParentHandle);
							}
							
							aB.setTitle(parentNode.getName());
							((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
							((ManagerActivity)context).supportInvalidateOptionsMenu();
							
							parentHandle = megaShareInList.get(position).getNode().getHandle();
							((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);
							adapterList.setParentHandle(parentHandle);
							
							childrenNodes=megaApi.getChildren(parentNode, orderGetChildren);
							
							megaShareInList.clear();
							
							for(int i=0; i<childrenNodes.size();i++){
								
								MegaNode nodeChild = childrenNodes.get(i);
								MegaShareAndroidElement msIn = new MegaShareAndroidElement(user, nodeChild);
								megaShareInList.add(msIn);
								
							}
																
							adapterList.setNodes(megaShareInList);
							listView.setSelection(0);
							
							//If folder has no files
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
						else{
							if (MimeType.typeForName(megaShareInList.get(position).getNode().getName()).isImage()){
								Intent intent = new Intent(context, FullScreenImageViewer.class);
								intent.putExtra("position", position);
								intent.putExtra("adapterType", ManagerActivity.SHARED_WITH_ME_ADAPTER);
								if (megaApi.getParentNode(megaShareInList.get(position).getNode()).getType() == MegaNode.TYPE_INCOMING){
									intent.putExtra("parentNodeHandle", -1L);
								}
								else{
									intent.putExtra("parentNodeHandle", megaApi.getParentNode(megaShareInList.get(position).getNode()).getHandle());
								}
								intent.putExtra("orderGetChildren", orderGetChildren);
								startActivity(intent);
							}
							else if (MimeType.typeForName(megaShareInList.get(position).getNode().getName()).isVideo() || MimeType.typeForName(megaShareInList.get(position).getNode().getName()).isAudio()){
								MegaNode file = megaShareInList.get(position).getNode();
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
								handleList.add(megaShareInList.get(position).getNode().getHandle());
								((ManagerActivity) context).onFileClick(handleList);
							}
						}
						
					}
					else{
						 //Pulsamos el contacto
					}
				}
				 else{
					 //MODE OUT
						if(megaShareOutList.get(position).getNode()!=null){
							if (megaShareOutList.get(position).getNode().isFolder()){
								MegaNode parentNode = megaShareOutList.get(position).getNode();
								
								MegaUser user= megaShareOutList.get(position).getUser();
								owner=user;
								ArrayList<MegaNode> childrenNodes;
								
								if(parentHandle==-1){						
									initialParentHandle=megaShareOutList.get(position).getNode().getHandle();
									log("------------------Initial Parent Handle: "+initialParentHandle);
								}
								
								aB.setTitle(parentNode.getName());
								((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
								((ManagerActivity)context).supportInvalidateOptionsMenu();
								
								parentHandle = megaShareOutList.get(position).getNode().getHandle();
								((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);
								adapterList.setParentHandle(parentHandle);
								
								childrenNodes=megaApi.getChildren(parentNode, orderGetChildren);
								
								megaShareOutList.clear();
								
								for(int i=0; i<childrenNodes.size();i++){
									
									MegaNode nodeChild = childrenNodes.get(i);
									MegaShareAndroidElement msIn = new MegaShareAndroidElement(user, nodeChild);
									megaShareOutList.add(msIn);
									
								}
																	
								adapterList.setNodes(megaShareOutList);
								listView.setSelection(0);
								
								//If folder has no files
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
							else{
								if (MimeType.typeForName(megaShareOutList.get(position).getNode().getName()).isImage()){
									Intent intent = new Intent(context, FullScreenImageViewer.class);
									intent.putExtra("position", position);
									intent.putExtra("adapterType", ManagerActivity.SHARED_WITH_ME_ADAPTER);
									if (megaApi.getParentNode(megaShareOutList.get(position).getNode()).getType() == MegaNode.TYPE_INCOMING){
										intent.putExtra("parentNodeHandle", -1L);
									}
									else{
										intent.putExtra("parentNodeHandle", megaApi.getParentNode(megaShareOutList.get(position).getNode()).getHandle());
									}
									intent.putExtra("orderGetChildren", orderGetChildren);
									startActivity(intent);
								}
								else if (MimeType.typeForName(megaShareOutList.get(position).getNode().getName()).isVideo() || MimeType.typeForName(megaShareOutList.get(position).getNode().getName()).isAudio()){
									MegaNode file = megaShareOutList.get(position).getNode();
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
									handleList.add(megaShareOutList.get(position).getNode().getHandle());
									((ManagerActivity) context).onFileClick(handleList);
								}
							}
							
						}
						else{
							 //Pulsamos el contacto
						}
					 
					 
					 
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
				MegaNode document = adapterList.getNodeAt(checkedItems.keyAt(i));
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

			parentHandle = adapterList.getParentHandle();
			log("ParentHAndle onBack: "+parentHandle);

			((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);

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

					aB.setTitle(parentNode.getName());
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);

					((ManagerActivity)context).supportInvalidateOptionsMenu();

					parentHandle = parentNode.getHandle();
					((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);
					adapterList.setParentHandle(parentHandle);

					ArrayList<MegaNode> childrenNodes;
					childrenNodes=megaApi.getChildren(parentNode, orderGetChildren);

					megaShareInList.clear();

					for(int i=0; i<childrenNodes.size();i++){

						MegaNode nodeChild = childrenNodes.get(i);
						MegaShareAndroidElement msIn = new MegaShareAndroidElement(owner, nodeChild);
						megaShareInList.add(msIn);

					}				

					adapterList.setNodes(megaShareInList);
					listView.setSelection(0);
					adapterList.setParentHandle(parentHandle);	
					return 2;					
					
				}
				else{
					if(initialParentHandle!=-1){
						if(parentHandle==initialParentHandle){		
							log("Set Initial Screen: "+parentHandle);
													
							aB.setTitle(getString(R.string.section_shared_with_me));	
							((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);							
		
							megaShareInList.clear();
							megaShareOutList.clear();
		
							if(modeShare==MODE_IN){
		
								uL = megaApi.getContacts();
		
								for(int i=0; i<uL.size();i++){
									MegaUser user=uL.get(i);
									log("USER: " + user.getEmail());
									ArrayList<MegaNode> inNodeList=megaApi.getInShares(user);
									if(inNodeList.size()>0){
										MegaShareAndroidElement mSI = new MegaShareAndroidElement(user, null);
										megaShareInList.add(mSI);
										for(int j=0; j<inNodeList.size();j++){
											MegaNode node = inNodeList.get(j);
											mSI = new MegaShareAndroidElement(user, node);
											log("node.getName() = " + node.getName());
											megaShareInList.add(mSI);
										}					
									}				
								}
							}
							else{
								//TODO: el modo out		
								
								outNodeList = megaApi.getOutShares();
								//outNodeList = megaApi.getOutShares();
								log("outNodeList.size:" +outNodeList.size());
								
								long lastFolder=-1;
								megaShareInList.clear();
								megaShareOutList.clear();
								
								for(int k=0;k<outNodeList.size();k++){
									
									
									MegaShare mS = outNodeList.get(k);				
									MegaNode node = megaApi.getNodeByHandle(mS.getNodeHandle());
									
									log("Node: "+node.getHandle());
									
									if(lastFolder!=node.getHandle()){
										log("Entro por distinto");
										if(mS.getUser()!=null){						
											MegaUser user=megaApi.getContact(mS.getUser());					
											MegaShareAndroidElement mSOut = new MegaShareAndroidElement(user, node);					
											megaShareOutList.add(mSOut);	
													
										}	
										else{
											
											//Ojo el node que es public link
											log("Public link");
											
											MegaUser user=null;					
											MegaShareAndroidElement mSOut = new MegaShareAndroidElement(user, node);
											megaShareOutList.add(mSOut);

										}
										lastFolder=node.getHandle();
									}
									else{
										log("Entro por igual");
										if(mS.getUser()!=null){
											
											MegaUser user=megaApi.getContact(mS.getUser());					
											MegaShareAndroidElement mSOut = new MegaShareAndroidElement(user, node);
											mSOut.setRepeat(true);
											megaShareOutList.add(mSOut);	
											lastFolder=node.getHandle();						
											
										}	
										else{						
											//Ojo el node que es public link
											log("Public link");						
											MegaUser user=null;					
											MegaShareAndroidElement mSOut = new MegaShareAndroidElement(user, node);
											mSOut.setRepeat(true);
											megaShareOutList.add(mSOut);

										}
									}
								}											
								
							}
		
							owner=null;
							adapterList.setNodes(megaShareInList);
							listView.setSelection(0);
							adapterList.setParentHandle(-1);
							parentHandle=-1;
							((ManagerActivity)context).setParentHandleSharedWithMe(-1);
							initialParentHandle=-1;	
							return 2;	
						}
						else{
							return 0;
						}					
					}
					else{
						adapterList.setParentHandle(-1);
						((ManagerActivity)context).setParentHandleSharedWithMe(-1);
						owner=null;						
						return 0;
					}
				}				
			}
		}
		else{
			//GRID ADAPTER
			parentHandle = adapterGrid.getParentHandle();
			((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);

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
					if (parentNode.getHandle() == megaApi.getInboxNode().getHandle()){
						aB.setTitle(getString(R.string.section_shared_with_me));	
						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
					}
					else{
						aB.setTitle(parentNode.getName());					
						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
					}

					((ManagerActivity)context).supportInvalidateOptionsMenu();

					parentHandle = parentNode.getHandle();
					((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);
					//					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					//					adapterGrid.setNodes(nodes);
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
	
	public void setNodes(ArrayList<MegaShareAndroidElement> _megaShareInList){
		this.megaShareInList = _megaShareInList;
		if (isList){
			if (adapterList != null){
				adapterList.setNodes(megaShareInList);
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
				/*adapterGrid.setNodes(nodes);
				if (adapterGrid.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApi.getInboxNode().getHandle()==parentHandle) {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						emptyTextView.setText(R.string.file_browser_empty_shared_with_me);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_empty_folder);
						emptyTextView.setText(R.string.file_browser_empty_folder);
					}
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}*/			
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
	
	public int getModeShare (){		
		return modeShare;		
	}
	
	public void setModeShare(int mode){
		modeShare=mode;	
	}
	
	private static void log(String log) {
		Util.log("SharedWithMeFragment", log);
	}
}
