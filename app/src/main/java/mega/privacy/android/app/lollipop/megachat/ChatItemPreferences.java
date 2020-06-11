package mega.privacy.android.app.lollipop.megachat;


public class ChatItemPreferences {

    String chatHandle = "";
    String typeMuteNotifications = "";
    String writtenText = "";

    public ChatItemPreferences(String chatHandle, String typeMuteNotifications, String writtenText) {
        this.chatHandle = chatHandle;
        this.typeMuteNotifications = typeMuteNotifications;
        this.writtenText = writtenText;
    }

    public String getChatHandle() {
        return chatHandle;
    }

    public void setChatHandle(String chatHandle) {
        this.chatHandle = chatHandle;
    }

    public String getNotificationsEnabled() {
        return typeMuteNotifications;
    }

    public void setNotificationsEnabled(String typeMuteNotifications) {
        this.typeMuteNotifications = typeMuteNotifications;
    }

    public String getWrittenText() {
        return writtenText;
    }

    public void setWrittenText(String writtenText) {
        this.writtenText = writtenText;
    }
}
