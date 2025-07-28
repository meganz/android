package mega.privacy.android.app.presentation.folderlink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.myAccount.StorageStatusDialogState
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.copynode.toCopyRequestResult
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.snackBarMessageId
import mega.privacy.android.app.presentation.folderlink.model.FolderLinkState
import mega.privacy.android.app.presentation.folderlink.model.LinkErrorState
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.UrlDownloadException
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetLocalFileForNodeUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.StopAudioService
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.achievements.AreAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.advertisements.QueryAdsUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicLinkInformationUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderLinkChildrenNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderParentNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.GetFolderLinkNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.MegaNavigator
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View Model class for [FolderLinkComposeActivity]
 *
 * @param monitorMiscLoadedUseCase Use case to monitor when misc data is loaded
 */
@HiltViewModel
class FolderLinkViewModel @Inject constructor(
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorViewType: MonitorViewType,
    private val loginToFolderUseCase: LoginToFolderUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
    private val copyRequestMessageMapper: CopyRequestMessageMapper,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val setViewType: SetViewType,
    private val fetchFolderNodesUseCase: FetchFolderNodesUseCase,
    private val getFolderParentNodeUseCase: GetFolderParentNodeUseCase,
    private val getFolderLinkChildrenNodesUseCase: GetFolderLinkChildrenNodesUseCase,
    private val addNodeType: AddNodeType,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val areAchievementsEnabledUseCase: AreAchievementsEnabledUseCase,
    private val getAccountTypeUseCase: GetAccountTypeUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val getPricing: GetPricing,
    private val containsMediaItemUseCase: ContainsMediaItemUseCase,
    private val getLocalFileForNodeUseCase: GetLocalFileForNodeUseCase,
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase,
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase,
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val getFileUriUseCase: GetFileUriUseCase,
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase,
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val getFolderLinkNodeContentUriUseCase: GetFolderLinkNodeContentUriUseCase,
    private val megaNavigator: MegaNavigator,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val updateCrashAndPerformanceReportersUseCase: UpdateCrashAndPerformanceReportersUseCase,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val stopAudioService: StopAudioService,
    @ApplicationScope private val applicationScope: CoroutineScope,
    val monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
    private val getPublicLinkInformationUseCase: GetPublicLinkInformationUseCase,
    private val queryAdsUseCase: QueryAdsUseCase,
) : ViewModel() {

    /**
     * The FolderLink UI State
     */
    private val _state = MutableStateFlow(FolderLinkState())

    /**
     * The FolderLink UI State accessible outside the ViewModel
     */
    val state: StateFlow<FolderLinkState> = _state

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    init {
        checkViewType()
    }

    /**
     * This method will monitor view type and update it on state
     */
    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _state.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    /**
     * Performs Login to folder
     *
     * @param folderLink Link of the folder to login
     */
    fun folderLogin(folderLink: String, decryptionIntroduced: Boolean = false) {
        viewModelScope.launch {
            val result = runCatching {
                loginToFolderUseCase(folderLink)
            }.getOrDefault(FolderLoginStatus.ERROR)

            when (result) {
                FolderLoginStatus.SUCCESS -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = true,
                            errorState = LinkErrorState.NoError
                        )
                    }
                    with(state.value) {
                        queryAds(folderLink)
                        if (!isNodesFetched) {
                            fetchNodes(folderSubHandle)
                        }
                    }
                    checkCookiesSettings()
                }

                FolderLoginStatus.API_INCOMPLETE -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = false,
                            askForDecryptionKeyDialogEvent = triggered,
                            errorState = LinkErrorState.NoError
                        )
                    }
                }

                FolderLoginStatus.INCORRECT_KEY -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = false,
                            askForDecryptionKeyDialogEvent = if (decryptionIntroduced) triggered else consumed,
                            errorState = if (decryptionIntroduced) LinkErrorState.NoError else LinkErrorState.Unavailable,
                            snackBarMessage = if (decryptionIntroduced) -1 else result.snackBarMessageId
                        )
                    }
                }

                FolderLoginStatus.ERROR -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = false,
                            askForDecryptionKeyDialogEvent = consumed,
                            errorState = LinkErrorState.Unavailable,
                            snackBarMessage = result.snackBarMessageId
                        )
                    }
                }
            }
        }
    }

    private fun checkCookiesSettings() {
        viewModelScope.launch {
            runCatching {
                updateCrashAndPerformanceReportersUseCase()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Decrypt the url and login to folder
     *
     * @param mKey  Decryption key
     * @param url   Url to decrypt and login
     */
    fun decrypt(mKey: String?, url: String?) {
        if (TextUtils.isEmpty(mKey)) return
        var urlWithKey = ""

        url?.let {
            if (it.contains("#F!")) {
                // old folder link format
                urlWithKey = if (mKey?.startsWith("!") == true) {
                    Timber.d("Decryption key with exclamation!")
                    "$url$mKey"
                } else {
                    "$url!$mKey"
                }
            } else if (it.contains("${Constants.SEPARATOR}folder${Constants.SEPARATOR}")) {
                // new folder link format
                urlWithKey = if (mKey?.startsWith("#") == true) {
                    Timber.d("Decryption key with hash!")
                    "$url$mKey"
                } else {
                    "$url#$mKey"
                }
            }
        }

        Timber.d("Folder link to import: $urlWithKey")
        folderLogin(urlWithKey, true)
    }

    /**
     * Reset the askForDecryptionKeyDialog boolean
     */
    fun resetAskForDecryptionKeyDialog() {
        _state.update {
            it.copy(askForDecryptionKeyDialogEvent = consumed)
        }
    }

    /**
     * Handle node imports
     *
     * @param toHandle  Handle of the destination node
     */
    fun importNodes(toHandle: Long) {
        viewModelScope.launch {
            if (isMultipleNodeSelected()) {
                getSelectedNodes().map { it.id.longValue }
            } else {
                listOfNotNull(
                    (state.value.importNode ?: state.value.rootNode)?.id?.longValue
                ).also {
                    resetImportNode()
                }
            }.takeIf { it.isNotEmpty() }?.let {
                checkNameCollision(it, toHandle)
            } ?: showSnackbar(R.string.context_no_copied)
        }
    }

    /**
     * Checks the list of nodes to copy in order to know which names already exist
     *
     * @param nodeHandles         List of node handles to copy.
     * @param toHandle      Handle of destination node
     */
    fun checkNameCollision(nodeHandles: List<Long>, toHandle: Long) = viewModelScope.launch {
        runCatching {
            checkNodesNameCollisionUseCase(
                nodes = nodeHandles.associateWith { toHandle },
                type = NodeNameCollisionType.COPY
            )
        }.onSuccess { result ->
            if (result.conflictNodes.isNotEmpty()) {
                _state.update {
                    it.copy(collisionsEvent = triggered(result.conflictNodes.values.toList()))
                }
            }
            if (result.noConflictNodes.isNotEmpty()) {
                runCatching {
                    copyNodesUseCase(result.noConflictNodes)
                }.onSuccess { copyResult ->
                    _state.update {
                        it.copy(
                            copyResultEvent = triggered(copyRequestMessageMapper(copyResult.toCopyRequestResult()) to null)
                        )
                    }
                }.onFailure { throwable ->
                    _state.update {
                        it.copy(
                            copyResultEvent = triggered(null to throwable)
                        )
                    }
                }
            }
        }.onFailure { throwable ->
            Timber.e(throwable)
        }
    }

    /**
     * Handle Storage Quota Exceed Exception
     */
    fun handleQuotaException(throwable: Throwable) = viewModelScope.launch {
        val isAchievementsEnabled = areAchievementsEnabledUseCase()
        val accountType = getAccountTypeUseCase()

        if (accountType == AccountType.UNKNOWN) {
            Timber.w("Do not show dialog, not info of the account received yet")
            return@launch
        }

        val product =
            getProductAccounts().firstOrNull { it.level == Constants.PRO_III && it.months == 1 }

        when (throwable) {
            is QuotaExceededMegaException -> {
                val storageState = StorageStatusDialogState(
                    storageState = StorageState.Red,
                    accountType = accountType,
                    product = product,
                    isAchievementsEnabled = isAchievementsEnabled,
                    overQuotaAlert = true,
                    preWarning = false
                )
                _state.update { it.copy(storageStatusDialogState = storageState) }
            }

            is NotEnoughQuotaMegaException -> {
                val storageState = StorageStatusDialogState(
                    storageState = StorageState.Orange,
                    accountType = accountType,
                    product = product,
                    isAchievementsEnabled = isAchievementsEnabled,
                    overQuotaAlert = true,
                    preWarning = true
                )
                _state.update { it.copy(storageStatusDialogState = storageState) }
            }
        }
    }

    /**
     * Get product accounts
     *
     */
    private suspend fun getProductAccounts(): List<Product> =
        runCatching { getPricing(false).products }.getOrElse { Pricing(emptyList()).products }

    /**
     * Reset values once collision activity is launched
     */
    fun resetLaunchCollisionActivity() {
        _state.update {
            it.copy(collisionsEvent = consumed())
        }
    }

    /**
     * Reset values once show copy result is processed
     */
    fun resetShowCopyResult() {
        _state.update {
            it.copy(copyResultEvent = consumed())
        }
    }

    /**
     * Check if login is required
     */
    fun checkLoginRequired() {
        viewModelScope.launch {
            val hasCredentials = hasCredentialsUseCase()
            val showLogin = hasCredentials && !rootNodeExistsUseCase()
            _state.update {
                it.copy(
                    showLoginEvent = if (showLogin) triggered else consumed,
                    hasDbCredentials = hasCredentials
                )
            }
            with(state.value) {
                if (isInitialState && !showLogin) {
                    url?.let { folderLogin(it) }
                }
            }
        }
    }

    /**
     * Fetch the nodes to show
     *
     * @param folderSubHandle   Handle of the folder to fetch the nodes for
     */
    fun fetchNodes(folderSubHandle: String?) {
        viewModelScope.launch {
            runCatching { fetchFolderNodesUseCase(folderSubHandle) }
                .onSuccess { result ->
                    val hasMediaItem = containsMediaItemUseCase(result.childrenNodes)
                    _state.update {
                        it.copy(
                            isNodesFetched = true,
                            nodesList = result.childrenNodes.map { typedNode ->
                                NodeUIItem(
                                    typedNode,
                                    isSelected = false,
                                    isInvisible = false,
                                )
                            },
                            hasMediaItem = hasMediaItem,
                            rootNode = result.rootNode,
                            parentNode = result.parentNode,
                            title = result.rootNode?.name ?: ""
                        )
                    }

                    folderSubHandle?.let {
                        handleMediaFolderNavigation()
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isNodesFetched = true,
                            errorState = if (throwable is FetchFolderNodesException.Expired) LinkErrorState.Expired else LinkErrorState.Unavailable,
                        )
                    }
                }
        }
    }

    /**
     * Handle item long click
     *
     * @param nodeUIItem    Item that is long clicked
     */
    fun onItemLongClick(nodeUIItem: NodeUIItem<TypedNode>) {
        val list = _state.value.nodesList
        val index = list.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        val newNode = NodeUIItem(nodeUIItem.node, !nodeUIItem.isSelected, false)
        val newNodesList = list.updateItemAt(index = index, item = newNode)

        val selectedNodeCount = newNodesList.count { it.isSelected }

        _state.update {
            it.copy(nodesList = newNodesList, selectedNodeCount = selectedNodeCount)
        }
    }

    /**
     * Get all the selected nodes
     */
    fun getSelectedNodes(): List<NodeUIItem<TypedNode>> =
        _state.value.nodesList.filter { it.isSelected }

    /**
     * Handle select all clicked
     */
    fun onSelectAllClicked() {
        val list = _state.value.nodesList
        list.forEach { it.isSelected = true }
        _state.update {
            it.copy(nodesList = list, selectedNodeCount = list.size)
        }
    }

    /**
     * Handle clear all clicked
     */
    fun clearAllSelection() {
        val list = _state.value.nodesList
        list.forEach { it.isSelected = false }
        _state.update {
            it.copy(nodesList = list, selectedNodeCount = 0)
        }
    }

    /**
     * Handle switch between grid and list
     */
    fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_state.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    /**
     * Handle back press
     */
    fun handleBackPress() {
        viewModelScope.launch {
            if (state.value.selectedNodeCount > 0) {
                clearAllSelection()
                return@launch
            }
            state.value.parentNode?.let { parentNode ->
                runCatching { getFolderParentNodeUseCase(parentNode.id) }
                    .onSuccess { newParentNode ->
                        runCatching {
                            getFolderLinkChildrenNodesUseCase(
                                newParentNode.id.longValue,
                                null
                            )
                        }
                            .onSuccess { children ->
                                val hasMediaItem = containsMediaItemUseCase(children)
                                _state.update {
                                    it.copy(
                                        nodesList = children.map { typedNode ->
                                            NodeUIItem(
                                                node = typedNode,
                                                isSelected = false,
                                                isInvisible = false,
                                            )
                                        },
                                        hasMediaItem = hasMediaItem,
                                        parentNode = newParentNode,
                                        title = newParentNode.name
                                    )
                                }
                            }
                            .onFailure {
                                FetchFolderNodesException.GenericError().let {
                                    _state.update {
                                        it.copy(
                                            snackBarMessage = it.snackBarMessage,
                                        )
                                    }
                                }
                            }
                    }
                    .onFailure {
                        Timber.w("parentNode == NULL")
                        _state.update { it.copy(finishActivityEvent = triggered) }
                    }
                ""
            } ?: run {
                Timber.w("parentNode == NULL")
                _state.update { it.copy(finishActivityEvent = triggered) }
            }
        }
    }

    /**
     * Handle intent
     */
    fun handleIntent(intent: Intent) {
        if (intent.action == Constants.ACTION_OPEN_MEGA_FOLDER_LINK) {
            val folderUrl = intent.dataString
            var folderSubHandle: String? = null
            folderUrl?.let { url ->
                Timber.d("URL: $url")
                val s =
                    url.split("!".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                Timber.d("URL parts: ${s.size}")
                for (i in s.indices) {
                    when (i) {
                        1 -> {
                            Timber.d("URL_handle: ${s[1]}")
                        }

                        2 -> {
                            Timber.d("URL_key: ${s[2]}")
                        }

                        3 -> {
                            folderSubHandle = s[3]
                            Timber.d("URL_subhandle: $folderSubHandle")
                        }
                    }
                }
                _state.update { it.copy(url = folderUrl, folderSubHandle = folderSubHandle) }
            } ?: Timber.w("url NULL")
        }
    }

    private fun queryAds(url: String) {
        viewModelScope.launch {
            runCatching {
                val info = getPublicLinkInformationUseCase(url)
                Timber.d("Public link info: $info")
                queryAdsUseCase(info.id.longValue)
            }.onSuccess { value ->
                _state.update { it.copy(shouldShowAdsForLink = value) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Navigate to selected folder
     *
     * @param nodeUIItem    Folder node to navigate to
     */
    fun openFolder(nodeUIItem: NodeUIItem<TypedNode>) {
        viewModelScope.launch {
            val children =
                runCatching { getFolderLinkChildrenNodesUseCase(nodeUIItem.id.longValue, null) }
                    .getOrDefault(emptyList())
            _state.update {
                val hasMediaItem = containsMediaItemUseCase(children)
                it.copy(
                    parentNode = addNodeType(nodeUIItem.node as FolderNode) as TypedFolderNode,
                    title = nodeUIItem.name,
                    nodesList = children.map { childNode ->
                        NodeUIItem(childNode, isSelected = false, isInvisible = false)
                    },
                    hasMediaItem = hasMediaItem
                )
            }
        }
    }

    /**
     * Get if multiple nodes are selected
     */
    fun isMultipleNodeSelected(): Boolean = state.value.selectedNodeCount > 0

    /**
     * Reset and notify that openFile event is consumed
     */
    fun resetOpenFile() = _state.update { it.copy(openFile = consumed()) }

    /**
     * Handle import button click
     *
     * @param node  Node for which import is clicked
     */
    fun handleImportClick(node: NodeUIItem<TypedNode>?) {
        state.value.rootNode?.let {
            _state.update { it.copy(importNode = node, selectImportLocation = triggered) }
        }
    }

    /**
     * Reset and notify that selectImportLocation event is consumed
     */
    fun resetSelectImportLocation() = _state.update { it.copy(selectImportLocation = consumed) }

    /**
     * Reset import node
     */
    fun resetImportNode() = _state.update { it.copy(importNode = null) }

    /**
     * Handle Save to device button click
     */
    fun handleSaveToDevice(nodeUIItem: NodeUIItem<TypedNode>?) {
        viewModelScope.launch {
            val nodes = if (isMultipleNodeSelected()) {
                getSelectedNodes().map { it.node }.also {
                    clearAllSelection()
                }
            } else {
                val node = nodeUIItem?.node
                    ?: state.value.parentNode
                    ?: state.value.rootNode
                listOfNotNull(node)
            }.mapNotNull {
                runCatching {
                    mapNodeToPublicLinkUseCase(it as UnTypedNode, null)
                }.onFailure { error ->
                    Timber.e(error)
                }.getOrNull()
            }
            _state.update {
                it.copy(
                    downloadEvent = triggered(
                        TransferTriggerEvent.StartDownloadNode(
                            nodes = nodes,
                            withStartMessage = false,
                        )
                    )
                )
            }
        }
    }

    /**
     * update intent values for image
     */
    fun updateImageIntent(intent: Intent) {
        _state.update { it.copy(openFile = triggered(intent)) }
    }

    /**
     * Update intent values for pdf
     */
    fun updatePdfIntent(pdfIntent: Intent, fileNode: FileNode, mimeType: String) {
        viewModelScope.launch {
            runCatching {
                pdfIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    putExtra(Constants.INTENT_EXTRA_KEY_IS_FOLDER_LINK, true)
                    putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
                    putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
                    putExtra(Constants.INTENT_EXTRA_KEY_APP, true)
                    putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FOLDER_LINK_ADAPTER)
                }
                getLocalFileForNodeUseCase(fileNode)?.let {
                    val path = it.path
                    if (path.contains(Environment.getExternalStorageDirectory().path)) {
                        val uri = getFileUriUseCase(it, Constants.AUTHORITY_STRING_FILE_PROVIDER)
                        pdfIntent.setDataAndType(Uri.parse(uri), mimeType)
                    } else {
                        pdfIntent.setDataAndType(Uri.fromFile(it), mimeType)
                    }
                    pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } ?: run {
                    setStreamingIntentParams(
                        pdfIntent,
                        state.value.hasDbCredentials,
                        fileNode.id.longValue,
                        mimeType
                    )
                }
                pdfIntent
            }.onSuccess { intent ->
                intent.let { _state.update { it.copy(openFile = triggered(intent)) } }
            }.onFailure {
                Timber.e("itemClick:ERROR:httpServerGetLocalLink")
            }
        }
    }

    /**
     * Update intent value for text editor
     */
    fun updateTextEditorIntent(intent: Intent, fileNode: FileNode) {
        intent.apply {
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
            putExtra(TextEditorViewModel.MODE, TextEditorViewModel.VIEW_MODE)
            putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FOLDER_LINK_ADAPTER)
        }
        _state.update { it.copy(openFile = triggered(intent)) }
    }

    internal fun openOtherTypeFile(context: Context, fileNode: TypedNode) {
        viewModelScope.launch {
            val typedFileNode = fileNode as? TypedFileNode ?: return@launch
            getNodePreviewFileUseCase(typedFileNode)?.let { localFile ->
                if (fileNode.type is ZipFileTypeInfo) {
                    openZipFile(
                        context = context,
                        localFile = localFile,
                        fileNode = typedFileNode,
                    )
                } else {
                    handleOtherFiles(
                        context = context,
                        localFile = localFile,
                        currentFileNode = fileNode,
                    )
                }
            } ?: updateNodeToPreview(fileNode)
        }
    }

    private fun openZipFile(
        context: Context,
        localFile: File,
        fileNode: TypedFileNode,
    ) {
        Timber.d("The file is zip, open in-app.")
        megaNavigator.openZipBrowserActivity(
            context = context,
            zipFilePath = localFile.absolutePath,
            nodeHandle = fileNode.id.longValue,
        ) {
            showSnackbar(R.string.message_zip_format_error)
        }
    }

    private fun handleOtherFiles(
        context: Context,
        localFile: File,
        currentFileNode: TypedFileNode,
    ) {
        Intent(Intent.ACTION_VIEW).apply {
            nodeContentUriIntentMapper(
                intent = this,
                content = NodeContentUri.LocalContentUri(localFile),
                mimeType = currentFileNode.type.mimeType,
                isSupported = false
            )
            runCatching {
                context.startActivity(this)
            }.onFailure { error ->
                Timber.e(error)
                openShareIntent(context = context)
            }
        }
    }

    private fun Intent.openShareIntent(context: Context) {
        if (resolveActivity(context.packageManager) == null) {
            action = Intent.ACTION_SEND
        }
        runCatching {
            context.startActivity(this)
        }.onFailure {
            Timber.e(it)
            showSnackbar(R.string.intent_not_available)
        }
    }

    /**
     * Update nodes to download
     */
    private suspend fun updateNodeToPreview(node: TypedNode) =
        runCatching {
            mapNodeToPublicLinkUseCase(node as UnTypedNode, null)
        }.onSuccess { linkNode ->
            _state.update {
                it.copy(
                    downloadEvent = triggered(
                        TransferTriggerEvent.StartDownloadForPreview(
                            node = linkNode,
                            isOpenWith = false
                        )
                    )
                )
            }
        }.onFailure {
            Timber.e(it)
        }

    private suspend fun setStreamingIntentParams(
        intent: Intent,
        hasDbCredentials: Boolean,
        handle: Long,
        mimeType: String,
    ) {
        val path = if (hasDbCredentials) {
            startMegaApiHttpServer(intent)
            getLocalFolderLinkFromMegaApiUseCase(handle) ?: throw UrlDownloadException()
        } else {
            startMegaApiFolderHttpServer(intent)
            getLocalFolderLinkFromMegaApiFolderUseCase(handle) ?: throw UrlDownloadException()
        }
        intent.setDataAndType(Uri.parse(path), mimeType)
    }

    /**
     * Start the server if not started
     * @param intent [Intent]
     */
    private suspend fun startMegaApiFolderHttpServer(intent: Intent): Intent {
        if (megaApiFolderHttpServerIsRunningUseCase() == 0) {
            megaApiFolderHttpServerStartUseCase()
            intent.putExtra(ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }
        return intent
    }

    /**
     * Start the server if not started
     * @param intent [Intent]
     */
    private suspend fun startMegaApiHttpServer(intent: Intent): Intent {
        if (httpServerIsRunning() == 0) {
            httpServerStart()
            intent.putExtra(ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }
        return intent
    }

    /**
     * Reset and notify that downloadNodes event is consumed
     */
    fun resetDownloadNode() = _state.update {
        it.copy(downloadEvent = consumed())
    }

    /**
     * Trigger event to show Snackbar message
     *
     * @param messageId     String id of content for snack bar
     */
    fun showSnackbar(messageId: Int) {
        val message = getStringFromStringResMapper(messageId)
        showSnackbar(message)
    }

    /**
     * Trigger event to show Snackbar message
     *
     * @param message     Content for snack bar
     */
    fun showSnackbar(message: String) =
        _state.update { it.copy(snackbarMessageContent = triggered(message)) }


    /**
     * Reset and notify that snackbarMessage is consumed
     */
    fun resetSnackbarMessage() =
        _state.update {
            it.copy(snackbarMessageContent = consumed())
        }

    /**
     * Handle more options/ 3 dots clicked
     */
    fun handleMoreOptionClick(nodeUIItem: NodeUIItem<TypedNode>?) {
        val moreOptionNode = nodeUIItem ?: state.value.parentNode?.let {
            NodeUIItem<TypedNode>(
                it,
                isSelected = false,
                isInvisible = false
            )
        }
        if (moreOptionNode != null) {
            _state.update { it.copy(moreOptionNode = moreOptionNode, openMoreOption = triggered) }
        }
    }

    /**
     * Reset and notify openMoreOption is consumed
     */
    fun resetOpenMoreOption() {
        _state.update { it.copy(openMoreOption = consumed) }
    }

    /**
     * Reset moreOptionNode on closing of bottom sheet
     */
    fun resetMoreOptionNode() {
        _state.update { it.copy(moreOptionNode = null) }
    }

    /**
     * Reset storageStatusDialogState to dismiss the dialog
     */
    fun dismissStorageStatusDialog() {
        _state.update { it.copy(storageStatusDialogState = null) }
    }

    /**
     * Get current user email
     */
    suspend fun getEmail() = runCatching { getCurrentUserEmail() }
        .onFailure { Timber.e(it) }
        .getOrDefault("")

    /**
     * Reset finishActivityEvent when consumed
     */
    fun onShowLoginEventConsumed() {
        _state.update { it.copy(showLoginEvent = consumed) }
    }

    /**
     * Reset finishActivityEvent when consumed
     */
    fun onFinishActivityEventConsumed() {
        _state.update { it.copy(finishActivityEvent = consumed) }
    }

    internal suspend fun getNodeContentUri(fileNode: TypedFileNode) =
        getFolderLinkNodeContentUriUseCase(fileNode)

    /**
     * onCleared
     */
    override fun onCleared() {
        super.onCleared()
        stopAudioPlayerServiceWithoutLogin()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun stopAudioPlayerServiceWithoutLogin() = applicationScope.launch {
        if (!isUserLoggedInUseCase()) stopAudioService()
    }

    /**
     * Handle media folder navigation for specific sub-handles
     * This function navigates to a media folder if the sub-handle corresponds to an image or video node
     */
    private fun handleMediaFolderNavigation() {
        viewModelScope.launch {
            _state.value.folderSubHandle?.let { subHandle ->
                _state.value.nodesList.firstOrNull { nodeUIItem ->
                    nodeUIItem.base64Id == subHandle
                }?.let { nodeUIItem ->
                    if (nodeUIItem.node is FileNode &&
                        (nodeUIItem.node.type is ImageFileTypeInfo || nodeUIItem.node.type is VideoFileTypeInfo)
                    ) {
                        openFile(nodeUIItem)
                    }
                }
            }
        }
    }

    /**
     * Open file using NodeUIItem
     * This method triggers the file opening event for the Activity to handle
     *
     * @param nodeUIItem The NodeUIItem to open
     */
    fun openFile(nodeUIItem: NodeUIItem<TypedNode>) {
        _state.update {
            it.copy(openFileNodeEvent = triggered(nodeUIItem))
        }
    }

    /**
     * Reset and notify that openFileNodeEvent is consumed
     */
    fun resetOpenFileNodeEvent() {
        _state.update {
            it.copy(openFileNodeEvent = consumed())
        }
    }
}
