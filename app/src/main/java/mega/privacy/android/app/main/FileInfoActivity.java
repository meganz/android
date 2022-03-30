package mega.privacy.android.app.main;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.os.Looper;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.namecollision.data.NameCollisionType;
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase;
import mega.privacy.android.app.usecase.CopyNodeUseCase;
import mega.privacy.android.app.usecase.MoveNodeUseCase;
import mega.privacy.android.app.usecase.exception.MegaNodeException;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.attacher.MegaAttacher;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.interfaces.ActionNodeCallback;
import mega.privacy.android.app.listeners.ShareListener;
import mega.privacy.android.app.main.adapters.MegaFileInfoSharedContactAdapter;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.LocationInfo;
import mega.privacy.android.app.utils.CameraUploadUtil;
import mega.privacy.android.app.utils.ContactUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
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

import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.CameraUploadUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.ColorUtils.getColorForElevation;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LinksUtil.showGetLinkActivity;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import javax.inject.Inject;

@SuppressLint("NewApi")
@AndroidEntryPoint
public class FileInfoActivity extends PasscodeActivity implements OnClickListener,
        MegaRequestListenerInterface, MegaGlobalListenerInterface, ActionNodeCallback,
        SnackbarShower {

    @Inject
    CheckNameCollisionUseCase checkNameCollisionUseCase;
    @Inject
    MoveNodeUseCase moveNodeUseCase;
    @Inject
    CopyNodeUseCase copyNodeUseCase;

	public static int MAX_WIDTH_FILENAME_LAND=400;
	public static int MAX_WIDTH_FILENAME_LAND_2=400;

	public static int MAX_WIDTH_FILENAME_PORT=170;
	public static int MAX_WIDTH_FILENAME_PORT_2=200;

	public static String NODE_HANDLE = "NODE_HANDLE";

	static int TYPE_EXPORT_GET = 0;
	static int TYPE_EXPORT_REMOVE = 1;
	static int TYPE_EXPORT_MANAGE = 2;
	static int FROM_FILE_BROWSER = 13;
    FileInfoActivity fileInfoActivity = this;
	boolean firstIncomingLevel=true;

    private final static String KEY_SELECTED_SHARE_HANDLE = "KEY_SELECTED_SHARE_HANDLE";

    private final MegaAttacher nodeAttacher = new MegaAttacher(this);
    private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
            AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));

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

	private boolean isShareContactExpanded = false;

	float scaleText;

	LinearLayout availableOfflineLayout;

	RelativeLayout sizeLayout;
	RelativeLayout locationLayout;
	RelativeLayout contentLayout;
	RelativeLayout addedLayout;
	RelativeLayout modifiedLayout;
	RelativeLayout publicLinkLayout;
	RelativeLayout publicLinkCopyLayout;
	TextView publicLinkText;
	private TextView publicLinkDate;
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
    SwitchMaterial offlineSwitch;

	TextView sizeTextView;
	TextView sizeTitleTextView;

    TextView locationTextView;

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
	EmojiTextView ownerLabel;
	TextView ownerLabelowner;
	TextView ownerInfo;
	ImageView ownerRoundeImage;
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

	private ContactController cC;

    AlertDialog statusDialog;
	boolean publicLink=false;

	private Handler handler;

	boolean moveToRubbish = false;

	public static int REQUEST_CODE_SELECT_CONTACT = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;

	Display display;
	DisplayMetrics outMetrics;
	float density;
	float scaleW;
	float scaleH;

	boolean shareIt = true;
	int from;

	DatabaseHandler dbH = null;

	AlertDialog permissionsDialog;

	String contactMail;
    boolean isRemoveOffline;
    long handle;

    private int adapterType;

 	private MegaShare selectedShare;
    final int MAX_NUMBER_OF_CONTACTS_IN_LIST = 5;
    private ArrayList<MegaShare> listContacts;
    private ArrayList<MegaShare> fullListContacts;
    private Button moreButton;
    private MegaFileInfoSharedContactAdapter adapter;
    private ActionMode actionMode;

    int versionsToRemove = 0;
    int versionsRemoved = 0;
    int errorVersionRemove = 0;

    private FileContactsListBottomSheetDialogFragment bottomSheetDialogFragment;

    private int currentColorFilter;

    private final BroadcastReceiver manageShareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (adapter != null) {
                if (adapter.isMultipleSelect()) {
                    adapter.clearSelections();
                    hideMultipleSelect();
                }
                adapter.setShareList(listContacts);
            }

            if (statusDialog != null) {
                statusDialog.dismiss();
            }

            if (permissionsDialog != null) {
                permissionsDialog.dismiss();
            }
        }
    };

    private final BroadcastReceiver contactUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;

            if (intent.getAction().equals(ACTION_UPDATE_NICKNAME)
                || intent.getAction().equals(ACTION_UPDATE_FIRST_NAME)
                || intent.getAction().equals(ACTION_UPDATE_LAST_NAME)
                || intent.getAction().equals(ACTION_UPDATE_CREDENTIALS)) {
                updateAdapter(intent.getLongExtra(EXTRA_USER_HANDLE, INVALID_HANDLE));
            }
        }
    };

    public void activateActionMode(){
        logDebug("activateActionMode");
        if (!adapter.isMultipleSelect()){
            adapter.setMultipleSelect(true);
            actionMode = startSupportActionMode(new ActionBarCallBack());
        }
    }

    @Override
    public void showSnackbar(int type, String content, long chatId) {
        showSnackbar(type, fragmentContainer, content, chatId);
    }

    @Override
    public void finishRenameActionWithSuccess(@NonNull String newName) {
        node = megaApi.getNodeByHandle(node.getHandle());

        if (node != null && collapsingToolbar != null) {
            collapsingToolbar.setTitle(node.getName());
        }
    }

    @Override
    public void actionConfirmed() {
        //No update needed
    }

    @Override
    public void createFolder(@NotNull String folderName) {
        //No action needed
    }

    private class ActionBarCallBack implements ActionMode.Callback {
        
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            logDebug("onActionItemClicked");
            final ArrayList<MegaShare> shares = adapter.getSelectedShares();
            switch(item.getItemId()){
                case R.id.action_file_contact_list_permissions:{

                    //Change permissions
                    MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(fileInfoActivity, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));

                    final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                    dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            clearSelections();
                            if(permissionsDialog != null){
                                permissionsDialog.dismiss();
                            }
                            statusDialog = createProgressDialog(fileInfoActivity, getString(R.string.context_permissions_changing_folder));
                            cC.changePermissions(cC.getEmailShares(shares), item, node);
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
                    break;
                }
                case R.id.cab_menu_unselect_all:{
                    clearSelections();
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
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            logDebug("onDestroyActionMode");
            adapter.clearSelections();
            adapter.setMultipleSelect(false);
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
                menu.findItem(R.id.cab_menu_select_all).setVisible(selected.size() != adapter.getItemCount());
                unselect.setTitle(getString(R.string.action_unselect_all));
                unselect.setVisible(true);
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

        if(shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return;
        }

        fileInfoActivity = this;
        handler = new Handler();
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

        cC = new ContactController(this);
        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

        adapterType = getIntent().getIntExtra("adapterType", FILE_BROWSER_ADAPTER);

        setContentView(R.layout.activity_file_info);

        permissionInfo = (TextView) findViewById(R.id.file_properties_permission_info);
        permissionInfo.setVisibility(View.GONE);

        fragmentContainer = findViewById(R.id.file_info_fragment_container);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        aB = getSupportActionBar();

        collapsingToolbar = findViewById(R.id.file_info_collapse_toolbar);

        nestedScrollView = findViewById(R.id.nested_layout);
        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> changeViewElevation(aB, v.canScrollVertically(-1) && v.getVisibility() == View.VISIBLE, outMetrics));

        aB.setDisplayShowTitleEnabled(false);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        iconToolbarLayout = findViewById(R.id.file_info_icon_layout);

        iconToolbarView = findViewById(R.id.file_info_toolbar_icon);
        CollapsingToolbarLayout.LayoutParams params = (CollapsingToolbarLayout.LayoutParams) iconToolbarLayout.getLayoutParams();
        Rect rect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        params.setMargins(dp2px(16, outMetrics), dp2px(90, outMetrics) + rect.top, 0, dp2px(14, outMetrics));
        iconToolbarLayout.setLayoutParams(params);

        imageToolbarLayout = findViewById(R.id.file_info_image_layout);
        imageToolbarView = findViewById(R.id.file_info_toolbar_image);
        imageToolbarLayout.setVisibility(View.GONE);

        //Available Offline Layout
        availableOfflineLayout = findViewById(R.id.available_offline_layout);
        availableOfflineLayout.setVisibility(View.VISIBLE);
        availableOfflineView = findViewById(R.id.file_properties_available_offline_text);
        offlineSwitch = findViewById(R.id.file_properties_switch);

        //Share with Layout
        sharedLayout = findViewById(R.id.file_properties_shared_layout);
        sharedLayout.setOnClickListener(this);
        usersSharedWithTextButton = findViewById(R.id.file_properties_shared_info_button);
        usersSharedWithTextButton.setOnClickListener(this);
        dividerSharedLayout = findViewById(R.id.divider_shared_layout);
        appBarLayout = findViewById(R.id.app_bar);

        //Owner Layout
        ownerLayout = findViewById(R.id.file_properties_owner_layout);
        ownerRoundeImage= findViewById(R.id.contact_list_thumbnail);
        ownerLinear = findViewById(R.id.file_properties_owner_linear);
        ownerLabel =  findViewById(R.id.file_properties_owner_label);
        ownerLabelowner = findViewById(R.id.file_properties_owner_label_owner);
        String ownerString = "("+getString(R.string.file_properties_owner)+")";
        ownerLabelowner.setText(ownerString);
        ownerInfo = findViewById(R.id.file_properties_owner_info);
        ownerState = findViewById(R.id.file_properties_owner_state_icon);
        if(!isScreenInPortrait(this)){
            ownerLabel.setMaxWidthEmojis(dp2px(MAX_WIDTH_FILENAME_LAND, outMetrics));
            ownerInfo.setMaxWidth(dp2px(MAX_WIDTH_FILENAME_LAND_2, outMetrics));
        }else{
            ownerLabel.setMaxWidthEmojis(dp2px(MAX_WIDTH_FILENAME_PORT, outMetrics));
            ownerInfo.setMaxWidth(dp2px(MAX_WIDTH_FILENAME_PORT_2, outMetrics));
        }
        ownerLayout.setVisibility(View.GONE);

        //Info Layout

        //Size Layout
        sizeLayout = findViewById(R.id.file_properties_size_layout);
        sizeTitleTextView  = findViewById(R.id.file_properties_info_menu_size);
        sizeTextView = findViewById(R.id.file_properties_info_data_size);

        //Folder Versions Layout
        folderVersionsLayout = findViewById(R.id.file_properties_folder_versions_layout);
        folderVersionsText = findViewById(R.id.file_properties_info_data_folder_versions);
        folderVersionsLayout.setVisibility(View.GONE);

        folderCurrentVersionsLayout = findViewById(R.id.file_properties_folder_current_versions_layout);
        folderCurrentVersionsText = findViewById(R.id.file_properties_info_data_folder_current_versions);
        folderCurrentVersionsLayout.setVisibility(View.GONE);

        folderPreviousVersionsLayout = findViewById(R.id.file_properties_folder_previous_versions_layout);
        folderPreviousVersionsText = findViewById(R.id.file_properties_info_data_folder_previous_versions);
        folderPreviousVersionsLayout.setVisibility(View.GONE);

        //Content Layout
        contentLayout = findViewById(R.id.file_properties_content_layout);
        contentTitleTextView  = findViewById(R.id.file_properties_info_menu_content);
        contentTextView = findViewById(R.id.file_properties_info_data_content);

        dividerLinkLayout = findViewById(R.id.divider_link_layout);
        publicLinkLayout = findViewById(R.id.file_properties_link_layout);
        publicLinkCopyLayout = findViewById(R.id.file_properties_copy_layout);

        publicLinkText = findViewById(R.id.file_properties_link_text);
        publicLinkDate = findViewById(R.id.file_properties_link_date);
        publicLinkButton = findViewById(R.id.file_properties_link_button);
        publicLinkButton.setText(getString(R.string.context_copy));
        publicLinkButton.setOnClickListener(this);

        //Added Layout
        addedLayout = findViewById(R.id.file_properties_added_layout);
        addedTextView = findViewById(R.id.file_properties_info_data_added);

        //Modified Layout
        modifiedLayout = findViewById(R.id.file_properties_created_layout);
        modifiedTextView = findViewById(R.id.file_properties_info_data_created);

        //Versions Layout
        versionsLayout = findViewById(R.id.file_properties_versions_layout);
        versionsButton = findViewById(R.id.file_properties_text_number_versions);
        separatorVersions = findViewById(R.id.separator_versions);

        megaApi.addGlobalListener(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            from = extras.getInt("from");
            if(from==FROM_INCOMING_SHARES){
                firstIncomingLevel = extras.getBoolean(INTENT_EXTRA_KEY_FIRST_LEVEL);
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

                if (isEmptyFolder(node)) {
                    availableOfflineLayout.setVisibility(View.GONE);

                    View view = findViewById(R.id.available_offline_separator);
                    if (view != null) {
                        view.setVisibility(View.GONE);
                    }
                }
            } else {
                modifiedLayout.setVisibility(View.VISIBLE);
            }

            String name = node.getName();

            collapsingToolbar.setTitle(name);
            if (nC == null) {
                nC = new NodeController(this);
            }
            MegaNode parent = nC.getParent(node);

            if (!node.isTakenDown() && parent.getHandle() != megaApi.getRubbishNode().getHandle()) {
                offlineSwitch.setEnabled(true);
                offlineSwitch.setOnCheckedChangeListener((view, isChecked) -> onClick(view));
                availableOfflineView.setTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087));
            } else {
                offlineSwitch.setEnabled(false);
                availableOfflineView.setTextColor(ContextCompat.getColor(this, R.color.grey_700_026_grey_300_026));
            }

            if(megaApi.hasVersions(node)){
                versionsLayout.setVisibility(View.VISIBLE);

                String text = getQuantityString(R.plurals.number_of_versions, megaApi.getNumVersions(node), megaApi.getNumVersions(node));
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

        RecyclerView listView = findViewById(R.id.file_info_contact_list_view);
        listView.setItemAnimator(noChangeRecyclerViewItemAnimator());
        listView.addItemDecoration(new SimpleDividerItemDecoration(this));
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(mLayoutManager);

        //get shared contact list and max number can be displayed in the list is five
        setContactList();

        moreButton = findViewById(R.id.more_button);
        moreButton.setOnClickListener(this);
        setMoreButtonText();

        //setup adapter
        adapter = new MegaFileInfoSharedContactAdapter(this,node,listContacts, listView);
        adapter.setShareList(listContacts);
        adapter.setPositionClicked(-1);
        adapter.setMultipleSelect(false);

        listView.setAdapter(adapter);

        refreshProperties();

        setIconResource();

        registerReceiver(manageShareReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE));

        IntentFilter contactUpdateFilter = new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE);
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS);
        registerReceiver(contactUpdateReceiver, contactUpdateFilter);

        getActionBarDrawables();

        int statusBarColor = getColorForElevation(this, getResources().getDimension(R.dimen.toolbar_elevation));
        collapsingToolbar.setStatusBarScrimColor(statusBarColor);

        if(isDarkMode(this)) {
            collapsingToolbar.setContentScrimColor(statusBarColor);
        }

        if (node.hasPreview() || node.hasThumbnail()) {
            appBarLayout.addOnOffsetChangedListener((appBarLayout, offset) -> {
                if (offset < 0 && Math.abs(offset) >= appBarLayout.getTotalScrollRange() / 2) {
                    // Collapsed
                    setActionBarDrawablesColorFilter(getResources().getColor(R.color.grey_087_white_087));
                } else {
                    setActionBarDrawablesColorFilter(getResources().getColor(R.color.white_alpha_087));
                }
            });

            collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.grey_087_white_087));
            collapsingToolbar.setExpandedTitleColor(getResources().getColor(R.color.white_alpha_087));
        } else {
            setActionBarDrawablesColorFilter(getResources().getColor(R.color.grey_087_white_087));
        }

        //Location Layout
        locationLayout = findViewById(R.id.file_properties_location_layout);
        locationTextView = findViewById(R.id.file_properties_info_data_location);

        LocationInfo locationInfo = getNodeLocationInfo(adapterType, from == FROM_INCOMING_SHARES,
                node.getHandle());
        if (locationInfo != null) {
            locationTextView.setText(locationInfo.getLocation());
            locationTextView.setOnClickListener(v -> {
                handleLocationClick(this, adapterType, locationInfo);
            });
        } else {
            locationLayout.setVisibility(View.GONE);
        }

        if(savedInstanceState != null){
            long handle = savedInstanceState.getLong(KEY_SELECTED_SHARE_HANDLE, INVALID_HANDLE);
            if(handle == INVALID_HANDLE || node == null){
                return;
            }
            ArrayList<MegaShare> list = megaApi.getOutShares(node);
            for (MegaShare share: list) {
                if(handle == share.getNodeHandle()){
                    selectedShare = share;
                    break;
                }
            }

            nodeAttacher.restoreState(savedInstanceState);
            nodeSaver.restoreState(savedInstanceState);
        }
	}

    private void getActionBarDrawables() {
        drawableDots = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_dots_vertical_white);
        if (drawableDots != null) {
            drawableDots = drawableDots.mutate();
        }

        upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_arrow_back_white);
        if (upArrow != null) {
            upArrow = upArrow.mutate();
        }

        drawableRemoveLink = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_remove_link);
        if (drawableRemoveLink != null) {
            drawableRemoveLink = drawableRemoveLink.mutate();
        }

        drawableLink = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_link_white);
        if (drawableLink != null) {
            drawableLink = drawableLink.mutate();
        }

        drawableShare = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_share);
        if (drawableShare != null) {
            drawableShare = drawableShare.mutate();
        }

        drawableDownload = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_download_white);
        if (drawableDownload != null) {
            drawableDownload = drawableDownload.mutate();
        }

        drawableLeave = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_leave_share_w);
        if (drawableLeave != null) {
            drawableLeave = drawableLeave.mutate();
        }

        drawableCopy = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_copy_white);
        if (drawableCopy != null) {
            drawableCopy.mutate();
        }

        drawableChat = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_send_to_contact);
        if (drawableChat != null) {
            drawableChat.mutate();
        }
    }

    void setOwnerState(long userHandle) {
        setContactStatus(megaChatApi.getUserOnlineStatus(userHandle), ownerState, StatusIconLocation.STANDARD);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_info_action, menu);

        downloadMenuItem = menu.findItem(R.id.cab_menu_file_info_download);
        shareMenuItem = menu.findItem(R.id.cab_menu_file_info_share_folder);
        getLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_get_link);
        getLinkMenuItem.setTitle(StringResourcesUtils.getQuantityString(R.plurals.get_links, 1));
        editLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_edit_link);
        removeLinkMenuItem = menu.findItem(R.id.cab_menu_file_info_remove_link);
        renameMenuItem = menu.findItem(R.id.cab_menu_file_info_rename);
        moveMenuItem = menu.findItem(R.id.cab_menu_file_info_move);
        copyMenuItem = menu.findItem(R.id.cab_menu_file_info_copy);
        rubbishMenuItem = menu.findItem(R.id.cab_menu_file_info_rubbish);
        deleteMenuItem = menu.findItem(R.id.cab_menu_file_info_delete);
        leaveMenuItem = menu.findItem(R.id.cab_menu_file_info_leave);
        sendToChatMenuItem = menu.findItem(R.id.cab_menu_file_info_send_to_chat);

        setIconsColorFilter();

        MegaNode parent = megaApi.getNodeByHandle(node.getHandle());

        if (parent != null) {
            parent = MegaNodeUtil.getRootParentNode(megaApi, parent);

            if (parent.getHandle() == megaApi.getRubbishNode().getHandle()) {
                deleteMenuItem.setVisible(true);
            } else {
                if (!node.isTakenDown() && !node.isFolder()) {
                    sendToChatMenuItem.setVisible(true);
                    sendToChatMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }

                if (from == FROM_INCOMING_SHARES) {
                    if (!node.isTakenDown()) {
                        downloadMenuItem.setVisible(true);
                        downloadMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        leaveMenuItem.setVisible(firstIncomingLevel);
                        leaveMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        copyMenuItem.setVisible(true);
                    }

                    switch (megaApi.getAccess(node)) {
                        case MegaShare.ACCESS_OWNER:
                        case MegaShare.ACCESS_FULL:
                            rubbishMenuItem.setVisible(!firstIncomingLevel);
                            renameMenuItem.setVisible(true);
                            break;

                        case MegaShare.ACCESS_READ:
                            copyMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                            break;
                    }
                } else {
                    if (!node.isTakenDown()) {
                        downloadMenuItem.setVisible(true);
                        downloadMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                        if (node.isFolder()) {
                            shareMenuItem.setVisible(true);
                            shareMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        }

                        if (node.isExported()) {
                            editLinkMenuItem.setVisible(true);
                            removeLinkMenuItem.setVisible(true);
                            removeLinkMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        } else {
                            getLinkMenuItem.setVisible(true);
                            getLinkMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        }

                        copyMenuItem.setVisible(true);
                    }

                    rubbishMenuItem.setVisible(true);
                    renameMenuItem.setVisible(true);
                    moveMenuItem.setVisible(true);
                }
            }
        }

        return super.onCreateOptionsMenu(menu);
	}

    /**
     * Changes the drawables color in ActionBar depending on the color received.
     *
     * @param color Can be Color.WHITE or Color.WHITE.
     */
    private void setActionBarDrawablesColorFilter(int color) {
        if (currentColorFilter == color || aB == null) {
            return;
        }

        currentColorFilter = color;

        upArrow.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        aB.setHomeAsUpIndicator(upArrow);

        drawableDots.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        toolbar.setOverflowIcon(drawableDots);

        setIconsColorFilter();
    }

    /**
     * Sets the toolbar icons color.
     */
    public void setIconsColorFilter() {
        if (removeLinkMenuItem == null || getLinkMenuItem == null || downloadMenuItem == null
                || shareMenuItem == null || leaveMenuItem == null || copyMenuItem == null
                || sendToChatMenuItem == null) {
            return;
        }


        drawableRemoveLink.setColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN);
        removeLinkMenuItem.setIcon(drawableRemoveLink);

        drawableLink.setColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN);
        getLinkMenuItem.setIcon(drawableLink);

        drawableDownload.setColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN);
        downloadMenuItem.setIcon(drawableDownload);

        drawableShare.setColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN);
        shareMenuItem.setIcon(drawableShare);

        drawableLeave.setColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN);
        leaveMenuItem.setIcon(drawableLeave);

        drawableCopy.setColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN);
        copyMenuItem.setIcon(drawableCopy);

        drawableChat.setColorFilter(currentColorFilter, PorterDuff.Mode.SRC_IN);
        sendToChatMenuItem.setIcon(drawableChat);
    }

	@SuppressLint("NonConstantResourceId")
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
			    nodeSaver.saveNode(node, false, false, false, false);
				break;
			}
			case R.id.cab_menu_file_info_share_folder: {
				Intent intent = new Intent();
				intent.setClass(this, AddContactActivity.class);
				intent.putExtra("contactType", CONTACT_TYPE_BOTH);
                intent.putExtra("MULTISELECT", 0);
				intent.putExtra(AddContactActivity.EXTRA_NODE_HANDLE, node.getHandle());
				startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
				break;
			}
			case R.id.cab_menu_file_info_get_link:
			case R.id.cab_menu_file_info_edit_link:{
                if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
                    return false;
                }

                showGetLinkActivity(this, node.getHandle());
				break;
			}
			case R.id.cab_menu_file_info_remove_link: {
			    if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
			        return false;
                }
				shareIt = false;
				AlertDialog removeLinkDialog;
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

				LayoutInflater inflater = getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
				TextView url = dialoglayout.findViewById(R.id.dialog_link_link_url);
				TextView key = dialoglayout.findViewById(R.id.dialog_link_link_key);
				TextView symbol = dialoglayout.findViewById(R.id.dialog_link_symbol);
				TextView removeText = dialoglayout.findViewById(R.id.dialog_link_text_remove);

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

				builder.setPositiveButton(getString(R.string.context_remove), (dialog, which) -> {
                    typeExport=TYPE_EXPORT_REMOVE;
                    megaApi.disableExport(node, fileInfoActivity);
                });

				builder.setNegativeButton(getString(R.string.general_cancel), (dialog, which) -> {

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
			    showRenameNodeDialog(this, node, this, this);
				break;
			}
			case R.id.cab_menu_file_info_leave:
				showConfirmationLeaveIncomingShare(this, this, node);
				break;

			case R.id.cab_menu_file_info_rubbish:
			case R.id.cab_menu_file_info_delete: {
				moveToTrash();
				break;
			}
            case R.id.cab_menu_file_info_send_to_chat: {
                logDebug("Send chat option");

                if (node != null) {
                    nodeAttacher.attachNode(node);
                }
                break;
            }
		}
		return super.onOptionsItemSelected(item);
	}

	private void refreshProperties(){
        logDebug("refreshProperties");

		if(node==null){
			finish();
		}

		boolean result=true;

		if(!node.isTakenDown() && node.isExported()){
			publicLink=true;
            dividerLinkLayout.setVisibility(View.VISIBLE);
			publicLinkLayout.setVisibility(View.VISIBLE);
			publicLinkCopyLayout.setVisibility(View.VISIBLE);
			publicLinkText.setText(node.getPublicLink());
			publicLinkDate.setText(getString(R.string.general_date_label, formatLongDateTime(node.getPublicLinkCreationTime())));
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
			sizeTextView.setText(getSizeString(node.getSize()));

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

			Bitmap thumb;
			Bitmap preview;
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
                String text = getQuantityString(R.plurals.number_of_versions, megaApi.getNumVersions(node), megaApi.getNumVersions(node));
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

			contentTextView.setText(getMegaNodeFolderInfo(node));

			long sizeFile=megaApi.getSize(node);
			sizeTextView.setText(getSizeString(sizeFile));
			setIconResource();

			if(from==FROM_INCOMING_SHARES){
				//Show who is the owner
				ownerRoundeImage.setImageBitmap(null);
				ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
				for(int j=0; j<sharesIncoming.size();j++){
					MegaShare mS = sharesIncoming.get(j);
					if(mS.getNodeHandle()==node.getHandle()){
						MegaUser user= megaApi.getContact(mS.getUser());
						contactMail=user.getEmail();
						if(user!=null){
                            String name = getMegaUserNameDB(user);
                            if (name == null) {
                                name = user.getEmail();
                            }
                            ownerLabel.setText(name);
                            ownerInfo.setText(user.getEmail());
                            setOwnerState(user.getHandle());
                            createDefaultAvatar(ownerRoundeImage, user, name);
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
                    usersSharedWithTextButton.setText(getQuantityString(R.plurals.general_selection_num_contacts,
                                    sl.size(), sl.size()));

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
            offlineSwitch.setChecked(true);
            return;
        }

        availableOfflineBoolean = false;
        offlineSwitch.setChecked(false);
	}

	private void createDefaultAvatar(ImageView ownerRoundeImage, MegaUser user, String name){
		ownerRoundeImage.setImageBitmap(getDefaultAvatar(getColorAvatar(user), name, AVATAR_SIZE, true));
	}


    private void sharedContactClicked() {
        FrameLayout sharedContactLayout = (FrameLayout)findViewById(R.id.shared_contact_list_container);
        if (isShareContactExpanded) {
            if (sl != null) {
                usersSharedWithTextButton.setText(getQuantityString(R.plurals.general_selection_num_contacts,
                                sl.size(), sl.size()));
            }
            sharedContactLayout.setVisibility(View.GONE);
        } else {
            usersSharedWithTextButton.setText(R.string.general_close);
            sharedContactLayout.setVisibility(View.VISIBLE);
        }

        isShareContactExpanded = !isShareContactExpanded;
    }

	@SuppressLint("NonConstantResourceId")
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
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", node.getPublicLink());
                clipboard.setPrimaryClip(clip);
                showSnackbar(SNACKBAR_TYPE, getString(R.string.file_properties_get_link), -1);
				break;
			}
			case R.id.file_properties_shared_layout:
			case R.id.file_properties_shared_info_button:{
                sharedContactClicked();
				break;
			}
            case R.id.more_button:
                Intent i = new Intent(this, FileContactListActivity.class);
                i.putExtra(NAME, node.getHandle());
                startActivity(i);
                break;
			case R.id.file_properties_switch:{
                boolean isChecked = offlineSwitch.isChecked();

                if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
                    showOverDiskQuotaPaywallWarning();
                    offlineSwitch.setChecked(!isChecked);
                    return;
                }

				if(owner){
                    logDebug("Owner: me");
					if (!isChecked){
                        logDebug("isChecked");
                        isRemoveOffline = true;
                        handle = node.getHandle();
						availableOfflineBoolean = false;
						offlineSwitch.setChecked(false);
						mOffDelete = dbH.findByHandle(handle);
                        removeOffline(mOffDelete, dbH, this);
                        Util.showSnackbar(this,
                                getResources().getString(R.string.file_removed_offline));
                    }
					else{
                        logDebug("NOT Checked");
                        isRemoveOffline = false;
                        handle = -1;
						availableOfflineBoolean = true;
						offlineSwitch.setChecked(true);

						File destination = getOfflineParentFile(this, from, node, megaApi);
                        logDebug("Path destination: " + destination);

						if (isFileAvailable(destination) && destination.isDirectory()){
							File offlineFile = new File(destination, node.getName());
                            if (isFileAvailable(offlineFile)
                                    && node.getSize() == offlineFile.length()
                                    && offlineFile.getName().equals(node.getName())) {
                                //This means that is already available offline
                                return;
                            }
						}

                        logDebug("Handle to save for offline : " + node.getHandle());
                        saveOffline(destination, node, fileInfoActivity);

                    }
                    supportInvalidateOptionsMenu();
                }
				else{
                    logDebug("Not owner");
					if (!isChecked){
						availableOfflineBoolean = false;
						offlineSwitch.setChecked(false);
						mOffDelete = dbH.findByHandle(node.getHandle());
                        removeOffline(mOffDelete, dbH, this);
						supportInvalidateOptionsMenu();
					}
					else{
						availableOfflineBoolean = true;
						offlineSwitch.setChecked(true);

						supportInvalidateOptionsMenu();

                        logDebug("Checking the node" + node.getHandle());

						//check the parent
						long result = -1;
						result = findIncomingParentHandle(node, megaApi);
                        logDebug("IncomingParentHandle: " + result);
						if(result!=-1){
							File destination = getOfflineParentFile(this, FROM_INCOMING_SHARES, node, megaApi);

							if (isFileAvailable(destination) && destination.isDirectory()){
								File offlineFile = new File(destination, node.getName());
								if (isFileAvailable(offlineFile) && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getName())){ //This means that is already available offline
									return;
								}
							}
							saveOffline(destination, node, fileInfoActivity);
						}
						else{
                            logWarning("result=findIncomingParentHandle NOT result!");
						}
					}
				}
				break;
			}
		}
	}

	public void showCopy(){

		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());

		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_COPY_FOLDER);
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

		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_MOVE_FOLDER);
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

        moveToRubbish = parent.getHandle() != megaApi.getRubbishNode().getHandle();

		DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //TODO remove the outgoing shares
                    //Check if the node is not yet in the rubbish bin (if so, remove it)

                    if (moveToRubbish){
                        megaApi.moveNode(megaApi.getNodeByHandle(handle), rubbishNode, fileInfoActivity);
                        AlertDialog temp;
                        try{
                            temp = MegaProgressDialogUtil.createProgressDialog(fileInfoActivity, getString(R.string.context_move_to_trash));
                            temp.show();
                        }
                        catch(Exception e){
                            return;
                        }
                        statusDialog = temp;
                    }
                    else{
                        megaApi.remove(megaApi.getNodeByHandle(handle), fileInfoActivity);
                        AlertDialog temp;
                        try{
                            temp = MegaProgressDialogUtil.createProgressDialog(fileInfoActivity, getString(R.string.context_delete_from_mega));
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
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
		if (moveToRubbish){
			int stringMessageID;
            if (getPrimaryFolderHandle() == handle && CameraUploadUtil.isPrimaryEnabled()) {
                stringMessageID = R.string.confirmation_move_cu_folder_to_rubbish;
            } else if (getSecondaryFolderHandle() == handle && CameraUploadUtil.isSecondaryEnabled()) {
                stringMessageID = R.string.confirmation_move_mu_folder_to_rubbish;
            } else {
                stringMessageID = R.string.confirmation_move_to_rubbish;
            }
			String message= getResources().getString(stringMessageID);
			builder.setMessage(message).setPositiveButton(R.string.general_move, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
		else{
            String message= getResources().getString(R.string.confirmation_delete_from_mega);
			builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
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
                    String text = getQuantityString(R.plurals.number_of_versions_inside_folder, numVersions, numVersions);
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
        } else if (request.getType() == MegaRequest.TYPE_MOVE){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {
                logDebug(ex.getMessage());
            }

			if (moveToRubbish){
				moveToRubbish = false;
                logDebug("Move to rubbish request finished");
			} else {
			    logDebug("Move nodes request finished");
			}

            if (e.getErrorCode() == MegaError.API_OK) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved), -1);
                Intent intent = new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN);
                sendBroadcast(intent);
                finish();
            } else if (e.getErrorCode() == MegaError.API_EOVERQUOTA && api.isForeignNode(request.getParentHandle())) {
                showForeignStorageOverQuotaWarningDialog(this);
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
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.version_history_deleted_erroneously)
                                        + "\n" + getQuantityString(R.plurals.versions_deleted_succesfully, versionsRemoved, versionsRemoved)
                                        + "\n" + getQuantityString(R.plurals.versions_not_deleted, errorVersionRemove, errorVersionRemove),
                                MEGACHAT_INVALID_HANDLE);
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
                } else if (e.getErrorCode() == MegaError.API_EMASTERONLY) {
                    showSnackbar(SNACKBAR_TYPE, e.getErrorString(), -1);
                } else{
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_removed), -1);
                }
            }
        } else if (request.getType() == MegaApiJava.USER_ATTR_AVATAR) {
			try{
				statusDialog.dismiss();
			}catch (Exception ex){
			    logError(ex.getMessage());
            }

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        nodeSaver.handleRequestPermissionsResult(requestCode);
    }

    /**
     * Checks if there is a name collision before moving or copying the node.
     *
     * @param parentHandle Parent handle of the node in which the node will be moved or copied.
     * @param type         Type of name collision to check.
     */
    private void checkCollision(long parentHandle, NameCollisionType type) {
        checkNameCollisionUseCase.check(handle, parentHandle, type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(collision -> {
                            dismissAlertDialogIfExists(statusDialog);
                            nameCollisionActivityContract.launch(collision);
                        },
                        throwable -> {
                            if (throwable instanceof MegaNodeException.ParentDoesNotExistException) {
                                showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.general_error), MEGACHAT_INVALID_HANDLE);
                            } else if (throwable instanceof MegaNodeException.ChildDoesNotExistsException) {
                                if (type == NameCollisionType.MOVEMENT) {
                                    move(parentHandle);
                                } else {
                                    copy(parentHandle);
                                }
                            }
                        });
    }

    /**
     * Moves the node.
     *
     * @param parentHandle Parent handle in which the node will be moved.
     */
    private void move(long parentHandle) {
        moveNodeUseCase.move(handle, parentHandle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                            dismissAlertDialogIfExists(statusDialog);
                            showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_correctly_moved), MEGACHAT_INVALID_HANDLE);
                            sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN));
                            finish();
                        }, throwable -> {
                            dismissAlertDialogIfExists(statusDialog);
                            if (!manageThrowable(throwable)) {
                                showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_no_moved), MEGACHAT_INVALID_HANDLE);
                            }
                        }
                );
    }

    /**
     * Copies the node.
     *
     * @param parentHandle Parent handle in which the node will be copied.
     */
    private void copy(long parentHandle) {
        copyNodeUseCase.copy(handle, parentHandle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                            dismissAlertDialogIfExists(statusDialog);
                            showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_correctly_copied), MEGACHAT_INVALID_HANDLE);
                        }, throwable -> {
                            dismissAlertDialogIfExists(statusDialog);

                            if (!manageThrowable(throwable)) {
                                showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_no_copied), MEGACHAT_INVALID_HANDLE);
                            }
                        }
                );
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    super.onActivityResult(requestCode, resultCode, intent);
        logDebug("onActivityResult " + requestCode + "____" + resultCode);

        if (nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this)) {
            return;
        }

        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return;
        }

		if (intent == null) {
			return;
		}

        if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {
            if (!isOnline(this)) {
                showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
                return;
            }

            final long toHandle = intent.getLongExtra("MOVE_TO", 0);

            moveToRubbish = false;

            AlertDialog temp;
            try {
                temp = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.context_moving));
                temp.show();
            } catch (Exception e) {
                return;
            }
            statusDialog = temp;

            checkCollision(toHandle, NameCollisionType.MOVEMENT);
        } else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK) {
            if (!isOnline(this)) {
                showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
                return;
            }

            final long toHandle = intent.getLongExtra("COPY_TO", 0);

            AlertDialog temp;
            try {
                temp = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.context_copying));
                temp.show();
            } catch (Exception e) {
                return;
            }
            statusDialog = temp;

            checkCollision(toHandle, NameCollisionType.COPY);
        } else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK) {
			if(!isOnline(this)){
				showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);

            if (node.isFolder()){
                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(fileInfoActivity, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
                dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
                final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        statusDialog = createProgressDialog(fileInfoActivity, getString(R.string.context_sharing_folder));
                        permissionsDialog.dismiss();
                        nC.shareFolder(node, contactsData, item);
                    }
                });
                permissionsDialog = dialogBuilder.create();
                permissionsDialog.show();
            }
            else{
                logWarning("ERROR, the file is not folder");
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
        for (MegaNode nodeToCheck : nodes) {
            if (nodeToCheck != null) {
                if (nodeToCheck.getHandle() == node.getHandle()) {
                    thisNode = true;
                    n = nodeToCheck;
                    break;
                } else {
                    if (node.isFolder()) {
                        MegaNode parent = megaApi.getNodeByHandle(nodeToCheck.getParentHandle());
                        while (parent != null) {
                            if (parent.getHandle() == node.getHandle()) {
                                updateContentFoder = true;
                                break;
                            }
                            parent = megaApi.getNodeByHandle(parent.getParentHandle());
                        }
                    } else {
                        if (nodeVersions != null) {
                            for (int j = 0; j < nodeVersions.size(); j++) {
                                if (nodeToCheck.getHandle() == nodeVersions.get(j).getHandle()) {
                                    if (anyChild == false) {
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

		if(!node.isTakenDown() && node.isExported()){
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
			sizeTextView.setText(getSizeString(sizeFile));

			contentTextView.setText(getMegaNodeFolderInfo(node));
			setIconResource();
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
                    usersSharedWithTextButton.setText(getQuantityString(R.plurals.general_selection_num_contacts,
                                    sl.size(), sl.size()));
				}
			}
		}
		else{

			sizeTextView.setText(getSizeString(node.getSize()));
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
            String text = getQuantityString(R.plurals.number_of_versions, megaApi.getNumVersions(node), megaApi.getNumVersions(node));
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

    	if (upArrow != null) upArrow.setColorFilter(null);
    	if (drawableRemoveLink != null) drawableRemoveLink.setColorFilter(null);
        if (drawableLink != null) drawableLink.setColorFilter(null);
        if (drawableShare != null) drawableShare.setColorFilter(null);
        if (drawableDots != null) drawableDots.setColorFilter(null);
        if (drawableDownload != null) drawableDownload.setColorFilter(null);
        if (drawableLeave != null) drawableLeave.setColorFilter(null);
        if (drawableCopy != null) drawableCopy.setColorFilter(null);
        if (drawableChat != null) drawableChat.setColorFilter(null);
        unregisterReceiver(contactUpdateReceiver);
        unregisterReceiver(manageShareReceiver);

        nodeSaver.destroy();
    }

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    if(selectedShare != null && node != null){
            outState.putLong(KEY_SELECTED_SHARE_HANDLE, selectedShare.getNodeHandle());
        }

        nodeAttacher.saveState(outState);
        nodeSaver.saveState(outState);
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
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
        retryConnectionsAndSignalPresence();

        if(isRemoveOffline){
            Intent intent = new Intent();
            intent.putExtra(NODE_HANDLE, handle);
            setResult(RESULT_OK, intent);
        }

        super.onBackPressed();
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

    public void itemClick(int position) {
        logDebug("Position: " + position);

        if (adapter.isMultipleSelect()) {
            adapter.toggleSelection(position);
            updateActionModeTitle();
        } else {
            String megaUser = listContacts.get(position).getUser();
            MegaUser contact = megaApi.getContact(megaUser);
            if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                ContactUtil.openContactInfoActivity(this, megaUser);
            }
        }
    }

    public void showOptionsPanel(MegaShare sShare){
        logDebug("showNodeOptionsPanel");

        if (node == null ||sShare == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedShare = sShare;
        bottomSheetDialogFragment = new FileContactsListBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
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
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
        final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
        dialogBuilder.setSingleChoiceItems(items, selectedShare.getAccess(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                statusDialog = createProgressDialog(fileInfoActivity, getString(R.string.context_permissions_changing_folder));
                permissionsDialog.dismiss();
                cC.changePermission(selectedShare.getUser(), item, node, new ShareListener(fileInfoActivity, ShareListener.CHANGE_PERMISSIONS_LISTENER, 1));
            }
        });
        permissionsDialog = dialogBuilder.create();
        permissionsDialog.show();
    }

    public void removeFileContactShare(){
        showConfirmationRemoveContactFromShare(selectedShare.getUser());
    }

    public void showConfirmationRemoveContactFromShare(final String email) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        String message = getResources().getString(R.string.remove_contact_shared_folder, email);
        builder.setMessage(message)
                .setPositiveButton(R.string.general_remove, (dialog, which) -> removeShare(email))
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {})
                .show();
    }

    public void removeShare(String email) {
        statusDialog = createProgressDialog(fileInfoActivity, getString(R.string.context_removing_contact_folder));
        nC.removeShare(new ShareListener(fileInfoActivity, ShareListener.REMOVE_SHARE_LISTENER, 1), node, email);
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

    public void showConfirmationRemoveMultipleContactFromShare (final ArrayList<MegaShare> contacts){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        String message= getResources().getString(R.string.remove_multiple_contacts_shared_folder,contacts.size());
        builder.setMessage(message)
                .setPositiveButton(R.string.general_remove, (dialog, which) -> removeMultipleShares(contacts))
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {})
                .show();
    }

    public void removeMultipleShares(ArrayList<MegaShare> shares){
        logDebug("removeMultipleShares");
        statusDialog = createProgressDialog(fileInfoActivity, getString(R.string.context_removing_contact_folder));
        nC.removeShares(shares, node);
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
            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    private void updateAdapter(long handleReceived) {
        if (listContacts != null && !listContacts.isEmpty()) {
            for (int i = 0; i < listContacts.size(); i++) {
                String email = listContacts.get(i).getUser();
                MegaUser contact = megaApi.getContact(email);
                long handleUser = contact.getHandle();
                if (handleUser == handleReceived) {
                    adapter.notifyItemChanged(i);
                    break;
                }
            }
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
        
        actionMode.setTitle(getQuantityString(R.plurals.general_selection_num_contacts,
                        contacts.size(), contacts.size()));
        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            logError("Invalidate error", e);
            e.printStackTrace();
        }
    }

    private void setIconResource() {
        int resource;

        if (node.isFolder()) {
            resource = getFolderIcon(node, adapterType == OUTGOING_SHARES_ADAPTER ? DrawerItem.SHARED_ITEMS : DrawerItem.CLOUD_DRIVE);
        } else {
            resource = MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId();
        }

        iconToolbarView.setImageResource(resource);
    }
}
