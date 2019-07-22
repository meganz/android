package mega.privacy.android.app.lollipop.megachat;

public class PendingMessageSingle {

    public static int STATE_PREPARING = 0;
    public static int STATE_PREPARING_FROM_EXPLORER = 1;
    public static int STATE_UPLOADING = 2;
    public static int STATE_ATTACHING = 3;
    public static int STATE_SENT = 20;
    //Error negative figures
    public static int STATE_ERROR_UPLOADING = -1;
    public static int STATE_ERROR_ATTACHING = -2;

    long id;
    long chatId;

    int type;
    long uploadTimestamp;
    int state = 0;
    long tempIdKarere;
    String videoDownSampled;
    String filePath;
    long nodeHandle=-1;
    String fingerprint;
    String name;
    int transferTag = -1;

    public PendingMessageSingle() {
    }

    public PendingMessageSingle(long chatId, long uploadTimestamp, String filePath, String fingerprint, String name){

        this.chatId = chatId;
        this.uploadTimestamp = uploadTimestamp;
        this.state = PendingMessageSingle.STATE_PREPARING;
        this.filePath = filePath;
        this.fingerprint = fingerprint;
        this.name = name;

    }

    public PendingMessageSingle(long id, long chatId, long uploadTimestamp, long tempIdKarere, String filePath, String fingerprint, String name, long nodeHandle, int transferTag, int state) {
        this.chatId = chatId;
        this.id = id;
        this.uploadTimestamp = uploadTimestamp;
        this.state = state;
        this.tempIdKarere = tempIdKarere;
        this.filePath = filePath;
        this.nodeHandle = nodeHandle;
        this.fingerprint = fingerprint;
        this.name = name;
        this.transferTag = transferTag;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }


    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getTempIdKarere() {
        return tempIdKarere;
    }

    public void setTempIdKarere(long tempIdKarere) {
        this.tempIdKarere = tempIdKarere;
    }

    public String getVideoDownSampled() {
        return videoDownSampled;
    }

    public void setVideoDownSampled(String videoDownSampled) {
        this.videoDownSampled = videoDownSampled;
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

    public int getTransferTag() {
        return transferTag;
    }

    public void setTransferTag(int transferTag) {
        this.transferTag = transferTag;
    }
}
