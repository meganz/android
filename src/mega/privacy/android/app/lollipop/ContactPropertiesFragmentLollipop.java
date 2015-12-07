package mega.privacy.android.app.lollipop;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import com.nirhart.parallaxscroll.views.ParallaxScrollView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.utils.FixedCenterCrop;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;


public class ContactPropertiesFragmentLollipop extends Fragment implements OnClickListener, MegaRequestListenerInterface, OnItemClickListener {

	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 250; //in pixels
	
	ParallaxScrollView sV;
	RelativeLayout mainLayout;
	RelativeLayout imageLayout;
	RelativeLayout optionsBackLayout;
	ImageView toolbarBack;
	ImageView toolbarOverflow;
	RelativeLayout overflowMenuLayout;
	ListView overflowMenuList;
	
	TextView initialLetter;
	ImageView contactPropertiesImage;
	
	DisplayMetrics outMetrics;
	
	
	
	RelativeLayout contentLayout;
	TextView infoEmail;
	TextView sharedFoldersButton;	
	TextView sharedFoldersLabel;
	String userEmail;	
	Context context;
	ActionBar aB;
	
	//	private ListView overflowMenuList;
	private boolean overflowVisible = false; 
	private boolean name = false;
	private boolean firstName = false;
	String nameText;
	String firstNameText;
	
	MegaApiAndroid megaApi;
	MegaUser contact;

	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

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

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = null;

		if (userEmail != null){
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
			params.setMargins(0, getStatusBarHeight(), 0, Util.px2dp(100, outMetrics));
			optionsBackLayout.setLayoutParams(params);
			
			toolbarBack = (ImageView) v.findViewById(R.id.contact_properties_toolbar_back);
			params = (RelativeLayout.LayoutParams) toolbarBack.getLayoutParams();
			int leftMarginBack = getResources().getDimensionPixelSize(R.dimen.left_margin_back_arrow);
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
			params.setMargins(0, getStatusBarHeight() + Util.px2dp(5, outMetrics), Util.px2dp(5, outMetrics), 0);
			overflowMenuLayout.setLayoutParams(params);
			overflowMenuList = (ListView) v.findViewById(R.id.contact_properties_overflow_menu_list);
			overflowMenuLayout.setVisibility(View.GONE);
			
			createOverflowMenu(overflowMenuList);
			overflowMenuList.setOnItemClickListener(this);
			
			contactPropertiesImage = (ImageView) v.findViewById(R.id.contact_properties_toolbar_image);
			initialLetter = (TextView) v.findViewById(R.id.contact_properties_toolbar_initial_letter);
			
			infoEmail = (TextView) v.findViewById(R.id.contact_properties_email);
			
			sharedFoldersLabel = (TextView) v.findViewById(R.id.contact_properties_shared_folders_label);
			sharedFoldersButton = (TextView) v.findViewById(R.id.contact_properties_shared_folders_button);
			sharedFoldersButton.setOnClickListener(this);
			
			infoEmail.setText(userEmail);		
			name=false;
			firstName=false;
			megaApi.getUserAttribute(contact, 1, this);
			megaApi.getUserAttribute(contact, 2, this);

			sharedFoldersButton.setText(getDescription(megaApi.getInShares(contact)));

			Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels,outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(defaultAvatar);
			Paint p = new Paint();
			p.setAntiAlias(true);
			p.setColor(Color.TRANSPARENT);
			c.drawPaint(p);
			contactPropertiesImage.setImageBitmap(defaultAvatar);
			
		    int avatarTextSize = getAvatarTextSize(density);
		    log("DENSITY: " + density + ":::: " + avatarTextSize);
		    if (userEmail != null){
			    if (userEmail.length() > 0){
			    	log("TEXT: " + userEmail);
			    	log("TEXT AT 0: " + userEmail.charAt(0));
			    	String firstLetter = userEmail.charAt(0) + "";
			    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
			    	initialLetter.setText(firstLetter);
			    	initialLetter.setTextSize(100);
			    	initialLetter.setTextColor(Color.WHITE);
			    	initialLetter.setVisibility(View.VISIBLE);
			    }
		    }
		    
			File avatar = null;
			if (context.getExternalCacheDir() != null){
				avatar = new File(context.getExternalCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
			}
			else{
				avatar = new File(context.getCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
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
			//infoAdded.setText(contact.getTimestamp()+"");
		}

		return v;
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
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
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
			case R.id.contact_properties_shared_folders_button:{
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
				if(request.getParamType()==1){
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
					/*if (collapsingToolbarLayout != null){
						collapsingToolbarLayout.setTitle(nameText+" "+firstNameText);
					}*/
					name= false;
					firstName = false;
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
		Util.log("ContactPropertiesFragment", log);
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
