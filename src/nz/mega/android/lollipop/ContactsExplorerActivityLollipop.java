package nz.mega.android.lollipop;

import java.util.ArrayList;

import nz.mega.android.ContactsExplorerAdapter.OnItemCheckClickListener;
import nz.mega.android.MegaApplication;
import nz.mega.android.PinActivity;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


public class ContactsExplorerActivityLollipop extends PinActivity implements OnClickListener, OnItemCheckClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface {
	
	public static String ACTION_PICK_CONTACT_SHARE_FOLDER = "ACTION_PICK_CONTACT_SHARE_FOLDER";
	public static String ACTION_PICK_CONTACT_SEND_FILE = "ACTION_PICK_CONTACT_SEND_FILE";
	
	public static String EXTRA_NODE_HANDLE = "node_handle";
	public static String EXTRA_MEGA_CONTACTS = "mega_contacts";
	public static String EXTRA_CONTACTS = "extra_contacts";
	public static String EXTRA_EMAIL = "extra_email";
	public static String EXTRA_PHONE = "extra_phone";
	
	MegaApiAndroid megaApi;
	
	boolean megaContacts = true;
	
	int multipleSelectIntent;
	int sendToInbox;
	private TextView windowTitle;
	private Button button;
	private RecyclerView listView;
	private RecyclerView.LayoutManager mLayoutManager;
	private ImageButton addContactButton;
	private ImageButton megaPhoneContacts;
	
	long nodeHandle = -1;
	long[] nodeHandles;
	
	ContactsExplorerLollipopAdapter adapter;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	private AlertDialog addContactDialog;
	
	public class PhoneContacts{
		long id;
		String name;
		String email;
		String phoneNumber;
		
		public PhoneContacts(long id, String name, String email, String phoneNumber) {
			this.id = id;
			this.name = name;
			this.email = email;
			this.phoneNumber = phoneNumber;
		}
		
		public long getId(){
			return id;
		}
		
		public String getName(){
			return name;
		}
		
		public String getEmail(){
			return email;
		}
		
		public String getPhoneNumber(){
			return phoneNumber;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	        // do nothing
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	} 
	
	private class RefreshContactsTask extends AsyncTask<String, Void, Boolean> {

		ArrayList<PhoneContacts> contactList;
		
		@Override
		protected Boolean doInBackground(String... args) {
			// Fetch emails from contact list
	        contactList = refreshPhoneContacts();
	        
	        if (contactList != null){
	        	return true;
	        }
	        else{
	        	return false;
	        }
		}
		
		@Override
		protected void onPostExecute(Boolean res) {
			// Show emails on screen

			if (!megaContacts){
				if (adapter != null){
					adapter.setContacts(null, contactList);
				}
			}
		}
		
	}
	
	@SuppressLint("InlinedApi")
	private ArrayList<PhoneContacts> refreshPhoneContacts() {
       ArrayList<PhoneContacts> contactList = new ArrayList<PhoneContacts>();
         
       try {
 
            /**************************************************/
             
            ContentResolver cr = getBaseContext().getContentResolver();
            
            @SuppressLint("InlinedApi")
            String SORT_ORDER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? Contacts.SORT_KEY_PRIMARY : Contacts.DISPLAY_NAME;
            
            Cursor c = cr.query(
                    Data.CONTENT_URI, 
                    null, 
                    "(" + Data.MIMETYPE + "= ? OR " + Data.MIMETYPE + "= ?) AND " + 
                    Data.CONTACT_ID + " IN (SELECT " + Contacts._ID + " FROM contacts WHERE " + Contacts.HAS_PHONE_NUMBER + "!=0) AND " + Contacts.IN_VISIBLE_GROUP + "=1", 
                    new String[]{Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE}, SORT_ORDER);
            
            while (c.moveToNext()){
            	long id = c.getLong(c.getColumnIndex(Data.CONTACT_ID));
                String name = c.getString(c.getColumnIndex(Data.DISPLAY_NAME));
                String data1 = c.getString(c.getColumnIndex(Data.DATA1));
                String mimetype = c.getString(c.getColumnIndex(Data.MIMETYPE));
                if (mimetype.compareTo(Email.CONTENT_ITEM_TYPE) == 0){
                	log("ID: " + id + "___ NAME: " + name + "____ EMAIL: " + data1);
                	PhoneContacts pc = new PhoneContacts(id, name, data1, null);
                	contactList.add(pc);
                }
                else if (mimetype.compareTo(Phone.CONTENT_ITEM_TYPE) == 0){
                	PhoneContacts pc = new PhoneContacts(id, name, null, data1);
                	contactList.add(pc);
                	log("ID: " + id + "___ NAME: " + name + "____ PHONE: " + data1);
                }
            }
            
            c.close();
            
            return contactList;
 
        } catch (Exception e) {}
         
        return null;
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			MegaApplication app = (MegaApplication)getApplication();
			megaApi = app.getMegaApi();
		}			
		
		megaApi.addGlobalListener(this);		
		
		setContentView(R.layout.activity_contactsexplorer);
		
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(EXTRA_NODE_HANDLE)) {				
				if(multipleSelectIntent==0){
					nodeHandle =  savedInstanceState.getLong(EXTRA_NODE_HANDLE);
				}
				else if(multipleSelectIntent==1){
					nodeHandles = savedInstanceState.getLongArray(EXTRA_NODE_HANDLE);
				}
			}
		}
		
		windowTitle = (TextView) findViewById(R.id.contacts_explorer_window_title);
		
		listView = (RecyclerView) findViewById(R.id.contacts_explorer_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);		
		
		button = (Button) findViewById(R.id.contacts_explorer_button);
		button.setVisibility(View.GONE);
		addContactButton = (ImageButton) findViewById(R.id.contacts_explorer_add_contact);
		megaPhoneContacts = (ImageButton) findViewById(R.id.contacts_explorer_phone_mega_contacts);
		
		if (megaContacts){
			windowTitle.setText(getResources().getString(R.string.context_mega_contacts));
			megaPhoneContacts.setImageDrawable(getResources().getDrawable(R.drawable.ic_phone));
			
			contacts = megaApi.getContacts();
			visibleContacts.clear();
			for (int i=0;i<contacts.size();i++){
				log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
				if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
					visibleContacts.add(contacts.get(i));
				}
			}
			
			if (adapter == null){
				adapter = new ContactsExplorerLollipopAdapter(this, visibleContacts, null, megaContacts);
				
				listView.setAdapter(adapter);
				
				adapter.SetOnItemClickListener(new ContactsExplorerLollipopAdapter.OnItemClickListener() {
					
					@Override
					public void onItemClick(View view, int position) {
						itemClick(view, position);
					}
				});
			}
			else{
				adapter.setContacts(visibleContacts, null);
			}			
		}
		else{
			windowTitle.setText(getResources().getString(R.string.context_phone_contacts));
			megaPhoneContacts.setImageDrawable(getResources().getDrawable(R.drawable.ic_user_contact));
		}
		
		
		Intent intent = getIntent();
		
		multipleSelectIntent = intent.getIntExtra("MULTISELECT", -1);
		if(multipleSelectIntent==0){
			nodeHandle =  intent.getLongExtra(EXTRA_NODE_HANDLE, -1);
		}
		else if(multipleSelectIntent==1){
			nodeHandles=intent.getLongArrayExtra(EXTRA_NODE_HANDLE);
		}
		
		sendToInbox= intent.getIntExtra("SEND_FILE", -1);
				
		button.setOnClickListener(this);
		addContactButton.setOnClickListener(this);
		megaPhoneContacts.setOnClickListener(this);
		
	}
	
	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	if (megaApi != null){
    		megaApi.removeGlobalListener(this);
    		megaApi.removeRequestListener(this);
    	}
	}
	
	@Override
	public void onSaveInstanceState(Bundle state) {
		if(multipleSelectIntent==0){
			state.putLong(EXTRA_NODE_HANDLE, nodeHandle);
		}
		else if(multipleSelectIntent==1){
			state.putLongArray(EXTRA_NODE_HANDLE, nodeHandles);
		}

		super.onSaveInstanceState(state);
	}
	
	/*
	 * Hide bottom action button
	 */
	private void hideButton() {
		button.setVisibility(View.GONE);
	}
	
	/*
	 * Show bottom action button
	 */
	private void showButton() {
		button.setVisibility(View.VISIBLE);
	}
	

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.contacts_explorer_button:{
				break;
			}
			case R.id.contacts_explorer_add_contact:{
				onAddContactClick();
				break;
			}
			case R.id.contacts_explorer_phone_mega_contacts:{
				megaContacts = !megaContacts;
				if (megaContacts){
					addContactButton.setVisibility(View.VISIBLE);
					windowTitle.setText(getResources().getString(R.string.context_mega_contacts));
					megaPhoneContacts.setImageDrawable(getResources().getDrawable(R.drawable.ic_phone));
					
					contacts = megaApi.getContacts();
					visibleContacts.clear();
					for (int i=0;i<contacts.size();i++){
						log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
						if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
							visibleContacts.add(contacts.get(i));
						}
					}
					
					if (adapter == null){
						adapter = new ContactsExplorerLollipopAdapter(this, visibleContacts, null, megaContacts);
						
						listView.setAdapter(adapter);
						
					}
					else{
						adapter.setContacts(visibleContacts, null);
						adapter.setMegaContacts(megaContacts);
					}
				}
				else{
					addContactButton.setVisibility(View.INVISIBLE);
					windowTitle.setText(getResources().getString(R.string.context_phone_contacts));
					megaPhoneContacts.setImageDrawable(getResources().getDrawable(R.drawable.ic_user_contact));
					
					if (adapter != null){
						adapter.setMegaContacts(megaContacts);
					}
					
					new RefreshContactsTask().execute("");
					
//					visibleContacts.clear();
//					if (adapter == null){
//						adapter = new ContactsExplorerAdapter(this, visibleContacts, megaContacts);
//						
//						listView.setAdapter(adapter);
//						listView.setOnItemClickListener(this);
//						listView.setChoiceMode(RecyclerView.CHOICE_MODE_MULTIPLE);
//						listView.setItemsCanFocus(false);
//
//						adapter.setOnItemCheckClickListener(this);
//					}
//					else{
//						adapter.setContacts(visibleContacts);
//						adapter.setMegaContacts(megaContacts);
//					}
				}
				break;
			}
		}
		
	}

	@Override
	public void onItemCheckClick(int position) {
//		boolean isChecked = listView.isItemChecked(position);
//		listView.setItemChecked(position, !isChecked);
		updateButton();
	}
	
	// Update bottom button text and state
	private void updateButton() {
//		int folders = 0;
//		int files = 0;
//		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
//		FileDocument document;
//		for (int i = 0; i < checkedItems.size(); i++) {
//			if (checkedItems.valueAt(i) != true) {
//				continue;
//			}
//			int position = checkedItems.keyAt(i);
//			document = adapter.getDocumentAt(position);
//			if (document.getFile().isDirectory()) {
//				log(document.getFile().getAbsolutePath() + " of file");
//				folders++;
//			} else {
//				files++;
//			}
//		}
//		
//		if (files > 0 || folders > 0) {
//			String filesString = files + " " + getResources().getQuantityString(R.plurals.general_num_files, files);
//
//			String foldersString = folders + " " + getResources().getQuantityString(R.plurals.general_num_folders, folders);
//
//			String buttonText = getString(R.string.general_upload) + " ";
//
//			if (files == 0) {
//				buttonText += foldersString;
//			} else if (folders == 0) {
//				buttonText += filesString;
//			} else {
//				buttonText += foldersString + ", " + filesString;
//			}
//			button.setText(buttonText);
//			showButton();
//		} 
//		else {
//			hideButton();
//		}
	}

	public void itemClick(View view, int position) {
		log("on item click");
		
		if (megaContacts){
			MegaUser contact = (MegaUser) adapter.getDocumentAt(position);
			if(contact == null)
			{
				return;
			}
			ArrayList<String> emails = new ArrayList<String>();
			emails.add(contact.getEmail());
			setResultContacts(emails, megaContacts);
//			listView.setItemChecked(position, false);
		}
		else{
			PhoneContacts contact = (PhoneContacts) adapter.getDocumentAt(position);
			if(contact == null)
			{
				return;
			}
			
			ArrayList<String> contacts = new ArrayList<String>();
			if (contact.getEmail() != null){
				contacts.add(ContactsExplorerActivityLollipop.EXTRA_EMAIL);
				contacts.add(contact.getEmail());
			}
			else if (contact.getPhoneNumber() != null){
				contacts.add(ContactsExplorerActivityLollipop.EXTRA_PHONE);
				contacts.add(contact.getPhoneNumber());
			}
			setResultContacts(contacts, megaContacts);
//			listView.setItemChecked(position, false);
		}
	}
	
	/*
	 * Set selected files to pass to the caller activity and finish this
	 * activity
	 */
	private void setResultContacts(ArrayList<String> emails, boolean megaContacts) {
		Intent intent = new Intent();
		intent.putStringArrayListExtra(EXTRA_CONTACTS, emails);
		if(multipleSelectIntent==0){
			intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
			intent.putExtra("MULTISELECT", 0);
		}
		else if(multipleSelectIntent==1){
			intent.putExtra(EXTRA_NODE_HANDLE, nodeHandles);
			intent.putExtra("MULTISELECT", 1);
		}	
		
		if(sendToInbox==0){
			intent.putExtra("SEND_FILE",0);
		}
		else
		{
			intent.putExtra("SEND_FILE",1);
		}
		intent.putExtra(EXTRA_MEGA_CONTACTS, megaContacts);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	/*
	 * Count all selected items
	 */
	private int getCheckedItemCount() {
		int count = 0;
//		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
//		for (int i = 0; i < checkedItems.size(); i++) {
//			if (checkedItems.valueAt(i) == true) {
//				count++;
//			}
//		}
		return count;
	}
	
	public void onAddContactClick(){
		log("onAddcontactClick");
		String text = getString(R.string.context_new_contact_name);
		final EditText input = new EditText(this);
		input.setSingleLine();
		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
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
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});
		
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(
				this, getString(R.string.menu_add_contact),
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
	}
	
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		view.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) ContactsExplorerActivityLollipop.this.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}
	
	/*
	 * Add contact
	 */
	private void addContact(String emailContact) {
		log(emailContact + " of Contact");
		megaApi.addContact(emailContact, this); 
	}
	
	public static void log(String message) {
		Util.log("ContactsExplorerActivityLollipop", message);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		
		if (request.getType() == MegaRequest.TYPE_ADD_CONTACT){
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Contact added", Toast.LENGTH_LONG).show();
				log("add contact");
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		log("onUsersUpdate");
		
		contacts = megaApi.getContacts();
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}
		
		if (megaContacts){
			if (adapter == null){
				adapter = new ContactsExplorerLollipopAdapter(this, visibleContacts, null, megaContacts);
				listView.setAdapter(adapter);
			}
			else{
				adapter.setContacts(visibleContacts, null);
			}
		}
		
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		log("onNodesUpdate");
	}
	
	@Override
	public void onReloadNeeded(MegaApiJava api) {
		log("onReloadNeeded");
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

}
