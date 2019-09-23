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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.NewHeaderItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.SortUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class OutgoingSharesFragmentLollipop extends RotatableFragment{

	public static ImageView imageDrag;

	Context context;

	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;
	FastScroller fastScroller;

	ImageView emptyImageView;
	LinearLayout emptyLinearLayout;
	TextView emptyTextViewFirst;

	boolean allFiles = true;

	MegaNodeAdapter adapter;
	RelativeLayout transfersOverViewLayout;

	Stack<Integer> lastPositionStack;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	MegaApiAndroid megaApi;
		
	ArrayList<MegaNode> nodes;
	
	public NewHeaderItemDecoration headerItemDecoration;

	public ActionMode actionMode;

	DatabaseHandler dbH;
	MegaPreferences prefs;
	String downloadLocationDefaultPath;
	
	private int placeholderCount;

	@Override
	protected MegaNodeAdapter getAdapter() {
		return adapter;
	}

	@Override
	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	public void updateScrollPosition(int position) {
		logDebug("Position: " + position);
		if (adapter != null) {
			if (adapter.getAdapterType() == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null){
				mLayoutManager.scrollToPosition(position);
			}
			else if (gridLayoutManager != null) {
				gridLayoutManager.scrollToPosition(position);
			}
		}
	}
    
    public void addSectionTitle(List<MegaNode> nodes,int type) {
        Map<Integer, String> sections = new HashMap<>();
        int folderCount = 0;
        int fileCount = 0;
        for (MegaNode node : nodes) {
            if(node == null) {
                continue;
            }
            if (node.isFolder()) {
                folderCount++;
            }
            if (node.isFile()) {
                fileCount++;
            }
        }

        if (type == MegaNodeAdapter.ITEM_VIEW_TYPE_GRID) {
            int spanCount = 2;
            if (recyclerView instanceof NewGridRecyclerView) {
                spanCount = ((NewGridRecyclerView)recyclerView).getSpanCount();
            }
            if(folderCount > 0) {
                for (int i = 0;i < spanCount;i++) {
                    sections.put(i, getString(R.string.general_folders));
                }
            }
            
            if(fileCount > 0 ) {
                placeholderCount =  (folderCount % spanCount) == 0 ? 0 : spanCount - (folderCount % spanCount);
                if (placeholderCount == 0) {
                    for (int i = 0;i < spanCount;i++) {
                        sections.put(folderCount + i, getString(R.string.general_files));
                    }
                } else {
                    for (int i = 0;i < spanCount;i++) {
                        sections.put(folderCount + placeholderCount + i, getString(R.string.general_files));
                    }
                }
            }
        } else {
            placeholderCount = 0;
            sections.put(0, getString(R.string.general_folders));
            sections.put(folderCount, getString(R.string.general_files));
        }

		if (headerItemDecoration == null) {
			logDebug("Create new decoration");
			headerItemDecoration = new NewHeaderItemDecoration(context);
		} else {
			logDebug("Remove old decoration");
			recyclerView.removeItemDecoration(headerItemDecoration);
		}
		headerItemDecoration.setType(type);
		headerItemDecoration.setKeys(sections);
		recyclerView.addItemDecoration(headerItemDecoration);
    }

	public ImageView getImageDrag(int position) {
		logDebug("Position: " + position);
		if (adapter != null) {
			if (adapter.getAdapterType() == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
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
		
		boolean showRename = false;
		boolean showMove = false;
		boolean showCopy = false;
		boolean showLink = false;
		boolean showRemoveLink = false;
		boolean showTrash = false;
		boolean showSendToChat = false;
		boolean showShare = false;
		boolean showEditLink = false;


		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = adapter.getSelectedNodes();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList, false);
					break;
				}
				case R.id.cab_menu_rename:{

					if (documents.size()==1){
						((ManagerActivityLollipop) context).showRenameDialog(documents.get(0), documents.get(0).getName());
					}
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
					clearSelections();
					hideMultipleSelect();
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

					NodeController nC = new NodeController(context);
					nC.selectContactToShareFolders(handleList);
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_share_link:{

					if(documents.get(0)==null){
						logWarning("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showGetLinkActivity(documents.get(0).getHandle());
					break;
				}
				case R.id.cab_menu_share_link_remove:{

					logDebug("Remove public link option");
					if(documents.get(0)==null){
						logWarning("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(documents.get(0));

					break;
				}
				case R.id.cab_menu_edit_link:{

					logDebug("Edit link option");
					if(documents.get(0)==null){
						logWarning("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showGetLinkActivity(documents.get(0).getHandle());
					break;
				}
				case R.id.cab_menu_send_to_chat:{
					logDebug("Send files to chat");
					ArrayList<MegaNode> nodesSelected = adapter.getArrayListSelectedNodes();
					NodeController nC = new NodeController(context);
					nC.checkIfNodesAreMineAndSelectChatsToSendNodes(nodesSelected);
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
			((ManagerActivityLollipop)context).hideFabButton();
			((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
			((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
			checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
			((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
			((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ZERO_DELAY);
		    checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();
			MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);

			menu.findItem(R.id.cab_menu_send_to_chat).setIcon(mutateIconSecondary(context, R.drawable.ic_send_to_contact, R.color.white));

			// Link
			if ((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {

				if(selected.get(0).isExported()){
					//Node has public link
					showRemoveLink=true;
					showLink=false;
					showEditLink = true;

				}
				else{
					showRemoveLink=false;
					showLink=true;
					showEditLink = false;
				}

			}
			if (selected.size() != 0) {
				showTrash = true;
				showMove = true;
				showShare = true;
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
					if (isChatEnabled()) {
						showSendToChat = true;
					}
					else {
						showSendToChat = false;
					}
				}else{
					showSendToChat = false;
				}

				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
					showLink=false;
					showRemoveLink=false;
					showEditLink=false;
					if(selected.size()==1){
						showRename=true;
					}else{
						showRename=false;
					}
				}
				else if(selected.size()==1){
					showRename=true;
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
					showLink=false;
					showRemoveLink=false;
					showRename=false;
					showEditLink=false;
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(true);
			menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			menu.findItem(R.id.cab_menu_send_to_chat).setVisible(showSendToChat);
			menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);

			if(showCopy){
				if(selected.size()>1){
					menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				}else{
					menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

				}
			}
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share).setVisible(showShare);
			menu.findItem(R.id.cab_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.findItem(R.id.cab_menu_edit_link).setVisible(showEditLink);




			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			if(showLink){
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}else{
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			}

			menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);
			if(showRemoveLink){
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}else{
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			}

			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			return false;
		}
		
	}

	public static OutgoingSharesFragmentLollipop newInstance() {
		logDebug("newInstance");
		OutgoingSharesFragmentLollipop fragment = new OutgoingSharesFragmentLollipop();
		return fragment;
	}
			
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		downloadLocationDefaultPath = getDownloadLocation(context);

		lastPositionStack = new Stack<>();
		nodes = new ArrayList<MegaNode>();
		super.onCreate(savedInstanceState);
		logDebug("onCreate");
	}

	public void checkScroll () {
		if ((recyclerView.canScrollVertically(-1) && recyclerView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect())){
			((ManagerActivityLollipop) context).changeActionBarElevation(true);
		}
		else {
			((ManagerActivityLollipop) context).changeActionBarElevation(false);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		logDebug("onCreateView");
		
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

		((ManagerActivityLollipop)context).showFabButton();

		if (((ManagerActivityLollipop)context).isList){
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyLinearLayout = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);

			transfersOverViewLayout = (RelativeLayout) v.findViewById(R.id.transfers_overview_item_layout);
			transfersOverViewLayout.setVisibility(View.GONE);
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			if (adapter == null){
				logDebug("Creating the adapter: " + ((ManagerActivityLollipop)context).parentHandleOutgoing);
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop)context).parentHandleOutgoing, recyclerView, null, OUTGOING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setListFragment(recyclerView);
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}

			if (((ManagerActivityLollipop)context).parentHandleOutgoing == -1){
				logWarning("Parent Handle == -1");
				findNodes();
			}else{
				MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleOutgoing);
				logDebug("Parent Handle: " + ((ManagerActivityLollipop)context).parentHandleOutgoing);
				nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
			}

			((ManagerActivityLollipop)context).setToolbarTitle();
			addSectionTitle(nodes,adapter.getAdapterType());
			adapter.setNodes(nodes);
			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);
			fastScroller.setRecyclerView(recyclerView);

			if (adapter != null){
				addSectionTitle(nodes,adapter.getAdapterType());
				adapter.setNodes(nodes);
				visibilityFastScroller();

				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyLinearLayout.setVisibility(View.VISIBLE);

					if (megaApi.getRootNode().getHandle()==((ManagerActivityLollipop)context).parentHandleOutgoing||((ManagerActivityLollipop)context).parentHandleOutgoing==-1) {
						if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
							emptyImageView.setImageResource(R.drawable.outgoing_empty_landscape);
						}else{
							emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
						}
						String textToShow = String.format(context.getString(R.string.context_empty_outgoing));
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
					}else {
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
					emptyImageView.setVisibility(View.GONE);
					emptyLinearLayout.setVisibility(View.GONE);
				}			
			}

			return v;
		}
		else{
			logDebug("Grid View");
			
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
			
			recyclerView = (NewGridRecyclerView) v.findViewById(R.id.file_grid_view_browser);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			recyclerView.setPadding(0, 0, 0, scaleHeightPx(80, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.setHasFixedSize(true);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();

			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyLinearLayout = (LinearLayout) v.findViewById(R.id.file_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_grid_empty_text_first);
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			if (adapter == null){
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop)context).parentHandleOutgoing, recyclerView, null, OUTGOING_SHARES_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setParentHandle(((ManagerActivityLollipop)context).parentHandleOutgoing);
				adapter.setListFragment(recyclerView);
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}

			if (((ManagerActivityLollipop)context).parentHandleOutgoing == -1){
				logWarning("Parent Handle == -1");
				findNodes();
			}
			else{
				MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleOutgoing);
				logDebug("Parent Handle: " + ((ManagerActivityLollipop)context).parentHandleOutgoing);

				nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
				addSectionTitle(nodes,adapter.getAdapterType());

				adapter.setNodes(nodes);
			}

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);
			fastScroller.setRecyclerView(recyclerView);
			visibilityFastScroller();
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyLinearLayout.setVisibility(View.VISIBLE);

				if (megaApi.getRootNode().getHandle()==((ManagerActivityLollipop)context).parentHandleOutgoing||((ManagerActivityLollipop)context).parentHandleOutgoing==-1) {

					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.outgoing_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_outgoing));
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

				}else {
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
				emptyImageView.setVisibility(View.GONE);
				emptyLinearLayout.setVisibility(View.GONE);
			}

			return v;
		}		
	}

	public void refresh (){
		logDebug("Parent Handle: " + ((ManagerActivityLollipop)context).parentHandleOutgoing);

        if (((ManagerActivityLollipop)context).parentHandleOutgoing == -1){
            findNodes();

            ((ManagerActivityLollipop)context).setToolbarTitle();
            if(adapter != null){
				logDebug("adapter != null");
				addSectionTitle(nodes,adapter.getAdapterType());
                adapter.setNodes(nodes);
				visibilityFastScroller();

                if (adapter.getItemCount() == 0){
					logDebug("adapter.getItemCount() = 0");
                    recyclerView.setVisibility(View.GONE);
                    emptyImageView.setVisibility(View.VISIBLE);
                    emptyLinearLayout.setVisibility(View.VISIBLE);

					if (megaApi.getRootNode().getHandle()==((ManagerActivityLollipop)context).parentHandleOutgoing||((ManagerActivityLollipop)context).parentHandleOutgoing==-1) {

						if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
							emptyImageView.setImageResource(R.drawable.outgoing_empty_landscape);
						}else{
							emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
						}
						String textToShow = String.format(context.getString(R.string.context_empty_outgoing));
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

					}else {
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
					logDebug("adapter.getItemCount() != 0");
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyImageView.setVisibility(View.GONE);
                    emptyLinearLayout.setVisibility(View.GONE);
                }
			}
        }
        else{
            MegaNode n = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleOutgoing);

           	((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			((ManagerActivityLollipop)context).setToolbarTitle();

			nodes = megaApi.getChildren(n, ((ManagerActivityLollipop)context).orderCloud);
			addSectionTitle(nodes,adapter.getAdapterType());

            adapter.setNodes(nodes);
			visibilityFastScroller();

            //If folder has no files
            if (adapter.getItemCount() == 0){
                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyLinearLayout.setVisibility(View.VISIBLE);

                if (megaApi.getRootNode().getHandle()==n.getHandle()) {

					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.outgoing_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_outgoing));
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
                emptyImageView.setVisibility(View.GONE);
                emptyLinearLayout.setVisibility(View.GONE);
            }
        }
        ((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
	}

	public void refreshContent (){
		logDebug("Parent Handle: " + ((ManagerActivityLollipop)context).parentHandleOutgoing);

		if (((ManagerActivityLollipop)context).parentHandleOutgoing == -1){
			findNodes();
			if(adapter != null){
				logDebug("adapter != null");
				addSectionTitle(nodes,adapter.getAdapterType());
				adapter.setNodes(nodes);
				visibilityFastScroller();

				if (adapter.getItemCount() == 0){
					logDebug("adapter.getItemCount() = 0");
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyLinearLayout.setVisibility(View.VISIBLE);
				}
				else{
					logDebug("adapter.getItemCount() != 0");
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyLinearLayout.setVisibility(View.GONE);
				}
			}
		}
		else{
			MegaNode n = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleOutgoing);
			nodes = megaApi.getChildren(n, ((ManagerActivityLollipop)context).orderCloud);
			addSectionTitle(nodes,adapter.getAdapterType());

			adapter.setNodes(nodes);
			visibilityFastScroller();

			//If folder has no files
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyLinearLayout.setVisibility(View.VISIBLE);

				if (megaApi.getRootNode().getHandle()==n.getHandle()) {

					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.outgoing_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_outgoing));
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
			}else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyLinearLayout.setVisibility(View.GONE);
			}
		}
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
	}

	public void findNodes(){
		logDebug("findNodes");
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
		orderNodes();
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

	public void openFile(MegaNode node, int position, int[] screenPosition, ImageView imageView) {
		if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()) {
			Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
			//Put flag to notify FullScreenImageViewerLollipop.
			intent.putExtra("placeholder", placeholderCount);
			intent.putExtra("position", position);
			intent.putExtra("adapterType", OUTGOING_SHARES_ADAPTER);
			intent.putExtra("isFolderLink", false);

			if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT) {
				intent.putExtra("parentNodeHandle", -1L);
			} else {
				intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
			}

			if (((ManagerActivityLollipop) context).parentHandleOutgoing == -1) {
				intent.putExtra("orderGetChildren", ((ManagerActivityLollipop) context).orderOthers);
			} else {
				intent.putExtra("orderGetChildren", ((ManagerActivityLollipop) context).orderCloud);
			}

			intent.putExtra("screenPosition", screenPosition);
			startActivity(intent);
			((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
			imageDrag = imageView;
		} else if (MimeTypeList.typeForName(nodes.get(position).getName()).isVideoReproducible() || MimeTypeList.typeForName(nodes.get(position).getName()).isAudio()) {
			MegaNode file = nodes.get(position);

			String mimeType = MimeTypeList.typeForName(file.getName()).getType();

			Intent mediaIntent;
			boolean internalIntent;
			boolean opusFile = false;
			if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()) {
				mediaIntent = new Intent(Intent.ACTION_VIEW);
				internalIntent = false;
				String[] s = file.getName().split("\\.");
				if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) {
					opusFile = true;
				}
			} else {
				internalIntent = true;
				mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
			}
			mediaIntent.putExtra("position", position);
			if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_ROOT) {
				mediaIntent.putExtra("parentNodeHandle", -1L);
			} else {
				mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
			}

			if (((ManagerActivityLollipop) context).parentHandleOutgoing == -1) {
				mediaIntent.putExtra("orderGetChildren", ((ManagerActivityLollipop) context).orderOthers);
			} else {
				mediaIntent.putExtra("orderGetChildren", ((ManagerActivityLollipop) context).orderCloud);
			}
			mediaIntent.putExtra("placeholder", placeholderCount);
			mediaIntent.putExtra("screenPosition", screenPosition);

			mediaIntent.putExtra("HANDLE", file.getHandle());
			mediaIntent.putExtra("FILENAME", file.getName());
			mediaIntent.putExtra("adapterType", OUTGOING_SHARES_ADAPTER);
			imageDrag = imageView;
			boolean isOnMegaDownloads = false;
			String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
			File f = new File(downloadLocationDefaultPath, file.getName());
			if (f.exists() && (f.length() == file.getSize())) {
				isOnMegaDownloads = true;
			}
			if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))) {
				File mediaFile = new File(localPath);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
					mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
				} else {
					mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
				}
				mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			} else {
				if (megaApi.httpServerIsRunning() == 0) {
					megaApi.httpServerStart();
				}

				ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
				ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				activityManager.getMemoryInfo(mi);

				if (mi.totalMem > BUFFER_COMP) {
					logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
				} else {
					logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
				}

				String url = megaApi.httpServerGetLocalLink(file);
				mediaIntent.setDataAndType(Uri.parse(url), mimeType);
			}
			if (opusFile) {
				mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
			}
			if (internalIntent) {
				context.startActivity(mediaIntent);
			} else {
				if (isIntentAvailable(context, mediaIntent)) {
					context.startActivity(mediaIntent);
				} else {
					((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getResources().getString(R.string.intent_not_available), -1);
					adapter.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList, true);
				}
			}
			((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
		} else if (MimeTypeList.typeForName(nodes.get(position).getName()).isPdf()) {
			MegaNode file = nodes.get(position);

			String mimeType = MimeTypeList.typeForName(file.getName()).getType();

			Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);

			pdfIntent.putExtra("inside", true);
			pdfIntent.putExtra("adapterType", OUTGOING_SHARES_ADAPTER);
			boolean isOnMegaDownloads = false;
			String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
			File f = new File(downloadLocationDefaultPath, file.getName());
			if (f.exists() && (f.length() == file.getSize())) {
				isOnMegaDownloads = true;
			}
			if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))) {
				File mediaFile = new File(localPath);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
					pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
				} else {
					pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
				}
				pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			} else {
				if (megaApi.httpServerIsRunning() == 0) {
					megaApi.httpServerStart();
				}

				ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
				ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				activityManager.getMemoryInfo(mi);

				if (mi.totalMem > BUFFER_COMP) {
					logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
				} else {
					logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
					megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
				}

				String url = megaApi.httpServerGetLocalLink(file);
				pdfIntent.setDataAndType(Uri.parse(url), mimeType);
			}
			pdfIntent.putExtra("HANDLE", file.getHandle());
			pdfIntent.putExtra("screenPosition", screenPosition);
			imageDrag = imageView;
			if (isIntentAvailable(context, pdfIntent)) {
				startActivity(pdfIntent);
			} else {
				Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(nodes.get(position).getHandle());
				NodeController nC = new NodeController(context);
				nC.prepareForDownload(handleList, true);
			}
			((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
		} else if (MimeTypeList.typeForName(nodes.get(position).getName()).isURL()) {
			logDebug("Is URL file");
			MegaNode file = nodes.get(position);

			boolean isOnMegaDownloads = false;
			String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
			File f = new File(downloadLocationDefaultPath, file.getName());
			if (f.exists() && (f.length() == file.getSize())) {
				isOnMegaDownloads = true;
			}
			logDebug("isOnMegaDownloads: " + isOnMegaDownloads);
			if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))) {
				File mediaFile = new File(localPath);
				InputStream instream = null;

				try {
					// open the file for reading
					instream = new FileInputStream(f.getAbsolutePath());

					// if file the available for reading
					if (instream != null) {
						// prepare the file for reading
						InputStreamReader inputreader = new InputStreamReader(instream);
						BufferedReader buffreader = new BufferedReader(inputreader);

						String line1 = buffreader.readLine();
						if (line1 != null) {
							String line2 = buffreader.readLine();

							String url = line2.replace("URL=", "");

							logDebug("Is URL - launch browser intent");
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setData(Uri.parse(url));
							startActivity(i);
						} else {
							logDebug("Not expected format: Exception on processing url file");
							Intent intent = new Intent(Intent.ACTION_VIEW);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
								intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", f), "text/plain");
							} else {
								intent.setDataAndType(Uri.fromFile(f), "text/plain");
							}
							intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

							if (isIntentAvailable(context, intent)) {
								startActivity(intent);
							} else {
								ArrayList<Long> handleList = new ArrayList<Long>();
								handleList.add(nodes.get(position).getHandle());
								NodeController nC = new NodeController(context);
								nC.prepareForDownload(handleList, true);
							}
						}
					}
				} catch (Exception ex) {

					Intent intent = new Intent(Intent.ACTION_VIEW);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", f), "text/plain");
					} else {
						intent.setDataAndType(Uri.fromFile(f), "text/plain");
					}
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					if (isIntentAvailable(context, intent)) {
						startActivity(intent);
					} else {
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList, true);
					}

				} finally {
					// close the file.
					try {
						instream.close();
					} catch (IOException e) {
						logError("EXCEPTION closing InputStream", e);
					}
				}
			} else {
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(nodes.get(position).getHandle());
				NodeController nC = new NodeController(context);
				nC.prepareForDownload(handleList, true);
			}
		} else {
			adapter.notifyDataSetChanged();
			ArrayList<Long> handleList = new ArrayList<Long>();
			handleList.add(nodes.get(position).getHandle());
			NodeController nC = new NodeController(context);
			nC.prepareForDownload(handleList, true);
		}
	}

	public void itemClick(int position, int[] screenPosition, ImageView imageView) {
		if (adapter.isMultipleSelect()) {
			logDebug("multiselect ON");

			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0) {
				updateActionModeTitle();
			}
		} else {
			if (nodes.get(position).isFolder()) {

				((ManagerActivityLollipop) context).increaseDeepBrowserTreeOutgoing();
				logDebug("deepBrowserTree after clicking folder" + ((ManagerActivityLollipop) context).deepBrowserTreeOutgoing);

				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;
				if (((ManagerActivityLollipop) context).isList) {
					lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
				} else {
					lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
					if(lastFirstVisiblePosition==-1){
						logWarning("Completely -1 then find just visible position");
						lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
					}
				}

				logDebug("Push to stack " + lastFirstVisiblePosition + " position");

				lastPositionStack.push(lastFirstVisiblePosition);

				((ManagerActivityLollipop) context).parentHandleOutgoing = n.getHandle();

				((ManagerActivityLollipop) context).supportInvalidateOptionsMenu();
				((ManagerActivityLollipop) context).setToolbarTitle();

				nodes = megaApi.getChildren(nodes.get(position), ((ManagerActivityLollipop) context).orderCloud);
				addSectionTitle(nodes, adapter.getAdapterType());

				adapter.setNodes(nodes);
				recyclerView.scrollToPosition(0);
				visibilityFastScroller();

				//If folder has no files
				if (adapter.getItemCount() == 0) {
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyLinearLayout.setVisibility(View.VISIBLE);

					if (megaApi.getRootNode().getHandle() == n.getHandle()) {

						if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
							emptyImageView.setImageResource(R.drawable.outgoing_empty_landscape);
						} else {
							emptyImageView.setImageResource(R.drawable.outgoing_shares_empty);
						}

						String textToShow = String.format(context.getString(R.string.context_empty_outgoing));
						try {
							textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
							textToShow = textToShow.replace("[/A]", "</font>");
							textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
							textToShow = textToShow.replace("[/B]", "</font>");
						} catch (Exception e) {
						}
						Spanned result = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
						} else {
							result = Html.fromHtml(textToShow);
						}
						emptyTextViewFirst.setText(result);

					} else {
						if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
							emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
						} else {
							emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
						}
						String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
						try {
							textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
							textToShow = textToShow.replace("[/A]", "</font>");
							textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
							textToShow = textToShow.replace("[/B]", "</font>");
						} catch (Exception e) {
						}
						Spanned result = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
						} else {
							result = Html.fromHtml(textToShow);
						}
						emptyTextViewFirst.setText(result);

					}
				} else {
					recyclerView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyLinearLayout.setVisibility(View.GONE);
				}
				checkScroll();
				((ManagerActivityLollipop) context).showFabButton();
			} else {
				//Is file
				openFile(nodes.get(position), position, screenPosition, imageView);
			}
		}
	}

	@Override
	public void multipleItemClick(int position) {
		adapter.toggleSelection(position);
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
		logDebug("setNodes");
		this.nodes = nodes;
		orderNodes();
	}

	private void orderNodes() {
		if (((ManagerActivityLollipop) context).orderOthers == MegaApiJava.ORDER_DEFAULT_DESC) {
			sortByNameDescending(this.nodes);
		} else {
			sortByNameAscending(this.nodes);
		}

		addSectionTitle(nodes, adapter.getAdapterType());
		adapter.setNodes(nodes);
	}
			
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
	}

	@Override
	protected void updateActionModeTitle() {
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
			logError("Invalidate error", e);
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
		logDebug("deepBrowserTree: " + ((ManagerActivityLollipop) context).deepBrowserTreeOutgoing);
					
		if (adapter == null){
			return 0;
		}

		((ManagerActivityLollipop) context).decreaseDeepBrowserTreeOutgoing();
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		if(((ManagerActivityLollipop) context).deepBrowserTreeOutgoing==0){
			logDebug("deepBrowserTree==0");
			//In the beginning of the navigation
			((ManagerActivityLollipop)context).parentHandleOutgoing = -1;

			((ManagerActivityLollipop)context).setToolbarTitle();
			findNodes();
			addSectionTitle(nodes,adapter.getAdapterType());
			adapter.setNodes(nodes);
			visibilityFastScroller();

			int lastVisiblePosition = 0;
			if(!lastPositionStack.empty()){
				lastVisiblePosition = lastPositionStack.pop();
				logDebug("Pop of the stack " + lastVisiblePosition + " position");
			}
			logDebug("Scroll to " + lastVisiblePosition + " position");

			if(lastVisiblePosition>=0){

				if(((ManagerActivityLollipop)context).isList){
					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
				else{
					gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
			}

			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyLinearLayout.setVisibility(View.GONE);
			((ManagerActivityLollipop) context).showFabButton();
			return 3;
		}
		else if (((ManagerActivityLollipop) context).deepBrowserTreeOutgoing>0){
			logDebug("Keep navigation");

			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(((ManagerActivityLollipop)context).parentHandleOutgoing));

			if (parentNode != null){
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyLinearLayout.setVisibility(View.GONE);

				((ManagerActivityLollipop)context).parentHandleOutgoing=parentNode.getHandle();

				((ManagerActivityLollipop)context).setToolbarTitle();
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

				nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
				addSectionTitle(nodes,adapter.getAdapterType());

				adapter.setNodes(nodes);
				visibilityFastScroller();

				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");

				if(lastVisiblePosition>=0){

					if(((ManagerActivityLollipop)context).isList){
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
					else{
						gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
					}
				}

			}
			((ManagerActivityLollipop) context).showFabButton();
			return 2;
		}
		else{
			logDebug("Back to Cloud");
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyLinearLayout.setVisibility(View.GONE);
			((ManagerActivityLollipop) context).deepBrowserTreeOutgoing=0;
			return 0;
		}
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
	}

	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}

	public int getItemCount(){
		if(adapter!=null){
			return adapter.getItemCount();
		}
		return 0;
	}

	public boolean isMultipleselect(){
		return adapter.isMultipleSelect();
	}

	public void visibilityFastScroller(){
		if(adapter == null){
			fastScroller.setVisibility(View.GONE);
		}else{
			if(adapter.getItemCount() < MIN_ITEMS_SCROLLBAR){
				fastScroller.setVisibility(View.GONE);
			}else{
				fastScroller.setVisibility(View.VISIBLE);
			}
		}

	}
}
