package mega.privacy.android.app.lollipop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
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
import mega.privacy.android.app.lollipop.adapters.MegaSharedFolderLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.listeners.FileContactMultipleRequestListener;
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment;
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
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet.isBottomSheetDialogShown;

public class FileContactListActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, OnClickListener, MegaGlobalListenerInterface {

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

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = startSupportActionMode(new ActionBarCallBack());
		}
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			final List<MegaShare> shares = adapter.getSelectedShares();
			
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
                            ProgressDialog temp;
                            try{
                                temp = new ProgressDialog(FileContactListActivityLollipop.this);
                                temp.setMessage(getString(R.string.context_permissions_changing_folder));
                                temp.show();
                            }
                            catch(Exception e){
                                log(e.getMessage());
                                return;
                            }
                            statusDialog = temp;

							ContactController controller = new ContactController(FileContactListActivityLollipop.this);
							MegaRequestListenerInterface callback;
							if (shares.size() > 1) {
								callback = new FileContactMultipleRequestListener(Constants.MULTIPLE_CHANGE_PERMISSION, FileContactListActivityLollipop.this, requestCompletedCallback);
							} else if (shares.size() == 1) {
								callback = FileContactListActivityLollipop.this;
							} else {
								log("Shares array is empty");
								return;
							}
							controller.checkShares(shares, item, node, callback);
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
								log("Remove multiple contacts");
								showConfirmationRemoveMultipleContactFromShare(shares);
							} else {
								log("Remove one contact");
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
			log("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_contact_shared_browser_action, menu);
			fab.setVisibility(View.GONE);
			Util.changeStatusBarColorActionMode(fileContactListActivityLollipop, getWindow(), handler, 1);
			checkScroll();
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			adapter.clearSelections();
			adapter.setMultipleSelect(false);
			fab.setVisibility(View.VISIBLE);
			Util.changeStatusBarColorActionMode(fileContactListActivityLollipop, getWindow(), handler, 3);
			checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaShare> selected = adapter.getSelectedShares();
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

			tempListContacts = megaApi.getPendingOutShares(node);

			if(tempListContacts!=null && !tempListContacts.isEmpty()){
				log("Size of pending out shares: "+tempListContacts);
				listContacts.addAll(megaApi.getPendingOutShares(node));
			}
			
			listView = (RecyclerView) findViewById(R.id.file_contact_list_view_browser);
			listView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
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
			aB.setElevation(Util.px2dp(4, outMetrics));
		}
		else {
			aB.setElevation(0);
		}
	}
	
	
	public void showOptionsPanel(MegaShare sShare){
		log("showNodeOptionsPanel");

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
    		megaApi.removeRequestListener(this);
    	}
    	handler.removeCallbacksAndMessages(null);
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
		addSharingContact.setIcon(Util.mutateIconSecondary(this, R.drawable.ic_share_white, R.color.black));
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
		intent.putExtra("contactType", Constants.CONTACT_TYPE_BOTH);
		intent.putExtra("MULTISELECT", 0);
		intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
		startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_CONTACT);
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

		if (request.getType() == MegaRequest.TYPE_SHARE){
			log(" MegaRequest.TYPE_SHARE");

			String message;
			if (e.getErrorCode() == MegaError.API_OK){
                if (removeShare) {
                    log("OK onRequestFinish remove");
                    removeShare = false;
                    adapter.setShareList(listContacts);
                    listView.invalidate();
                    message = getString(R.string.context_share_correctly_removed);
                } else if (changeShare) {
                    log("OK onRequestFinish change");
                    permissionsDialog.dismiss();
                    changeShare = false;
                    adapter.setShareList(listContacts);
                    listView.invalidate();
                    message = getString(R.string.context_permissions_changed);
                } else {
                    message = getString(R.string.context_correctly_shared);
                }
			}
			else{
                if (removeShare) {
                    log("ERROR onRequestFinish remove");
                    removeShare = false;
                    message = getString(R.string.context_contact_not_removed);
                } else if (changeShare) {
                    log("ERROR onRequestFinish change");
                    changeShare = false;
                    message = getString(R.string.context_permissions_not_changed);
                }else{
                    message = getString(R.string.context_no_shared);
                }
			}
            showSnackbar(Constants.SNACKBAR_TYPE,container,message);
			log("Finish onRequestFinish");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			
			if (e.getErrorCode() == MegaError.API_OK){
				adapter.setShareList(listContacts);
				listView.invalidate();

				showSnackbar(getString(R.string.context_node_private));
			}
			else{
				showSnackbar(e.getErrorString());
			}
		}
	}


	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}
	
	public static void log(String log) {
		Util.log("FileContactListActivityLollipop", log);
	}

	public void itemClick(int position) {
		log("itemClick");

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
		log("onBackPressed");
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
		log("updateActionModeTitle");
		if (actionMode == null) {
			return;
		}
		List<MegaShare> contacts = adapter.getSelectedShares();
		if(contacts!=null){
			log("Contacts selected: "+contacts.size());
		}

		Resources res = getResources();
		String format = "%d %s";
	
		actionMode.setTitle(String.format(format, contacts.size(),res.getQuantityString(R.plurals.general_num_contacts, contacts.size())));
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
			case R.id.floating_button_file_contact_list:{
				shareOption();
				break;
			}
		}
	}

	public void removeFileContactShare(){
		log("removeFileContactShare");
		notifyDataSetChanged();

		showConfirmationRemoveContactFromShare(selectedShare.getUser());
	}

	public void changePermissions(){
		log("changePermissions");
		notifyDataSetChanged();
		dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
		final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
		dialogBuilder.setSingleChoiceItems(items, selectedShare.getAccess(), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				removeShare = false;
				changeShare = true;
				ProgressDialog temp = null;
				try{
					temp = new ProgressDialog(fileContactListActivityLollipop);
					temp.setMessage(getString(R.string.context_permissions_changing_folder));
					temp.show();
				}
				catch(Exception e){
					return;
				}
				statusDialog = temp;
				permissionsDialog.dismiss();

				switch(item) {
					case 0:{
						MegaUser u = megaApi.getContact(selectedShare.getUser());
						if(u!=null){
							megaApi.share(node, u, MegaShare.ACCESS_READ, fileContactListActivityLollipop);
						}
						else{
							megaApi.share(node, selectedShare.getUser(), MegaShare.ACCESS_READ, fileContactListActivityLollipop);
						}

						break;
					}
					case 1:{
						MegaUser u = megaApi.getContact(selectedShare.getUser());
						if(u!=null){
							megaApi.share(node, u, MegaShare.ACCESS_READWRITE, fileContactListActivityLollipop);
						}
						else{
							megaApi.share(node, selectedShare.getUser(), MegaShare.ACCESS_READWRITE, fileContactListActivityLollipop);
						}
						break;
					}
					case 2:{
						MegaUser u = megaApi.getContact(selectedShare.getUser());
						if(u!=null){
							megaApi.share(node, u, MegaShare.ACCESS_FULL, fileContactListActivityLollipop);
						}
						else{
							megaApi.share(node, selectedShare.getUser(), MegaShare.ACCESS_FULL, fileContactListActivityLollipop);
						}
						break;
					}
				}
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
		
		if (requestCode == Constants.REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			if(!Util.isOnline(this)){
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
							ProgressDialog temp = null;
							try{
								temp = new ProgressDialog(fileContactListActivityLollipop);
								temp.setMessage(getString(R.string.context_sharing_folder));
								temp.show();
							}
							catch(Exception e){
								return;
							}
							statusDialog = temp;
							permissionsDialog.dismiss();
							
							switch(item) {
			                    case 0:{
			                    	for (int i=0;i<emails.size();i++){
			                    		MegaUser u = megaApi.getContact(emails.get(i));

										if(u!=null){
											log("Share: "+ node.getName() + " to "+ u.getEmail());
											megaApi.share(node, u, MegaShare.ACCESS_READ, fileContactListActivityLollipop);
										}
										else{
											log("USER is NULL when sharing!->SHARE WITH NON CONTACT");
											megaApi.share(node, emails.get(i), MegaShare.ACCESS_READ, fileContactListActivityLollipop);
										}
			                    	}
			                    	break;
			                    }
			                    case 1:{
			                    	for (int i=0;i<emails.size();i++){
			                    		MegaUser u = megaApi.getContact(emails.get(i));
										if(u!=null){
											log("Share: "+ node.getName() + " to "+ u.getEmail());
											megaApi.share(node, u, MegaShare.ACCESS_READWRITE, fileContactListActivityLollipop);
										}
										else{
											log("USER is NULL when sharing!->SHARE WITH NON CONTACT");
											megaApi.share(node, emails.get(i), MegaShare.ACCESS_READWRITE, fileContactListActivityLollipop);
										}
			                    	}
			                        break;
			                    }
			                    case 2:{
			                    	for (int i=0;i<emails.size();i++){
			                    		MegaUser u = megaApi.getContact(emails.get(i));
										if(u!=null){
											log("Share: "+ node.getName() + " to "+ u.getEmail());
											megaApi.share(node, u, MegaShare.ACCESS_FULL, fileContactListActivityLollipop);
										}
										else{
											log("USER is NULL when sharing!->SHARE WITH NON CONTACT");
											megaApi.share(node, emails.get(i), MegaShare.ACCESS_FULL, fileContactListActivityLollipop);
										}
			                    	}
			                        break;
			                    }
			                }
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

		try {
			statusDialog.dismiss();
		}
		catch (Exception ex) {
			log("Error dismiss status dialog");
		}

		if (node.isFolder()){
			listContacts.clear();

			tempListContacts = megaApi.getOutShares(node);
			if(tempListContacts!=null && !tempListContacts.isEmpty()){
				listContacts.addAll(megaApi.getOutShares(node));
			}

			tempListContacts = megaApi.getPendingOutShares(node);
			if(tempListContacts!=null && !tempListContacts.isEmpty()){
				listContacts.addAll(megaApi.getPendingOutShares(node));
			}

//			listContacts = megaApi.getOutShares(node);
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
		log("showConfirmationRemoveContactFromShare");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						removeShare(email);
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
		String message= getResources().getString(R.string.remove_contact_shared_folder,email);
		builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationRemoveMultipleContactFromShare (final List<MegaShare> contacts){
		log("showConfirmationRemoveMultipleContactFromShare");

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

	public void removeShare (String email)
	{
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_removing_contact_folder));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		if (email != null){
			removeShare = true;
			megaApi.share(node, email, MegaShare.ACCESS_UNKNOWN, this);
		}
		else{
			megaApi.disableExport(node, this);
		}

	}

	public void removeMultipleShares(List<MegaShare> shares){
		log("removeMultipleShares");
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_removing_contact_folder));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		FileContactMultipleRequestListener removeMultipleListener = new FileContactMultipleRequestListener(Constants.MULTIPLE_REMOVE_CONTACT_SHARED_FOLDER, this,requestCompletedCallback);
		for(int j=0;j<shares.size();j++){
			if(shares.get(j).getUser()!=null){
				MegaUser u = megaApi.getContact(shares.get(j).getUser());
				if(u!=null){
					megaApi.share(node, u, MegaShare.ACCESS_UNKNOWN, removeMultipleListener);
				}
				else{
					megaApi.share(node, shares.get(j).getUser(), MegaShare.ACCESS_UNKNOWN, removeMultipleListener);
				}
			}
			else{
				megaApi.disableExport(node, removeMultipleListener);
			}
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

