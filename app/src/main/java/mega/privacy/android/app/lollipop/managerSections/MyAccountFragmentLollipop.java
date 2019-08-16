package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.interfaces.AbortPendingTransferCallback;
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.adapters.LastContactsAdapter;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;

import static android.graphics.Color.WHITE;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;

public class MyAccountFragmentLollipop extends Fragment implements OnClickListener, AbortPendingTransferCallback {
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels
	final int WIDTH = 500;

	ScrollView scrollView;
	Context context;
	MyAccountInfo myAccountInfo;

	RelativeLayout avatarLayout;
	RoundedImageView myAccountImage;

	TextView nameView;

	TextView typeAccount;
	TextView infoEmail;
	TextView usedSpace;
	TextView lastSession;
	TextView connections;

	ImageView editImageView;

	Button upgradeButton;
	Button logoutButton;
	Button mkButton;
	Button changePassButton;

	LinearLayout typeLayout;
	LinearLayout lastSessionLayout;
    RelativeLayout connectionsLayout;
    private CustomizedGridRecyclerView lastContactsGridView;

	LinearLayout achievementsLayout;
	LinearLayout achievementsSeparator;

	LinearLayout parentLinearLayout;

	ArrayList<MegaUser> lastContacted;
	
	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	private Bitmap qrAvatarSave;

	int numOfClicksLastSession = 0;
	boolean staging = false;

	DatabaseHandler dbH;

	@Override
	public void onCreate (Bundle savedInstanceState){
		log("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);

		super.onCreate(savedInstanceState);
	}

	public void checkScroll () {
		if (scrollView != null) {
			if (scrollView.canScrollVertically(-1)) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			}
			else {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if(megaChatApi==null){
			megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = ((Activity) context).getResources().getDisplayMetrics().density;

		View v = null;
		v = inflater.inflate(R.layout.fragment_my_account, container, false);

		scrollView = (ScrollView) v.findViewById(R.id.my_account_complete_relative_layout);
		new ListenScrollChangesHelper().addViewToListen(scrollView, new ListenScrollChangesHelper.OnScrollChangeListenerCompat() {
			@Override
			public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
				if (scrollView.canScrollVertically(-1)){
					((ManagerActivityLollipop) context).changeActionBarElevation(true);
				}
				else {
					((ManagerActivityLollipop) context).changeActionBarElevation(false);
				}
			}
		});
		
		if(megaApi.getMyUser() == null){
			return null;
		}

		log("My user handle string: "+megaApi.getMyUserHandle());

		avatarLayout = (RelativeLayout) v.findViewById(R.id.my_account_relative_layout_avatar);
		avatarLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.avatar_qr_background));
		avatarLayout.setOnClickListener(this);

		if (savedInstanceState != null) {
			byte[] avatarByteArray = savedInstanceState.getByteArray("qrAvatar");
			if (avatarByteArray != null) {
				log("savedInstanceState avatarByteArray != null");
				qrAvatarSave = BitmapFactory.decodeByteArray(avatarByteArray, 0, avatarByteArray.length);
				if (qrAvatarSave != null) {
					log("savedInstanceState qrAvatarSave != null");
					avatarLayout.setBackground(new BitmapDrawable(qrAvatarSave));
				}
				else {
					megaApi.contactLinkCreate(false, (ManagerActivityLollipop) context);
				}
			}
			else {
				megaApi.contactLinkCreate(false, (ManagerActivityLollipop) context);
			}
		}
		else {
			megaApi.contactLinkCreate(false, (ManagerActivityLollipop) context);
		}

		nameView = (TextView) v.findViewById(R.id.my_account_name);
		nameView.setOnClickListener(this);

		editImageView = (ImageView) v.findViewById(R.id.my_account_edit_icon);
		editImageView.setOnClickListener(this);

		infoEmail = (TextView) v.findViewById(R.id.my_account_email);
		infoEmail.setText(megaApi.getMyEmail());
		infoEmail.setOnClickListener(this);
		
		myAccountImage = (RoundedImageView) v.findViewById(R.id.my_account_thumbnail);

		mkButton = (Button) v.findViewById(R.id.MK_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			mkButton.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
		}

		mkButton.setOnClickListener(this);
		mkButton.setVisibility(View.VISIBLE);

		setMkButtonText();

		changePassButton = (Button) v.findViewById(R.id.change_pass_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			changePassButton.setBackground(ContextCompat.getDrawable(context, R.drawable.white_rounded_corners_button));
		}
		else{
			changePassButton.setBackgroundResource(R.drawable.black_button_border);
//			mkButton.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
		}
		changePassButton.setOnClickListener(this);

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			log("onCreate: Landscape configuration");
			nameView.setMaxWidth(Util.scaleWidthPx(250, outMetrics));
			infoEmail.setMaxWidth(Util.scaleWidthPx(250, outMetrics));
		}
		else{
			nameView.setMaxWidth(Util.scaleWidthPx(180, outMetrics));
			infoEmail.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
		}

		achievementsLayout = (LinearLayout) v.findViewById(R.id.my_account_achievements_layout);
		achievementsSeparator = (LinearLayout)v.findViewById(R.id.my_account_achievements_separator);

		if(megaApi.isAchievementsEnabled()){
			log("Achievements enabled!!");
			achievementsLayout.setVisibility(View.VISIBLE);
			achievementsSeparator.setVisibility(View.VISIBLE);
			achievementsLayout.setOnClickListener(this);
		}
		else{
			log("NO Achievements enabled!!");
			achievementsLayout.setVisibility(View.GONE);
			achievementsSeparator.setVisibility(View.GONE);
		}

		typeLayout = (LinearLayout) v.findViewById(R.id.my_account_account_type_layout);

		typeAccount = (TextView) v.findViewById(R.id.my_account_account_type_text);

		usedSpace = (TextView) v.findViewById(R.id.my_account_used_space);

		upgradeButton = (Button) v.findViewById(R.id.my_account_account_type_button);

		upgradeButton.setText(getString(R.string.my_account_upgrade_pro));
		upgradeButton.setOnClickListener(this);
		upgradeButton.setVisibility(View.VISIBLE);

		lastSessionLayout = (LinearLayout) v.findViewById(R.id.my_account_last_session_layout);
		lastSessionLayout.setOnClickListener(this);
		lastSession = (TextView) v.findViewById(R.id.my_account_last_session);

		connectionsLayout = (RelativeLayout) v.findViewById(R.id.my_account_connections_layout);
		connections = (TextView) v.findViewById(R.id.my_account_connections);

		logoutButton = (Button) v.findViewById(R.id.logout_button);

		logoutButton.setOnClickListener(this);
		logoutButton.setVisibility(View.VISIBLE);

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.parent_linear_layout);

		if(myAccountInfo==null){
			myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
		}

		if((myAccountInfo.getFullName()!=null) && (!myAccountInfo.getFullName().isEmpty())){
			log("MyName is:"+ myAccountInfo.getFullName());
			nameView.setText(myAccountInfo.getFullName());
		}

		this.updateAvatar(true);

		updateContactsCount();

		lastContacted = MegaApiUtils.getLastContactedUsers(context);
		//Draw contact's connection component if lastContacted.size > 0
        lastContactsGridView = (CustomizedGridRecyclerView)v.findViewById(R.id.last_contacts_gridview);
        
        lastContactsGridView.setColumnCount(LastContactsAdapter.MAX_COLUMN);
        lastContactsGridView.setClipToPadding(false);
        lastContactsGridView.setHasFixedSize(false);
        
        lastContactsGridView.setAdapter(new LastContactsAdapter(getActivity(),lastContacted));

		setAccountDetails();

		return v;
	}
    
    @Override
    public void onResume() {
        super.onResume();
        //Refresh
		megaApi.contactLinkCreate(false, (ManagerActivityLollipop) context);
		refreshAccountInfo();
        updateView();
    }
    
    /**
     * Update last contacts list and refresh last contacts' avatar.
     */
    public void updateView() {

        if(lastContactsGridView!=null){
			lastContacted = MegaApiUtils.getLastContactedUsers(context);
			lastContactsGridView.setAdapter(new LastContactsAdapter(getActivity(),lastContacted));
		}
    }

    public void updateContactsCount(){
    	log("updateContactsCounts");
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		ArrayList<MegaUser> visibleContacts=new ArrayList<MegaUser>();

		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}
		connections.setText(visibleContacts.size()+" " + context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));
	}

	public void setMkButtonText(){
		log("setMkButtonText");
		File file= buildExternalStorageFile(RK_FILE);
		String mkButtonText;
		if(isFileAvailable(file)){
			mkButtonText = getString(R.string.action_remove_master_key);
		}
		else{
			mkButtonText= getString(R.string.action_export_master_key);
		}

		if(mkButtonText.length()>27){
			boolean found = false;
			int mid = mkButtonText.length()/2;

			for(int i=mid;i<mkButtonText.length()-1;i++){
				char letter = mkButtonText.charAt(i);
				if(letter == ' '){
					StringBuilder sb = new StringBuilder(mkButtonText);
					sb.setCharAt(i, '\n');
					mkButtonText = sb.toString();
					found = true;
					break;
				}
			}

			if(!found){
				for(int i=0;i<mid;i++){
					char letter = mkButtonText.charAt(i);
					if(letter == ' '){
						StringBuilder sb = new StringBuilder(mkButtonText);
						sb.setCharAt(i, '\n');
						mkButtonText = sb.toString();
						break;
					}
				}
			}
		}

		mkButton.setText(mkButtonText);
	}

	public static MyAccountFragmentLollipop newInstance() {
		log("newInstance");
		MyAccountFragmentLollipop fragment = new MyAccountFragmentLollipop();
		return fragment;
	}

	public void refreshAccountInfo(){
		log("refreshAccountInfo");

		//Check if the call is recently
		log("Check the last call to getAccountDetails");
		if(DBUtil.callToAccountDetails(context)){
			log("megaApi.getAccountDetails SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForAccountDetails();
		}
		log("Check the last call to getExtendedAccountDetails");
		if(DBUtil.callToExtendedAccountDetails(context)){
			log("megaApi.getExtendedAccountDetails SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForExtendedAccountDetails();
		}
		log("Check the last call to getPaymentMethods");
		if(DBUtil.callToPaymentMethods(context)){
			log("megaApi.getPaymentMethods SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPaymentMethods();
		}
	}

	public void setAccountDetails(){
		log("setAccountDetails");

		if((getActivity() == null) || (!isAdded())){
			log("Fragment MyAccount NOT Attached!");
			return;
		}
		//Set account details
		if(myAccountInfo.getAccountType()<0||myAccountInfo.getAccountType()>4){
			typeAccount.setText(getString(R.string.recovering_info));
		}
		else{
			switch(myAccountInfo.getAccountType()){

				case 0:{
					typeAccount.setText(R.string.my_account_free);
					break;
				}

				case 1:{
					typeAccount.setText(getString(R.string.my_account_pro1));
					break;
				}

				case 2:{
					typeAccount.setText(getString(R.string.my_account_pro2));
					break;
				}

				case 3:{
					typeAccount.setText(getString(R.string.my_account_pro3));
					break;
				}

				case 4:{
					typeAccount.setText(getString(R.string.my_account_prolite));
					break;
				}

			}
		}

//		if (getPaymentMethodsBoolean == true){
//			if (upgradeButton != null){
//				if ((myAccountInfo.getAccountInfo().getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_NONE) || (myAccountInfo.getAccountInfo().getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID)){
//					Time now = new Time();
//					now.setToNow();
//					if (myAccountInfo.getAccountType() != 0){
//						if (now.toMillis(false) >= (myAccountInfo.getAccountInfo().getProExpiration()*1000)){
//							if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
//								upgradeButton.setVisibility(View.VISIBLE);
//							}
//						}
//					}
//					else{
//						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
//							upgradeButton.setVisibility(View.VISIBLE);
//						}
//					}
//				}
//			}
//		}
		if(myAccountInfo.getUsedFormatted().trim().length()<=0){
			usedSpace.setText(getString(R.string.recovering_info));
		}
		else{
			String usedSpaceString = myAccountInfo.getUsedFormatted() + " " + getString(R.string.general_x_of_x) + " " + myAccountInfo.getTotalFormatted();
			usedSpace.setText(usedSpaceString);
		}

		if(myAccountInfo.getLastSessionFormattedDate()!=null) {
			if (myAccountInfo.getLastSessionFormattedDate().trim().length() <= 0) {
				lastSession.setText(getString(R.string.recovering_info));
			} else {
				lastSession.setText(myAccountInfo.getLastSessionFormattedDate());
			}
		}
		else{
			lastSession.setText(getString(R.string.recovering_info));
		}
		///////////
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

	@Override
	public void onAttach(Activity activity) {
		log("onAttach");
		super.onAttach(activity);
		context = activity;
	}

	@Override
	public void onAttach(Context context) {
		log("onAttach context");
		super.onAttach(context);
		this.context = context;
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		switch (v.getId()) {

			case R.id.logout_button:{
				log("Logout button");
				Util.checkPendingTransfer(megaApi, getContext(), this);
				break;
			}
			case R.id.my_account_relative_layout_avatar:{
				log("Click layout avatar");
				((ManagerActivityLollipop)context).showMyAccountOptionsPanel();
				break;
			}
			case R.id.my_account_name:
			case R.id.my_account_email:
			case R.id.my_account_edit_icon:{
				log("Click user attributes text");
				((ManagerActivityLollipop)context).showDialogChangeUserAttribute();
				break;
			}
			case R.id.MK_button:{
				log("Master Key button");
				log("Exists MK in: "+getExternalStoragePath(RK_FILE));
				File file= buildExternalStorageFile(RK_FILE);
				if(isFileAvailable(file)){
					((ManagerActivityLollipop)context).showConfirmationRemoveMK();
				}
				else{
					((ManagerActivityLollipop)context).showMKLayout();
				}

				break;
			}

			case R.id.change_pass_button:{
				log("Change pass button");

				Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
				startActivity(intent);
				break;
			}
			case R.id.my_account_account_type_button:{
				log("Upgrade Account button");
				((ManagerActivityLollipop) context).setAccountFragmentPreUpgradeAccount(Constants.MY_ACCOUNT_FRAGMENT);
				((ManagerActivityLollipop)context).showUpAF();
				break;
			}
			case R.id.my_account_achievements_layout:{
				log("Show achievements");

				if(!Util.isOnline(context)){
					((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				}
				else{
					Intent intent = new Intent(context, AchievementsActivity.class);
//				intent.putExtra("orderGetChildren", orderGetChildren);
					startActivity(intent);
				}
				break;
			}
			case R.id.my_account_last_session_layout:{
				numOfClicksLastSession++;
				if (numOfClicksLastSession == 5){
					numOfClicksLastSession = 0;
					staging = false;
					if (dbH != null){
						MegaAttributes attrs = dbH.getAttributes();
						if (attrs != null) {
							if (attrs.getStaging() != null){
								try{
									staging = Boolean.parseBoolean(attrs.getStaging());
								} catch (Exception e){ staging = false;}
							}
						}
					}

					if (!staging) {
						DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch (which){
									case DialogInterface.BUTTON_POSITIVE:
										staging = true;
										megaApi.changeApiUrl("https://staging.api.mega.co.nz/");
										if (dbH != null){
											dbH.setStaging(true);
										}

										Intent intent = new Intent(context, LoginActivityLollipop.class);
										intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
										intent.setAction(Constants.ACTION_REFRESH_STAGING);

										startActivityForResult(intent, Constants.REQUEST_CODE_REFRESH_STAGING);
										break;

									case DialogInterface.BUTTON_NEGATIVE:
										//No button clicked
										break;
								}
							}
						};

						AlertDialog.Builder builder = new AlertDialog.Builder(context);
						builder.setTitle(getResources().getString(R.string.staging_api_url_title));
						builder.setMessage(getResources().getString(R.string.staging_api_url_text));

						builder.setPositiveButton(R.string.general_yes, dialogClickListener);
						builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
						builder.show();
					}
					else{
						staging = false;
						megaApi.changeApiUrl("https://g.api.mega.co.nz/");
						if (dbH != null){
							dbH.setStaging(false);
						}
						Intent intent = new Intent(context, LoginActivityLollipop.class);
						intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						intent.setAction(Constants.ACTION_REFRESH_STAGING);

						startActivityForResult(intent, Constants.REQUEST_CODE_REFRESH_STAGING);
					}
				}
				break;
			}
		}
	}

	public int onBackPressed(){
		log("onBackPressed");
		return 0;
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
			info = numFolders +  " " + context.getResources().getQuantityString(R.plurals.general_num_shared_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + context.getResources().getQuantityString(R.plurals.general_num_shared_folders, numFiles);
			}
		}
		else {
			if (numFiles == 0){
				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_shared_folders, numFolders);
			}
			else{
				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_shared_folders, numFiles);
			}
		}

		return info;
	}

	public void updateNameView(String fullName){
		log("updateNameView");

		if (nameView != null) {
			nameView.setText(fullName);
		}

		updateAvatar(false);
	}

	public void updateMailView(String newMail){
		log("updateMailView: "+newMail);

		if (newMail != null){
			infoEmail.setText(newMail);
		}

		File avatar = buildAvatarFile(context,newMail + ".jpg");

		if (!isFileAvailable(avatar)){
			setDefaultAvatar();
		}
	}

	public static void log(String log) {
		Util.log("MyAccountFragmentLollipop", log);
	}

	public void resetPass(){
		AccountController aC = new AccountController(context);
		aC.resetPass(megaApi.getMyEmail());
	}

	public void updateAvatar(boolean retry){
		log("updateAvatar");
		File avatar = null;
		String contactEmail = megaApi.getMyEmail();
		if(context!=null){
			log("context is not null");
			avatar = buildAvatarFile(context,contactEmail + ".jpg");
		}
		else{
			log("context is null!!!");
			if(getActivity()!=null){
				log("getActivity is not null");
                avatar = buildAvatarFile(getActivity(),contactEmail + ".jpg");
			}
			else{
				log("getActivity is ALSOOO null");
				return;
			}
		}

		if(avatar!=null){
			setProfileAvatar(avatar, retry);
		}
		else{
			setDefaultAvatar();
		}
	}

	public void setDefaultAvatar(){
		log("setDefaultAvatar");

		String color = megaApi.getUserAvatarColor(megaApi.getMyUser());

		myAccountImage.setImageBitmap(Util.createDefaultAvatar(color, myAccountInfo.getFirstLetter()));

	}

	public void setProfileAvatar(File avatar, boolean retry){
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
					if(retry){
						log("Retry!");
                        megaApi.getUserAvatar(megaApi.getMyUser(),buildAvatarFile(context,megaApi.getMyEmail()).getAbsolutePath(),(ManagerActivityLollipop)context);
                    }
					else{
						log("DO NOT Retry!");
						setDefaultAvatar();
					}
				}
				else{
					log("Show my avatar");
					myAccountImage.setImageBitmap(imBitmap);
				}
			}
		}else{
			log("my avatar NOT exists!");
			log("Call to getUserAvatar");
			if(retry){
				log("Retry!");
                megaApi.getUserAvatar(megaApi.getMyUser(),buildAvatarFile(context,megaApi.getMyEmail()).getAbsolutePath(),(ManagerActivityLollipop)context);
			}
			else{
				log("DO NOT Retry!");
				setDefaultAvatar();
			}
		}
	}

	public Bitmap queryQR (String contactLink) {
		log("queryQR");

		Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<>();
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

		BitMatrix bitMatrix = null;

		try {
			bitMatrix = new MultiFormatWriter().encode(contactLink, BarcodeFormat.QR_CODE, 40, 40, hints);
		} catch (WriterException e) {
			e.printStackTrace();
			return null;
		}
		int w = bitMatrix.getWidth();
		int h = bitMatrix.getHeight();
		int[] pixels = new int[w * h];
		int color = ContextCompat.getColor(context, R.color.grey_achievements_invite_friends_sub);
		float resize = 12.2f;

		Bitmap bitmap = Bitmap.createBitmap(WIDTH, WIDTH, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(WHITE);
		c.drawRect(0, 0, WIDTH, WIDTH, paint);
		paint.setColor(color);

		for (int y = 0; y < h; y++) {
			int offset = y * w;
			for (int x = 0; x < w; x++) {
				pixels[offset + x] = bitMatrix.get(x, y) ? color : WHITE;
				if (pixels[offset + x] == color){
					c.drawCircle(x*resize, y*resize, 5, paint);
				}
//				log("pixels[offset + x]: "+Integer.toString(pixels[offset + x])+ " offset+x: "+(offset+x));
			}
		}
		paint.setColor(WHITE);
		c.drawRect(3*resize, 3*resize, 11.5f*resize, 11.5f*resize, paint);
		c.drawRect(28.5f*resize, 3*resize, 37*resize, 11.5f*resize, paint);
		c.drawRect(3*resize, 28.5f*resize, 11.5f*resize, 37*resize, paint);

		paint.setColor(color);

		if (Build.VERSION.SDK_INT >= 21) {
			c.drawRoundRect(3.75f * resize, 3.75f * resize, 10.75f * resize, 10.75f * resize, 30, 30, paint);
			c.drawRoundRect(29.25f * resize, 3.75f * resize, 36.25f * resize, 10.75f * resize, 30, 30, paint);
			c.drawRoundRect(3.75f * resize, 29.25f * resize, 10.75f * resize, 36.25f * resize, 30, 30, paint);

			paint.setColor(WHITE);
			c.drawRoundRect(4.75f * resize, 4.75f * resize, 9.75f * resize, 9.75f * resize, 25, 25, paint);
			c.drawRoundRect(30.25f * resize, 4.75f * resize, 35.25f * resize, 9.75f * resize, 25, 25, paint);
			c.drawRoundRect(4.75f * resize, 30.25f * resize, 9.75f * resize, 35.25f * resize, 25, 25, paint);
		}
		else {
			c.drawRoundRect(new RectF(3.75f * resize, 3.75f * resize, 10.75f * resize, 10.75f * resize), 30, 30, paint);
			c.drawRoundRect(new RectF(29.25f * resize, 3.75f * resize, 36.25f * resize, 10.75f * resize), 30, 30, paint);
			c.drawRoundRect(new RectF(3.75f * resize, 29.25f * resize, 10.75f * resize, 36.25f * resize), 30, 30, paint);

			paint.setColor(WHITE);
			c.drawRoundRect(new RectF(4.75f * resize, 4.75f * resize, 9.75f * resize, 9.75f * resize), 25, 25, paint);
			c.drawRoundRect(new RectF(30.25f * resize, 4.75f * resize, 35.25f * resize, 9.75f * resize), 25, 25, paint);
			c.drawRoundRect(new RectF(4.75f * resize, 30.25f * resize, 9.75f * resize, 35.25f * resize), 25, 25, paint);
		}

		paint.setColor(color);
		c.drawCircle(7.25f*resize, 7.25f*resize, 17.5f, paint);
		c.drawCircle(32.75f*resize, 7.25f*resize, 17.5f, paint);
		c.drawCircle(7.25f*resize, 32.75f*resize, 17.5f, paint);

//        bitmap.setPixels(pixels, 0, w, 0, 0, w,  h);

		return bitmap;
	}

	public void initCreateQR(MegaRequest request, MegaError e){
		log("initCreateQR");
		if (e.getErrorCode() == MegaError.API_OK) {
			log("Contact link create LONG: " + request.getNodeHandle());
			log("Contact link create BASE64: " + "https://mega.nz/C!" + MegaApiAndroid.handleToBase64(request.getNodeHandle()));

			String contactLink = "https://mega.nz/C!" + MegaApiAndroid.handleToBase64(request.getNodeHandle());
			new QRBackgroundTask().execute(contactLink);
		}
		else {
			log("Error request.getType() == MegaRequest.TYPE_CONTACT_LINK_CREATE: " + e.getErrorString());
		}
	}

	class QRBackgroundTask extends AsyncTask<String, Void, Void> {


		@Override
		protected Void doInBackground(String... strings) {
			qrAvatarSave = queryQR(strings[0]);

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			avatarLayout.setBackground(new BitmapDrawable(qrAvatarSave));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (qrAvatarSave != null){
			log("onSaveInstanceState qrAvatarSave != null");
			ByteArrayOutputStream qrAvatarOutputStream = new ByteArrayOutputStream();
			qrAvatarSave.compress(Bitmap.CompressFormat.PNG, 100, qrAvatarOutputStream);
			byte[] qrAvatarByteArray = qrAvatarOutputStream.toByteArray();
			outState.putByteArray("qrAvatar", qrAvatarByteArray);
		}
	}

	@Override
	public void onAbortConfirm() {
		log("onAbortConfirm");
		megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD);
		megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD);
		((ManagerActivityLollipop)getContext()).setPasswordReminderFromMyAccount(true);
		megaApi.shouldShowPasswordReminderDialog(true, (ManagerActivityLollipop)context);
	}

	@Override
	public void onAbortCancel() {
		log("onAbortCancel");
	}
}
