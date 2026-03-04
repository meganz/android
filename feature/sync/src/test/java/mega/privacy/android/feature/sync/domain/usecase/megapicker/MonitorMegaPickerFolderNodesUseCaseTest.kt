package mega.privacy.android.feature.sync.domain.usecase.megapicker

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
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
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
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
    private val getFolderPairsUseCase: GetFolderPairsUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getBackupInfoUseCase: GetBackupInfoUseCase = mock()
    private val getDeviceIdUseCase: GetDeviceIdUseCase = mock()
    private val getDeviceIdAndNameMapUseCase: GetDeviceIdAndNameMapUseCase = mock()
    private val determineNodeRelationshipUseCase: DetermineNodeRelationshipUseCase = mock()
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase = mock()

    private lateinit var underTest: MonitorMegaPickerFolderNodesUseCase

    @BeforeEach
    fun setUp() {
        reset(
            getTypedNodesFromFolder,
            getCameraUploadsFolderHandleUseCase,
            getMediaUploadsFolderHandleUseCase,
            getMyChatsFilesFolderIdUseCase,
            getFolderPairsUseCase,
            getFeatureFlagValueUseCase,
            getBackupInfoUseCase,
            getDeviceIdUseCase,
            getDeviceIdAndNameMapUseCase,
            determineNodeRelationshipUseCase,
            nodeExistsInCurrentLocationUseCase,
        )
        underTest = MonitorMegaPickerFolderNodesUseCase(
            getTypedNodesFromFolder = getTypedNodesFromFolder,
            getCameraUploadsFolderHandleUseCase = getCameraUploadsFolderHandleUseCase,
            getMediaUploadsFolderHandleUseCase = getMediaUploadsFolderHandleUseCase,
            getMyChatsFilesFolderIdUseCase = getMyChatsFilesFolderIdUseCase,
            getFolderPairsUseCase = getFolderPairsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getBackupInfoUseCase = getBackupInfoUseCase,
            getDeviceIdUseCase = getDeviceIdUseCase,
            getDeviceIdAndNameMapUseCase = getDeviceIdAndNameMapUseCase,
            determineNodeRelationshipUseCase = determineNodeRelationshipUseCase,
            nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
        )
    }

    @Test
    fun `test that emits one result with empty nodes when at root and getTypedNodesFromFolder returns empty`() =
        runTest {
            val rootFolderId = NodeId(123L)
            val rootFolder: FolderNode = mock {
                on { id } doReturn rootFolderId
            }
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)
            whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flowOf(emptyList()))
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getFolderPairsUseCase()).thenReturn(emptyList())

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
        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)
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
        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(cuFolderId.longValue)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(getFolderPairsUseCase()).thenReturn(emptyList())
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
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)
            whenever(getBackupInfoUseCase()).thenReturn(emptyList())
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getFolderPairsUseCase()).thenReturn(emptyList())
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
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)
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
        whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
            .thenReturn(false)
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(getFolderPairsUseCase()).thenReturn(
            listOf(
                FolderPair(
                    id = 1L,
                    syncType = mega.privacy.android.domain.entity.sync.SyncType.TYPE_TWOWAY,
                    pairName = "Sync",
                    localFolderPath = "/path",
                    remoteFolder = RemoteFolder(syncedRemoteId, "Remote"),
                    syncStatus = mega.privacy.android.feature.sync.domain.entity.SyncStatus.SYNCED,
                )
            )
        )
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
            whenever(getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup))
                .thenReturn(false)
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(getFolderPairsUseCase()).thenReturn(emptyList())
            whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(
                kotlinx.coroutines.flow.flow { throw RuntimeException("Network error") }
            )

            underTest(rootFolder, rootFolderId, false, null).test {
                awaitComplete()
            }
        }
}
