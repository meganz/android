package mega.privacy.android.app.lollipop.adapters;

import java.util.List;

public interface RotatableAdapter {
    /**
     * @return the selected item before rotation change
     */
    List<Integer> getSelectedItems();

    /**
     * @return the number of folder in adapter
     */
    int getFolderCount();

    /**
     * @return the number of place holder in adapter in folder section
     */
    int getPlaceholderCount();

    /**
     * @return the index of unhandled node before the rotation recreate
     */
    int getUnhandledItem();
}
