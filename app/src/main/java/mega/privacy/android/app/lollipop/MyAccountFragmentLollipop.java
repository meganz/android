package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;


public class MyAccountFragmentLollipop extends Fragment implements OnClickListener{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	Context context;
	ActionBar aB;
	MyAccountInfo myAccountInfo;

	RelativeLayout avatarLayout;
	TextView initialLetter;
	RoundedImageView myAccountImage;
	ImageView mailIcon;
	
	TextView nameView;
	boolean mKLayoutVisible;

	String myEmail;
	MegaUser myUser;

	TextView typeAccount;
	TextView infoEmail;
	TextView usedSpace;
	TextView lastSession;
	TextView connections;
	TextView fingerprint;

	Button upgradeButton;
	Button logoutButton;
	Button mkButton;
	Button deleteAccountButton;
	
	RelativeLayout typeLayout;
	LinearLayout lastSessionLayout;
	LinearLayout connectionsLayout;
	LinearLayout fingerprintLayout;

	RelativeLayout exportMKLayout;
	LinearLayout exportMKButtonsLayout;
	TextView titleExportMK;
	TextView subTitleExportMK;
	TextView firstParExportMK;
	TextView secondParExportMK;
	TextView thirdParExportMK;
	TextView actionExportMK;
	Button copyMK;
	Button saveMK;

	LinearLayout parentLinearLayout;
	
	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;

	@Override
	public void onCreate (Bundle savedInstanceState){
		log("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		
		float scaleText;
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}

		View v = null;
		v = inflater.inflate(R.layout.fragment_my_account, container, false);
		
		myUser = megaApi.getMyUser();
		if(myUser == null){
			return null;
		}

		avatarLayout = (RelativeLayout) v.findViewById(R.id.my_account_relative_layout_avatar);
		avatarLayout.setOnClickListener(this);
		mailIcon = (ImageView)  v.findViewById(R.id.my_account_email_icon);
		LinearLayout.LayoutParams mailIconParams = (LinearLayout.LayoutParams)mailIcon.getLayoutParams();
		mailIconParams.setMargins(Util.scaleWidthPx(16, outMetrics), Util.scaleHeightPx(28, outMetrics), 0, 0);
		mailIcon.setLayoutParams(mailIconParams);

		nameView = (TextView) v.findViewById(R.id.my_account_name);
		nameView.setEllipsize(TextUtils.TruncateAt.END);
		nameView.setSingleLine();
		LinearLayout.LayoutParams nameViewParams = (LinearLayout.LayoutParams)nameView.getLayoutParams();
		nameViewParams.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(26, outMetrics), 0, 0);
		nameView.setLayoutParams(nameViewParams);

		nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		nameView.setOnClickListener(this);

		infoEmail = (TextView) v.findViewById(R.id.my_account_email);
		myEmail = megaApi.getMyUser().getEmail();
		infoEmail.setText(myEmail);
		LinearLayout.LayoutParams infoEmailParams = (LinearLayout.LayoutParams)infoEmail.getLayoutParams();
		infoEmailParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, Util.scaleHeightPx(26, outMetrics));
		infoEmail.setLayoutParams(infoEmailParams);
		infoEmail.setOnClickListener(this);
		
		myAccountImage = (RoundedImageView) v.findViewById(R.id.my_account_thumbnail);

		RelativeLayout.LayoutParams avatarLayoutParams = (RelativeLayout.LayoutParams)avatarLayout.getLayoutParams();
		avatarLayoutParams.setMargins(0, Util.scaleHeightPx(16, outMetrics), Util.scaleWidthPx(16, outMetrics), Util.scaleHeightPx(16, outMetrics));
		avatarLayout.setLayoutParams(avatarLayoutParams);

		initialLetter = (TextView) v.findViewById(R.id.my_account_initial_letter);

		mkButton = (Button) v.findViewById(R.id.MK_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			mkButton.setBackground(ContextCompat.getDrawable(context, R.drawable.white_rounded_corners_button));
		}
		else{
			mkButton.setBackgroundResource(R.drawable.black_button_border);
		}
		mkButton.setOnClickListener(this);
		mkButton.setVisibility(View.VISIBLE);

		LinearLayout.LayoutParams mkButtonParams = (LinearLayout.LayoutParams)mkButton.getLayoutParams();
		mkButtonParams.setMargins(Util.scaleWidthPx(57, outMetrics), Util.scaleHeightPx(8, outMetrics), 0, Util.scaleHeightPx(8, outMetrics));
		mkButton.setLayoutParams(mkButtonParams);

		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
		log("Exists MK in: "+path);
		File file= new File(path);
		if(file.exists()){
			mkButton.setText(getString(R.string.action_remove_master_key));
		}
		else{
			mkButton.setText(getString(R.string.action_export_master_key));
		}

		typeLayout = (RelativeLayout) v.findViewById(R.id.my_account_account_type_layout);
		LinearLayout.LayoutParams typeLayoutParams = (LinearLayout.LayoutParams)typeLayout.getLayoutParams();
		typeLayoutParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(27, outMetrics), 0, 0);
		typeLayout.setLayoutParams(typeLayoutParams);

		typeAccount = (TextView) v.findViewById(R.id.my_account_account_type_text);
		LinearLayout.LayoutParams typeAccountParams = (LinearLayout.LayoutParams)typeAccount.getLayoutParams();
		typeAccountParams.setMargins(0, Util.scaleHeightPx(5, outMetrics), 0, 0);
		typeAccount.setLayoutParams(typeAccountParams);

		usedSpace = (TextView) v.findViewById(R.id.my_account_used_space);

		upgradeButton = (Button) v.findViewById(R.id.my_account_account_type_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			upgradeButton.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
		}
		upgradeButton.setText(getString(R.string.my_account_upgrade_pro).toUpperCase(Locale.getDefault()));
		upgradeButton.setOnClickListener(this);
		upgradeButton.setVisibility(View.VISIBLE);

		RelativeLayout.LayoutParams upgradeButtonParams = (RelativeLayout.LayoutParams)upgradeButton.getLayoutParams();
		upgradeButtonParams.setMargins(0, 0, Util.scaleWidthPx(16, outMetrics), 0);
		upgradeButton.setLayoutParams(upgradeButtonParams);

		lastSessionLayout = (LinearLayout) v.findViewById(R.id.my_account_last_session_layout);
		LinearLayout.LayoutParams lastSessionParams = (LinearLayout.LayoutParams)lastSessionLayout.getLayoutParams();
		lastSessionParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		lastSessionLayout.setLayoutParams(lastSessionParams);

		lastSession = (TextView) v.findViewById(R.id.my_account_last_session);

		fingerprintLayout = (LinearLayout) v.findViewById(R.id.my_account_fingerprint_layout);
		LinearLayout.LayoutParams fingerprintParams = (LinearLayout.LayoutParams)fingerprintLayout.getLayoutParams();
		fingerprintParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		fingerprintLayout.setLayoutParams(fingerprintParams);

		fingerprint = (TextView) v.findViewById(R.id.my_account_fingerprint);

		connectionsLayout = (LinearLayout) v.findViewById(R.id.my_account_connections_layout);
		LinearLayout.LayoutParams connectionsParams = (LinearLayout.LayoutParams)connectionsLayout.getLayoutParams();
		connectionsParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		connectionsLayout.setLayoutParams(connectionsParams);

		connections = (TextView) v.findViewById(R.id.my_account_connections);

		logoutButton = (Button) v.findViewById(R.id.logout_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			logoutButton.setBackground(ContextCompat.getDrawable(context, R.drawable.white_rounded_corners_button));
		}
		else{
			logoutButton.setBackgroundResource(R.drawable.black_button_border);
		}
		logoutButton.setOnClickListener(this);
		logoutButton.setVisibility(View.VISIBLE);

		LinearLayout.LayoutParams logoutButtonParams = (LinearLayout.LayoutParams)logoutButton.getLayoutParams();
		logoutButtonParams.setMargins(Util.scaleWidthPx(57, outMetrics), Util.scaleHeightPx(24, outMetrics), 0, Util.scaleHeightPx(0, outMetrics));
		logoutButton.setLayoutParams(logoutButtonParams);

		deleteAccountButton = (Button) v.findViewById(R.id.delete_account_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			deleteAccountButton.setBackground(ContextCompat.getDrawable(context, R.drawable.red_rounded_corners_button));
		}
		deleteAccountButton.setOnClickListener(this);

		LinearLayout.LayoutParams deleteAccountParams = (LinearLayout.LayoutParams)deleteAccountButton.getLayoutParams();
		deleteAccountParams.setMargins(Util.scaleWidthPx(57, outMetrics), Util.scaleHeightPx(24, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));
		deleteAccountButton.setLayoutParams(deleteAccountParams);

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.parent_linear_layout);
		exportMKLayout = (RelativeLayout) v.findViewById(R.id.export_mk_full_layout);
		LinearLayout.LayoutParams exportMKButtonsParams = (LinearLayout.LayoutParams)exportMKLayout.getLayoutParams();
		exportMKButtonsParams.setMargins(0, 0, 0, Util.scaleHeightPx(10, outMetrics));
		exportMKLayout.setLayoutParams(exportMKButtonsParams);

		exportMKButtonsLayout = (LinearLayout) v.findViewById(R.id.MK_buttons_layout);

		titleExportMK = (TextView) v.findViewById(R.id.title_export_MK_layout);
		RelativeLayout.LayoutParams titleExportMKParams = (RelativeLayout.LayoutParams)titleExportMK.getLayoutParams();
		titleExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(50, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
		titleExportMK.setLayoutParams(titleExportMKParams);

		subTitleExportMK = (TextView) v.findViewById(R.id.subtitle_export_MK_layout);
		RelativeLayout.LayoutParams subTitleExportMKParams = (RelativeLayout.LayoutParams)subTitleExportMK.getLayoutParams();
		subTitleExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(24, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
		subTitleExportMK.setLayoutParams(subTitleExportMKParams);

		firstParExportMK = (TextView) v.findViewById(R.id.first_par_export_MK_layout);
		RelativeLayout.LayoutParams firstParExportMKParams = (RelativeLayout.LayoutParams)firstParExportMK.getLayoutParams();
		firstParExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
		firstParExportMK.setLayoutParams(firstParExportMKParams);

		secondParExportMK = (TextView) v.findViewById(R.id.second_par_export_MK_layout);
		RelativeLayout.LayoutParams secondParExportMKParams = (RelativeLayout.LayoutParams)secondParExportMK.getLayoutParams();
		secondParExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
		secondParExportMK.setLayoutParams(secondParExportMKParams);

		thirdParExportMK = (TextView) v.findViewById(R.id.third_par_export_MK_layout);
		RelativeLayout.LayoutParams thirdParExportMKParams = (RelativeLayout.LayoutParams)thirdParExportMK.getLayoutParams();
		thirdParExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(24, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
		thirdParExportMK.setLayoutParams(thirdParExportMKParams);

		actionExportMK = (TextView) v.findViewById(R.id.action_export_MK_layout);
		RelativeLayout.LayoutParams actionExportMKParams = (RelativeLayout.LayoutParams)actionExportMK.getLayoutParams();
		actionExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
		actionExportMK.setLayoutParams(actionExportMKParams);

		copyMK = (Button) v.findViewById(R.id.copy_MK_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			copyMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
		}
		LinearLayout.LayoutParams copyMKParams = (LinearLayout.LayoutParams)copyMK.getLayoutParams();
		copyMKParams.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), 0, 0);
		copyMK.setLayoutParams(copyMKParams);
		copyMK.setOnClickListener(this);

		saveMK = (Button) v.findViewById(R.id.save_MK_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			saveMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
		}
		LinearLayout.LayoutParams saveMKParams = (LinearLayout.LayoutParams)saveMK.getLayoutParams();
		saveMKParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(20, outMetrics), 0, 0);
		saveMK.setLayoutParams(saveMKParams);
		saveMK.setOnClickListener(this);

		if(myAccountInfo==null){
			log("MyAccountInfo is NULL");
			myAccountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
		}

		mKLayoutVisible = ((ManagerActivityLollipop)context).isMkLayoutVisible();

		if(myAccountInfo!=null){
			if((myAccountInfo.getFullName()!=null) && (!myAccountInfo.getFullName().isEmpty())){
				log("MyName is:"+ myAccountInfo.getFullName());
				nameView.setText(myAccountInfo.getFullName());
			}
			else{
				myAccountInfo.setFirstName(false);
				myAccountInfo.setLastName(false);

				megaApi.getUserAttribute(myUser, MegaApiJava.USER_ATTR_FIRSTNAME, myAccountInfo);
				megaApi.getUserAttribute(myUser, MegaApiJava.USER_ATTR_LASTNAME, myAccountInfo);
			}
		}
		else{
			myAccountInfo.setFirstName(false);
			myAccountInfo.setLastName(false);

			megaApi.getUserAttribute(myUser, MegaApiJava.USER_ATTR_FIRSTNAME, myAccountInfo);
			megaApi.getUserAttribute(myUser, MegaApiJava.USER_ATTR_LASTNAME, myAccountInfo);
		}

		this.updateAvatar(true);

		if(mKLayoutVisible){
			log("on Create MK visible");
			((ManagerActivityLollipop)context).showMKLayout(true);
			showMKLayout();
		}

		fingerprintLayout.setVisibility(View.GONE);
		if (megaApi.getMyFingerprint() != null){
			if (megaApi.getMyFingerprint().compareTo("") != 0){
				fingerprintLayout.setVisibility(View.VISIBLE);
				String fingerprintString = megaApi.getMyFingerprint();
				String fingerprintUIString = "";
				for (int i=0;i<fingerprintString.length();i++){
					if (i != 0){
						if ((i % 20) == 0){
							fingerprintUIString = fingerprintUIString + "\n" + fingerprintString.charAt(i);
						}
						else if ((i % 4) == 0){
							fingerprintUIString = fingerprintUIString + " " + fingerprintString.charAt(i);
						}
						else{
							fingerprintUIString = fingerprintUIString + fingerprintString.charAt(i);
						}
					}
					else{
						fingerprintUIString = fingerprintUIString + fingerprintString.charAt(i);
					}
				}

				fingerprint.setText(fingerprintUIString);
			}
		}
		
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		ArrayList<MegaUser> visibleContacts=new ArrayList<MegaUser>();

		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}		
		connections.setText(visibleContacts.size()+" " + context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));

		setAccountDetails();

		refreshAccountInfo();

		return v;
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
			megaApi.getAccountDetails(myAccountInfo);
		}
		log("Check the last call to getExtendedAccountDetails");
		if(DBUtil.callToExtendedAccountDetails(context)){
			log("megaApi.getExtendedAccountDetails SEND");
			megaApi.getExtendedAccountDetails(true, false, false, myAccountInfo);
		}
		log("Check the last call to callToPaymentMethods");
		if(DBUtil.callToPaymentMethods(context)){
			log("megaApi.getPaymentMethods SEND");
			megaApi.getPaymentMethods(myAccountInfo);
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
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}

	@Override
	public void onAttach(Context context) {
		log("onAttach context");
		super.onAttach(context);
		this.context = context;
		aB = ((AppCompatActivity)getActivity()).getSupportActionBar();
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		switch (v.getId()) {

			case R.id.logout_button:{
				log("Logout button");
				AccountController aC = new AccountController(context);
				aC.logout(context, megaApi, false);
				break;
			}
			case R.id.my_account_relative_layout_avatar:{
				log("Click layout avatar");
				((ManagerActivityLollipop)context).showMyAccountOptionsPanel();
				break;
			}
			case R.id.my_account_name:
			case R.id.my_account_email:{
				log("Click user attributes text");
				((ManagerActivityLollipop)context).showDialogChangeUserAttribute();
				break;
			}
			case R.id.MK_button:{
				log("Master Key button");
				String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
				log("Exists MK in: "+path);
				File file= new File(path);
				if(file.exists()){
					((ManagerActivityLollipop)context).showConfirmationRemoveMK();
				}
				else{
					((ManagerActivityLollipop)context).showMKLayout(true);
					showMKLayout();
				}

				break;
			}
			case R.id.copy_MK_button:{
				log("Copy Master Key button");
				hideMKLayout();
				AccountController aC = new AccountController(context);
				aC.copyMK();
				break;
			}
			case R.id.save_MK_button:{
				log("Save Master Key button");
				hideMKLayout();
				AccountController aC = new AccountController(context);
				aC.exportMK();
				break;
			}
			case R.id.delete_account_button:{
				log("Delete Account button");
				((ManagerActivityLollipop)context).askConfirmationDeleteAccount();
				break;
			}
			case R.id.my_account_account_type_button:{
				log("Upgrade Account button");
				((ManagerActivityLollipop)context).showUpAF();

				break;
			}
		}
	}

	public void updateMKButton(){
		log("updateMKButton");
		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
		log("update MK Button - Exists MK in: "+path);
		File file= new File(path);
		if(file.exists()){
			mkButton.setText(getString(R.string.action_remove_master_key));
		}
		else{
			mkButton.setText(getString(R.string.action_export_master_key));
		}
	}
	
	public int onBackPressed(){
		log("onBackPressed");

		if(exportMKLayout.getVisibility()==View.VISIBLE){
			log("Master Key layout is VISIBLE");
			hideMKLayout();
			return 1;
		}

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
		myEmail=newMail;
		if (newMail != null){
			infoEmail.setText(newMail);

		}
		File avatar = null;
		if (context.getExternalCacheDir() != null){
			avatar = new File(context.getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
		}
		else{
			avatar = new File(context.getCacheDir().getAbsolutePath(), myEmail + ".jpg");
		}

		if (!avatar.exists()){
			initialLetter.setText(myAccountInfo.getFirstLetter());
			initialLetter.setTextSize(30);
			initialLetter.setTextColor(Color.WHITE);
			initialLetter.setVisibility(View.VISIBLE);
		}
	}

	public static void log(String log) {
		Util.log("MyAccountFragmentLollipop", log);
	}

	public void showMKLayout(){
		log("showMKLayout");
		parentLinearLayout.setVisibility(View.GONE);
		exportMKLayout.setVisibility(View.VISIBLE);
		mKLayoutVisible=false;
	}

	public void resetPass(){
		AccountController aC = new AccountController(context);
		aC.resetPass(myEmail);
	}

	public void hideMKLayout(){
		log("hideMKLayout");
		exportMKLayout.setVisibility(View.GONE);
		parentLinearLayout.setVisibility(View.VISIBLE);
		mKLayoutVisible=false;
		((ManagerActivityLollipop)context).hideMKLayout();
	}

	public void updateAvatar(boolean retry){
		log("updateAvatar");
		File avatar = null;
		String contactEmail = myUser.getEmail();
		if(context!=null){
			log("context is not null");

			if (context.getExternalCacheDir() != null){
				avatar = new File(context.getExternalCacheDir().getAbsolutePath(), contactEmail + ".jpg");
			}
			else{
				avatar = new File(context.getCacheDir().getAbsolutePath(), contactEmail + ".jpg");
			}
		}
		else{
			log("context is null!!!");
			if(getActivity()!=null){
				log("getActivity is not null");
				if (getActivity().getExternalCacheDir() != null){
					avatar = new File(getActivity().getExternalCacheDir().getAbsolutePath(), contactEmail + ".jpg");
				}
				else{
					avatar = new File(getActivity().getCacheDir().getAbsolutePath(), contactEmail + ".jpg");
				}
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
		myAccountImage.setImageBitmap(defaultAvatar);

		int avatarTextSize = getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);

		initialLetter.setText(myAccountInfo.getFirstLetter());
		initialLetter.setTextSize(30);
		initialLetter.setTextColor(Color.WHITE);
		initialLetter.setVisibility(View.VISIBLE);
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
						if (context.getExternalCacheDir() != null){
							megaApi.getUserAvatar(myUser, context.getExternalCacheDir().getAbsolutePath() + "/" + myEmail, myAccountInfo);
						}
						else{
							megaApi.getUserAvatar(myUser, context.getCacheDir().getAbsolutePath() + "/" + myEmail, myAccountInfo);
						}
					}
					else{
						log("DO NOT Retry!");
						setDefaultAvatar();
					}
				}
				else{
					log("Show my avatar");
					myAccountImage.setImageBitmap(imBitmap);
					initialLetter.setVisibility(View.GONE);
				}
			}
		}else{
			log("my avatar NOT exists!");
			log("Call to getUserAvatar");
			if(retry){
				log("Retry!");
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(myUser, context.getExternalCacheDir().getAbsolutePath() + "/" + myEmail, myAccountInfo);
				}
				else{
					megaApi.getUserAvatar(myUser, context.getCacheDir().getAbsolutePath() + "/" + myEmail, myAccountInfo);
				}
			}
			else{
				log("DO NOT Retry!");
				setDefaultAvatar();
			}
		}
	}

	public MyAccountInfo getMyAccountInfo() {
		return myAccountInfo;
	}

	public void setMyAccountInfo(MyAccountInfo myAccountInfo) {
		log("setMyAccountInfo");
		this.myAccountInfo = myAccountInfo;
	}

}
