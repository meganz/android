package mega.privacy.android.app.interfaces;

public interface MyChatFilesExisitListener<T> {
    void storedUnhandledData(T preservedData);
    void handleStoredData();
}
