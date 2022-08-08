package mega.privacy.android.app.main;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_MANAGE_SHARE;
import static mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE;
import static mega.privacy.android.app.listeners.ShareListener.CHANGE_PERMISSIONS_LISTENER;
import static mega.privacy.android.app.listeners.ShareListener.REMOVE_SHARE_LISTENER;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.Constants.CONTACT_TYPE_BOTH;
import static mega.privacy.android.app.utils.Constants.NAME;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_CONTACT;
import static mega.privacy.android.app.utils.ContactUtil.openContactInfoActivity;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE;
import static mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle;
import static mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.Util.changeViewElevation;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.listeners.ShareListener;
import mega.privacy.android.app.main.adapters.MegaSharedFolderAdapter;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment;
import mega.privacy.android.app.namecollision.data.NameCollision;
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase;
import mega.privacy.android.app.usecase.UploadUseCase;
import mega.privacy.android.app.utils.AlertDialogUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;
import timber.log.Timber;

@AndroidEntryPoint
public class FileContactListActivity extends PasscodeActivity implements OnClickListener, MegaGlobalListenerInterface {

    @Inject
    CheckNameCollisionUseCase checkNameCollisionUseCase;
    @Inject
    UploadUseCase uploadUseCase;

    private ContactController cC;
    private NodeController nC;
    ActionBar aB;
    Toolbar tB;
    FileContactListActivity fileContactListActivity = this;
    MegaShare selectedShare;

    TextView nameView;
    ImageView imageView;
    TextView createdView;

    CoordinatorLayout coordinatorLayout;
    RelativeLayout container;
    RelativeLayout contactLayout;
    RecyclerView listView;
    LinearLayoutManager mLayoutManager;
    ImageView emptyImage;
    TextView emptyText;
    FloatingActionButton fab;

    ArrayList<MegaShare> listContacts;
    ArrayList<MegaShare> tempListContacts;

//	ArrayList<MegaUser> listContactsArray = new ArrayList<MegaUser>();

    long nodeHandle;
    MegaNode node;
    ArrayList<MegaNode> contactNodes;

    MegaSharedFolderAdapter adapter;

    long parentHandle = -1;

    Stack<Long> parentHandleStack = new Stack<Long>();

    private ActionMode actionMode;

    AlertDialog statusDialog;
    AlertDialog permissionsDialog;

    private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

    private List<ShareInfo> filePreparedInfos;

    MegaPreferences prefs = null;

    MenuItem addSharingContact;
    MenuItem selectMenuItem;
    MenuItem unSelectMenuItem;

    Handler handler;
    DisplayMetrics outMetrics;

    private MaterialAlertDialogBuilder dialogBuilder;

    private FileContactsListBottomSheetDialogFragment bottomSheetDialogFragment;

    private BroadcastReceiver manageShareReceiver = new BroadcastReceiver() {
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

            if (permissionsDialog != null) {
                permissionsDialog.dismiss();
            }

            if (statusDialog != null) {
                statusDialog.dismiss();
            }
        }
    };

    private BroadcastReceiver contactUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;

            if (intent.getAction().equals(ACTION_UPDATE_NICKNAME)
                    || intent.getAction().equals(ACTION_UPDATE_CREDENTIALS)
                    || intent.getAction().equals(ACTION_UPDATE_FIRST_NAME)
                    || intent.getAction().equals(ACTION_UPDATE_LAST_NAME)) {
                updateAdapter(intent.getLongExtra(EXTRA_USER_HANDLE, INVALID_HANDLE));
            }
        }
    };

    public void activateActionMode() {
        Timber.d("activateActionMode");
        if (!adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
            actionMode = startSupportActionMode(new ActionBarCallBack());
        }
    }

    private class ActionBarCallBack implements ActionMode.Callback {

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Timber.d("onActionItemClicked");
            final ArrayList<MegaShare> shares = adapter.getSelectedShares();

            switch (item.getItemId()) {
                case R.id.action_file_contact_list_permissions: {
                    //Change permissions
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));

                    final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                    dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            clearSelections();
                            if (permissionsDialog != null) {
                                permissionsDialog.dismiss();
                            }
                            statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_permissions_changing_folder));
                            cC.changePermissions(cC.getEmailShares(shares), item, node);
                        }
                    });

                    permissionsDialog = dialogBuilder.create();
                    permissionsDialog.show();
                    break;
                }
                case R.id.action_file_contact_list_delete: {
                    if (shares != null && !shares.isEmpty()) {
                        if (shares.size() > 1) {
                            Timber.d("Remove multiple contacts");
                            showConfirmationRemoveMultipleContactFromShare(shares);
                        } else {
                            Timber.d("Remove one contact");
                            showConfirmationRemoveContactFromShare(shares.get(0).getUser());
                        }
                    }
                    break;
                }
                case R.id.cab_menu_select_all: {
                    selectAll();
                    break;
                }
                case R.id.cab_menu_unselect_all: {
                    clearSelections();
                    break;
                }
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Timber.d("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_contact_shared_browser_action, menu);
            fab.setVisibility(View.GONE);
            checkScroll();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            Timber.d("onDestroyActionMode");
            adapter.clearSelections();
            adapter.setMultipleSelect(false);
            fab.setVisibility(View.VISIBLE);
            checkScroll();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Timber.d("onPrepareActionMode");
            ArrayList<MegaShare> selected = adapter.getSelectedShares();
            boolean deleteShare = false;
            boolean permissions = false;

            if (selected.size() != 0) {
                permissions = true;
                deleteShare = true;

                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                if (selected.size() == adapter.getItemCount()) {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                } else {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                }
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
            }

            menu.findItem(R.id.action_file_contact_list_permissions).setVisible(permissions);
            if (permissions) {
                menu.findItem(R.id.action_file_contact_list_permissions).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            } else {
                menu.findItem(R.id.action_file_contact_list_permissions).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            }

            menu.findItem(R.id.action_file_contact_list_delete).setVisible(deleteShare);
            if (deleteShare) {
                menu.findItem(R.id.action_file_contact_list_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            } else {
                menu.findItem(R.id.action_file_contact_list_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            }

            return false;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return;
        }

        dialogBuilder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

        megaApi.addGlobalListener(this);

        handler = new Handler();

        listContacts = new ArrayList<>();

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            nodeHandle = extras.getLong(NAME);
            node = megaApi.getNodeByHandle(nodeHandle);

            setContentView(R.layout.activity_file_contact_list);

            //Set toolbar
            tB = findViewById(R.id.toolbar_file_contact_list);
            setSupportActionBar(tB);
            aB = getSupportActionBar();
            if (aB != null) {
                aB.setDisplayHomeAsUpEnabled(true);
                aB.setDisplayShowHomeEnabled(true);
                aB.setTitle(node.getName());
                aB.setSubtitle(R.string.file_properties_shared_folder_select_contact);
            }

            coordinatorLayout = findViewById(R.id.coordinator_layout_file_contact_list);
            container = findViewById(R.id.file_contact_list);
            imageView = findViewById(R.id.file_properties_icon);
            nameView = findViewById(R.id.node_name);
            createdView = findViewById(R.id.node_last_update);
            contactLayout = findViewById(R.id.file_contact_list_layout);
            contactLayout.setVisibility(View.GONE);
            findViewById(R.id.separator_file_contact_list).setVisibility(View.GONE);
//			contactLayout.setOnClickListener(this);

            fab = (FloatingActionButton) findViewById(R.id.floating_button_file_contact_list);
            fab.setOnClickListener(this);

            nameView.setText(node.getName());

            imageView.setImageResource(R.drawable.ic_folder_outgoing_list);

            tempListContacts = megaApi.getOutShares(node);
            if (tempListContacts != null && !tempListContacts.isEmpty()) {
                listContacts.addAll(megaApi.getOutShares(node));
            }

            listView = findViewById(R.id.file_contact_list_view_browser);
            listView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
            listView.setClipToPadding(false);
            listView.addItemDecoration(new SimpleDividerItemDecoration(this));
            mLayoutManager = new LinearLayoutManager(this);
            listView.setLayoutManager(mLayoutManager);
            listView.setItemAnimator(noChangeRecyclerViewItemAnimator());
            listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    checkScroll();
                }
            });

            emptyImage = findViewById(R.id.file_contact_list_empty_image);
            emptyText = findViewById(R.id.file_contact_list_empty_text);
            emptyImage.setImageResource(R.drawable.ic_empty_contacts);
            emptyText.setText(R.string.contacts_list_empty_text);

            if (listContacts.size() != 0) {
                emptyImage.setVisibility(View.GONE);
                emptyText.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            } else {
                emptyImage.setVisibility(View.VISIBLE);
                emptyText.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);

            }

            if (node.getCreationTime() != 0) {
                try {
                    createdView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));
                } catch (Exception ex) {
                    createdView.setText("");
                }

            } else {
                createdView.setText("");
            }

            if (adapter == null) {

                adapter = new MegaSharedFolderAdapter(this, node, listContacts, listView);
                listView.setAdapter(adapter);
                adapter.setShareList(listContacts);
            } else {
                adapter.setShareList(listContacts);
                //adapter.setParentHandle(-1);
            }

            adapter.setPositionClicked(-1);
            adapter.setMultipleSelect(false);

            listView.setAdapter(adapter);
        }

        cC = new ContactController(this);
        nC = new NodeController(this);

        registerReceiver(manageShareReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE));

        IntentFilter contactUpdateFilter = new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE);
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS);
        registerReceiver(contactUpdateReceiver, contactUpdateFilter);
    }

    public void checkScroll() {
        if (listView != null) {
            changeViewElevation(aB, (listView.canScrollVertically(-1) && listView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect()), outMetrics);
        }
    }


    public void showOptionsPanel(MegaShare sShare) {
        Timber.d("showNodeOptionsPanel");

        if (node == null || sShare == null || isBottomSheetDialogShown(bottomSheetDialogFragment))
            return;

        selectedShare = sShare;
        bottomSheetDialogFragment = new FileContactsListBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (megaApi != null) {
            megaApi.removeGlobalListener(this);
        }
        handler.removeCallbacksAndMessages(null);

        unregisterReceiver(manageShareReceiver);
        unregisterReceiver(contactUpdateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_folder_contact_list, menu);

        menu.findItem(R.id.action_delete_version_history).setVisible(false);

        selectMenuItem = menu.findItem(R.id.action_select);
        unSelectMenuItem = menu.findItem(R.id.action_unselect);

        selectMenuItem.setVisible(true);
        unSelectMenuItem.setVisible(false);

        addSharingContact = menu.findItem(R.id.action_folder_contacts_list_share_folder);
        addSharingContact.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.action_select: {
                selectAll();
                return true;
            }
            case R.id.action_folder_contacts_list_share_folder: {
                shareOption();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    void shareOption() {
        Intent intent = new Intent();
        intent.setClass(this, AddContactActivity.class);
        intent.putExtra("contactType", CONTACT_TYPE_BOTH);
        intent.putExtra("MULTISELECT", 0);
        intent.putExtra(AddContactActivity.EXTRA_NODE_HANDLE, node.getHandle());
        startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
    }

    // Clear all selected items
    private void clearSelections() {
        if (adapter.isMultipleSelect()) {
            adapter.clearSelections();
        }
    }

    public void selectAll() {
        Timber.d("selectAll");
        if (adapter != null) {
            if (adapter.isMultipleSelect()) {
                adapter.selectAll();
            } else {
                adapter.setMultipleSelect(true);
                adapter.selectAll();

                actionMode = startSupportActionMode(new ActionBarCallBack());
            }
            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    public boolean showSelectMenuItem() {
        if (adapter != null) {
            return adapter.isMultipleSelect();
        }

        return false;
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);

        if (adapter.isMultipleSelect()) {
            adapter.toggleSelection(position);
            updateActionModeTitle();
        } else {
            MegaUser contact = megaApi.getContact(listContacts.get(position).getUser());
            if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                openContactInfoActivity(this, listContacts.get(position).getUser());
            }
        }
    }

    @Override
    public void onBackPressed() {
        Timber.d("onBackPressed");
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
        retryConnectionsAndSignalPresence();

        if (adapter.getPositionClicked() != -1) {
            adapter.setPositionClicked(-1);
            adapter.notifyDataSetChanged();
        } else {
            if (parentHandleStack.isEmpty()) {
                super.onBackPressed();
            } else {
                parentHandle = parentHandleStack.pop();
                listView.setVisibility(View.VISIBLE);
                emptyImage.setVisibility(View.GONE);
                emptyText.setVisibility(View.GONE);
                if (parentHandle == -1) {
                    aB.setTitle(StringResourcesUtils.getString(R.string.file_properties_shared_folder_select_contact));

                    aB.setLogo(R.drawable.ic_action_navigation_accept_white);
                    supportInvalidateOptionsMenu();
                    adapter.setShareList(listContacts);
                    listView.scrollToPosition(0);
                } else {
                    contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
                    aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
                    aB.setLogo(R.drawable.ic_action_navigation_previous_item);
                    supportInvalidateOptionsMenu();
                    adapter.setShareList(listContacts);
                    listView.scrollToPosition(0);
                }
            }
        }
    }

    private void updateActionModeTitle() {
        Timber.d("updateActionModeTitle");
        if (actionMode == null) {
            return;
        }
        ArrayList<MegaShare> contacts = adapter.getSelectedShares();
        if (contacts != null) {
            Timber.d("Contacts selected: %s", contacts.size());
        }

        actionMode.setTitle(getQuantityString(R.plurals.general_selection_num_contacts,
                contacts.size(), contacts.size()));
        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            Timber.e(e, "Invalidate error");
        }
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        adapter.setMultipleSelect(false);
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.floating_button_file_contact_list) {
            shareOption();
        }
    }

    public void removeFileContactShare() {
        notifyDataSetChanged();

        showConfirmationRemoveContactFromShare(selectedShare.getUser());
    }

    public void changePermissions() {
        Timber.d("changePermissions");
        notifyDataSetChanged();
        int nodeType = checkBackupNodeTypeByHandle(megaApi, node);
        if (nodeType != BACKUP_NONE) {
            showWarningDialog();
            return;
        }
        dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
        final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
        dialogBuilder.setSingleChoiceItems(items, selectedShare.getAccess(), (dialog, item) -> {
            statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_permissions_changing_folder));
            permissionsDialog.dismiss();
            cC.changePermission(selectedShare.getUser(), item, node, new ShareListener(getApplicationContext(), CHANGE_PERMISSIONS_LISTENER, 1));
        });
        permissionsDialog = dialogBuilder.create();
        permissionsDialog.show();
    }

    /**
     * Show the warning dialog when change the permissions of this folder
     *
     * @return The dialog
     */
    private AlertDialog showWarningDialog() {
        DialogInterface.OnClickListener dialogClickListener =
                (dialog, which) -> {
                    dialog.dismiss();
                };
        LayoutInflater layout = this.getLayoutInflater();
        View view = layout.inflate(R.layout.dialog_backup_operate_tip, null);
        TextView tvTitle = view.findViewById(R.id.title);
        TextView tvContent = view.findViewById(R.id.backup_tip_content);
        tvTitle.setText(R.string.backup_share_permission_title);
        tvContent.setText(R.string.backup_share_permission_text);
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this)
                .setView(view);
        builder.setPositiveButton(
                getString(R.string.button_permission_info),
                dialogClickListener
        );
        AlertDialog dialog = builder.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    public void setPositionClicked(int positionClicked) {
        if (adapter != null) {
            adapter.setPositionClicked(positionClicked);
        }
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /*
     * Handle processed upload intent
     */
    public void onIntentProcessed() {
        List<ShareInfo> infos = filePreparedInfos;

        MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
        if (parentNode == null) {
            AlertDialogUtil.dismissAlertDialogIfExists(statusDialog);
            showSnackbar(StringResourcesUtils.getString(R.string.error_temporary_unavaible));
            return;
        }

        if (infos == null) {
            AlertDialogUtil.dismissAlertDialogIfExists(statusDialog);
            showSnackbar(StringResourcesUtils.getString(R.string.upload_can_not_open));
            return;
        }

        if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
            AlertDialogUtil.dismissAlertDialogIfExists(statusDialog);
            showOverDiskQuotaPaywallWarning();
            return;
        }

        checkNameCollisionUseCase.checkShareInfoList(infos, parentNode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result, throwable) -> {
                    AlertDialogUtil.dismissAlertDialogIfExists(statusDialog);

                    if (throwable != null) {
                        showSnackbar(StringResourcesUtils.getString(R.string.error_temporary_unavaible));
                    } else {
                        ArrayList<NameCollision> collisions = result.getFirst();
                        List<ShareInfo> withoutCollisions = result.getSecond();

                        if (!collisions.isEmpty()) {
                            nameCollisionActivityContract.launch(collisions);
                        }

                        if (!withoutCollisions.isEmpty()) {
                            String text = StringResourcesUtils.getQuantityString(R.plurals.upload_began, withoutCollisions.size(), withoutCollisions.size());

                            uploadUseCase.uploadInfos(this, withoutCollisions, null, parentNode.getHandle())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(() -> showSnackbar(text));
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if (intent == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK) {
            if (!isOnline(this)) {
                showSnackbar(getString(R.string.error_server_connection_problem));
                return;
            }

            final ArrayList<String> emails = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);
            final long nodeHandle = intent.getLongExtra(AddContactActivity.EXTRA_NODE_HANDLE, -1);

            if (nodeHandle != -1) {
                node = megaApi.getNodeByHandle(nodeHandle);
            }
            if (node != null) {
                if (node.isFolder()) {
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
                    final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                    dialogBuilder.setSingleChoiceItems(items, -1, (dialog, item) -> {
                        statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_sharing_folder));
                        permissionsDialog.dismiss();
                        nC.shareFolder(node, emails, item);
                    });
                    permissionsDialog = dialogBuilder.create();
                    permissionsDialog.show();
                }
            }
        }
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
        Timber.d("onUserupdate");

    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        Timber.d("onUserAlertsUpdate");
    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {

    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
        Timber.d("onNodesUpdate");

        try {
            statusDialog.dismiss();
        } catch (Exception ex) {
            Timber.e(ex, "Error dismiss status dialog");
        }

        if (node.isFolder()) {
            listContacts.clear();

            tempListContacts = megaApi.getOutShares(node);
            if (tempListContacts != null && !tempListContacts.isEmpty()) {
                listContacts.addAll(megaApi.getOutShares(node));
            }

            if (listContacts != null) {
                if (listContacts.size() > 0) {
                    listView.setVisibility(View.VISIBLE);
                    emptyImage.setVisibility(View.GONE);
                    emptyText.setVisibility(View.GONE);

                    if (adapter != null) {
                        adapter.setNode(node);
                        adapter.setContext(this);
                        adapter.setShareList(listContacts);
                        adapter.setListFragment(listView);
                    } else {
                        adapter = new MegaSharedFolderAdapter(this, node, listContacts, listView);
                    }
                } else {
                    listView.setVisibility(View.GONE);
                    emptyImage.setVisibility(View.VISIBLE);
                    emptyText.setVisibility(View.VISIBLE);
                    //((RelativeLayout.LayoutParams)infoTable.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.file_properties_image);
                }
            }
        }

        listView.invalidate();
    }

    public void showConfirmationRemoveContactFromShare(final String email) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        String message = getResources().getString(R.string.remove_contact_shared_folder, email);
        builder.setMessage(message)
                .setPositiveButton(R.string.general_remove, (dialog, which) -> {
                    statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_removing_contact_folder));
                    nC.removeShare(new ShareListener(this, REMOVE_SHARE_LISTENER, 1), node, email);
                })
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                })
                .show();
    }

    public void showConfirmationRemoveMultipleContactFromShare(final ArrayList<MegaShare> contacts) {
        Timber.d("showConfirmationRemoveMultipleContactFromShare");

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE: {
                    removeMultipleShares(contacts);
                    break;
                }
                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        String message = getResources().getQuantityString(R.plurals.remove_multiple_contacts_shared_folder, contacts.size(), contacts.size());
        builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

    public void removeMultipleShares(ArrayList<MegaShare> shares) {
        Timber.d("Number of shared to remove: %s", shares.size());

        statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_removing_contact_folder));
        nC.removeShares(shares, node);
    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {
        // TODO Auto-generated method stub

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

    public void showSnackbar(String s) {
        showSnackbar(contactLayout, s);
    }

    public MegaUser getSelectedContact() {
        String email = selectedShare.getUser();
        return megaApi.getContact(email);
    }

    public MegaShare getSelectedShare() {
        return selectedShare;
    }

    public void setSelectedShare(MegaShare selectedShare) {
        this.selectedShare = selectedShare;
    }

    private void updateAdapter(long handleReceived) {
        if (listContacts == null || listContacts.isEmpty()) return;

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

