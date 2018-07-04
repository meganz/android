package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.snackbarListeners.SnackbarNavigateOption;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaFolderInfo;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;


@SuppressLint("NewApi")
public class FileInfoActivityLollipop extends PinActivityLollipop implements OnClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface{

	public static int MAX_WIDTH_FILENAME_LAND=400;
	public static int MAX_WIDTH_FILENAME_LAND_2=400;

	public static int MAX_WIDTH_FILENAME_PORT=200;
	public static int MAX_WIDTH_FILENAME_PORT_2=200;

	static int TYPE_EXPORT_GET = 0;
	static int TYPE_EXPORT_REMOVE = 1;
	static int TYPE_EXPORT_MANAGE = 2;
	static int FROM_FILE_BROWSER = 13;
	static public int FROM_INCOMING_SHARES= 14;
	static public int FROM_INBOX= 16;

	boolean firstIncomingLevel=true;

    private android.support.v7.app.AlertDialog downloadConfirmationDialog;

    NodeController nC;

	ArrayList<MegaNode> nodeVersions;

	RelativeLayout iconToolbarLayout;
	ImageView iconToolbarView;
	ImageView iconToolbarViewLink;

	Drawable upArrow;
	Drawable drawableRemoveLink;
	Drawable drawableLink;
	Drawable drawableShare;
	Drawable drawableDots;

	RelativeLayout imageToolbarLayout;
	ImageView imageToolbarView;

	CoordinatorLayout fragmentContainer;
	CollapsingToolbarLayout collapsingToolbar;

	Toolbar toolbar;
	ActionBar aB;

	int accountType;

	private boolean isGetLink = false;

	float scaleText;

	RelativeLayout container;

	LinearLayout availableOfflineLayout;

	RelativeLayout sizeLayout;
	RelativeLayout locationLayout;
	RelativeLayout contentLayout;
	RelativeLayout addedLayout;
	RelativeLayout modifiedLayout;
	RelativeLayout publicLinkLayout;
	RelativeLayout publicLinkCopyLayout;
	TextView publicLinkText;
	ImageView shareIcon;
	ImageView infoIcon;
	TextView infoTittle;
	RelativeLayout sharedLayout;
	Button usersSharedWithTextButton;
	View dividerSharedLayout;

    RelativeLayout folderVersionsLayout;
    RelativeLayout folderCurrentVersionsLayout;
    RelativeLayout folderPreviousVersionsLayout;
    TextView folderVersionsText;
    TextView folderCurrentVersionsText;
    TextView folderPreviousVersionsText;

	TextView availableOfflineView;

	ImageView publicLinkIcon;
	Button publicLinkButton;

	RelativeLayout versionsLayout;
	Button versionsButton;
	View separatorVersions;

//	ImageView publicLinkImage;
	SwitchCompat offlineSwitch;

	TextView sizeTextView;
	TextView sizeTitleTextView;

    TextView locationTextView;
    TextView locationTitleTextView;

	TextView contentTextView;
	TextView contentTitleTextView;

	TextView addedTextView;
	TextView modifiedTextView;
	AppBarLayout appBarLayout;

	//RelativeLayout permissionsLayout;
	//TextView permissionLabel;
	TextView permissionInfo;
	//ImageView permissionsIcon;

	boolean owner= true;
	int typeExport = -1;

	ArrayList<MegaShare> sl;
	MegaOffline mOffDelete;

	RelativeLayout ownerLayout;
	LinearLayout ownerLinear;
	TextView ownerLabel;
	TextView ownerLabelowner;
	TextView ownerInfo;
	ImageView ownerRoundeImage;
	TextView ownerLetter;

	MenuItem downloadMenuItem;
	MenuItem shareMenuItem;
	MenuItem getLinkMenuItem;
	MenuItem editLinkMenuItem;
	MenuItem removeLinkMenuItem;
	MenuItem renameMenuItem;
	MenuItem moveMenuItem;
	MenuItem copyMenuItem;
	MenuItem rubbishMenuItem;
	MenuItem deleteMenuItem;
	MenuItem leaveMenuItem;

	MegaNode node;

	boolean availableOfflineBoolean = false;

	private MegaApiAndroid megaApi = null;
	MegaChatApiAndroid megaChatApi;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	public FileInfoActivityLollipop fileInfoActivity;

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
//	public static String DB_FILE = "0";
//	public static String DB_FOLDER = "1";

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

	String contactMail;

    private int adapterType;
 	private String path;
 	private File file;
 	private long fragmentHandle  = -1;
 	private String pathNavigation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		log("onCreate");

        fileInfoActivity = this;
        handler = new Handler();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            imageId = extras.getInt("imageId");
        }

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);

        if (scaleH < scaleW){
            scaleText = scaleH;
        }
        else{
            scaleText = scaleW;
        }

//		  dbH = new DatabaseHandler(getApplicationContext());
        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

        adapterType = getIntent().getIntExtra("adapterType", 0);
        path = getIntent().getStringExtra("path");

        setContentView(R.layout.activity_file_info);

        permissionInfo = (TextView) findViewById(R.id.file_properties_permission_info);
        permissionInfo.setVisibility(View.GONE);

        fragmentContainer = (CoordinatorLayout) findViewById(R.id.file_info_fragment_container);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        aB = getSupportActionBar();
        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.file_info_collapse_toolbar);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            collapsingToolbar.setExpandedTitleMarginBottom(Util.scaleHeightPx(60, outMetrics));
        }else{
            collapsingToolbar.setExpandedTitleMarginBottom(Util.scaleHeightPx(35, outMetrics));
        }
        collapsingToolbar.setExpandedTitleMarginStart((int) getResources().getDimension(R.dimen.recycler_view_separator));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.transparent_black));
        }

        iconToolbarLayout = (RelativeLayout) findViewById(R.id.file_info_icon_layout);

        iconToolbarView = (ImageView) findViewById(R.id.file_info_toolbar_icon);
        iconToolbarView.setImageResource(imageId);
        iconToolbarViewLink=(ImageView) findViewById(R.id.file_properties_public_link_image_title);

        imageToolbarLayout = (RelativeLayout) findViewById(R.id.file_info_image_layout);
        imageToolbarView = (ImageView) findViewById(R.id.file_info_toolbar_image);
        imageToolbarLayout.setVisibility(View.GONE);

        //Available Offline Layout

        availableOfflineLayout = (LinearLayout) findViewById(R.id.available_offline_layout);
        availableOfflineLayout.setVisibility(View.VISIBLE);

        availableOfflineView = (TextView) findViewById(R.id.file_properties_available_offline_text);

        offlineSwitch = (SwitchCompat) findViewById(R.id.file_properties_switch);

        //Share with Layout

        sharedLayout = (RelativeLayout) findViewById(R.id.file_properties_shared_layout);
        sharedLayout.setOnClickListener(this);

        shareIcon = (ImageView) findViewById(R.id.file_properties_shared_image);

        usersSharedWithTextButton = (Button) findViewById(R.id.file_properties_shared_info_button);
        usersSharedWithTextButton.setOnClickListener(this);

        dividerSharedLayout = (View) findViewById(R.id.divider_shared_layout);

        //Permissions Layout
        //permissionsLayout = (RelativeLayout) findViewById(R.id.file_properties_permissions_layout);
        //permissionsLayout.setVisibility(View.GONE);


        //permissionsIcon = (ImageView) findViewById(R.id.file_properties_permissions_image);

        //permissionLabel = (TextView) findViewById(R.id.file_properties_permission_label);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

        //Owner Layout
        ownerLayout = (RelativeLayout) findViewById(R.id.file_properties_owner_layout);
        ownerRoundeImage= (RoundedImageView) findViewById(R.id.contact_list_thumbnail);
        ownerLetter = (TextView) findViewById(R.id.contact_list_initial_letter);

        ownerLinear = (LinearLayout) findViewById(R.id.file_properties_owner_linear);
        ownerLabel =  (TextView) findViewById(R.id.file_properties_owner_label);
        ownerLabelowner = (TextView) findViewById(R.id.file_properties_owner_label_owner);
        String ownerString = "("+getString(R.string.file_properties_owner)+")";
        ownerLabelowner.setText(ownerString);
        ownerInfo = (TextView) findViewById(R.id.file_properties_owner_info);


        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            log("Landscape configuration");
            float width1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, getResources().getDisplayMetrics());
            float width2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND_2, getResources().getDisplayMetrics());

            ownerLabel.setMaxWidth((int) width1);
            ownerInfo.setMaxWidth((int) width2);

        }
        else{
            log("Portrait configuration");
            float width1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, getResources().getDisplayMetrics());
            float width2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT_2, getResources().getDisplayMetrics());

            ownerLabel.setMaxWidth((int) width1);
            ownerInfo.setMaxWidth((int) width2);
        }

        ownerLayout.setVisibility(View.GONE);

        //Info Layout
        infoTittle = (TextView) findViewById(R.id.file_properties_info_menu_info);

        infoIcon = (ImageView) findViewById(R.id.file_properties_info_image);

        //Size Layout
        sizeLayout = (RelativeLayout) findViewById(R.id.file_properties_size_layout);
        sizeTitleTextView  = (TextView) findViewById(R.id.file_properties_info_menu_size);
        sizeTextView = (TextView) findViewById(R.id.file_properties_info_data_size);

        //Folder Versions Layout
        folderVersionsLayout = (RelativeLayout) findViewById(R.id.file_properties_folder_versions_layout);
        folderVersionsText = (TextView) findViewById(R.id.file_properties_info_data_folder_versions);
        folderVersionsLayout.setVisibility(View.GONE);

        folderCurrentVersionsLayout = (RelativeLayout) findViewById(R.id.file_properties_folder_current_versions_layout);
        folderCurrentVersionsText = (TextView) findViewById(R.id.file_properties_info_data_folder_current_versions);
        folderCurrentVersionsLayout.setVisibility(View.GONE);

        folderPreviousVersionsLayout = (RelativeLayout) findViewById(R.id.file_properties_folder_previous_versions_layout);
        folderPreviousVersionsText = (TextView) findViewById(R.id.file_properties_info_data_folder_previous_versions);
        folderPreviousVersionsLayout.setVisibility(View.GONE);

        //Location Layout
        locationLayout = (RelativeLayout) findViewById(R.id.file_properties_location_layout);
        locationTitleTextView  = (TextView) findViewById(R.id.file_properties_info_menu_location);
        locationTextView = (TextView) findViewById(R.id.file_properties_info_data_location);
        locationTextView.setOnClickListener(this);

        //Content Layout
        contentLayout = (RelativeLayout) findViewById(R.id.file_properties_content_layout);
        contentTitleTextView  = (TextView) findViewById(R.id.file_properties_info_menu_content);
        contentTextView = (TextView) findViewById(R.id.file_properties_info_data_content);

        publicLinkLayout = (RelativeLayout) findViewById(R.id.file_properties_link_layout);
        publicLinkCopyLayout = (RelativeLayout) findViewById(R.id.file_properties_copy_layout);

        publicLinkText = (TextView) findViewById(R.id.file_properties_link_text);
        publicLinkIcon = (ImageView) findViewById(R.id.file_properties_public_link_image);
        publicLinkButton = (Button) findViewById(R.id.file_properties_link_button);
        publicLinkButton.setText(getString(R.string.context_copy));
        publicLinkButton.setOnClickListener(this);

        //Added Layout
        addedLayout = (RelativeLayout) findViewById(R.id.file_properties_added_layout);
        addedTextView = (TextView) findViewById(R.id.file_properties_info_data_added);

        //Modified Layout
        modifiedLayout = (RelativeLayout) findViewById(R.id.file_properties_modified_layout);
        modifiedTextView = (TextView) findViewById(R.id.file_properties_info_data_modified);

        //Versions Layout
        versionsLayout = (RelativeLayout) findViewById(R.id.file_properties_versions_layout);
        versionsButton = (Button) findViewById(R.id.file_properties_text_number_versions);
        separatorVersions = (View) findViewById(R.id.separator_versions);

        if (adapterType == Constants.OFFLINE_ADAPTER){
            iconToolbarViewLink.setVisibility(View.GONE);
            collapsingToolbar.setTitle(getIntent().getStringExtra("name"));
            availableOfflineLayout.setVisibility(View.GONE);
            sharedLayout.setVisibility(View.GONE);
            dividerSharedLayout.setVisibility(View.GONE);
            publicLinkLayout.setVisibility(View.GONE);
            publicLinkCopyLayout.setVisibility(View.GONE);
            contentLayout.setVisibility(View.GONE);
            addedLayout.setVisibility(View.GONE);
            modifiedLayout.setVisibility(View.GONE);
            versionsLayout.setVisibility(View.GONE);
            separatorVersions.setVisibility(View.GONE);

            if (path != null){
                log("Path no NULL");
                file = new File (path);
                sizeTextView.setText(Util.getSizeString(file.length()));
                String location = file.getParentFile().getName();
                if (location.equals("in")){
                    locationTextView.setText(getResources().getString(R.string.section_saved_for_offline));
                }
                else {
                    String offlineLocation = file.getParentFile().getParentFile().getName() + '/' + location;
                    if (offlineLocation.equals(Util.offlineDIR)){
                        locationTextView.setText(getResources().getString(R.string.section_saved_for_offline));
                    }
                    else {
                        locationTextView.setText(location + " ("+ getResources().getString(R.string.section_saved_for_offline) +")");
                    }
                }
                log("Path: "+file.getAbsolutePath()+ " size: "+file.length());
            }
            else {
                log("Path is NULL");
            }
            pathNavigation = getIntent().getStringExtra("pathNavigation");
        }
        else {
            if (megaApi == null){
                MegaApplication app = (MegaApplication)getApplication();
                megaApi = app.getMegaApi();
            }
            if(megaApi==null||megaApi.getRootNode()==null){
                log("Refresh session - sdk");
                Intent intent = new Intent(this, LoginActivityLollipop.class);
                intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return;
            }
            if(Util.isChatEnabled()) {
                if (megaChatApi == null) {
                    megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                }

                if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
                    log("Refresh session - karere");
                    Intent intent = new Intent(this, LoginActivityLollipop.class);
                    intent.putExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return;
                }
            }

            megaApi.addGlobalListener(this);

            if (extras != null){
                from = extras.getInt("from");
                if(from==FROM_INCOMING_SHARES){
                    firstIncomingLevel = extras.getBoolean("firstLevel");
                }
                accountType = extras.getInt("typeAccount", MegaAccountDetails.ACCOUNT_TYPE_FREE);
                long handleNode = extras.getLong("handle", -1);
                log("Handle of the selected node: "+handleNode);
                node = megaApi.getNodeByHandle(handleNode);
                if (node == null){
                    log("Node is NULL");
                    finish();
                    return;
                }

                String name = node.getName();

                collapsingToolbar.setTitle(name);

                MegaNode parent = megaApi.getNodeByHandle(node.getHandle());
                while (megaApi.getParentNode(parent) != null){
                    parent = megaApi.getParentNode(parent);
                }
                if (from == FROM_INCOMING_SHARES){
                    fragmentHandle = -1;
                    if (megaApi.getParentNode(node) != null){
                        locationTextView.setText(megaApi.getParentNode(node).getName()+" ("+ getResources().getString(R.string.title_incoming_shares_explorer) +")");
                    }
                    else {
                        locationTextView.setText(getResources().getString(R.string.title_incoming_shares_explorer));
                    }
                }
                else{
                    if (parent.getHandle() == megaApi.getRootNode().getHandle()){
                        fragmentHandle = megaApi.getRootNode().getHandle();
                    }
                    else if (parent.getHandle() == megaApi.getRubbishNode().getHandle()){
                        fragmentHandle = megaApi.getRubbishNode().getHandle();
                    }
                    else if (parent.getHandle() == megaApi.getInboxNode().getHandle()){
                        fragmentHandle = megaApi.getInboxNode().getHandle();
                    }

                    if (megaApi.getParentNode(node) == null){ // It is because of the parent node is Incoming Shares
                        locationTextView.setText(getResources().getString(R.string.title_incoming_shares_explorer));
                    }
                    else {
                        if (parent.getHandle() == megaApi.getRootNode().getHandle() ||
                                parent.getHandle() == megaApi.getRubbishNode().getHandle() ||
                                parent.getHandle() == megaApi.getInboxNode().getHandle()){
                            if (megaApi.getParentNode(node).getHandle() == parent.getHandle()){
                                locationTextView.setText(megaApi.getParentNode(node).getName());
                            }
                            else {
                                locationTextView.setText(megaApi.getParentNode(node).getName()+" ("+ parent.getName() +")");
                            }
                        }
                        else {
                            locationTextView.setText(megaApi.getParentNode(node).getName()+" ("+ getResources().getString(R.string.title_incoming_shares_explorer) +")");
                        }
                    }
                }

                if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
                    offlineSwitch.setEnabled(true);
                    offlineSwitch.setOnClickListener(this);
                    availableOfflineView.setTextColor(getResources().getColor(R.color.name_my_account));
                }else{
                    offlineSwitch.setEnabled(false);
                    availableOfflineView.setTextColor(getResources().getColor(R.color.invite_button_deactivated));

                }

                if(megaApi.hasVersions(node)){
                    versionsLayout.setVisibility(View.VISIBLE);

                    String text = getResources().getQuantityString(R.plurals.number_of_versions, megaApi.getNumVersions(node), megaApi.getNumVersions(node));
                    versionsButton.setText(text);
                    versionsButton.setOnClickListener(this);
                    separatorVersions.setVisibility(View.VISIBLE);

                    nodeVersions = megaApi.getVersions(node);
                }
                else{
                    versionsLayout.setVisibility(View.GONE);
                    separatorVersions.setVisibility(View.GONE);
                }

            }
            else{
                log("Extras is NULL");
            }

            refreshProperties();
            supportInvalidateOptionsMenu();

            ((MegaApplication) getApplication()).sendSignalPresenceActivity();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		drawableDots = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_dots_vertical_white);
		drawableDots = drawableDots.mutate();
		upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_arrow_back_white);
		upArrow = upArrow.mutate();

		drawableRemoveLink = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_remove_link_w);
		drawableRemoveLink = drawableRemoveLink.mutate();
		drawableLink = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_link_white);
		drawableLink = drawableLink.mutate();
		drawableShare = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_share_white);
		drawableShare = drawableShare.mutate();

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.file_info_action, menu);

		downloadMenuItem = menu.findItem(R.id.cab_menu_file_info_download);
		shareMenuItem = menu.findItem(R.id.cab_menu_file_info_share_folder);
		getLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_get_link);
		editLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_edit_link);
		removeLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_remove_link);
		renameMenuItem = menu.findItem(R.id.cab_menu_file_info_rename);
		moveMenuItem = menu.findItem(R.id.cab_menu_file_info_move);
		copyMenuItem = menu.findItem(R.id.cab_menu_file_info_copy);
		rubbishMenuItem = menu.findItem(R.id.cab_menu_file_info_rubbish);
		deleteMenuItem = menu.findItem(R.id.cab_menu_file_info_delete);
		leaveMenuItem = menu.findItem(R.id.cab_menu_file_info_leave);


		if (adapterType == Constants.OFFLINE_ADAPTER){
            downloadMenuItem.setVisible(false);
            shareMenuItem.setVisible(false);
            getLinkMenuItem.setVisible(false);
            editLinkMenuItem.setVisible(false);
            removeLinkMenuItem.setVisible(false);
            renameMenuItem.setVisible(false);
            moveMenuItem.setVisible(false);
            copyMenuItem.setVisible(false);
            rubbishMenuItem.setVisible(false);
            deleteMenuItem.setVisible(false);
            leaveMenuItem.setVisible(false);
        }
        else {
            MegaNode parent = megaApi.getNodeByHandle(node.getHandle());
            if(parent != null) {

                while (megaApi.getParentNode(parent) != null) {
                    parent = megaApi.getParentNode(parent);

                }
                if (parent.getHandle() == megaApi.getRubbishNode().getHandle()) {
                    downloadMenuItem.setVisible(false);
                    shareMenuItem.setVisible(false);
                    getLinkMenuItem.setVisible(false);
                    editLinkMenuItem.setVisible(false);
                    removeLinkMenuItem.setVisible(false);
                    renameMenuItem.setVisible(false);
                    moveMenuItem.setVisible(true);
                    copyMenuItem.setVisible(false);
                    rubbishMenuItem.setVisible(false);
                    deleteMenuItem.setVisible(true);
                    leaveMenuItem.setVisible(false);
                } else {

                    if (node.isExported()) {
                        getLinkMenuItem.setVisible(false);
                        menu.findItem(R.id.cab_menu_file_info_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                        editLinkMenuItem.setVisible(true);
                        removeLinkMenuItem.setVisible(true);
                        menu.findItem(R.id.cab_menu_file_info_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    } else {

                        getLinkMenuItem.setVisible(true);
                        menu.findItem(R.id.cab_menu_file_info_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                        editLinkMenuItem.setVisible(false);
                        removeLinkMenuItem.setVisible(false);
                        menu.findItem(R.id.cab_menu_file_info_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    }

                    if (from == FROM_INCOMING_SHARES) {

                        downloadMenuItem.setVisible(true);

                        shareMenuItem.setVisible(false);
                        menu.findItem(R.id.cab_menu_file_info_share_folder).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                        deleteMenuItem.setVisible(false);

                        if (firstIncomingLevel) {
                            leaveMenuItem.setVisible(true);
                            menu.findItem(R.id.cab_menu_file_info_leave).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                        } else {
                            leaveMenuItem.setVisible(false);
                            menu.findItem(R.id.cab_menu_file_info_leave).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                        }

                        int accessLevel = megaApi.getAccess(node);
                        log("Node: " + node.getName());

                        switch (accessLevel) {

                            case MegaShare.ACCESS_OWNER:
                            case MegaShare.ACCESS_FULL: {
                                if (firstIncomingLevel) {
                                    rubbishMenuItem.setVisible(false);
                                } else {
                                    rubbishMenuItem.setVisible(true);
                                }
                                renameMenuItem.setVisible(true);
                                moveMenuItem.setVisible(false);
                                copyMenuItem.setVisible(true);
                                menu.findItem(R.id.cab_menu_file_info_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                                getLinkMenuItem.setVisible(false);
                                menu.findItem(R.id.cab_menu_file_info_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                                editLinkMenuItem.setVisible(false);
                                removeLinkMenuItem.setVisible(false);
                                menu.findItem(R.id.cab_menu_file_info_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                                break;
                            }
                            case MegaShare.ACCESS_READ: {
                                renameMenuItem.setVisible(false);
                                moveMenuItem.setVisible(false);
                                copyMenuItem.setVisible(true);
                                menu.findItem(R.id.cab_menu_file_info_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                                rubbishMenuItem.setVisible(false);

                                getLinkMenuItem.setVisible(false);
                                menu.findItem(R.id.cab_menu_file_info_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                                editLinkMenuItem.setVisible(false);
                                removeLinkMenuItem.setVisible(false);
                                menu.findItem(R.id.cab_menu_file_info_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                                break;
                            }
                            case MegaShare.ACCESS_READWRITE: {
                                renameMenuItem.setVisible(false);
                                moveMenuItem.setVisible(false);
                                copyMenuItem.setVisible(true);
                                menu.findItem(R.id.cab_menu_file_info_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                                rubbishMenuItem.setVisible(false);

                                getLinkMenuItem.setVisible(false);
                                menu.findItem(R.id.cab_menu_file_info_get_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                                editLinkMenuItem.setVisible(false);
                                removeLinkMenuItem.setVisible(false);
                                menu.findItem(R.id.cab_menu_file_info_remove_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                                break;
                            }
                        }
                    } else {
                        downloadMenuItem.setVisible(true);

                        if (node.isFolder()) {
                            shareMenuItem.setVisible(true);
                            menu.findItem(R.id.cab_menu_file_info_share_folder).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                        } else {
                            shareMenuItem.setVisible(false);
                            menu.findItem(R.id.cab_menu_file_info_share_folder).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                        }

                        rubbishMenuItem.setVisible(true);
                        deleteMenuItem.setVisible(false);
                        leaveMenuItem.setVisible(false);
                        menu.findItem(R.id.cab_menu_file_info_leave).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                        renameMenuItem.setVisible(true);
                        moveMenuItem.setVisible(true);
                        copyMenuItem.setVisible(true);
                    }
                }

                if (node.hasPreview() || node.hasThumbnail()) {

                    upArrow.setColorFilter(ContextCompat.getColor(fileInfoActivity, R.color.white), PorterDuff.Mode.SRC_ATOP);
                    getSupportActionBar().setHomeAsUpIndicator(upArrow);

                    drawableDots.setColorFilter(ContextCompat.getColor(fileInfoActivity, R.color.white), PorterDuff.Mode.SRC_ATOP);
                    toolbar.setOverflowIcon(drawableDots);

                    if (removeLinkMenuItem != null) {
                        drawableRemoveLink.setColorFilter(ContextCompat.getColor(fileInfoActivity, R.color.white), PorterDuff.Mode.SRC_ATOP);
                        removeLinkMenuItem.setIcon(drawableRemoveLink);
                    }
                    if (getLinkMenuItem != null) {
                        drawableLink.setColorFilter(ContextCompat.getColor(fileInfoActivity, R.color.white), PorterDuff.Mode.SRC_ATOP);
                        getLinkMenuItem.setIcon(drawableLink);
                    }

                    collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));
                    collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.white));
                    collapsingToolbar.setStatusBarScrimColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));


                    if (iconToolbarViewLink.getVisibility() == View.VISIBLE) {
                        iconToolbarViewLink.setColorFilter(ContextCompat.getColor(fileInfoActivity, R.color.white), PorterDuff.Mode.SRC_ATOP);
                        iconToolbarViewLink.setAlpha(0.8f);
                    }
                }
			/*Folder*/
                else {

                    appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                        @Override
                        public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
                            if (offset < -200) {
                                upArrow.setColorFilter(ContextCompat.getColor(fileInfoActivity, R.color.white), PorterDuff.Mode.SRC_ATOP);
                                getSupportActionBar().setHomeAsUpIndicator(upArrow);
                                drawableDots.setColorFilter(ContextCompat.getColor(fileInfoActivity, R.color.white), PorterDuff.Mode.SRC_ATOP);
                                toolbar.setOverflowIcon(drawableDots);
                            } else {
                                upArrow.setColorFilter(ContextCompat.getColor(fileInfoActivity, R.color.white), PorterDuff.Mode.SRC_ATOP);
                                getSupportActionBar().setHomeAsUpIndicator(upArrow);
                                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                                drawableDots.setColorFilter(ContextCompat.getColor(fileInfoActivity, R.color.white), PorterDuff.Mode.SRC_ATOP);
                                toolbar.setOverflowIcon(drawableDots);
                            }
                        }
                    });

                    collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));
                    collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.name_my_account));
                }
            }
        }

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		int id = item.getItemId();
		switch (id) {
			case android.R.id.home: {
				finish();
				break;
			}
			case R.id.cab_menu_file_info_download: {
				if (!availableOfflineBoolean){
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(node.getHandle());
                    if(nC==null){
                        nC = new NodeController(this);
                    }
                    nC.prepareForDownload(handleList);
				}
				else{

					File destination = null;
					File offlineFile = null;
					if (Environment.getExternalStorageDirectory() != null){
						destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/"+MegaApiUtils.createStringTree(node, this));
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

					try {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							intent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", offlineFile), MimeTypeList.typeForName(offlineFile.getName()).getType());
						} else {
							intent.setDataAndType(Uri.fromFile(offlineFile), MimeTypeList.typeForName(offlineFile.getName()).getType());
						}
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						if (MegaApiUtils.isIntentAvailable(this, intent)) {
							startActivity(intent);
						} else {
							Snackbar.make(fragmentContainer, getString(R.string.intent_not_available), Snackbar.LENGTH_LONG).show();
						}
					}
					catch(Exception e){
						Snackbar.make(fragmentContainer, getString(R.string.intent_not_available), Snackbar.LENGTH_LONG).show();
					}
				}
				break;
			}
			case R.id.cab_menu_file_info_share_folder: {
				Intent intent = new Intent(AddContactActivityLollipop.ACTION_PICK_CONTACT_SHARE_FOLDER);
				intent.setClass(this, AddContactActivityLollipop.class);
				intent.putExtra("contactType", Constants.CONTACT_TYPE_BOTH);
				intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
				startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
				break;
			}
			case R.id.cab_menu_file_info_get_link:
			case R.id.cab_menu_file_info_edit_link:{
				showGetLinkActivity(node.getHandle());
				break;
			}
			case R.id.cab_menu_file_info_remove_link: {
				shareIt = false;
				AlertDialog removeLinkDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);


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


				if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){

					removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (10*scaleW));

				}else{
					removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (15*scaleW));

				}

				builder.setView(dialoglayout);

				builder.setPositiveButton(getString(R.string.context_remove), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						typeExport=TYPE_EXPORT_REMOVE;
						megaApi.disableExport(node, fileInfoActivity);
					}
				});

				builder.setNegativeButton(getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});

				removeLinkDialog = builder.create();
				removeLinkDialog.show();
				break;
			}
			case R.id.cab_menu_file_info_copy: {
				showCopy();
				break;
			}
			case R.id.cab_menu_file_info_move: {
				showMove();
				break;
			}
			case R.id.cab_menu_file_info_rename: {
				showRenameDialog();
				break;
			}
			case R.id.cab_menu_file_info_leave: {
				leaveIncomingShare();
				break;
			}
			case R.id.cab_menu_file_info_rubbish:
			case R.id.cab_menu_file_info_delete: {
				moveToTrash();
				break;
			}
		}
		return true;
	}

	private void refreshProperties(){
		log("refreshProperties");

		if(node==null){
			finish();
		}

		boolean result=true;

		if(node.isExported()){
			publicLink=true;
			publicLinkLayout.setVisibility(View.VISIBLE);
			publicLinkCopyLayout.setVisibility(View.VISIBLE);
			iconToolbarViewLink.setVisibility(View.VISIBLE);
			publicLinkText.setText(node.getPublicLink());
		}
		else{
			publicLink=false;
			publicLinkLayout.setVisibility(View.GONE);
			publicLinkCopyLayout.setVisibility(View.GONE);
			iconToolbarViewLink.setVisibility(View.GONE);
		}

		if (node.isFile()){
			log("onCreate node is FILE");
			sharedLayout.setVisibility(View.GONE);
			dividerSharedLayout.setVisibility(View.GONE);
			sizeTitleTextView.setText(getString(R.string.file_properties_info_size_file));

			sizeTextView.setText(Formatter.formatFileSize(this, node.getSize()));

			contentLayout.setVisibility(View.GONE);

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
				imageToolbarView.setImageBitmap(thumb);
				imageToolbarLayout.setVisibility(View.VISIBLE);
				iconToolbarLayout.setVisibility(View.GONE);
			}
			else{
				thumb = ThumbnailUtils.getThumbnailFromFolder(node, this);
				if (thumb != null){
					imageToolbarView.setImageBitmap(thumb);
					imageToolbarLayout.setVisibility(View.VISIBLE);
					iconToolbarLayout.setVisibility(View.GONE);
				}
			}
			preview = PreviewUtils.getPreviewFromCache(node);
			if (preview != null){
				PreviewUtils.previewCache.put(node.getHandle(), preview);
				imageToolbarView.setImageBitmap(preview);
				imageToolbarLayout.setVisibility(View.VISIBLE);
				iconToolbarLayout.setVisibility(View.GONE);
			}
			else{
				preview = PreviewUtils.getPreviewFromFolder(node, this);
				if (preview != null){
					PreviewUtils.previewCache.put(node.getHandle(), preview);
					imageToolbarView.setImageBitmap(preview);
					imageToolbarLayout.setVisibility(View.VISIBLE);
					iconToolbarLayout.setVisibility(View.GONE);
				}
				else{
					if (node.hasPreview()){
						File previewFile = new File(PreviewUtils.getPreviewFolder(this), node.getBase64Handle()+".jpg");
						megaApi.getPreview(node, previewFile.getAbsolutePath(), this);
					}
				}
			}

			if(megaApi.hasVersions(node)){
				versionsLayout.setVisibility(View.VISIBLE);
                String text = getResources().getQuantityString(R.plurals.number_of_versions, megaApi.getNumVersions(node), megaApi.getNumVersions(node));
                versionsButton.setText(text);
				versionsButton.setOnClickListener(this);
				separatorVersions.setVisibility(View.VISIBLE);

				nodeVersions = megaApi.getVersions(node);
			}
			else{
				versionsLayout.setVisibility(View.GONE);
				separatorVersions.setVisibility(View.GONE);
			}
		}
		else{ //Folder

            megaApi.getFolderInfo(node, this);
			contentTextView.setVisibility(View.VISIBLE);
			contentTitleTextView.setVisibility(View.VISIBLE);

			contentTextView.setText(MegaApiUtils.getInfoFolder(node, this));

			long sizeFile=megaApi.getSize(node);
			sizeTextView.setText(Formatter.formatFileSize(this, sizeFile));

			iconToolbarView.setImageResource(imageId);

			if(from==FROM_INCOMING_SHARES){
				//Show who is the owner
				ownerRoundeImage.setImageBitmap(null);
				ownerLetter.setText("");

				ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
				for(int j=0; j<sharesIncoming.size();j++){
					MegaShare mS = sharesIncoming.get(j);
					if(mS.getNodeHandle()==node.getHandle()){
						MegaUser user= megaApi.getContact(mS.getUser());
						contactMail=user.getEmail();
						if(user!=null){
							MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));

							if(contactDB!=null){
								if(!contactDB.getName().equals("")){
									ownerLabel.setText(contactDB.getName()+" "+contactDB.getLastName());
									ownerInfo.setText(user.getEmail());
									createDefaultAvatar(ownerRoundeImage, user, contactDB.getName());
								}
								else{
									ownerLabel.setText(user.getEmail());
									ownerInfo.setText(user.getEmail());
									createDefaultAvatar(ownerRoundeImage, user, user.getEmail());
								}
							}
							else{
								log("The contactDB is null: ");
								ownerLabel.setText(user.getEmail());
								ownerInfo.setText(user.getEmail());
								createDefaultAvatar(ownerRoundeImage, user, user.getEmail());
							}
						}
						else{
							ownerLabel.setText(mS.getUser());
							ownerInfo.setText(mS.getUser());
							createDefaultAvatar(ownerRoundeImage, user, mS.getUser());
						}


						File avatar = null;
						if (this.getExternalCacheDir() != null){
							avatar = new File(this.getExternalCacheDir().getAbsolutePath(), contactMail + ".jpg");
						}
						else{
							avatar = new File(this.getCacheDir().getAbsolutePath(), contactMail + ".jpg");
						}

						Bitmap bitmap = null;
						if (avatar.exists()){
							if (avatar.length() > 0){
								BitmapFactory.Options bOpts = new BitmapFactory.Options();
								bOpts.inPurgeable = true;
								bOpts.inInputShareable = true;
								bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
								if (bitmap == null) {
									avatar.delete();
									if (this.getExternalCacheDir() != null){
										megaApi.getUserAvatar(user,this.getExternalCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", this);
									}
									else{
										megaApi.getUserAvatar(user,this.getCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", this);
									}
								}
								else{
									ownerLetter.setVisibility(View.GONE);
									ownerRoundeImage.setImageBitmap(bitmap);
								}
							}
							else{
								if (this.getExternalCacheDir() != null){
									megaApi.getUserAvatar(user,this.getExternalCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", this);
								}
								else{
									megaApi.getUserAvatar(user, this.getCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", this);
								}
							}
						}
						else{
							if (this.getExternalCacheDir() != null){
								megaApi.getUserAvatar(user, this.getExternalCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", this);
							}
							else{
								megaApi.getUserAvatar(user, this.getCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", this);
							}
						}
						ownerLayout.setVisibility(View.VISIBLE);
					}
				}
			}


			sl = megaApi.getOutShares(node);

			if (sl != null){

				if (sl.size() == 0){

					sharedLayout.setVisibility(View.GONE);
					dividerSharedLayout.setVisibility(View.GONE);
//					If I am the owner
					if (megaApi.checkAccess(node, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK){

//						permissionLabel.setVisibility(View.GONE);
//						permissionInfo.setVisibility(View.GONE);
						//permissionsLayout.setVisibility(View.GONE);
						permissionInfo.setVisibility(View.GONE);
						//permissionInfo.setText(getResources().getString(R.string.file_properties_owner));

					}
					else{

						owner = false;
						//If I am not the owner
						//permissionsLayout.setVisibility(View.VISIBLE);
						permissionInfo.setVisibility(View.VISIBLE);
//						permissionLabel.setVisibility(View.VISIBLE);
//						permissionInfo.setVisibility(View.VISIBLE);

						int accessLevel= megaApi.getAccess(node);
						log("Node: "+node.getName());

						switch(accessLevel){
							case MegaShare.ACCESS_OWNER:
							case MegaShare.ACCESS_FULL:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_full_access).toUpperCase(Locale.getDefault()));

								//permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess);
								break;
							}
							case MegaShare.ACCESS_READ:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_only).toUpperCase(Locale.getDefault()));
								//permissionsIcon.setImageResource(R.drawable.ic_shared_read);
								break;
							}
							case MegaShare.ACCESS_READWRITE:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_write).toUpperCase(Locale.getDefault()));
								//permissionsIcon.setImageResource(R.drawable.ic_shared_read_write);
								break;
							}
						}
					}
				}
				else{
					sharedLayout.setVisibility(View.VISIBLE);
					dividerSharedLayout.setVisibility(View.VISIBLE);
					usersSharedWithTextButton.setText(sl.size()+" "+getResources().getQuantityString(R.plurals.general_num_users,sl.size()));

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

		//Choose the button offlineSwitch

		File offlineFile = null;

		if(dbH.exists(node.getHandle())) {
			log("Exists OFFLINE in the DB!!!");

			MegaOffline offlineNode = dbH.findByHandle(node.getHandle());
			if (offlineNode != null) {
				log("YESS FOUND: " + node.getName());
				if (from == FROM_INCOMING_SHARES) {
					log("FROM_INCOMING_SHARES");
					//Find in the filesystem
					if (Environment.getExternalStorageDirectory() != null) {
						offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + offlineNode.getHandleIncoming() + offlineNode.getPath()+ "/" + node.getName());
						log("offline File INCOMING: " + offlineFile.getAbsolutePath());
					} else {
						offlineFile = this.getFilesDir();
					}

				}
				else if(from==FROM_INBOX){

					if (Environment.getExternalStorageDirectory() != null) {
						offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/" + offlineNode.getPath()+ "/" + node.getName());
						log("offline File INCOMING: " + offlineFile.getAbsolutePath());
					} else {
						offlineFile = this.getFilesDir();
					}
				}
				else {
					log("NOT INCOMING NEITHER INBOX");
					//Find in the filesystem
					if (Environment.getExternalStorageDirectory() != null) {
						offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + megaApi.getNodePath(node));
						log("offline File: " + offlineFile.getAbsolutePath());
					} else {
						offlineFile = this.getFilesDir();
					}
				}

				if (offlineFile != null) {
					if (offlineFile.exists()) {
						log("FOUND!!!: " + node.getHandle() + " " + node.getName());
						availableOfflineBoolean = true;
						offlineSwitch.setChecked(true);
					} else {
						log("Not found: " + node.getHandle() + " " + node.getName());
						availableOfflineBoolean = false;
						offlineSwitch.setChecked(false);
					}
				} else {
					log("Not found offLineFile is NULL");
					availableOfflineBoolean = false;
					offlineSwitch.setChecked(false);
				}
			}
			else{
				log("offLineNode is NULL");
				availableOfflineBoolean = false;
				offlineSwitch.setChecked(false);
			}

		}
		else{
			log("NOT Exists in DB OFFLINE: setChecket FALSE: "+node.getHandle());
			availableOfflineBoolean = false;
			offlineSwitch.setChecked(false);
		}
	}

	public void createDefaultAvatar(ImageView ownerRoundeImage, MegaUser user, String name){
		log("createDefaultAvatar()");

		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		String color = megaApi.getUserAvatarColor(user);
		if(color!=null){
			log("The color to set the avatar is "+color);
			p.setColor(Color.parseColor(color));
		}
		else{
			log("Default color to the avatar");
			p.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
		}

		int radius;
		if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
			radius = defaultAvatar.getWidth()/2;
		else
			radius = defaultAvatar.getHeight()/2;

		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		ownerRoundeImage.setImageBitmap(defaultAvatar);

		Display display = this.getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = this.getResources().getDisplayMetrics().density;


		int avatarTextSize = getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);

		String firstLetter = name.charAt(0) + "";
		firstLetter = firstLetter.toUpperCase(Locale.getDefault());
		ownerLetter.setText(firstLetter);
		ownerLetter.setTextColor(Color.WHITE);
		ownerLetter.setVisibility(View.VISIBLE);
		ownerLetter.setTextSize(24);

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

//	private boolean checkChildrenStatus(ArrayList<MegaNode> childrenList){
//
//		boolean children = true;
//		ArrayList<MegaNode> childrenListRec;
//
//		if(childrenList.size()>0){
//			for(int i=0;i<childrenList.size();i++){
//
//				if(!dbH.exists(childrenList.get(i).getHandle())){
//					children=false;
//					return children;
//				}
//				else{
//					if(childrenList.get(i).isFolder()){
//
//						childrenListRec=megaApi.getChildren(childrenList.get(i));
//
//						if(childrenListRec.size()>0){
//							boolean result=checkChildrenStatus(childrenListRec);
//							if(!result){
//								children=false;
//								return children;
//							}
//						}
//					}
//					else{
//
//						if(!dbH.exists(childrenList.get(i).getHandle())){
//							children=false;
//							return children;
//						}
//
//					}
//				}
//			}
//		}
//		return children;
//	}
	@Override
	public void onClick(View v) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		switch (v.getId()) {
			case R.id.file_properties_text_number_versions:{
                Intent i = new Intent(this, VersionsFileActivity.class);
                i.putExtra("handle", node.getHandle());
                startActivity(i);
				break;
			}
			case R.id.file_properties_link_button:{
				log("copy link button");
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
					android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					clipboard.setText(node.getPublicLink());
				} else {
					android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", node.getPublicLink());
					clipboard.setPrimaryClip(clip);
				}
				Snackbar.make(fragmentContainer, getString(R.string.file_properties_get_link), Snackbar.LENGTH_LONG).show();
				break;
			}
			case R.id.file_properties_shared_layout:
			case R.id.file_properties_shared_info_button:{
				Intent i = new Intent(this, FileContactListActivityLollipop.class);
				i.putExtra("name", node.getHandle());
				startActivity(i);
				break;
			}
			case R.id.file_properties_switch:{
				boolean isChecked = offlineSwitch.isChecked();

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

						File destination = null;
						if (from == FROM_INCOMING_SHARES) {
							log("FROM_INCOMING_SHARES");
							//Find in the filesystem
							if (Environment.getExternalStorageDirectory() != null) {
								long handleIncoming = findIncomingParentHandle(node);
								destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + Long.toString(handleIncoming) + "/" + MegaApiUtils.createStringTree(node, this));
								log("offline File INCOMING: " + destination.getAbsolutePath());
							} else {
								destination = this.getFilesDir();
							}

						}
						else if(from==FROM_INBOX){
							log("FROM_INBOX");
							if (Environment.getExternalStorageDirectory() != null) {
								destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/" + MegaApiUtils.createStringTree(node, this));
								log("offline File INBOX: " + destination.getAbsolutePath());
							} else {
								destination = this.getFilesDir();
							}
						}
						else {
							log("NOT INCOMING NOT INBOX");
							//Find in the filesystem

							if (Environment.getExternalStorageDirectory() != null){
								destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/"+MegaApiUtils.createStringTree(node, this));
							}
							else{
								destination = getFilesDir();
							}
						}

						log("Path destination: "+destination);

						if (destination.exists() && destination.isDirectory()){
							File offlineFile = new File(destination, node.getName());
							if (offlineFile.exists() && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getName())){ //This means that is already available offline
								return;
							}
						}

						log("Handle to save for offline : "+node.getHandle());
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
							String destinationPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + handleString + "/"+MegaApiUtils.createStringTree(node, this);
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
				break;
			}
            case R.id.file_properties_info_data_location:{

                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                intent.setAction(Constants.ACTION_OPEN_FOLDER);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("locationFileInfo", true);
                if (adapterType == Constants.OFFLINE_ADAPTER){
                    intent.putExtra("offline_adapter", true);
                    if (path != null){
                        intent.putExtra("path", path);
                        intent.putExtra("pathNavigation", pathNavigation);
                    }
                }
                else {
                    if (megaApi.getParentNode(node) != null){
                        intent.putExtra("PARENT_HANDLE", megaApi.getParentNode(node).getHandle());
                    }
                    intent.putExtra("fragmentHandle", fragmentHandle);
                }
                startActivity(intent);
                this.finish();
                break;
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}

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

		if(mOffDelete.getType().equals(MegaOffline.FOLDER)){
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
		log("Path: "+mOffDelete.getPath());
		if(mOffDelete.getOrigin()==MegaOffline.INCOMING){
			if (Environment.getExternalStorageDirectory() != null){
				destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + mOffDelete.getHandleIncoming() +"/" + mOffDelete.getPath());
			}
			else{
				destination = new File(getFilesDir(), mOffDelete.getHandle()+"");
			}

			log("Remove incoming: "+destination.getAbsolutePath());

			try{
				File offlineFile = new File(destination,  mOffDelete.getName());
				Util.deleteFolderAndSubfolders(this, offlineFile);
			}
			catch(Exception e){
				log("EXCEPTION: removeOffline - file");
				log("EXCEPTION: "+e.toString());
			};

			dbH.removeById(mOffDelete.getId());
		}
		else
		{
			if(from==FROM_INBOX){
				if (Environment.getExternalStorageDirectory() != null) {
					destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/" + mOffDelete.getPath());
					log("offline File INCOMING: " + destination.getAbsolutePath());
				} else {
					destination = this.getFilesDir();
				}
			}
			else{
				if (Environment.getExternalStorageDirectory() != null){
					destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + mOffDelete.getPath());
				}
				else{
					destination = new File(getFilesDir(), mOffDelete.getHandle()+"");
				}
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
		    		megaApi.remove(node,fileInfoActivity);
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
	    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
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
							megaApi.moveNode(megaApi.getNodeByHandle(handle), rubbishNode, fileInfoActivity);
							ProgressDialog temp = null;
							try{
								temp = new ProgressDialog(fileInfoActivity);
								temp.setMessage(getString(R.string.context_move_to_trash));
								temp.show();
							}
							catch(Exception e){
								return;
							}
							statusDialog = temp;
						}
						else{
							megaApi.remove(megaApi.getNodeByHandle(handle), fileInfoActivity);
							ProgressDialog temp = null;
							try{
								temp = new ProgressDialog(fileInfoActivity);
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
			builder.setMessage(message).setPositiveButton(R.string.general_move, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
		else{
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_delete_from_mega);
			builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
	}

	public void showRenameDialog(){
		log("showRenameDialog");

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);
//	    layout.setLayoutParams(params);

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, node.isFolder());
//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setTextColor(getResources().getColor(R.color.text_secondary));
//		input.setHint(getString(R.string.context_new_folder_name));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),EditorInfo.IME_ACTION_DONE);
		input.setText(node.getName());
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

		layout.addView(input, params);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(17, outMetrics), 0);

		final RelativeLayout error_layout = new RelativeLayout(FileInfoActivityLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(FileInfoActivityLollipop.this);
		error_icon.setImageDrawable(FileInfoActivityLollipop.this.getResources().getDrawable(R.drawable.ic_input_warning));
		error_layout.addView(error_icon);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();


		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon.setLayoutParams(params_icon);

		error_icon.setColorFilter(ContextCompat.getColor(FileInfoActivityLollipop.this, R.color.login_warning));

		final TextView textError = new TextView(FileInfoActivityLollipop.this);
		error_layout.addView(textError);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(Util.scaleWidthPx(3, outMetrics), 0,0,0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(FileInfoActivityLollipop.this, R.color.login_warning));

		error_layout.setVisibility(View.GONE);

		input.getBackground().mutate().clearColorFilter();
		input.getBackground().mutate().setColorFilter(getResources().getColor(R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		input.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if(error_layout.getVisibility() == View.VISIBLE){
					error_layout.setVisibility(View.GONE);
					input.getBackground().mutate().clearColorFilter();
					input.getBackground().mutate().setColorFilter(getResources().getColor(R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
										  KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						input.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_string));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();
						return true;
					}
					rename(value);
					renameDialog.dismiss();
					return true;
				}
				return false;
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.context_rename) + " "	+ new String(node.getName()));
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
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				input.getBackground().clearColorFilter();
			}
		});
		builder.setView(layout);
		renameDialog = builder.create();
		renameDialog.show();
		renameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String value = input.getText().toString().trim();
				if (value.length() == 0) {
					input.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError.setText(getString(R.string.invalid_string));
					error_layout.setVisibility(View.VISIBLE);
					input.requestFocus();
				}
				else{
					rename(value);
					renameDialog.dismiss();
				}
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

	public void showGetLinkActivity(long handle){
		log("showGetLinkActivity");
		Intent linkIntent = new Intent(this, GetLinkActivityLollipop.class);
		linkIntent.putExtra("handle", handle);
		linkIntent.putExtra("account", accountType);
		startActivity(linkIntent);
	}

	public void setIsGetLink(boolean value){
		this.isGetLink = value;
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

//		node = megaApi.getNodeByHandle(request.getNodeHandle());

		log("onRequestFinish: "+request.getType() + "__" + request.getRequestString());

		if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE){
			if (e.getErrorCode() == MegaError.API_OK){
				File previewDir = PreviewUtils.getPreviewFolder(this);
				File preview = new File(previewDir, node.getBase64Handle()+".jpg");
				if (preview.exists()) {
					if (preview.length() > 0) {
						Bitmap bitmap = PreviewUtils.getBitmapForCache(preview, this);
						PreviewUtils.previewCache.put(node.getHandle(), bitmap);
						if (iconToolbarView != null){
							imageToolbarView.setImageBitmap(bitmap);
							imageToolbarLayout.setVisibility(View.VISIBLE);
							iconToolbarLayout.setVisibility(View.GONE);
						}
					}
				}
			}
		}
		else if(request.getType() == MegaRequest.TYPE_FOLDER_INFO){
            if (e.getErrorCode() == MegaError.API_OK){
                MegaFolderInfo info = request.getMegaFolderInfo();
                int numVersions = info.getNumVersions();
                log("Num versions: "+numVersions);
                if(numVersions>0){
                    folderVersionsLayout.setVisibility(View.VISIBLE);
                    String text = getResources().getQuantityString(R.plurals.number_of_versions_inside_folder, numVersions, numVersions);
                    folderVersionsText.setText(text);

                    long currentVersions = info.getCurrentSize();
                    log("Current versions: "+currentVersions);
                    if(currentVersions>0){
                        folderCurrentVersionsText.setText(Util.getSizeString(currentVersions));
                        folderCurrentVersionsLayout.setVisibility(View.VISIBLE);
                    }

                }
                else{
                    folderVersionsLayout.setVisibility(View.GONE);
                    folderCurrentVersionsLayout.setVisibility(View.GONE);
                }

                long previousVersions = info.getVersionsSize();
                log("Previous versions: "+previousVersions);
                if(previousVersions>0){
                    folderPreviousVersionsText.setText(Util.getSizeString(previousVersions));
                    folderPreviousVersionsLayout.setVisibility(View.VISIBLE);
                }
                else{
                    folderPreviousVersionsLayout.setVisibility(View.GONE);
                }
            }
            else{
                folderPreviousVersionsLayout.setVisibility(View.GONE);
                folderVersionsLayout.setVisibility(View.GONE);
                folderCurrentVersionsLayout.setVisibility(View.GONE);
            }
        }
		else if (request.getType() == MegaRequest.TYPE_RENAME){

			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.context_correctly_renamed), Snackbar.LENGTH_LONG).show();
				collapsingToolbar.setTitle(megaApi.getNodeByHandle(request.getNodeHandle()).getName());
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_renamed), Snackbar.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_moved), Snackbar.LENGTH_LONG).show();
					finish();
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_moved), Snackbar.LENGTH_LONG).show();
					finish();
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
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
				Snackbar.make(fragmentContainer, getString(R.string.context_no_removed), Snackbar.LENGTH_LONG).show();
			}

		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				if (request.getEmail() != null){
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_copied_contact), Snackbar.LENGTH_LONG).show();
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_copied), Snackbar.LENGTH_LONG).show();
				}
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		}
		if (request.getType() == MegaRequest.TYPE_SHARE){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.context_correctly_shared), Snackbar.LENGTH_LONG).show();
				ArrayList<MegaShare> sl = megaApi.getOutShares(node);
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_shared), Snackbar.LENGTH_LONG).show();
			}
		}


		if(request.getType() == MegaApiJava.USER_ATTR_AVATAR){
			try{
				statusDialog.dismiss();
			}catch (Exception ex){}

			if (e.getErrorCode() == MegaError.API_OK){
				boolean avatarExists = false;
				if (contactMail.compareTo(request.getEmail()) == 0){
					File avatar = null;
					if (this.getExternalCacheDir() != null){
						avatar = new File(this.getExternalCacheDir().getAbsolutePath(), contactMail + ".jpg");
					}
					else{
						avatar = new File(this.getCacheDir().getAbsolutePath(), contactMail + ".jpg");
					}
					Bitmap bitmap = null;
					if (avatar.exists()){
						if (avatar.length() > 0){
							BitmapFactory.Options bOpts = new BitmapFactory.Options();
							bOpts.inPurgeable = true;
							bOpts.inInputShareable = true;
							bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
							if (bitmap == null) {
								avatar.delete();
							}
							else{
								avatarExists = true;
								ownerRoundeImage.setImageBitmap(bitmap);
								ownerRoundeImage.setVisibility(View.VISIBLE);
								ownerLetter.setVisibility(View.GONE);
							}
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
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (intent == null) {
			return;
		}

		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);


            if(nC==null){
                nC = new NodeController(this);
            }
            nC.checkSizeBeforeDownload(parentPath, url, size, hashes);
//			Util.showToast(this, R.string.download_began);
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
				MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
				if (cN != null){
					log("cN != null, i = " + i + " of " + copyHandles.length);
					megaApi.copyNode(cN, parent, this);
				}
				else{
					log("cN == null, i = " + i + " of " + copyHandles.length);
					try {
						statusDialog.dismiss();
						Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
					}
					catch (Exception ex) {}
				}
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
			final long nodeHandle = intent.getLongExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, -1);
			final boolean megaContacts = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_MEGA_CONTACTS, true);

			if (megaContacts){
				if (node.isFolder()){
					AlertDialog.Builder dialogBuilder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
					dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
					final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
					dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							ProgressDialog temp = null;
							try{
								temp = new ProgressDialog(fileInfoActivity);
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
			                    		megaApi.share(node, u, MegaShare.ACCESS_READ, fileInfoActivity);
			                    	}
			                    	break;
			                    }
			                    case 1:{
			                    	for (int i=0;i<contactsData.size();i++){
			                    		MegaUser u = megaApi.getContact(contactsData.get(i));
			                    		megaApi.share(node, u, MegaShare.ACCESS_READWRITE, fileInfoActivity);
			                    	}
			                        break;
			                    }
			                    case 2:{
			                    	for (int i=0;i<contactsData.size();i++){
			                    		MegaUser u = megaApi.getContact(contactsData.get(i));
			                    		megaApi.share(node, u, MegaShare.ACCESS_FULL, fileInfoActivity);
			                    	}
			                        break;
			                    }
			                }
						}
					});
					permissionsDialog = dialogBuilder.create();
					permissionsDialog.show();
					/*int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
					View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
					titleDivider.setBackgroundColor(resources.getColor(R.color.mega));*/
				}
				else{
					for (int i=0;i<contactsData.size();i++){
						MegaUser u = megaApi.getContact(contactsData.get(i));
						megaApi.sendFileToUser(node, u, fileInfoActivity);
					}
				}
			}
			else{
				if (node.isFolder()){
					for (int i=0; i < contactsData.size();i++){
						String type = contactsData.get(i);
						if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_EMAIL) == 0){
							i++;
							Snackbar.make(fragmentContainer, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
						}
						else if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_PHONE) == 0){
							i++;
							Snackbar.make(fragmentContainer, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
						}
					}
				}
				else{
					for (int i=0; i < contactsData.size();i++){
						String type = contactsData.get(i);
						if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_EMAIL) == 0){
							i++;
							Snackbar.make(fragmentContainer, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
						}
						else if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_PHONE) == 0){
							i++;
							Snackbar.make(fragmentContainer, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
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

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		log("onUsersUpdate");
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		log("onNodesUpdate");

		boolean thisNode = false;
		boolean anyChild = false;
		boolean updateContentFoder = false;
		if(nodes==null){
			return;
		}
		MegaNode n = null;
		Iterator<MegaNode> it = nodes.iterator();
		while (it.hasNext()){
			MegaNode nodeToCheck = it.next();
			if (nodeToCheck != null){
				if (nodeToCheck.getHandle() == node.getHandle()){
					thisNode = true;
					n = nodeToCheck;
					break;
				}
				else{
                    if(node.isFolder()){
                        MegaNode parent = megaApi.getNodeByHandle(nodeToCheck.getParentHandle());
                        while(parent!=null){
                            if(parent.getHandle() == node.getHandle()){
                                updateContentFoder = true;
                                break;
                            }
                            parent = megaApi.getNodeByHandle(parent.getParentHandle());
                        }
                    }
                    else{
                        if(nodeVersions!=null){
                            for(int j=0; j<nodeVersions.size();j++){
                                if(nodeToCheck.getHandle()==nodeVersions.get(j).getHandle()){
                                    if(anyChild==false){
                                        anyChild = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
				}
			}
		}

		if(updateContentFoder){
		    megaApi.getFolderInfo(node, this);
        }

		if ((!thisNode)&&(!anyChild)){
			log("exit onNodesUpdate - Not related to this node");
			return;
		}

		//Check if the parent handle has changed
		if(n!=null){
			if(n.hasChanged(MegaNode.CHANGE_TYPE_PARENT)){
				MegaNode oldParent = megaApi.getParentNode(node);
				MegaNode newParent = megaApi.getParentNode(n);
				if(oldParent.getHandle()==newParent.getHandle()){
					log("Parents match");
					if(newParent.isFile()){
						log("New version added");
						node = newParent;
					}
					else{
                        node = n;
					}
				}
				else{
					node = n;
				}
				if(megaApi.hasVersions(node)){
					nodeVersions = megaApi.getVersions(node);
				}
				else{
					nodeVersions = null;
				}
			}
			else if(n.hasChanged(MegaNode.CHANGE_TYPE_REMOVED)){
				if(thisNode){
					if(nodeVersions!=null){
//						node = nodeVersions.get(1);
						long nodeHandle = nodeVersions.get(1).getHandle();
						if(megaApi.getNodeByHandle(nodeHandle)!=null){
							node = megaApi.getNodeByHandle(nodeHandle);
							if(megaApi.hasVersions(node)){
								nodeVersions = megaApi.getVersions(node);
							}
							else{
								nodeVersions = null;
							}
						}
						else{
							finish();
						}
					}
					else{
						finish();
					}
				}
				else if(anyChild){
					if(megaApi.hasVersions(n)){
						nodeVersions = megaApi.getVersions(n);
					}
					else{
						nodeVersions = null;
					}
				}

			}
			else{
				node = n;
				if(megaApi.hasVersions(node)){
					nodeVersions = megaApi.getVersions(node);
				}
				else{
					nodeVersions = null;
				}
			}
		}
		else{
			if(anyChild){
				if(megaApi.hasVersions(node)){
					nodeVersions = megaApi.getVersions(node);
				}
				else{
					nodeVersions = null;
				}
			}
		}

		if (moveToRubbish){
			supportInvalidateOptionsMenu();
		}

		if (node == null){
			return;
		}

		if(node.isExported()){
			log("Node HAS public link");
			publicLink=true;
			publicLinkLayout.setVisibility(View.VISIBLE);
			publicLinkCopyLayout.setVisibility(View.VISIBLE);
			iconToolbarViewLink.setVisibility(View.VISIBLE);
			publicLinkText.setText(node.getPublicLink());
			supportInvalidateOptionsMenu();


		}else{
			log("Node NOT public link");
			publicLink=false;
			publicLinkLayout.setVisibility(View.GONE);
			publicLinkCopyLayout.setVisibility(View.GONE);
			iconToolbarViewLink.setVisibility(View.GONE);
			supportInvalidateOptionsMenu();

		}

		if (node.isFolder()){
			long sizeFile=megaApi.getSize(node);
			sizeTextView.setText(Formatter.formatFileSize(this, sizeFile));

			contentTextView.setText(MegaApiUtils.getInfoFolder(node, this));

			if (node.isInShare()){
				imageId = R.drawable.ic_folder_incoming;
			}
			else if (node.isOutShare()||megaApi.isPendingShare(node)){
				imageId = R.drawable.ic_folder_outgoing;
			}
			else{
				imageId = R.drawable.ic_folder;
			}
			iconToolbarView.setImageResource(imageId);
			sl = megaApi.getOutShares(node);
			if (sl != null){
				if (sl.size() == 0){
					log("sl.size==0");
					sharedLayout.setVisibility(View.GONE);
					dividerSharedLayout.setVisibility(View.GONE);

//					If I am the owner
					if (megaApi.checkAccess(node, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK){

//						permissionLabel.setVisibility(View.GONE);
//						permissionInfo.setVisibility(View.GONE);
						//permissionsLayout.setVisibility(View.GONE);
						permissionInfo.setVisibility(View.GONE);
						//permissionInfo.setText(getResources().getString(R.string.file_properties_owner));

					}
					else{

						//If I am not the owner
						owner = false;
						//permissionsLayout.setVisibility(View.VISIBLE);
						permissionInfo.setVisibility(View.VISIBLE);
//						permissionLabel.setVisibility(View.VISIBLE);
//						permissionInfo.setVisibility(View.VISIBLE);

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
					sharedLayout.setVisibility(View.VISIBLE);
					dividerSharedLayout.setVisibility(View.VISIBLE);
					usersSharedWithTextButton.setText((sl.size())+" "+getResources().getQuantityString(R.plurals.general_num_users,(sl.size()-1)));
				}
			}
		}
		else{

			sizeTextView.setText(Formatter.formatFileSize(this, node.getSize()));
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

		if(megaApi.hasVersions(node)){
			versionsLayout.setVisibility(View.VISIBLE);
            String text = getResources().getQuantityString(R.plurals.number_of_versions, megaApi.getNumVersions(node), megaApi.getNumVersions(node));
            versionsButton.setText(text);
			versionsButton.setOnClickListener(this);
			separatorVersions.setVisibility(View.VISIBLE);
		}
		else{
			versionsLayout.setVisibility(View.GONE);
			separatorVersions.setVisibility(View.GONE);
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

		upArrow.setColorFilter(null);
		drawableRemoveLink.setColorFilter(null);
		drawableLink.setColorFilter(null);
		drawableShare.setColorFilter(null);
		drawableDots.setColorFilter(null);
    }

	public static void log(String message) {
		Util.log("FileInfoActivityLollipop", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResume() {
		log("onResume-FileInfoActivityLollipop");
		super.onResume();

        if (adapterType != Constants.OFFLINE_ADAPTER){
            refreshProperties();
            supportInvalidateOptionsMenu();
            ((MegaApplication) getApplication()).sendSignalPresenceActivity();
        }
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {

	}

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	@Override
	public void onBackPressed() {

		super.onBackPressed();
	}

	public void showSnackbar(String s){
		log("showSnackbar");
		Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}


    public void openAdvancedDevices (long handleToDownload){
        log("openAdvancedDevices");
//		handleToDownload = handle;
        String externalPath = Util.getExternalCardPath();

        if(externalPath!=null){
            log("ExternalPath for advancedDevices: "+externalPath);
            MegaNode node = megaApi.getNodeByHandle(handleToDownload);
            if(node!=null){

//				File newFile =  new File(externalPath+"/"+node.getName());
                File newFile =  new File(node.getName());
                log("File: "+newFile.getPath());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Create a file with the requested MIME type.
                String mimeType = MimeTypeList.getMimeType(newFile);
                log("Mimetype: "+mimeType);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_TITLE, node.getName());
                intent.putExtra("handleToDownload", handleToDownload);
                try{
                    startActivityForResult(intent, Constants.WRITE_SD_CARD_REQUEST_CODE);
                }
                catch(Exception e){
                    log("Exception in External SDCARD");
                    Environment.getExternalStorageDirectory();
                    Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
        else{
            log("No external SD card");
            Environment.getExternalStorageDirectory();
            Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes){
        log("askSizeConfirmationBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(getResources().getColor(R.color.text_secondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));

        builder.setMessage(getString(R.string.alert_larger_file, Util.getSizeString(sizeC)));
        builder.setPositiveButton(getString(R.string.general_download),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskSizeDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(fileInfoActivity);
                        }
                        nC.checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(dontShowAgain.isChecked()){
                    dbH.setAttrAskSizeDownload("false");
                }
            }
        });

        downloadConfirmationDialog = builder.create();
        downloadConfirmationDialog.show();
    }

    public void askConfirmationNoAppInstaledBeforeDownload (String parentPath, String url, long size, long [] hashes, String nodeToDownload){
        log("askConfirmationNoAppInstaledBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(getResources().getColor(R.color.text_secondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));
        builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
        builder.setPositiveButton(getString(R.string.general_download),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskNoAppDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(fileInfoActivity);
                        }
                        nC.download(parentPathC, urlC, sizeC, hashesC);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(dontShowAgain.isChecked()){
                    dbH.setAttrAskNoAppDownload("false");
                }
            }
        });
        downloadConfirmationDialog = builder.create();
        downloadConfirmationDialog.show();
    }

    public void showSnackbarNotSpace(){
        log("showSnackbarNotSpace");
        Snackbar mySnackbar = Snackbar.make(fragmentContainer, R.string.error_not_enough_free_space, Snackbar.LENGTH_LONG);
        mySnackbar.setAction("Settings", new SnackbarNavigateOption(this));
        mySnackbar.show();
    }
}
