package nz.mega.android.lollipop;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import nz.mega.android.FullScreenImageViewer;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaStreamingService;
import nz.mega.android.MimeTypeList;
import nz.mega.android.MimeTypeMime;
import nz.mega.android.R;
import nz.mega.android.lollipop.FileBrowserFragmentLollipop.RecyclerViewOnGestureListener;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.components.SlidingUpPanelLayout;
import nz.mega.components.SlidingUpPanelLayout.PanelState;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class SearchFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

	Context context;
	ActionBar aB;
	RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	ImageView emptyImageView;
	TextView emptyTextView;
	MegaBrowserLollipopAdapter adapterList;
	LinearLayout buttonsLayout;
	LinearLayout outSpaceLayout=null;
	LinearLayout getProLayout=null;
	SearchFragmentLollipop searchFragment = this;
	TextView contentText;
	MegaNode selectedNode = null;
	MegaApiAndroid megaApi;
		
	long parentHandle = -1;
	int levels = -1;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	ArrayList<MegaNode> nodes;
	ArrayList<MegaNode> searchNodes;
	String searchQuery = null;
		
	private ActionMode actionMode;
	GestureDetectorCompat detector;
	
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
	
	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

	    public void onLongPress(MotionEvent e) {
	        View view = listView.findChildViewUnder(e.getX(), e.getY());
	        int position = listView.getChildPosition(view);

	        // handle long press
	        if (!adapterList.isMultipleSelect()){
				adapterList.setMultipleSelect(true);
			
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());			

		        itemClick(position);
			}  
	        super.onLongPress(e);
	    }
	}
	////
	
	private class ActionBarCallBack implements ActionMode.Callback {

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
				showLink = true;
			}
			
			if (selected.size() > 0) {
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
				}
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
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
		
		super.onCreate(savedInstanceState);
		log("onCreate");		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		
		if (megaApi.getRootNode() == null){
			return null;
		}
		
		if (parentHandle == -1){
			nodes = megaApi.search(megaApi.getRootNode(), searchQuery, true);
			searchNodes = megaApi.search(megaApi.getRootNode(), searchQuery, true);
			
			aB.setTitle(getString(R.string.action_search)+": "+searchQuery);
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		}
		else{
			MegaNode n = megaApi.getNodeByHandle(parentHandle);
					
			aB.setTitle(n.getName());					
			aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
			
			((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
			nodes = megaApi.getChildren(n, orderGetChildren);
		}
		
		detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
		
		View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
		
		listView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnItemTouchListener(this);
		listView.setItemAnimator(new DefaultItemAnimator()); 
		
		buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_layout);
		buttonsLayout.setVisibility(View.GONE);
		
		outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space);
		outSpaceLayout.setVisibility(View.GONE);
		
		getProLayout=(LinearLayout) v.findViewById(R.id.get_pro_account);
		getProLayout.setVisibility(View.GONE);
				
		contentText = (TextView) v.findViewById(R.id.content_text);
		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
		
		if (adapterList == null){
			adapterList = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, listView, aB, ManagerActivityLollipop.SEARCH_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
		}
		else{
			adapterList.setParentHandle(parentHandle);
			adapterList.setNodes(nodes);
		}
		
		adapterList.setPositionClicked(-1);
		adapterList.setMultipleSelect(false);

		listView.setAdapter(adapterList);
		
		setNodes(nodes);
		
		contentText.setText(getInfoNode());
		
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
//			holder.optionPublicLink.getLayoutParams().width = Util.px2dp((60), outMetrics);
//			((LinearLayout.LayoutParams) holder.optionPublicLink.getLayoutParams()).setMargins(Util.px2dp((17 * scaleW), outMetrics),Util.px2dp((4 * scaleH), outMetrics), 0, 0);

		optionShare = (LinearLayout) v.findViewById(R.id.file_list_option_share_layout);
		optionPermissions = (LinearLayout) v.findViewById(R.id.file_list_option_permissions_layout);
		
		optionDelete = (LinearLayout) v.findViewById(R.id.file_list_option_delete_layout);			
		optionRemoveTotal = (LinearLayout) v.findViewById(R.id.file_list_option_remove_layout);

//			holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
//			((LinearLayout.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);

		optionClearShares = (LinearLayout) v.findViewById(R.id.file_list_option_clear_share_layout);	
		optionMoveTo = (LinearLayout) v.findViewById(R.id.file_list_option_move_layout);		
		optionCopyTo = (LinearLayout) v.findViewById(R.id.file_list_option_copy_layout);			
		
		optionDownload.setOnClickListener(this);
		optionShare.setOnClickListener(this);
		optionProperties.setOnClickListener(this);
		optionRename.setOnClickListener(this);
		optionDelete.setOnClickListener(this);
		optionRemoveTotal.setOnClickListener(this);
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
				
		adapterList.setPositionClicked(-1);
		slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
		slidingOptionsPanel.setVisibility(View.GONE);
	}
	
	public PanelState getPanelState ()
	{
		log("getPanelState: "+slidingOptionsPanel.getPanelState());
		return slidingOptionsPanel.getPanelState();
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
		
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
		case R.id.file_list_out_options:
			hideOptionsPanel();
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
		case R.id.file_list_option_delete_layout: {
			log("Delete option");
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
			slidingOptionsPanel.setVisibility(View.GONE);
			setPositionClicked(-1);
			notifyDataSetChanged();
			ArrayList<Long> handleList = new ArrayList<Long>();
			handleList.add(selectedNode.getHandle());

			((ManagerActivityLollipop) context).moveToTrash(handleList);

			break;
		}
		case R.id.file_list_option_public_link_layout: {
			log("Public link option");
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
			slidingOptionsPanel.setVisibility(View.GONE);
			setPositionClicked(-1);
			notifyDataSetChanged();
			((ManagerActivityLollipop) context).getPublicLinkAndShareIt(selectedNode);

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
		
		case R.id.file_list_option_share_layout: {	
			log("Share option");
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
			slidingOptionsPanel.setVisibility(View.GONE);
			setPositionClicked(-1);
			notifyDataSetChanged();
			((ManagerActivityLollipop) context).shareFolderLollipop(selectedNode);
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
	}
	}
	
    public void itemClick(int position) {
		
		if (adapterList.isMultipleSelect()){
			adapterList.toggleSelection(position);
			updateActionModeTitle();
			adapterList.notifyDataSetChanged();
		}
		else{
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);
				
				aB.setTitle(n.getName());
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			
				parentHandle = nodes.get(position).getHandle();
				((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
				adapterList.setParentHandle(parentHandle);
				nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
				adapterList.setNodes(nodes);
				listView.scrollToPosition(0);
				
				levels++;
				
				//If folder has no files
				if (adapterList.getItemCount() == 0){
					listView.setVisibility(View.GONE);
					contentText.setVisibility(View.GONE);
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
					contentText.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
			}
			else{
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewer.class);
					intent.putExtra("position", position);
					intent.putExtra("searchQuery", searchQuery);
					intent.putExtra("adapterType", ManagerActivityLollipop.SEARCH_ADAPTER);
					if (parentHandle == -1){
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
	
	public void selectAll(){
		log("selectAll");
//		if (isList){
			if(adapterList.isMultipleSelect()){
				adapterList.selectAll();
			}
			else{			
				
				adapterList.setMultipleSelect(true);
				adapterList.selectAll();
				
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
			}
			
			updateActionModeTitle();
//		}
//		else{
//			if (adapterGrid != null){
//				adapterGrid.selectAll();
//			}
//		}
	}
	
	public boolean showSelectMenuItem(){

		if (adapterList != null){
			return adapterList.isMultipleSelect();
		}
		
		return false;
	}
	
	public int onBackPressed(){
		
		
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
		
		parentHandle = adapterList.getParentHandle();
		((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
		
		if (adapterList.isMultipleSelect()){
			hideMultipleSelect();
			return 3;
		}
		
		if (adapterList.getPositionClicked() != -1){
			adapterList.setPositionClicked(-1);
			adapterList.notifyDataSetChanged();
			return 1;
		}
		else{
			if (levels > 0){
				
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
						aB.setTitle(getString(R.string.section_cloud_drive));	
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
					((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					adapterList.setNodes(nodes);
					listView.scrollToPosition(0);
					adapterList.setParentHandle(parentHandle);
					levels--;
					return 2;
				}
				else{
					return 0;
				}
			}
			else if (levels == -1){
				return 0;
			}
			else{
				parentHandle = -1;
				((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
				nodes = megaApi.search(megaApi.getRootNode(), searchQuery, true);
				adapterList.setNodes(nodes);
				listView.scrollToPosition(0);
				adapterList.setParentHandle(parentHandle);
				levels--;
				aB.setTitle(getString(R.string.action_search)+": "+searchQuery);	
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				return 3;
			}
		}
	}
	
	public long getParentHandle(){
		return adapterList.getParentHandle();

	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapterList != null){
			adapterList.setParentHandle(parentHandle);
		}
	}
	
	public int getLevels(){
		return levels;
	}
	
	public void setLevels(int levels){
		this.levels = levels;
	}
	
	public RecyclerView getListView(){
		return listView;
	}
	
	public void setSearchQuery(String searchQuery){
		this.searchQuery = searchQuery;
	}
	
	public void setSearchNodes (ArrayList<MegaNode> searchNodes){
		this.searchNodes = searchNodes;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		
		if (adapterList != null){
			adapterList.setNodes(nodes);
			if (adapterList.getItemCount() == 0){
				listView.setVisibility(View.GONE);
				contentText.setVisibility(View.GONE);
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
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}			
		}
	}
	
	public void setPositionClicked(int positionClicked){
		if (adapterList != null){
			adapterList.setPositionClicked(positionClicked);
		}	
	}
	
	public void notifyDataSetChanged(){
		if (adapterList != null){
			adapterList.notifyDataSetChanged();
		}
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
		
		if (adapterList != null){
			adapterList.setOrder(orderGetChildren);
		}
	}
	
	private static void log(String log) {
		Util.log("SearchFragmentLollipop", log);
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
