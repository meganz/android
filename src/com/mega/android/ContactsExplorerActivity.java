package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mega.android.ContactsExplorerAdapter.OnItemCheckClickListener;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaUser;
import com.mega.sdk.UserList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ContactsExplorerActivity extends PinActivity implements OnClickListener, OnItemClickListener, OnItemCheckClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface {
	
	public static String ACTION_PICK_CONTACT_SHARE_FOLDER = "ACTION_PICK_CONTACT_SHARE_FOLDER";
	
	public static String EXTRA_NODE_HANDLE = "node_handle";
	public static String EXTRA_CONTACTS = "extra_contacts";
	
	MegaApiAndroid megaApi;
	
//	public static String EXTRA_URL = "fileurl";
//	public static String EXTRA_SIZE = "filesize";
//	public static String EXTRA_DOCUMENT_HASHES = "document_hash";
//	public static String EXTRA_BUTTON_PREFIX = "button_prefix";
//	public static String EXTRA_PATH = "filepath";
//	public static String EXTRA_FILES = "fileslist";
	
//	private File path;
//	private File root;
	
//	private String buttonPrefix;
	
	private TextView windowTitle;
	private Button button;
	private ListView listView;
	private ImageButton addContactButton;
	
	long nodeHandle = -1;
	
//	private String url;
//	private long size;
//	private long[] documentHashes;
	
//	FileStorageAdapter adapter;
	
	ContactsExplorerAdapter adapter;
	
	UserList contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	private AlertDialog addContactDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			MegaApplication app = (MegaApplication)getApplication();
			megaApi = app.getMegaApi();
		}			
		
		megaApi.addGlobalListener(this);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_contactsexplorer);
		
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(EXTRA_NODE_HANDLE)) {
				nodeHandle = savedInstanceState.getLong(EXTRA_NODE_HANDLE);
			}
		}
		
		windowTitle = (TextView) findViewById(R.id.contacts_explorer_window_title);
		windowTitle.setText(getString(R.string.section_contacts));
		listView = (ListView) findViewById(R.id.contacts_explorer_list_view);
		button = (Button) findViewById(R.id.contacts_explorer_button);
		button.setVisibility(View.GONE);
		addContactButton = (ImageButton) findViewById(R.id.contacts_explorer_add_contact);
		
		Intent intent = getIntent();
		nodeHandle =  intent.getLongExtra(EXTRA_NODE_HANDLE, -1);
		
		button.setOnClickListener(this);
		addContactButton.setOnClickListener(this);
		
		contacts = megaApi.getContacts();
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}
		
		adapter = new ContactsExplorerAdapter(this, visibleContacts);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);

		adapter.setOnItemCheckClickListener(this);
	}
	
	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	if (megaApi != null){
    		megaApi.removeGlobalListener(this);
    	}
	}
	
	@Override
	public void onSaveInstanceState(Bundle state) {
		state.putLong(EXTRA_NODE_HANDLE, nodeHandle);
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
		}
		
	}

	@Override
	public void onItemCheckClick(int position) {
		boolean isChecked = listView.isItemChecked(position);
		listView.setItemChecked(position, !isChecked);
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

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		log("on item click");
		
		MegaUser contact = adapter.getDocumentAt(position);
		ArrayList<String> emails = new ArrayList<String>();
		emails.add(contact.getEmail());
		setResultContacts(emails);
		listView.setItemChecked(position, false);
	}
	
	/*
	 * Set selected files to pass to the caller activity and finish this
	 * activity
	 */
	private void setResultContacts(ArrayList<String> emails) {
		Intent intent = new Intent();
		intent.putStringArrayListExtra(EXTRA_CONTACTS, emails);
		intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	/*
	 * Count all selected items
	 */
	private int getCheckedItemCount() {
		int count = 0;
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				count++;
			}
		}
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
				InputMethodManager imm = (InputMethodManager) ContactsExplorerActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
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
		Util.log("ContactsExplorerActivity", message);
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
	public void onUsersUpdate(MegaApiJava api) {
		log("onUsersUpdate");
		
		contacts = megaApi.getContacts();
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}
		
		if (adapter == null){
			adapter = new ContactsExplorerAdapter(this, visibleContacts);
			listView.setAdapter(adapter);
		}
		else{
			adapter.setContacts(visibleContacts);
		}
		
	}

	@Override
	public void onNodesUpdate(MegaApiJava api) {
		log("onNodesUpdate");
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		log("onReloadNeeded");
	}

}
