package mega.privacy.android.app.lollipop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Locale;

import com.nirhart.parallaxscroll.views.ParallaxScrollView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MyAccountFragmentLollipop extends Fragment implements OnClickListener, MegaRequestListenerInterface, OnItemClickListener {
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	public static int MY_ACCOUNT_FRAGMENT = 5000;
	public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	public static int PAYMENT_FRAGMENT = 5002;

	Context context;
	
	ParallaxScrollView sV;
	RelativeLayout mainLayout;
	RelativeLayout imageLayout;
	RelativeLayout optionsBackLayout;
	ImageView toolbarBack;
	ImageView toolbarOverflow;
	RelativeLayout overflowMenuLayout;
	ListView overflowMenuList;
	
	TextView initialLetter;
	ImageView myAccountImage;
	
	TextView nameView;
	ImageView nameIcon;
	
	String myEmail;
	MegaUser myUser;
	
	TextView typeAccount;
	TextView infoEmail;
	TextView expirationAccount;
	TextView usedSpace;
	TextView lastSession;
	TextView connections;
	
	Button upgradeButton;
	
	RelativeLayout typeLayout;
	LinearLayout expirationLayout;
	LinearLayout usedSpaceLayout;
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

	public int getStatusBarHeight() { 
	      int result = 0;
	      int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
	      if (resourceId > 0) {
	          result = getResources().getDimensionPixelSize(resourceId);
	      } 
	      return result;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
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
		
		sV = (ParallaxScrollView) v.findViewById(R.id.my_account_scroll_view);
		sV.post(new Runnable() { 
	        public void run() { 
	             sV.scrollTo(0, outMetrics.heightPixels/3);
	        } 
		});
		
		mainLayout = (RelativeLayout) v.findViewById(R.id.my_account_main_layout);
		mainLayout.setOnClickListener(this);
		imageLayout = (RelativeLayout) v.findViewById(R.id.my_account_image_layout);
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageLayout.getLayoutParams();
//		params.setMargins(0, -getStatusBarHeight(), 0, 0);
		params.setMargins(0, 0, 0, 0);
		imageLayout.setLayoutParams(params);
		
		optionsBackLayout = (RelativeLayout) v.findViewById(R.id.my_account_toolbar_back_options_layout);
		params = (RelativeLayout.LayoutParams) optionsBackLayout.getLayoutParams();
//		params.setMargins(0, getStatusBarHeight(), 0, Util.px2dp(100, outMetrics));
		params.setMargins(0, 0, 0, Util.px2dp(100, outMetrics));
		optionsBackLayout.setLayoutParams(params);
		
		toolbarBack = (ImageView) v.findViewById(R.id.my_account_toolbar_back);
		params = (RelativeLayout.LayoutParams) toolbarBack.getLayoutParams();
		int leftMarginBack = getResources().getDimensionPixelSize(R.dimen.left_margin_back_arrow);
		params.setMargins(leftMarginBack, 0, 0, 0);
		toolbarBack.setLayoutParams(params);
		toolbarBack.setOnClickListener(this);
		
		toolbarOverflow = (ImageView) v.findViewById(R.id.my_account_toolbar_overflow);
		params = (RelativeLayout.LayoutParams) toolbarOverflow.getLayoutParams();
		params.setMargins(0, 0, leftMarginBack, 0);
		toolbarOverflow.setLayoutParams(params);
		toolbarOverflow.setOnClickListener(this);
		
		overflowMenuLayout = (RelativeLayout) v.findViewById(R.id.my_account_overflow_menu_layout);
		params = (RelativeLayout.LayoutParams) overflowMenuLayout.getLayoutParams();
//		params.setMargins(0, getStatusBarHeight() + Util.px2dp(5, outMetrics), Util.px2dp(5, outMetrics), 0);
		params.setMargins(0, 0, 0, 0);
		overflowMenuLayout.setLayoutParams(params);
		overflowMenuList = (ListView) v.findViewById(R.id.my_account_overflow_menu_list);
		overflowMenuLayout.setVisibility(View.GONE);
		
		createOverflowMenu(overflowMenuList);
		overflowMenuList.setOnItemClickListener(this);
		
		myAccountImage = (ImageView) v.findViewById(R.id.my_account_toolbar_image);
		initialLetter = (TextView) v.findViewById(R.id.my_account_toolbar_initial_letter);
		
		nameView = (TextView) v.findViewById(R.id.my_account_name);
		nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		nameView.setSingleLine();
		nameView.setTypeface(null, Typeface.BOLD);	
		
		nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
		
		nameIcon = (ImageView) v.findViewById(R.id.my_account_name_icon);
		RelativeLayout.LayoutParams lpPL = new RelativeLayout.LayoutParams(nameIcon .getLayoutParams());
		lpPL.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
		nameIcon.setLayoutParams(lpPL);
		nameIcon.setVisibility(View.INVISIBLE);
		
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
		usedSpaceLayout = (LinearLayout) v.findViewById(R.id.my_account_used_space_layout);
		lastSessionLayout = (LinearLayout) v.findViewById(R.id.my_account_last_session_layout);
		connectionsLayout = (LinearLayout) v.findViewById(R.id.my_account_connections_layout);
		
		typeLayout.setVisibility(View.GONE);
		expirationLayout.setVisibility(View.GONE);
		usedSpaceLayout.setVisibility(View.GONE);
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
		
		
		Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels,outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(Color.TRANSPARENT);
		c.drawPaint(p);
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
		p.setColor(context.getResources().getColor(R.color.color_default_avatar_mega));
		
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
	
	@SuppressLint("NewApi")
	private void createOverflowMenu(ListView list){
		ArrayList<String> menuOptions = new ArrayList<String>();
		
		menuOptions.add(getString(R.string.action_kill_all_sessions));
		menuOptions.add(getString(R.string.my_account_change_password));
		
		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
		log("Export in: "+path);
		File file= new File(path);
		if(file.exists()){
			menuOptions.add(getString(R.string.action_remove_master_key));
		}
		else{
			menuOptions.add(getString(R.string.action_export_master_key)); 
		}
		
		menuOptions.add(getString(R.string.action_help));
		menuOptions.add(getString(R.string.action_upgrade_account));
		menuOptions.add(getString(R.string.action_logout));
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, menuOptions);
		if (list.getAdapter() != null){
			ArrayAdapter<String> ad = (ArrayAdapter<String>) list.getAdapter();
			ad.clear();
			ad.addAll(menuOptions);
			ad.notifyDataSetChanged();
		}
		else{
			list.setAdapter(arrayAdapter);
		}
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
			case R.id.my_account_main_layout:{
				if (overflowMenuLayout != null){
					if (overflowMenuLayout.getVisibility() == View.VISIBLE){
						overflowMenuLayout.setVisibility(View.GONE);
						return;
					}
				}
				break;
			}
			case R.id.my_account_toolbar_back:{
				((ManagerActivityLollipop)context).showCloudDrive();
				break;
			}
			case R.id.my_account_toolbar_overflow:{
				overflowMenuLayout.setVisibility(View.VISIBLE);
				break;
			}
			case R.id.my_account_logout:{
				ManagerActivityLollipop.logout(context, megaApi, false);
				break;
			}
			case R.id.my_account_account_type_button:{
				
//				if (myEmail.compareTo("android102@yopmail.com") == 0){
//					((ManagerActivityLollipop)context).paySubs();
//				}				
				
				((ManagerActivityLollipop)context).showUpAF(null);
				
//				((ManagerActivityLollipop)context).launchPayment(ManagerActivityLollipop.SKU_PRO_LITE_MONTH);
				
//				Toast.makeText(context, "EOEOEOE", Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
	
	public int onBackPressed(){
		if (overflowMenuLayout != null){
			if (overflowMenuLayout.getVisibility() == View.VISIBLE){
				overflowMenuLayout.setVisibility(View.GONE);
				return 1;
			}
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
								initialLetter.setText(firstLetter);
								initialLetter.setTextSize(100);
								initialLetter.setTextColor(Color.WHITE);
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
			    
		        usedSpaceLayout.setVisibility(View.VISIBLE);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_KILL_SESSION){
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(mainLayout, getString(R.string.success_kill_all_sessions), Snackbar.LENGTH_LONG).show();
			}
			else
			{
				log("error when killing sessions: "+e.getErrorString());
				Snackbar.make(mainLayout, getString(R.string.error_kill_all_sessions), Snackbar.LENGTH_LONG).show();
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
	
	@SuppressLint("NewApi") 
	void showAlert(String message, String title) {
		AlertDialog.Builder bld;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {	
			bld = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
		}
		else{
			bld = new AlertDialog.Builder(context);
		}
        bld.setMessage(message);
        bld.setTitle(title);
//        bld.setNeutralButton("OK", null);
        bld.setPositiveButton("OK",null);
        log("Showing alert dialog: " + message);
        bld.create().show();
    }

	@Override
	@SuppressLint("NewApi")
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		overflowMenuLayout.setVisibility(View.GONE);
		String itemText = (String) parent.getItemAtPosition(position);
		if (itemText.compareTo(getString(R.string.action_kill_all_sessions)) == 0){
			megaApi.killSession(-1, this);
		}
		else if (itemText.compareTo(getString(R.string.my_account_change_password)) == 0){
			Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
			startActivity(intent);
		}
		else if (itemText.compareTo(getString(R.string.action_export_master_key)) == 0){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (!hasStoragePermission) {
					ActivityCompat.requestPermissions((ManagerActivityLollipop)context,
			                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
			                ManagerActivityLollipop.REQUEST_WRITE_STORAGE);
				}
			}
			
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:
			        	String key = megaApi.exportMasterKey();
						
						BufferedWriter out;         
						try {						

							final String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
							final File f = new File(path);
							log("Export in: "+path);
							FileWriter fileWriter= new FileWriter(path);	
							out = new BufferedWriter(fileWriter);	
							out.write(key);	
							out.close(); 								
							String message = getString(R.string.toast_master_key) + " " + path;
//			    			Snackbar.make(fragmentContainer, toastMessage, Snackbar.LENGTH_LONG).show();

			    			showAlert(message, "MasterKey exported!");
							/*removeMasterKeyMenuItem.setVisible(true);
				        	exportMasterKeyMenuItem.setVisible(false);*/

						}catch (FileNotFoundException e) {
						 e.printStackTrace();
						}catch (IOException e) {
						 e.printStackTrace();
						}
						
						if (overflowMenuList != null){
							createOverflowMenu(overflowMenuList);
						}
			        	
			            break;

			        case DialogInterface.BUTTON_NEGATIVE:
			            //No button clicked
			            break;
			        }
			    }
			};
			
			AlertDialog.Builder builder;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {	
				builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
			}
			else{
				builder = new AlertDialog.Builder(context);
			}
			builder.setTitle(getString(R.string.confirmation_alert));
			builder.setMessage(R.string.export_key_confirmation).setPositiveButton(R.string.general_export, dialogClickListener)
			    .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
		else if (itemText.compareTo(getString(R.string.action_remove_master_key)) == 0){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (!hasStoragePermission) {
					ActivityCompat.requestPermissions((ManagerActivityLollipop)context,
			                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
			                ManagerActivityLollipop.REQUEST_WRITE_STORAGE);
				}
			}
			
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:

						final String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
						final File f = new File(path);
			        	f.delete();	
			        	/*removeMasterKeyMenuItem.setVisible(false);
			        	exportMasterKeyMenuItem.setVisible(true);*/
			        	String message = getString(R.string.toast_master_key_removed);
			        	showAlert(message, "MasterKey removed!");
			        	
			        	createOverflowMenu(overflowMenuList);
			        	
			            break;

			        case DialogInterface.BUTTON_NEGATIVE:
			            //No button clicked
			            break;
			        }
			    }
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
			builder.setTitle(getString(R.string.confirmation_alert));
			builder.setMessage(R.string.remove_key_confirmation).setPositiveButton(R.string.general_remove, dialogClickListener)
			    .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
		else if (itemText.compareTo(getString(R.string.action_help)) == 0){
			Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://mega.co.nz/#help/android"));
            startActivity(intent);
		}
		else if (itemText.compareTo(getString(R.string.action_upgrade_account)) == 0){
			((ManagerActivityLollipop)context).showUpAF(null);
		}
		else if (itemText.compareTo(getString(R.string.action_logout)) == 0){
			ManagerActivityLollipop.logout(context, megaApi, false);
		}
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
