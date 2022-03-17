package mega.privacy.android.app.fragments.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

@HiltViewModel
class ActionModeViewModel @Inject constructor() : ViewModel() {

    // The full set of nodes
    private lateinit var nodesData: List<NodeItem>

    private var uiDirty: Boolean = true
    val deselectedNodeIndices = mutableSetOf<Int>()

    // Notify the fragment that the user intends to enter action mode by long click
    private val _longClick = MutableLiveData<Event<NodeItem>>()
    val longClick: LiveData<Event<NodeItem>> = _longClick

    // All nodes have been selected so far
    private val _selectedNodes = MutableLiveData<List<NodeItem>>()
    val selectedNodes: LiveData<List<NodeItem>> = _selectedNodes

    // Nodes should play the animation for this time of selection
    private val _animNodeIndices = MutableLiveData<Set<Int>>()
    val animNodeIndices: LiveData<Set<Int>> = _animNodeIndices

    private val _actionModeDestroy = MutableLiveData<Event<Unit>>()
    val actionModeDestroy: LiveData<Event<Unit>> = _actionModeDestroy

    private val selectedNodeList = mutableListOf<NodeItem>()

    fun onNodeClick(nodeItem: NodeItem) = updateSelectedNodeList(nodeItem)

    fun onNodeLongClick(nodeItem: NodeItem): Boolean {
        _longClick.value = Event(nodeItem)
        return true
    }

    fun enterActionMode(nodeItem: NodeItem) = updateSelectedNodeList(nodeItem)

    private fun updateSelectedNodeList(nodeItem: NodeItem) {
        nodeItem.selected = !nodeItem.selected
        nodeItem.uiDirty = uiDirty

        _animNodeIndices.value = hashSetOf(nodeItem.index)

        if (nodeItem.selected) {
            selectedNodeList.add(nodeItem)
        } else {
            selectedNodeList.remove(nodeItem)
            deselectedNodeIndices.add(nodeItem.index)
        }

        _selectedNodes.value = selectedNodeList
    }

    fun clearSelection() {
        if (selectedNodeList.isEmpty()) return
        val animNodeIndices = mutableSetOf<Int>()

        selectedNodeList.forEach {
            it.selected = false
            it.uiDirty = uiDirty
            animNodeIndices.add(it.index)
            deselectedNodeIndices.add(it.index)
        }

        _animNodeIndices.value = animNodeIndices
        selectedNodeList.clear()
        _selectedNodes.value = selectedNodeList
    }

    fun selectAll() {
        val animNodeIndices = mutableSetOf<Int>()

        nodesData.forEach {
            if (!it.selected) {
                it.selected = true
                it.uiDirty = uiDirty
                animNodeIndices.add(it.index)
            }
        }

        _animNodeIndices.value = animNodeIndices
        selectedNodeList.clear()
        selectedNodeList.addAll(nodesData)
        _selectedNodes.value = selectedNodeList
    }

    fun actionModeDestroy() {
        _actionModeDestroy.value = Event(Unit)
    }

    /**
     * Set the full range of the nodes
     */
    fun setNodesData(nodeItems: List<NodeItem>) {
        nodesData = nodeItems
        initSelectedNodeList()
    }

    private fun initSelectedNodeList() {
        // Some selected nodes may have been removed(e.g. by another Mega client),
        // so refresh selectedNodeList
        selectedNodeList.clear()
        nodesData.forEach {
            if (it.selected) {
                selectedNodeList.add(it)
            }
        }
        _selectedNodes.value = selectedNodeList
    }

    /**
     * Set the boolean value for the NodeItems
     */
    fun setUIDirty(uiDirty: Boolean) {
        this.uiDirty = uiDirty
    }

    /**
     * Set all the Favourite nodes to unFavourite
     */
    fun removeFavourites(megaApi: MegaApiAndroid, nodeItems: List<MegaNode> ) {
        nodeItems.forEach {
                megaApi.setNodeFavourite(it,false)
        }
    }
}