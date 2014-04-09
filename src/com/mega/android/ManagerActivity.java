package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mega.android.FileStorageActivity.Mode;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaTransferListenerInterface;
import com.mega.sdk.MegaUser;
import com.mega.sdk.NodeList;
import com.mega.sdk.UserList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils.TruncateAt;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class ManagerActivity extends ActionBarActivity implements OnItemClickListener, OnClickListener, MegaRequestListenerInterface, MegaTransferListenerInterface, MegaGlobalListenerInterface {
			
	public enum DrawerItem {
		CLOUD_DRIVE, SAVED_FOR_OFFLINE, SHARED_WITH_ME, RUBBISH_BIN, CONTACTS, IMAGE_VIEWER, TRANSFERS, ACCOUNT;

		public String getTitle(Context context) {
			switch(this)
			{
				case CLOUD_DRIVE: return context.getString(R.string.section_cloud_drive);
				case SAVED_FOR_OFFLINE: return context.getString(R.string.section_saved_for_offline);
				case SHARED_WITH_ME: return context.getString(R.string.section_shared_with_me);
				case RUBBISH_BIN: return context.getString(R.string.section_rubbish_bin);
				case CONTACTS: return context.getString(R.string.section_contacts);
				case IMAGE_VIEWER: return context.getString(R.string.section_image_viewer);
				case TRANSFERS: return context.getString(R.string.section_transfers);
				case ACCOUNT: return context.getString(R.string.section_account);
			}
			return null;
		}
	}
	
	public static int REQUEST_CODE_GET = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_GET_LOCAL = 1003;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_REFRESH = 1005;
	public static int REQUEST_CODE_SORT_BY = 1006;
	
	public static String ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD";
	public static String ACTION_CANCEL_UPLOAD = "CANCEL_UPLOAD";
	
	private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    
   	private SearchView mSearchView;    
	private MenuItem searchMenuItem;
	
	private MenuItem createFolderMenuItem;
	private MenuItem uploadMenuItem;
	private MenuItem moreOptionsMenuItem;
	
	private static DrawerItem drawerItem;
	
	private TableLayout topControlBar;
	private TableLayout bottomControlBar;
	private ImageView imageProfile;
	private TextView userName;
	private TextView userEmail;
	private TextView used_space;
	
	MegaUser contact = null;
	
	ImageButton customListGrid;
	LinearLayout customSearch;

	private boolean firstTime = true;
	
	long parentHandle;
	private boolean isListCloudDrive = true;
	private boolean isListContacts = true;
	private boolean isListRubbishBin = true;
	private FileBrowserFragment fbF;
	private ContactsFragment cF;
	private RubbishBinFragment rbF;
    private TransfersFragment tF; 
    
    static ManagerActivity managerActivity;
    private MegaApiAndroid megaApi;
    
    private static int EDIT_TEXT_ID = 1;
    
    private AlertDialog renameDialog;
    private AlertDialog newFolderDialog;
    private AlertDialog addContactDialog;
    
    private Handler handler;
    
    private boolean moveToRubbish = false;
    
    ProgressDialog statusDialog;
    
	public UploadHereDialog uploadDialog;
	
	private List<ShareInfo> filePreparedInfos;
	
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		managerActivity = this;
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
	    
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		if (Preferences.getCredentials(this) == null){
			logout(this, (MegaApplication)getApplication(), megaApi);
			return;
		}
		
		megaApi.addGlobalListener(this);
		
		handler = new Handler();
		
		setContentView(R.layout.activity_manager);

		imageProfile = (ImageView) findViewById(R.id.profile_photo);
		userEmail = (TextView) findViewById(R.id.profile_user_email);
		userEmail.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		userEmail.getLayoutParams().width = Util.px2dp((235*scaleW), outMetrics);
		userEmail.setSingleLine();
		userEmail.setEllipsize(TruncateAt.END);
		userName = (TextView) findViewById(R.id.profile_user_name);
		userName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		userName.getLayoutParams().width = Util.px2dp((235*scaleW), outMetrics);
		userName.setSingleLine();
		userName.setEllipsize(TruncateAt.END);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer_list);
        topControlBar = (TableLayout) findViewById(R.id.top_control_bar);
        topControlBar.setOnClickListener(this);
        bottomControlBar = (TableLayout) findViewById(R.id.bottom_control_bar);
        bottomControlBar.setOnClickListener(this);
        used_space = (TextView) findViewById(R.id.used_space);
		
		MegaNode rootNode = megaApi.getRootNode();
		if (rootNode == null){
			Intent intent = new Intent(managerActivity,LoginActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		else{
			
			UserList contacts = megaApi.getContacts();
			for (int i=0; i < contacts.size(); i++){
				if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_ME){
					contact = contacts.get(i);
				}
			}
			
			if (contact != null){
				userEmail.setText(contact.getEmail());
				String userNameString = contact.getEmail();
				String [] sp = userNameString.split("@");
				if (sp.length != 0){
					userNameString = sp[0];
					userName.setText(userNameString);
				}
				
				File avatar = new File(getCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
				Bitmap imBitmap = null;
				if (avatar.exists()){
					if (avatar.length() > 0){
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bOpts.inPurgeable = true;
						bOpts.inInputShareable = true;
						imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
						if (imBitmap == null) {
							avatar.delete();
							megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
						}
						else{
							Bitmap circleBitmap = Bitmap.createBitmap(imBitmap.getWidth(), imBitmap.getHeight(), Bitmap.Config.ARGB_8888);
							
							BitmapShader shader = new BitmapShader (imBitmap,  TileMode.CLAMP, TileMode.CLAMP);
					        Paint paint = new Paint();
					        paint.setShader(shader);
					
					        Canvas c = new Canvas(circleBitmap);
					        int radius; 
					        if (imBitmap.getWidth() < imBitmap.getHeight())
					        	radius = imBitmap.getWidth()/2;
					        else
					        	radius = imBitmap.getHeight()/2;
					        
						    c.drawCircle(imBitmap.getWidth()/2, imBitmap.getHeight()/2, radius, paint);
					        imageProfile.setImageBitmap(circleBitmap);
						}
					}
					else{
						megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
					}
				}
				else{
					megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
				}
			}
			
	        String used = "11";
	        String total = "50";
	        
	        String used_space_string = getString(R.string.used_space, used, total);
	        used_space.setText(used_space_string);
	        
	        Spannable wordtoSpan = new SpannableString(used_space_string);        
	
	        wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_OK)), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	        wordtoSpan.setSpan(new RelativeSizeSpan(1.5f), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	        wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.navigation_drawer_mail)), 6, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	        wordtoSpan.setSpan(new RelativeSizeSpan(1.5f), 9, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	        used_space.setText(wordtoSpan);
	        
	        List<String> items = new ArrayList<String>();
			for (DrawerItem item : DrawerItem.values()) {
				items.add(item.getTitle(this));
			}
	        
	        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
					R.layout.drawer_list_item, items)
					{
						public View getView(int position, View rowView, ViewGroup parentView) {
							TextView view = (TextView)super.getView(position, rowView, parentView);
							switch(position)
							{
							case 0:
								view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contacts,0,0,0);
								break;
							case 1:
								view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contacts,0,0,0);
								break;
							case 2:
								view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contacts,0,0,0);
								break;
							case 3:
								view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contacts,0,0,0);
								break;
							case 4:
								view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contacts,0,0,0);
								break;
							case 5:
								view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contacts,0,0,0);
								break;
							case 6:
								view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contacts,0,0,0);
								break;
							case 7:
								view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contacts,0,0,0);
								break;
							}
							return view;
						}
					}
					
					);
	        
	        mDrawerList.setOnItemClickListener(this);
	        
	        getSupportActionBar().setIcon(R.drawable.ic_launcher);
	        getSupportActionBar().setHomeButtonEnabled(true);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	        
	        mDrawerToggle = new ActionBarDrawerToggle(
	                this,                  /* host Activity */
	                mDrawerLayout,         /* DrawerLayout object */
	                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
	                R.string.app_name,  /* "open drawer" description for accessibility */
	                R.string.app_name  /* "close drawer" description for accessibility */
	                ) {
	            public void onDrawerClosed(View view) {
	                supportInvalidateOptionsMenu();	// creates call to onPrepareOptionsMenu()
	            }
	
	            public void onDrawerOpened(View drawerView) {
	                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
	            }
	        };
	        mDrawerToggle.setDrawerIndicatorEnabled(true);
	        mDrawerLayout.setDrawerListener(mDrawerToggle);
	        if (savedInstanceState == null){
	        	mDrawerLayout.openDrawer(Gravity.LEFT);
	        }
	        else{
				mDrawerLayout.closeDrawer(Gravity.LEFT);
	        }
	        
	        //Create the actionBar Menu
	        getSupportActionBar().setDisplayShowCustomEnabled(true);
	        getSupportActionBar().setCustomView(R.layout.custom_action_bar_top);
	        
	        customSearch = (LinearLayout) getSupportActionBar().getCustomView().findViewById(R.id.custom_search);
	        customSearch.setOnClickListener(this);
			
			customListGrid = (ImageButton) getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
			customListGrid.setOnClickListener(this);
			
			parentHandle = -1;
			if (savedInstanceState != null){
				int visibleFragment = savedInstanceState.getInt("visibleFragment");
				parentHandle = savedInstanceState.getLong("parentHandle");
				switch (visibleFragment){
					case 1:{
						drawerItem = DrawerItem.CLOUD_DRIVE;
						isListCloudDrive = true;
						break;
					}
					case 2:{
						drawerItem = DrawerItem.CLOUD_DRIVE;
						isListCloudDrive = false;
						break;
					}
					case 3:{
						drawerItem = DrawerItem.CONTACTS;
						isListContacts = true;
						break;
					}
					case 4:{
						drawerItem = DrawerItem.CONTACTS;
						isListContacts = false;
						break;
					}
					case 5:{
						drawerItem = DrawerItem.RUBBISH_BIN;
						isListRubbishBin = true;
						break;
					}
					case 6:{
						drawerItem = DrawerItem.RUBBISH_BIN;
						isListRubbishBin = false;
						break;
					}
					case 7:{
						drawerItem = DrawerItem.TRANSFERS;
						break;
					}
				}
			}
			
			if (drawerItem == null) {
				drawerItem = DrawerItem.CLOUD_DRIVE;
			}
			else{
				mDrawerLayout.closeDrawer(Gravity.LEFT);
			}
	
			//INITIAL FRAGMENT
			selectDrawerItem(drawerItem);
		}
	}
    
    @Override
	protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	long parentHandle = -1;
    	int visibleFragment = -1;
    	if (fbF != null){
    		if (fbF.isVisible()){
    			parentHandle = fbF.getParentHandle();
    			if (isListCloudDrive){
    				visibleFragment = 1;
    			}
    			else{
    				visibleFragment = 2;
    			}
    		}
    	}
    	if (cF != null){
    		if (cF.isVisible()){
    			if (isListContacts){
    				visibleFragment = 3;
    			}
    			else{
    				visibleFragment = 4;
    			}
    		}
    	}
    	
    	if (rbF != null){
    		if (rbF.isVisible()){
    			if (isListRubbishBin){
    				visibleFragment = 5;
    			}
    			else{
    				visibleFragment = 6;
    			}
    		}
    	}
    	
    	if (tF != null){
	    	if (tF.isVisible()){
	    		visibleFragment = 7;
	    	}
    	}
    	
    	outState.putInt("visibleFragment", visibleFragment);
    	outState.putLong("parentHandle", parentHandle);
    }
    
    @Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	megaApi.removeGlobalListener(this);
    }
    
    @Override
	protected void onNewIntent(Intent intent){
    	super.onNewIntent(intent);
    	setIntent(intent);    	
	}
    
    @Override
	protected void onResume() {
    	super.onResume();
    	managerActivity = this;
    	
    	log("onResume");
    	
    	if(Preferences.getCredentials(this) == null){	
			logout(this, (MegaApplication)getApplication(), megaApi);
			return;
		}
    	
    	Intent intent = getIntent();
    	
    	if (intent != null) {
    		log("intent != null");
    		if (intent.getAction() != null){
    			log("getAction != null");
    			if(intent.getAction().equals(ACTION_CANCEL_UPLOAD) || intent.getAction().equals(ACTION_CANCEL_DOWNLOAD)){
    				log("ACTION_CANCEL_UPLOAD or ACTION_CANCEL_DOWNLOAD");
					Intent tempIntent = null;
					String title = null;
					String text = null;
					if(intent.getAction().equals(ACTION_CANCEL_UPLOAD)){
						tempIntent = new Intent(this, UploadService.class);
						tempIntent.setAction(UploadService.ACTION_CANCEL);
						title = getString(R.string.upload_uploading);
						text = getString(R.string.upload_cancel_uploading);
					}
					else{
						tempIntent = new Intent(this, DownloadService.class);
						tempIntent.setAction(DownloadService.ACTION_CANCEL);
						title = getString(R.string.download_downloading);
						text = getString(R.string.download_cancel_downloading);
					}
					
					final Intent cancelIntent = tempIntent;
					AlertDialog.Builder builder = Util.getCustomAlertBuilder(this,
							title, text, null);
					builder.setPositiveButton(getString(R.string.general_yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									startService(cancelIntent);
								}
							});
					builder.setNegativeButton(getString(R.string.general_no), null);
					final AlertDialog dialog = builder.create();
					try {
						dialog.show(); 
					}
					catch(Exception ex)	{ 
						startService(cancelIntent); 
					}
				}
    			intent.setAction(null);
				setIntent(null);
    		}
    	}
    }
    
    public void selectDrawerItem(DrawerItem item){
    	switch (item){
    		case CLOUD_DRIVE:{
				if (fbF == null){
					fbF = new FileBrowserFragment();
					fbF.setParentHandle(parentHandle);
					fbF.setIsList(isListCloudDrive);
				}
				
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fbF, "fbF").commit();
				if (isListCloudDrive){					
					customListGrid.setImageResource(R.drawable.ic_menu_action_grid);
				}
				else{
    				customListGrid.setImageResource(R.drawable.ic_menu_action_list);
    			}
    			    			
    			if (!firstTime){
    				mDrawerLayout.closeDrawer(Gravity.LEFT);
    			}
    			else{
    				firstTime = false;
    			}
    			
    			customListGrid.setVisibility(View.VISIBLE);
    			customSearch.setVisibility(View.VISIBLE);
    			

    			if (createFolderMenuItem != null){
	    			createFolderMenuItem.setVisible(true);
	    			uploadMenuItem.setVisible(true);
	    			moreOptionsMenuItem.setVisible(true);
	    			
	    			createFolderMenuItem.setIcon(R.drawable.ic_menu_new_folder_dark);
	    			uploadMenuItem.setIcon(R.drawable.ic_menu_upload_here_dark);
	    			uploadMenuItem.setEnabled(true);
	    			moreOptionsMenuItem.setIcon(R.drawable.ic_action_content_new);
    			}
    			
    			break;
    		}
    		case CONTACTS:{
    			
    			if (cF == null){
    				cF = new ContactsFragment();
    				cF.setIsList(isListContacts);
    			}
    			
    			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, cF, "cF").commit();
    			if (isListContacts){
    				customListGrid.setImageResource(R.drawable.ic_menu_action_grid);
				}
				else{
    				customListGrid.setImageResource(R.drawable.ic_menu_action_list);
    			}
    			
    			customListGrid.setVisibility(View.VISIBLE);
    			customSearch.setVisibility(View.VISIBLE);
    			
    			mDrawerLayout.closeDrawer(Gravity.LEFT);

    			if (createFolderMenuItem != null){
    				createFolderMenuItem.setVisible(true);
	    			uploadMenuItem.setVisible(true);
	    			moreOptionsMenuItem.setVisible(true);
	    			
	    			createFolderMenuItem.setIcon(R.drawable.ic_action_social_add_person);
	    			uploadMenuItem.setIcon(R.drawable.ic_action_bar_null);
	    			uploadMenuItem.setEnabled(false);
	    			moreOptionsMenuItem.setIcon(R.drawable.ic_action_content_new);
    			}
    			break;
    		}
    		case RUBBISH_BIN:{
    			
    			if (rbF == null){
    				rbF = new RubbishBinFragment();
    				rbF.setIsList(isListRubbishBin);
    			}
    			
    			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, rbF, "rbF").commit();
    			if (isListRubbishBin){
    				customListGrid.setImageResource(R.drawable.ic_menu_action_grid);
				}
				else{
    				customListGrid.setImageResource(R.drawable.ic_menu_action_list);
    			}
    			
    			customListGrid.setVisibility(View.VISIBLE);
    			customSearch.setVisibility(View.VISIBLE);
    			mDrawerLayout.closeDrawer(Gravity.LEFT);

    			break;
    		}
    		case TRANSFERS:{
    			
    			if (tF == null){
    				tF = new TransfersFragment();
    			}
    			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, tF).commit();
    			customListGrid.setVisibility(View.GONE);
    			customSearch.setVisibility(View.GONE);

    			mDrawerLayout.closeDrawer(Gravity.LEFT);

    			break;
    		}
		default:
			break;
    	}
    }
	
	@Override
	public void onBackPressed() {
		if (fbF != null){
			if (fbF.isVisible()){
				if (fbF.onBackPressed() == 0){
					super.onBackPressed();
					return;
				}
			}
		}
		
		if (cF != null){
			if (cF.isVisible()){
				if (cF.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItem(drawerItem);
					return;
				}
			}
		}
		
		if (rbF != null){
			if (rbF.isVisible()){
				if (rbF.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItem(drawerItem);
					return;
				}
			}
		}
		
		if (tF != null){
			if (tF.isVisible()){
				if (tF.onBackPressed() == 0){
					super.onBackPressed();
					return;
				}
			}
		}
	}


	@Override
	public void onPostCreate(Bundle savedInstanceState){
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_manager, menu);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
	    
	    final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchMenuItem = menu.findItem(R.id.action_search);
		searchMenuItem.setVisible(false);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		
		if (searchView != null){
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setIconifiedByDefault(true);
		}
		
		uploadMenuItem = menu.findItem(R.id.action_upload);
		moreOptionsMenuItem = menu.findItem(R.id.action_more_options);
		createFolderMenuItem = menu.findItem(R.id.action_new_folder);
		
		if (fbF != null){
			if (fbF.isVisible()){
    			createFolderMenuItem.setVisible(true);
    			uploadMenuItem.setVisible(true);
    			moreOptionsMenuItem.setVisible(true);
    			
    			createFolderMenuItem.setIcon(R.drawable.ic_menu_new_folder_dark);
    			uploadMenuItem.setIcon(R.drawable.ic_menu_upload_here_dark);
    			uploadMenuItem.setEnabled(true);
    			moreOptionsMenuItem.setIcon(R.drawable.ic_action_content_new);
			}
		}
		
		if (cF != null){
			if (cF.isVisible()){
				createFolderMenuItem.setVisible(true);
    			uploadMenuItem.setVisible(true);
    			moreOptionsMenuItem.setVisible(true);
    			
    			createFolderMenuItem.setIcon(R.drawable.ic_action_social_add_person);
    			uploadMenuItem.setIcon(R.drawable.ic_action_bar_null);
    			uploadMenuItem.setEnabled(false);
    			moreOptionsMenuItem.setIcon(R.drawable.ic_action_content_new);
			}
		}
	    	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		// Handle presses on the action bar items
	    switch (item.getItemId()) {
		    case android.R.id.home:{
//		    case R.id.home:
//		    case R.id.homeAsUp:
	    	//case 16908332: //Algo pasa con la CyanogenMod
		    	if (mDrawerToggle.isDrawerIndicatorEnabled()) {
					mDrawerToggle.onOptionsItemSelected(item);
				}
		    	else {
		    		if (fbF != null){
		    			if (fbF.isVisible()){
		    				fbF.onBackPressed();
		    			}
		    		}
				}
		    	return true;
		    }
//	        case R.id.action_logout:{
//	        	logout(this, (MegaApplication)getApplication(), megaApi);
//	        	Toast.makeText(this,  "Temporal Logout Button Clicked!", Toast.LENGTH_SHORT).show();
//	            return true;
//	        }
	        case R.id.action_search:{
	        	mSearchView.setIconified(false);
	        	return true;
	        }
	        case R.id.action_new_folder:{
	        	if (drawerItem == DrawerItem.CLOUD_DRIVE){
	        		showNewFolderDialog(null);
	        	}
	        	else if (drawerItem == DrawerItem.CONTACTS){
	        		showNewContactDialog(null);
	        	}
	        	return true;
	        }
	        case R.id.action_more_options:{
	        	showMoreOptionsMenu();
	        	return true;
	        }
	        case R.id.action_upload:{
				uploadDialog = new UploadHereDialog();
				uploadDialog.show(getSupportFragmentManager(), "fragment_upload");
	        	return true;
	        }
            default:{
	            return super.onOptionsItemSelected(item);
            }
	    }
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		drawerItem = DrawerItem.values()[position];
		selectDrawerItem(DrawerItem.values()[position]);
	}

	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.menu_action_bar_grid:{
				
				if (fbF != null){
					if (fbF.isVisible()){
						Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("fbF");
						FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.detach(currentFragment);
						fragTransaction.commit();
						
						isListCloudDrive = !isListCloudDrive;
						fbF.setIsList(isListCloudDrive);
						fbF.setParentHandle(parentHandle);
						
						fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.attach(currentFragment);
						fragTransaction.commit();
						
						if (isListCloudDrive){
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_action_grid);
						}
						else{
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_action_list);
						}
					}
				}
				
				if (cF != null){
					if (cF.isVisible()){
						Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("cF");
						FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.detach(currentFragment);
						fragTransaction.commit();
						
						isListContacts = !isListContacts;
						cF.setIsList(isListContacts);
						
						fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.attach(currentFragment);
						fragTransaction.commit();
						
						if (isListContacts){
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_action_grid);
						}
						else{
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_action_list);
						}
					}
				}
				
				if (rbF != null){
					if (rbF.isVisible()){
						Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("rbF");
						FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.detach(currentFragment);
						fragTransaction.commit();
						
						isListRubbishBin = !isListRubbishBin;
						rbF.setIsList(isListRubbishBin);
						
						fragTransaction = getSupportFragmentManager().beginTransaction();
						fragTransaction.attach(currentFragment);
						fragTransaction.commit();
						
						if (isListRubbishBin){
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_action_grid);
						}
						else{
							ImageButton customListGrid = (ImageButton)getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
							customListGrid.setImageResource(R.drawable.ic_menu_action_list);
						}
					}
				}
				break;
			}
			case R.id.custom_search:{
				if (searchMenuItem != null) {
					MenuItemCompat.expandActionView(searchMenuItem);
				}
				else{
					Toast.makeText(this, "HOLA!", Toast.LENGTH_LONG).show();
				}
				break;
			}
			case R.id.top_control_bar:{
				Toast.makeText(this, "Link to \'My Account\' (top)", Toast.LENGTH_LONG).show();
				break;
			}
			case R.id.bottom_control_bar:{
				Toast.makeText(this, "Link to \'My Account\' (bottom)", Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
	
	private void showMoreOptionsMenu(){

		switch(drawerItem){
			case CLOUD_DRIVE:{
				CharSequence options[] = new CharSequence[] {"Refresh", "Sort by...", "Help", "Upgrade Account", "Logout"};
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setItems(options, new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    	switch(which){
					    	case 0:{
					    		Intent intent = new Intent(managerActivity, LoginActivity.class);
					    		intent.setAction(LoginActivity.ACTION_REFRESH);
					    		intent.putExtra("PARENT_HANDLE", parentHandle);
					    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
					    		break;
					    	}
					    	case 1:{
					    		Intent intent = new Intent(managerActivity, SortByDialogActivity.class);
					    		intent.setAction(SortByDialogActivity.ACTION_SORT_BY);
					    		startActivityForResult(intent, REQUEST_CODE_SORT_BY);
					    		break;
					    	}
					    	case 2:{
					    		Toast.makeText(managerActivity, "Help not yet implemented (refresh, sort by and logout are implemented)", Toast.LENGTH_SHORT).show();
					    		break;
					    	}
					    	case 3:{
					    		Toast.makeText(managerActivity, "Upgrade Account not yet implemented (refresh, sort by and logout are implemented)", Toast.LENGTH_SHORT).show();
					    		break;
					    	}			    	
					    	case 4:{
					    		logout(managerActivity, (MegaApplication)getApplication(), megaApi);
					    		break;
					    	}
				    	}
				    }
				});
				builder.show();
				break;
			}
			case CONTACTS:{
				CharSequence options[] = new CharSequence[] {"Refresh", "Sort by...", "Help", "Upgrade Account", "Logout"};
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setItems(options, new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    	switch(which){
					    	case 0:{
					    		Intent intent = new Intent(managerActivity, LoginActivity.class);
					    		intent.setAction(LoginActivity.ACTION_REFRESH);
					    		intent.putExtra("PARENT_HANDLE", parentHandle);
					    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
					    		break;
					    	}
					    	case 1:{
					    		Toast.makeText(managerActivity, "Sort by (in contacts) not yet implemented (it's implemented on CLOUD_DRIVE)", Toast.LENGTH_LONG).show();
					    		break;
					    	}
					    	case 2:{
					    		Toast.makeText(managerActivity, "Help not yet implemented (refresh and logout are implemented)", Toast.LENGTH_SHORT).show();
					    		break;
					    	}
					    	case 3:{
					    		Toast.makeText(managerActivity, "Upgrade Account not yet implemented (refresh and logout are implemented)", Toast.LENGTH_SHORT).show();
					    		break;
					    	}			    	
					    	case 4:{
					    		logout(managerActivity, (MegaApplication)getApplication(), megaApi);
					    		break;
					    	}
				    	}
				    }
				});
				builder.show();
				break;
			}
		}
	}
	
	/*
	 * Logout user
	 */
	static public void logout(Context context, MegaApplication app, MegaApiAndroid megaApi) {
//		context.stopService(new Intent(context, BackgroundService.class));
//		context.stopService(new Intent(context, CameraSyncService.class));

		Preferences.clearCredentials(context);
		megaApi.logout();
		drawerItem = null;
		
		if(managerActivity != null)
		{
			Intent intent = new Intent(managerActivity, TourActivity.class);
	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			managerActivity.startActivity(intent);
			managerActivity.finish();
			managerActivity = null;
		}
	}	
	

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout request start");
		}	
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fecthnodes request start");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			log("move request start");
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			log("remove request start");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			log("export request start");
		}
		else if(request.getType() == MegaRequest.TYPE_RENAME){
			log("rename request start");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("copy request start");
		}
		else if (request.getType() == MegaRequest.TYPE_MKDIR){
			log("create folder start");
		}
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout finished");
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fecthnodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, "Correctly moved to Rubbish bin", Toast.LENGTH_SHORT).show();
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
					}		
				}
				else{
					Toast.makeText(this, "The file has not been removed", Toast.LENGTH_LONG).show();
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, "Correctly moved", Toast.LENGTH_SHORT).show();
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
					}		
				}
				else{
					Toast.makeText(this, "The file has not been moved", Toast.LENGTH_LONG).show();
				}
				log("move nodes request finished");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Correctly deleted from MEGA", Toast.LENGTH_SHORT).show();
				if (fbF.isVisible()){
					NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
					fbF.setNodes(nodes);
					fbF.getListView().invalidateViews();
				}
			}
			else{
				Toast.makeText(this, "The file has not been removed", Toast.LENGTH_LONG).show();
			}
			log("remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				String link = request.getLink();
				if (managerActivity != null){
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_TEXT, link);
					startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
				}
			}
			else{
				Toast.makeText(this, "Impossible to get the link", Toast.LENGTH_LONG).show();
			}
			log("export request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Correctly renamed", Toast.LENGTH_SHORT).show();
				if (fbF.isVisible()){
					NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
					fbF.setNodes(nodes);
					fbF.getListView().invalidateViews();
				}
			}
			else{
				Toast.makeText(this, "The file has not been renamed", Toast.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Correctly copied", Toast.LENGTH_SHORT).show();
				if (fbF.isVisible()){
					NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
					fbF.setNodes(nodes);
					fbF.getListView().invalidateViews();
				}		
			}
			else{
				Toast.makeText(this, "The file has not been copied", Toast.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_MKDIR){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Folder created", Toast.LENGTH_LONG).show();
				if (fbF.isVisible()){
					NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
					fbF.setNodes(nodes);
					fbF.getListView().invalidateViews();
				}		
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			if (e.getErrorCode() == MegaError.API_OK){
				
				File avatar = new File(getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
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
							Bitmap circleBitmap = Bitmap.createBitmap(imBitmap.getWidth(), imBitmap.getHeight(), Bitmap.Config.ARGB_8888);
							
							BitmapShader shader = new BitmapShader (imBitmap,  TileMode.CLAMP, TileMode.CLAMP);
					        Paint paint = new Paint();
					        paint.setShader(shader);
					
					        Canvas c = new Canvas(circleBitmap);
					        int radius; 
					        if (imBitmap.getWidth() < imBitmap.getHeight())
					        	radius = imBitmap.getWidth()/2;
					        else
					        	radius = imBitmap.getHeight()/2;
					        
						    c.drawCircle(imBitmap.getWidth()/2, imBitmap.getHeight()/2, radius, paint);
					        imageProfile.setImageBitmap(circleBitmap);
						}
					}
				}
			}
			log("avatar user downloaded");
		}
		else if (request.getType() == MegaRequest.TYPE_ADD_CONTACT){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Contact added", Toast.LENGTH_LONG).show();
				if (cF.isVisible()){	
					UserList contacts = megaApi.getContacts();
					cF.setContacts(contacts);
					cF.getListView().invalidateViews();
				}
			}
			log("add contact");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,MegaError e) {

		if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout temporary error");
		}	
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fetchnodes temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			log("move temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			log("remove temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			log("export temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			log("rename temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("copy temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_MKDIR){
			log("create folder temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			log("get user attribute temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_ADD_CONTACT){
			log("add contact temporary error");
		}
	}
	
	public ActionBarDrawerToggle getmDrawerToggle() {
		return mDrawerToggle;
	}

	public void setmDrawerToggle(ActionBarDrawerToggle mDrawerToggle) {
		this.mDrawerToggle = mDrawerToggle;
	}
	
	File destination;
	
	public void onFileClick(ArrayList<Long> handleList){
		
		long[] hashes = new long[handleList.size()];
		for (int i=0;i<handleList.size();i++){
			hashes[i] = handleList.get(i);
		}
		
		Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
		intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
		intent.setClass(this, FileStorageActivity.class);
		intent.putExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES, hashes);
		startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
	}
	
	public void moveToTrash(ArrayList<Long> handleList){
		
		if (fbF.isVisible()){
			fbF.setPositionClicked(-1);
			fbF.notifyDataSetChanged();
		}
		
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
			temp.setMessage(getString(R.string.context_move_to_trash));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		
		MegaNode rubbishNode = megaApi.getRubbishNode();

		for (int i=0;i<handleList.size();i++){
			//Check if the node is not yet in the rubbish bin (if so, remove it)
			MegaNode parent = megaApi.getNodeByHandle(handleList.get(i));
			while (megaApi.getParentNode(parent) != null){
				parent = megaApi.getParentNode(parent);
			}
				
			if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
				moveToRubbish = true;
				megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)), rubbishNode, this);
			}
			else{
				megaApi.remove(megaApi.getNodeByHandle(handleList.get(i)), this);
			}
		}
	}
	
	public void getPublicLinkAndShareIt(MegaNode document){
		
		if (fbF.isVisible()){
			fbF.setPositionClicked(-1);
			fbF.notifyDataSetChanged();
		}
		
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
		
		megaApi.exportNode(document, this);
	}
	
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!fbF.isVisible()) {
					return;
				}
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}
	
	public void showNewContactDialog(String editText){
		if (cF.isVisible()){
			cF.setPositionClicked(-1);
			cF.notifyDataSetChanged();
		}
		
		String text;
		if ((editText == null) || editText.equals("")){
			text = getString(R.string.context_new_contact_name);
		}
		else{
			text = editText;
		}
		
		final EditText input = new EditText(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					addContact(value);
					addContactDialog.dismiss();
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_add),
				KeyEvent.KEYCODE_ENTER);
		input.setText(text);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.menu_add_contact),
				null, input);
		builder.setPositiveButton(getString(R.string.general_add),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						addContact(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		addContactDialog = builder.create();
		addContactDialog.show();
	}
	
	public void showNewFolderDialog(String editText){
		
		if (fbF.isVisible()){
			fbF.setPositionClicked(-1);
			fbF.notifyDataSetChanged();
		}
		
		String text;
		if (editText == null || editText.equals("")){
			text = getString(R.string.context_new_folder_name);
		}
		else{
			text = editText;
		}
		
		final EditText input = new EditText(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					createFolder(value);
					newFolderDialog.dismiss();
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_create),
				KeyEvent.KEYCODE_ENTER);
		input.setText(text);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.menu_new_folder),
				null, input);
		builder.setPositiveButton(getString(R.string.general_create),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						createFolder(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		newFolderDialog = builder.create();
		newFolderDialog.show();
	}
	
	private void addContact(String contactEmail){
		
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		statusDialog = null;
		try {
			statusDialog = new ProgressDialog(this);
			statusDialog.setMessage(getString(R.string.context_adding_contact));
			statusDialog.show();
		}
		catch(Exception e){
			return;
		}
		
		megaApi.addContact(contactEmail, this);
	}
	
	private void createFolder(String title) {
		
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		statusDialog = null;
		try {
			statusDialog = new ProgressDialog(this);
			statusDialog.setMessage(getString(R.string.context_creating_folder));
			statusDialog.show();
		}
		catch(Exception e){
			return;
		}
		
		long parentHandle;
		if (fbF.isVisible()){
			parentHandle = fbF.getParentHandle();
		}
		else{
			return;
		}
		
		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
		
		if (parentNode == null){
			parentNode = megaApi.getRootNode();
		}
		
		megaApi.createFolder(title, parentNode, this);
		
	}
	
	public void showRenameDialog(final MegaNode document, String text){
		
		if (fbF.isVisible()){
			fbF.setPositionClicked(-1);
			fbF.notifyDataSetChanged();
		}
		
		final EditText input = new EditText(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),
				KeyEvent.KEYCODE_ENTER);
		input.setText(text);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});

		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_rename) + " "	+ new String(document.getName()), null, input);
		builder.setPositiveButton(getString(R.string.context_rename),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						rename(document, value);
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
					rename(document, value);
					return true;
				}
				return false;
			}
		});
	}
	
	private void rename(MegaNode document, String newName){
		if (newName.equals(document.getName())) {
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
		
		log("renaming " + document.getName() + " to " + newName);
		
		megaApi.renameNode(document, newName, this);
	}
	
	public void showMove(ArrayList<Long> handleList){
		
		if (fbF.isVisible()){
			fbF.setPositionClicked(-1);
			fbF.notifyDataSetChanged();
		}
		
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}
	
	public void showCopy(ArrayList<Long> handleList){
		
		if (fbF.isVisible()){
			fbF.setPositionClicked(-1);
			fbF.notifyDataSetChanged();
		}
		
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}
	
	/*
	 * If there is an application that can manage the Intent, returns true. Otherwise, false.
	 */
	public static boolean isIntentAvailable(Context ctx, Intent intent) {

		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("Download started");
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		log("Download finished");
		if (e.getErrorCode() != MegaError.API_OK){
			
		}
		else{
			log("Dowload OK (" + transfer.getPath() + ")");
			String finalPath = null;
			try{
				finalPath = destination.getCanonicalPath();
			}
			catch(Exception ex){
				finalPath = destination.getAbsolutePath();
			}
			
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(destination), MimeType.typeForName(api.getNodeByHandle(transfer.getNodeHandle()).getName()).getType());
			if (isIntentAvailable(this, intent)){
				startActivity(intent);
			}
		}
		
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		log(transfer.getFileName() + "(" + (int)((transfer.getTransferredBytes()*100)/transfer.getTotalBytes()) + "%): " + transfer.getTransferredBytes() + "/" + transfer.getTotalBytes());
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (intent == null) {
			return;
		}
		if (requestCode == REQUEST_CODE_GET && resultCode == RESULT_OK) {
			Uri uri = intent.getData();
			intent.setAction(Intent.ACTION_GET_CONTENT);
			FilePrepareTask filePrepareTask = new FilePrepareTask(this);
			filePrepareTask.execute(intent);
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.upload_prepare));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
		} 
		else if (requestCode == REQUEST_CODE_GET_LOCAL && resultCode == RESULT_OK) {
			
			String folderPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			ArrayList<String> paths = intent.getStringArrayListExtra(FileStorageActivity.EXTRA_FILES);
			
			int i = 0;
			long parentHandle;
			if (fbF.isVisible()){
				parentHandle = fbF.getParentHandle();
			}
			else{
				return;
			}
			
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode == null){
				parentNode = megaApi.getRootNode();
			}
			
			for (String path : paths) {
				Toast.makeText(this, "Upload(" + i + "): " + path + " to MEGA." + parentNode.getName(), Toast.LENGTH_SHORT).show();
				Intent uploadServiceIntent = new Intent (this, UploadService.class);
				File file = new File (path);
				if (file.isDirectory()){
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, file.getName());
				}
				else{
					ShareInfo info = ShareInfo.infoFromFile(file);
					if (info == null){
						continue;
					}
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
					uploadServiceIntent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
				}
				
				uploadServiceIntent.putExtra(UploadService.EXTRA_FOLDERPATH, folderPath);
				uploadServiceIntent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				startService(uploadServiceIntent);				
				i++;
			}
			
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
		else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			double availableFreeSpace = Double.MAX_VALUE;
			try{
				StatFs stat = new StatFs(parentPath);
				availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
			}
			catch(Exception ex){}
			
			String url = intent.getStringExtra(FileStorageActivity.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivity.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES);
			
			if(hashes.length == 1){
				MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);
				if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
					String localPath = Util.getLocalFile(this, tempNode.getName(), tempNode.getSize(), parentPath);
					if(localPath != null){	
						try { 
							Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName())); 
						}
						catch(Exception e) {}
						
						Intent viewIntent = new Intent(Intent.ACTION_VIEW);
						viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeType.typeForName(tempNode.getName()).getType());
						if (isIntentAvailable(this, viewIntent))
							startActivity(viewIntent);
						else{
							Intent intentShare = new Intent(Intent.ACTION_SEND);
							intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeType.typeForName(tempNode.getName()).getType());
							if (isIntentAvailable(this, intentShare))
								startActivity(intentShare);
							String toastMessage = getString(R.string.already_downloaded) + ": " + localPath;
							Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
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
						
						if(availableFreeSpace <document.getSize()){
							Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, this);
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
						Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space), false, this);
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
			Util.showToast(this, R.string.download_began);
		}
		else if (requestCode == REQUEST_CODE_REFRESH && resultCode == RESULT_OK) {
			parentHandle = intent.getLongExtra("PARENT_HANDLE", -1);
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode != null){
				if (fbF != null){
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(parentNode, orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
					}
				}
			}
			else{
				if (fbF != null){
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
					}
				}
			}
		}
		else if (requestCode == REQUEST_CODE_SORT_BY && resultCode == RESULT_OK){
			orderGetChildren = intent.getIntExtra("ORDER_GET_CHILDREN", 0);
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode != null){
				if (fbF != null){
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(parentNode, orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
					}
				}
			}
			else{
				if (fbF != null){
					if (fbF.isVisible()){
						NodeList nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
						fbF.setNodes(nodes);
						fbF.getListView().invalidateViews();
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
		NodeList nodeList = megaApi.getChildren(parent, orderGetChildren);
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
	
	/*
	 * Background task to process files for uploading
	 */
	private class FilePrepareTask extends AsyncTask<Intent, Void, List<ShareInfo>> {
		Context context;
		
		FilePrepareTask(Context context){
			this.context = context;
		}
		
		@Override
		protected List<ShareInfo> doInBackground(Intent... params) {
			return ShareInfo.processIntent(params[0], context);
		}

		@Override
		protected void onPostExecute(List<ShareInfo> info) {
			filePreparedInfos = info;
			onIntentProcessed();
		}
	}
	
	/*
	 * Handle processed upload intent
	 */
	public void onIntentProcessed() {
		List<ShareInfo> infos = filePreparedInfos;
		if (statusDialog != null) {
			try { 
				statusDialog.dismiss(); 
			} 
			catch(Exception ex){}
		}
		
		long parentHandle = -1;
		if (fbF.isVisible()){
			parentHandle = fbF.getParentHandle();
		}
		
		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle); 
		if(parentNode == null){
			Util.showErrorAlertDialog(getString(R.string.error_temporary_unavaible), false, this);
			return;
		}
			
		if (infos == null) {
			Util.showErrorAlertDialog(getString(R.string.upload_can_not_open),
					false, this);
		} 
		else {
			Toast.makeText(getApplicationContext(), getString(R.string.upload_began),
					Toast.LENGTH_SHORT).show();
			for (ShareInfo info : infos) {
				Intent intent = new Intent(this, UploadService.class);
				intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
				intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
				intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
				startService(intent);
			}
		}
	}

	@Override
	public void onUsersUpdate(MegaApiJava api) {
		if (cF != null){
			if (cF.isVisible()){	
				UserList contacts = megaApi.getContacts();
				cF.setContacts(contacts);
				cF.getListView().invalidateViews();
			}
		}
	}

	@Override
	public void onNodesUpdate(MegaApiJava api) {
		if (fbF != null){
			if (fbF.isVisible()){
				NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
				fbF.setNodes(nodes);
				fbF.getListView().invalidateViews();
			}
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Fetch nodes from MEGA		
	}	
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
	}

	public static void log(String message) {
		Util.log("ManagerActivity", message);
	}
}
