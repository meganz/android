package mega.privacy.android.feature.sync.domain.usecase.megapicker

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetTypedNodesFromFolderUseCase
import mega.privacy.android.domain.usecase.backup.GetBackupInfoUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdAndNameMapUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetFullNodePathByIdUseCase
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.feature.sync.domain.entity.megapicker.MegaPickerFolderResult
import mega.privacy.android.feature.sync.domain.entity.megapicker.MegaPickerNodeInfo
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncedNodeIdsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case that loads folder nodes for the Mega Picker with exclude/disabled and other-device
 * connection info.
 */
internal class MonitorMegaPickerFolderNodesUseCase @Inject constructor(
    private val getTypedNodesFromFolder: GetTypedNodesFromFolderUseCase,
    private val getCameraUploadsFolderHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getMediaUploadsFolderHandleUseCase: GetSecondaryFolderNodeUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
    private val getSyncedNodeIdsUseCase: GetSyncedNodeIdsUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
    private val getDeviceIdAndNameMapUseCase: GetDeviceIdAndNameMapUseCase,
    private val getFullNodePathByIdUseCase: GetFullNodePathByIdUseCase,
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
) {

    /**
     * Returns a Flow of [MegaPickerFolderResult] for the given folder and options.
     * Emits whenever the list of child nodes or backup info changes.
     *
     * @param currentFolder The folder to load children for
     * @param rootFolderId The root folder id (null if not at root), used for exclude list and isSelectEnabled
     * @param isStopBackup True when picker is used for stop-backup flow
     * @param folderName Optional folder name for isFolderExists check when isStopBackup
     */
    operator fun invoke(
        currentFolder: Node,
        rootFolderId: NodeId?,
        isStopBackup: Boolean,
        folderName: String?,
    ): Flow<MegaPickerFolderResult> = flow {
        // Fast path for stop backup flow - no need to check sync/backup usage
        // User is just picking a destination to move the backup folder to
        if (isStopBackup) {
            val isSelectEnabled = runCatching {
                folderName?.let {
                    !nodeExistsInCurrentLocationUseCase(currentFolder.id, it)
                } ?: true
            }.getOrElse { true }

            getTypedNodesFromFolder(currentFolder.id)
                .catch { Timber.d(it, "Error getting child folders") }
                .collect { childFolders ->
                    emit(
                        MegaPickerFolderResult(
                            currentFolder = currentFolder,
                            nodes = childFolders.map { node ->
                                MegaPickerNodeInfo(
                                    node = node,
                                    isDisabled = false,
                                    isUsedBySyncOrBackup = false,
                                    backupId = null,
                                    deviceName = null,
                                )
                            },
                            isSelectEnabled = isSelectEnabled,
                        )
                    )
                }
            return@flow
        }

        // Original logic for sync setup flow
        val isFeatureEnabled =
            getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup)
        val currentDeviceId = runCatching { getDeviceIdUseCase() }.getOrNull()
        val syncedNodeIds = runCatching {
            getSyncedNodeIdsUseCase()
        }.getOrElse {
            Timber.e(it, "Error getting synced node ids")
            emptyList()
        }
        val excludeFolders = if (currentFolder.id == rootFolderId) {
            runCatching {
                buildExcludeFoldersAtRoot(syncedNodeIds)
            }.onFailure {
                Timber.e(it, "Error getting handles of CU and MyChat files")
            }.getOrNull()
        } else {
            null
        }
        // Check if RestrictSyncAcrossDevices feature flag is enabled
        // When disabled (default), folders used by Sync/Backup on OTHER devices are allowed to be selected
        // (Sync-Sync across devices is allowed), but folders used by Camera/Media Uploads on
        // other devices are still blocked
        // When enabled, reverts to old behavior (blocks Sync-Sync across devices)
        val restrictSyncAcrossDevices = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.RestrictSyncAcrossDevices)
        }.getOrElse { false }

        val syncBackupInfoListOtherDevice = runCatching {
            if (isFeatureEnabled) {
                getBackupInfoUseCase()
                    .filter { backup ->
                        (backup.deviceId != null && backup.deviceId != currentDeviceId) &&
                                when (backup.type) {
                                    // Camera/Media Uploads from other devices - always check
                                    BackupInfoType.CAMERA_UPLOADS,
                                    BackupInfoType.MEDIA_UPLOADS,
                                        -> true

                                    // Sync from other devices
                                    BackupInfoType.TWO_WAY_SYNC,
                                        -> {
                                        // Include if feature flag is enabled (revert to old behavior)
                                        // Exclude if feature flag is disabled (allow Sync-Sync across devices)
                                        restrictSyncAcrossDevices
                                    }

                                    else -> false
                                }
                    }
            } else {
                emptyList()
            }
        }.getOrElse {
            Timber.d(it, "Error getting backup info for folder usage check")
            emptyList()
        }
        val deviceNameMap = runCatching {
            getDeviceIdAndNameMapUseCase()
        }.getOrElse {
            Timber.d(it, "Error getting device name map")
            emptyMap()
        }

        // Pre-fetch all backup root paths
        val backupPaths: Map<NodeId, String> =
            syncBackupInfoListOtherDevice.mapNotNull { backupInfo ->
                runCatching {
                    getFullNodePathByIdUseCase(backupInfo.rootHandle)?.let { path ->
                        backupInfo.rootHandle to path
                    }
                }.getOrNull()
            }.toMap()
        getTypedNodesFromFolder(currentFolder.id)
            .catch {
                Timber.d(it, "Error getting child folders of current folder ${currentFolder.name}")
            }
            .collect { childFolders ->
                val folderNodePaths: Map<NodeId, String> = childFolders
                    .filterIsInstance<FolderNode>()
                    .mapNotNull { node ->
                        runCatching {
                            getFullNodePathByIdUseCase(node.id)?.let { path ->
                                node.id to path
                            }
                        }.getOrNull()
                    }.toMap()

                // Map nodes using pre-fetched paths
                val nodes = childFolders.map { node ->
                    mapNodeToMegaPickerNodeInfo(
                        node = node,
                        nodePath = folderNodePaths[node.id],
                        excludeFolders = excludeFolders,
                        backupPaths = backupPaths,
                        syncBackupInfoListOtherDevice = syncBackupInfoListOtherDevice,
                        deviceNameMap = deviceNameMap,
                        syncedFolderIds = syncedNodeIds,
                    )
                }

                val notAtRoot = currentFolder.id != rootFolderId
                val noChildUsedBySyncOrBackup = !nodes.any { it.isUsedBySyncOrBackup }
                val isSelectEnabled = notAtRoot && noChildUsedBySyncOrBackup

                emit(
                    MegaPickerFolderResult(
                        currentFolder = currentFolder,
                        nodes = nodes,
                        isSelectEnabled = isSelectEnabled,
                    )
                )
            }
    }

    private suspend fun buildExcludeFoldersAtRoot(syncedNodeIds: List<NodeId>): List<NodeId>? =
        coroutineScope {
            val cameraUploadsFolderHandle =
                async { getCameraUploadsFolderHandleUseCase().takeIf { isCameraUploadsEnabledUseCase() } }
            val mediaUploadsFolderHandle =
                async { getMediaUploadsFolderHandleUseCase()?.id.takeIf { isMediaUploadsEnabledUseCase() } }
            val myChatsUploadsFolderHandle = async { getMyChatsFilesFolderIdUseCase() }

            val list = listOfNotNull(
                cameraUploadsFolderHandle.await()?.let { NodeId(it) },
                mediaUploadsFolderHandle.await(),
                myChatsUploadsFolderHandle.await(),
            ).filterNot { it == NodeId(-1L) }.plus(syncedNodeIds)

            return@coroutineScope list.ifEmpty { null }
        }

    private fun mapNodeToMegaPickerNodeInfo(
        node: TypedNode,
        nodePath: String?,
        excludeFolders: List<NodeId>?,
        backupPaths: Map<NodeId, String>,
        syncBackupInfoListOtherDevice: List<BackupInfo>,
        deviceNameMap: Map<String, String>,
        syncedFolderIds: List<NodeId>,
    ): MegaPickerNodeInfo {
        val isExcluded = excludeFolders?.contains(node.id) == true

        val matchingBackupInfoOtherDevice = if (nodePath != null) {
            syncBackupInfoListOtherDevice.firstOrNull { backupInfo ->
                val backupPath = backupPaths[backupInfo.rootHandle]
                backupPath != null && isNodeInBackupPath(
                    nodeId = node.id,
                    nodePath = nodePath,
                    backupRootHandle = backupInfo.rootHandle,
                    backupPath = backupPath,
                )
            }
        } else null

        val isUsedBySyncOrBackup =
            matchingBackupInfoOtherDevice != null || node.id in syncedFolderIds
        val isDisabled = isExcluded || isUsedBySyncOrBackup

        return MegaPickerNodeInfo(
            node = node,
            isDisabled = isDisabled,
            isUsedBySyncOrBackup = isUsedBySyncOrBackup,
            backupId = matchingBackupInfoOtherDevice?.id,
            deviceName = matchingBackupInfoOtherDevice?.deviceId?.let { deviceNameMap[it] },
        )
    }

    /**
     * Checks if the node is the backup root or inside the backup tree.
     *
     * The backup root is matched by [NodeId] so we do not treat two different folders as the same
     * when paths are ambiguous (e.g. same display name or same leaf segment only).
     * Descendants are detected via path prefix using [UriPath.isSubPathOf].
     */
    private fun isNodeInBackupPath(
        nodeId: NodeId,
        nodePath: String,
        backupRootHandle: NodeId,
        backupPath: String,
    ): Boolean {
        if (nodeId == backupRootHandle) return true

        val normalizedNodePath = nodePath.trimEnd('/')
        val normalizedBackupPath = backupPath.trimEnd('/')
        if (normalizedBackupPath.isEmpty()) return false

        return UriPath(normalizedNodePath).isSubPathOf(UriPath(normalizedBackupPath))
    }
}
