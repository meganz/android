package mega.privacy.android.app.fragments.photos

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

@FragmentScoped
class ActionModeViewModel @Inject constructor() : ViewModel() {
    private val _selectedNodes = MutableLiveData<List<SelectableNode>>()
    val selectedNodes: LiveData<List<SelectableNode>> = _selectedNodes

    private val _selectAllEvent = MutableLiveData<Event<Unit>>()
    val selectAllEvent = _selectAllEvent

    private val selectedNodeList = mutableListOf<SelectableNode>()

    fun onPhotoClick(node: SelectableNode) {
        Log.i("Alex", "onClick:$this")
        updateSelectedNodeList(node)
    }

    fun onPhotoLongClick(node: SelectableNode): Boolean {
        updateSelectedNodeList(node)

//        if (_inActionMode.value!!.isNotEmpty()) {  // Already in action mode
//
//        } else {    // Enter action mode
//            _inActionMode.value = _inActionMode.value!!.
//        }

        Log.i("Alex", "onLongClick:$this")
        return true
    }

    private fun updateSelectedNodeList(node: SelectableNode) {
        node.selected = !node.selected
        if (node.selected) selectedNodeList.add(node) else selectedNodeList.remove(node)
        _selectedNodes.value = selectedNodeList
    }

    fun clearSelection() {
        selectedNodeList.clear()
        _selectedNodes.value = selectedNodeList
    }

    fun trySelectAll() {
        _selectAllEvent.value = Event(Unit)
    }

    fun selectAll(nodes: List<SelectableNode>) {
        nodes.forEach {
            it.selected = true
        }

        selectedNodeList.clear()
        selectedNodeList.addAll(nodes)
        _selectedNodes.value = selectedNodeList
    }
}