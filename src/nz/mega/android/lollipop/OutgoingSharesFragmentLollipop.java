package nz.mega.android.lollipop;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nz.mega.android.FileContactListActivity;
import nz.mega.android.FullScreenImageViewer;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaBrowserGridAdapter;
import nz.mega.android.MegaStreamingService;
import nz.mega.android.MimeTypeList;
import nz.mega.android.MimeTypeMime;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.components.SlidingUpPanelLayout;
import nz.mega.components.SlidingUpPanelLayout.PanelState;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.Toast;


public class OutgoingSharesFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

	Context context;
	ActionBar aB;
	RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserLollipopAdapter adapterList;
	MegaBrowserGridAdapter adapterGrid;
	OutgoingSharesFragmentLollipop outgoingSharesFragment = this;
	LinearLayout buttonsLayout=null;
	TextView outSpaceText;
	Button outSpaceButton;
	int usedSpacePerc;
	Button leftNewFolder;
	Button rightUploadButton;
	TextView contentText;

	LinearLayout outSpaceLayout=null;
	LinearLayout getProLayout=null;
	
	MegaApiAndroid megaApi;
		
	long parentHandle = -1;
	int deepBrowserTree = 0;
	boolean isList = true;
	boolean overflowMenu = false;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	ArrayList<MegaNode> nodes;
	MegaNode selectedNode;
	
	HashMap<Long, MegaTransfer> mTHash = null;
	
	private ActionMode actionMode;
	
	//OPTIONS PANEL
		private SlidingUpPanelLayout slidingOptionsPanel;
		public FrameLayout optionsOutLayout;
		public LinearLayout optionsLayout;
		public LinearLayout optionDownload;
		public LinearLayout optionProperties;
		public LinearLayout optionRename;
		public LinearLayout optionPublicLink;
		public LinearLayout optionShare;
		public LinearLayout optionPermissions;
		public LinearLayout optionDelete;
		public LinearLayout optionRemoveTotal;
		public LinearLayout optionClearShares;
		public LinearLayout optionLeaveShare;
		public LinearLayout optionMoveTo;
		public LinearLayout optionCopyTo;	
		public TextView propertiesText;
		////
		
		public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

//			@Override
//		    public boolean onSingleTapConfirmed(MotionEvent e) {
//		        View view = listView.findChildViewUnder(e.getX(), e.getY());
//		        int position = listView.getChildPosition(view);
	//
//		        // handle single tap
//		        itemClick(view, position);
	//
//		        return super.onSingleTapConfirmed(e);
//		    }

		    public void onLongPress(MotionEvent e) {
		        View view = listView.findChildViewUnder(e.getX(), e.getY());
		        int position = listView.getChildPosition(view);

		        // handle long press
				if (adapterList.getPositionClicked() == -1){
					adapterList.setMultipleSelect(true);
				
					actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());			

			        itemClick(position);
				}  
		        super.onLongPress(e);
		    }
		}
	
	private class ActionBarCallBack implements ActionMode.Callback {
		
		boolean showRename = false;
		boolean showMove = false;
		boolean showLink = false;
		boolean showTrash = false;
		boolean showShare = false;

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = adapterList.getSelectedNodes();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
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
						((ManagerActivityLollipop) context).showRenameDialog(documents.get(0), documents.get(0).getName());
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
					((ManagerActivityLollipop) context).showCopyLollipop(handleList);
					break;
				}	
				case R.id.cab_menu_share:{
					//Check that all the selected options are folders
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						if(documents.get(i).isFolder()){
							handleList.add(documents.get(i).getHandle());
						}
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivityLollipop) context).shareFolderLollipop(handleList);					
					break;
				}
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivityLollipop) context).showMoveLollipop(handleList);
					break;
				}
				case R.id.cab_menu_share_link:{
					clearSelections();
					hideMultipleSelect();
					if (documents.size()==1){
						((ManagerActivityLollipop) context).getPublicLinkAndShareIt(documents.get(0));
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
					((ManagerActivityLollipop) context).moveToTrash(handleList);
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
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapterList.getSelectedNodes();
					
			if (selected.size() != 0) {
				showTrash = true;
				showMove = true;
				showShare = true;
				
				if (selected.size() == 1) {
					showLink=true;
					showRename=true;
				}
				else{
					showLink=false;
					showRename=false;
				}
				
				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showTrash = false;
						showMove = false;
						break;
					}
				}
				
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
			
			menu.findItem(R.id.cab_menu_download).setVisible(true);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(true);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share).setVisible(showShare);
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
		
		nodes = new ArrayList<MegaNode>();
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
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}
		
		if (megaApi.getRootNode() == null){
			return null;
		}
		
						
		if (isList){
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			listView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator()); 
					
			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			contentText = (TextView) v.findViewById(R.id.content_text);
			
			emptyImageView.setImageResource(R.drawable.ic_empty_shared);
			emptyTextView.setText(R.string.file_browser_empty_outgoing_shares);			
			
			buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_layout);
			leftNewFolder = (Button) v.findViewById(R.id.btnLeft_new);
			rightUploadButton = (Button) v.findViewById(R.id.btnRight_upload);	
			buttonsLayout.setVisibility(View.GONE);
			leftNewFolder.setVisibility(View.GONE);
			rightUploadButton.setVisibility(View.GONE);
			
			getProLayout=(LinearLayout) v.findViewById(R.id.get_pro_account);
			getProLayout.setVisibility(View.GONE);
			
			outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space);
			outSpaceText =  (TextView) v.findViewById(R.id.out_space_text);
			outSpaceButton = (Button) v.findViewById(R.id.out_space_btn);
			
			outSpaceButton.setOnClickListener(this);
			
			usedSpacePerc=((ManagerActivityLollipop)context).getUsedPerc();
			
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

			
			if (adapterList == null){
				adapterList = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, listView, aB, ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				if (mTHash != null){
					adapterList.setTransfers(mTHash);
				}
			}
			else{
				adapterList.setParentHandle(parentHandle);
				adapterList.setNodes(nodes);
			}
			
			if (parentHandle == -1){			
				((ManagerActivityLollipop)context).setParentHandleOutgoing(-1);					
				findNodes();	
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByNameDescending();
				}
				else{
					sortByNameAscending();
				}
				
				aB.setTitle(getString(R.string.section_shared_items));	
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}
			else{
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);			
				
				aB.setTitle(parentNode.getName());
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}	
			
			adapterList.setPositionClicked(-1);
			adapterList.setMultipleSelect(false);
			
			listView.setAdapter(adapterList);		
			
			if (adapterList != null){
				adapterList.setNodes(nodes);
				if (adapterList.getItemCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);	
					contentText.setVisibility(View.GONE);
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					contentText.setText(getInfoNode());
					contentText.setVisibility(View.VISIBLE);
				}			
			}	
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout);
			optionsLayout = (LinearLayout) v.findViewById(R.id.file_list_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.file_list_out_options);
			optionRename = (LinearLayout) v.findViewById(R.id.file_list_option_rename_layout);
			optionRename.setVisibility(View.GONE);
			optionLeaveShare = (LinearLayout) v.findViewById(R.id.file_list_option_leave_share_layout);
			optionLeaveShare.setVisibility(View.GONE);
			
			optionDownload = (LinearLayout) v.findViewById(R.id.file_list_option_download_layout);
			optionProperties = (LinearLayout) v.findViewById(R.id.file_list_option_properties_layout);
			propertiesText = (TextView) v.findViewById(R.id.file_list_option_properties_text);			

			optionPublicLink = (LinearLayout) v.findViewById(R.id.file_list_option_public_link_layout);
//				holder.optionPublicLink.getLayoutParams().width = Util.px2dp((60), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);

			optionShare = (LinearLayout) v.findViewById(R.id.file_list_option_share_layout);
			optionPermissions = (LinearLayout) v.findViewById(R.id.file_list_option_permissions_layout);
			
			optionDelete = (LinearLayout) v.findViewById(R.id.file_list_option_delete_layout);			
			optionRemoveTotal = (LinearLayout) v.findViewById(R.id.file_list_option_remove_layout);

//				holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);

			optionClearShares = (LinearLayout) v.findViewById(R.id.file_list_option_clear_share_layout);	
			optionMoveTo = (LinearLayout) v.findViewById(R.id.file_list_option_move_layout);		
			optionCopyTo = (LinearLayout) v.findViewById(R.id.file_list_option_copy_layout);			
			
			optionDownload.setOnClickListener(this);
			optionShare.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionRename.setOnClickListener(this);
			optionClearShares.setOnClickListener(this);
			optionPermissions.setOnClickListener(this);
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
			log("Grid View");
			
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false); /*
			
			listView = (ListView) v.findViewById(R.id.file_grid_view_browser);
			listView.setOnItemClickListener(null);
			listView.setItemsCanFocus(false);
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			contentText = (TextView) v.findViewById(R.id.content_grid_text);
			
			buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_grid_layout);
			leftNewFolder = (Button) v.findViewById(R.id.btnLeft_grid_new);
			rightUploadButton = (Button) v.findViewById(R.id.btnRight_grid_upload);		
			buttonsLayout.setVisibility(View.GONE);
			leftNewFolder.setVisibility(View.GONE);
			rightUploadButton.setVisibility(View.GONE);
			
			outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space_grid);
			outSpaceLayout.setVisibility(View.GONE);
	        
			if (adapterGrid == null){
				adapterGrid = new MegaBrowserGridAdapter(context, nodes, parentHandle, listView, aB, ManagerActivityLollipop.FILE_BROWSER_ADAPTER);
				if (mTHash != null){
					adapterGrid.setTransfers(mTHash);
				}
			}
			else{
				adapterGrid.setParentHandle(parentHandle);
				adapterGrid.setNodes(nodes);
			}
				
			if (parentHandle == -1){			
				((ManagerActivityLollipop)context).setParentHandleOutgoing(-1);					
				findNodes();	
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByNameDescending();
				}
				else{
					sortByNameAscending();
				}
				
				if(((ManagerActivityLollipop)context).getmDrawerToggle() != null)
				{
					aB.setTitle(getString(R.string.section_shared_items));	
					((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				}
			}
			else{
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);			
				if(((ManagerActivityLollipop)context).getmDrawerToggle() != null)
				{
					aB.setTitle(parentNode.getName());
					((ManagerActivityLollipop)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				}
			}	
			
			adapterGrid.setPositionClicked(-1);
			
			listView.setAdapter(adapterGrid);
			
			if (adapterGrid != null){
				adapterGrid.setNodes(nodes);
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
			}	*/	
			
			return v;
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
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		contentText.setText(getInfoFolder(n));
		adapterList.setParentHandle(parentHandle);
		nodes = megaApi.getChildren(n, orderGetChildren);
		adapterList.setNodes(nodes);
		setPositionClicked(-1);
		
		//If folder has no files
		if (adapterList.getItemCount() == 0){
			listView.setVisibility(View.GONE);
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
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}	
	}

	public void refresh(){
		log("refresh");
		findNodes();	
		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}
		if(adapterList!=null){
			adapterList.setNodes(nodes);
		}
	}
		
	public void findNodes(){	
		log("findNodes");
		deepBrowserTree=0;
		ArrayList<MegaShare> outNodeList = megaApi.getOutShares();
		nodes.clear();
		long lastFolder=-1;		
		
		for(int k=0;k<outNodeList.size();k++){
			
			if(outNodeList.get(k).getUser()!=null){
				MegaShare mS = outNodeList.get(k);				
				MegaNode node = megaApi.getNodeByHandle(mS.getNodeHandle());
				
				if(lastFolder!=node.getHandle()){
					lastFolder=node.getHandle();
					nodes.add(node);			
				}	
			}
		}
	}
	
	public void showOptionsPanel(MegaNode sNode){
		log("showOptionsPanel");		
	
		this.selectedNode = sNode;
		
		if (selectedNode.isFolder()) {
			propertiesText.setText(R.string.general_folder_info);
			optionShare.setVisibility(View.VISIBLE);
		}else{
			propertiesText.setText(R.string.general_file_info);
			optionShare.setVisibility(View.GONE);
		}
		
		optionDownload.setVisibility(View.VISIBLE);
		optionProperties.setVisibility(View.VISIBLE);		
		optionRename.setVisibility(View.VISIBLE);
		optionMoveTo.setVisibility(View.VISIBLE);
		optionCopyTo.setVisibility(View.VISIBLE);
		optionClearShares.setVisibility(View.VISIBLE);
		optionPermissions.setVisibility(View.VISIBLE);
		
		//Hide
		optionDelete.setVisibility(View.GONE);
		optionDelete.setVisibility(View.GONE);
		optionRemoveTotal.setVisibility(View.GONE);
		optionPublicLink.setVisibility(View.GONE);
					
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
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.btnLeft_new:
				((ManagerActivityLollipop)getActivity()).showNewFolderDialog(null);				
				break;
				
			case R.id.btnRight_upload:
				((ManagerActivityLollipop)getActivity()).uploadFile();
				break;
			case R.id.btnLeft_grid_new:
				((ManagerActivityLollipop)getActivity()).showNewFolderDialog(null);				
				break;
				
			case R.id.btnRight_grid_upload:
				((ManagerActivityLollipop)getActivity()).uploadFile();
				break;
				
			case R.id.out_space_btn:
				((ManagerActivityLollipop)getActivity()).upgradeAccountButton();
				break;
			case R.id.file_list_option_download_layout: {
				log("Download option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());
				((ManagerActivityLollipop) context).onFileClick(handleList);
				break;
			}
			case R.id.file_list_option_properties_layout: {
				log("Properties option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				Intent i = new Intent(context, FilePropertiesActivityLollipop.class);
				i.putExtra("handle", selectedNode.getHandle());
				
				if (selectedNode.isFolder()) {
					if (megaApi.isShared(selectedNode)){
						i.putExtra("imageId", R.drawable.folder_shared_mime);	
					}
					else{
						i.putExtra("imageId", R.drawable.folder_mime);
					}
				} 
				else {
					i.putExtra("imageId", MimeTypeMime.typeForName(selectedNode.getName()).getIconResourceId());
				}
				i.putExtra("name", selectedNode.getName());
				context.startActivity(i);

				break;
			}
			case R.id.file_list_option_rename_layout: {
				log("Rename option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).showRenameDialog(selectedNode, selectedNode.getName());
				break;
			}	
			case R.id.file_list_option_copy_layout: {
				log("Copy option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());									
				((ManagerActivityLollipop) context).showCopyLollipop(handleList);
				break;
			}
			case R.id.file_list_option_move_layout:{
				log("Move option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());									
				((ManagerActivityLollipop) context).showMoveLollipop(handleList);

				break;
			}
			case R.id.file_list_option_clear_share_layout: {
				log("Clear shares");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<MegaShare> shareList = megaApi.getOutShares(selectedNode);				
				((ManagerActivityLollipop) context).removeAllSharingContacts(shareList, selectedNode);
				break;				
			}
			case R.id.file_list_option_share_layout: {	
				log("Share option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).shareFolderLollipop(selectedNode);
				break;
			}
			
			case R.id.file_list_option_permissions_layout: {
				log("Share with");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				Intent i = new Intent(context, FileContactListActivity.class);
				i.putExtra("name", selectedNode.getHandle());
				context.startActivity(i);			
				break;
			}			
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
	
    public void itemClick(int position) {
	
		if (isList){
			if (adapterList.isMultipleSelect()){
				adapterList.toggleSelection(position);
				updateActionModeTitle();
				adapterList.notifyDataSetChanged();
			}
			else{
				if (nodes.get(position).isFolder()){
					
					deepBrowserTree = deepBrowserTree+1;
					log("deepBrowserTree "+deepBrowserTree);
					
					MegaNode n = nodes.get(position);
										
					aB.setTitle(n.getName());
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					
					parentHandle = nodes.get(position).getHandle();
					MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
					contentText.setText(getInfoFolder(infoNode));
					((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
					adapterList.setParentHandle(parentHandle);
					nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
					adapterList.setNodes(nodes);
					setPositionClicked(-1);
					
					//If folder has no files
					if (adapterList.getItemCount() == 0){
						listView.setVisibility(View.GONE);
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
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);
					}
				}
				else{
					//Is file
					if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
						Intent intent = new Intent(context, FullScreenImageViewer.class);
						intent.putExtra("position", position);
						intent.putExtra("adapterType", ManagerActivityLollipop.FILE_BROWSER_ADAPTER);
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
				  		if (ManagerActivityLollipop.isIntentAvailable(context, mediaIntent)){
				  			context.startActivity(mediaIntent);
				  		}
				  		else{
				  			Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
				  			adapterList.setPositionClicked(-1);
							adapterList.notifyDataSetChanged();
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(nodes.get(position).getHandle());
							((ManagerActivityLollipop) context).onFileClick(handleList);
				  		}						
					}
					else{
						adapterList.setPositionClicked(-1);
						adapterList.notifyDataSetChanged();
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						((ManagerActivityLollipop) context).onFileClick(handleList);
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
	
	public void selectAll(){
		if (isList){
			if(adapterList.isMultipleSelect()){
				adapterList.selectAll();
			}
			else{
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
				
				adapterList.setMultipleSelect(true);
				adapterList.selectAll();
			}
			
			updateActionModeTitle();
		}
		else{
			if (adapterGrid != null){
//				adapterGrid.selectAll();
			}
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
	
public void sortByNameDescending(){
		
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaNode> tempOffline = new ArrayList<MegaNode>();
		
		
		for(int k = 0; k < nodes.size() ; k++) {
			MegaNode node = nodes.get(k);
			if(node.isFolder()){
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
			for(int j = 0; j < nodes.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = nodes.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(nodes.get(j));
				}				
			}
			
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < nodes.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = nodes.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(nodes.get(j));					
				}				
			}
			
		}
		
		nodes.clear();
		nodes.addAll(tempOffline);

		if (isList){
			adapterList.setNodes(nodes);
		}
		else{
			adapterGrid.setNodes(nodes);
		}
	}

	
	public void sortByNameAscending(){
		log("sortByNameAscending");
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaNode> tempOffline = new ArrayList<MegaNode>();
				
		for(int k = 0; k < nodes.size() ; k++) {
			MegaNode node = nodes.get(k);
			if(node.isFolder()){
				foldersOrder.add(node.getName());
			}
			else{
				filesOrder.add(node.getName());
			}
		}		
	
		Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);

		for(int k = 0; k < foldersOrder.size() ; k++) {
			for(int j = 0; j < nodes.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = nodes.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(nodes.get(j));
				}				
			}			
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < nodes.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = nodes.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(nodes.get(j));
				}				
			}
			
		}
		
		nodes.clear();
		nodes.addAll(tempOffline);

		if (isList){
			adapterList.setNodes(nodes);
		}
		else{
			adapterGrid.setNodes(nodes);
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
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaNode> documents = adapterList.getSelectedNodes();
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
		// actionMode.
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

		log("onBackPressed");
		deepBrowserTree = deepBrowserTree-1;
		log("deepBrowserTree "+deepBrowserTree);
		
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
				((ManagerActivityLollipop)context).setParentHandleOutgoing(-1);
				parentHandle=-1;
				log("Shared With Me");
				aB.setTitle(getString(R.string.section_shared_items));
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				findNodes();
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByNameDescending();
				}
				else{
					sortByNameAscending();
				}
				adapterList.setNodes(nodes);
				contentText.setText(getInfoNode());
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				return 3;
			}
			else if (deepBrowserTree>0){
				log("Keep navigation");
				parentHandle = adapterList.getParentHandle();
				//((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);			
				
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				
				contentText.setText(getInfoFolder(parentNode));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					
					aB.setTitle(parentNode.getName());		
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					
					parentHandle = parentNode.getHandle();
					((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					adapterList.setNodes(nodes);
					setPositionClicked(-1);
					adapterList.setParentHandle(parentHandle);
					return 2;
				}	
				return 2;
			}
			else{
				log("Back to Cloud");
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				((ManagerActivityLollipop)context).setParentHandleBrowser(megaApi.getRootNode().getHandle());
				deepBrowserTree=0;
				return 0;
			}
			
		}
		else{
			parentHandle = adapterGrid.getParentHandle();
			((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
			
			if (adapterGrid == null){
				return 0;
			}
			
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
					
					aB.setTitle(parentNode.getName());					
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					
					parentHandle = parentNode.getHandle();
					((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					adapterGrid.setNodes(nodes);
					setPositionClicked(-1);
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
	
	public RecyclerView getListView(){
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
//					contentText.setText(getInfoNode());
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
	
	public int getItemCount(){
		if(adapterList!=null){
			return adapterList.getItemCount();
		}
		return 0;
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
		Util.log("OutgoingSharesFragmentLollipop", log);
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
