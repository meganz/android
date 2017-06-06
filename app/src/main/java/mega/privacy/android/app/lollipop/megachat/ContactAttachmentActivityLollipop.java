package mega.privacy.android.app.lollipop.megachat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaSharedFolderLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.FileContactMultipleRequestListener;
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ContactAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.NodeAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatHandleList;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

public class ContactAttachmentActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, OnClickListener {

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	ActionBar aB;
	Toolbar tB;
	ContactAttachmentActivityLollipop contactAttachmentActivityLollipop = this;
	public MegaUser selectedUser;

	RelativeLayout container;
	RecyclerView listView;
	View separator;
	Button inviteButton;
	LinearLayout optionsBar;
	LinearLayoutManager mLayoutManager;
	GestureDetectorCompat detector;

	ChatController cC;

	AndroidMegaChatMessage message = null;
	public long chatId;
	public long messageId;

	ArrayList<MegaContactAdapter> contacts;

	MegaContactsLollipopAdapter adapter;

	private ActionMode actionMode;

	MenuItem addSharingContact;
	MenuItem selectMenuItem;
	MenuItem unSelectMenuItem;

	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

	    public void onLongPress(MotionEvent e) {
			log("onLongPress -- RecyclerViewOnGestureListener");
			// handle long press
			if (!adapter.isMultipleSelect()){
				adapter.setMultipleSelect(true);

				actionMode = startSupportActionMode(new ActionBarCallBack());
			}
			super.onLongPress(e);
	    }
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			final ArrayList<MegaUser> contacts = adapter.getSelectedUsers();
						
			switch(item.getItemId()){
				case R.id.action_file_contact_list_permissions:{
					


					break;
				}
				case R.id.action_file_contact_list_delete:{


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
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			adapter.clearSelections();
			adapter.setMultipleSelect(false);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaUser> selected = adapter.getSelectedUsers();
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
			menu.findItem(R.id.action_file_contact_list_delete).setVisible(deleteShare);
			
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

		if (megaChatApi == null){
			megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
		}

		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		cC = new ChatController(this);

		Intent intent = getIntent();
		if (intent != null) {
			chatId = intent.getLongExtra("chatId", -1);
			messageId = intent.getLongExtra("messageId", -1);
			log("Id Chat and Message id: "+chatId+ "___"+messageId);
			MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
			if(messageMega!=null){
				message = new AndroidMegaChatMessage(messageMega);
			}
		}

		if(message!=null){
			contacts = new ArrayList<>();

			long userCount  = message.getMessage().getUsersCount();
			for(int i=0; i<userCount;i++) {
				String name = "";
				name = message.getMessage().getUserName(i);
				if (name.trim().isEmpty()) {
					name = message.getMessage().getUserEmail(i);
				}
				String email = message.getMessage().getUserEmail(i);
				log("Contact Name: " + name);
				MegaUser contact = megaApi.getContact(email);
				if (contact != null) {
					MegaContactAdapter contactDB = new MegaContactAdapter(null, contact, name);
					contacts.add(contactDB);
				} else {
					log("Error - this contact is NULL");
				}
			}
		}
		else{
			finish();
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
		}

		setContentView(R.layout.activity_contact_attachment_chat);

		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_contact_attachment_chat);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);
		aB.setTitle(getString(R.string.activity_title_contacts_attached));

		if(message.getMessage().getUserHandle()==megaChatApi.getMyUserHandle()) {
			aB.setSubtitle(megaChatApi.getMyFullname());
		}
		else{
			String fullNameAction = cC.getFullName(message.getMessage().getUserHandle(), chatId);

			if(fullNameAction==null){
				fullNameAction = "";
			}
			aB.setSubtitle(fullNameAction);
		}

		container = (RelativeLayout) findViewById(R.id.contact_attachment_chat);

		optionsBar = (LinearLayout) findViewById(R.id.options_contact_attachment_chat_layout);
		separator = (View) findViewById(R.id.contact_attachment_chat_separator_3);

		inviteButton = (Button) findViewById(R.id.contact_attachment_chat_invite_button);
		inviteButton.setOnClickListener(this);

		detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());

		listView = (RecyclerView) findViewById(R.id.contact_attachment_chat_view_browser);
		listView.setClipToPadding(false);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnItemTouchListener(this);
		listView.setItemAnimator(new DefaultItemAnimator());

		if (adapter == null){
			adapter = new MegaContactsLollipopAdapter(this, contacts, listView);
		}

		adapter.setPositionClicked(-1);
		adapter.setMultipleSelect(false);

		listView.setAdapter(adapter);

		((MegaApplication) getApplication()).sendSignalPresenceActivity();

	}

	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	if(megaApi != null)
    	{
    		megaApi.removeRequestListener(this);
    	}
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_folder_contact_list, menu);
	    
//	    permissionButton = menu.findItem(R.id.action_file_contact_list_permissions);
//	    deleteShareButton = menu.findItem(R.id.action_file_contact_list_delete);
	    addSharingContact = menu.findItem(R.id.action_folder_contacts_list_share_folder);
	    	    
//	    permissionButton.setVisible(false);
//	    deleteShareButton.setVisible(false);
	    addSharingContact.setVisible(true);
	    
	    selectMenuItem = menu.findItem(R.id.action_select);
		unSelectMenuItem = menu.findItem(R.id.action_unselect);
		
		selectMenuItem.setVisible(true);
		unSelectMenuItem.setVisible(false);
	    
	    return super.onCreateOptionsMenu(menu);
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
		    case R.id.action_folder_contacts_list_share_folder:{
		    	//Option add new contact to share


		    	
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

	/*
	 * Clear all selected items
	 */
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

	public void showOptionsPanel(MegaUser sUser){
		log("showNodeOptionsPanel-Offline");
		if(sUser!=null){
			this.selectedUser = sUser;
			ContactAttachmentBottomSheetDialogFragment bottomSheetDialogFragment = new ContactAttachmentBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_SHARE) {
			log("onRequestStart - Share");
		}
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
		log("onRequestFinish: " + request.getType());
		log("onRequestFinish: " + request.getRequestString());

	}


	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}
	
	public static void log(String log) {
		Util.log("ContactAttachmentActivityLollipop", log);
	}

	public void itemClick(int position) {
		log("itemClick");
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
		if (adapter.isMultipleSelect()){
			adapter.toggleSelection(position);
			updateActionModeTitle();
		}
	}

	
	private void updateActionModeTitle() {
		log("updateActionModeTitle");
		if (actionMode == null) {
			return;
		}
		List<MegaUser> contacts = adapter.getSelectedUsers();
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
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
		switch (v.getId()){		
			case R.id.contact_attachment_chat_invite_button:{
				log("Click on Invite button");
//				Intent i = new Intent(this, ManagerActivityLollipop.class);
//				i.setAction(Constants.ACTION_REFRESH_PARENTHANDLE_BROWSER);
//				i.putExtra("parentHandle", node.getHandle());
//				startActivity(i);
//				finish();
				break;
			}

		}
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (intent == null) {
			return;
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

	public void showSnackbar(String s){
		log("showSnackbar");
		Snackbar snackbar = Snackbar.make(container, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}

}

