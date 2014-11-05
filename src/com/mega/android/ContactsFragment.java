package com.mega.android;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ListView;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaUser;

public class ContactsFragment extends Fragment implements OnClickListener, OnItemClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface{

	public static final String ARG_OBJECT = "object";
	
	private ProgressDialog statusDialog;	
    private AlertDialog addContactDialog;
	
	MegaApiAndroid megaApi;
	
	Context context;
	ActionBar aB;
	ListView listView;
	MegaContactsListAdapter adapterList;
	MegaContactsGridAdapter adapterGrid;
	
	private Button addContactButton;
	
	boolean isList = true;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
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
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			listView.setItemsCanFocus(false);
			if (adapterList == null){
				adapterList = new MegaContactsListAdapter(context, visibleContacts);
			}
			else{
				adapterList.setContacts(visibleContacts);
			}
			
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
			addContactButton = (Button) v.findViewById(R.id.add_contact_button);
			addContactButton.setOnClickListener(this);
			
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
			
			listView = (ListView) v.findViewById(R.id.contact_grid_view_browser);
	        listView.setOnItemClickListener(null);
	        listView.setItemsCanFocus(false);
	        
	        if (adapterGrid == null){
	        	adapterGrid = new MegaContactsGridAdapter(context, visibleContacts);
	        }
	        else{
	        	adapterGrid.setContacts(visibleContacts);
	        }
	        
	        adapterGrid.setPositionClicked(-1);
			listView.setAdapter(adapterGrid);
			addContactButton = (Button) v.findViewById(R.id.add_contact_button);
			addContactButton.setOnClickListener(this);
			
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
			Intent i = new Intent(context, ContactPropertiesMainActivity.class);
			i.putExtra("name", visibleContacts.get(position).getEmail());
			startActivity(i);
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
	

}
