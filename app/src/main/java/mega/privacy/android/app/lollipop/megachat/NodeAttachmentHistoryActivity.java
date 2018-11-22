package mega.privacy.android.app.lollipop.megachat;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.PositionDividerItemDecoration;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.NodeAttachmentHistoryAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatNodeHistoryListenerInterface;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;

public class NodeAttachmentHistoryActivity extends PinActivityLollipop implements MegaChatRequestListenerInterface, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, OnClickListener, MegaChatListenerInterface, MegaChatNodeHistoryListenerInterface {

	public static int NUMBER_MESSAGES_TO_LOAD = 20;
	public static int NUMBER_MESSAGES_BEFORE_LOAD = 8;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	ActionBar aB;
	Toolbar tB;
	NodeAttachmentHistoryActivity nodeAttachmentHistoryActivity = this;

	MegaChatMessage selectedMessage;

	int selectedPosition;

	RelativeLayout container;
	RecyclerView listView;
	LinearLayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	ImageView emptyImage;
	TextView emptyText;

	ArrayList<MegaChatMessage> messages;

	MegaChatRoom chatRoom;
	
	NodeAttachmentHistoryAdapter adapter;

	private ActionMode actionMode;
	
	ProgressDialog statusDialog;

	MenuItem selectMenuItem;
	MenuItem unSelectMenuItem;

	Handler handler;
	int stateHistory;
	long chatId = -1;

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

		if (megaChatApi == null){
			megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
		}

		if(megaChatApi==null||megaChatApi.getInitState()==MegaChatApi.INIT_ERROR||megaChatApi.getInitState()==0){
			log("Refresh session - karere");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		log("addChatListener");
		megaChatApi.addChatListener(this);
		megaChatApi.addNodeHistoryListener(chatId,this);

		handler = new Handler();
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
		}

		setContentView(R.layout.activity_versions_file);

		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_versions_file);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
//			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);
		aB.setTitle(getString(R.string.title_section_versions));

		container = (RelativeLayout) findViewById(R.id.versions_main_layout);

		detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());

		listView = (RecyclerView) findViewById(R.id.recycler_view_versions_file);
		listView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
		listView.setClipToPadding(false);
		listView.addItemDecoration(new PositionDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnItemTouchListener(this);
		listView.setItemAnimator(new DefaultItemAnimator());

		emptyImage = (ImageView) findViewById(R.id.versions_file_empty_image);
		emptyText = (TextView) findViewById(R.id.versions_file_empty_text);
		emptyImage.setImageResource(R.drawable.ic_empty_contacts);
		emptyText.setText(R.string.contacts_list_empty_text);

		if (savedInstanceState != null){
			chatId = savedInstanceState.getLong("chatId", -1);
		}

	    Bundle extras = getIntent().getExtras();
		if (extras != null){
			if(chatId==-1){
				chatId = extras.getLong("chatId");
			}

			chatRoom=megaChatApi.getChatRoom(chatId);

			if(chatRoom!=null){
				messages = new ArrayList<>();

				if (messages.size() != 0){
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
				}
				else{
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
				}

				boolean result = megaChatApi.openNodeHistory(chatId, this);
				if(result){
					log("Node history opened correctly");

					messages = new ArrayList<MegaChatMessage>();

					if (adapter == null){
						adapter = new NodeAttachmentHistoryAdapter(this, messages, listView, NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST);
					}

					listView.setAdapter(adapter);
					adapter.setMultipleSelect(false);

					stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD);
				}
			}
			else{
				log("ERROR: node is NULL");
			}

			((MegaApplication) getApplication()).sendSignalPresenceActivity();
		}
	}
	
	public void showOptionsPanel(MegaChatMessage sMessage, int sPosition){
		log("showOptionsPanel");
		if(sMessage!=null){
			this.selectedMessage = sMessage;
			this.selectedPosition = sPosition;
			//VersionBottomSheetDialogFragment bottomSheetDialogFragment = new VersionBottomSheetDialogFragment();
			//bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	@Override
    protected void onDestroy(){
		log("onDestroy");
    	super.onDestroy();

		megaChatApi.removeChatListener(this);
		megaChatApi.removeNodeHistoryListener(chatId,this);
		megaChatApi.closeNodeHistory(chatId, null);
    	handler.removeCallbacksAndMessages(null);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_folder_contact_list, menu);

	    selectMenuItem = menu.findItem(R.id.action_select);
		unSelectMenuItem = menu.findItem(R.id.action_unselect);

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
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
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
		    default:{
	            return super.onOptionsItemSelected(item);
	        }
	    }
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

	public static void log(String log) {
		Util.log("VersionsFileActivity", log);
	}

	public void itemClick(int position) {
		log("itemClick");
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
		if (adapter.isMultipleSelect()){
			adapter.toggleSelection(position);
			updateActionModeTitle();
		}
		else{
			MegaChatMessage m = messages.get(position);
			//showOptionsPanel(m, position);
		}
	}

	@Override
	public void onBackPressed() {
		log("onBackPressed");
		super.onBackPressed();
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}
	
	private void updateActionModeTitle() {
		log("updateActionModeTitle");
		if (actionMode == null) {
			return;
		}
		List<MegaChatMessage> msgs = adapter.getSelectedMessages();

		Resources res = getResources();
		String format = "%d %s";
	
		actionMode.setTitle(String.format(format, msgs.size(),res.getQuantityString(R.plurals.general_num_files, msgs.size())));
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
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
		switch (v.getId()){		
			case R.id.file_contact_list_layout:{
				Intent i = new Intent(this, ManagerActivityLollipop.class);
				i.setAction(Constants.ACTION_REFRESH_PARENTHANDLE_BROWSER);
				//i.putExtra("parentHandle", node.getHandle());
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
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
		detector.onTouchEvent(e);
		return false;
	}

	@Override
	public void onRequestDisallowInterceptTouchEvent(boolean arg0) {
	}

	@Override
	public void onTouchEvent(RecyclerView arg0, MotionEvent arg1) {
	}

	public MegaChatMessage getSelectedMessage() {
		return selectedMessage;
	}

	public void showSnackbar(String s){
		log("showSnackbar");
		Snackbar snackbar = Snackbar.make(container, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
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
		outState.putLong("chatId", chatRoom.getChatId());
	}

	//Multiselection

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
			final List<MegaChatMessage> nodes = adapter.getSelectedMessages();

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
					//showConfirmationRemoveVersions(nodes);
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
			Util.changeStatusBarColorActionMode(getApplicationContext(), getWindow(), handler, 0);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaChatMessage> selected = adapter.getSelectedMessages();

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
	public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

	}

	@Override
	public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

	}

	@Override
	public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {

	}

	@Override
	public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {

	}

	@Override
	public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

	}

	@Override
	public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {

	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	@Override
	public void onAttachmentLoaded(MegaChatApiJava api, MegaChatMessage msg) {
		log("onAttachmentLoaded");
	}

	@Override
	public void onAttachmentReceived(MegaChatApiJava api, MegaChatMessage msg) {
		log("onAttachmentReceived");
	}

	@Override
	public void onAttachmentDeleted(MegaChatApiJava api, long msgid) {
		log("onAttachmentDeleted");
	}

	@Override
	public void onTruncate(MegaChatApiJava api, long msgid) {
		log("onTruncate");
	}
}

