package mega.privacy.android.app.interfaces;

import java.util.ArrayList;

public interface MyChatFilesExisitListener<T extends Object> {
    void storedUnhandledData(ArrayList<T> preservedData);
    void handleStoredData();
}
