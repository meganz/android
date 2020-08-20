package mega.privacy.android.app.fragments.photos

import androidx.appcompat.view.ActionMode
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class ActionModeViewModel @Inject constructor() : ViewModel() {
    // The full set of nodes
    private lateinit var nodesData: List<SelectableNode>

    // Which nodes have been selected so far
    private val _selectedNodes = MutableLiveData<List<SelectableNode>>()
    val selectedNodes: LiveData<List<SelectableNode>> = _selectedNodes

    // Which nodes should play the animation for this time of selection
    private val _animNodeIndices = MutableLiveData<Set<Int>>()
    val animNodeIndices: LiveData<Set<Int>> = _animNodeIndices

    private val _actionModeDestroy = MutableLiveData<Event<Unit>>()
    val actionModeDestroy: LiveData<Event<Unit>> = _actionModeDestroy

    private val selectedNodeList = mutableListOf<SelectableNode>()

    fun onPhotoClick(node: SelectableNode) {
        updateSelectedNodeList(node)
    }

    fun onPhotoLongClick(node: SelectableNode): Boolean {
        updateSelectedNodeList(node)
        return true
    }

    private fun updateSelectedNodeList(node: SelectableNode) {
        node.selected = !node.selected
        node.uiDirty = true

        _animNodeIndices.value = hashSetOf(node.index)
        if (node.selected) selectedNodeList.add(node) else selectedNodeList.remove(node)
        _selectedNodes.value = selectedNodeList
    }

    fun clearSelection() {
        if (selectedNodeList.isEmpty()) return
        val animNodeIndices = mutableSetOf<Int>()

        selectedNodeList.forEach {
            it.selected = false
            it.uiDirty = true
            animNodeIndices.add(it.index)
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
                it.uiDirty = true
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

    fun setNodesData(nodes: List<SelectableNode>) {
        nodesData = nodes

        // Some selected nodes may have been removed, so refresh selectedNodeList
        selectedNodeList.clear()
        nodesData.forEach {
            if (it.selected) {
                selectedNodeList.add(it)
            }
        }
        _selectedNodes.value = selectedNodeList
    }
}