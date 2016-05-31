package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
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

	TextView initialLetter;
	RoundedImageView myAccountImage;
	
	TextView nameView;

	String myEmail;
	MegaUser myUser;
	
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
	
	DisplayMetrics outMetrics;
	

//	String userEmail;	
	
	MegaApiAndroid megaApi;
	
	long numberOfSubscriptions = -1;
	
	private boolean name = false;
	private boolean firstName = false;
	String nameText;
	String firstNameText;
	
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

		nameView = (TextView) v.findViewById(R.id.my_account_name);
		nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		nameView.setSingleLine();
		nameView.setTypeface(null, Typeface.BOLD);	
		
		nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));

		buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_layout);
		
		myAccountImage = (RoundedImageView) v.findViewById(R.id.my_account_thumbnail);
		initialLetter = (TextView) v.findViewById(R.id.my_account_initial_letter);

		deleteAccountButton = (Button) v.findViewById(R.id.delete_account_button);
		deleteAccountButton.setOnClickListener(this);
		deleteAccountButton.setVisibility(View.VISIBLE);

		mkButton = (Button) v.findViewById(R.id.MK_button);
		mkButton.setOnClickListener(this);
		mkButton.setVisibility(View.VISIBLE);

		logoutButton = (Button) v.findViewById(R.id.logout_button);
		logoutButton.setOnClickListener(this);
		logoutButton.setVisibility(View.VISIBLE);
		
		infoEmail = (TextView) v.findViewById(R.id.my_account_email);
		typeAccount = (TextView) v.findViewById(R.id.my_account_account_type_text);
		expirationAccount = (TextView) v.findViewById(R.id.my_account_expiration);
		usedSpace = (TextView) v.findViewById(R.id.my_account_used_space);
		lastSession = (TextView) v.findViewById(R.id.my_account_last_session);
		connections = (TextView) v.findViewById(R.id.my_account_connections);
		
		typeLayout = (RelativeLayout) v.findViewById(R.id.my_account_account_type_layout);
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) typeLayout.getLayoutParams();
		lp.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
		typeLayout.setLayoutParams(lp);
		
		expirationLayout = (LinearLayout) v.findViewById(R.id.my_account_expiration_layout);

		lastSessionLayout = (LinearLayout) v.findViewById(R.id.my_account_last_session_layout);
		connectionsLayout = (LinearLayout) v.findViewById(R.id.my_account_connections_layout);
		
		typeLayout.setVisibility(View.GONE);
		expirationLayout.setVisibility(View.GONE);
		lastSessionLayout.setVisibility(View.GONE);
		
		upgradeButton = (Button) v.findViewById(R.id.my_account_account_type_button);
		upgradeButton.setText(getString(R.string.my_account_upgrade_pro).toUpperCase(Locale.getDefault()));
		upgradeButton.setOnClickListener(this);
		upgradeButton.setVisibility(View.VISIBLE);
		
		infoEmail.setText(myEmail);
		
		name=false;
		firstName=false;
		megaApi.getUserAttribute(myUser, 1, this);
		megaApi.getUserAttribute(myUser, 2, this);

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
	    if (myEmail != null){
		    if (myEmail.length() > 0){
		    	log("TEXT: " + myEmail);
		    	log("TEXT AT 0: " + myEmail.charAt(0));
		    	String firstLetter = myEmail.charAt(0) + "";
		    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
		    	initialLetter.setText(firstLetter);
		    	initialLetter.setTextSize(100);
		    	initialLetter.setTextColor(Color.WHITE);
		    	initialLetter.setVisibility(View.VISIBLE);
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
		
		/*
		
		avatarLayout = (RelativeLayout) v.findViewById(R.id.my_account_avatar_layout);
		avatarLayout.getLayoutParams().width = Util.px2dp((200*scaleW), outMetrics);
		avatarLayout.getLayoutParams().height = Util.px2dp((200*scaleW), outMetrics);
	
		imageView = (RoundedImageView) v.findViewById(R.id.my_avatar_image);
		imageView.getLayoutParams().width = Util.px2dp((200*scaleW), outMetrics);
		imageView.getLayoutParams().height = Util.px2dp((200*scaleW), outMetrics);
		
		initialLetter = (TextView) v.findViewById(R.id.my_account_initial_letter);
		
		userNameTextView = (TextView) v.findViewById(R.id.my_name);
		infoEmail = (TextView) v.findViewById(R.id.my_email);
		bottomControlBar = (TableLayout) v.findViewById(R.id.progress_my_account);
		
		usedSpace = (TextView) v.findViewById(R.id.used_space_my_account);
	    usedSpaceText = (TextView) v.findViewById(R.id.used_space_text_my_account);
	    usedSpaceBar = (ProgressBar) v.findViewById(R.id.my_account_used_space_bar);	      
	    usedSpaceBar.setProgress(0);    
	    
		titleTypeAccount = (TextView) v.findViewById(R.id.my_account_title);	  
		typeAccount = (TextView) v.findViewById(R.id.my_account_type_account);	 
		expirationAccount = (TextView) v.findViewById(R.id.my_account_expiration);
		expiresOn = (TextView) v.findViewById(R.id.my_account_expires_on);
		expirationAccount.setVisibility(View.GONE);
		expiresOn.setVisibility(View.GONE);
		titleLastSession = (TextView) v.findViewById(R.id.my_last_session_title);	
		lastSession= (TextView) v.findViewById(R.id.my_last_session);	
		titleConnections = (TextView) v.findViewById(R.id.my_connections_title);	
		connections = (TextView) v.findViewById(R.id.my_connections);	
			
		upgradeButton = (Button) v.findViewById(R.id.btn_upgrade); 
		upgradeButton.setOnClickListener(this); 

		myEmail=megaApi.getMyUser().getEmail();
		infoEmail.setText(myEmail);
		
		logoutButton = (Button) v.findViewById(R.id.my_account_logout);
		logoutButton.setOnClickListener(this);
		
		//My Name
		megaApi.getUserData(this);
		
		userNameTextView.setText(myEmail);		
		myUser = megaApi.getMyUser();
		
		name=false;
		firstName=false;
		megaApi.getUserAttribute(1, this);
		megaApi.getUserAttribute(2, this);

		logoutButton.setText(R.string.action_logout);
		lastSession.setText(R.string.general_not_yet_implemented);
		
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		ArrayList<MegaUser> visibleContacts=new ArrayList<MegaUser>();

		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}		
		connections.setText(visibleContacts.size()+" " + context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));
		
		getPaymentMethodsBoolean = false;
		accountDetailsBoolean = false;
		megaApi.getPaymentMethods(this);
		megaApi.getAccountDetails(this);
		numberOfSubscriptions = ((ManagerActivityLollipop)context).getNumberOfSubscriptions();

		upgradeButton.setVisibility(View.INVISIBLE);
//		if (numberOfSubscriptions > 0){
//			upgradeButton.setVisibility(View.INVISIBLE);
//		}
//		else if (numberOfSubscriptions == 0){
//			upgradeButton.setVisibility(View.VISIBLE);
//		}
		
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
		imageView.setImageBitmap(defaultAvatar);
		
	    int avatarTextSize = getAvatarTextSize(density);
	    log("DENSITY: " + density + ":::: " + avatarTextSize);
	    if (myEmail != null){
		    if (myEmail.length() > 0){
		    	log("TEXT: " + myEmail);
		    	log("TEXT AT 0: " + myEmail.charAt(0));
		    	String firstLetter = myEmail.charAt(0) + "";
		    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
		    	initialLetter.setText(firstLetter);
		    	initialLetter.setTextSize(100);
		    	initialLetter.setTextColor(Color.WHITE);
		    	initialLetter.setVisibility(View.VISIBLE);
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
					imageView.setImageBitmap(imBitmap);
					initialLetter.setVisibility(View.GONE);
				}
			}
		}
		//infoAdded.setText(contact.getTimestamp()+"");
		
		*/
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

		switch (v.getId()) {

			case R.id.logout_button:{
				AccountController aC = new AccountController(context);
				aC.logout(context, megaApi, false);
				break;
			}
//			case R.id.my_account_main_layout:{
//				if (overflowMenuLayout != null){
//					if (overflowMenuLayout.getVisibility() == View.VISIBLE){
//						overflowMenuLayout.setVisibility(View.GONE);
//						return;
//					}
//				}
//				break;
//			}
//			case R.id.my_account_toolbar_back:{
//				((ManagerActivityLollipop)context).showCloudDrive();
//				break;
//			}
//			case R.id.my_account_toolbar_overflow:{
//				overflowMenuLayout.setVisibility(View.VISIBLE);
//				break;
//			}
//			case R.id.my_account_logout:{
//				AccountController aC = new AccountController(context);
//				aC.logout(context, megaApi, false);
//				break;
//			}
//			case R.id.my_account_account_type_button:{
//
//				((ManagerActivityLollipop)context).showUpAF(null);
//
//				break;
//			}
		}
	}
	
	public int onBackPressed(){
//		if (overflowMenuLayout != null){
//			if (overflowMenuLayout.getVisibility() == View.VISIBLE){
//				overflowMenuLayout.setVisibility(View.GONE);
//				return 1;
//			}
//		}
		
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

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart()");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
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
								initialLetter.setVisibility(View.GONE);
							}
						}
					}
				}
				else if(request.getParamType()==1){
					log("(1)request.getText(): "+request.getText());
					nameText=request.getText();
					name=true;
				}
				else if(request.getParamType()==2){
					log("(2)request.getText(): "+request.getText());
					firstNameText = request.getText();
					firstName = true;
				}
				if(name&&firstName){
					if (nameView != null){
						nameView.setText(nameText+" "+firstNameText);
						if (nameText != null){
							if (nameText.length() > 0){
								String firstLetter = nameText.charAt(0) + "";
								firstLetter = firstLetter.toUpperCase(Locale.getDefault());
//								initialLetter.setText(firstLetter);
//								initialLetter.setTextSize(100);
//								initialLetter.setTextColor(Color.WHITE);
							}
						}
					}
					name= false;
					firstName = false;
				}
				
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
						typeAccount.setTextColor(getActivity().getResources().getColor(R.color.green_free_account));
						typeAccount.setText(R.string.free_account);
						expirationLayout.setVisibility(View.GONE);
						break;
					}
						
					case 1:{
						typeAccount.setText(getString(R.string.pro1_account));
						expirationLayout.setVisibility(View.VISIBLE);
						expirationAccount.setText(Util.getDateString(accountInfo.getProExpiration()));
						break;
					}
					
					case 2:{
						typeAccount.setText(getString(R.string.pro2_account));
						expirationLayout.setVisibility(View.VISIBLE);
						expirationAccount.setText(Util.getDateString(accountInfo.getProExpiration()));
						break;
					}
					
					case 3:{
						typeAccount.setText(getString(R.string.pro3_account));
						expirationLayout.setVisibility(View.VISIBLE);
						expirationAccount.setText(Util.getDateString(accountInfo.getProExpiration()));
						break;
					}
					
					case 4:{
						typeAccount.setText(getString(R.string.prolite_account));
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
				
				String usedSpaceString = used + " / " + total;
		        usedSpace.setText(usedSpaceString);
				typeLayout.setVisibility(View.VISIBLE);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_KILL_SESSION){
			if (e.getErrorCode() == MegaError.API_OK){
//				Snackbar.make(mainLayout, getString(R.string.success_kill_all_sessions), Snackbar.LENGTH_LONG).show();
			}
			else
			{
				log("error when killing sessions: "+e.getErrorString());
//				Snackbar.make(mainLayout, getString(R.string.error_kill_all_sessions), Snackbar.LENGTH_LONG).show();
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
						initialLetter.setVisibility(View.GONE);
					}
				}
			}
		}
	}
	
	public void updateUserName(String name){
		nameView.setText(name);
	}
}
