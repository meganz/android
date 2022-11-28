package mega.privacy.android.app.presentation.clouddrive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import nz.mega.sdk.MegaNode
import javax.inject.Inject

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val monitorMediaDiscoveryView: MonitorMediaDiscoveryView
) : ViewModel() {

    private val _state = MutableStateFlow(MediaDiscoveryViewSettings.INITIAL.ordinal)

    /**
     * State flow
     */
    val state: StateFlow<Int> = _state

    init {
        viewModelScope.launch {
            monitorMediaDiscoveryView().collect { mediaDiscoveryViewSettings ->
                _state.update {
                    mediaDiscoveryViewSettings ?: MediaDiscoveryViewSettings.INITIAL.ordinal
                }
            }
        }
    }

    /**
     * If a folder only contains images or videos, then go to MD mode directly
     */
    fun shouldEnterMDMode(nodes: List<MegaNode>, mediaDiscoveryViewSettings: Int): Boolean {
        if (nodes.isEmpty())
            return false
        val isMediaDiscoveryEnable =
            mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.ENABLED.ordinal ||
                    mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.INITIAL.ordinal
        if (!isMediaDiscoveryEnable)
            return false

        for (node: MegaNode in nodes) {
            if (node.isFolder ||
                !MimeTypeList.typeForName(node.name).isImage &&
                !MimeTypeList.typeForName(node.name).isVideoReproducible
            ) {
                return false
            }
        }
        return true
    }
}