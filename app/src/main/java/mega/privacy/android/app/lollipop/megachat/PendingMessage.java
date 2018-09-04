package mega.privacy.android.app.lollipop.megachat;

public class PendingMessage {

    public static int STATE_SENDING = 0;
    public static int STATE_SENT = 1;
    public static int STATE_ERROR = -1;

    long chatId;
//    ArrayList<PendingNodeAttachment> nodeAttachments = new ArrayList<>();
    long id;
    long uploadTimestamp;
    int state;
    long tempIdKarere;
    String videoDownSampled;
    PendingNodeAttachment nodeAttachment;

    public PendingMessage(long id, long chatId, String filePath, int state) {
        this.id = id;
        this.chatId = chatId;
        this.state = state;

        PendingNodeAttachment nodeAttachment = new PendingNodeAttachment(filePath);
        this.nodeAttachment = nodeAttachment;
    }

    public PendingMessage(long id, long chatId, PendingNodeAttachment nodeAttachment, long uploadTimestamp, int state) {
        this.chatId = chatId;
        this.nodeAttachment = nodeAttachment;
        this.id = id;
        this.uploadTimestamp = uploadTimestamp;
        this.state = state;
    }

    public PendingMessage(long id, long chatId, long uploadTimestamp, long idKarere, int state) {
        this.chatId = chatId;
        this.id = id;
        this.uploadTimestamp = uploadTimestamp;
        this.tempIdKarere = idKarere;
        this.state = state;
    }

    public PendingMessage(long id, long chatId, long uploadTimestamp) {
        this.chatId = chatId;
        this.id = id;
        this.uploadTimestamp = uploadTimestamp;
    }

    public PendingNodeAttachment getNodeAttachment() {
        return nodeAttachment;
    }

    public void setNodeAttachment(PendingNodeAttachment nodeAttachment) {
        this.nodeAttachment = nodeAttachment;
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

    public long getNodeHandle(){
        return nodeAttachment.getNodeHandle();
    }

    public String getFilePath(){
        return nodeAttachment.getFilePath();
    }

    public String getName(){
        return nodeAttachment.getName();
    }

    public String getVideoDownSampled() {
        return videoDownSampled;
    }

    public void setVideoDownSampled(String videoDownSampled) {
        this.videoDownSampled = videoDownSampled;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
