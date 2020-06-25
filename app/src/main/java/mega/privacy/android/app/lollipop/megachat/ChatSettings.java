package mega.privacy.android.app.lollipop.megachat;

import static mega.privacy.android.app.utils.Constants.*;

public class ChatSettings {

    String notificationsEnabled = "";
    String notificationsSound = "";
    String vibrationEnabled = "";
    String sendOriginalAttachments = "";

    public ChatSettings(String notificationsEnabled, String notificationsSound, String vibrationEnabled, String sendOriginalAttachments) {
        this.notificationsEnabled = notificationsEnabled;
        this.notificationsSound = notificationsSound;
        this.vibrationEnabled = vibrationEnabled;
        this.sendOriginalAttachments = sendOriginalAttachments;
    }

    public ChatSettings() {
        this.notificationsEnabled = NOTIFICATIONS_ENABLED;
        this.notificationsSound = "";
        this.vibrationEnabled = true+"";
        this.sendOriginalAttachments = false+"";
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
