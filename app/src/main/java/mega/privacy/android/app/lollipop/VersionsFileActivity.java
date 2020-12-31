package mega.privacy.android.app.lollipop;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.VersionsFileAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.modalbottomsheet.VersionBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;
import static nz.mega.sdk.MegaShare.*;

public class VersionsFileActivity extends PinActivityLollipop implements MegaRequestListenerInterface, OnClickListener, MegaGlobalListenerInterface {
	private static final String IS_CHECKING_REVERT_VERSION = "IS_CHECKING_REVERT_VERSION";
	private static final String SELECTED_NODE_HANDLE = "SELECTED_NODE_HANDLE";
	private static final String SELECTED_POSITION =  "SELECTED_POSITION";

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	ActionBar aB;
    MaterialToolbar tB;
	VersionsFileActivity versionsFileActivity = this;

	MegaNode selectedNode;
	private long selectedNodeHandle;

	int selectedPosition;

	RelativeLayout container;
	RecyclerView listView;
	LinearLayoutManager mLayoutManager;

	ArrayList<MegaNode> nodeVersions;

	MegaNode node;
	
	VersionsFileAdapter adapter;
	public String versionsSize = null;

	private ActionMode actionMode;
	
	MenuItem selectMenuItem;
	MenuItem unSelectMenuItem;
	MenuItem deleteVersionsMenuItem;

	Handler handler;
	DisplayMetrics outMetrics;

	int totalRemoveSelected = 0;
	int errorRemove = 0;
	int completedRemove = 0;

	private VersionBottomSheetDialogFragment bottomSheetDialogFragment;

	private int accessLevel;

	private boolean ischeckingRevertVersion;
	private class GetVersionsSizeTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			long sizeNumber = 0;
			if(nodeVersions!=null){
				for(int i=0; i<nodeVersions.size();i++){
					MegaNode node = nodeVersions.get(i);
					sizeNumber = sizeNumber + node.getSize();
				}
			}
			String size = getSizeString(sizeNumber);
			logDebug("doInBackground-AsyncTask GetVersionsSizeTask: " + size);
			return size;
		}

		@Override
		protected void onPostExecute(String size) {
			logDebug("GetVersionsSizeTask::onPostExecute");
			updateSize(size);
		}
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			logDebug("onActionItemClicked");
			final List<MegaNode> nodes = adapter.getSelectedNodes();
						
			switch(item.getItemId()){
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					break;
				}
				case R.id.action_download_versions:{
					if (nodes.size() == 1) {
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(nodes.get(0).getHandle());
						NodeController nC = new NodeController(versionsFileActivity);
						nC.prepareForDownload(handleList, false);
						clearSelections();
						actionMode.invalidate();
					}
					break;
				}
				case R.id.action_delete_versions:{
					showConfirmationRemoveVersions(nodes);
					break;
				}
				case R.id.action_revert_version:{
					if (nodes.size() == 1) {
						selectedNode = nodes.get(0);
						selectedNodeHandle = selectedNode.getHandle();
						checkRevertVersion();
						clearSelections();
						actionMode.invalidate();
					}
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			logDebug("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.versions_files_action, menu);
			menu.findItem(R.id.cab_menu_select_all).setVisible(true);
			menu.findItem(R.id.action_download_versions).setVisible(false);
			menu.findItem(R.id.action_delete_versions).setVisible(false);
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			adapter.clearSelections();
			adapter.setMultipleSelect(false);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			logDebug("onPrepareActionMode");
			List<MegaNode> selected = adapter.getSelectedNodes();

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

				if (selected.size() == 1) {
					if (getSelectedPosition() == 0) {
						menu.findItem(R.id.action_revert_version).setVisible(false);
					} else {
						menu.findItem(R.id.action_revert_version).setVisible(true);
					}
					menu.findItem(R.id.action_download_versions).setVisible(true);
				}
				else {
					menu.findItem(R.id.action_revert_version).setVisible(false);
					menu.findItem(R.id.action_download_versions).setVisible(false);
				}

				menu.findItem(R.id.action_delete_versions).setVisible(true);
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
				menu.findItem(R.id.action_download_versions).setVisible(false);
				menu.findItem(R.id.action_delete_versions).setVisible(false);
				menu.findItem(R.id.action_revert_version).setVisible(false);
			}
			
			return false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate");
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		if(megaApi==null||megaApi.getRootNode()==null){
			logDebug("Refresh session - sdk");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		if (megaChatApi == null) {
			megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
		}

		if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
			logDebug("Refresh session - karere");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		
		megaApi.addGlobalListener(this);

		handler = new Handler();
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		setContentView(R.layout.activity_versions_file);

		//Set toolbar
		tB = findViewById(R.id.toolbar_versions_file);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
//			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);
		aB.setTitle(getString(R.string.title_section_versions).toUpperCase());

		container = (RelativeLayout) findViewById(R.id.versions_main_layout);

		listView = (RecyclerView) findViewById(R.id.recycler_view_versions_file);
		listView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
		listView.setClipToPadding(false);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);
		listView.setItemAnimator(new DefaultItemAnimator());
		listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				checkScroll();
			}
		});

		long nodeHandle = INVALID_HANDLE;

		if (savedInstanceState != null){
			nodeHandle = savedInstanceState.getLong(EXTRA_NODE_HANDLE, INVALID_HANDLE);
			ischeckingRevertVersion = savedInstanceState.getBoolean(IS_CHECKING_REVERT_VERSION, false);
			selectedNodeHandle = savedInstanceState.getLong(SELECTED_NODE_HANDLE, INVALID_HANDLE);
			selectedPosition = savedInstanceState.getInt(SELECTED_POSITION);
		}

	    Bundle extras = getIntent().getExtras();
		if (extras != null){
			if(nodeHandle == INVALID_HANDLE){
				nodeHandle = extras.getLong("handle");
			}

			node=megaApi.getNodeByHandle(nodeHandle);

			if(node!=null){
				accessLevel = megaApi.getAccess(node);
				nodeVersions = megaApi.getVersions(node);

				GetVersionsSizeTask getVersionsSizeTask = new GetVersionsSizeTask();
				getVersionsSizeTask.execute();

				listView.setVisibility(View.VISIBLE);

				if (adapter == null){

					adapter = new VersionsFileAdapter(this, nodeVersions, listView);
					listView.setAdapter(adapter);
				}
				else{
					adapter.setNodes(nodeVersions);
				}

				adapter.setMultipleSelect(false);

				listView.setAdapter(adapter);
			}
			else{
				logError("ERROR: node is NULL");
			}
		}

		if (ischeckingRevertVersion) {
			selectedNode = megaApi.getNodeByHandle(selectedNodeHandle);
			if (selectedNode != null) {
				checkRevertVersion();
			}
		}
	}

	void checkScroll (){
		if (listView != null) {
			changeViewElevation(aB, (listView.canScrollVertically(-1) && listView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect()), outMetrics);
		}
	}
	
	public void showOptionsPanel(MegaNode sNode, int sPosition){
		logDebug("showOptionsPanel");
		if (node == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedNode = sNode;
		selectedNodeHandle = selectedNode.getHandle();
		selectedPosition = sPosition;
		bottomSheetDialogFragment = new VersionBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	if(megaApi != null)
    	{
    		megaApi.removeGlobalListener(this);
    		megaApi.removeRequestListener(this);
    	}
    	handler.removeCallbacksAndMessages(null);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_folder_contact_list, menu);

	    selectMenuItem = menu.findItem(R.id.action_select);
		unSelectMenuItem = menu.findItem(R.id.action_unselect);
		deleteVersionsMenuItem = menu.findItem(R.id.action_delete_version_history);

		menu.findItem(R.id.action_folder_contacts_list_share_folder).setVisible(false);

	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		switch (accessLevel) {
			case ACCESS_FULL:
			case ACCESS_OWNER:
				selectMenuItem.setVisible(true);
				unSelectMenuItem.setVisible(false);
				deleteVersionsMenuItem.setVisible(true);
				break;

			default:
				selectMenuItem.setVisible(false);
				unSelectMenuItem.setVisible(false);
				deleteVersionsMenuItem.setVisible(false);

		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
	    switch (item.getItemId()) {
		    case android.R.id.home:{
		    	onBackPressed();
		    	return true;
		    }
		    case R.id.action_select:{
		    	
		    	selectAll();
		    	return true;
		    }
			case R.id.action_delete_version_history: {
				showDeleteVersionHistoryDialog();
				return true;
			}
		    default:{
	            return super.onOptionsItemSelected(item);
	        }
	    }
	}

	void showDeleteVersionHistoryDialog () {
		logDebug("showDeleteVersionHistoryDialog");
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
		builder.setTitle(R.string.title_delete_version_history)
				.setMessage(R.string.text_delete_version_history)
				.setPositiveButton(R.string.context_delete, (dialog, which) -> deleteVersionHistory())
				.setNegativeButton(R.string.general_cancel, (dialog, which) -> {})
				.show();
	}

	void deleteVersionHistory () {
		Intent intent = new Intent();
		intent.putExtra("deleteVersionHistory", true);
		setResult(RESULT_OK, intent);
		finish();
	}

	// Clear all selected items
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
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
				
				actionMode = startSupportActionMode(new ActionBarCallBack());
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
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_SHARE) {
			logDebug("onRequestStart - Share");
		}
	}	

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
		logDebug("onRequestFinish: " + request.getType());
		logDebug("onRequestFinish: " + request.getRequestString());
		if(adapter!=null && adapter.isMultipleSelect()){
			adapter.clearSelections();
			hideMultipleSelect();
		}

		if (request.getType() == MegaRequest.TYPE_REMOVE) {
			logDebug("MegaRequest.TYPE_REMOVE");
			totalRemoveSelected --;
			if (e.getErrorCode() == MegaError.API_OK) {
				completedRemove++;
				checkScroll();
			}
			else {
				errorRemove++;
			}

			if (totalRemoveSelected == 0) {
				if (completedRemove > 0 && errorRemove == 0) {
					showSnackbar(getResources().getQuantityString(R.plurals.versions_deleted_succesfully, completedRemove, completedRemove));
				}
				else if (completedRemove > 0 && errorRemove > 0) {
					showSnackbar(getResources().getQuantityString(R.plurals.versions_deleted_succesfully, completedRemove, completedRemove) +"\n"
							+ getResources().getQuantityString(R.plurals.versions_not_deleted, errorRemove, errorRemove));
				}
				else {
					showSnackbar(getResources().getQuantityString(R.plurals.versions_not_deleted, errorRemove, errorRemove));
				}
			}
		}
		else if(request.getType() == MegaRequest.TYPE_RESTORE){
			logDebug("MegaRequest.TYPE_RESTORE");
			if (e.getErrorCode() == MegaError.API_OK) {
				if (getAccessLevel() <= ACCESS_READWRITE) {
					showSnackbar(getString(R.string.version_as_new_file_created));
				} else {
					showSnackbar(getString(R.string.version_restored));
				}
			}
			else {
				showSnackbar(getString(R.string.general_text_error));
			}
		}
	}


	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError");
	}

	public void itemClick(int position) {
		logDebug("Position: " + position);
		if (adapter.isMultipleSelect()){
			adapter.toggleSelection(position);
			updateActionModeTitle();
		}
		else{
			MegaNode n = nodeVersions.get(position);
			showOptionsPanel(n, position);
		}
	}
	
	private void updateActionModeTitle() {
		logDebug("updateActionModeTitle");
		if (actionMode == null) {
			return;
		}
		List<MegaNode> nodes = adapter.getSelectedNodes();

		Resources res = getResources();
		String format = "%d %s";
	
		actionMode.setTitle(String.format(format, nodes.size(),res.getQuantityString(R.plurals.general_num_files, nodes.size())));
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			logError("Invalidate error", e);
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
	
	@Override
	public void onClick(View v) {
		switch (v.getId()){		
			case R.id.file_contact_list_layout:{
				Intent i = new Intent(this, ManagerActivityLollipop.class);
				i.setAction(ACTION_REFRESH_PARENTHANDLE_BROWSER);
				i.putExtra("parentHandle", node.getHandle());
				startActivity(i);
				finish();
				break;
			}
		}
	}

	public void notifyDataSetChanged(){		
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}		
	}
	
	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		logDebug("onUserupdate");

	}

	@Override
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		logDebug("onUserAlertsUpdate");
	}

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		logDebug("onNodesUpdate");

		boolean thisNode = false;
		boolean anyChild = false;
		if(nodes==null){
			return;
		}
		MegaNode n = null;
		Iterator<MegaNode> it = nodes.iterator();
		while (it.hasNext()){
			MegaNode nodeToCheck = it.next();
			if (nodeToCheck != null){
				if (nodeToCheck.getHandle() == node.getHandle()){
					thisNode = true;
					n = nodeToCheck;
				}
				else{
					for(int j=0; j<nodeVersions.size();j++){
						if(nodeToCheck.getHandle()==nodeVersions.get(j).getHandle()){
							if(anyChild==false){
								anyChild = true;
							}
						}
					}
				}
			}
		}

		if ((!thisNode)&&(!anyChild)){
			logWarning("Exit - Not related to this node");
			return;
		}

		//Check if the parent handle has changed
		if(n!=null){
			if(n.hasChanged(MegaNode.CHANGE_TYPE_PARENT)){
				MegaNode oldParent = megaApi.getParentNode(node);
				MegaNode newParent = megaApi.getParentNode(n);
				if(oldParent.getHandle()==newParent.getHandle()){
					if(newParent.isFile()){
						logDebug("New version added");
						node = newParent;
					}
					else{
						finish();
					}
				}
				else{
					node = n;
				}
				logDebug("Node name: " + node.getName());
				if(megaApi.hasVersions(node)){
					nodeVersions = megaApi.getVersions(node);
				}
				else{
					nodeVersions = null;
				}
			}
			else if(n.hasChanged(MegaNode.CHANGE_TYPE_REMOVED)){
				if(thisNode){
					if(nodeVersions!=null){
						node = nodeVersions.get(1);
						if(megaApi.hasVersions(node)){
							nodeVersions = megaApi.getVersions(node);
						}
						else{
							nodeVersions = null;
						}
					}
					else{
						finish();
					}
				}
				else if(anyChild){
					if(megaApi.hasVersions(n)){
						nodeVersions = megaApi.getVersions(n);
					}
					else{
						nodeVersions = null;
					}
				}

			}
			else{
				node = n;
				if(megaApi.hasVersions(node)){
					nodeVersions = megaApi.getVersions(node);
				}
				else{
					nodeVersions = null;
				}
			}
		}
		else{
			if(anyChild){
				if(megaApi.hasVersions(node)){
					nodeVersions = megaApi.getVersions(node);
				}
				else{
					nodeVersions = null;
				}

			}
		}

		if(nodeVersions == null ||nodeVersions.size()==1){
			finish();
		}
		else{
			logDebug("After update - nodeVersions size: " + nodeVersions.size());

			if(adapter!=null){
				adapter.setNodes(nodeVersions);
				adapter.notifyDataSetChanged();
			}
			else{
				adapter = new VersionsFileAdapter(this, nodeVersions, listView);
				listView.setAdapter(adapter);
			}

			GetVersionsSizeTask getVersionsSizeTask = new GetVersionsSizeTask();
			getVersionsSizeTask.execute();
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api,
			ArrayList<MegaContactRequest> requests) {
		// TODO Auto-generated method stub
		
	}

	public void checkRevertVersion() {
		if (getAccessLevel() <= ACCESS_READWRITE) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
			builder.setCancelable(false)
					.setTitle(R.string.permissions_error_label)
					.setMessage(R.string.alert_not_enough_permissions_revert)
					.setPositiveButton(R.string.create_new_file_action, (dialog, which) -> revertVersion())
					.setNegativeButton(R.string.general_cancel, ((dialog, which) -> ischeckingRevertVersion = false))
					.show();
			ischeckingRevertVersion = true;
		} else {
			revertVersion();
		}
	}

	private void revertVersion(){
		logDebug("revertVersion");
		megaApi.restoreVersion(selectedNode, this);
	}

	public void removeVersion(){
		logDebug("removeVersion");
		megaApi.removeVersion(selectedNode, this);
	}

	public void removeVersions(List<MegaNode> removeNodes){
		logDebug("removeVersion");
		totalRemoveSelected = removeNodes.size();
		errorRemove = 0;
		completedRemove = 0;

		for(int i=0; i<removeNodes.size();i++){
			megaApi.removeVersion(removeNodes.get(i), this);
		}
	}

	public MegaNode getSelectedNode() {
		return selectedNode;
	}

	public void showSnackbar(String s){
		showSnackbar(container, s);
	}

	public void updateSize(String size){
		logDebug("Size: " + size);
		this.versionsSize = size;
		adapter.notifyItemChanged(1);
	}

	public int getSelectedPosition() {
		return selectedPosition;
	}

	public void setSelectedPosition(int selectedPosition) {
		this.selectedPosition = selectedPosition;
	}

	public void showConfirmationRemoveVersion() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
		builder.setTitle(getResources().getQuantityString(R.plurals.title_dialog_delete_version, 1))
				.setMessage(getString(R.string.content_dialog_delete_version))
				.setPositiveButton(R.string.context_delete, (dialog, which) -> removeVersion())
				.setNegativeButton(R.string.general_cancel, (dialog, which) -> {
				})
				.show();
	}

	public void showConfirmationRemoveVersions(final List<MegaNode> removeNodes) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
		String message;
		String title;

		if (removeNodes.size() == 1) {
			title = getResources().getQuantityString(R.plurals.title_dialog_delete_version, 1);
			message = getResources().getString(R.string.content_dialog_delete_version);
		} else {
			title = getResources().getQuantityString(R.plurals.title_dialog_delete_version, removeNodes.size());
			message = getResources().getString(R.string.content_dialog_delete_multiple_version, removeNodes.size());
		}

		builder.setTitle(title)
				.setMessage(message)
				.setPositiveButton(R.string.context_delete, (dialog, which) -> removeVersions(removeNodes))
				.setNegativeButton(R.string.general_cancel, (dialog, which) -> {})
				.show();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		logDebug("onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putLong(EXTRA_NODE_HANDLE, node.getHandle());
		outState.putBoolean(IS_CHECKING_REVERT_VERSION, ischeckingRevertVersion);
		outState.putLong(SELECTED_NODE_HANDLE, selectedNodeHandle);
		outState.putInt(SELECTED_POSITION, selectedPosition);
	}

	public int getAccessLevel() {
		return accessLevel;
	}

	public void startActionMode(int position) {
		actionMode = startSupportActionMode(new ActionBarCallBack());
		itemClick(position);
	}
}

