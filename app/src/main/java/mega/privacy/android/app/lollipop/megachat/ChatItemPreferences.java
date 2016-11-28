package mega.privacy.android.app.lollipop.megachat;


public class ChatItemPreferences {

    String chatHandle = "";
    String notificationsEnabled = "";
    String ringtone= "";
    String notificationsSound = "";

    public ChatItemPreferences(String chatHandle, String notificationsEnabled, String ringtone, String notificationsSound) {
        this.chatHandle = chatHandle;
        this.notificationsEnabled = notificationsEnabled;
        this.notificationsSound = notificationsSound;
        this.ringtone = ringtone;
    }

    public String getChatHandle() {
        return chatHandle;
    }

    public void setChatHandle(String chatHandle) {
        this.chatHandle = chatHandle;
    }

    public String getNotificationsSound() {
        return notificationsSound;
    }

    public void setNotificationsSound(String notificationsSound) {
        this.notificationsSound = notificationsSound;
    }

    public String getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(String notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getRingtone() {
        return ringtone;
    }

    public void setRingtone(String ringtone) {
        this.ringtone = ringtone;
    }
}
