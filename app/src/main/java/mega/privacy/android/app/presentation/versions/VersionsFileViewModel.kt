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
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import javax.inject.Inject

/**
 * [ViewModel] class for [mega.privacy.android.app.main.VersionsFileActivity]
 *
 * @property isNodeInBackupsUseCase Checks whether the Node is a Backup Node or not
 */
@HiltViewModel
class VersionsFileViewModel @Inject constructor(
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
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
            val isNodeInBackups = isNodeInBackupsUseCase(nonNullHandle)
            _state.update { it.copy(isNodeInBackups = isNodeInBackups) }
        }
    }

    /**
     * Handles the display of the top-right "Delete" button
     *
     * @param selectedVersions The number of Versions selected during Long Press
     * @param isCurrentVersionSelected Whether the current Version was selected during Long Press
     * or not
     *
     * @return true if the Node is not in Backups or if the selected Version is a Previous Version.
     * false is returned if none of the conditions are met
     */
    fun showDeleteVersionsButton(selectedVersions: Int, isCurrentVersionSelected: Boolean) =
        if (selectedVersions <= 0) false
        else _state.value.isNodeInBackups.not() || isCurrentVersionSelected.not()

    /**
     * Handles the display of the top-right "Revert" button
     *
     * @param selectedVersions The number of Versions selected during Long Press
     * @param isCurrentVersionSelected Whether the current Version was selected during Long Press
     * or not
     *
     * @return true if there is only one previous Version selected and the Node is not in Backups.
     * Otherwise, false is returned
     */
    fun showRevertVersionButton(selectedVersions: Int, isCurrentVersionSelected: Boolean) =
        when {
            selectedVersions <= 0 -> false
            selectedVersions == 1 -> {
                _state.value.isNodeInBackups.not() && isCurrentVersionSelected.not()
            }

            else -> false
        }
}