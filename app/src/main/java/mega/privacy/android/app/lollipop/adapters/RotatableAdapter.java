package mega.privacy.android.app.lollipop.adapters;

import java.util.List;

public interface RotatableAdapter {
    List<Integer> getSelectedItems();

    int getFolderCount();

    int getPlaceholderCount();
}
