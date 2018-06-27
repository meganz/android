package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

public class SearchFragmentLollipop extends Fragment{

	private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

	public static ImageView imageDrag;

	Context context;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;
	FastScroller fastScroller;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

	MegaBrowserLollipopAdapter adapter;
	SearchFragmentLollipop searchFragment = this;
	TextView contentText;
	RelativeLayout contentTextLayout;
	ProgressBar progressBar;
	MegaApiAndroid megaApi;
	RelativeLayout transfersOverViewLayout;

	Stack<Integer> lastPositionStack;

    private MenuItem trashIcon;

	ArrayList<MegaNode> nodes;

	private ActionMode actionMode;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	boolean allFiles = true;
	DatabaseHandler dbH;
	MegaPreferences prefs;
	String downloadLocationDefaultPath = Util.downloadDIR;

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	public void updateScrollPosition(int position) {
		log("updateScrollPosition");
		if (adapter != null) {
			if (adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
				mLayoutManager.scrollToPosition(position);
			}
			else if (gridLayoutManager != null) {
				gridLayoutManager.scrollToPosition(position);
			}
		}
	}


	public ImageView getImageDrag(int position) {
		log("getImageDrag");
		if (adapter != null) {
			if (adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
				View v = mLayoutManager.findViewByPosition(position);
				if (v != null) {
					return (ImageView) v.findViewById(R.id.file_list_thumbnail);
				}
			}
			else if (gridLayoutManager != null){
				View v = gridLayoutManager.findViewByPosition(position);
				if (v != null) {
					return (ImageView) v.findViewById(R.id.file_grid_thumbnail);
				}
			}
		}
		return null;
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
			List<MegaNode> documents = adapter.getSelectedNodes();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList);
					break;
				}
				case R.id.cab_menu_rename:{

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

					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
					break;
				}	
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
					break;
				}
				case R.id.cab_menu_share_link:{

					if (documents.size()==1){
						NodeController nC = new NodeController(context);
						nC.exportLink(documents.get(0));
					}
					break;
				}
				case R.id.cab_menu_send_to_chat:{
					log("Send files to chat");
					ArrayList<MegaNode> nodesSelected = adapter.getArrayListSelectedNodes();
					NodeController nC = new NodeController(context);
					nC.selectChatsToSendNodes(nodesSelected);
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_trash:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
					break;
				}
				case R.id.cab_menu_select_all:{
					((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);
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
            trashIcon = menu.findItem(R.id.cab_menu_trash);

			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();
			MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);

			boolean showDownload = false;
			boolean showSendToChat = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			boolean itemsSelected = false;

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
				allFiles = true;

				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showTrash = false;
						showMove = false;
						break;
					}
				}
				//showSendToChat
				for(int i=0; i<selected.size();i++)	{
					if(!selected.get(i).isFile()){
						allFiles = false;
					}
				}

				if(allFiles){
					showSendToChat = true;
				}else{
					showSendToChat = false;
				}


				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(context.getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else if(selected.size()==1){

                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(context.getString(R.string.action_unselect_all));
					unselect.setVisible(true);

					if (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK) {
						showLink = true;
					}

					final long handle = selected.get(0).getHandle();
					MegaNode parent = megaApi.getNodeByHandle(handle);
					while (megaApi.getParentNode(parent) != null){
						parent = megaApi.getParentNode(parent);
					}

					if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
						trashIcon.setTitle(context.getString(R.string.context_move_to_trash));
					}else{
						trashIcon.setTitle(context.getString(R.string.context_remove));
					}
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(context.getString(R.string.action_unselect_all));
					unselect.setVisible(true);

					for(MegaNode i:selected){

						final long handle = i.getHandle();
						MegaNode parent = megaApi.getNodeByHandle(handle);
						while (megaApi.getParentNode(parent) != null){
							parent = megaApi.getParentNode(parent);
						}
						if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
							itemsSelected=true;
						}
					}

					if(!itemsSelected){
						trashIcon.setTitle(context.getString(R.string.context_remove));
					}else{
						trashIcon.setTitle(context.getString(R.string.context_move_to_trash));
					}
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);

			menu.findItem(R.id.cab_menu_send_to_chat).setVisible(showSendToChat);
			menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

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
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(recyclerView.getLayoutManager()!=null){
			outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, recyclerView.getLayoutManager().onSaveInstanceState());
		}
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		if (prefs != null){
			log("prefs != null");
			if (prefs.getStorageAskAlways() != null){
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
					log("askMe==false");
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}
		lastPositionStack = new Stack<>();
		super.onCreate(savedInstanceState);
		log("onCreate");		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		log("onCreateView");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (megaApi.getRootNode() == null){
			return null;
		}
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if(((ManagerActivityLollipop)context).parentHandleSearch==-1){
			if(((ManagerActivityLollipop)context).searchQuery!=null){
				if(!((ManagerActivityLollipop)context).searchQuery.isEmpty()){
					log("SEARCH NODES: " + ((ManagerActivityLollipop)context).searchQuery);
					nodes = megaApi.search(((ManagerActivityLollipop)context).searchQuery);
					log("Nodes found = " + nodes.size());

				}
			}
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleSearch);
			if(parentNode!=null){
				log("parentNode: "+parentNode.getName());
				nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
			}
		}

		if (((ManagerActivityLollipop)context).isList){
			
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			//recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setHasFixedSize(true);
			recyclerView.setItemAnimator(new DefaultItemAnimator());

			progressBar = (ProgressBar) v.findViewById(R.id.transfers_overview_progress_bar);
			progressBar.setVisibility(View.GONE);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_text_layout);
			contentText = (TextView) v.findViewById(R.id.content_text);

			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);

			transfersOverViewLayout = (RelativeLayout) v.findViewById(R.id.transfers_overview_item_layout);
			transfersOverViewLayout.setVisibility(View.GONE);

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, ((ManagerActivityLollipop)context).parentHandleSearch, recyclerView, null, Constants.SEARCH_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				adapter.setNodes(nodes);
			}

			adapter.setMultipleSelect(false);
	
			recyclerView.setAdapter(adapter);
			fastScroller.setRecyclerView(recyclerView);

			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recyclerView.getLayoutParams();
			params.addRule(RelativeLayout.BELOW, contentTextLayout.getId());
			recyclerView.setLayoutParams(params);
			
			setNodes(nodes);

			return v;
		}
		else{
			log("Grid View");
			
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.file_grid_view_browser);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			//recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
			recyclerView.setClipToPadding(false);

			recyclerView.setHasFixedSize(true);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			
			progressBar = (ProgressBar) v.findViewById(R.id.file_grid_download_progress_bar);

			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.file_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_grid_empty_text_first);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_grid_text_layout);

			contentText = (TextView) v.findViewById(R.id.content_grid_text);			

			if (adapter == null){
				adapter = new MegaBrowserLollipopAdapter(context, this, nodes, ((ManagerActivityLollipop)context).parentHandleSearch, recyclerView, null, Constants.SEARCH_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setAdapterType(MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_GRID);
				adapter.setNodes(nodes);
			}
			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);
			fastScroller.setRecyclerView(recyclerView);

			setNodes(nodes);

			return v;
		}
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

	@Override
	public void onAttach(Context context) {
		log("onAttach");
		super.onAttach(context);
		this.context = context;
	}
	
    public void itemClick(int position, int[] screenPosition, ImageView imageView) {
		log("itemClick: "+position);
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (adapter.isMultipleSelect()){
			log("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
				((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);

			}
		}
		else{
			((ManagerActivityLollipop)context).textSubmitted = true;
			log("nodes.size(): "+nodes.size());
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;
				if(((ManagerActivityLollipop)context).isList){
					lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
				}
				else{
					lastFirstVisiblePosition = ((CustomizedGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
					if(lastFirstVisiblePosition==-1){
						log("Completely -1 then find just visible position");
						lastFirstVisiblePosition = ((CustomizedGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
					}
				}

				log("Push to stack "+lastFirstVisiblePosition+" position");
				lastPositionStack.push(lastFirstVisiblePosition);
				
				contentText.setText(MegaApiUtils.getInfoFolder(n, context));

				((ManagerActivityLollipop)context).parentHandleSearch= n.getHandle();
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				((ManagerActivityLollipop)context).setToolbarTitle();

				nodes = megaApi.getChildren(n, ((ManagerActivityLollipop)context).orderCloud);
				adapter.setNodes(nodes);
				recyclerView.scrollToPosition(0);

				((ManagerActivityLollipop)context).levelsSearch++;

				visibilityFastScroller();

				//If folder has no files
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					contentText.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					if (megaApi.getRootNode().getHandle()==n.getHandle()) {
						if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
							emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
						}else{
							emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
						}

						String textToShow = String.format(context.getString(R.string.context_empty_inbox), context.getString(R.string.section_cloud_drive));
						try{
							textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
							textToShow = textToShow.replace("[/A]", "</font>");
							textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
							textToShow = textToShow.replace("[/B]", "</font>");
						}
						catch (Exception e){}
						Spanned result = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
						} else {
							result = Html.fromHtml(textToShow);
						}
						emptyTextViewFirst.setText(result);
					} else {
//						emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//						emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
						if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
							emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
						}else{
							emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
						}
						String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
						try{
							textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
							textToShow = textToShow.replace("[/A]", "</font>");
							textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
							textToShow = textToShow.replace("[/B]", "</font>");
						}
						catch (Exception e){}
						Spanned result = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
						} else {
							result = Html.fromHtml(textToShow);
						}
						emptyTextViewFirst.setText(result);
					}
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentText.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}

				((ManagerActivityLollipop) context).showFabButton();
			}
			else{
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					intent.putExtra("searchQuery", ((ManagerActivityLollipop)context).searchQuery);
					intent.putExtra("adapterType", Constants.SEARCH_ADAPTER);
					if (((ManagerActivityLollipop)context).parentHandleSearch == -1){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}
					MyAccountInfo accountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
					if(accountInfo!=null){
						intent.putExtra("typeAccount", accountInfo.getAccountType());
					}

					intent.putExtra("orderGetChildren", ((ManagerActivityLollipop)context).orderCloud);
					intent.putExtra("screenPosition", screenPosition);
					context.startActivity(intent);
					((ManagerActivityLollipop) context).overridePendingTransition(0,0);
					imageDrag = imageView;
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isVideoReproducible() || MimeTypeList.typeForName(nodes.get(position).getName()).isAudio() ){
					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					log("FILENAME: " + file.getName());

					Intent mediaIntent;
					if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()){
						mediaIntent = new Intent(Intent.ACTION_VIEW);
					}
					else {
						mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
					}
					mediaIntent.putExtra("position", position);
					mediaIntent.putExtra("searchQuery", ((ManagerActivityLollipop)context).searchQuery);
					mediaIntent.putExtra("adapterType", Constants.SEARCH_ADAPTER);
					if (((ManagerActivityLollipop)context).parentHandleSearch == -1){
						mediaIntent.putExtra("parentNodeHandle", -1L);
					}
					else{
						mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}
					mediaIntent.putExtra("orderGetChildren", ((ManagerActivityLollipop)context).orderCloud);
					mediaIntent.putExtra("screenPosition", screenPosition);
					MyAccountInfo accountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
					if(accountInfo!=null){
						mediaIntent.putExtra("typeAccount", accountInfo.getAccountType());
					}
					mediaIntent.putExtra("HANDLE", file.getHandle());
					mediaIntent.putExtra("FILENAME", file.getName());
					imageDrag = imageView;
					boolean isOnMegaDownloads = false;
					String localPath = Util.getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
					File f = new File(downloadLocationDefaultPath, file.getName());
					if(f.exists() && (f.length() == file.getSize())){
						isOnMegaDownloads = true;
					}
					if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))){
						File mediaFile = new File(localPath);
						//mediaIntent.setDataAndType(Uri.parse(localPath), mimeType);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && prefs.getStorageDownloadLocation().contains(Environment.getExternalStorageDirectory().getPath())
								&& localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
							mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						else{
							mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}
					else {
						if (megaApi.httpServerIsRunning() == 0) {
							megaApi.httpServerStart();
						}

						ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
						ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
						activityManager.getMemoryInfo(mi);

						if(mi.totalMem>Constants.BUFFER_COMP){
							log("Total mem: "+mi.totalMem+" allocate 32 MB");
							megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
						}
						else{
							log("Total mem: "+mi.totalMem+" allocate 16 MB");
							megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
						}

						String url = megaApi.httpServerGetLocalLink(file);
						mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					}
			  		if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
			  			context.startActivity(mediaIntent);
			  		}
			  		else{
			  			Toast.makeText(context, context.getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
			  			adapter.notifyDataSetChanged();
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList);
			  		}
			  		((ManagerActivityLollipop) context).overridePendingTransition(0,0);
				}else if (MimeTypeList.typeForName(nodes.get(position).getName()).isPdf()){
					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					log("FILENAME: " + file.getName() + "TYPE: "+mimeType);

					Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);
					MyAccountInfo accountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
					if(accountInfo!=null){
						pdfIntent.putExtra("typeAccount", accountInfo.getAccountType());
					}
					pdfIntent.putExtra("inside", true);
					pdfIntent.putExtra("adapterType", Constants.SEARCH_ADAPTER);
					boolean isOnMegaDownloads = false;
					String localPath = Util.getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
					File f = new File(downloadLocationDefaultPath, file.getName());
					if(f.exists() && (f.length() == file.getSize())){
						isOnMegaDownloads = true;
					}
					if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))){
						File mediaFile = new File(localPath);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && prefs.getStorageDownloadLocation().contains(Environment.getExternalStorageDirectory().getPath())
								&& localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
							pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						else{
							pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
						}
						pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}
					else {
						if (megaApi.httpServerIsRunning() == 0) {
							megaApi.httpServerStart();
						}

						ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
						ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
						activityManager.getMemoryInfo(mi);

						if(mi.totalMem>Constants.BUFFER_COMP){
							log("Total mem: "+mi.totalMem+" allocate 32 MB");
							megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
						}
						else{
							log("Total mem: "+mi.totalMem+" allocate 16 MB");
							megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
						}

						String url = megaApi.httpServerGetLocalLink(file);
						pdfIntent.setDataAndType(Uri.parse(url), mimeType);
					}
					pdfIntent.putExtra("HANDLE", file.getHandle());
					pdfIntent.putExtra("screenPosition", screenPosition);
					imageDrag = imageView;
					if (MegaApiUtils.isIntentAvailable(context, pdfIntent)){
						context.startActivity(pdfIntent);
					}
					else{
						Toast.makeText(context, context.getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList);
					}
					((ManagerActivityLollipop) context).overridePendingTransition(0,0);
				}
				else{
					adapter.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList);
				}
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
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null || context == null) {
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
		Resources res = context.getResources();

		String title;
		int sum=files+folders;

		if (files == 0 && folders == 0) {
			title = Integer.toString(sum);
		} else if (files == 0) {
			title = Integer.toString(folders);
		} else if (folders == 0) {
			title = Integer.toString(files);
		} else {
			title = Integer.toString(sum);
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
		((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_TRANSPARENT_BLACK);

		if (actionMode != null) {
			actionMode.finish();
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
	
	public int onBackPressed(){
		log("onBackPressed");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (((ManagerActivityLollipop)context).levelsSearch > 0){
			log("levels > 0");
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleSearch));
			if (parentNode != null){
				contentText.setText(MegaApiUtils.getInfoFolder(parentNode, context));
				recyclerView.setVisibility(View.VISIBLE);
				contentText.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				((ManagerActivityLollipop)context).parentHandleSearch=parentNode.getHandle();
				((ManagerActivityLollipop)context).setToolbarTitle();
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

				nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);

				visibilityFastScroller();

				if(nodes!=null){
					log("nodes.size: "+nodes.size());
					if(nodes.size()>0){
						recyclerView.setVisibility(View.VISIBLE);
						contentText.setVisibility(View.VISIBLE);
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);
					}
					else{
						recyclerView.setVisibility(View.GONE);
						contentText.setVisibility(View.GONE);
						emptyImageView.setVisibility(View.VISIBLE);
						emptyTextView.setVisibility(View.VISIBLE);
					}
				}
				adapter.setNodes(nodes);

				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					log("Pop of the stack "+lastVisiblePosition+" position");
				}
				log("Scroll to "+lastVisiblePosition+" position");

				if(lastVisiblePosition>=0){

					if(((ManagerActivityLollipop)context).isList){
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
					else{
						gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
				}

				((ManagerActivityLollipop)context).levelsSearch--;
				((ManagerActivityLollipop) context).showFabButton();
				return 2;
			}
			else{
				((ManagerActivityLollipop) context).showFabButton();
				return 0;
			}
		}
		else if (((ManagerActivityLollipop)context).levelsSearch == -1){
			log("levels == -1");
			((ManagerActivityLollipop) context).showFabButton();
			return 0;
		}
		else{
			log("levels searchQuery: "+((ManagerActivityLollipop)context).levelsSearch);
			log("searchQuery: "+((ManagerActivityLollipop)context).searchQuery);
			((ManagerActivityLollipop)context).setParentHandleSearch(-1);
			nodes = megaApi.search(((ManagerActivityLollipop)context).searchQuery);
			adapter.setNodes(nodes);
			setNodes(nodes);
			visibilityFastScroller();

			contentText.setText(MegaApiUtils.getInfoNode(nodes, (ManagerActivityLollipop)context));
			if(nodes!=null){
				log("nodes.size: "+nodes.size());
				if(nodes.size()>0){
					contentText.setVisibility(View.VISIBLE);
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
				else{
					contentText.setVisibility(View.GONE);
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
				}
			}
			int lastVisiblePosition = 0;
			if(!lastPositionStack.empty()){
				lastVisiblePosition = lastPositionStack.pop();
				log("Pop of the stack "+lastVisiblePosition+" position");
			}
			log("Scroll to "+lastVisiblePosition+" position");

			if(lastVisiblePosition>=0){

				if(((ManagerActivityLollipop)context).isList){
					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
				else{
					gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
			}
			((ManagerActivityLollipop)context).levelsSearch--;
			((ManagerActivityLollipop)context).setToolbarTitle();
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			((ManagerActivityLollipop) context).showFabButton();
			return 3;
		}
	}
	
	public long getParentHandle(){
		return ((ManagerActivityLollipop)context).parentHandleSearch;

	}

	public void refresh(){
		log("refresh ");
		if(((ManagerActivityLollipop)context).parentHandleSearch==-1){
			nodes = megaApi.search(((ManagerActivityLollipop)context).searchQuery);
		}else{
			MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleSearch);
			if(parentNode!=null){
				log("parentNode: "+parentNode.getName());
				nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
				contentText.setText(MegaApiUtils.getInfoFolder(parentNode, context));
			}else{
				nodes = megaApi.search(((ManagerActivityLollipop)context).searchQuery);
			}
		}
		setNodes(nodes);

		if(adapter != null){
			adapter.notifyDataSetChanged();
		}

		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		visibilityFastScroller();

	}

//	public void refresh(){
//		log("refresh ");
//		if(((ManagerActivityLollipop)context).parentHandleSearch==-1){
//			nodes = megaApi.search(((ManagerActivityLollipop)context).searchQuery);
//		}
//		else{
//			MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleSearch);
//			if(parentNode!=null){
//				log("parentNode: "+parentNode.getName());
//				nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
//				contentText.setText(MegaApiUtils.getInfoFolder(parentNode, context));
//			}
//		}
//		setNodes(nodes);
//
//		if(adapter != null){
//			adapter.notifyDataSetChanged();
//		}
//	}

	public RecyclerView getRecyclerView(){
		return recyclerView;
	}

	public ArrayList<MegaNode> getNodes(){
		return nodes;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		log("setNodes");

		this.nodes = nodes;
		if (adapter != null){
			adapter.setNodes(nodes);
			visibilityFastScroller();

			if (adapter.getItemCount() == 0){
				log("no results");
				recyclerView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if(((ManagerActivityLollipop)context).parentHandleSearch==-1){
//					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
					}
					emptyTextViewFirst.setText(R.string.no_results_found);
				}
				else if (megaApi.getRootNode().getHandle()==((ManagerActivityLollipop)context).parentHandleSearch) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_inbox), getString(R.string.section_cloud_drive));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				}
				else {
//					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//					emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
					}
					String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				}
			}
			else{
				contentTextLayout.setVisibility(View.VISIBLE);
				contentText.setText(MegaApiUtils.getInfoNode(nodes, context));
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}			
		}
	}

	public static SearchFragmentLollipop newInstance() {
		log("newInstance");
		SearchFragmentLollipop fragment = new SearchFragmentLollipop();
		return fragment;
	}

	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}

	private static void log(String log) {
		Util.log("SearchFragmentLollipop", log);
	}

	public void visibilityFastScroller(){
		if(adapter == null){
			fastScroller.setVisibility(View.GONE);
		}else{
			if(adapter.getItemCount() < Constants.MIN_ITEMS_SCROLLBAR){
				fastScroller.setVisibility(View.GONE);
			}else{
				fastScroller.setVisibility(View.VISIBLE);
			}
		}
	}

}
