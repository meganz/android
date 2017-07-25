package mega.privacy.android.app.lollipop.megachat;


import java.util.ArrayList;

import nz.mega.sdk.MegaChatMessage;

public class AndroidMegaChatMessage {
    MegaChatMessage message;
    PendingMessage pendingMessage;
    int infoToShow=-1;
    boolean uploading = false;

    public AndroidMegaChatMessage(MegaChatMessage message) {
        this.message = message;
    }

    public AndroidMegaChatMessage(PendingMessage pendingMessage, boolean uploading) {
        this.pendingMessage = pendingMessage;
        this.uploading = uploading;
    }

    public int getInfoToShow() {
        return infoToShow;
    }

    public void setInfoToShow(int infoToShow) {
        this.infoToShow = infoToShow;
    }

    public MegaChatMessage getMessage() {
        return message;
    }

    public void setMessage(MegaChatMessage message) {
        this.message = message;
    }

    public boolean isUploading() {
        return uploading;
    }

    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }

    public PendingMessage getPendingMessage() {
        return pendingMessage;
    }

    public void setPendingMessage(PendingMessage pendingMessage) {
        this.pendingMessage = pendingMessage;
    }
}
