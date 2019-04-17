package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.ImageView;

import java.util.ArrayList;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.utils.Util;

public abstract class RotatableFragment extends Fragment {
    //This attribute is to preserve how many folder place holder in folder grid view
    private int lastPlaceHolderCount;

    protected abstract RotatableAdapter getAdapter();

    public abstract void activateActionMode();

    public abstract void itemClick(int position,int[] screenPosition, ImageView imageView);

    private static void log(String tag, String log) {
        Util.log(tag, log);
    }

    private void reDoTheSelectionAfterRotation() {
        String className = this.getClass().getName();
        log(className, "re select the items which are selected before rotation");
        RotatableAdapter adapter = getAdapter();
        if (adapter != null) {
            ArrayList<Integer> selectedItems = (ArrayList<Integer>) (adapter.getSelectedItems());
            int folderCount = adapter.getFolderCount();
            log(className, "There are" + folderCount + "folders");
            int currentPlaceHolderCount = adapter.getPlaceholderCount();
            log(className, "There are" + currentPlaceHolderCount + "folder place holder in current screen");
            if (selectedItems != null && selectedItems.size() > 0) {
                activateActionMode();
                for (int selectedItem : selectedItems) {
                    if (((ManagerActivityLollipop)getActivity()).isList) {
                        log(className, "Do the list selection. The selectedItem is " + selectedItem);
                        itemClick(selectedItem, null, null);
                    }
                    else {
                        if (selectedItem < folderCount) {
                            log(className, "Do the list folder selection. The selectedItem is " + selectedItem);
                            itemClick((selectedItem), null, null);
                        } else {
                            log(className, "Do the list file selection. The selectedItem is " + selectedItem + "the lastPlaceHolderCount is " + lastPlaceHolderCount + ". The currentPlaceHolderCount is " + currentPlaceHolderCount);
                            itemClick((selectedItem - lastPlaceHolderCount + currentPlaceHolderCount), null, null);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        reDoTheSelectionAfterRotation();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.lastPlaceHolderCount = getAdapter().getPlaceholderCount();
    }
}
