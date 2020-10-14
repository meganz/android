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
import android.os.Handler;
import android.os.Looper;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.concurrent.CopyOnWriteArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.SearchNodesTask;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.NewHeaderItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.SearchNodesTask.setSearchProgressView;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

public class SearchFragmentLollipop extends RotatableFragment{

	public static final String ARRAY_SEARCH = "ARRAY_SEARCH";

	private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

	public static ImageView imageDrag;

	private Context context;
	private RecyclerView recyclerView;
	private LinearLayoutManager mLayoutManager;
	private CustomizedGridLayoutManager gridLayoutManager;
	private FastScroller fastScroller;

	private ImageView emptyImageView;
	private LinearLayout emptyTextView;
	private TextView emptyTextViewFirst;

	private MegaNodeAdapter adapter;
	private MegaApiAndroid megaApi;

	private Stack<Integer> lastPositionStack;

    private MenuItem trashIcon;

	private ArrayList<MegaNode> nodes = new ArrayList<>();

	private ActionMode actionMode;

	private DisplayMetrics outMetrics;
	private Display display;

	private boolean allFiles = true;
	private String downloadLocationDefaultPath;

    private int placeholderCount;

	private NewHeaderItemDecoration headerItemDecoration;

	private SearchNodesTask searchNodesTask;
	private RelativeLayout contentLayout;
	private ProgressBar searchProgressBar;

	@Override
	protected RotatableAdapter getAdapter() {
		return adapter;
	}

	public void activateActionMode(){
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	@Override
	public void multipleItemClick(int position) {
		adapter.toggleSelection(position);
	}

	@Override
	public void reselectUnHandledSingleItem(int position) {
		adapter.filClicked(position);
	}

	public void updateScrollPosition(int position) {
		logDebug("Position: " + position);
		if (adapter != null) {
			if (adapter.getAdapterType() == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
				mLayoutManager.scrollToPosition(position);
			}
			else if (gridLayoutManager != null) {
				gridLayoutManager.scrollToPosition(position);
			}
		}
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
//						NodeController nC = new NodeController(context);
//						nC.exportLink(documents.get(0));
						((ManagerActivityLollipop) context).showGetLinkActivity(documents.get(0).getHandle());
						clearSelections();
						hideMultipleSelect();
					}
					break;
				}
				case R.id.cab_menu_share_link_remove:{

					logDebug("Remove public link option");
					if(documents.get(0)==null){
						logWarning("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(documents.get(0));
					clearSelections();
					hideMultipleSelect();

					break;
				}
				case R.id.cab_menu_edit_link:{

					logDebug("Edit link option");
					if(documents.get(0)==null){
						logWarning("The selected node is NULL");
						break;
					}
					((ManagerActivityLollipop) context).showGetLinkActivity(documents.get(0).getHandle());
					clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_send_to_chat:{
					logDebug("Send files to chat");
					if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
						showOverDiskQuotaPaywallWarning();
						break;
					}
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
            trashIcon = menu.findItem(R.id.cab_menu_trash);
			((ManagerActivityLollipop) context).setTextSubmitted();
			checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
			checkScroll();
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
			boolean showEditLink = false;
			boolean showRemoveLink = false;
			boolean showTrash = false;
			boolean itemsSelected = false;

			// Rename
			if((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)) {
				showRename = true;
			}
			
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

				showSendToChat = allFiles;

				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(context.getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else if(selected.size()==1){

                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(context.getString(R.string.action_unselect_all));
					unselect.setVisible(true);

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
			if(showLink){
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}else{
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			}

			menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);
			if(showRemoveLink){
				menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}else{
				menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			}
			menu.findItem(R.id.cab_menu_edit_link).setVisible(showEditLink);
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
	public void onDestroy() {
		if (adapter != null) {
			adapter.clearTakenDownDialog();
		}

		super.onDestroy();
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		downloadLocationDefaultPath = getDownloadLocation();

		lastPositionStack = new Stack<>();
		super.onCreate(savedInstanceState);
	}

	public void checkScroll () {
		if (recyclerView != null) {
			if (recyclerView.canScrollVertically(-1) || (adapter != null && adapter.isMultipleSelect())) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			}
			else {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
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

        String folderStr = context.getResources().getQuantityString(R.plurals.general_num_folders,folderCount);
        String fileStr = context.getResources().getQuantityString(R.plurals.general_num_files,fileCount);
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
            sections.put(0, context.getString(R.string.general_folders));
            sections.put(folderCount, context.getString(R.string.general_files));
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
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

		View v;
		if (((ManagerActivityLollipop)context).isList){
			
			v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

			recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

            //Add bottom padding for recyclerView like in other fragments.
            recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
            recyclerView.setClipToPadding(false);
			recyclerView.setClipToPadding(false);
//			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			recyclerView.setHasFixedSize(true);
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);

			if (adapter == null){
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop)context).getParentHandleSearch(), recyclerView, null, SEARCH_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setListFragment(recyclerView);
                adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}
		} else {
			logDebug("Grid View");
			
			v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.file_grid_view_browser);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			//recyclerView.setPadding(0, 0, 0, scaleHeightPx(80, outMetrics));
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
			emptyTextView = (LinearLayout) v.findViewById(R.id.file_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_grid_empty_text_first);

			if (adapter == null){
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop)context).getParentHandleSearch(), recyclerView, null, SEARCH_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setListFragment(recyclerView);
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}
		}

		adapter.setMultipleSelect(false);

		recyclerView.setAdapter(adapter);
		fastScroller.setRecyclerView(recyclerView);

		contentLayout = v.findViewById(R.id.content_layout);
		searchProgressBar = v.findViewById(R.id.progressbar);

		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		String query = ((ManagerActivityLollipop) context).getSearchQuery();
		if (isAdded() && query != null) {
			newSearchNodesTask();
			((ManagerActivityLollipop) context).showFabButton();
		}
	}

	public void newSearchNodesTask() {
		setProgressView(true);
		cancelPreviousAsyncTask();
		searchNodesTask = new SearchNodesTask(context,
				this,
				((ManagerActivityLollipop) context).getSearchQuery(),
				((ManagerActivityLollipop) context).getParentHandleSearch(),
				nodes);
		searchNodesTask.execute();
	}

	public void cancelPreviousAsyncTask() {
		if (searchNodesTask != null) {
			searchNodesTask.cancelSearch();
			searchNodesTask.cancel(true);
		}
	}

	public void setProgressView(boolean inProgress) {
		setSearchProgressView(contentLayout, searchProgressBar, recyclerView, inProgress);
	}

	@Override
	public void onAttach(Context context) {
		logDebug("onAttach");
		super.onAttach(context);
		this.context = context;
	}

	private void manageNodes(Intent intent) {
		ArrayList<String> serialized = new ArrayList<>();
		for (MegaNode node : nodes) {
			if (node != null) {
				serialized.add(String.valueOf(node.getHandle()));
			}
		}
		intent.putExtra(ARRAY_SEARCH, serialized);
	}
	
    public void itemClick(int position, int[] screenPosition, ImageView imageView) {
		logDebug("Position: " + position);

		if (adapter.isMultipleSelect()){
			logDebug("Multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();
			}
		}
		else{
			logDebug("nodes.size(): "+nodes.size());
			((ManagerActivityLollipop) context).setTextSubmitted();

			// If search text is empty and try to open a folder in search fragment.
			if (!((ManagerActivityLollipop) context).isValidSearchQuery() && nodes.get(position).isFolder()) {
				((ManagerActivityLollipop) context).closeSearchView();
				((ManagerActivityLollipop) context).openSearchFolder(nodes.get(position));
				return;
			}

			if (nodes.get(position).isFolder()){
				logDebug("is a folder");
				((ManagerActivityLollipop)context).setParentHandleSearch(nodes.get(position).getHandle());
				((ManagerActivityLollipop)context).levelsSearch ++;

				int lastFirstVisiblePosition;
				if(((ManagerActivityLollipop)context).isList){
					lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
				}
				else{
					lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstCompletelyVisibleItemPosition();
					if(lastFirstVisiblePosition==-1){
						logWarning("Completely -1 then find just visible position");
						lastFirstVisiblePosition = ((NewGridRecyclerView) recyclerView).findFirstVisibleItemPosition();
					}
				}

				logDebug("Push to stack "+lastFirstVisiblePosition+" position");
				lastPositionStack.push(lastFirstVisiblePosition);
				clickAction();
			}
			else{
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
                    //Put flag to notify FullScreenImageViewerLollipop.
                    intent.putExtra("placeholder", placeholderCount);
					intent.putExtra("position", position);
					intent.putExtra("searchQuery", ((ManagerActivityLollipop)context).getSearchQuery());
					intent.putExtra("adapterType", SEARCH_ADAPTER);
					if (((ManagerActivityLollipop)context).getParentHandleSearch() == -1){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}

					intent.putExtra("orderGetChildren", ((ManagerActivityLollipop)context).orderCloud);
					intent.putExtra("screenPosition", screenPosition);
					manageNodes(intent);
					startActivity(intent);
					getActivity().overridePendingTransition(0,0);
					imageDrag = imageView;
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isVideoReproducible() || MimeTypeList.typeForName(nodes.get(position).getName()).isAudio() ){
					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					logDebug("FILE HANDLE: " + file.getHandle());

					Intent mediaIntent;
					boolean internalIntent;
					boolean opusFile = false;
					if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()){
						mediaIntent = new Intent(Intent.ACTION_VIEW);
						internalIntent = false;
						String[] s = file.getName().split("\\.");
						if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
							opusFile = true;
						}
					}
					else {
						internalIntent = true;
						mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
					}
                    mediaIntent.putExtra("placeholder", placeholderCount);
					mediaIntent.putExtra("position", position);
					mediaIntent.putExtra("searchQuery", ((ManagerActivityLollipop)context).getSearchQuery());
					mediaIntent.putExtra("adapterType", SEARCH_ADAPTER);
					if (((ManagerActivityLollipop)context).getParentHandleSearch() == -1){
						mediaIntent.putExtra("parentNodeHandle", -1L);
					}
					else{
						mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}
					mediaIntent.putExtra("orderGetChildren", ((ManagerActivityLollipop)context).orderCloud);
					mediaIntent.putExtra("screenPosition", screenPosition);
					manageNodes(mediaIntent);

					mediaIntent.putExtra("HANDLE", file.getHandle());
					mediaIntent.putExtra("FILENAME", file.getName());
					imageDrag = imageView;
					String localPath = getLocalFile(context, file.getName(), file.getSize());

					if (localPath != null){
						File mediaFile = new File(localPath);
						//mediaIntent.setDataAndType(Uri.parse(localPath), mimeType);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
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

						if(mi.totalMem>BUFFER_COMP){
							logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
							megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
						}
						else{
							logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
							megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
						}

						String url = megaApi.httpServerGetLocalLink(file);
						mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					}
					if (opusFile){
						mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
					}
					if (internalIntent) {
						context.startActivity(mediaIntent);
					}
					else {
						if (isIntentAvailable(context, mediaIntent)) {
							context.startActivity(mediaIntent);
						}
						else {
							((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getResources().getString(R.string.intent_not_available), -1);
							adapter.notifyDataSetChanged();
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(nodes.get(position).getHandle());
							NodeController nC = new NodeController(context);
							nC.prepareForDownload(handleList, true);
						}
					}
			  		((ManagerActivityLollipop) context).overridePendingTransition(0,0);
				}else if (MimeTypeList.typeForName(nodes.get(position).getName()).isPdf()){
					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					logDebug("FILEHANDLE: " + file.getHandle() + ", TYPE: " + mimeType);

					Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);

					pdfIntent.putExtra("inside", true);
					pdfIntent.putExtra("adapterType", SEARCH_ADAPTER);

					String localPath = getLocalFile(context, file.getName(), file.getSize());

					if (localPath != null) {
						File mediaFile = new File(localPath);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
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

						if(mi.totalMem>BUFFER_COMP){
							logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
							megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
						}
						else{
							logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
							megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
						}

						String url = megaApi.httpServerGetLocalLink(file);
						pdfIntent.setDataAndType(Uri.parse(url), mimeType);
					}
					pdfIntent.putExtra("HANDLE", file.getHandle());
					pdfIntent.putExtra("screenPosition", screenPosition);
					imageDrag = imageView;
					if (isIntentAvailable(context, pdfIntent)){
						context.startActivity(pdfIntent);
					}
					else{
						Toast.makeText(context, context.getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(position).getHandle());
						NodeController nC = new NodeController(context);
						nC.prepareForDownload(handleList, true);
					}
					((ManagerActivityLollipop) context).overridePendingTransition(0,0);
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isURL()) {
					logDebug("Is URL file");
					MegaNode file = nodes.get(position);

					String localPath = getLocalFile(context, file.getName(), file.getSize());

					if (localPath != null) {
						File mediaFile = new File(localPath);
						InputStream instream = null;

						try {
							// open the file for reading
							instream = new FileInputStream(mediaFile.getAbsolutePath());

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
									logWarning("Not expected format: Exception on processing url file");
									Intent intent = new Intent(Intent.ACTION_VIEW);
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
										intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), "text/plain");
									} else {
										intent.setDataAndType(Uri.fromFile(mediaFile), "text/plain");
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
								intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), "text/plain");
							} else {
								intent.setDataAndType(Uri.fromFile(mediaFile), "text/plain");
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
				} else{
					adapter.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(nodes.get(position).getHandle());
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList, true);
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

	@Override
	protected void updateActionModeTitle() {
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
	
	public void selectAll(){
		logDebug("selectAll");
		if (adapter != null){
			if(adapter.isMultipleSelect()){
				adapter.selectAll();
			}
			else{			
				
				adapter.setMultipleSelect(true);
				adapter.selectAll();
				
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
			}

			new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
		}
	}
	
	public boolean showSelectMenuItem(){

		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}
	
	public int onBackPressed(){
		logDebug("onBackPressed");
		cancelPreviousAsyncTask();
		int levelSearch = ((ManagerActivityLollipop)context).levelsSearch;

		if (levelSearch >= 0) {
			if (levelSearch > 0) {
				MegaNode node = megaApi.getNodeByHandle(((ManagerActivityLollipop) context).getParentHandleSearch());

				if (node == null) {
					((ManagerActivityLollipop) context).setParentHandleSearch(-1);
				} else {
					MegaNode parentNode = megaApi.getParentNode(node);
					if (parentNode == null) {
						((ManagerActivityLollipop) context).setParentHandleSearch(-1);
					} else {
						((ManagerActivityLollipop) context).setParentHandleSearch(parentNode.getHandle());
					}
				}
			} else {
				((ManagerActivityLollipop) context).setParentHandleSearch(-1);
			}

			((ManagerActivityLollipop)context).levelsSearch--;
			clickAction();

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
			return 2;
		}

		logDebug("levels == -1");
		((ManagerActivityLollipop) context).showFabButton();
		return 0;
	}

	private void clickAction() {
		newSearchNodesTask();
		((ManagerActivityLollipop)context).setToolbarTitle();
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		((ManagerActivityLollipop) context).showFabButton();
	}
	
	public long getParentHandle(){
		return ((ManagerActivityLollipop)context).getParentHandleSearch();

	}

	public void refresh(){
		logDebug("refresh ");
		newSearchNodesTask();

		if(adapter != null){
			adapter.notifyDataSetChanged();
		}

		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
		visibilityFastScroller();

	}

	public RecyclerView getRecyclerView(){
		return recyclerView;
	}

	public ArrayList<MegaNode> getNodes(){
	    //remove the null placeholder.
		if (nodes != null) {
			CopyOnWriteArrayList<MegaNode> safeList = new CopyOnWriteArrayList<>(nodes);
			for (MegaNode node : safeList) {
				if (node == null) {
					safeList.remove(node);
				}
			}
			return new ArrayList<>(safeList);
		}
	    return null;
	}

	public void setNodes(ArrayList<MegaNode> nodes) {
		this.nodes = nodes;

		if (nodes == null || adapter == null) {
			return;
		}

		addSectionTitle(nodes, adapter.getAdapterType());
		adapter.setNodes(nodes);
		visibilityFastScroller();

		if (adapter.getItemCount() == 0) {
			logDebug("No results");
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			if (((ManagerActivityLollipop) context).getParentHandleSearch() == -1) {
				if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
				} else {
					emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
				}
				emptyTextViewFirst.setText(R.string.no_results_found);
			} else if (megaApi.getRootNode().getHandle() == ((ManagerActivityLollipop) context).getParentHandleSearch()) {
				if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					emptyImageView.setImageResource(R.drawable.cloud_empty_landscape);
				} else {
					emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
				}
				String textToShow = String.format(context.getString(R.string.context_empty_cloud_drive));
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
			emptyTextView.setVisibility(View.GONE);
		}

		if (isWaitingForSearchedNodes()) {
			reDoTheSelectionAfterRotation();
			reSelectUnhandledItem();
		}
	}

	public static SearchFragmentLollipop newInstance() {
		logDebug("newInstance");
		SearchFragmentLollipop fragment = new SearchFragmentLollipop();
		return fragment;
	}

	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
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

	public void setHeaderItemDecoration(NewHeaderItemDecoration headerItemDecoration) {
		this.headerItemDecoration = headerItemDecoration;
	}
}
