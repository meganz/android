package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Util.context;


@SuppressLint("NewApi")
public class ContactInfoActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, OnClickListener, MegaRequestListenerInterface, OnItemClickListener {

	ContactController cC;

	public static int MAX_WIDTH_FILENAME_LAND=450;
	public static int MAX_WIDTH_FILENAME_PORT=170;
	public static int MAX_WIDTH_APPBAR_LAND=250;
	public static int MAX_WIDTH_APPBAR_PORT=350;


	RelativeLayout imageLayout;
	android.app.AlertDialog permissionsDialog;
	ProgressDialog statusDialog;

	ContactInfoActivityLollipop contactInfoActivityLollipop;
	CoordinatorLayout fragmentContainer;
	CollapsingToolbarLayout collapsingToolbar;

	View imageGradient;
	ImageView contactPropertiesImage;
	LinearLayout optionsLayout;
	LinearLayout notificationsLayout;
	SwitchCompat notificationsSwitch;
	TextView notificationsTitle;
	View dividerNotificationsLayout;

	ChatSettings chatSettings = null;
	ChatItemPreferences chatPrefs = null;
	boolean generalChatNotifications = true;

	RelativeLayout sharedFoldersLayout;
	ImageView sharedFoldersIcon;
	TextView sharedFoldersText;
	Button sharedFoldersButton;
	View dividerSharedFoldersLayout;

	//RelativeLayout shareContactLayout;
	//RelativeLayout shareContactContentLayout;
	//TextView shareContactText;
	//ImageView shareContactIcon;

	TextView emailContact;
	TextView nameContact;
	ImageView contactStateIcon;

	TextView nameLength;
	TextView emailLength;

	//View dividerShareContactLayout;

	RelativeLayout clearChatLayout;
	View dividerClearChatLayout;
	RelativeLayout removeContactChatLayout;


	Toolbar toolbar;
	ActionBar aB;
	AppBarLayout appBarLayout;

	MegaUser user;
	long chatHandle;
	String userEmailExtra;
	MegaChatRoom chat;

	private MegaApiAndroid megaApi = null;
	MegaChatApiAndroid megaChatApi = null;

	boolean fromContacts = true;

	private Handler handler;

	Display display;
	DisplayMetrics outMetrics;
	float density;
	float scaleW;
	float scaleH;

	DatabaseHandler dbH = null;

	MenuItem shareMenuItem;
	MenuItem viewFoldersMenuItem;
	MenuItem startConversationMenuItem;

	private void setAppBarOffset(int offsetPx){
		CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
		AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
		behavior.onNestedPreScroll(fragmentContainer, appBarLayout, null, 0, offsetPx, new int[]{0, 0});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		log("onCreate");
		contactInfoActivityLollipop = this;
		if (megaApi == null) {
			MegaApplication app = (MegaApplication) getApplication();
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

		handler = new Handler();
		cC = new ContactController(this);

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = getResources().getDisplayMetrics().density;

		scaleW = Util.getScaleW(outMetrics, density);
		scaleH = Util.getScaleH(outMetrics, density);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {

			setContentView(R.layout.activity_chat_contact_properties);
			fragmentContainer = (CoordinatorLayout) findViewById(R.id.fragment_container);
			toolbar = (Toolbar) findViewById(R.id.toolbar);
			appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
			setSupportActionBar(toolbar);
			aB = getSupportActionBar();


			imageLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_image_layout);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				Window window = this.getWindow();
				window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				window.setStatusBarColor(ContextCompat.getColor(this, R.color.transparent_black));
			}

			collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapse_toolbar);
			contactStateIcon = (ImageView) findViewById(R.id.contact_drawable_state);

			/*TITLE*/
			nameContact = (TextView) findViewById(R.id.name_contact);
			nameLength = (TextView) findViewById(R.id.name_length);

			/*SUBTITLE*/
			emailContact = (TextView) findViewById(R.id.email_contact);
			emailLength =(TextView) findViewById(R.id.email_length);

			if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				log("Landscape configuration");

				CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
				params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_APPBAR_LAND, context.getResources().getDisplayMetrics());
				appBarLayout.setLayoutParams(params);

				float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, getResources().getDisplayMetrics());
				nameContact.setMaxWidth((int) width);
				nameLength.setMaxWidth((int) width);
				emailContact.setMaxWidth((int) width);
				emailLength.setMaxWidth((int) width);

				emailContact.setPadding(0,0,0,5);
				emailLength.setPadding(0,0,0,5);

			}
			else{
				log("Portrait configuration");

				CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
				params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_APPBAR_PORT, context.getResources().getDisplayMetrics());
				appBarLayout.setLayoutParams(params);

				float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, getResources().getDisplayMetrics());
				nameContact.setMaxWidth((int) width);
				nameLength.setMaxWidth((int) width);
				emailContact.setMaxWidth((int) width);
				emailLength.setMaxWidth((int) width);

				emailContact.setPadding(0,0,0,11);
				emailLength.setPadding(0,0,0,11);
			}

			imageGradient = (View) findViewById(R.id.gradient_view);

			setTitle(null);
			aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
			aB.setHomeButtonEnabled(true);
			aB.setDisplayHomeAsUpEnabled(true);

			contactPropertiesImage = (ImageView) findViewById(R.id.chat_contact_properties_toolbar_image);

			dbH = DatabaseHandler.getDbHandler(getApplicationContext());

			appBarLayout.post(new Runnable() {
				@Override
				public void run() {
					setAppBarOffset(50);
				}
			});

			//OPTIONS LAYOUT
			optionsLayout = (LinearLayout) findViewById(R.id.chat_contact_properties_options);

			//Notifications Layout

			notificationsLayout = (LinearLayout) findViewById(R.id.chat_contact_properties_notifications_layout);
			notificationsLayout.setVisibility(View.VISIBLE);

			notificationsTitle = (TextView) findViewById(R.id.chat_contact_properties_notifications_text);

			notificationsSwitch = (SwitchCompat) findViewById(R.id.chat_contact_properties_switch);
			notificationsSwitch.setOnClickListener(this);

			dividerNotificationsLayout = (View) findViewById(R.id.divider_notifications_layout);

			//Shared folders layout
			sharedFoldersLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_shared_folders_layout);
			sharedFoldersLayout.setOnClickListener(this);

			sharedFoldersIcon = (ImageView) findViewById(R.id.chat_contact_properties_shared_folder_icon);

			sharedFoldersText = (TextView) findViewById(R.id.chat_contact_properties_shared_folders_label);

			sharedFoldersButton = (Button) findViewById(R.id.chat_contact_properties_shared_folders_button);
			sharedFoldersButton.setOnClickListener(this);

			dividerSharedFoldersLayout = (View) findViewById(R.id.divider_shared_folder_layout);

			//Share Contact Layout

			//shareContactLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_share_contact_layout);
			//shareContactLayout.setOnClickListener(this);

			//shareContactIcon = (ImageView) findViewById(R.id.chat_contact_properties_email_icon);

			//shareContactContentLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_share_contact_content);

			//shareContactText = (TextView) findViewById(R.id.chat_contact_properties_share_contact);

			//	dividerShareContactLayout = (View) findViewById(R.id.divider_share_contact_layout);

			//Clear chat Layout
			clearChatLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_clear_layout);
			clearChatLayout.setOnClickListener(this);

			dividerClearChatLayout = (View) findViewById(R.id.divider_clear_chat_layout);

			//Remove contact Layout
			removeContactChatLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_remove_contact_layout);
			removeContactChatLayout.setOnClickListener(this);

			chatHandle = extras.getLong("handle",-1);
			userEmailExtra = extras.getString("name");
			if (chatHandle != -1) {

				log("From chat!!");
				fromContacts = false;
				chat = megaChatApi.getChatRoom(chatHandle);

				long userHandle = chat.getPeerHandle(0);

				String userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle);
				user = megaApi.getContact(userHandleEncoded);
				if(user!=null){
					log("User foundd!!!");
				}

				chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chatHandle));

				if (chat.getTitle() != null && !chat.getTitle().isEmpty() && !chat.getTitle().equals("")){
					nameContact.setText(chat.getTitle());
					nameLength.setText(chat.getTitle());
				}
				else {
					if (userEmailExtra != null) {

						nameContact.setText(userEmailExtra);
						nameLength.setText(userEmailExtra);
					}
				}
				String fullname = (String)nameContact.getText();
				setDefaultAvatar(fullname);
			}
			else{
				log("From contacts!!");

				fromContacts = true;
				user = megaApi.getContact(userEmailExtra);

				String fullName = "";
				if(user!=null){
					log("User handle: "+user.getHandle());
					MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
					if(contactDB!=null){
						log("Contact DB found!");
						String firstNameText = "";
						String lastNameText = "";

						firstNameText = contactDB.getName();
						lastNameText = contactDB.getLastName();

						if (firstNameText.trim().length() <= 0){
							fullName = lastNameText;
						}
						else{
							fullName = firstNameText + " " + lastNameText;
						}

						if (fullName.trim().length() <= 0){
							log("Put email as fullname");
							fullName= user.getEmail();
						}

						nameContact.setText(fullName);
						nameLength.setText(fullName);
					}
					else{
						log("The contactDB is null: ");
					}
				}

				//Find chat with this contact
				if(Util.isChatEnabled()){
					chat = megaChatApi.getChatRoomByUser(user.getHandle());

					if(chat!=null){
						chatHandle = chat.getChatId();
						if(chatHandle==-1){
							notificationsLayout.setVisibility(View.GONE);
							dividerNotificationsLayout.setVisibility(View.GONE);
						}
						else{
							chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chatHandle));
						}
					}
					else{
						notificationsLayout.setVisibility(View.GONE);
						dividerNotificationsLayout.setVisibility(View.GONE);
					}

					if (megaChatApi == null){
						megaChatApi = ((MegaApplication) ((Activity)this).getApplication()).getMegaChatApi();
					}

				}
				setDefaultAvatar(fullName);
			}

			 if(Util.isChatEnabled()){
				 contactStateIcon.setVisibility(View.VISIBLE);
			 	if (megaChatApi != null){
			 		int userStatus = megaChatApi.getUserOnlineStatus(user.getHandle());
			 		if(userStatus == MegaChatApi.STATUS_ONLINE){
			 			log("This user is connected");
						contactStateIcon.setVisibility(View.VISIBLE);
			 			contactStateIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_online));
			 		}else if(userStatus == MegaChatApi.STATUS_AWAY){
			 			log("This user is away");
						contactStateIcon.setVisibility(View.VISIBLE);
			 			contactStateIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_away));
			 		} else if(userStatus == MegaChatApi.STATUS_BUSY){
			 			log("This user is busy");
						contactStateIcon.setVisibility(View.VISIBLE);
			 			contactStateIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_busy));
			 		}
			 		else if(userStatus == MegaChatApi.STATUS_OFFLINE){
						log("This user is offline");
						contactStateIcon.setVisibility(View.VISIBLE);
						contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_offline));
					}
					else if(userStatus == MegaChatApi.STATUS_INVALID){
						log("INVALID status: "+userStatus);
						contactStateIcon.setVisibility(View.GONE);
					}
					else{
						log("This user status is: "+userStatus);
						contactStateIcon.setVisibility(View.GONE);
					}
			 	}
			 } else{
			 	contactStateIcon.setVisibility(View.GONE);
			 }

			if(Util.isOnline(this)){
				log("online -- network connection");
				setAvatar();

				if(user!=null){
					sharedFoldersLayout.setVisibility(View.VISIBLE);
					dividerSharedFoldersLayout.setVisibility(View.VISIBLE);

					sharedFoldersButton.setText(getDescription(megaApi.getInShares(user)));
					//shareContactText.setText(user.getEmail());
					emailContact.setText(user.getEmail());
					emailLength.setText(user.getEmail());

					if(Util.isChatEnabled()){
						if(chat!=null){
							clearChatLayout.setVisibility(View.VISIBLE);
							dividerClearChatLayout.setVisibility(View.VISIBLE);
							//dividerShareContactLayout.setVisibility(View.VISIBLE);
						}
						else{
							clearChatLayout.setVisibility(View.GONE);
							dividerClearChatLayout.setVisibility(View.GONE);
							//dividerShareContactLayout.setVisibility(View.GONE);
						}
					}
					else{
						clearChatLayout.setVisibility(View.GONE);
						dividerClearChatLayout.setVisibility(View.GONE);
						//dividerShareContactLayout.setVisibility(View.GONE);
					}
				}
				else{
					sharedFoldersLayout.setVisibility(View.GONE);
					dividerSharedFoldersLayout.setVisibility(View.GONE);

					if(Util.isChatEnabled()){
						if(chat!=null){
							//shareContactText.setText(chat.getPeerEmail(0));
							emailContact.setText(chat.getPeerEmail(0));
							emailLength.setText(chat.getPeerEmail(0));

							clearChatLayout.setVisibility(View.VISIBLE);
							dividerClearChatLayout.setVisibility(View.VISIBLE);
							//dividerShareContactLayout.setVisibility(View.VISIBLE);
						}
						else{
							clearChatLayout.setVisibility(View.GONE);
							dividerClearChatLayout.setVisibility(View.GONE);
							//dividerShareContactLayout.setVisibility(View.GONE);
						}
					}
					else{
						clearChatLayout.setVisibility(View.GONE);
						dividerClearChatLayout.setVisibility(View.GONE);
						//dividerShareContactLayout.setVisibility(View.GONE);
					}
				}
			}
			else{
				log("OFFLINE -- NO network connection");
				if(chat!=null){
					String userEmail = chat.getPeerEmail(0);
					setOfflineAvatar(userEmail);
				//	shareContactText.setText(userEmail);
					emailContact.setText(userEmail);
					emailLength.setText(userEmail);
				}
				sharedFoldersLayout.setVisibility(View.GONE);
				dividerSharedFoldersLayout.setVisibility(View.GONE);
				clearChatLayout.setVisibility(View.GONE);
				dividerClearChatLayout.setVisibility(View.GONE);

				//dividerShareContactLayout.setVisibility(View.GONE);
			}

			((MegaApplication) getApplication()).sendSignalPresenceActivity();

			if(Util.isChatEnabled()){

				chatSettings = dbH.getChatSettings();
				if(chatSettings==null){
					log("Chat settings null - notifications ON");
					setUpIndividualChatNotifications();
				}
				else {
					log("There is chat settings");
					if (chatSettings.getNotificationsEnabled() == null) {
						generalChatNotifications = true;

					} else {
						generalChatNotifications = Boolean.parseBoolean(chatSettings.getNotificationsEnabled());

					}

					if (generalChatNotifications) {
						setUpIndividualChatNotifications();

					} else {
						log("General notifications OFF");
						boolean notificationsEnabled = false;
						notificationsSwitch.setChecked(notificationsEnabled);

						notificationsLayout.setVisibility(View.VISIBLE);
						dividerNotificationsLayout.setVisibility(View.VISIBLE);
					}
				}

			}else{

				notificationsLayout.setVisibility(View.GONE);
				dividerNotificationsLayout.setVisibility(View.GONE);
			}

		} else {
			log("Extras is NULL");
		}
	}

	public void setUpIndividualChatNotifications(){
		log("setUpIndividualChatNotifications");
		//SET Preferences (if exist)
		if(chatPrefs!=null){
			log("There is individual chat preferences");

			boolean notificationsEnabled = true;
			if (chatPrefs.getNotificationsEnabled() != null){
				notificationsEnabled = Boolean.parseBoolean(chatPrefs.getNotificationsEnabled());
			}
			notificationsSwitch.setChecked(notificationsEnabled);
		}
		else{
			log("NO individual chat preferences");
			notificationsSwitch.setChecked(true);
		}

		notificationsLayout.setVisibility(View.VISIBLE);
		dividerNotificationsLayout.setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contact_properties_action, menu);

		shareMenuItem = menu.findItem(R.id.cab_menu_share_folder);
		//viewFoldersMenuItem = menu.findItem(R.id.cab_menu_view_shares);
		startConversationMenuItem = menu.findItem(R.id.cab_menu_start_conversation);

		if(Util.isOnline(this)){
			ArrayList<MegaNode> shares = megaApi.getInShares(user);
			if(shares!=null){
				if(shares.size()>0){
					//viewFoldersMenuItem.setVisible(true);
				}
				else{
					//viewFoldersMenuItem.setVisible(false);
				}
			}
			else{
				//viewFoldersMenuItem.setVisible(false);
			}

			if(Util.isChatEnabled()){
				if(fromContacts){
					startConversationMenuItem.setVisible(true);
				}
				else{
					startConversationMenuItem.setVisible(false);
				}
			}
			else{
				startConversationMenuItem.setVisible(false);
			}

		}
		else{
			log("Hide all - no network connection");
			shareMenuItem.setVisible(false);
			//viewFoldersMenuItem.setVisible(false);
			startConversationMenuItem.setVisible(false);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");

		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		int id = item.getItemId();
		switch(id){
			case android.R.id.home:{
				finish();
				break;
			}
			case R.id.cab_menu_share_folder:{
				pickFolderToShare(user.getEmail());
				break;
			}
			/*case R.id.cab_menu_view_shares:{
				if(!Util.isOnline(this)){

					showSnackbar(getString(R.string.error_server_connection_problem));
					return true;
				}

				Intent i = new Intent(this, ContactFileListActivityLollipop.class);
				i.putExtra("name", user.getEmail());
				this.startActivity(i);
				break;
			}*/
			case R.id.cab_menu_start_conversation:{

				if(!Util.isOnline(this)){

					showSnackbar(getString(R.string.error_server_connection_problem));
					return true;
				}

				if(user!=null){
					MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
					if(chat==null){
						log("No chat, create it!");
						MegaChatPeerList peers = MegaChatPeerList.createInstance();
						peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
						megaChatApi.createChat(false, peers, this);
					}
					else{
						log("There is already a chat, open it!");
						if(fromContacts){
							Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
							intentOpenChat.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
							intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
							this.startActivity(intentOpenChat);
						}
						else{
							Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
							intentOpenChat.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
							intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
							intentOpenChat.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							this.startActivity(intentOpenChat);
						}
					}
				}
				break;
			}
		}
		return true;
	}

	public void pickFolderToShare(String email){
		log("pickFolderToShare");
//		MegaUser user = megaApi.getContact(email);
		if (email != null){
			Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
			intent.setAction(FileExplorerActivityLollipop.ACTION_SELECT_FOLDER_TO_SHARE);
			ArrayList<String> contacts = new ArrayList<String>();
//			String[] longArray = new String[1];
//			longArray[0] = email;
			contacts.add(email);
			intent.putExtra("SELECTED_CONTACTS", contacts);
			startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_FOLDER);
		}
		else{
			showSnackbar(getString(R.string.error_sharing_folder));
			log("Error sharing folder");
		}
	}


	public String getDescription(ArrayList<MegaNode> nodes){
		int numFolders = 0;
		int numFiles = 0;

		for (int i=0;i<nodes.size();i++){
			MegaNode c = nodes.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}

		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFolders).toUpperCase(Locale.getDefault());
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + getResources().getQuantityString(R.plurals.general_num_folders, numFiles).toUpperCase(Locale.getDefault());
			}
		}
		else {
			if (numFiles == 0){
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFolders).toUpperCase(Locale.getDefault());
			}
			else{
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFiles).toUpperCase(Locale.getDefault());
			}
		}

		return info;
	}

	public void setAvatar() {
		log("setAvatar");
		File avatar = null;
		if (getExternalCacheDir() != null) {
			avatar = new File(getExternalCacheDir().getAbsolutePath(), user.getEmail() + ".jpg");
		} else {
			avatar = new File(getCacheDir().getAbsolutePath(), user.getEmail() + ".jpg");
		}

		if (avatar != null) {
			setProfileAvatar(avatar);
		}
	}

	public void setOfflineAvatar(String email) {
		log("setOfflineAvatar");
		File avatar = null;
		if (getExternalCacheDir() != null) {
			avatar = new File(getExternalCacheDir().getAbsolutePath(), email + ".jpg");
		} else {
			avatar = new File(getCacheDir().getAbsolutePath(), email + ".jpg");
		}

		if (avatar != null) {
			Bitmap imBitmap = null;
			if (avatar.exists()) {
				if (avatar.length() > 0) {
					BitmapFactory.Options bOpts = new BitmapFactory.Options();
					bOpts.inPurgeable = true;
					bOpts.inInputShareable = true;
					imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
					if (imBitmap != null) {
						contactPropertiesImage.setImageBitmap(imBitmap);
						imageGradient.setVisibility(View.VISIBLE);

						if (imBitmap != null && !imBitmap.isRecycled()) {
//						Palette palette = Palette.from(imBitmap).generate();
//						int colorBackground = palette.getDarkMutedColor(ContextCompat.getColor(this, R.color.black));
							int colorBackground = getDominantColor1(imBitmap);
							imageLayout.setBackgroundColor(colorBackground);
						}
					}
				}
			}
		}
	}

	public void setProfileAvatar(File avatar) {
		log("setProfileAvatar");
		Bitmap imBitmap = null;
		if (avatar.exists()) {
			if (avatar.length() > 0) {
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (imBitmap == null) {
					avatar.delete();
					if (getExternalCacheDir() != null) {
						megaApi.getUserAvatar(user, getExternalCacheDir().getAbsolutePath() + "/" + user.getEmail(), this);
					} else {
						megaApi.getUserAvatar(user, getCacheDir().getAbsolutePath() + "/" + user.getEmail(), this);
					}
				} else {
					contactPropertiesImage.setImageBitmap(imBitmap);
					imageGradient.setVisibility(View.VISIBLE);

					if (imBitmap != null && !imBitmap.isRecycled()) {
//						Palette palette = Palette.from(imBitmap).generate();
//						int colorBackground = palette.getDarkMutedColor(ContextCompat.getColor(this, R.color.black));
						int colorBackground = getDominantColor1(imBitmap);
						imageLayout.setBackgroundColor(colorBackground);
					}
				}
			}
		}
	}

	public int getDominantColor1(Bitmap bitmap) {

		if (bitmap == null)
			throw new NullPointerException();

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int size = width * height;
		int pixels[] = new int[size];

		Bitmap bitmap2 = bitmap.copy(Bitmap.Config.ARGB_4444, false);

		bitmap2.getPixels(pixels, 0, width, 0, 0, width, height);

		final List<HashMap<Integer, Integer>> colorMap = new ArrayList<HashMap<Integer, Integer>>();
		colorMap.add(new HashMap<Integer, Integer>());
		colorMap.add(new HashMap<Integer, Integer>());
		colorMap.add(new HashMap<Integer, Integer>());

		int color = 0;
		int r = 0;
		int g = 0;
		int b = 0;
		Integer rC, gC, bC;
		log("getDominantColor1: "+pixels.length);
		int j=0;
		//for (int i = 0; i < pixels.length; i++) {
		while (j < pixels.length){

			color = pixels[j];

			r = Color.red(color);
			g = Color.green(color);
			b = Color.blue(color);

			rC = colorMap.get(0).get(r);
			if (rC == null)
				rC = 0;
			colorMap.get(0).put(r, ++rC);

			gC = colorMap.get(1).get(g);
			if (gC == null)
				gC = 0;
			colorMap.get(1).put(g, ++gC);

			bC = colorMap.get(2).get(b);
			if (bC == null)
				bC = 0;
			colorMap.get(2).put(b, ++bC);
			j = j+width+1;
		}

		int[] rgb = new int[3];
		for (int i = 0; i < 3; i++) {
			int max = 0;
			int val = 0;
			for (Map.Entry<Integer, Integer> entry : colorMap.get(i).entrySet()) {
				if (entry.getValue() > max) {
					max = entry.getValue();
					val = entry.getKey();
				}
			}
			rgb[i] = val;
		}

		int dominantColor = Color.rgb(rgb[0], rgb[1], rgb[2]);

		return dominantColor;
	}

	public static int convertSpToPixels(float sp, Context context) {
		int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
		return px;
	}

	public void setDefaultAvatar(String title) {
		log("setDefaultAvatar");


		Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(Color.TRANSPARENT);
		c.drawPaint(p);

		String color = megaApi.getUserAvatarColor(user);
		if (color != null) {
			log("The color to set the avatar is " + color);
			imageLayout.setBackgroundColor(Color.parseColor(color));
		} else {
			log("Default color to the avatar");
			imageLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
		}

		contactPropertiesImage.setImageBitmap(defaultAvatar);

	}

	@Override
	public void onClick(View v) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		switch (v.getId()) {
			case R.id.chat_contact_properties_clear_layout: {
				log("Clear chat option");
				if(fromContacts){
					showConfirmationClearChat();
				}
				else{
					intentToClearChat();
					finish();
				}

				break;
			}
			case R.id.chat_contact_properties_remove_contact_layout: {
				log("Remove contact chat option");

				if(user!=null){
					showConfirmationRemoveContact(user);
				}
				break;


			}
			/*case R.id.chat_contact_properties_share_contact_layout: {
				log("Share contact option");
				showSnackbar("Coming soon...");
				break;
			}*/
			case R.id.chat_contact_properties_shared_folders_button:
			case R.id.chat_contact_properties_shared_folders_layout:{
				Intent i = new Intent(this, ContactFileListActivityLollipop.class);
				i.putExtra("name", user.getEmail());
				this.startActivity(i);
				break;
			}
			case R.id.chat_contact_properties_switch:{
				log("Change notification switch");

				if(!generalChatNotifications){
					notificationsSwitch.setChecked(false);
					showSnackbar("The chat notifications are disabled, go to settings to set up them");
				}
				else{
					boolean enabled = notificationsSwitch.isChecked();

					ChatController chatC = new ChatController(this);
					if(enabled){
						chatC.unmuteChat(chatHandle);
					}
					else{
						chatC.muteChat(chatHandle);
					}

					if(chatPrefs!=null){
						chatPrefs.setNotificationsEnabled(Boolean.toString(enabled));
					}
					else{
						if(chat!=null){
							chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chat.getChatId()));
						}
					}

					if(enabled){
						setUpIndividualChatNotifications();
					}
				}
				break;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		log("onActivityResult, resultCode: "+resultCode);

		if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {

			if (!Util.isOnline(this)) {
				showSnackbar(getString(R.string.error_server_connection_problem));
				return;
			}

			final ArrayList<String> selectedContacts = intent.getStringArrayListExtra("SELECTED_CONTACTS");
			final long folderHandle = intent.getLongExtra("SELECT", 0);

			final MegaNode parent = megaApi.getNodeByHandle(folderHandle);

			if (parent.isFolder()){
				android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this);
				dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {

						ProgressDialog temp = null;
						try{
							temp = new ProgressDialog(contactInfoActivityLollipop);
							temp.setMessage(getString(R.string.context_sharing_folder));
							temp.show();
						}
						catch(Exception e){
							return;
						}
						statusDialog = temp;
						permissionsDialog.dismiss();

						log("item "+item);

						switch(item) {
							case 0:{
								for (int i=0;i<selectedContacts.size();i++){
									MegaUser user= megaApi.getContact(selectedContacts.get(i));
									log("user: "+user);
									log("parentNode: "+parent.getName()+"_"+parent.getHandle());
									megaApi.share(parent, user, MegaShare.ACCESS_READ, contactInfoActivityLollipop);
								}
								break;
							}
							case 1:{
								for (int i=0;i<selectedContacts.size();i++){
									MegaUser user= megaApi.getContact(selectedContacts.get(i));
									megaApi.share(parent, user, MegaShare.ACCESS_READWRITE, contactInfoActivityLollipop);
								}
								break;
							}
							case 2:{
								for (int i=0;i<selectedContacts.size();i++){
									MegaUser user= megaApi.getContact(selectedContacts.get(i));
									megaApi.share(parent, user, MegaShare.ACCESS_FULL, contactInfoActivityLollipop);
								}
								break;
							}
						}
					}
				});
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
				Resources resources = permissionsDialog.getContext().getResources();
				int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
				TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
				alertTitle.setTextColor(ContextCompat.getColor(this, R.color.black));
				/*int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
				View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
				titleDivider.setBackgroundColor(resources.getColor(R.color.mega));*/
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);

	}

	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	public void showConfirmationRemoveContact(final MegaUser c){
		log("showConfirmationRemoveContact");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						cC.removeContact(c);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String title = getResources().getQuantityString(R.plurals.title_confirmation_remove_contact, 1);
		builder.setTitle(title);
		String message= getResources().getQuantityString(R.plurals.confirmation_remove_contact, 1);
		builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

		log("onRequestFinish: " + request.getType() + "__" + request.getRequestString());

		if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {

			log("MegaRequest.TYPE_GET_ATTR_USER");
			if (e.getErrorCode() == MegaError.API_OK) {
				File avatar = null;
				if (getExternalCacheDir() != null) {
					avatar = new File(getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				} else {
					avatar = new File(getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				}
				Bitmap imBitmap = null;
				if (avatar.exists()) {
					if (avatar.length() > 0) {
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bOpts.inPurgeable = true;
						bOpts.inInputShareable = true;
						imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
						if (imBitmap == null) {
							avatar.delete();
						} else {
							contactPropertiesImage.setImageBitmap(imBitmap);
							imageGradient.setVisibility(View.VISIBLE);

							if (imBitmap != null && !imBitmap.isRecycled()) {
								Palette palette = Palette.from(imBitmap).generate();
								Palette.Swatch swatch =  palette.getDarkVibrantSwatch();

//								Palette.Swatch swatch = palette.getSwatches();
//								int colorBackground = color.getDarkMutedColor(ContextCompat.getColor(this, R.color.black));
								imageLayout.setBackgroundColor(swatch.getBodyTextColor());
							}
						}
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_SHARE){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				log("Shared folder correctly: "+request.getNodeHandle());
				showSnackbar(getString(R.string.context_correctly_shared));
			}
			else{
				showSnackbar(getString(R.string.context_no_shared));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_REMOVE_CONTACT){
			log("Contact removed");
			finish();
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
										MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public static void log(String message) {
		Util.log("ContactInfoActivityLollipop", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResume() {
		log("onResume-ContactChatInfoActivityLollipop");
		super.onResume();

		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	}

	public void showConfirmationClearChat(){
		log("showConfirmationClearChat");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						log("Clear chat!");
//						megaChatApi.truncateChat(chatHandle, MegaChatHandle.MEGACHAT_INVALID_HANDLE);
						log("Clear history selected!");
						ChatController chatC = new ChatController(contactInfoActivityLollipop);
						chatC.clearHistory(chat);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		String message= getResources().getString(R.string.confirmation_clear_chat,chat.getTitle());
		builder.setTitle(R.string.title_confirmation_clear_group_chat);
		builder.setMessage(message).setPositiveButton(R.string.general_clear, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void intentToClearChat(){
		Intent clearChat = new Intent(this, ChatActivityLollipop.class);
		clearChat.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
		clearChat.setAction(Constants.ACTION_CLEAR_CHAT);
		startActivity(clearChat);
	}

	@Override
	public void onBackPressed() {
//		if (overflowMenuLayout != null){
//			if (overflowMenuLayout.getVisibility() == View.VISIBLE){
//				overflowMenuLayout.setVisibility(View.GONE);
//				return;
//			}
//		}
		super.onBackPressed();
	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		log("onRequestFinish");

		if(request.getType() == MegaChatRequest.TYPE_TRUNCATE_HISTORY){
			log("Truncate history request finish!!!");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Ok. Clear history done");
				showSnackbar(getString(R.string.clear_history_success));
			}
			else{
				log("Error clearing history: "+e.getErrorString());
				showSnackbar(getString(R.string.clear_history_error));
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
			log("Create chat request finish!!!");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Chat CREATEDD!!!---> open it!");

				if(fromContacts){
					Intent intent = new Intent(this, ChatActivityLollipop.class);
					intent.setAction(Constants.ACTION_NEW_CHAT);
					intent.putExtra("CHAT_ID", request.getChatHandle());
					this.startActivity(intent);
					finish();
				}
				else{
					Intent intent = new Intent(this, ChatActivityLollipop.class);
					intent.setAction(Constants.ACTION_NEW_CHAT);
					intent.putExtra("CHAT_ID", request.getChatHandle());
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					this.startActivity(intent);
					finish();
				}
			}
			else{
				log("EEEERRRRROR WHEN CREATING CHAT " + e.getErrorString());
				showSnackbar(getString(R.string.create_chat_error));
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	public void showSnackbar(String s){
		log("showSnackbar: "+s);
		Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}
}
