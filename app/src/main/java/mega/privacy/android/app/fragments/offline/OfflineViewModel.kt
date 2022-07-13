package mega.privacy.android.app.fragments.offline

import android.content.Context
import androidx.collection.SparseArrayCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.DefaultDispatcher
import mega.privacy.android.app.di.MainDispatcher
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.repo.MegaNodeRepo
import mega.privacy.android.app.utils.Constants.BACK_PRESS_HANDLED
import mega.privacy.android.app.utils.Constants.BACK_PRESS_NOT_HANDLED
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.OFFLINE_ROOT
import mega.privacy.android.app.utils.FileUtil.getFileFolderInfo
import mega.privacy.android.app.utils.FileUtil.getFileInfo
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.OfflineUtils.getURLOfflineFileContent
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.domain.usecase.GetThumbnail
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.Stack
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

@HiltViewModel
class OfflineViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: MegaNodeRepo,
    monitorNodeUpdates: MonitorNodeUpdates,
    private val getThumbnail: GetThumbnail,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : BaseRxViewModel() {

    private var order = ORDER_DEFAULT_ASC
    private var searchQuery: String? = null
    private var historySearchQuery: String? = null
    private var historySearchPath: String? = null
    private var navigationDepthInSearch = 0
    private val firstVisiblePositionStack = Stack<Int>()

    private val openNodeAction = PublishSubject.create<Pair<Int, OfflineNode>>()
    private val openFolderFullscreenAction = PublishSubject.create<String>()
    private val showOptionsPanelAction = PublishSubject.create<MegaOffline>()

    private val _nodes = MutableLiveData<Pair<List<OfflineNode>, Int>>()
    private val _nodeToOpen = MutableLiveData<Event<Pair<Int, OfflineNode>>>()
    private val _actionBarTitle = MutableLiveData<String?>()
    private val _actionMode = MutableLiveData<Boolean>()
    private val _nodesToAnimate = MutableLiveData<Set<Int>>()
    private val _pathLiveData = MutableLiveData<String>()
    private val _submitSearchQuery = MutableLiveData<Boolean>()
    private val _closeSearchView = MutableLiveData<Boolean>()
    private val _openFolderFullscreen = MutableLiveData<Event<String>>()
    private val _showOptionsPanel = MutableLiveData<Event<MegaOffline>>()
    private val _showSortedBy = MutableLiveData<Event<Boolean>>()
    private val _urlFileOpenAsUrl = MutableLiveData<Event<String>>()
    private val _urlFileOpenAsFile = MutableLiveData<Event<File>>()

    private val creatingThumbnailNodes = HashSet<String>()
    private val selectedNodes: SparseArrayCompat<MegaOffline> = SparseArrayCompat(5)
    private var rootFolderOnly = false
    private var isList = true
    private var gridSpanCount = 2

    val nodes: LiveData<Pair<List<OfflineNode>, Int>> = _nodes
    val nodeToOpen: LiveData<Event<Pair<Int, OfflineNode>>> = _nodeToOpen
    val actionBarTitle: LiveData<String?> = _actionBarTitle
    val actionMode: LiveData<Boolean> = _actionMode
    val nodesToAnimate: LiveData<Set<Int>> = _nodesToAnimate
    val pathLiveData: LiveData<String> = _pathLiveData
    val submitSearchQuery: LiveData<Boolean> = _submitSearchQuery
    val closeSearchView: LiveData<Boolean> = _closeSearchView
    val openFolderFullscreen: LiveData<Event<String>> = _openFolderFullscreen
    val showOptionsPanel: LiveData<Event<MegaOffline>> = _showOptionsPanel
    val showSortedBy: LiveData<Event<Boolean>> = _showSortedBy
    val urlFileOpenAsUrl: LiveData<Event<String>> = _urlFileOpenAsUrl
    val urlFileOpenAsFile: LiveData<Event<File>> = _urlFileOpenAsFile

    var path = ""
        private set
    var selecting = false
        private set
    var placeholderCount = 0
        private set
    var skipNextAutoScroll = false

    /**
     * Monitor global node updates
     */
    var updateNodes: Flow<List<MegaNode>> =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate") }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())


    /**
     * Job for processing OfflineNodes loading
     */
    var loadOfflineNodesJob: Job? = null

    init {
        add(
            openNodeAction.throttleFirst(1, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _nodeToOpen.value = Event(it) },
                    logErr("OfflineViewModel openNodeAction")
                )
        )
        add(
            openFolderFullscreenAction.throttleFirst(1, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _openFolderFullscreen.value = Event(it) },
                    logErr("OfflineViewModel openFolderFullscreenAction")
                )
        )
        add(
            showOptionsPanelAction.throttleFirst(1, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _showOptionsPanel.value = Event(it) },
                    logErr("OfflineViewModel showOptionsPanelAction")
                )
        )
    }

    fun getSelectedNodes(): List<MegaOffline> {
        val list = ArrayList<MegaOffline>()

        for (i in 0 until selectedNodes.size()) {
            list.add(selectedNodes.valueAt(i))
        }

        return list
    }

    fun getSelectedNodesCount(): Int {
        return selectedNodes.size()
    }

    fun getDisplayedNodesCount(): Int = nodes.value?.first?.size ?: 0

    fun selectAll() {
        val nodeList = nodes.value?.first ?: return

        val animNodeIndices = mutableSetOf<Int>()

        for ((position, node) in nodeList.withIndex()) {
            if (node == OfflineNode.HEADER || node == OfflineNode.PLACE_HOLDER) {
                continue
            }
            if (!node.selected) {
                animNodeIndices.add(position)
            }
            node.selected = true
            node.uiDirty = true
            selectedNodes.put(node.node.id, node.node)
        }

        _nodesToAnimate.value = animNodeIndices
        selecting = true
        _nodes.value = Pair(ArrayList(nodeList), -1)
        _actionMode.value = true
    }

    fun clearSelection() {
        if (!selecting) {
            return
        }

        selecting = false
        _actionMode.value = false
        selectedNodes.clear()

        val animNodeIndices = mutableSetOf<Int>()
        val nodeList = nodes.value?.first ?: return

        for ((position, node) in nodeList.withIndex()) {
            if (node.selected) {
                animNodeIndices.add(position)
            }
            node.selected = false
            node.uiDirty = true
        }

        _nodesToAnimate.value = animNodeIndices
        _nodes.value = Pair(ArrayList(nodeList), -1)
    }

    fun onNodeClicked(position: Int, node: OfflineNode, firstVisiblePosition: Int) {
        if (selecting) {
            handleSelection(position, node)
        } else {
            val nodeFile = getOfflineFile(context, node.node)

            if (isFileAvailable(nodeFile)) {
                when {
                    nodeFile.isDirectory -> navigateIn(node.node, firstVisiblePosition)
                    nodeFile.isFile -> openNodeAction.onNext(Pair(position, node))
                }
            }
        }
    }

    fun onNodeLongClicked(position: Int, node: OfflineNode) {
        if (!rootFolderOnly) {
            selecting = true
        }

        onNodeClicked(position, node, INVALID_POSITION)
    }

    fun onNodeOptionsClicked(position: Int, node: OfflineNode) {
        if (selecting) {
            onNodeClicked(position, node, INVALID_POSITION)
        } else {
            showOptionsPanelAction.onNext(node.node)
        }
    }

    private fun handleSelection(position: Int, node: OfflineNode) {
        val nodes = _nodes.value?.first

        if (nodes == null || position < 0 || position >= nodes.size
            || nodes[position].node.id != node.node.id
        ) {
            return
        }

        nodes[position].selected = !nodes[position].selected

        if (nodes[position].selected) {
            selectedNodes.put(node.node.id, node.node)
        } else {
            selectedNodes.remove(node.node.id)
        }

        nodes[position].uiDirty = true
        selecting = !selectedNodes.isEmpty
        _actionMode.value = selecting

        _nodesToAnimate.value = hashSetOf(position)
    }

    private fun navigateIn(folder: MegaOffline, firstVisiblePosition: Int) {
        if (searchQuery == "") {
            searchQuery = null
            _closeSearchView.value = true
        }

        val query = searchQuery
        searchQuery = null

        // submit search query and push search action into back stack when click folder
        when {
            query != null -> {
                historySearchQuery = query
                historySearchPath = path
                navigationDepthInSearch++
                _submitSearchQuery.value = true
            }

            historySearchQuery != null -> {
                navigationDepthInSearch++
            }
            else -> {
                firstVisiblePositionStack
                    .push(if (firstVisiblePosition >= 0) firstVisiblePosition else 0)
            }
        }

        if (rootFolderOnly) {
            _pathLiveData.value = folder.path + folder.name + "/"
            openFolderFullscreenAction.onNext(_pathLiveData.value)
        } else {
            navigateTo(folder.path + folder.name + "/", folder.name)
        }
    }

    fun navigateOut(initPath: String): Int {
        // no search action in back stack, should dismiss OfflineFragment
        if (path == "" || (path == initPath && searchQuery == null)) {
            return BACK_PRESS_NOT_HANDLED
        }

        val query = searchQuery
        searchQuery = null

        // has active search, should exit search mode
        if (query != null) {
            Timber.d("navigateOut exit search mode")
            navigateTo(path, titleFromPath(path), 0)
            return BACK_PRESS_HANDLED
        }

        val searchPath = historySearchPath

        // has search action in back stack, should pop back stack
        if (navigationDepthInSearch > 0 && searchPath != null) {
            navigationDepthInSearch--
            // and if back stack is empty, then should re-enter search mode
            if (navigationDepthInSearch == 0) {
                searchQuery = historySearchQuery
                Timber.d("navigateOut from searchPath")
                path = searchPath
                historySearchQuery = null
                historySearchPath = null
                navigateTo(path, titleFromPath(path))
                return BACK_PRESS_HANDLED
            }
        }

        // if back stack isn't empty, or no search action in back stack, just navigate out
        path = path.substring(0, path.length - 1)
        path = path.substring(0, path.lastIndexOf("/") + 1)

        val autoScrollPos = if (!searchMode() && firstVisiblePositionStack.isNotEmpty()) {
            firstVisiblePositionStack.pop()
        } else {
            -1
        }

        navigateTo(path, titleFromPath(path), autoScrollPos)

        return BACK_PRESS_HANDLED
    }

    private fun titleFromPath(path: String): String {
        val query = searchQuery

        return when {
            query != null -> {
                context.getString(R.string.action_search) + ": " + query
            }
            path == OFFLINE_ROOT || path == "" -> {
                context.getString(R.string.section_saved_for_offline_new)
            }
            else -> {
                val pathWithoutLastSlash = path.substring(0, path.length - 1)
                pathWithoutLastSlash.substring(
                    pathWithoutLastSlash.lastIndexOf("/") + 1, pathWithoutLastSlash.length
                )
            }
        }
    }

    private fun navigateTo(path: String, title: String, autoScrollPos: Int = -1) {
        this.path = path
        _pathLiveData.value = path
        _actionBarTitle.value = title
        loadOfflineNodes(autoScrollPos = autoScrollPos)
    }

    fun setOrder(order: Int) {
        if (!rootFolderOnly) {
            this.order = order
            loadOfflineNodes()
        }
    }

    fun setSearchQuery(query: String?) {
        searchQuery = query
        loadOfflineNodes(autoScrollPos = if (query == null) 0 else -1)
    }

    fun onSearchQuerySubmitted() {
        _actionBarTitle.value = titleFromPath(path)
    }

    fun clearEmptySearchQuery() {
        if (searchQuery == "") {
            searchQuery = null
        }
    }

    fun searchMode() = searchQuery != null || historySearchQuery != null

    fun setDisplayParam(
        rootFolderOnly: Boolean,
        isList: Boolean,
        spanCount: Int,
        path: String,
        order: Int,
    ) {
        Timber.d("setDisplayParam rootFolderOnly $rootFolderOnly, isList $isList")

        this.rootFolderOnly = rootFolderOnly
        this.isList = isList
        gridSpanCount = spanCount
        this.path = path

        if (!rootFolderOnly) {
            this.order = order
        }

        _actionBarTitle.value = titleFromPath(path)
        loadOfflineNodes()
    }

    fun refreshActionBarTitle() {
        val title = _actionBarTitle.value

        if (title != null) {
            _actionBarTitle.value = title
        }
    }

    fun processUrlFile(file: File) {
        add(
            Maybe.fromCallable { return@fromCallable getURLOfflineFileContent(file) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _urlFileOpenAsUrl.value = Event(it) },
                    { logErr("processUrlFile") },
                    { _urlFileOpenAsFile.value = Event(file) }
                )
        )
    }

    fun loadOfflineNodes(refreshUi: Boolean = true, autoScrollPos: Int = -1) {
        if (path == "") {
            return
        }

        loadOfflineNodesJob?.cancel()
        loadOfflineNodesJob = viewModelScope.launch(defaultDispatcher) {
            val nodes = ArrayList<OfflineNode>()
            val nodesWithoutThumbnail = ArrayList<MegaOffline>()
            var folderCount = 0
            repo.loadOfflineNodes(path, order, searchQuery).map { node ->
                if (node.isFolder) {
                    folderCount++
                }
                val thumbnail = kotlin.runCatching {
                    getThumbnail(node.handle.toLong())
                }.fold(
                    onSuccess = { it },
                    onFailure = { null }
                )
                nodes.add(
                    OfflineNode(
                        node, if (isFileAvailable(thumbnail)) thumbnail else null,
                        getNodeInfo(node), selectedNodes.containsKey(node.id), refreshUi
                    )
                )

                val mime = typeForName(node.name)
                if ((mime.isVideo || mime.isImage || mime.isPdf || mime.isAudio) &&
                    !isFileAvailable(thumbnail) && !creatingThumbnailNodes.contains(node.handle)
                ) {
                    creatingThumbnailNodes.add(node.handle)
                    nodesWithoutThumbnail.add(node)
                }
            }

            var placeholders = 0
            if (!isList && gridSpanCount != 0) {
                placeholders = if (folderCount % gridSpanCount == 0) {
                    0
                } else {
                    gridSpanCount - (folderCount % gridSpanCount)
                }
                if (placeholders != 0) {
                    for (i in 0 until placeholders) {
                        nodes.add(folderCount + i, OfflineNode.PLACE_HOLDER)
                    }
                }
            }
            placeholderCount = placeholders
            withContext(mainDispatcher) {
                _nodes.value = Pair(nodes, autoScrollPos)
            }
        }
        loadOfflineNodesJob?.start()
    }

    private fun getNodeInfo(node: MegaOffline): String {
        val file = getOfflineFile(context, node)

        return if (file.isDirectory) {
            getFileFolderInfo(file)
        } else {
            getFileInfo(file)
        }
    }
}
