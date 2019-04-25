package mega.privacy.android.app.lollipop.megachat;

public class RemovedMessage {
    long msgId;
    long msgTempId;
    public RemovedMessage(long msgTempId, long msgId) {
        this.msgTempId = msgTempId;
        this.msgId = msgId;

    }
    public long getMsgId() {
        return msgId;
    }
    public long getMsgTempId() {
        return msgTempId;
    }

}
