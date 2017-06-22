package mega.privacy.android.app.lollipop.megachat;

public class PendingMessage {
    long chatId;
    String filePath;

    public PendingMessage(String filePath, long chatId) {
        this.filePath = filePath;
        this.chatId = chatId;
    }

    public String getHandle() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }
}
