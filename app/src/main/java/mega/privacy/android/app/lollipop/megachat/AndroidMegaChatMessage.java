package mega.privacy.android.app.lollipop.megachat;


import java.util.ArrayList;

import nz.mega.sdk.MegaChatMessage;

public class AndroidMegaChatMessage {
    MegaChatMessage message;
    int infoToShow=-1;
    ArrayList<String> fingerprints;
    ArrayList<String> names;
    boolean uploading;
    long uploadTimestamp;

    public AndroidMegaChatMessage(MegaChatMessage message) {
        this.message = message;
    }

    public AndroidMegaChatMessage(ArrayList<String> fingerprints, ArrayList<String> names, boolean uploading, long uploadTimestamp) {
        this.fingerprints = fingerprints;
        this.fingerprints = fingerprints;
        this.uploading = uploading;
        this.uploadTimestamp = uploadTimestamp;
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

    public ArrayList<String> getFingerPrints() {
        return fingerprints;
    }

    public void setFingerprints(ArrayList<String> fingerprints) {
        this.fingerprints = fingerprints;
    }

    public ArrayList<String> getNames() {
        return fingerprints;
    }

    public void setNames(ArrayList<String> fingerprints) {
        this.fingerprints = fingerprints;
    }

    public void addFingerprintAndName (String path, String name){
        fingerprints.add(path);
        fingerprints.add(name);
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public boolean isUploading() {
        return uploading;
    }

    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }
}
