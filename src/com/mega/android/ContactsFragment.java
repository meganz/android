package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.widget.AdapterView.OnItemLongClickListener;
import android.support.v7.view.ActionMode;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ListView;

import com.mega.android.FileStorageActivity.Mode;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaShare;
import com.mega.sdk.MegaUser;

public class ContactsFragment extends Fragment implements OnClickListener, OnItemClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface, OnItemLongClickListener{

	public static final String ARG_OBJECT = "object";
	
	private ProgressDialog statusDialog;	
    private AlertDialog addContactDialog;
	
	MegaApiAndroid megaApi;	
	
	Context context;
	ActionBar aB;
	ListView listView;
	MegaContactsListAdapter adapterList;
	MegaContactsGridAdapter adapterGrid;
	ImageView emptyImageView;
	TextView emptyTextView;
	
	private Button addContactButton;
	private ActionMode actionMode;
	
	boolean isList = true;
	
	ContactsFragment contactsFragment = this;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	/////Multiselect/////
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaUser> users = getSelectedUsers();
			
			switch(item.getItemId()){
				case R.id.cab_menu_settings:{
					startActivity(new Intent(getActivity(), SettingsActivity.class));
					break;
				}
				case R.id.cab_menu_upgrade_account:{
					Intent intent = new Intent(getActivity(), UpgradeActivity.class);
					startActivity(intent);
					break;
				}
				case R.id.cab_menu_help:{
					Toast.makeText(getActivity(), "Help not yet implemented (refresh, sort by and logout are implemented)", Toast.LENGTH_SHORT).show();
					break;
				}	
				case R.id.cab_menu_share_folder:{
					clearSelections();
					hideMultipleSelect();
					if (users.size()>0){
						((ManagerActivity) context).pickFolderToShare(users);
					}	
									
					break;
				}
				case R.id.cab_menu_delete:{
					Toast.makeText(getActivity(), "Delete not yet implemented", Toast.LENGTH_SHORT).show();
					break;
				}
			}
			return false;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.contact_fragment_action, menu);
			
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			adapterList.setMultipleSelect(false);
			listView.setOnItemLongClickListener(contactsFragment);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaUser> selected = getSelectedUsers();

			if (selected.size() > 0) {
			menu.findItem(R.id.cab_menu_delete).setVisible(true);
			menu.findItem(R.id.cab_menu_share_folder).setVisible(true);
			menu.findItem(R.id.cab_menu_help).setVisible(true);
			menu.findItem(R.id.cab_menu_upgrade_account).setVisible(true);
			menu.findItem(R.id.cab_menu_settings).setVisible(true);
			}
			
			return false;
		}
		
	}
	
	/*
	 * Get list of all selected contacts
	 */
	private List<MegaUser> getSelectedUsers() {
		ArrayList<MegaUser> usersSelected = new ArrayList<MegaUser>();
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				MegaUser user = adapterList.getContactAt(checkedItems.keyAt(i));
				if (user != null){
					log("User "+user.getEmail());
					usersSelected.add(user);
				}
			}
		}
		return usersSelected;
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				listView.setItemChecked(checkedPosition, false);
			}
		}
		updateActionModeTitle();
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaUser> users = getSelectedUsers();
		
		Resources res = getResources();
		String format = "%d %s";
		
		actionMode.setTitle(String.format(format, users.size(),res.getQuantityString(R.plurals.general_num_contacts, contacts.size())+ " selected"));

		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
	}
		
	//End Multiselect/////
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		megaApi.addGlobalListener(this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		contacts = megaApi.getContacts();
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}
		
		
		if (isList){
			View v = inflater.inflate(R.layout.fragment_contactslist, container, false);
			
			listView = (ListView) v.findViewById(R.id.contacts_list_view);
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setItemsCanFocus(false);
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			
			
			emptyImageView = (ImageView) v.findViewById(R.id.contact_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_list_empty_text);
			
			listView.setItemsCanFocus(false);
			if (adapterList == null){
				adapterList = new MegaContactsListAdapter(context, visibleContacts, emptyImageView, emptyTextView, listView);
			}
			else{
				adapterList.setContacts(visibleContacts);
			}
		
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
			addContactButton = (Button) v.findViewById(R.id.add_contact_button);
			addContactButton.setOnClickListener(this);
						
			if (adapterList.getCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				emptyTextView.setText(R.string.contacts_list_empty_text);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
			
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
			
			listView = (ListView) v.findViewById(R.id.contact_grid_view_browser);
	        listView.setOnItemClickListener(null);
	        listView.setItemsCanFocus(false);
	        
	        if (adapterGrid == null){
	        	adapterGrid = new MegaContactsGridAdapter(context, visibleContacts, emptyImageView, emptyTextView, listView);
	        }
	        else{
	        	adapterGrid.setContacts(visibleContacts);
	        }
	        
	        adapterGrid.setPositionClicked(-1);   
			listView.setAdapter(adapterGrid);
			addContactButton = (Button) v.findViewById(R.id.add_contact_button);
			addContactButton.setOnClickListener(this);
			
			
			if (adapterGrid.getCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				emptyTextView.setText(R.string.contacts_list_empty_text);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
			
			return v;
		}			
	}
	
	public void setContacts(ArrayList<MegaUser> contacts){
		this.contacts = contacts;
		
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}
		
		if (isList){
			adapterList.setContacts(visibleContacts);
		}
		else{
			adapterGrid.setContacts(visibleContacts);
		}
		
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.add_contact_button:
				
				String text;
				
				text = getString(R.string.context_new_contact_name);

				final EditText input = new EditText(context);
//				input.setId(EDIT_TEXT_ID);
				input.setSingleLine();
				input.setSelectAllOnFocus(true);
				input.setImeOptions(EditorInfo.IME_ACTION_DONE);
				input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
				input.setOnEditorActionListener(new OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							String value = v.getText().toString().trim();
							if (value.length() == 0) {
								return true;
							}
							addContact(value);
							addContactDialog.dismiss();
							return true;
						}
						return false;
					}
				});
				input.setImeActionLabel(getString(R.string.general_add),
						KeyEvent.KEYCODE_ENTER);
				input.setText(text);
//				input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//					@Override
//					public void onFocusChange(View v, boolean hasFocus) {
//						if (hasFocus) {
//							showKeyboardDelayed(v);
//						}
//					}
//				});
				AlertDialog.Builder builder = Util.getCustomAlertBuilder(getActivity(), getString(R.string.menu_add_contact),
						null, input);
				builder.setPositiveButton(getString(R.string.general_add),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String value = input.getText().toString().trim();
								if (value.length() == 0) {
									return;
								}
								addContact(value);
							}
						});
				builder.setNegativeButton(getString(android.R.string.cancel), null);
				addContactDialog = builder.create();
				addContactDialog.show();
				
				break;
		}
	}
	
	
	private void addContact(String contactEmail){
		log("addContact");
		if (!Util.isOnline(context)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, getActivity());
			return;
		}		

		statusDialog = null;
		try {
			statusDialog = new ProgressDialog(context);
			statusDialog.setMessage(getString(R.string.context_adding_contact));
			statusDialog.show();
		}
		catch(Exception e){
			return;
		}
		
		megaApi.addContact(contactEmail, this);
	}
	
	
	
	
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
		
		if (isList){
			
			if (adapterList.isMultipleSelect()){
				SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
				if (checkedItems.get(position, false) == true){
					listView.setItemChecked(position, true);
				}
				else{
					listView.setItemChecked(position, false);
				}				
				updateActionModeTitle();
				adapterList.notifyDataSetChanged();
			}
			else{
		
				Intent i = new Intent(context, ContactPropertiesMainActivity.class);
				i.putExtra("name", visibleContacts.get(position).getEmail());
				startActivity(i);
			}
		}
    }
	
	public int onBackPressed(){
		
		if (isList){
			if (adapterList.getPositionClicked() != -1){
				adapterList.setPositionClicked(-1);
				adapterList.notifyDataSetChanged();
				return 1;
			}
			else{
				return 0;
			}
		}
		else{
			if (adapterGrid.getPositionClicked() != -1){
				adapterGrid.setPositionClicked(-1);
				adapterGrid.notifyDataSetChanged();
				return 1;
			}
			else{
				return 0;
			}
		}
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	public void setPositionClicked(int positionClicked){
		if (isList){
			if (adapterList != null){
				adapterList.setPositionClicked(positionClicked);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setPositionClicked(positionClicked);
			}	
		}		
	}
	
	public void notifyDataSetChanged(){
		if (isList){
			if (adapterList != null){
				adapterList.notifyDataSetChanged();
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.notifyDataSetChanged();
			}
		}
	}
	
	public ListView getListView(){
		return listView;
	}
	
	private static void log(String log) {
		Util.log("ContactsFragment", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

		if (request.getType() == MegaRequest.TYPE_ADD_CONTACT){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(context, "Contact added", Toast.LENGTH_LONG).show();
							
				if (this.isVisible()){	
					ArrayList<MegaUser> contacts = megaApi.getContacts();
					setContacts(contacts);
					getListView().invalidateViews();
				}
				}
			
			log("Contact Added");
		}
		
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,MegaError e) {}

	@Override
	public void onUsersUpdate(MegaApiJava api) {
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		this.setContacts(contacts);
		this.getListView().invalidateViews();
		
	}

	@Override
	public void onNodesUpdate(MegaApiJava api) {}

	@Override
	public void onReloadNeeded(MegaApiJava api) {}
	
	@Override
	public void onDestroy(){
    	log("onDestroy()");
    	super.onDestroy();    	    	
    	
    	if (megaApi != null){
    		megaApi.removeGlobalListener(this); 
    	}
    }

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (adapterList.getPositionClicked() == -1){
			clearSelections();
			actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
			listView.setItemChecked(position, true);
			adapterList.setMultipleSelect(true);
			updateActionModeTitle();
			listView.setOnItemLongClickListener(null);
		}
		return true;
	}
	

}
