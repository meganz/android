package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.CreateThumbPreviewService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MegaStreamingService;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.DrawerItem;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;


public class InboxFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

	public static int GRID_WIDTH =400;
	
	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	RecyclerView.LayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	MegaBrowserLollipopAdapter adapter;
	public InboxFragmentLollipop inboxFragment = this;
	MegaNode inboxNode;
	boolean isList = true;
	long parentHandle = -1;
	int orderGetChildren;
	
	ArrayList<MegaNode> nodes;
	MegaNode selectedNode;
	
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	RelativeLayout contentTextLayout;
	boolean downloadInProgress = false;
	ProgressBar progressBar;
	ImageView transferArrow;
	
	MegaApiAndroid megaApi;
	
	private ActionMode actionMode;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	DatabaseHandler dbH;
	MegaPreferences prefs;

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
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList);
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
					NodeController nC = new NodeController(context);
					nC.copyNodes(handleList);
					break;
				}	
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					NodeController nC = new NodeController(context);
					nC.moveNodes(handleList);
					break;
				}
				case R.id.cab_menu_trash:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
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
			
			// Rename
			if((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)) {
				showRename = true;
			}
			
			if (selected.size() > 0) {
				showDownload = true;
				showTrash = true;
				showMove = true;
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
			if (showTrash){
				menu.findItem(R.id.cab_menu_trash).setTitle(context.getString(R.string.context_move_to_trash));
			}
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			
			return false;
		}		
	}
	
	public boolean showSelectMenuItem(){
		if (adapter != null){
			return adapter.isMultipleSelect();
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
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");

		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}

		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;

		orderGetChildren = ((ManagerActivityLollipop)context).getOrderCloud();

		if (parentHandle == -1){

			long parentHandleInbox = ((ManagerActivityLollipop)context).getParentHandleInbox();
			if(parentHandleInbox!=-1){
				log("After consulting... the parent is: "+parentHandleInbox);
				parentHandle = parentHandleInbox;
			}
		}

		if (parentHandle == -1||parentHandle==megaApi.getInboxNode().getHandle()) {
			log("parentHandle -1");
			if (megaApi.getInboxNode() != null){
				parentHandle = megaApi.getInboxNode().getHandle();
				inboxNode = megaApi.getInboxNode();
				//		((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
				nodes = megaApi.getChildren(inboxNode, orderGetChildren);
			}
			aB.setTitle(getString(R.string.section_inbox));
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);

		}
		else{
			log("parentHandle: " + parentHandle);
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

			if(parentNode!=null){
				log("parentNode: "+parentNode.getName());
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
				aB.setTitle(parentNode.getName());
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
			}

		}
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
	    isList = ((ManagerActivityLollipop)context).isList();
	    
		if (isList){
			View v = inflater.inflate(R.layout.fragment_inboxlist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.inbox_list_view);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.addOnItemTouchListener(this);
			recyclerView.setItemAnimator(new DefaultItemAnimator());      
	
			emptyImageView = (ImageView) v.findViewById(R.id.inbox_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.inbox_list_empty_text);
			emptyImageView.setImageResource(R.drawable.inbox_empty);
			emptyTextView.setText(R.string.file_browser_empty_folder);
			
			progressBar = (ProgressBar) v.findViewById(R.id.inbox_list_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.inbox_list_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);
			
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.inbox_list_content_text_layout);
			contentText = (TextView) v.findViewById(R.id.inbox_list_content_text);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(73, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, ManagerActivityLollipop.INBOX_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}	

			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);
			
			if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
				showProgressBar();
				progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
			}
			else{					
				contentText.setText(getInfoFolder(inboxNode));
			}			
			
			setNodes(nodes);

			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_inboxgrid, container, false);
    
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
		    
		    detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.inbox_grid_view);
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

	        emptyImageView = (ImageView) v.findViewById(R.id.inbox_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.inbox_grid_empty_text);
			emptyImageView.setImageResource(R.drawable.inbox_empty);
			emptyTextView.setText(R.string.empty_inbox);
			
			progressBar = (ProgressBar) v.findViewById(R.id.inbox_grid_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.inbox_grid_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);
			
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.inbox_grid_content_text_layout);
			contentText = (TextView) v.findViewById(R.id.inbox_content_grid_text);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(73, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, ManagerActivityLollipop.INBOX_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			
			adapter.setPositionClicked(-1);
			recyclerView.setAdapter(adapter);

			if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
				showProgressBar();
				progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
			}
			else{					
				contentText.setText(getInfoFolder(inboxNode));
			}
			
			setNodes(nodes);

			return v;	
		}
	}
	
	public void refresh(){
		log("refresh");
		if(parentHandle==-1||parentHandle==inboxNode.getHandle()){
			nodes = megaApi.getChildren(inboxNode, orderGetChildren);
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode!=null){
				log("parentNode: "+parentNode.getName());
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
			}
		}
		setNodes(nodes);
		if(adapter != null){				
			adapter.notifyDataSetChanged();
		}		
	}

	public void showProgressBar(){
		log("showProgressBar");
		downloadInProgress = true;
		if(progressBar!=null){
			progressBar.setVisibility(View.VISIBLE);
			transferArrow.setVisibility(View.VISIBLE);
		}
		contentText.setText(R.string.text_downloading);
		contentTextLayout.setOnClickListener(this);
	}
	
	public void hideProgressBar(){
		log("hideProgressBar");
		downloadInProgress = false;
		progressBar.setVisibility(View.GONE);	
		transferArrow.setVisibility(View.GONE);
		contentText.setText(getInfoFolder(inboxNode));
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

		switch(v.getId()){
			case R.id.inbox_list_content_text_layout:
			case R.id.inbox_grid_content_text_layout:{
				log("click show transfersFragment");
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					((ManagerActivityLollipop)getActivity()).selectDrawerItemLollipop(DrawerItem.TRANSFERS);
				}				
				break;
			}
		}
	}	

    public void itemClick(int position) {
		log("itemClick");

		if (adapter.isMultipleSelect()){
			adapter.toggleSelection(position);
			List<MegaNode> selectedDocuments = adapter.getSelectedNodes();
			if (selectedDocuments.size() > 0){
				updateActionModeTitle();
				adapter.notifyDataSetChanged();
			}
			else{
				hideMultipleSelect();
			}
		}
		else{

			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);

				aB.setTitle(n.getName());
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
//				((ManagerActivityLollipop)context).setParentHandleBrowser(parentHandle);
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

					if (megaApi.getInboxNode().getHandle()==n.getHandle()) {
						emptyImageView.setImageResource(R.drawable.inbox_empty);
						emptyTextView.setText(R.string.empty_inbox);
					} else {
						emptyImageView.setImageResource(R.drawable.ic_empty_folder);
						emptyTextView.setText(R.string.file_browser_empty_folder);
					}
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
			else{
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", ManagerActivityLollipop.RUBBISH_BIN_ADAPTER);
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_RUBBISH){
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
					if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
						startActivity(mediaIntent);
					}
					else{
						Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
						adapter.setPositionClicked(-1);
						adapter.notifyDataSetChanged();
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList);
					}
				}
				else{
					adapter.setPositionClicked(-1);
					adapter.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList);
				}
			}
		}
    }
	
	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaNode> documents = adapter.getSelectedNodes();
		int files = documents.size();

		Resources res = getActivity().getResources();
		String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));		
		String title = filesStr;

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
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
		updateActionModeTitle();
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
		else{
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
			if (parentNode != null) {
				log("ParentNode: "+parentNode.getName());
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				if (parentNode.getHandle() == megaApi.getInboxNode().getHandle()){
					aB.setTitle(getString(R.string.section_inbox));
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				}
				else{
					aB.setTitle(parentNode.getName());
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				}

				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

				parentHandle = parentNode.getHandle();
				((ManagerActivityLollipop)context).setParentHandleInbox(parentHandle);
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
	
	private String getInfoFolder(MegaNode n) {
		int numFiles = megaApi.getNumChildFiles(n);

		String info = "";
		
		info = numFiles
				+ " "
				+ context.getResources().getQuantityString(
						R.plurals.general_num_files, numFiles);
		

		return info;
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	public long getParentHandle(){
		return adapter.getParentHandle();
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
	
	public void setNodes(ArrayList<MegaNode> nodes){
		log("setNodes");
		this.nodes = nodes;
		if (adapter != null){
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (megaApi.getInboxNode().getHandle()==parentHandle) {
					emptyImageView.setImageResource(R.drawable.inbox_empty);
					emptyTextView.setText(R.string.empty_inbox);
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
	
	public void setOrder(int orderGetChildren){
		log("setOrder:Inbox");
		this.orderGetChildren = orderGetChildren;
	}
	
	private static void log(String log) {
		Util.log("InboxFragmentLollipop", log);
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
		if(adapter != null){
			return adapter.getItemCount();
		}
		return 0;
	}

	public void resetAdapter(){
		log("resetAdapter");
		if(adapter!=null){
			adapter.setPositionClicked(-1);
		}
	}
}
