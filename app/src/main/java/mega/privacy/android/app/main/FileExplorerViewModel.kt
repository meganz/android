package mega.privacy.android.app.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.parcelable
import mega.privacy.android.app.presentation.extensions.parcelableArrayList
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.fileexplorer.model.FileExplorerUiState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.upload.UploadDestinationActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ShareTextInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetDocumentsFromSharedUrisUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel class responsible for preparing and managing the data for FileExplorerActivity.
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
    private val getDocumentsFromSharedUrisUseCase: GetDocumentsFromSharedUrisUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileExplorerUiState())

    val uiState = _uiState.asStateFlow()

    private var dataAlreadyRequested = false
    var latestCopyTargetPath: Long? = null
    var latestCopyTargetPathTab: Int = 0
    var latestMoveTargetPath: Long? = null
    var latestMoveTargetPathTab: Int = 0
    private val _textInfo = MutableLiveData<ShareTextInfo>()

    /**
     * Storage state
     */
    val storageState: StorageState
        get() = monitorStorageStateEventUseCase.getState()

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


    init {
        viewModelScope.launch {
            combine(
                savedStateHandle.getStateFlow(
                    key = FileExplorerActivity.EXTRA_HAS_MULTIPLE_SCANS,
                    initialValue = false,
                ),
                savedStateHandle.getStateFlow(
                    key = FileExplorerActivity.EXTRA_SCAN_FILE_TYPE,
                    initialValue = -1,
                ),
            ) { hasMultipleScans: Boolean, scanFileTypeInt: Int ->
                { state: FileExplorerUiState ->
                    state.copy(
                        hasMultipleScans = hasMultipleScans,
                        isUploadingScans = scanFileTypeInt != -1,
                    )
                }
            }.collect { _uiState.update(it) }
        }
    }

    /**
     * Sets up the Cloud Drive Explorer content
     */
    fun initCloudDriveExplorerContent() = viewModelScope.launch {
        if (isHiddenNodesActive()) {
            _accountDetail = monitorAccountDetailUseCase().firstOrNull()
            _showHiddenItems = monitorShowHiddenItemsUseCase().firstOrNull() ?: true
        }
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    /**
     * Set file names
     *
     * @param fileNames
     */
    fun setFileNames(fileNames: Map<String, String>) {
        _uiState.update { uiState ->
            val documents = uiState.documents.map { doc ->
                fileNames[doc.originalName]?.let { doc.copy(name = it) } ?: doc
            }
            uiState.copy(documents = documents)
        }
        textInfoContent?.let {
            _textInfo.postValue(it.copy(subject = fileNames.values.firstOrNull() ?: ""))
        }
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
                updateFilesFromIntent(intent, context)
            }
        }
    }

    /**
     * Update text info from intent
     *
     * @param intent
     */
    private fun updateTextInfoFromIntent(intent: Intent, context: Context) {
        val sharedText = intent.getClipboardText()
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

        val nameMap =
            intent.serializable<HashMap<String, String>>(UploadDestinationActivity.EXTRA_NAME_MAP)
                ?: mapOf(subject to subject)
        setFileNames(nameMap)
        _textInfo.postValue(ShareTextInfo(isUrl, subject, fileContent, messageContent))
    }

    private fun Intent.getClipboardText(): String? =
        this.getStringExtra(Intent.EXTRA_TEXT)
            ?: getClipboardItem()?.text?.toString()


    private fun Intent.getClipboardItem() = this.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)

    /**
     * Update files info from intent
     *
     * @param intent
     * @param context
     */
    private fun updateFilesFromIntent(
        intent: Intent,
        context: Context,
    ) {
        viewModelScope.launch {
            setDocuments(getDocuments(intent, context))
        }
    }

    private fun grantUriPermission(context: Context, uris: List<Uri>) {
        uris.forEach { uri ->
            runCatching {
                context.grantUriPermission(
                    context.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.onFailure {
                Timber.e(it, "Error granting uri permission")
            }
        }
    }

    internal fun setDocuments(documents: List<DocumentEntity>?) {
        _uiState.update { uiState -> uiState.copy(documents = documents ?: emptyList()) }
    }

    /**
     * Get share info list
     *
     * @param intent
     * @param context
     */
    private suspend fun getDocuments(
        intent: Intent,
        context: Context,
    ): List<DocumentEntity>? =
        getSharedUrisFromIntent(intent, context)?.let { uriPaths ->
            return getDocumentsFromSharedUrisUseCase(intent.action, uriPaths)
        }

    private fun getSharedUrisFromIntent(intent: Intent, context: Context): List<UriPath>? =
        with(intent) {
            parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)?.let {
                it.mapNotNull { item -> item as? Uri }.let { uris ->
                    Timber.d("Multiple files")
                    grantUriPermission(context, uris)
                    uris.map { uri -> UriPath(uri.toString()) }.ifEmpty { null }
                }
            } ?: (intent.parcelable<Parcelable>(Intent.EXTRA_STREAM) as? Uri)
                ?.let { uri ->
                    Timber.d("Single file")
                    grantUriPermission(context, listOf(uri))
                    listOf(UriPath(uri.toString()))
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
            return _textInfo.value?.let { shareText ->
                """
                ${uiState.value.documentsByUriPathValue[shareText.subject] ?: shareText.subject}
                
                ${shareText.messageContent}
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
     * Upload files and nodes to the specified chats
     * It will call [toDoAfter] after starting the upload
     */
    fun uploadFilesToChat(
        chatIds: List<Long>,
        documents: List<DocumentEntity>,
        nodeIds: List<NodeId>,
        toDoAfter: () -> Unit,
    ) {
        viewModelScope.launch {
            chatIds.forEach {
                attachNodes(it, nodeIds)
            }
            attachFiles(chatIds, documents)
            toDoAfter()
        }
    }

    private suspend fun attachFiles(chatIds: List<Long>, documents: List<DocumentEntity>) {
        val filePathsWithNames = documents.associate { it.uri to it.name }
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
            mapOf(file.absolutePath to uiState.value.documentsByUriPathValue[file.name]?.name),
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
            uploadFiles(namesByUriPathValues, NodeId(destination))
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
                        waitNotificationPermissionResponseToStart = true,
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

    /**
     * Get the documents
     */
    fun getDocuments() = uiState.value.documents

    /**
     * Handles Back Navigation logic by checking if the there are scans to be uploaded or not
     */
    fun handleBackNavigation() {
        if (_uiState.value.isUploadingScans) {
            setIsScanUploadingAborted(true)
        } else {
            setShouldFinishScreen(true)
        }
    }


    /**
     * Sets the value of [FileExplorerUiState.isScanUploadingAborted]
     *
     * @param isAborting true if the User is in the process of uploading the scans, but decides to
     * back out of the process
     */
    fun setIsScanUploadingAborted(isAborting: Boolean) {
        _uiState.update { it.copy(isScanUploadingAborted = isAborting) }
    }

    /**
     * Sets the value of [FileExplorerUiState.shouldFinishScreen]
     *
     * @param shouldFinishScreen true if the File Explorer should be finished
     */
    fun setShouldFinishScreen(shouldFinishScreen: Boolean) {
        _uiState.update { it.copy(shouldFinishScreen = shouldFinishScreen) }
    }
}