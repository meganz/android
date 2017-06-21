package mega.privacy.android.app.lollipop.megachat;


import nz.mega.sdk.MegaChatMessage;

public class AndroidMegaChatMessage {
    MegaChatMessage message;
    int infoToShow=-1;
    String path;
    String name;
    boolean uploading;

    public AndroidMegaChatMessage(MegaChatMessage message) {
        this.message = message;
    }

    public AndroidMegaChatMessage(String path, boolean uploading)
    {
        this.path = path;
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


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isUploading() {
        return uploading;
    }

    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }
}
