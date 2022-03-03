package mega.privacy.android.app.main.megachat;


import mega.privacy.android.app.contacts.usecase.InviteContactUseCase;
import nz.mega.sdk.MegaChatMessage;

public class AndroidMegaChatMessage {

    final public static int CHAT_ADAPTER_SHOW_TIME = 1;
    final public static int CHAT_ADAPTER_SHOW_NOTHING = 0;
    final public static int CHAT_ADAPTER_SHOW_ALL = 2;

    MegaChatMessage message;
    PendingMessageSingle pendingMessage;
    int infoToShow=-1;
    boolean showAvatar = true;
    boolean uploading = false;

    AndroidMegaRichLinkMessage richLinkMessage = null;
    InviteContactUseCase.ContactLinkResult contactLinkResult;

    public AndroidMegaChatMessage(MegaChatMessage message) {
        this.message = message;
    }

    public AndroidMegaChatMessage(PendingMessageSingle pendingMessage, boolean uploading) {
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

    public PendingMessageSingle getPendingMessage() {
        return pendingMessage;
    }

    public void setPendingMessage(PendingMessageSingle pendingMessage) {
        this.pendingMessage = pendingMessage;
    }

    public boolean isShowAvatar() {
        return showAvatar;
    }

    public void setShowAvatar(boolean showAvatar) {
        this.showAvatar = showAvatar;
    }

    public AndroidMegaRichLinkMessage getRichLinkMessage() {
        return richLinkMessage;
    }

    public void setRichLinkMessage(AndroidMegaRichLinkMessage richLinkMessage) {
        this.richLinkMessage = richLinkMessage;
    }

    public InviteContactUseCase.ContactLinkResult getContactLinkResult() {
        return contactLinkResult;
    }

    public void setContactLinkResult(InviteContactUseCase.ContactLinkResult contactLinkResult) {
        this.contactLinkResult = contactLinkResult;
    }
}
