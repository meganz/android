package mega.privacy.android.app.main;

import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.utils.Constants.ACTION_REFRESH_PARENTHANDLE_BROWSER;
import static mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER;
import static mega.privacy.android.app.utils.Constants.EXTRA_NODE_HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER;
import static mega.privacy.android.app.utils.Constants.VIEWER_FROM_FILE_BROWSER;
import static mega.privacy.android.app.utils.Constants.VIEWER_FROM_FILE_VERSIONS;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode;
import static mega.privacy.android.app.utils.MegaNodeUtil.setupStreamingServer;
import static mega.privacy.android.app.utils.Util.changeViewElevation;
import static mega.privacy.android.app.utils.Util.getMediaIntent;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaShare.ACCESS_FULL;
import static nz.mega.sdk.MegaShare.ACCESS_OWNER;
import static nz.mega.sdk.MegaShare.ACCESS_READWRITE;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.main.adapters.VersionsFileAdapter;
import mega.privacy.android.app.modalbottomsheet.VersionBottomSheetDialogFragment;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.permission.PermissionUtils;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;
import timber.log.Timber;

public class VersionsFileActivity extends PasscodeActivity implements MegaRequestListenerInterface,
        OnClickListener, MegaGlobalListenerInterface, SnackbarShower {

    private static final String CHECKING_REVERT_VERSION = "CHECKING_REVERT_VERSION";
    private static final String SELECTED_NODE_HANDLE = "SELECTED_NODE_HANDLE";
    private static final String SELECTED_POSITION = "SELECTED_POSITION";

    public static final String KEY_DELETE_VERSION_HISTORY = "deleteVersionHistory";
    public static final String KEY_DELETE_NODE_HANDLE = "nodeHandle";

    public static final String DELETING_VERSION_DIALOG_SHOWN = "DELETING_VERSION_DIALOG_SHOWN";
    public static final String DELETING_HISTORY_VERSION_DIALOG_SHOWN = "DELETING_HISTORY_VERSION_DIALOG_SHOWN";

    ActionBar aB;
    MaterialToolbar tB;

    MegaNode selectedNode;
    private long selectedNodeHandle;

    int selectedPosition;

    RelativeLayout container;
    RecyclerView listView;
    LinearLayoutManager mLayoutManager;

    ArrayList<MegaNode> nodeVersions;

    MegaNode node;

    VersionsFileAdapter adapter;
    public String versionsSize = null;

    private ActionMode actionMode;

    MenuItem selectMenuItem;
    MenuItem unSelectMenuItem;
    MenuItem deleteVersionsMenuItem;

    Handler handler;
    DisplayMetrics outMetrics;

    int totalRemoveSelected = 0;
    int errorRemove = 0;
    int completedRemove = 0;

    private VersionBottomSheetDialogFragment bottomSheetDialogFragment;

    private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
            AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));

    private int accessLevel;

    private AlertDialog deleteVersionConfirmationDialog;
    private AlertDialog checkPermissionRevertVersionDialog;
    private AlertDialog deleteVersionHistoryDialog;

    @Override
    public void showSnackbar(int type, @Nullable String content, long chatId) {
        showSnackbar(type, container, content, chatId);
    }

    private class GetVersionsSizeTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            long sizeNumber = 0;
            if (nodeVersions != null) {
                for (int i = 0; i < nodeVersions.size(); i++) {
                    MegaNode node = nodeVersions.get(i);
                    sizeNumber = sizeNumber + node.getSize();
                }
            }
            String size = getSizeString(sizeNumber);
            Timber.d("doInBackground-AsyncTask GetVersionsSizeTask: %s", size);
            return size;
        }

        @Override
        protected void onPostExecute(String size) {
            Timber.d("GetVersionsSizeTask::onPostExecute");
            updateSize(size);
        }
    }

    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Timber.d("onActionItemClicked");
            final List<MegaNode> nodes = adapter.getSelectedNodes();

            switch (item.getItemId()) {
                case R.id.cab_menu_select_all: {
                    selectAll();
                    break;
                }
                case R.id.cab_menu_unselect_all: {
                    clearSelections();
                    break;
                }
                case R.id.action_download_versions: {
                    if (nodes.size() == 1) {
                        downloadNodes(nodes);
                        clearSelections();
                        actionMode.invalidate();
                    }
                    break;
                }
                case R.id.action_delete_versions: {
                    showConfirmationRemoveVersions(nodes);
                    break;
                }
                case R.id.action_revert_version: {
                    if (nodes.size() == 1) {
                        selectedNode = nodes.get(0);
                        selectedNodeHandle = selectedNode.getHandle();
                        checkRevertVersion();
                        clearSelections();
                        actionMode.invalidate();
                    }
                    break;
                }
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Timber.d("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.versions_files_action, menu);
            menu.findItem(R.id.cab_menu_select_all).setVisible(true);
            menu.findItem(R.id.action_download_versions).setVisible(false);
            menu.findItem(R.id.action_delete_versions).setVisible(false);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            Timber.d("onDestroyActionMode");
            adapter.clearSelections();
            adapter.setMultipleSelect(false);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Timber.d("onPrepareActionMode");
            List<MegaNode> selected = adapter.getSelectedNodes();

            if (selected.size() != 0) {
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

                if (selected.size() == 1) {
                    if (getSelectedPosition() == 0) {
                        menu.findItem(R.id.action_revert_version).setVisible(false);
                    } else {
                        menu.findItem(R.id.action_revert_version).setVisible(true);
                    }
                    menu.findItem(R.id.action_download_versions).setVisible(true);
                } else {
                    menu.findItem(R.id.action_revert_version).setVisible(false);
                    menu.findItem(R.id.action_download_versions).setVisible(false);
                }

                menu.findItem(R.id.action_delete_versions).setVisible(true);
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
                menu.findItem(R.id.action_download_versions).setVisible(false);
                menu.findItem(R.id.action_delete_versions).setVisible(false);
                menu.findItem(R.id.action_revert_version).setVisible(false);
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

        megaApi.addGlobalListener(this);

        handler = new Handler();

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        setContentView(R.layout.activity_versions_file);

        //Set toolbar
        tB = findViewById(R.id.toolbar_versions_file);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(getString(R.string.title_section_versions));

        container = (RelativeLayout) findViewById(R.id.versions_main_layout);

        listView = (RecyclerView) findViewById(R.id.recycler_view_versions_file);
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

        long nodeHandle = INVALID_HANDLE;

        if (savedInstanceState != null) {
            nodeHandle = savedInstanceState.getLong(EXTRA_NODE_HANDLE, INVALID_HANDLE);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (nodeHandle == INVALID_HANDLE) {
                nodeHandle = extras.getLong("handle");
            }

            node = megaApi.getNodeByHandle(nodeHandle);

            if (node != null) {
                accessLevel = megaApi.getAccess(node);
                nodeVersions = megaApi.getVersions(node);

                GetVersionsSizeTask getVersionsSizeTask = new GetVersionsSizeTask();
                getVersionsSizeTask.execute();

                listView.setVisibility(View.VISIBLE);

                if (adapter == null) {

                    adapter = new VersionsFileAdapter(this, nodeVersions, listView);
                    listView.setAdapter(adapter);
                } else {
                    adapter.setNodes(nodeVersions);
                }

                adapter.setMultipleSelect(false);

                listView.setAdapter(adapter);
            } else {
                Timber.e("ERROR: node is NULL");
            }
        }
    }

    void checkScroll() {
        if (listView != null) {
            changeViewElevation(aB, (listView.canScrollVertically(-1) && listView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect()), outMetrics);
        }
    }

    public void showOptionsPanel(MegaNode sNode, int sPosition) {
        Timber.d("showOptionsPanel");
        if (node == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedNode = sNode;
        selectedNodeHandle = selectedNode.getHandle();
        selectedPosition = sPosition;
        bottomSheetDialogFragment = new VersionBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void downloadNodes(List<MegaNode> nodes) {
        PermissionUtils.checkNotificationsPermission(this);
        nodeSaver.saveNodes(nodes, false, false, false, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        nodeSaver.handleActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        nodeSaver.handleRequestPermissionsResult(requestCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (megaApi != null) {
            megaApi.removeGlobalListener(this);
            megaApi.removeRequestListener(this);
        }
        handler.removeCallbacksAndMessages(null);

        nodeSaver.destroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_folder_contact_list, menu);

        selectMenuItem = menu.findItem(R.id.action_select);
        unSelectMenuItem = menu.findItem(R.id.action_unselect);
        deleteVersionsMenuItem = menu.findItem(R.id.action_delete_version_history);

        menu.findItem(R.id.action_folder_contacts_list_share_folder).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        switch (accessLevel) {
            case ACCESS_FULL:
            case ACCESS_OWNER:
                selectMenuItem.setVisible(true);
                unSelectMenuItem.setVisible(false);
                deleteVersionsMenuItem.setVisible(true);
                break;

            default:
                selectMenuItem.setVisible(false);
                unSelectMenuItem.setVisible(false);
                deleteVersionsMenuItem.setVisible(false);

        }

        return super.onPrepareOptionsMenu(menu);
    }

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
            case R.id.action_delete_version_history: {
                showDeleteVersionHistoryDialog();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    void showDeleteVersionHistoryDialog() {
        Timber.d("showDeleteVersionHistoryDialog");
        deleteVersionHistoryDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_delete_version_history)
                .setMessage(R.string.text_delete_version_history)
                .setPositiveButton(R.string.context_delete, (dialog, which) -> deleteVersionHistory())
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                })
                .show();
    }

    void deleteVersionHistory() {
        Intent intent = new Intent();
        intent.putExtra(KEY_DELETE_VERSION_HISTORY, true);
        intent.putExtra(KEY_DELETE_NODE_HANDLE, node.getHandle());
        setResult(RESULT_OK, intent);
        finish();
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

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        if (request.getType() == MegaRequest.TYPE_SHARE) {
            Timber.d("onRequestStart - Share");
        }
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
Timber.d("onRequestFinish: %s",  request.getType());
Timber.d("onRequestFinish: %s",  request.getRequestString());
        if (adapter != null && adapter.isMultipleSelect()) {
            adapter.clearSelections();
            hideMultipleSelect();
        }

        if (request.getType() == MegaRequest.TYPE_REMOVE) {
            Timber.d("MegaRequest.TYPE_REMOVE");
            totalRemoveSelected--;
            if (e.getErrorCode() == MegaError.API_OK) {
                completedRemove++;
                checkScroll();
            } else {
                errorRemove++;
            }

            if (totalRemoveSelected == 0) {
                if (completedRemove > 0 && errorRemove == 0) {
                    showSnackbar(getResources().getQuantityString(R.plurals.versions_deleted_succesfully, completedRemove, completedRemove));
                } else if (completedRemove > 0 && errorRemove > 0) {
                    showSnackbar(getResources().getQuantityString(R.plurals.versions_deleted_succesfully, completedRemove, completedRemove) + "\n"
                            + getResources().getQuantityString(R.plurals.versions_not_deleted, errorRemove, errorRemove));
                } else {
                    showSnackbar(getResources().getQuantityString(R.plurals.versions_not_deleted, errorRemove, errorRemove));
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_RESTORE) {
            Timber.d("MegaRequest.TYPE_RESTORE");
            if (e.getErrorCode() == MegaError.API_OK) {
                if (getAccessLevel() <= ACCESS_READWRITE) {
                    showSnackbar(getString(R.string.version_as_new_file_created));
                } else {
                    showSnackbar(getString(R.string.version_restored));
                }
            } else {
                showSnackbar(getString(R.string.general_text_error));
            }
        }
    }


    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
                                        MegaError e) {
        Timber.w("onRequestTemporaryError");
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);

        MegaNode vNode = nodeVersions.get(position);
        MimeTypeList mimetype = MimeTypeList.typeForName(vNode.getName());

        if (adapter.isMultipleSelect()) {
            adapter.toggleSelection(position);
            updateActionModeTitle();
        } else if (mimetype.isImage()) {
            Intent intent = ImageViewerActivity.getIntentForSingleNode(this, vNode.getHandle(), true);
            putThumbnailLocation(intent, listView, position, VIEWER_FROM_FILE_VERSIONS, adapter);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (mimetype.isVideoReproducible() || mimetype.isAudio()) {
            Intent mediaIntent;
            boolean internalIntent;
            boolean opusFile = false;
            if (mimetype.isVideoNotSupported() || mimetype.isAudioNotSupported()) {
                mediaIntent = new Intent(Intent.ACTION_VIEW);
                internalIntent = false;
                String[] s = vNode.getName().split("\\.");
                if (s.length > 1 && s[s.length - 1].equals("opus")) {
                    opusFile = true;
                }
            } else {
                mediaIntent = getMediaIntent(this, vNode.getName());
                internalIntent = true;
            }

            mediaIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, VERSIONS_ADAPTER);
            putThumbnailLocation(mediaIntent, listView, position, VIEWER_FROM_FILE_BROWSER, adapter);

            mediaIntent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, vNode.getName());

            String localPath = getLocalFile(vNode);

            if (localPath != null) {
                File mediaFile = new File(localPath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                    Uri mediaFileUri = FileProvider.getUriForFile(this, AUTHORITY_STRING_FILE_PROVIDER, mediaFile);
                    if (mediaFileUri == null) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                    } else {
                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(vNode.getName()).getType());
                    }
                } else {
                    Uri mediaFileUri = Uri.fromFile(mediaFile);
                    if (mediaFileUri == null) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                    } else {
                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(vNode.getName()).getType());
                    }
                }
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                setupStreamingServer(megaApi, this);

                String url = megaApi.httpServerGetLocalLink(vNode);
                if (url != null) {
                    Uri parsedUri = Uri.parse(url);
                    if (parsedUri != null) {
                        mediaIntent.setDataAndType(parsedUri, mimetype.getType());
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                    }
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                }
            }
            mediaIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, vNode.getHandle());
            if (opusFile) {
                mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
            }
            if (internalIntent) {
                startActivity(mediaIntent);
            } else {
                if (isIntentAvailable(this, mediaIntent)) {
                    startActivity(mediaIntent);
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);
                    downloadNodes(Collections.singletonList(vNode));
                }
            }
            overridePendingTransition(0, 0);
        } else if (mimetype.isURL()) {
            manageURLNode(this, megaApi, vNode);
        } else if (mimetype.isPdf()) {
            Intent pdfIntent = new Intent(this, PdfViewerActivity.class);

            pdfIntent.putExtra(INTENT_EXTRA_KEY_INSIDE, true);
            pdfIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, VERSIONS_ADAPTER);

            String localPath = getLocalFile(vNode);

            if (localPath != null) {
                File mediaFile = new File(localPath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                    pdfIntent.setDataAndType(FileProvider.getUriForFile(this, AUTHORITY_STRING_FILE_PROVIDER, mediaFile), MimeTypeList.typeForName(vNode.getName()).getType());
                } else {
                    pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(vNode.getName()).getType());
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                setupStreamingServer(megaApi, this);
                String url = megaApi.httpServerGetLocalLink(vNode);
                if (url != null) {
                    Uri parsedUri = Uri.parse(url);
                    if (parsedUri != null) {
                        pdfIntent.setDataAndType(parsedUri, mimetype.getType());
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                    }
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                }
            }
            pdfIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, vNode.getHandle());
            putThumbnailLocation(pdfIntent, listView, position, VIEWER_FROM_FILE_BROWSER, adapter);
            if (isIntentAvailable(this, pdfIntent)) {
                startActivity(pdfIntent);
            } else {
                Toast.makeText(this, StringResourcesUtils.getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
                downloadNodes(Collections.singletonList(vNode));
            }
            overridePendingTransition(0, 0);
        } else if (mimetype.isOpenableTextFile(vNode.getSize())) {
            manageTextFileIntent(this, vNode, VERSIONS_ADAPTER);
        } else {
            showOptionsPanel(vNode, position);
        }
    }

    private void updateActionModeTitle() {
        Timber.d("updateActionModeTitle");
        if (actionMode == null) {
            return;
        }
        List<MegaNode> nodes = adapter.getSelectedNodes();

        Resources res = getResources();
        String format = "%d %s";

        actionMode.setTitle(String.format(format, nodes.size(), res.getQuantityString(R.plurals.general_num_files, nodes.size())));
        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            e.printStackTrace();
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
        switch (v.getId()) {
            case R.id.file_contact_list_layout: {
                Intent i = new Intent(this, ManagerActivity.class);
                i.setAction(ACTION_REFRESH_PARENTHANDLE_BROWSER);
                i.putExtra("parentHandle", node.getHandle());
                startActivity(i);
                finish();
                break;
            }
        }
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
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

        boolean thisNode = false;
        boolean anyChild = false;
        if (nodes == null) {
            return;
        }
        MegaNode n = null;
        Iterator<MegaNode> it = nodes.iterator();
        while (it.hasNext()) {
            MegaNode nodeToCheck = it.next();
            if (nodeToCheck != null) {
                if (nodeToCheck.getHandle() == node.getHandle()) {
                    thisNode = true;
                    n = nodeToCheck;
                } else {
                    for (int j = 0; j < nodeVersions.size(); j++) {
                        if (nodeToCheck.getHandle() == nodeVersions.get(j).getHandle()) {
                            if (anyChild == false) {
                                anyChild = true;
                            }
                        }
                    }
                }
            }
        }

        if ((!thisNode) && (!anyChild)) {
            Timber.w("Exit - Not related to this node");
            return;
        }

        //Check if the parent handle has changed
        if (n != null) {
            if (n.hasChanged(MegaNode.CHANGE_TYPE_PARENT)) {
                MegaNode oldParent = megaApi.getParentNode(node);
                MegaNode newParent = megaApi.getParentNode(n);
                if (oldParent.getHandle() == newParent.getHandle()) {
                    if (newParent.isFile()) {
                        Timber.d("New version added");
                        node = newParent;
                    } else {
                        finish();
                    }
                } else {
                    node = n;
                }
Timber.d("Node name: %s",  node.getName());
                if (megaApi.hasVersions(node)) {
                    nodeVersions = megaApi.getVersions(node);
                } else {
                    nodeVersions = null;
                }
            } else if (n.hasChanged(MegaNode.CHANGE_TYPE_REMOVED)) {
                if (thisNode) {
                    if (nodeVersions != null) {
                        node = nodeVersions.get(1);
                        if (megaApi.hasVersions(node)) {
                            nodeVersions = megaApi.getVersions(node);
                        } else {
                            nodeVersions = null;
                        }
                    } else {
                        finish();
                    }
                } else if (anyChild) {
                    if (megaApi.hasVersions(n)) {
                        nodeVersions = megaApi.getVersions(n);
                    } else {
                        nodeVersions = null;
                    }
                }

            } else {
                node = n;
                if (megaApi.hasVersions(node)) {
                    nodeVersions = megaApi.getVersions(node);
                } else {
                    nodeVersions = null;
                }
            }
        } else {
            if (anyChild) {
                if (megaApi.hasVersions(node)) {
                    nodeVersions = megaApi.getVersions(node);
                } else {
                    nodeVersions = null;
                }

            }
        }

        if (nodeVersions == null || nodeVersions.size() == 1) {
            finish();
        } else {
Timber.d("After update - nodeVersions size: %s",  nodeVersions.size());

            if (adapter != null) {
                adapter.setNodes(nodeVersions);
                adapter.notifyDataSetChanged();
            } else {
                adapter = new VersionsFileAdapter(this, nodeVersions, listView);
                listView.setAdapter(adapter);
            }

            GetVersionsSizeTask getVersionsSizeTask = new GetVersionsSizeTask();
            getVersionsSizeTask.execute();
        }
    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
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

    public void checkRevertVersion() {
        if (getAccessLevel() <= ACCESS_READWRITE) {
            checkPermissionRevertVersionDialog = new MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setTitle(R.string.permissions_error_label)
                    .setMessage(R.string.alert_not_enough_permissions_revert)
                    .setPositiveButton(R.string.create_new_file_action, (dialog, which) -> revertVersion())
                    .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                    })
                    .show();
        } else {
            revertVersion();
        }
    }

    private void revertVersion() {
        Timber.d("revertVersion");
        megaApi.restoreVersion(selectedNode, this);
    }

    public void removeVersion() {
        Timber.d("removeVersion");
        megaApi.removeVersion(selectedNode, this);
    }

    public void removeVersions(List<MegaNode> removeNodes) {
        Timber.d("removeVersion");
        totalRemoveSelected = removeNodes.size();
        errorRemove = 0;
        completedRemove = 0;

        for (int i = 0; i < removeNodes.size(); i++) {
            megaApi.removeVersion(removeNodes.get(i), this);
        }
    }

    public MegaNode getSelectedNode() {
        return selectedNode;
    }

    public void showSnackbar(String s) {
        showSnackbar(container, s);
    }

    public void updateSize(String size) {
        Timber.d("Size: %s", size);
        this.versionsSize = size;
        adapter.notifyItemChanged(1);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    public void showConfirmationRemoveVersion() {
        deleteVersionConfirmationDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getResources().getQuantityString(R.plurals.title_dialog_delete_version, 1))
                .setMessage(getString(R.string.content_dialog_delete_version))
                .setPositiveButton(R.string.context_delete, (dialog, which) -> removeVersion())
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                })
                .show();
    }

    public void showConfirmationRemoveVersions(final List<MegaNode> removeNodes) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        String message;
        String title;

        if (removeNodes.size() == 1) {
            title = getResources().getQuantityString(R.plurals.title_dialog_delete_version, 1);
            message = getResources().getString(R.string.content_dialog_delete_version);
        } else {
            title = getResources().getQuantityString(R.plurals.title_dialog_delete_version, removeNodes.size());
            message = getResources().getString(R.string.content_dialog_delete_multiple_version, removeNodes.size());
        }

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.context_delete, (dialog, which) -> removeVersions(removeNodes))
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                })
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_NODE_HANDLE, node.getHandle());
        outState.putBoolean(CHECKING_REVERT_VERSION, checkPermissionRevertVersionDialog != null && checkPermissionRevertVersionDialog.isShowing());
        outState.putLong(SELECTED_NODE_HANDLE, selectedNodeHandle);
        outState.putInt(SELECTED_POSITION, selectedPosition);
        outState.putBoolean(DELETING_VERSION_DIALOG_SHOWN, deleteVersionConfirmationDialog != null && deleteVersionConfirmationDialog.isShowing());
        outState.putBoolean(DELETING_HISTORY_VERSION_DIALOG_SHOWN, deleteVersionHistoryDialog != null && deleteVersionHistoryDialog.isShowing());

        nodeSaver.saveState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        selectedNodeHandle = savedInstanceState.getLong(SELECTED_NODE_HANDLE, INVALID_HANDLE);
        selectedPosition = savedInstanceState.getInt(SELECTED_POSITION);

        nodeSaver.restoreState(savedInstanceState);
        selectedNode = megaApi.getNodeByHandle(selectedNodeHandle);

        if (selectedNode != null) {
            if (savedInstanceState.getBoolean(CHECKING_REVERT_VERSION, false)) {
                checkRevertVersion();
            }
            if (savedInstanceState.getBoolean(DELETING_VERSION_DIALOG_SHOWN, false)) {
                showConfirmationRemoveVersion();
            }
        }
        if (savedInstanceState.getBoolean(DELETING_HISTORY_VERSION_DIALOG_SHOWN, false)) {
            showDeleteVersionHistoryDialog();
        }

    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void startActionMode(int position) {
        actionMode = startSupportActionMode(new ActionBarCallBack());
        itemClick(position);
    }
}

