package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
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
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.adapters.MegaContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.AskForDisplayOverDialog;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static android.graphics.Color.WHITE;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;

public class ContactsFragmentLollipop extends Fragment implements MegaRequestListenerInterface, View.OnClickListener{

	public static int GRID_WIDTH =400;
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150;
	
	public static final String ARG_OBJECT = "object";

	String myEmail;
	private RoundedImageView avatarImage;
	private TextView contactName;
	private TextView contactMail;
	private Button invite;
	private Button view;
	private TextView initialLetterInvite;
	private TextView dialogTitle;
	private TextView dialogText;
	private Button dialogButton;
	private AlertDialog inviteAlertDialog;
	private AlertDialog requestedAlertDialog;
	private long handleContactLink = -1;
	
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	MyAccountInfo myAccountInfo;
	TextView initialLetter;

	Context context;
	RecyclerView recyclerView;
	MegaContactsLollipopAdapter adapter;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

	private ActionMode actionMode;
	DatabaseHandler dbH = null;

//	DatabaseHandler dbH = null;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;

	ContactsFragmentLollipop contactsFragment = this;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaContactAdapter> visibleContacts = new ArrayList<MegaContactAdapter>();
	
	MegaUser selectedUser = null;

	private boolean isContact = false;
	private boolean inviteShown = false;
	private boolean dialogshown = false;

	public int dialogTitleContent = -1;
	public int dialogTextContent = -1;
	private String contactNameContent;

	private Bitmap avatarSave;
	private String initialLetterSave;
	private boolean contentAvatar = false;
	private boolean success;

	private MegaUser userQuery;

	private AskForDisplayOverDialog askForDisplayOverDialog;

	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_QUERY){
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Contact link query " + request.getNodeHandle() + "_" + MegaApiAndroid.handleToBase64(request.getNodeHandle()) + "_" + request.getEmail() + "_" + request.getName() + "_" + request.getText());

				myEmail = request.getEmail();
				handleContactLink = request.getNodeHandle();
				contactNameContent = request.getName() + " " + request.getText();

				dbH.setLastPublicHandle(handleContactLink);
				dbH.setLastPublicHandleTimeStamp();
				dbH.setLastPublicHandleType(MegaApiJava.AFFILIATE_TYPE_CONTACT);

				userQuery = queryIfIsContact();
				showInviteDialog();
			}
			else if (e.getErrorCode() == MegaError.API_EEXIST){
				dialogTitleContent = R.string.invite_not_sent;
				dialogTextContent = R.string.invite_not_sent_text_already_contact;
				showAlertDialog(dialogTitleContent, dialogTextContent, true);
			}
			else {
				dialogTitleContent = R.string.invite_not_sent;
				dialogTextContent = R.string.invite_not_sent_text;
				showAlertDialog(dialogTitleContent, dialogTextContent, false);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {
			if (e.getErrorCode() == MegaError.API_OK) {
				logDebug("Get user avatar OK");
				setAvatar();
			}
			else {
				logWarning("Get user avatal FAIL");
				setDefaultAvatar();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (inviteShown){
			outState.putBoolean("inviteShown",inviteShown);
			outState.putString("contactNameContent", contactNameContent);
			outState.putBoolean("isContact", isContact);
		}
		if (dialogshown){
			outState.putBoolean("dialogshown", dialogshown);
			outState.putInt("dialogTitleContent", dialogTitleContent);
			outState.putInt("dialogTextContent", dialogTextContent);
		}
		if (dialogshown || inviteShown){
			outState.putString("myEmail", myEmail);
			outState.putLong("handleContactLink", handleContactLink);
			outState.putBoolean("success", success);
			if (avatarImage != null){
				avatarImage.buildDrawingCache(true);
				Bitmap avatarBitmap = avatarImage.getDrawingCache(true);

				if (avatarBitmap != null) {
					ByteArrayOutputStream avatarOutputStream = new ByteArrayOutputStream();
					avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, avatarOutputStream);
					byte[] avatarByteArray = avatarOutputStream.toByteArray();
					outState.putByteArray("avatar", avatarByteArray);
					outState.putBoolean("contentAvatar", contentAvatar);
				}
			}
			if (!contentAvatar && initialLetterInvite != null){
				outState.putString("initialLetter", initialLetterInvite.getText().toString());
			}
		}
	}

	void showInviteDialog (){
		if (inviteAlertDialog != null){
			contactName.setText(contactNameContent);
			if (isContact){
				contactMail.setText(getResources().getString(R.string.context_contact_already_exists, myEmail));
				invite.setVisibility(View.GONE);
				view.setVisibility(View.VISIBLE);
			}
			else {
				contactMail.setText(myEmail);
				invite.setVisibility(View.VISIBLE);
				view.setVisibility(View.GONE);
			}
			setAvatar();
		}
		else {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			LayoutInflater inflater = getActivity().getLayoutInflater();

			View v = inflater.inflate(R.layout.dialog_accept_contact, null);
			builder.setView(v);
			invite = (Button) v.findViewById(R.id.accept_contact_invite);
			invite.setOnClickListener(this);
			view = (Button) v.findViewById(R.id.view_contact);
			view.setOnClickListener(this);

			avatarImage = (RoundedImageView) v.findViewById(R.id.accept_contact_avatar);
			initialLetterInvite = (TextView) v.findViewById(R.id.accept_contact_initial_letter);
			contactName = (TextView) v.findViewById(R.id.accept_contact_name);
			contactMail = (TextView) v.findViewById(R.id.accept_contact_mail);

			if (avatarSave != null){
				avatarImage.setImageBitmap(avatarSave);
				if (contentAvatar){
					initialLetterInvite.setVisibility(View.GONE);
				}
				else {
					if (initialLetterSave != null) {
						initialLetterInvite.setText(initialLetterSave);
						initialLetterInvite.setTextSize(30);
						initialLetterInvite.setTextColor(WHITE);
						initialLetterInvite.setVisibility(View.VISIBLE);
					}
					else {
						setAvatar();
					}
				}
			}
			else {
				setAvatar();
			}

			if (isContact){
				contactMail.setText(getResources().getString(R.string.context_contact_already_exists, myEmail));
				invite.setVisibility(View.GONE);
				view.setVisibility(View.VISIBLE);
			}
			else {
				contactMail.setText(myEmail);
				invite.setVisibility(View.VISIBLE);
				view.setVisibility(View.GONE);
			}
			inviteAlertDialog = builder.create();
			inviteAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					logDebug("onDismiss");
					inviteShown = false;
				}
			});
			contactName.setText(contactNameContent);
		}
		inviteAlertDialog.show();
		inviteShown = true;
	}

	MegaUser queryIfIsContact() {

		ArrayList<MegaUser> contacts = megaApi.getContacts();

		for (int i=0; i<contacts.size(); i++){
			if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){
				logDebug("Contact[" + i + "] Handle: " +contacts.get(i).getHandle());
				if (contacts.get(i).getEmail().equals(myEmail)){
					isContact = true;
					return contacts.get(i);
				}
			}
		}
		isContact = false;
		return null;
	}

	public void setAvatar(){
		logDebug("updateAvatar");
		if (!isContact){
			setDefaultAvatar();
		}
		else {
			File avatar = null;
			if(context!=null){
				logDebug("Context is not null");
                avatar = buildAvatarFile(context, myEmail + ".jpg");
			}
			else{
				logWarning("context is null!!!");
				if(getActivity()!=null){
					logDebug("getActivity is not null");
                    avatar = buildAvatarFile(getActivity(), myEmail + ".jpg");
				}
				else{
					logWarning("getActivity is ALSO null");
					return;
				}
			}

			if(isFileAvailable(avatar)){
				setProfileAvatar(avatar);
			}
			else{
				setDefaultAvatar();
			}
		}
	}

	public void setProfileAvatar(File avatar){
		logDebug("setProfileAvatar");

		Bitmap imBitmap = null;
		if (avatar.exists()){
			if (avatar.length() > 0){
				logDebug("My avatar exists!");
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (imBitmap == null) {
					avatar.delete();
					logDebug("Call to getUserAvatar");
					setDefaultAvatar();
				}
				else{
					logDebug("Show my avatar");
					avatarImage.setImageBitmap(imBitmap);
					initialLetterInvite.setVisibility(View.GONE);
				}
			}
		}else{
			logDebug("My avatar NOT exists!");
			logDebug("Call to getUserAvatar");
			logDebug("DO NOT Retry!");
			megaApi.getUserAvatar(myEmail, avatar.getPath(), this);
//			setDefaultAvatar();
		}
	}

	public void setDefaultAvatar(){
		logDebug("setDefaultAvatar");
		Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);

		if (isContact && userQuery != null){
			String color = megaApi.getUserAvatarColor(userQuery);
			if(color!=null){
				logDebug("The color to set the avatar is " + color);
				p.setColor(Color.parseColor(color));
			}
			else{
				logDebug("Default color to the avatar");
				p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
			}
		}
		else {
			p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
		}

		int radius;
		if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
			radius = defaultAvatar.getWidth()/2;
		else
			radius = defaultAvatar.getHeight()/2;

		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		avatarImage.setImageBitmap(defaultAvatar);

		float density = ((Activity) context).getResources().getDisplayMetrics().density;
		int avatarTextSize = getAvatarTextSize(density);
		logDebug("DENSITY: " + density + ":::: " + avatarTextSize);
		String fullName = "";
		if(contactName.getText() != null){
			fullName = contactName.getText().toString();
		}
		else{
			//No name, ask for it and later refresh!!
			fullName = myEmail;
		}
		String firstLetter = fullName.charAt(0) + "";
		firstLetter = firstLetter.toUpperCase(Locale.getDefault());

		initialLetterInvite.setText(firstLetter);
		initialLetterInvite.setTextSize(30);
		initialLetterInvite.setTextColor(WHITE);
		initialLetterInvite.setVisibility(View.VISIBLE);
	}

	private int getAvatarTextSize (float density){
		float textSize = 0.0f;

		if (density > 3.0){
			textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
		}
		else if (density > 2.0){
			textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
		}
		else if (density > 1.5){
			textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
		}
		else if (density > 1.0){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
		}
		else if (density > 0.75){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
		}
		else{
			textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f);
		}

		return (int)textSize;
	}

	public void invite (long handle){
		logDebug("Handle: " + handle);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		LayoutInflater inflater = getActivity().getLayoutInflater();

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		megaApi.contactLinkQuery(handle, this);

		View v = inflater.inflate(R.layout.dialog_accept_contact, null);
		builder.setView(v);

		invite = (Button) v.findViewById(R.id.accept_contact_invite);
		invite.setOnClickListener(this);
		view = (Button) v.findViewById(R.id.view_contact);
		view.setOnClickListener(this);

		avatarImage = (RoundedImageView) v.findViewById(R.id.accept_contact_avatar);
		initialLetterInvite = (TextView) v.findViewById(R.id.accept_contact_initial_letter);
		contactName = (TextView) v.findViewById(R.id.accept_contact_name);
		contactMail = (TextView) v.findViewById(R.id.accept_contact_mail);

		inviteAlertDialog = builder.create();
		inviteAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				inviteShown = false;
			}
		});

		((ManagerActivityLollipop) context).deleteInviteContactHandle();
	}

	public void showAlertDialog (int title, int text, final boolean success) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View v = inflater.inflate(R.layout.dialog_invite, null);
		builder.setView(v);

		dialogTitle = (TextView) v.findViewById(R.id.dialog_invite_title);
		dialogText = (TextView) v.findViewById(R.id.dialog_invite_text);
		dialogButton = (Button) v.findViewById(R.id.dialog_invite_button);
		dialogButton.setOnClickListener(this);

		this.success = success;

		if (dialogTitleContent == -1){
			dialogTitleContent = title;
		}
		if (dialogTextContent == -1) {
			dialogTextContent = text;
		}
		dialogTitle.setText(getResources().getString(dialogTitleContent));
		if (success){
			dialogText.setText(getResources().getString(dialogTextContent, myEmail));
		}
		else {
			dialogText.setText(getResources().getString(dialogTextContent));
		}

		requestedAlertDialog = builder.create();
		requestedAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (success){
					dialogshown = false;
				}
			}
		});
		dialogshown = true;
		requestedAlertDialog.show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.accept_contact_invite: {
				inviteShown = false;
				megaApi.inviteContact(myEmail, null, MegaContactRequest.INVITE_ACTION_ADD, handleContactLink, (ManagerActivityLollipop) context);
				if (inviteAlertDialog != null){
					inviteAlertDialog.dismiss();
				}
				break;
			}
			case R.id.dialog_invite_button: {
				dialogshown = false;
				if (requestedAlertDialog != null){
					requestedAlertDialog.dismiss();
				}
				break;
			}
			case R.id.view_contact: {
				inviteShown = false;
				if (inviteAlertDialog != null){
					inviteAlertDialog.dismiss();
				}
				Intent intent = new Intent(context, ContactInfoActivityLollipop.class);
				intent.putExtra("name", myEmail);
				startActivity(intent);
				break;
			}
		}
	}

	/////Multiselect/////
	private class ActionBarCallBack implements ActionMode.Callback {
//		
//		boolean selectAll = true;
//		boolean unselectAll = false;
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			ArrayList<MegaUser> users = adapter.getSelectedUsers();

			switch(item.getItemId()){
				case R.id.cab_menu_share_folder:{

					if (users.size()>0){
						ContactController cC = new ContactController(context);
						cC.pickFolderToShare(users);
						clearSelections();
						hideMultipleSelect();
					}

					break;
				}
				case R.id.cab_menu_send_file:{

					if (users.size()>0){
						ContactController cC = new ContactController(context);
						cC.pickFileToSend(users);
						clearSelections();
						hideMultipleSelect();
					}										
					break;
				}
				case R.id.cab_menu_start_conversation:{

					if(users.get(0)==null){
						logWarning("Selected contact NULL");
						break;
					}
					ArrayList<String> contactsNewGroup = new ArrayList<>();
					for(int i=0;i<users.size();i++){
						contactsNewGroup.add(users.get(i).getEmail());
					}

					Intent intent = new Intent(context, AddContactActivityLollipop.class);
					intent.putStringArrayListExtra("contactsNewGroup", contactsNewGroup);
					intent.putExtra("newGroup", true);
					intent.putExtra("contactType", CONTACT_TYPE_MEGA);
					((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CREATE_CHAT);

					clearSelections();
					hideMultipleSelect();

					break;
				}
				case R.id.cab_menu_delete:{
					((ManagerActivityLollipop)context).showConfirmationRemoveContacts(users);
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_send_to_chat:{
					ChatController cC = new ChatController(context);
					cC.selectChatsToAttachContacts(users);

					clearSelections();
					hideMultipleSelect();
					break;
				}
			}
			return false;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.contact_fragment_action, menu);
			((ManagerActivityLollipop)context).hideFabButton();
            ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
			checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			logDebug("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
			((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ZERO_DELAY);
			checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaUser> selected = adapter.getSelectedUsers();
			MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
			menu.findItem(R.id.cab_menu_send_file).setIcon(mutateIconSecondary(context, R.drawable.ic_send_to_contact, R.color.white));
			if (selected.size() != 0) {
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_share_folder).setVisible(true);
				menu.findItem(R.id.cab_menu_share_folder).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_send_file).setVisible(true);
				menu.findItem(R.id.cab_menu_send_file).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_start_conversation).setVisible(true);
				menu.findItem(R.id.cab_menu_start_conversation).setIcon(mutateIconSecondary(context, R.drawable.ic_chat, R.color.white));
				menu.findItem(R.id.cab_menu_start_conversation).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_send_to_chat).setVisible(true);
				menu.findItem(R.id.cab_menu_send_to_chat).setIcon(mutateIconSecondary(getContext(), R.drawable.ic_share_contact, R.color.white));
				menu.findItem(R.id.cab_menu_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
			}	
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);	
			}
			menu.findItem(R.id.cab_menu_help).setVisible(false);
			menu.findItem(R.id.cab_menu_upgrade_account).setVisible(false);
			//menu.findItem(R.id.cab_menu_settings).setVisible(false);
//			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			return false;
		}		
	}

	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		logDebug("hideMultipleSelect");
		if(adapter!=null){
			adapter.setMultipleSelect(false);
		}
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public void selectAll(){
		if (adapter != null){
			if(adapter.isMultipleSelect()){
				adapter.selectAll();
			}
			else{
				adapter.setMultipleSelect(true);
				adapter.selectAll();
				
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
			}
			
			updateActionModeTitle();
		}
	}
	
	/*
	 * Clear all selected items
	 */
	public void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
	}

	public void clearSelectionsNoAnimations() {
		adapter.clearSelectionsNoAnimations();

	}
	
	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaUser> users = adapter.getSelectedUsers();
		
		Resources res = getResources();
		String format = "%d";
		
		actionMode.setTitle(String.format(format, users.size()));

		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			logError("Invalidate error", e);
		}
	}
		
	//End Multiselect/////

	public static ContactsFragmentLollipop newInstance() {
		logDebug("newInstance");
		ContactsFragmentLollipop fragment = new ContactsFragmentLollipop();
		return fragment;
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		logDebug("onCreate");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (megaChatApi == null) {
			megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);

		if (savedInstanceState != null){
			isContact = savedInstanceState.getBoolean("isContact", false);
			inviteShown = savedInstanceState.getBoolean("inviteShown", false);
			dialogshown = savedInstanceState.getBoolean("dialogshown", false);
			dialogTitleContent = savedInstanceState.getInt("dialogTitleContent", -1);
			dialogTextContent = savedInstanceState.getInt("dialogTextContent", -1);
			contactNameContent = savedInstanceState.getString("contactNameContent");
			success = savedInstanceState.getBoolean("success", true);
			myEmail = savedInstanceState.getString("myEmail");
			handleContactLink = savedInstanceState.getLong("handleContactLink", 0);

			byte[] avatarByteArray = savedInstanceState.getByteArray("avatar");
			if (avatarByteArray != null){
				avatarSave = BitmapFactory.decodeByteArray(avatarByteArray, 0, avatarByteArray.length);
				contentAvatar = savedInstanceState.getBoolean("contentAvatar", false);
				if (!contentAvatar){
					initialLetterSave = savedInstanceState.getString("initialLetter");
				}
			}
		}

		askForDisplayOverDialog = new AskForDisplayOverDialog(context);
	}

	public void checkScroll () {
		if (recyclerView != null) {
			if (recyclerView.canScrollVertically(-1) || (adapter != null && adapter.isMultipleSelect())) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			}
			else {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		logDebug("onCreateView");

		if(myAccountInfo == null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;

		if (((ManagerActivityLollipop)context).isList()){
			logDebug("isList View");
			View v = inflater.inflate(R.layout.fragment_contactslist, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.contacts_list_view);
			recyclerView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			recyclerView.setHasFixedSize(true);
			LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
		    recyclerView.setLayoutManager(linearLayoutManager);
		    recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = (ImageView) v.findViewById(R.id.contact_list_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.contact_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.contact_list_empty_text_first);

			setContacts(megaApi.getContacts());
			if (adapter == null){
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setContacts(visibleContacts);
				adapter.setListFragment(recyclerView);
				adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
		
			adapter.setPositionClicked(-1);
			recyclerView.setAdapter(adapter);
						
			if (adapter.getItemCount() == 0){
                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				}

				String textToShow = String.format(context.getString(R.string.context_empty_contacts)).toUpperCase();
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);

			}else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

			if (inviteShown){
				showInviteDialog();
			}
			else if (dialogshown){
				showAlertDialog(dialogTitleContent, dialogTextContent, success);
			}
            showAskForDisplayOverDialog();
			return v;
		}
		else{
			logDebug("isGrid View");
			View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.contacts_grid_view);
			recyclerView.setPadding(0, 0, 0, scaleHeightPx(80, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.setHasFixedSize(true);
			((CustomizedGridRecyclerView) recyclerView).setWrapContent();
			final GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
			gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
				@Override
			      public int getSpanSize(int position) {
					return 1;
				}
			});

			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = (ImageView) v.findViewById(R.id.contact_grid_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.contact_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.contact_grid_empty_text_first);

			setContacts(megaApi.getContacts());
			if (adapter == null){
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setContacts(visibleContacts);
				adapter.setListFragment(recyclerView);
				adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}

			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			recyclerView.setAdapter(adapter);

			if (adapter.getItemCount() == 0){

                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				}
				String textToShow = String.format(getString(R.string.context_empty_contacts)).toUpperCase();
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);

			}else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

			if (inviteShown){
				showInviteDialog();
			}
			else if (dialogshown){
				showAlertDialog(dialogTitleContent, dialogTextContent, success);
			}
            showAskForDisplayOverDialog();
			return v;
		}			
	}

	private void showAskForDisplayOverDialog() {
        if(askForDisplayOverDialog != null) {
            askForDisplayOverDialog.showDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(askForDisplayOverDialog != null) {
            askForDisplayOverDialog.recycle();
        }
	}

    public void setContacts(ArrayList<MegaUser> contacts){
		this.contacts = contacts;

		visibleContacts.clear();

		for (int i=0;i<contacts.size();i++){
			logDebug("Contact: " + contacts.get(i).getHandle() + "_" + contacts.get(i).getVisibility());
			if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

				MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contacts.get(i).getHandle()+""));
				String fullName = "";
				if(contactDB!=null){
					ContactController cC = new ContactController(context);
					fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contacts.get(i).getEmail());
				}
				else{
					//No name, ask for it and later refresh!!
					fullName = contacts.get(i).getEmail();
				}

				MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contacts.get(i), fullName);
				visibleContacts.add(megaContactAdapter);
			}
		}

		sortBy();

		if (!visibleContacts.isEmpty()) {
			for (int i = 0; i < visibleContacts.size(); i++) {
				int userStatus = megaChatApi.getUserOnlineStatus(visibleContacts.get(i).getMegaUser().getHandle());
				if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
					logDebug("Request last green for user");
					megaChatApi.requestLastGreen(visibleContacts.get(i).getMegaUser().getHandle(), null);
				}
			}
		}
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public void itemClick(int position) {
		logDebug("Position: " + position);

		if (adapter.isMultipleSelect()){
			logDebug("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaUser> users = adapter.getSelectedUsers();
			if (users.size() > 0){
				updateActionModeTitle();
			}
		}
		else{
			Intent i = new Intent(context, ContactInfoActivityLollipop.class);
			i.putExtra("name", visibleContacts.get(position).getMegaUser().getEmail());
			startActivity(i);
		}
    }
	
	public int onBackPressed(){
		logDebug("onBackPressed");

		if (adapter.isMultipleSelect()){
			hideMultipleSelect();
			return 2;
		}
		
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else{
			return 0;
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
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
	}

	public void updateView () {
		logDebug("updateView");
		setContacts(megaApi.getContacts());

		if(adapter == null){
			if (((ManagerActivityLollipop)context).isList()) {
				logDebug("isList");
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
		}
		else{
			adapter.setContacts(visibleContacts);
		}
		
		if (visibleContacts.size() == 0){
			logDebug("CONTACTS SIZE == 0");
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);

			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
			}else{
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
			}
			String textToShow = String.format(getString(R.string.context_empty_contacts)).toUpperCase();
			try{
				textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
				textToShow = textToShow.replace("[/A]", "</font>");
				textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
				textToShow = textToShow.replace("[/B]", "</font>");
			}
			catch (Exception e){}
			Spanned result = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
			} else {
				result = Html.fromHtml(textToShow);
			}
			emptyTextViewFirst.setText(result);

		}
		else{
			logDebug("CONTACTS SIZE != 0 ---> "+visibleContacts.size());
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
	}
	
	public void updateShares(){
		logDebug("updateShares");
		adapter.notifyDataSetChanged();
	}

	public void contactPresenceUpdate(long userHandle, int status) {
		logDebug("User Handle: " + userHandle + ", Status: " + status);

		int indexToReplace = -1;
		ListIterator<MegaContactAdapter> itrReplace = visibleContacts.listIterator();
		while (itrReplace.hasNext()) {
			MegaContactAdapter contact = itrReplace.next();
			if (contact != null) {
				if (contact.getMegaUser().getHandle() == userHandle) {
					if(status != MegaChatApi.STATUS_ONLINE && status != MegaChatApi.STATUS_BUSY && status != MegaChatApi.STATUS_INVALID){
						logDebug("Request last green for user");
						megaChatApi.requestLastGreen(userHandle, ((ManagerActivityLollipop)context));
					}
					else{
						contact.setLastGreen("");
					}
					indexToReplace = itrReplace.nextIndex() - 1;
					break;
				}
			} else {
				break;
			}
		}
		if (indexToReplace != -1) {
			logDebug("Index to replace: " + indexToReplace);
			adapter.updateContactStatus(indexToReplace);
		}
	}

	public void contactLastGreenUpdate(long userHandle, int lastGreen) {
		logDebug("User Handle: " + userHandle + ", Last green: " + lastGreen);

		int state = megaChatApi.getUserOnlineStatus(userHandle);

		if(state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID){
			String formattedDate = lastGreenDate(context, lastGreen);

			int indexToReplace = -1;
			ListIterator<MegaContactAdapter> itrReplace = visibleContacts.listIterator();
			while (itrReplace.hasNext()) {
				MegaContactAdapter contact = itrReplace.next();
				if (contact != null) {
					if (contact.getMegaUser().getHandle() == userHandle) {
						contact.setLastGreen(formattedDate);
						//contact.setLastGreen("Veryyy veryy longg tooo check what happens");
						indexToReplace = itrReplace.nextIndex() - 1;
						break;
					}
				} else {
					break;
				}
			}

			if (indexToReplace != -1) {
				logDebug("Index to replace: " + indexToReplace);
				adapter.updateContactStatus(indexToReplace);
			}

			logDebug("Date last green: "+formattedDate);
		}
	}

	public void sortBy(){
		logDebug("sortBy");

		switch (((ManagerActivityLollipop)context).orderContacts) {
			case ORDER_DEFAULT_DESC:
				Collections.sort(visibleContacts,  Collections.reverseOrder((c1, c2) -> {
					String name1 = c1.getFullName();
					String name2 = c2.getFullName();
					int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
					if (res == 0) {
						res = name1.compareTo(name2);
					}
					return res;
				}));
				break;

			case ORDER_CREATION_ASC:
				Collections.sort(visibleContacts, (c1, c2) -> {
					long timestamp1 = c1.getMegaUser().getTimestamp();
					long timestamp2 = c2.getMegaUser().getTimestamp();

					long result = timestamp2 - timestamp1;
					return (int)result;
				});
				break;

			case ORDER_CREATION_DESC:
				Collections.sort(visibleContacts,  Collections.reverseOrder((c1, c2) -> {
					long timestamp1 = c1.getMegaUser().getTimestamp();
					long timestamp2 = c2.getMegaUser().getTimestamp();

					long result = timestamp2 - timestamp1;
					return (int)result;
				}));
				break;

			default:
				Collections.sort(visibleContacts, (c1, c2) -> {
					String name1 = c1.getFullName();
					String name2 = c2.getFullName();
					int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
					if (res == 0) {
						res = name1.compareTo(name2);
					}
					return res;
				});
				break;
		}

		if (isAdded() && adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	public boolean showSelectMenuItem(){
		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}

	public int getItemCount(){
		if(adapter!=null){
			return adapter.getItemCount();
		}
		return 0;
	}

	public ArrayList<MegaContactAdapter> getVisibleContacts() {
		return visibleContacts;
	}

	public void setVisibleContacts(ArrayList<MegaContactAdapter> visibleContacts) {
		this.visibleContacts = visibleContacts;
	}

	public boolean isMultipleselect(){
		return adapter.isMultipleSelect();
	}
}
