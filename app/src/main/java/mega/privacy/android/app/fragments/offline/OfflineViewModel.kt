package mega.privacy.android.app.fragments.offline

import android.content.Context
import android.content.Intent
import androidx.collection.SparseArrayCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.saver.OfflineNodeSaver
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.repo.MegaNodeRepo
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.OfflineUtils.*
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.TimeUtils.formatLongDateTime
import mega.privacy.android.app.utils.Util.getSizeString
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaUtilsAndroid.createThumbnail
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

class OfflineViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
    private val repo: MegaNodeRepo,
    private val nodeSaver: OfflineNodeSaver
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
    private val _actionBarTitle = MutableLiveData<String>()
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
    val actionBarTitle: LiveData<String> = _actionBarTitle
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

    init {
        add(
            openNodeAction.throttleFirst(1, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer { _nodeToOpen.value = Event(it) },
                    logErr("OfflineViewModel openNodeAction")
                )
        )
        add(
            openFolderFullscreenAction.throttleFirst(1, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer { _openFolderFullscreen.value = Event(it) },
                    logErr("OfflineViewModel openFolderFullscreenAction")
                )
        )
        add(
            showOptionsPanelAction.throttleFirst(1, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer { _showOptionsPanel.value = Event(it) },
                    logErr("OfflineViewModel showOptionsPanelAction")
                )
        )
    }

    override fun onCleared() {
        super.onCleared()
        nodeSaver.destroy()
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

            if (isFileAvailable(nodeFile) && nodeFile.isDirectory) {
                navigateIn(node.node, firstVisiblePosition)
            } else if (isFileAvailable(nodeFile) && nodeFile.isFile) {
                openNodeAction.onNext(Pair(position, node))
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
            logDebug("navigateOut exit search mode")
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
                logDebug("navigateOut from searchPath")
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
                context.getString(R.string.action_search).toUpperCase(Locale.ROOT) + ": " + query
            }
            path == OFFLINE_ROOT || path == "" -> {
                context.getString(R.string.section_saved_for_offline_new).toUpperCase(Locale.ROOT)
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
        order: Int
    ) {
        logDebug("setDisplayParam rootFolderOnly $rootFolderOnly, isList $isList")

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

    fun saveNodeToDevice(node: MegaOffline, activityStarter: (Intent, Int) -> Unit) {
        nodeSaver.save(node, false, activityStarter)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return nodeSaver.handleActivityResult(requestCode, resultCode, data)
    }

    fun processUrlFile(file: File) {
        add(Single
            .fromCallable {
                var reader: BufferedReader? = null
                try {
                    reader = BufferedReader(InputStreamReader(FileInputStream(file)))
                    if (reader.readLine() != null) {
                        val line2 = reader.readLine()
                        return@fromCallable line2.replace("URL=", "")
                    }
                } finally {
                    reader?.close()
                }

                return@fromCallable null
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer {
                if (it != null) {
                    _urlFileOpenAsUrl.value = Event(it)
                } else {
                    _urlFileOpenAsFile.value = Event(file)
                }
            }, logErr("processUrlFile"))
        )
    }

    fun loadOfflineNodes(refreshUi: Boolean = true, autoScrollPos: Int = -1) {
        if (path == "") {
            return
        }

        add(Single.fromCallable { repo.loadOfflineNodes(path, order, searchQuery) }
            .map {
                val nodes = ArrayList<OfflineNode>()
                val nodesWithoutThumbnail = ArrayList<MegaOffline>()
                var folderCount = 0
                for (node in it) {
                    if (node.isFolder) {
                        folderCount++
                    }
                    val thumbnail = getThumbnailFile(context, node)
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

                if (nodes.isNotEmpty() && !rootFolderOnly && !searchMode()) {
                    placeholders++
                    nodes.add(0, OfflineNode.HEADER)
                }
                placeholderCount = placeholders

                createThumbnails(nodesWithoutThumbnail)
                nodes
            }
            // loadOfflineNodes would be called multiple times when load fragment because of
            // observing different LiveData, use single thread scheduler to avoid concurrency issue
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                Consumer { _nodes.value = Pair(it, autoScrollPos) },
                logErr("loadOfflineNodes")
            )
        )
    }

    private fun getNodeInfo(node: MegaOffline): String {
        val file = getOfflineFile(context, node)

        return if (file.isDirectory) {
            getFolderInfo(context.resources, file)
        } else {
            String.format(
                "%s . %s",
                getSizeString(file.length()),
                formatLongDateTime(file.lastModified() / 1000)
            )
        }
    }

    private fun createThumbnails(nodes: List<MegaOffline>) {
        add(Observable.fromIterable(nodes)
            .subscribeOn(Schedulers.io())
            .map { Pair(getOfflineFile(context, it), getThumbnailFile(context, it)) }
            .filter { it.first.exists() }
            .map { createThumbnail(it.first, it.second) }
            .throttleLatest(1, SECONDS, true)
            .subscribe(Consumer { loadOfflineNodes() }, logErr("createThumbnail"))
        )
    }
}
