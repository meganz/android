package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
import mega.privacy.android.app.MegaContact;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
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


@SuppressLint("NewApi")
public class ContactInfoActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, OnClickListener, MegaRequestListenerInterface, OnCheckedChangeListener, OnItemClickListener {

	RelativeLayout imageLayout;
	android.app.AlertDialog permissionsDialog;
	ProgressDialog statusDialog;

	ContactInfoActivityLollipop contactInfoActivityLollipop;
	CoordinatorLayout fragmentContainer;
	CollapsingToolbarLayout collapsingToolbar;
	TextView initialLetter;
	ImageView contactPropertiesImage;
	LinearLayout optionsLayout;
	LinearLayout notificationsLayout;
	SwitchCompat notificationsSwitch;
	TextView notificationsTitle;
	View dividerNotificationsLayout;

	RelativeLayout messageSoundLayout;
	TextView messageSoundText;
	View dividerMessageSoundLayout;

	RelativeLayout ringtoneLayout;
	TextView ringtoneText;
	View dividerRingtoneLayout;

	RelativeLayout sharedFoldersLayout;
	ImageView sharedFoldersIcon;
	TextView sharedFoldersText;
	Button sharedFoldersButton;
	View dividerSharedFoldersLayout;

	RelativeLayout shareContactLayout;
	RelativeLayout shareContactContentLayout;
	TextView shareContactText;
	ImageView shareContactIcon;
	View dividerShareContactLayout;

	RelativeLayout clearChatLayout;

	Toolbar toolbar;
	ActionBar aB;

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
	ChatItemPreferences chatPrefs = null;

	MenuItem shareMenuItem;
	MenuItem viewFoldersMenuItem;
	MenuItem startConversationMenuItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		log("onCreate");
		contactInfoActivityLollipop = this;
		if (megaApi == null) {
			MegaApplication app = (MegaApplication) getApplication();
			megaApi = app.getMegaApi();
		}

		if(Util.isChatEnabled()){
			if (megaChatApi == null) {
				MegaApplication app = (MegaApplication) getApplication();
				megaChatApi = app.getMegaChatApi();
			}
		}

		handler = new Handler();

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
			setSupportActionBar(toolbar);
			aB = getSupportActionBar();
			imageLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_image_layout);
			collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapse_toolbar);

			collapsingToolbar.setExpandedTitleMarginBottom(Util.scaleHeightPx(24, outMetrics));
			collapsingToolbar.setExpandedTitleMarginStart(Util.scaleWidthPx(72, outMetrics));
			getSupportActionBar().setDisplayShowTitleEnabled(false);

			collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));
			collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.white));

			aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
			aB.setHomeButtonEnabled(true);
			aB.setDisplayHomeAsUpEnabled(true);

			contactPropertiesImage = (ImageView) findViewById(R.id.chat_contact_properties_toolbar_image);
			initialLetter = (TextView) findViewById(R.id.chat_contact_properties_toolbar_initial_letter);

			dbH = DatabaseHandler.getDbHandler(getApplicationContext());

			chatHandle = extras.getLong("handle",-1);
			if (chatHandle != -1) {
				log("From chat!!");
				fromContacts = false;
				chat = megaChatApi.getChatRoom(chatHandle);

				long userHandle = chat.getPeerHandle(0);

				String userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle);
				user = megaApi.getContact(userHandleEncoded);

				chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chatHandle));

				collapsingToolbar.setTitle(chat.getTitle());
				setDefaultAvatar(chat.getTitle());
			}
			else{
				log("From contacts!!");
				fromContacts = true;
				userEmailExtra = extras.getString("name");
				user = megaApi.getContact(userEmailExtra);

				String fullName = "";
				if(user!=null){
					MegaContact contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
					if(contactDB!=null){

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
							String email = user.getEmail();
							String[] splitEmail = email.split("[@._]");
							fullName = splitEmail[0];
						}

						collapsingToolbar.setTitle(fullName);
					}
					else{
						log("The contactDB is null: ");
					}
				}

				//Find chat with this contact
				if(Util.isChatEnabled()){
					chat = megaChatApi.getChatRoomByUser(user.getHandle());
					if(chat!=null){
						chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chat.getChatId()));
					}
				}

				setDefaultAvatar(fullName);
			}

			setAvatar();

			//OPTIONS LAYOUT
			optionsLayout = (LinearLayout) findViewById(R.id.chat_contact_properties_options);

			//Notifications Layout

			notificationsLayout = (LinearLayout) findViewById(R.id.chat_contact_properties_notifications_layout);
			notificationsLayout.setVisibility(View.VISIBLE);

			notificationsTitle = (TextView) findViewById(R.id.chat_contact_properties_notifications_text);

			notificationsSwitch = (SwitchCompat) findViewById(R.id.chat_contact_properties_switch);
			notificationsSwitch.setOnCheckedChangeListener(this);
			LinearLayout.LayoutParams paramsSwitch = (LinearLayout.LayoutParams) notificationsSwitch.getLayoutParams();
			paramsSwitch.rightMargin = Util.scaleWidthPx(16, outMetrics);
			notificationsSwitch.setLayoutParams(paramsSwitch);

			dividerNotificationsLayout = (View) findViewById(R.id.divider_notifications_layout);

			//Chat message sound Layout

			messageSoundLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_messages_sound_layout);
			messageSoundLayout.setOnClickListener(this);
			LinearLayout.LayoutParams paramsSound = (LinearLayout.LayoutParams) messageSoundLayout.getLayoutParams();
			paramsSound.leftMargin = Util.scaleWidthPx(72, outMetrics);
			messageSoundLayout.setLayoutParams(paramsSound);

			messageSoundText = (TextView) findViewById(R.id.chat_contact_properties_messages_sound);

			dividerMessageSoundLayout = (View) findViewById(R.id.divider_message_sound_layout);
			LinearLayout.LayoutParams paramsDividerSound = (LinearLayout.LayoutParams) dividerMessageSoundLayout.getLayoutParams();
			paramsDividerSound.leftMargin = Util.scaleWidthPx(72, outMetrics);
			dividerMessageSoundLayout.setLayoutParams(paramsDividerSound);

			//Call ringtone Layout

			ringtoneLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_ringtone_layout);
			ringtoneLayout.setOnClickListener(this);
			LinearLayout.LayoutParams paramsRingtone = (LinearLayout.LayoutParams) ringtoneLayout.getLayoutParams();
			paramsRingtone.leftMargin = Util.scaleWidthPx(72, outMetrics);
			ringtoneLayout.setLayoutParams(paramsRingtone);

			ringtoneText = (TextView) findViewById(R.id.chat_contact_properties_ringtone);

			dividerRingtoneLayout = (View) findViewById(R.id.divider_ringtone_layout);

			//Shared folders layout
			sharedFoldersLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_shared_folders_layout);
			sharedFoldersLayout.setOnClickListener(this);

			sharedFoldersIcon = (ImageView) findViewById(R.id.chat_contact_properties_shared_folder_icon);
			RelativeLayout.LayoutParams paramsSharedFoldersIcon = (RelativeLayout.LayoutParams) sharedFoldersIcon.getLayoutParams();
			paramsSharedFoldersIcon.leftMargin = Util.scaleWidthPx(16, outMetrics);
			sharedFoldersIcon.setLayoutParams(paramsSharedFoldersIcon);

			sharedFoldersText = (TextView) findViewById(R.id.chat_contact_properties_shared_folders_label);
			RelativeLayout.LayoutParams paramsSharedFoldersText = (RelativeLayout.LayoutParams) sharedFoldersText.getLayoutParams();
			paramsSharedFoldersText.leftMargin = Util.scaleWidthPx(32, outMetrics);
			sharedFoldersText.setLayoutParams(paramsSharedFoldersText);

			sharedFoldersButton = (Button) findViewById(R.id.chat_contact_properties_shared_folders_button);
			sharedFoldersButton.setOnClickListener(this);
			RelativeLayout.LayoutParams paramsSharedFoldersButton = (RelativeLayout.LayoutParams) sharedFoldersButton.getLayoutParams();
			paramsSharedFoldersButton.rightMargin = Util.scaleWidthPx(16, outMetrics);
			sharedFoldersButton.setLayoutParams(paramsSharedFoldersButton);

			sharedFoldersButton.setText(getDescription(megaApi.getInShares(user)));

			dividerSharedFoldersLayout = (View) findViewById(R.id.divider_shared_folder_layout);
			LinearLayout.LayoutParams paramsSharedFoldersDivider = (LinearLayout.LayoutParams) dividerSharedFoldersLayout.getLayoutParams();
			paramsSharedFoldersDivider.leftMargin = Util.scaleWidthPx(72, outMetrics);
			dividerSharedFoldersLayout.setLayoutParams(paramsSharedFoldersDivider);

			//Share Contact Layout

			shareContactLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_share_contact_layout);
			shareContactLayout.setOnClickListener(this);

			shareContactIcon = (ImageView) findViewById(R.id.chat_contact_properties_email_icon);
			RelativeLayout.LayoutParams paramsShareContactIcon = (RelativeLayout.LayoutParams) shareContactIcon.getLayoutParams();
			paramsShareContactIcon.leftMargin = Util.scaleWidthPx(16, outMetrics);
			shareContactIcon.setLayoutParams(paramsShareContactIcon);

			shareContactContentLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_share_contact_content);
			RelativeLayout.LayoutParams paramsShareContact = (RelativeLayout.LayoutParams) shareContactContentLayout.getLayoutParams();
			paramsShareContact.leftMargin = Util.scaleWidthPx(32, outMetrics);
			shareContactContentLayout.setLayoutParams(paramsShareContact);

			shareContactText = (TextView) findViewById(R.id.chat_contact_properties_share_contact);
			shareContactText.setText(user.getEmail());

			dividerShareContactLayout = (View) findViewById(R.id.divider_share_contact_layout);
			LinearLayout.LayoutParams paramsShareContactDivider = (LinearLayout.LayoutParams) dividerShareContactLayout.getLayoutParams();
			paramsShareContactDivider.leftMargin = Util.scaleWidthPx(72, outMetrics);
			dividerShareContactLayout.setLayoutParams(paramsShareContactDivider);

			//Clear chat Layout
			clearChatLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_clear_layout);
			clearChatLayout.setOnClickListener(this);
			LinearLayout.LayoutParams paramsClearChat = (LinearLayout.LayoutParams) clearChatLayout.getLayoutParams();
			paramsClearChat.leftMargin = Util.scaleWidthPx(72, outMetrics);
			clearChatLayout.setLayoutParams(paramsClearChat);

			if(chat!=null){
				clearChatLayout.setVisibility(View.VISIBLE);
				dividerShareContactLayout.setVisibility(View.VISIBLE);
			}
			else{
				clearChatLayout.setVisibility(View.GONE);
				dividerShareContactLayout.setVisibility(View.GONE);
			}

			if(Util.isChatEnabled()){
				//SET Preferences (if exist)
				if(chatPrefs!=null){

					boolean notificationsEnabled = true;
					if (chatPrefs.getNotificationsEnabled() != null){
						notificationsEnabled = Boolean.parseBoolean(chatPrefs.getNotificationsEnabled());
					}
					notificationsSwitch.setChecked(notificationsEnabled);

					if(!notificationsEnabled){
						ringtoneText.setTextColor(ContextCompat.getColor(this, R.color.accentColorTransparent));
						messageSoundText.setTextColor(ContextCompat.getColor(this, R.color.accentColorTransparent));
					}

					String ringtoneString = chatPrefs.getRingtone();
					Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(ringtoneString));
					String title = ringtone.getTitle(this);
					ringtoneText.setText(title);

					String soundString = chatPrefs.getNotificationsSound();
					Ringtone sound = RingtoneManager.getRingtone(this, Uri.parse(soundString));
					String titleSound = sound.getTitle(this);
					messageSoundText.setText(titleSound);

				}
				else{
					Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
					Ringtone defaultRingtone = RingtoneManager.getRingtone(this, defaultRingtoneUri);
					ringtoneText.setText(defaultRingtone.getTitle(this));

					Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
					Ringtone defaultSound = RingtoneManager.getRingtone(this, defaultSoundUri);
					messageSoundText.setText(defaultSound.getTitle(this));

					notificationsSwitch.setChecked(true);
				}

				notificationsLayout.setVisibility(View.VISIBLE);
				dividerNotificationsLayout.setVisibility(View.VISIBLE);
				ringtoneLayout.setVisibility(View.VISIBLE);
				dividerRingtoneLayout.setVisibility(View.VISIBLE);
				messageSoundLayout.setVisibility(View.VISIBLE);
				dividerMessageSoundLayout.setVisibility(View.VISIBLE);
			}
			else{
				notificationsLayout.setVisibility(View.GONE);
				dividerNotificationsLayout.setVisibility(View.GONE);
				ringtoneLayout.setVisibility(View.GONE);
				dividerRingtoneLayout.setVisibility(View.GONE);
				messageSoundLayout.setVisibility(View.GONE);
				dividerMessageSoundLayout.setVisibility(View.GONE);
			}

		} else {
			log("Extras is NULL");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contact_properties_action, menu);

		shareMenuItem = menu.findItem(R.id.cab_menu_share_folder);
		viewFoldersMenuItem = menu.findItem(R.id.cab_menu_view_shares);
		startConversationMenuItem = menu.findItem(R.id.cab_menu_start_conversation);

		ArrayList<MegaNode> shares = megaApi.getInShares(user);
		if(shares!=null){
			if(shares.size()>0){
				viewFoldersMenuItem.setVisible(true);
			}
			else{
				viewFoldersMenuItem.setVisible(false);
			}
		}
		else{
			viewFoldersMenuItem.setVisible(false);
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

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");
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
			case R.id.cab_menu_view_shares:{
				Intent i = new Intent(this, ContactFileListActivityLollipop.class);
				i.putExtra("name", user.getEmail());
				this.startActivity(i);
				break;
			}
			case R.id.cab_menu_start_conversation:{
				showSnackbar("Start conversation");
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
						Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
						intentOpenChat.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
						intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
						this.startActivity(intentOpenChat);
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
					initialLetter.setVisibility(View.GONE);

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

		int avatarTextSize = getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);

		boolean setInitialByMail = false;

		if (title != null) {
			if (title.trim().length() > 0) {
				String firstLetter = title.charAt(0) + "";
				firstLetter = firstLetter.toUpperCase(Locale.getDefault());
				initialLetter.setText(firstLetter);
				initialLetter.setTextSize(100);
				initialLetter.setTextColor(Color.WHITE);
				initialLetter.setVisibility(View.VISIBLE);
			} else {
				setInitialByMail = true;
			}
		} else {
			setInitialByMail = true;
		}
		if (setInitialByMail) {
			if (user.getEmail() != null) {
				if (user.getEmail().length() > 0) {
					log("email TEXT: " + user.getEmail());
					log("email TEXT AT 0: " + user.getEmail().charAt(0));
					String firstLetter = user.getEmail().charAt(0) + "";
					firstLetter = firstLetter.toUpperCase(Locale.getDefault());
					initialLetter.setText(firstLetter);
					initialLetter.setTextSize(100);
					initialLetter.setTextColor(Color.WHITE);
					initialLetter.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private int getAvatarTextSize(float density) {
		float textSize = 0.0f;

		if (density > 3.0) {
			textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
		} else if (density > 2.0) {
			textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
		} else if (density > 1.5) {
			textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
		} else if (density > 1.0) {
			textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
		} else if (density > 0.75) {
			textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
		} else {
			textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f);
		}

		return (int) textSize;
	}

	@Override
	public void onClick(View v) {

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
			case R.id.chat_contact_properties_share_contact_layout: {
				log("Share contact option");
				showSnackbar("Coming soon...");
				break;
			}
			case R.id.chat_contact_properties_ringtone_layout: {
				log("Ringtone option");

				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.call_ringtone_title));
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
				this.startActivityForResult(intent, Constants.SELECT_RINGTONE);

				break;
			}
			case R.id.chat_contact_properties_messages_sound_layout: {
				log("Message sound option");

				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_sound_title));
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
				this.startActivityForResult(intent, Constants.SELECT_NOTIFICATION_SOUND);
				break;
			}
			case R.id.chat_contact_properties_shared_folders_button:
			case R.id.chat_contact_properties_shared_folders_layout:{
				Intent i = new Intent(this, ContactFileListActivityLollipop.class);
				i.putExtra("name", user.getEmail());
				this.startActivity(i);
				break;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		log("onActivityResult, resultCode: "+resultCode);

		if (resultCode == RESULT_OK && requestCode == Constants.SELECT_RINGTONE)
		{
			log("Selected ringtone OK");

			Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
			String title = ringtone.getTitle(this);

			if(title!=null){
				log("Title ringtone: "+title);
				ringtoneText.setText(title);
			}

			if (uri != null)
			{
				String chosenRingtone = uri.toString();
				if(chatPrefs==null){
					Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);

					chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(true), chosenRingtone, defaultSoundUri.toString());
					dbH.setChatItemPreferences(chatPrefs);
				}
				else{
					chatPrefs.setRingtone(chosenRingtone);
					dbH.setRingtoneChatItem(chosenRingtone, Long.toString(chatHandle));
				}
			}
			else
			{
				log("Error not chosen ringtone");
			}
		}
		else if (resultCode == RESULT_OK && requestCode == Constants.SELECT_NOTIFICATION_SOUND)
		{
			log("Selected notification sound OK");

			Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			Ringtone sound = RingtoneManager.getRingtone(this, uri);
			String title = sound.getTitle(this);

			if(title!=null){
				log("Title sound notification: "+title);
				messageSoundText.setText(title);
			}

			if (uri != null)
			{
				String chosenSound = uri.toString();
				if(chatPrefs==null){
					Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);

					chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(true), defaultRingtoneUri.toString(), chosenSound);
					dbH.setChatItemPreferences(chatPrefs);
				}
				else{
					chatPrefs.setNotificationsSound(chosenSound);
					dbH.setNotificationSoundChatItem(chosenSound, Long.toString(chatHandle));
				}
			}
			else
			{
				log("Error not chosen notification sound");
			}
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {

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
				alertTitle.setTextColor(resources.getColor(R.color.mega));
				/*int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
				View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
				titleDivider.setBackgroundColor(resources.getColor(R.color.mega));*/
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);

	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		log("onCheckedChanged");

		notificationsSwitch.setChecked(isChecked);

		if(!isChecked){
			ringtoneText.setTextColor(ContextCompat.getColor(this, R.color.accentColorTransparent));
			messageSoundText.setTextColor(ContextCompat.getColor(this, R.color.accentColorTransparent));
		}
		else{
			ringtoneText.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
			messageSoundText.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
		}

		if(chatPrefs==null){
			Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
			Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);

			chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(isChecked), defaultRingtoneUri.toString(), defaultSoundUri.toString());
			dbH.setChatItemPreferences(chatPrefs);
		}
		else{
			chatPrefs.setNotificationsEnabled(Boolean.toString(isChecked));
			dbH.setNotificationEnabledChatItem(Boolean.toString(isChecked), Long.toString(chatHandle));
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
							initialLetter.setVisibility(View.GONE);

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
		Util.log("ContactChatInfoActivityLollipop", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResume() {
		log("onResume-ContactChatInfoActivityLollipop");
		super.onResume();
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
				Intent intent = new Intent(this, ChatActivityLollipop.class);
				intent.setAction(Constants.ACTION_CHAT_NEW);
				intent.putExtra("CHAT_ID", request.getChatHandle());
				this.startActivity(intent);
				finish();
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
