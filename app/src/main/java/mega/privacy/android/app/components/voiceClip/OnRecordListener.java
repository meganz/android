package mega.privacy.android.app.components.voiceClip;

public interface OnRecordListener {
    void onStart();

    void onCancel();

    void onLock();

    void onFinish(long recordTime);

    void onLessThanSecond();

    void finishedSound();

    void changeTimer(CharSequence time);
}