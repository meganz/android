package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaStreamingService;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.SlidingUpPanelLayout;
import mega.privacy.android.app.components.SlidingUpPanelLayout.PanelState;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;


public class IncomingSharesFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	RecyclerView.LayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserLollipopAdapter adapter;
	IncomingSharesFragmentLollipop incomingSharesFragment = this;
    ImageButton fabButton;

	MegaApiAndroid megaApi;
	
	TextView contentText;	
	RelativeLayout contentTextLayout;
	boolean downloadInProgress = false;
	ProgressBar progressBar;
	ImageView transferArrow;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	long parentHandle = -1;
	int deepBrowserTree = 0;
	boolean isList = true;
	int orderGetChildren;
	
	ArrayList<MegaNode> nodes;
	MegaNode selectedNode;
	
	HashMap<Long, MegaTransfer> mTHash = null;
	
	private ActionMode actionMode;
	
	SlidingUpPanelLayout.PanelSlideListener slidingPanelListener;
	//UPLOAD PANEL
	private SlidingUpPanelLayout slidingUploadPanel;
	public FrameLayout uploadOutLayout;
	public LinearLayout uploadLayout;
	public LinearLayout uploadImage;
	public LinearLayout uploadAudio;
	public LinearLayout uploadVideo;
	public LinearLayout uploadFromSystem;	
	
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
	public LinearLayout optionSendToInbox;
	////
	
	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

	    public void onLongPress(MotionEvent e) {
	        View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
	        int position = recyclerView.getChildPosition(view);

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
		
		boolean showRename = false;
		boolean showMove = false;
		boolean showLink = false;
		boolean showCopy = false;


		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = adapter.getSelectedNodes();
			
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
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_leave_multiple_share: {
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivityLollipop) context).leaveMultipleShares(handleList);					
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
			adapter.setMultipleSelect(false);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();
					
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
		log("onCreateView: parentHandle is: "+parentHandle);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}
		
		if (megaApi.getRootNode() == null){
			return null;
		}
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
				
	    isList = ((ManagerActivityLollipop)context).isList();
		orderGetChildren = ((ManagerActivityLollipop)context).getOrderOthers();

		if (parentHandle == -1){

			long parentHandleIncoming = ((ManagerActivityLollipop)context).getParentHandleIncoming();
			if(parentHandleIncoming!=-1){
				log("After consulting... the INCOMING parent is: "+parentHandleIncoming);
				parentHandle = parentHandleIncoming;
				deepBrowserTree = ((ManagerActivityLollipop)context).getDeepBrowserTreeIncoming();
				log("AND deepBrowserTree: "+deepBrowserTree);
			}
		}

		if (isList){
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.addOnItemTouchListener(this);
			recyclerView.setItemAnimator(new DefaultItemAnimator()); 	        
		
			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);

			progressBar = (ProgressBar) v.findViewById(R.id.file_list_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.file_list_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_text_layout);
			contentText = (TextView) v.findViewById(R.id.content_text);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(73, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);			
			
			emptyImageView.setImageResource(R.drawable.incoming_shares_empty);			
			emptyTextView.setText(R.string.file_browser_empty_incoming_shares);
			
			fabButton = (ImageButton) v.findViewById(R.id.file_upload_button);
			fabButton.setOnClickListener(this);
			fabButton.setVisibility(View.GONE);
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, ManagerActivityLollipop.INCOMING_SHARES_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				if (mTHash != null){
					adapter.setTransfers(mTHash);
				}
//				adapterList.setNodes(nodes);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
//				adapterList.setNodes(nodes);
			}

			if (parentHandle == -1){
				log("ParentHandle -1");
				findNodes();
				adapter.setParentHandle(-1);
				aB.setTitle(getString(R.string.section_shared_items));
				log("aB.setHomeAsUpIndicator_333");
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}
			else{
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				log("ParentHandle to find children: "+parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				adapter.setNodes(nodes);
				aB.setTitle(parentNode.getName());
				log("ic_arrow_back_white_68");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
			}
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

//			if (deepBrowserTree == 0){
//				contentText.setText(getInfoNode());
//				aB.setTitle(getString(R.string.section_shared_items));
//				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
//				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
//			}
//			else{
//				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
//				contentText.setText(getInfoFolder(infoNode));
//				aB.setTitle(infoNode.getName());
//			}
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);
			
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);	
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
//			setNodes(nodes);	
			
			if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
				showProgressBar();
				progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
			}
			else{					
				contentText.setText(getInfoNode());
			}
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout);
			optionsLayout = (LinearLayout) v.findViewById(R.id.file_list_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.file_list_out_options);
			optionRename = (LinearLayout) v.findViewById(R.id.file_list_option_rename_layout);
			optionLeaveShare = (LinearLayout) v.findViewById(R.id.file_list_option_leave_share_layout);
			optionDownload = (LinearLayout) v.findViewById(R.id.file_list_option_download_layout);
			optionProperties = (LinearLayout) v.findViewById(R.id.file_list_option_properties_layout);
			propertiesText = (TextView) v.findViewById(R.id.file_list_option_properties_text);		
			optionMoveTo = (LinearLayout) v.findViewById(R.id.file_list_option_move_layout);		
			optionCopyTo = (LinearLayout) v.findViewById(R.id.file_list_option_copy_layout);

			optionPublicLink = (LinearLayout) v.findViewById(R.id.file_list_option_public_link_layout);
			optionPublicLink.setVisibility(View.GONE);
//				holder.optionPublicLink.getLayoutParams().width = Util.px2dp((60), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);

			optionShare = (LinearLayout) v.findViewById(R.id.file_list_option_share_layout);
			optionShare.setVisibility(View.GONE);
			optionPermissions = (LinearLayout) v.findViewById(R.id.file_list_option_permissions_layout);
			optionPermissions.setVisibility(View.GONE);
			optionDelete = (LinearLayout) v.findViewById(R.id.file_list_option_delete_layout);	
			optionDelete.setVisibility(View.GONE);
			optionRemoveTotal = (LinearLayout) v.findViewById(R.id.file_list_option_remove_layout);
			optionRemoveTotal.setVisibility(View.GONE);
//				holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);

			optionClearShares = (LinearLayout) v.findViewById(R.id.file_list_option_clear_share_layout);
			optionClearShares.setVisibility(View.GONE);	
			optionSendToInbox = (LinearLayout) v.findViewById(R.id.file_list_option_send_inbox_layout);
			optionSendToInbox.setVisibility(View.GONE);	
			
			optionDownload.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionLeaveShare.setOnClickListener(this);
			optionRename.setOnClickListener(this);
			optionMoveTo.setOnClickListener(this);
			optionCopyTo.setOnClickListener(this);			
			optionSendToInbox.setOnClickListener(this);
			optionsOutLayout.setOnClickListener(this);
			optionDelete.setOnClickListener(this);
			
			slidingOptionsPanel.setVisibility(View.INVISIBLE);
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);		
			
			slidingPanelListener = new SlidingUpPanelLayout.PanelSlideListener() {
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
	        };			
			
			slidingOptionsPanel.setPanelSlideListener(slidingPanelListener);	
			
			slidingUploadPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout_upload);
			uploadLayout = (LinearLayout) v.findViewById(R.id.file_list_upload);
			uploadOutLayout = (FrameLayout) v.findViewById(R.id.file_list_out_upload);
			uploadImage = (LinearLayout) v.findViewById(R.id.file_list_upload_image_layout);
			uploadAudio= (LinearLayout) v.findViewById(R.id.file_list_upload_audio_layout);
			uploadVideo = (LinearLayout) v.findViewById(R.id.file_list_upload_video_layout);
			uploadFromSystem = (LinearLayout) v.findViewById(R.id.file_list_upload_from_system_layout);
			
			uploadImage.setOnClickListener(this);
			uploadAudio.setOnClickListener(this);
			uploadVideo.setOnClickListener(this);
			uploadFromSystem.setOnClickListener(this);
			
			uploadOutLayout.setOnClickListener(this);
			
			slidingUploadPanel.setVisibility(View.INVISIBLE);
			slidingUploadPanel.setPanelState(PanelState.HIDDEN);		
			
			slidingUploadPanel.setPanelSlideListener(slidingPanelListener);
			
			return v;
		}
		else{
			log("Grid View");
			
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.file_grid_view_browser);
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
		
			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);

			progressBar = (ProgressBar) v.findViewById(R.id.file_grid_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.file_grid_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_grid_text_layout);
			contentText = (TextView) v.findViewById(R.id.content_grid_text);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(73, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);
			
			emptyImageView.setImageResource(R.drawable.incoming_shares_empty);			
			emptyTextView.setText(R.string.file_browser_empty_incoming_shares);
			
			fabButton = (ImageButton) v.findViewById(R.id.file_upload_button_grid);
			fabButton.setOnClickListener(this);
			fabButton.setVisibility(View.GONE);
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, ManagerActivityLollipop.INCOMING_SHARES_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
				if (mTHash != null){
					adapter.setTransfers(mTHash);
				}
//				adapterList.setNodes(nodes);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
//				adapterList.setNodes(nodes);
			}

			if (parentHandle == -1){
				log("ParentHandle -1");
				findNodes();
				adapter.setParentHandle(-1);
			}
			else{
				adapter.setParentHandle(parentHandle);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				log("ParentHandle: "+parentHandle);

				nodes = megaApi.getChildren(parentNode, orderGetChildren);
			}
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

			if (deepBrowserTree == 0){
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
					progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
				}
				else{					
					contentText.setText(getInfoNode());
				}
//				aB.setTitle(getString(R.string.section_shared_items));
//				log("aB.setHomeAsUpIndicator_59");
//				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
//				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
				}
				else{					
					contentText.setText(getInfoFolder(infoNode));
				};
				aB.setTitle(infoNode.getName());
			}						
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);		

			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);	
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
//			setNodes(nodes);	
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout_grid);
			optionsLayout = (LinearLayout) v.findViewById(R.id.file_grid_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.file_grid_out_options);
			optionRename = (LinearLayout) v.findViewById(R.id.file_grid_option_rename_layout);
			optionLeaveShare = (LinearLayout) v.findViewById(R.id.file_grid_option_leave_share_layout);
			optionDownload = (LinearLayout) v.findViewById(R.id.file_grid_option_download_layout);
			optionProperties = (LinearLayout) v.findViewById(R.id.file_grid_option_properties_layout);
			propertiesText = (TextView) v.findViewById(R.id.file_grid_option_properties_text);		
			optionMoveTo = (LinearLayout) v.findViewById(R.id.file_grid_option_move_layout);		
			optionCopyTo = (LinearLayout) v.findViewById(R.id.file_grid_option_copy_layout);

			optionPublicLink = (LinearLayout) v.findViewById(R.id.file_grid_option_public_link_layout);
			optionPublicLink.setVisibility(View.GONE);
//				holder.optionPublicLink.getLayoutParams().width = Util.px2dp((60), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);

			optionShare = (LinearLayout) v.findViewById(R.id.file_grid_option_share_layout);
			optionShare.setVisibility(View.GONE);
			optionPermissions = (LinearLayout) v.findViewById(R.id.file_grid_option_permissions_layout);
			optionPermissions.setVisibility(View.GONE);
			optionDelete = (LinearLayout) v.findViewById(R.id.file_grid_option_delete_layout);	
			optionDelete.setVisibility(View.GONE);
			optionRemoveTotal = (LinearLayout) v.findViewById(R.id.file_grid_option_remove_layout);
			optionRemoveTotal.setVisibility(View.GONE);
//				holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);

			optionClearShares = (LinearLayout) v.findViewById(R.id.file_grid_option_clear_share_layout);
			optionClearShares.setVisibility(View.GONE);		
			
			optionSendToInbox = (LinearLayout) v.findViewById(R.id.file_grid_option_send_inbox_layout);
			optionSendToInbox.setVisibility(View.GONE);
			
			optionDownload.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionLeaveShare.setOnClickListener(this);
			optionRename.setOnClickListener(this);
			optionMoveTo.setOnClickListener(this);
			optionCopyTo.setOnClickListener(this);			
			optionSendToInbox.setOnClickListener(this);	
			optionsOutLayout.setOnClickListener(this);
			optionDelete.setOnClickListener(this);
			
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
			
			slidingUploadPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout_grid_upload);
			uploadLayout = (LinearLayout) v.findViewById(R.id.file_grid_upload);
			uploadOutLayout = (FrameLayout) v.findViewById(R.id.file_grid_out_upload);
			uploadImage = (LinearLayout) v.findViewById(R.id.file_grid_upload_image_layout);
			uploadAudio= (LinearLayout) v.findViewById(R.id.file_grid_upload_audio_layout);
			uploadVideo = (LinearLayout) v.findViewById(R.id.file_grid_upload_video_layout);
			uploadFromSystem = (LinearLayout) v.findViewById(R.id.file_grid_upload_from_system_layout);
			
			uploadImage.setOnClickListener(this);
			uploadAudio.setOnClickListener(this);
			uploadVideo.setOnClickListener(this);
			uploadFromSystem.setOnClickListener(this);
			
			uploadOutLayout.setOnClickListener(this);
			
			slidingUploadPanel.setVisibility(View.INVISIBLE);
			slidingUploadPanel.setPanelState(PanelState.HIDDEN);
			
			return v;
		}		
	}

//	public void refresh(){
//		log("refresh");
//		//TODO conservar el path
//		findNodes();
//	}
	
	public void refresh (long _parentHandle){
		log("refresh");

		parentHandle = _parentHandle;
		MegaNode parentNode=null;
		if (_parentHandle == -1){

			log("ParentHandle -1");
			findNodes();
			adapter.setParentHandle(-1);

			aB.setTitle(getString(R.string.section_shared_items));
			log("aB.setHomeAsUpIndicator_112");
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
		}
		else{
			if (megaApi.getNodeByHandle(parentHandle) == null){
				findNodes();

				parentHandle = -1;
				adapter.setParentHandle(-1);

				aB.setTitle(getString(R.string.section_shared_items));
				log("aB.setHomeAsUpIndicator_111");
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}
			else {
				adapter.setParentHandle(_parentHandle);
				parentNode = megaApi.getNodeByHandle(_parentHandle);
				log("ParentHandle: " + _parentHandle);

				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				adapter.setNodes(nodes);

				aB.setTitle(parentNode.getName());
				log("aB.setHomeAsUpIndicator_60");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop) context).setFirstNavigationLevel(false);
			}
		}
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		adapter.setPositionClicked(-1);

		if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
			showProgressBar();
		}
		else{
			if(deepBrowserTree==0){
				contentText.setText(getInfoNode());
			}
			else{
				if(parentNode!=null){
					contentText.setText(getInfoFolder(parentNode));
				}

			}
		}

		//If folder has no files
		if (adapter.getItemCount() == 0){
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
			emptyTextView.setText(R.string.file_browser_empty_incoming_shares);

		}
		else{
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
	
	}
	
	public void showUploadPanel(){
		log("showUploadPanel");
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions((ManagerActivityLollipop)context,
		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
		                ManagerActivityLollipop.REQUEST_WRITE_STORAGE);
			}
		}
		
		fabButton.setVisibility(View.GONE);
		slidingUploadPanel.setVisibility(View.VISIBLE);
		slidingUploadPanel.setPanelState(PanelState.EXPANDED);
	}
	
	public void hideUploadPanel(){
		log("hideUploadPanel");
		if(deepBrowserTree==0){
			fabButton.setVisibility(View.GONE);
		}
		else{
			fabButton.setVisibility(View.VISIBLE);
		}
		slidingUploadPanel.setPanelState(PanelState.HIDDEN);
		slidingUploadPanel.setVisibility(View.GONE);
	}
	
	public void showOptionsPanel(MegaNode sNode){
		log("showOptionsPanel");		
	
		this.selectedNode = sNode;
		
		fabButton.setVisibility(View.GONE);
		
		
		if (selectedNode.isFolder()) {
			propertiesText.setText(R.string.general_folder_info);
			optionSendToInbox.setVisibility(View.GONE);	
		}else{
			propertiesText.setText(R.string.general_file_info);
			optionSendToInbox.setVisibility(View.VISIBLE);	
		}			

		int accessLevel = megaApi.getAccess(selectedNode);
		log("Node: "+selectedNode.getName()+" "+accessLevel);
		
		switch (accessLevel) {			
			case MegaShare.ACCESS_FULL: {
				log("access FULL");
				optionDownload.setVisibility(View.VISIBLE);
				optionProperties.setVisibility(View.VISIBLE);
				
				if(selectedNode.isFile()){
					optionLeaveShare.setVisibility(View.GONE);
				}
				else {
					optionLeaveShare.setVisibility(View.VISIBLE);
				}
				optionPublicLink.setVisibility(View.GONE);
				optionRemoveTotal.setVisibility(View.GONE);
				optionClearShares.setVisibility(View.GONE);
				optionRename.setVisibility(View.VISIBLE);
				optionDelete.setVisibility(View.VISIBLE);
				optionMoveTo.setVisibility(View.GONE);
	
				break;
			}
			case MegaShare.ACCESS_READ: {
				log("access read");
				optionDownload.setVisibility(View.VISIBLE);
				optionProperties.setVisibility(View.VISIBLE);	
				optionPublicLink.setVisibility(View.GONE);
				optionRename.setVisibility(View.GONE);
				optionDelete.setVisibility(View.GONE);
				optionRemoveTotal.setVisibility(View.GONE);
				optionClearShares.setVisibility(View.GONE);
				optionMoveTo.setVisibility(View.GONE);
				
				if(selectedNode.isFile()){
					optionLeaveShare.setVisibility(View.GONE);
				}
				else {
					optionLeaveShare.setVisibility(View.VISIBLE);
				}						
				break;
			}
			case MegaShare.ACCESS_READWRITE: {
				log("readwrite");
				optionDownload.setVisibility(View.VISIBLE);
				optionProperties.setVisibility(View.VISIBLE);
				//						holder.shareDisabled.setVisibility(View.VISIBLE);
				optionPublicLink.setVisibility(View.GONE);
				optionRename.setVisibility(View.GONE);
				optionDelete.setVisibility(View.GONE);
				optionRemoveTotal.setVisibility(View.GONE);
				optionClearShares.setVisibility(View.GONE);
				optionMoveTo.setVisibility(View.GONE);
				if(selectedNode.isFile()){
					optionLeaveShare.setVisibility(View.GONE);
				}
				else {
					optionLeaveShare.setVisibility(View.VISIBLE);
				}
				break;
			}
		}
					
		slidingOptionsPanel.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.COLLAPSED);
		log("Show the slidingPanel");
	}
	
	public void hideOptionsPanel(){
		log("hideOptionsPanel");
		
		if(deepBrowserTree==0){
			fabButton.setVisibility(View.GONE);
		}
		else{
			fabButton.setVisibility(View.VISIBLE);
		}
				
		adapter.setPositionClicked(-1);
		slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
		slidingOptionsPanel.setVisibility(View.GONE);
	}
	
	public void showProgressBar(){
		log("showProgressBar");
		downloadInProgress = true;
		progressBar.setVisibility(View.VISIBLE);	
		transferArrow.setVisibility(View.VISIBLE);
		contentText.setText(R.string.text_downloading);
		contentTextLayout.setOnClickListener(this);
	}
	
	public void hideProgressBar(){
		log("hideProgressBar");
		downloadInProgress = false;
		progressBar.setVisibility(View.GONE);
		transferArrow.setVisibility(View.GONE);
		if (deepBrowserTree == 0){		
			contentText.setText(getInfoNode());			
		}
		else{
			MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
			contentText.setText(getInfoFolder(infoNode));			
		}
		contentTextLayout.setOnClickListener(null);
	}
	
	public void updateProgressBar(int progress){
		if(downloadInProgress){
			progressBar.setProgress(progress);
		}
		else{
			showProgressBar();
			progressBar.setProgress(progress);
		}
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
		log("onclick");
		
		switch(v.getId()){
				
			case R.id.btnRight_upload:
			case R.id.btnRight_grid_upload:
				((ManagerActivityLollipop)getActivity()).uploadFile();
				break;
				
			case R.id.file_upload_button:
			case R.id.file_upload_button_grid:{
				log("file_upload_button");
//				((ManagerActivityLollipop)getActivity()).uploadFile();
				showUploadPanel();
				break;			
			}
			
			case R.id.file_list_option_send_inbox_layout:
			case R.id.file_grid_option_send_inbox_layout: {
				log("send to inbox");
				hideOptionsPanel();
				((ManagerActivityLollipop) context).sendToInboxLollipop(selectedNode);
//				ArrayList<Long> handleList = new ArrayList<Long>();
//				handleList.add(selectedNode.getHandle());
//				((ManagerActivityLollipop) context).onFileClick(handleList);
				break;
			}
			
			case R.id.file_list_upload_audio_layout:
			case R.id.file_grid_upload_audio_layout:{
				log("click upload audio");
				hideUploadPanel();
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_GET_CONTENT);
				intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
				intent.setType("audio/*");
				((ManagerActivityLollipop)getActivity()).startActivityForResult(Intent.createChooser(intent, null), ManagerActivityLollipop.REQUEST_CODE_GET);
				break;
			}
			
			case R.id.file_list_upload_video_layout:
			case R.id.file_grid_upload_video_layout:{
				log("click upload video");
				hideUploadPanel();
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_GET_CONTENT);
				intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
				intent.setType("video/*");
				((ManagerActivityLollipop)getActivity()).startActivityForResult(Intent.createChooser(intent, null), ManagerActivityLollipop.REQUEST_CODE_GET);
				break;
			}
			
			case R.id.file_list_upload_image_layout:
			case R.id.file_grid_upload_image_layout:{
				log("click upload image");
				hideUploadPanel();
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_GET_CONTENT);
				intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
				intent.setType("image/*");
				((ManagerActivityLollipop)getActivity()).startActivityForResult(Intent.createChooser(intent, null), ManagerActivityLollipop.REQUEST_CODE_GET);
				break;
			}
			
			case R.id.file_list_upload_from_system_layout:
			case R.id.file_grid_upload_from_system_layout:{
				log("click upload from_system");
				hideUploadPanel();
				Intent intent = new Intent();
				intent.setAction(FileStorageActivityLollipop.Mode.PICK_FILE.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
				intent.setClass(getActivity(), FileStorageActivityLollipop.class);
				((ManagerActivityLollipop)getActivity()).startActivityForResult(intent, ManagerActivityLollipop.REQUEST_CODE_GET_LOCAL);
				break;
			}
			
			case R.id.file_list_out_upload:
			case R.id.file_grid_out_upload:{
				hideUploadPanel();
				break;
			}
			
			case R.id.file_list_out_options:
			case R.id.file_grid_out_options:{
				hideOptionsPanel();
				break;
			}

			case R.id.file_list_option_delete_layout:
			case R.id.file_grid_option_delete_layout: {
				log("Delete option");
				hideOptionsPanel();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());

				((ManagerActivityLollipop) context).moveToTrash(handleList);
				break;
			}
				
			case R.id.file_list_option_download_layout: 
			case R.id.file_grid_option_download_layout: {
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
			case R.id.file_list_option_leave_share_layout: 
			case R.id.file_grid_option_leave_share_layout: {
				log("Leave share option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).leaveIncomingShare(selectedNode);
				break;
			}
			case R.id.file_list_option_move_layout:
			case R.id.file_grid_option_move_layout:{
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
			case R.id.file_list_option_properties_layout:
			case R.id.file_grid_option_properties_layout: {
				log("Properties option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				Intent i = new Intent(context, FilePropertiesActivityLollipop.class);
				i.putExtra("handle", selectedNode.getHandle());
				i.putExtra("from", FilePropertiesActivityLollipop.FROM_INCOMING_SHARES);
				
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
			case R.id.file_list_option_rename_layout: 
			case R.id.file_grid_option_rename_layout: {
				log("Rename option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).showRenameDialog(selectedNode, selectedNode.getName());
				break;
			}	
			case R.id.file_list_option_copy_layout: 
			case R.id.file_grid_option_copy_layout: {
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
    	log("itemClick");
    	
		if (adapter.isMultipleSelect()){
			adapter.toggleSelection(position);
			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
				adapter.notifyDataSetChanged();
			}
			else{
				hideMultipleSelect();
			}
		}
		else{
			if (nodes.get(position).isFolder()){
				log("Is folder");
				deepBrowserTree = deepBrowserTree+1;
				
				MegaNode n = nodes.get(position);
				
				aB.setTitle(n.getName());
				log("aB.setHomeAsUpIndicator_61");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = nodes.get(position).getHandle();
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
														
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
				}
				else{					
					contentText.setText(getInfoFolder(infoNode));
				}
				((ManagerActivityLollipop)context).setParentHandleIncoming(parentHandle);
				adapter.setParentHandle(parentHandle);
				nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
				adapter.setNodes(nodes);
				setPositionClicked(-1);
				
				//If folder has no files
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
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
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
				
				//Check the folder's permissions
				int accessLevel= megaApi.getAccess(n);
				log("Node: "+n.getName());
																		
				switch(accessLevel){
					case MegaShare.ACCESS_OWNER:
					case MegaShare.ACCESS_READWRITE:
					case MegaShare.ACCESS_FULL:{
						fabButton.setVisibility(View.VISIBLE);						
						break;
					}
					case MegaShare.ACCESS_READ:{
						fabButton.setVisibility(View.GONE);
						break;
					}						
				}
			}
			else{
				//Is file
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
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
			  		if (ManagerActivityLollipop.isIntentAvailable(context, mediaIntent)){
			  			startActivity(mediaIntent);
			  		}
			  		else{
			  			Toast.makeText(context, getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
			  			adapter.setPositionClicked(-1);
			  			adapter.notifyDataSetChanged();
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						((ManagerActivityLollipop) context).onFileClick(handleList);
			  		}						
				}
				else{
					adapter.setPositionClicked(-1);
					adapter.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					((ManagerActivityLollipop) context).onFileClick(handleList);
				}
			}
		}
	}

	public void findNodes(){
		log("findNodes");
//		deepBrowserTree=0;
//		ArrayList<MegaUser> contacts = megaApi.getContacts();
//		nodes.clear();
//		for (int i=0;i<contacts.size();i++){
//			ArrayList<MegaNode> nodeContact=megaApi.getInShares(contacts.get(i));
//			if(nodeContact!=null){
//				if(nodeContact.size()>0){
//					nodes.addAll(nodeContact);
//				}
//			}
//		}
		nodes=megaApi.getInShares();
		for(int i=0;i<nodes.size();i++){
			log("NODE: "+nodes.get(i).getName());
		}

		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByMailDescending();
		}

		adapter.setNodes(nodes);

		if (adapter.getItemCount() == 0){
			log("adapter.getItemCount() = 0");
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
		}
		else{
			log("adapter.getItemCount() != 0");
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}

		if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
			showProgressBar();
			progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
		}
		else{
			contentText.setText(getInfoNode());
		}
	}

	public void selectAll(){
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
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaNode> documents = adapter.getSelectedNodes();
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
	public void hideMultipleSelect() {
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		log("onBackPressed deepBrowserTree:"+deepBrowserTree);
		
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
		
		if(slidingOptionsPanel.getPanelState()!=PanelState.HIDDEN||slidingOptionsPanel.getVisibility()==View.VISIBLE){
			log("PanelState NOT HIDDEN or VISIBLE");
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
		
		if(slidingUploadPanel.getVisibility()==View.VISIBLE){
			log("hideUploadPanel");
			hideUploadPanel();
			return 4;
		}
		
		log("deepBrowserTree-1");
		deepBrowserTree = deepBrowserTree-1;
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		if(deepBrowserTree==0){
			//In the beginning of the navigation
			log("deepTree==0");
			((ManagerActivityLollipop)context).setParentHandleIncoming(-1);
			fabButton.setVisibility(View.GONE);
			parentHandle=-1;
			aB.setTitle(getString(R.string.section_shared_items));	
			log("aB.setHomeAsUpIndicator_62");
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			findNodes();
//				adapterList.setNodes(nodes);
			recyclerView.setVisibility(View.VISIBLE);
			if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
				showProgressBar();
			}
			else{					
				contentText.setText(getInfoNode());
			}

			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			return 3;
		}
		else if (deepBrowserTree>0){
			log("deepTree>0");
			parentHandle = adapter.getParentHandle();
			//((ManagerActivityLollipop)context).setParentHandleSharedWithMe(parentHandle);	
			fabButton.setVisibility(View.VISIBLE);
			
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));	
			if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
				showProgressBar();
			}
			else{					
				contentText.setText(getInfoFolder(parentNode));
			}

			if (parentNode != null){
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				aB.setTitle(parentNode.getName());	
				log("aB.setHomeAsUpIndicator_63");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = parentNode.getHandle();
				((ManagerActivityLollipop)context).setParentHandleIncoming(parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				//TODO
				adapter.setNodes(nodes);
				setPositionClicked(-1);
				adapter.setParentHandle(parentHandle);
				
				//Check the folder's permissions
				int accessLevel= megaApi.getAccess(parentNode);
				log("Node: "+parentNode.getName());
																		
				switch(accessLevel){
					case MegaShare.ACCESS_OWNER:
					case MegaShare.ACCESS_READWRITE:
					case MegaShare.ACCESS_FULL:{
						fabButton.setVisibility(View.VISIBLE);
						break;
					}
					case MegaShare.ACCESS_READ:{
						fabButton.setVisibility(View.GONE);
						break;
					}						
				}			
				
				return 2;
			}	
			return 2;
		}
		else{
			log("ELSE deepTree");
//			((ManagerActivityLollipop)context).setParentHandleBrowser(megaApi.getRootNode().getHandle());
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			deepBrowserTree=0;
			return 0;
		}
	}
	
	public long getParentHandle(){
		if (adapter != null){
			return adapter.getParentHandle();
		}
		else{
			return -1;
		}
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
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
		if (adapter != null){
			adapter.setPositionClicked(positionClicked);
		}	
	}
	
	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
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
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		log("setNodes");
		this.nodes = nodes;
		adapter.setNodes(nodes);
	}
	
	public void sortByMailDescending(){
		log("sortByNameDescending");
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

		Collections.reverse(folderNodes);
		Collections.reverse(fileNodes);

		nodes.clear();
		nodes.addAll(folderNodes);
		nodes.addAll(fileNodes);
	}

	
//	public void sortByMailAscending(){
//		log("sortByNameAscending");
//
//		ArrayList<MegaNode> folderNodes = new ArrayList<MegaNode>();
//		ArrayList<MegaNode> fileNodes = new ArrayList<MegaNode>();
//
//		for (int i=0;i<nodes.size();i++){
//			if (nodes.get(i).isFolder()){
//				folderNodes.add(nodes.get(i));
//			}
//			else{
//				fileNodes.add(nodes.get(i));
//			}
//		}
//
//		Collections.reverse(folderNodes);
//		Collections.reverse(fileNodes);
//
//		nodes.clear();
//		nodes.addAll(folderNodes);
//		nodes.addAll(fileNodes);
//
//		adapter.setNodes(nodes);
//	}
//
	public int getItemCount(){
		if(adapter != null){
			return adapter.getItemCount();
		}
		return 0;
	}
	
	public void setTransfers(HashMap<Long, MegaTransfer> _mTHash){
		this.mTHash = _mTHash;
		
		if (adapter != null){
			adapter.setTransfers(mTHash);
		}	
	}
	
	public void setCurrentTransfer(MegaTransfer mT){
		if (adapter != null){
			adapter.setCurrentTransfer(mT);
		}		
	}
	
	private static void log(String log) {
		Util.log("IncomingSharesFragmentLollipop", log);
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

	public int getDeepBrowserTree() {
		return deepBrowserTree;
	}

	public void setDeepBrowserTree(int deepBrowserTree) {
		log("setDeepBrowserTree:" + deepBrowserTree);
		this.deepBrowserTree = deepBrowserTree;
	}

}
