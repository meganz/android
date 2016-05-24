package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.SlidingUpPanelLayout;
import mega.privacy.android.app.components.SlidingUpPanelLayout.PanelState;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.DrawerItem;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;


public class RubbishBinFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

	public static int GRID_WIDTH =400;
	
	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	RecyclerView.LayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	MegaBrowserLollipopAdapter adapter;
	public RubbishBinFragmentLollipop rubbishBinFragment = this;
		
	boolean isList = true;
	long parentHandle = -1;
	int orderGetChildren;
	
	ArrayList<MegaNode> nodes;
	MegaNode selectedNode = null;
	
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
	
	//OPTIONS PANEL
	private SlidingUpPanelLayout slidingOptionsPanel;
	public FrameLayout optionsOutLayout;
	public LinearLayout optionsLayout;
	public LinearLayout optionProperties;
	public LinearLayout optionRemoveTotal;
	public LinearLayout optionMoveTo;
	public TextView propertiesText;
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

				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
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
			showRename = false;
			showLink = false;

			
			if (selected.size() > 0) {
				showTrash = true;
				showMove = true;

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
			if (showTrash){
				menu.findItem(R.id.cab_menu_trash).setTitle(context.getString(R.string.context_remove));
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
		log("onCreate");

		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
		
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

		orderGetChildren = ((ManagerActivityLollipop)context).getOrderCloud();
		isList = ((ManagerActivityLollipop)context).isList();

		if (parentHandle == -1){

			long parentHandleRubbish = ((ManagerActivityLollipop)context).getParentHandleRubbish();
			if(parentHandleRubbish!=-1){
				log("After consulting... the parentRubbish is: "+parentHandleRubbish);
				parentHandle = parentHandleRubbish;
			}
		}
		
		if (parentHandle == -1||parentHandle==megaApi.getRubbishNode().getHandle()){

			if(aB!=null){
				aB.setTitle(getString(R.string.section_rubbish_bin));
				log("indicator_arrow_back_445");
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
			}

			nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			
//			if (parentNode == null){
//				parentNode = megaApi.getRubbishNode();
//				if (parentNode != null){
//					parentHandle = parentNode.getHandle();
//					((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
//				}
//			}
			
			if (parentNode != null){
				log("The parent node is: "+parentNode.getName());
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
			
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

				if(aB!=null){
					aB.setTitle(parentNode.getName());
					log("indicator_arrow_back_035");
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				}
				else {
					log("AB still is NULL");
				}

			}
			nodes = megaApi.getChildren(parentNode, orderGetChildren);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		}

		if (isList){
			log("isList View");
			View v = inflater.inflate(R.layout.fragment_rubbishbinlist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.rubbishbin_list_view);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.addOnItemTouchListener(this);
			recyclerView.setItemAnimator(new DefaultItemAnimator()); 
			
			emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.rubbishbin_list_empty_text);
			emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
			emptyTextView.setText(R.string.file_browser_empty_folder);
			
			progressBar = (ProgressBar) v.findViewById(R.id.rubbishbin_list_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.rubbishbin_list_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);
			
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.rubbishbin_content_text_layout);
			contentText = (TextView) v.findViewById(R.id.rubbishbin_list_content_text);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(73, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.RUBBISH_BIN_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
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
			
			if(megaApi.getRubbishNode()!=null){
				if (parentHandle == megaApi.getRubbishNode().getHandle()){
					if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
						showProgressBar();
						progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
					}
					else{
						MegaNode infoNode = megaApi.getRubbishNode();
						contentText.setText(getInfoFolder(infoNode));
					}				
				}
				else{
					if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
						showProgressBar();
						progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
					}
					else{
						MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
						contentText.setText(getInfoFolder(infoNode));
					}
				}
			}			
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout_rubbish);
			optionsLayout = (LinearLayout) v.findViewById(R.id.rubbishbin_list_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.rubbishbin_list_out_options);
			
			optionProperties = (LinearLayout) v.findViewById(R.id.rubbishbin_list_option_properties_layout);
			propertiesText = (TextView) v.findViewById(R.id.rubbishbin_list_option_properties_text);			
		
			optionRemoveTotal = (LinearLayout) v.findViewById(R.id.rubbishbin_list_option_remove_layout);

//				holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);
	
			optionMoveTo = (LinearLayout) v.findViewById(R.id.rubbishbin_list_option_move_layout);		

			optionProperties.setOnClickListener(this);
			optionRemoveTotal.setOnClickListener(this);
			optionMoveTo.setOnClickListener(this);			
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
			log("isGrid View");
			View v = inflater.inflate(R.layout.fragment_rubbishbingrid, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.rubbishbin_grid_view);
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
			
			emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.rubbishbin_grid_empty_text);
			emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
			emptyTextView.setText(R.string.file_browser_empty_folder);
			
			progressBar = (ProgressBar) v.findViewById(R.id.rubbishbin_grid_download_progress_bar);
			transferArrow = (ImageView) v.findViewById(R.id.rubbishbin_grid_transfer_arrow);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)transferArrow.getLayoutParams();
			lp.setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(4, outMetrics)); 
			transferArrow.setLayoutParams(lp);
			
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.rubbishbin_grid_content_text_layout);
			contentText = (TextView) v.findViewById(R.id.rubbishbin_grid_content_text);			
			//Margins
			RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)contentText.getLayoutParams();
			contentTextParams.setMargins(Util.scaleWidthPx(73, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics)); 
			contentText.setLayoutParams(contentTextParams);			
			
			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, recyclerView, aB, Constants.RUBBISH_BIN_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setParentHandle(parentHandle);
				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			
			if(megaApi.getRubbishNode()!=null){
				if (parentHandle == megaApi.getRubbishNode().getHandle()){
					if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
						showProgressBar();
						progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
					}
					else{
						MegaNode infoNode = megaApi.getRubbishNode();
						contentText.setText(getInfoFolder(infoNode));
					}				
				}
				else{
					if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
						showProgressBar();
						progressBar.setProgress(((ManagerActivityLollipop)context).getProgressPercent());
					}
					else{
						MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
						contentText.setText(getInfoFolder(infoNode));
					}
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
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout_rubbish_grid);
			optionsLayout = (LinearLayout) v.findViewById(R.id.rubbishbin_grid_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.rubbishbin_grid_out_options);
			
			optionProperties = (LinearLayout) v.findViewById(R.id.rubbishbin_grid_option_properties_layout);
			propertiesText = (TextView) v.findViewById(R.id.rubbishbin_grid_option_properties_text);			
		
			optionRemoveTotal = (LinearLayout) v.findViewById(R.id.rubbishbin_grid_option_remove_layout);

//				holder.optionDelete.getLayoutParams().width = Util.px2dp((60 * scaleW), outMetrics);
//				((LinearLayout.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((1 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), 0, 0);
	
			optionMoveTo = (LinearLayout) v.findViewById(R.id.rubbishbin_grid_option_move_layout);		

			optionProperties.setOnClickListener(this);
			optionRemoveTotal.setOnClickListener(this);
			optionMoveTo.setOnClickListener(this);			
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
	
	public void showOptionsPanel(MegaNode sNode){
		log("showNodeOptionsPanel");
		
		this.selectedNode = sNode;
		
		if (selectedNode.isFolder()) {
			propertiesText.setText(R.string.general_folder_info);
		}else{
			propertiesText.setText(R.string.general_file_info);
		}
		
		optionProperties.setVisibility(View.VISIBLE);				
		optionMoveTo.setVisibility(View.VISIBLE);
		optionRemoveTotal.setVisibility(View.VISIBLE);
					
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
		
			case R.id.rubbishbin_content_text_layout:
			case R.id.rubbishbin_grid_content_text_layout:{
				log("click show transfersFragment");
				if(((ManagerActivityLollipop)getActivity()).isTransferInProgress()){
					((ManagerActivityLollipop)getActivity()).selectDrawerItemLollipop(DrawerItem.TRANSFERS);
				}				
				break;
			}
			case R.id.rubbishbin_list_out_options:
			case R.id.rubbishbin_grid_out_options:{
				hideOptionsPanel();
				break;
			}
			case R.id.rubbishbin_list_option_move_layout:
			case R.id.rubbishbin_grid_option_move_layout:{
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());
				NodeController nC = new NodeController(context);
				nC.chooseLocationToMoveNodes(handleList);
				break;
			}
			case R.id.rubbishbin_list_option_properties_layout: 
			case R.id.rubbishbin_grid_option_properties_layout: {
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
				notifyDataSetChanged();
				break;
			}
			case R.id.rubbishbin_list_option_remove_layout: 
			case R.id.rubbishbin_grid_option_remove_layout: {
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
				break;
			}
		}
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
				MegaNode n = nodes.get(position);
				
				aB.setTitle(n.getName());
				log("indicator_arrow_back_190");
				aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = nodes.get(position).getHandle();
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				contentText.setText(getInfoFolder(infoNode));
				((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
				adapter.setParentHandle(parentHandle);
				nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
				adapter.setNodes(nodes);
				recyclerView.scrollToPosition(0);
				
				//If folder has no files
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApi.getRubbishNode().getHandle()==n.getHandle()) {
						emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
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
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", Constants.RUBBISH_BIN_ADAPTER);
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_RUBBISH){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}
					intent.putExtra("orderGetChildren", orderGetChildren);
					startActivity(intent);
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
		
		parentHandle = adapter.getParentHandle();
		((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
		
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
			if (parentNode != null){
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()){
					aB.setTitle(getString(R.string.section_rubbish_bin));	
					log("aB.setHomeAsUpIndicator_47");
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				}
				else{
					aB.setTitle(parentNode.getName());
					log("indicator_arrow_back_191");
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				}
				
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				
				parentHandle = parentNode.getHandle();
				((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
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
				contentText.setText(getInfoFolder(parentNode));
				return 2;
			}
			else{
				return 0;
			}
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
	
	public void setContentText(){
		log("setContentText");
		if (parentHandle == megaApi.getRubbishNode().getHandle()){
			MegaNode infoNode = megaApi.getRubbishNode();
			if (infoNode !=  null){
				contentText.setText(getInfoFolder(infoNode));
			}
		}
		else{
			MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
			if (infoNode !=  null){
				contentText.setText(getInfoFolder(infoNode));
			}
		}
	}
	
	public void setIsList(boolean isList){
		log("setIsList");
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
		
		if(megaApi.getRubbishNode()==null){
			log("megaApi.getRubbishNode() is NULL");
			return;
		}
		
		if (adapter != null){
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRubbishNode().getHandle()==parentHandle) {
					emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
					emptyTextView.setText(R.string.empty_rubbish_bin);
				} else {
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.empty_rubbish_bin);
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
		log("setOrder:Rubbish");
		this.orderGetChildren = orderGetChildren;
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
		return adapter.getItemCount();
	}
	
	private static void log(String log) {
		Util.log("RubbishBinFragmentLollipop", log);
	}
}
