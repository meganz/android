package mega.privacy.android.app.lollipop.megachat;


import nz.mega.sdk.MegaChatMessage;

public class AndroidMegaChatMessage {
    MegaChatMessage message;
    int infoToShow=-1;

    public AndroidMegaChatMessage(MegaChatMessage message) {
        this.message = message;
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

}
