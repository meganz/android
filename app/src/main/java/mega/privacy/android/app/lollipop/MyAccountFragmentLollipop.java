package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.Time;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaAccountSession;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;


public class MyAccountFragmentLollipop extends Fragment implements OnClickListener, MegaRequestListenerInterface{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	public static int MY_ACCOUNT_FRAGMENT = 5000;
	public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	public static int PAYMENT_FRAGMENT = 5002;

	Context context;
	ActionBar aB;

	RelativeLayout avatarLayout;
	TextView initialLetter;
	RoundedImageView myAccountImage;
	ImageView mailIcon;
	
	TextView nameView;

	String myEmail;
	MegaUser myUser;

	int countUserAttributes=0;
	int errorUserAttibutes=0;
	
	TextView typeAccount;
	TextView infoEmail;
	TextView expirationAccount;
	TextView usedSpace;
	TextView lastSession;
	TextView connections;

	LinearLayout buttonsLayout;
	Button upgradeButton;
	Button logoutButton;
	Button mkButton;
	Button deleteAccountButton;
	
	RelativeLayout typeLayout;
	LinearLayout expirationLayout;
	LinearLayout lastSessionLayout;
	LinearLayout connectionsLayout;

	RelativeLayout exportMKLayout;
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
	

//	String userEmail;	
	
	MegaApiAndroid megaApi;
	
	long numberOfSubscriptions = -1;
	
	private boolean name = false;
	private boolean firstName = false;
	String firstNameText;
	String lastNameText;
	
	long paymentBitSetLong;
	BitSet paymentBitSet = null;
	int accountType;
	MegaAccountDetails accountInfo = null;
	
	boolean getPaymentMethodsBoolean = false;
	boolean accountDetailsBoolean = false;

	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
		log("onCreate");
	}
	
	public void onDestroy()
	{
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}

		if(aB!=null){
			aB.setTitle(getString(R.string.section_account));
			log("indicator_menu_white_559");
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

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

		buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_layout);

		mkButton = (Button) v.findViewById(R.id.MK_button);
		mkButton.setOnClickListener(this);
		mkButton.setVisibility(View.VISIBLE);

		LinearLayout.LayoutParams mkButtonParams = (LinearLayout.LayoutParams)mkButton.getLayoutParams();
		mkButtonParams.setMargins(Util.scaleWidthPx(55, outMetrics), Util.scaleHeightPx(8, outMetrics), 0, Util.scaleHeightPx(8, outMetrics));
		mkButton.setLayoutParams(mkButtonParams);

		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
		log("Exists MK in: "+path);
		File file= new File(path);
		if(file.exists()){
			mkButton.setText(getString(R.string.action_remove_master_key));
		}
		else{
			mkButton.setText(getString(R.string.action_export_master_key));
		}

		logoutButton = (Button) v.findViewById(R.id.logout_button);
		logoutButton.setOnClickListener(this);
		logoutButton.setVisibility(View.VISIBLE);

		LinearLayout.LayoutParams logoutButtonParams = (LinearLayout.LayoutParams)logoutButton.getLayoutParams();
		logoutButtonParams.setMargins(Util.scaleWidthPx(18, outMetrics), Util.scaleHeightPx(8, outMetrics), 0, Util.scaleHeightPx(8, outMetrics));
		logoutButton.setLayoutParams(logoutButtonParams);

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
		upgradeButton.setText(getString(R.string.my_account_upgrade_pro).toUpperCase(Locale.getDefault()));
		upgradeButton.setOnClickListener(this);
		upgradeButton.setVisibility(View.VISIBLE);

		RelativeLayout.LayoutParams upgradeButtonParams = (RelativeLayout.LayoutParams)upgradeButton.getLayoutParams();
		upgradeButtonParams.setMargins(0, 0, Util.scaleWidthPx(16, outMetrics), 0);
		upgradeButton.setLayoutParams(upgradeButtonParams);

		expirationLayout = (LinearLayout) v.findViewById(R.id.my_account_expiration_layout);
		LinearLayout.LayoutParams expirationParams = (LinearLayout.LayoutParams)expirationLayout.getLayoutParams();
		expirationParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		expirationLayout.setLayoutParams(expirationParams);
		expirationAccount = (TextView) v.findViewById(R.id.my_account_expiration);

		lastSessionLayout = (LinearLayout) v.findViewById(R.id.my_account_last_session_layout);
		LinearLayout.LayoutParams lastSessionParams = (LinearLayout.LayoutParams)lastSessionLayout.getLayoutParams();
		lastSessionParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		lastSessionLayout.setLayoutParams(lastSessionParams);

		lastSession = (TextView) v.findViewById(R.id.my_account_last_session);

		connectionsLayout = (LinearLayout) v.findViewById(R.id.my_account_connections_layout);
		LinearLayout.LayoutParams connectionsParams = (LinearLayout.LayoutParams)connectionsLayout.getLayoutParams();
		connectionsParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(30, outMetrics), 0, 0);
		connectionsLayout.setLayoutParams(connectionsParams);

		connections = (TextView) v.findViewById(R.id.my_account_connections);

		deleteAccountButton = (Button) v.findViewById(R.id.delete_account_button);
		deleteAccountButton.setOnClickListener(this);

		LinearLayout.LayoutParams deleteAccountParams = (LinearLayout.LayoutParams)deleteAccountButton.getLayoutParams();
		deleteAccountParams.setMargins(Util.scaleWidthPx(55, outMetrics), Util.scaleHeightPx(27, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));
		deleteAccountButton.setLayoutParams(deleteAccountParams);

		typeLayout.setVisibility(View.GONE);
		expirationLayout.setVisibility(View.GONE);
		lastSessionLayout.setVisibility(View.GONE);

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.parent_linear_layout);
		exportMKLayout = (RelativeLayout) v.findViewById(R.id.export_mk_full_layout);

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
		LinearLayout.LayoutParams copyMKParams = (LinearLayout.LayoutParams)copyMK.getLayoutParams();
		copyMKParams.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), 0, 0);
		copyMK.setLayoutParams(copyMKParams);
		copyMK.setOnClickListener(this);

		saveMK = (Button) v.findViewById(R.id.save_MK_button);
		LinearLayout.LayoutParams saveMKParams = (LinearLayout.LayoutParams)saveMK.getLayoutParams();
		saveMKParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(20, outMetrics), 0, 0);
		saveMK.setLayoutParams(saveMKParams);
		saveMK.setOnClickListener(this);

		name=false;
		firstName=false;
		megaApi.getUserAttribute(myUser, 1, this);
		megaApi.getUserAttribute(myUser, 2, this);
		megaApi.getExtendedAccountDetails(true, false, false, this);

		Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));

		int radius;
		if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
			radius = defaultAvatar.getWidth()/2;
		else
			radius = defaultAvatar.getHeight()/2;

		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		myAccountImage.setImageBitmap(defaultAvatar);

	    int avatarTextSize = getAvatarTextSize(density);
	    log("DENSITY: " + density + ":::: " + avatarTextSize);
		boolean setInitialByMail = false;
		if (firstNameText != null){
			if (firstNameText.length() > 0){
				String firstLetter = firstNameText.charAt(0) + "";
				firstLetter = firstLetter.toUpperCase(Locale.getDefault());
				initialLetter.setText(firstLetter);
				initialLetter.setTextSize(40);
				initialLetter.setTextColor(Color.WHITE);
			}else{
				setInitialByMail=true;
			}
		}
		else{
			setInitialByMail=true;
		}
		if(setInitialByMail){
			if (myEmail != null){
				if (myEmail.length() > 0){
					log("email TEXT: " + myEmail);
					log("email TEXT AT 0: " + myEmail.charAt(0));
					String firstLetter = myEmail.charAt(0) + "";
					firstLetter = firstLetter.toUpperCase(Locale.getDefault());
					initialLetter.setText(firstLetter);
					initialLetter.setTextSize(40);
					initialLetter.setTextColor(Color.WHITE);
					initialLetter.setVisibility(View.VISIBLE);
				}
			}
		}
		
	    File avatar = null;
		if (context.getExternalCacheDir() != null){
			avatar = new File(context.getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
		}
		else{
			avatar = new File(context.getCacheDir().getAbsolutePath(), myEmail + ".jpg");
		}

		Bitmap imBitmap = null;
		if (avatar.exists()){
			if (avatar.length() > 0){
				log("my avatar exists!");
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (imBitmap == null) {
					avatar.delete();
					log("Call to getUserAvatar");
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(myUser, context.getExternalCacheDir().getAbsolutePath() + "/" + myEmail, this);
					}
					else{
						megaApi.getUserAvatar(myUser, context.getCacheDir().getAbsolutePath() + "/" + myEmail, this);
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
			if (context.getExternalCacheDir() != null){
				megaApi.getUserAvatar(myUser, context.getExternalCacheDir().getAbsolutePath() + "/" + myEmail, this);
			}
			else{
				megaApi.getUserAvatar(myUser, context.getCacheDir().getAbsolutePath() + "/" + myEmail, this);
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
		
		megaApi.getAccountDetails(this);
		megaApi.getPaymentMethods(this);
		return v;
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
	
	public void setMyEmail(String myEmail){
		this.myEmail = myEmail;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
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
			case R.id.my_account_name:
			case R.id.my_account_email:{
				log("Click user attributes text");
				((ManagerActivityLollipop)context).showDialogChangeUserAttribute();
				break;
			}
			case R.id.MK_button:{
				log("Master Key button");
				String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
				log("Exists MK in: "+path);
				File file= new File(path);
				if(file.exists()){
					((ManagerActivityLollipop)context).showConfirmationRemoveMK();
				}
				else{
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
				((ManagerActivityLollipop)context).showUpAF(null);

				break;
			}
		}
	}

	public void updateMKButton(){
		log("updateMKButton");
		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
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

	public void updateUserName(String firstName, String lastName){
		firstNameText = firstName;
		lastNameText=lastName;
		updateNameView();
	}

	public void updateNameView(){
		log("updateNameView");
		if (nameView != null){
			nameView.setText(firstNameText+" "+lastNameText);

		}
		File avatar = null;
		if (context.getExternalCacheDir() != null){
			avatar = new File(context.getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
		}
		else{
			avatar = new File(context.getCacheDir().getAbsolutePath(), myEmail + ".jpg");
		}
		boolean setInitialByMail = false;
		if (!avatar.exists()){
			if (firstNameText != null){
				if (firstNameText.length() > 0){
					String firstLetter = firstNameText.charAt(0) + "";
					firstLetter = firstLetter.toUpperCase(Locale.getDefault());
					initialLetter.setText(firstLetter);
					initialLetter.setTextSize(40);
					initialLetter.setTextColor(Color.WHITE);
				}else{
					setInitialByMail=true;
				}
			}
			else{
				setInitialByMail=true;
			}
			if(setInitialByMail){
				if (myEmail != null){
					if (myEmail.length() > 0){
						log("email TEXT: " + myEmail);
						log("email TEXT AT 0: " + myEmail.charAt(0));
						String firstLetter = myEmail.charAt(0) + "";
						firstLetter = firstLetter.toUpperCase(Locale.getDefault());
						initialLetter.setText(firstLetter);
						initialLetter.setTextSize(40);
						initialLetter.setTextColor(Color.WHITE);
						initialLetter.setVisibility(View.VISIBLE);
					}
				}
			}
		}
	}

	public void updateMailView(String newMail){
		log("updateNameView: "+newMail);
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
		boolean setInitialByMail = false;
		if (!avatar.exists()){
			if (firstNameText != null){
				if (firstNameText.length() > 0){
					String firstLetter = firstNameText.charAt(0) + "";
					firstLetter = firstLetter.toUpperCase(Locale.getDefault());
					initialLetter.setText(firstLetter);
					initialLetter.setTextSize(40);
					initialLetter.setTextColor(Color.WHITE);
				}else{
					setInitialByMail=true;
				}
			}
			else{
				setInitialByMail=true;
			}
			if(setInitialByMail){
				if (myEmail != null){
					if (myEmail.length() > 0){
						log("email TEXT: " + myEmail);
						log("email TEXT AT 0: " + myEmail.charAt(0));
						String firstLetter = myEmail.charAt(0) + "";
						firstLetter = firstLetter.toUpperCase(Locale.getDefault());
						initialLetter.setText(firstLetter);
						initialLetter.setTextSize(40);
						initialLetter.setTextColor(Color.WHITE);
						initialLetter.setVisibility(View.VISIBLE);
					}
				}
			}
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			log("MegaRequest.TYPE_GET_ATTR_USER");
			if (e.getErrorCode() == MegaError.API_OK){
				if(request.getParamType()==0){
					log("(0)request");
					File avatar = null;
					if (context.getExternalCacheDir() != null){
						avatar = new File(context.getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
					}
					else{
						avatar = new File(context.getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
					}
					Bitmap imBitmap = null;
					if (avatar.exists()){
						if (avatar.length() > 0){
							BitmapFactory.Options bOpts = new BitmapFactory.Options();
							bOpts.inPurgeable = true;
							bOpts.inInputShareable = true;
							imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
							if (imBitmap == null) {
								avatar.delete();
							}
							else{
								myAccountImage.setImageBitmap(imBitmap);
								log("initialLetter.setVisibility");
								initialLetter.setVisibility(View.GONE);
							}
						}
					}
				}
				else if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
					log("(1)request.getText(): "+request.getText());
					firstNameText=request.getText();
					name=true;
				}
				else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
					log("(2)request.getText(): "+request.getText());
					lastNameText = request.getText();
					firstName = true;
				}
				if(name&&firstName){
					updateNameView();
					name= false;
					firstName = false;
				}
				
			}
		}
		else if(request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
			log("TYPE_SET_ATTR_USER");
			if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
				log("(1)request.getText(): "+request.getText());
                countUserAttributes--;
				firstNameText=request.getText();
				if (e.getErrorCode() == MegaError.API_OK){
					log("The first name has changed");
					updateNameView();
					((ManagerActivityLollipop) context).updateUserNameNavigationView(firstNameText, lastNameText);
				}
				else{
					log("Error with first name");
					errorUserAttibutes++;
				}
			}
			else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
				log("(2)request.getText(): "+request.getText());
                countUserAttributes--;
				lastNameText = request.getText();
				if (e.getErrorCode() == MegaError.API_OK){
					log("The last name has changed");
					updateNameView();
					((ManagerActivityLollipop) context).updateUserNameNavigationView(firstNameText, lastNameText);
				}
				else{
					log("Error with last name");
					errorUserAttibutes++;
				}
			}

			if(countUserAttributes==0){
				if(errorUserAttibutes==0){
					log("All user attributes changed!");
					((ManagerActivityLollipop) context).showSnackbar(getString(R.string.success_changing_user_attributes));
				}
				else{
					log("Some error ocurred when changing an attribute: "+errorUserAttibutes);
					((ManagerActivityLollipop) context).showSnackbar(getString(R.string.error_changing_user_attributes));
				}
				AccountController aC = new AccountController(context);
				errorUserAttibutes=0;
				aC.setCount(0);
			}
		}
		else if(request.getType() == MegaRequest.TYPE_GET_CHANGE_EMAIL_LINK) {
			log("TYPE_GET_CHANGE_EMAIL_LINK: "+request.getEmail());
			if (e.getErrorCode() == MegaError.API_OK){
				log("The change link has been sent");
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.email_verification_text_change_pass), getString(R.string.email_verification_title));
			}
			else if(e.getErrorCode() == MegaError.API_EEXIST){
				log("The new mail already exists");
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.mail_already_used), getString(R.string.email_verification_title));
			}
			else{
				log("Error when asking for change mail link");
				log(e.getErrorString() + "___" + e.getErrorCode());
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_GET_RECOVERY_LINK){
			log("TYPE_GET_RECOVERY_LINK");
			if (e.getErrorCode() == MegaError.API_OK){
				log("The recovery link has been sent");
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.email_verification_text_change_pass), getString(R.string.email_verification_title));
			}
			else if (e.getErrorCode() == MegaError.API_ENOENT){
				log("No account with this mail");
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.invalid_email_text), getString(R.string.invalid_email_title));
			}
			else{
				log("Error when asking for recovery pass link");
				log(e.getErrorString() + "___" + e.getErrorCode());
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_GET_CANCEL_LINK){
			log("TYPE_GET_CANCEL_LINK request");
			if (e.getErrorCode() == MegaError.API_OK){
				log("The cancel link has been sent");
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.email_verification_text), getString(R.string.email_verification_title));
			}
			else{
				log("ERROR when asking for link to cancel account");
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_CONFIRM_CANCEL_LINK){
			log("TYPE_CONFIRM_CANCEL_LINK request");
			if (e.getErrorCode() == MegaError.API_OK){
				log("The account has been canceled");
			}
			else{
				log("ERROR when cancelling account: "+e.getErrorString());
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_CONFIRM_CHANGE_EMAIL_LINK){
			log("TYPE_CONFIRM_CHANGE_EMAIL_LINK request");
			if (e.getErrorCode() == MegaError.API_OK){
				log("The mail has been changed");
				updateMailView(request.getEmail());
				((ManagerActivityLollipop) context).updateMailNavigationView(request.getEmail());
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.success_changing_user_mail), getString(R.string.change_mail_title_last_step));
			}
			else{
				log("ERROR when changing email: "+e.getErrorString());
				Util.showAlert(((ManagerActivityLollipop) context), getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_METHODS){
			if (e.getErrorCode() == MegaError.API_OK){
				paymentBitSetLong = request.getNumber();
				paymentBitSet = Util.convertToBitSet(request.getNumber());
				getPaymentMethodsBoolean = true;
				if (accountDetailsBoolean == true){
					if (upgradeButton != null){
						if (accountInfo != null){
							if ((accountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_NONE) || (accountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID)){
								Time now = new Time();
								now.setToNow();
								if (accountType != 0){
									if (now.toMillis(false) >= (accountInfo.getProExpiration()*1000)){
										if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
											upgradeButton.setVisibility(View.VISIBLE);
										}
									}
								}
								else{
									if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
										upgradeButton.setVisibility(View.VISIBLE);
									}
								}
							}
						}
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
			log ("account_details request");
			if (e.getErrorCode() == MegaError.API_OK && typeAccount != null)
			{
				accountInfo = request.getMegaAccountDetails();

				accountType = accountInfo.getProLevel();
				accountDetailsBoolean = true;
				typeLayout.setVisibility(View.VISIBLE);
				switch(accountType){

					case 0:{

						typeAccount.setText(R.string.my_account_free);
						expirationLayout.setVisibility(View.GONE);
						break;
					}

					case 1:{
						typeAccount.setText(getString(R.string.my_account_pro1));
						expirationLayout.setVisibility(View.VISIBLE);
						expirationAccount.setText(Util.getDateString(accountInfo.getProExpiration()));
						break;
					}

					case 2:{
						typeAccount.setText(getString(R.string.my_account_pro2));
						expirationLayout.setVisibility(View.VISIBLE);
						expirationAccount.setText(Util.getDateString(accountInfo.getProExpiration()));
						break;
					}

					case 3:{
						typeAccount.setText(getString(R.string.my_account_pro3));
						expirationLayout.setVisibility(View.VISIBLE);
						expirationAccount.setText(Util.getDateString(accountInfo.getProExpiration()));
						break;
					}

					case 4:{
						typeAccount.setText(getString(R.string.my_account_prolite));
						expirationLayout.setVisibility(View.VISIBLE);
						expirationAccount.setText(Util.getDateString(accountInfo.getProExpiration()));
						break;
					}

				}

				if (getPaymentMethodsBoolean == true){
					if (upgradeButton != null){
						if ((accountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_NONE) || (accountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID)){
							Time now = new Time();
							now.setToNow();
							if (accountType != 0){
								if (now.toMillis(false) >= (accountInfo.getProExpiration()*1000)){
									if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
										upgradeButton.setVisibility(View.VISIBLE);
									}
								}
							}
							else{
								if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
									upgradeButton.setVisibility(View.VISIBLE);
								}
							}
						}
					}
				}

				long totalStorage = accountInfo.getStorageMax();
				long usedStorage = accountInfo.getStorageUsed();

		        int usedPerc = 0;
		        if (totalStorage != 0){
		        	usedPerc = (int)((100 * usedStorage) / totalStorage);
		        }

				boolean totalGb = false;

				totalStorage = ((totalStorage / 1024) / 1024) / 1024;
				String total = "";
				if (totalStorage >= 1024){
					totalStorage = totalStorage / 1024;
					total = total + totalStorage + " TB";
				}
				else{
					 total = total + totalStorage + " GB";
					 totalGb = true;
				}

				usedStorage = ((usedStorage / 1024) / 1024) / 1024;
				String used = "";
				if(totalGb){

					used = used + usedStorage + " GB";

				}
				else{
					if (usedStorage >= 1024){
						usedStorage = usedStorage / 1024;
						used = used + usedStorage + " TB";
					}
					else{
						used = used + usedStorage + " GB";
					}
				}

				String usedSpaceString = used + " " + getString(R.string.general_x_of_x) + " " + total;
		        usedSpace.setText(usedSpaceString);
				typeLayout.setVisibility(View.VISIBLE);
			}
			if(request.getMegaAccountDetails()!=null){
				log("getMegaAccountDetails not Null");

				MegaAccountSession megaAccountSession = request.getMegaAccountDetails().getSession(0);

				if(megaAccountSession!=null){
					log("getMegaAccountSESSION not Null");
					long mostRecentSession = megaAccountSession.getMostRecentUsage();
					log("The last session: "+mostRecentSession);
					java.text.DateFormat df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.MEDIUM,Locale.getDefault());
					Date date = new Date(mostRecentSession * 1000);
					Calendar cal = Calendar.getInstance();
					TimeZone tz = cal.getTimeZone();
					df.setTimeZone(tz);
					String formattedDate = df.format(date);
					log("Formatted date: "+formattedDate);
					lastSession.setText(formattedDate);
					lastSessionLayout.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}

	public static void log(String log) {
		Util.log("MyAccountFragmentLollipop", log);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}

	public void showMKLayout(){
		log("showMKLayout");
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}
		aB.hide();
		parentLinearLayout.setVisibility(View.GONE);
		exportMKLayout.setVisibility(View.VISIBLE);
	}

	public void resetPass(){
		megaApi.resetPassword(myEmail, true, this);
	}

	public void hideMKLayout(){
		log("hideMKLayout");
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}
		aB.show();
		exportMKLayout.setVisibility(View.GONE);
		parentLinearLayout.setVisibility(View.VISIBLE);
	}

	public void updateAvatar(File avatar){
		if(avatar!=null){
			Bitmap imBitmap = null;
			if (avatar.exists()){
				if (avatar.length() > 0){
					BitmapFactory.Options bOpts = new BitmapFactory.Options();
					bOpts.inPurgeable = true;
					bOpts.inInputShareable = true;
					imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
					if (imBitmap == null) {
						avatar.delete();
						if (context.getExternalCacheDir() != null){
							megaApi.getUserAvatar(myUser, context.getExternalCacheDir().getAbsolutePath() + "/" + myEmail, this);
						}
						else{
							megaApi.getUserAvatar(myUser, context.getCacheDir().getAbsolutePath() + "/" + myEmail, this);
						}
					}
					else{
						myAccountImage.setImageBitmap(imBitmap);
						log("initialLetter.setVisibility2");
						initialLetter.setVisibility(View.GONE);
					}
				}
			}
		}
	}


	public int getCountUserAttributes() {
		return countUserAttributes;
	}

	public void setCountUserAttributes(int countUserAttributes) {
		log("setCountUserAttributes: "+countUserAttributes);
		this.countUserAttributes = countUserAttributes;
	}

}
