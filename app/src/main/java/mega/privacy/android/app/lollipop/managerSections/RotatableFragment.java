package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;

import java.util.ArrayList;

import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;

public abstract class RotatableFragment extends BaseFragment {

    private final static String UNHANDLED_ITEM = "unHandledItem";
    private final static String SELECTED_ITEMS = "selectedItems";
    private final static String LAST_PLACE_HOLDER_COUNT = "lastPlaceHolderCount";

    protected abstract RotatableAdapter getAdapter();

    public abstract void activateActionMode();

    public abstract void multipleItemClick(int position);

    /** Reselect the unhandled item after rotation
     * @param position the index of unhandled item
     */
    public abstract void reselectUnHandledSingleItem(int position);

    protected abstract void updateActionModeTitle();

    private ArrayList<Integer> selectedItems;

    private int unHandledItem = -1;

    private int lastPlaceHolderCount;

    private boolean waitingForSearchedNodes;

    protected void reDoTheSelectionAfterRotation() {
        logDebug("Reselect items");
        setWaitingForSearchedNodes(false);

        if (selectedItems == null) return;

        RotatableAdapter adapter = getAdapter();
        if (adapter == null) return;

        if (selectedItems.size() > 0) {
            activateActionMode();

            for (int selectedItem : selectedItems) {
                multipleItemClick(transferPosition(selectedItem, adapter));
            }
        }

        updateActionModeTitle();
    }

    protected void reSelectUnhandledItem() {
        if (unHandledItem == -1) {
            return;
        }
        RotatableAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        reselectUnHandledSingleItem(transferPosition(unHandledItem, adapter));
        unHandledItem = -1;
    }

    /**
     * @param originalPosition original position before rotation
     * @param adapter the adapter where rotation happens
     * @return the list position after rotation of adapter
     */
    private int transferPosition(int originalPosition, RotatableAdapter adapter) {
        int position;

        int folderCount = adapter.getFolderCount();

        if (isList() || folderCount == 0 || originalPosition < folderCount) {
            position = originalPosition;
        } else if (isScreenInPortrait(getContext())) {
            position = originalPosition - (lastPlaceHolderCount - adapter.getPlaceholderCount());
        } else {
            position = originalPosition + (adapter.getPlaceholderCount() - lastPlaceHolderCount);
        }
        return position;
    }

    private boolean isList() {
        if (getActivity() instanceof ManagerActivityLollipop) {
            return ((ManagerActivityLollipop) getActivity()).isList();
        } else if (getActivity() instanceof FileExplorerActivityLollipop) {
            return ((FileExplorerActivityLollipop) getActivity()).isList();
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        RotatableAdapter currentAdapter = getAdapter();
        if(currentAdapter != null){
            ArrayList<Integer> selectedItems = (ArrayList<Integer>) (currentAdapter.getSelectedItems());
            outState.putSerializable(SELECTED_ITEMS, selectedItems);
            outState.putInt(LAST_PLACE_HOLDER_COUNT, currentAdapter.getPlaceholderCount());
            outState.putInt(UNHANDLED_ITEM, currentAdapter.getUnhandledItem());
            lastPlaceHolderCount = -1;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            selectedItems = (ArrayList<Integer>) savedInstanceState.getSerializable(SELECTED_ITEMS);
            unHandledItem = savedInstanceState.getInt(UNHANDLED_ITEM, -1);
            lastPlaceHolderCount = savedInstanceState.getInt(LAST_PLACE_HOLDER_COUNT, -1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isWaitingForSearchedNodes()) {
            reDoTheSelectionAfterRotation();
            selectedItems = null;
            reSelectUnhandledItem();
            unHandledItem = -1;
        }
    }

    public boolean isWaitingForSearchedNodes() {
        return waitingForSearchedNodes;
    }

    public void setWaitingForSearchedNodes(boolean waitingForSearchedNodes) {
        this.waitingForSearchedNodes = waitingForSearchedNodes;
    }
}
