package mega.privacy.android.app.lollipop.megachat;


public class ChatItemPreferences {

    String chatHandle = "";
    String writtenText = "";

    public ChatItemPreferences(String chatHandle, String writtenText) {
        this.chatHandle = chatHandle;
        this.writtenText = writtenText;
    }

    public String getChatHandle() {
        return chatHandle;
    }

    public void setChatHandle(String chatHandle) {
        this.chatHandle = chatHandle;
    }

    public String getWrittenText() {
        return writtenText;
    }

    public void setWrittenText(String writtenText) {
        this.writtenText = writtenText;
    }
}
