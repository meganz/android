package nz.mega.android.lollipop;

import java.util.ArrayList;
import java.util.List;

import nz.mega.components.SlidingUpPanelLayout;
import nz.mega.components.SlidingUpPanelLayout.PanelState;

import nz.mega.android.ContactPropertiesMainActivity;
import nz.mega.android.FullScreenImageViewer;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaBrowserNewGridAdapter;
import nz.mega.android.MimeTypeList;
import nz.mega.android.MimeTypeMime;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class RubbishBinFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

	public static int GRID_WIDTH =400;
	
	Context context;
	ActionBar aB;
	RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	MegaBrowserLollipopAdapter adapterList;
	MegaBrowserNewGridAdapter adapterGrid;
	public RubbishBinFragmentLollipop rubbishBinFragment = this;
	
	LinearLayout outSpaceLayout=null;
	TextView outSpaceText;
	Button outSpaceButton;
	int usedSpacePerc;
	
	boolean isList = true;
	long parentHandle = -1;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	ArrayList<MegaNode> nodes;
	MegaNode selectedNode = null;
	
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	
	MegaApiAndroid megaApi;
	
	private ActionMode actionMode;
	
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

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = adapterList.getSelectedNodes();
			
			switch(item.getItemId()){

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
				adapterGrid.selectAll();
			}
		}
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}
		
		if (parentHandle == -1){
			if (megaApi.getRubbishNode() != null){
				parentHandle = megaApi.getRubbishNode().getHandle();
				((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
				nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
				
				aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
				((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			
			if (parentNode == null){
				parentNode = megaApi.getRubbishNode();
				if (parentNode != null){
					parentHandle = parentNode.getHandle();
					((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
				}
			}
			
			if (parentNode != null){
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
			
				if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()){
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
				}
				else{
					aB.setTitle(parentNode.getName());
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
				}
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}			
		}

		if (isList){
			View v = inflater.inflate(R.layout.fragment_rubbishbinlist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			listView = (RecyclerView) v.findViewById(R.id.rubbishbin_list_view);
			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator()); 
			
			emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.rubbishbin_list_empty_text);
			emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
			emptyTextView.setText(R.string.file_browser_empty_folder);
			contentText = (TextView) v.findViewById(R.id.rubbishbin_list_content_text);
			if (adapterList == null){
				adapterList = new MegaBrowserLollipopAdapter(context, this, nodes, parentHandle, listView, aB, ManagerActivityLollipop.RUBBISH_BIN_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapterList.setParentHandle(parentHandle);
				adapterList.setNodes(nodes);
			}
			
			outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space_rubbish);
			outSpaceText =  (TextView) v.findViewById(R.id.out_space_text_rubbish);
			outSpaceButton = (Button) v.findViewById(R.id.out_space_btn_rubbish);
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
			
			if (parentHandle == megaApi.getRubbishNode().getHandle()){
//				aB.setTitle(getString(R.string.section_rubbish_bin));
				MegaNode infoNode = megaApi.getRubbishNode();
				contentText.setText(getInfoFolder(infoNode));
			}
			else{
				aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
				contentText.setText(getInfoFolder(infoNode));
			}
			
			adapterList.setPositionClicked(-1);
			adapterList.setMultipleSelect(false);

			listView.setAdapter(adapterList);
			
			setNodes(nodes);
			
			if (adapterList.getItemCount() == 0){
				
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout);
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
			View v = inflater.inflate(R.layout.fragment_rubbishbingrid, container, false);
//			
//			Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
//			DisplayMetrics outMetrics = new DisplayMetrics ();
//		    display.getMetrics(outMetrics);
//		    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
//			
//		    float scaleW = Util.getScaleW(outMetrics, density);
//		    float scaleH = Util.getScaleH(outMetrics, density);
//		    
//		    int totalWidth = outMetrics.widthPixels;
//		    int totalHeight = outMetrics.heightPixels;
//		    
//		    int numberOfCells = totalWidth / GRID_WIDTH;
//		    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
//		    	if (numberOfCells < 3){
//					numberOfCells = 3;
//				}	
//		    }
//		    else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
//		    	if (numberOfCells < 2){
//					numberOfCells = 2;
//				}	
//		    }
//				        
//	        listView = (RecyclerView) v.findViewById(R.id.rubbishbin_grid_view);
//	        listView.setOnItemClickListener(null);
//	        listView.setItemsCanFocus(false);
//
//	        emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_grid_empty_image);
//			emptyTextView = (TextView) v.findViewById(R.id.rubbishbin_grid_empty_text);
//			emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
//			emptyTextView.setText(R.string.file_browser_empty_folder);
//			contentText = (TextView) v.findViewById(R.id.rubbishbin_content_grid_text);
//			
//			outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space_grid_rubbish);
//			outSpaceText =  (TextView) v.findViewById(R.id.out_space_text_grid_rubbish);
//			outSpaceButton = (Button) v.findViewById(R.id.out_space_btn_grid_rubbish);
//			outSpaceButton.setVisibility(View.VISIBLE);
//			outSpaceButton.setOnClickListener(this);
//			
//			usedSpacePerc=((ManagerActivityLollipop)context).getUsedPerc();
//			
//			if(usedSpacePerc>95){
//				//Change below of ListView
//				log("usedSpacePerc>95");
////				RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
////				p.addRule(RelativeLayout.ABOVE, R.id.out_space);
////				listView.setLayoutParams(p);
//				outSpaceLayout.setVisibility(View.VISIBLE);
//				outSpaceLayout.bringToFront();
//				
//				Handler handler = new Handler();
//				handler.postDelayed(new Runnable() {					
//					
//					@Override
//					public void run() {
//						log("BUTTON DISAPPEAR");
//						log("altura: "+outSpaceLayout.getHeight());
//						
//						TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, outSpaceLayout.getHeight());
//						animTop.setDuration(2000);
//						animTop.setFillAfter(true);
//						outSpaceLayout.setAnimation(animTop);
//					
//						outSpaceLayout.setVisibility(View.GONE);
//						outSpaceLayout.invalidate();
////						RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
////						p.addRule(RelativeLayout.ABOVE, R.id.buttons_layout);
////						listView.setLayoutParams(p);
//					}
//				}, 15 * 1000);
//				
//			}	
//			else{
//				outSpaceLayout.setVisibility(View.GONE);
//			}			
//		
//			if (adapterGrid == null){
//				adapterGrid = new MegaBrowserNewGridAdapter(context, nodes, parentHandle, listView, aB, numberOfCells, ManagerActivityLollipop.RUBBISH_BIN_ADAPTER, orderGetChildren, emptyImageView, emptyTextView, null, null, contentText);
//			}
//			else{
//				adapterGrid.setParentHandle(parentHandle);
//				adapterGrid.setNodes(nodes);
//			}
//			
//			if (parentHandle == megaApi.getRubbishNode().getHandle()){
//				aB.setTitle(getString(R.string.section_rubbish_bin));
//				MegaNode infoNode = megaApi.getRubbishNode();
//				contentText.setText(getInfoFolder(infoNode));
//			}
//			else{
//				aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
//				MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
//				contentText.setText(getInfoFolder(infoNode));
//			}
//			
//			adapterGrid.setPositionClicked(-1);
//			listView.setAdapter(adapterGrid);
//			
//			setNodes(nodes);
			
			//TODO comprobar la vista vacía
			
			return v;	
		}
	}
	
	public void showOptionsPanel(MegaNode sNode){
		log("showOptionsPanel");
		
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
		 	case R.id.out_space_btn_grid_rubbish:
			case R.id.out_space_btn_rubbish:
			{
				((ManagerActivityLollipop)getActivity()).upgradeAccountButton();
				break;
			}
			case R.id.rubbishbin_list_option_move_layout:{
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());
				((ManagerActivityLollipop) context).showMoveLollipop(handleList);	
				break;
			}
			case R.id.rubbishbin_list_option_properties_layout: {
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
			case R.id.rubbishbin_list_option_remove_layout: {
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).moveToTrash(handleList);
				break;
			}
		}
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
					MegaNode n = nodes.get(position);
					
					aB.setTitle(n.getName());
					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
					((ManagerActivityLollipop)context).setFirstNavigationLevel(false);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					
					parentHandle = nodes.get(position).getHandle();
					MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
					contentText.setText(getInfoFolder(infoNode));
					((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
					adapterList.setParentHandle(parentHandle);
					nodes = megaApi.getChildren(nodes.get(position), orderGetChildren);
					adapterList.setNodes(nodes);
					listView.scrollToPosition(0);
					
					//If folder has no files
					if (adapterList.getItemCount() == 0){
						listView.setVisibility(View.GONE);
						emptyImageView.setVisibility(View.VISIBLE);
						emptyTextView.setVisibility(View.VISIBLE);
						if (megaApi.getRubbishNode().getHandle()==n.getHandle()) {
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
					if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
						Intent intent = new Intent(context, FullScreenImageViewer.class);
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
			parentHandle = adapterList.getParentHandle();
			((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
			
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
			else{
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()){
						aB.setTitle(getString(R.string.section_rubbish_bin));	
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
					((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					adapterList.setNodes(nodes);
					listView.post(new Runnable() 
				    {
				        @Override
				        public void run() 
				        {
				        	listView.scrollToPosition(0);
				            View v = listView.getChildAt(0);
				            if (v != null) 
				            {
				                v.requestFocus();
				            }
				        }
				    });
					adapterList.setParentHandle(parentHandle);
					contentText.setText(getInfoFolder(parentNode));
					return 2;
				}
				else{
					return 0;
				}
			}
		}
		else{
			parentHandle = adapterGrid.getParentHandle();
			((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
			
			if (adapterGrid == null){
				return 0;
			}
			
			if (adapterGrid.isMultipleSelect()){
				adapterGrid.hideMultipleSelect();
				return 2;
			}
			
			if (adapterGrid.getPositionClicked() != -1){
				adapterGrid.setPositionClicked(-1);
				adapterGrid.notifyDataSetChanged();
				return 1;
			}
			else{
				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
				if (parentNode != null){
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()){
						aB.setTitle(getString(R.string.section_rubbish_bin));	
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
					((ManagerActivityLollipop)context).setParentHandleRubbish(parentHandle);
					nodes = megaApi.getChildren(parentNode, orderGetChildren);
					adapterGrid.setNodes(nodes);
					listView.post(new Runnable() 
				    {
				        @Override
				        public void run() 
				        {
				        	listView.scrollToPosition(0);
				            View v = listView.getChildAt(0);
				            if (v != null) 
				            {
				                v.requestFocus();
				            }
				        }
				    });
					adapterGrid.setParentHandle(parentHandle);
					contentText.setText(getInfoFolder(parentNode));
					contentText.setText(getInfoFolder(parentNode));
					return 2;
				}
				else{
					return 0;
				}
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
		
		if (parentHandle == megaApi.getRubbishNode().getHandle()){
			MegaNode infoNode = megaApi.getRubbishNode();
			if (infoNode !=  null){
				contentText.setText(getInfoFolder(infoNode));
				aB.setTitle(getString(R.string.section_rubbish_bin));
			}
		}
		else{
			MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
			if (infoNode !=  null){
				contentText.setText(getInfoFolder(infoNode));
				aB.setTitle(infoNode.getName());
			}
		}
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	public long getParentHandle(){
		if (isList){
			return adapterList.getParentHandle();
		}
		else{
			return adapterGrid.getParentHandle();
		}
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (isList){
			if (adapterGrid != null){
				adapterGrid.setParentHandle(parentHandle);
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
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if (isList){
			if (adapterList != null){
				adapterList.setNodes(nodes);
				if (adapterList.getItemCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApi.getRubbishNode().getHandle()==parentHandle) {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						emptyTextView.setText(R.string.file_browser_empty_rubbish_bin);
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
		else{
			if (adapterGrid != null){
				adapterGrid.setNodes(nodes);
				if (adapterGrid.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApi.getRubbishNode().getHandle()==parentHandle) {
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						emptyTextView.setText(R.string.file_browser_empty_rubbish_bin);
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
	}
	
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
		return adapterList.getItemCount();
	}
	
	private static void log(String log) {
		Util.log("RubbishBinFragmentLollipop", log);
	}
}
