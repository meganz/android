package mega.privacy.android.app.lollipop.providers;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.providers.FileProviderActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class IncomingSharesProviderFragmentLollipop extends Fragment{

	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes;
	long parentHandle = -1;
	
	long [] hashes;
	
	MegaProviderLollipopAdapter adapter;
	public String name;

	RecyclerView listView;
	LinearLayoutManager mLayoutManager;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;
	TextView contentText;

	int deepBrowserTree = -1;

	Stack<Integer> lastPositionStack;

	public ActionMode actionMode;

	Handler handler;

	public void activateActionMode(){
		logDebug("activateActionMode");
		if(!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			logDebug("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			changeStatusBarColorActionMode(context, ((FileProviderActivity) context).getWindow(), handler, 1);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			logDebug("onPrepareActionMode");
			List<MegaNode> selected = adapter.getSelectedNodes();

			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showEditLink = false;
			boolean showRemoveLink = false;
			boolean showTrash = false;
			boolean showShare = false;

			if (selected.size() != 0) {

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
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}


			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);

			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);

			menu.findItem(R.id.cab_menu_move).setVisible(showMove);

			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);

			menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);

			menu.findItem(R.id.cab_menu_edit_link).setVisible(showEditLink);

			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

			menu.findItem(R.id.cab_menu_share).setVisible(showShare);
			menu.findItem(R.id.cab_menu_share).setTitle(context.getResources().getQuantityString(R.plurals.context_share_folders, selected.size()));

			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			logDebug("onActionItemClicked");
			List<MegaNode> documents = adapter.getSelectedNodes();

			switch (item.getItemId()) {
				case R.id.action_mode_close_button: {
					logDebug("Close button");
				}
				case R.id.cab_menu_select_all: {
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all: {
					clearSelections();
					hideMultipleSelect();
					break;
				}
			}
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logDebug("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			changeStatusBarColorActionMode(context, ((FileProviderActivity) context).getWindow(), handler, 0);
			updateActionModeTitle();
		}
	}


	public static IncomingSharesProviderFragmentLollipop newInstance() {
		logDebug("newInstance");
		IncomingSharesProviderFragmentLollipop fragment = new IncomingSharesProviderFragmentLollipop();
		return fragment;
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		logDebug("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (megaApi.getRootNode() == null){
			return;
		}

		lastPositionStack = new Stack<>();

		nodes = new ArrayList<MegaNode>();

		handler = new Handler();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		logDebug("onCreateView");
	
		View v = inflater.inflate(R.layout.fragment_clouddriveprovider, container, false);	
		
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);

		listView = (RecyclerView) v.findViewById(R.id.provider_list_view_browser);

		listView.addItemDecoration(new SimpleDividerItemDecoration(context, metrics));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.setItemAnimator(new DefaultItemAnimator());
		
		contentText = (TextView) v.findViewById(R.id.provider_content_text);
		contentText.setVisibility(View.GONE);

		emptyImageView = (ImageView) v.findViewById(R.id.provider_list_empty_image);
		emptyTextView = (LinearLayout) v.findViewById(R.id.provider_list_empty_text);
		emptyTextViewFirst = (TextView) v.findViewById(R.id.provider_list_empty_text_first);

		if (context instanceof FileProviderActivity){
			parentHandle = ((FileProviderActivity)context).getIncParentHandle();
			deepBrowserTree = ((FileProviderActivity)context).getIncomingDeepBrowserTree();
			logDebug("The parent handle is: " + parentHandle);
			logDebug("The browser tree deep is: " + deepBrowserTree);
		}
		
		if (adapter == null){
			adapter = new MegaProviderLollipopAdapter(context, this, nodes, parentHandle, listView, emptyImageView, emptyTextView, INCOMING_SHARES_PROVIDER_ADAPTER);
		}
		listView.setAdapter(adapter);

		if (parentHandle == -1){
			findNodes();
			setNodes(nodes);
			adapter.setParentHandle(-1);
		}
		else{
			adapter.setParentHandle(parentHandle);
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode!=null){
				nodes = megaApi.getChildren(parentNode);
				setNodes(nodes);
				logDebug("INCOMING is in: " + parentNode.getName());
				changeActionBarTitle(parentNode.getName());
			}
			else{
				logWarning("ERROR parentNode is NULL");
				findNodes();
				setNodes(nodes);
				parentHandle = -1;
				adapter.setParentHandle(-1);
				changeActionBarTitle(getString(R.string.file_provider_title).toUpperCase());
				if (context instanceof FileProviderActivity){
					((FileProviderActivity)context).setParentHandle(parentHandle);
					logDebug("PArentHandle change to: " + parentHandle);
				}
			}
		}
	
		adapter.setPositionClicked(-1);

		return v;
	}
	
	public void findNodes(){
		logDebug("findNodes");

		deepBrowserTree=0;
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setIncomingDeepBrowserTree(deepBrowserTree);
			logDebug("The browser tree change to: " + deepBrowserTree);
		}
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		nodes.clear();
		for (int i=0;i<contacts.size();i++){			
			ArrayList<MegaNode> nodeContact=megaApi.getInShares(contacts.get(i));
			if(nodeContact!=null){
				if(nodeContact.size()>0){
					nodes.addAll(nodeContact);
				}
			}			
		}

		changeActionBarTitle(getString(R.string.file_provider_title).toUpperCase());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);
	}

	public void changeActionBarTitle(String folder){
		if (context instanceof FileProviderActivity){
			int tabShown = ((FileProviderActivity)context).getTabShown();

			if(tabShown==FileProviderActivity.INCOMING_TAB){
				((FileProviderActivity) context).changeTitle(folder);
			}
		}
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public void itemClick(int position) {
		logDebug("deepBrowserTree: " + deepBrowserTree);
		if (adapter.isMultipleSelect()) {
			logDebug("Multiselect ON");
			adapter.toggleSelection(position);

			List<MegaNode> selectedNodes = adapter.getSelectedNodes();
			if(selectedNodes.size()>0){
				updateActionModeTitle();
				((FileProviderActivity)context).activateButton(true);
				((FileProviderActivity)context).attachFiles(selectedNodes);
			}
			else{

				((FileProviderActivity)context).activateButton(false);
			}
		}
		else {
			((FileProviderActivity)context).activateButton(false);
			if (nodes.get(position).isFolder()) {

				deepBrowserTree = deepBrowserTree + 1;
				if (context instanceof FileProviderActivity) {
					((FileProviderActivity) context).setIncomingDeepBrowserTree(deepBrowserTree);
					logDebug("The browser tree change to: " + deepBrowserTree);
				}

				MegaNode n = nodes.get(position);

				int lastFirstVisiblePosition = 0;

				lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

				logDebug("Push to stack " + lastFirstVisiblePosition + " position");
				lastPositionStack.push(lastFirstVisiblePosition);

				String path = n.getName();
				String[] temp;
				temp = path.split("/");
				name = temp[temp.length - 1];

				changeActionBarTitle(name);

				parentHandle = nodes.get(position).getHandle();
				if (context instanceof FileProviderActivity) {
					((FileProviderActivity) context).setIncParentHandle(parentHandle);
					logDebug("The parent handle change to: " + parentHandle);
				}
				adapter.setParentHandle(parentHandle);
				nodes = megaApi.getChildren(nodes.get(position));
				setNodes(nodes);
				listView.scrollToPosition(0);
			} else {
				//File selected to download
				MegaNode n = nodes.get(position);
				hashes = new long[1];
				hashes[0] = n.getHandle();
				((FileProviderActivity) context).downloadAndAttachAfterClick(n.getSize(), hashes);
			}
		}
	}


	public int onBackPressed(){
		logDebug("deepBrowserTree: "+deepBrowserTree);
		deepBrowserTree = deepBrowserTree-1;
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setIncomingDeepBrowserTree(deepBrowserTree);
			logDebug("The browser tree change to: " + deepBrowserTree);
		}

		if(deepBrowserTree==0){
			parentHandle=-1;
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setIncParentHandle(parentHandle);
				logDebug("The parent handle change to: " + parentHandle);
			}
			changeActionBarTitle(getString(R.string.file_provider_title).toUpperCase());
			findNodes();
			
			setNodes(nodes);
			int lastVisiblePosition = 0;
			if(!lastPositionStack.empty()){
				lastVisiblePosition = lastPositionStack.pop();
				logDebug("Pop of the stack " + lastVisiblePosition + " position");
			}
			logDebug("Scroll to " + lastVisiblePosition + " position");

			if(lastVisiblePosition>=0){
				mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
			}
			adapter.setParentHandle(parentHandle);

			return 3;
		}
		else if (deepBrowserTree>0){
			parentHandle = adapter.getParentHandle();
			//((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);			
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setIncParentHandle(parentHandle);
				logDebug("The parent handle change to: " + parentHandle);
			}
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				

			if (parentNode != null){
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				changeActionBarTitle(parentNode.getName());
				
				parentHandle = parentNode.getHandle();
				if (context instanceof FileProviderActivity){
					((FileProviderActivity)context).setIncParentHandle(parentHandle);
					logDebug("The parent handle change to: " + parentHandle);
				}
				nodes = megaApi.getChildren(parentNode);

				setNodes(nodes);
				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					logDebug("Pop of the stack " + lastVisiblePosition + " position");
				}
				logDebug("Scroll to " + lastVisiblePosition + " position");

				if(lastVisiblePosition>=0){
						mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
				adapter.setParentHandle(parentHandle);
				return 2;
			}	
			return 2;
		}
		else{
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			deepBrowserTree=0;
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setIncomingDeepBrowserTree(deepBrowserTree);
				logDebug("The browser tree change to: " + deepBrowserTree);
			}
			return 0;
		}
	}
	
	public long getParentHandle(){
		return adapter.getParentHandle();
	}
	
	public void setParentHandle(long parentHandle){
		logDebug("setParentHandle");
		this.parentHandle = parentHandle;
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setIncParentHandle(parentHandle);
			logDebug("The parent handle change to: " + parentHandle);
		}
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		logDebug("setNodes");
		this.nodes = nodes;
		if (adapter != null){
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if (parentHandle==-1) {
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_incoming));
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
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
	}
	
	public RecyclerView getListView(){
		return listView;
	}

	public int getDeepBrowserTree() {
		return deepBrowserTree;
	}

	public void setDeepBrowserTree(int deepBrowserTree) {
		this.deepBrowserTree = deepBrowserTree;
	}

	public void hideMultipleSelect(){
		logDebug("hideMultipleSelect");
		adapter.setMultipleSelect(false);

		if (actionMode != null) {
			actionMode.finish();
		}
	}

	public void selectAll(){
		logDebug("selectAll");
		if(adapter != null){
			adapter.selectAll();
		}
		else {
			adapter.setMultipleSelect(true);
			adapter.selectAll();

			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	private void updateActionModeTitle() {
		logDebug("updateActionModeTitle");
		if (actionMode == null || getActivity() == null) {
			logDebug("RETURN: actionMode == null || getActivity() == null");
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

	}

	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
			((FileProviderActivity)context).activateButton(false);
		}
	}
}
