package mega.privacy.android.app.main

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.domain.entity.ShareTextInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import javax.inject.Inject

/**
 * ViewModel class responsible for preparing and managing the data for FileExplorerActivity.
 *
 * @property storageState    [StorageState]
 * @property isImportingText True if it is importing text, false if it is importing files.
 * @property fileNames       File names.
 */
@HiltViewModel
class FileExplorerActivityViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
) : ViewModel() {

    private val filesInfo = MutableLiveData<List<ShareInfo>>()
    private val textInfo = MutableLiveData<ShareTextInfo>()
    val fileNames = MutableLiveData<HashMap<String, String>>()

    var isImportingText = false

    val storageState: StorageState
        get() = monitorStorageStateEvent.getState()

    private var dataAlreadyRequested = false

    /**
     * Notifies observers about filesInfo changes.
     */
    fun getFilesInfo(): LiveData<List<ShareInfo>> = filesInfo

    /**
     * Notifies observers about textInfo updates.
     */
    fun getTextInfo(): LiveData<ShareTextInfo> = textInfo

    /**
     * Gets [ShareTextInfo].
     */
    fun getTextInfoContent() = textInfo.value

    /**
     * Get the ShareInfo list
     *
     * @param activity Current activity
     * @param intent   The intent that started the current activity
     */
    fun ownFilePrepareTask(activity: Activity?, intent: Intent) {
        if (dataAlreadyRequested) return

        viewModelScope.launch(ioDispatcher) {
            dataAlreadyRequested = true
            val names = HashMap<String, String>()
            initializeImportingText(intent)

            if (isImportingText) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val isUrl =
                    sharedText != null && (URLUtil.isHttpUrl(sharedText) || URLUtil.isHttpsUrl(
                        sharedText))
                val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                val sharedEmail = intent.getStringExtra(Intent.EXTRA_EMAIL)
                val subject = sharedSubject ?: ""

                names[subject] = subject

                val fileContent =
                    buildFileContent(sharedText, sharedSubject, sharedEmail, isUrl)
                val messageContent = buildMessageContent(sharedText, sharedSubject, sharedEmail)

                fileNames.postValue(names)
                textInfo.postValue(ShareTextInfo(isUrl, subject, fileContent, messageContent))
            } else {
                @Suppress("UNCHECKED_CAST")
                var shareInfo = with(intent) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        getSerializableExtra(FileExplorerActivity.EXTRA_SHARE_INFOS,
                            ArrayList::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        getSerializableExtra(FileExplorerActivity.EXTRA_SHARE_INFOS)
                    } as List<ShareInfo>?
                }

                if (shareInfo == null) {
                    shareInfo = ShareInfo.processIntent(intent, activity)
                }

                if (shareInfo != null) {
                    for (info in shareInfo) {
                        var name = info.getTitle()
                        if (TextUtils.isEmpty(name)) {
                            name = info.originalFileName
                        }
                        names[name] = name
                    }
                }

                fileNames.postValue(names)
                filesInfo.postValue(shareInfo)
            }
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
    private fun buildFileContent(
        text: String?,
        subject: String?,
        email: String?,
        isUrl: Boolean,
    ): String {
        val builder = StringBuilder()
        if (isUrl && text != null) {
            builder.append("[InternetShortcut]\n").append("URL=").append(text).append("\n\n")
            if (subject != null) {
                builder.append(StringResourcesUtils.getString(R.string.new_file_subject_when_uploading))
                    .append(": ").append(subject).append("\n")
            }
            if (email != null) {
                builder.append(StringResourcesUtils.getString(R.string.new_file_email_when_uploading))
                    .append(": ").append(email)
            }
        } else {
            return buildMessageContent(text, subject, email)
        }
        return builder.toString()
    }

    /**
     * Builds message content from the shared text.
     *
     * @param text    Shared text.
     * @param subject Shared subject.
     * @param email   Shared email.
     * @return The message content.
     */
    private fun buildMessageContent(text: String?, subject: String?, email: String?): String {
        val builder = StringBuilder()
        if (subject != null) {
            builder.append(StringResourcesUtils.getString(R.string.new_file_subject_when_uploading))
                .append(": ").append(subject).append("\n\n")
        }
        if (email != null) {
            builder.append(StringResourcesUtils.getString(R.string.new_file_email_when_uploading))
                .append(": ").append(email).append("\n\n")
        }
        if (text != null) {
            builder.append(text)
        }
        return builder.toString()
    }

    /**
     * Checks if it is importing a text instead of files.
     * This is true if the action of the intent is ACTION_SEND, the type of the intent
     * is TYPE_TEXT_PLAIN and the intent does not contain EXTRA_STREAM extras.
     *
     * @param intent Intent to get the info.
     */
    private fun initializeImportingText(intent: Intent) {
        isImportingText =
            if (Intent.ACTION_SEND == intent.action && Constants.TYPE_TEXT_PLAIN == intent.type) {
                val extras = intent.extras
                extras != null && !extras.containsKey(Intent.EXTRA_STREAM)
            } else {
                false
            }
    }

    /**
     * Builds the final content text to share as chat message.
     *
     * @return Text to share as chat message.
     */
    val messageToShare: String?
        get() {
            val info = textInfo.value
            val names = fileNames.value
            if (info != null) {
                val typedName = if (names != null) fileNames.value!![info.subject] else info.subject
                return """
                $typedName
                
                ${info.messageContent}
                """.trimIndent()
            }
            return null
        }
}