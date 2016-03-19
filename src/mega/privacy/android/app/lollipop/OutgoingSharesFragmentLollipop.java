package mega.privacy.android.app.lollipop;

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
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.DrawerItem;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
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


public class OutgoingSharesFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	RecyclerView.LayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserLollipopAdapter adapter;
	OutgoingSharesFragmentLollipop outgoingSharesFragment = this;
	
	SlidingUpPanelLayout.PanelSlideListener slidingPanelListener;
	
	TextView contentText;	
	RelativeLayout contentTextLayout;
	boolean downloadInProgress = false;
	ProgressBar progressBar;
	ImageView transferArrow;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
    ImageButton fabButton;
	
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
	public LinearLayout optionSendToInbox;
	////
	
	//UPLOAD PANEL
	private SlidingUpPanelLayout slidingUploadPanel;
	public FrameLayout uploadOutLayout;
	public LinearLayout uploadLayout;
	public LinearLayout uploadImage;
	public LinearLayout uploadAudio;
	public LinearLayout uploadVideo;
	public LinearLayout uploadFromSystem;	
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
		boolean showTrash = false;
		boolean showShare = false;

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
					hideMultipleSelect();
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
			adapter.setMultipleSelect(false);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();
					
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
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;		
					
	    isList = ((ManagerActivityLollipop)context).isList();
	    
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
			
			emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
			emptyTextView.setText(R.string.file_browser_empty_outgoing_shares);			
			
			fabButton = (ImageButton) v.findViewById(R.id.file_upload_button);
			fabButton.setOnClickListener(this);
			fabButton.setVisibility(View.GONE);
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				if (mTHash != null){
					adapter.setTransfers(mTHash);
				}
			}
			else{
				adapter.setParentHandle(parentHandle);
//				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
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
				
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
//				log("aB.setHomeAsUpIndicator_34");
//				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
//				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
//				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}
			else{
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);			
				
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
//				log("aB.setHomeAsUpIndicator_35");
//				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
//				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
//				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);		
			
			if (adapter != null){
				adapter.setNodes(nodes);
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
			}	
			
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
			
			optionSendToInbox = (LinearLayout) v.findViewById(R.id.file_list_option_send_inbox_layout);
			optionSendToInbox.setVisibility(View.GONE);
			
			optionDownload.setOnClickListener(this);
			optionShare.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionRename.setOnClickListener(this);
			optionClearShares.setOnClickListener(this);
			optionPermissions.setOnClickListener(this);
			optionMoveTo.setOnClickListener(this);
			optionCopyTo.setOnClickListener(this);
			optionSendToInbox.setOnClickListener(this);
			optionsOutLayout.setOnClickListener(this);
			
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
			
			emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
			emptyTextView.setText(R.string.file_browser_empty_outgoing_shares);
						
			fabButton = (ImageButton) v.findViewById(R.id.file_upload_button_grid);
			fabButton.setOnClickListener(this);
			fabButton.setVisibility(View.GONE);
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, ManagerActivityLollipop.OUTGOING_SHARES_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
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
				((ManagerActivityLollipop)context).setParentHandleIncoming(-1);					
				findNodes();	
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					sortByNameDescending();
				}
				else{
					sortByNameAscending();
				}
				
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
//				aB.setTitle(getString(R.string.section_shared_items));
//				log("aB.setHomeAsUpIndicator_36");
//				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
//				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
//				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

				adapter.parentHandle=-1;
			}
			else{
				adapter.parentHandle=parentHandle;
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
				((ManagerActivityLollipop)context).setParentHandleIncoming(parentHandle);

				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
//				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
//					sortByNameDescending();
//				}
//				else{
//					sortByNameAscending();
//				}
				
//				aB.setTitle(parentNode.getName());
//				log("aB.setHomeAsUpIndicator_37");
//				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
//				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
//				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}	

			if (deepBrowserTree == 0){
				
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
					progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
				}
				else{					
					contentText.setText(getInfoNode());
				}
//				aB.setTitle(getString(R.string.section_shared_items));
//				log("aB.setHomeAsUpIndicator_38");
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
				}				
				aB.setTitle(infoNode.getName());
			}						
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);		
			
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
			optionLeaveShare.setVisibility(View.GONE);

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
	
	public void refresh (long _parentHandle){
		MegaNode n = megaApi.getNodeByHandle(_parentHandle);
		if(n == null)
		{
			refresh();
			return;
		}
		
		aB.setTitle(n.getName());
		log("aB.setHomeAsUpIndicator_39");
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		
		if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
			showProgressBar();
		}
		else{					
			contentText.setText(getInfoFolder(n));
		}
		
		adapter.setParentHandle(parentHandle);
		nodes = megaApi.getChildren(n, orderGetChildren);
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
		if(adapter != null){
			log("adapter != null");
			adapter.setNodes(nodes);
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
			optionShare.setVisibility(View.VISIBLE);
			optionSendToInbox.setVisibility(View.GONE);	
		}else{
			propertiesText.setText(R.string.general_file_info);
			optionShare.setVisibility(View.GONE);
			optionSendToInbox.setVisibility(View.VISIBLE);	
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
				
		adapter.setPositionClicked(-1);
		if(deepBrowserTree==0){
			fabButton.setVisibility(View.GONE);
		}
		else{
			fabButton.setVisibility(View.VISIBLE);
		}
		slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
		slidingOptionsPanel.setVisibility(View.GONE);
	}
	
	public PanelState getPanelState ()
	{
		log("getPanelState: "+slidingOptionsPanel.getPanelState());
		return slidingOptionsPanel.getPanelState();
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
		
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {
		log("onClick");
		switch(v.getId()){
		
			case R.id.content_text_layout:
			case R.id.content_grid_text_layout:{
				log("click show transfersFragment");
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					((ManagerActivityLollipop)getActivity()).selectDrawerItemLollipop(DrawerItem.TRANSFERS);
				}				
				break;
			}
			
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
				
			case R.id.file_list_out_options:
			case R.id.file_grid_out_options:{
				hideOptionsPanel();
				break;
			}
			
			case R.id.file_list_out_upload:
			case R.id.file_grid_out_upload:{
				hideUploadPanel();
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
			case R.id.file_list_option_properties_layout:
			case R.id.file_grid_option_properties_layout: {
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
			case R.id.file_list_option_rename_layout: 
			case R.id.file_grid_option_rename_layout:{
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
			case R.id.file_list_option_clear_share_layout: 
			case R.id.file_grid_option_clear_share_layout: {
				log("Clear shares");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<MegaShare> shareList = megaApi.getOutShares(selectedNode);				
				((ManagerActivityLollipop) context).removeAllSharingContacts(shareList, selectedNode);
				break;				
			}
			case R.id.file_list_option_share_layout: 
			case R.id.file_grid_option_share_layout: {
				log("Share option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).shareFolderLollipop(selectedNode);
				break;
			}
			
			case R.id.file_list_option_permissions_layout: 
			case R.id.file_grid_option_permissions_layout: {
				log("Share with");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				Intent i = new Intent(context, FileContactListActivityLollipop.class);
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
				
				deepBrowserTree = deepBrowserTree+1;
				log("deepBrowserTree "+deepBrowserTree);
				
				MegaNode n = nodes.get(position);
									
				aB.setTitle(n.getName());
				log("aB.setHomeAsUpIndicator_40");
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

				((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
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

				fabButton.setVisibility(View.VISIBLE);	
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

	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
			sortByNameDescending();
		}
		else{
			sortByNameAscending();
		}
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

		adapter.setNodes(nodes);
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

		adapter.setNodes(nodes);
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
	void hideMultipleSelect() {
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){

		log("onBackPressed");

		log("deepBrowserTree "+deepBrowserTree);
					
		if (adapter == null){
			return 0;
		}
		
		if (adapter.isMultipleSelect()){
			hideMultipleSelect();
			return 2;
		}
		
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
			adapter.setPositionClicked(-1);
			notifyDataSetChanged();
			return 4;
		}
		
		if(slidingUploadPanel.getVisibility()==View.VISIBLE){
			hideUploadPanel();
			return 4;
		}
		
		deepBrowserTree = deepBrowserTree-1;
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		if(deepBrowserTree==0){
			log("deepBrowserTree==0");
			fabButton.setVisibility(View.GONE);
			//In the beginning of the navigation
			((ManagerActivityLollipop)context).setParentHandleOutgoing(-1);
			parentHandle=-1;
			log("Shared With Me");
			aB.setTitle(getString(R.string.section_shared_items));
			log("aB.setHomeAsUpIndicator_41");
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			findNodes();
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}
			adapter.setNodes(nodes);
			
			if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
				showProgressBar();
			}
			else{					
				contentText.setText(getInfoNode());
			}
			
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);

			return 3;
		}
		else if (deepBrowserTree>0){
			log("Keep navigation");

			fabButton.setVisibility(View.VISIBLE);

			parentHandle = adapter.getParentHandle();
			//((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);			
			
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
				log("aB.setHomeAsUpIndicator_42");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = parentNode.getHandle();
				((ManagerActivityLollipop)context).setParentHandleOutgoing(parentHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				adapter.setNodes(nodes);
				setPositionClicked(-1);
				adapter.setParentHandle(parentHandle);
				return 2;
			}	
			return 2;
		}
		else{
			log("Back to Cloud");
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
//			((ManagerActivityLollipop)context).setParentHandleBrowser(megaApi.getRootNode().getHandle());
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
		if (adapter != null){
			adapter.setOrder(orderGetChildren);
		}
	}
	
	public int getItemCount(){
		if(adapter!=null){
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
