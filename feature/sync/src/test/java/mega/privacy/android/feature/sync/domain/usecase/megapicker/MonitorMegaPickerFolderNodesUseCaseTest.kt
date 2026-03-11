package mega.privacy.android.feature.sync.domain.usecase.megapicker

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
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
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncedNodeIdsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorMegaPickerFolderNodesUseCaseTest {

    private val getTypedNodesFromFolder: GetTypedNodesFromFolderUseCase = mock()
    private val getCameraUploadsFolderHandleUseCase: GetPrimarySyncHandleUseCase = mock()
    private val getMediaUploadsFolderHandleUseCase: GetSecondaryFolderNodeUseCase = mock()
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase = mock()
    private val getSyncedNodeIdsUseCase: GetSyncedNodeIdsUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getBackupInfoUseCase: GetBackupInfoUseCase = mock()
    private val getDeviceIdUseCase: GetDeviceIdUseCase = mock()
    private val getDeviceIdAndNameMapUseCase: GetDeviceIdAndNameMapUseCase = mock()
    private val getFullNodePathByIdUseCase: GetFullNodePathByIdUseCase = mock()
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase = mock()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase = mock()

    private lateinit var underTest: MonitorMegaPickerFolderNodesUseCase

    @BeforeAll
    fun setUp() {
        underTest = MonitorMegaPickerFolderNodesUseCase(
            getTypedNodesFromFolder = getTypedNodesFromFolder,
            getCameraUploadsFolderHandleUseCase = getCameraUploadsFolderHandleUseCase,
            getMediaUploadsFolderHandleUseCase = getMediaUploadsFolderHandleUseCase,
            getMyChatsFilesFolderIdUseCase = getMyChatsFilesFolderIdUseCase,
            getSyncedNodeIdsUseCase = getSyncedNodeIdsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getBackupInfoUseCase = getBackupInfoUseCase,
            getDeviceIdUseCase = getDeviceIdUseCase,
            getDeviceIdAndNameMapUseCase = getDeviceIdAndNameMapUseCase,
            getFullNodePathByIdUseCase = getFullNodePathByIdUseCase,
            nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isMediaUploadsEnabledUseCase = isMediaUploadsEnabledUseCase,
        )
    }

    @BeforeEach
    fun clear() {
        reset(
            getTypedNodesFromFolder,
            getCameraUploadsFolderHandleUseCase,
            getMediaUploadsFolderHandleUseCase,
            getMyChatsFilesFolderIdUseCase,
            getSyncedNodeIdsUseCase,
            getFeatureFlagValueUseCase,
            getBackupInfoUseCase,
            getDeviceIdUseCase,
            getDeviceIdAndNameMapUseCase,
            getFullNodePathByIdUseCase,
            nodeExistsInCurrentLocationUseCase,
            isCameraUploadsEnabledUseCase,
            isMediaUploadsEnabledUseCase,
        )
    }

    @Test
    fun `test that emits one result with empty nodes when at root and getTypedNodesFromFolder returns empty`() =
        runTest {
            val rootFolderId = NodeId(123L)
            val rootFolder: FolderNode = mock {
                on { id } doReturn rootFolderId
            }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)
            whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flowOf(emptyList()))
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getSyncedNodeIdsUseCase()).thenReturn(emptyList())
            whenever(getDeviceIdUseCase()).thenReturn(null)
            whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)

            underTest(rootFolder, rootFolderId, false, null).test {
                val result = awaitItem()
                assertThat(result.currentFolder).isEqualTo(rootFolder)
                assertThat(result.nodes).isEmpty()
                assertThat(result.isSelectEnabled).isFalse()
                awaitComplete()
            }
        }
    @Test
    fun `test that isSelectEnabled is true when currentFolder is not root`() = runTest {
        val rootFolderId = NodeId(123L)
        val childFolderId = NodeId(456L)
        val rootFolder: FolderNode = mock { on { id } doReturn rootFolderId }
        val childFolder: FolderNode = mock { on { id } doReturn childFolderId }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)
        whenever(getSyncedNodeIdsUseCase()).thenReturn(emptyList())
        whenever(getDeviceIdUseCase()).thenReturn(null)
        whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
        whenever(getTypedNodesFromFolder(childFolderId)).thenReturn(flowOf(emptyList()))

        underTest(childFolder, rootFolderId, false, null).test {
            val result = awaitItem()
            assertThat(result.isSelectEnabled).isTrue()
            awaitComplete()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that node in exclude list at root is disabled`() = runTest {
        val rootFolderId = NodeId(123L)
        val cuFolderId = NodeId(146L)
        val rootFolder: FolderNode = mock { on { id } doReturn rootFolderId }
        val cuFolder: TypedNode = mock { on { id } doReturn cuFolderId }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(cuFolderId.longValue)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(getSyncedNodeIdsUseCase()).thenReturn(emptyList())
        whenever(getDeviceIdUseCase()).thenReturn(null)
        whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
        whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flowOf(listOf(cuFolder)))

        underTest(rootFolder, rootFolderId, false, null).test {
            val result = awaitItem()
            assertThat(result.nodes).hasSize(1)
            assertThat(result.nodes[0].node).isEqualTo(cuFolder)
            assertThat(result.nodes[0].isDisabled).isTrue()
            awaitComplete()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when feature flag is off backup list is empty and nodes are not disabled by backup`() =
        runTest {
            val rootFolderId = NodeId(123L)
            val childId = NodeId(789L)
            val rootFolder: FolderNode = mock { on { id } doReturn rootFolderId }
            val childNode: TypedNode = mock { on { id } doReturn childId }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)
            whenever(getBackupInfoUseCase()).thenReturn(emptyList())
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getSyncedNodeIdsUseCase()).thenReturn(emptyList())
            whenever(getDeviceIdUseCase()).thenReturn(null)
            whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flowOf(listOf(childNode)))

            underTest(rootFolder, rootFolderId, false, null).test {
                val result = awaitItem()
                assertThat(result.nodes).hasSize(1)
                assertThat(result.nodes[0].isDisabled).isFalse()
                awaitComplete()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isSelectEnabled is false when isStopBackup and folder exists in current location`() =
        runTest {
            val rootFolderId = NodeId(123L)
            val currentFolderId = NodeId(456L)
            val folderName = "ExistingFolder"
            val currentFolder: FolderNode = mock {
                on { id } doReturn currentFolderId
            }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)
            whenever(getSyncedNodeIdsUseCase()).thenReturn(emptyList())
            whenever(getDeviceIdUseCase()).thenReturn(null)
            whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
            whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flowOf(emptyList()))
            whenever(nodeExistsInCurrentLocationUseCase(currentFolderId, folderName))
                .thenReturn(true)

            underTest(currentFolder, rootFolderId, true, folderName).test {
                val result = awaitItem()
                assertThat(result.isSelectEnabled).isFalse()
                awaitComplete()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that synced folder handles are included in exclude list at root`() = runTest {
        val rootFolderId = NodeId(123L)
        val syncedRemoteId = NodeId(999L)
        val rootFolder: FolderNode = mock { on { id } doReturn rootFolderId }
        val syncedFolder: TypedNode = mock { on { id } doReturn syncedRemoteId }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(getSyncedNodeIdsUseCase()).thenReturn(listOf(syncedRemoteId))
        whenever(getDeviceIdUseCase()).thenReturn(null)
        whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
        whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flowOf(listOf(syncedFolder)))

        underTest(rootFolder, rootFolderId, false, null).test {
            val result = awaitItem()
            assertThat(result.nodes).hasSize(1)
            assertThat(result.nodes[0].isDisabled).isTrue()
            awaitComplete()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when getTypedNodesFromFolder throws flow completes without emitting`() =
        runTest {
            val rootFolderId = NodeId(123L)
            val rootFolder: FolderNode = mock { on { id } doReturn rootFolderId }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getSyncedNodeIdsUseCase()).thenReturn(emptyList())
            whenever(getDeviceIdUseCase()).thenReturn(null)
            whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(
                kotlinx.coroutines.flow.flow { throw RuntimeException("Network error") }
            )

            underTest(rootFolder, rootFolderId, false, null).test {
                awaitComplete()
            }
        }

    @Test
    fun `test that isSelectEnabled is false when currentFolder is not root but has a child used by sync or backup`() =
        runTest {
            val rootFolderId = NodeId(123L)
            val parentFolderId = NodeId(456L)
            val syncedChildId = NodeId(789L)
            val otherChildId = NodeId(101L)
            val backupRootHandle = NodeId(789L)
            val parentFolder: TypedFolderNode = mock { on { id } doReturn parentFolderId }
            val syncedChild: TypedFolderNode = mock { on { id } doReturn syncedChildId }
            val otherChild: TypedFolderNode = mock { on { id } doReturn otherChildId }
            val backupInfo = mock<BackupInfo> {
                on { rootHandle } doReturn backupRootHandle
                on { type } doReturn BackupInfoType.TWO_WAY_SYNC
                on { deviceId } doReturn "otherDevice"
            }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getBackupInfoUseCase()).thenReturn(listOf(backupInfo))
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getSyncedNodeIdsUseCase()).thenReturn(emptyList())
            whenever(getDeviceIdUseCase()).thenReturn("device1")
            whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(getTypedNodesFromFolder(parentFolderId))
                .thenReturn(flowOf(listOf<TypedNode>(syncedChild, otherChild)))
            // Mock path lookups: syncedChild path matches backup path (ExactMatch)
            whenever(getFullNodePathByIdUseCase(backupRootHandle)).thenReturn("/Cloud Drive/SyncedFolder")
            whenever(getFullNodePathByIdUseCase(syncedChildId)).thenReturn("/Cloud Drive/SyncedFolder")
            whenever(getFullNodePathByIdUseCase(otherChildId)).thenReturn("/Cloud Drive/OtherFolder")

            underTest(parentFolder, rootFolderId, false, null).test {
                val result = awaitItem()
                assertThat(result.isSelectEnabled).isFalse()
                assertThat(result.nodes).hasSize(2)
                assertThat(result.nodes[0].isUsedBySyncOrBackup).isTrue()
                assertThat(result.nodes[1].isUsedBySyncOrBackup).isFalse()
                awaitComplete()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isSelectEnabled is true when currentFolder is not root and no child is used by sync or backup`() =
        runTest {
            val rootFolderId = NodeId(123L)
            val parentFolderId = NodeId(456L)
            val childId = NodeId(789L)
            val parentFolder: FolderNode = mock { on { id } doReturn parentFolderId }
            val childNode: TypedNode = mock { on { id } doReturn childId }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getBackupInfoUseCase()).thenReturn(emptyList())
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getSyncedNodeIdsUseCase()).thenReturn(emptyList())
            whenever(getDeviceIdUseCase()).thenReturn(null)
            whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(getTypedNodesFromFolder(parentFolderId)).thenReturn(flowOf(listOf(childNode)))

            underTest(parentFolder, rootFolderId, false, null).test {
                val result = awaitItem()
                assertThat(result.isSelectEnabled).isTrue()
                assertThat(result.nodes).hasSize(1)
                assertThat(result.nodes[0].isUsedBySyncOrBackup).isFalse()
                awaitComplete()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isSelectEnabled is false when currentFolder is not root but has a child in folder pairs`() =
        runTest {
            val rootFolderId = NodeId(123L)
            val parentFolderId = NodeId(456L)
            val syncedChildId = NodeId(789L)
            val otherChildId = NodeId(101L)
            val parentFolder: FolderNode = mock { on { id } doReturn parentFolderId }
            val syncedChild: TypedNode = mock { on { id } doReturn syncedChildId }
            val otherChild: TypedNode = mock { on { id } doReturn otherChildId }
            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)
            whenever(getBackupInfoUseCase()).thenReturn(emptyList())
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getSyncedNodeIdsUseCase()).thenReturn(listOf(syncedChildId))
            whenever(getDeviceIdUseCase()).thenReturn(null)
            whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
            whenever(getTypedNodesFromFolder(parentFolderId))
                .thenReturn(flowOf(listOf(syncedChild, otherChild)))

            underTest(parentFolder, rootFolderId, false, null).test {
                val result = awaitItem()
                assertThat(result.isSelectEnabled).isFalse()
                assertThat(result.nodes).hasSize(2)
                assertThat(result.nodes[0].isUsedBySyncOrBackup).isTrue()
                assertThat(result.nodes[1].isUsedBySyncOrBackup).isFalse()
                awaitComplete()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that parent folder of synced folder is not disabled`() =
        runTest {
            // Scenario: CloudDrive > ParentFolder > SyncedFolder
            // When viewing CloudDrive, ParentFolder should NOT be grayed out
            // because it's a PARENT of the synced folder, not the synced folder itself

            val rootFolderId = NodeId(123L)
            val parentFolderId = NodeId(456L)
            val syncedChildId = NodeId(789L)
            val backupRootHandle = NodeId(789L) // synced child is the backup root

            val rootFolder: FolderNode = mock { on { id } doReturn rootFolderId }
            val parentFolder: TypedNode = mock { on { id } doReturn parentFolderId }

            val backupInfo = mock<BackupInfo> {
                on { rootHandle } doReturn backupRootHandle
                on { type } doReturn BackupInfoType.TWO_WAY_SYNC
                on { deviceId } doReturn "device1"
            }

            whenever(getFeatureFlagValueUseCase(ApiFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(true)
            whenever(getBackupInfoUseCase()).thenReturn(listOf(backupInfo))
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getSyncedNodeIdsUseCase()).thenReturn(emptyList())
            whenever(getDeviceIdUseCase()).thenReturn("device1")
            whenever(getDeviceIdAndNameMapUseCase()).thenReturn(emptyMap())
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)

            // When loading root folder, parent is shown as child
            whenever(getTypedNodesFromFolder(rootFolderId))
                .thenReturn(flowOf(listOf(parentFolder)))

            // Mock path lookups:
            // - Backup path: /Cloud Drive/ParentFolder/SyncedFolder
            // - Parent path: /Cloud Drive/ParentFolder
            // Parent is NOT inside backup (backup is INSIDE parent), so parent should NOT be disabled
            whenever(getFullNodePathByIdUseCase(backupRootHandle)).thenReturn("/Cloud Drive/ParentFolder/SyncedFolder")
            whenever(getFullNodePathByIdUseCase(parentFolderId)).thenReturn("/Cloud Drive/ParentFolder")

            underTest(rootFolder, rootFolderId, false, null).test {
                val result = awaitItem()
                // Parent should NOT be disabled (not grayed out)
                assertThat(result.nodes[0].isDisabled).isFalse()
                assertThat(result.nodes[0].isUsedBySyncOrBackup).isFalse()
                awaitComplete()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
