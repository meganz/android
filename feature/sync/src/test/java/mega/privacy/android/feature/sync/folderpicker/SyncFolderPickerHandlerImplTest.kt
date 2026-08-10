package mega.privacy.android.feature.sync.folderpicker

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.backup.RemoveDeviceFolderConnectionUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.megapicker.MegaPickerFolderResult
import mega.privacy.android.feature.sync.domain.entity.megapicker.MegaPickerNodeInfo
import mega.privacy.android.feature.sync.domain.usecase.megapicker.MonitorMegaPickerFolderNodesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.TryNodeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.ui.formatter.FolderConflictMessageFormatter
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import mega.privacy.android.shared.sync.folderpicker.SyncFolderPickerRestrictedNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncFolderPickerHandlerImplTest {

    private val monitorMegaPickerFolderNodesUseCase: MonitorMegaPickerFolderNodesUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase: IsFolderUsedBySyncOrBackupAcrossDevicesUseCase =
        mock()
    private val folderConflictMessageFormatter: FolderConflictMessageFormatter = mock()
    private val tryNodeSyncUseCase: TryNodeSyncUseCase = mock()
    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper = mock()
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase = mock()
    private val setSelectedMegaFolderUseCase: SetSelectedMegaFolderUseCase = mock()
    private val removeDeviceFolderConnectionUseCase: RemoveDeviceFolderConnectionUseCase = mock()

    private val underTest = SyncFolderPickerHandlerImpl(
        monitorMegaPickerFolderNodesUseCase = monitorMegaPickerFolderNodesUseCase,
        getRootNodeUseCase = getRootNodeUseCase,
        getNodeByHandleUseCase = getNodeByHandleUseCase,
        isFolderUsedBySyncOrBackupAcrossDevicesUseCase = isFolderUsedBySyncOrBackupAcrossDevicesUseCase,
        folderConflictMessageFormatter = folderConflictMessageFormatter,
        tryNodeSyncUseCase = tryNodeSyncUseCase,
        deviceFolderUINodeErrorMessageMapper = deviceFolderUINodeErrorMessageMapper,
        nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
        setSelectedMegaFolderUseCase = setSelectedMegaFolderUseCase,
        removeDeviceFolderConnectionUseCase = removeDeviceFolderConnectionUseCase,
    )

    private val folderId = NodeId(2323L)

    @AfterEach
    fun tearDown() {
        reset(
            monitorMegaPickerFolderNodesUseCase,
            getRootNodeUseCase,
            getNodeByHandleUseCase,
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase,
            folderConflictMessageFormatter,
            tryNodeSyncUseCase,
            deviceFolderUINodeErrorMessageMapper,
            nodeExistsInCurrentLocationUseCase,
            setSelectedMegaFolderUseCase,
            removeDeviceFolderConnectionUseCase,
        )
    }

    @Test
    fun `test that monitorPickerNodes maps the restricted nodes and the select availability`() =
        runTest {
            val rootFolderId = NodeId(123456L)
            val rootFolder: FolderNode = mock { on { id } doReturn rootFolderId }
            val currentFolder: FolderNode = mock { on { id } doReturn folderId }
            val enabledNode: TypedNode = mock {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Enabled folder"
            }
            val restrictedNode: TypedNode = mock {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "Restricted folder"
            }
            whenever(getRootNodeUseCase()).thenReturn(rootFolder)
            whenever(getNodeByHandleUseCase(folderId.longValue)).thenReturn(currentFolder)
            whenever(
                monitorMegaPickerFolderNodesUseCase(currentFolder, rootFolderId, false, null)
            ).thenReturn(
                flowOf(
                    MegaPickerFolderResult(
                        currentFolder = currentFolder,
                        nodes = listOf(
                            MegaPickerNodeInfo(node = enabledNode, isDisabled = false),
                            MegaPickerNodeInfo(
                                node = restrictedNode,
                                isDisabled = true,
                                isUsedBySyncOrBackup = true,
                                backupId = 555L,
                                deviceName = "Other device",
                            ),
                        ),
                        isSelectEnabled = true,
                    )
                )
            )

            underTest.monitorPickerNodes(folderId, false, null).test {
                val result = awaitItem()
                assertThat(result.isSelectEnabled).isTrue()
                assertThat(result.restrictedNodes).containsExactly(
                    NodeId(2L),
                    SyncFolderPickerRestrictedNode(
                        nodeId = NodeId(2L),
                        name = "Restricted folder",
                        isUsedBySyncOrBackup = true,
                        backupId = 555L,
                        deviceName = "Other device",
                    ),
                )
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorPickerNodes emits nothing when the current folder is not found`() =
        runTest {
            whenever(getRootNodeUseCase()).thenReturn(null)
            whenever(getNodeByHandleUseCase(folderId.longValue)).thenReturn(null)

            underTest.monitorPickerNodes(folderId, false, null).test {
                awaitComplete()
            }
            verify(monitorMegaPickerFolderNodesUseCase, never()).invoke(
                any(), any(), any(), any()
            )
        }

    @Test
    fun `test that getFolderUsageConflictMessage formats the conflict with the folder name`() =
        runTest {
            val folderName = "Conflicting folder"
            val conflictMessage = "conflict message"
            val node: FolderNode = mock {
                on { id } doReturn folderId
                on { name } doReturn folderName
            }
            whenever(getNodeByHandleUseCase(folderId.longValue)).thenReturn(node)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    nodeId = folderId,
                    isSyncFolderSelection = true,
                    shouldExcludeCurrentDevice = false,
                    useCache = false,
                )
            ).thenReturn(FolderUsageResult.UsedByCameraUpload)
            whenever(
                folderConflictMessageFormatter.formatFromFolderUsage(
                    folderDisplayName = folderName,
                    folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
                    result = FolderUsageResult.UsedByCameraUpload,
                )
            ).thenReturn(conflictMessage)

            assertThat(underTest.getFolderUsageConflictMessage(folderId))
                .isEqualTo(conflictMessage)
        }

    @Test
    fun `test that getFolderUsageConflictMessage returns null when the folder is not used`() =
        runTest {
            val node: FolderNode = mock {
                on { id } doReturn folderId
                on { name } doReturn "Free folder"
            }
            whenever(getNodeByHandleUseCase(folderId.longValue)).thenReturn(node)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    nodeId = folderId,
                    isSyncFolderSelection = true,
                    shouldExcludeCurrentDevice = false,
                    useCache = false,
                )
            ).thenReturn(FolderUsageResult.NotUsed)
            whenever(
                folderConflictMessageFormatter.formatFromFolderUsage(
                    folderDisplayName = "Free folder",
                    folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
                    result = FolderUsageResult.NotUsed,
                )
            ).thenReturn(null)

            assertThat(underTest.getFolderUsageConflictMessage(folderId)).isNull()
        }

    @Test
    fun `test that getFolderUsageConflictMessage treats a failed usage check as not used`() =
        runTest {
            val node: FolderNode = mock {
                on { id } doReturn folderId
                on { name } doReturn "Some folder"
            }
            whenever(getNodeByHandleUseCase(folderId.longValue)).thenReturn(node)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    nodeId = folderId,
                    isSyncFolderSelection = true,
                    shouldExcludeCurrentDevice = false,
                    useCache = false,
                )
            ).thenThrow(RuntimeException("API error"))
            whenever(
                folderConflictMessageFormatter.formatFromFolderUsage(
                    folderDisplayName = "Some folder",
                    folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
                    result = FolderUsageResult.NotUsed,
                )
            ).thenReturn(null)

            assertThat(underTest.getFolderUsageConflictMessage(folderId)).isNull()
        }

    @Test
    fun `test that validateNodeSyncability returns null when the node is syncable`() = runTest {
        whenever(tryNodeSyncUseCase(folderId)).thenReturn(Unit)

        assertThat(underTest.validateNodeSyncability(folderId)).isNull()
    }

    @Test
    fun `test that validateNodeSyncability maps the sync error to its message`() = runTest {
        val syncError = SyncError.ACTIVE_SYNC_ABOVE_PATH
        val errorMessageRes = 12345
        doAnswer { throw MegaSyncException(18, "error", syncError = syncError) }
            .whenever(tryNodeSyncUseCase).invoke(folderId)
        whenever(deviceFolderUINodeErrorMessageMapper(syncError)).thenReturn(errorMessageRes)

        assertThat(underTest.validateNodeSyncability(folderId)).isEqualTo(errorMessageRes)
    }

    @Test
    fun `test that validateNodeSyncability falls back to the unknown error message`() = runTest {
        val syncError = SyncError.ACTIVE_SYNC_ABOVE_PATH
        val unknownErrorRes = 99999
        doAnswer { throw MegaSyncException(18, "error", syncError = syncError) }
            .whenever(tryNodeSyncUseCase).invoke(folderId)
        whenever(deviceFolderUINodeErrorMessageMapper(syncError)).thenReturn(null)
        whenever(deviceFolderUINodeErrorMessageMapper(SyncError.UNKNOWN_ERROR))
            .thenReturn(unknownErrorRes)

        assertThat(underTest.validateNodeSyncability(folderId)).isEqualTo(unknownErrorRes)
    }

    @Test
    fun `test that validateNodeSyncability falls back to the generic error message`() = runTest {
        doAnswer { throw RuntimeException("boom") }
            .whenever(tryNodeSyncUseCase).invoke(folderId)
        whenever(deviceFolderUINodeErrorMessageMapper(SyncError.UNKNOWN_ERROR)).thenReturn(null)

        assertThat(underTest.validateNodeSyncability(folderId))
            .isEqualTo(sharedR.string.general_text_error)
    }

    @Test
    fun `test that folderNameExists delegates to the use case`() = runTest {
        val folderName = "Backup folder"
        whenever(nodeExistsInCurrentLocationUseCase(folderId, folderName)).thenReturn(true)

        assertThat(underTest.folderNameExists(folderId, folderName)).isTrue()
    }

    @Test
    fun `test that saveSelectedFolder propagates the selected folder`() = runTest {
        val folderName = "Selected folder"
        val node: FolderNode = mock {
            on { id } doReturn folderId
            on { name } doReturn folderName
        }
        whenever(getNodeByHandleUseCase(folderId.longValue)).thenReturn(node)

        assertThat(underTest.saveSelectedFolder(folderId)).isTrue()
        verify(setSelectedMegaFolderUseCase).invoke(RemoteFolder(folderId, folderName))
    }

    @Test
    fun `test that saveSelectedFolder returns false when the node no longer exists`() = runTest {
        whenever(getNodeByHandleUseCase(folderId.longValue)).thenReturn(null)

        assertThat(underTest.saveSelectedFolder(folderId)).isFalse()
        verify(setSelectedMegaFolderUseCase, never()).invoke(any())
    }

    @Test
    fun `test that removeFolderConnection delegates to the use case`() = runTest {
        val backupId = 555L
        whenever(removeDeviceFolderConnectionUseCase(backupId)).thenReturn(mock())

        underTest.removeFolderConnection(backupId)

        verify(removeDeviceFolderConnectionUseCase).invoke(backupId)
    }
}
