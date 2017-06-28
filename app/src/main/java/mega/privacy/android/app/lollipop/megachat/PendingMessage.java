package mega.privacy.android.app.lollipop.megachat;

import java.util.ArrayList;

public class PendingMessage {
    long chatId;
    ArrayList<String> filePaths;
    ArrayList<Long> nodeHandles;
    long id;
    ArrayList<String> fingerprints;
    ArrayList<String> names;
    long uploadTimestamp;

    public PendingMessage(long id, long chatId, ArrayList<String> filePaths) {
        this.id = id;
        this.chatId = chatId;
        this.filePaths = filePaths;
        this.nodeHandles = new ArrayList<>();
    }

    public PendingMessage(long id, long chatId, ArrayList<String> filePaths, ArrayList<String> names, ArrayList<String> fingerprints, long uploadTimestamp) {
        this.chatId = chatId;
        this.filePaths = filePaths;
        this.id = id;
        this.fingerprints = fingerprints;
        this.names = names;
        this.uploadTimestamp = uploadTimestamp;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public ArrayList<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(ArrayList<String> filePaths) {
        this.filePaths = filePaths;
    }

    public ArrayList<Long> getNodeHandles() {
        return nodeHandles;
    }

    public void setNodeHandles(ArrayList<Long> nodeHandles) {
        this.nodeHandles = nodeHandles;
    }

    public void addNodeHandle(long handle){
        nodeHandles.add(handle);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ArrayList<String> getFingerPrints() {
        return fingerprints;
    }

    public void setFingerprints(ArrayList<String> fingerprints) {
        this.fingerprints = fingerprints;
    }

    public ArrayList<String> getNames() {
        return names;
    }

    public void setNames(ArrayList<String> fingerprints) {
        this.names = names;
    }

    public void addFingerprintAndName (String fingerprint, String name){
        fingerprints.add(fingerprint);
        names.add(name);
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }
}
