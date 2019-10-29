package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;

import static mega.privacy.android.app.utils.LogUtil.*;

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

    private void reDoTheSelectionAfterRotation() {
        logDebug("Reselect items");
        RotatableAdapter adapter = getAdapter();
        if (adapter != null) {
            int folderCount = adapter.getFolderCount();
            logDebug("Folders: " + folderCount);
            int currentPlaceHolderCount = adapter.getPlaceholderCount();
            logDebug("Place holder: " + currentPlaceHolderCount);
            if (selectedItems != null && selectedItems.size() > 0) {
                activateActionMode();
                for (int selectedItem : selectedItems) {
                    if (isList()) {
                        logDebug("selectedItem:" + selectedItem);
                        multipleItemClick(selectedItem);
                    } else {
                        if (selectedItem < folderCount) {
                            logDebug("List folder, selectedItem: " + selectedItem);
                            multipleItemClick(selectedItem);
                        } else {
                            logDebug("File selection, selectedItem: " + selectedItem + "lastPlaceHolderCount:" + lastPlaceHolderCount + ". currentPlaceHolderCount: " + currentPlaceHolderCount);
                            multipleItemClick((selectedItem - lastPlaceHolderCount + currentPlaceHolderCount));
                        }
                    }
                }
            }
            updateActionModeTitle();
        }
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
        if (selectedItems != null) {
            reDoTheSelectionAfterRotation();
            selectedItems = null;
        }
    }

}
