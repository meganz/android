package mega.privacy.android.app.lollipop.megachat;

public class ChatSettings {

    String enabled = "";
    String notificationsEnabled = "";
    String notificationsSound = "";
    String vibrationEnabled = "";
    String chatStatus = "";

    public ChatSettings(String enabled, String notificationsEnabled, String notificationsSound, String vibrationEnabled, String chatStatus) {
        this.enabled = enabled;
        this.notificationsEnabled = notificationsEnabled;
        this.notificationsSound = notificationsSound;
        this.vibrationEnabled = vibrationEnabled;
        this.chatStatus = chatStatus;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(String notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getNotificationsSound() {
        return notificationsSound;
    }

    public void setNotificationsSound(String notificationsSound) {
        this.notificationsSound = notificationsSound;
    }

    public String getVibrationEnabled() {
        return vibrationEnabled;
    }

    public void setVibrationEnabled(String vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    public String getChatStatus() {
        return chatStatus;
    }

    public void setChatStatus(String chatStatus) {
        this.chatStatus = chatStatus;
    }
}
