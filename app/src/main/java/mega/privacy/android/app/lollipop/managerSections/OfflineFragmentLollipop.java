package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.NewHeaderItemDecoration;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaOfflineLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static nz.mega.sdk.MegaApiJava.*;
import static mega.privacy.android.app.utils.Util.*;

public class OfflineFragmentLollipop extends RotatableFragment{

	public static ImageView imageDrag;
	public static final String REFRESH_OFFLINE_FILE_LIST = "refresh_offline_file_list";

	private Context context;
	private ActionBar aB;
	private RecyclerView recyclerView;
	private LinearLayoutManager mLayoutManager;
	private CustomizedGridLayoutManager gridLayoutManager;

	private Stack<Integer> lastPositionStack;
	public NewHeaderItemDecoration headerItemDecoration;
	private ImageView emptyImageView;
	private LinearLayout emptyTextView;
	private TextView emptyTextViewFirst;

	MegaOfflineLollipopAdapter adapter;
	DatabaseHandler dbH = null;
	ArrayList<MegaOffline> mOffList = null;
	String pathNavigation = null;
	TextView contentText;
	int orderGetChildren;
	private static final String DB_FILE = "0";
	private static final String DB_FOLDER = "1";
	private MegaApiAndroid megaApi;
	private RelativeLayout contentTextLayout;

	private ActionMode actionMode;

	private int placeholderCount;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refresh();
		}
	};

	@Override
	public void onResume(){
		super.onResume();
		IntentFilter filter = new IntentFilter(REFRESH_OFFLINE_FILE_LIST);
		LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, filter);
	}

	@Override
	public void onPause(){
		super.onPause();
		LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
	}

	@Override
	protected RotatableAdapter getAdapter() {
		return adapter;
	}

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
			if (getAdapterType() == MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
				mLayoutManager.scrollToPosition(position);
			}
			else if (gridLayoutManager != null) {
				gridLayoutManager.scrollToPosition(position);
			}
		}
	}
	
	private int getAdapterType() {
		return ((ManagerActivityLollipop)context).isList ? MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST : MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID;
	}
    
    public void addSectionTitle(List<MegaOffline> nodes) {
	    if(adapter != null) {
	        adapter.setRecylerView(recyclerView);
        }
        Map<Integer, String> sections = new HashMap<>();
        int folderCount = 0;
        int fileCount = 0;
        for (MegaOffline node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.isFolder()) {
                folderCount++;
            } else {
                fileCount++;
            }
        }

        if (getAdapterType() == MegaNodeAdapter.ITEM_VIEW_TYPE_GRID) {
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
			headerItemDecoration = new NewHeaderItemDecoration(context);
			recyclerView.addItemDecoration(headerItemDecoration);
		}
		headerItemDecoration.setType(getAdapterType());
		headerItemDecoration.setKeys(sections);
    }

	public ImageView getImageDrag(int position) {
		logDebug("Position: " + position);
		if (adapter != null) {
			if (getAdapterType() == MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
				View v = mLayoutManager.findViewByPosition(position);
				if (v != null) {
					return (ImageView) v.findViewById(R.id.offline_list_thumbnail);
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
			logDebug("ActionBarCallBack::onActionItemClicked");
			List<MegaOffline> documents = adapter.getSelectedOfflineNodes();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						String path = documents.get(i).getPath() + documents.get(i).getName();
						MegaNode n = megaApi.getNodeByPath(path);	
						if(n == null)
						{
							continue;
						}
						handleList.add(n.getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList, false);
					break;
				}
				case R.id.cab_menu_rename:{

					if (documents.size()==1){
						String path = documents.get(0).getPath() + documents.get(0).getName();
						MegaNode n = megaApi.getNodeByPath(path);
						if(n == null)
						{
							break;
						}
						((ManagerActivityLollipop) context).showRenameDialog(n, n.getName());
					}
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_share_link:{

					if (documents.size()==1){
						String path = documents.get(0).getPath() + documents.get(0).getName();
						MegaNode n = megaApi.getNodeByPath(path);
						if(n == null)
						{
							break;
						}
						NodeController nC = new NodeController(context);
						nC.exportLink(n);
					}

					break;
				}
				case R.id.cab_menu_share:{
					//Check that all the selected options are folders
					ArrayList<Long> handleList = new ArrayList<>();
					for (int i=0;i<documents.size();i++){
						String path = documents.get(i).getPath() + documents.get(i).getName();
						MegaNode n = megaApi.getNodeByPath(path);
						if(n == null)
						{
							continue;
						}
						handleList.add(n.getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.selectContactToShareFolders(handleList);
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_move:{					
					ArrayList<Long> handleList = new ArrayList<>();
					for (int i=0;i<documents.size();i++){
						String path = documents.get(i).getPath() + documents.get(i).getName();
						MegaNode n = megaApi.getNodeByPath(path);			
						if(n == null)
						{
							continue;
						}
						handleList.add(n.getHandle());
					}
					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();					
					for (int i=0;i<documents.size();i++){
						String path = documents.get(i).getPath() + documents.get(i).getName();
						MegaNode n = megaApi.getNodeByPath(path);
						if(n == null)
						{
							continue;
						}
						handleList.add(n.getHandle());
					}

					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_delete:{
					((ManagerActivityLollipop) context).showConfirmationRemoveSomeFromOffline(documents);
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					hideMultipleSelect();
					break;
				}				
			}
			return false;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			logDebug("ActionBarCallBack::onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.offline_browser_action, menu);
			((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
			((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
			checkScroll();
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("ActionBarCallBack::onDestroyActionMode");
			hideMultipleSelect();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
			((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ZERO_DELAY);
			checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			logDebug("ActionBarCallBack::onPrepareActionMode");
			List<MegaOffline> selected = adapter.getSelectedOfflineNodes();
			
			if (isOnline(context)){
				if (selected.size() != 0) {
					menu.findItem(R.id.cab_menu_download).setVisible(false);
					menu.findItem(R.id.cab_menu_share).setVisible(false);

					if(selected.size() == adapter.getItemCount()){
						menu.findItem(R.id.cab_menu_select_all).setVisible(false);
						menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);
					}else{
						menu.findItem(R.id.cab_menu_select_all).setVisible(true);
						menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);	
					}

				}else{

					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_download).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
				}
				menu.findItem(R.id.cab_menu_share_link).setVisible(false);
				menu.findItem(R.id.cab_menu_copy).setVisible(false);
				menu.findItem(R.id.cab_menu_move).setVisible(false);				
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_rename).setVisible(false);
			}
			else{
				if (selected.size() != 0) {
					menu.findItem(R.id.cab_menu_delete).setVisible(true);
					menu.findItem(R.id.cab_menu_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

					if(selected.size()==adapter.getItemCount()){
						menu.findItem(R.id.cab_menu_select_all).setVisible(false);
						menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);			
					}else{
						menu.findItem(R.id.cab_menu_select_all).setVisible(true);
						menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);	
					}	
				}else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
				}

				menu.findItem(R.id.cab_menu_download).setVisible(false);			
				menu.findItem(R.id.cab_menu_copy).setVisible(false);
				menu.findItem(R.id.cab_menu_move).setVisible(false);
				menu.findItem(R.id.cab_menu_share_link).setVisible(false);
				menu.findItem(R.id.cab_menu_rename).setVisible(false);
			}
			return false;
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
			
			updateActionModeTitle();
		}
	}
	
	public boolean showSelectMenuItem(){
		logDebug("showSelectMenuItem");
		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}
		
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		logDebug("onCreate");
		
		if (isOnline(context)){
			if (megaApi == null){
				megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
			}
		}
		else{
			megaApi=null;
		}			

		lastPositionStack = new Stack<>();

		dbH = DatabaseHandler.getDbHandler(context);
		
		mOffList = new ArrayList<>();
	}

	public void checkScroll () {
		if (recyclerView != null ) {
			if (recyclerView.canScrollVertically(-1) || (adapter != null && adapter.isMultipleSelect())) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			}
			else {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}

		if (context instanceof ManagerActivityLollipop){

			String pathNavigationOffline = ((ManagerActivityLollipop)context).getPathNavigationOffline();
			if(pathNavigationOffline!=null){
				pathNavigation = pathNavigationOffline;
			}

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			orderGetChildren = ((ManagerActivityLollipop)context).getOrderOthers();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
	    display.getMetrics(outMetrics);

		//Check pathNAvigation
		if (((ManagerActivityLollipop)context).isList){
			logDebug("onCreateList");
			View v = inflater.inflate(R.layout.fragment_offlinelist, container, false);
			recyclerView = v.findViewById(R.id.offline_view_browser);
            recyclerView.removeItemDecoration(headerItemDecoration);
            headerItemDecoration = null;
			mLayoutManager = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager);
			//Add bottom padding for recyclerView like in other fragments.
			recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = v.findViewById(R.id.offline_empty_image);
			emptyTextView = v.findViewById(R.id.offline_empty_text);
			emptyTextViewFirst = v.findViewById(R.id.offline_empty_text_first);
			contentTextLayout = v.findViewById(R.id.offline_content_text_layout);
			contentText = v.findViewById(R.id.offline_content_text);

			findNodes();
			addSectionTitle(mOffList);
			if (adapter == null){
				adapter = new MegaOfflineLollipopAdapter(this, context, mOffList, recyclerView, emptyImageView, emptyTextView, aB, MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				adapter.setPositionClicked(-1);
				adapter.setMultipleSelect(false);
				recyclerView.setAdapter(adapter);

				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);

					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.offline_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_empty_offline);
					}
					String textToShow = getString(R.string.context_empty_offline);
					try {
						textToShow = textToShow.replace("[A]","<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]","</font>");
						textToShow = textToShow.replace("[B]","<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]","</font>");
					} catch (Exception e) {
						e.printStackTrace();
					}
					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
			}
			else{
			    adapter.setRecylerView(recyclerView);
			}

			return v;
		}
		else{
			logDebug("onCreateGRID");
			View v = inflater.inflate(R.layout.fragment_offlinegrid, container, false);
			
			recyclerView = (NewGridRecyclerView) v.findViewById(R.id.offline_view_browser_grid);
			recyclerView.removeItemDecoration(headerItemDecoration);
			headerItemDecoration = null;
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
			
			emptyImageView = v.findViewById(R.id.offline_empty_image_grid);
			emptyTextView = v.findViewById(R.id.offline_empty_text_grid);
			emptyTextViewFirst = v.findViewById(R.id.offline_empty_text_grid_first);
			contentTextLayout = v.findViewById(R.id.offline_content_grid_text_layout);

			contentText = v.findViewById(R.id.offline_content_text_grid);

			findNodes();
			addSectionTitle(mOffList);
			if (adapter == null){
				adapter = new MegaOfflineLollipopAdapter(this, context, mOffList, recyclerView, emptyImageView, emptyTextView, aB, MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID);
				adapter.setPositionClicked(-1);
				adapter.setMultipleSelect(false);
				recyclerView.setAdapter(adapter);

				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);

					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.offline_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_empty_offline);
					}
					String textToShow = getString(R.string.context_empty_offline);
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){}
					Spanned result;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
			}
			else{
                adapter.setRecylerView(recyclerView);
			}

			return v;
		}		
	}

	public void findNodes(){
		logDebug("findNodes");

		if((getActivity() == null) || (!isAdded())){
			logWarning("Fragment NOT Attached!");
			return;
		}

		mOffList=dbH.findByPath(pathNavigation);

		logDebug("Number of elements: " + mOffList.size());

		for(int i=0; i<mOffList.size();i++){

			MegaOffline checkOffline = mOffList.get(i);
			File offlineFile = getOfflineFile(context, checkOffline);
			if (!isFileAvailable(offlineFile)) {
				mOffList.remove(i);
				i--;
			}
			addSectionTitle(mOffList);
		}

		refreshOrder();

		if(adapter!=null){
			adapter.setNodes(mOffList);

			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			recyclerView.setAdapter(adapter);

			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.offline_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_empty_offline);
				}

				String textToShow = getString(R.string.context_empty_offline);
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				}
				catch (Exception e){
					e.printStackTrace();
				}
				Spanned result;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
	}

	private void sortBy(int order) {
		sort(order, mOffList, getContext());
		if (adapter != null) {
			adapter.setNodes(mOffList);
		}
	}

	public void sortByNameAscending() {
		logDebug("sortByNameAscending");
		sortBy(ORDER_DEFAULT_ASC);
	}

	public void sortByNameDescending() {
		logDebug("sortByNameDescending");
		sortBy(ORDER_DEFAULT_DESC);
	}

	public void sortByModificationDateAscending() {
		logDebug("sortByModificationDateAscending");
		sortBy(ORDER_MODIFICATION_ASC);
	}

	public void sortByModificationDateDescending() {
		logDebug("sortByModificationDateDescending");
		sortBy(ORDER_MODIFICATION_DESC);
	}

	public void sortBySizeAscending() {
		logDebug("sortBySizeAscending");
		sortBy(ORDER_SIZE_ASC);
	}

	public void sortBySizeDescending() {
		logDebug("sortBySizeDescending");
		sortBy(ORDER_SIZE_DESC);
	}

	public boolean isFolder(String path){
		logDebug("isFolder");

		MegaNode n = megaApi.getNodeByPath(path);
		if(n == null)
		{
			return false;
		}
		return n.isFolder();
	}

	@Override
    public void onAttach(Activity activity) {
		logDebug("onAttach");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
        if(mOffList != null && mOffList.size() != 0) {
			addSectionTitle(mOffList);
		}
    }

    public void itemClick(int position, int[] screenPosition, ImageView imageView) {
		logDebug("Position: " + position);
		//Otherwise out of bounds exception happens.
		if(position >= adapter.folderCount && getAdapterType() == MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID && placeholderCount != 0) {
			position -= placeholderCount;
		}
		if (adapter.isMultipleSelect()){
			logDebug("Multiselect");

			adapter.toggleSelection(position);
			List<MegaOffline> selectedNodes = adapter.getSelectedOfflineNodes();
			if (selectedNodes.size() > 0){
				updateActionModeTitle();

			}
		}
		else{
			MegaOffline currentNode = mOffList.get(position);
			File currentFile = getOfflineFile(context, currentNode);

			if(isFileAvailable(currentFile) && currentFile.isDirectory()){

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

				logDebug("Push to stack " + lastFirstVisiblePosition + " position");
				lastPositionStack.push(lastFirstVisiblePosition);

				pathNavigation= currentNode.getPath()+ currentNode.getName()+"/";
				
				if (context instanceof ManagerActivityLollipop){
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					((ManagerActivityLollipop)context).setPathNavigationOffline(pathNavigation);
				}
				((ManagerActivityLollipop)context).setToolbarTitle();

				mOffList=dbH.findByPath(currentNode.getPath()+currentNode.getName()+"/");
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);						
				}
				else{
					File offlineDirectory = null;
					String path = getOfflineAbsolutePath(context, currentNode);
											
					for(int i=0; i<mOffList.size();i++){
						
						if (Environment.getExternalStorageDirectory() != null){
							offlineDirectory = new File(path + mOffList.get(i).getPath()+mOffList.get(i).getName());
						}
						else{
							offlineDirectory = context.getFilesDir();
						}	

						if (!offlineDirectory.exists()){
							//Updating the DB because the file does not exist
							dbH.removeById(mOffList.get(i).getId());
							mOffList.remove(i);
							i--;
						}			
					}
				}
				adapter.setNodes(mOffList);
				
				refreshOrder();
				
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);

					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.offline_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_empty_offline);
					}
					String textToShow = getString(R.string.context_empty_offline);
					try{
						textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
						textToShow = textToShow.replace("[/A]", "</font>");
						textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
						textToShow = textToShow.replace("[/B]", "</font>");
					}
					catch (Exception e){
						e.printStackTrace();
					}
					Spanned result;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}
					emptyTextViewFirst.setText(result);
				}
				else{
					recyclerView.setVisibility(View.VISIBLE);
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
				
				adapter.setPositionClicked(-1);
				recyclerView.scrollToPosition(0);
				notifyDataSetChanged();
			}
			else{
				if(currentFile.exists() && currentFile.isFile()){			
					
					//Open it!
					if(MimeTypeList.typeForName(currentFile.getName()).isZip()){
						logDebug("MimeTypeList ZIP");
						Intent intentZip = new Intent();
						intentZip.setClass(context, ZipBrowserActivityLollipop.class);
						intentZip.setAction(ZipBrowserActivityLollipop.ACTION_OPEN_ZIP_FILE);
						intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_ZIP_FILE_TO_OPEN, pathNavigation);
						intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, currentFile.getAbsolutePath());
						context.startActivity(intentZip);
					}
					else if (MimeTypeList.typeForName(currentFile.getName()).isImage()){
						Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
                        intent.putExtra("placeholder", placeholderCount);
						intent.putExtra("position", position);
						intent.putExtra("adapterType", OFFLINE_ADAPTER);
						intent.putExtra("parentNodeHandle", -1L);
						intent.putExtra("offlinePathDirectory", currentFile.getParent());
						intent.putExtra("pathNavigation", pathNavigation);
						intent.putExtra("orderGetChildren", orderGetChildren);
						intent.putExtra("screenPosition", screenPosition);

						startActivity(intent);
						((ManagerActivityLollipop) context).overridePendingTransition(0,0);
						imageDrag = imageView;
					}
					else if (MimeTypeList.typeForName(currentFile.getName()).isVideoReproducible() || MimeTypeList.typeForName(currentFile.getName()).isAudio()) {
						logDebug("Video file");

						Intent mediaIntent;
						boolean internalIntent;
						boolean opusFile = false;
						if (MimeTypeList.typeForName(currentFile.getName()).isVideoNotSupported() || MimeTypeList.typeForName(currentFile.getName()).isAudioNotSupported()) {
							mediaIntent = new Intent(Intent.ACTION_VIEW);
							internalIntent = false;
							String[] s = currentFile.getName().split("\\.");
							if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
								opusFile = true;
							}
						}
						else {
							internalIntent = true;
							mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
						}

						mediaIntent.putExtra("HANDLE", Long.parseLong(currentNode.getHandle()));
						mediaIntent.putExtra("FILENAME", currentNode.getName());
						mediaIntent.putExtra("path", currentFile.getAbsolutePath());
						mediaIntent.putExtra("adapterType", OFFLINE_ADAPTER);
                        mediaIntent.putExtra("placeholder", placeholderCount);
						mediaIntent.putExtra("position", position);
						mediaIntent.putExtra("parentNodeHandle", -1L);
						mediaIntent.putExtra("offlinePathDirectory", currentFile.getParent());
						mediaIntent.putExtra("pathNavigation", pathNavigation);
						mediaIntent.putExtra("orderGetChildren", orderGetChildren);
						mediaIntent.putExtra("screenPosition", screenPosition);
						mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						else{
							mediaIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						if (opusFile){
							mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
						}
						if (internalIntent){
							startActivity(mediaIntent);
						}
						else {
							if (isIntentAvailable(context, mediaIntent)){
								startActivity(mediaIntent);
							}
							else {
								((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);

								Intent intentShare = new Intent(Intent.ACTION_SEND);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
									intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
								}
								else {
									intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
								}
								intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
								if (isIntentAvailable(context, intentShare)) {
									logDebug("Call to startActivity(intentShare)");
									context.startActivity(intentShare);
								}
							}
						}
						((ManagerActivityLollipop) context).overridePendingTransition(0,0);
						imageDrag = imageView;
					}
					else if (MimeTypeList.typeForName(currentFile.getName()).isPdf()){
						logDebug("PDF file");

						Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);

						pdfIntent.putExtra("inside", true);
						pdfIntent.putExtra("HANDLE", Long.parseLong(currentNode.getHandle()));
						pdfIntent.putExtra("adapterType", OFFLINE_ADAPTER);
						pdfIntent.putExtra("path", currentFile.getAbsolutePath());
						pdfIntent.putExtra("pathNavigation", pathNavigation);
						pdfIntent.putExtra("screenPosition", screenPosition);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						else{
							pdfIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						context.startActivity(pdfIntent);
						((ManagerActivityLollipop) context).overridePendingTransition(0,0);
						imageDrag = imageView;
					}
					else if (MimeTypeList.typeForName(currentFile.getName()).isURL()) {
						logDebug("Is URL file");
						InputStream instream = null;
						try {
							// open the file for reading
							instream = new FileInputStream(currentFile.getAbsolutePath());

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
									openFile(currentFile);
								}
							}
						} catch (Exception ex) {

							openFile(currentFile);

						} finally {
							// close the file.
							try {
								instream.close();
							} catch (Exception e) {
								logDebug("EXCEPTION closing InputStream");
							}
						}
					}
					else{
						openFile(currentFile);
					}
				}
			}
		}
    }

	@Override
	public void multipleItemClick(int position) {
		if (position >= adapter.folderCount && getAdapterType() == MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID && placeholderCount != 0) {
			position -= placeholderCount;
		}
		adapter.toggleSelection(position);
	}

    public void openFile (File currentFile){
		logDebug("openFile");
    	Intent viewIntent = new Intent(Intent.ACTION_VIEW);

    	String type;
		if (MimeTypeList.typeForName(currentFile.getName()).isURL()){
			type = "text/plain";
		}
		else{
			type = MimeTypeList.typeForName(currentFile.getName()).getType();
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			viewIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", currentFile), type);
		}
		else{
			viewIntent.setDataAndType(Uri.fromFile(currentFile), type);
		}
		viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if (isIntentAvailable(context, viewIntent)){
			context.startActivity(viewIntent);
		}
		else{
			Intent intentShare = new Intent(Intent.ACTION_SEND);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
			}
			else{
				intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
			}
			intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			if (isIntentAvailable(context, intentShare)){
				context.startActivity(intentShare);
			}
		}
    }

	@Override
	protected void updateActionModeTitle() {
		logDebug("updateActionModeTitle");
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaOffline> documents = adapter.getSelectedOfflineNodes();
		int folders=0;
		int files=0;
		
		if(documents.size()>0){
			String pathI = getOfflineAbsolutePath(context, documents.get(0));
			
			for(int i=0; i<documents.size();i++){
				MegaOffline mOff = documents.get(i);
				String path = pathI + mOff.getPath() + mOff.getName();			

				File destination = new File(path);
				if (destination.exists()){
					if(destination.isFile()){
						files++;					
					}
					else{
						folders++;					
					}
				}
				else{
					logWarning("File do not exist");
				}		
			}
		}

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
		adapter.clearSelections();
		adapter.setMultipleSelect(false);

		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		logDebug("onBackPressed");

		if (adapter == null){
			return 0;
		}

		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else if(pathNavigation != null){
			if(pathNavigation.isEmpty()){
				return 0;
			}
			if (!pathNavigation.equals("/")){
				pathNavigation=pathNavigation.substring(0,pathNavigation.length()-1);
				int index=pathNavigation.lastIndexOf("/");
				pathNavigation=pathNavigation.substring(0,index+1);
				
				if (context instanceof ManagerActivityLollipop){
					((ManagerActivityLollipop)context).setPathNavigationOffline(pathNavigation);
					((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
					((ManagerActivityLollipop)context).setToolbarTitle();
				}

				mOffList = dbH.findByPath(pathNavigation);

				refreshOrder();

				setNodes(mOffList);

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
			else{
				logDebug("Navigation path is ROOT");
				return 0;
			}
		}
		else{
				return 0;
		}
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
	}
	
	public void setNodes(ArrayList<MegaOffline> _mOff){
		logDebug("setNodes");
		this.mOffList = _mOff;

		if (adapter != null){
			adapter.setNodes(mOffList);
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.offline_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_empty_offline);
				}
				String textToShow = getString(R.string.context_empty_offline);
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				}
				catch (Exception e){
					e.printStackTrace();
				}
				Spanned result;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}			
		}
	}
	
	public void setPositionClicked(int positionClicked){
		logDebug("Position: " + positionClicked);
		if (adapter != null){
			adapter.setPositionClicked(positionClicked);
		}
	}
	
	public void notifyDataSetChanged(){
		logDebug("notifyDataSetChanged");
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}

	public void refresh(){
		logDebug("refresh");

		mOffList=dbH.findByPath(pathNavigation);

		refreshOrder();

		if (((ManagerActivityLollipop)context).isList) {
			addSectionTitle(mOffList);
			if (adapter == null) {
				adapter = new MegaOfflineLollipopAdapter(this,context,mOffList,recyclerView,emptyImageView,emptyTextView,aB,MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			} else {
				adapter.setNodes(mOffList);
				adapter.notifyDataSetChanged();
				recyclerView.invalidate();
			}
		} else {
			addSectionTitle(mOffList);
			if (adapter == null) {
				adapter = new MegaOfflineLollipopAdapter(this,context,mOffList,recyclerView,emptyImageView,emptyTextView,aB,MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			} else {
				adapter.setNodes(mOffList);
                adapter.notifyDataSetChanged();
                recyclerView.invalidate();
			}
		}
		if (adapter.getItemCount() == 0){
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			contentTextLayout.setVisibility(View.GONE);
			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				emptyImageView.setImageResource(R.drawable.offline_empty_landscape);
			}else{
				emptyImageView.setImageResource(R.drawable.ic_empty_offline);
			}
			String textToShow = getString(R.string.context_empty_offline);
			try{
				textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
				textToShow = textToShow.replace("[/A]", "</font>");
				textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
				textToShow = textToShow.replace("[/B]", "</font>");
			}
			catch (Exception e){
				e.printStackTrace();
			}
			Spanned result;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
			} else {
				result = Html.fromHtml(textToShow);
			}
			emptyTextViewFirst.setText(result);
			onBackPressed();

		}else{
			recyclerView.setVisibility(View.VISIBLE);
			contentTextLayout.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
		addSectionTitle(mOffList);
		setPositionClicked(-1);
		notifyDataSetChanged();
	}
	
	public void refreshPaths(){
		logDebug("refreshPaths()");
		mOffList=dbH.findByPath("/");	
		
		refreshOrder();

		if (((ManagerActivityLollipop)context).isList) {
			addSectionTitle(mOffList);
			if (adapter == null) {
				adapter = new MegaOfflineLollipopAdapter(this,context,mOffList,recyclerView,emptyImageView,emptyTextView,aB,MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			} else {
				adapter.setNodes(mOffList);
			}
		} else {
			addSectionTitle(mOffList);
			if (adapter == null) {
				adapter = new MegaOfflineLollipopAdapter(this,context,mOffList,recyclerView,emptyImageView,emptyTextView,aB,MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			} else {
				adapter.setNodes(mOffList);
			}
		}
		addSectionTitle(mOffList);
		recyclerView.invalidate();
	}
	
	public void refreshPaths(MegaOffline mOff){
		logDebug("Offline node handle: " + mOff.getHandle());
		
		//Find in the tree, the last existing node
		String pNav= mOff.getPath();
		
		if(mOff.getType().equals(DB_FILE)){
			
			int index=pNav.lastIndexOf("/");
			pNav=pNav.substring(0,index+1);
			
		}
		else{
			pNav=pNav.substring(0,pNav.length()-1);
		}	
			
		if(pNav.length()==0){
			mOffList=dbH.findByPath("/");
		}
		else{
			findPath(pNav);			
		}
				
		refreshOrder();

		((ManagerActivityLollipop)context).setToolbarTitle();
		setNodes(mOffList);
	}
	
	public int getItemCount(){
		logDebug("getItemCount");
		if(adapter != null){
			return adapter.getItemCount();
		}
		return 0;
	}
	
	private void findPath (String pNav){
		MegaOffline nodeToShow;
		
		if(!pNav.equals("/")){
			
			if (pNav.endsWith("/")) {
				pNav = pNav.substring(0, pNav.length() - 1);
			}
			
			int index=pNav.lastIndexOf("/");	
			String pathToShow = pNav.substring(0, index+1);
			String nameToShow = pNav.substring(index+1, pNav.length());
			
			nodeToShow = dbH.findbyPathAndName(pathToShow, nameToShow);
			if(nodeToShow!=null){
				//Show the node
				logDebug("Node Handle: "+ nodeToShow.getHandle());
				pathNavigation=pathToShow+nodeToShow.getName()+"/";
				return;
			}
			else{
				if(pathNavigation.equals("/")){
					logDebug("Return Path /");
					return;
				}
				else{
					findPath(pathToShow);
				}				
			}
		}
		else{
			pathNavigation="/";
		}		
	}

	public void setPathNavigation(String _pathNavigation){
		logDebug("setPathNavigation()");
		this.pathNavigation = _pathNavigation;
		addSectionTitle(mOffList);
		if (adapter != null){
			adapter.setNodes(dbH.findByPath(_pathNavigation));
		}
	}

	public void setOrder(int orderGetChildren){
		logDebug("setOrder");
		this.orderGetChildren = orderGetChildren;
	}

	private void refreshOrder() {
		switch (orderGetChildren) {
			case MegaApiJava.ORDER_DEFAULT_ASC: {
				sortByNameAscending();
				break;
			}
			case ORDER_DEFAULT_DESC: {
				sortByNameDescending();
				break;
			}
			case ORDER_MODIFICATION_ASC: {
				sortByModificationDateAscending();
				break;
			}
			case ORDER_MODIFICATION_DESC: {
				sortByModificationDateDescending();
				break;
			}
			case ORDER_SIZE_ASC: {
				sortBySizeAscending();
				break;
			}
			case ORDER_SIZE_DESC: {
				sortBySizeDescending();
				break;
			}
			default: {
				break;
			}
		}
	}
	
	public String getPathNavigation() {
		logDebug("getPathNavigation");
		return pathNavigation;
	}
}