package mega.privacy.android.app.fragments.homepage.audio

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.TypedFilesRepository
import mega.privacy.android.app.fragments.homepage.nodesChange
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC

class AudioViewModel @ViewModelInject constructor(
    private val repository: TypedFilesRepository
) : ViewModel() {

    private var _query = MutableLiveData<String>("")

    var order: Int = ORDER_DEFAULT_ASC
        private set
    var searchMode = false
    var listMode = true   // false for grid mode
    var searchQuery = ""

    private var forceUpdate = false
    private var ignoredFirst = false

    val items: LiveData<List<NodeItem>> = _query.switchMap {
        if (forceUpdate) {
            viewModelScope.launch {
                repository.getFiles(MegaApiJava.NODE_AUDIO, order)
            }
        } else {
            repository.emitFiles()
        }

        repository.fileNodeItems
    }.map { nodes ->
        var index = 0
        var filteredNodes = nodes

        if (!TextUtil.isTextEmpty(_query.value)) {
            filteredNodes = nodes.filter {
                it.node?.name?.contains(
                    _query.value!!,
                    true
                ) ?: false
            }
        }

        filteredNodes.forEach {
            it.index = index++
        }

        filteredNodes
    }

    private val nodesChangeObserver = Observer<Boolean> {
        if (!ignoredFirst) {
            ignoredFirst = true
            return@Observer
        }

        if (it) {
            loadAudio(true)
        } else {
            refreshUi()
        }
    }

    init {
//        loadAudio(true, order)
        nodesChange.observeForever(nodesChangeObserver)
    }

    /**
     * Load audio by calling Mega Api or just filter loaded nodes
     * @param forceUpdate True if retrieve all nodes by calling API
     * , false if filter current nodes by searchQuery
     */
    fun loadAudio(forceUpdate: Boolean = false, order: Int = this.order) {
        this.forceUpdate = forceUpdate
        this.order = order
        _query.value = searchQuery
    }

    /**
     * Make the list adapter to rebind all item views with data since
     * the underlying data may have been changed.
     */
    fun refreshUi() {
        items.value?.forEach { item ->
            item.uiDirty = true
        }
        loadAudio()
    }

    fun shouldShowSearchMenu() = items.value?.isNotEmpty() ?: false

    fun getNodePositionByHandle(handle: Long): Int {
        return items.value?.find {
            it.node?.handle == handle
        }?.index ?: INVALID_POSITION
    }

    fun getHandlesOfAudio(): LongArray? {
        val list = items.value?.map { node -> node.node?.handle ?: INVALID_HANDLE }

        return list?.toLongArray()
    }

    override fun onCleared() {
        nodesChange.removeObserver(nodesChangeObserver)
    }
}
