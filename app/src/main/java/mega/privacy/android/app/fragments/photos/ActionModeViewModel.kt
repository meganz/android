package mega.privacy.android.app.fragments.photos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

@FragmentScoped
class ActionModeViewModel @Inject constructor() : ViewModel() {

    private val _selectedNodes = MutableLiveData<List<SelectableNode>>()
    val selectedNodes: LiveData<List<SelectableNode>> = _selectedNodes

    private val _animNodeIndices = MutableLiveData<Set<Int>>()
    val animNodeIndices: LiveData<Set<Int>> = _animNodeIndices

    private val _selectAllEvent = MutableLiveData<Event<Unit>>()
    val selectAllEvent = _selectAllEvent

    private val _actionModeDestroy = MutableLiveData<Event<Unit>>()
    val actionModeDestroy = _actionModeDestroy

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

    fun trySelectAll() {
        _selectAllEvent.value = Event(Unit)
    }

    fun selectAll(nodes: List<SelectableNode>) {
        val animNodeIndices = mutableSetOf<Int>()

        nodes.forEach {
            if (!it.selected) {
                it.selected = true
                it.uiDirty = true
                animNodeIndices.add(it.index)
            }
        }

        _animNodeIndices.value = animNodeIndices
        selectedNodeList.clear()
        selectedNodeList.addAll(nodes)
        _selectedNodes.value = selectedNodeList
    }

    fun actionModeDestroy() {
        _actionModeDestroy.value = Event(Unit)
    }
}