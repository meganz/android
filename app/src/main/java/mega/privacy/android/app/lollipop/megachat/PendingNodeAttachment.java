package mega.privacy.android.app.lollipop.megachat;


public class PendingNodeAttachment {

    String filePath;
    long nodeHandle=-1;
    String fingerprint;
    String name;

    public PendingNodeAttachment(String filePath, String fingerprint, String name) {
        this.filePath = filePath;
        this.fingerprint = fingerprint;
        this.name = name;
        this.nodeHandle = -1;
    }

    public PendingNodeAttachment(String filePath, String fingerprint, String name, long nodeHandle) {
        this.filePath = filePath;
        this.fingerprint = fingerprint;
        this.name = name;
        this.nodeHandle = nodeHandle;
    }

    public PendingNodeAttachment(String filePath) {
        this.filePath = filePath;
        this.nodeHandle = -1;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getNodeHandle() {
        return nodeHandle;
    }

    public void setNodeHandle(long nodeHandle) {
        this.nodeHandle = nodeHandle;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
