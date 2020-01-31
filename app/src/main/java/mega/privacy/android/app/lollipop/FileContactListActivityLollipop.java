package mega.privacy.android.app.lollipop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.listeners.ShareListener;
import mega.privacy.android.app.lollipop.adapters.MegaSharedFolderLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.listeners.FileContactMultipleRequestListener;
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.listeners.ShareListener.*;
import static mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet.*;
import static mega.privacy.android.app.utils.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ProgressDialogUtil.getProgressDialog;
import static mega.privacy.android.app.utils.Util.*;

public class FileContactListActivityLollipop extends PinActivityLollipop implements OnClickListener, MegaGlobalListenerInterface {

	private ContactController cC;
	private NodeController nC;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	ActionBar aB;
	Toolbar tB;
	FileContactListActivityLollipop fileContactListActivityLollipop = this;
	MegaShare selectedShare;
	
	TextView nameView;
	ImageView imageView;
	TextView createdView;

	CoordinatorLayout coordinatorLayout;
	RelativeLayout container;
	RelativeLayout contactLayout;
	RecyclerView listView;
	LinearLayoutManager mLayoutManager;
	ImageView emptyImage;
	TextView emptyText;
	FloatingActionButton fab;
	
	ArrayList<MegaShare> listContacts;
	ArrayList<MegaShare> tempListContacts;
	
//	ArrayList<MegaUser> listContactsArray = new ArrayList<MegaUser>();
	
	long nodeHandle;
	MegaNode node;
	ArrayList<MegaNode> contactNodes;
	
	MegaSharedFolderLollipopAdapter adapter;
	
	long parentHandle = -1;
	
	Stack<Long> parentHandleStack = new Stack<Long>();
	
	private ActionMode actionMode;
	
	boolean removeShare = false;
	boolean changeShare = false;
	
	ProgressDialog statusDialog;
	AlertDialog permissionsDialog;

	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	private List<ShareInfo> filePreparedInfos;

	MegaPreferences prefs = null;
	
	MenuItem addSharingContact;
	MenuItem selectMenuItem;
	MenuItem unSelectMenuItem;

	Handler handler;
	DisplayMetrics outMetrics;

	private AlertDialog.Builder dialogBuilder;
	private FileContactMultipleRequestListener.RequestCompletedCallback requestCompletedCallback;

	private FileContactsListBottomSheetDialogFragment bottomSheetDialogFragment;

	private BroadcastReceiver manageShareReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null) return;

			switch (intent.getStringExtra(TYPE_SHARE)) {
				case SHARE_LISTENER:
				case CHANGE_PERMISSIONS_LISTENER:
					if (adapter != null) {
						if(adapter.isMultipleSelect()){
							adapter.clearSelections();
							hideMultipleSelect();
						}
						adapter.setShareList(listContacts);
					}
					break;
			}

			if (permissionsDialog != null) {
				permissionsDialog.dismiss();
			}

			if (statusDialog != null) {
				statusDialog.dismiss();
			}
		}
	};

	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = startSupportActionMode(new ActionBarCallBack());
		}
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			logDebug("onActionItemClicked");
			final ArrayList<MegaShare> shares = adapter.getSelectedShares();
			
			switch(item.getItemId()){
				case R.id.action_file_contact_list_permissions:{
                    //Change permissions
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
                    
                    final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                    dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                        	clearSelections();
                            if(permissionsDialog != null){
                                permissionsDialog.dismiss();
                            }
                            removeShare = false;
                            changeShare = true;

                            statusDialog = getProgressDialog(fileContactListActivityLollipop, getString(R.string.context_permissions_changing_folder));
                            cC.changePermissions(cC.getEmailShares(shares), item, node);
                        }
                    });
                    
                    permissionsDialog = dialogBuilder.create();
                    permissionsDialog.show();
                    break;
				}
				case R.id.action_file_contact_list_delete:{
	
					removeShare = true;
					changeShare = false;
	
					if(shares!=null){

						if(shares.size()!=0){

							if (shares.size() > 1) {
								logDebug("Remove multiple contacts");
								showConfirmationRemoveMultipleContactFromShare(shares);
							} else {
								logDebug("Remove one contact");
								showConfirmationRemoveContactFromShare(shares.get(0).getUser());
							}
						}
					}
					break;
				}
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
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			logDebug("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_contact_shared_browser_action, menu);
			fab.setVisibility(View.GONE);
			changeStatusBarColorActionMode(fileContactListActivityLollipop, getWindow(), handler, 1);
			checkScroll();
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			adapter.clearSelections();
			adapter.setMultipleSelect(false);
			fab.setVisibility(View.VISIBLE);
			changeStatusBarColorActionMode(fileContactListActivityLollipop, getWindow(), handler, 3);
			checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			logDebug("onPrepareActionMode");
			ArrayList<MegaShare> selected = adapter.getSelectedShares();
			boolean deleteShare = false;
			boolean permissions = false;
			
			if (selected.size() != 0) {
				permissions = true;
				deleteShare = true;

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
			
			menu.findItem(R.id.action_file_contact_list_permissions).setVisible(permissions);
			if(permissions == true){
				menu.findItem(R.id.action_file_contact_list_permissions).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}else{
				menu.findItem(R.id.action_file_contact_list_permissions).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

			}

			menu.findItem(R.id.action_file_contact_list_delete).setVisible(deleteShare);
			if(deleteShare == true){
				menu.findItem(R.id.action_file_contact_list_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}else{
				menu.findItem(R.id.action_file_contact_list_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

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
			intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		if(isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
			}

			if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
				logDebug("Refresh session - karere");
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return;
			}
		}

		requestCompletedCallback = new FileContactMultipleRequestListener.RequestCompletedCallback() {
			@Override
			public void onRequestCompleted(String message) {
				clearSelections();
				hideMultipleSelect();
				showSnackbar(message);
			}
		};

		dialogBuilder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleAddContacts);

		megaApi.addGlobalListener(this);

		handler = new Handler();

		getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_search));
		
		listContacts = new ArrayList<MegaShare>();
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
	    
	    Bundle extras = getIntent().getExtras();
		if (extras != null){
			nodeHandle = extras.getLong("name");
			node=megaApi.getNodeByHandle(nodeHandle);
			
			setContentView(R.layout.activity_file_contact_list);
			
			//Set toolbar
			tB = (Toolbar) findViewById(R.id.toolbar_file_contact_list);
			setSupportActionBar(tB);
			aB = getSupportActionBar();
			aB.setDisplayHomeAsUpEnabled(true);
			aB.setDisplayShowHomeEnabled(true);
			aB.setTitle(node.getName().toUpperCase());
			aB.setSubtitle(R.string.file_properties_shared_folder_select_contact);

			coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout_file_contact_list);
			container = (RelativeLayout) findViewById(R.id.file_contact_list);
			imageView = (ImageView) findViewById(R.id.file_properties_icon);
			nameView = (TextView) findViewById(R.id.node_name);
			createdView = (TextView) findViewById(R.id.node_last_update);
			contactLayout = (RelativeLayout) findViewById(R.id.file_contact_list_layout);
			contactLayout.setVisibility(View.GONE);
			findViewById(R.id.separator_file_contact_list).setVisibility(View.GONE);
//			contactLayout.setOnClickListener(this);

			fab = (FloatingActionButton) findViewById(R.id.floating_button_file_contact_list);
			fab.setOnClickListener(this);

			nameView.setText(node.getName());
			
			imageView.setImageResource(R.drawable.ic_folder_outgoing_list);
			
			tempListContacts = megaApi.getOutShares(node);
			if(tempListContacts!=null && !tempListContacts.isEmpty()){
				listContacts.addAll(megaApi.getOutShares(node));
			}
			
			listView = (RecyclerView) findViewById(R.id.file_contact_list_view_browser);
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
			
			emptyImage = (ImageView) findViewById(R.id.file_contact_list_empty_image);
			emptyText = (TextView) findViewById(R.id.file_contact_list_empty_text);
			emptyImage.setImageResource(R.drawable.ic_empty_contacts);
			emptyText.setText(R.string.contacts_list_empty_text);

			if (listContacts.size() != 0){
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
			}
			else{
				emptyImage.setVisibility(View.VISIBLE);
				emptyText.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);

			}
			
			if (node.getCreationTime() != 0){
				try {createdView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));
				}catch(Exception ex){
					createdView.setText("");
				}
				
			}
			else{
				createdView.setText("");
			}
			
			if (adapter == null){
				
				adapter = new MegaSharedFolderLollipopAdapter(this, node, listContacts, listView);
				listView.setAdapter(adapter);
				adapter.setShareList(listContacts);
			}
			else{
				adapter.setShareList(listContacts);
				//adapter.setParentHandle(-1);
			}
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			listView.setAdapter(adapter);
		}

		cC = new ContactController(this);
		nC = new NodeController(this);

		LocalBroadcastManager.getInstance(this).registerReceiver(manageShareReceiver,
				new IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE));
	}

	public void checkScroll() {
		if (listView != null) {
			if ((listView.canScrollVertically(-1) && listView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect())) {
				changeActionBarElevation(true);
			}
			else if ((adapter != null && !adapter.isMultipleSelect())) {
				changeActionBarElevation(false);
			}
		}
	}

	public void changeActionBarElevation(boolean whitElevation){
		if (whitElevation) {
			aB.setElevation(px2dp(4, outMetrics));
		}
		else {
			aB.setElevation(0);
		}
	}
	
	
	public void showOptionsPanel(MegaShare sShare){
		logDebug("showNodeOptionsPanel");

		if (node == null || sShare == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedShare = sShare;
		bottomSheetDialogFragment = new FileContactsListBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	if(megaApi != null)
    	{
    		megaApi.removeGlobalListener(this);
    	}
    	handler.removeCallbacksAndMessages(null);

		LocalBroadcastManager.getInstance(this).unregisterReceiver(manageShareReceiver);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_folder_contact_list, menu);

	    menu.findItem(R.id.action_delete_version_history).setVisible(false);
	    
	    selectMenuItem = menu.findItem(R.id.action_select);
		unSelectMenuItem = menu.findItem(R.id.action_unselect);
		
		selectMenuItem.setVisible(true);
		unSelectMenuItem.setVisible(false);

		addSharingContact = menu.findItem(R.id.action_folder_contacts_list_share_folder);
		addSharingContact.setIcon(mutateIconSecondary(this, R.drawable.ic_share_white, R.color.black));
		addSharingContact.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	    
	    return super.onCreateOptionsMenu(menu);
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
			case R.id.action_folder_contacts_list_share_folder: {
				shareOption();
				return true;
			}
		    default:{
	            return super.onOptionsItemSelected(item);
	        }
	    }
	}

	void shareOption() {
		removeShare = false;
		changeShare = false;

		Intent intent = new Intent();
		intent.setClass(this, AddContactActivityLollipop.class);
		intent.putExtra("contactType", CONTACT_TYPE_BOTH);
		intent.putExtra("MULTISELECT", 0);
		intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
		startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
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
			updateActionModeTitle();
		}
	}
	
	public boolean showSelectMenuItem(){
		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}

	public void itemClick(int position) {
		logDebug("Position: " + position);

		if (adapter.isMultipleSelect()){
			adapter.toggleSelection(position);
			updateActionModeTitle();
		}
		else{
			MegaUser contact = megaApi.getContact(listContacts.get(position).getUser());

			if(contact!=null && contact.getVisibility()==MegaUser.VISIBILITY_VISIBLE){
				Intent i = new Intent(this, ContactInfoActivityLollipop.class);
				i.putExtra("name", listContacts.get(position).getUser());
				startActivity(i);
			}

		}
	}

	@Override
	public void onBackPressed() {
		logDebug("onBackPressed");
		retryConnectionsAndSignalPresence();

		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
		}
		else{
			if (parentHandleStack.isEmpty()){
				super.onBackPressed();
			}
			else{
				parentHandle = parentHandleStack.pop();
				listView.setVisibility(View.VISIBLE);
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				if (parentHandle == -1){
					aB.setTitle(getString(R.string.file_properties_shared_folder_select_contact));
					aB.setLogo(R.drawable.ic_action_navigation_accept_white);
					supportInvalidateOptionsMenu();
					adapter.setShareList(listContacts);
					listView.scrollToPosition(0);
				}
				else{
					contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
					aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
					aB.setLogo(R.drawable.ic_action_navigation_previous_item);
					supportInvalidateOptionsMenu();
					adapter.setShareList(listContacts);
					listView.scrollToPosition(0);
				}
			}
		}
	}
	
	private void updateActionModeTitle() {
		logDebug("updateActionModeTitle");
		if (actionMode == null) {
			return;
		}
		ArrayList<MegaShare> contacts = adapter.getSelectedShares();
		if(contacts!=null){
			logDebug("Contacts selected: " + contacts.size());
		}

		Resources res = getResources();
		String format = "%d %s";
	
		actionMode.setTitle(String.format(format, contacts.size(),res.getQuantityString(R.plurals.general_num_contacts, contacts.size())));
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
			case R.id.floating_button_file_contact_list:{
				shareOption();
				break;
			}
		}
	}

	public void removeFileContactShare(){
		notifyDataSetChanged();

		showConfirmationRemoveContactFromShare(selectedShare.getUser());
	}

	public void changePermissions(){
		logDebug("changePermissions");
		notifyDataSetChanged();
		dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
		final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
		dialogBuilder.setSingleChoiceItems(items, selectedShare.getAccess(), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				removeShare = false;
				changeShare = true;

				statusDialog = getProgressDialog(fileContactListActivityLollipop, getString(R.string.context_permissions_changing_folder));
				permissionsDialog.dismiss();
				cC.changePermission(selectedShare.getUser(), item, node, new ShareListener(getApplicationContext(), CHANGE_PERMISSIONS_LISTENER, 1));
			}
		});
		permissionsDialog = dialogBuilder.create();
		permissionsDialog.show();
	}
	
	public void setPositionClicked(int positionClicked){
		if (adapter != null){
			adapter.setPositionClicked(positionClicked);
		}
	}
	
	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}

	/*
	 * Handle processed upload intent
	 */
	public void onIntentProcessed() {
		List<ShareInfo> infos = filePreparedInfos;
		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			}
			catch(Exception ex){}
		}
		
		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
		if(parentNode == null){
			showSnackbar(getString(R.string.error_temporary_unavaible));
			return;
		}
		
		if (infos == null) {
			showSnackbar(getString(R.string.upload_can_not_open));
		}
		else {
			showSnackbar(getString(R.string.upload_began));
			for (ShareInfo info : infos) {
				Intent intent = new Intent(this, UploadService.class);
				intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
				intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
				intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
				startService(intent);
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (intent == null) {
			return;
		}
		
		if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			if(!isOnline(this)){
				showSnackbar(getString(R.string.error_server_connection_problem));
				return;
			}
			
			final ArrayList<String> emails = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
			final long nodeHandle = intent.getLongExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, -1);
			
			if(nodeHandle!=-1){
				node=megaApi.getNodeByHandle(nodeHandle);
			}
			if(node!=null)
			{
				if (node.isFolder()){
					dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
					final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
					dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							statusDialog = getProgressDialog(fileContactListActivityLollipop, getString(R.string.context_sharing_folder));
							permissionsDialog.dismiss();
							nC.shareFolder(node, emails, item);
						}
					});
					permissionsDialog = dialogBuilder.create();
					permissionsDialog.show();
				}
			}
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

		try {
			statusDialog.dismiss();
		}
		catch (Exception ex) {
			logError("Error dismiss status dialog", ex);
		}

		if (node.isFolder()){
			listContacts.clear();

			tempListContacts = megaApi.getOutShares(node);
			if(tempListContacts!=null && !tempListContacts.isEmpty()){
				listContacts.addAll(megaApi.getOutShares(node));
			}

			if (listContacts != null){
				if (listContacts.size() > 0){
					listView.setVisibility(View.VISIBLE);
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);

					if (adapter != null){
						adapter.setNode(node);
						adapter.setContext(this);
						adapter.setShareList(listContacts);
						adapter.setListFragment(listView);
					}
					else{
						adapter = new MegaSharedFolderLollipopAdapter(this, node, listContacts, listView);
					}
				}
				else{
					listView.setVisibility(View.GONE);
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					//((RelativeLayout.LayoutParams)infoTable.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.file_properties_image);
				}
			}
		}

		listView.invalidate();
	}

	public void showConfirmationRemoveContactFromShare (final String email){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        String message = getResources().getString(R.string.remove_contact_shared_folder, email);
        builder.setMessage(message)
                .setPositiveButton(R.string.general_remove, (dialog, which) -> removeShare(email))
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {})
                .show();
	}

	public void showConfirmationRemoveMultipleContactFromShare (final ArrayList<MegaShare> contacts){
		logDebug("showConfirmationRemoveMultipleContactFromShare");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						removeMultipleShares(contacts);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.remove_multiple_contacts_shared_folder,contacts.size());
		builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void removeShare (String email) {
		statusDialog = getProgressDialog(fileContactListActivityLollipop, getString(R.string.context_removing_contact_folder));

		if (email != null){
			removeShare = true;
			nC.removeShare(new ShareListener(this, REMOVE_SHARE_LISTENER, 1), node, email);
		}
	}

	public void removeMultipleShares(ArrayList<MegaShare> shares){
		logDebug("Number of shared to remove: " + shares.size());

		statusDialog = getProgressDialog(fileContactListActivityLollipop, getString(R.string.context_removing_contact_folder));
		nC.removeShares(shares, node);
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
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

	public void showSnackbar(String s){
		showSnackbar(contactLayout, s);
	}

	public MegaUser getSelectedContact() {
		String email = selectedShare.getUser();
		return megaApi.getContact(email);
	}

	public MegaShare getSelectedShare() {
		return selectedShare;
	}

	public void setSelectedShare(MegaShare selectedShare) {
		this.selectedShare = selectedShare;
	}
}

