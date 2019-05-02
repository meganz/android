package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.utils.Util;

public abstract class RotatableFragment extends Fragment {

    protected abstract RotatableAdapter getAdapter();

    public abstract void activateActionMode();

    public abstract void multipleItemClick(int position);

    protected abstract void updateActionModeTitle();

    private static void log(String tag, String log) {
        Util.log(tag, log);
    }

    private ArrayList<Integer> selectedItems;

    private int lastPlaceHolderCount;

    private void reDoTheSelectionAfterRotation() {
        String className = this.getClass().getName();
        log(className, "re select the items which are selected before rotation");
        RotatableAdapter adapter = getAdapter();
        if (adapter != null) {
            int folderCount = adapter.getFolderCount();
            log(className, "There are" + folderCount + "folders");
            int currentPlaceHolderCount = adapter.getPlaceholderCount();
            log(className, "There are" + currentPlaceHolderCount + "folder place holder in current screen");
            if (selectedItems != null && selectedItems.size() > 0) {
                activateActionMode();
                for (int selectedItem : selectedItems) {
                    if (((ManagerActivityLollipop)getActivity()).isList) {
                        log(className, "Do the list selection. The selectedItem is " + selectedItem);
                        multipleItemClick(selectedItem);
                    }
                    else {
                        if (selectedItem < folderCount) {
                            log(className, "Do the list folder selection. The selectedItem is " + selectedItem);
                            multipleItemClick(selectedItem);
                        } else {
                            log(className, "Do the list file selection. The selectedItem is " + selectedItem + "the lastPlaceHolderCount is " + lastPlaceHolderCount + ". The currentPlaceHolderCount is " + currentPlaceHolderCount);
                            multipleItemClick((selectedItem - lastPlaceHolderCount + currentPlaceHolderCount));
                        }
                    }
                }
            }
            updateActionModeTitle();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        RotatableAdapter currentAdapter = getAdapter();
        ArrayList<Integer> selectedItems = (ArrayList<Integer>) (currentAdapter.getSelectedItems());
        outState.putSerializable("selectedItems", selectedItems);
        outState.putInt("folderCount", currentAdapter.getFolderCount());
        outState.putInt("lastPlaceHolderCount", currentAdapter.getPlaceholderCount());
        selectedItems = null;
        lastPlaceHolderCount = -1;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null ) {
            selectedItems = (ArrayList<Integer>) savedInstanceState.getSerializable("selectedItems");
            lastPlaceHolderCount = savedInstanceState.getInt("lastPlaceHolderCount") ;
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
