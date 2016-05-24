package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.SlidingUpPanelLayout;
import mega.privacy.android.app.components.SlidingUpPanelLayout.PanelState;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

public class OfflineFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{
	
	static int FROM_OFFLINE= 15;

	MegaPreferences prefs;
	
	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	GestureDetectorCompat detector;
	RecyclerView.LayoutManager mLayoutManager;
	MegaOffline selectedNode = null;
	
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
	
	//OPTIONS PANEL
	private SlidingUpPanelLayout slidingOptionsPanel;
	public FrameLayout optionsOutLayout;
	public LinearLayout optionsLayout;
	public LinearLayout optionDownload;
	public LinearLayout optionProperties;
	public LinearLayout optionRename;
	public LinearLayout optionPublicLink;
	public LinearLayout optionShare;
	public LinearLayout optionDelete;
	public LinearLayout optionMoveTo;
	public LinearLayout optionCopyTo;	
	public TextView propertiesText;
	////
	
	private ActionMode actionMode;
	
	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

	    public void onLongPress(MotionEvent e) {
	        View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
	        int position = recyclerView.getChildPosition(view);
	        MegaOffline currentNode = (MegaOffline) adapter.getItem(position);
	        if(currentNode.getHandle().equals("0")){
	        	String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
				File file= new File(path);
				if(file.exists()){
					return;
				}
	        }

	        // handle long press
			if (!adapter.isMultipleSelect()){
				adapter.setMultipleSelect(true);
			
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());			

		        itemClick(position);
			}  
	        super.onLongPress(e);
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
					clearSelections();
					hideMultipleSelect();
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList);
					break;
				}
				case R.id.cab_menu_rename:{
					clearSelections();
					hideMultipleSelect();
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
				
					clearSelections();
					hideMultipleSelect();
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
					clearSelections();
					hideMultipleSelect();
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
					clearSelections();
					hideMultipleSelect();
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
					clearSelections();
					hideMultipleSelect();
					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
					break;
				}
				case R.id.cab_menu_delete:{
					
					for (int i=0;i<documents.size();i++){						
						deleteOffline(context, documents.get(i));
					}					
					clearSelections();
					hideMultipleSelect();
					refreshPaths(documents.get(0));
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
			log("ActionBarCallBack::onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.offline_browser_action, menu);
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			adapter.setMultipleSelect(false);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("ActionBarCallBack::onPrepareActionMode");
			List<MegaOffline> selected = adapter.getSelectedOfflineNodes();
			
			if (Util.isOnline(context)){
				if (selected.size() != 0) {
					
					menu.findItem(R.id.cab_menu_download).setVisible(true);
					menu.findItem(R.id.cab_menu_share).setVisible(true);
					
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
					menu.findItem(R.id.cab_menu_share_link).setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_share_link).setVisible(false);
				}
				
				menu.findItem(R.id.cab_menu_copy).setVisible(false);
				menu.findItem(R.id.cab_menu_move).setVisible(false);				
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_rename).setVisible(false);
			}
			else{
				if (selected.size() != 0) {
					
					menu.findItem(R.id.cab_menu_delete).setVisible(true);
				
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
	
	public int deleteOffline(Context context,MegaOffline node){
		
		log("deleteOffline");

//		dbH = new DatabaseHandler(context);
		dbH = DatabaseHandler.getDbHandler(context);

		ArrayList<MegaOffline> mOffListParent=new ArrayList<MegaOffline>();
		ArrayList<MegaOffline> mOffListChildren=new ArrayList<MegaOffline>();			
		MegaOffline parentNode = null;	
		
		//Delete children
		mOffListChildren=dbH.findByParentId(node.getId());
		if(mOffListChildren.size()>0){
			//The node have childrens, delete
			deleteChildrenDB(mOffListChildren);			
		}
		
		log("Remove the node physically");
		//Remove the node physically
		File destination = null;								
		//Check if the node is incoming
		if(node.isIncoming()){
			if (Environment.getExternalStorageDirectory() != null){
				destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + node.getHandleIncoming() + node.getPath());
			}
			else{
				destination = context.getFilesDir();
			}						
		}
		else{
			if (Environment.getExternalStorageDirectory() != null){
				destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + node.getPath());
			}
			else{
				destination = context.getFilesDir();
			}
		}

		try{
			File offlineFile = new File(destination, node.getName());	
			log("Delete in phone: "+node.getName());
			Util.deleteFolderAndSubfolders(context, offlineFile);
		}
		catch(Exception e){
			log("EXCEPTION: deleteOffline - adapter");
		};		
		
		dbH.removeById(node.getId());
		
		//Check if the parent has to be deleted
		
		int parentId = node.getParentId();
		parentNode = dbH.findById(parentId);
	
		if(parentNode != null){
			log("Parent to check: "+parentNode.getName());
			checkParentDeletion(parentNode);			
		}	
		
		return 1;		
	}
	
	public void checkParentDeletion (MegaOffline parentToDelete){
		log("checkParentDeletion: "+parentToDelete.getName());
		
		ArrayList<MegaOffline> mOffListChildren=dbH.findByParentId(parentToDelete.getId());
		File destination = null;	
		if(mOffListChildren.size()<=0){
			log("The parent has NO children");
			//The node have NO childrens, delete it
			
			dbH.removeById(parentToDelete.getId());
			if(parentToDelete.isIncoming()){
				if (Environment.getExternalStorageDirectory() != null){
					destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + parentToDelete.getHandleIncoming() + parentToDelete.getPath());
				}
				else{
					destination = context.getFilesDir();
				}						
			}
			else{
				if (Environment.getExternalStorageDirectory() != null){
					destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + parentToDelete.getPath());
				}
				else{
					destination = context.getFilesDir();
				}
			}
			
			try{
				File offlineFile = new File(destination, parentToDelete.getName());	
				log("Delete in phone: "+parentToDelete.getName());
				Util.deleteFolderAndSubfolders(context, offlineFile);
			}
			catch(Exception e){
				log("EXCEPTION: deleteOffline - adapter");
			};				
	
			int parentId = parentToDelete.getParentId();
			if(parentId==-1){
				File rootIncomingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + parentToDelete.getHandleIncoming());
				
				if(rootIncomingFile.list().length==0){
					try{
						rootIncomingFile.delete();
					}
					catch(Exception e){
						log("EXCEPTION: deleteParentIncoming: "+destination);
					};
				}				
			}
			else{
				//Check if the parent has to be deleted	
				
				parentToDelete = dbH.findById(parentId);			
				if(parentToDelete != null){
					log("Parent to check: "+parentToDelete.getName());
					checkParentDeletion(parentToDelete);
						
				}
			}

		}
		else{
			log("The parent has children!!! RETURN!!");
			return;
		}
		
	}

	public void deleteChildrenDB(ArrayList<MegaOffline> mOffListChildren){

		log("deleteChildenDB: "+mOffListChildren.size());
		MegaOffline mOffDelete=null;

		for(int i=0; i<mOffListChildren.size(); i++){	

			mOffDelete=mOffListChildren.get(i);

			log("Children "+i+ ": "+ mOffDelete.getName());
			ArrayList<MegaOffline> mOffListChildren2=dbH.findByParentId(mOffDelete.getId());
			if(mOffListChildren2.size()>0){
				//The node have children, delete				
				deleteChildrenDB(mOffListChildren2);				
			}	

			int lines = dbH.removeById(mOffDelete.getId());		
			log("Borradas; "+lines);
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
		else{
			prefs = dbH.getPreferences();		
			if (prefs != null){
				isList = Boolean.parseBoolean(prefs.getPreferredViewList());
				if(prefs.getPreferredSortOthers()!=null){
					orderGetChildren = Integer.parseInt(prefs.getPreferredSortOthers());
					log("The orderOthers preference is: "+orderGetChildren);
				}
				else{
					orderGetChildren = megaApi.ORDER_DEFAULT_ASC;
					log("Preference orderOthers is NULL -> ORDER_DEFAULT_ASC");
				}
			}
			else{
				isList = true;
				orderGetChildren = megaApi.ORDER_DEFAULT_ASC;
			}
			((OfflineActivityLollipop)context).setListOffline(isList);
		}	
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;	    

		//Check pathNAvigation
		if (isList){
			log("onCreateList");
			View v = inflater.inflate(R.layout.fragment_offlinelist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.offline_view_browser);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.addOnItemTouchListener(this);
			recyclerView.setItemAnimator(new DefaultItemAnimator()); 
			
			emptyImageView = (ImageView) v.findViewById(R.id.offline_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.offline_empty_text);		
					
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.offline_content_text_layout);

			contentText = (TextView) v.findViewById(R.id.offline_content_text);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(73, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);

			mOffList=dbH.findByPath(pathNavigation);
			
			log("Number of elements: "+mOffList.size());
									
			for(int i=0; i<mOffList.size();i++){
				
				MegaOffline checkOffline = mOffList.get(i);
				
				if(!checkOffline.isIncoming()){				
					log("NOT isIncomingOffline");
					File offlineDirectory = null;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + checkOffline.getPath()+checkOffline.getName());
					}
					else{
						offlineDirectory = context.getFilesDir();
					}	
					
					if (!offlineDirectory.exists()){
						log("Path to remove A: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
						//dbH.removeById(mOffList.get(i).getId());
						mOffList.remove(i);		
						i--;
					}	
				}
				else{
					log("isIncomingOffline");
					File offlineDirectory = null;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +checkOffline.getHandleIncoming() + "/" + checkOffline.getPath()+checkOffline.getName());
						log("offlineDirectory: "+offlineDirectory);
					}
					else{
						offlineDirectory = context.getFilesDir();
					}	
					
					if (!offlineDirectory.exists()){
						log("Path to remove B: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
						//dbH.removeById(mOffList.get(i).getId());
						mOffList.remove(i);
						i--;
					}	
					
				}
			}
			
			if (adapter == null){
				adapter = new MegaOfflineLollipopAdapter(this, context, mOffList, recyclerView, emptyImageView, emptyTextView, aB, MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				contentText.setText(getInfoFolder(mOffList));
				adapter.setNodes(mOffList);
				adapter.setAdapterType(MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);
						
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setImageResource(R.drawable.ic_empty_offline);
				emptyTextView.setText(R.string.file_browser_empty_folder);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				contentText.setText(getInfoFolder(mOffList));
			}			
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout);
			optionsLayout = (LinearLayout) v.findViewById(R.id.offline_list_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.offline_list_out_options);
			optionRename = (LinearLayout) v.findViewById(R.id.offline_list_option_rename_layout);
			optionRename.setVisibility(View.GONE);
			
			optionDownload = (LinearLayout) v.findViewById(R.id.offline_list_option_download_layout);
			optionProperties = (LinearLayout) v.findViewById(R.id.offline_list_option_properties_layout);
			propertiesText = (TextView) v.findViewById(R.id.offline_list_option_properties_text);			

			optionPublicLink = (LinearLayout) v.findViewById(R.id.offline_list_option_public_link_layout);
//				holder.optionPublicLink.getLayoutParams().width = Util.px2dp((60), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);

			optionShare = (LinearLayout) v.findViewById(R.id.offline_list_option_share_layout);			
			optionDelete = (LinearLayout) v.findViewById(R.id.offline_list_option_delete_layout);			

//				holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);

			optionMoveTo = (LinearLayout) v.findViewById(R.id.offline_list_option_move_layout);	
			optionMoveTo.setVisibility(View.GONE);
			optionCopyTo = (LinearLayout) v.findViewById(R.id.offline_list_option_copy_layout);			
			optionCopyTo.setVisibility(View.GONE);
			
			optionDownload.setOnClickListener(this);
			optionShare.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionRename.setOnClickListener(this);
			optionDelete.setOnClickListener(this);
			optionPublicLink.setOnClickListener(this);
			optionMoveTo.setOnClickListener(this);
			optionCopyTo.setOnClickListener(this);
			
			optionsOutLayout.setOnClickListener(this);
			
			slidingOptionsPanel.setVisibility(View.INVISIBLE);
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);		
			
			slidingOptionsPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
	            @Override
	            public void onPanelSlide(View panel, float slideOffset) {
	            	log("onPanelSlide, offset " + slideOffset);
//	            	if(slideOffset==0){
//	            		hideOptionsPanel();
//	            	}
	            }

	            @Override
	            public void onPanelExpanded(View panel) {
	            	log("onPanelExpanded");

	            }

	            @Override
	            public void onPanelCollapsed(View panel) {
	            	log("onPanelCollapsed");
	            	

	            }

	            @Override
	            public void onPanelAnchored(View panel) {
	            	log("onPanelAnchored");
	            }

	            @Override
	            public void onPanelHidden(View panel) {
	                log("onPanelHidden");                
	            }
	        });			
		
			return v;
		}
		else{
			log("onCreateGRID");
			View v = inflater.inflate(R.layout.fragment_offlinegrid, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.offline_view_browser_grid);
			recyclerView.setHasFixedSize(true);
			final GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
			gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
				@Override
			      public int getSpanSize(int position) {
					return 1;
				}
			});
			
			recyclerView.addOnItemTouchListener(this);
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			
			emptyImageView = (ImageView) v.findViewById(R.id.offline_empty_image_grid);
			emptyTextView = (TextView) v.findViewById(R.id.offline_empty_text_grid);		

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.offline_content_grid_text_layout);

			contentText = (TextView) v.findViewById(R.id.offline_content_text_grid);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(73, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);

			mOffList=dbH.findByPath(pathNavigation);
			
			log("Number of elements: "+mOffList.size());
									
			for(int i=0; i<mOffList.size();i++){
				
				MegaOffline checkOffline = mOffList.get(i);
				
				if(!checkOffline.isIncoming()){				
					log("NOT isIncomingOffline");
					File offlineDirectory = null;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + checkOffline.getPath()+checkOffline.getName());
					}
					else{
						offlineDirectory = context.getFilesDir();
					}	
					
					if (!offlineDirectory.exists()){
						log("Path to remove A: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
						//dbH.removeById(mOffList.get(i).getId());
						mOffList.remove(i);		
						i--;
					}	
				}
				else{
					log("isIncomingOffline");
					File offlineDirectory = null;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +checkOffline.getHandleIncoming() + "/" + checkOffline.getPath()+checkOffline.getName());
						log("offlineDirectory: "+offlineDirectory);
					}
					else{
						offlineDirectory = context.getFilesDir();
					}	
					
					if (!offlineDirectory.exists()){
						log("Path to remove B: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
						//dbH.removeById(mOffList.get(i).getId());
						mOffList.remove(i);
						i--;
					}	
					
				}
			}
			
			if (adapter == null){

				adapter = new MegaOfflineLollipopAdapter(this, context, mOffList, recyclerView, emptyImageView, emptyTextView, aB, MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				contentText.setText(getInfoFolder(mOffList));
				adapter.setNodes(mOffList);
				adapter.setAdapterType(MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);
						
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setImageResource(R.drawable.ic_empty_offline);
				emptyTextView.setText(R.string.file_browser_empty_folder);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				contentText.setText(getInfoFolder(mOffList));
			}			
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout_offline_grid);
			optionsLayout = (LinearLayout) v.findViewById(R.id.offline_grid_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.offline_grid_out_options);
			optionRename = (LinearLayout) v.findViewById(R.id.offline_grid_option_rename_layout);
			optionRename.setVisibility(View.GONE);
			
			optionDownload = (LinearLayout) v.findViewById(R.id.offline_grid_option_download_layout);
			optionProperties = (LinearLayout) v.findViewById(R.id.offline_grid_option_properties_layout);
			propertiesText = (TextView) v.findViewById(R.id.offline_grid_option_properties_text);			

			optionPublicLink = (LinearLayout) v.findViewById(R.id.offline_grid_option_public_link_layout);
//				holder.optionPublicLink.getLayoutParams().width = Util.px2dp((60), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);

			optionShare = (LinearLayout) v.findViewById(R.id.offline_grid_option_share_layout);			
			optionDelete = (LinearLayout) v.findViewById(R.id.offline_grid_option_delete_layout);			

//				holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);

			optionMoveTo = (LinearLayout) v.findViewById(R.id.offline_grid_option_move_layout);	
			optionMoveTo.setVisibility(View.GONE);
			optionCopyTo = (LinearLayout) v.findViewById(R.id.offline_grid_option_copy_layout);			
			optionCopyTo.setVisibility(View.GONE);
			
			optionDownload.setOnClickListener(this);
			optionShare.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionRename.setOnClickListener(this);
			optionDelete.setOnClickListener(this);
			optionPublicLink.setOnClickListener(this);
			optionMoveTo.setOnClickListener(this);
			optionCopyTo.setOnClickListener(this);
			
			optionsOutLayout.setOnClickListener(this);
			
			slidingOptionsPanel.setVisibility(View.INVISIBLE);
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);		
			
			slidingOptionsPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
	            @Override
	            public void onPanelSlide(View panel, float slideOffset) {
	            	log("onPanelSlide, offset " + slideOffset);
//	            	if(slideOffset==0){
//	            		hideOptionsPanel();
//	            	}
	            }

	            @Override
	            public void onPanelExpanded(View panel) {
	            	log("onPanelExpanded");

	            }

	            @Override
	            public void onPanelCollapsed(View panel) {
	            	log("onPanelCollapsed");
	            	

	            }

	            @Override
	            public void onPanelAnchored(View panel) {
	            	log("onPanelAnchored");
	            }

	            @Override
	            public void onPanelHidden(View panel) {
	                log("onPanelHidden");                
	            }
	        });			
		
			return v;
		}		
	}
	
	public void download (String path){
		
		if (Util.isOnline(context)){
			ArrayList<Long> handleList = new ArrayList<Long>();
			MegaNode node = megaApi.getNodeByPath(path);
			if(node == null)
			{
				return;
			}
			
			handleList.add(node.getHandle());
			log("download "+node.getName());
			if (context instanceof ManagerActivityLollipop){
				NodeController nC = new NodeController(context);
				nC.prepareForDownload(handleList);
			}
			else{
				//TODO toast no connection
			}			
		}
		else{
			//TODO toast no connection
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
		adapter.setNodes(mOffList);
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
		adapter.setNodes(mOffList);
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
	public void showProperties (String path, String handle){
		log("showProperties: "+path);
		
		MegaNode n = megaApi.getNodeByHandle(Long.valueOf(handle));		
		if(n == null)
		{
			return;
		}
		
		Intent i = new Intent(context, FilePropertiesActivityLollipop.class);
		i.putExtra("handle", Long.valueOf(handle));
		i.putExtra("from", FROM_OFFLINE);

		if (n.isFolder()) {
			if (megaApi.isShared(n)){
				i.putExtra("imageId", R.drawable.folder_shared_mime);	
			}
			else{
				i.putExtra("imageId", R.drawable.folder_mime);
			}

		} 
		else {
			i.putExtra("imageId", MimeTypeMime.typeForName(n.getName()).getIconResourceId());
		}
		i.putExtra("name", n.getName());
		context.startActivity(i);
	}
	
	public void getLink (String path){
		MegaNode n = megaApi.getNodeByPath(path);
		if(n == null)
		{
			return;
		}
		NodeController nC = new NodeController(context);
		nC.exportLink(n);
	}
	
	public void shareFolder (String path){
		MegaNode n = megaApi.getNodeByPath(path);
		if(n == null)
		{
			return;
		}
		NodeController nC = new NodeController(context);
		nC.selectContactToShareFolder(n);
	}
	
	public void rename (String path){
		MegaNode n = megaApi.getNodeByPath(path);
		if(n == null)
		{
			return;
		}
		
		((ManagerActivityLollipop) context).showRenameDialog(n, n.getName());
	}
	
	public void move (String path){
		MegaNode n = megaApi.getNodeByPath(path);
		if(n == null)
		{
			return;
		}
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(n.getHandle());
		NodeController nC = new NodeController(context);
		nC.chooseLocationToMoveNodes(handleList);
	}
	
	public void copy (String path){
		MegaNode n = megaApi.getNodeByPath(path);
		if(n == null)
		{
			return;
		}
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(n.getHandle());
		NodeController nC = new NodeController(context);
		nC.chooseLocationToCopyNodes(handleList);
	}
	
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
			if(mOffInfo.get(0).isIncoming()){
				pathI = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + mOffInfo.get(0).getHandleIncoming() + "/";
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
		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
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
	
	public void showOptionsPanel(MegaOffline sNode){
		log("showNodeOptionsPanel");
		
		this.selectedNode = sNode;
		
		//Check if the node is the Master Key file
        if(selectedNode.getHandle().equals("0")){
        	String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
			File file= new File(path);
			if(file.exists()){
				optionShare.setVisibility(View.GONE);			
				optionDownload.setVisibility(View.GONE);
				optionProperties.setVisibility(View.GONE);				
				optionDelete.setVisibility(View.VISIBLE);
				optionPublicLink.setVisibility(View.GONE);
				optionRename.setVisibility(View.GONE);
				optionMoveTo.setVisibility(View.GONE);
				optionCopyTo.setVisibility(View.GONE);
				
				slidingOptionsPanel.setVisibility(View.VISIBLE);
				slidingOptionsPanel.setPanelState(PanelState.COLLAPSED);
				log("Show the slidingPanel");
				
				return;
			}
        }
		
		//Check connection or not connection
		
		if (Util.isOnline(context)){
			//With connection
			
			if (selectedNode.getType().equals(DB_FOLDER)) {
				propertiesText.setText(R.string.general_folder_info);
				optionShare.setVisibility(View.VISIBLE);
			}else{
				propertiesText.setText(R.string.general_file_info);
				optionShare.setVisibility(View.GONE);
			}
			
			optionDownload.setVisibility(View.VISIBLE);
			optionProperties.setVisibility(View.VISIBLE);				
			optionDelete.setVisibility(View.VISIBLE);
			optionPublicLink.setVisibility(View.VISIBLE);
			optionRename.setVisibility(View.GONE);
			optionMoveTo.setVisibility(View.GONE);
			optionCopyTo.setVisibility(View.GONE);				
				
		}
		else{
			//No connection
			optionShare.setVisibility(View.GONE);			
			optionDownload.setVisibility(View.GONE);
			optionProperties.setVisibility(View.GONE);				
			optionDelete.setVisibility(View.VISIBLE);
			optionPublicLink.setVisibility(View.GONE);
			optionRename.setVisibility(View.GONE);
			optionMoveTo.setVisibility(View.GONE);
			optionCopyTo.setVisibility(View.GONE);
		}
					
		slidingOptionsPanel.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.COLLAPSED);
		log("Show the slidingPanel");
	}
	
	public void hideOptionsPanel(){
		log("hideOptionsPanel");
				
		adapter.setPositionClicked(-1);
		slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
		slidingOptionsPanel.setVisibility(View.GONE);
	}
	
	public PanelState getPanelState ()
	{
		log("getPanelState: "+slidingOptionsPanel.getPanelState());
		return slidingOptionsPanel.getPanelState();
	}
		
	@Override
    public void onAttach(Activity activity) {
		log("onAttach");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {
		log("onClick");
		switch(v.getId()){
			case R.id.offline_list_out_options:
			case R.id.offline_grid_out_options:{
				hideOptionsPanel();
				break;
			}
			case R.id.offline_list_option_download_layout: 
			case R.id.offline_grid_option_download_layout: {
				log("Download option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(Long.parseLong(selectedNode.getHandle()));
				NodeController nC = new NodeController(context);
				nC.prepareForDownload(handleList);
				break;
			}
			case R.id.offline_list_option_move_layout:
			case R.id.offline_grid_option_move_layout:{
				log("Move option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();			
				if (Util.isOnline(context)){
					String path = selectedNode.getPath() + selectedNode.getName();
					move(path);
				}
				break;
			}
			case R.id.offline_list_option_copy_layout: 
			case R.id.offline_grid_option_copy_layout: {
				log("Copy option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				if (Util.isOnline(context)){
					String path = selectedNode.getPath() + selectedNode.getName();
					copy(path);
				}
				break;
			}
			case R.id.offline_list_option_properties_layout:
			case R.id.offline_grid_option_properties_layout:{
				log("Properties option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				if (Util.isOnline(context)){
					String path = selectedNode.getPath() + selectedNode.getName();
					String handle = selectedNode.getHandle();
					showProperties(path, handle);
				}
				break;
			}
			case R.id.offline_list_option_public_link_layout:
			case R.id.offline_grid_option_public_link_layout:{
				log("Get link");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				if (Util.isOnline(context)){
					String path = selectedNode.getPath() + selectedNode.getName();
					getLink(path);
				}					
				break;
			}
			case R.id.offline_list_option_delete_layout:
			case R.id.offline_grid_option_delete_layout:{
				log("Delete Offline");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				
				if(selectedNode.getHandle().equals("0")){
		        	String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
					File file= new File(path);
					if(file.exists()){
						file.delete();	
						
						mOffList=dbH.findByPath(pathNavigation);
						
						log("Number of elements: "+mOffList.size());
												
						for(int i=0; i<mOffList.size();i++){
							
							MegaOffline checkOffline = mOffList.get(i);
							
							if(!checkOffline.isIncoming()){				
								log("NOT isIncomingOffline");
								File offlineDirectory = null;
								if (Environment.getExternalStorageDirectory() != null){
									offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + checkOffline.getPath()+checkOffline.getName());
								}
								else{
									offlineDirectory = context.getFilesDir();
								}	
								
								if (!offlineDirectory.exists()){
									log("Path to remove A: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
									//dbH.removeById(mOffList.get(i).getId());
									mOffList.remove(i);		
									i--;
								}	
							}
							else{
								log("isIncomingOffline");
								File offlineDirectory = null;
								if (Environment.getExternalStorageDirectory() != null){
									offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +checkOffline.getHandleIncoming() + "/" + checkOffline.getPath()+checkOffline.getName());
									log("offlineDirectory: "+offlineDirectory);
								}
								else{
									offlineDirectory = context.getFilesDir();
								}	
								
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
						
						
						if (adapter.getItemCount() == 0){
							recyclerView.setVisibility(View.GONE);
							emptyImageView.setVisibility(View.VISIBLE);
							emptyTextView.setVisibility(View.VISIBLE);
							contentTextLayout.setVisibility(View.GONE);
							emptyImageView.setImageResource(R.drawable.ic_empty_offline);
							emptyTextView.setText(R.string.file_browser_empty_folder);
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
						break;
					}
		        }
				else{
					deleteOffline(context, selectedNode);								
					refreshPaths(selectedNode);
					
					if (adapter.getItemCount() == 0){
						recyclerView.setVisibility(View.GONE);
						emptyImageView.setVisibility(View.VISIBLE);
						emptyTextView.setVisibility(View.VISIBLE);
						contentTextLayout.setVisibility(View.GONE);
						emptyImageView.setImageResource(R.drawable.ic_empty_offline);
						emptyTextView.setText(R.string.file_browser_empty_folder);
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
					break;
				}
			}
			case R.id.offline_list_option_share_layout: 
			case R.id.offline_grid_option_share_layout: {
				log("Share option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				if (Util.isOnline(context)){
					String path = selectedNode.getPath() + selectedNode.getName();
					shareFolder(path);
				}
				break;
			}	
			case R.id.offline_list_option_rename_layout: 
			case R.id.offline_grid_option_rename_layout: {
				log("Rename option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				if (Util.isOnline(context)){
					String path = selectedNode.getPath() + selectedNode.getName();
					rename(path);
				}
				break;
			}	
		}
	}
	
    public void itemClick(int position) {
		log("onItemClick");
		if (adapter.isMultipleSelect()){
			log("multiselect");
			adapter.toggleSelection(position);
			List<MegaOffline> selectedNodes = adapter.getSelectedOfflineNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
				adapter.notifyDataSetChanged();
			}
			else{
				hideMultipleSelect();
			}
		}
		else{
			MegaOffline currentNode = mOffList.get(position);
			
			File currentFile=null;
			
			if(currentNode.getHandle().equals("0")){
				log("click on Master Key");
				String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
				openFile(new File(path));
//				viewIntent.setDataAndType(Uri.fromFile(new File(path)), MimeTypeList.typeForName("MEGAMasterKey.txt").getType());
//				((ManagerActivityLollipop)context).clickOnMasterKeyFile();
				return;
			}
						
			if(currentNode.isIncoming()){
				String handleString = currentNode.getHandleIncoming();
				currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + handleString + "/"+currentNode.getPath() + "/" + currentNode.getName());
			}
			else{
				currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode.getPath() + "/" + currentNode.getName());
			}
			
			if(currentFile.exists() && currentFile.isDirectory()){
				aB.setTitle(currentNode.getName());
				pathNavigation= currentNode.getPath()+ currentNode.getName()+"/";	
				
				if (context instanceof ManagerActivityLollipop){
					log("aB.setHomeAsUpIndicator_31");
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					((ManagerActivityLollipop)context).setPathNavigationOffline(pathNavigation);
				}
				else if (context instanceof OfflineActivityLollipop){
					((OfflineActivityLollipop)context).setPathNavigationOffline(pathNavigation);
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
					
					if(currentNode.isIncoming()){
						path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + currentNode.getHandleIncoming();
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
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
				
				adapter.setPositionClicked(-1);
				
				notifyDataSetChanged();

			}
			else{
				if(currentFile.exists() && currentFile.isFile()){			
					
					//Open it!
					if (MimeTypeList.typeForName(currentFile.getName()).isImage()){
						Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
						intent.putExtra("position", position);
						intent.putExtra("adapterType", ManagerActivityLollipop.OFFLINE_ADAPTER);
						intent.putExtra("parentNodeHandle", -1L);
						intent.putExtra("offlinePathDirectory", currentFile.getParent());
						intent.putExtra("pathNavigation", pathNavigation);
						intent.putExtra("orderGetChildren", orderGetChildren);
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
		viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
		if (MegaApiUtils.isIntentAvailable(context, viewIntent)){
			context.startActivity(viewIntent);
		}
		else{
			Intent intentShare = new Intent(Intent.ACTION_SEND);
			intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
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
		updateActionModeTitle();
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
			if(documents.get(0).isIncoming()){
				pathI = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + documents.get(0).getHandleIncoming() + "/";
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
	void hideMultipleSelect() {
		log("hideMultipleSelect");
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		log("onBackPressed");
		
		PanelState pS=slidingOptionsPanel.getPanelState();
		
		if(pS==null){
			log("NULLL");
		}
		else{
			if(pS==PanelState.HIDDEN){
				log("Hidden");
			}
			else if(pS==PanelState.COLLAPSED){
				log("Collapsed");
			}
			else{
				log("ps: "+pS);
			}
		}		
		
		if(slidingOptionsPanel.getPanelState()!=PanelState.HIDDEN){
			log("getPanelState()!=PanelState.HIDDEN");
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
			slidingOptionsPanel.setVisibility(View.GONE);
			setPositionClicked(-1);
			notifyDataSetChanged();
			return 4;
		}
		
		log("Sliding not shown");
		
		if (adapter == null){
			return 0;
		}
		
		if (adapter.isMultipleSelect()){
			hideMultipleSelect();
			return 2;
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
				else if (context instanceof OfflineActivityLollipop){
					((OfflineActivityLollipop)context).setPathNavigationOffline(pathNavigation);
					
					if (pathNavigation.equals("/")){
						aB.setTitle(getString(R.string.section_saved_for_offline));
						((OfflineActivityLollipop)context).supportInvalidateOptionsMenu();
					}
					else{
						
						String title = pathNavigation;
						index=title.lastIndexOf("/");				
						title=title.substring(0,index);
						index=title.lastIndexOf("/");				
						title=title.substring(index+1,title.length());			
						aB.setTitle(title);	
						((OfflineActivityLollipop)context).supportInvalidateOptionsMenu();
					}
				}
				ArrayList<MegaOffline> mOffListNavigation= new ArrayList<MegaOffline>();				
				mOffListNavigation=dbH.findByPath(pathNavigation);
				
				contentText.setText(getInfoFolder(mOffListNavigation));
				this.setNodes(mOffListNavigation);
				
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByNameDescending();
				}
				else{
					sortByNameAscending();
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
				emptyTextView.setText(R.string.file_browser_empty_folder);
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
		log("refreshPaths(MegaOffline mOff)");
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

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
		detector.onTouchEvent(e);
		return false;
	}

	@Override
	public void onRequestDisallowInterceptTouchEvent(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTouchEvent(RecyclerView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}

	public String getPathNavigation() {
		return pathNavigation;
	}
}