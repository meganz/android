package mega.privacy.android.app.lollipop.megachat;

public class ChatSettings {

    String enabled = "";
    String notificationsEnabled = "";
    String notificationsSound = "";
    String vibrationEnabled = "";
    String sendOriginalAttachments = "";

    public ChatSettings(String enabled, String notificationsEnabled, String notificationsSound, String vibrationEnabled, String sendOriginalAttachments) {
        this.enabled = enabled;
        this.notificationsEnabled = notificationsEnabled;
        this.notificationsSound = notificationsSound;
        this.vibrationEnabled = vibrationEnabled;
        this.sendOriginalAttachments = sendOriginalAttachments;
    }

    public ChatSettings() {
        this.enabled = true+"";
        this.notificationsEnabled = true+"";
        this.notificationsSound = "";
        this.vibrationEnabled = true+"";
        this.sendOriginalAttachments = false+"";
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

    public String getSendOriginalAttachments() {
        return sendOriginalAttachments;
    }

    public void setSendOriginalAttachments(String sendOriginalAttachments) {
        this.sendOriginalAttachments = sendOriginalAttachments;
    }
}
