package mega.privacy.android.app.lollipop.megachat;


public class ChatItemPreferences {

    String chatHandle = "";
    String notificationsEnabled = "";
    String ringtone= "";
    String notificationsSound = "";
    String writtenText = "";
    String lastBeep = "";

    public ChatItemPreferences(String chatHandle, String notificationsEnabled, String ringtone, String notificationsSound, String writtenText, String lastBeep) {
        this.chatHandle = chatHandle;
        this.notificationsEnabled = notificationsEnabled;
        this.notificationsSound = notificationsSound;
        this.ringtone = ringtone;
        this.writtenText = writtenText;
        this.lastBeep = lastBeep;
    }

    public ChatItemPreferences(String chatHandle, String notificationsEnabled, String ringtone, String notificationsSound) {
        this.chatHandle = chatHandle;
        this.notificationsEnabled = notificationsEnabled;
        this.notificationsSound = notificationsSound;
        this.ringtone = ringtone;
        this.writtenText = "";
        this.lastBeep = "";
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

    public String getWrittenText() {
        return writtenText;
    }

    public void setWrittenText(String writtenText) {
        this.writtenText = writtenText;
    }
}
