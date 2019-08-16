package mega.privacy.android.app.utils.conversion;

import mega.privacy.android.app.jobservices.SyncRecord;

public interface VideoCompressionCallback {

    void onCompressUpdateProgress(int progress);
    
    void onCompressSuccessful(SyncRecord record);
    
    void onCompressFailed(SyncRecord record);

    void onCompressFinished(String currentIndexString);

    void onCompressNotSupported(SyncRecord record);

    void onInsufficientSpace();
}
