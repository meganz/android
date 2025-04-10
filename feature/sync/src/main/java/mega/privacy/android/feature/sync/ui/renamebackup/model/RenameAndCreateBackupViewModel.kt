package mega.privacy.android.feature.sync.ui.renamebackup.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.exception.BackupAlreadyExistsException
import mega.privacy.android.feature.sync.domain.usecase.sync.SyncFolderPairUseCase
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] containing all functionalities for [RenameAndCreateBackupDialog]
 */
@HiltViewModel
internal class RenameAndCreateBackupViewModel @Inject constructor(
    private val syncFolderPairUseCase: SyncFolderPairUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RenameAndCreateBackupState())

    /**
     * The State of [RenameAndCreateBackupDialog]
     */
    val state: StateFlow<RenameAndCreateBackupState> = _state.asStateFlow()

    /**
     * Renames and Creates the Backup
     *
     * @param newBackupName The new name for the Backup
     * @param localPath Path of the local folder to back up from the device
     */
    fun renameAndCreateBackup(
        newBackupName: String,
        localPath: String,
    ) = viewModelScope.launch {
        val trimmedName = newBackupName.trim()
        if (trimmedName.isBlank()) {
            _state.update { it.copy(errorMessage = sharedR.string.sync_rename_and_create_backup_dialog_error_message_empty_backup_name) }
        } else if (INVALID_CHARACTER_REGEX.toRegex().containsMatchIn(trimmedName)) {
            _state.update { it.copy(errorMessage = sharedR.string.general_invalid_characters_defined) }
        } else {
            runCatching {
                syncFolderPairUseCase(
                    syncType = SyncType.TYPE_BACKUP,
                    name = trimmedName,
                    localPath = localPath,
                    remotePath = RemoteFolder(NodeId(-1L), ""),
                )
            }.onSuccess {
                _state.update {
                    it.copy(
                        errorMessage = null,
                        successEvent = triggered
                    )
                }
            }.onFailure { exception ->
                when (exception) {
                    is BackupAlreadyExistsException -> _state.update { it.copy(errorMessage = sharedR.string.sync_rename_and_create_backup_dialog_error_message_name_already_exists) }
                    else -> Timber.e(exception)
                }
            }
        }
    }

    /**
     * Clears the Error Message from the Dialog
     */
    fun clearErrorMessage() = _state.update { it.copy(errorMessage = null) }

    /**
     * Notifies [RenameAndCreateBackupState.successEvent] that it has been consumed
     */
    fun resetSuccessfulEvent() = _state.update { it.copy(successEvent = consumed) }

    companion object {
        private const val INVALID_CHARACTER_REGEX = "[\\\\*/:<>?\"|]"
    }
}