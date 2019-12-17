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
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class RubbishBinFragmentLollipop extends Fragment{

	public static ImageView imageDrag;

	public static int GRID_WIDTH =400;
	
	Context context;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	CustomizedGridLayoutManager gridLayoutManager;
	MegaNodeAdapter adapter;
	private int placeholderCount;
	public NewHeaderItemDecoration headerItemDecoration;

	ArrayList<MegaNode> nodes;
	
	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

	TextView contentText;
	RelativeLayout contentTextLayout;

	MegaApiAndroid megaApi;
	
	public ActionMode actionMode;
	
	float density;
	DisplayMetrics outMetrics;
	Display display;

	Stack<Integer> lastPositionStack;

	DatabaseHandler dbH;
	MegaPreferences prefs;
	String downloadLocationDefaultPath;
	////

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
		if (adapter != null){
			if (adapter.getAdapterType() == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null){
				View v = mLayoutManager.findViewByPosition(position);
				if (v != null){
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

	public void checkScroll() {
		if (recyclerView != null) {
			if ((recyclerView.canScrollVertically(-1) && recyclerView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect())) {
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
            sections.put(0, getString(R.string.general_folders));
            sections.put(folderCount, getString(R.string.general_files));
        }
		if (headerItemDecoration == null) {
			headerItemDecoration = new NewHeaderItemDecoration(context);
			recyclerView.addItemDecoration(headerItemDecoration);
		}
		headerItemDecoration.setType(type);
		headerItemDecoration.setKeys(sections);
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

					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
					clearSelections();
					hideMultipleSelect();
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
				case R.id.cab_menu_restore_from_rubbish:{

					if(documents!=null){
						if(documents.size()>1){
							logDebug("Restore multiple: " + documents.size());
							MultipleRequestListener moveMultipleListener = new MultipleRequestListener(MULTIPLE_RESTORED_FROM_RUBBISH, (ManagerActivityLollipop) context);
							for (int i=0;i<documents.size();i++){
								MegaNode newParent = megaApi.getNodeByHandle(documents.get(i).getRestoreHandle());
								if(newParent !=null){
									megaApi.moveNode(documents.get(i), newParent, moveMultipleListener);
								}
								else{
									logWarning("The restore folder no longer exists");
								}
							}
						}
						else{
							logDebug("Restore single item");
							((ManagerActivityLollipop) context).restoreFromRubbish(documents.get(0));

						}
					}
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
            ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
            checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
            ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ZERO_DELAY);
            checkScroll();
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
			boolean showRestore = true;
			showRename = false;
			showLink = false;

			if (selected.size() != 0) {
				showTrash = true;
				showMove = true;
                showCopy = true;

//				for(int i=0; i<selected.size();i++)	{
//					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
//						showTrash = false;
//						showMove = false;
//						break;
//					}
//				}

				if(selected.size()==1){
					showRename=true;
				}
				else{
					showRename=false;
				}

				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}

				for(int i = 0; i<selected.size();i++){
					long restoreHandle = selected.get(i).getRestoreHandle();
					if(restoreHandle!=-1){
						MegaNode restoreNode = megaApi.getNodeByHandle(restoreHandle);
						if((!megaApi.isInRubbish(selected.get(i))) || restoreNode==null || megaApi.isInRubbish(restoreNode)){
							showRestore = false;
							break;
						}
					}
					else{
						showRestore = false;
						break;
					}
				}

				if(showRestore){
					menu.findItem(R.id.cab_menu_restore_from_rubbish).setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_restore_from_rubbish).setVisible(false);
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
				menu.findItem(R.id.cab_menu_restore_from_rubbish).setVisible(false);
			}

			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);

			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);

			if(showCopy){
				menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			menu.findItem(R.id.cab_menu_move).setVisible(showMove);

			if(showMove){
				menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			if (showTrash){
				menu.findItem(R.id.cab_menu_trash).setTitle(context.getString(R.string.context_remove));
			}
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			if(showTrash){
				menu.findItem(R.id.cab_menu_trash).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
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

	public static RubbishBinFragmentLollipop newInstance() {
		logDebug("newInstance");
		RubbishBinFragmentLollipop fragment = new RubbishBinFragmentLollipop();
		return fragment;
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");

		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();

		downloadLocationDefaultPath = getDownloadLocation(context);

		lastPositionStack = new Stack<>();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		logDebug("onCreateView");
		
		if (megaApi.getRootNode() == null){
			return null;
		}

		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		density  = getResources().getDisplayMetrics().density;

		if (((ManagerActivityLollipop)context).getParentHandleRubbish() == -1||((ManagerActivityLollipop)context).getParentHandleRubbish()==megaApi.getRubbishNode().getHandle()){
			logDebug("Parent is the Rubbish: " + ((ManagerActivityLollipop)context).getParentHandleRubbish());

			nodes = megaApi.getChildren(megaApi.getRubbishNode(), ((ManagerActivityLollipop)context).orderCloud);

		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).getParentHandleRubbish());

			if (parentNode != null){
				logDebug("The parent node is: " + parentNode.getHandle());
				nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
			
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			}
			nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
		}

		((ManagerActivityLollipop)context).setToolbarTitle();
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		if (((ManagerActivityLollipop)context).isList){
			logDebug("List View");
			View v = inflater.inflate(R.layout.fragment_rubbishbinlist, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.rubbishbin_list_view);

//			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
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
			
			emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_list_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.rubbishbin_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.rubbishbin_list_empty_text_first);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.rubbishbin_content_text_layout);
			contentText = (TextView) v.findViewById(R.id.rubbishbin_list_content_text);
			
//			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			if (adapter == null){
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop)context).getParentHandleRubbish(), recyclerView, null, RUBBISH_BIN_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setParentHandle(((ManagerActivityLollipop)context).getParentHandleRubbish());
				adapter.setListFragment(recyclerView);
//				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
			}

			if(megaApi.getRubbishNode()!=null){
				logDebug("Set content of the Rubbish Bin: " + ((ManagerActivityLollipop)context).getParentHandleRubbish());
				if (((ManagerActivityLollipop)context).getParentHandleRubbish() == megaApi.getRubbishNode().getHandle()||((ManagerActivityLollipop)context).getParentHandleRubbish()==-1){
					contentText.setText(getInfoFolder(megaApi.getRubbishNode(), context));

				}
				else{
					MegaNode infoNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).getParentHandleRubbish());
					contentText.setText(getInfoFolder(infoNode, context));
				}
			}

			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);

			setNodes(nodes);

			if (adapter.getItemCount() == 0){
				
				recyclerView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (megaApi.getRubbishNode().getHandle()==((ManagerActivityLollipop)context).getParentHandleRubbish()||((ManagerActivityLollipop)context).getParentHandleRubbish()==-1) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.rubbish_bin_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_rubbish_bin));

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
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
			
			return v;
		}
		else{
			logDebug("Grid View");
			View v = inflater.inflate(R.layout.fragment_rubbishbingrid, container, false);

			recyclerView = (RecyclerView) v.findViewById(R.id.rubbishbin_grid_view);
			recyclerView.setHasFixedSize(true);
			gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
//			gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//				@Override
//			      public int getSpanSize(int position) {
//					return 1;
//				}
//			});
			
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});
			
			emptyImageView = (ImageView) v.findViewById(R.id.rubbishbin_grid_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.rubbishbin_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.rubbishbin_grid_empty_text_first);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.rubbishbin_grid_content_text_layout);
			contentText = (TextView) v.findViewById(R.id.rubbishbin_grid_content_text);
			
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			if (adapter == null){
				adapter = new MegaNodeAdapter(context, this, nodes, ((ManagerActivityLollipop)context).getParentHandleRubbish(), recyclerView, null, RUBBISH_BIN_ADAPTER, MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setParentHandle(((ManagerActivityLollipop)context).getParentHandleRubbish());
				adapter.setListFragment(recyclerView);
				adapter.setNodes(nodes);
				adapter.setAdapterType(MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
			}

			if(megaApi.getRubbishNode()!=null){
				if (((ManagerActivityLollipop)context).getParentHandleRubbish() == megaApi.getRubbishNode().getHandle()||((ManagerActivityLollipop)context).getParentHandleRubbish()==-1){
					contentText.setText(getInfoFolder(megaApi.getRubbishNode(), context));
				}
				else{
					MegaNode infoNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).getParentHandleRubbish());
					contentText.setText(getInfoFolder(infoNode, context));
				}
			}

			adapter.setMultipleSelect(false);

			recyclerView.setAdapter(adapter);
			
			setNodes(nodes);
			
			if (adapter.getItemCount() == 0){
				
				recyclerView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (megaApi.getRubbishNode().getHandle()==((ManagerActivityLollipop)context).getParentHandleRubbish()||((ManagerActivityLollipop)context).getParentHandleRubbish()==-1) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.rubbish_bin_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_rubbish_bin));

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
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
			return v;
		}
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
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
			if (nodes.get(position).isFolder()){
				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;
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

				((ManagerActivityLollipop)context).setParentHandleRubbish(n.getHandle());

				((ManagerActivityLollipop)context).setToolbarTitle();
				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

				MegaNode infoNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).getParentHandleRubbish());
				contentText.setText(getInfoFolder(infoNode, context));

				adapter.setParentHandle(((ManagerActivityLollipop)context).getParentHandleRubbish());
				nodes = megaApi.getChildren(nodes.get(position), ((ManagerActivityLollipop)context).orderCloud);
				addSectionTitle(nodes,adapter.getAdapterType());
				adapter.setNodes(nodes);
				recyclerView.scrollToPosition(0);
				
				//If folder has no files
				if (adapter.getItemCount() == 0){
					recyclerView.setVisibility(View.GONE);
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);

					if (megaApi.getRubbishNode().getHandle()==((ManagerActivityLollipop)context).getParentHandleRubbish()||((ManagerActivityLollipop)context).getParentHandleRubbish()==-1) {
						if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
							emptyImageView.setImageResource(R.drawable.rubbish_bin_empty_landscape);
						}else{
							emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
						}

						String textToShow = String.format(context.getString(R.string.context_empty_rubbish_bin));

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
					contentTextLayout.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
				checkScroll();
			}
			else{
				//Is FILE
				if (MimeTypeList.typeForName(nodes.get(position).getName()).isImage()){
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
					//Put flag to notify FullScreenImageViewerLollipop.
					intent.putExtra("placeholder",placeholderCount);
					intent.putExtra("position", position);
					intent.putExtra("adapterType", RUBBISH_BIN_ADAPTER);
					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_RUBBISH){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
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
					logDebug("FILE HANDLE: " + file.getHandle() + ", TYPE: " + mimeType);

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
					mediaIntent.putExtra("screenPosition", screenPosition);
					mediaIntent.putExtra("FILENAME", file.getName());
					mediaIntent.putExtra("adapterType", RUBBISH_BIN_ADAPTER);

					if (megaApi.getParentNode(nodes.get(position)).getType() == MegaNode.TYPE_RUBBISH){
						mediaIntent.putExtra("parentNodeHandle", -1L);
					}
					else{
						mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(position)).getHandle());
					}
					boolean isOnMegaDownloads = false;
					String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
					File f = new File(downloadLocationDefaultPath, file.getName());
					if(f.exists() && (f.length() == file.getSize())){
						isOnMegaDownloads = true;
					}
					if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))){
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
					mediaIntent.putExtra("HANDLE", file.getHandle());
					imageDrag = imageView;
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
				}
				else if (MimeTypeList.typeForName(nodes.get(position).getName()).isPdf()){
					MegaNode file = nodes.get(position);

					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					logDebug("FILE HANDLE: " + file.getHandle() + ", TYPE: " + mimeType);

					Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);
					pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

					pdfIntent.putExtra("adapterType", RUBBISH_BIN_ADAPTER);
					pdfIntent.putExtra("inside", true);
					pdfIntent.putExtra("APP", true);
					boolean isOnMegaDownloads = false;
					String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
					File f = new File(downloadLocationDefaultPath, file.getName());
					if(f.exists() && (f.length() == file.getSize())){
						isOnMegaDownloads = true;
					}
					if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))){
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
						startActivity(pdfIntent);
					}
					else{
						Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

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
									logWarning("Not expected format: Exception on processing url file");
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
		/*String format = "%d %s";
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
		}*/

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
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
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

		if (adapter == null){
			return 0;
		}

		if (((ManagerActivityLollipop) context).comesFromNotifications && ((ManagerActivityLollipop) context).comesFromNotificationHandle == (((ManagerActivityLollipop)context).getParentHandleRubbish())) {
			((ManagerActivityLollipop) context).comesFromNotifications = false;
			((ManagerActivityLollipop) context).comesFromNotificationHandle = -1;
			((ManagerActivityLollipop) context).selectDrawerItemLollipop(ManagerActivityLollipop.DrawerItem.NOTIFICATIONS);
			((ManagerActivityLollipop)context).setParentHandleRubbish(((ManagerActivityLollipop)context).comesFromNotificationHandleSaved);
			((ManagerActivityLollipop)context).comesFromNotificationHandleSaved = -1;

			return 2;
		}
		else {
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(((ManagerActivityLollipop)context).getParentHandleRubbish()));
			if (parentNode != null){
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
				((ManagerActivityLollipop)context).setParentHandleRubbish(parentNode.getHandle());

				((ManagerActivityLollipop)context).setToolbarTitle();
				nodes = megaApi.getChildren(parentNode, ((ManagerActivityLollipop)context).orderCloud);
				addSectionTitle(nodes,adapter.getAdapterType());
				adapter.setNodes(nodes);

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

//			adapter.setParentHandle(parentHandle);
				contentText.setText(getInfoFolder(parentNode, context));
				return 2;
			}
			else{
				return 0;
			}
		}
	}
	
	public void setContentText(){
		logDebug("setContentText");
		MegaNode rN = megaApi.getRubbishNode();
		if(rN!=null){
			if (((ManagerActivityLollipop)context).getParentHandleRubbish() == rN.getHandle()||((ManagerActivityLollipop)context).getParentHandleRubbish()==-1){
				contentText.setText(getInfoFolder(rN, context));

			}
			else{
				MegaNode infoNode = megaApi.getNodeByHandle(((ManagerActivityLollipop)context).getParentHandleRubbish());
				if (infoNode !=  null){
					contentText.setText(getInfoFolder(infoNode, context));
				}
				else{
					logWarning("INFO NODE null");
				}
			}
		}
		else{
			logWarning("INFO NODE null");
		}
	}

	public long getParentHandle(){
		return ((ManagerActivityLollipop)context).getParentHandleRubbish();
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		logDebug("setNodes");
		this.nodes = nodes;

		if(megaApi!=null){
			if(megaApi.getRubbishNode()==null){
				logError("megaApi.getRubbishNode() is NULL");
				return;
			}
		}

		this.nodes = nodes;
		if (((ManagerActivityLollipop)context).isList) {
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_LIST);
		}
		else {
			addSectionTitle(nodes,MegaNodeAdapter.ITEM_VIEW_TYPE_GRID);
		}
		
		if (adapter != null){
			adapter.setNodes(this.nodes);
			if (adapter.getItemCount() == 0){
				recyclerView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (megaApi.getRubbishNode().getHandle()==((ManagerActivityLollipop)context).getParentHandleRubbish()||((ManagerActivityLollipop)context).getParentHandleRubbish()==-1) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.rubbish_bin_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.rubbish_bin_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_rubbish_bin));

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
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}			
		}

		setContentText();
	}

	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}

	public boolean isMultipleselect(){
		return adapter.isMultipleSelect();
	}

	public int getItemCount(){
		return adapter.getItemCount();
	}
}
