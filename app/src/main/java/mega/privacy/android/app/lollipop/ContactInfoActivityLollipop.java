package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

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
import mega.privacy.android.app.components.AppBarStateChangeListener;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.listeners.CreateChatToPerformActionListener;
import mega.privacy.android.app.lollipop.listeners.MultipleAttachChatListener;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.modalbottomsheet.ContactInfoBottomSheetDialogFragment;
import mega.privacy.android.app.utils.AskForDisplayOverDialog;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ProgressDialogUtil.getProgressDialog;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.Constants.*;
import mega.privacy.android.app.components.AppBarStateChangeListener.State;



@SuppressLint("NewApi")
public class ContactInfoActivityLollipop extends DownloadableActivity implements MegaChatRequestListenerInterface, OnClickListener, MegaRequestListenerInterface, MegaChatListenerInterface, OnItemClickListener, MegaGlobalListenerInterface {

	ContactController cC;
    private android.support.v7.app.AlertDialog downloadConfirmationDialog;
    private android.support.v7.app.AlertDialog renameDialog;

	private final static int MAX_WIDTH_APPBAR_LAND = 400;
	private final static int MAX_WIDTH_APPBAR_PORT = 200;

	RelativeLayout imageLayout;
	android.app.AlertDialog permissionsDialog;
	ProgressDialog statusDialog;

	ContactInfoActivityLollipop contactInfoActivityLollipop;
	CoordinatorLayout fragmentContainer;
	CollapsingToolbarLayout collapsingToolbar;

	View imageGradient;
	ImageView contactPropertiesImage;
	LinearLayout optionsLayout;

	//Info of the user
	private EmojiTextView nameText;
	private TextView emailText;

	LinearLayout chatOptionsLayout;
	View dividerChatOptionsLayout;
	RelativeLayout sendMessageLayout;
	RelativeLayout audioCallLayout;
	RelativeLayout videoCallLayout;

	LinearLayout notificationsLayout;
	SwitchCompat notificationsSwitch;
	TextView notificationsTitle;
	View dividerNotificationsLayout;

	ChatSettings chatSettings = null;
	ChatItemPreferences chatPrefs = null;
	boolean generalChatNotifications = true;

	boolean startVideo = false;

	RelativeLayout sharedFoldersLayout;
	TextView sharedFoldersText;
	Button sharedFoldersButton;
	View dividerSharedFoldersLayout;

	RelativeLayout shareContactLayout;
	View dividerShareContactLayout;

	RelativeLayout sharedFilesLayout;
	View dividerSharedFilesLayout;

	//Toolbar elements
	private EmojiTextView firstLineTextToolbar;
	private MarqueeTextView secondLineTextToolbar;
	private ImageView contactStateIcon;
	private State stateToolbar = State.IDLE;

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

	Drawable drawableShare;
	Drawable drawableSend;
	Drawable drawableArrow;
	Drawable drawableDots;

	MenuItem shareMenuItem;
	MenuItem sendFileMenuItem;
	boolean isShareFolderExpanded;
    ContactSharedFolderFragment sharedFoldersFragment;
    MegaNode selectedNode;
    NodeController nC;
    boolean moveToRubbish;
    long parentHandle;

    private ContactInfoBottomSheetDialogFragment bottomSheetDialogFragment;

    private AskForDisplayOverDialog askForDisplayOverDialog;

	private void setAppBarOffset(int offsetPx){
		CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
		AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
		behavior.onNestedPreScroll(fragmentContainer, appBarLayout, null, 0, offsetPx, new int[]{0, 0});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		logDebug("onCreate");
		contactInfoActivityLollipop = this;
		if (megaApi == null) {
			MegaApplication app = (MegaApplication) getApplication();
			megaApi = app.getMegaApi();
		}

		if(megaApi==null||megaApi.getRootNode()==null){
			logDebug("Refresh session - sdk");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		if(isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
			}

			if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
				logDebug("Refresh session - karere");
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return;
			}

			megaChatApi.addChatListener(this);
		}

		handler = new Handler();
		cC = new ContactController(this);
        megaApi.addGlobalListener(this);
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = getResources().getDisplayMetrics().density;

		scaleW = getScaleW(outMetrics, density);
		scaleH = getScaleH(outMetrics, density);

		askForDisplayOverDialog = new AskForDisplayOverDialog(this);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {

			setContentView(R.layout.activity_chat_contact_properties);
            fragmentContainer = findViewById(R.id.fragment_container);
			toolbar = findViewById(R.id.toolbar);
			appBarLayout = findViewById(R.id.app_bar);
			setSupportActionBar(toolbar);
			aB = getSupportActionBar();

			imageLayout = findViewById(R.id.chat_contact_properties_image_layout);

			collapsingToolbar = findViewById(R.id.collapse_toolbar);
			contactStateIcon = findViewById(R.id.contact_drawable_state);

			/*Toolbar*/
			firstLineTextToolbar = findViewById(R.id.first_line_toolbar);
			secondLineTextToolbar = findViewById(R.id.second_line_toolbar);
			nameText = findViewById(R.id.chat_contact_properties_name_text);
			emailText = findViewById(R.id.chat_contact_properties_email_text);
			int width;
			if(isScreenInPortrait(this)){
				width = px2dp(MAX_WIDTH_APPBAR_PORT, outMetrics);
				secondLineTextToolbar.setPadding(0,0,0,11);
			}else{
				width = px2dp(MAX_WIDTH_APPBAR_LAND, outMetrics);
				secondLineTextToolbar.setPadding(0,0,0,5);
			}
			firstLineTextToolbar.setMaxWidthEmojis(width);
			secondLineTextToolbar.setMaxWidth(width);

			imageGradient = findViewById(R.id.gradient_view);

			setTitle(null);
			aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
			aB.setHomeButtonEnabled(true);
			aB.setDisplayHomeAsUpEnabled(true);

			contactPropertiesImage = findViewById(R.id.chat_contact_properties_toolbar_image);

			dbH = DatabaseHandler.getDbHandler(getApplicationContext());

			appBarLayout.post(new Runnable() {
				@Override
				public void run() {
					setAppBarOffset(50);
				}
			});

			//OPTIONS LAYOUT
			optionsLayout = findViewById(R.id.chat_contact_properties_options);

			//CHAT OPTIONS
			chatOptionsLayout = findViewById(R.id.chat_contact_properties_chat_options_layout);
			dividerChatOptionsLayout = findViewById(R.id.divider_chat_options_layout);
			sendMessageLayout = findViewById(R.id.chat_contact_properties_chat_send_message_layout);
			sendMessageLayout.setOnClickListener(this);
			audioCallLayout = findViewById(R.id.chat_contact_properties_chat_call_layout);
			audioCallLayout.setOnClickListener(this);
			videoCallLayout = findViewById(R.id.chat_contact_properties_chat_video_layout);
			videoCallLayout.setOnClickListener(this);

			//Notifications Layout

			notificationsLayout = findViewById(R.id.chat_contact_properties_notifications_layout);
			notificationsLayout.setVisibility(View.VISIBLE);

			notificationsTitle = findViewById(R.id.chat_contact_properties_notifications_text);

			notificationsSwitch = findViewById(R.id.chat_contact_properties_switch);
			notificationsSwitch.setOnClickListener(this);

			dividerNotificationsLayout = findViewById(R.id.divider_notifications_layout);

			//Shared folders layout
			sharedFoldersLayout = findViewById(R.id.chat_contact_properties_shared_folders_layout);
			sharedFoldersLayout.setOnClickListener(this);

			sharedFoldersText = findViewById(R.id.chat_contact_properties_shared_folders_label);

			sharedFoldersButton = findViewById(R.id.chat_contact_properties_shared_folders_button);
			sharedFoldersButton.setOnClickListener(this);

			dividerSharedFoldersLayout = findViewById(R.id.divider_shared_folder_layout);

			//Share Contact Layout

			shareContactLayout = findViewById(R.id.chat_contact_properties_share_contact_layout);
			shareContactLayout.setOnClickListener(this);

			dividerShareContactLayout = findViewById(R.id.divider_share_contact_layout);

			//Chat Shared Files Layout

			sharedFilesLayout = findViewById(R.id.chat_contact_properties_chat_files_shared_layout);
			sharedFilesLayout.setOnClickListener(this);

			dividerSharedFilesLayout = findViewById(R.id.divider_chat_files_shared_layout);

			//Clear chat Layout
			clearChatLayout = findViewById(R.id.chat_contact_properties_clear_layout);
			clearChatLayout.setOnClickListener(this);

			dividerClearChatLayout = findViewById(R.id.divider_clear_chat_layout);

			//Remove contact Layout
			removeContactChatLayout = findViewById(R.id.chat_contact_properties_remove_contact_layout);
			removeContactChatLayout.setOnClickListener(this);

			chatHandle = extras.getLong("handle",-1);
			userEmailExtra = extras.getString("name");
			if (chatHandle != -1) {

				logDebug("From chat!!");
				fromContacts = false;
				chat = megaChatApi.getChatRoom(chatHandle);

				long userHandle = chat.getPeerHandle(0);

				String userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle);
				user = megaApi.getContact(userHandleEncoded);
				if(user!=null){
					logDebug("User foundd!!!");
				}

				chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chatHandle));

				if (chat.getTitle() != null && !chat.getTitle().isEmpty() && !chat.getTitle().equals("")){
					firstLineTextToolbar.setText(chat.getTitle());
					nameText.setText(chat.getTitle());
				}
				else {
					if (userEmailExtra != null) {
						firstLineTextToolbar.setText(userEmailExtra);
						nameText.setText(userEmailExtra);
					}
				}
				String fullname = firstLineTextToolbar.getText().toString();
				setDefaultAvatar(fullname);
			}
			else{
				logDebug("From contacts!!");

				fromContacts = true;
				user = megaApi.getContact(userEmailExtra);

				String fullName = "";
				if(user!=null){
					logDebug("User handle: " + user.getHandle());
					MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
					if(contactDB!=null){
						logDebug("Contact DB found!");
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
							logDebug("Put email as fullname");
							fullName= user.getEmail();
						}

						firstLineTextToolbar.setText(fullName);
						nameText.setText(fullName);
					}
					else{
						logWarning("The contactDB is null: ");
					}
				}

				//Find chat with this contact
				if(isChatEnabled()){
					chat = megaChatApi.getChatRoomByUser(user.getHandle());

					if(chat!=null){
						chatHandle = chat.getChatId();
						if(chatHandle==-1){
							notificationsLayout.setVisibility(View.GONE);
							dividerNotificationsLayout.setVisibility(View.GONE);

							sharedFilesLayout.setVisibility(View.GONE);
							dividerSharedFilesLayout.setVisibility(View.GONE);
						}
						else{
							chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chatHandle));
						}
					}
					else{
						notificationsLayout.setVisibility(View.GONE);
						dividerNotificationsLayout.setVisibility(View.GONE);

						sharedFilesLayout.setVisibility(View.GONE);
						dividerSharedFilesLayout.setVisibility(View.GONE);
					}

					if (megaChatApi == null){
						megaChatApi = ((MegaApplication) this.getApplication()).getMegaChatApi();
					}

				}
				setDefaultAvatar(fullName);
			}

			if(isOnline(this)){
				logDebug("online -- network connection");
				setAvatar();

				if(user!=null){
					sharedFoldersLayout.setVisibility(View.VISIBLE);
					dividerSharedFoldersLayout.setVisibility(View.VISIBLE);

					ArrayList<MegaNode> nodes = megaApi.getInShares(user);
                    setFoldersButtonText(nodes);
					emailText.setText(user.getEmail());

					if(isChatEnabled()){
						if(chat!=null){
							clearChatLayout.setVisibility(View.VISIBLE);
							dividerClearChatLayout.setVisibility(View.VISIBLE);
						}
						else{
							clearChatLayout.setVisibility(View.GONE);
							dividerClearChatLayout.setVisibility(View.GONE);
						}

						shareContactLayout.setVisibility(View.VISIBLE);
						dividerShareContactLayout.setVisibility(View.VISIBLE);

						chatOptionsLayout.setVisibility(View.VISIBLE);
						dividerChatOptionsLayout.setVisibility(View.VISIBLE);
					}
					else{
						clearChatLayout.setVisibility(View.GONE);
						dividerClearChatLayout.setVisibility(View.GONE);
						shareContactLayout.setVisibility(View.GONE);
						dividerShareContactLayout.setVisibility(View.GONE);
						chatOptionsLayout.setVisibility(View.GONE);
						dividerChatOptionsLayout.setVisibility(View.GONE);
					}
				}
				else{
					sharedFoldersLayout.setVisibility(View.GONE);
					dividerSharedFoldersLayout.setVisibility(View.GONE);
					chatOptionsLayout.setVisibility(View.GONE);
					dividerChatOptionsLayout.setVisibility(View.GONE);

					if(isChatEnabled()){
						if(chat!=null){
							emailText.setText(user.getEmail());
							clearChatLayout.setVisibility(View.VISIBLE);
							dividerClearChatLayout.setVisibility(View.VISIBLE);
						}
						else{
							clearChatLayout.setVisibility(View.GONE);
							dividerClearChatLayout.setVisibility(View.GONE);
						}

						shareContactLayout.setVisibility(View.VISIBLE);
						dividerShareContactLayout.setVisibility(View.VISIBLE);
					}
					else{
						clearChatLayout.setVisibility(View.GONE);
						dividerClearChatLayout.setVisibility(View.GONE);
						shareContactLayout.setVisibility(View.GONE);
						dividerShareContactLayout.setVisibility(View.GONE);
					}
				}
			}
			else{
				logDebug("OFFLINE -- NO network connection");
				if(chat!=null){
					String userEmail = chat.getPeerEmail(0);
					setOfflineAvatar(userEmail);
					emailText.setText(user.getEmail());
				}
				sharedFoldersLayout.setVisibility(View.GONE);
				dividerSharedFoldersLayout.setVisibility(View.GONE);
				clearChatLayout.setVisibility(View.GONE);
				dividerClearChatLayout.setVisibility(View.GONE);

				shareContactLayout.setVisibility(View.GONE);
				dividerShareContactLayout.setVisibility(View.GONE);

				chatOptionsLayout.setVisibility(View.VISIBLE);
				dividerChatOptionsLayout.setVisibility(View.VISIBLE);
			}

			if(isChatEnabled()){

				chatSettings = dbH.getChatSettings();
				if(chatSettings==null){
					logDebug("Chat settings null - notifications ON");
					setUpIndividualChatNotifications();
				}
				else {
					logDebug("There is chat settings");
					if (chatSettings.getNotificationsEnabled() == null) {
						generalChatNotifications = true;

					} else {
						generalChatNotifications = Boolean.parseBoolean(chatSettings.getNotificationsEnabled());
					}

					if (generalChatNotifications) {
						setUpIndividualChatNotifications();

					} else {
						logDebug("General notifications OFF");
						boolean notificationsEnabled = false;
						notificationsSwitch.setChecked(notificationsEnabled);

						notificationsLayout.setVisibility(View.VISIBLE);
						dividerNotificationsLayout.setVisibility(View.VISIBLE);
					}
				}

			}else{

				notificationsLayout.setVisibility(View.GONE);
				dividerNotificationsLayout.setVisibility(View.GONE);

				sharedFilesLayout.setVisibility(View.GONE);
				dividerSharedFilesLayout.setVisibility(View.GONE);
			}

		} else {
			logWarning("Extras is NULL");
		}
        if(askForDisplayOverDialog != null) {
            askForDisplayOverDialog.showDialog();
        }
	}

	private void visibilityStateIcon() {
		if (megaChatApi == null) {
			contactStateIcon.setVisibility(View.GONE);
			return;
		}

		int userStatus = megaChatApi.getUserOnlineStatus(user.getHandle());
		if (stateToolbar == State.EXPANDED && (userStatus == MegaChatApi.STATUS_ONLINE || userStatus == MegaChatApi.STATUS_AWAY || userStatus == MegaChatApi.STATUS_BUSY || userStatus == MegaChatApi.STATUS_OFFLINE)) {
			contactStateIcon.setVisibility(View.VISIBLE);
			return;
		}

		contactStateIcon.setVisibility(View.GONE);
		return;
	}


	private void setContactPresenceStatus(){
		logDebug("setContactPresenceStatus");
		if (megaChatApi != null){
			int userStatus = megaChatApi.getUserOnlineStatus(user.getHandle());
			if(userStatus == MegaChatApi.STATUS_ONLINE){
				logDebug("This user is connected");
				contactStateIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_online));
				secondLineTextToolbar.setVisibility(View.VISIBLE);
				secondLineTextToolbar.setText(getString(R.string.online_status));
			}else if(userStatus == MegaChatApi.STATUS_AWAY){
				logDebug("This user is away");
				contactStateIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_away));
				secondLineTextToolbar.setVisibility(View.VISIBLE);
				secondLineTextToolbar.setText(getString(R.string.away_status));
			} else if(userStatus == MegaChatApi.STATUS_BUSY){
				logDebug("This user is busy");
				contactStateIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_busy));
				secondLineTextToolbar.setVisibility(View.VISIBLE);
				secondLineTextToolbar.setText(getString(R.string.busy_status));
			}
			else if(userStatus == MegaChatApi.STATUS_OFFLINE){
				logDebug("This user is offline");
				contactStateIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_offline));
				secondLineTextToolbar.setVisibility(View.VISIBLE);
				secondLineTextToolbar.setText(getString(R.string.offline_status));
			}
			else if(userStatus == MegaChatApi.STATUS_INVALID){
				logDebug("INVALID status: " + userStatus);
				secondLineTextToolbar.setVisibility(View.GONE);
			}
			else{
				logDebug("This user status is: " + userStatus);
				secondLineTextToolbar.setVisibility(View.GONE);
			}
		}
		visibilityStateIcon();
	}

	public void setUpIndividualChatNotifications(){
		logDebug("setUpIndividualChatNotifications");
		//SET Preferences (if exist)
		if(chatPrefs!=null){
			logDebug("There is individual chat preferences");

			boolean notificationsEnabled = true;
			if (chatPrefs.getNotificationsEnabled() != null){
				notificationsEnabled = Boolean.parseBoolean(chatPrefs.getNotificationsEnabled());
			}
			notificationsSwitch.setChecked(notificationsEnabled);
		}
		else{
			logDebug("NO individual chat preferences");
			notificationsSwitch.setChecked(true);
		}

		notificationsLayout.setVisibility(View.VISIBLE);
		dividerNotificationsLayout.setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		logDebug("onCreateOptionsMenuLollipop");

		drawableDots = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_dots_vertical_white);
		drawableDots = drawableDots.mutate();
		drawableArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_arrow_back_white);
		drawableArrow = drawableArrow.mutate();

		drawableShare = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_share_white);
		drawableShare = drawableShare.mutate();
		drawableSend = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_send_to_contact);
		drawableSend = drawableSend.mutate();

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contact_properties_action, menu);

		shareMenuItem = menu.findItem(R.id.cab_menu_share_folder);
		sendFileMenuItem = menu.findItem(R.id.cab_menu_send_file);
		sendFileMenuItem.setIcon(mutateIconSecondary(this, R.drawable.ic_send_to_contact, R.color.white));

		if(isOnline(this)){
			if(isChatEnabled() && fromContacts){
				sendFileMenuItem.setVisible(true);
			}
			else{
				sendFileMenuItem.setVisible(false);
			}
		}
		else{
			logDebug("Hide all - no network connection");
			shareMenuItem.setVisible(false);
			sendFileMenuItem.setVisible(false);
		}

		appBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
			@Override
			public void onStateChanged(AppBarLayout appBarLayout, State state) {
				stateToolbar = state;
				if (stateToolbar == State.EXPANDED) {
					firstLineTextToolbar.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
					secondLineTextToolbar.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
					setColorFilterWhite();
					visibilityStateIcon();
				} else if (stateToolbar == State.COLLAPSED) {
					firstLineTextToolbar.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
					secondLineTextToolbar.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
					setColorFilterBlack();
					visibilityStateIcon();
				}
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	void setColorFilterWhite () {

		drawableArrow.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP);
		getSupportActionBar().setHomeAsUpIndicator(drawableArrow);

		drawableDots.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP);
		toolbar.setOverflowIcon(drawableDots);

		if (shareMenuItem != null) {
			drawableShare.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP);
			shareMenuItem.setIcon(drawableShare);
		}
		if (sendFileMenuItem != null) {
			drawableSend.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP);
			sendFileMenuItem.setIcon(drawableSend);
		}
	}

	void setColorFilterBlack () {
		drawableArrow.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_ATOP);
		getSupportActionBar().setHomeAsUpIndicator(drawableArrow);

		drawableDots.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_ATOP);
		toolbar.setOverflowIcon(drawableDots);

		if (shareMenuItem != null) {
			drawableShare.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_ATOP);
			shareMenuItem.setIcon(drawableShare);
		}
		if (sendFileMenuItem != null) {
			drawableSend.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_ATOP);
			sendFileMenuItem.setIcon(drawableSend);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		logDebug("onOptionsItemSelected");

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
			case R.id.cab_menu_send_file:{

				if(!isOnline(this)){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
					return true;
				}

				sendFileToChat();
				break;
			}
		}
		return true;
	}

	public void sendFileToChat(){
		logDebug("sendFileToChat");

		if(user==null){
			logWarning("Selected contact NULL");
			return;
		}
		List<MegaUser> userList = new ArrayList<MegaUser>();
		userList.add(user);
		ContactController cC = new ContactController(this);
		cC.pickFileToSend(userList);
	}

	public void sendMessageToChat(){
		logDebug("sendMessageToChat");
		if(user!=null){
			MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
			if(chat==null){
				logDebug("No chat, create it!");
				MegaChatPeerList peers = MegaChatPeerList.createInstance();
				peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
				megaChatApi.createChat(false, peers, this);
			}
			else{
				logDebug("There is already a chat, open it!");
				if(fromContacts){
					Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
					intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
					intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
					this.startActivity(intentOpenChat);
				}
				else{
					Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
					intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
					intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
					intentOpenChat.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					this.startActivity(intentOpenChat);
				}
			}
		}
	}

	public void startCall(boolean startVideo) {
		MegaChatRoom chatRoomTo = megaChatApi.getChatRoomByUser(user.getHandle());
		if (chatRoomTo != null) {
			if (megaChatApi.getChatCall(chatRoomTo.getChatId()) != null) {
				Intent i = new Intent(this, ChatCallActivity.class);
				i.putExtra(CHAT_ID, chatRoomTo.getChatId());
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			} else {
				((MegaApplication) getApplication()).setSpeakerStatus(chatRoomTo.getChatId(), startVideo);
				megaChatApi.startChatCall(chatRoomTo.getChatId(), startVideo, this);
			}
		} else {
			//Create first the chat
			ArrayList<MegaChatRoom> chats = new ArrayList<>();
			ArrayList<MegaUser> usersNoChat = new ArrayList<>();
			usersNoChat.add(user);
			CreateChatToPerformActionListener listener = null;

			if (startVideo) {
				listener = new CreateChatToPerformActionListener(chats, usersNoChat, -1, this, CreateChatToPerformActionListener.START_VIDEO_CALL);
			} else {
				listener = new CreateChatToPerformActionListener(chats, usersNoChat, -1, this, CreateChatToPerformActionListener.START_AUDIO_CALL);
			}

			MegaChatPeerList peers = MegaChatPeerList.createInstance();
			peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
			megaChatApi.createChat(false, peers, listener);
		}
	}

	public boolean checkPermissionsCall(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

			boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
			if (!hasCameraPermission) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
				return false;
			}

			boolean hasRecordAudioPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
			if (!hasRecordAudioPermission) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
				return false;
			}

			return true;
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		logDebug("onRequestPermissionsResult");
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case REQUEST_CAMERA: {
				logDebug("REQUEST_CAMERA");
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if(checkPermissionsCall()){
						startCall(startVideo);
					}
				}
				break;
			}
			case RECORD_AUDIO: {
				logDebug("RECORD_AUDIO");
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if(checkPermissionsCall()){
						startCall(startVideo);
					}
				}
				break;
			}

		}
	}


	public void openChat(long chatId, String text){
		logDebug("openChat: " + chatId);

		if(chatId!=-1){
			MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
			if(chat!=null){
				logDebug("Open chat with id: " + chatId);
				Intent intentToChat = new Intent(this, ChatActivityLollipop.class);
				intentToChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
				intentToChat.putExtra("CHAT_ID", chatId);
				if(text!=null){
					intentToChat.putExtra("showSnackbar", text);
				}
				this.startActivity(intentToChat);
			}
			else{
				logWarning("Error, chat is NULL");
			}
		}
		else{
			logWarning("Error, chat id is -1");
		}
	}

	public void pickFolderToShare(String email){
		logDebug("pickFolderToShare");
		if (email != null){
			Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
			intent.setAction(FileExplorerActivityLollipop.ACTION_SELECT_FOLDER_TO_SHARE);
			ArrayList<String> contacts = new ArrayList<String>();
			contacts.add(email);
			intent.putExtra(SELECTED_CONTACTS, contacts);
			startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER);
		}
		else{
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_sharing_folder), -1);
			logWarning("Error sharing folder");
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
		logDebug("setAvatar");
		File avatar = buildAvatarFile(this,user.getEmail() + ".jpg");
		if (isFileAvailable(avatar)) {
			setProfileAvatar(avatar);
		}
	}

	public void setOfflineAvatar(String email) {
		logDebug("setOfflineAvatar");
		File avatar = buildAvatarFile(this, email + ".jpg");

        if (isFileAvailable(avatar)) {
            Bitmap imBitmap = null;
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(),bOpts);
                if (imBitmap != null) {
                    contactPropertiesImage.setImageBitmap(imBitmap);
                    imageGradient.setVisibility(View.VISIBLE);

                    if (imBitmap != null && !imBitmap.isRecycled()) {
                        int colorBackground = getDominantColor1(imBitmap);
                        imageLayout.setBackgroundColor(colorBackground);
                    }
                }
            }
        }
	}

	public void setProfileAvatar(File avatar) {
		logDebug("setProfileAvatar");
		Bitmap imBitmap = null;
		if (avatar.exists()) {
			if (avatar.length() > 0) {
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (imBitmap == null) {
					avatar.delete();
                    megaApi.getUserAvatar(user,buildAvatarFile(this, user.getEmail()).getAbsolutePath(), this);
                } else {
					contactPropertiesImage.setImageBitmap(imBitmap);
					imageGradient.setVisibility(View.VISIBLE);

					if (imBitmap != null && !imBitmap.isRecycled()) {
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
		logDebug("pixels.length: " + pixels.length);
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

	public void setDefaultAvatar(String title) {
		logDebug("setDefaultAvatar");


		Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(Color.TRANSPARENT);
		c.drawPaint(p);

		String color = megaApi.getUserAvatarColor(user);
		if (color != null) {
			logDebug("The color to set the avatar is " + color);
			imageLayout.setBackgroundColor(Color.parseColor(color));
		} else {
			logDebug("Default color to the avatar");
			imageLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
		}

		contactPropertiesImage.setImageBitmap(defaultAvatar);

	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.chat_contact_properties_clear_layout: {
				logDebug("Clear chat option");
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
				logDebug("Remove contact chat option");

				if(user!=null){
					showConfirmationRemoveContact(user);
				}
				break;
			}
			case R.id.chat_contact_properties_chat_send_message_layout:{
				logDebug("Send message option");
				if(!checkConnection()) return;

				sendMessageToChat();
				break;
			}
			case R.id.chat_contact_properties_chat_call_layout:{
				logDebug("Start audio call option");
				if(!checkConnection()) return;

				if(megaChatApi!=null){
					if(!participatingInACall(megaChatApi)){
						logDebug("I'm not in a call");
						startVideo = false;
						if(checkPermissionsCall()){
							startCall(startVideo);
						}
					}
				}
				break;
			}
			case R.id.chat_contact_properties_chat_video_layout:{
				logDebug("Star video call option");
				if(!checkConnection()) return;

				if(megaChatApi!=null) {
					if (!participatingInACall(megaChatApi)) {
						logDebug("I'm not in a call");
						startVideo = true;
						if (checkPermissionsCall()) {
							startCall(startVideo);
						}
					}
				}
				break;
			}
			case R.id.chat_contact_properties_share_contact_layout: {
				logDebug("Share contact option");
				if(user==null){
					logDebug("Selected contact NULL");
					return;
				}

				ChatController cC = new ChatController(this);

				cC.selectChatsToAttachContact(user);
				break;
			}
			case R.id.chat_contact_properties_shared_folders_button:
			case R.id.chat_contact_properties_shared_folders_layout:{
				sharedFolderClicked();
				break;
			}
			case R.id.chat_contact_properties_switch:{
				logDebug("Change notification switch");

				if(!generalChatNotifications){
					notificationsSwitch.setChecked(false);
					showSnackbar(SNACKBAR_TYPE, "The chat notifications are disabled, go to settings to set up them", -1);
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
			case R.id.chat_contact_properties_chat_files_shared_layout:{
				Intent nodeHistoryIntent = new Intent(this, NodeAttachmentHistoryActivity.class);
				if(chat!=null){
					nodeHistoryIntent.putExtra("chatId", chat.getChatId());
				}
				startActivity(nodeHistoryIntent);
				break;
			}
		}
	}

	private boolean checkConnection() {
		if (!isOnline(this)) {
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
			return false;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		logDebug("resultCode: " + resultCode);

		if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {

			if (!isOnline(this)) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return;
			}

			final ArrayList<String> selectedContacts = intent.getStringArrayListExtra(SELECTED_CONTACTS);
			final long folderHandle = intent.getLongExtra("SELECT", 0);

			final MegaNode parent = megaApi.getNodeByHandle(folderHandle);

			if (parent.isFolder()){
				android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleAddContacts);
				dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						statusDialog = getProgressDialog(getString(R.string.context_sharing_folder));
						permissionsDialog.dismiss();
						nC.shareFolder(parent, selectedContacts, item);
					}
				});
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
				Resources resources = permissionsDialog.getContext().getResources();
				int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
				TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
				alertTitle.setTextColor(ContextCompat.getColor(this, R.color.black));
			}
        } else if (requestCode == REQUEST_CODE_TREE) {
            onRequestSDCardWritePermission(intent, resultCode, false, nC);
        }
		else if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
			logDebug("requestCode == REQUEST_CODE_SELECT_FILE");
			if (intent == null) {
				logWarning("Return.....");
				return;
			}

			long fileHandles[] = intent.getLongArrayExtra(NODE_HANDLES);

			if (fileHandles == null) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error), -1);
				return;
			}

			MegaChatRoom chatRoomToSend = megaChatApi.getChatRoomByUser(user.getHandle());
			if(chatRoomToSend!=null){
				checkIfNodesAreMineBeforeAttach(fileHandles, chatRoomToSend.getChatId());
			}
			else{
				//Create first the chat
				ArrayList<MegaChatRoom> chats = new ArrayList<>();
				ArrayList<MegaUser> usersNoChat = new ArrayList<>();
				usersNoChat.add(user);
				CreateChatToPerformActionListener listener = new CreateChatToPerformActionListener(chats, usersNoChat, fileHandles, this, CreateChatToPerformActionListener.SEND_FILES, -1);
				MegaChatPeerList peers = MegaChatPeerList.createInstance();
				peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
				megaChatApi.createChat(false, peers, listener);
			}
		}
        else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
            logDebug("onActivityResult: REQUEST_CODE_SELECT_LOCAL_FOLDER");
            if (intent == null) {
                logDebug("Return.....");
                return;
            }

            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
            logDebug("url: "+url);
            long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
            logDebug("size: "+size);
            long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
            logDebug("hashes size: "+hashes.length);

            boolean highPriority = intent.getBooleanExtra(HIGH_PRIORITY_TRANSFER, false);

            nC.checkSizeBeforeDownload(parentPath,url, size, hashes, highPriority);
        }
		else if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK){
            logDebug("Attach nodes to chats: REQUEST_CODE_SELECT_CHAT");

			long[] chatHandles = intent.getLongArrayExtra("SELECTED_CHATS");
			logDebug("Send to " + chatHandles.length + " chats");

			int countChat = chatHandles.length;
			logDebug("Selected: " + countChat + " chats to send");

			for (long chatHandle : chatHandles) {
				MegaHandleList handleList = MegaHandleList.createInstance();
				handleList.addMegaHandle(user.getHandle());
				megaChatApi.attachContacts(chatHandle, handleList);
			}

			if(countChat==1){
				openChat(chatHandles[0], null);
			}
			else{
				String message = getResources().getQuantityString(R.plurals.plural_contact_sent_to_chats, 1);
				showSnackbar(SNACKBAR_TYPE, message, -1);
			}
		}else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER	&& resultCode == RESULT_OK) {
            if (!isOnline(this)) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }
            
            ProgressDialog temp = null;
            try {
                temp = new ProgressDialog(this);
                temp.setMessage(getString(R.string.context_copying));
                temp.show();
            } catch (Exception e) {
                return;
            }
            statusDialog = temp;
            
            final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
            final long toHandle = intent.getLongExtra("COPY_TO", 0);
            final int totalCopy = copyHandles.length;
            
            MegaNode parent = megaApi.getNodeByHandle(toHandle);
            for (int i = 0; i < copyHandles.length; i++) {
				logDebug("NODE TO COPY: " + megaApi.getNodeByHandle(copyHandles[i]).getName());
				logDebug("WHERE: " + parent.getName());
				logDebug("NODES: " + copyHandles[i] + "_" + parent.getHandle());
                MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
                if (cN != null){
					logDebug("cN != null");
                    megaApi.copyNode(cN, parent, this);
                }
                else{
					logWarning("cN == null");
                    try {
                        statusDialog.dismiss();
                        if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                            showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_sent_node), -1);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        }

		super.onActivityResult(requestCode, resultCode, intent);
	}

	public void checkIfNodesAreMineBeforeAttach(long[] fileHandles, long chatId) {
		new ChatController(this).checkIfNodesAreMineAndAttachNodes(fileHandles, chatId);
	}

	public void sendFilesToChat(long[] fileHandles, long chatId) {
		MultipleAttachChatListener listener = new MultipleAttachChatListener(this, chatId, fileHandles.length);
		for (long fileHandle : fileHandles) {
			megaChatApi.attachNode(chatId, fileHandle, listener);
		}
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
		logDebug("showConfirmationRemoveContact");
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
		logDebug("onRequestStart: " + request.getName());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

		logDebug("onRequestFinish: " + request.getType() + "__" + request.getRequestString());

		if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {

			logDebug("MegaRequest.TYPE_GET_ATTR_USER");
			if (e.getErrorCode() == MegaError.API_OK) {
				File avatar = buildAvatarFile(this, request.getEmail() + ".jpg");
				Bitmap imBitmap = null;
				if (isFileAvailable(avatar)) {
					if (avatar.length() > 0) {
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
						if (imBitmap == null) {
							avatar.delete();
						} else {
							contactPropertiesImage.setImageBitmap(imBitmap);
							imageGradient.setVisibility(View.VISIBLE);

							if (imBitmap != null && !imBitmap.isRecycled()) {
								Palette palette = Palette.from(imBitmap).generate();
								Palette.Swatch swatch =  palette.getDarkVibrantSwatch();
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
				logDebug("Shared folder correctly: " + request.getNodeHandle());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_shared), -1);
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_shared), -1);
			}
		}else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}
            
            if (e.getErrorCode() == MegaError.API_OK){
                if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_created), -1);
                    sharedFoldersFragment.setNodes();
                }
            }
            else{
                if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_no_created), -1);
                    sharedFoldersFragment.setNodes();
                }
            }
        }
        else if (request.getType() == MegaRequest.TYPE_RENAME){
            
            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}
            
            if (e.getErrorCode() == MegaError.API_OK){
                if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                    sharedFoldersFragment.clearSelections();
                    sharedFoldersFragment.hideMultipleSelect();
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_renamed), -1);
                }
            }
            else{
                if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                    sharedFoldersFragment.clearSelections();
                    sharedFoldersFragment.hideMultipleSelect();
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_renamed), -1);
                }
            }
			logDebug("Rename nodes request finished");
        }
        else if (request.getType() == MegaRequest.TYPE_COPY) {
            try {
                statusDialog.dismiss();
            } catch (Exception ex) {
            }
            
            if (e.getErrorCode() == MegaError.API_OK){
                if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                    sharedFoldersFragment.clearSelections();
                    sharedFoldersFragment.hideMultipleSelect();
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied), -1);
                }
            }
            else{
                if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
                    Intent intent = new Intent(this, ManagerActivityLollipop.class);
                    intent.setAction(ACTION_OVERQUOTA_STORAGE);
                    startActivity(intent);
                    finish();
                }
                else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
					logDebug("PRE OVERQUOTA ERROR: " + e.getErrorCode());
                    Intent intent = new Intent(this, ManagerActivityLollipop.class);
                    intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
                    startActivity(intent);
                    finish();
                }
                else{
                    if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                        sharedFoldersFragment.clearSelections();
                        sharedFoldersFragment.hideMultipleSelect();
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
                    }
                }
            }

			logDebug("Copy nodes request finished");
        }
        else if (request.getType() == MegaRequest.TYPE_MOVE){
            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}
            
            if(moveToRubbish){
				logDebug("Finish move to Rubbish!");
                if (e.getErrorCode() == MegaError.API_OK){
                    if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                        sharedFoldersFragment.clearSelections();
                        sharedFoldersFragment.hideMultipleSelect();
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved_to_rubbish), -1);
                    }
                }
                else{
                    if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                        sharedFoldersFragment.clearSelections();
                        sharedFoldersFragment.hideMultipleSelect();
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
                    }
                }
            }
            else{
                if (e.getErrorCode() == MegaError.API_OK){
                    if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                        sharedFoldersFragment.clearSelections();
                        sharedFoldersFragment.hideMultipleSelect();
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved), -1);
                    }
                }
                else{
                    if(sharedFoldersFragment!=null && sharedFoldersFragment.isVisible()){
                        sharedFoldersFragment.clearSelections();
                        sharedFoldersFragment.hideMultipleSelect();
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
                    }
                }
            }
            moveToRubbish=false;
			logDebug("Move request finished");
        }
        else if (request.getType() == MegaRequest.TYPE_SHARE){
            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}
            
            if (e.getErrorCode() == MegaError.API_OK){
                sharedFoldersFragment.clearSelections();
                sharedFoldersFragment.hideMultipleSelect();
				logDebug("Shared folder correctly: " + request.getNodeHandle());
                Toast.makeText(this, getString(R.string.context_correctly_shared), Toast.LENGTH_SHORT).show();
            }
            else{
                sharedFoldersFragment.clearSelections();
                sharedFoldersFragment.hideMultipleSelect();
                Toast.makeText(this, getString(R.string.context_no_shared), Toast.LENGTH_LONG).show();
            }
        }
		else if(request.getType() == MegaRequest.TYPE_REMOVE_CONTACT){
			logDebug("Contact removed");
			finish();
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
										MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getName());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (drawableArrow != null) {
			drawableArrow.setColorFilter(null);
		}
		if (drawableDots != null) {
			drawableDots.setColorFilter(null);
		}
		if (drawableSend != null) {
			drawableSend.setColorFilter(null);
		}
		if (drawableShare != null) {
			drawableShare.setColorFilter(null);
		}
        if (askForDisplayOverDialog != null) {
            askForDisplayOverDialog.recycle();
        }
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResume() {
		logDebug("onResume");
		super.onResume();

		if(isChatEnabled()){
			setContactPresenceStatus();
		} else{
			contactStateIcon.setVisibility(View.GONE);
		}

		if(isChatEnabled()){
			requestLastGreen(-1);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	}

	public void requestLastGreen(int state){
		logDebug("state: " + state);
		if(state == -1){
			state = megaChatApi.getUserOnlineStatus(user.getHandle());
		}

		if(state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID){
			logDebug("Request last green for user");
			megaChatApi.requestLastGreen(user.getHandle(), this);
		}
	}

	public void showConfirmationClearChat(){
		logDebug("showConfirmationClearChat");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						logDebug("Clear chat!");
						logDebug("Clear history selected!");
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
		clearChat.setAction(ACTION_CLEAR_CHAT);
		startActivity(clearChat);
	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		logDebug("onRequestFinish");

		if(request.getType() == MegaChatRequest.TYPE_TRUNCATE_HISTORY){
			logDebug("Truncate history request finish!!!");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("Ok. Clear history done");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.clear_history_success), -1);
			}
			else{
				logWarning("Error clearing history: " + e.getErrorString());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.clear_history_error), -1);
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
			logDebug("Create chat request finish!!!");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("Chat CREATEDD!!!---> open it!");

				if(fromContacts){
					Intent intent = new Intent(this, ChatActivityLollipop.class);
					intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
					intent.putExtra("CHAT_ID", request.getChatHandle());
					this.startActivity(intent);
					finish();
				}
				else{
					Intent intent = new Intent(this, ChatActivityLollipop.class);
					intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
					intent.putExtra("CHAT_ID", request.getChatHandle());
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					this.startActivity(intent);
					finish();
				}
			}
			else{
				logDebug("ERROR WHEN CREATING CHAT " + e.getErrorString());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.create_chat_error), -1);
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_START_CHAT_CALL){
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("TYPE_START_CHAT_CALL finished with success");
				//getFlag - Returns true if it is a video-audio call or false for audio call
			}
			else{
				logDebug("ERROR WHEN TYPE_START_CHAT_CALL " + e.getErrorString());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error), -1);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	public void showSnackbar(int type, String s, long idChat){
		showSnackbar(type, fragmentContainer, s, idChat);
	}
	
	private void sharedFolderClicked(){
        RelativeLayout sharedFolderLayout = (RelativeLayout)findViewById(R.id.shared_folder_list_container);
		if(isShareFolderExpanded){
			sharedFolderLayout.setVisibility(View.GONE);
			if (user != null) {
				setFoldersButtonText(megaApi.getInShares(user));
			}
		}
		else{
			sharedFolderLayout.setVisibility(View.VISIBLE);
			sharedFoldersButton.setText(R.string.general_close);
            if (sharedFoldersFragment == null){
                sharedFoldersFragment = new ContactSharedFolderFragment();
                sharedFoldersFragment.setUserEmail(user.getEmail());
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_shared_folders, sharedFoldersFragment, "sharedFoldersFragment").commitNow();
            }
		}
		isShareFolderExpanded = !isShareFolderExpanded;
	}
    
    public void showOptionsPanel(MegaNode node){
		logDebug("showOptionsPanel");

        if (node == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedNode = node;
		bottomSheetDialogFragment = new ContactInfoBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }
    
    public MegaNode getSelectedNode() {
        return selectedNode;
    }
    
    public void setSelectedNode(MegaNode selectedNode) {
        this.selectedNode = selectedNode;
    }
    
    public void onFileClick(ArrayList<Long> handleList) {
        
        if(nC==null){
            nC = new NodeController(this);
        }
        nC.prepareForDownload(handleList, true);
    }
    
    public void showConfirmationLeaveIncomingShare (final MegaNode n){
		logDebug("Node handle: " + n.getHandle());
        
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
                        megaApi.remove(n);
                        break;
                    }
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };
        
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        String message= getResources().getString(R.string.confirmation_leave_share_folder);
        builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }
    
    public void showConfirmationLeaveIncomingShare (final ArrayList<Long> handleList){
		logDebug("showConfirmationLeaveIncomingShare");
        
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
                        leaveMultipleShares(handleList);
                        break;
                    }
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };
        
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        String message= getResources().getString(R.string.confirmation_leave_share_folder);
        builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }
    
    public void leaveMultipleShares (ArrayList<Long> handleList){
        
        for (int i=0; i<handleList.size(); i++){
            MegaNode node = megaApi.getNodeByHandle(handleList.get(i));
            megaApi.remove(node);
        }
    }
    
    public void showMoveLollipop(ArrayList<Long> handleList){
        moveToRubbish=false;
        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("MOVE_FROM", longArray);
        startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
    }
    
    public void showCopyLollipop(ArrayList<Long> handleList) {
        
        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i = 0; i < handleList.size(); i++) {
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("COPY_FROM", longArray);
        startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
    }
    
    public void askConfirmationMoveToRubbish(final ArrayList<Long> handleList){
		logDebug("askConfirmationMoveToRubbish");
        
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        moveToTrash(handleList);
                        break;
                    
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };
        
        if(handleList!=null){
            
            if (handleList.size() > 0){
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//				builder.setTitle(getResources().getString(R.string.section_rubbish_bin));
                if (handleList.size() > 1){
                    builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish_plural));
                }
                else{
                    builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish));
                }
                builder.setPositiveButton(R.string.general_move, dialogClickListener);
                builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
                builder.show();
            }
        }
        else{
			logWarning("handleList NULL");
            return;
        }
    }
    
    public boolean isEmptyParentHandleStack() {
        if (sharedFoldersFragment != null) {
            return sharedFoldersFragment.isEmptyParentHandleStack();
        }
		logWarning("Fragment NULL");
        return true;
    }
    
    public void moveToTrash(final ArrayList<Long> handleList){
		logDebug("moveToTrash: ");
        moveToRubbish=true;
        if (!isOnline(this)) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
            return;
        }
        
        MultipleRequestListener moveMultipleListener = null;
        MegaNode parent;
        //Check if the node is not yet in the rubbish bin (if so, remove it)
        if(handleList!=null){
            if(handleList.size()>1){
				logDebug("MOVE multiple: " + handleList.size());
                moveMultipleListener = new MultipleRequestListener(MULTIPLE_SEND_RUBBISH, this);
                for (int i=0;i<handleList.size();i++){
                    megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)), megaApi.getRubbishNode(), moveMultipleListener);
                }
            }
            else{
				logDebug("MOVE single");
                megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(0)), megaApi.getRubbishNode(), this);
                
            }
        }
        else{
			logWarning("handleList NULL");
            return;
        }
    }
    
    public void showRenameDialog(final MegaNode document, String text){
		logDebug("Node Handle: " + document.getHandle());
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);
        
        final EditTextCursorWatcher input = new EditTextCursorWatcher(this, document.isFolder());
        input.setSingleLine();
        input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        
        input.setImeActionLabel(getString(R.string.context_rename),EditorInfo.IME_ACTION_DONE);
        input.setText(text);
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, boolean hasFocus) {
                if (hasFocus) {
                    if (document.isFolder()){
                        input.setSelection(0, input.getText().length());
                    }
                    else{
                        String [] s = document.getName().split("\\.");
                        if (s != null){
                            int numParts = s.length;
                            int lastSelectedPos = 0;
                            if (numParts == 1){
                                input.setSelection(0, input.getText().length());
                            }
                            else if (numParts > 1){
                                for (int i=0; i<(numParts-1);i++){
                                    lastSelectedPos += s[i].length();
                                    lastSelectedPos++;
                                }
                                lastSelectedPos--; //The last point should not be selected)
                                input.setSelection(0, lastSelectedPos);
                            }
                        }
                        showKeyboardDelayed(v);
                    }
                }
            }
        });
        
        layout.addView(input, params);
        
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);
        
        final RelativeLayout error_layout = new RelativeLayout(ContactInfoActivityLollipop.this);
        layout.addView(error_layout, params1);
        
        final ImageView error_icon = new ImageView(ContactInfoActivityLollipop.this);
        error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
        error_layout.addView(error_icon);
        RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();
        
        
        params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        error_icon.setLayoutParams(params_icon);
        
        error_icon.setColorFilter(ContextCompat.getColor(ContactInfoActivityLollipop.this, R.color.login_warning));
        
        final TextView textError = new TextView(ContactInfoActivityLollipop.this);
        error_layout.addView(textError);
        RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
        params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
        params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
        textError.setLayoutParams(params_text_error);
        
        textError.setTextColor(ContextCompat.getColor(ContactInfoActivityLollipop.this, R.color.login_warning));
        
        error_layout.setVisibility(View.GONE);
        
        input.getBackground().mutate().clearColorFilter();
        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            
            }
            
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            
            }
            
            @Override
            public void afterTextChanged(Editable editable) {
                if(error_layout.getVisibility() == View.VISIBLE){
                    error_layout.setVisibility(View.GONE);
                    input.getBackground().mutate().clearColorFilter();
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(ContactInfoActivityLollipop.this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });
        
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
					logDebug("actionId is IME_ACTION_DONE");
                    String value = v.getText().toString().trim();
                    if (value.length() == 0) {
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(ContactInfoActivityLollipop.this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                        textError.setText(getString(R.string.invalid_string));
                        error_layout.setVisibility(View.VISIBLE);
                        input.requestFocus();
                    }
                    else{
                        rename(document, value);
                        renameDialog.dismiss();
                    }
                    return true;
                }
                return false;
            }
        });
        
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getString(R.string.context_rename) + " "	+ new String(document.getName()));
        builder.setPositiveButton(getString(R.string.context_rename),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString().trim();
                        if (value.length() == 0) {
                            return;
                        }
                        rename(document, value);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                input.getBackground().clearColorFilter();
            }
        });
        builder.setView(layout);
        renameDialog = builder.create();
        renameDialog.show();
        renameDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String value = input.getText().toString().trim();
                if (value.length() == 0) {
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(ContactInfoActivityLollipop.this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                    textError.setText(getString(R.string.invalid_string));
                    error_layout.setVisibility(View.VISIBLE);
                    input.requestFocus();
                }
                else{
                    rename(document, value);
                    renameDialog.dismiss();
                }
            }
        });
    }
    
    private void rename(MegaNode document, String newName) {
        if (newName.equals(document.getName())) {
            return;
        }
        
        if (!isOnline(this)) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
            return;
        }
        
        if (isFinishing()) {
            return;
        }
        
        ProgressDialog temp = null;
        try {
            temp = new ProgressDialog(this);
            temp.setMessage(getString(R.string.context_renaming));
            temp.show();
        } catch (Exception e) {
            return;
        }
        statusDialog = temp;

		logDebug("Renaming " + document.getName() + " to " + newName);
        
        megaApi.renameNode(document, newName, this);
    }
    
    public void setParentHandle(long parentHandle) {
        this.parentHandle = parentHandle;
    }
    
    private void setFoldersButtonText(ArrayList<MegaNode> nodes){
		if (nodes != null) {
			sharedFoldersButton.setText(getDescription(nodes));
			if (nodes.size() == 0) {
				sharedFoldersButton.setClickable(false);
				sharedFoldersLayout.setClickable(false);
			}
		}
    }
    
    public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes, final boolean highPriority){
        logDebug("askSizeConfirmationBeforeDownload");
        
        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;
        
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);
        
        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        
        confirmationLayout.addView(dontShowAgain, params);
        
        builder.setView(confirmationLayout);
        
        builder.setMessage(getString(R.string.alert_larger_file, getSizeString(sizeC)));
        builder.setPositiveButton(getString(R.string.general_save_to_device),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskSizeDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(ContactInfoActivityLollipop.this);
                        }
                        nC.checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(dontShowAgain.isChecked()){
                    dbH.setAttrAskSizeDownload("false");
                }
            }
        });
        
        downloadConfirmationDialog = builder.create();
        downloadConfirmationDialog.show();
    }
    
    public void askConfirmationNoAppInstaledBeforeDownload (String parentPath, String url, long size, long [] hashes, String nodeToDownload, final boolean highPriority){
        logDebug("askConfirmationNoAppInstaledBeforeDownload");
        
        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;
        
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);
        
        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        
        confirmationLayout.addView(dontShowAgain, params);
        
        builder.setView(confirmationLayout);
        
        builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
        builder.setPositiveButton(getString(R.string.general_save_to_device),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskNoAppDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(ContactInfoActivityLollipop.this);
                        }
                        nC.download(parentPathC, urlC, sizeC, hashesC, highPriority);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(dontShowAgain.isChecked()){
                    dbH.setAttrAskNoAppDownload("false");
                }
            }
        });
        downloadConfirmationDialog = builder.create();
        downloadConfirmationDialog.show();
    }
    
    
    @Override
    public void onUsersUpdate(MegaApiJava api,ArrayList<MegaUser> users) {
    
    }

	@Override
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		logDebug("onUserAlertsUpdate");
	}
    
    @Override
    public void onNodesUpdate(MegaApiJava api,ArrayList<MegaNode> nodeList) {
        if (sharedFoldersFragment != null){
            if (sharedFoldersFragment.isVisible()){
                sharedFoldersFragment.setNodes(parentHandle);
            }
        }
        ArrayList<MegaNode> nodes = megaApi.getInShares(user);
        setFoldersButtonText(nodes);
    }
    
    @Override
    public void onReloadNeeded(MegaApiJava api) {
    
    }
    
    @Override
    public void onAccountUpdate(MegaApiJava api) {
    
    }
    
    @Override
    public void onContactRequestsUpdate(MegaApiJava api,ArrayList<MegaContactRequest> requests) {
    
    }
    
    @Override
    public void onEvent(MegaApiJava api,MegaEvent event) {
    
    }

	@Override
	public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

	}

	@Override
	public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

	}

	@Override
	public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {
		logDebug("userhandle: " + userhandle + ", status: " + status + ", inProgress: " + inProgress);
		setContactPresenceStatus();
		requestLastGreen(status);
	}

	@Override
	public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {

	}

	@Override
	public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

	}

	@Override
	public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
		if(userhandle == user.getHandle()){
			logDebug("Update last green");

			int state = megaChatApi.getUserOnlineStatus(user.getHandle());

			if(state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID){
				String formattedDate = lastGreenDate(this, lastGreen);
				secondLineTextToolbar.setVisibility(View.VISIBLE);
				firstLineTextToolbar.setPadding(0, px2dp(6, outMetrics), 0, 0);
				secondLineTextToolbar.setText(formattedDate);
				secondLineTextToolbar.isMarqueeIsNecessary(this);
				logDebug("Date last green: " + formattedDate);
			}
		}
	}
}
