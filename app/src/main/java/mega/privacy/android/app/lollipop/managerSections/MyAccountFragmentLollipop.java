package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

public class MyAccountFragmentLollipop extends Fragment implements OnClickListener{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	Context context;
	MyAccountInfo myAccountInfo;

	RelativeLayout avatarLayout;
	TextView initialLetter;
	RoundedImageView myAccountImage;

	TextView nameView;

	String myEmail;
	MegaUser myUser;

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

	RelativeLayout typeLayout;
	LinearLayout lastSessionLayout;
	LinearLayout connectionsLayout;

	LinearLayout achievementsLayout;
	LinearLayout achievementsSeparator;

	LinearLayout parentLinearLayout;
	
	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

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

		if(megaChatApi==null){
			megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = ((Activity) context).getResources().getDisplayMetrics().density;

		View v = null;
		v = inflater.inflate(R.layout.fragment_my_account, container, false);
		
		myUser = megaApi.getMyUser();
		if(myUser == null){
			return null;
		}

		log("My user handle: "+myUser.getHandle()+"****"+MegaApiJava.userHandleToBase64(myUser.getHandle()));
		avatarLayout = (RelativeLayout) v.findViewById(R.id.my_account_relative_layout_avatar);
		avatarLayout.setOnClickListener(this);

		nameView = (TextView) v.findViewById(R.id.my_account_name);
		nameView.setOnClickListener(this);

		editImageView = (ImageView) v.findViewById(R.id.my_account_edit_icon);
		editImageView.setOnClickListener(this);

		infoEmail = (TextView) v.findViewById(R.id.my_account_email);
		myEmail = megaApi.getMyUser().getEmail();
		infoEmail.setText(myEmail);
		infoEmail.setOnClickListener(this);
		
		myAccountImage = (RoundedImageView) v.findViewById(R.id.my_account_thumbnail);

		initialLetter = (TextView) v.findViewById(R.id.my_account_initial_letter);

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

		typeLayout = (RelativeLayout) v.findViewById(R.id.my_account_account_type_layout);

		typeAccount = (TextView) v.findViewById(R.id.my_account_account_type_text);

		usedSpace = (TextView) v.findViewById(R.id.my_account_used_space);

		upgradeButton = (Button) v.findViewById(R.id.my_account_account_type_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			upgradeButton.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
		}
		upgradeButton.setText(getString(R.string.my_account_upgrade_pro));
		upgradeButton.setOnClickListener(this);
		upgradeButton.setVisibility(View.VISIBLE);

		lastSessionLayout = (LinearLayout) v.findViewById(R.id.my_account_last_session_layout);
		lastSession = (TextView) v.findViewById(R.id.my_account_last_session);

		connectionsLayout = (LinearLayout) v.findViewById(R.id.my_account_connections_layout);

		connections = (TextView) v.findViewById(R.id.my_account_connections);

		logoutButton = (Button) v.findViewById(R.id.logout_button);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			logoutButton.setBackground(ContextCompat.getDrawable(context, R.drawable.white_rounded_corners_button));
		}
		else{
			logoutButton.setBackgroundResource(R.drawable.black_button_border);
//			logoutButton.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
		}
		logoutButton.setOnClickListener(this);
		logoutButton.setVisibility(View.VISIBLE);

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.parent_linear_layout);

		if(myAccountInfo==null){
			log("MyAccountInfo is NULL");
			myAccountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
		}

		if(myAccountInfo!=null){
			log("myAccountInfo!=NULL");
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
			log("myAccountInfo is NULL");
			myAccountInfo.setFirstName(false);
			myAccountInfo.setLastName(false);

			megaApi.getUserAttribute(myUser, MegaApiJava.USER_ATTR_FIRSTNAME, myAccountInfo);
			megaApi.getUserAttribute(myUser, MegaApiJava.USER_ATTR_LASTNAME, myAccountInfo);
		}

		this.updateAvatar(true);

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

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		return v;
	}

	public void setMkButtonText(){
		log("setMkButtonText");
		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
		log("Exists MK in: "+path);
		File file= new File(path);
		String mkButtonText;
		if(file.exists()){
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
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		switch (v.getId()) {

			case R.id.logout_button:{
				log("Logout button");

				((ManagerActivityLollipop)getContext()).setPasswordReminderFromMyAccount(true);
				megaApi.shouldShowPasswordReminderDialog(true, myAccountInfo);
//				((ManagerActivityLollipop) getContext()).showRememberPasswordDialog(true);

//				AccountController aC = new AccountController(this);
//				aC.logout(this, megaApi);
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
				String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
				log("Exists MK in: "+path);
				File file= new File(path);
				if(file.exists()){
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
				((ManagerActivityLollipop)context).showUpAF();

				break;
			}
			case R.id.my_account_achievements_layout:{
				log("Show achievements");

				if(!Util.isOnline(context)){
					((ManagerActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
				}
				else{
					Intent intent = new Intent(context, AchievementsActivity.class);
//				intent.putExtra("orderGetChildren", orderGetChildren);
					startActivity(intent);
				}
				break;
			}
		}
	}

	public int onBackPressed(){
		log("onBackPressed");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

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

	public void resetPass(){
		AccountController aC = new AccountController(context);
		aC.resetPass(myEmail);
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
			p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
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
