package mega.privacy.android.feature.sync.folderpicker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.backup.RemoveDeviceFolderConnectionUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.megapicker.MonitorMegaPickerFolderNodesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.TryNodeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.ui.formatter.FolderConflictMessageFormatter
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerHandler
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerNodesResult
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerRestrictedNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [SyncFolderPickerHandler], delegating to the sync domain logic
 * already used by the MEGA folder picker of the sync feature.
 */
internal class SyncFolderPickerHandlerImpl @Inject constructor(
    private val monitorMegaPickerFolderNodesUseCase: MonitorMegaPickerFolderNodesUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase: IsFolderUsedBySyncOrBackupAcrossDevicesUseCase,
    private val folderConflictMessageFormatter: FolderConflictMessageFormatter,
    private val tryNodeSyncUseCase: TryNodeSyncUseCase,
    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper,
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
    private val setSelectedMegaFolderUseCase: SetSelectedMegaFolderUseCase,
    private val removeDeviceFolderConnectionUseCase: RemoveDeviceFolderConnectionUseCase,
) : SyncFolderPickerHandler {

    override fun monitorPickerNodes(
        folderId: NodeId,
        isStopBackup: Boolean,
        stopBackupFolderName: String?,
    ): Flow<SyncFolderPickerNodesResult> = flow {
        val rootFolder = runCatching { getRootNodeUseCase() }
            .onFailure { Timber.e(it, "Error getting root folder") }
            .getOrNull()
        val currentFolder = runCatching { getNodeByHandleUseCase(folderId.longValue) }
            .onFailure { Timber.e(it, "Error getting current folder") }
            .getOrNull() ?: return@flow
        emitAll(
            monitorMegaPickerFolderNodesUseCase(
                currentFolder,
                rootFolder?.id,
                isStopBackup,
                stopBackupFolderName,
            ).map { result ->
                SyncFolderPickerNodesResult(
                    restrictedNodes = result.nodes
                        .filter { it.isDisabled }
                        .associate { info ->
                            info.node.id to SyncFolderPickerRestrictedNode(
                                nodeId = info.node.id,
                                name = info.node.name,
                                isUsedBySyncOrBackup = info.isUsedBySyncOrBackup,
                                backupId = info.backupId,
                                deviceName = info.deviceName,
                            )
                        },
                    isSelectEnabled = result.isSelectEnabled,
                )
            }
        )
    }

    override suspend fun getFolderUsageConflictMessage(folderId: NodeId): String? {
        val folderUsageResult = runCatching {
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId = folderId,
                isSyncFolderSelection = true,
                shouldExcludeCurrentDevice = false,
                useCache = false,
            )
        }.getOrNull() ?: FolderUsageResult.NotUsed

        val folderName = runCatching { getNodeByHandleUseCase(folderId.longValue)?.name }
            .getOrNull().orEmpty()

        return folderConflictMessageFormatter.formatFromFolderUsage(
            folderDisplayName = folderName,
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = folderUsageResult,
        )
    }

    override suspend fun validateNodeSyncability(folderId: NodeId): Int? = runCatching {
        tryNodeSyncUseCase(folderId)
    }.fold(
        onSuccess = { null },
        onFailure = {
            val error = (it as? MegaSyncException)?.syncError ?: SyncError.UNKNOWN_ERROR
            deviceFolderUINodeErrorMessageMapper(error)
                ?: deviceFolderUINodeErrorMessageMapper(SyncError.UNKNOWN_ERROR)
                ?: sharedR.string.general_text_error
        },
    )

    override suspend fun folderNameExists(parentId: NodeId, folderName: String): Boolean =
        nodeExistsInCurrentLocationUseCase(parentId, folderName)

    override suspend fun saveSelectedFolder(folderId: NodeId): Boolean {
        val node = runCatching { getNodeByHandleUseCase(folderId.longValue) }
            .onFailure { Timber.e(it, "Error getting selected folder") }
            .getOrNull() ?: return false
        setSelectedMegaFolderUseCase(RemoteFolder(node.id, node.name))
        return true
    }

    override suspend fun removeFolderConnection(backupId: Long) {
        removeDeviceFolderConnectionUseCase(backupId)
    }
}
