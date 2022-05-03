package mega.privacy.android.app.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.appcompat.view.ActionMode;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.PositionDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.fragments.homepage.EventObserver;
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel;
import mega.privacy.android.app.fragments.managerFragments.LinksFragment;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.PdfViewerActivity;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.main.adapters.RotatableAdapter;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.main.managerSections.IncomingSharesFragment;
import mega.privacy.android.app.main.managerSections.OutgoingSharesFragment;
import mega.privacy.android.app.main.managerSections.RotatableFragment;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.fragments.managerFragments.LinksFragment.getLinksOrderCloud;
import static mega.privacy.android.app.main.ManagerActivity.*;
import static mega.privacy.android.app.main.adapters.MegaNodeAdapter.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode;
import static mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped;
import static mega.privacy.android.app.utils.MegaNodeUtil.showConfirmationLeaveIncomingShares;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;

@AndroidEntryPoint
public abstract class MegaNodeBaseFragment extends RotatableFragment {
    private static int MARGIN_BOTTOM_LIST = 85;

    @Inject
    protected
    SortOrderManagement sortOrderManagement;

    protected ManagerActivity managerActivity;

    protected ActionMode actionMode;

    protected ArrayList<MegaNode> nodes = new ArrayList<>();
    protected MegaNodeAdapter adapter;

    protected MegaPreferences prefs;
    protected String downloadLocationDefaultPath;
    protected Stack<Integer> lastPositionStack = new Stack<>();

    protected FastScroller fastScroller;
    protected RecyclerView recyclerView;
    protected LinearLayoutManager mLayoutManager;
    protected CustomizedGridLayoutManager gridLayoutManager;

    protected ImageView emptyImageView;
    protected LinearLayout emptyLinearLayout;
    protected TextView emptyTextViewFirst;

    protected SortByHeaderViewModel sortByHeaderViewModel;

    protected abstract void setNodes(ArrayList<MegaNode> nodes);

    protected abstract void setEmptyView();

    protected abstract int onBackPressed();

    protected abstract void itemClick(int position);

    /**
     * Navigates to a new child folder.
     *
     * @param node The folder node.
     */
    protected abstract void navigateToFolder(MegaNode node);

    protected abstract void refresh();

    public MegaNodeBaseFragment() {
        prefs = dbH.getPreferences();
        downloadLocationDefaultPath = getDownloadLocation();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sortByHeaderViewModel = new ViewModelProvider(this).get(SortByHeaderViewModel.class);

        sortByHeaderViewModel.getShowDialogEvent().observe(getViewLifecycleOwner(),
                new EventObserver<>(this::showSortByPanel));

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    protected abstract class BaseActionBarCallBack implements ActionMode.Callback {

        protected List<MegaNode> selected;
        private int currentTab;

        public BaseActionBarCallBack (int currentTab) {
            this.currentTab = currentTab;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.cloud_storage_action, menu);
            if (context instanceof ManagerActivity) {
                managerActivity.hideFabButton();
                managerActivity.hideTabs(true, currentTab);
                managerActivity.showHideBottomNavigationView(true);
            }
            checkScroll();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            selected = adapter.getSelectedNodes();

            menu.findItem(R.id.cab_menu_share_link)
                    .setTitle(StringResourcesUtils.getQuantityString(R.plurals.get_links, selected.size()));

            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            logDebug("onActionItemClicked");
            ArrayList<Long> handleList = new ArrayList<>();
            for (MegaNode node : selected) {
                handleList.add(node.getHandle());
            }

            NodeController nC = new NodeController(context);

            switch (item.getItemId()) {
                case R.id.cab_menu_download:
                    managerActivity.saveNodesToDevice(selected, false, false, false, false);
                    hideActionMode();
                    break;

                case R.id.cab_menu_rename:
                    if (selected.get(0) == null) {
                        logWarning("The selected node is NULL");
                        break;
                    }
                    managerActivity.showRenameDialog(selected.get(0));
                    hideActionMode();
                    break;

                case R.id.cab_menu_copy:
                    nC.chooseLocationToCopyNodes(handleList);
                    hideActionMode();
                    break;

                case R.id.cab_menu_move:
                    nC.chooseLocationToMoveNodes(handleList);
                    hideActionMode();
                    break;

                case R.id.cab_menu_share_folder:
                    nC.selectContactToShareFolders(handleList);
                    hideActionMode();
                    break;

                case R.id.cab_menu_share_out:
                    MegaNodeUtil.shareNodes(context, selected);
                    hideActionMode();
                    break;

                case R.id.cab_menu_share_link:
                case R.id.cab_menu_edit_link:
                    if (selected.get(0) == null) {
                        logWarning("The selected node is NULL");
                        break;
                    }
                    managerActivity.showGetLinkActivity(selected.get(0).getHandle());
                    hideActionMode();
                    break;

                case R.id.cab_menu_remove_link:
                    if (selected.size() == 1 && selected.get(0) == null) {
                        logWarning("The selected node is NULL");
                        break;
                    }

                    ArrayList<MegaNode> nodes = new ArrayList<>(selected);
                    managerActivity.showConfirmationRemoveSeveralPublicLinks(nodes);
                    hideActionMode();
                    break;

                case R.id.cab_menu_leave_share:
                    showConfirmationLeaveIncomingShares(requireActivity(),
                            (SnackbarShower) requireActivity(), handleList);
                    break;

                case R.id.cab_menu_send_to_chat:
                    managerActivity.attachNodesToChats(adapter.getArrayListSelectedNodes());
                    hideActionMode();
                    break;

                case R.id.cab_menu_trash:
                    managerActivity.askConfirmationMoveToRubbish(handleList);
                    break;

                case R.id.cab_menu_select_all:
                    selectAll();
                    break;

                case R.id.cab_menu_clear_selection:
                    hideActionMode();
                    break;

                case R.id.cab_menu_remove_share:
                    managerActivity.showConfirmationRemoveAllSharingContacts(selected);
                    break;
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            clearSelections();
            adapter.setMultipleSelect(false);
            if (context instanceof ManagerActivity) {
                managerActivity.showFabButton();
                managerActivity.hideTabs(false, currentTab);
                managerActivity.showHideBottomNavigationView(false);
            }
            checkScroll();
        }

        protected boolean notAllNodesSelected() {
            return selected.size() < adapter.getItemCount() - adapter.getPlaceholderCount();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof ManagerActivity) {
            managerActivity = (ManagerActivity) context;
        }
    }

    @Override
    public void onDestroy() {
        if (adapter != null) {
            adapter.clearTakenDownDialog();
        }
        super.onDestroy();
    }

    @Override
    protected RotatableAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void activateActionMode() {
        if (adapter != null && !adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
        }
    }

    @Override
    public void multipleItemClick(int position) {
        if (adapter != null) {
            adapter.toggleSelection(position);
        }
    }

    @Override
    public void reselectUnHandledSingleItem(int position) {
        if (adapter != null) {
            adapter.filClicked(position);
        }
    }

    @Override
    protected void updateActionModeTitle() {
        if (actionMode == null || getActivity() == null || adapter == null) {
            return;
        }
        List<MegaNode> documents = adapter.getSelectedNodes();
        int files = 0;
        int folders = 0;
        for (MegaNode document : documents) {
            if (document.isFile()) {
                files++;
            } else if (document.isFolder()) {
                folders++;
            }
        }

        String title;
        int sum = files + folders;

        if (files == 0 && folders == 0) {
            title = Integer.toString(sum);
        } else if (files == 0) {
            title = Integer.toString(folders);
        } else if (folders == 0) {
            title = Integer.toString(files);
        } else {
            title = Integer.toString(sum);
        }
        actionMode.setTitle(title);
        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            e.printStackTrace();
            logError("Invalidate error", e);
        }
    }

    /**
     * Shows the Sort by panel.
     *
     * @param unit Unit event.
     * @return Null.
     */
    protected Unit showSortByPanel(Unit unit) {
        managerActivity.showNewSortByPanel(getCurrentSharesTab() == INCOMING_TAB
                ? ORDER_OTHERS
                : ORDER_CLOUD);

        return null;
    }

    public ActionMode getActionMode() {
        return actionMode;
    }

    public boolean isMultipleSelect() {
        return adapter != null && adapter.isMultipleSelect();
    }

    public void selectAll() {
        if (adapter != null) {
            if (!adapter.isMultipleSelect()) {
                activateActionMode();
            }

            adapter.selectAll();

            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    public void hideMultipleSelect() {
        if (adapter != null) {
            adapter.setMultipleSelect(false);
        }

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void clearSelections() {
        if (adapter != null && adapter.isMultipleSelect()) {
            adapter.clearSelections();
        }
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
    }

    public void visibilityFastScroller() {
        if (adapter == null || adapter.getItemCount() < MIN_ITEMS_SCROLLBAR) {
            fastScroller.setVisibility(View.GONE);
        } else {
            fastScroller.setVisibility(View.VISIBLE);
        }
    }

    public void checkScroll() {
        if (recyclerView != null) {
            if ((recyclerView.canScrollVertically(-1) && recyclerView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect())) {
                managerActivity.changeAppBarElevation(true);
            } else {
                managerActivity.changeAppBarElevation(false);
            }
        }
    }

    protected void checkEmptyView() {
        if (adapter != null && adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyImageView.setVisibility(View.VISIBLE);
            emptyLinearLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyImageView.setVisibility(View.GONE);
            emptyLinearLayout.setVisibility(View.GONE);
        }
    }

    public void openFile(MegaNode node, int fragmentAdapter, int position) {
        MimeTypeList mimeType = MimeTypeList.typeForName(node.getName());
        String mimeTypeType = mimeType.getType();
        Intent intent;
        boolean internalIntent = false;

        if (mimeType.isImage()) {
            intent = ImageViewerActivity.getIntentForParentNode(
                    requireContext(),
                    getParentHandle(fragmentAdapter),
                    getIntentOrder(fragmentAdapter),
                    node.getHandle()
            );

            launchIntent(intent, true, position);
        } else if (mimeType.isVideoReproducible() || mimeType.isAudio()) {
            boolean opusFile = false;

            if (mimeType.isVideoNotSupported() || mimeType.isAudioNotSupported()) {
                intent = new Intent(Intent.ACTION_VIEW);
                String[] s = node.getName().split("\\.");
                opusFile = s.length > 1 && s[s.length - 1].equals("opus");
            } else {
                intent = getMediaIntent(context, node.getName());
                internalIntent = true;
            }

            intent.putExtra("position", position);
            intent.putExtra("placeholder", adapter.getPlaceholderCount());
            intent.putExtra("parentNodeHandle", getParentHandle(fragmentAdapter));
            intent.putExtra("orderGetChildren", getIntentOrder(fragmentAdapter));
            intent.putExtra("adapterType", fragmentAdapter);
            intent.putExtra("HANDLE", node.getHandle());
            intent.putExtra("FILENAME", node.getName());

            String localPath = getLocalFile(node);
            if (localPath != null) {
                File mediaFile = new File(localPath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, mediaFile), MimeTypeList.typeForName(node.getName()).getType());
                } else {
                    intent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(node.getName()).getType());
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart();
                    intent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
                } else {
                    logWarning("ERROR:httpServerAlreadyRunning");
                }

                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);

                if (mi.totalMem > BUFFER_COMP) {
                    logDebug("total mem: " + mi.totalMem + " allocate 32 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                } else {
                    logDebug("total mem: " + mi.totalMem + " allocate 16 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                }

                String url = megaApi.httpServerGetLocalLink(node);
                Uri parsedUri = null;
                if (url != null) {
                    parsedUri = Uri.parse(url);
                }

                if (parsedUri != null) {
                    intent.setDataAndType(parsedUri, mimeTypeType);
                } else {
                    logError("ERROR:httpServerGetLocalLink");
                    managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), INVALID_HANDLE);
                    return;
                }
            }

            if (opusFile) {
                intent.setDataAndType(intent.getData(), "audio/*");
            }

            launchIntent(intent, internalIntent, position);
        } else if (mimeType.isURL()) {
            manageURLNode(context, megaApi, node);
        } else if (mimeType.isPdf()) {
            logDebug("isFile:isPdf");
            intent = new Intent(context, PdfViewerActivity.class);
            intent.putExtra("inside", true);
            intent.putExtra("adapterType", fragmentAdapter);
            intent.putExtra("HANDLE", node.getHandle());

            String localPath = getLocalFile(node);
            if (localPath != null) {
                File mediaFile = new File(localPath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, mediaFile), mimeTypeType);
                } else {
                    intent.setDataAndType(Uri.fromFile(mediaFile), mimeTypeType);
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart();
                    intent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
                }

                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);

                if (mi.totalMem > BUFFER_COMP) {
                    logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                } else {
                    logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                }

                String url = megaApi.httpServerGetLocalLink(node);
                Uri parsedUri = null;
                if (url != null) {
                    parsedUri = Uri.parse(url);
                }

                if (parsedUri != null) {
                    intent.setDataAndType(parsedUri, mimeTypeType);
                } else {
                    logError("ERROR:httpServerGetLocalLink");
                    managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), INVALID_HANDLE);
                    return;
                }
            }

            launchIntent(intent, false, position);
        } else if (mimeType.isOpenableTextFile(node.getSize())) {
            manageTextFileIntent(requireContext(), node, fragmentAdapter);
        } else {
            logDebug("itemClick:isFile:otherOption");
            onNodeTapped(context, node, managerActivity::saveNodeByTap, managerActivity, managerActivity);
        }
    }

    /**
     * Launch corresponding intent to open the file based on its type.
     *
     * @param intent Intent to launch activity.
     * @param internalIntent true, if the intent is for launching an intent in-app; false, otherwise.
     * @param position Clicked item position.
     */
    private void launchIntent(Intent intent, boolean internalIntent, int position) {
        if (intent != null) {
            if (internalIntent || isIntentAvailable(context, intent)) {
                putThumbnailLocation(intent, recyclerView, position, viewerFrom(), adapter);
                context.startActivity(intent);
                managerActivity.overridePendingTransition(0, 0);
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
            }
        }
    }

    protected abstract int viewerFrom();

    private int getIntentOrder(int fragmentAdapter) {
        switch (fragmentAdapter) {
            case LINKS_ADAPTER:
                return getLinksOrderCloud(sortOrderManagement.getOrderCloud(),
                        managerActivity.isFirstNavigationLevel());

            case INCOMING_SHARES_ADAPTER:
            case OUTGOING_SHARES_ADAPTER:
                if (managerActivity.isFirstNavigationLevel()) {
                    return sortOrderManagement.getOrderOthers();
                }

            default:
                return sortOrderManagement.getOrderCloud();
        }
    }

    protected View getListView(LayoutInflater inflater, ViewGroup container) {
        View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

        recyclerView = v.findViewById(R.id.file_list_view_browser);
        mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new PositionDividerItemDecoration(requireContext(), getOutMetrics()));
        fastScroller = v.findViewById(R.id.fastscroll);
        setRecyclerView();
        recyclerView.setItemAnimator(noChangeRecyclerViewItemAnimator());

        emptyImageView = v.findViewById(R.id.file_list_empty_image);
        emptyLinearLayout = v.findViewById(R.id.file_list_empty_text);
        emptyTextViewFirst = v.findViewById(R.id.file_list_empty_text_first);

        if (adapter != null) {
            adapter.setAdapterType(ITEM_VIEW_TYPE_LIST);
        }

        return v;
    }

    protected View getGridView(LayoutInflater inflater, ViewGroup container) {
        View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);

        recyclerView = v.findViewById(R.id.file_grid_view_browser);
        gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();
        fastScroller = v.findViewById(R.id.fastscroll);
        setRecyclerView();
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        emptyImageView = v.findViewById(R.id.file_grid_empty_image);
        emptyLinearLayout = v.findViewById(R.id.file_grid_empty_text);
        emptyTextViewFirst = v.findViewById(R.id.file_grid_empty_text_first);

        if (adapter != null) {
            adapter.setAdapterType(ITEM_VIEW_TYPE_GRID);
        }

        return v;
    }

    /**
     * Gets the current shares tab depending on the current Fragment instance.
     *
     * @return The current shares tab.
     */
    private int getCurrentSharesTab() {
        int tab = ERROR_TAB;

        if (MegaNodeBaseFragment.this instanceof IncomingSharesFragment) {
            tab = INCOMING_TAB;
        } else if (MegaNodeBaseFragment.this instanceof OutgoingSharesFragment) {
            tab = OUTGOING_TAB;
        } else if (MegaNodeBaseFragment.this instanceof LinksFragment) {
            tab = LINKS_TAB;
        }

        return tab;
    }

    private void setRecyclerView() {
        recyclerView.setPadding(0, 0, 0, dp2px(MARGIN_BOTTOM_LIST, outMetrics));
        recyclerView.setHasFixedSize(true);
        recyclerView.setClipToPadding(false);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int tab = getCurrentSharesTab();

                if (managerActivity.getTabItemShares() == tab) {
                    checkScroll();
                }
            }
        });
        fastScroller.setRecyclerView(recyclerView);
    }

    private String getGeneralEmptyView() {
        if (isScreenInPortrait(context)) {
            emptyImageView.setImageResource(R.drawable.empty_folder_portrait);
        } else {
            emptyImageView.setImageResource(R.drawable.empty_folder_landscape);
        }

        return context.getString(R.string.file_browser_empty_folder_new);
    }

    protected void setFinalEmptyView(String text) {
        if (text == null) {
            text = getGeneralEmptyView();
        }

        try {
            text = text.replace("[A]","<font color=\'"
                    + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                    + "\'>");
            text = text.replace("[/A]","</font>");
            text = text.replace("[B]","<font color=\'"
                    + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                    + "\'>");
            text = text.replace("[/B]","</font>");
        } catch (Exception e) {
            logWarning("Exception formatting text", e);
        }

        emptyTextViewFirst.setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY));
        checkEmptyView();
    }

    private long getParentHandle(int fragmentAdapter) {
        switch (fragmentAdapter) {
            case INCOMING_SHARES_ADAPTER:
                return managerActivity.getParentHandleIncoming();

            case OUTGOING_SHARES_ADAPTER:
                return managerActivity.getParentHandleOutgoing();

            case LINKS_ADAPTER:
                return managerActivity.getParentHandleLinks();

            default:
                return INVALID_HANDLE;
        }

    }

    protected void hideActionMode() {
        clearSelections();
        hideMultipleSelect();
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeDragSupportEvents(getViewLifecycleOwner(), recyclerView, viewerFrom());
    }
}
