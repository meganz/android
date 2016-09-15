package mega.privacy.android.app.lollipop.megachat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;


@SuppressLint("NewApi")
public class ContactChatInfoActivityLollipop extends PinActivityLollipop implements OnClickListener, MegaRequestListenerInterface, OnCheckedChangeListener, OnItemClickListener{

	RelativeLayout imageLayout;

//	RelativeLayout overflowMenuLayout;
//	RelativeLayout colorAvatar;
	CollapsingToolbarLayout collapsingToolbar;
	TextView initialLetter;
	ImageView contactPropertiesImage;
	LinearLayout optionsLayout;
	TextView nameView;
	LinearLayout availableOfflineLayout;

	RelativeLayout sizeLayout;
	RelativeLayout contentLayout;
	RelativeLayout addedLayout;
	RelativeLayout modifiedLayout;
	RelativeLayout mailLayout;
	ImageView mailIcon;
	ImageView shareIcon;
	ImageView infoIcon;
	ImageView contentIcon;
	ImageView addedIcon;
	ImageView modifiedIcon;
	RelativeLayout sharedLayout;
	TextView usersSharedWithText;
	View dividerSharedLayout;

	TextView availableOfflineView;
	TextView userEmailTextView;

	ImageView publicLinkIcon;
	Toolbar toolbar;
	ActionBar aB;

	//	ImageView publicLinkImage;
	Switch offlineSwitch;

	TextView sizeTextView;
	TextView sizeTitleTextView;

	TextView contentTextView;
	TextView contentTitleTextView;

	TextView addedTextView;
	TextView modifiedTextView;

	RelativeLayout permissionsLayout;
	TextView permissionLabel;
	TextView permissionInfo;
	ImageView permissionsIcon;

	RelativeLayout ownerLayout;
	TextView ownerLabel;
	TextView ownerInfo;
	ImageView ownerIcon;

	MegaUser user;
	String userEmail;
	String fullName;

	private MegaApiAndroid megaApi = null;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	ProgressDialog statusDialog;

	private static int EDIT_TEXT_ID = 1;
	private Handler handler;

	Display display;
	DisplayMetrics outMetrics;
	float density;
	float scaleW;
	float scaleH;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	AlertDialog permissionsDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		log("onCreate");
		if (megaApi == null){
			MegaApplication app = (MegaApplication)getApplication();
			megaApi = app.getMegaApi();
		}

		handler = new Handler();

//		dbH = new DatabaseHandler(getApplicationContext());
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;

	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);

		Bundle extras = getIntent().getExtras();
		if (extras != null){
			userEmail = extras.getString("userEmail");
			if(userEmail==null){
				log("userMail is NULL");
				finish();
				return;
			}
			fullName = extras.getString("userFullName");

			user = megaApi.getContact(userEmail);
			if(user==null){
				log("MegaUser is NULL");
				finish();
				return;
			}

			setContentView(R.layout.activity_chat_contact_properties);
			toolbar = (Toolbar) findViewById(R.id.toolbar);
			setSupportActionBar(toolbar);
			aB = getSupportActionBar();
			imageLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_image_layout);
			collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapse_toolbar);
			collapsingToolbar.setTitle(fullName);
			getSupportActionBar().setDisplayShowTitleEnabled(false);

			collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));
			collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.white));

			aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
			aB.setHomeButtonEnabled(true);
			aB.setDisplayHomeAsUpEnabled(true);

			contactPropertiesImage = (ImageView) findViewById(R.id.chat_contact_properties_toolbar_image);
			initialLetter = (TextView) findViewById(R.id.chat_contact_properties_toolbar_initial_letter);

			float scaleText;
			if (scaleH < scaleW){
				scaleText = scaleH;
			}
			else{
				scaleText = scaleW;
			}

			setDefaultAvatar();

			setAvatar();

			//Available Offline Layout

			availableOfflineLayout = (LinearLayout) findViewById(R.id.available_offline_layout);
			availableOfflineLayout.setVisibility(View.VISIBLE);

			availableOfflineView = (TextView) findViewById(R.id.chat_contact_properties_available_offline_text);
			LinearLayout.LayoutParams params4 = (LinearLayout.LayoutParams) availableOfflineView.getLayoutParams();
			params4.leftMargin = Util.scaleWidthPx(55, outMetrics);
			params4.topMargin = Util.scaleHeightPx(15, outMetrics);
			params4.bottomMargin = Util.scaleHeightPx(15, outMetrics);
			availableOfflineView.setLayoutParams(params4);

			offlineSwitch = (Switch) findViewById(R.id.chat_contact_properties_switch);
			offlineSwitch.setOnCheckedChangeListener(this);

			//Mail Layout
			userEmailTextView = (TextView) findViewById(R.id.chat_contact_properties_email);
			userEmailTextView.setText(userEmail);
			mailLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_email_layout);
//			RelativeLayout.LayoutParams lpML = new RelativeLayout.LayoutParams(mailLayout.getLayoutParams());
//			lpML.setMargins(0, Util.scaleHeightPx(10, outMetrics), 0, 0);
//			mailLayout.setLayoutParams(lpML);

			mailIcon = (ImageView) findViewById(R.id.chat_contact_properties_email_icon);
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(mailIcon.getLayoutParams());
			lp.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
			mailIcon.setLayoutParams(lp);

			//Share with Layout

			sharedLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_shared_layout);
			sharedLayout.setOnClickListener(this);

			shareIcon = (ImageView) findViewById(R.id.chat_contact_properties_shared_image);
			RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(shareIcon.getLayoutParams());
			lp1.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
			shareIcon.setLayoutParams(lp1);

			usersSharedWithText = (TextView) findViewById(R.id.chat_contact_properties_shared_info);
			RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) usersSharedWithText.getLayoutParams();
			params1.rightMargin = Util.scaleWidthPx(10, outMetrics);
			usersSharedWithText.setLayoutParams(params1);

			dividerSharedLayout = (View) findViewById(R.id.divider_shared_layout);
			LinearLayout.LayoutParams paramsDivider = (LinearLayout.LayoutParams) dividerSharedLayout.getLayoutParams();
			paramsDivider.leftMargin = Util.scaleWidthPx(55, outMetrics);
			dividerSharedLayout.setLayoutParams(paramsDivider);

			//OPTIONS LAYOUT
			optionsLayout = (LinearLayout) findViewById(R.id.chat_contact_properties_options);

			//Permissions Layout
			permissionsLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_permissions_layout);
			permissionsLayout.setVisibility(View.GONE);

		    permissionsIcon = (ImageView) findViewById(R.id.chat_contact_properties_permissions_image);
		    RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(permissionsIcon.getLayoutParams());
			lp3.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
			permissionsIcon.setLayoutParams(lp3);

			permissionLabel = (TextView) findViewById(R.id.chat_contact_properties_permission_label);
			permissionInfo = (TextView) findViewById(R.id.chat_contact_properties_permission_info);

			//Owner Layout
			ownerLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_owner_layout);

		    ownerIcon = (ImageView) findViewById(R.id.chat_contact_properties_owner_image);
		    RelativeLayout.LayoutParams lp4 = new RelativeLayout.LayoutParams(ownerIcon.getLayoutParams());
			lp4.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
			ownerIcon.setLayoutParams(lp4);

			ownerLabel =  (TextView) findViewById(R.id.chat_contact_properties_owner_label);
			ownerInfo = (TextView) findViewById(R.id.chat_contact_properties_owner_info);
			ownerLayout.setVisibility(View.GONE);

			//Info Layout

		    infoIcon = (ImageView) findViewById(R.id.chat_contact_properties_size_image);
		    RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(infoIcon.getLayoutParams());
			lp2.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
			infoIcon.setLayoutParams(lp2);

			sizeLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_size_layout);
			sizeTitleTextView  = (TextView) findViewById(R.id.chat_contact_properties_info_menu_size);
			sizeTextView = (TextView) findViewById(R.id.chat_contact_properties_info_data_size);
			RelativeLayout.LayoutParams params5 = (RelativeLayout.LayoutParams) sizeTextView.getLayoutParams();
			params5.rightMargin = Util.scaleWidthPx(10, outMetrics);
			sizeTextView.setLayoutParams(params5);

			//Content Layout
			contentLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_content_layout);

			contentIcon = (ImageView) findViewById(R.id.chat_contact_properties_content_image);
		    RelativeLayout.LayoutParams lpContent = new RelativeLayout.LayoutParams(contentIcon.getLayoutParams());
		    lpContent.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
			contentIcon.setLayoutParams(lpContent);

			contentTitleTextView  = (TextView) findViewById(R.id.chat_contact_properties_info_menu_content);
			contentTextView = (TextView) findViewById(R.id.chat_contact_properties_info_data_content);

			//Added Layout

			addedLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_added_layout);

			addedIcon = (ImageView) findViewById(R.id.chat_contact_properties_added_image);
		    RelativeLayout.LayoutParams lpAdded = new RelativeLayout.LayoutParams(addedIcon.getLayoutParams());
		    lpAdded.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
		    addedIcon.setLayoutParams(lpAdded);
		    addedTextView = (TextView) findViewById(R.id.chat_contact_properties_info_data_added);

		    //Modified Layout
		    modifiedLayout = (RelativeLayout) findViewById(R.id.chat_contact_properties_modified_layout);

		    modifiedIcon = (ImageView) findViewById(R.id.chat_contact_properties_modified_image);
		    RelativeLayout.LayoutParams lpModified = new RelativeLayout.LayoutParams(modifiedIcon.getLayoutParams());
		    lpModified.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
		    modifiedIcon.setLayoutParams(lpModified);
			modifiedTextView = (TextView) findViewById(R.id.chat_contact_properties_info_data_modified);

		}
		else{
			log("Extras is NULL");
		}

	}

	public void setAvatar(){
		log("setAvatar");
		File avatar = null;
		if (getExternalCacheDir() != null){
			avatar = new File(getExternalCacheDir().getAbsolutePath(), userEmail + ".jpg");
		}
		else{
			avatar = new File(getCacheDir().getAbsolutePath(), userEmail + ".jpg");
		}

		if(avatar!=null){
			setProfileAvatar(avatar);
		}
	}

	public void setProfileAvatar(File avatar){
		log("setProfileAvatar");
		Bitmap imBitmap = null;
		if (avatar.exists()){
			if (avatar.length() > 0){
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (imBitmap == null) {
					avatar.delete();
					if (getExternalCacheDir() != null){
						megaApi.getUserAvatar(user, getExternalCacheDir().getAbsolutePath() + "/" + userEmail, this);
					}
					else{
						megaApi.getUserAvatar(user, getCacheDir().getAbsolutePath() + "/" + userEmail, this);
					}
				}
				else{
					contactPropertiesImage.setImageBitmap(imBitmap);
					initialLetter.setVisibility(View.GONE);
				}
			}
		}
	}

	public void setDefaultAvatar(){
		log("setDefaultAvatar");

		Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels,outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(Color.TRANSPARENT);
		c.drawPaint(p);

		String color = megaApi.getUserAvatarColor(user);
		if(color!=null){
			log("The color to set the avatar is "+color);
			imageLayout.setBackgroundColor(Color.parseColor(color));
		}
		else{
			log("Default color to the avatar");
			imageLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
		}

		contactPropertiesImage.setImageBitmap(defaultAvatar);

		int avatarTextSize = getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);

		boolean setInitialByMail = false;

		if (fullName != null){
			if (fullName.trim().length() > 0){
				String firstLetter = fullName.charAt(0) + "";
				firstLetter = firstLetter.toUpperCase(Locale.getDefault());
				initialLetter.setText(firstLetter);
				initialLetter.setTextSize(100);
				initialLetter.setTextColor(Color.WHITE);
				initialLetter.setVisibility(View.VISIBLE);
			}else{
				setInitialByMail=true;
			}
		}
		else{
			setInitialByMail=true;
		}
		if(setInitialByMail){
			if (userEmail != null){
				if (userEmail.length() > 0){
					log("email TEXT: " + userEmail);
					log("email TEXT AT 0: " + userEmail.charAt(0));
					String firstLetter = userEmail.charAt(0) + "";
					firstLetter = firstLetter.toUpperCase(Locale.getDefault());
					initialLetter.setText(firstLetter);
					initialLetter.setTextSize(100);
					initialLetter.setTextColor(Color.WHITE);
					initialLetter.setVisibility(View.VISIBLE);
				}
			}
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

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
//			case R.id.chat_contact_properties_main_layout:{
//				if (overflowMenuLayout != null){
//					if (overflowMenuLayout.getVisibility() == View.VISIBLE){
//						overflowMenuLayout.setVisibility(View.GONE);
//						return;
//					}
//				}
//				break;
//			}
			case R.id.chat_contact_properties_shared_layout:{
//				Intent i = new Intent(this, FileContactListActivityLollipop.class);
//				i.putExtra("name", node.getHandle());
//				startActivity(i);
				break;
			}
//			case R.id.chat_contact_properties_toolbar_back:{
//				finish();
//				break;
//			}
//			case R.id.chat_contact_properties_toolbar_overflow:{
//				overflowMenuLayout.setVisibility(View.VISIBLE);
//				break;
//			}
//			case R.id.chat_contact_properties_toolbar_download:{
//
//				break;
//			}
//			case R.id.chat_contact_properties_toolbar_rubbish_bin:{
//
//				break;
//			}
//			case R.id.file_properties_content_table:{
//				Intent i = new Intent(this, FileContactListActivityLollipop.class);
//				i.putExtra("name", node.getHandle());
//				startActivity(i);
//				finish();
//				break;
//			}
//
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelectedLollipop");
		int id = item.getItemId();
		switch(id) {
			case android.R.id.home: {
				finish();
			}
		}
		return true;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		log("onCheckedChanged");

		if (!isChecked){
			log("isChecked");

			supportInvalidateOptionsMenu();
		}
		else{
			log("NOT Checked");


			supportInvalidateOptionsMenu();
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
	public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {

		log("onRequestFinish: "+request.getType() + "__" + request.getRequestString());

		if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){

			log("MegaRequest.TYPE_GET_ATTR_USER");
			if (e.getErrorCode() == MegaError.API_OK){
				File avatar = null;
				if (getExternalCacheDir() != null){
					avatar = new File(getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				}
				else{
					avatar = new File(getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
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
							contactPropertiesImage.setImageBitmap(imBitmap);
							initialLetter.setVisibility(View.GONE);
						}
					}
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}

	@Override
	protected void onDestroy(){
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
}
