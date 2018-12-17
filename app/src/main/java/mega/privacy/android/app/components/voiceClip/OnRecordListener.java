package mega.privacy.android.app.components.voiceClip;

public interface OnRecordListener {
    void onStart();
    void onCancel();
    void onFinish(long recordTime);
    void onLessThanSecond();
}