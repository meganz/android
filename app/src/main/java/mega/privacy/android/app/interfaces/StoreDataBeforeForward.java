package mega.privacy.android.app.interfaces;

import java.util.ArrayList;

import nz.mega.sdk.MegaChatMessage;

public interface StoreDataBeforeForward<T> extends MyChatFilesExisitListener<T> {
    /**
     * store unhandled data if "My Chat Files" folder do not exist
     */
    void storedUnhandledData(ArrayList<MegaChatMessage> messagesSelected, ArrayList<MegaChatMessage> messagesToImport);
}
