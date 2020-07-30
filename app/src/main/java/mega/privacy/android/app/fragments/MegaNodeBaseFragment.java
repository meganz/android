package mega.privacy.android.app.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.NewHeaderItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.managerSections.RotatableFragment;
import mega.privacy.android.app.utils.MegaNodeUtil;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.fragments.managerFragments.LinksFragment.getLinksOrderCloud;
import static mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;

public abstract class MegaNodeBaseFragment extends RotatableFragment {
    private static int MARGIN_BOTTOM_LIST = 85;

    protected ManagerActivityLollipop managerActivity;

    public static ImageView imageDrag;

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

    public NewHeaderItemDecoration headerItemDecoration;
    protected int placeholderCount;

    protected ImageView emptyImageView;
    protected LinearLayout emptyLinearLayout;
    protected TextView emptyTextViewFirst;

    protected abstract void setNodes(ArrayList<MegaNode> nodes);

    protected abstract void setEmptyView();

    protected abstract int onBackPressed();

    protected abstract void itemClick(int position, int[] screenPosition, ImageView imageView);

    protected abstract void refresh();

    public MegaNodeBaseFragment() {
        prefs = dbH.getPreferences();
        downloadLocationDefaultPath = getDownloadLocation();
    }

    protected abstract class BaseActionBarCallBack implements ActionMode.Callback {

        protected List<MegaNode> selected;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.cloud_storage_action, menu);
            if (context instanceof ManagerActivityLollipop) {
                managerActivity.hideFabButton();
                managerActivity.showHideBottomNavigationView(true);
                managerActivity.changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
            }
            checkScroll();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            selected = adapter.getSelectedNodes();
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
                    nC.prepareForDownload(handleList, false);
                    break;

                case R.id.cab_menu_rename:
                    if (selected.get(0) == null) {
                        logWarning("The selected node is NULL");
                        break;
                    }
                    managerActivity.showRenameDialog(selected.get(0), selected.get(0).getName());
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

                    ArrayList<MegaNode> nodes = new ArrayList<>();
                    nodes.addAll(selected);
                    managerActivity.showConfirmationRemoveSeveralPublicLinks(nodes);
                    hideActionMode();
                case R.id.cab_menu_leave_share:
                    managerActivity.showConfirmationLeaveMultipleShares(handleList);
                    break;

                case R.id.cab_menu_send_to_chat:
                    nC.checkIfNodesAreMineAndSelectChatsToSendNodes(adapter.getArrayListSelectedNodes());
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
            if (context instanceof ManagerActivityLollipop) {
                managerActivity.showFabButton();
                managerActivity.showHideBottomNavigationView(false);
                managerActivity.changeStatusBarColor(COLOR_STATUS_BAR_ZERO_DELAY);
            }
            checkScroll();
        }

        protected boolean notAllNodesSelected() {
            return selected.size() < adapter.getItemCount() - adapter.getPlaceholderCount();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ManagerActivityLollipop) {
            managerActivity = (ManagerActivityLollipop) context;
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

            updateActionModeTitle();
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

    public ImageView getImageDrag(int position) {
        logDebug("Position: " + position);
        if (adapter != null) {
            if (adapter.getAdapterType() == ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
                View v = mLayoutManager.findViewByPosition(position);
                if (v != null) {
                    return (ImageView) v.findViewById(R.id.file_list_thumbnail);
                }
            } else if (gridLayoutManager != null) {
                View v = gridLayoutManager.findViewByPosition(position);
                if (v != null) {
                    return (ImageView) v.findViewById(R.id.file_grid_thumbnail);
                }
            }
        }
        return null;
    }

    public void updateScrollPosition(int position) {
        logDebug("Position: " + position);
        if (adapter != null) {
            if (adapter.getAdapterType() == ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
                mLayoutManager.scrollToPosition(position);
            } else if (gridLayoutManager != null) {
                gridLayoutManager.scrollToPosition(position);
            }
        }
    }

    public void addSectionTitle(List<MegaNode> nodes, int type) {
        Map<Integer, String> sections = new HashMap<>();
        int folderCount = 0;
        int fileCount = 0;
        for (MegaNode node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.isFolder()) {
                folderCount++;
            }
            if (node.isFile()) {
                fileCount++;
            }
        }

        if (type == ITEM_VIEW_TYPE_GRID) {
            int spanCount = 2;
            if (recyclerView instanceof NewGridRecyclerView) {
                spanCount = ((NewGridRecyclerView) recyclerView).getSpanCount();
            }
            if (folderCount > 0) {
                for (int i = 0; i < spanCount; i++) {
                    sections.put(i, getString(R.string.general_folders));
                }
            }

            if (fileCount > 0) {
                placeholderCount = (folderCount % spanCount) == 0 ? 0 : spanCount - (folderCount % spanCount);
                if (placeholderCount == 0) {
                    for (int i = 0; i < spanCount; i++) {
                        sections.put(folderCount + i, getString(R.string.general_files));
                    }
                } else {
                    for (int i = 0; i < spanCount; i++) {
                        sections.put(folderCount + placeholderCount + i, getString(R.string.general_files));
                    }
                }
            }
        } else {
            placeholderCount = 0;
            sections.put(0, getString(R.string.general_folders));
            sections.put(folderCount, getString(R.string.general_files));
        }

        if (headerItemDecoration == null) {
            logDebug("Create new decoration");
            headerItemDecoration = new NewHeaderItemDecoration(context);
        } else {
            logDebug("Remove old decoration");
            recyclerView.removeItemDecoration(headerItemDecoration);
        }
        headerItemDecoration.setType(type);
        headerItemDecoration.setKeys(sections);
        recyclerView.addItemDecoration(headerItemDecoration);
    }

    public void checkScroll() {
        if (recyclerView != null) {
            if ((recyclerView.canScrollVertically(-1) && recyclerView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect())) {
                managerActivity.changeActionBarElevation(true);
            } else {
                managerActivity.changeActionBarElevation(false);
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

    public void openFile(MegaNode node, int fragmentAdapter, int position, int[] screenPosition, ImageView imageView) {
        MimeTypeList mimeType = MimeTypeList.typeForName(node.getName());
        String mimeTypeType = mimeType.getType();
        Intent intent = null;
        boolean internalIntent = false;

        if (mimeType.isImage()) {
            internalIntent = true;
            intent = new Intent(context, FullScreenImageViewerLollipop.class);
            intent.putExtra("placeholder", placeholderCount);
            intent.putExtra("position", position);
            intent.putExtra("adapterType", fragmentAdapter);
            intent.putExtra("isFolderLink", false);
            intent.putExtra("parentNodeHandle", getParentHandle(fragmentAdapter));
            intent.putExtra("orderGetChildren", getIntentOrder(fragmentAdapter));
            intent.putExtra("screenPosition", screenPosition);
        } else if (mimeType.isVideoReproducible() || mimeType.isAudio()) {
            boolean opusFile = false;

            if (mimeType.isVideoNotSupported() || mimeType.isAudioNotSupported()) {
                intent = new Intent(Intent.ACTION_VIEW);
                internalIntent = false;
                String[] s = node.getName().split("\\.");
                opusFile = s.length > 1 && s[s.length - 1].equals("opus");
            } else {
                intent = new Intent(context, AudioVideoPlayerLollipop.class);
                internalIntent = true;
            }

            intent.putExtra("position", position);
            intent.putExtra("placeholder", placeholderCount);
            intent.putExtra("parentNodeHandle", getParentHandle(fragmentAdapter));
            intent.putExtra("orderGetChildren", getIntentOrder(fragmentAdapter));
            intent.putExtra("adapterType", fragmentAdapter);
            intent.putExtra("screenPosition", screenPosition);
            intent.putExtra("HANDLE", node.getHandle());
            intent.putExtra("FILENAME", node.getName());

            String localPath = getLocalFile(context, node.getName(), node.getSize());
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
        } else if (mimeType.isURL()) {
            logDebug("Is URL file");
            String localPath = getLocalFile(context, node.getName(), node.getSize());
            
            if (localPath != null) {
                File f = new File(localPath);
                InputStream instream = null;

                try {
                    // open the file for reading
                    instream = new FileInputStream(f.getAbsolutePath());
                    // prepare the file for reading
                    InputStreamReader inputreader = new InputStreamReader(instream);
                    BufferedReader buffreader = new BufferedReader(inputreader);

                    String line1 = buffreader.readLine();
                    if (line1 != null) {
                        String line2 = buffreader.readLine();

                        String url = line2.replace("URL=", "");

                        logDebug("Is URL - launch browser intent");
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                        return;
                    }
                } catch (Exception ex) {
                    logError("EXCEPTION reading file", ex);
                } finally {
                    try {
                        if (instream != null) {
                            instream.close();
                        }
                    } catch (IOException e) {
                        logError("EXCEPTION closing InputStream", e);
                    }
                }

                intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, f), TYPE_TEXT_PLAIN);
                } else {
                    intent.setDataAndType(Uri.fromFile(f), TYPE_TEXT_PLAIN);
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        } else if (mimeType.isPdf()) {
            logDebug("isFile:isPdf");
            intent = new Intent(context, PdfViewerActivityLollipop.class);
            intent.putExtra("inside", true);
            intent.putExtra("adapterType", fragmentAdapter);
            intent.putExtra("HANDLE", node.getHandle());
            intent.putExtra("screenPosition", screenPosition);

            String localPath = getLocalFile(context, node.getName(), node.getSize());
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
        }

        if (intent != null) {
            if (internalIntent || isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                managerActivity.overridePendingTransition(0, 0);
                imageDrag = imageView;

                return;
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
            }
        }

        ArrayList<Long> handleList = new ArrayList<>();
        handleList.add(node.getHandle());
        NodeController nC = new NodeController(context);
        nC.prepareForDownload(handleList, true);
    }

    private int getIntentOrder(int fragmentAdapter) {
        switch (fragmentAdapter) {
            case LINKS_ADAPTER:
                return getLinksOrderCloud(managerActivity.orderCloud, managerActivity.isFirstNavigationLevel());

            case INCOMING_SHARES_ADAPTER:
            case OUTGOING_SHARES_ADAPTER:
                if (managerActivity.isFirstNavigationLevel()) {
                    return managerActivity.getOrderOthers();
                }

            default:
                return managerActivity.orderCloud;
        }
    }

    protected View getListView(LayoutInflater inflater, ViewGroup container) {
        View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

        recyclerView = v.findViewById(R.id.file_list_view_browser);
        mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);
        fastScroller = v.findViewById(R.id.fastscroll);
        setRecyclerView();

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

        emptyImageView = v.findViewById(R.id.file_grid_empty_image);
        emptyLinearLayout = v.findViewById(R.id.file_grid_empty_text);
        emptyTextViewFirst = v.findViewById(R.id.file_grid_empty_text_first);

        if (adapter != null) {
            adapter.setAdapterType(ITEM_VIEW_TYPE_GRID);
        }

        return v;
    }

    private void setRecyclerView() {
        recyclerView.setPadding(0, 0, 0, px2dp(MARGIN_BOTTOM_LIST, outMetrics));
        recyclerView.setHasFixedSize(true);
        recyclerView.setClipToPadding(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });
        fastScroller.setRecyclerView(recyclerView);
    }

    private String getGeneralEmptyView() {
        if (isScreenInPortrait(context)) {
            emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
        } else {
            emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
        }

        return context.getString(R.string.file_browser_empty_folder_new);
    }

    protected void setFinalEmptyView(String text) {
        if (text == null) {
            text = getGeneralEmptyView();
        }

        try {
            text = text.replace("[A]", "<font color=\'#000000\'>");
            text = text.replace("[/A]", "</font>");
            text = text.replace("[B]", "<font color=\'#7a7a7a\'>");
            text = text.replace("[/B]", "</font>");
        } catch (Exception e) {
            logWarning("Exception formatting text", e);
        }

        emptyTextViewFirst.setText(getSpannedHtmlText(text));
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
}
