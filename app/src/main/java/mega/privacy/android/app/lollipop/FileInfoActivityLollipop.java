package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.MegaFileInfoSharedContactLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.listeners.CreateChatToPerformActionListener;
import mega.privacy.android.app.lollipop.listeners.FileContactMultipleRequestListener;
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
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
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;


@SuppressLint("NewApi")
public class FileInfoActivityLollipop extends PinActivityLollipop implements OnClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaChatRequestListenerInterface {

	public static int MAX_WIDTH_FILENAME_LAND=400;
	public static int MAX_WIDTH_FILENAME_LAND_2=400;

	public static int MAX_WIDTH_FILENAME_PORT=170;
	public static int MAX_WIDTH_FILENAME_PORT_2=200;

	public static String NODE_HANDLE = "NODE_HANDLE";

	static int TYPE_EXPORT_GET = 0;
	static int TYPE_EXPORT_REMOVE = 1;
	static int TYPE_EXPORT_MANAGE = 2;
	static int FROM_FILE_BROWSER = 13;
    FileInfoActivityLollipop fileInfoActivityLollipop = this;
	boolean firstIncomingLevel=true;

    private android.support.v7.app.AlertDialog downloadConfirmationDialog;

    // The flag to indicate whether select chat is processing
    private static boolean isSelectingChat = false;

    NodeController nC;

	ArrayList<MegaNode> nodeVersions;

	NestedScrollView nestedScrollView;

	RelativeLayout iconToolbarLayout;
	ImageView iconToolbarView;

    Drawable upArrow;
    Drawable drawableRemoveLink;
    Drawable drawableLink;
    Drawable drawableShare;
    Drawable drawableDots;
    Drawable drawableDownload;
    Drawable drawableLeave;
    Drawable drawableCopy;
    Drawable drawableChat;

	RelativeLayout imageToolbarLayout;
	ImageView imageToolbarView;

	CoordinatorLayout fragmentContainer;
	CollapsingToolbarLayout collapsingToolbar;

	Toolbar toolbar;
	ActionBar aB;

	private boolean isGetLink = false;
	private boolean isShareContactExpanded = false;
    boolean removeShare = false;
    boolean changeShare = false;

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
	RelativeLayout sharedLayout;
	Button usersSharedWithTextButton;
	View dividerSharedLayout;
	View dividerLinkLayout;

    RelativeLayout folderVersionsLayout;
    RelativeLayout folderCurrentVersionsLayout;
    RelativeLayout folderPreviousVersionsLayout;
    TextView folderVersionsText;
    TextView folderCurrentVersionsText;
    TextView folderPreviousVersionsText;

	TextView availableOfflineView;

	Button publicLinkButton;

	RelativeLayout versionsLayout;
	Button versionsButton;
	View separatorVersions;
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
	TextView permissionInfo;

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
	ImageView ownerState;

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
	MenuItem sendToChatMenuItem;

	MegaNode node;

	boolean availableOfflineBoolean = false;

	private MegaApiAndroid megaApi = null;
	MegaChatApiAndroid megaChatApi;
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	public FileInfoActivityLollipop fileInfoActivity;

	ProgressDialog statusDialog;
	boolean publicLink=false;

	private Handler handler;

	private AlertDialog renameDialog;

	boolean moveToRubbish = false;

	public static int REQUEST_CODE_SELECT_CONTACT = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;

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
    boolean isRemoveOffline;
    long handle;

    private int adapterType;
 	private String path;
 	private File file;
 	private long fragmentHandle  = -1;
 	private String pathNavigation;

 	private MegaShare selectedShare;
    final int MAX_NUMBER_OF_CONTACTS_IN_LIST = 5;
    private RecyclerView listView;
    private ArrayList<MegaShare> listContacts;
    private ArrayList<MegaShare> fullListContacts;
    private Button moreButton;
    private MegaFileInfoSharedContactLollipopAdapter adapter;
    private ActionMode actionMode;

    int countChat = 0;
    int errorSent = 0;
    int successSent = 0;

    int versionsToRemove = 0;
    int versionsRemoved = 0;
    int errorVersionRemove = 0;

    private FileContactMultipleRequestListener.RequestCompletedCallback requestCompletedCallback;

    public void activateActionMode(){
        logDebug("activateActionMode");
        if (!adapter.isMultipleSelect()){
            adapter.setMultipleSelect(true);
            actionMode = startSupportActionMode(new ActionBarCallBack());
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("File sent correctly");
                successSent++;
            }
            else{
                logWarning("File NOT sent: "+ e.getErrorCode() + "___" + e.getErrorString());
                errorSent++;
            }

            if(countChat==errorSent+successSent){
                if(successSent==countChat){
                    if(countChat==1){
                        showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.sent_as_message), request.getChatHandle());
                    }
                    else{
                        showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.sent_as_message), -1);
                    }
                }
                else if(errorSent==countChat){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_attaching_node_from_cloud), -1);
                }
                else{
                    showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.error_sent_as_message), -1);
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    private class ActionBarCallBack implements ActionMode.Callback {
        
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            logDebug("onActionItemClicked");
            final List<MegaShare> shares = adapter.getSelectedShares();
            switch(item.getItemId()){
                case R.id.action_file_contact_list_permissions:{

                    //Change permissions
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(fileInfoActivityLollipop, R.style.AppCompatAlertDialogStyleAddContacts);
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));

                    final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                    dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            clearSelections();
                            if(permissionsDialog != null){
                                permissionsDialog.dismiss();
                            }
                            removeShare = false;
                            changeShare = true;
                            ProgressDialog temp;
                            try{
                                temp = new ProgressDialog(fileInfoActivityLollipop);
                                temp.setMessage(getString(R.string.context_permissions_changing_folder));
                                temp.show();
                            }
                            catch(Exception e){
                                logError("Exception", e);
                                return;
                            }
                            statusDialog = temp;

                            ContactController controller = new ContactController(FileInfoActivityLollipop.this);
                            MegaRequestListenerInterface callback;
                            if (shares.size() > 1) {
                                callback = new FileContactMultipleRequestListener(MULTIPLE_CHANGE_PERMISSION, FileInfoActivityLollipop.this, requestCompletedCallback);
                            } else if (shares.size() == 1) {
                                callback = FileInfoActivityLollipop.this;
                            } else {
                                logWarning("Shares array is empty");
                                return;
                            }
                            controller.checkShares(shares, item, node, callback);
                        }
                    });

                    permissionsDialog = dialogBuilder.create();
                    permissionsDialog.show();
                    break;
                }
                case R.id.action_file_contact_list_delete:{
                    if(shares!=null){
                        if(shares.size()!=0){
                            if (shares.size() > 1) {
                                logDebug("Remove multiple contacts");
                                showConfirmationRemoveMultipleContactFromShare(shares);
                            } else {
                                logDebug("Remove one contact");
                                showConfirmationRemoveContactFromShare(shares.get(0).getUser());
                            }
                        }
                    }
                    break;
                }
                case R.id.cab_menu_select_all:{
                    selectAll();
                    actionMode.invalidate();
                    break;
                }
                case R.id.cab_menu_unselect_all:{
                    clearSelections();
                    actionMode.invalidate();
                    break;
                }
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            logDebug("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_contact_shared_browser_action, menu);
            getWindow().setStatusBarColor(ContextCompat.getColor(fileInfoActivity, R.color.accentColorDark));
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            logDebug("onDestroyActionMode");
            adapter.clearSelections();
            adapter.setMultipleSelect(false);
            getWindow().setStatusBarColor(ContextCompat.getColor(fileInfoActivity, R.color.status_bar_search));
            supportInvalidateOptionsMenu();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            logDebug("onPrepareActionMode");
            List<MegaShare> selected = adapter.getSelectedShares();
            boolean deleteShare = false;
            boolean permissions = false;

            if (selected.size() != 0) {
                permissions = true;
                deleteShare = true;

                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                if(selected.size()==adapter.getItemCount()){
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                }
                else{
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                }
            }
            else{
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
            }

            menu.findItem(R.id.action_file_contact_list_permissions).setVisible(permissions);
            if(permissions == true){
                menu.findItem(R.id.action_file_contact_list_permissions).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }else{
                menu.findItem(R.id.action_file_contact_list_permissions).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            }

            menu.findItem(R.id.action_file_contact_list_delete).setVisible(deleteShare);
            if(deleteShare == true){
                menu.findItem(R.id.action_file_contact_list_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }else{
                menu.findItem(R.id.action_file_contact_list_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            }

            return false;
        }

    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
        logDebug("onCreate");

        fileInfoActivity = this;
        handler = new Handler();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            imageId = extras.getInt("imageId");
        }

        requestCompletedCallback = new FileContactMultipleRequestListener.RequestCompletedCallback() {
            @Override
            public void onRequestCompleted(String message) {
                editMultipleSharesCallback(message);
            }
        };

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        scaleW = getScaleW(outMetrics, density);
        scaleH = getScaleH(outMetrics, density);

        if (scaleH < scaleW){
            scaleText = scaleH;
        }
        else{
            scaleText = scaleW;
        }

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

        nestedScrollView = (NestedScrollView) findViewById(R.id.nested_layout);
        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if ((v.canScrollVertically(-1) && v.getVisibility() == View.VISIBLE)) {
                    aB.setElevation(px2dp(4, outMetrics));
                }
                else {
                    aB.setElevation(0);
                }
            }
        });

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        iconToolbarLayout = (RelativeLayout) findViewById(R.id.file_info_icon_layout);

        iconToolbarView = (ImageView) findViewById(R.id.file_info_toolbar_icon);
        iconToolbarView.setImageResource(imageId);
        CollapsingToolbarLayout.LayoutParams params = (CollapsingToolbarLayout.LayoutParams) iconToolbarLayout.getLayoutParams();
        Rect rect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        params.setMargins(px2dp(16, outMetrics), px2dp(90, outMetrics) + rect.top, 0, px2dp(14, outMetrics));
        iconToolbarLayout.setLayoutParams(params);

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
        usersSharedWithTextButton = (Button) findViewById(R.id.file_properties_shared_info_button);
        usersSharedWithTextButton.setOnClickListener(this);
        dividerSharedLayout = findViewById(R.id.divider_shared_layout);
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
        ownerState = (ImageView) findViewById(R.id.file_properties_owner_state_icon);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            logDebug("Landscape configuration");
            float width1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, getResources().getDisplayMetrics());
            float width2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND_2, getResources().getDisplayMetrics());

            ownerLabel.setMaxWidth((int) width1);
            ownerInfo.setMaxWidth((int) width2);

        }
        else{
            logDebug("Portrait configuration");
            float width1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, getResources().getDisplayMetrics());
            float width2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT_2, getResources().getDisplayMetrics());

            ownerLabel.setMaxWidth((int) width1);
            ownerInfo.setMaxWidth((int) width2);
        }

        ownerLayout.setVisibility(View.GONE);

        //Info Layout

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

        dividerLinkLayout = (View) findViewById(R.id.divider_link_layout);
        publicLinkLayout = (RelativeLayout) findViewById(R.id.file_properties_link_layout);
        publicLinkCopyLayout = (RelativeLayout) findViewById(R.id.file_properties_copy_layout);

        publicLinkText = (TextView) findViewById(R.id.file_properties_link_text);
        publicLinkButton = (Button) findViewById(R.id.file_properties_link_button);
        publicLinkButton.setText(getString(R.string.context_copy));
        publicLinkButton.setOnClickListener(this);

        //Added Layout
        addedLayout = (RelativeLayout) findViewById(R.id.file_properties_added_layout);
        addedTextView = (TextView) findViewById(R.id.file_properties_info_data_added);

        //Modified Layout
        modifiedLayout = (RelativeLayout) findViewById(R.id.file_properties_created_layout);
        modifiedTextView = (TextView) findViewById(R.id.file_properties_info_data_created);

        //Versions Layout
        versionsLayout = (RelativeLayout) findViewById(R.id.file_properties_versions_layout);
        versionsButton = (Button) findViewById(R.id.file_properties_text_number_versions);
        separatorVersions = (View) findViewById(R.id.separator_versions);

        if (adapterType == OFFLINE_ADAPTER){
            collapsingToolbar.setTitle(getIntent().getStringExtra("name").toUpperCase());
            availableOfflineLayout.setVisibility(View.GONE);
            sharedLayout.setVisibility(View.GONE);
            dividerSharedLayout.setVisibility(View.GONE);
            dividerLinkLayout.setVisibility(View.GONE);
            publicLinkLayout.setVisibility(View.GONE);
            publicLinkCopyLayout.setVisibility(View.GONE);
            contentLayout.setVisibility(View.GONE);
            addedLayout.setVisibility(View.GONE);
            modifiedLayout.setVisibility(View.GONE);
            versionsLayout.setVisibility(View.GONE);
            separatorVersions.setVisibility(View.GONE);

            if (path != null){
                logDebug("Path no NULL");
                file = new File (path);
                sizeTextView.setText(getSizeString(file.length()));
                String location = file.getParentFile().getName();
                if (location.equals("in")){
                    locationTextView.setText(getResources().getString(R.string.section_saved_for_offline_new));
                }
                else {
                    String offlineLocation = file.getParentFile().getParentFile().getName() + '/' + location;
                    if (offlineLocation.equals(OFFLINE_DIR)){
                        locationTextView.setText(getResources().getString(R.string.section_saved_for_offline_new));
                    }
                    else {
                        locationTextView.setText(location + " ("+ getResources().getString(R.string.section_saved_for_offline_new) +")");
                    }
                }
                logDebug("Path: " + file.getAbsolutePath() + ", Size: " + file.length());
            }
            else {
                logWarning("Path is NULL");
            }
            pathNavigation = getIntent().getStringExtra("pathNavigation");
        }
        else {
            if (megaApi == null){
                MegaApplication app = (MegaApplication)getApplication();
                megaApi = app.getMegaApi();
            }
            if(megaApi==null||megaApi.getRootNode()==null){
                logDebug("Refresh session - sdk");
                Intent intent = new Intent(this, LoginActivityLollipop.class);
                intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return;
            }
            if(isChatEnabled()) {
                if (megaChatApi == null) {
                    megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                }

                if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
                    logDebug("Refresh session - karere");
                    Intent intent = new Intent(this, LoginActivityLollipop.class);
                    intent.putExtra("visibleFragment", LOGIN_FRAGMENT);
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

                long handleNode = extras.getLong("handle", -1);
                logDebug("Handle of the selected node: " + handleNode);
                node = megaApi.getNodeByHandle(handleNode);
                if (node == null){
                    logWarning("Node is NULL");
                    finish();
                    return;
                }

                if (node.isFolder()) {
                    modifiedLayout.setVisibility(View.GONE);
                } else {
                    modifiedLayout.setVisibility(View.VISIBLE);
                }

                String name = node.getName();

                collapsingToolbar.setTitle(name.toUpperCase());
                if (nC == null) {
                    nC = new NodeController(this);
                }
                MegaNode parent = nC.getParent(node);
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
                                locationTextView.setText(getTranslatedNameForParentNodes(parent.getHandle()));
                            }
                            else {
                                locationTextView.setText(megaApi.getParentNode(node).getName()+" ("+ getTranslatedNameForParentNodes(parent.getHandle()) +")");
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
                    availableOfflineView.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
                }else{
                    offlineSwitch.setEnabled(false);
                    availableOfflineView.setTextColor(ContextCompat.getColor(this, R.color.invite_button_deactivated));

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
                logWarning("Extras is NULL");
            }
    
            listView = (RecyclerView)findViewById(R.id.file_info_contact_list_view);
            //listView.addOnItemTouchListener(this);
            listView.setItemAnimator(new DefaultItemAnimator());
            listView.addItemDecoration(new SimpleDividerItemDecoration(this,outMetrics));
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
            listView.setLayoutManager(mLayoutManager);

            //get shared contact list and max number can be displayed in the list is five
            setContactList();

            moreButton = (Button)findViewById(R.id.more_button);
            moreButton.setOnClickListener(this);
            setMoreButtonText();

            //setup adapter
            adapter = new MegaFileInfoSharedContactLollipopAdapter(this,node,listContacts,listView);
            adapter.setShareList(listContacts);
            adapter.setPositionClicked(-1);
            adapter.setMultipleSelect(false);

            listView.setAdapter(adapter);

            refreshProperties();
            supportInvalidateOptionsMenu();

        }
	}
	
	private String getTranslatedNameForParentNodes(long parentHandle){
        String translated;
        Context context = getApplicationContext();
        if(parentHandle == megaApi.getRootNode().getHandle()){
            translated = context.getString(R.string.section_cloud_drive);
        }else if(parentHandle == megaApi.getRubbishNode().getHandle()){
            translated = context.getString(R.string.section_rubbish_bin);
        }else if(parentHandle == megaApi.getInboxNode().getHandle()){
            translated = context.getString(R.string.section_inbox);
        }else {
            translated = megaApi.getNodeByHandle(parentHandle).getName();
        }
        return translated;
    }

	void setOwnerState (long userHandle) {
        if(isChatEnabled()){
            ownerState.setVisibility(View.VISIBLE);
            if (megaChatApi != null){
                int userStatus = megaChatApi.getUserOnlineStatus(userHandle);
                if(userStatus == MegaChatApi.STATUS_ONLINE){
                    logDebug("This user is connected");
                    ownerState.setVisibility(View.VISIBLE);
                    ownerState.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.circle_status_contact_online));
                }
                else if(userStatus == MegaChatApi.STATUS_AWAY){
                    logDebug("This user is away");
                    ownerState.setVisibility(View.VISIBLE);
                    ownerState.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.circle_status_contact_away));
                }
                else if(userStatus == MegaChatApi.STATUS_BUSY){
                    logDebug("This user is busy");
                    ownerState.setVisibility(View.VISIBLE);
                    ownerState.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.circle_status_contact_busy));
                }
                else if(userStatus == MegaChatApi.STATUS_OFFLINE){
                    logDebug("This user is offline");
                    ownerState.setVisibility(View.VISIBLE);
                    ownerState.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.circle_status_contact_offline));
                }
                else if(userStatus == MegaChatApi.STATUS_INVALID){
                    logWarning("INVALID status: " + userStatus);
                    ownerState.setVisibility(View.GONE);
                }
                else{
                    logDebug("This user status is: " + userStatus);
                    ownerState.setVisibility(View.GONE);
                }
            }
        }
        else{
            ownerState.setVisibility(View.GONE);
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
        drawableDownload = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_download_white);
        drawableDownload = drawableDownload.mutate();
        drawableLeave = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_leave_share_w);
        drawableLeave = drawableLeave.mutate();
        drawableCopy = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_copy_white);
        drawableCopy.mutate();
        drawableChat = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_send_to_contact);
        drawableChat.mutate();

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
		sendToChatMenuItem = menu.findItem(R.id.cab_menu_file_info_send_to_chat);


		if (adapterType == OFFLINE_ADAPTER){
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
            sendToChatMenuItem.setVisible(false);

            setColorFilterBlack();
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
                    renameMenuItem.setVisible(true);
                    moveMenuItem.setVisible(true);
                    copyMenuItem.setVisible(true);
                    sendToChatMenuItem.setVisible(false);
                    rubbishMenuItem.setVisible(false);
                    deleteMenuItem.setVisible(true);
                    leaveMenuItem.setVisible(false);
                } else {

                    if (node.isFolder() || !isChatEnabled()) {
                        sendToChatMenuItem.setVisible(false);
                        menu.findItem(R.id.cab_menu_file_info_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    }
                    else {
                        sendToChatMenuItem.setVisible(true);
                        menu.findItem(R.id.cab_menu_file_info_send_to_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    }

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
                        menu.findItem(R.id.cab_menu_file_info_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

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
                        logDebug("Node: " + node.getHandle());

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
                    appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                        @Override
                        public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
                            if (offset == 0) {
                                // Expanded
                                setColorFilterWhite();
                            }
                            else {
                                if (offset<0 && Math.abs(offset)>=appBarLayout.getTotalScrollRange()/2) {
                                    // Collapsed
                                    setColorFilterBlack();
                                }
                                else {
                                   setColorFilterWhite();
                                }
                            }
                        }
                    });

                    collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.name_my_account));
                    collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.white));
                    collapsingToolbar.setStatusBarScrimColor(ContextCompat.getColor(this, R.color.status_bar_search));
                }
			/*Folder*/
                else {
                    getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_search));
                    setColorFilterBlack();
                }
            }
        }

		return super.onCreateOptionsMenu(menu);
	}

	void setColorFilterBlack () {
        upArrow.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        drawableDots.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        toolbar.setOverflowIcon(drawableDots);

        if (removeLinkMenuItem != null) {
            drawableRemoveLink.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            removeLinkMenuItem.setIcon(drawableRemoveLink);
        }
        if (getLinkMenuItem != null) {
            drawableLink.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            getLinkMenuItem.setIcon(drawableLink);
        }
        if (downloadMenuItem != null) {
            drawableDownload.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            downloadMenuItem.setIcon(drawableDownload);
        }
        if (shareMenuItem != null) {
            drawableShare.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            shareMenuItem.setIcon(drawableShare);
        }
        if (leaveMenuItem != null) {
            drawableLeave.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            leaveMenuItem.setIcon(drawableLeave);
        }
        if (copyMenuItem != null) {
            drawableCopy.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            copyMenuItem.setIcon(drawableCopy);
        }
        if (sendToChatMenuItem != null) {
            drawableChat.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            sendToChatMenuItem.setIcon(drawableChat);
        }
    }

    void setColorFilterWhite () {
        upArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        drawableDots.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        toolbar.setOverflowIcon(drawableDots);

        if (removeLinkMenuItem != null) {
            drawableRemoveLink.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            removeLinkMenuItem.setIcon(drawableRemoveLink);
        }
        if (getLinkMenuItem != null) {
            drawableLink.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            getLinkMenuItem.setIcon(drawableLink);
        }
        if (downloadMenuItem != null) {
            drawableDownload.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            downloadMenuItem.setIcon(drawableDownload);
        }
        if (shareMenuItem != null) {
            drawableShare.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            shareMenuItem.setIcon(drawableShare);
        }
        if (leaveMenuItem != null) {
            drawableLeave.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            leaveMenuItem.setIcon(drawableLeave);
        }
        if (copyMenuItem != null) {
            drawableCopy.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            copyMenuItem.setIcon(drawableCopy);
        }
        if (sendToChatMenuItem != null) {
            drawableChat.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            sendToChatMenuItem.setIcon(drawableChat);
        }
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");

		int id = item.getItemId();
		switch (id) {
			case android.R.id.home: {
                onBackPressed();
				break;
			}
			case R.id.cab_menu_file_info_download: {
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                if(nC==null){
                    nC = new NodeController(this);
                }
                nC.prepareForDownload(handleList, false);

				break;
			}
			case R.id.cab_menu_file_info_share_folder: {
				Intent intent = new Intent();
				intent.setClass(this, AddContactActivityLollipop.class);
				intent.putExtra("contactType", CONTACT_TYPE_BOTH);
                intent.putExtra("MULTISELECT", 0);
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

				((RelativeLayout.LayoutParams) removeText.getLayoutParams()).setMargins(scaleWidthPx(25, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(10, outMetrics), 0);

				url.setVisibility(View.GONE);
				key.setVisibility(View.GONE);
				symbol.setVisibility(View.GONE);
				removeText.setVisibility(View.VISIBLE);

				removeText.setText(getString(R.string.context_remove_link_warning_text));

				Display display = getWindowManager().getDefaultDisplay();
				DisplayMetrics outMetrics = new DisplayMetrics();
				display.getMetrics(outMetrics);
				float density = getResources().getDisplayMetrics().density;

				float scaleW = getScaleW(outMetrics, density);
				float scaleH = getScaleH(outMetrics, density);


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
            case R.id.cab_menu_file_info_send_to_chat: {
                logDebug("Send chat option");
                // Have the flag to stop triggering multiple selection page
                if (!isSelectingChat) {
                    sendToChat();
                    isSelectingChat = true;
                }
                break;
            }
		}
		return super.onOptionsItemSelected(item);
	}

	void sendToChat () {
        if(node==null){
            logWarning("The selected node is NULL");
            return;
        }

        if (nC == null) {
            nC =  new NodeController(this);
        }

        nC.checkIfNodeIsMineAndSelectChatsToSendNode(node);
    }

	private void refreshProperties(){
        logDebug("refreshProperties");

		if(node==null){
			finish();
		}

		boolean result=true;

		if(node.isExported()){
			publicLink=true;
            dividerLinkLayout.setVisibility(View.VISIBLE);
			publicLinkLayout.setVisibility(View.VISIBLE);
			publicLinkCopyLayout.setVisibility(View.VISIBLE);
			publicLinkText.setText(node.getPublicLink());
		}
		else{
			publicLink=false;
            dividerLinkLayout.setVisibility(View.GONE);
			publicLinkLayout.setVisibility(View.GONE);
			publicLinkCopyLayout.setVisibility(View.GONE);
		}

		if (node.isFile()){
            logDebug("Node is FILE");
			sharedLayout.setVisibility(View.GONE);
			dividerSharedLayout.setVisibility(View.GONE);
			sizeTitleTextView.setText(getString(R.string.file_properties_info_size_file));

			sizeTextView.setText(Formatter.formatFileSize(this, node.getSize()));

			contentLayout.setVisibility(View.GONE);

			if (node.getCreationTime() != 0){

				try {addedTextView.setText(formatLongDateTime(node.getCreationTime()));}catch(Exception ex)	{addedTextView.setText("");}

				if (node.getModificationTime() != 0){
					try {modifiedTextView.setText(formatLongDateTime(node.getModificationTime()));}catch(Exception ex)	{modifiedTextView.setText("");}
				}
				else{
					try {modifiedTextView.setText(formatLongDateTime(node.getCreationTime()));}catch(Exception ex)	{modifiedTextView.setText("");}
				}
			}
			else{
				addedTextView.setText("");
				modifiedTextView.setText("");
			}

			Bitmap thumb = null;
			Bitmap preview = null;
			thumb = getThumbnailFromCache(node);
			if (thumb != null){
				imageToolbarView.setImageBitmap(thumb);
				imageToolbarLayout.setVisibility(View.VISIBLE);
				iconToolbarLayout.setVisibility(View.GONE);
			}
			else{
				thumb = getThumbnailFromFolder(node, this);
				if (thumb != null){
					imageToolbarView.setImageBitmap(thumb);
					imageToolbarLayout.setVisibility(View.VISIBLE);
					iconToolbarLayout.setVisibility(View.GONE);
				}
			}
			preview = getPreviewFromCache(node);
			if (preview != null){
				previewCache.put(node.getHandle(), preview);
				imageToolbarView.setImageBitmap(preview);
				imageToolbarLayout.setVisibility(View.VISIBLE);
				iconToolbarLayout.setVisibility(View.GONE);
			}
			else{
				preview = getPreviewFromFolder(node, this);
				if (preview != null){
					previewCache.put(node.getHandle(), preview);
					imageToolbarView.setImageBitmap(preview);
					imageToolbarLayout.setVisibility(View.VISIBLE);
					iconToolbarLayout.setVisibility(View.GONE);
				}
				else{
					if (node.hasPreview()){
						File previewFile = new File(getPreviewFolder(this), node.getBase64Handle()+".jpg");
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

			contentTextView.setText(getInfoFolder(node, this));

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
									setOwnerState(user.getHandle());
									createDefaultAvatar(ownerRoundeImage, user, contactDB.getName());
								}
								else{
									ownerLabel.setText(user.getEmail());
									ownerInfo.setText(user.getEmail());
                                    setOwnerState(user.getHandle());
									createDefaultAvatar(ownerRoundeImage, user, user.getEmail());
								}
							}
							else{
                                logWarning("The contactDB is null: ");
								ownerLabel.setText(user.getEmail());
								ownerInfo.setText(user.getEmail());
                                setOwnerState(user.getHandle());
								createDefaultAvatar(ownerRoundeImage, user, user.getEmail());
							}
						}
						else{
							ownerLabel.setText(mS.getUser());
							ownerInfo.setText(mS.getUser());
                            setOwnerState(-1);
							createDefaultAvatar(ownerRoundeImage, user, mS.getUser());
						}


						File avatar = buildAvatarFile(this, contactMail + ".jpg");
						Bitmap bitmap = null;
						if (isFileAvailable(avatar)){
							if (avatar.length() > 0){
								BitmapFactory.Options bOpts = new BitmapFactory.Options();
								bOpts.inPurgeable = true;
								bOpts.inInputShareable = true;
								bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
								if (bitmap == null) {
									avatar.delete();
                                    megaApi.getUserAvatar(user,buildAvatarFile(this, contactMail + ".jpg").getAbsolutePath(), this);
                                }
								else{
									ownerLetter.setVisibility(View.GONE);
									ownerRoundeImage.setImageBitmap(bitmap);
								}
							}
							else{
                                megaApi.getUserAvatar(user,buildAvatarFile(this, contactMail + ".jpg").getAbsolutePath(), this);
							}
						}
						else{
                            megaApi.getUserAvatar(user,buildAvatarFile(this, contactMail + ".jpg").getAbsolutePath(), this);
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
					//If I am the owner
					if (megaApi.checkAccess(node, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK){
						permissionInfo.setVisibility(View.GONE);
					}
					else{

						owner = false;
						//If I am not the owner
						//permissionsLayout.setVisibility(View.VISIBLE);
						permissionInfo.setVisibility(View.VISIBLE);

						int accessLevel= megaApi.getAccess(node);
                        logDebug("Node: " + node.getHandle());

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
        if (availableOffline(this, node)) {
            availableOfflineBoolean = true;
            availableOfflineView.setText(R.string.context_delete_offline);
            offlineSwitch.setChecked(true);
            return;
        }

        availableOfflineBoolean = false;
        availableOfflineView.setText(R.string.save_for_offline);
        offlineSwitch.setChecked(false);
	}

	public void createDefaultAvatar(ImageView ownerRoundeImage, MegaUser user, String name){
        logDebug("createDefaultAvatar()");

		Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		String color = megaApi.getUserAvatarColor(user);
		if(color!=null){
            logDebug("The color to set the avatar is " + color);
			p.setColor(Color.parseColor(color));
		}
		else{
            logDebug("Default color to the avatar");
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
        logDebug("DENSITY: " + density + ":::: " + avatarTextSize);

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

    private void sharedContactClicked() {
        FrameLayout sharedContactLayout = (FrameLayout)findViewById(R.id.shared_contact_list_container);
        if (isShareContactExpanded) {
            if (sl != null) {
                usersSharedWithTextButton.setText(sl.size() + " " + getResources().getQuantityString(R.plurals.general_num_users, sl.size()));
            }
            sharedContactLayout.setVisibility(View.GONE);
        } else {
            usersSharedWithTextButton.setText(R.string.general_close);
            sharedContactLayout.setVisibility(View.VISIBLE);
        }

        isShareContactExpanded = !isShareContactExpanded;
    }

	@Override
	public void onClick(View v) {

        hideMultipleSelect();
		switch (v.getId()) {
			case R.id.file_properties_text_number_versions:{
                Intent i = new Intent(this, VersionsFileActivity.class);
                i.putExtra("handle", node.getHandle());
                startActivityForResult(i, REQUEST_CODE_DELETE_VERSIONS_HISTORY);
				break;
			}
			case R.id.file_properties_link_button:{
                logDebug("Copy link button");
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
					android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					clipboard.setText(node.getPublicLink());
				} else {
					android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", node.getPublicLink());
					clipboard.setPrimaryClip(clip);
				}
				showSnackbar(SNACKBAR_TYPE, getString(R.string.file_properties_get_link), -1);
				break;
			}
			case R.id.file_properties_shared_layout:
			case R.id.file_properties_shared_info_button:{
                sharedContactClicked();
				break;
			}
            case R.id.more_button:
                Intent i = new Intent(this, FileContactListActivityLollipop.class);
                i.putExtra("name", node.getHandle());
                startActivity(i);
                break;
			case R.id.file_properties_switch:{
				boolean isChecked = offlineSwitch.isChecked();

				if(owner){
                    logDebug("Owner: me");
					if (!isChecked){
                        logDebug("isChecked");
                        isRemoveOffline = true;
                        handle = node.getHandle();
						availableOfflineBoolean = false;
                        availableOfflineView.setText(R.string.save_for_offline);
						offlineSwitch.setChecked(false);
						mOffDelete = dbH.findByHandle(handle);
                        removeOffline(mOffDelete, dbH, this);
						supportInvalidateOptionsMenu();
					}
					else{
                        logDebug("NOT Checked");
                        isRemoveOffline = false;
                        handle = -1;
						availableOfflineBoolean = true;
                        availableOfflineView.setText(R.string.context_delete_offline);
						offlineSwitch.setChecked(true);

						File destination = getOfflineParentFile(this, from, node, megaApi);
                        logDebug("Path destination: " + destination);

						if (isFileAvailable(destination) && destination.isDirectory()){
							File offlineFile = new File(destination, node.getName());
							if (offlineFile.exists() && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getName())){ //This means that is already available offline
								return;
							}
						}

                        logDebug("Handle to save for offline : " + node.getHandle());
                        saveOffline(destination, node, this, fileInfoActivity, megaApi);

						supportInvalidateOptionsMenu();
					}
				}
				else{
                    logDebug("Not owner");
					if (!isChecked){
						availableOfflineBoolean = false;
                        availableOfflineView.setText(R.string.save_for_offline);
						offlineSwitch.setChecked(false);
						mOffDelete = dbH.findByHandle(node.getHandle());
                        removeOffline(mOffDelete, dbH, this);
						supportInvalidateOptionsMenu();
					}
					else{
						availableOfflineBoolean = true;
                        availableOfflineView.setText(R.string.context_delete_offline);
						offlineSwitch.setChecked(true);

						supportInvalidateOptionsMenu();

                        logDebug("Checking the node" + node.getHandle());

						//check the parent
						long result = -1;
						result= findIncomingParentHandle(node, megaApi);
                        logDebug("IncomingParentHandle: " + result);
						if(result!=-1){
							File destination = getOfflineParentFile(this, INCOMING_REQUEST_ADAPTER, node, megaApi);

							if (isFileAvailable(destination) && destination.isDirectory()){
								File offlineFile = new File(destination, node.getName());
								if (offlineFile.exists() && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getName())){ //This means that is already available offline
									return;
								}
							}
							saveOffline(destination, node, this, fileInfoActivity, megaApi);
						}
						else{
                            logWarning("result=findIncomingParentHandle NOT result!");
						}
					}
				}
				break;
			}
            case R.id.file_properties_info_data_location:{

                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                intent.setAction(ACTION_OPEN_FOLDER);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("locationFileInfo", true);
                if (adapterType == OFFLINE_ADAPTER){
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
        logDebug("leaveIncomingShare");

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
        logDebug("moveToTrash");

		final long handle = node.getHandle();
		moveToRubbish = false;
		if (!isOnline(this)){
			showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}

		if(isFinishing()){
			return;
		}

		final MegaNode rubbishNode = megaApi.getRubbishNode();

		MegaNode parent = nC.getParent(node);

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
        logDebug("showRenameDialog");

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, node.isFolder());
		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
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
		params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

		final RelativeLayout error_layout = new RelativeLayout(FileInfoActivityLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(FileInfoActivityLollipop.this);
		error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
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
		params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(FileInfoActivityLollipop.this, R.color.login_warning));

		error_layout.setVisibility(View.GONE);

		input.getBackground().mutate().clearColorFilter();
		input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
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
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
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
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
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
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
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

		if(!isOnline(this)){
			showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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

        logDebug("Renaming " + node.getName() + " to " + newName);

		megaApi.renameNode(node, newName, this);
	}

	public void showGetLinkActivity(long handle){
        logDebug("Handle: " + handle);
		Intent linkIntent = new Intent(this, GetLinkActivityLollipop.class);
		linkIntent.putExtra("handle", handle);
		startActivity(linkIntent);
	}

	public void setIsGetLink(boolean value){
		this.isGetLink = value;
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart: " + request.getName());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        if(adapter!=null){
            if(adapter.isMultipleSelect()){
                adapter.clearSelections();
                hideMultipleSelect();
            }
        }

        logDebug("onRequestFinish: " + request.getType() + "__" + request.getRequestString());

		if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE){
			if (e.getErrorCode() == MegaError.API_OK){
				File previewDir = getPreviewFolder(this);
				File preview = new File(previewDir, node.getBase64Handle()+".jpg");
				if (preview.exists()) {
					if (preview.length() > 0) {
						Bitmap bitmap = getBitmapForCache(preview, this);
						previewCache.put(node.getHandle(), bitmap);
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
                logDebug("Num versions: " + numVersions);
                if(numVersions>0){
                    folderVersionsLayout.setVisibility(View.VISIBLE);
                    String text = getResources().getQuantityString(R.plurals.number_of_versions_inside_folder, numVersions, numVersions);
                    folderVersionsText.setText(text);

                    long currentVersions = info.getCurrentSize();
                    logDebug("Current versions: " + currentVersions);
                    if(currentVersions>0){
                        folderCurrentVersionsText.setText(getSizeString(currentVersions));
                        folderCurrentVersionsLayout.setVisibility(View.VISIBLE);
                    }

                }
                else{
                    folderVersionsLayout.setVisibility(View.GONE);
                    folderCurrentVersionsLayout.setVisibility(View.GONE);
                }

                long previousVersions = info.getVersionsSize();
                logDebug("Previous versions: " + previousVersions);
                if(previousVersions>0){
                    folderPreviousVersionsText.setText(getSizeString(previousVersions));
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
			    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_renamed), -1);
				collapsingToolbar.setTitle(megaApi.getNodeByHandle(request.getNodeHandle()).getName().toUpperCase());
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_renamed), -1);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (moveToRubbish){
				moveToRubbish = false;
                logDebug("Move to rubbish request finished");
			} else {
			    logDebug("Move nodes request finished");
			}

            if (e.getErrorCode() == MegaError.API_OK) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved), -1);
                Intent intent = new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                finish();
            } else{
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
            }

		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			if (versionsToRemove > 0) {
                logDebug("Remove request finished");
                if (e.getErrorCode() == MegaError.API_OK){
                    versionsRemoved++;
                }
                else{
                    errorVersionRemove++;
                }

                if (versionsRemoved+errorVersionRemove == versionsToRemove) {
                    if (versionsRemoved == versionsToRemove) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.version_history_deleted), -1);
                    }
                    else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.version_history_deleted_erroneously)
                                + getResources().getQuantityString(R.plurals.versions_deleted_succesfully, versionsRemoved)
                                + getResources().getQuantityString(R.plurals.versions_not_deleted, errorVersionRemove), -1);
                    }
                    versionsToRemove = 0;
                    versionsRemoved = 0;
                    errorVersionRemove = 0;
                }
            }
            else {
                logDebug("Remove request finished");
                if (e.getErrorCode() == MegaError.API_OK){
                    finish();
                }
                else{
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_removed), -1);
                }
            }
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				if (request.getEmail() != null){
				    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied_contact) + request.getEmail(), -1);
				}
				else{
				    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied), -1);
				}
			}
            else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
                logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                intent.setAction(ACTION_OVERQUOTA_STORAGE);
                startActivity(intent);
                finish();

            }
            else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
                logWarning("PRE OVERQUOTA ERROR: " + e.getErrorCode());
                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
                startActivity(intent);
                finish();
            }
			else{
			    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
			}
            logDebug("Copy nodes request finished");
		}else if (request.getType() == MegaRequest.TYPE_SHARE){
            logDebug("MegaRequest.TYPE_SHARE");
            String message;
            if (statusDialog != null) {
                statusDialog.dismiss();
            }
            if (e.getErrorCode() == MegaError.API_OK) {
                if (removeShare) {
                    logDebug("OK remove");
                    removeShare = false;
                    adapter.setShareList(listContacts);
                    listView.invalidate();
                    message = getString(R.string.context_share_correctly_removed);
                } else if (changeShare) {
                    logDebug("OK change");
                    permissionsDialog.dismiss();
                    changeShare = false;
                    adapter.setShareList(listContacts);
                    listView.invalidate();
                    message = getString(R.string.context_permissions_changed);
                } else {
                    message = getString(R.string.context_correctly_shared);
                }
            } else {
                if (removeShare) {
                    logWarning("ERROR remove");
                    removeShare = false;
                    message = getString(R.string.context_contact_not_removed);
                } else if (changeShare) {
                    logWarning("ERROR change");
                    changeShare = false;
                    message = getString(R.string.context_permissions_not_changed);
                }else{
                    message = getString(R.string.context_no_shared);
                }
            }
            showSnackbar(SNACKBAR_TYPE,message,-1);
            logDebug("Finish onRequestFinish");
        } else if(request.getType() == MegaApiJava.USER_ATTR_AVATAR){
			try{
				statusDialog.dismiss();
			}catch (Exception ex){}

			if (e.getErrorCode() == MegaError.API_OK){
				boolean avatarExists = false;
				if (contactMail.compareTo(request.getEmail()) == 0){
					File avatar = buildAvatarFile(this, contactMail + ".jpg");
					Bitmap bitmap = null;
					if (isFileAvailable(avatar)){
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
        logWarning("onRequestTemporaryError: " + request.getName());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logDebug("onActivityResult " + requestCode + "____" + resultCode);

        if (requestCode == REQUEST_CODE_SELECT_CHAT) {
            logDebug("Select chat has been finished");
            isSelectingChat = false;
        }

		if (intent == null) {
			return;
		}

		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
            logDebug("Local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
            logDebug("URL: " + url + "___SIZE: " + size);


            if(nC==null){
                nC = new NodeController(this);
            }
            nC.checkSizeBeforeDownload(parentPath, url, size, hashes, false);
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {

			if(!isOnline(this)){
				showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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
			if(!isOnline(this)){
				showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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
                    logDebug("cN != null, i = " + i + " of " + copyHandles.length);
					megaApi.copyNode(cN, parent, this);
				}
				else{
                    logDebug("cN == null, i = " + i + " of " + copyHandles.length);
					try {
						statusDialog.dismiss();
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
					}
					catch (Exception ex) {}
				}
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			if(!isOnline(this)){
				showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);

            if (node.isFolder()){
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(fileInfoActivityLollipop, R.style.AppCompatAlertDialogStyleAddContacts);
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

                                    if(u!=null){
                                        logDebug("Share: " + node.getName() + " to " + u.getEmail());
                                        megaApi.share(node, u, MegaShare.ACCESS_READ, fileInfoActivity);
                                    }
                                    else{
                                        logDebug("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                                        megaApi.share(node, contactsData.get(i), MegaShare.ACCESS_READ, fileInfoActivity);
                                    }
                                }
                                break;
                            }
                            case 1:{
                                for (int i=0;i<contactsData.size();i++){
                                    MegaUser u = megaApi.getContact(contactsData.get(i));
                                    if(u!=null){
                                        logDebug("Share: " + node.getName() + " to " + u.getEmail());
                                        megaApi.share(node, u, MegaShare.ACCESS_READWRITE, fileInfoActivity);
                                    }
                                    else{
                                        logDebug("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                                        megaApi.share(node, contactsData.get(i), MegaShare.ACCESS_READWRITE, fileInfoActivity);
                                    }
                                }
                                break;
                            }
                            case 2:{
                                for (int i=0;i<contactsData.size();i++){
                                    MegaUser u = megaApi.getContact(contactsData.get(i));
                                    if(u!=null){
                                        logDebug("Share: " + node.getName() + " to " + u.getEmail());
                                        megaApi.share(node, u, MegaShare.ACCESS_FULL, fileInfoActivity);
                                    }
                                    else{
                                        logDebug("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                                        megaApi.share(node, contactsData.get(i), MegaShare.ACCESS_FULL, fileInfoActivity);
                                    }
                                }
                                break;
                            }
                        }
                    }
                });
                permissionsDialog = dialogBuilder.create();
                permissionsDialog.show();
            }
            else{
                logWarning("ERROR, the file is not folder");
            }
		}
        else if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK){
            long[] chatHandles = intent.getLongArrayExtra("SELECTED_CHATS");
            long[] contactHandles = intent.getLongArrayExtra("SELECTED_USERS");
            logDebug("Send to " + (chatHandles.length+contactHandles.length) + " chats");

            long[] nodeHandles = intent.getLongArrayExtra("NODE_HANDLES");
            logDebug("Send " + nodeHandles.length + " nodes");

            if ((chatHandles != null && chatHandles.length > 0) || (contactHandles != null && contactHandles.length > 0)) {
                if (contactHandles != null && contactHandles.length > 0) {
                    ArrayList<MegaChatRoom> chats = new ArrayList<>();
                    ArrayList<MegaUser> users = new ArrayList<>();

                    for (int i=0; i<contactHandles.length; i++) {
                        MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]));
                        if (user != null) {
                            users.add(user);
                        }
                    }

                    if (chatHandles != null) {
                        for (int i = 0; i < chatHandles.length; i++) {
                            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandles[i]);
                            if (chatRoom != null) {
                                chats.add(chatRoom);
                            }
                        }
                    }

                    if(nodeHandles!=null){
                        CreateChatToPerformActionListener listener = new CreateChatToPerformActionListener(chats, users, nodeHandles[0], this, CreateChatToPerformActionListener.SEND_FILE);
                        for (MegaUser user : users) {
                            MegaChatPeerList peers = MegaChatPeerList.createInstance();
                            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                            megaChatApi.createChat(false, peers, listener);
                        }
                    }
                    else{
                        logWarning("Error on sending to chat");
                    }
                }
                else {
                    countChat = chatHandles.length;
                    for (int i = 0; i < chatHandles.length; i++) {
                        megaChatApi.attachNode(chatHandles[i], nodeHandles[0], this);
                    }
                }
            }
		}
		else if (requestCode == REQUEST_CODE_DELETE_VERSIONS_HISTORY && resultCode == RESULT_OK) {
            if(!isOnline(this)){
                showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
                return;
            }
            if (intent.getBooleanExtra("deleteVersionHistory", false)) {
                ArrayList<MegaNode> versions = megaApi.getVersions(node);
                versionsToRemove = versions.size() -1;
                for (int i=1; i<versions.size(); i++) {
                    megaApi.removeVersion(versions.get(i), this);
                }
            }
        }
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
        logDebug("onUsersUpdate");
	}

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        logDebug("onUserAlertsUpdate");
    }

    @Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
        logDebug("onNodesUpdate");

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
            logDebug("Not related to this node");
			return;
		}

		//Check if the parent handle has changed
		if(n!=null){
			if(n.hasChanged(MegaNode.CHANGE_TYPE_PARENT)){
				MegaNode oldParent = megaApi.getParentNode(node);
				MegaNode newParent = megaApi.getParentNode(n);
				if(oldParent.getHandle()==newParent.getHandle()){
                    logDebug("Parents match");
					if(newParent.isFile()){
                        logDebug("New version added");
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
            logDebug("Node HAS public link");
			publicLink=true;
            dividerLinkLayout.setVisibility(View.VISIBLE);
			publicLinkLayout.setVisibility(View.VISIBLE);
			publicLinkCopyLayout.setVisibility(View.VISIBLE);
			publicLinkText.setText(node.getPublicLink());
			supportInvalidateOptionsMenu();


		}else{
            logDebug("Node NOT public link");
			publicLink=false;
            dividerLinkLayout.setVisibility(View.GONE);
			publicLinkLayout.setVisibility(View.GONE);
			publicLinkCopyLayout.setVisibility(View.GONE);
			supportInvalidateOptionsMenu();

		}

		if (node.isFolder()){
			long sizeFile=megaApi.getSize(node);
			sizeTextView.setText(Formatter.formatFileSize(this, sizeFile));

			contentTextView.setText(getInfoFolder(node, this));

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
                    logDebug("sl.size == 0");
					sharedLayout.setVisibility(View.GONE);
					dividerSharedLayout.setVisibility(View.GONE);

					//If I am the owner
					if (megaApi.checkAccess(node, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK){
						permissionInfo.setVisibility(View.GONE);
					}
					else{

						//If I am not the owner
						owner = false;
						permissionInfo.setVisibility(View.VISIBLE);
						int accessLevel= megaApi.getAccess(node);
                        logDebug("Node: " + node.getHandle());

						switch(accessLevel){
							case MegaShare.ACCESS_OWNER:
							case MegaShare.ACCESS_FULL:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_full_access).toUpperCase(Locale.getDefault()));
								break;
							}
							case MegaShare.ACCESS_READ:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_only).toUpperCase(Locale.getDefault()));

								break;
							}
							case MegaShare.ACCESS_READWRITE:{
								permissionInfo.setText(getResources().getString(R.string.file_properties_shared_folder_read_write).toUpperCase(Locale.getDefault()));
								break;
							}
						}
					}
				}
				else{
					sharedLayout.setVisibility(View.VISIBLE);
					dividerSharedLayout.setVisibility(View.VISIBLE);
                    usersSharedWithTextButton.setText((sl.size()) + " " + getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
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

        refresh();
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
        logDebug("onReloadNeeded");
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
        drawableDownload.setColorFilter(null);
        drawableLeave.setColorFilter(null);
        drawableCopy.setColorFilter(null);
        drawableChat.setColorFilter(null);
    }

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}

//	@Override
//	protected void onResume() {
//		log("onResume-FileInfoActivityLollipop");
//		super.onResume();
//
//        if (adapterType != OFFLINE_ADAPTER){
//            refreshProperties();
//            supportInvalidateOptionsMenu();
//        }
//	}

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
        retryConnectionsAndSignalPresence();

        if(isRemoveOffline){
            Intent intent = new Intent();
            intent.putExtra(NODE_HANDLE, handle);
            setResult(RESULT_OK, intent);
        }

        super.onBackPressed();
	}
	
	public void editMultipleSharesCallback(String message){
	    if(statusDialog != null){
            statusDialog.dismiss();
        }
        
        if(adapter!=null){
            if(adapter.isMultipleSelect()){
                adapter.clearSelections();
                hideMultipleSelect();
            }
        }
        showSnackbar(SNACKBAR_TYPE, message, -1);
    }

	public void showSnackbar(int type, String s, long idChat){
	    showSnackbar(type, fragmentContainer, s, idChat);
	}

    public void openAdvancedDevices(long handleToDownload, boolean highPriority) {
        logDebug("handleToDownload: " + handleToDownload + ", highPriority: " + highPriority);
        String externalPath = getExternalCardPath();

        if(externalPath!=null){
            logDebug("ExternalPath for advancedDevices: " + externalPath);
            MegaNode node = megaApi.getNodeByHandle(handleToDownload);
            if(node!=null){

                File newFile =  new File(node.getName());
                logDebug("File: " + newFile.getPath());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Create a file with the requested MIME type.
                String mimeType = MimeTypeList.getMimeType(newFile);
                logDebug("Mimetype: " + mimeType);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_TITLE, node.getName());
                intent.putExtra("handleToDownload", handleToDownload);
                intent.putExtra(HIGH_PRIORITY_TRANSFER, highPriority);
                try{
                    startActivityForResult(intent, WRITE_SD_CARD_REQUEST_CODE);
                }
                catch(Exception e){
                    logError("Exception in External SDCARD", e);
                    Environment.getExternalStorageDirectory();
                    Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
        else{
            logWarning("No external SD card");
            Environment.getExternalStorageDirectory();
            Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes, final boolean highPriority){
        logDebug("askSizeConfirmationBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

        builder.setMessage(getString(R.string.alert_larger_file, getSizeString(sizeC)));
        builder.setPositiveButton(getString(R.string.general_save_to_device),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskSizeDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(fileInfoActivity);
                        }
                        nC.checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
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

    public void askConfirmationNoAppInstaledBeforeDownload (String parentPath, String url, long size, long [] hashes, String nodeToDownload, final boolean highPriority){
        logDebug("askConfirmationNoAppInstaledBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

        builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
        builder.setPositiveButton(getString(R.string.general_save_to_device),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskNoAppDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(fileInfoActivity);
                        }
                        nC.download(parentPathC, urlC, sizeC, hashesC, highPriority);
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

    public void itemClick(int position) {
        logDebug("Position: " + position);

        if (adapter.isMultipleSelect()) {
            adapter.toggleSelection(position);
            updateActionModeTitle();
        } else {
            String megaUser = listContacts.get(position).getUser();
            MegaUser contact = megaApi.getContact(megaUser);
            if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                Intent i = new Intent(this,ContactInfoActivityLollipop.class);
                i.putExtra("name",megaUser);
                startActivity(i);
            }

        }
    }

    public void showOptionsPanel(MegaShare sShare){
        logDebug("showNodeOptionsPanel");
        if(node!=null){
            this.selectedShare = sShare;
            FileContactsListBottomSheetDialogFragment bottomSheetDialogFragment = new FileContactsListBottomSheetDialogFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }


    public void hideMultipleSelect() {
	    if(adapter != null){
            adapter.setMultipleSelect(false);
        }

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public MegaUser getSelectedContact() {
        String email = selectedShare.getUser();
        return megaApi.getContact(email);
    }

    public MegaShare getSelectedShare() {
        return selectedShare;
    }

    public void changePermissions(){
        logDebug("changePermissions");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleAddContacts);
        dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
        final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
        dialogBuilder.setSingleChoiceItems(items, selectedShare.getAccess(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                removeShare = false;
                changeShare = true;
                ProgressDialog temp = null;
                try{
                    temp = new ProgressDialog(fileInfoActivityLollipop);
                    temp.setMessage(getString(R.string.context_permissions_changing_folder));
                    temp.show();
                }
                catch(Exception e){
                    return;
                }
                statusDialog = temp;
                permissionsDialog.dismiss();

                switch(item) {
                    case 0:{
                        MegaUser u = megaApi.getContact(selectedShare.getUser());
                        if(u!=null){
                            megaApi.share(node, u, MegaShare.ACCESS_READ, fileInfoActivityLollipop);
                        }
                        else{
                            megaApi.share(node, selectedShare.getUser(), MegaShare.ACCESS_READ, fileInfoActivityLollipop);
                        }

                        break;
                    }
                    case 1:{
                        MegaUser u = megaApi.getContact(selectedShare.getUser());
                        if(u!=null){
                            megaApi.share(node, u, MegaShare.ACCESS_READWRITE, fileInfoActivityLollipop);
                        }
                        else{
                            megaApi.share(node, selectedShare.getUser(), MegaShare.ACCESS_READWRITE, fileInfoActivityLollipop);
                        }
                        break;
                    }
                    case 2:{
                        MegaUser u = megaApi.getContact(selectedShare.getUser());
                        if(u!=null){
                            megaApi.share(node, u, MegaShare.ACCESS_FULL, fileInfoActivityLollipop);
                        }
                        else{
                            megaApi.share(node, selectedShare.getUser(), MegaShare.ACCESS_FULL, fileInfoActivityLollipop);
                        }
                        break;
                    }
                }
            }
        });
        permissionsDialog = dialogBuilder.create();
        permissionsDialog.show();
    }

    public void removeFileContactShare(){
        logDebug("removeFileContactShare");
        showConfirmationRemoveContactFromShare(selectedShare.getUser());
    }

    public void showConfirmationRemoveContactFromShare (final String email){
        logDebug("showConfirmationRemoveContactFromShare");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
                        removeShare(email);
                        break;
                    }
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        String message= getResources().getString(R.string.remove_contact_shared_folder,email);
        builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void removeShare (String email)
    {
        ProgressDialog temp = new ProgressDialog(this);
            temp.setMessage(getString(R.string.context_removing_contact_folder));
            temp.show();
    
        removeShare = true;
        changeShare = false;
        statusDialog = temp;
        if (email != null){
            megaApi.share(node, email, MegaShare.ACCESS_UNKNOWN, this);
        }
        else{
            megaApi.disableExport(node, this);
        }
    }

    public void refresh(){
        setContactList();
        setMoreButtonText();

        adapter.setShareList(listContacts);
        adapter.notifyDataSetChanged();
    }

    private void setContactList() {

        fullListContacts = new ArrayList<>();
        listContacts = new ArrayList<>();
        if (node != null) {
            fullListContacts = megaApi.getOutShares(node);

            if (fullListContacts.size() > MAX_NUMBER_OF_CONTACTS_IN_LIST) {
                listContacts = new ArrayList<>(fullListContacts.subList(0,MAX_NUMBER_OF_CONTACTS_IN_LIST));
            } else {
                listContacts = fullListContacts;
            }
        }
    }

    private void setMoreButtonText() {
        int fullSize = fullListContacts.size();
        if (fullSize > MAX_NUMBER_OF_CONTACTS_IN_LIST) {
            moreButton.setVisibility(View.VISIBLE);
            moreButton.setText((fullSize - MAX_NUMBER_OF_CONTACTS_IN_LIST) + " " + getResources().getString(R.string.label_more).toUpperCase());
        } else {
            moreButton.setVisibility(View.GONE);
        }
    }

    public void showConfirmationRemoveMultipleContactFromShare (final List<MegaShare> contacts){
        logDebug("showConfirmationRemoveMultipleContactFromShare");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
                        removeMultipleShares(contacts);
                        break;
                    }
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        String message= getResources().getString(R.string.remove_multiple_contacts_shared_folder,contacts.size());
        builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void removeMultipleShares(List<MegaShare> shares){
        logDebug("removeMultipleShares");
        removeShare = true;
        changeShare = false;
        ProgressDialog temp = null;
        try{
            temp = new ProgressDialog(this);
            temp.setMessage(getString(R.string.context_removing_contact_folder));
            temp.show();
        }
        catch(Exception e){
            return;
        }
        statusDialog = temp;

        FileContactMultipleRequestListener removeMultipleListener = new FileContactMultipleRequestListener(MULTIPLE_REMOVE_CONTACT_SHARED_FOLDER, this, requestCompletedCallback);
        for(int j=0;j<shares.size();j++){
            if(shares.get(j).getUser()!=null){
                MegaUser u = megaApi.getContact(shares.get(j).getUser());
                if(u!=null){
                    megaApi.share(node, u, MegaShare.ACCESS_UNKNOWN, removeMultipleListener);
                }
                else{
                    megaApi.share(node, shares.get(j).getUser(), MegaShare.ACCESS_UNKNOWN, removeMultipleListener);
                }
            }
            else{
                megaApi.disableExport(node, removeMultipleListener);
            }
        }
    }
    
    // Clear all selected items
    private void clearSelections() {
        if(adapter.isMultipleSelect()){
            adapter.clearSelections();
        }
    }
    
    public void selectAll(){
        logDebug("selectAll");
        if (adapter != null){
            if(adapter.isMultipleSelect()){
                adapter.selectAll();
            }
            else{
                adapter.setMultipleSelect(true);
                adapter.selectAll();
                
                actionMode = startSupportActionMode(new ActionBarCallBack());
            }
            updateActionModeTitle();
        }
    }
    
    private void updateActionModeTitle() {
        logDebug("updateActionModeTitle");
        if (actionMode == null) {
            return;
        }
        List<MegaShare> contacts = adapter.getSelectedShares();
        if(contacts!=null){
            logDebug("Contacts selected: " + contacts.size());
        }
        
        Resources res = getResources();
        String format = "%d %s";
        
        actionMode.setTitle(String.format(format, contacts.size(),res.getQuantityString(R.plurals.general_num_contacts, contacts.size())));
        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            logError("Invalidate error", e);
            e.printStackTrace();
        }
    }

}
