package mega.privacy.android.app.utils.conversion;

public interface ProgressUpdateCallback {

    void updateProgress(int progress,String currentIndexString);

    void finish(String currentIndexString);
}
