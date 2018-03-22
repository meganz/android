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
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.adapters.MegaContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static android.graphics.Color.WHITE;

public class ContactsFragmentLollipop extends Fragment implements MegaRequestListenerInterface, View.OnClickListener{

	public static int GRID_WIDTH =400;
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150;
	
	public static final String ARG_OBJECT = "object";

	String myEmail;
	MegaUser myUser;
	private RoundedImageView avatarImage;
	private TextView contactName;
	private TextView contactMail;
	private Button invite;
	private TextView initialLetterInvite;
	private TextView dialogTitle;
	private TextView dialogText;
	private Button dialogButton;
	private AlertDialog inviteAlertDialog;
	private AlertDialog requestedAlertDialog;
	private long handleContactLink = -1;
	
	MegaApiAndroid megaApi;
	MyAccountInfo myAccountInfo;
	TextView initialLetter;

	Context context;
	RecyclerView recyclerView;
	MegaContactsLollipopAdapter adapter;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

	TextView contentText;
	RelativeLayout contentTextLayout;
	private ActionMode actionMode;
	DatabaseHandler dbH = null;

//	DatabaseHandler dbH = null;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	boolean isList = true;

	ContactsFragmentLollipop contactsFragment = this;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaContactAdapter> visibleContacts = new ArrayList<MegaContactAdapter>();
	
	int orderContacts;

	MegaUser selectedUser = null;

	public void activateActionMode(){
		log("activateActionMode");
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
				log("Contact link query " + request.getNodeHandle() + "_" + MegaApiAndroid.handleToBase64(request.getNodeHandle()) + "_" + request.getEmail() + "_" + request.getName() + "_" + request.getText());

				myEmail = request.getEmail();
				if (megaApi == null){
					megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
				}
				handleContactLink = request.getNodeHandle();
				contactName.setText(request.getName() + " " + request.getText());
				contactMail.setText(request.getEmail());
				setAvatar();

				inviteAlertDialog.show();
			}
			else {
				showAlertDialog( R.string.invite_not_sent, R.string.invite_not_sent_link_text, false);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {
			if (e.getErrorCode() == MegaError.API_OK) {
				log("Get user avatar OK");
				setAvatar();
			}
			else {
				log("Get user avatal FAIL");
				setDefaultAvatar();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

	}

	public void setAvatar(){
		log("updateAvatar");
		File avatar = null;
		if(context!=null){
			log("context is not null");

			if (context.getExternalCacheDir() != null){
				avatar = new File(context.getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
			}
			else{
				avatar = new File(context.getCacheDir().getAbsolutePath(), myEmail + ".jpg");
			}
		}
		else{
			log("context is null!!!");
			if(getActivity()!=null){
				log("getActivity is not null");
				if (getActivity().getExternalCacheDir() != null){
					avatar = new File(getActivity().getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
				}
				else{
					avatar = new File(getActivity().getCacheDir().getAbsolutePath(), myEmail + ".jpg");
				}
			}
			else{
				log("getActivity is ALSOOO null");
				return;
			}
		}

		if(avatar!=null){
			setProfileAvatar(avatar);
		}
		else{
			setDefaultAvatar();
		}
	}

	public void setProfileAvatar(File avatar){
		log("setProfileAvatar");

		Bitmap imBitmap = null;
		if (avatar.exists()){
			log("avatar path: "+avatar.getAbsolutePath());
			if (avatar.length() > 0){
				log("my avatar exists!");
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (imBitmap == null) {
					avatar.delete();
					log("Call to getUserAvatar");
					setDefaultAvatar();
				}
				else{
					log("Show my avatar");
					avatarImage.setImageBitmap(imBitmap);
					initialLetterInvite.setVisibility(View.GONE);
				}
			}
		}else{
			log("my avatar NOT exists!");
			log("Call to getUserAvatar");
			log("DO NOT Retry!");
			megaApi.getUserAvatar(myEmail, avatar.getPath(), this);
//			setDefaultAvatar();
		}
	}

	public void setDefaultAvatar(){
		log("setDefaultAvatar");
		Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);

		String color = megaApi.getUserAvatarColor(myUser);
		if(color!=null){
			log("The color to set the avatar is "+color);
			p.setColor(Color.parseColor(color));
		}
		else{
			log("Default color to the avatar");
			p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
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
		log("DENSITY: " + density + ":::: " + avatarTextSize);
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
		log("invite");
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		LayoutInflater inflater = getActivity().getLayoutInflater();
		log("Handle: "+handle);
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		megaApi.contactLinkQuery(handle, this);

		View v = inflater.inflate(R.layout.dialog_accept_contact, null);
		builder.setView(v);

		invite = (Button) v.findViewById(R.id.accept_contact_invite);
		invite.setOnClickListener(this);

		avatarImage = (RoundedImageView) v.findViewById(R.id.accept_contact_avatar);
		initialLetterInvite = (TextView) v.findViewById(R.id.accept_contact_initial_letter);
		contactName = (TextView) v.findViewById(R.id.accept_contact_name);
		contactMail = (TextView) v.findViewById(R.id.accept_contact_mail);

		inviteAlertDialog = builder.create();
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
//            dialogTitle.setText(getResources().getString(R.string.invite_accepted));
//            dialogText.setText(getResources().getString(R.string.invite_accepted_text, myEmail));

		if (success){
			dialogTitle.setText(getResources().getString(title));
			dialogText.setText(getResources().getString(text, myEmail));
		}
		else {
			dialogTitle.setText(getResources().getString(title));
			dialogText.setText(getResources().getString(text));
		}

		requestedAlertDialog = builder.create();
		requestedAlertDialog.show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.accept_contact_invite: {
				megaApi.inviteContact(myEmail, null, MegaContactRequest.INVITE_ACTION_ADD, handleContactLink, (ManagerActivityLollipop) context);
				if (inviteAlertDialog != null){
					inviteAlertDialog.dismiss();
				}
				break;
			}
			case R.id.dialog_invite_button: {
				if (requestedAlertDialog != null){
					requestedAlertDialog.dismiss();
				}
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

			((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

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
					}										
					break;
				}
				case R.id.cab_menu_start_conversation:{
					ArrayList<Long> contactHandles = new ArrayList<>();

					if(users.get(0)==null){
						log("Selected contact NULL");
						break;
					}
					if(users.size()  == 1){
						((ManagerActivityLollipop) context).startOneToOneChat(users.get(0));
					}else{
						for(int i=0;i<users.size();i++){
							contactHandles.add(users.get(i).getHandle());
						}

						((ManagerActivityLollipop)context).startGroupConversation(contactHandles);
					}

					clearSelections();
					hideMultipleSelect();

					break;
				}
				case R.id.cab_menu_delete:{
					((ManagerActivityLollipop)context).showConfirmationRemoveContacts(users);
					break;
				}
				case R.id.cab_menu_select_all:{
					((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);
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
			}
			return false;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.contact_fragment_action, menu);
			((ManagerActivityLollipop)context).hideFabButton();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaUser> selected = adapter.getSelectedUsers();
			MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
			if (selected.size() != 0) {
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_share_folder).setVisible(true);
				menu.findItem(R.id.cab_menu_share_folder).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_send_file).setVisible(true);
				menu.findItem(R.id.cab_menu_send_file).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

				menu.findItem(R.id.cab_menu_start_conversation).setVisible(true);
				menu.findItem(R.id.cab_menu_start_conversation).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

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
		log("hideMultipleSelect");
		if(adapter!=null){
			adapter.setMultipleSelect(false);
		}

		((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_TRANSPARENT_BLACK);
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
			log("oninvalidate error");
		}
	}
		
	//End Multiselect/////

	public static ContactsFragmentLollipop newInstance() {
		log("newInstance");
		ContactsFragmentLollipop fragment = new ContactsFragmentLollipop();
		return fragment;
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
		contacts = megaApi.getContacts();
		visibleContacts.clear();

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		
//		for (int i=0;i<contacts.size();i++){
//
//			MegaContact contactDB = dbH.findContactByHandle(String.valueOf(contacts.get(i).getHandle()));
//			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility()+"__"+contactDB.getName()+" "+contactDB.getLastName());
//			if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){
//				visibleContacts.add(contacts.get(i));
//			}
//		}

		if(myAccountInfo == null){
			myAccountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();

		}

		for (int i=0;i<contacts.size();i++){

//			MegaContact contactDB = dbH.findContactByHandle(String.valueOf(contacts.get(i).getHandle()));
//			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility()+"__"+contactDB.getName()+" "+contactDB.getLastName());
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility()+ "_" + contacts.get(i).getTimestamp());
			if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

				MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contacts.get(i).getHandle()+""));
				String fullName = "";
				if(contactDB!=null){
					ContactController cC = new ContactController(context);
					fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contacts.get(i).getEmail());
				}
				else{
					//No name, ask for it and later refresh!!
					log("CONTACT DB is null");
					fullName = contacts.get(i).getEmail();
				}

				MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contacts.get(i), fullName);
				visibleContacts.add(megaContactAdapter);
			}
		}
		orderContacts = ((ManagerActivityLollipop)context).getOrderContacts();
		sortBy(orderContacts);
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
	    
	    isList = ((ManagerActivityLollipop)context).isList();
		
		if (isList){
			log("isList");
			View v = inflater.inflate(R.layout.fragment_contactslist, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.contacts_list_view);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			recyclerView.setHasFixedSize(true);
			LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
		    recyclerView.setLayoutManager(linearLayoutManager);
		    recyclerView.setItemAnimator(new DefaultItemAnimator());

			emptyImageView = (ImageView) v.findViewById(R.id.contact_list_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.contact_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.contact_list_empty_text_first);
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.contact_list_content_text_layout);
			contentText = (TextView) v.findViewById(R.id.contact_list_content_text);

			if (adapter == null){
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setContacts(visibleContacts);
				adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}

			if (visibleContacts.size() > 0) {
				contentTextLayout.setVisibility(View.VISIBLE);

				contentText.setText(visibleContacts.size()+ " " +context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));
			}else if(visibleContacts.size() == 0){
				contentTextLayout.setVisibility(View.GONE);
			}

		
			adapter.setPositionClicked(-1);
			recyclerView.setAdapter(adapter);
						
			if (adapter.getItemCount() == 0){
                recyclerView.setVisibility(View.GONE);
                contentTextLayout.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				}

				String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
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
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

			return v;
		}
		else{
			log("isGrid View");
			View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.contacts_grid_view);
//			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
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

			emptyImageView = (ImageView) v.findViewById(R.id.contact_grid_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.contact_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.contact_grid_empty_text_first);
			contentTextLayout = (RelativeLayout) v.findViewById(R.id.contact_content_grid_text_layout);

			contentText = (TextView) v.findViewById(R.id.contact_content_text_grid);

			if (adapter == null){
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setContacts(visibleContacts);
				adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}

			if (visibleContacts.size() > 0) {
				contentTextLayout.setVisibility(View.VISIBLE);

				contentText.setText(visibleContacts.size()+ " " +context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));
			}else if(visibleContacts.size() == 0){
				contentTextLayout.setVisibility(View.GONE);
			}

			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			recyclerView.setAdapter(adapter);
						
			if (adapter.getItemCount() == 0){

                recyclerView.setVisibility(View.GONE);
                contentTextLayout.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				}
				String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
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
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}

			((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();
			return v;
		}			
	}

	public void setContacts(ArrayList<MegaUser> contacts){
		this.contacts = contacts;

		visibleContacts.clear();

		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
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

		orderContacts = ((ManagerActivityLollipop)context).getOrderContacts();
		sortBy(orderContacts);
		
		adapter.setContacts(visibleContacts);

		if (visibleContacts.size() > 0) {
			contentTextLayout.setVisibility(View.VISIBLE);

			contentText.setText(visibleContacts.size()+ " " +context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));
		}else if(visibleContacts.size() == 0){
			contentTextLayout.setVisibility(View.GONE);
		}
	}

	public void updateOrder(){
		if(isAdded()){
			adapter.notifyDataSetChanged();
		}
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public void itemClick(int position) {
		log("itemClick");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (adapter.isMultipleSelect()){
			log("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaUser> users = adapter.getSelectedUsers();
			if (users.size() > 0){
				updateActionModeTitle();
				((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);
			}
		}
		else{
			Intent i = new Intent(context, ContactInfoActivityLollipop.class);
			i.putExtra("name", visibleContacts.get(position).getMegaUser().getEmail());
			startActivity(i);
		}
    }
	
	public int onBackPressed(){
		log("onBackPressed");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

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
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
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
	
	private static void log(String log) {
		Util.log("ContactsFragmentLollipop", log);
	}

	public void updateView () {
		log("updateView");
		ArrayList<MegaUser> contacts = megaApi.getContacts();

		if(adapter == null){
			isList = ((ManagerActivityLollipop)context).isList();

			if (isList) {
				log("isList");
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
		}
		else{
			this.setContacts(contacts);
		}
		
		if (visibleContacts.size() == 0){
			log("CONTACTS SIZE == 0");
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);

			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
			}else{
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
			}
			String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
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
			log("CONTACTS SIZE != 0 ---> "+visibleContacts.size());
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
	}
	
	public void updateShares(){
		log("updateShares");
		adapter.notifyDataSetChanged();
	}

	public void contactStatusUpdate(long userHandle, int status) {
		log("contactStatusUpdate: "+userHandle);

		int indexToReplace = -1;
		ListIterator<MegaContactAdapter> itrReplace = visibleContacts.listIterator();
		while (itrReplace.hasNext()) {
			MegaContactAdapter contact = itrReplace.next();
			if (contact != null) {
				if (contact.getMegaUser().getHandle() == userHandle) {
					indexToReplace = itrReplace.nextIndex() - 1;
					break;
				}
			} else {
				break;
			}
		}
		if (indexToReplace != -1) {
			log("Index to replace: " + indexToReplace);
			adapter.updateContactStatus(indexToReplace, userHandle, status);
		}
	}

	public void sortBy(int orderContacts){
		log("sortBy");

		if(orderContacts == MegaApiJava.ORDER_DEFAULT_DESC){
			Collections.sort(visibleContacts,  Collections.reverseOrder(new Comparator<MegaContactAdapter>(){

				public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
					String name1 = c1.getFullName();
					String name2 = c2.getFullName();
					int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
					if (res == 0) {
						res = name1.compareTo(name2);
					}
					return res;
				}
			}));
		}
		else if(orderContacts == MegaApiJava.ORDER_CREATION_ASC){
			Collections.sort(visibleContacts,  new Comparator<MegaContactAdapter>(){

				public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
					long timestamp1 = c1.getMegaUser().getTimestamp();
					long timestamp2 = c2.getMegaUser().getTimestamp();

					long result = timestamp2 - timestamp1;
					return (int)result;
				}
			});
		}
		else if(orderContacts == MegaApiJava.ORDER_CREATION_DESC){
			Collections.sort(visibleContacts,  Collections.reverseOrder(new Comparator<MegaContactAdapter>(){

				public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
					long timestamp1 = c1.getMegaUser().getTimestamp();
					long timestamp2 = c2.getMegaUser().getTimestamp();

					long result = timestamp2 - timestamp1;
					return (int)result;
				}
			}));
		}
		else{
			Collections.sort(visibleContacts, new Comparator<MegaContactAdapter>(){

				public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
					String name1 = c1.getFullName();
					String name2 = c2.getFullName();
					int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
					if (res == 0) {
						res = name1.compareTo(name2);
					}
					return res;
				}
			});
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
	
	public void setOrder(int orderContacts){
		log("setOrder:Contacts");
		this.orderContacts = orderContacts;
	}

	public ArrayList<MegaContactAdapter> getVisibleContacts() {
		return visibleContacts;
	}

	public void setVisibleContacts(ArrayList<MegaContactAdapter> visibleContacts) {
		this.visibleContacts = visibleContacts;
	}
}
