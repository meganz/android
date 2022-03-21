package mega.privacy.android.app.main;

import static mega.privacy.android.app.main.FileExplorerActivity.EXTRA_SHARE_INFOS;

import android.app.Activity;
import android.content.Intent;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mega.privacy.android.app.ShareInfo;

/**
 * ViewModel class responsible for preparing and managing the data for FileExplorerActivity
 */
public class FileExplorerActivityViewModel extends ViewModel {

    public MutableLiveData<List<ShareInfo>> info = new MutableLiveData<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Get the ShareInfo list
     *
     * @param activity Current activity
     * @param intent The intent that started the current activity
     */
    public void ownFilePrepareTask(Activity activity, Intent intent) {
        final Intent i = intent;
        if (!executor.isShutdown()) {
            executor.submit(() -> {
                List<ShareInfo> shareInfos = (List<ShareInfo>) i.getSerializableExtra(EXTRA_SHARE_INFOS);
                if (shareInfos == null) {
                    shareInfos = ShareInfo.processIntent(i, activity);
                }
                info.postValue(shareInfos);
            });
        }
    }

    /**
     * Shutdown the executor service
     */
    public void shutdownExecutorService(){
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
