package mega.privacy.android.app.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;
import static nz.mega.sdk.MegaError.*;
import static nz.mega.sdk.MegaShare.*;

public abstract class MegaNodeBaseFragment extends RotatableFragment {
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
        downloadLocationDefaultPath = getDownloadLocation(getContext());
    }

    protected class BaseActionBarCallBack implements ActionMode.Callback {

        protected List<MegaNode> selected;

        protected boolean onlyOneFileSelected;
        protected boolean showShare;
        protected boolean allFiles;
        protected boolean showRename;
        protected boolean showCopy;
        protected boolean showMove;
        protected boolean showRemoveLink;
        protected boolean showLink;
        protected boolean showEditLink;
        protected boolean showSendToChat;
        protected boolean showTrash;
        protected boolean showSelectAll;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.file_browser_action, menu);
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
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            logDebug("onActionItemClicked");
            ArrayList<Long> handleList = new ArrayList<Long>();
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
                    clearSelections();
                    hideMultipleSelect();
                    break;

                case R.id.cab_menu_copy:
                    nC.chooseLocationToCopyNodes(handleList);
                    clearSelections();
                    hideMultipleSelect();
                    break;

                case R.id.cab_menu_move:
                    nC.chooseLocationToMoveNodes(handleList);
                    clearSelections();
                    hideMultipleSelect();
                    break;

                case R.id.cab_menu_share:
                    nC.selectContactToShareFolders(handleList);
                    clearSelections();
                    hideMultipleSelect();
                    break;

                case R.id.cab_menu_share_link:
                    if (selected.get(0) == null) {
                        logWarning("The selected node is NULL");
                        break;
                    }
                    managerActivity.showGetLinkActivity(selected.get(0).getHandle());
                    clearSelections();
                    hideMultipleSelect();
                    break;

                case R.id.cab_menu_share_link_remove:
                    if (onlyOneFileSelected && selected.get(0) == null) {
                        logWarning("The selected node is NULL");
                        break;
                    }

                    ArrayList<MegaNode> nodes = new ArrayList<>();
                    nodes.addAll(selected);
                    managerActivity.showConfirmationRemoveSeveralPublicLinks(null, nodes);
                    clearSelections();
                    hideMultipleSelect();
                    break;

                case R.id.cab_menu_edit_link:
                    if (selected.get(0) == null) {
                        logWarning("The selected node is NULL");
                        break;
                    }

                    managerActivity.showGetLinkActivity(selected.get(0).getHandle());
                    clearSelections();
                    hideMultipleSelect();
                    break;

                case R.id.cab_menu_leave_multiple_share:
                    ((ManagerActivityLollipop) context).showConfirmationLeaveMultipleShares(handleList);
                    break;

                case R.id.cab_menu_send_to_chat:
                    nC.checkIfNodesAreMineAndSelectChatsToSendNodes(adapter.getArrayListSelectedNodes());
                    clearSelections();
                    hideMultipleSelect();
                    break;

                case R.id.cab_menu_trash:
                    managerActivity.askConfirmationMoveToRubbish(handleList);
                    break;

                case R.id.cab_menu_select_all:
                    selectAll();
                    break;

                case R.id.cab_menu_unselect_all:
                    clearSelections();
                    hideMultipleSelect();
                    break;

                case R.id.cab_menu_remove_share:
                    break;
            }

            return false;
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

        protected void checkOptions() {
            onlyOneFileSelected = false;
            showShare = true;
            allFiles = true;
            showRename = false;
            showCopy = true;
            showMove = true;
            showRemoveLink = false;
            showLink = false;
            showEditLink = false;
            showSendToChat = true;
            showTrash = true;


            for (MegaNode node : selected) {
                if (!node.isFile()) {
                    allFiles = false;
                }

                if (!node.isFolder()) {
                    showShare = false;
                }

                if (megaApi.checkMove(node, megaApi.getRubbishNode()).getErrorCode() != API_OK) {
                    showTrash = false;
                }
            }

            if(!allFiles || !isChatEnabled()){
                showSendToChat = false;
            }

            if (selected.size() == 1) {
                onlyOneFileSelected = true;
                MegaNode node = selected.get(0);
                if (megaApi.checkAccess(selected.get(0), ACCESS_FULL).getErrorCode() == API_OK) {
                    showRename = true;
                }

                if (!node.isTakenDown() && megaApi.checkAccess(selected.get(0), ACCESS_OWNER).getErrorCode() == API_OK) {
                    if (node.isExported()) {
                        showRemoveLink = true;
                        showLink = false;
                        showEditLink = true;
                    } else {
                        showRemoveLink = false;
                        showLink = true;
                        showEditLink = false;
                    }
                }
            }
        }

        protected void checkSelectOptions(Menu menu, boolean fromTrash) {
            selected = adapter.getSelectedNodes();

            if (selected.size() == adapter.getItemCount()) {
                showSelectAll = false;
            } else {
                showSelectAll = true;
            }
            menu.findItem(R.id.cab_menu_select_all).setVisible(showSelectAll);
            if (!fromTrash) {
                menu.findItem(R.id.cab_menu_restore_from_rubbish).setVisible(false);
            }
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

    public boolean isMultipleselect() {
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
        if (adapter == null) {
            fastScroller.setVisibility(View.GONE);
        } else {
            if (adapter.getItemCount() < MIN_ITEMS_SCROLLBAR) {
                fastScroller.setVisibility(View.GONE);
            } else {
                fastScroller.setVisibility(View.VISIBLE);
            }
        }
    }

    public ImageView getImageDrag(int position) {
        logDebug("Position: " + position);
        if (adapter != null) {
            if (adapter.getAdapterType() == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
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
            if (adapter.getAdapterType() == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST && mLayoutManager != null) {
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

        if (type == MegaNodeAdapter.ITEM_VIEW_TYPE_GRID) {
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
        if (MimeTypeList.typeForName(node.getName()).isImage()) {
            Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
            intent.putExtra("placeholder", placeholderCount);
            intent.putExtra("position", position);
            intent.putExtra("adapterType", fragmentAdapter);
            intent.putExtra("isFolderLink", false);
            if ((fragmentAdapter == LINKS_ADAPTER && managerActivity.getParentHandleLinks() == INVALID_HANDLE)
                    || megaApi.getParentNode(node).getType() == MegaNode.TYPE_ROOT) {
                intent.putExtra("parentNodeHandle", -1L);
            } else {
                intent.putExtra("parentNodeHandle", megaApi.getParentNode(node).getHandle());
            }

            intent.putExtra("orderGetChildren", getIntentOrder(fragmentAdapter));
            intent.putExtra("screenPosition", screenPosition);
            context.startActivity(intent);
            ((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
            imageDrag = imageView;
        } else if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isAudio()) {
            MegaNode file = node;

            String mimeType = MimeTypeList.typeForName(file.getName()).getType();

            Intent mediaIntent;
            boolean internalIntent;
            boolean opusFile = false;
            if (MimeTypeList.typeForName(file.getName()).isVideoNotSupported() || MimeTypeList.typeForName(file.getName()).isAudioNotSupported()) {
                mediaIntent = new Intent(Intent.ACTION_VIEW);
                internalIntent = false;
                String[] s = file.getName().split("\\.");
                if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) {
                    opusFile = true;
                }
            } else {
                mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
                internalIntent = true;
            }
            mediaIntent.putExtra("position", position);
            mediaIntent.putExtra("placeholder", placeholderCount);
            if ((fragmentAdapter == LINKS_ADAPTER && managerActivity.getParentHandleLinks() == INVALID_HANDLE)
                    || megaApi.getParentNode(node).getType() == MegaNode.TYPE_ROOT) {
                mediaIntent.putExtra("parentNodeHandle", -1L);
            } else {
                mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(node).getHandle());
            }
            mediaIntent.putExtra("orderGetChildren", getIntentOrder(fragmentAdapter));
            mediaIntent.putExtra("adapterType", fragmentAdapter);
            mediaIntent.putExtra("screenPosition", screenPosition);

            mediaIntent.putExtra("FILENAME", file.getName());
            boolean isOnMegaDownloads = false;
            String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
            File f = new File(downloadLocationDefaultPath, file.getName());
            if (f.exists() && (f.length() == file.getSize())) {
                isOnMegaDownloads = true;
            }
            if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))) {
                File mediaFile = new File(localPath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                    logDebug("itemClick:FileProviderOption");
                    Uri mediaFileUri = FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                    if (mediaFileUri == null) {
                        logDebug("itemClick:ERROR:NULLmediaFileUri");
                        managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                    } else {
                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(file.getName()).getType());
                    }
                } else {
                    Uri mediaFileUri = Uri.fromFile(mediaFile);
                    if (mediaFileUri == null) {
                        logError("itemClick:ERROR:NULLmediaFileUri");
                        managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                    } else {
                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(file.getName()).getType());
                    }
                }
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                logDebug("itemClick:localPathNULL");

                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart();
                } else {
                    logWarning("itemClick:ERROR:httpServerAlreadyRunning");
                }

                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);

                if (mi.totalMem > BUFFER_COMP) {
                    logDebug("itemClick:total mem: " + mi.totalMem + " allocate 32 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                } else {
                    logDebug("itemClick:total mem: " + mi.totalMem + " allocate 16 MB");
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                }

                String url = megaApi.httpServerGetLocalLink(file);
                if (url != null) {
                    Uri parsedUri = Uri.parse(url);
                    if (parsedUri != null) {
                        mediaIntent.setDataAndType(parsedUri, mimeType);
                    } else {
                        logError("itemClick:ERROR:httpServerGetLocalLink");
                        managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                    }
                } else {
                    logError("itemClick:ERROR:httpServerGetLocalLink");
                    managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                }
            }
            mediaIntent.putExtra("HANDLE", file.getHandle());
            if (opusFile) {
                mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
            }
            imageDrag = imageView;
            if (internalIntent) {
                context.startActivity(mediaIntent);
            } else {
                logDebug("itemClick:externalIntent");
                if (isIntentAvailable(context, mediaIntent)) {
                    context.startActivity(mediaIntent);
                } else {
                    logWarning("itemClick:noAvailableIntent");
                    managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);

                    ArrayList<Long> handleList = new ArrayList<Long>();
                    handleList.add(node.getHandle());
                    NodeController nC = new NodeController(context);
                    nC.prepareForDownload(handleList, true);
                }
            }
            managerActivity.overridePendingTransition(0, 0);
        } else if (MimeTypeList.typeForName(node.getName()).isURL()) {
            logDebug("Is URL file");
            MegaNode file = node;

            boolean isOnMegaDownloads = false;
            String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
            File f = new File(downloadLocationDefaultPath, file.getName());
            if (f.exists() && (f.length() == file.getSize())) {
                isOnMegaDownloads = true;
            }
            logDebug("isOnMegaDownloads: " + isOnMegaDownloads);
            if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))) {
                File mediaFile = new File(localPath);
                InputStream instream = null;

                try {
                    // open the file for reading
                    instream = new FileInputStream(f.getAbsolutePath());

                    // if file the available for reading
                    if (instream != null) {
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
                        } else {
                            logDebug("Not expected format: Exception on processing url file");
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", f), "text/plain");
                            } else {
                                intent.setDataAndType(Uri.fromFile(f), "text/plain");
                            }
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            if (isIntentAvailable(context, intent)) {
                                startActivity(intent);
                            } else {
                                ArrayList<Long> handleList = new ArrayList<Long>();
                                handleList.add(node.getHandle());
                                NodeController nC = new NodeController(context);
                                nC.prepareForDownload(handleList, true);
                            }
                        }
                    }
                } catch (Exception ex) {

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", f), "text/plain");
                    } else {
                        intent.setDataAndType(Uri.fromFile(f), "text/plain");
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    if (isIntentAvailable(context, intent)) {
                        startActivity(intent);
                    } else {
                        ArrayList<Long> handleList = new ArrayList<Long>();
                        handleList.add(node.getHandle());
                        NodeController nC = new NodeController(context);
                        nC.prepareForDownload(handleList, true);
                    }
                } finally {
                    // close the file.
                    try {
                        instream.close();
                    } catch (IOException e) {
                        logError("EXCEPTION closing InputStream", e);
                    }
                }
            } else {
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                NodeController nC = new NodeController(context);
                nC.prepareForDownload(handleList, true);
            }
        } else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
            logDebug("itemClick:isFile:isPdf");
            MegaNode file = node;

            String mimeType = MimeTypeList.typeForName(file.getName()).getType();

            Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);

            pdfIntent.putExtra("inside", true);
            pdfIntent.putExtra("adapterType", FILE_BROWSER_ADAPTER);
            boolean isOnMegaDownloads = false;
            String localPath = getLocalFile(context, file.getName(), file.getSize(), downloadLocationDefaultPath);
            File f = new File(downloadLocationDefaultPath, file.getName());
            if (f.exists() && (f.length() == file.getSize())) {
                isOnMegaDownloads = true;
            }
            logDebug("isOnMegaDownloads: " + isOnMegaDownloads);
            if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))) {
                File mediaFile = new File(localPath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                    pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                } else {
                    pdfIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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

                String url = megaApi.httpServerGetLocalLink(file);
                pdfIntent.setDataAndType(Uri.parse(url), mimeType);
            }
            pdfIntent.putExtra("HANDLE", file.getHandle());
            pdfIntent.putExtra("screenPosition", screenPosition);
            imageDrag = imageView;
            if (isIntentAvailable(context, pdfIntent)) {
                context.startActivity(pdfIntent);
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();

                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                NodeController nC = new NodeController(context);
                nC.prepareForDownload(handleList, true);
            }
            managerActivity.overridePendingTransition(0, 0);
        } else {
            logDebug("itemClick:isFile:otherOption");
            ArrayList<Long> handleList = new ArrayList<Long>();
            handleList.add(node.getHandle());
            NodeController nC = new NodeController(context);
            nC.prepareForDownload(handleList, true);
        }
    }

    private int getIntentOrder(int fragmentAdapter) {
        switch (fragmentAdapter) {
            case LINKS_ADAPTER:
                return getLinksOrderCloud();

            case INCOMING_SHARES_ADAPTER:
            case OUTGOING_SHARES_ADAPTER:
                if (managerActivity.isFirstNavigationLevel()) {
                    return managerActivity.getOrderOthers();
                }

            default:
                return managerActivity.orderCloud;
        }
    }

    protected int getLinksOrderCloud() {
        int order = managerActivity.orderCloud;

        if (!managerActivity.isFirstNavigationLevel()) {
            return order;
        }

        switch (order) {
            case ORDER_MODIFICATION_ASC:
                return ORDER_CREATION_ASC;

            case ORDER_MODIFICATION_DESC:
                return ORDER_CREATION_DESC;

            default:
                return order;
        }
    }
}
