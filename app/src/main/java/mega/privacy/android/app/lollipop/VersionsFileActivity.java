package mega.privacy.android.app.lollipop;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.VersionsFileAdapter;
import mega.privacy.android.app.modalbottomsheet.VersionBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
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

import static mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet.isBottomSheetDialogShown;

public class VersionsFileActivity extends PinActivityLollipop implements MegaRequestListenerInterface, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, OnClickListener, MegaGlobalListenerInterface {

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	ActionBar aB;
	Toolbar tB;
	VersionsFileActivity versionsFileActivity = this;

	MegaNode selectedNode;

	int selectedPosition;

	RelativeLayout container;
	RecyclerView listView;
	LinearLayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	ImageView emptyImage;
	TextView emptyText;

	ArrayList<MegaNode> nodeVersions;

	MegaNode node;
	
	VersionsFileAdapter adapter;
	public String versionsSize = null;
	
	long parentHandle = -1;

	private ActionMode actionMode;
	
	ProgressDialog statusDialog;

	MegaPreferences prefs = null;
	
	MenuItem selectMenuItem;
	MenuItem unSelectMenuItem;
	MenuItem deleteVersionsMenuItem;

	Handler handler;
	DisplayMetrics outMetrics;

	private VersionBottomSheetDialogFragment bottomSheetDialogFragment;

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
			String size = Util.getSizeString(sizeNumber);
			log("doInBackground-AsyncTask GetVersionsSizeTask: "+size);
			return size;
		}

		@Override
		protected void onPostExecute(String size) {
			log("GetVersionsSizeTask::onPostExecute");
			updateSize(size);
		}
	}

	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

	    public void onLongPress(MotionEvent e) {
			log("onLongPress -- RecyclerViewOnGestureListener");

			View view = listView.findChildViewUnder(e.getX(), e.getY());
			int position = listView.getChildLayoutPosition(view);

			if (!adapter.isMultipleSelect()){

				if(position<0){
					log("Position not valid: "+position);
				}
				else{
					adapter.setMultipleSelect(true);

					actionMode = startSupportActionMode(new ActionBarCallBack());

					itemClick(position);
				}
			}

			super.onLongPress(e);
	    }
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			final List<MegaNode> nodes = adapter.getSelectedNodes();
						
			switch(item.getItemId()){
				case R.id.cab_menu_select_all:{
					selectAll();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					actionMode.invalidate();
					break;
				}
				case R.id.action_download_versions:{

					break;
				}
				case R.id.action_delete_versions:{
					showConfirmationRemoveVersions(nodes);
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			log("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.versions_files_action, menu);
			menu.findItem(R.id.cab_menu_select_all).setVisible(true);
			menu.findItem(R.id.action_download_versions).setVisible(false);
			menu.findItem(R.id.action_delete_versions).setVisible(false);
			Util.changeStatusBarColorActionMode(getApplicationContext(), getWindow(), handler, 1);
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			adapter.clearSelections();
			adapter.setMultipleSelect(false);
			Util.changeStatusBarColorActionMode(getApplicationContext(), getWindow(), handler, 3);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
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
				menu.findItem(R.id.action_download_versions).setVisible(false);
				menu.findItem(R.id.action_delete_versions).setVisible(true);
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
				menu.findItem(R.id.action_download_versions).setVisible(false);
				menu.findItem(R.id.action_delete_versions).setVisible(false);
			}
			
			return false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		if(megaApi==null||megaApi.getRootNode()==null){
			log("Refresh session - sdk");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		if(Util.isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
			}

			if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
				log("Refresh session - karere");
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return;
			}
		}
		
		megaApi.addGlobalListener(this);

		handler = new Handler();

		getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_search));
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		setContentView(R.layout.activity_versions_file);

		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_versions_file);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
//			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);
		aB.setTitle(getString(R.string.title_section_versions).toUpperCase());

		container = (RelativeLayout) findViewById(R.id.versions_main_layout);

		detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());

		listView = (RecyclerView) findViewById(R.id.recycler_view_versions_file);
		listView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
		listView.setClipToPadding(false);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnItemTouchListener(this);
		listView.setItemAnimator(new DefaultItemAnimator());
		listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				checkScroll();
			}
		});

		emptyImage = (ImageView) findViewById(R.id.versions_file_empty_image);
		emptyText = (TextView) findViewById(R.id.versions_file_empty_text);
		emptyImage.setImageResource(R.drawable.ic_empty_contacts);
		emptyText.setText(R.string.contacts_list_empty_text);

		long nodeHandle = -1;

		if (savedInstanceState != null){
			nodeHandle = savedInstanceState.getLong(Constants.EXTRA_NODE_HANDLE, -1);
		}

	    Bundle extras = getIntent().getExtras();
		if (extras != null){
			if(nodeHandle==-1){
				nodeHandle = extras.getLong("handle");
			}

			node=megaApi.getNodeByHandle(nodeHandle);

			if(node!=null){
				nodeVersions = megaApi.getVersions(node);

				GetVersionsSizeTask getVersionsSizeTask = new GetVersionsSizeTask();
				getVersionsSizeTask.execute();

				if (nodeVersions.size() != 0){
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
				}
				else{
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
				}

				if (adapter == null){

					adapter = new VersionsFileAdapter(this, nodeVersions, listView);
					listView.setAdapter(adapter);
				}
				else{
					adapter.setNodes(nodeVersions);
					//adapter.setParentHandle(-1);
				}

				adapter.setMultipleSelect(false);

				listView.setAdapter(adapter);
			}
			else{
				log("ERROR: node is NULL");
			}
		}
	}

	void checkScroll (){
		if (listView != null) {
			if ((listView.canScrollVertically(-1) && listView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect())) {
				changeActionBarElevation(true);
			}
			else if (adapter != null && !adapter.isMultipleSelect()) {
				changeActionBarElevation(false);
			}
		}
	}

	public void changeActionBarElevation(boolean whitElevation){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			if (whitElevation) {
				aB.setElevation(Util.px2dp(4, outMetrics));
			}
			else {
				aB.setElevation(0);
			}
		}
	}
	
	public void showOptionsPanel(MegaNode sNode, int sPosition){
		log("showOptionsPanel");
		if (node == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedNode = sNode;
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
		selectMenuItem.setVisible(true);
		unSelectMenuItem.setVisible(false);
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
		log("showDeleteVersionHistoryDialog");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						deleteVersionHistory();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.title_delete_version_history);
		builder.setMessage(R.string.text_delete_version_history).setPositiveButton(R.string.context_delete, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
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
		log("selectAll");
		if (adapter != null){
			if(adapter.isMultipleSelect()){
				adapter.selectAll();
			}
			else{						
				adapter.setMultipleSelect(true);
				adapter.selectAll();
				
				actionMode = startSupportActionMode(new ActionBarCallBack());
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
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_SHARE) {
			log("onRequestStart - Share");
		}
	}	

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
		log("onRequestFinish: " + request.getType());
		log("onRequestFinish: " + request.getRequestString());
		if(adapter!=null){
			if(adapter.isMultipleSelect()){
				adapter.clearSelections();
				hideMultipleSelect();
			}
		}

//		if (request.getType() == MegaRequest.TYPE_REMOVE) {
//			log(" MegaRequest.TYPE_REMOVE");
//			adapter.notifyDataSetChanged();
//		}
//		else if(request.getType() == MegaRequest.TYPE_RESTORE){
//			log(" MegaRequest.TYPE_RESTORE");
//			adapter.notifyDataSetChanged();
//		}
	}


	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}
	
	public static void log(String log) {
		Util.log("VersionsFileActivity", log);
	}

	public void itemClick(int position) {
		log("itemClick");
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
		log("updateActionModeTitle");
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
			log("oninvalidate error");
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
				i.setAction(Constants.ACTION_REFRESH_PARENTHANDLE_BROWSER);
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
		log("onUserupdate");

	}

	@Override
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		log("onUserAlertsUpdate");
	}

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		log("onNodesUpdate");

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
			log("exit onNodesUpdate - Not related to this node");
			return;
		}

		//Check if the parent handle has changed
		if(n!=null){
			if(n.hasChanged(MegaNode.CHANGE_TYPE_PARENT)){
				MegaNode oldParent = megaApi.getParentNode(node);
				MegaNode newParent = megaApi.getParentNode(n);
				if(oldParent.getHandle()==newParent.getHandle()){
					if(newParent.isFile()){
						log("New version added");
						node = newParent;
					}
					else{
						finish();
					}
				}
				else{
					node = n;
				}
				log("Node name: "+node.getName());
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
			log("After update - nodeVersions size: "+nodeVersions.size());

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

//		for(int i=0; i<nodes.size();i++){
//			MegaNode node = nodes.get(i);
//			if(node.hasChanged(MegaNode.CHANGE_TYPE_REMOVED)){
//				for(int j=0; i<node)
//			}
//		}

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

	public void revertVersion(){
		log("revertVersion");
		megaApi.restoreVersion(selectedNode, this);
	}

	public void removeVersion(){
		log("removeVersion");
		megaApi.removeVersion(selectedNode, this);
	}

	public void removeVersions(List<MegaNode> removeNodes){
		log("removeVersion");
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
		log("updateSize");
		this.versionsSize = size;
		adapter.notifyItemChanged(1);
	}

	public int getSelectedPosition() {
		return selectedPosition;
	}

	public void setSelectedPosition(int selectedPosition) {
		this.selectedPosition = selectedPosition;
	}

	public void showConfirmationRemoveVersion(){
		log("showConfirmationRemoveContact");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						removeVersion();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getQuantityString(R.plurals.title_dialog_delete_version, 1));
		builder.setMessage(getString(R.string.content_dialog_delete_version)).setPositiveButton(R.string.context_delete, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationRemoveVersions(final List<MegaNode> removeNodes){
		log("showConfirmationRemoveContactRequests");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						removeVersions(removeNodes);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		String message="";
		String title="";
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if(removeNodes.size()==1){

			title = getResources().getQuantityString(R.plurals.title_dialog_delete_version, 1);

			message= getResources().getString(R.string.content_dialog_delete_version);
		}else{
			title = getResources().getQuantityString(R.plurals.title_dialog_delete_version, removeNodes.size());
			message= getResources().getString(R.string.content_dialog_delete_multiple_version,removeNodes.size());
		}
		builder.setTitle(title);
		builder.setMessage(message).setPositiveButton(R.string.context_delete, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		log("onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putLong(Constants.EXTRA_NODE_HANDLE, node.getHandle());
	}
}

