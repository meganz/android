package mega.privacy.android.app.fragments.offline

import android.util.Base64
import androidx.collection.SparseArrayCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.repo.MegaNodeRepo
import mega.privacy.android.app.utils.FileUtils.isFileAvailable
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.SingleLiveEvent
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop
import mega.privacy.android.app.utils.TimeUtils.formatLongDateTime
import mega.privacy.android.app.utils.Util.getSizeString
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaUtilsAndroid.createThumbnail
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.TimeUnit.SECONDS

class OfflineViewModel @ViewModelInject constructor(
    private val repo: MegaNodeRepo
) : BaseRxViewModel() {
    private var order = ORDER_DEFAULT_ASC

    private val openNodeAction = PublishSubject.create<Pair<Int, OfflineNode>>()
    private val openFolderFullscreenAction = PublishSubject.create<String>()
    private val showOptionsPanelAction = PublishSubject.create<MegaOffline>()

    private val _nodes = MutableLiveData<List<OfflineNode>>()
    private val _nodeToOpen = SingleLiveEvent<Pair<Int, OfflineNode>>()
    private val _actionBarTitle = MutableLiveData<String>()
    private val _actionMode = SingleLiveEvent<Boolean>()
    private val _nodeToAnimate = SingleLiveEvent<Pair<Int, OfflineNode>>()
    private val _pathLiveData = SingleLiveEvent<String>()
    private val _openFolderFullscreen = SingleLiveEvent<String>()
    private val _showOptionsPanel = SingleLiveEvent<MegaOffline>()
    private val _urlFileOpenAsUrl = SingleLiveEvent<String>()
    private val _urlFileOpenAsFile = SingleLiveEvent<File>()

    private val creatingThumbnailNodes = HashSet<String>()
    private val selectedNodes: SparseArrayCompat<MegaOffline> = SparseArrayCompat(5)
    private var rootFolderOnly = false
    private var isList = true
    private var gridSpanCount = 2

    val nodes: LiveData<List<OfflineNode>> = _nodes
    val nodeToOpen: LiveData<Pair<Int, OfflineNode>> = _nodeToOpen
    val actionBarTitle: LiveData<String> = _actionBarTitle
    val actionMode: LiveData<Boolean> = _actionMode
    val nodeToAnimate: LiveData<Pair<Int, OfflineNode>> = _nodeToAnimate
    val pathLiveData: LiveData<String> = _pathLiveData
    val openFolderFullscreen: LiveData<String> = _openFolderFullscreen
    val showOptionsPanel: LiveData<MegaOffline> = _showOptionsPanel
    val urlFileOpenAsUrl: LiveData<String> = _urlFileOpenAsUrl
    val urlFileOpenAsFile: LiveData<File> = _urlFileOpenAsFile

    var path = "/"
        private set
    var selecting = false
        private set
    var placeholderCount = 0
        private set

    init {
        add(
            openNodeAction.throttleFirst(1, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer { _nodeToOpen.value = it },
                    logErr("OfflineViewModel openNodeAction")
                )
        )
        add(
            openFolderFullscreenAction.throttleFirst(1, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer { _openFolderFullscreen.value = it },
                    logErr("OfflineViewModel openFolderFullscreenAction")
                )
        )
        add(
            showOptionsPanelAction.throttleFirst(1, SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer { _showOptionsPanel.value = it },
                    logErr("OfflineViewModel showOptionsPanelAction")
                )
        )
    }

    fun buildSectionTitle(
        nodes: List<OfflineNode>,
        isList: Boolean,
        spanCount: Int
    ): Map<Int, String> {
        val sections = HashMap<Int, String>()
        var folderCount = 0
        var fileCount = 0
        for (node in nodes) {
            if (node.node.isFolder) {
                folderCount++
            } else {
                fileCount++
            }
        }

        placeholderCount = 0
        val res = getApplication<MegaApplication>().resources
        if (isList) {
            sections[0] = res.getString(R.string.general_folders)
            sections[folderCount] = res.getString(R.string.general_files)
        } else {
            if (folderCount > 0) {
                for (i in 0 until spanCount) {
                    sections[i] = res.getString(R.string.general_folders)
                }
            }
            if (fileCount > 0) {
                placeholderCount =
                    if (folderCount % spanCount == 0) 0 else spanCount - (folderCount % spanCount)
                if (placeholderCount == 0) {
                    for (i in 0 until spanCount) {
                        sections[folderCount + i] = res.getString(R.string.general_files)
                    }
                } else {
                    for (i in 0 until spanCount) {
                        sections[folderCount + placeholderCount + i] =
                            res.getString(R.string.general_files)
                    }
                }
            }
        }
        return sections
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

    fun selectAll() {
        val nodeList = nodes.value ?: return

        for (node in nodeList) {
            node.selected = true
            selectedNodes.put(node.node.id, node.node)
        }
        selecting = true
        _nodes.value = nodeList
        _actionMode.value = true
    }

    fun clearSelection() {
        if (!selecting) {
            return
        }

        selecting = false
        _actionMode.value = false
        selectedNodes.clear()

        val nodeList = nodes.value ?: return
        for (node in nodeList) {
            node.selected = false
        }
        _nodes.value = nodeList
    }

    fun onNodeClicked(position: Int, node: OfflineNode) {
        if (selecting) {
            handleSelection(position, node)
        } else {
            val nodeFile = getOfflineFile(getApplication(), node.node)
            if (isFileAvailable(nodeFile) && nodeFile.isDirectory) {
                navigateIn(node.node)
            } else if (isFileAvailable(nodeFile) && nodeFile.isFile) {
                openNodeAction.onNext(Pair(position, node))
            }
        }
    }

    fun onNodeLongClicked(position: Int, node: OfflineNode) {
        if (!rootFolderOnly) {
            selecting = true
        }
        onNodeClicked(position, node)
    }

    fun onNodeOptionsClicked(position: Int, node: OfflineNode) {
        if (selecting) {
            onNodeClicked(position, node)
        } else {
            showOptionsPanelAction.onNext(node.node)
        }
    }

    private fun handleSelection(position: Int, node: OfflineNode) {
        val nodes = _nodes.value
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
        selecting = !selectedNodes.isEmpty
        _actionMode.value = selecting

        _nodeToAnimate.value = Pair(position, node)
    }

    private fun navigateIn(folder: MegaOffline) {
        if (rootFolderOnly) {
            _pathLiveData.value = folder.path + folder.name + "/"
            openFolderFullscreenAction.onNext(_pathLiveData.value)
        } else {
            navigateTo(folder.path + folder.name + "/", folder.name)
        }
    }

    fun navigateOut(): Int {
        if (path == "/") {
            return 0
        }

        path = path.substring(0, path.length - 1)
        path = path.substring(0, path.lastIndexOf("/") + 1)
        navigateTo(path, titleFromPath(path))
        return 2
    }

    private fun titleFromPath(path: String): String {
        return if (path == "/") {
            getApplication<MegaApplication>().getString(R.string.tab_offline)
                .toUpperCase(Locale.ROOT)
        } else {
            val pathWithoutLastSlash = path.substring(0, path.length - 1)
            pathWithoutLastSlash.substring(
                pathWithoutLastSlash.lastIndexOf("/") + 1, pathWithoutLastSlash.length
            )
        }
    }

    private fun navigateTo(path: String, title: String) {
        this.path = path
        _pathLiveData.value = path
        _actionBarTitle.value = title
        loadOfflineNodes()
    }

    fun setOrder(order: Int) {
        this.order = order
        loadOfflineNodes()
    }

    fun setDisplayParam(rootFolderOnly: Boolean, isList: Boolean, spanCount: Int, path: String) {
        this.rootFolderOnly = rootFolderOnly
        this.isList = isList
        gridSpanCount = spanCount
        this.path = path

        _actionBarTitle.value = titleFromPath(path)
        loadOfflineNodes()
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
                    _urlFileOpenAsUrl.value = it
                } else {
                    _urlFileOpenAsFile.value = file
                }
            }, logErr("processUrlFile"))
        )
    }

    fun loadOfflineNodes() {
        add(Single.fromCallable { repo.loadOfflineNodes(path, order) }
            .map {
                val nodes = ArrayList<OfflineNode>()
                val nodesWithoutThumbnail = ArrayList<MegaOffline>()
                var folderCount = 0
                for (node in it) {
                    if (node.isFolder) {
                        folderCount++
                    }
                    val thumbnail = getThumbnailFile(node)
                    nodes.add(
                        OfflineNode(
                            node, if (isFileAvailable(thumbnail)) thumbnail else null,
                            getNodeInfo(node), selectedNodes.containsKey(node.id)
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

                if (!isList && gridSpanCount != 0) {
                    val placeholderCount = if (folderCount % gridSpanCount == 0) {
                        0
                    } else {
                        gridSpanCount - (folderCount % gridSpanCount)
                    }
                    if (placeholderCount != 0) {
                        for (i in 0 until placeholderCount) {
                            nodes.add(folderCount + i, OfflineNode.PLACE_HOLDER)
                        }
                    }
                }

                createThumbnails(nodesWithoutThumbnail)
                nodes
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer { _nodes.value = it }, logErr("loadOfflineNodes"))
        )
    }

    private fun getNodeInfo(node: MegaOffline): String {
        val file = getOfflineFile(getApplication(), node)

        return if (file.isDirectory) {
            getFolderInfo(file)
        } else {
            String.format(
                "%s . %s",
                getSizeString(file.length()),
                formatLongDateTime(file.lastModified() / 1000)
            )
        }
    }

    private fun getFolderInfo(file: File): String {
        val files = file.listFiles() ?: return " "

        var folderNum = 0
        var fileNum = 0
        for (f in files) {
            if (f.isDirectory) {
                folderNum++
            } else {
                fileNum++
            }
        }
        val res = getApplication<MegaApplication>().resources

        return if (folderNum > 0 && fileNum > 0) {
            "$folderNum " + res.getQuantityString(R.plurals.general_num_folders, folderNum) +
                    ", $fileNum " + res.getQuantityString(R.plurals.general_num_files, folderNum)
        } else if (folderNum > 0) {
            "$folderNum " + res.getQuantityString(R.plurals.general_num_folders, folderNum)
        } else {
            "$fileNum " + res.getQuantityString(R.plurals.general_num_files, fileNum)
        }
    }

    private fun createThumbnails(nodes: List<MegaOffline>) {
        add(Observable.fromIterable(nodes)
            .subscribeOn(Schedulers.io())
            .filter {
                getOfflineFile(getApplication(), it).exists()
            }
            .map {
                createThumbnail(getOfflineFile(getApplication(), it), getThumbnailFile(it))
            }
            .throttleFirst(1, SECONDS)
            .subscribe(Consumer { loadOfflineNodes() }, logErr("createThumbnail"))
        )
    }

    private fun getThumbnailFile(node: MegaOffline): File {
        val thumbDir = ThumbnailUtilsLollipop.getThumbFolder(getApplication())
        val thumbName = Base64.encodeToString(node.handle.toByteArray(), Base64.DEFAULT)
        return File(thumbDir, "$thumbName.jpg")
    }
}
