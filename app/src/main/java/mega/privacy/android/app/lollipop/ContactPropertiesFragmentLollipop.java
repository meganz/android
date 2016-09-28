package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nirhart.parallaxscroll.views.ParallaxScrollView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContact;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;


public class ContactPropertiesFragmentLollipop extends Fragment implements OnClickListener, MegaRequestListenerInterface, OnItemClickListener {

	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 250; //in pixels
	
	ParallaxScrollView sV;
	RelativeLayout mainLayout;
	RelativeLayout imageLayout;
	RelativeLayout optionsBackLayout;
	RelativeLayout colorAvatar;
	ImageView toolbarBack;
	ImageView toolbarOverflow;
	RelativeLayout overflowMenuLayout;
	ListView overflowMenuList;
	
	RelativeLayout mailLayout;
	TextView initialLetter;
	ImageView contactPropertiesImage;
	
	DisplayMetrics outMetrics;
	ImageView sharedIcon;
	ImageView mailIcon;
	
	TextView nameView;
	ImageView nameIcon;
	TextView infoEmail;
	View separator;
	RelativeLayout sharedLayout;
	TextView sharedFoldersButton;	
	TextView sharedFoldersLabel;
	String userEmail;

	Context context;
	ActionBar aB;
	DatabaseHandler dbH;

	//	private ListView overflowMenuList;
	private boolean overflowVisible = false; 
	String firstNameText;
	String lastNameText;
	
	MegaApiAndroid megaApi;
	MegaUser contact;

	float density;

	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		dbH = DatabaseHandler.getDbHandler(context);
		super.onCreate(savedInstanceState);
		log("onCreate");
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
		log("onCreateView");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
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

		if (userEmail != null){
			log("userMail is NOT null");
			v = inflater.inflate(R.layout.fragment_contact_properties, container, false);
			
			contact = megaApi.getContact(userEmail);
			if(contact == null)
			{
				return null;
			}
			sV = (ParallaxScrollView) v.findViewById(R.id.contact_properties_scroll_view);
			sV.post(new Runnable() { 
		        public void run() { 
		             sV.scrollTo(0, outMetrics.heightPixels/3);
		        } 
			});
			
			mainLayout = (RelativeLayout) v.findViewById(R.id.contact_properties_main_layout);
			mainLayout.setOnClickListener(this);
			imageLayout = (RelativeLayout) v.findViewById(R.id.contact_properties_image_layout);
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageLayout.getLayoutParams();
			params.setMargins(0, -getStatusBarHeight(), 0, 0);
			imageLayout.setLayoutParams(params);
			
			optionsBackLayout = (RelativeLayout) v.findViewById(R.id.contact_properties_toolbar_back_options_layout);
			params = (RelativeLayout.LayoutParams) optionsBackLayout.getLayoutParams();

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
				params.setMargins(0,  0, 0, Util.scaleHeightPx(100, outMetrics));
			}
			else{
				params.setMargins(0, getStatusBarHeight(), 0, Util.scaleHeightPx(100, outMetrics));
			}
			optionsBackLayout.setLayoutParams(params);
			
			toolbarBack = (ImageView) v.findViewById(R.id.contact_properties_toolbar_back);
			params = (RelativeLayout.LayoutParams) toolbarBack.getLayoutParams();
//			int leftMarginBack = getResources().getDimensionPixelSize(R.dimen.left_margin_back_arrow);
			int leftMarginBack = Util.scaleWidthPx(3, outMetrics);
			params.setMargins(leftMarginBack, 0, 0, 0);
			toolbarBack.setLayoutParams(params);
			toolbarBack.setOnClickListener(this);
			
			toolbarOverflow = (ImageView) v.findViewById(R.id.contact_properties_toolbar_overflow);
			params = (RelativeLayout.LayoutParams) toolbarOverflow.getLayoutParams();
			params.setMargins(0, 0, leftMarginBack, 0);
			toolbarOverflow.setLayoutParams(params);
			toolbarOverflow.setOnClickListener(this);
			
			overflowMenuLayout = (RelativeLayout) v.findViewById(R.id.contact_properties_overflow_menu_layout);
			params = (RelativeLayout.LayoutParams) overflowMenuLayout.getLayoutParams();
			params.setMargins(0, getStatusBarHeight() + Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(5, outMetrics), 0);
			overflowMenuLayout.setLayoutParams(params);
			overflowMenuList = (ListView) v.findViewById(R.id.contact_properties_overflow_menu_list);
			overflowMenuLayout.setVisibility(View.GONE);
			
			createOverflowMenu(overflowMenuList);
			overflowMenuList.setOnItemClickListener(this);

			colorAvatar = (RelativeLayout) v.findViewById(R.id.color_avatar_layout);
			contactPropertiesImage = (ImageView) v.findViewById(R.id.contact_properties_toolbar_image);
			initialLetter = (TextView) v.findViewById(R.id.contact_properties_toolbar_initial_letter);
			
			nameView = (TextView) v.findViewById(R.id.contact_properties_name);
			nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			nameView.setSingleLine();
			nameView.setTypeface(null, Typeface.BOLD);	
			
			nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
			
			nameIcon = (ImageView) v.findViewById(R.id.contact_properties_name_icon);
			RelativeLayout.LayoutParams lpPL = new RelativeLayout.LayoutParams(nameIcon .getLayoutParams());
			lpPL.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
			nameIcon.setLayoutParams(lpPL);
			nameIcon.setVisibility(View.INVISIBLE);
			
			//Mail Layout
			mailLayout = (RelativeLayout) v.findViewById(R.id.contact_properties_email_layout);
//			RelativeLayout.LayoutParams lpML = new RelativeLayout.LayoutParams(mailLayout.getLayoutParams());
//			lpML.setMargins(0, Util.scaleHeightPx(10, outMetrics), 0, 0);
//			mailLayout.setLayoutParams(lpML);				
			
			mailIcon = (ImageView) v.findViewById(R.id.contact_properties_email_icon);
			RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(mailIcon.getLayoutParams());
			lp1.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
			mailIcon.setLayoutParams(lp1);	
			
			infoEmail = (TextView) v.findViewById(R.id.contact_properties_email);
			
			separator = (View) v.findViewById(R.id.divider_shared_layout);
			RelativeLayout.LayoutParams paramsDivider = (RelativeLayout.LayoutParams) separator.getLayoutParams();
			paramsDivider.leftMargin = Util.scaleWidthPx(55, outMetrics);
			separator.setLayoutParams(paramsDivider);

			infoEmail.setText(userEmail);		

			//Shared Layout
			sharedLayout = (RelativeLayout) v.findViewById(R.id.contact_properties_shared_folders_layout);
			sharedLayout.setOnClickListener(this);
			sharedIcon = (ImageView) v.findViewById(R.id.contact_properties_shared_folder_icon);
			RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(sharedIcon.getLayoutParams());
			lp2.setMargins(Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(3, outMetrics), Util.scaleHeightPx(3, outMetrics));
			sharedIcon.setLayoutParams(lp2);
			
			sharedFoldersLabel = (TextView) v.findViewById(R.id.contact_properties_shared_folders_label);
			sharedFoldersButton = (TextView) v.findViewById(R.id.contact_properties_shared_folders_button);
			
			RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) sharedFoldersButton.getLayoutParams();
			params1.rightMargin = Util.scaleWidthPx(10, outMetrics);
			sharedFoldersButton.setLayoutParams(params1);
			
//			sharedFoldersButton.setOnClickListener(this);
			sharedFoldersButton.setText(getDescription(megaApi.getInShares(contact)));

			MegaContact contactDB = dbH.findContactByHandle(String.valueOf(contact.getHandle()));
			if(contactDB!=null){

				firstNameText = contactDB.getName();
				lastNameText = contactDB.getLastName();

				String fullName;

				if (firstNameText.trim().length() <= 0){
					fullName = lastNameText;
				}
				else{
					fullName = firstNameText + " " + lastNameText;
				}

				if (fullName.trim().length() <= 0){
					log("Put email as fullname");
					String email = contact.getEmail();
					String[] splitEmail = email.split("[@._]");
					fullName = splitEmail[0];
				}

				nameView.setText(fullName);
			}
			else{
				log("The contactDB is null: ");
			}

			setDefaultAvatar();

			setAvatar();
		}

		return v;
	}

	public void setAvatar(){
		log("setAvatar");
		File avatar = null;
		if (context.getExternalCacheDir() != null){
			avatar = new File(context.getExternalCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
		}
		else{
			avatar = new File(context.getCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
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
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail(), this);
					}
					else{
						megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail(), this);
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

		String color = megaApi.getUserAvatarColor(contact);
		if(color!=null){
			log("The color to set the avatar is "+color);
			colorAvatar.setBackgroundColor(Color.parseColor(color));
		}
		else{
			log("Default color to the avatar");
			colorAvatar.setBackgroundColor(context.getResources().getColor(R.color.lollipop_primary_color));
		}

		contactPropertiesImage.setImageBitmap(defaultAvatar);

		int avatarTextSize = getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);

		String fullName;
		boolean setInitialByMail = false;

		if (firstNameText.trim().length() <= 0){
			fullName = lastNameText;
		}
		else{
			fullName = firstNameText + " " + lastNameText;
		}

		if (fullName.trim().length() <= 0){
			log("Put email as fullname");
			String email = userEmail;
			String[] splitEmail = email.split("[@._]");
			fullName = splitEmail[0];
		}

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
	
	@SuppressLint("NewApi")
	private void createOverflowMenu(ListView list){
		ArrayList<String> menuOptions = new ArrayList<String>();
		
		menuOptions.add(getString(R.string.context_share_folder));
		menuOptions.add(getString(R.string.context_view_shared_folders));
		
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

	public void setUserEmail(String userEmail){
		this.userEmail = userEmail;
	}
	
	public String getUserEmail(){
		return this.userEmail;
	}

	@Override
	public void onAttach(Activity activity) {
		log("deprecated onAttach");
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
		if(aB!=null){
			log("aB hide attach activity");
			aB.hide();
		}
	}

	@Override
	public void onAttach(Context context) {
		log("onAttach");
		super.onAttach(context);
		this.context = context;
		aB = ((AppCompatActivity)context).getSupportActionBar();
		if(aB!=null){
			log("aB hide attach context");
			aB.hide();
		}
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.contact_properties_main_layout:{
				if (overflowMenuLayout != null){
					if (overflowMenuLayout.getVisibility() == View.VISIBLE){
						overflowMenuLayout.setVisibility(View.GONE);
						return;
					}
				}
				break;
			}
			case R.id.contact_properties_toolbar_back:{
				((ContactPropertiesActivityLollipop)context).finish();
				break;
			}
			case R.id.contact_properties_toolbar_overflow:{
				overflowMenuLayout.setVisibility(View.VISIBLE);
				break;
			}
			case R.id.contact_properties_shared_folders_layout:{
				((ContactPropertiesActivityLollipop)context).onContentClick(userEmail);
				break;
			}
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

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart()");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: "+request.getType());
		if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){

			log("MegaRequest.TYPE_GET_ATTR_USER");
			if (e.getErrorCode() == MegaError.API_OK){
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
							contactPropertiesImage.setImageBitmap(imBitmap);
							initialLetter.setVisibility(View.GONE);
						}
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_USER_DATA) {
			if (e.getErrorCode() == MegaError.API_OK) {
				log("MegaRequest.TYPE_GET_USER_DATA: "+request.getName());
				log("ParamType: "+request.getParamType());
//				if (aB != null){
//					aB.setTitle(request.getName());
//				}
//				userNameTextView.setText(request.getName());
			}
		}
		else{
			log("Otro finish");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}

	public static void log(String log) {
		Util.log("ContactPropertiesFragmentLollipop", log);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		overflowMenuLayout.setVisibility(View.GONE);
		String itemText = (String) parent.getItemAtPosition(position);
		if (itemText.compareTo(getString(R.string.context_share_folder)) == 0){
			((ContactPropertiesActivityLollipop)context).pickFolderToShare(userEmail);
		}
		else if (itemText.compareTo(getString(R.string.context_view_shared_folders)) == 0){
			((ContactPropertiesActivityLollipop)context).onContentClick(userEmail);
		}
	}
}
