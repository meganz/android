package nz.mega.android.lollipop;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nz.mega.android.DatabaseHandler;
import nz.mega.android.FullScreenImageViewer;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaOffline;
import nz.mega.android.MegaOfflineGridAdapter;
import nz.mega.android.MimeTypeList;
import nz.mega.android.MimeTypeMime;
import nz.mega.android.OfflineActivity;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.components.SlidingUpPanelLayout;
import nz.mega.components.SlidingUpPanelLayout.PanelState;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OfflineFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{
	
	static int FROM_FILE_BROWSER = 13;
	static int FROM_INCOMING_SHARES= 14;
	static int FROM_OFFLINE= 15;

	Context context;
	ActionBar aB;
	RecyclerView listView;
	GestureDetectorCompat detector;
	RecyclerView.LayoutManager mLayoutManager;
	MegaOffline selectedNode = null;
	
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaOfflineLollipopAdapter adapterList;
	MegaOfflineGridAdapter adapterGrid;
	OfflineFragmentLollipop offlineFragment = this;
	DatabaseHandler dbH = null;
	ArrayList<MegaOffline> mOffList= null;
	String pathNavigation = null;
	TextView contentText;
	long parentHandle = -1;
	boolean isList = true;
	boolean gridNavigation=false;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	public static String DB_FILE = "0";
	public static String DB_FOLDER = "1";
	MegaApiAndroid megaApi;
	LinearLayout outSpaceLayout=null;
	TextView outSpaceText;
	Button outSpaceButton;
	int usedSpacePerc;
	
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

//		@Override
//	    public boolean onSingleTapConfirmed(MotionEvent e) {
//	        View view = listView.findChildViewUnder(e.getX(), e.getY());
//	        int position = listView.getChildPosition(view);
//
//	        // handle single tap
//	        itemClick(view, position);
//
//	        return super.onSingleTapConfirmed(e);
//	    }

	    public void onLongPress(MotionEvent e) {
	        View view = listView.findChildViewUnder(e.getX(), e.getY());
	        int position = listView.getChildPosition(view);

	        // handle long press
			if (adapterList.getPositionClicked() == -1){
				adapterList.setMultipleSelect(true);
			
				actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());			

		        itemClick(position);
			}  
	        super.onLongPress(e);
	    }
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("ActionBarCallBack::onActionItemClicked");
			List<MegaOffline> documents = adapterList.getSelectedOfflineNodes();
			
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
					((ManagerActivityLollipop) context).onFileClick(handleList);
					
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
						((ManagerActivityLollipop) context).getPublicLinkAndShareIt(n);
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
					((ManagerActivityLollipop) context).shareFolderLollipop(handleList);					
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
					((ManagerActivityLollipop) context).showMoveLollipop(handleList);
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
					((ManagerActivityLollipop) context).showCopyLollipop(handleList);
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
			adapterList.setMultipleSelect(false);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("ActionBarCallBack::onPrepareActionMode");
			List<MegaOffline> selected = adapterList.getSelectedOfflineNodes();
			
			if (Util.isOnline(context)){
				if (selected.size() != 0) {
					
					menu.findItem(R.id.cab_menu_download).setVisible(true);
					menu.findItem(R.id.cab_menu_share).setVisible(true);
					
					if(selected.size()==adapterList.getItemCount()){
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
				
					if(selected.size()==adapterList.getItemCount()){
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
		
		int parentId = node.getParentId();
		log("Finding parents...");
		//Delete parents
		if(parentId!=-1){
			mOffListParent=dbH.findByParentId(parentId);
			
			log("Same Parent?:" +mOffListParent.size());
			
			if(mOffListParent.size()<1){
				//No more node with the same parent, keep deleting				

				parentNode = dbH.findById(parentId);
				log("Recursive parent: "+parentNode.getName());
				if(parentNode != null){
					deleteOffline(context, parentNode);	
						
				}	
			}			
		}	
		
		log("Remove the node physically");
		//Remove the node physically
		File destination = null;								

		if (Environment.getExternalStorageDirectory() != null){
			destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + node.getPath());
		}
		else{
			destination = context.getFilesDir();
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
		
		return 1;		
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
		if (isList){
			if(adapterList.isMultipleSelect()){
				adapterList.selectAll();
			}
			else{
				actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
				
				adapterList.setMultipleSelect(true);
				adapterList.selectAll();
			}
			
			updateActionModeTitle();
		}
		else{
//			if (adapterGrid != null){
//				adapterGrid.selectAll();
//			}
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
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		aB.setTitle(getString(R.string.section_saved_for_offline));	
		if (context instanceof ManagerActivityLollipop && ((ManagerActivityLollipop)context).getmDrawerToggle() != null){
			((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		}
		
		//Check pathNAvigation
		if (isList){
			View v = inflater.inflate(R.layout.fragment_offlinelist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			listView = (RecyclerView) v.findViewById(R.id.offline_view_browser);
			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator()); 
			
			emptyImageView = (ImageView) v.findViewById(R.id.offline_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.offline_empty_text);		
					
			contentText = (TextView) v.findViewById(R.id.offline_content_text);
			
			outSpaceLayout = (LinearLayout) v.findViewById(R.id.offline_out_space);
			outSpaceText =  (TextView) v.findViewById(R.id.offline_out_space_text);
			outSpaceButton = (Button) v.findViewById(R.id.offline_out_space_btn);
			
			if (context instanceof ManagerActivityLollipop){
				
				outSpaceButton.setOnClickListener(this);
				usedSpacePerc=((ManagerActivityLollipop)context).getUsedPerc();
				
				if(usedSpacePerc>95){
					//Change below of ListView
					log("usedSpacePerc>95");
//					RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//					p.addRule(RelativeLayout.ABOVE, R.id.out_space);
//					listView.setLayoutParams(p);
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
//							RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//							p.addRule(RelativeLayout.ABOVE, R.id.buttons_layout);
//							listView.setLayoutParams(p);
						}
					}, 15 * 1000);
					
				}	
				else{
					outSpaceLayout.setVisibility(View.GONE);
				}
			}
			else{
				outSpaceLayout.setVisibility(View.GONE);
			}		
			

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
			
			if (adapterList == null){
				adapterList = new MegaOfflineLollipopAdapter(this, context, mOffList, listView, emptyImageView, emptyTextView, aB);
			}
			else{
				adapterList.setNodes(mOffList);
			}
			
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}
			
			adapterList.setPositionClicked(-1);
			adapterList.setMultipleSelect(false);
			
			listView.setAdapter(adapterList);
						
			if (adapterList.getItemCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				contentText.setVisibility(View.GONE);
				emptyImageView.setImageResource(R.drawable.ic_empty_offline);
				emptyTextView.setText(R.string.file_browser_empty_folder);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				contentText.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
			contentText.setText(getInfoFolder(mOffList));
			
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
	            	if(slideOffset==0){
	            		hideOptionsPanel();
	            	}
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
			View v = inflater.inflate(R.layout.fragment_offlinegrid, container, false);
			/*
			listView = (ListView) v.findViewById(R.id.offline_grid_view_browser);
			listView.setOnItemClickListener(null);
			listView.setItemsCanFocus(false);
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.offline_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.offline_grid_empty_text);
			
			mOffList=dbH.findByPath(pathNavigation);
			
			for(int i=0; i<mOffList.size();i++){
				
				File offlineDirectory = null;
				if (Environment.getExternalStorageDirectory() != null){
					offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + mOffList.get(i).getPath()+mOffList.get(i).getName());
				}
				else{
					offlineDirectory = context.getFilesDir();
				}	
				
				if (!offlineDirectory.exists()){
					dbH.removeById(mOffList.get(i).getId());
					mOffList.remove(i);
					
				}			
			}			
			
			if (adapterGrid == null){
				adapterGrid = new MegaOfflineGridAdapter(this, context, mOffList, listView, emptyImageView, emptyTextView, aB);
			}
			else{
				adapterGrid.setNodes(mOffList);
			}
			
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}			
						
			if (adapterGrid.getCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				emptyImageView.setImageResource(R.drawable.ic_empty_offline);
				emptyTextView.setText(R.string.file_browser_empty_folder);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
			
			contentText.setText(getInfoFolder(mOffList));
			
			adapterGrid.setPositionClicked(-1);
			//adapterGrid.setMultipleSelect(false);
			listView.setAdapter(adapterGrid);*/
			
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
			((ManagerActivityLollipop) context).onFileClick(handleList);
		}
		else{
			//TODO toast no connection
		}
	}
	
	public void sortByNameDescending(){
		
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();
		
		
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

		if (isList){
			adapterList.setNodes(mOffList);
		}
		else{
			adapterGrid.setNodes(mOffList);
		}
	}

	
	public void sortByNameAscending(){
		log("sortByNameAscending");
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();
				
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

		if (isList){
			adapterList.setNodes(mOffList);
		}
		else{
			adapterGrid.setNodes(mOffList);
		}

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
		
		((ManagerActivityLollipop) context).getPublicLinkAndShareIt(n);

	}
	
	public void shareFolder (String path){
		MegaNode n = megaApi.getNodeByPath(path);
		if(n == null)
		{
			return;
		}
		
		((ManagerActivityLollipop) context).shareFolderLollipop(n);
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
		((ManagerActivityLollipop) context).showMoveLollipop(handleList);
	}
	
	public void copy (String path){
		MegaNode n = megaApi.getNodeByPath(path);
		if(n == null)
		{
			return;
		}
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(n.getHandle());									
		((ManagerActivityLollipop) context).showCopyLollipop(handleList);
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
		log("showOptionsPanel");
		
		this.selectedNode = sNode;
		
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
				
		adapterList.setPositionClicked(-1);
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
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {
		log("onClick");
		switch(v.getId()){
			case R.id.offline_out_space_btn:{				
			
				log("Click Account Button");
				((ManagerActivityLollipop)getActivity()).upgradeAccountButton();
				break;
			}
			case R.id.offline_list_option_download_layout: {
				log("Download option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(Long.parseLong(selectedNode.getHandle()));
				((ManagerActivityLollipop) context).onFileClick(handleList);
				break;
			}
			case R.id.offline_list_option_move_layout:{
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
			case R.id.offline_list_option_copy_layout: {
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
			case R.id.offline_list_option_properties_layout:{				
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
			case R.id.offline_list_option_public_link_layout:{
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
			case R.id.offline_list_option_delete_layout:{
				log("Delete Offline");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();		
									
				deleteOffline(context, selectedNode);								
				refreshPaths(selectedNode);				
				break;
			}
			case R.id.offline_list_option_share_layout: {	
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
			case R.id.offline_list_option_rename_layout: {
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
		if (isList){
			log("mode List");
			if (adapterList.isMultipleSelect()){
				log("multiselect");
				adapterList.toggleSelection(position);
				updateActionModeTitle();
				adapterList.notifyDataSetChanged();
			}
			else{
				MegaOffline currentNode = mOffList.get(position);
				
				aB.setTitle(currentNode.getName());
				pathNavigation= currentNode.getPath()+ currentNode.getName()+"/";	
				if (context instanceof ManagerActivityLollipop){
					((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					((ManagerActivityLollipop)context).setPathNavigationOffline(pathNavigation);
				}
				else if (context instanceof OfflineActivity){
					((OfflineActivity)context).setPathNavigationOffline(pathNavigation);
				}
				
				File currentFile=null;
				
				if(currentNode.isIncoming()){
					String handleString = currentNode.getHandleIncoming();
					currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + handleString + "/"+currentNode.getPath() + "/" + currentNode.getName());
				}
				else{
					currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode.getPath() + "/" + currentNode.getName());
				}
				
				if(currentFile.exists()&&currentFile.isDirectory()){

					mOffList=dbH.findByPath(currentNode.getPath()+currentNode.getName()+"/");
					if (adapterList.getItemCount() == 0){
						listView.setVisibility(View.GONE);
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

					if (adapterList == null){						
						adapterList = new MegaOfflineLollipopAdapter(this, context, mOffList, listView, emptyImageView, emptyTextView, aB);
					}
					else{						
						adapterList.setNodes(mOffList);
					}
					
					if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
						sortByNameDescending();
					}
					else{
						sortByNameAscending();
					}
					
					contentText.setText(getInfoFolder(mOffList));
					adapterList.setPositionClicked(-1);
					
					notifyDataSetChanged();

				}else{
					if(currentFile.exists()&&currentFile.isFile()){
						//Open it!
						if (MimeTypeList.typeForName(currentFile.getName()).isImage()){
							Intent intent = new Intent(context, FullScreenImageViewer.class);
							intent.putExtra("position", position);
							intent.putExtra("adapterType", ManagerActivityLollipop.OFFLINE_ADAPTER);
							intent.putExtra("parentNodeHandle", -1L);
							intent.putExtra("offlinePathDirectory", currentFile.getParent());
							startActivity(intent);
						}
//						else if(currentFile.exists()&&MimeType.typeForName(currentFile.getName()).isPdf()){
//		    			    log("Offline - File PDF");		    			    
//		    			    Intent intentPdf = new Intent();
//		    			    intentPdf.setDataAndType(Uri.fromFile(currentFile), "application/pdf");
//		    			    intentPdf.setClass(context, OpenPDFActivity.class);
//		    			    intentPdf.setAction("android.intent.action.VIEW");
//		    				this.startActivity(intentPdf);						
//							
//						}
						else{
							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
							if (ManagerActivityLollipop.isIntentAvailable(context, viewIntent)){
								context.startActivity(viewIntent);
							}
							else{
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
								if (ManagerActivityLollipop.isIntentAvailable(context, intentShare)){
									context.startActivity(intentShare);
								}
							}
						}
						
					}					
				}			
				
				
//				String currentPath = paths.get(position);
//				File currentFile = new File (currentPath);
				
//				if (MimeType.typeForName(currentFile.getName()).isImage()){
//					Intent intent = new Intent(context, FullScreenImageViewer.class);
//					intent.putExtra("position", position);
//					intent.putExtra("adapterType", ManagerActivityLollipop.OFFLINE_ADAPTER);
//					intent.putExtra("parentNodeHandle", -1L);
//					startActivity(intent);
//				}
//				else{
//					Intent viewIntent = new Intent(Intent.ACTION_VIEW);
//					viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
//					if (ManagerActivityLollipop.isIntentAvailable(context, viewIntent)){
//						context.startActivity(viewIntent);
//					}
//					else{
//						Intent intentShare = new Intent(Intent.ACTION_SEND);
//						intentShare.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
//						if (ManagerActivityLollipop.isIntentAvailable(context, intentShare)){
//							context.startActivity(intentShare);
//						}
//					}
//				}
			}
		}
    }	
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapterList.isMultipleSelect()){
			adapterList.clearSelections();
		}
		updateActionModeTitle();
	}
	
	private void updateActionModeTitle() {
		log("updateActionModeTitle");
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaOffline> documents = adapterList.getSelectedOfflineNodes();
		int files = 0;
		int folders = 0;
		for (MegaOffline document : documents) {
			File f = new File(document.getName());
			if (f.isFile()) {
				files++;
			} 
			else {
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
		// actionMode.
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		log("hideMultipleSelect");
		adapterList.setMultipleSelect(false);
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
							((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
							((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
						}
						else{
							String title = pathNavigation;
							index=title.lastIndexOf("/");				
							title=title.substring(0,index);
							index=title.lastIndexOf("/");				
							title=title.substring(index+1,title.length());			
							aB.setTitle(title);
							((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
							((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
						}
					}
					else if (context instanceof OfflineActivity){
						((OfflineActivity)context).setPathNavigationOffline(pathNavigation);
						
						if (pathNavigation.equals("/")){
							aB.setTitle(getString(R.string.section_saved_for_offline));
							((OfflineActivity)context).supportInvalidateOptionsMenu();
						}
						else{
							
							String title = pathNavigation;
							index=title.lastIndexOf("/");				
							title=title.substring(0,index);
							index=title.lastIndexOf("/");				
							title=title.substring(index+1,title.length());			
							aB.setTitle(title);	
							((OfflineActivity)context).supportInvalidateOptionsMenu();
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
		else{
		
			if (adapterGrid.getPositionClicked() != -1){
				adapterGrid.setPositionClicked(-1);
				adapterGrid.notifyDataSetChanged();
				return 1;
			}
			else if(pathNavigation != null){
				if (!pathNavigation.equals("/")){
					//TODO En caso de que no esté en el raíz del offline, pues navegar para atrás.
		
					// Esto es, poner el nuevo path y adapterList.setNodes() y adapterList.notifyDataSetChanged();
					((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
					
					pathNavigation=pathNavigation.substring(0,pathNavigation.length()-1);
					int index=pathNavigation.lastIndexOf("/");				
					pathNavigation=pathNavigation.substring(0,index+1);
					if (context instanceof ManagerActivityLollipop){
						((ManagerActivityLollipop)context).setPathNavigationOffline(pathNavigation);
					}
					else if (context instanceof OfflineActivity){
						((OfflineActivity)context).setPathNavigationOffline(pathNavigation);
					}
					
					ArrayList<MegaOffline> mOffListNavigation= new ArrayList<MegaOffline>();				
					mOffListNavigation=dbH.findByPath(pathNavigation);
					this.setNodes(mOffListNavigation);
					
					if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
						sortByNameDescending();
					}
					else{
						sortByNameAscending();
					}
					//adapterGrid.setNodes(mOffList);
					//adapterGrid.setPathNavigation?
					
					return 2;
				}
				else{
					return 0;
				}
			}
			else{
					return 0;
			}
					
//			parentHandle = adapterGrid.getParentHandle();
//			((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
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
//						((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//					}
//					else{
//						aB.setTitle(parentNode.getName());					
//						((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					}
//					
//					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
//					
//					parentHandle = parentNode.getHandle();
//					((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
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
		}
	}
	
	public RecyclerView getListView(){
		return listView;
	}
	
	public void setNodes(ArrayList<MegaOffline> _mOff){
		log("setNodes");
		this.mOffList = _mOff;

		if (isList){
			if (adapterList != null){
				adapterList.setNodes(mOffList);
				if (adapterList.getItemCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					emptyImageView.setImageResource(R.drawable.ic_empty_offline);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}	
		}
//		else{
//			if (adapterGrid != null){
//				adapterGrid.setPaths(paths);
//				if (adapterGrid.getCount() == 0){
//					listView.setVisibility(View.GONE);
//					emptyImageView.setVisibility(View.VISIBLE);
//					emptyTextView.setVisibility(View.VISIBLE);
//					emptyImageView.setImageResource(R.drawable.ic_empty_offline);
//					emptyTextView.setText(R.string.file_browser_empty_folder);
//				}
//				else{
//					listView.setVisibility(View.VISIBLE);
//					emptyImageView.setVisibility(View.GONE);
//					emptyTextView.setVisibility(View.GONE);
//				}			
//			}
//		}
	}
	
	public void setPositionClicked(int positionClicked){
		log("setPositionClicked");
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
		log("notifyDataSetChanged");
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
		listView.invalidate();
	}
	
	public void refreshPaths(MegaOffline mOff){
		log("refreshPaths(MegaOffline mOff");
		int index=0;
		MegaOffline retFindPath = null;
		
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
		}
		else{
			retFindPath=findPath(pNav);	
			mOffList=dbH.findByPath(retFindPath.getPath()+retFindPath.getName()+"/");
		}
				
		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}
		
//		setNodes(mOffList);
		pathNavigation=pNav;
		listView.invalidate();
	}
	
	public int getItemCount(){
		if(adapterList!=null){
			return adapterList.getItemCount();
		}
		return 0;
	}
	
	private MegaOffline findPath (String pNav){
		
		log("Path Navigation" + pNav);
		
		MegaOffline nodeToShow = null;
		int index=pNav.lastIndexOf("/");	
		String pathToShow = pNav.substring(0, index+1);
		log("Path: "+ pathToShow);
		String nameToShow = pNav.substring(index+1, pNav.length());
		log("Name: "+ nameToShow);
		
		nodeToShow = dbH.findbyPathAndName(pathToShow, nameToShow);
		if(nodeToShow!=null){
			//Show the node
			log("NOde: "+ nodeToShow.getName());
			
			return nodeToShow;
		}
		else{
			findPath (pathToShow);
		}
		
		return nodeToShow;		
	}	
	
	public void setPathNavigation(String _pathNavigation){
		log("setPathNavigation");
		this.pathNavigation = _pathNavigation;
		if (isList){
			if (adapterList != null){	
				adapterList.setNodes(dbH.findByPath(_pathNavigation));
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setNodes(dbH.findByPath(_pathNavigation));
			}
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
}
