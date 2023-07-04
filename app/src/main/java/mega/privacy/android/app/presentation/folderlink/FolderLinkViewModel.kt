package mega.privacy.android.app.presentation.folderlink

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetPublicNodeListByIds
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.myAccount.StorageStatusDialogState
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.errorDialogContentId
import mega.privacy.android.app.presentation.extensions.errorDialogTitleId
import mega.privacy.android.app.presentation.extensions.snackBarMessageId
import mega.privacy.android.app.presentation.folderlink.model.FolderLinkState
import mega.privacy.android.app.presentation.mapper.GetIntentFromFolderLinkToOpenFileMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.achievements.AreAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderLinkChildrenNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderParentNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model class for [FolderLinkActivity]
 */
@HiltViewModel
class FolderLinkViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorViewType: MonitorViewType,
    private val loginToFolderUseCase: LoginToFolderUseCase,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val legacyCopyNodeUseCase: LegacyCopyNodeUseCase,
    private val copyRequestMessageMapper: CopyRequestMessageMapper,
    private val hasCredentials: HasCredentials,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val setViewType: SetViewType,
    private val fetchFolderNodesUseCase: FetchFolderNodesUseCase,
    private val getFolderParentNodeUseCase: GetFolderParentNodeUseCase,
    private val getFolderLinkChildrenNodesUseCase: GetFolderLinkChildrenNodesUseCase,
    private val addNodeType: AddNodeType,
    private val getIntentFromFolderLinkToOpenFileMapper: GetIntentFromFolderLinkToOpenFileMapper,
    private val getPublicNodeListByIds: GetPublicNodeListByIds,
    private val getNodeUseCase: GetNodeUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val areAchievementsEnabledUseCase: AreAchievementsEnabledUseCase,
    private val getAccountTypeUseCase: GetAccountTypeUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val getPricing: GetPricing,
) : ViewModel() {

    /**
     * The FolderLink UI State
     */
    private val _state = MutableStateFlow(FolderLinkState())

    private val rxSubscriptions = CompositeDisposable()

    /**
     * The FolderLink UI State accessible outside the ViewModel
     */
    val state: StateFlow<FolderLinkState> = _state

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

    init {
        checkViewType()
    }

    override fun onCleared() {
        rxSubscriptions.clear()
        super.onCleared()
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
            when (val result = loginToFolderUseCase(folderLink)) {
                FolderLoginStatus.SUCCESS -> {
                    _state.update { it.copy(isInitialState = false, isLoginComplete = true) }
                }

                FolderLoginStatus.API_INCOMPLETE -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = false,
                            askForDecryptionKeyDialog = true,
                        )
                    }
                }

                FolderLoginStatus.INCORRECT_KEY -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = false,
                            askForDecryptionKeyDialog = decryptionIntroduced,
                            errorDialogTitle = result.errorDialogTitleId,
                            errorDialogContent = result.errorDialogContentId,
                            snackBarMessage = result.snackBarMessageId
                        )
                    }
                }

                FolderLoginStatus.ERROR -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = false,
                            askForDecryptionKeyDialog = false,
                            errorDialogTitle = result.errorDialogTitleId,
                            errorDialogContent = result.errorDialogContentId,
                            snackBarMessage = result.snackBarMessageId
                        )
                    }
                }
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
     * Update whether nodes are fetched or not
     *
     * @param value Whether nodes are fetched
     */
    fun updateIsNodesFetched(value: Boolean) {
        _state.update {
            it.copy(isNodesFetched = value)
        }
    }

    /**
     * Reset the askForDecryptionKeyDialog boolean
     */
    fun resetAskForDecryptionKeyDialog() {
        _state.update {
            it.copy(askForDecryptionKeyDialog = false)
        }
    }

    /**
     * Checks the list of nodes to copy in order to know which names already exist
     *
     * @param nodes         List of node handles to copy.
     * @param toHandle      Handle of destination node
     */
    @SuppressLint("CheckResult")
    fun checkNameCollision(nodes: List<MegaNode>, toHandle: Long) {
        checkNameCollisionUseCase.checkNodeList(nodes, toHandle, NameCollisionType.COPY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: Pair<ArrayList<NameCollision>, List<MegaNode>> ->
                    val collisions: ArrayList<NameCollision> = result.first
                    if (collisions.isNotEmpty()) {
                        _state.update {
                            it.copy(collisions = collisions)
                        }
                    }
                    val nodesWithoutCollisions: List<MegaNode> = result.second
                    if (nodesWithoutCollisions.isNotEmpty()) {
                        legacyCopyNodeUseCase.copy(nodesWithoutCollisions, toHandle)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { copyRequestResult: CopyRequestResult?, copyThrowable: Throwable? ->
                                if (copyThrowable != null) {
                                    _state.update {
                                        it.copy(copyThrowable = copyThrowable)
                                    }
                                } else {
                                    _state.update {
                                        it.copy(
                                            copyResultText = copyRequestMessageMapper(
                                                copyRequestResult
                                            )
                                        )
                                    }
                                }
                            }.addTo(rxSubscriptions)
                    }
                },
                { throwable: Throwable -> Timber.e(throwable) }
            ).addTo(rxSubscriptions)

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
    suspend fun getProductAccounts(): List<Product> =
        runCatching { getPricing(false).products }.getOrElse { Pricing(emptyList()).products }

    /**
     * Reset values once collision activity is launched
     */
    fun resetLaunchCollisionActivity() {
        _state.update {
            it.copy(collisions = null)
        }
    }

    /**
     * Reset values once show copy result is processed
     */
    fun resetShowCopyResult() {
        _state.update {
            it.copy(copyResultText = null, copyThrowable = null)
        }
    }

    /**
     * Check if login is required
     */
    fun checkLoginRequired() {
        viewModelScope.launch {
            val hasCredentials = hasCredentials()
            _state.update {
                it.copy(
                    shouldLogin = (hasCredentials && !rootNodeExistsUseCase()),
                    hasDbCredentials = hasCredentials
                )
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
                            rootNode = result.rootNode,
                            parentNode = result.parentNode,
                            title = result.rootNode?.name ?: ""
                        )
                    }
                }
                .onFailure { throwable ->
                    var errorTitle = FetchFolderNodesException.GenericError().errorDialogTitleId
                    var errorContent = FetchFolderNodesException.GenericError().errorDialogContentId
                    var snackBarContent = FetchFolderNodesException.GenericError().snackBarMessageId
                    if (throwable is FetchFolderNodesException) {
                        errorTitle = throwable.errorDialogTitleId
                        errorContent = throwable.errorDialogContentId
                        snackBarContent = throwable.snackBarMessageId
                    }
                    _state.update {
                        it.copy(
                            isNodesFetched = true,
                            errorDialogTitle = errorTitle,
                            errorDialogContent = errorContent,
                            snackBarMessage = snackBarContent
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
                                _state.update {
                                    it.copy(
                                        nodesList = children.map { typedNode ->
                                            NodeUIItem(
                                                node = typedNode,
                                                isSelected = false,
                                                isInvisible = false,
                                            )
                                        },
                                        parentNode = newParentNode,
                                        title = newParentNode.name
                                    )
                                }
                            }
                            .onFailure {
                                FetchFolderNodesException.GenericError().let {
                                    _state.update {
                                        it.copy(
                                            errorDialogTitle = it.errorDialogTitle,
                                            errorDialogContent = it.errorDialogContent,
                                            snackBarMessage = it.snackBarMessage
                                        )
                                    }
                                }
                            }
                    }
                    .onFailure {
                        Timber.w("parentNode == NULL")
                        _state.update { it.copy(finishActivity = true) }
                    }
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

    /**
     * Handle item click
     *
     * @param nodeUIItem    Item that is clicked
     * @param activity      Activity
     */
    fun onItemClick(nodeUIItem: NodeUIItem<TypedNode>, activity: Activity) {
        viewModelScope.launch {
            if (isMultipleNodeSelected()) {
                onItemLongClick(nodeUIItem)
            } else {
                if (nodeUIItem.node is FolderNode) {
                    openFolder(nodeUIItem)
                } else if (nodeUIItem.node is FileNode) {
                    runCatching {
                        getIntentFromFolderLinkToOpenFileMapper(
                            activity = activity,
                            fileNode = nodeUIItem.node,
                            viewType = Constants.FOLDER_LINK_ADAPTER,
                            childrenNodeIds = state.value.nodesList.map { it.id.longValue }
                        )
                    }.onSuccess { intent ->
                        intent?.let { _state.update { it.copy(openFile = triggered(intent)) } }
                    }.onFailure {
                        Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                    }
                }
            }
        }
    }

    /**
     * Navigate to selected folder
     *
     * @param nodeUIItem    Folder node to navigate to
     */
    private fun openFolder(nodeUIItem: NodeUIItem<TypedNode>) {
        viewModelScope.launch {
            val children = getFolderLinkChildrenNodesUseCase(nodeUIItem.id.longValue, null)
            _state.update {
                it.copy(
                    parentNode = addNodeType(nodeUIItem.node as FolderNode) as TypedFolderNode,
                    title = nodeUIItem.name,
                    nodesList = children.map { childNode ->
                        NodeUIItem(childNode, isSelected = false, isInvisible = false)
                    }
                )
            }
        }
    }

    /**
     * Get if multiple nodes are selected
     */
    private fun isMultipleNodeSelected(): Boolean = state.value.selectedNodeCount > 0

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
     * Handle node imports
     *
     * @param toHandle  Handle of the destination node
     */
    fun importNodes(toHandle: Long) {
        viewModelScope.launch {
            if (isMultipleNodeSelected()) {
                Timber.d("Is multiple select")
                val selectedNodes = getSelectedNodes()
                if (selectedNodes.isEmpty())
                    return@launch
                val selectedNodeIds = getSelectedNodes().map { it.id.longValue }
                val selectedMegaNodes =
                    getNodeUseCase.getAuthorizedNodes(selectedNodeIds).filterNotNull()
                checkNameCollision(selectedMegaNodes, toHandle)
            } else {
                val importNodeHandle =
                    state.value.importNode?.id?.longValue ?: state.value.rootNode?.id?.longValue
                if (importNodeHandle != null) {
                    val selectedNode = getNodeUseCase.getAuthorizedNode(importNodeHandle)
                    selectedNode?.let { checkNameCollision(listOf(it), toHandle) }
                    resetImportNode()
                } else {
                    Timber.w("Selected Node is NULL")
                    showSnackbar(R.string.context_no_copied)
                }
            }
        }
    }

    /**
     * Handle Save to device button click
     */
    fun handleSaveToDevice(nodeUIItem: NodeUIItem<TypedNode>?) {
        viewModelScope.launch {
            if (isMultipleNodeSelected()) {
                val selectedNodeIds = getSelectedNodes().map { it.id.longValue }
                val selectedNodes = getPublicNodeListByIds(selectedNodeIds)
                _state.update { it.copy(downloadNodes = triggered(selectedNodes)) }
                clearAllSelection()
            } else {
                val downloadNodeId =
                    nodeUIItem?.id?.longValue
                        ?: state.value.parentNode?.id?.longValue
                        ?: state.value.rootNode?.id?.longValue

                downloadNodeId?.let {
                    getPublicNodeListByIds(listOf(downloadNodeId)).let { downloadNode ->
                        _state.update { it.copy(downloadNodes = triggered(downloadNode)) }
                    }
                } ?: Timber.w("rootNode null!!")
            }
        }
    }

    /**
     * Reset and notify that downloadNodes event is consumed
     */
    fun resetDownloadNode() = _state.update { it.copy(downloadNodes = consumed()) }

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
     * Handle action click on StorageStatusDialog
     */
    fun handleActionClick(context: Context) = viewModelScope.launch {
        val email = getCurrentUserEmail() ?: ""
        state.value.storageStatusDialogState?.accountType?.let { accountType ->
            dismissStorageStatusDialog()
            when (accountType) {
                AccountType.PRO_III -> {
                    AlertsAndWarnings.askForCustomizedPlan(context, email, accountType)
                }

                else -> {
                    context.startActivity(Intent(context, UpgradeAccountActivity::class.java))
                }
            }
        }
    }
}