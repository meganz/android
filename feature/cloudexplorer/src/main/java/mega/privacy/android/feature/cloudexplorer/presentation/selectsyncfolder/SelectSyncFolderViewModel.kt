package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder.SelectSyncFolderViewModel.Companion.INVALID_FOLDER_HANDLE
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerHandler
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerRestrictedNode
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import timber.log.Timber

/**
 * ViewModel applying the sync specific logic of the MEGA folder picker to the cloud explorer,
 * both when selecting the remote folder of a new sync and when selecting the destination of a
 * stopped backup.
 */
@HiltViewModel(assistedFactory = SelectSyncFolderViewModel.Factory::class)
internal class SelectSyncFolderViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val syncFolderPickerHandler: SyncFolderPickerHandler,
    val syncPermissionsManager: SyncPermissionsManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: Args): SelectSyncFolderViewModel
    }

    /**
     * @property folderHandle Handle of the folder to display, or [INVALID_FOLDER_HANDLE] to resolve
     * and display the cloud drive root.
     * @property isStopBackup Whether the picker is selecting the destination of a stopped backup.
     * @property stopBackupFolderName Name of the backup folder being moved, used to detect a name
     * clash in the destination (stop backup flow only).
     */
    data class Args(
        val folderHandle: Long,
        val isStopBackup: Boolean,
        val stopBackupFolderName: String?,
    )

    private val disableBatteryOptimizationsEventChannel = Channel<StateEvent>(Channel.BUFFERED)
    private val warningEventChannel = Channel<StateEventWithContent<LocalizedText>>(Channel.BUFFERED)
    private val folderConfirmedEventChannel = Channel<StateEvent>(Channel.BUFFERED)

    /** VM-controlled signals that affect the view but are not one-shot events. */
    private data class SelectionState(
        val removeConnectionNode: SyncFolderPickerRestrictedNode? = null,
        val isProcessing: Boolean = false,
    )

    private val selectionState = MutableStateFlow(SelectionState())

    private var disableBatteryOptimizationsHandled = false

    /**
     * Resolves the displayed folder (the cloud drive root for the entry screen) and monitors its
     * children to know which are restricted and whether the folder itself can be selected.
     */
    private val pickerNodesFlow: Flow<SyncFolderPickerNodes> = flow {
        val folderId = if (args.folderHandle == INVALID_FOLDER_HANDLE) {
            runCatching { getRootNodeIdUseCase() }
                .onFailure { Timber.e(it, "Error getting root folder") }
                .getOrNull()
        } else {
            NodeId(args.folderHandle)
        } ?: return@flow

        emit(SyncFolderPickerNodes(currentFolderId = folderId))
        emitAll(
            syncFolderPickerHandler.monitorPickerNodes(
                folderId = folderId,
                isStopBackup = args.isStopBackup,
                stopBackupFolderName = args.stopBackupFolderName,
            ).map { result ->
                SyncFolderPickerNodes(
                    currentFolderId = folderId,
                    restrictedNodes = result.restrictedNodes,
                    isSelectEnabled = result.isSelectEnabled,
                )
            }
        )
    }.catch { Timber.e(it, "Error monitoring picker nodes") }

    val uiState: StateFlow<SelectSyncFolderUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            pickerNodesFlow,
            selectionState,
            disableBatteryOptimizationsEventChannel.receiveAsFlow().onStart { emit(consumed) },
            warningEventChannel.receiveAsFlow().onStart { emit(consumed()) },
            folderConfirmedEventChannel.receiveAsFlow().onStart { emit(consumed) },
        ) { picker, selection, batteryEvent, warningEvent, folderConfirmedEvent ->
            SelectSyncFolderUiState.Data(
                currentFolderId = picker.currentFolderId,
                restrictedNodes = picker.restrictedNodes,
                isSelectEnabled = picker.isSelectEnabled,
                removeConnectionNode = selection.removeConnectionNode,
                isProcessing = selection.isProcessing,
                disableBatteryOptimizationsEvent = batteryEvent,
                warningEvent = warningEvent,
                folderConfirmedEvent = folderConfirmedEvent,
            )
        }.catch { Timber.e(it, "Error building sync folder picker state") }
            .asUiStateFlow(viewModelScope, SelectSyncFolderUiState.Loading)
    }

    fun handleAction(action: SelectSyncFolderAction) {
        when (action) {
            SelectSyncFolderAction.CurrentFolderSelected -> selectCurrentFolder()

            SelectSyncFolderAction.DisableBatteryOptimizationsHandled -> {
                disableBatteryOptimizationsHandled = true
            }

            is SelectSyncFolderAction.RestrictedFolderClicked ->
                onRestrictedFolderClicked(action.nodeId)

            SelectSyncFolderAction.RemoveConnectionConfirmed ->
                removeFolderConnection()

            SelectSyncFolderAction.RemoveConnectionNodeConsumed ->
                selectionState.update { it.copy(removeConnectionNode = null) }

            SelectSyncFolderAction.DisableBatteryOptimizationsEventConsumed ->
                disableBatteryOptimizationsEventChannel.trySend(consumed)

            SelectSyncFolderAction.WarningEventConsumed ->
                warningEventChannel.trySend(consumed())

            SelectSyncFolderAction.FolderConfirmedEventConsumed ->
                folderConfirmedEventChannel.trySend(consumed)
        }
    }

    private fun onRestrictedFolderClicked(nodeId: NodeId) {
        // Only ask to remove the connection when the folder has a removable backup (other device).
        val restrictedNode = (uiState.value as? SelectSyncFolderUiState.Data)
            ?.restrictedNodes?.get(nodeId)
        if (restrictedNode?.backupId != null) {
            selectionState.update { it.copy(removeConnectionNode = restrictedNode) }
        }
    }

    private fun selectCurrentFolder() {
        viewModelScope.launch {
            val folderId = (uiState.value as? SelectSyncFolderUiState.Data)?.currentFolderId
                ?: return@launch

            selectionState.update { it.copy(isProcessing = true) }

            // Skip sync/backup validation for stop backup flow - user is just picking a move
            // destination
            if (!args.isStopBackup) {
                val conflictMessage = runCatching {
                    syncFolderPickerHandler.getFolderUsageConflictMessage(folderId)
                }.onFailure { Timber.e(it, "Error checking folder usage") }
                    .getOrNull()
                if (conflictMessage != null) {
                    emitWarning(LocalizedText.Literal(conflictMessage))
                    return@launch
                }
            }

            if (syncPermissionsManager.isDisableBatteryOptimizationGranted()
                || !getFeatureFlagValueUseCase(SyncFeatures.DisableBatteryOptimization)
            ) {
                disableBatteryOptimizationsHandled = true
            }

            if (args.isStopBackup) {
                val folderExists = runCatching {
                    args.stopBackupFolderName
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { syncFolderPickerHandler.folderNameExists(folderId, it) }
                        ?: false
                }.onFailure { Timber.e(it, "Error checking if folder exists") }
                    .getOrDefault(false)
                Timber.d("Folder exists: $folderExists")
                if (folderExists) {
                    emitWarning(
                        LocalizedText.StringRes(
                            sharedR.string.create_new_folder_dialog_error_existing_folder
                        )
                    )
                    return@launch
                }
            } else {
                val errorMessageRes = syncFolderPickerHandler.validateNodeSyncability(folderId)
                if (errorMessageRes != null) {
                    emitWarning(LocalizedText.StringRes(errorMessageRes))
                    return@launch
                }
            }
            folderSelected(folderId)
        }
    }

    private suspend fun folderSelected(folderId: NodeId) {
        Timber.d("Folder selected, id: $folderId")
        if (disableBatteryOptimizationsHandled) {
            val saved = runCatching { syncFolderPickerHandler.saveSelectedFolder(folderId) }
                .onFailure { Timber.e(it, "Error saving selected folder") }
                .getOrDefault(false)
            if (saved) {
                folderConfirmedEventChannel.send(triggered)
            } else {
                emitWarning(LocalizedText.StringRes(sharedR.string.general_text_error))
            }
        } else {
            disableBatteryOptimizationsEventChannel.send(triggered)
        }
    }

    /** Surfaces a warning to the user and releases the UI blocked during the selection. */
    private suspend fun emitWarning(message: LocalizedText) {
        selectionState.update { it.copy(isProcessing = false) }
        warningEventChannel.send(triggered(message))
    }

    private fun removeFolderConnection() {
        val backupId = selectionState.value.removeConnectionNode?.backupId
        if (backupId == null) {
            Timber.w("RemoveConnectionConfirmed received but no removable node is stored")
            selectionState.update { it.copy(removeConnectionNode = null) }
            return
        }
        viewModelScope.launch {
            runCatching {
                syncFolderPickerHandler.removeFolderConnection(backupId)
            }.onSuccess {
                selectionState.update { it.copy(removeConnectionNode = null) }
                warningEventChannel.send(
                    triggered(
                        LocalizedText.StringRes(
                            sharedR.string.device_center_snackbar_message_connection_removed
                        )
                    )
                )
            }.onFailure {
                Timber.e(it, "Failed to remove folder connection")
                selectionState.update { it.copy(removeConnectionNode = null) }
                warningEventChannel.send(
                    triggered(LocalizedText.StringRes(sharedR.string.general_text_error))
                )
            }
        }
    }

    companion object {
        /**
         * Sentinel folder handle telling the picker to resolve and display the cloud drive root.
         */
        const val INVALID_FOLDER_HANDLE = -1L
    }
}
