package mega.privacy.android.app.lollipop;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.CreateThumbPreviewService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
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
import nz.mega.sdk.MegaUser;
import android.Manifest;
import android.annotation.SuppressLint;
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


public class FileBrowserFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

	public static int GRID_WIDTH =400;
		
	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	GestureDetectorCompat detector;
	ImageView transferArrow;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserLollipopAdapter adapter;
	FileBrowserFragmentLollipop fileBrowserFragment = this;
	TextView contentText;	
	RelativeLayout contentTextLayout;
	boolean downloadInProgress = false;
	ProgressBar progressBar;
	
	MegaApiAndroid megaApi;		

	long parentHandle = -1;
	boolean isList = true;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	DatabaseHandler dbH;
	MegaPreferences prefs;
	
	ArrayList<MegaNode> nodes;
	
	HashMap<Long, MegaTransfer> mTHash = null;
	
	private ActionMode actionMode;

    ImageButton fabButton;
	private RecyclerView.LayoutManager mLayoutManager;
	MegaNode selectedNode = null;
	
	SlidingUpPanelLayout.PanelSlideListener slidingPanelListener;
	
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
	public LinearLayout optionSendToInbox;	
	public LinearLayout optionMoveTo;
	public LinearLayout optionCopyTo;	
	public TextView propertiesText;
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
				case R.id.cab_menu_send_file:{
					//Check that all the selected options are files
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						if(documents.get(i).isFile()){
							handleList.add(documents.get(i).getHandle());
						}
					}
					clearSelections();
					hideMultipleSelect();
					log("sendToInbox no of files: "+handleList.size());
					((ManagerActivityLollipop) context).sendToInboxLollipop(handleList);					
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
			
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			boolean showShare = true;
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
					if(showShare){
						if(selected.get(i).isFile()){
							showShare = false;
						}
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
				showShare = false;
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			menu.findItem(R.id.cab_menu_share).setVisible(showShare);
			
			menu.findItem(R.id.cab_menu_send_file).setVisible(true);
			
			for(int i=0;i<selected.size();i++){
				MegaNode n = selected.get(i);
				if(n.isFolder()){
					menu.findItem(R.id.cab_menu_send_file).setVisible(false);
					break;
				}
			}
			
			return false;
		}
		
	}
			
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		
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
	    
		if (parentHandle == -1){
			parentHandle = megaApi.getRootNode().getHandle();
			((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);

			nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
			log("aB.setTitle fbF 1");
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
//			aB.setTitle(getString(R.string.section_cloud_drive));	
//			log("aB.setHomeAsUpIndicator_66");
//			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
//			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
//			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);

			nodes = megaApi.getChildren(parentNode, orderGetChildren);
			
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			
//			if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
//				log("aB.setTitle fbF 2");
//				aB.setTitle(getString(R.string.section_cloud_drive));
//				log("aB.setHomeAsUpIndicator_67");
//				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
//				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
//			}
//			else{
//				aB.setTitle(parentNode.getName());	
//				log("aB.setHomeAsUpIndicator_68");
//				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
//				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
//			}
//			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		}	
		
		if (Util.CREATE_THUMB_PREVIEW_SERVICE){
			if (context != null){
				Intent intent = new Intent(context, CreateThumbPreviewService.class);
				intent.putExtra(CreateThumbPreviewService.EXTRA_PARENT_HASH, parentHandle);
				context.startService(intent);
			}
		}
				
		if (isList){
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.addOnItemTouchListener(this);
			recyclerView.setItemAnimator(new DefaultItemAnimator()); 
			
			progressBar = (ProgressBar) v.findViewById(R.id.file_list_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.file_list_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);
//			progressBar.setVisibility(View.VISIBLE);
//			transferArrow.setVisibility(View.VISIBLE);
			
			fabButton = (ImageButton) v.findViewById(R.id.file_upload_button);
			fabButton.setOnClickListener(this);
			
			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_text_layout);
			contentText = (TextView) v.findViewById(R.id.content_text);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(68, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, ManagerActivityLollipop.FILE_BROWSER_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				adapter.setNodes(nodes);				
			}
			
			if (mTHash != null){
				adapter.setTransfers(mTHash);
			}
			
			if (parentHandle == megaApi.getRootNode().getHandle()||parentHandle==-1){
				MegaNode infoNode = megaApi.getRootNode();
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
					progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
				}
				else{					
					contentText.setText(getInfoFolder(infoNode));
				}
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
					progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
				}
				else{					
					contentText.setText(getInfoFolder(infoNode));
				}
			}						
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);			
			
			setNodes(nodes);
			
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
			
			optionDownload.setOnClickListener(this);
			optionShare.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionRename.setOnClickListener(this);
			optionDelete.setOnClickListener(this);
			optionRemoveTotal.setOnClickListener(this);
			optionPublicLink.setOnClickListener(this);
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
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
			recyclerView.setClipToPadding(false);

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
			
			progressBar = (ProgressBar) v.findViewById(R.id.file_grid_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.file_grid_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);
			
			fabButton = (ImageButton) v.findViewById(R.id.file_upload_button_grid);
			fabButton.setOnClickListener(this);
			
			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);			
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_grid_text_layout);

			contentText = (TextView) v.findViewById(R.id.content_grid_text);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(78, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, ManagerActivityLollipop.FILE_BROWSER_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
				adapter.setNodes(nodes);
			}
			
			if (mTHash != null){
				adapter.setTransfers(mTHash);
			}
			
			if (parentHandle == megaApi.getRootNode().getHandle()){
				MegaNode infoNode = megaApi.getRootNode();
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
					progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
				}
				else{					
					contentText.setText(getInfoFolder(infoNode));
				}
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					showProgressBar();
					progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
				}
				else{					
					contentText.setText(getInfoFolder(infoNode));
				}
			}						
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);			
			
			setNodes(nodes);
			
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
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout_grid);
			optionsLayout = (LinearLayout) v.findViewById(R.id.file_grid_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.file_grid_out_options);
			optionRename = (LinearLayout) v.findViewById(R.id.file_grid_option_rename_layout);
			optionRename.setVisibility(View.GONE);
			optionLeaveShare = (LinearLayout) v.findViewById(R.id.file_grid_option_leave_share_layout);
			optionLeaveShare.setVisibility(View.GONE);
			
			optionDownload = (LinearLayout) v.findViewById(R.id.file_grid_option_download_layout);
			optionProperties = (LinearLayout) v.findViewById(R.id.file_grid_option_properties_layout);
			propertiesText = (TextView) v.findViewById(R.id.file_grid_option_properties_text);			

			optionPublicLink = (LinearLayout) v.findViewById(R.id.file_grid_option_public_link_layout);
//				holder.optionPublicLink.getLayoutParams().width = Util.px2dp((60), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);

			optionShare = (LinearLayout) v.findViewById(R.id.file_grid_option_share_layout);
			optionPermissions = (LinearLayout) v.findViewById(R.id.file_grid_option_permissions_layout);
			
			optionDelete = (LinearLayout) v.findViewById(R.id.file_grid_option_delete_layout);			
			optionRemoveTotal = (LinearLayout) v.findViewById(R.id.file_grid_option_remove_layout);

//				holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);

			optionClearShares = (LinearLayout) v.findViewById(R.id.file_grid_option_clear_share_layout);	
			optionMoveTo = (LinearLayout) v.findViewById(R.id.file_grid_option_move_layout);		
			optionCopyTo = (LinearLayout) v.findViewById(R.id.file_grid_option_copy_layout);
			optionSendToInbox = (LinearLayout) v.findViewById(R.id.file_grid_option_send_inbox_layout);	
			
			optionDownload.setOnClickListener(this);
			optionShare.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionRename.setOnClickListener(this);
			optionDelete.setOnClickListener(this);
			optionRemoveTotal.setOnClickListener(this);
			optionPublicLink.setOnClickListener(this);
			optionMoveTo.setOnClickListener(this);
			optionCopyTo.setOnClickListener(this);
			optionSendToInbox.setOnClickListener(this);
			
			optionsOutLayout.setOnClickListener(this);
			
			slidingOptionsPanel.setVisibility(View.INVISIBLE);
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);		
			
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
			
			/*slidingOptionsPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
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
	        });*/
			
			
			
			/*
			Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
			DisplayMetrics outMetrics = new DisplayMetrics ();
		    display.getMetrics(outMetrics);
		    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
			
		    float scaleW = Util.getScaleW(outMetrics, density);
		    float scaleH = Util.getScaleH(outMetrics, density);
		    
		    int totalWidth = outMetrics.widthPixels;
		    int totalHeight = outMetrics.heightPixels;
		    
		    int numberOfCells = totalWidth / GRID_WIDTH;
		    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
		    	if (numberOfCells < 3){
					numberOfCells = 3;
				}	
		    }
		    else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
		    	if (numberOfCells < 2){
					numberOfCells = 2;
				}	
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
			
			getProLayout=(LinearLayout) v.findViewById(R.id.get_pro_account_grid);
			getProText= (TextView) v.findViewById(R.id.get_pro_account_text_grid);
			leftCancelButton = (Button) v.findViewById(R.id.btnLeft_cancel_grid);
			rightUpgradeButton = (Button) v.findViewById(R.id.btnRight_upgrade_grid);
			leftCancelButton.setOnClickListener(this);
			rightUpgradeButton.setOnClickListener(this);			
		
			usedSpacePerc=((ManagerActivityLollipop)context).getUsedPerc();
			
			if(usedSpacePerc>95){
				//Change below of ListView
				log("usedSpacePerc>95");
				buttonsLayout.setVisibility(View.GONE);				
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
			
			
//		    Toast.makeText(context, totalWidth + "x" + totalHeight + "= " + numberOfCells, Toast.LENGTH_LONG).show();
			
		    if (adapterGrid == null){
				adapterGrid = new MegaBrowserNewGridAdapter(context, nodes, parentHandle, listView, aB, numberOfCells, ManagerActivityLollipop.FILE_BROWSER_ADAPTER, orderGetChildren, emptyImageView, emptyTextView, leftNewFolder, rightUploadButton, contentText);
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
			}		*/		
			
			return v;
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
		fabButton.setVisibility(View.VISIBLE);
		slidingUploadPanel.setPanelState(PanelState.HIDDEN);
		slidingUploadPanel.setVisibility(View.GONE);
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
		setContentText();
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
	
	public void showOptionsPanel(MegaNode sNode){
		log("showOptionsPanel");
		
		fabButton.setVisibility(View.GONE);
		
		this.selectedNode = sNode;
		
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
		optionDelete.setVisibility(View.VISIBLE);
		optionPublicLink.setVisibility(View.VISIBLE);
		optionDelete.setVisibility(View.VISIBLE);
		optionRename.setVisibility(View.VISIBLE);
		optionMoveTo.setVisibility(View.VISIBLE);
		optionCopyTo.setVisibility(View.VISIBLE);
		
		//Hide
		optionClearShares.setVisibility(View.GONE);
		optionRemoveTotal.setVisibility(View.GONE);
		optionPermissions.setVisibility(View.GONE);
					
		slidingOptionsPanel.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.COLLAPSED);
		log("Show the slidingPanel");
	}
	
	public void hideOptionsPanel(){
		log("hideOptionsPanel");
				
		adapter.setPositionClicked(-1);
		fabButton.setVisibility(View.VISIBLE);
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
		
	@SuppressLint("InlinedApi")
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
//				((ManagerActivityLollipop)getActivity()).uploadFile();
				showUploadPanel();
				break;			
			}
			
			case R.id.file_list_upload_audio_layout:
			case R.id.file_grid_upload_audio_layout:{
				log("click upload audio");
				hideUploadPanel();
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
				intent.setAction(Intent.ACTION_GET_CONTENT);
				intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
				intent.setType("*/*");
				((ManagerActivityLollipop)getActivity()).startActivityForResult(Intent.createChooser(intent, null), ManagerActivityLollipop.REQUEST_CODE_GET);
							    
				break;
			}
			
			case R.id.file_list_upload_video_layout:
			case R.id.file_grid_upload_video_layout:{
				log("click upload video");
				hideUploadPanel();
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
				intent.setAction(Intent.ACTION_GET_CONTENT);
				intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
				intent.setType("*/*");
				((ManagerActivityLollipop)getActivity()).startActivityForResult(Intent.createChooser(intent, null), ManagerActivityLollipop.REQUEST_CODE_GET);
				break;
			}
			
			case R.id.file_list_upload_image_layout:
			case R.id.file_grid_upload_image_layout:{
				log("click upload image");
				hideUploadPanel();
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
				intent.setAction(Intent.ACTION_GET_CONTENT);
				intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
				intent.setType("*/*");
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
			
			case R.id.file_list_option_download_layout:
			case R.id.file_grid_option_download_layout: {
				log("Download option");
				hideOptionsPanel();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());
				((ManagerActivityLollipop) context).onFileClick(handleList);
				break;
			}
			
			case R.id.file_list_option_send_inbox_layout:
			case R.id.file_grid_option_send_inbox_layout: {
				hideOptionsPanel();
				((ManagerActivityLollipop) context).sendToInboxLollipop(selectedNode);
//				ArrayList<Long> handleList = new ArrayList<Long>();
//				handleList.add(selectedNode.getHandle());
//				((ManagerActivityLollipop) context).onFileClick(handleList);
				break;
			}
//		case R.id.file_list_option_leave_share_layout: {
//			positionClicked = -1;	
//			notifyDataSetChanged();
//			if (type == ManagerActivityLollipop.CONTACT_FILE_ADAPTER) {
//				((ContactPropertiesMainActivity) context).leaveIncomingShare(n);
//			}
//			else
//			{
//				((ManagerActivityLollipop) context).leaveIncomingShare(n);
//			}			
//			//Toast.makeText(context, context.getString(R.string.general_not_yet_implemented), Toast.LENGTH_LONG).show();
//			break;
//		}
			case R.id.file_list_option_move_layout:
			case R.id.file_grid_option_move_layout: {
				log("Move option");
				hideOptionsPanel();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());									
				((ManagerActivityLollipop) context).showMoveLollipop(handleList);
	
				break;
			}
			
			case R.id.file_list_option_properties_layout: 
			case R.id.file_grid_option_properties_layout: {
				log("Properties option");
				hideOptionsPanel();
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
			
			case R.id.file_list_option_delete_layout: 
			case R.id.file_grid_option_delete_layout: {
				log("Delete option");
				hideOptionsPanel();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());
	
				((ManagerActivityLollipop) context).moveToTrash(handleList);
	
				break;
			}
			
			case R.id.file_list_option_public_link_layout: 
			case R.id.file_grid_option_public_link_layout: {
				log("Public link option");
				hideOptionsPanel();
				((ManagerActivityLollipop) context).getPublicLinkAndShareIt(selectedNode);
	
				break;
			}
			
			case R.id.file_list_option_rename_layout: 
			case R.id.file_grid_option_rename_layout: {
				log("Rename option");
				hideOptionsPanel();
				((ManagerActivityLollipop) context).showRenameDialog(selectedNode, selectedNode.getName());
				break;
			}	
			
			case R.id.file_list_option_share_layout: 
			case R.id.file_grid_option_share_layout: {
				log("Share option");
				hideOptionsPanel();
				((ManagerActivityLollipop) context).shareFolderLollipop(selectedNode);
				break;
			}	
			
			case R.id.file_list_option_copy_layout: 
			case R.id.file_grid_option_copy_layout: {
				log("Copy option");
				hideOptionsPanel();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());									
				((ManagerActivityLollipop) context).showCopyLollipop(handleList);
				break;
			}
		}
	}
	
	public void setPositionClicked(int positionClicked){		
		if (adapter!= null){
			adapter.setPositionClicked(positionClicked);
		}			
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
		log("item click position: " + position);
		
		/*if (isList){*/
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
//				adapterList.notifyDataSetChanged();
			}
			else{
				if (nodes.get(position).isFolder()){
					MegaNode n = nodes.get(position);
					String cameraSyncHandle = null;
//					if ((n.getName().compareTo(CameraSyncService.CAMERA_UPLOADS) == 0) && (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT)){
//						((ManagerActivityLollipop)context).cameraUploadsClicked();
//						return;
//					}
					//Check if the item is the Camera Uploads folder
					if(dbH.getPreferences()!=null){
						prefs = dbH.getPreferences();
						if(prefs.getCamSyncHandle()!=null){
							cameraSyncHandle = prefs.getCamSyncHandle();
						}
						else{
							cameraSyncHandle = null;
						}
					}
					else{
						prefs=null;						
					}
					
					if(cameraSyncHandle!=null){
						if(!(cameraSyncHandle.equals("")))
						{
							if ((n.getHandle()==Long.parseLong(cameraSyncHandle))){
								((ManagerActivityLollipop)context).cameraUploadsClicked();
								return;
							}
						}
					}
					else{
						if(n.getName().equals("Camera Uploads")){
							prefs.setCamSyncHandle(String.valueOf(n.getHandle()));
							dbH.setCamSyncHandle(n.getHandle());
							log("FOUND Camera Uploads!!: "+n.getHandle());
							((ManagerActivityLollipop)context).cameraUploadsClicked();
							return;
						}
					}

					//Check if the item is the Media Uploads folder
					
					String secondaryMediaHandle = null;
					
					if(prefs!=null){						
						if(prefs.getMegaHandleSecondaryFolder()!=null){
							secondaryMediaHandle =prefs.getMegaHandleSecondaryFolder();
						}
						else{
							secondaryMediaHandle = null;
						}
					}
					
					if(secondaryMediaHandle!=null){
						if(!(secondaryMediaHandle.equals("")))
						{
							if ((n.getHandle()==Long.parseLong(secondaryMediaHandle))){
								log("Click on Media Uploads");
								((ManagerActivityLollipop)context).secondaryMediaUploadsClicked();
								return;
							}
						}
					}
					else{
						if(n.getName().equals("Media Uploads")){
							prefs.setMegaHandleSecondaryFolder(String.valueOf(n.getHandle()));
							dbH.setSecondaryFolderHandle(n.getHandle());
							log("FOUND Media Uploads!!: "+n.getHandle());
							((ManagerActivityLollipop)context).secondaryMediaUploadsClicked();
							return;
						}
					}
					
					aB.setTitle(n.getName());
					log("aB.setHomeAsUpIndicator_69");
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
					((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
					adapter.setParentHandle(parentHandle);
					nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
					adapter.setNodes(nodes);
					recyclerView.scrollToPosition(0);
					
					if (Util.CREATE_THUMB_PREVIEW_SERVICE){
						if (context != null){
							Intent intent = new Intent(context, CreateThumbPreviewService.class);
							intent.putExtra(CreateThumbPreviewService.EXTRA_PARENT_HASH, parentHandle);
							context.startService(intent);
						}
					}
					
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
				  			startActivity(mediaIntent);
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
		/*}
		else{
			MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
			contentText.setText(getInfoFolder(infoNode));
			
			if (adapter.getItemCount() == 0){				

				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				leftNewFolder.setVisibility(View.VISIBLE);
				rightUploadButton.setVisibility(View.VISIBLE);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				leftNewFolder.setVisibility(View.GONE);
				rightUploadButton.setVisibility(View.GONE);
			}			
		}*/
	}	
	
	public boolean showSelectMenuItem(){
		if (isList){
			if (adapter != null){
				return adapter.isMultipleSelect();
			}
		}
		else{
			if (adapter != null){
				return adapter.isMultipleSelect();
			}
		}
		
		return false;
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
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
//		adapterList.startMultiselection();
//		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
//		for (int i = 0; i < checkedItems.size(); i++) {
//			if (checkedItems.valueAt(i) == true) {
//				int checkedPosition = checkedItems.keyAt(i);
//				listView.setItemChecked(checkedPosition, false);
//			}
//		}
		updateActionModeTitle();
	}
	
	private void updateActionModeTitle() {
		log("updateActionModeTitle");
		if (actionMode == null || getActivity() == null) {
			log("RETURN");
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
			adapter.setPositionClicked(-1);
			notifyDataSetChanged();
			return 4;
		}
		
		if(slidingUploadPanel.getVisibility()==View.VISIBLE){
			hideUploadPanel();
			return 4;
		}
		
		log("Sliding not shown");

		if (isList){
			if (adapter != null){
				parentHandle = adapter.getParentHandle();
				((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
				
				if (adapter.isMultipleSelect()){
					hideMultipleSelect();
					return 3;
				}
				
				if (adapter.getPositionClicked() != -1){
					adapter.setPositionClicked(-1);
					adapter.notifyDataSetChanged();
					return 1;
				}
				else{
					MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				
					if (parentNode != null){
						log("ParentNode: "+parentNode.getName());
						recyclerView.setVisibility(View.VISIBLE);
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);

						if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
							log("aB.setTitle fbF 5");
							aB.setTitle(getString(R.string.section_cloud_drive));	
							log("aB.setHomeAsUpIndicator_70");
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
						}
						else{
							aB.setTitle(parentNode.getName());
							log("aB.setHomeAsUpIndicator_71");
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
							((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
						}
						
						((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
						
						parentHandle = parentNode.getHandle();
						((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
						nodes = megaApi.getChildren(parentNode, orderGetChildren);
						adapter.setNodes(nodes);
						recyclerView.post(new Runnable() 
					    {
					        @Override
					        public void run() 
					        {
					        	recyclerView.scrollToPosition(0);
					            View v = recyclerView.getChildAt(0);
					            if (v != null) 
					            {
					                v.requestFocus();
					            }
					        }
					    });
						adapter.setParentHandle(parentHandle);
						if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
							showProgressBar();
						}
						else{					
							contentText.setText(getInfoFolder(parentNode));
						}
						return 2;
					}
					else{
						return 0;
					}
				}
			}
		}
		else{
			if (adapter != null){
				parentHandle = adapter.getParentHandle();
				((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
				
				if (adapter.isMultipleSelect()){
					hideMultipleSelect();
					return 3;
				}
				
				if (adapter.getPositionClicked() != -1){
					adapter.setPositionClicked(-1);
					adapter.notifyDataSetChanged();
					return 1;
				}
				else{
					MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
					if (parentNode != null){
						recyclerView.setVisibility(View.VISIBLE);
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);

						if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
							log("aB.setTitle fbF 6");
							aB.setTitle(getString(R.string.section_cloud_drive));	
							log("aB.setHomeAsUpIndicator_72");
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
						}
						else{
							aB.setTitle(parentNode.getName());
							log("aB.setHomeAsUpIndicator_73");
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
							((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
						}
						
						((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
						
						parentHandle = parentNode.getHandle();
						((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
						nodes = megaApi.getChildren(parentNode, orderGetChildren);
						adapter.setNodes(nodes);
						recyclerView.post(new Runnable() 
					    {
					        @Override
					        public void run() 
					        {
					        	recyclerView.scrollToPosition(0);
					            View v = recyclerView.getChildAt(0);
					            if (v != null) 
					            {
					                v.requestFocus();
					            }
					        }
					    });
						adapter.setParentHandle(parentHandle);
						if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
							showProgressBar();
						}
						else{					
							contentText.setText(getInfoFolder(parentNode));
						}						
						return 2;
					}
					else{
						return 0;
					}
				}
			}
		}
		
		return 0;
	}
	
	public long getParentHandle(){
		if (isList){
			if (adapter != null){
				return adapter.getParentHandle();
			}
			else{
				return -1;
			}
		}
		else{
			if (adapter != null){
				return adapter.getParentHandle();
			}
			else{
				return -1;
			}
		}
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (isList){
			if (adapter != null){
				adapter.setParentHandle(parentHandle);
			}
		}
		else{
			if (adapter != null){
				adapter.setParentHandle(parentHandle);
			}
		}
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if (isList){
			if (adapter != null){
				adapter.setNodes(nodes);
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);

					if (megaApi.getRootNode().getHandle()==parentHandle) {
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
		}
		else{
			if (adapter != null){
				adapter.setNodes(nodes);
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);

					if (megaApi.getRootNode().getHandle()==parentHandle) {
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
		}
	}
	
	public void notifyDataSetChanged(){
		log("notifyDataSetChanged");
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}		
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
//	public boolean getIsList(){
//		return isList;
//	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
		if (isList){
			if (adapter != null){
				adapter.setOrder(orderGetChildren);
			}
		}
		else{
			if (adapter != null){
				adapter.setOrder(orderGetChildren);
			}
		}
	}
	
	public void setTransfers(HashMap<Long, MegaTransfer> _mTHash){
		this.mTHash = _mTHash;
		
		if (isList){
			if (adapter != null){
				adapter.setTransfers(mTHash);
			}
		}
		else{
			if (adapter != null){
				adapter.setTransfers(mTHash);
			}
		}	
	
	}
	
	public void setCurrentTransfer(MegaTransfer mT){
		if (isList){
			if (adapter != null){
				adapter.setCurrentTransfer(mT);
			}
		}
		else{
			if (adapter != null){
				adapter.setCurrentTransfer(mT);
			}
		}		
	}
	
	private static void log(String log) {
		Util.log("FileBrowserFragmentLollipop", log);
	}
	
	public void setContentText(){
		
		if (megaApi.getRootNode() != null){
			if (parentHandle == megaApi.getRootNode().getHandle()){
				MegaNode infoNode = megaApi.getRootNode();
				if (infoNode !=  null){
					contentText.setText(getInfoFolder(infoNode));
	//				aB.setTitle(getString(R.string.section_cloud_drive));
				}
			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				if (infoNode !=  null){
					contentText.setText(getInfoFolder(infoNode));
	//				aB.setTitle(infoNode.getName());
				}
			}
		}
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
	
	public int getItemCount(){
		if(adapter!=null){
			return adapter.getItemCount();
		}
		return 0;
	}
}
