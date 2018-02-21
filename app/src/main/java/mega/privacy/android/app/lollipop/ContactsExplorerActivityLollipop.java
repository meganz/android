package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
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


public class ContactsExplorerActivityLollipop extends PinActivityLollipop implements ContactsExplorerLollipopAdapter.OnItemCheckClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface {
	
	public static String ACTION_PICK_CONTACT_SHARE_FOLDER = "ACTION_PICK_CONTACT_SHARE_FOLDER";
	public static String ACTION_PICK_CONTACT_SEND_FILE = "ACTION_PICK_CONTACT_SEND_FILE";
	
	public static String EXTRA_NODE_HANDLE = "node_handle";
	public static String EXTRA_MEGA_CONTACTS = "mega_contacts";
	public static String EXTRA_CONTACTS = "extra_contacts";
	public static String EXTRA_EMAIL = "extra_email";
	public static String EXTRA_PHONE = "extra_phone";
	
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	Handler handler;
	
	ActionBar aB;
	Toolbar tB;
	
	MenuItem addContactMenuItem;
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	ImageView emptyImageView;
	TextView emptyTextView;
	int multipleSelectIntent;
	int sendToInbox;
//	private TextView windowTitle;
//	private Button button;
	private RecyclerView listView;
	LinearLayoutManager mLayoutManager;
//	private ImageButton addContactButton;
//	private ImageButton megaPhoneContacts;
	
	long nodeHandle = -1;
	long[] nodeHandles;
	
	ContactsExplorerLollipopAdapter adapter;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	private AlertDialog addContactDialog;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	        // do nothing
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	} 	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			MegaApplication app = (MegaApplication)getApplication();
			megaApi = app.getMegaApi();
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
		
		setContentView(R.layout.activity_contactsexplorer);
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_contacts_explorer);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
//		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		handler = new Handler();
		
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
		aB.setTitle(getResources().getString(R.string.section_contacts));
		
		listView = (RecyclerView) findViewById(R.id.contacts_explorer_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);		
		
		emptyImageView = (ImageView) findViewById(R.id.contact_explorer_list_empty_image);
		emptyTextView = (TextView) findViewById(R.id.contact_explorer_list_empty_text);
			
		contacts = megaApi.getContacts();
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				log("VISIBLECONTACTS: " + contacts.get(i).getEmail());
				visibleContacts.add(contacts.get(i));
			}
		}
		
//		if (visibleContacts.size() == 0){
//			Toast.makeText(this, getString(R.string.no_contacts), Toast.LENGTH_SHORT).show();
//			finish();
//		}
		
		if (adapter == null){
			adapter = new ContactsExplorerLollipopAdapter(this, visibleContacts);
			
			listView.setAdapter(adapter);
			
			adapter.SetOnItemClickListener(new ContactsExplorerLollipopAdapter.OnItemClickListener() {
				
				@Override
				public void onItemClick(View view, int position) {
					itemClick(view, position);
				}
			});
		}
		else{
			adapter.setContacts(visibleContacts);
		}			
		
		if (adapter.getItemCount() == 0){				
			listView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
		}
		else{
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
		
		Intent intent = getIntent();
		
		multipleSelectIntent = intent.getIntExtra("MULTISELECT", -1);
		if(multipleSelectIntent==0){
			nodeHandle =  intent.getLongExtra(EXTRA_NODE_HANDLE, -1);
		}
		else if(multipleSelectIntent==1){
			log("onCreate multiselect YES!");
			nodeHandles=intent.getLongArrayExtra(EXTRA_NODE_HANDLE);
		}
		sendToInbox= intent.getIntExtra("SEND_FILE", -1);

		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.contacts_explorer_action, menu);
	    
	    addContactMenuItem = menu.findItem(R.id.cab_menu_add_contact);	   
	    
		if(sendToInbox==0){
			addContactMenuItem.setVisible(true);
		}
		else{
			addContactMenuItem.setVisible(false);
		}
  
	    return super.onCreateOptionsMenu(menu);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
			case android.R.id.home: {
				finish();
				break;
			}
			case R.id.cab_menu_add_contact:{
				showNewContactDialog(); 
        		break;
			}
		}
		return super.onOptionsItemSelected(item);
	}
	
	@SuppressLint("NewApi")
	public void showNewContactDialog(){
		log("showNewContactDialog");		
		
		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);
		
		final EditText input = new EditText(this);
	    layout.addView(input, params);
	    
//		input.setId(1);
		input.setSingleLine();
		input.setHint(getString(R.string.context_new_contact_name));
		input.setTextColor(getResources().getColor(R.color.text_secondary));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = input.getText().toString().trim();
					String emailError = getEmailError(value);
					if (emailError != null) {
						input.setError(emailError);
						input.requestFocus();
					} else {						
						ArrayList<String> emails = new ArrayList<String>();
						emails.add(value);
						setResultContacts(emails, true);
						addContactDialog.dismiss();
					}				
				}
				else{
					log("other IME" + actionId);
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_add),EditorInfo.IME_ACTION_DONE);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.menu_add_contact_and_share));
		builder.setPositiveButton(getString(R.string.general_add),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
														
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);
		addContactDialog = builder.create();
		addContactDialog.show();
		addContactDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String value = input.getText().toString().trim();
				if(value!=null){
					String emailError = getEmailError(value);
					if (emailError != null) {
						input.setError(emailError);
					} else {
						ArrayList<String> emails = new ArrayList<String>();
						emails.add(value);
						log("The user is: "+value);
						setResultContacts(emails, true);
						addContactDialog.dismiss();
					}
				}				
			}
		});
	}
	
	/*
	 * Validate email
	 */
	private String getEmailError(String value) {
		log("getEmailError");
		if (value.length() == 0) {
			return getString(R.string.error_enter_email);
		}
		if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
			return getString(R.string.error_invalid_email);
		}
		return null;
	}
	
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		log("showKeyboardDelayed");
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
//				if (fbFLol != null){
//					if (!(drawerItem == DrawerItem.CLOUD_DRIVE)){
//						return;
//					}
//				}
//				String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);		
//				cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
//				if (cFLol != null){
//					if (drawerItem == DrawerItem.CONTACTS){
//						return;
//					}
//				}
//				if (inSFLol != null){
//					if (drawerItem == DrawerItem.SHARED_ITEMS){
//						return;
//					}
//				}
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
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


	@Override
	public void onItemCheckClick(int position) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}

	public void itemClick(View view, int position) {
		log("on item click");
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		MegaUser contact = (MegaUser) adapter.getDocumentAt(position);
		if(contact == null)
		{
			return;
		}
		ArrayList<String> emails = new ArrayList<String>();
		emails.add(contact.getEmail());
		setResultContacts(emails, true);		
	}
	
	/*
	 * Set selected files to pass to the caller activity and finish this
	 * activity
	 */
	private void setResultContacts(ArrayList<String> emails, boolean megaContacts) {
//		log("setResultContacts");
		Intent intent = new Intent();
		intent.putStringArrayListExtra(EXTRA_CONTACTS, emails);
		if(emails!=null){
			for(int i=0; i<emails.size();i++){
				log("setResultContacts: "+emails.get(i));
			}
		}
		
		if(multipleSelectIntent==0){
			log("multiselectIntent == 0");
			intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
			intent.putExtra("MULTISELECT", 0);
		}
		else if(multipleSelectIntent==1){
			log("multiselectIntent == 1");
			if(nodeHandles!=null){
				log("number of items selected: "+nodeHandles.length);
			}
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
	 * Add contact - remove (now with invitation)
	 */
//	private void addContact(String emailContact) {
//		log(emailContact + " of Contact");
//		megaApi.inviteContact(emailContact, this); 
//	}
//	
//	public void onAddContactClick(){
//		log("onAddcontactClick");
//		String text = getString(R.string.context_new_contact_name);
//		final EditText input = new EditText(this);
//		input.setSingleLine();
//		input.setSelectAllOnFocus(true);
//		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
//		input.setOnEditorActionListener(new OnEditorActionListener() {
//			@Override
//			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//				if (actionId == EditorInfo.IME_ACTION_DONE) {
//					String value = v.getText().toString().trim();
//					if (value.length() == 0) {
//						return true;
//					}
//					addContact(value);
//					addContactDialog.dismiss();
//					return true;
//				}
//				return false;
//			}
//		});
//		
//		input.setImeActionLabel(getString(R.string.general_add),
//				KeyEvent.KEYCODE_ENTER);
//		input.setText(text);
//		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//			@Override
//			public void onFocusChange(View v, boolean hasFocus) {
//				if (hasFocus) {
//					showKeyboardDelayed(v);
//				}
//			}
//		});
//		
//		AlertDialog.Builder builder = Util.getCustomAlertBuilder(
//				this, getString(R.string.menu_add_contact),
//				null, input);
//		builder.setPositiveButton(getString(R.string.general_add),
//				new DialogInterface.OnClickListener() {
//					public void onClick(DialogInterface dialog, int whichButton) {
//						String value = input.getText().toString().trim();
//						if (value.length() == 0) {
//							return;
//						}
//						addContact(value);
//					}
//				});
//		
//		builder.setNegativeButton(getString(android.R.string.cancel), null);
//		addContactDialog = builder.create();
//		addContactDialog.show();
//	}
//	
//	/*
//	 * Display keyboard
//	 */
//	private void showKeyboardDelayed(final View view) {
//		view.postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				InputMethodManager imm = (InputMethodManager) ContactsExplorerActivityLollipop.this.getSystemService(Context.INPUT_METHOD_SERVICE);
//				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
//			}
//		}, 50);
//	}	
	
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

		if (adapter == null){
			adapter = new ContactsExplorerLollipopAdapter(this, visibleContacts);
			listView.setAdapter(adapter);
		}
		else{
			adapter.setContacts(visibleContacts);
		}
		
	}

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

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
