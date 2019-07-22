package mega.privacy.android.app.interfaces;

public interface MyChatFilesExisitListener<T> {
    /**
     * store unhandled data if "My Chat Files" folder do not exist
     */
    void storedUnhandledData(T preservedData);
    /**
     * process stored data after the folder is created
     */
    void handleStoredData();
}
