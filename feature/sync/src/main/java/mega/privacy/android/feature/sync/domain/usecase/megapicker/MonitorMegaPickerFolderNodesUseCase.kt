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
import mega.privacy.android.domain.featuretoggle.DomainFeatures
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
        val isFeatureEnabled =
            getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup)
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
        val syncBackupInfoListOtherDevice = runCatching {
            if (isFeatureEnabled) {
                getBackupInfoUseCase()
                    .filter {
                        (it.deviceId != null && it.deviceId != currentDeviceId) &&
                                (it.type == BackupInfoType.BACKUP_UPLOAD
                                        || it.type == BackupInfoType.TWO_WAY_SYNC
                                        || it.type == BackupInfoType.CAMERA_UPLOADS
                                        || it.type == BackupInfoType.MEDIA_UPLOADS)
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
        val isSelectEnabledForStopBackup = if (isStopBackup) {
            runCatching {
                folderName?.let {
                    !nodeExistsInCurrentLocationUseCase(currentFolder.id, it)
                } ?: true
            }.getOrElse { true }
        } else {
            null
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

                val isSelectEnabled = isSelectEnabledForStopBackup ?: run {
                    val notAtRoot = currentFolder.id != rootFolderId
                    val noChildUsedBySyncOrBackup = !nodes.any { it.isUsedBySyncOrBackup }
                    notAtRoot && noChildUsedBySyncOrBackup
                }

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
                backupPath != null && isNodeInBackupPath(nodePath, backupPath)
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
     * Checks if the node is in the backup hierarchy using path comparison.
     * Returns true if nodePath IS the backup or is INSIDE the backup (not a parent of it).
     */
    private fun isNodeInBackupPath(nodePath: String, backupPath: String): Boolean {
        val normalizedNodePath = nodePath.trimEnd('/')
        val normalizedBackupPath = backupPath.trimEnd('/')

        return when {
            // Exact match: node IS the backup folder
            normalizedNodePath == normalizedBackupPath -> true
            // Node is INSIDE the backup folder (backup is an ancestor of node)
            UriPath(normalizedNodePath).isSubPathOf(UriPath(normalizedBackupPath)) -> true
            // Otherwise: node is a parent of backup or unrelated
            else -> false
        }
    }
}
