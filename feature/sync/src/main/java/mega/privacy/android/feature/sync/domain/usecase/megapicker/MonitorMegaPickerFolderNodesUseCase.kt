package mega.privacy.android.feature.sync.domain.usecase.megapicker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeRelationship
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.usecase.GetTypedNodesFromFolderUseCase
import mega.privacy.android.domain.usecase.backup.GetBackupInfoUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdAndNameMapUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.DetermineNodeRelationshipUseCase
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.feature.sync.domain.entity.megapicker.MegaPickerFolderResult
import mega.privacy.android.feature.sync.domain.entity.megapicker.MegaPickerNodeInfo
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
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
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
    private val getDeviceIdAndNameMapUseCase: GetDeviceIdAndNameMapUseCase,
    private val determineNodeRelationshipUseCase: DetermineNodeRelationshipUseCase,
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
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
        val excludeFolders = if (currentFolder.id == rootFolderId) {
            runCatching {
                buildExcludeFoldersAtRoot()
            }.onFailure {
                Timber.d(it, "Error getting handles of CU and MyChat files")
            }.getOrNull()
        } else {
            null
        }

        val syncBackupInfoList = runCatching {
            val isFeatureEnabled =
                getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup)
            if (isFeatureEnabled) {
                getBackupInfoUseCase()
                    .filter {
                        it.type == BackupInfoType.BACKUP_UPLOAD ||
                                it.type == BackupInfoType.TWO_WAY_SYNC
                    }
            } else {
                emptyList()
            }
        }.getOrElse {
            Timber.d(it, "Error getting backup info for folder usage check")
            emptyList()
        }

        val currentDeviceId = runCatching { getDeviceIdUseCase() }.getOrNull()
        val syncBackupInfoListOtherDevice = syncBackupInfoList.filter {
            it.deviceId != null && it.deviceId != currentDeviceId
        }

        val deviceNameMap = runCatching {
            getDeviceIdAndNameMapUseCase()
        }.getOrElse {
            Timber.d(it, "Error getting device name map")
            emptyMap()
        }

        val isSelectEnabled = if (isStopBackup) {
            runCatching {
                folderName?.let {
                    !nodeExistsInCurrentLocationUseCase(currentFolder.id, it)
                } ?: true
            }.getOrElse { true }
        } else {
            currentFolder.id != rootFolderId
        }

        Timber.d(
            "Current folder: ${currentFolder.name}, id: ${currentFolder.id}, " +
                    "RootFolderId: $rootFolderId, Exclude folders: $excludeFolders"
        )

        getTypedNodesFromFolder(currentFolder.id)
            .catch {
                Timber.d(it, "Error getting child folders of current folder ${currentFolder.name}")
            }
            .collect { childFolders ->
                val nodes = childFolders.map { node ->
                    mapNodeToMegaPickerNodeInfo(
                        node = node,
                        excludeFolders = excludeFolders,
                        syncBackupInfoList = syncBackupInfoList,
                        syncBackupInfoListOtherDevice = syncBackupInfoListOtherDevice,
                        deviceNameMap = deviceNameMap,
                    )
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

    private suspend fun buildExcludeFoldersAtRoot(): List<NodeId>? {
        val cameraUploadsFolderHandle = getCameraUploadsFolderHandleUseCase()
        val mediaUploadsFolderHandle = getMediaUploadsFolderHandleUseCase()?.id
        val myChatsUploadsFolderHandle = getMyChatsFilesFolderIdUseCase()
        val syncedFolderHandles = getFolderPairsUseCase().map { it.remoteFolder.id }

        val list = listOfNotNull(
            NodeId(cameraUploadsFolderHandle),
            mediaUploadsFolderHandle,
            myChatsUploadsFolderHandle,
        ).filterNot { it == NodeId(-1L) }
            .plus(syncedFolderHandles)

        return list.ifEmpty { null }
    }

    private suspend fun mapNodeToMegaPickerNodeInfo(
        node: TypedNode,
        excludeFolders: List<NodeId>?,
        syncBackupInfoList: List<BackupInfo>,
        syncBackupInfoListOtherDevice: List<BackupInfo>,
        deviceNameMap: Map<String, String>,
    ): MegaPickerNodeInfo {
        val isExcluded = excludeFolders?.contains(node.id) == true

        val matchingBackupInfoAny = syncBackupInfoList.firstOrNull { backupInfo ->
            runCatching {
                determineNodeRelationshipUseCase(
                    node.id,
                    backupInfo.rootHandle
                ) != NodeRelationship.NoMatch
            }.getOrElse { false }
        }
        val matchingBackupInfoOtherDevice =
            syncBackupInfoListOtherDevice.firstOrNull { backupInfo ->
                runCatching {
                    determineNodeRelationshipUseCase(
                        node.id,
                        backupInfo.rootHandle
                    ) != NodeRelationship.NoMatch
                }.getOrElse { false }
            }

        val isUsedBySyncOrBackup = matchingBackupInfoAny != null
        val isDisabled = isExcluded || isUsedBySyncOrBackup

        return MegaPickerNodeInfo(
            node = node,
            isDisabled = isDisabled,
            subtitle = matchingBackupInfoOtherDevice?.deviceId?.let { deviceNameMap[it] },
            backupId = matchingBackupInfoOtherDevice?.id,
            deviceName = matchingBackupInfoOtherDevice?.deviceId?.let { deviceNameMap[it] },
        )
    }
}
