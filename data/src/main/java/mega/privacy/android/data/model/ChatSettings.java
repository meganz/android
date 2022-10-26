package mega.privacy.android.data.model;

public class ChatSettings {

    private String notificationsSound;
    private String vibrationEnabled;
    private String videoQuality;

    public ChatSettings(String notificationsSound, String vibrationEnabled, String videoQuality) {
        this.notificationsSound = notificationsSound;
        this.vibrationEnabled = vibrationEnabled;
        this.videoQuality = videoQuality;
    }

    public ChatSettings() {
        this.notificationsSound = "";
        this.vibrationEnabled = true+"";
        this.videoQuality = false+"";
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

    public String getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(String videoQuality) {
        this.videoQuality = videoQuality;
    }
}
