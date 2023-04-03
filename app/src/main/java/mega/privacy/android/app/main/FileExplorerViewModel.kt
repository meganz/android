package mega.privacy.android.app.main

import android.content.Context
import android.content.Intent
import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ShareTextInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.account.GetLatestTargetPath
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import javax.inject.Inject

/**
 * ViewModel class responsible for preparing and managing the data for FileExplorerActivity.
 *
 * @property storageState    [StorageState]
 * @property isImportingText True if it is importing text, false if it is importing files.
 * @property fileNames       File names.
 */
@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val getLatestTargetPath: GetLatestTargetPath,
) : ViewModel() {

    private var dataAlreadyRequested = false
    private val _filesInfo = MutableLiveData<List<ShareInfo>>()
    private val _textInfo = MutableLiveData<ShareTextInfo>()
    private val _fileNames = MutableLiveData<HashMap<String, String>>()

    /**
     * File names
     */
    val fileNames: LiveData<HashMap<String, String>> = _fileNames

    /**
     * Storage state
     */
    val storageState: StorageState
        get() = monitorStorageStateEventUseCase.getState()

    /**
     * Notifies observers about filesInfo changes.
     */
    val filesInfo: LiveData<List<ShareInfo>> = _filesInfo

    /**
     * Notifies observers about textInfo updates.
     */
    val textInfo: LiveData<ShareTextInfo> = _textInfo

    /**
     * Gets [ShareTextInfo].
     */
    val textInfoContent get() = _textInfo.value

    private val _targetPathFlow = MutableSharedFlow<Long?>()

    /**
     * Gets the latest used target path of move/copy
     */
    val targetPathFlow: SharedFlow<Long?> = _targetPathFlow

    /**
     * Set file names
     *
     * @param fileNames
     */
    fun setFileNames(fileNames: HashMap<String, String>) {
        _fileNames.value = fileNames
    }

    /**
     * Get the ShareInfo list
     *
     * @param context Current context
     * @param intent  The intent that started the current activity
     */
    fun ownFilePrepareTask(context: Context, intent: Intent) {
        if (dataAlreadyRequested) return

        viewModelScope.launch(ioDispatcher) {
            dataAlreadyRequested = true
            if (isImportingText(intent)) {
                updateTextInfoFromIntent(intent, context)
            } else {
                updateFilesInfoFromIntent(intent, context)
            }
        }
    }

    /**
     * Update text info from intent
     *
     * @param intent
     */
    private fun updateTextInfoFromIntent(intent: Intent, context: Context) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val isUrl = URLUtil.isHttpUrl(sharedText) || URLUtil.isHttpsUrl(sharedText)
        val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val sharedEmail = intent.getStringExtra(Intent.EXTRA_EMAIL)
        val subject = sharedSubject ?: ""

        val fileContent = buildFileContent(
            text = sharedText,
            subject = sharedSubject,
            email = sharedEmail,
            isUrl = isUrl,
            context = context,
        )
        val messageContent = buildMessageContent(
            text = sharedText,
            subject = sharedSubject,
            email = sharedEmail,
            context = context,
        )

        _fileNames.postValue(hashMapOf(subject to subject))
        _textInfo.postValue(ShareTextInfo(isUrl, subject, fileContent, messageContent))
    }

    /**
     * Update files info from intent
     *
     * @param intent
     * @param context
     */
    private fun updateFilesInfoFromIntent(
        intent: Intent,
        context: Context?,
    ) {
        val shareInfo: List<ShareInfo> =
            getShareInfoList(intent, context)

        _fileNames.postValue(getShareInfoFileNamesMap(shareInfo))
        _filesInfo.postValue(shareInfo)
    }

    /**
     * Get share info list
     *
     * @param intent
     * @param context
     */
    private fun getShareInfoList(
        intent: Intent,
        context: Context?,
    ) = (intent.serializable(FileExplorerActivity.EXTRA_SHARE_INFOS)
        ?: ShareInfo.processIntent(intent, context))

    private fun getShareInfoFileNamesMap(shareInfo: List<ShareInfo>?) =
        shareInfo?.map { info ->
            info.getTitle().takeUnless {
                it.isNullOrBlank()
            } ?: info.originalFileName
        }?.associateTo(hashMapOf()) { it to it }

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
        context: Context,
    ): String {
        return if (isUrl && text != null) {
            buildUrlContent(text, subject, email, context)
        } else {
            buildMessageContent(text, subject, email, context)
        }
    }

    /**
     * Build url content
     *
     * @param text
     * @param subject
     * @param email
     */
    private fun buildUrlContent(
        text: String?,
        subject: String?,
        email: String?,
        context: Context,
    ) = with(StringBuilder()) {
        append("[InternetShortcut]\n").append("URL=").append(text).append("\n\n")
        subject?.let {
            append(context.getString(R.string.new_file_subject_when_uploading))
                .append(": ").append(it).append("\n")
        }
        email?.let {
            append(context.getString(R.string.new_file_email_when_uploading))
                .append(": ").append(it)
        }
    }.toString()

    /**
     * Builds message content from the shared text.
     *
     * @param text    Shared text.
     * @param subject Shared subject.
     * @param email   Shared email.
     * @return The message content.
     */
    private fun buildMessageContent(
        text: String?,
        subject: String?,
        email: String?,
        context: Context,
    ) =
        with(StringBuilder()) {
            subject?.let {
                append(context.getString(R.string.new_file_subject_when_uploading))
                    .append(": ").append(it).append("\n\n")
            }
            email?.let {
                append(context.getString(R.string.new_file_email_when_uploading))
                    .append(": ").append(it).append("\n\n")
            }
            text?.let {
                append(it)
            }
        }.toString()

    /**
     * Builds the final content text to share as chat message.
     *
     * @return Text to share as chat message.
     */
    val messageToShare: String?
        get() {
            return _textInfo.value?.let {
                """
                ${fileNames.value?.get(it.subject) ?: it.subject}
                
                ${it.messageContent}
                """.trimIndent()
            }
        }

    /**
     * Checks if it is importing a text instead of files.
     * This is true if the action of the intent is ACTION_SEND, the type of the intent
     * is TYPE_TEXT_PLAIN and the intent does not contain EXTRA_STREAM extras.
     *
     */
    fun isImportingText(intent: Intent): Boolean =
        intent.action == Intent.ACTION_SEND
                && intent.type == Constants.TYPE_TEXT_PLAIN
                && intent.extras?.containsKey(Intent.EXTRA_STREAM)?.not() ?: true

    /**
     * Get the last target path of move/copy if not valid then return null
     */
    fun getTargetPath() {
        viewModelScope.launch {
            _targetPathFlow.emit(getLatestTargetPath())
        }
    }

}