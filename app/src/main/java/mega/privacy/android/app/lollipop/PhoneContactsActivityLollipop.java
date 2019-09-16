package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.PhoneContactsLollipopAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;


public class PhoneContactsActivityLollipop extends PinActivityLollipop implements PhoneContactsLollipopAdapter.OnItemCheckClickListener, MegaRequestListenerInterface {

	ActionBar aB;
	Toolbar tB;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;

	ImageView emptyImageView;
	TextView emptyTextView;
	private RecyclerView listView;
	LinearLayoutManager mLayoutManager;

	PhoneContactsLollipopAdapter adapter;
	RelativeLayout fragmentContainer;
	ArrayList<PhoneContactInfo> phoneContacts;

	@SuppressLint("InlinedApi")
	//Get the contacts explicitly added
	private ArrayList<PhoneContactInfo> getPhoneContacts() {
       ArrayList<PhoneContactInfo> contactList = new ArrayList<PhoneContactInfo>();

       try {
            ContentResolver cr = getBaseContext().getContentResolver();
            String SORT_ORDER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? Contacts.SORT_KEY_PRIMARY : Contacts.DISPLAY_NAME;
		   	String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''  AND " + Contacts.IN_VISIBLE_GROUP + "=1";
            Cursor c = cr.query(
					ContactsContract.Data.CONTENT_URI,
                    null,filter,
                    null, SORT_ORDER);

            while (c.moveToNext()){
            	long id = c.getLong(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                String name = c.getString(c.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
				String emailAddress = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
				LogUtil.logDebug("ID: " + id + "___ NAME: " + name + "____ EMAIL: " + emailAddress);

				if ((!emailAddress.equalsIgnoreCase("")) && (emailAddress.contains("@")) && (!emailAddress.contains("s.whatsapp.net"))) {
					LogUtil.logDebug("VALID Contact: "+ name + " ---> "+ emailAddress);
					PhoneContactInfo contactPhone = new PhoneContactInfo(id, name, emailAddress, null);
					contactList.add(contactPhone);
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

		if(megaApi==null||megaApi.getRootNode()==null){
			LogUtil.logDebug("Refresh session - sdk");
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
				LogUtil.logDebug("Refresh session - karere");
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return;
			}
		}

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

		aB.setTitle(getResources().getString(R.string.section_contacts));

		fragmentContainer = (RelativeLayout)  findViewById(R.id.fragment_container_contacts_explorer);
		listView = (RecyclerView) findViewById(R.id.contacts_explorer_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);

		emptyImageView = (ImageView) findViewById(R.id.contact_explorer_list_empty_image);
		emptyTextView = (TextView) findViewById(R.id.contact_explorer_list_empty_text);

		phoneContacts = getPhoneContacts();

		if (adapter == null){
			adapter = new PhoneContactsLollipopAdapter(this, phoneContacts);

			listView.setAdapter(adapter);

			adapter.SetOnItemClickListener(new PhoneContactsLollipopAdapter.OnItemClickListener() {

				@Override
				public void onItemClick(View view, int position) {
					itemClick(view, position);
				}
			});
		}
		else{
			adapter.setContacts(phoneContacts);
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
	}

//	@Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//		log("onCreateOptionsMenuLollipop");
//
//		// Inflate the menu items for use in the action bar
//	    MenuInflater inflater = getMenuInflater();
//	    inflater.inflate(R.menu.contacts_explorer_action, menu);
//
//	    addContactMenuItem = menu.findItem(R.id.cab_menu_add_contact);
//
//		if(sendToInbox==0){
//			addContactMenuItem.setVisible(true);
//		}
//		else{
//			addContactMenuItem.setVisible(false);
//		}
//
//	    return super.onCreateOptionsMenu(menu);
//	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
			case android.R.id.home: {
				finish();
				break;
			}
//			case R.id.cab_menu_add_contact:{
//				showNewContactDialog();
//        		break;
//			}
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * Validate email
	 */
	private String getEmailError(String value) {
		LogUtil.logDebug("getEmailError");
		if (value.length() == 0) {
			return getString(R.string.error_enter_email);
		}
		if (!Constants.EMAIL_ADDRESS.matcher(value).matches()) {
			return getString(R.string.error_invalid_email);
		}
		return null;
	}

	@Override
	public void onItemCheckClick(int position) {

	}

	public void itemClick(View view, int position) {
		LogUtil.logDebug("Position: " + position);

		final PhoneContactInfo contact = (PhoneContactInfo) adapter.getItem(position);
		if(contact == null)
		{
			return;
		}

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						inviteContact(contact.getEmail());
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		String message= getResources().getString(R.string.confirmation_add_contact,contact.getEmail());
		builder.setMessage(message).setPositiveButton(R.string.menu_add_contact, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void inviteContact(String email){
		LogUtil.logDebug("inviteContact");
		megaApi.inviteContact(email, null, MegaContactRequest.INVITE_ACTION_ADD, this);
	}

	void showAlert(String message) {
		AlertDialog.Builder bld = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		bld.setMessage(message);
		bld.setPositiveButton("OK",new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		LogUtil.logDebug("Showing alert dialog: " + message);
		bld.create().show();
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		LogUtil.logDebug("onRequestStart");
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		LogUtil.logDebug("onRequest finished: " + request.getEmail());
		if (e.getErrorCode() == MegaError.API_OK) {
			showAlert(getString(R.string.context_contact_request_sent, request.getEmail()));
		}
		else{
			if(e.getErrorCode()==MegaError.API_EEXIST)
			{
				showAlert(getString(R.string.context_contact_already_exists));
			}
			else{
				showAlert(getString(R.string.general_error));
			}
			LogUtil.logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
		LogUtil.logWarning("onRequestTemporaryError");
	}
}
