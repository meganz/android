package mega.privacy.android.app.presentation.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.versions.model.VersionsFileState
import mega.privacy.android.domain.usecase.IsNodeInInbox
import javax.inject.Inject

/**
 * [ViewModel] class for [mega.privacy.android.app.main.VersionsFileActivity]
 *
 * @property isNodeInInbox Checks whether the Node is a Backup Node or not
 */
@HiltViewModel
class VersionsFileViewModel @Inject constructor(
    private val isNodeInInbox: IsNodeInInbox,
) : ViewModel() {

    private val _state = MutableStateFlow(VersionsFileState())

    /**
     * UI State of [mega.privacy.android.app.main.VersionsFileActivity]
     */
    val state: StateFlow<VersionsFileState> = _state.asStateFlow()

    /**
     * Initializes the ViewModel
     *
     * @param nodeHandle The handle of the Node with Versions
     */
    fun init(nodeHandle: Long?) = viewModelScope.launch {
        nodeHandle?.let { nonNullHandle ->
            val isNodeInBackups = isNodeInInbox(nonNullHandle)
            _state.update { it.copy(isNodeInBackups = isNodeInBackups) }
        }
    }

    /**
     * Handles the display of the top-right "Delete" button
     *
     * @param isCurrentVersionSelected Whether the current Version was selected during Long Press
     * or not
     *
     * @return Whether the top-right "Delete" button is shown or not
     */
    fun showDeleteVersionsButton(isCurrentVersionSelected: Boolean) =
        _state.value.isNodeInBackups.not() || isCurrentVersionSelected.not()
}