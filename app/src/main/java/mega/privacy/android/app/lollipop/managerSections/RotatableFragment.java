package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;

public abstract class RotatableFragment extends Fragment {

    private final static String SELECTED_ITEMS = "selectedItems";
    private final static String FOLDER_COUNT = "folderCount";
    private final static String LAST_PLACE_HOLDER_COUNT = "lastPlaceHolderCount";
    protected abstract RotatableAdapter getAdapter();

    public abstract void activateActionMode();

    public abstract void multipleItemClick(int position);

    protected abstract void updateActionModeTitle();

    private ArrayList<Integer> selectedItems;

    private int lastPlaceHolderCount;

    private boolean waitingForSearchedNodes;

    public void reDoTheSelectionAfterRotation() {
        logDebug("Reselect items");
        setWaitingForSearchedNodes(false);

        if (selectedItems == null) return;

        RotatableAdapter adapter = getAdapter();
        if (adapter == null) return;

        if (selectedItems.size() > 0) {
            activateActionMode();

            for (int selectedItem : selectedItems) {
                int position;

                if (isList() || adapter.getFolderCount() == 0) {
                    position = selectedItem;
                } else if (isScreenInPortrait(getContext())){
                    position = selectedItem - (lastPlaceHolderCount - adapter.getPlaceholderCount());
                } else {
                    position = selectedItem + (adapter.getPlaceholderCount() - lastPlaceHolderCount);
                }

                multipleItemClick(position);
            }
        }

        updateActionModeTitle();
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
            outState.putInt(FOLDER_COUNT, currentAdapter.getFolderCount());
            outState.putInt(LAST_PLACE_HOLDER_COUNT, currentAdapter.getPlaceholderCount());
            lastPlaceHolderCount = -1;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            selectedItems = (ArrayList<Integer>) savedInstanceState.getSerializable(SELECTED_ITEMS);
            lastPlaceHolderCount = savedInstanceState.getInt(LAST_PLACE_HOLDER_COUNT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isWaitingForSearchedNodes()) {
            reDoTheSelectionAfterRotation();
            selectedItems = null;
        }
    }

    public boolean isWaitingForSearchedNodes() {
        return waitingForSearchedNodes;
    }

    public void setWaitingForSearchedNodes(boolean waitingForSearchedNodes) {
        this.waitingForSearchedNodes = waitingForSearchedNodes;
    }
}
