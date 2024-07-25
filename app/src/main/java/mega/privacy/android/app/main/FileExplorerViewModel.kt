package mega.privacy.android.app.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.fileexplorer.model.FileExplorerUiState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ShareTextInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel class responsible for preparing and managing the data for FileExplorerActivity.
 *
 * @property storageState    [StorageState]
 * @property isImportingText True if it is importing text, false if it is importing files.
 * @property fileNames       File names.
 * @property uiState     [FileExplorerUiState]
 */
@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val getCopyLatestTargetPathUseCase: GetCopyLatestTargetPathUseCase,
    private val getMoveLatestTargetPathUseCase: GetMoveLatestTargetPathUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val attachNodeUseCase: AttachNodeUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val sendChatAttachmentsUseCase: SendChatAttachmentsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileExplorerUiState())

    val uiState = _uiState.asStateFlow()

    private var dataAlreadyRequested = false
    var latestCopyTargetPath: Long? = null
    var latestCopyTargetPathTab: Int = 0
    var latestMoveTargetPath: Long? = null
    var latestMoveTargetPathTab: Int = 0
    private val _filesInfo = MutableLiveData<List<ShareInfo>>()
    private val _textInfo = MutableLiveData<ShareTextInfo>()

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

    private val _copyTargetPathFlow = MutableStateFlow<Long?>(null)

    /**
     * Gets the latest used target path of move/copy
     */
    val copyTargetPathFlow: StateFlow<Long?> = _copyTargetPathFlow.asStateFlow()

    private val _moveTargetPathFlow = MutableStateFlow<Long?>(null)

    /**
     * Gets the latest used target path of move
     */
    val moveTargetPathFlow: StateFlow<Long?> = _moveTargetPathFlow.asStateFlow()

    private var _accountDetail: AccountDetail? = null

    val accountDetail: AccountDetail? get() = _accountDetail

    private var _showHiddenItems: Boolean = true

    val showHiddenItems: Boolean get() = _showHiddenItems

    fun init() = viewModelScope.launch {
        if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) {
            _accountDetail = monitorAccountDetailUseCase().firstOrNull()
            _showHiddenItems = monitorShowHiddenItemsUseCase().firstOrNull() ?: true
        }
    }

    /**
     * Set file names
     *
     * @param fileNames
     */
    fun setFileNames(fileNames: Map<String, String>) {
        _uiState.update { uiState -> uiState.copy(fileNames = fileNames) }
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
            email = sharedEmail,
            context = context,
        )

        setFileNames(mapOf(subject to subject))
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
        context?.let { getPathsAndNames(intent, it) }
        val shareInfo: List<ShareInfo> =
            getShareInfoList(intent, context) ?: emptyList()

        setFileNames(getShareInfoFileNamesMap(shareInfo))
        _filesInfo.postValue(shareInfo)
    }

    @SuppressLint("Recycle")
    @Suppress("DEPRECATION")
    private fun getPathsAndNames(intent: Intent, context: Context) {
        viewModelScope.launch {
            with(intent) {
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                })?.let { uris ->
                    Timber.d("Multiple files")
                    setUrisAndNames(uris.associateWith { uri -> getFileName(uri, context) })
                    uris
                } ?: (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    getParcelableExtra(Intent.EXTRA_STREAM)
                })?.let { uri ->
                    Timber.d("Single file")
                    setUrisAndNames(mapOf(uri to getFileName(uri, context)))
                }
            }
        }
    }

    internal fun setUrisAndNames(urisAndNames: Map<Uri, String?>) {
        _uiState.update { uiState -> uiState.copy(urisAndNames = urisAndNames) }
    }

    private suspend fun getFileName(uri: Uri, context: Context): String? =
        withContext(ioDispatcher) {
            runCatching {
                context.contentResolver?.acquireContentProviderClient(uri)
                    ?.let { client ->
                        client.query(uri, null, null, null, null)?.let { cursor ->
                            if (cursor.count == 0) {
                                cursor.close()
                                client.close()
                                null
                            } else {
                                cursor.moveToFirst()
                                val columnIndex = cursor.getColumnIndex("_display_name")
                                cursor.getString(columnIndex)
                            }
                        } ?: run {
                            client.close()
                            null
                        }
                    }
            }.getOrNull()
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
    ): List<ShareInfo>? = (intent.serializable(FileExplorerActivity.EXTRA_SHARE_INFOS)
        ?: ShareInfo.processIntent(intent, context))

    private fun getShareInfoFileNamesMap(shareInfo: List<ShareInfo>?) =
        shareInfo?.map { info ->
            info.getTitle().takeUnless {
                it.isNullOrBlank()
            } ?: info.originalFileName ?: ""
        }?.associateWith { it } ?: emptyMap()

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
            buildMessageContent(text, email, context)
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
    ): String {
        val builder = StringBuilder()
        builder.append("[InternetShortcut]\n").append("URL=").append(text).append("\n\n")
        subject?.let {
            builder.append(context.getString(R.string.new_file_subject_when_uploading))
                .append(": ").append(it).append("\n")
        }
        email?.let {
            builder.append(context.getString(R.string.new_file_email_when_uploading))
                .append(": ").append(it)
        }
        return builder.toString()
    }

    /**
     * Builds message content from the shared text.
     *
     * @param text    Shared text.
     * @param email   Shared email.
     * @return The message content.
     */
    private fun buildMessageContent(
        text: String?,
        email: String?,
        context: Context,
    ): String {
        val builder = StringBuilder()
        email?.let {
            builder.append(context.getString(R.string.new_file_email_when_uploading))
                .append(": ").append(it).append("\n\n")
        }
        text?.let {
            builder.append(it)
        }
        return builder.toString()
    }

    /**
     * Builds the final content text to share as chat message.
     *
     * @return Text to share as chat message.
     */
    val messageToShare: String?
        get() {
            return _textInfo.value?.let {
                """
                ${uiState.value.fileNames[it.subject] ?: it.subject}
                
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
     * Get the last target path of copy if not valid then return null
     */
    fun getCopyTargetPath() {
        viewModelScope.launch {
            latestCopyTargetPath = runCatching { getCopyLatestTargetPathUseCase() }.getOrNull()
            latestCopyTargetPath?.let {
                val accessPermission =
                    runCatching { getNodeAccessPermission(NodeId(it)) }.getOrNull()
                latestCopyTargetPathTab =
                    if (accessPermission == null || accessPermission == AccessPermission.OWNER)
                        FileExplorerActivity.CLOUD_TAB
                    else
                        FileExplorerActivity.INCOMING_TAB
            }
            _copyTargetPathFlow.emit(latestCopyTargetPath ?: -1)
        }
    }

    /**
     * Get the last target path of move if not valid then return null
     */
    fun getMoveTargetPath() {
        viewModelScope.launch {
            latestMoveTargetPath = runCatching { getMoveLatestTargetPathUseCase() }.getOrNull()
            latestMoveTargetPath?.let {
                val accessPermission =
                    runCatching { getNodeAccessPermission(NodeId(it)) }.getOrNull()
                latestMoveTargetPathTab =
                    if (accessPermission == null || accessPermission == AccessPermission.OWNER)
                        FileExplorerActivity.CLOUD_TAB
                    else
                        FileExplorerActivity.INCOMING_TAB
            }
            _moveTargetPathFlow.emit(latestMoveTargetPath ?: -1)
        }
    }

    /**
     * Reset copyTargetPathFlow state
     */
    fun resetCopyTargetPathState() {
        _copyTargetPathFlow.value = null
    }

    /**
     * Reset moveTargetPathFlow state
     */
    fun resetMoveTargetPathState() {
        _moveTargetPathFlow.value = null
    }

    /**
     * Upload files and nodes to the specified chats if the NewChatActivity feature flag is true, otherwise it invokes [toDoIfFalse]
     * In both cases, it will call [toDoAfter] after starting the upload
     */
    fun uploadFilesToChatIfFeatureFlagIsTrue(
        chatIds: List<Long>,
        filePaths: List<String>,
        nodeIds: List<NodeId>,
        toDoAfter: () -> Unit,
    ) {
        viewModelScope.launch {
            chatIds.forEach {
                attachNodes(it, nodeIds)
            }
            attachFiles(chatIds, filePaths)
            toDoAfter()
        }
    }

    private suspend fun attachFiles(chatIds: List<Long>, filePaths: List<String>) {
        val filePathsWithNames =
            filePaths.associateWith { uiState.value.fileNames[it.split(File.separator).last()] }
        runCatching {
            sendChatAttachmentsUseCase(
                filePathsWithNames, chatIds = chatIds.toLongArray()
            )
        }.onFailure {
            Timber.e("Error attaching files", it)
        }
    }

    private suspend fun attachNodes(chatId: Long, nodes: List<NodeId>) {
        nodes
            .mapNotNull { runCatching { getNodeByIdUseCase(it) }.getOrNull() }
            .filterIsInstance<FileNode>()
            .forEach {
                runCatching {
                    attachNodeUseCase(chatId, it as TypedFileNode)
                }.onFailure { Timber.e("Error attaching a node", it) }
            }
    }

    /**
     * Uploads a file to the specified destination.
     *
     * @param file The file to upload.
     * @param destination The destination where the file will be uploaded.
     */
    fun uploadFile(
        file: File,
        destination: Long,
    ) {
        uploadFiles(
            mapOf(file.absolutePath to uiState.value.fileNames[file.name]),
            NodeId(destination)
        )
    }

    /**
     * Uploads a list of files to the specified destination.
     *
     * @param destination The destination where the files will be uploaded.
     */
    fun uploadFiles(destination: Long) {
        with(uiState.value) {
            val pathsAndNames = urisAndNames.map { it.key }.associateWith {
                runCatching { uiState.value.fileNames[urisAndNames.getValue(it)] }
                    .getOrNull()
            }.mapKeys { it.key.toString() }
            uploadFiles(pathsAndNames, NodeId(destination))
        }
    }

    private fun uploadFiles(
        pathsAndNames: Map<String, String?>,
        destinationId: NodeId,
    ) {
        _uiState.update { state ->
            state.copy(
                uploadEvent = triggered(
                    TransferTriggerEvent.StartUpload.Files(
                        pathsAndNames = pathsAndNames,
                        destinationId = destinationId,
                    )
                )
            )
        }
    }

    /**
     * Consume upload event
     */
    fun consumeUploadEvent() {
        _uiState.update {
            it.copy(uploadEvent = consumed())
        }
    }
}