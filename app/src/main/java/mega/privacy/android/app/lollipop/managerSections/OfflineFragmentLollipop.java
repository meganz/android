package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.adapters.MegaOfflineLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

public class OfflineFragmentLollipop extends Fragment{
	
	MegaPreferences prefs;
	
	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;

	Stack<Integer> lastPositionStack;
	
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaOfflineLollipopAdapter adapter;
	OfflineFragmentLollipop offlineFragment = this;
	DatabaseHandler dbH = null;
	ArrayList<MegaOffline> mOffList= null;
	String pathNavigation = null;
	TextView contentText;
	boolean isList = true;
	int orderGetChildren;
	public static String DB_FILE = "0";
	public static String DB_FOLDER = "1";
	MegaApiAndroid megaApi;
	RelativeLayout contentTextLayout;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	private ActionMode actionMode;

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
			log("ActionBarCallBack::onActionItemClicked");
			List<MegaOffline> documents = adapter.getSelectedOfflineNodes();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						String path = documents.get(i).getPath() + documents.get(i).getName();
						MegaNode n = megaApi.getNodeByPath(path);	
						if(n == null)
						{
							continue;
						}
						handleList.add(n.getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList);
					break;
				}
				case R.id.cab_menu_rename:{

					if (documents.size()==1){
						String path = documents.get(0).getPath() + documents.get(0).getName();
						MegaNode n = megaApi.getNodeByPath(path);
						if(n == null)
						{
							break;
						}
						((ManagerActivityLollipop) context).showRenameDialog(n, n.getName());
					}
					break;
				}
				case R.id.cab_menu_share_link:{

					if (documents.size()==1){
						String path = documents.get(0).getPath() + documents.get(0).getName();
						MegaNode n = megaApi.getNodeByPath(path);
						if(n == null)
						{
							break;
						}
						NodeController nC = new NodeController(context);
						nC.exportLink(n);
					}

					break;
				}
				case R.id.cab_menu_share:{
					//Check that all the selected options are folders
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						String path = documents.get(i).getPath() + documents.get(i).getName();
						MegaNode n = megaApi.getNodeByPath(path);
						if(n == null)
						{
							continue;
						}
						handleList.add(n.getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.selectContactToShareFolders(handleList);
					break;
				}
				case R.id.cab_menu_move:{					
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						String path = documents.get(i).getPath() + documents.get(i).getName();
						MegaNode n = megaApi.getNodeByPath(path);			
						if(n == null)
						{
							continue;
						}
						handleList.add(n.getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();					
					for (int i=0;i<documents.size();i++){
						String path = documents.get(i).getPath() + documents.get(i).getName();
						MegaNode n = megaApi.getNodeByPath(path);
						if(n == null)
						{
							continue;
						}
						handleList.add(n.getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
					break;
				}
				case R.id.cab_menu_delete:{
					NodeController nC = new NodeController(context);
					for (int i=0;i<documents.size();i++){
						nC.deleteOffline(documents.get(i), pathNavigation);
					}					

					refreshPaths(documents.get(0));
					break;
				}
				case R.id.cab_menu_select_all:{
					((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);

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
			log("ActionBarCallBack::onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.offline_browser_action, menu);
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			clearSelections();
			adapter.setMultipleSelect(false);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("ActionBarCallBack::onPrepareActionMode");
			List<MegaOffline> selected = adapter.getSelectedOfflineNodes();
			
			if (Util.isOnline(context)){
				if (selected.size() != 0) {
					
					menu.findItem(R.id.cab_menu_download).setVisible(false);
					menu.findItem(R.id.cab_menu_share).setVisible(false);
					
					if(selected.size()==adapter.getItemCount()){
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
				
				if (selected.size() == 1) {
					menu.findItem(R.id.cab_menu_share_link).setVisible(false);
				}
				else{
					menu.findItem(R.id.cab_menu_share_link).setVisible(false);
				}
				
				menu.findItem(R.id.cab_menu_copy).setVisible(false);
				menu.findItem(R.id.cab_menu_move).setVisible(false);				
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_rename).setVisible(false);
			}
			else{
				if (selected.size() != 0) {
					
					menu.findItem(R.id.cab_menu_delete).setVisible(true);
					menu.findItem(R.id.cab_menu_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);


					if(selected.size()==adapter.getItemCount()){
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
				
//				if (selected.size() == 1) {
//					menu.findItem(R.id.cab_menu_rename).setVisible(true);
//				}
//				else{
//					menu.findItem(R.id.cab_menu_rename).setVisible(false);
//				}

				menu.findItem(R.id.cab_menu_download).setVisible(false);			
				menu.findItem(R.id.cab_menu_copy).setVisible(false);
				menu.findItem(R.id.cab_menu_move).setVisible(false);
				menu.findItem(R.id.cab_menu_share_link).setVisible(false);
				menu.findItem(R.id.cab_menu_rename).setVisible(false);
			}			
			return false;
		}
		
	}
	
	public void setIsListDB(boolean isList){
		dbH.setPreferredViewList(isList);
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
		
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (Util.isOnline(context)){
			if (megaApi == null){
				megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
			}
		}
		else{
			megaApi=null;
		}			
		
//		dbH = new DatabaseHandler(context);
		lastPositionStack = new Stack<>();

		dbH = DatabaseHandler.getDbHandler(context);
		
		mOffList = new ArrayList<MegaOffline>();		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}

		if (context instanceof ManagerActivityLollipop){

			String pathNavigationOffline = ((ManagerActivityLollipop)context).getPathNavigationOffline();
			if(pathNavigationOffline!=null){
				pathNavigation = pathNavigationOffline;
			}

			if(pathNavigation!=null){

				log("AFTER PathNavigation is: "+pathNavigation);
				if (pathNavigation.equals("/")){
					aB.setTitle(getString(R.string.section_saved_for_offline));
					log("aB.setHomeAsUpIndicator_30");
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				}
				else{
					log("The pathNavigation is: "+pathNavigation);
					String title = pathNavigation;
					int index=title.lastIndexOf("/");
					title=title.substring(0,index);
					index=title.lastIndexOf("/");
					title=title.substring(index+1,title.length());
					aB.setTitle(title);
					log("aB.setHomeAsUpIndicator_36");
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				}
			}
			else{
				log("PathNavigation is NULL");
			}

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		    isList = ((ManagerActivityLollipop)context).isList();
			orderGetChildren = ((ManagerActivityLollipop)context).getOrderOthers();

		}

		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		//Check pathNAvigation
		if (isList){
			log("onCreateList");
			View v = inflater.inflate(R.layout.fragment_offlinelist, container, false);
			recyclerView = (RecyclerView) v.findViewById(R.id.offline_view_browser);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setItemAnimator(new DefaultItemAnimator()); 
			
			emptyImageView = (ImageView) v.findViewById(R.id.offline_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.offline_empty_text);		
					
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.offline_content_text_layout);

			contentText = (TextView) v.findViewById(R.id.offline_content_text);			

			findNodes();

			if (adapter == null){
				adapter = new MegaOfflineLollipopAdapter(this, context, mOffList, recyclerView, emptyImageView, emptyTextView, aB, MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				adapter.setNodes(mOffList);
				adapter.setAdapterType(MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);

				adapter.setPositionClicked(-1);
				adapter.setMultipleSelect(false);

				recyclerView.setAdapter(adapter);

				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setImageResource(R.drawable.ic_empty_offline);
					emptyTextView.setText(R.string.offline_empty_folder);
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					contentText.setText(getInfoFolder(mOffList));
				}
			}
			else{
				adapter.setAdapterType(MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
		
			return v;
		}
		else{
			log("onCreateGRID");
			View v = inflater.inflate(R.layout.fragment_offlinegrid, container, false);
			
			recyclerView = (CustomizedGridRecyclerView) v.findViewById(R.id.offline_view_browser_grid);
			recyclerView.setHasFixedSize(true);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();

			recyclerView.setItemAnimator(new DefaultItemAnimator());
			
			emptyImageView = (ImageView) v.findViewById(R.id.offline_empty_image_grid);
			emptyTextView = (TextView) v.findViewById(R.id.offline_empty_text_grid);		

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.offline_content_grid_text_layout);

			contentText = (TextView) v.findViewById(R.id.offline_content_text_grid);			

			findNodes();
			
			if (adapter == null){
				adapter = new MegaOfflineLollipopAdapter(this, context, mOffList, recyclerView, emptyImageView, emptyTextView, aB, MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID);
				adapter.setNodes(mOffList);
				adapter.setAdapterType(MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID);

				adapter.setPositionClicked(-1);
				adapter.setMultipleSelect(false);

				recyclerView.setAdapter(adapter);

				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setImageResource(R.drawable.ic_empty_offline);
					emptyTextView.setText(R.string.offline_empty_folder);
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					contentText.setText(getInfoFolder(mOffList));
				}
			}
			else{
				adapter.setAdapterType(MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}

			return v;
		}		
	}

	public void findNodes(){
		log("findNodes");

		if((getActivity() == null) || (!isAdded())){
			log("Fragment NOT Attached!");
			return;
		}

		mOffList=dbH.findByPath(pathNavigation);

		log("Number of elements: "+mOffList.size());

		for(int i=0; i<mOffList.size();i++){

			MegaOffline checkOffline = mOffList.get(i);
			File offlineDirectory = null;
			if(checkOffline.getOrigin()==MegaOffline.INCOMING){

				log("isIncomingOffline");

				if (Environment.getExternalStorageDirectory() != null){
					offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +checkOffline.getHandleIncoming() + "/" + checkOffline.getPath()+checkOffline.getName());
					log("offlineDirectory: "+offlineDirectory);
				}
				else{
					offlineDirectory = context.getFilesDir();
				}
			}
			else if(checkOffline.getOrigin()==MegaOffline.INBOX){

				if (Environment.getExternalStorageDirectory() != null){
					offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/" + checkOffline.getPath()+checkOffline.getName());
					log("offlineDirectory: "+offlineDirectory);
				}
				else{
					offlineDirectory = context.getFilesDir();
				}
			}
			else{
				log("FROM other origin");

				if (Environment.getExternalStorageDirectory() != null){
					offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + checkOffline.getPath()+checkOffline.getName());
				}
				else{
					offlineDirectory = context.getFilesDir();
				}

				if (!offlineDirectory.exists()){
					log("Not exists for not incoming offline");

					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in" + checkOffline.getPath()+checkOffline.getName());
					}
					else{
						offlineDirectory = context.getFilesDir();
					}
				}
			}

			if(offlineDirectory!=null){
				if (!offlineDirectory.exists()){
					log("Path to remove B: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
					//dbH.removeById(mOffList.get(i).getId());
					mOffList.remove(i);
					i--;
				}
			}
		}

		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}

		if(adapter!=null){
			adapter.setNodes(mOffList);

			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);

			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setImageResource(R.drawable.ic_empty_offline);
				emptyTextView.setText(R.string.offline_empty_folder);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				contentText.setText(getInfoFolder(mOffList));
			}
		}
	}

	public void sortByNameDescending(){
		
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();
		
		//Remove MK before sorting
		if(mOffList.size()>0){
			MegaOffline lastItem = mOffList.get(mOffList.size()-1);
			if(lastItem.getHandle().equals("0")){
				mOffList.remove(mOffList.size()-1);
			}
		}		
		else{
			return;
		}
		
		for(int k = 0; k < mOffList.size() ; k++) {
			MegaOffline node = mOffList.get(k);
			if(node.getType().equals("1")){
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
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));
				}				
			}
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));					
				}				
			}
		}
		
		mOffList.clear();
		mOffList.addAll(tempOffline);

		contentText.setText(getInfoFolder(mOffList));
	}

	
	public void sortByNameAscending(){
		log("sortByNameAscending");
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();
		
		//Remove MK before sorting
		if(mOffList.size()>0){
			MegaOffline lastItem = mOffList.get(mOffList.size()-1);
			if(lastItem.getHandle().equals("0")){
				mOffList.remove(mOffList.size()-1);
			}
		}		
		else{
			return;
		}
				
		for(int k = 0; k < mOffList.size() ; k++) {
			MegaOffline node = mOffList.get(k);
			if(node.getType().equals("1")){
				foldersOrder.add(node.getName());
			}
			else{
				filesOrder.add(node.getName());
			}
		}		
	
		Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);

		for(int k = 0; k < foldersOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));
				}				
			}			
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));
				}				
			}
			
		}
		
		mOffList.clear();
		mOffList.addAll(tempOffline);
		contentText.setText(getInfoFolder(mOffList));
	}
	
//	public void updateView (){
//		log("updateView");
//		mOffList=dbH.findByPath(pathNavigation);
//		
//		for(int i=0; i<mOffList.size();i++){
//			
//			File offlineDirectory = null;
//			if (Environment.getExternalStorageDirectory() != null){
//				offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + mOffList.get(i).getPath()+mOffList.get(i).getName());
//			}
//			else{
//				offlineDirectory = context.getFilesDir();
//			}	
//			
//			if (!offlineDirectory.exists()){
//				dbH.removeById(mOffList.get(i).getId());
//				mOffList.remove(i);
//				
//			}			
//		}
//		this.setNodes(mOffList);
//	}
//	

	public boolean isFolder(String path){
		MegaNode n = megaApi.getNodeByPath(path);
		if(n == null)
		{
			return false;
		}
		
		if(n.isFile()){
			return false;
		}
		else{
			return true;
		}		
	}
	
	private String getInfoFolder(ArrayList<MegaOffline> mOffInfo) {
		log("getInfoFolder");
//		log("primer elemento: "+mOffInfo.get(0).getName());
		
		String info = "";
		int numFolders=0;
		int numFiles=0;
		
		String pathI=null;
		
		if(mOffInfo.size()>0){
			if(mOffInfo.get(0).getOrigin()==MegaOffline.INCOMING){
				pathI = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + mOffInfo.get(0).getHandleIncoming() + "/";
			}
			else if(mOffInfo.get(0).getOrigin()==MegaOffline.INBOX){
				pathI = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/";
			}
			else{
				pathI= Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR;
			}	
			
			for(int i=0; i<mOffInfo.size();i++){
				MegaOffline mOff = (MegaOffline) mOffInfo.get(i);
				String path = pathI + mOff.getPath() + mOff.getName();			

				File destination = new File(path);
				if (destination.exists()){
					if(destination.isFile()){
						numFiles++;					
					}
					else{
						numFolders++;					
					}
				}
				else{
					log("File do not exist");
				}		
			}
		}
		
		//Check if the file MarterKey is exported
		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
		log("Export in: "+path);
		File file= new File(path);
		if(file.exists()){
			numFiles++;
		}
		
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

		log(info);
		return info;
	}

	@Override
    public void onAttach(Activity activity) {
		log("onAttach");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

    public void itemClick(int position) {
		log("itemClick");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (adapter.isMultipleSelect()){
			log("multiselect");
			MegaOffline item = mOffList.get(position);
			if(!(item.getHandle().equals("0"))){
				adapter.toggleSelection(position);
				List<MegaOffline> selectedNodes = adapter.getSelectedOfflineNodes();
				if (selectedNodes.size() > 0){
					updateActionModeTitle();
					((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);

				}
			}
		}
		else{
			MegaOffline currentNode = mOffList.get(position);
			
			File currentFile=null;
			
			if(currentNode.getHandle().equals("0")){
				log("click on Master Key");
				String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
				openFile(new File(path));
//				viewIntent.setDataAndType(Uri.fromFile(new File(path)), MimeTypeList.typeForName("MEGAMasterKey.txt").getType());
//				((ManagerActivityLollipop)context).clickOnMasterKeyFile();
				return;
			}
						
			if(currentNode.getOrigin()==MegaOffline.INCOMING){
				String handleString = currentNode.getHandleIncoming();
				currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + handleString + "/"+currentNode.getPath() + "/" + currentNode.getName());
			}
			else if(currentNode.getOrigin()==MegaOffline.INBOX){
				String handleString = currentNode.getHandleIncoming();
				currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/"+currentNode.getPath() + "/" + currentNode.getName());
			}
			else{
				currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode.getPath() + "/" + currentNode.getName());
			}
			
			if(currentFile.exists() && currentFile.isDirectory()){

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

				aB.setTitle(currentNode.getName());
				pathNavigation= currentNode.getPath()+ currentNode.getName()+"/";	
				
				if (context instanceof ManagerActivityLollipop){
					log("aB.setHomeAsUpIndicator_31");
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					((ManagerActivityLollipop)context).setPathNavigationOffline(pathNavigation);
				}

				mOffList=dbH.findByPath(currentNode.getPath()+currentNode.getName()+"/");
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);						
				}
				else{
					File offlineDirectory = null;
					String path;

					if(currentNode.getOrigin()==MegaOffline.INCOMING){
						path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + currentNode.getHandleIncoming();
					}
					else if(currentNode.getOrigin()==MegaOffline.INBOX){
						path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in";
					}
					else{							
						path= Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR;
						log("Path NOT INCOMING: "+path);							
					}						
											
					for(int i=0; i<mOffList.size();i++){
						
						if (Environment.getExternalStorageDirectory() != null){								
							log("mOffList path: "+path+mOffList.get(i).getPath());
							offlineDirectory = new File(path + mOffList.get(i).getPath()+mOffList.get(i).getName());
						}
						else{
							offlineDirectory = context.getFilesDir();
						}	

						if (!offlineDirectory.exists()){
							//Updating the DB because the file does not exist	
							log("Path to remove C: "+(path + mOffList.get(i).getName()));
							dbH.removeById(mOffList.get(i).getId());
							mOffList.remove(i);
							i--;
						}			
					}
				}
				contentText.setText(getInfoFolder(mOffList));
				adapter.setNodes(mOffList);
				
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByNameDescending();
				}
				else{
					sortByNameAscending();
				}
				
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setImageResource(R.drawable.ic_empty_offline);
					emptyTextView.setText(R.string.offline_empty_folder);
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
				
				adapter.setPositionClicked(-1);
				recyclerView.scrollToPosition(0);
				notifyDataSetChanged();

			}
			else{
				if(currentFile.exists() && currentFile.isFile()){			
					
					//Open it!
					if (MimeTypeList.typeForName(currentFile.getName()).isImage()){
						Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
						intent.putExtra("position", position);
						intent.putExtra("adapterType", Constants.OFFLINE_ADAPTER);
						intent.putExtra("parentNodeHandle", -1L);
						intent.putExtra("offlinePathDirectory", currentFile.getParent());
						intent.putExtra("pathNavigation", pathNavigation);
						intent.putExtra("orderGetChildren", orderGetChildren);

						if (context instanceof ManagerActivityLollipop){
							MyAccountInfo accountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
							if(accountInfo!=null){
								intent.putExtra("typeAccount", accountInfo.getAccountType());
							}
						}

						startActivity(intent);
					}
					else{
						openFile(currentFile);
					}					
				}					
			}
		}
    }
    
    public void openFile (File currentFile){
    	Intent viewIntent = new Intent(Intent.ACTION_VIEW);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			viewIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
		}
		else{
			viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
		}
		viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if (MegaApiUtils.isIntentAvailable(context, viewIntent)){
			context.startActivity(viewIntent);
		}
		else{
			Intent intentShare = new Intent(Intent.ACTION_SEND);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
			}
			else{
				intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
			}
			intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			if (MegaApiUtils.isIntentAvailable(context, intentShare)){
				context.startActivity(intentShare);
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
		log("updateActionModeTitle");
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaOffline> documents = adapter.getSelectedOfflineNodes();
		int folders=0;
		int files=0;
		
		String pathI=null;
		
		if(documents.size()>0){
			if(documents.get(0).getOrigin()==MegaOffline.INCOMING){
				pathI = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + documents.get(0).getHandleIncoming() + "/";
			}
			else if(documents.get(0).getOrigin()==MegaOffline.INBOX){
				pathI = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/";
			}
			else{
				pathI= Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR;
			}	
			
			for(int i=0; i<documents.size();i++){
				MegaOffline mOff = (MegaOffline) documents.get(i);
				String path = pathI + mOff.getPath() + mOff.getName();			

				File destination = new File(path);
				if (destination.exists()){
					if(destination.isFile()){
						files++;					
					}
					else{
						folders++;					
					}
				}
				else{
					log("File do not exist");
				}		
			}
		}
		
		Resources res = getActivity().getResources();
		/*String format = "%d %s";
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
		}*/

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
		log("hideMultipleSelect");
		adapter.setMultipleSelect(false);
		((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_TRANSPARENT_BLACK);

		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		log("onBackPressed");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (adapter == null){
			return 0;
		}

		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else if(pathNavigation != null){
			if(pathNavigation.isEmpty()){
				return 0;
			}
			if (!pathNavigation.equals("/")){
				log("onBackPress: "+pathNavigation);
				pathNavigation=pathNavigation.substring(0,pathNavigation.length()-1);
				log("substring: "+pathNavigation);
				int index=pathNavigation.lastIndexOf("/");				
				pathNavigation=pathNavigation.substring(0,index+1);
				
				if (context instanceof ManagerActivityLollipop){
					((ManagerActivityLollipop)context).setPathNavigationOffline(pathNavigation);
					
					if (pathNavigation.equals("/")){
						aB.setTitle(getString(R.string.section_saved_for_offline));
						log("aB.setHomeAsUpIndicator_32");
						aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
						((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
						((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					}
					else{
						String title = pathNavigation;
						index=title.lastIndexOf("/");				
						title=title.substring(0,index);
						index=title.lastIndexOf("/");				
						title=title.substring(index+1,title.length());			
						aB.setTitle(title);
						log("aB.setHomeAsUpIndicator_33");
						aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
						((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
						((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					}
				}
				ArrayList<MegaOffline> mOffListNavigation= new ArrayList<MegaOffline>();				
				mOffListNavigation=dbH.findByPath(pathNavigation);
				
				contentText.setText(getInfoFolder(mOffListNavigation));
				
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByNameDescending();
				}
				else{
					sortByNameAscending();
				}

				setNodes(mOffListNavigation);

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

				//adapterList.setNodes(mOffList);
				
				return 2;
			}
			else{
				log("pathNavigation  / ");
				return 0;
			}
		}
		else{
				return 0;
		}
	}

	public void setTitle(){
		log("setTitle");

		if((getActivity() == null) || (!isAdded())){
			log("Fragment NOT Attached!");
			return;
		}

		pathNavigation = ((ManagerActivityLollipop)context).getPathNavigationOffline();
		if (pathNavigation!=null){
			if (pathNavigation.equals("/")){
				aB.setTitle(getString(R.string.section_saved_for_offline));
				log("aB.setHomeAsUpIndicator_0356");
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}
			else{
				String title = pathNavigation;
				int index=title.lastIndexOf("/");
				title=title.substring(0,index);
				index=title.lastIndexOf("/");
				title=title.substring(index+1,title.length());
				aB.setTitle(title);
				log("aB.setHomeAsUpIndicator_4528");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}
		}
		else{
			aB.setTitle(getString(R.string.section_saved_for_offline));
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		}
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
	}
	
	public void setNodes(ArrayList<MegaOffline> _mOff){
		log("setNodes");
		this.mOffList = _mOff;

		if (adapter != null){
			adapter.setNodes(mOffList);
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				emptyImageView.setImageResource(R.drawable.ic_empty_offline);
				emptyTextView.setText(R.string.offline_empty_folder);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}			
		}
	}
	
	public void setPositionClicked(int positionClicked){
		log("setPositionClicked");
		if (adapter != null){
			adapter.setPositionClicked(positionClicked);
		}
	}
	
	public void notifyDataSetChanged(){
		log("notifyDataSetChanged");
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}

	public void refresh(){
		log("refresh");

		mOffList=dbH.findByPath(pathNavigation);

		if (adapter == null){
			adapter = new MegaOfflineLollipopAdapter(this, context, mOffList, recyclerView, emptyImageView, emptyTextView, aB, MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			contentText.setText(getInfoFolder(mOffList));
			adapter.setNodes(mOffList);
			adapter.setAdapterType(MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);
		}
		else{
			contentText.setText(getInfoFolder(mOffList));
			adapter.setNodes(mOffList);
		}

		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}

		if (adapter.getItemCount() == 0){
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			contentTextLayout.setVisibility(View.GONE);
			emptyImageView.setImageResource(R.drawable.ic_empty_offline);
			emptyTextView.setText(R.string.offline_empty_folder);
		}
		else{
			recyclerView.setVisibility(View.VISIBLE);
			contentTextLayout.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			contentText.setText(getInfoFolder(mOffList));
		}

		setPositionClicked(-1);
		notifyDataSetChanged();
	}
	
	public void refreshPaths(){
		log("refreshPaths()");
		mOffList=dbH.findByPath("/");	
		
		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}
		
//		setNodes(mOffList);
		recyclerView.invalidate();
	}
	
	public void refreshPaths(MegaOffline mOff){
		log("refreshPaths(MegaOffline mOff): "+mOff.getName());
		int index=0;
//		MegaOffline retFindPath = null;
		
		//Find in the tree, the last existing node
		String pNav= mOff.getPath();
		
		if(mOff.getType()==DB_FILE){
			
			index=pNav.lastIndexOf("/");				
			pNav=pNav.substring(0,index+1);
			
		}
		else{
			pNav=pNav.substring(0,pNav.length()-1);
		}	
			
		if(pNav.length()==0){
			mOffList=dbH.findByPath("/");
			pNav="/";
		}
		else{
			findPath(pNav);			
		}
				
		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}
		
//		setNodes(mOffList);
//		pathNavigation=pNav;
		
		if(pathNavigation.equals("/")){
			mOffList=dbH.findByPath("/");
			aB.setTitle(getString(R.string.section_saved_for_offline));	
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
		}
		else{
			log("-------refreshPaths: "+pathNavigation);
			mOffList=dbH.findByPath(pathNavigation);
			index=pathNavigation.lastIndexOf("/");	
			String title = pathNavigation;
			index=title.lastIndexOf("/");				
			title=title.substring(0,index);
			index=title.lastIndexOf("/");				
			title=title.substring(index+1,title.length());			
			aB.setTitle(title);			
		}		
		
		log("At the end of refreshPaths the path is: "+pathNavigation);
		contentText.setText(getInfoFolder(mOffList));
		setNodes(mOffList);
	}
	
	public int getItemCount(){
		if(adapter != null){
			return adapter.getItemCount();
		}
		return 0;
	}
	
	private void findPath (String pNav){
		
		log("findPath:" + pNav);	

		MegaOffline nodeToShow = null;
		
		if(!pNav.equals("/")){
			
			if (pNav.endsWith("/")) {
				pNav = pNav.substring(0, pNav.length() - 1);
				log("NEW findPath:" + pNav);		
			}
			
			int index=pNav.lastIndexOf("/");	
			String pathToShow = pNav.substring(0, index+1);
			log("Path: "+ pathToShow);
			String nameToShow = pNav.substring(index+1, pNav.length());
			log("Name: "+ nameToShow);
			
			nodeToShow = dbH.findbyPathAndName(pathToShow, nameToShow);
			if(nodeToShow!=null){
				//Show the node
				log("findPath:Node: "+ nodeToShow.getName());
				pathNavigation=pathToShow+nodeToShow.getName()+"/";
				log("findPath:pathNavigation: "+pathNavigation);
				return;
			}
			else{
				if(pathNavigation.equals("/")){
					log("Return Path /");
					return;
				}
				else{
					findPath(pathToShow);
				}				
			}
		}
		else{
			pathNavigation="/";
		}		
	}	
	
	public void setPathNavigation(String _pathNavigation){
		log("setPathNavigation: "+pathNavigation);
		this.pathNavigation = _pathNavigation;
		if (adapter != null){	
//			contentText.setText(getInfoFolder(mOffList));
			adapter.setNodes(dbH.findByPath(_pathNavigation));
		}
	}
	
	public void setIsList(boolean isList){
		log("setIsList");
		this.isList = isList;
	}
	
	public boolean getIsList(){
		log("getIsList");
		return isList;
	}
	
	public void setOrder(int orderGetChildren){
		log("setOrder");
		this.orderGetChildren = orderGetChildren;
	}
	
	private static void log(String log) {
		Util.log("OfflineFragmentLollipop", log);
	}

	public String getPathNavigation() {
		return pathNavigation;
	}
}