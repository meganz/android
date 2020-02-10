package mega.privacy.android.app.fragments;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.NewHeaderItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.managerSections.RotatableFragment;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;

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

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.file_browser_action, menu);
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) context).hideFabButton();
                ((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
                ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
            }
            checkScroll();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            clearSelections();
            adapter.setMultipleSelect(false);
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) context).showFabButton();
                ((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
                ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ZERO_DELAY);
            }
            checkScroll();
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
                ((ManagerActivityLollipop) context).changeActionBarElevation(true);
            } else {
                ((ManagerActivityLollipop) context).changeActionBarElevation(false);
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
}
