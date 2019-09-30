package mega.privacy.android.app.lollipop.megachat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaContactsAttachedLollipopAdapter;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ContactAttachmentBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ContactAttachmentActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, MegaChatRequestListenerInterface, OnClickListener {

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	ActionBar aB;
	Toolbar tB;
	ContactAttachmentActivityLollipop contactAttachmentActivityLollipop = this;
	public String selectedEmail;

	RelativeLayout container;
	RecyclerView listView;
	View separator;
	Button actionButton;
	Button cancelButton;
	LinearLayout optionsBar;
	LinearLayoutManager mLayoutManager;

	boolean inviteAction=false;

	ChatController cC;

	AndroidMegaChatMessage message = null;
	public long chatId;
	public long messageId;

	ArrayList<MegaContactDB> contacts;

	MegaContactsAttachedLollipopAdapter adapter;

	DisplayMetrics outMetrics;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate");
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		if (megaChatApi == null){
			megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
		}

		if(megaApi==null||megaApi.getRootNode()==null){
			logDebug("Refresh session - sdk");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
			logDebug("Refresh session - karere");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		cC = new ChatController(this);

		Intent intent = getIntent();
		if (intent != null) {
			chatId = intent.getLongExtra("chatId", -1);
			messageId = intent.getLongExtra("messageId", -1);
			logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
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
				long handle = message.getMessage().getUserHandle(i);
				logDebug("Contact Handle: " + handle);
				String handleString = megaApi.userHandleToBase64(handle);

				MegaContactDB contactDB = new MegaContactDB(handleString, email, name, "");
				contacts.add(contactDB);
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

		actionButton = (Button) findViewById(R.id.contact_attachment_chat_option_button);
		actionButton.setOnClickListener(this);

		//Check owner of the message
		if(message.getMessage().getUserHandle()==megaChatApi.getMyUserHandle()){
			logDebug("My message, show START CONVERSATION button");
			actionButton.setText(R.string.group_chat_start_conversation_label);
		}
		else{
			//Check if any contact is not my own contact

			for(int i=0; i<contacts.size();i++){
				MegaUser checkContact =  megaApi.getContact(contacts.get(i).getMail());
				if(checkContact==null){
					logDebug("NULL contact - The user " + contacts.get(i).getHandle() + " is NOT my CONTACT");
					inviteAction = true;
					break;
				}
				else{
					if(checkContact.getVisibility()!=MegaUser.VISIBILITY_VISIBLE){
						logDebug("The user " + checkContact.getHandle() + " is NOT my CONTACT");
						inviteAction = true;
						break;
					}
				}

			}

			if(inviteAction){
				logDebug("NOT my message, show INVITE button");
				actionButton.setText(R.string.menu_add_contact);
			}
			else{
				logDebug("NOT my message, show START CONVERSATION button");
				actionButton.setText(R.string.group_chat_start_conversation_label);
			}
		}

		cancelButton = (Button) findViewById(R.id.contact_attachment_chat_cancel_button);
		cancelButton.setOnClickListener(this);

		listView = (RecyclerView) findViewById(R.id.contact_attachment_chat_view_browser);
		listView.setClipToPadding(false);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
		mLayoutManager = new LinearLayoutManager(this);
		listView.setLayoutManager(mLayoutManager);
		listView.setItemAnimator(new DefaultItemAnimator());

		if (adapter == null){
			adapter = new MegaContactsAttachedLollipopAdapter(this, contacts, listView);
		}

		adapter.setPositionClicked(-1);
		adapter.setMultipleSelect(false);

		listView.setAdapter(adapter);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch(id) {
			case android.R.id.home: {
				finish();
			}
		}
		return true;
	}

	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	if(megaApi != null)
    	{
    		megaApi.removeRequestListener(this);
    	}
    }

	public void showOptionsPanel(String email){
		logDebug("showOptionsPanel");
		if(email!=null){
			this.selectedEmail = email;
			ContactAttachmentBottomSheetDialogFragment bottomSheetDialogFragment = new ContactAttachmentBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_SHARE) {
			logDebug("Share");
		}
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
		logDebug("onRequestFinish: " + request.getType() + "__" + request.getRequestString());

		if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
			logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());

			if(request.getNumber()== MegaContactRequest.INVITE_ACTION_REMIND){
				showSnackbar(getString(R.string.context_contact_invitation_resent));
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					logDebug("OK INVITE CONTACT: " + request.getEmail());
					if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD)
					{
						showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
					}
				}
				else{
					logError("Code: " + e.getErrorString());
					if(e.getErrorCode()==MegaError.API_EEXIST)
					{
						showSnackbar(getString(R.string.context_contact_already_invited, request.getEmail()));
					}
					else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode()==MegaError.API_EARGS)
					{
						showSnackbar(getString(R.string.error_own_email_as_contact));
					}
					else{
						showSnackbar(getString(R.string.general_error));
					}
					logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
				}
			}
		}
	}


	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError");
	}

	public void itemClick(int position) {
		logDebug("Position: " + position);

		MegaContactDB c = contacts.get(position);
		if(c!=null){
			MegaUser contact = megaApi.getContact(c.getMail());

			if(contact!=null) {
				if (contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
					Intent i = new Intent(this, ContactInfoActivityLollipop.class);
					i.putExtra("name", c.getMail());
					this.startActivity(i);
				}
				else{
					logDebug("The user is not contact");
					showSnackbar(getString(R.string.alert_user_is_not_contact));
				}
			}
			else{
				logError("The contact is null");
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){		
			case R.id.contact_attachment_chat_option_button:{
				logDebug("Click on ACTION button");

				if(inviteAction){
					ArrayList<String> contactEmails = new ArrayList<>();
					ContactController contactControllerC = new ContactController(this);
					for(int i=0;i<contacts.size();i++){
						MegaContactDB contact = contacts.get(i);
						MegaUser checkContact = megaApi.getContact(contact.getMail());
						if(checkContact==null){
							String userMail = contact.getMail();
							contactEmails.add(userMail);
						}
						else{
							if(checkContact.getVisibility()!=MegaUser.VISIBILITY_VISIBLE){
								String userMail = contact.getMail();
								contactEmails.add(userMail);
							}
						}

					}
					if(contactEmails!=null){
						if(!contactEmails.isEmpty()){
							contactControllerC.inviteMultipleContacts(contactEmails);
						}
					}
				}
				else{
					ArrayList<Long> contactHandles = new ArrayList<>();

					for(int i=0;i<contacts.size();i++){
						String handle = contacts.get(i).getHandle();
						long userHandle = megaApi.base64ToUserHandle(handle);
						contactHandles.add(userHandle);
					}

					startGroupConversation(contactHandles);
				}

				break;
			}
			case R.id.contact_attachment_chat_cancel_button: {
				logDebug("Click on Cancel button");
				finish();
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

	public void showSnackbar(String s){
		showSnackbar(container, s);
	}

	public void startConversation(long handle){
		logDebug("Handle: " + handle);
		MegaChatRoom chat = megaChatApi.getChatRoomByUser(handle);
		MegaChatPeerList peers = MegaChatPeerList.createInstance();
		if(chat==null){
			logDebug("No chat, create it!");
			peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
			megaChatApi.createChat(false, peers, this);
		}
		else{
			logDebug("There is already a chat, open it!");
			Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
			intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
			intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
			finish();
//			intentOpenChat.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			this.startActivity(intentOpenChat);
		}
	}

	public void startGroupConversation(ArrayList<Long> userHandles){
		logDebug("startGroupConversation");
		MegaChatPeerList peers = MegaChatPeerList.createInstance();

		for(int i=0;i<userHandles.size();i++){
			long handle = userHandles.get(i);
			peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
		}

		megaChatApi.createChat(false, peers, this);
	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
		logDebug("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		logDebug("onRequestFinish: " + request.getRequestString());

		if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
			logDebug("Create chat request finish!!!");
			if(e.getErrorCode()==MegaChatError.ERROR_OK) {
				logDebug("Open new chat");
				Intent intent = new Intent(this, ChatActivityLollipop.class);
				intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
				intent.putExtra("CHAT_ID", request.getChatHandle());
				finish();
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				this.startActivity(intent);

			}
			else{
				logError("ERROR WHEN CREATING CHAT " + e.getErrorString());
				showSnackbar(getString(R.string.create_chat_error));
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}
}

