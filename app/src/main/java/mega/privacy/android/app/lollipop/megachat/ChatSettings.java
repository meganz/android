package mega.privacy.android.app.lollipop.megachat;

public class ChatSettings {

    String notificationsSound;
    String vibrationEnabled;
    String sendOriginalAttachments;

    public ChatSettings(String notificationsSound, String vibrationEnabled, String sendOriginalAttachments) {
        this.notificationsSound = notificationsSound;
        this.vibrationEnabled = vibrationEnabled;
        this.sendOriginalAttachments = sendOriginalAttachments;
    }

    public ChatSettings() {
        this.notificationsSound = "";
        this.vibrationEnabled = true+"";
        this.sendOriginalAttachments = false+"";
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
