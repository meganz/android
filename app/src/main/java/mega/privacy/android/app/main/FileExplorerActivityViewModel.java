package mega.privacy.android.app.main;

import static android.text.TextUtils.isEmpty;
import static android.webkit.URLUtil.isHttpUrl;
import static android.webkit.URLUtil.isHttpsUrl;
import static mega.privacy.android.app.main.FileExplorerActivity.EXTRA_SHARE_INFOS;
import static mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.domain.entity.ShareTextInfo;

/**
 * ViewModel class responsible for preparing and managing the data for FileExplorerActivity
 */
public class FileExplorerActivityViewModel extends ViewModel {

    public MutableLiveData<List<ShareInfo>> filesInfo = new MutableLiveData<>();
    public MutableLiveData<ShareTextInfo> textInfo = new MutableLiveData<>();
    public MutableLiveData<HashMap<String, String>> fileNames = new MutableLiveData<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public boolean isImportingText;

    /**
     * Get the ShareInfo list
     *
     * @param activity Current activity
     * @param intent   The intent that started the current activity
     */
    public void ownFilePrepareTask(Activity activity, Intent intent) {
        final Intent i = intent;
        if (!executor.isShutdown()) {
            executor.submit(() -> {
                HashMap<String, String> names = new HashMap<>();
                initializeImportingText(intent);

                if (isImportingText) {
                    String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                    boolean isUrl = sharedText != null && (isHttpUrl(sharedText) || isHttpsUrl(sharedText));
                    String sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                    String sharedEmail = intent.getStringExtra(Intent.EXTRA_EMAIL);
                    String subject = sharedSubject != null ? sharedSubject : "";
                    names.put(subject, subject);
                    String fileContent = buildFileContent(sharedText, sharedSubject, sharedEmail, isUrl);
                    String messageContent = buildMessageContent(sharedText, sharedSubject, sharedEmail);
                    fileNames.postValue(names);
                    textInfo.postValue(new ShareTextInfo(isUrl, subject, fileContent, messageContent));
                } else {
                    List<ShareInfo> shareInfo = (List<ShareInfo>) i.getSerializableExtra(EXTRA_SHARE_INFOS);
                    if (shareInfo == null) {
                        shareInfo = ShareInfo.processIntent(i, activity);
                    }

                    if (shareInfo != null) {
                        for (ShareInfo info : shareInfo) {
                            String name = info.getTitle();
                            if (isEmpty(name)) {
                                name = info.getOriginalFileName();
                            }

                            names.put(name, name);
                        }
                    }

                    fileNames.postValue(names);
                    filesInfo.postValue(shareInfo);
                }
            });
        }
    }

    /**
     * Builds file content from the shared text.
     *
     * @param text    Shared text.
     * @param subject Shared subject.
     * @param email   Shared email.
     * @param isUrl   True if it is sharing a link, false otherwise.
     * @return The file content.
     */
    private String buildFileContent(String text, String subject, String email, boolean isUrl) {
        StringBuilder builder =  new StringBuilder();

        if (isUrl && text != null) {
            builder.append("[InternetShortcut]\n").append("URL=").append(text).append("\n\n");

            if (subject != null) {
                builder.append(getString(R.string.new_file_subject_when_uploading))
                        .append(": ").append(subject).append("\n");
            }

            if (email != null) {
                builder.append(getString(R.string.new_file_email_when_uploading))
                        .append(": ").append(email);
            }
        } else {
            return buildMessageContent(text, subject, email);
        }

        return builder.toString();
    }

    /**
     * Builds message content from the shared text.
     *
     * @param text    Shared text.
     * @param subject Shared subject.
     * @param email   Shared email.
     * @return The message content.
     */
    private String buildMessageContent(String text, String subject, String email) {
        StringBuilder builder =  new StringBuilder();

        if (subject != null) {
            builder.append(getString(R.string.new_file_subject_when_uploading))
                    .append(": ").append(subject).append("\n\n");
        }

        if (email != null) {
            builder.append(getString(R.string.new_file_email_when_uploading))
                    .append(": ").append(email).append("\n\n");
        }

        if (text != null) {
            builder.append(text);
        }

        return builder.toString();
    }

    /**
     * Shutdown the executor service
     */
    public void shutdownExecutorService() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    /**
     * Checks if it is importing a text instead of files.
     * This is true if the action of the intent is ACTION_SEND, the type of the intent
     * is TYPE_TEXT_PLAIN and the intent does not contain EXTRA_STREAM extras.
     *
     * @param intent Intent to get the info.
     */
    private void initializeImportingText(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())
                && TYPE_TEXT_PLAIN.equals(intent.getType())) {
            Bundle extras = intent.getExtras();
            isImportingText = extras != null && !extras.containsKey(Intent.EXTRA_STREAM);
        } else {
            isImportingText = false;
        }
    }

    /**
     * Builds the final content text to share as chat message.
     *
     * @return Text to share as chat message.
     */
    public String getMessageToShare() {
        ShareTextInfo info = textInfo.getValue();
        HashMap<String, String> names = fileNames.getValue();

        if (info != null) {
            String typedName = names != null
                    ? fileNames.getValue().get(info.getSubject())
                    : info.getSubject();

            return typedName + "\n\n" + info.getMessageContent();
        }

        return null;
    }
}
