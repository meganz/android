package mega.privacy.android.app.lollipop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.FileStorageActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.PinActivity;
import mega.privacy.android.app.FileStorageActivity.Mode;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


@SuppressLint("NewApi")
public class FilePropertiesActivityLollipop extends PinActivityLollipop implements OnClickListener, MegaRequestListenerInterface, OnCheckedChangeListener, MegaGlobalListenerInterface{
	
	static int TYPE_EXPORT_GET = 0;
	static int TYPE_EXPORT_REMOVE = 1;
	static int TYPE_EXPORT_MANAGE = 2;
	static int FROM_FILE_BROWSER = 13;
	static int FROM_INCOMING_SHARES= 14;
	static int FROM_OFFLINE= 15;

	ImageView imageView;
	CoordinatorLayout container;
	LinearLayout optionsLayout;
	TextView nameView;
	LinearLayout availableOfflineLayout;
	LinearLayout permissionsLayout;
	LinearLayout sizeLayout;
	LinearLayout contentLayout;
	LinearLayout addedLayout;
	LinearLayout modifiedLayout;
	LinearLayout sharedLayout;
	TextView usersSharedWithText;
	View dividerSharedLayout;
	
	TextView availableOfflineView;
	
	ImageView publicLinkImage;
	Switch offlineSwitch;
	
	TextView sizeTextView;
	TextView sizeTitleTextView;
	
	TextView contentTextView;
	TextView contentTitleTextView;
	
	TextView addedTextView;
	TextView modifiedTextView;
	
	TextView permissionLabel;
	TextView permissionInfo;
	
	boolean owner= true;
	Toolbar tB;
	ActionBar aB;
	int typeExport = -1;
	
//	RelativeLayout sharedWith;
//	TableLayout contactTable;	
//	TableLayout sharedLayout;
//	TextView contentDetailedTextView;
//	TextView sharedWithTextView;
//	TextView publicLinkTextView;
	
	ArrayList<MegaShare> sl;
	MegaOffline mOffDelete;
	
	TextView ownerInfo;	

	ArrayList<MegaNode> dTreeList = null;
	
	MegaNode node;
	long handle;
	
	boolean availableOfflineBoolean = false;
	
	private MegaApiAndroid megaApi = null;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	public FilePropertiesActivityLollipop filePropertiesActivity;
	
	ProgressDialog statusDialog;
	boolean publicLink=false;
	
	private static int EDIT_TEXT_ID = 1;
	private Handler handler;
	
	private AlertDialog renameDialog;

	boolean moveToRubbish = false;
	
	public static int REQUEST_CODE_SELECT_CONTACT = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static String DB_FILE = "0";
	public static String DB_FOLDER = "1";
		
	MenuItem downloadMenuItem; 
	MenuItem shareFolderMenuItem;
	MenuItem getLinkMenuItem;
	MenuItem manageLinkMenuItem;
	MenuItem removeLinkMenuItem;
	MenuItem sendLinkMenuItem;
	MenuItem leaveShareMenuItem;
	MenuItem moveMenuItem;
	MenuItem renameMenuItem;
	ImageView statusImageView;
	MenuItem copyMenuItem;
	MenuItem removeMenuItem;
	
	Display display;
	DisplayMetrics outMetrics;
	float density;
	float scaleW;
	float scaleH;
	
	boolean shareIt = true;
	int imageId;
	int from;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
	AlertDialog permissionsDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {			
	
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			MegaApplication app = (MegaApplication)getApplication();
			megaApi = app.getMegaApi();
		}
		
		megaApi.addGlobalListener(this);
		
		filePropertiesActivity = this;
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
			imageId = extras.getInt("imageId");
			from = extras.getInt("from");
			String name = extras.getString("name");
			handle = extras.getLong("handle", -1);
			node = megaApi.getNodeByHandle(handle);
			if (node == null){  
				finish();
				return;
			}
			
			name = node.getName();
					
			setContentView(R.layout.activity_file_properties);
	        container = (CoordinatorLayout) findViewById(R.id.fragment_container_file_properties);
			tB = (Toolbar) findViewById(R.id.file_properties_toolbar);
			setSupportActionBar(tB);
			aB = getSupportActionBar();
//			aB.setHomeButtonEnabled(true);
//			aB.setDisplayShowTitleEnabled(true);
			aB.setDisplayHomeAsUpEnabled(true);
			aB.setDisplayShowHomeEnabled(true);
//			aB.setLogo(R.drawable.ic_arrow_back_black);
			aB.setElevation(0);
			
			CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.file_properties_collapsing_toolbar);
//			collapsingToolbarLayout.setTitle(getString(R.string.file_properties_activity));
			collapsingToolbarLayout.setTitle(name);
//			collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));
//			collapsingToolbarLayout.setContentScrimColor(getResources().getColor(R.color.filePropertiesToolBar));
//			collapsingToolbarLayout.setStatusBarScrimColor(getResources().getColor(R.color.filePropertiesToolBar));
			collapsingToolbarLayout.setExpandedTitleColor(Color.BLACK);
			collapsingToolbarLayout.setCollapsedTitleTextColor(Color.BLACK);			
			collapsingToolbarLayout.setContentScrimColor(Color.WHITE);
//			collapsingToolbarLayout.setStatusBarScrimColor(getResources().getColor(R.color.accentColor));
			collapsingToolbarLayout.setBackgroundColor(Color.WHITE);
			
			
			imageView = (ImageView) findViewById(R.id.file_properties_toolbar_image);
			imageView.setImageResource(imageId);
			nameView = (TextView) findViewById(R.id.file_properties_name);
//			((RelativeLayout.LayoutParams) imageView.getLayoutParams()).setMargins(Util.px2dp((9*scaleW), outMetrics), Util.px2dp((2*scaleH), outMetrics), Util.px2dp((9*scaleW), outMetrics), Util.px2dp((2*scaleH), outMetrics));
//			((RelativeLayout.LayoutParams) imageView.getLayoutParams()).setMargins(0, 0, 0, Util.px2dp((-30*scaleH), outMetrics));
			publicLinkImage = (ImageView) findViewById(R.id.file_properties_image_link);
//			((RelativeLayout.LayoutParams) publicLinkImage.getLayoutParams()).setMargins(Util.px2dp((9*scaleW), outMetrics), Util.px2dp((2*scaleH), outMetrics), Util.px2dp((9*scaleW), outMetrics), Util.px2dp((2*scaleH), outMetrics));
			((RelativeLayout.LayoutParams) publicLinkImage.getLayoutParams()).setMargins(Util.px2dp((-10*scaleH), outMetrics), Util.px2dp((-20*scaleH), outMetrics), 0, 0);
			publicLinkImage.setVisibility(View.GONE);			
			
			optionsLayout = (LinearLayout) findViewById(R.id.file_properties_options);
			
			permissionsLayout = (LinearLayout) findViewById(R.id.file_properties_permissions_layout);
			permissionsLayout.setVisibility(View.GONE);
			sizeLayout = (LinearLayout) findViewById(R.id.file_properties_size_layout);
			contentLayout = (LinearLayout) findViewById(R.id.file_properties_content_layout);
			addedLayout = (LinearLayout) findViewById(R.id.file_properties_added_layout);
			modifiedLayout = (LinearLayout) findViewById(R.id.file_properties_modified_layout);			
			
			availableOfflineLayout = (LinearLayout) findViewById(R.id.available_offline_layout);
			availableOfflineLayout.setVisibility(View.VISIBLE);	
			availableOfflineView = (TextView) findViewById(R.id.file_properties_available_offline_text);
			float scaleText;
			if (scaleH < scaleW){
				scaleText = scaleH;
			}
			else{
				scaleText = scaleW;
			}
			availableOfflineView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (15*scaleText));
			
			offlineSwitch = (Switch) findViewById(R.id.file_properties_switch);
			offlineSwitch.setOnCheckedChangeListener(this);			
			
//			availableSwitchOnline = (MySwitch) findViewById(R.id.file_properties_switch_online);
//			availableSwitchOnline.setChecked(true);
//			availableSwitchOffline = (MySwitch) findViewById(R.id.file_properties_switch_offline);
//			availableSwitchOffline.setChecked(false);
//			availableSwitchOnline.setOnCheckedChangeListener(this);			
//			availableSwitchOffline.setOnCheckedChangeListener(this);	
//			availableSwitchOnline.getLayoutParams().height = Util.px2dp((20 * scaleH), outMetrics);
//			availableSwitchOffline.getLayoutParams().height = Util.px2dp((20 * scaleH), outMetrics);
//			((LinearLayout.LayoutParams) availableSwitchOnline.getLayoutParams()).setMargins(Util.px2dp((5 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), Util.px2dp((5 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics));
//			((LinearLayout.LayoutParams) availableSwitchOffline.getLayoutParams()).setMargins(Util.px2dp((5 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics), Util.px2dp((5 * scaleW), outMetrics),Util.px2dp((5 * scaleH), outMetrics));
	
			sizeTitleTextView  = (TextView) findViewById(R.id.file_properties_info_menu_size);
			sizeTextView = (TextView) findViewById(R.id.file_properties_info_data_size);
			contentTitleTextView  = (TextView) findViewById(R.id.file_properties_info_menu_content);
			contentTextView = (TextView) findViewById(R.id.file_properties_info_data_content);			
			addedTextView = (TextView) findViewById(R.id.file_properties_info_data_added);
			modifiedTextView = (TextView) findViewById(R.id.file_properties_info_data_modified);
						
			imageView.setImageResource(imageId);
			nameView.setText(name);
			nameView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			nameView.setSingleLine();
			nameView.setTypeface(null, Typeface.BOLD);

			ownerInfo = (TextView) findViewById(R.id.file_properties_owner_info);
			ownerInfo.setVisibility(View.GONE);			
			
//			sharedWith = (RelativeLayout) findViewById(R.id.contacts_shared_with_eye);
//			sharedLayout= (TableLayout) findViewById(R.id.file_properties_content_table);
//			sharedLayout.setOnClickListener(this);			
			
//			((RelativeLayout.LayoutParams)sharedWith.getLayoutParams()).setMargins(0, Util.px2dp((-40*scaleH), outMetrics), 0, 0);
			//sharedWithText = (TextView) findViewById(R.id.public_link);
			//((RelativeLayout.LayoutParams)sharedWithText.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((15*scaleH), outMetrics), 0, Util.px2dp((15*scaleH), outMetrics));
			//sharedWithList = (NestedListView) findViewById(R.id.file_properties_shared_folder_shared_with_list);
			
			//TODO que hago con public link
//			publicLinkTextView = (TextView) findViewById(R.id.file_properties_public_link);		
			
			//sharedWithTextView = (TextView) findViewById(R.id.shared_with_detailed);
			
			sharedLayout = (LinearLayout) findViewById(R.id.file_properties_shared_layout);
			usersSharedWithText = (TextView) findViewById(R.id.file_properties_shared_info);	
			dividerSharedLayout = (View) findViewById(R.id.divider_shared_layout);
			usersSharedWithText.setOnClickListener(this);
			
			permissionLabel = (TextView) findViewById(R.id.file_properties_permission_label);				
			permissionInfo = (TextView) findViewById(R.id.file_properties_permission_info);
			permissionLabel.setVisibility(View.GONE);
			permissionInfo.setVisibility(View.GONE);

		}
		
		refreshProperties();
	}
	
	
	private void refreshProperties(){		
		log("refresh");
	
		boolean result=true;
		
		if(node.isExported()){
			publicLink=true;
			publicLinkImage.setVisibility(View.VISIBLE);
		}	
		else{
			publicLink=false;
			publicLinkImage.setVisibility(View.GONE);
		}
		
		if (node.isFile()){				
			log("onCreate node is FILE");
			sharedLayout.setVisibility(View.GONE);				
			dividerSharedLayout.setVisibility(View.GONE);		
			sizeTitleTextView.setText(getString(R.string.file_properties_info_size_file));
			
			sizeTextView.setText(Formatter.formatFileSize(this, node.getSize()));
			
			contentLayout.setVisibility(View.GONE);	
			
			if(from==FROM_INCOMING_SHARES){
				log("from FROM_INCOMING_SHARES");
				//Show who is the owner
				owner=false;
				ArrayList<MegaUser> usersIncoming = megaApi.getContacts();
				boolean found=false;
				int i=0;
				while(!found && i<usersIncoming.size()){
					MegaUser user = usersIncoming.get(i);
					ArrayList<MegaNode> nodesIncoming = megaApi.getInShares(user);
					
					for(int j=0; j<nodesIncoming.size();j++){
						MegaNode nI = nodesIncoming.get(j);
						
						if(nI.getName().equals(node.getName())){
							ownerInfo.setText(user.getEmail());
							ownerInfo.setVisibility(View.VISIBLE);	
							found=true;
							break;
						}
					}
					i++;
				}
			}
			
			availableOfflineView.setPadding(Util.px2dp(30*scaleW, outMetrics), 0, Util.px2dp(20*scaleW, outMetrics), 0);	
			
			//Choose the button offlineSwitch
			
			if(dbH.exists(node.getHandle())){
				log("Exists OFFLINE: setChecket TRUE");
				availableOfflineBoolean = true;
				offlineSwitch.setChecked(true);
			}
			else{
				log("NOT Exists OFFLINE: setChecket FALSE");
				availableOfflineBoolean = false;
				offlineSwitch.setChecked(false);
			}
			
			if(offlineSwitch.isChecked())
			{
				log("isChecked!");
			}
			else{
				log(" NOOOOT Checked!");
			}
			
//			availableOfflineView.setPadding(Util.px2dp(30*scaleW, outMetrics), 0, Util.px2dp(40*scaleW, outMetrics), 0);
			
			if (node.getCreationTime() != 0){
				try {addedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{addedTextView.setText("");}

				if (node.getModificationTime() != 0){
					try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getModificationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}
				}
				else{
					try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}	
				}
			}
			else{
				addedTextView.setText("");
				modifiedTextView.setText("");
			}
			
			Bitmap thumb = null;
			Bitmap preview = null;
			thumb = ThumbnailUtils.getThumbnailFromCache(node);
			if (thumb != null){
				imageView.setImageBitmap(thumb);
			}
			else{
				thumb = ThumbnailUtils.getThumbnailFromFolder(node, this);
				if (thumb != null){
					imageView.setImageBitmap(thumb);
				}
			}
			preview = PreviewUtils.getPreviewFromCache(node);
			if (preview != null){
				PreviewUtils.previewCache.put(node.getHandle(), preview);
				imageView.setImageBitmap(preview);
			}
			else{
				preview = PreviewUtils.getPreviewFromFolder(node, this);
				if (preview != null){
					PreviewUtils.previewCache.put(node.getHandle(), preview);
					imageView.setImageBitmap(preview);	
				}
				else{
					if (node.hasPreview()){	
						File previewFile = new File(PreviewUtils.getPreviewFolder(this), node.getBase64Handle()+".jpg");
						megaApi.getPreview(node, previewFile.getAbsolutePath(), this);
					}
				}
			}
		}
		else{ //Folder
			
			contentTextView.setVisibility(View.VISIBLE);
			contentTitleTextView.setVisibility(View.VISIBLE);
			
			contentTextView.setText(getInfoFolder(node));
			
			long sizeFile=megaApi.getSize(node);				
			sizeTextView.setText(Formatter.formatFileSize(this, sizeFile));				
			
			//Choose the button availableSwitch
			
			if(dbH.exists(node.getHandle())){
				
				ArrayList<MegaNode> childrenList=megaApi.getChildren(node);
				if(childrenList.size()>0){
					
					result=checkChildrenStatus(childrenList);
					
				}
				
				if(!result){
					log("false checkChildrenStatus: "+result);
					availableOfflineBoolean = false;
					offlineSwitch.setChecked(false);
				}
				else{
					log("true checkChildrenStatus: "+result);
					availableOfflineBoolean = true;
					offlineSwitch.setChecked(true);
				}
				
			}
			else{
				availableOfflineBoolean = false;
				offlineSwitch.setChecked(false);
			}

			availableOfflineView.setPadding(Util.px2dp(15*scaleW, outMetrics), 0, Util.px2dp(20*scaleW, outMetrics), 0);
			
			imageView.setImageResource(imageId);

			if(from==FROM_INCOMING_SHARES){
				//Show who is the owner
				ArrayList<MegaUser> usersIncoming = megaApi.getContacts();
				boolean found=false;
				int i=0;
				while(!found && i<usersIncoming.size()){
					MegaUser user = usersIncoming.get(i);
					ArrayList<MegaNode> nodesIncoming = megaApi.getInShares(user);
					
					for(int j=0; j<nodesIncoming.size();j++){
						MegaNode nI = nodesIncoming.get(j);
						
						if(nI.getName().equals(node.getName())){
							ownerInfo.setText(user.getEmail());
							ownerInfo.setVisibility(View.VISIBLE);	
							found=true;
							break;
						}
					}
					i++;
				}
			}
			
			sl = megaApi.getOutShares(node);		

			if (sl != null){

				if (sl.size() == 0){						
					sharedLayout.setVisibility(View.GONE);
					dividerSharedLayout.setVisibility(View.GONE);	
//					If I am the owner
					if (megaApi.checkAccess(node, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK){
						
						permissionLabel.setVisibility(View.GONE);
						permissionInfo.setVisibility(View.GONE);
						//permissionInfo.setText(getResources().getString(R.string.file_properties_owner));
						
					}
					else{	
						
						owner = false;
						//If I am not the owner
						permissionLabel.setVisibility(View.VISIBLE);
						permissionInfo.setVisibility(View.VISIBLE);
						
						int accessLevel= megaApi.getAccess(node);
						log("Node: "+node.getName());
																				
						switch(accessLevel){
							case MegaShare.ACCESS_OWNER:
							case MegaShare.ACCESS_FULL:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_full_access));	
								break;
							}
							case MegaShare.ACCESS_READ:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_only));
								break;
							}						
							case MegaShare.ACCESS_READWRITE:{								
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_write));
								break;
							}
						}
					}
					
				}
				else{		
					if(publicLink){
//						publicLinkTextView.setText(getResources().getString(R.string.file_properties_shared_folder_public_link));							
//
//						publicLinkTextView.setVisibility(View.VISIBLE);
						
						if(sl.size()>1){
							//It is public and shared
							imageView.setImageResource(R.drawable.folder_shared_mime);
							sharedLayout.setVisibility(View.VISIBLE);
							dividerSharedLayout.setVisibility(View.VISIBLE);	
							usersSharedWithText.setText(sl.size()+" "+getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
						}
						else{
							//It is just public
							imageView.setImageResource(R.drawable.folder_mime);
							sharedLayout.setVisibility(View.GONE);
							dividerSharedLayout.setVisibility(View.GONE);	
//							sharedWithButton.setText(R.string.file_properties_shared_folder_public_link);
						}
						
					}
					else{
//						publicLinkTextView.setText(getResources().getString(R.string.file_properties_shared_folder_private_folder));
						//It is private and shared
						imageView.setImageResource(R.drawable.folder_shared_mime);
						sharedLayout.setVisibility(View.VISIBLE);
						dividerSharedLayout.setVisibility(View.VISIBLE);	
						usersSharedWithText.setText(sl.size()+" "+getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
					}						
				}					
				
				if (node.getCreationTime() != 0){
					try {addedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{addedTextView.setText("");}

					if (node.getModificationTime() != 0){
						try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getModificationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}
					}
					else{
						try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}	
					}
				}
				else{
					addedTextView.setText("");
					modifiedTextView.setText("");
				}
			}
			else{
				sharedLayout.setVisibility(View.GONE);
				dividerSharedLayout.setVisibility(View.GONE);	
			}
		}
	}
		
	private boolean checkChildrenStatus(ArrayList<MegaNode> childrenList){

		boolean children = true;
		ArrayList<MegaNode> childrenListRec;
		
		if(childrenList.size()>0){
			for(int i=0;i<childrenList.size();i++){
				
				if(!dbH.exists(childrenList.get(i).getHandle())){
					children=false;	
					return children;
				}
				else{
					if(childrenList.get(i).isFolder()){
						
						childrenListRec=megaApi.getChildren(childrenList.get(i));

						if(childrenListRec.size()>0){
							boolean result=checkChildrenStatus(childrenListRec);
							if(!result){
								children=false;
								return children;
							}								
						}											
					}
					else{

						if(!dbH.exists(childrenList.get(i).getHandle())){
							children=false;
							return children;
						}
						
					}
				}
			}	
		}	
		return children;
	}
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
			case R.id.file_properties_shared_info:{
				Intent i = new Intent(this, FileContactListActivityLollipop.class);
				i.putExtra("name", node.getHandle());
				startActivity(i);				
				break;
			}
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
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		log("onCheckedChanged");
		
		if(owner){
			log("Owner: me");
			if (!isChecked){
				log("isChecked");
				availableOfflineBoolean = false;
				offlineSwitch.setChecked(false);	
				mOffDelete = dbH.findByHandle(node.getHandle());
				removeOffline(mOffDelete);			
				supportInvalidateOptionsMenu();
			}
			else{		
				log("NOT Checked");
				availableOfflineBoolean = true;
				offlineSwitch.setChecked(true);	
				
				log("Path destination: "+Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/"+createStringTree(node));
				
				File destination = null;
				if (Environment.getExternalStorageDirectory() != null){
					destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/"+createStringTree(node));
				}
				else{
					destination = getFilesDir();
				}

				if (destination.exists() && destination.isDirectory()){
					File offlineFile = new File(destination, node.getName());
					if (offlineFile.exists() && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getName())){ //This means that is already available offline
						return;
					}
				}
				
				saveOffline(destination);
				
				supportInvalidateOptionsMenu();
			}	
		}
		else{
			
			log("not owner");

			if (!isChecked){
				availableOfflineBoolean = false;
				offlineSwitch.setChecked(false);
				mOffDelete = dbH.findByHandle(node.getHandle());
				removeOffline(mOffDelete);			
				supportInvalidateOptionsMenu();
			}
			else{										
				availableOfflineBoolean = true;
				offlineSwitch.setChecked(true);
				
				supportInvalidateOptionsMenu();
				
				log("Comprobando el node"+node.getName());
				
				//check the parent
				long result = -1;
				result=findIncomingParentHandle(node);
				log("IncomingParentHandle: "+result);
				if(result!=-1){
					MegaNode megaNode = megaApi.getNodeByHandle(result);
					if(megaNode!=null){
						log("ParentHandleIncoming: "+megaNode.getName());
					}
					String handleString = Long.toString(result);
					String destinationPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + handleString + "/"+createStringTree(node);
					log("Not owner path destination: "+destinationPath);
					
					File destination = null;
					if (Environment.getExternalStorageDirectory() != null){
						destination = new File(destinationPath);
					}
					else{
						destination = getFilesDir();
					}

					if (destination.exists() && destination.isDirectory()){
						File offlineFile = new File(destination, node.getName());
						if (offlineFile.exists() && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getName())){ //This means that is already available offline
							return;
						}
					}				
					saveOffline(destination);
				}
				else{
					log("result=findIncomingParentHandle NOT result!");
				}
			}
		}
	}
	
	public long findIncomingParentHandle(MegaNode nodeToFind){
		log("findIncomingParentHandle");
		
		MegaNode parentNodeI = megaApi.getParentNode(nodeToFind);
		long result=-1;
		if(parentNodeI==null){
			log("findIncomingParentHandle A: "+nodeToFind.getHandle());
			return nodeToFind.getHandle();
		}
		else{
			result=findIncomingParentHandle(parentNodeI);
			while(result==-1){
				result=findIncomingParentHandle(parentNodeI);
			}	
			log("findIncomingParentHandle B: "+nodeToFind.getHandle());
			return result;
		}	
	}
	
	public void saveOffline (File destination){
		log("saveOffline");

		destination.mkdirs();
		
		log ("DESTINATION!!!!!: " + destination.getAbsolutePath());

		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(destination.getAbsolutePath());
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}

		Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
		if (node.getType() == MegaNode.TYPE_FOLDER) {
			log("saveOffline:isFolder");
			getDlList(dlFiles, node, new File(destination, new String(node.getName())));
		} else {
			log("saveOffline:isFile");
			dlFiles.put(node, destination.getAbsolutePath());			
		}

		for (MegaNode document : dlFiles.keySet()) {

			String path = dlFiles.get(document);	
			
			if(availableFreeSpace <document.getSize()){
				Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, this);
				continue;
			}
	
			String url = null;
			Intent service = new Intent(this, DownloadService.class);
			service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
			service.putExtra(DownloadService.EXTRA_URL, url);
			service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
			service.putExtra(DownloadService.EXTRA_PATH, path);
			service.putExtra(DownloadService.EXTRA_OFFLINE, true);
			startService(service);					
		}
		
	}

	public void removeOffline(MegaOffline mOffDelete){
		
		if (mOffDelete == null){
			return;
		}
		
		log("removeOffline - file(type): "+mOffDelete.getName()+"("+mOffDelete.getType()+")");	
		//mOffDelete = node;
		MegaOffline parentNode = null;	
		ArrayList<MegaOffline> mOffListParent=new ArrayList<MegaOffline>();
		ArrayList<MegaOffline> mOffListChildren=new ArrayList<MegaOffline>();			
				
		if(mOffDelete.getType().equals(DB_FOLDER)){
			log("Finding children... ");
			//Delete in DB
			//Delete children
			mOffListChildren=dbH.findByParentId(mOffDelete.getId());
			if(mOffListChildren.size()>0){
				log("Children: "+mOffListChildren.size());
				//The node have childrens, delete
				deleteChildrenDB(mOffListChildren);			
			}
		}
		else{
			log("NOT children... ");
		}
		
		int parentId = mOffDelete.getParentId();
		log("Finding parents... "+parentId);
		//Delete parents
		if(parentId!=-1){
			mOffListParent=dbH.findByParentId(parentId);
						
			if(mOffListParent.size()<1){
				//No more node with the same parent, keep deleting				
				parentNode = dbH.findById(parentId);
				if(parentNode != null){
					removeOffline(mOffDelete);						
				}	
			}			
		}		
		
		//Remove the node physically
		File destination = null;
		if(mOffDelete.isIncoming()){			
			if (Environment.getExternalStorageDirectory() != null){
				destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);
			}
			else{
				destination = new File(getFilesDir(), mOffDelete.getHandle()+"");
			}
			
			log("Remove incoming: "+destination.getAbsolutePath());
			
			try{
				File offlineFile = new File(destination,  mOffDelete.getHandleIncoming());
				Util.deleteFolderAndSubfolders(this, offlineFile);				
			}
			catch(Exception e){
				log("EXCEPTION: removeOffline - file");
			};	
			
			dbH.removeById(mOffDelete.getId());		
		}
		else
		{			
			if (Environment.getExternalStorageDirectory() != null){
				destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + mOffDelete.getPath());
			}
			else{
				destination = new File(getFilesDir(), mOffDelete.getHandle()+"");
			}
			
			log("Remove node: "+destination.getAbsolutePath());
			
			try{
				File offlineFile = new File(destination, mOffDelete.getName());
				Util.deleteFolderAndSubfolders(this, offlineFile);
			}
			catch(Exception e){
				log("EXCEPTION: removeOffline - file");
			};	
			
			dbH.removeById(mOffDelete.getId());		
		}
		
	}	
	
		
	private void deleteChildrenDB(ArrayList<MegaOffline> mOffList){
		
		log("deleteChildenDB");
		MegaOffline mOffDelete=null;
	
		for(int i=0; i< mOffList.size(); i++){
			
			mOffDelete=mOffList.get(i);
			ArrayList<MegaOffline> mOffListChildren2=dbH.findByParentId(mOffDelete.getId());
			if(mOffList.size()>0){
				//The node have children, delete
				deleteChildrenDB(mOffListChildren2);
				
			}			
			dbH.removeById(mOffDelete.getId());			
		}		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
		    case android.R.id.home:{
		    	finish();
		    	return true;
		    }
		    case R.id.action_file_properties_download:{
		    	if (!availableOfflineBoolean){
			    	ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(node.getHandle());
					downloadNode(handleList);
		    	}
		    	else{
		    		
		    		File destination = null;
					File offlineFile = null;
					if (Environment.getExternalStorageDirectory() != null){
						destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/"+createStringTree(node));
					}
					else{
						destination = new File(getFilesDir(), node.getHandle()+"");
					}
					
					if (destination.exists() && destination.isDirectory()){
						offlineFile = new File(destination, node.getName());
						if (offlineFile.exists() && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getName())){ //This means that is already available offline
							availableOfflineBoolean = true;
							offlineSwitch.setChecked(true);
						}
						else{
							availableOfflineBoolean = false;
							offlineSwitch.setChecked(false);
							mOffDelete = dbH.findByHandle(node.getHandle());
							removeOffline(mOffDelete);							
							supportInvalidateOptionsMenu();
						}
					}
					else{
						availableOfflineBoolean = false;
						offlineSwitch.setChecked(false);
						mOffDelete = dbH.findByHandle(node.getHandle());
						removeOffline(mOffDelete);		
						supportInvalidateOptionsMenu();
					}
		    		Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(offlineFile), MimeTypeList.typeForName(offlineFile.getName()).getType());
					if (ManagerActivityLollipop.isIntentAvailable(this, intent)){
						startActivity(intent);
					}
					else{
						Snackbar.make(container, getString(R.string.intent_not_available), Snackbar.LENGTH_LONG).show();
					}
		    	}
				return true;
		    }
		    case R.id.action_file_properties_share_folder:{
		    	
		    	Intent intent = new Intent(ContactsExplorerActivityLollipop.ACTION_PICK_CONTACT_SHARE_FOLDER);
		    	intent.setClass(this, ContactsExplorerActivityLollipop.class);
		    	intent.putExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
		    	startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
		    	break;
		    }
		    case R.id.action_file_properties_get_link:{
		    	shareIt = false;
		    	typeExport=TYPE_EXPORT_GET;
		    	getPublicLinkAndShareIt();
		    	return true;
		    }
		    case R.id.action_file_properties_manage_link:{
		    	shareIt = false;
		    	typeExport=TYPE_EXPORT_MANAGE;
		    	getPublicLinkAndShareIt();
		    	return true;
		    }
		    case R.id.action_file_properties_remove_link:{
		    	shareIt = false;
		    	AlertDialog removeLinkDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
				builder.setTitle(getString(R.string.context_remove_link_menu));
				
				LayoutInflater inflater = getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
				TextView url = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_url);
				TextView key = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_key);
				TextView symbol = (TextView) dialoglayout.findViewById(R.id.dialog_link_symbol);
				TextView removeText = (TextView) dialoglayout.findViewById(R.id.dialog_link_text_remove);
				
				((RelativeLayout.LayoutParams) removeText.getLayoutParams()).setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(10, outMetrics), 0);
				
				url.setVisibility(View.GONE);
				key.setVisibility(View.GONE);
				symbol.setVisibility(View.GONE);
				removeText.setVisibility(View.VISIBLE);
				
				removeText.setText(getString(R.string.context_remove_link_warning_text));
		    	
				Display display = getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics();
				display.getMetrics(outMetrics);
				float density = getResources().getDisplayMetrics().density;

				float scaleW = Util.getScaleW(outMetrics, density);
				float scaleH = Util.getScaleH(outMetrics, density);
				
				removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (18*scaleW));
				
				builder.setView(dialoglayout);
				
				builder.setPositiveButton(getString(R.string.context_remove), new android.content.DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						typeExport=TYPE_EXPORT_REMOVE;
				    	megaApi.disableExport(node, filePropertiesActivity);
					}
				});
				
				builder.setNegativeButton(getString(R.string.general_cancel), new android.content.DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
				
				removeLinkDialog = builder.create();
				removeLinkDialog.show();
		    	return true;
		    }
		    case R.id.action_file_properties_send_link:{
		    	shareIt = true;
		    	getPublicLinkAndShareIt();
		    	return true;
		    }
		    case R.id.action_file_properties_rename:{
		    	showRenameDialog();
		    	return true;
		    }
		    case R.id.action_file_properties_remove:{
		    	moveToTrash();
		    	return true;
		    }
		    case R.id.action_file_properties_move:{
		    	showMove();
		    	return true;
		    }
		    case R.id.action_file_properties_copy:{
		    	showCopy();
		    	return true;
		    }
		    case R.id.action_file_properties_leave_share: {
		    	leaveIncomingShare();
		    	return true;
		    }
		}	    
	    return super.onOptionsItemSelected(item);
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
	
	public void leaveIncomingShare (){
		log("leaveIncomingShare");
			
		
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	//TODO remove the incoming shares		        	
		    		megaApi.remove(node,filePropertiesActivity);		        	
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_yes, dialogClickListener)
	    	.setNegativeButton(R.string.general_no, dialogClickListener).show();
	}
	
	public void showCopy(){
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());
		
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}
	
	public void showMove(){
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());
		
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}

	public void downloadNode(ArrayList<Long> handleList){
		
		long size = 0;
		long[] hashes = new long[handleList.size()];
		for (int i=0;i<handleList.size();i++){
			hashes[i] = handleList.get(i);
			size += megaApi.getNodeByHandle(hashes[i]).getSize();
		}
		
		if (dbH == null){
//			dbH = new DatabaseHandler(getApplicationContext());
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		
		boolean askMe = true;
		String downloadLocationDefaultPath = "";
		prefs = dbH.getPreferences();		
		if (prefs != null){
			if (prefs.getStorageAskAlways() != null){
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							askMe = false;
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}		
			
		if (askMe){
			Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
			intent.putExtra(FileStorageActivity.EXTRA_SIZE, size);
			intent.setClass(this, FileStorageActivity.class);
			intent.putExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES, hashes);
			startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);	
		}
		else{
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}
	}
	
	public void moveToTrash(){
		log("moveToTrash");
		
		final long handle = node.getHandle();
		moveToRubbish = false;
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		final MegaNode rubbishNode = megaApi.getRubbishNode();
		
		MegaNode parent = megaApi.getNodeByHandle(handle);
		while (megaApi.getParentNode(parent) != null){
			parent = megaApi.getParentNode(parent);
		}
		
		if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
			moveToRubbish = true;			
		}
		else{
			moveToRubbish = false;
		}
		
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	//TODO remove the outgoing shares
		    		//Check if the node is not yet in the rubbish bin (if so, remove it)			
		    		
		    		if (moveToRubbish){
		    			megaApi.moveNode(megaApi.getNodeByHandle(handle), rubbishNode, filePropertiesActivity);
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(filePropertiesActivity);
		    				temp.setMessage(getString(R.string.context_move_to_trash));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}
		    		else{
		    			megaApi.remove(megaApi.getNodeByHandle(handle), filePropertiesActivity);
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(filePropertiesActivity);
		    				temp.setMessage(getString(R.string.context_delete_from_mega));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}
		        	
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};
		
		if (moveToRubbish){
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_move_to_rubbish);
			builder.setMessage(message).setPositiveButton(R.string.general_yes, dialogClickListener)
		    	.setNegativeButton(R.string.general_no, dialogClickListener).show();
		}
		else{
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_delete_from_mega);
			builder.setMessage(message).setPositiveButton(R.string.general_yes, dialogClickListener)
		    	.setNegativeButton(R.string.general_no, dialogClickListener).show();
		}
	}
	
	public void showRenameDialog(){
		
		final EditTextCursorWatcher input = new EditTextCursorWatcher(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setText(node.getName());

		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),
				KeyEvent.KEYCODE_ENTER);
		
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, boolean hasFocus) {
				if (hasFocus) {
					if (node.isFolder()){
						input.setSelection(0, input.getText().length());
					}
					else{
						String [] s = node.getName().split("\\.");
						if (s != null){
							int numParts = s.length;
							int lastSelectedPos = 0;
							if (numParts == 1){
								input.setSelection(0, input.getText().length());
							}
							else if (numParts > 1){
								for (int i=0; i<(numParts-1);i++){
									lastSelectedPos += s[i].length(); 
									lastSelectedPos++;
								}
								lastSelectedPos--; //The last point should not be selected)
								input.setSelection(0, lastSelectedPos);
							}
						}
						showKeyboardDelayed(v);
					}
				}
			}
		});

		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_rename) + " "	+ new String(node.getName()), null, input);
		builder.setPositiveButton(getString(R.string.context_rename),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						rename(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		renameDialog = builder.create();
		renameDialog.show();

		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					renameDialog.dismiss();
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					rename(value);
					return true;
				}
				return false;
			}
		});
	}
	
	private void rename(String newName){
		if (newName.equals(node.getName())) {
			return;
		}
		
		if(!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if (isFinishing()){
			return;
		}
		
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_renaming));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		
		log("renaming " + node.getName() + " to " + newName);
		
		megaApi.renameNode(node, newName, this);
	}
	
	public void getPublicLinkAndShareIt(){
		
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_creating_link));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		
		megaApi.exportNode(node, this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_file_properties, menu);
	    sendLinkMenuItem = menu.findItem(R.id.action_file_properties_send_link);
	    sendLinkMenuItem.setVisible(false);
		getLinkMenuItem = menu.findItem(R.id.action_file_properties_get_link);
		manageLinkMenuItem = menu.findItem(R.id.action_file_properties_manage_link);
		removeLinkMenuItem = menu.findItem(R.id.action_file_properties_remove_link);
		removeMenuItem = menu.findItem(R.id.action_file_properties_remove);
		leaveShareMenuItem = menu.findItem(R.id.action_file_properties_leave_share);
		moveMenuItem = menu.findItem(R.id.action_file_properties_move);
		renameMenuItem = menu.findItem(R.id.action_file_properties_rename);
		copyMenuItem = menu.findItem(R.id.action_file_properties_copy);			   
	    downloadMenuItem = menu.findItem(R.id.action_file_properties_download);
	    shareFolderMenuItem = menu.findItem(R.id.action_file_properties_share_folder);
	    
	    removeMenuItem.setVisible(true);
	    
	    if(node!=null){
	    	if (node.isFolder()){
	 	    	shareFolderMenuItem.setVisible(true);
	 	    }
	 	    else{
	 	    	shareFolderMenuItem.setVisible(false);
	 	    }
	    }
	    else
	    {
	    	onBackPressed();
	    }	   
	    
	    if(publicLink){
	    	getLinkMenuItem.setVisible(false);
	    	removeLinkMenuItem.setVisible(true);
	    	manageLinkMenuItem.setVisible(true);
	    }
	    else{
	    	getLinkMenuItem.setVisible(true);
	    	removeLinkMenuItem.setVisible(false);
	    	manageLinkMenuItem.setVisible(false);
	    }   
	    
		MegaNode parent = megaApi.getNodeByHandle(handle);
		while (megaApi.getParentNode(parent) != null){
			parent = megaApi.getParentNode(parent);
		}
		
		if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
			//Not Rubbish Bin
		    if(from==FROM_INCOMING_SHARES){
		    	downloadMenuItem.setVisible(true);
		    	shareFolderMenuItem.setVisible(false);
		    	getLinkMenuItem.setVisible(false);
		    	removeLinkMenuItem.setVisible(false);
		    	manageLinkMenuItem.setVisible(false);
		    	
		    	if (node.isFolder()){
		    		leaveShareMenuItem.setVisible(true);
		 	    }
		 	    else{
		 	    	leaveShareMenuItem.setVisible(false);
		 	    }
		    	
		    	int accessLevel= megaApi.getAccess(node);
				log("Node: "+node.getName());
				
				switch(accessLevel){
				
					case MegaShare.ACCESS_OWNER:
					case MegaShare.ACCESS_FULL:{
						renameMenuItem.setVisible(true);
						moveMenuItem.setVisible(true);
						copyMenuItem.setVisible(true); 
						removeMenuItem.setTitle(R.string.context_move_to_trash);
						removeMenuItem.setVisible(true); 
						break;
					}
					case MegaShare.ACCESS_READ:{
						copyMenuItem.setVisible(true); 
						renameMenuItem.setVisible(false);
						moveMenuItem.setVisible(false);
						removeMenuItem.setVisible(false);
						break;
					}
					case MegaShare.ACCESS_READWRITE:{
						copyMenuItem.setVisible(true); 
						renameMenuItem.setVisible(false);
						moveMenuItem.setVisible(false);
						removeMenuItem.setVisible(false);
						break;
					}
				}
		    }
		    else{
		    	leaveShareMenuItem.setVisible(false);
		    	downloadMenuItem.setVisible(true);
		    	renameMenuItem.setVisible(true); 
		    	copyMenuItem.setVisible(true); 
		    	removeMenuItem.setTitle(R.string.context_move_to_trash);
		    	removeMenuItem.setVisible(true); 
		    }

		}
		else{
			//File in the Rubbish Bin
			getLinkMenuItem.setVisible(false);
	    	removeLinkMenuItem.setVisible(false);
	    	manageLinkMenuItem.setVisible(false);
	    	downloadMenuItem.setVisible(false);
	    	renameMenuItem.setVisible(false); 
	    	copyMenuItem.setVisible(false); 
	    	removeMenuItem.setTitle(R.string.context_delete);
	    	leaveShareMenuItem.setVisible(false);
//	    	removeMenuItem.setVisible(true); 
		} 
	 
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		
		node = megaApi.getNodeByHandle(request.getNodeHandle());
		
		log("onRequestFinish: "+request.getType() + "__" + request.getRequestString());
		
		if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE){
			if (e.getErrorCode() == MegaError.API_OK){
				File previewDir = PreviewUtils.getPreviewFolder(this);
				File preview = new File(previewDir, node.getBase64Handle()+".jpg");
				if (preview.exists()) {
					if (preview.length() > 0) {
						Bitmap bitmap = PreviewUtils.getBitmapForCache(preview, this);
						PreviewUtils.previewCache.put(handle, bitmap);	
						if (imageView != null){
							imageView.setImageBitmap(bitmap);
						}
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			log("MegaRequest.TYPE_EXPORT");
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				
				if((typeExport==TYPE_EXPORT_GET) || (typeExport == TYPE_EXPORT_MANAGE)){
					log("typeExport==TYPE_EXPORT_GET or typeExport == TYPE_EXPORT_MANAGE");
					
					final String link = request.getLink();
					
					AlertDialog getLinkDialog;
					AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
					if (typeExport == TYPE_EXPORT_GET){
						builder.setTitle(getString(R.string.context_get_link_menu));
					}
					else{
						builder.setTitle(getString(R.string.context_manage_link_menu));
					}
					
					LayoutInflater inflater = getLayoutInflater();
					View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
					TextView url = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_url);
					((RelativeLayout.LayoutParams) url.getLayoutParams()).setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(15, outMetrics), Util.scaleWidthPx(25, outMetrics), 0);					
					TextView symbol = (TextView) dialoglayout.findViewById(R.id.dialog_link_symbol);
					((RelativeLayout.LayoutParams) symbol.getLayoutParams()).setMargins(Util.scaleWidthPx(25, outMetrics), 0, 0, 0);		
					TextView key = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_key);
					((RelativeLayout.LayoutParams) key.getLayoutParams()).setMargins(0, 0, Util.scaleWidthPx(15, outMetrics), 0);					
					
					String urlString = "";
					String keyString = "";
					String [] s = link.split("!");
					if (s.length == 3){
						urlString = s[0] + "!" + s[1];
						keyString = s[2];
					}					
					
					url.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));
					key.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));
					
					url.setText(urlString);
					key.setText(keyString);					
					
					builder.setView(dialoglayout);
					
					builder.setPositiveButton(getString(R.string.context_send_link), new android.content.DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(Intent.ACTION_SEND);
							intent.setType("text/plain");
							intent.putExtra(Intent.EXTRA_TEXT, link);
							startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
						}
					});
					
					builder.setNegativeButton(getString(R.string.context_copy_link), new android.content.DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
							    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
							    clipboard.setText(link);
							} else {
							    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
							    android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", link);
					            clipboard.setPrimaryClip(clip);
							}

							Snackbar.make(container, getString(R.string.file_properties_get_link), Snackbar.LENGTH_LONG).show();
						}
					});
					
					getLinkDialog = builder.create();
					getLinkDialog.show();
				}
				else if(typeExport==TYPE_EXPORT_REMOVE)
				{		
					log("TYPE_EXPORT_REMOVE");
					Snackbar.make(container, getString(R.string.file_properties_remove_link), Snackbar.LENGTH_LONG).show();
				}					
				
			}
			else{
				Snackbar.make(container, getString(R.string.context_no_link), Snackbar.LENGTH_LONG).show();
			}
			log("export request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(container, getString(R.string.context_correctly_renamed), Snackbar.LENGTH_LONG).show();
				nameView.setText(megaApi.getNodeByHandle(request.getNodeHandle()).getName());
			}			
			else{
				Snackbar.make(container, getString(R.string.context_no_renamed), Snackbar.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(container, getString(R.string.context_correctly_moved), Snackbar.LENGTH_LONG).show();
					finish();
				}
				else{
					Snackbar.make(container, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(container, getString(R.string.context_correctly_moved), Snackbar.LENGTH_LONG).show();
					finish();
				}
				else{
					Snackbar.make(container, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
				}
				log("move nodes request finished");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			
			log("remove request finished");
			if (e.getErrorCode() == MegaError.API_OK){				
				finish();				
			}
			else{
				Snackbar.make(container, getString(R.string.context_no_removed), Snackbar.LENGTH_LONG).show();
			}			
			
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				if (request.getEmail() != null){
					Snackbar.make(container, getString(R.string.context_correctly_copied_contact), Snackbar.LENGTH_LONG).show();
				}
				else{
					Snackbar.make(container, getString(R.string.context_correctly_copied), Snackbar.LENGTH_LONG).show();
				}
			}
			else{
				Snackbar.make(container, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		}
		if (request.getType() == MegaRequest.TYPE_SHARE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(container, getString(R.string.context_correctly_shared), Snackbar.LENGTH_LONG).show();
				ArrayList<MegaShare> sl = megaApi.getOutShares(node);
			}
			else{
				Snackbar.make(container, getString(R.string.context_no_shared), Snackbar.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (intent == null) {
			return;
		}
		
		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivity.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivity.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);

			
			downloadTo (parentPath, url, size, hashes);
			Util.showToast(this, R.string.download_began);
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {
			
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);
			final int totalMoves = moveHandles.length;
			
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			moveToRubbish = false;
			
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_moving));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
			
			for(int i=0; i<moveHandles.length;i++){
				megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, this);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;
			
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_copying));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
			
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for(int i=0; i<copyHandles.length;i++){
				megaApi.copyNode(megaApi.getNodeByHandle(copyHandles[i]), parent, this);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			final ArrayList<String> contactsData = intent.getStringArrayListExtra(ContactsExplorerActivityLollipop.EXTRA_CONTACTS);
			final long nodeHandle = intent.getLongExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, -1);
			final boolean megaContacts = intent.getBooleanExtra(ContactsExplorerActivityLollipop.EXTRA_MEGA_CONTACTS, true);
			
			if (megaContacts){
				if (node.isFolder()){
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
					dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
					final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
					dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							ProgressDialog temp = null;
							try{
								temp = new ProgressDialog(filePropertiesActivity);
								temp.setMessage(getString(R.string.context_sharing_folder));
								temp.show();
							}
							catch(Exception e){
								return;
							}
							statusDialog = temp;
							permissionsDialog.dismiss();
							
							switch(item) {
			                    case 0:{
			                    	for (int i=0;i<contactsData.size();i++){
			                    		MegaUser u = megaApi.getContact(contactsData.get(i));
			                    		megaApi.share(node, u, MegaShare.ACCESS_READ, filePropertiesActivity);
			                    	}
			                    	break;
			                    }
			                    case 1:{
			                    	for (int i=0;i<contactsData.size();i++){
			                    		MegaUser u = megaApi.getContact(contactsData.get(i));
			                    		megaApi.share(node, u, MegaShare.ACCESS_READWRITE, filePropertiesActivity);
			                    	}
			                        break;
			                    }
			                    case 2:{
			                    	for (int i=0;i<contactsData.size();i++){
			                    		MegaUser u = megaApi.getContact(contactsData.get(i));
			                    		megaApi.share(node, u, MegaShare.ACCESS_FULL, filePropertiesActivity);
			                    	}		                    	
			                        break;
			                    }
			                }
						}
					});
					permissionsDialog = dialogBuilder.create();
					permissionsDialog.show();
					Resources resources = permissionsDialog.getContext().getResources();
					int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
					TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
			        alertTitle.setTextColor(resources.getColor(R.color.mega));
					int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
					View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
					titleDivider.setBackgroundColor(resources.getColor(R.color.mega));
				}
				else{ 
					for (int i=0;i<contactsData.size();i++){
						MegaUser u = megaApi.getContact(contactsData.get(i));
						megaApi.sendFileToUser(node, u, filePropertiesActivity);
					}
				}
			}
			else{
				if (node.isFolder()){
					for (int i=0; i < contactsData.size();i++){
						String type = contactsData.get(i);
						if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_EMAIL) == 0){
							i++;
							Snackbar.make(container, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
						}
						else if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_PHONE) == 0){
							i++;
							Snackbar.make(container, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
						}
					}
				}
				else{
					for (int i=0; i < contactsData.size();i++){
						String type = contactsData.get(i);
						if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_EMAIL) == 0){
							i++;
							Snackbar.make(container, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
						}
						else if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_PHONE) == 0){
							i++;
							Snackbar.make(container, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
						}
					}
				}
			}			
		}
	}
	
	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
		
		if (megaApi.getRootNode() == null)
			return;
		
		folder.mkdir();
		ArrayList<MegaNode> nodeList = megaApi.getChildren(parent);
		for(int i=0; i<nodeList.size(); i++){
			MegaNode document = nodeList.get(i);
			if (document.getType() == MegaNode.TYPE_FOLDER) {
				File subfolder = new File(folder, new String(document.getName()));
				getDlList(dlFiles, document, subfolder);
			} 
			else {
				dlFiles.put(document, folder.getAbsolutePath());
			}
		}
	}
	
	private String getInfoFolder (MegaNode n){
		int numFolders = megaApi.getNumChildFolders(n);
		int numFiles = megaApi.getNumChildFiles(n);
		
		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_files, numFiles);
		}
		
		return info;
	}
	
	private String createStringTree (MegaNode node){
		log("createStringTree");
		dTreeList = new ArrayList<MegaNode>();
		MegaNode parentNode = null;
		MegaNode nodeTemp = node;
		StringBuilder dTree = new StringBuilder();
		String s;
		
		dTreeList.add(node);
		
		if(node.getType() != MegaNode.TYPE_ROOT){
			parentNode=megaApi.getParentNode(nodeTemp);
			
//			if(parentNode!=null){
//				while (parentNode.getType() != MegaNode.TYPE_ROOT){
//					if(parentNode!=null){
//						dTreeList.add(parentNode);
//						dTree.insert(0, parentNode.getName()+"/");	
//						nodeTemp=parentNode;
//						parentNode=megaApi.getParentNode(nodeTemp);
//					}					
//				}
//			}
			
			if(parentNode!=null){
				
				if(parentNode.getType() != MegaNode.TYPE_ROOT){					
					do{
						
						dTreeList.add(parentNode);
						dTree.insert(0, parentNode.getName()+"/");	
						nodeTemp=parentNode;
						parentNode=megaApi.getParentNode(nodeTemp);
						if(parentNode==null){
							break;
						}					
					}while (parentNode.getType() != MegaNode.TYPE_ROOT);
				
				}				
			}
		
		}			
		
		if(dTree.length()>0){
			s = dTree.toString();
		}
		else{
			s="";
		}
			
		log("createStringTree: "+s);
		return s;
	}
	
	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		log("onUsersUpdate");		
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		log("onNodesUpdate");
		
		supportInvalidateOptionsMenu();
		
		if(!node.isExported()){
			log("Node HAS public link");
			publicLink=true;
			publicLinkImage.setVisibility(View.VISIBLE);
		}	
		else{
			log("Node NOT public link");
			publicLink=false;
			publicLinkImage.setVisibility(View.GONE);
		}
		
		if (node.isFolder()){
			imageView.setImageResource(imageId);
			sl = megaApi.getOutShares(node);
			if (sl != null){

				if (sl.size() == 0){
					log("sl.size==0");
					sharedLayout.setVisibility(View.GONE);
					dividerSharedLayout.setVisibility(View.GONE);	
					imageView.setImageResource(R.drawable.folder_mime);
					
//					If I am the owner
					if (megaApi.checkAccess(node, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK){
						
						permissionLabel.setVisibility(View.GONE);
						permissionInfo.setVisibility(View.GONE);
						//permissionInfo.setText(getResources().getString(R.string.file_properties_owner));
						
					}
					else{	
						
						//If I am not the owner
						owner = false;
						permissionLabel.setVisibility(View.VISIBLE);
						permissionInfo.setVisibility(View.VISIBLE);
						
						int accessLevel= megaApi.getAccess(node);
						log("Node: "+node.getName());
						
						switch(accessLevel){
							case MegaShare.ACCESS_OWNER:
							case MegaShare.ACCESS_FULL:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_full_access));								
								break;
							}
							case MegaShare.ACCESS_READ:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_only));								
								break;
							}						
							case MegaShare.ACCESS_READWRITE:{								
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_write));
								break;
							}
						}
					}
					
				}
				else{	
					if(publicLink){
//						publicLinkTextView.setText(getResources().getString(R.string.file_properties_shared_folder_public_link));							
//
//						publicLinkTextView.setVisibility(View.VISIBLE);
						
						if(sl.size()>1){
							//It is public and shared
							imageView.setImageResource(R.drawable.folder_shared_mime);
							sharedLayout.setVisibility(View.VISIBLE);
							dividerSharedLayout.setVisibility(View.VISIBLE);	
							usersSharedWithText.setText((sl.size()-1)+" "+getResources().getQuantityString(R.plurals.general_num_users,(sl.size()-1)));
						}
						else{
							//It is just public
							imageView.setImageResource(R.drawable.folder_mime);
							sharedLayout.setVisibility(View.GONE);
							dividerSharedLayout.setVisibility(View.GONE);	
							usersSharedWithText.setText(R.string.file_properties_shared_folder_public_link);
						}
						
					}
					else{
//						publicLinkTextView.setText(getResources().getString(R.string.file_properties_shared_folder_private_folder));
						//It is private and shared
						imageView.setImageResource(R.drawable.folder_shared_mime);
						sharedLayout.setVisibility(View.VISIBLE);
						dividerSharedLayout.setVisibility(View.VISIBLE);	
						usersSharedWithText.setText(sl.size()+" "+getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
					}					
				}


				if (node.getCreationTime() != 0){
					try {addedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{addedTextView.setText("");}

					if (node.getModificationTime() != 0){
						try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getModificationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}
					}
					else{
						try {modifiedTextView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));}catch(Exception ex)	{modifiedTextView.setText("");}	
					}
				}
				else{
					addedTextView.setText("");
					modifiedTextView.setText("");
				}
			}

//			iconView.setImageResource(imageId);
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		log("onReloadNeeded");
	}
	
	@Override
	protected void onDestroy(){
    	super.onDestroy();
    	
    	if(megaApi != null)
    	{
    		megaApi.removeGlobalListener(this);
    		megaApi.removeRequestListener(this);
    	}
    }

	public static void log(String message) {
		Util.log("FilePropertiesActivityLollipop", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	protected void onResume() {
		log("onResume-FilePropertiesActivityLollipop");
		super.onResume();		
		
		refreshProperties();
	}
	
	public void downloadTo(String parentPath, String url, long size, long [] hashes){
		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}
		
		
		if (hashes == null){
			if(url != null) {
				if(availableFreeSpace < size) {
					Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space), false, this);
					return;
				}
				
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				startService(service);
			}
		}
		else{
			if(hashes.length == 1){
				MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);
				if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
					log("ISFILE");
					String localPath = Util.getLocalFile(this, tempNode.getName(), tempNode.getSize(), parentPath);
					if(localPath != null){	
						try { 
							Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName())); 
						}
						catch(Exception e) {}
						
						Intent viewIntent = new Intent(Intent.ACTION_VIEW);
						viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
						if (ManagerActivityLollipop.isIntentAvailable(this, viewIntent))
							startActivity(viewIntent);
						else{
							Intent intentShare = new Intent(Intent.ACTION_SEND);
							intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
							if (ManagerActivityLollipop.isIntentAvailable(this, intentShare))
								startActivity(intentShare);
							String toastMessage = getString(R.string.general_already_downloaded) + ": " + localPath;
							Snackbar.make(container, toastMessage, Snackbar.LENGTH_LONG).show();
						}								
						return;
					}
				}
			}
			
			for (long hash : hashes) {
				MegaNode node = megaApi.getNodeByHandle(hash);
				if(node != null){
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					if (node.getType() == MegaNode.TYPE_FOLDER) {
						getDlList(dlFiles, node, new File(parentPath, new String(node.getName())));
					} else {
						dlFiles.put(node, parentPath);
					}
					
					for (MegaNode document : dlFiles.keySet()) {
						
						String path = dlFiles.get(document);
						
						if(availableFreeSpace < document.getSize()){
							Snackbar.make(container, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
							continue;
						}
						
						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
						service.putExtra(DownloadService.EXTRA_URL, url);
						service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						startService(service);
					}
				}
				else if(url != null) {
					if(availableFreeSpace < size) {
						Snackbar.make(container, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
						continue;
					}
					
					Intent service = new Intent(this, DownloadService.class);
					service.putExtra(DownloadService.EXTRA_HASH, hash);
					service.putExtra(DownloadService.EXTRA_URL, url);
					service.putExtra(DownloadService.EXTRA_SIZE, size);
					service.putExtra(DownloadService.EXTRA_PATH, parentPath);
					startService(service);
				}
				else {
					log("node not found");
				}
			}
		}
	}


	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onContactRequestsUpdate(MegaApiJava api,
			ArrayList<MegaContactRequest> requests) {
		// TODO Auto-generated method stub
		
	}
}
