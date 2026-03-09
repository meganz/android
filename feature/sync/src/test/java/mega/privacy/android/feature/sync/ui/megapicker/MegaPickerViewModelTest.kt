package mega.privacy.android.feature.sync.ui.megapicker

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.backup.BackupRemovalStatus
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.backup.IsFolderUsedBySyncOrBackupAcrossDevicesUseCase
import mega.privacy.android.domain.usecase.backup.RemoveDeviceFolderConnectionUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.megapicker.MegaPickerFolderResult
import mega.privacy.android.feature.sync.domain.entity.megapicker.MegaPickerNodeInfo
import mega.privacy.android.feature.sync.domain.usecase.megapicker.MonitorMegaPickerFolderNodesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.TryNodeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSelectedMegaFolderUseCase
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.nullable
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaPickerViewModelTest {

    private val setSelectedMegaFolderUseCase: SetSelectedMegaFolderUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val tryNodeSyncUseCase: TryNodeSyncUseCase = mock()
    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper = mock()
    private val createFolderNodeUseCase: CreateFolderNodeUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase = mock()
    private val isFolderUsedBySyncOrBackupAcrossDevicesUseCase: IsFolderUsedBySyncOrBackupAcrossDevicesUseCase =
        mock()
    private val removeDeviceFolderConnectionUseCase: RemoveDeviceFolderConnectionUseCase = mock()
    private val monitorMegaPickerFolderNodesUseCase: MonitorMegaPickerFolderNodesUseCase = mock()

    private val typedNodeUiModels: List<TypedNodeUiModel> = emptyList()
    private val childrenNodes: List<TypedNode> = emptyList()

    private lateinit var underTest: MegaPickerViewModel

    @AfterEach
    fun resetAndTearDown() {
        reset(
            setSelectedMegaFolderUseCase,
            getRootNodeUseCase,
            getNodeByHandleUseCase,
            tryNodeSyncUseCase,
            deviceFolderUINodeErrorMessageMapper,
            createFolderNodeUseCase,
            getFeatureFlagValueUseCase,
            nodeExistsInCurrentLocationUseCase,
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase,
            removeDeviceFolderConnectionUseCase,
            monitorMegaPickerFolderNodesUseCase
        )
    }

    @Test
    fun `test that viewmodel fetches root folder and its children upon initialization`() = runTest {
        val rootFolderId = NodeId(123456L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)

        initViewModel()

        val expectedState = MegaPickerState(
            currentFolder = rootFolder,
            nodes = typedNodeUiModels,
            isSelectEnabled = false
        )

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that viewmodel disables CU and chat directories`() = runTest {
        val rootFolderId = NodeId(123456L)
        val cameraUploadsFolderId = NodeId(146L)
        val mediaUploadsFolderId = NodeId(147L)
        val mediaUploadsFolder = mock<TypedNode> {
            on { id } doReturn mediaUploadsFolderId
        }
        val chatFilesFolderId = NodeId(3211L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        val childrenNodesWithCUAndChat =
            listOf(
                cameraUploadsFolderId, mediaUploadsFolderId, chatFilesFolderId
            ).map { nodeId ->
                mock<TypedNode> { on { id } doReturn nodeId }
            }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        initViewModel(
            folderNodesStub = { _, _, _, _ ->
                flowOf(
                    MegaPickerFolderResult(
                        currentFolder = rootFolder,
                        nodes = childrenNodesWithCUAndChat.map {
                            MegaPickerNodeInfo(
                                it,
                                isDisabled = true
                            )
                        },
                        isSelectEnabled = false
                    )
                )
            }
        )
        val expectedState = MegaPickerState(
            currentFolder = rootFolder,
            nodes = childrenNodesWithCUAndChat.map {
                TypedNodeUiModel(it, true)
            },
            isSelectEnabled = false
        )

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that folder click fetches children of clicked folder`() = runTest {
        val rootFolderId = NodeId(123456L)
        val rootFolder: FolderNode = mock { on { id } doReturn rootFolderId }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)

        initViewModel()
        val clickedFolderId = NodeId(9845748L)
        val clickedFolder: TypedFolderNode = mock {
            on { id } doReturn clickedFolderId
        }
        whenever(
            monitorMegaPickerFolderNodesUseCase(
                any(),
                nullable(NodeId::class.java),
                anyBoolean(),
                nullable(String::class.java)
            )
        ).thenAnswer { invocation ->
            val currentFolder = invocation.getArgument<Node>(0)
            val rootFolderId = invocation.getArgument<NodeId?>(1)
            flowOf(
                MegaPickerFolderResult(
                    currentFolder = currentFolder,
                    nodes = emptyList(),
                    isSelectEnabled = currentFolder.id != rootFolderId
                )
            )
        }
        val expectedState = MegaPickerState(
            currentFolder = clickedFolder,
            nodes = typedNodeUiModels,
            isSelectEnabled = true,
        )

        underTest.handleAction(MegaPickerAction.FolderClicked(clickedFolder))

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that back click fetches children of parent folder`() = runTest {
        val currentFolderId = NodeId(43434L)
        val parentFolderId = NodeId(9845748L)
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { parentId } doReturn parentFolderId
        }
        val parentFolder: TypedFolderNode = mock {
            on { id } doReturn parentFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(getNodeByHandleUseCase(parentFolderId.longValue)).thenReturn(parentFolder)
        whenever(
            monitorMegaPickerFolderNodesUseCase(
                any(),
                nullable(NodeId::class.java),
                anyBoolean(),
                nullable(String::class.java)
            )
        ).thenAnswer { invocation ->
            val currentFolder = invocation.getArgument<Node>(0)
            val rootFolderId = invocation.getArgument<NodeId?>(1)
            flowOf(
                MegaPickerFolderResult(
                    currentFolder = currentFolder,
                    nodes = emptyList(),
                    isSelectEnabled = currentFolder.id != rootFolderId
                )
            )
        }

        initViewModel()

        underTest.handleAction(MegaPickerAction.BackClicked)

        val expectedState = MegaPickerState(
            currentFolder = parentFolder,
            nodes = typedNodeUiModels,
            isSelectEnabled = true,
        )

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that folder selection sets selected folder`() = runTest {
        val currentFolderId = NodeId(2323L)
        val currentFolderName = "some secret folder"
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { name } doReturn currentFolderName
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(
            monitorMegaPickerFolderNodesUseCase(
                any(),
                nullable(NodeId::class.java),
                anyBoolean(),
                nullable(String::class.java)
            )
        ).thenAnswer { invocation ->
            val currentFolder = invocation.getArgument<Node>(0)
            val rootFolderId = invocation.getArgument<NodeId?>(1)
            flowOf(
                MegaPickerFolderResult(
                    currentFolder = currentFolder,
                    nodes = emptyList(),
                    isSelectEnabled = currentFolder.id != rootFolderId
                )
            )
        }
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        verify(setSelectedMegaFolderUseCase).invoke(
            RemoteFolder(
                NodeId(currentFolderId.longValue), currentFolderName
            )
        )
    }

    @Test
    fun `test that all files access permission is shown when it is not granted`() = runTest {
        whenever(tryNodeSyncUseCase(NodeId(0))).thenReturn(Unit)

        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = false,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        underTest.state.test {
            assertThat(awaitItem().showAllFilesAccessDialog).isEqualTo(true)
        }
    }

    @Test
    fun `test that all files access permission is not shown when it is granted`() = runTest {
        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        underTest.state.test {
            assertThat(awaitItem().showAllFilesAccessDialog).isEqualTo(false)
        }
    }

    @Test
    fun `test that disable battery optimization permission is shown when it is not granted`() =
        runTest {
            initViewModel()
            whenever(getFeatureFlagValueUseCase.invoke(any())).thenReturn(true)

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = false
                )
            )

            underTest.state.test {
                assertThat(awaitItem().showDisableBatteryOptimizationsDialog).isEqualTo(true)
            }
        }

    @Test
    fun `test that disable battery optimization permission is not shown when it is granted`() =
        runTest {
            initViewModel()

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            underTest.state.test {
                assertThat(awaitItem().showDisableBatteryOptimizationsDialog).isEqualTo(
                    false
                )
            }
        }

    @Test
    fun `test that error is shown when selected directory has an error`() =
        runTest {
            val currentFolderId = NodeId(2323L)
            val currentFolderName = "some secret folder"
            val currentFolder: TypedFolderNode = mock {
                on { id } doReturn currentFolderId
                on { name } doReturn currentFolderName
            }
            whenever(getRootNodeUseCase()).thenReturn(currentFolder)
            val errorCode = 18
            val syncError = SyncError.ACTIVE_SYNC_ABOVE_PATH
            val errorStringRes = 12345
            val error = MegaSyncException(
                errorCode, "error", syncError = syncError
            )
            whenever(deviceFolderUINodeErrorMessageMapper(syncError)).thenReturn(errorStringRes)
            doAnswer { throw error }.whenever(tryNodeSyncUseCase).invoke(currentFolderId)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            initViewModel()

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            val event = underTest.state.value.snackbarMessageId
            assertThat(event).isEqualTo(errorStringRes)
        }

    @Test
    fun `test that error message event is null when viewmodel handle action is called`() = runTest {
        initViewModel()

        underTest.handleAction(
            MegaPickerAction.SnackbarShown
        )

        underTest.state.test {
            assertThat(awaitItem().snackbarMessageId).isEqualTo(null)
        }
    }

    @Test
    fun `test that all files access dialog is hidden when dialog shown action is called`() =
        runTest {
            initViewModel()

            underTest.handleAction(MegaPickerAction.AllFilesAccessPermissionDialogShown)

            underTest.state.test {
                assertThat(awaitItem().showAllFilesAccessDialog).isEqualTo(false)
            }
        }

    @Test
    fun `test that disable battery optimizations dialog is hidden when dialog shown action is called`() =
        runTest {
            initViewModel()

            underTest.handleAction(MegaPickerAction.DisableBatteryOptimizationsDialogShown)

            underTest.state.test {
                assertThat(awaitItem().showDisableBatteryOptimizationsDialog).isEqualTo(false)
            }
        }

    @Test
    fun `test that navigate next event is consumed when next screen opened action is called`() =
        runTest {
            initViewModel()

            underTest.handleAction(MegaPickerAction.NextScreenOpened)

            underTest.state.test {
                assertThat(awaitItem().navigateNextEvent).isEqualTo(de.palm.composestateevents.consumed)
            }
        }

    @Test
    fun `test that navigate next event is triggered when folder is selected with all permissions`() =
        runTest {
            val currentFolderId = NodeId(2323L)
            val currentFolderName = "some secret folder"
            val currentFolder: TypedFolderNode = mock {
                on { id } doReturn currentFolderId
                on { name } doReturn currentFolderName
            }
            whenever(getRootNodeUseCase()).thenReturn(currentFolder)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            whenever(tryNodeSyncUseCase(currentFolderId)).thenReturn(Unit)

            initViewModel()

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            underTest.state.test {
                assertThat(awaitItem().navigateNextEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that createFolder creates new folder and fetches its children`() = runTest {
        val parentFolderId = NodeId(123L)
        val newFolderId = NodeId(456L)
        val parentFolder: FolderNode = mock {
            on { id } doReturn parentFolderId
        }
        val newFolder: FolderNode = mock {
            on { id } doReturn newFolderId
        }

        whenever(createFolderNodeUseCase(name = "New Folder", parentNodeId = parentFolderId))
            .thenReturn(newFolderId)
        whenever(getNodeByHandleUseCase(newFolderId.longValue)).thenReturn(newFolder)
        initViewModel()

        underTest.createFolder("New Folder", parentFolder)

        verify(createFolderNodeUseCase).invoke(
            name = "New Folder",
            parentNodeId = parentFolderId
        )
        verify(getNodeByHandleUseCase).invoke(newFolderId.longValue)
    }

    @Test
    fun `test that isSelectEnabled is false when folder exists in stop backup mode`() = runTest {
        val rootFolderId = NodeId(123456L)
        val currentFolderId = NodeId(9845748L)
        val folderName = "ExistingFolder"
        val rootFolder: FolderNode = mock { on { id } doReturn rootFolderId }
        val clickedFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(currentFolderId, folderName)).thenReturn(true)

        initViewModel(
            isStopBackup = true,
            folderName = folderName,
            folderNodesStub = { currentFolder, rootFolderId, isStopBackupParam, folderNameParam ->
                val isSelectEnabled = when {
                    isStopBackupParam && folderNameParam == folderName && currentFolder.id == currentFolderId -> false
                    else -> currentFolder.id != rootFolderId
                }
                flowOf(
                    MegaPickerFolderResult(
                        currentFolder = currentFolder,
                        nodes = emptyList(),
                        isSelectEnabled = isSelectEnabled
                    )
                )
            }
        )

        underTest.handleAction(MegaPickerAction.FolderClicked(clickedFolder))

        underTest.state.test {
            assertThat(awaitItem().isSelectEnabled).isEqualTo(false)
        }
    }

    @Test
    fun `test that loading state is set to true when fetching folders`() = runTest {
        val rootFolderId = NodeId(123456L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel(
            folderNodesStub = { currentFolder, rootFolderId, _, _ ->
                flow {
                    kotlinx.coroutines.delay(100)
                    emit(
                        MegaPickerFolderResult(
                            currentFolder = currentFolder,
                            nodes = emptyList(),
                            isSelectEnabled = currentFolder.id != rootFolderId
                        )
                    )
                    awaitCancellation()
                }
            }
        )

        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isEqualTo(true)
            val finalState = awaitItem()
            assertThat(finalState.isLoading).isEqualTo(false)
        }
    }

    @Test
    fun `test that loading state is set to true when clicking folder`() = runTest {
        val rootFolderId = NodeId(123456L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        val clickedFolderId = NodeId(9845748L)
        val clickedFolder: TypedFolderNode = mock {
            on { id } doReturn clickedFolderId
        }

        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel(
            folderNodesStub = { currentFolder, rootFolderId, _, _ ->
                flow {
                    kotlinx.coroutines.delay(100)
                    emit(
                        MegaPickerFolderResult(
                            currentFolder = currentFolder,
                            nodes = emptyList(),
                            isSelectEnabled = currentFolder.id != rootFolderId
                        )
                    )
                    awaitCancellation()
                }
            }
        )

        underTest.handleAction(MegaPickerAction.FolderClicked(clickedFolder))

        underTest.state.test {
            awaitItem()
            val finalState = awaitItem()
            assertThat(finalState.isLoading).isEqualTo(false)
        }
    }

    @Test
    fun `test that error is shown when getMegaPickerFolderNodesUseCase fails`() = runTest {
        val rootFolderId = NodeId(123456L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel(
            folderNodesStub = { _, _, _, _ -> flow { awaitCancellation() } }
        )

        testScheduler.advanceUntilIdle()

        val state = underTest.state.value
        assertThat(state.isLoading).isEqualTo(true)
    }

    @Test
    fun `test that back click does nothing when current folder is null`() = runTest {
        // This test verifies that when the ViewModel is initialized but no folder is set,
        // back click doesn't perform any action
        whenever(getRootNodeUseCase()).thenReturn(null)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel()

        underTest.handleAction(MegaPickerAction.BackClicked)

        // Verify no additional calls are made since current folder is null
        verifyNoInteractions(getNodeByHandleUseCase)
    }

    @Test
    fun `test that back click does nothing when parent node children is null`() = runTest {
        val currentFolderId = NodeId(43434L)
        val parentFolderId = NodeId(9845748L)
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { parentId } doReturn parentFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(
            monitorMegaPickerFolderNodesUseCase(
                any(),
                nullable(NodeId::class.java),
                anyBoolean(),
                nullable(String::class.java)
            )
        ).thenAnswer { invocation ->
            val currentFolder = invocation.getArgument<Node>(0)
            val rootFolderId = invocation.getArgument<NodeId?>(1)
            flowOf(
                MegaPickerFolderResult(
                    currentFolder = currentFolder,
                    nodes = emptyList(),
                    isSelectEnabled = currentFolder.id != rootFolderId
                )
            )
        }
        whenever(getNodeByHandleUseCase(parentFolderId.longValue)).thenReturn(null)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel()

        underTest.handleAction(MegaPickerAction.BackClicked)

        verify(getNodeByHandleUseCase).invoke(parentFolderId.longValue)
    }

    @Test
    fun `test that folder creation handles error when createFolderNodeUseCase fails`() = runTest {
        val parentFolderId = NodeId(123L)
        val parentFolder: FolderNode = mock {
            on { id } doReturn parentFolderId
        }

        whenever(createFolderNodeUseCase(name = "New Folder", parentNodeId = parentFolderId))
            .thenThrow(RuntimeException("Create folder failed"))

        initViewModel()

        underTest.createFolder("New Folder", parentFolder)

        verifyNoInteractions(getNodeByHandleUseCase)
    }

    @Test
    fun `test that folder creation handles error when getNodeByHandleUseCase fails`() = runTest {
        val parentFolderId = NodeId(123L)
        val newFolderId = NodeId(456L)
        val parentFolder: FolderNode = mock {
            on { id } doReturn parentFolderId
        }

        whenever(createFolderNodeUseCase(name = "New Folder", parentNodeId = parentFolderId))
            .thenReturn(newFolderId)
        whenever(getNodeByHandleUseCase(newFolderId.longValue))
            .thenThrow(RuntimeException("Get node failed"))

        initViewModel()

        underTest.createFolder("New Folder", parentFolder)

        verify(createFolderNodeUseCase).invoke(
            name = "New Folder",
            parentNodeId = parentFolderId
        )
        verify(getNodeByHandleUseCase).invoke(newFolderId.longValue)
        verify(monitorMegaPickerFolderNodesUseCase, never()).invoke(
            any(),
            nullable(NodeId::class.java),
            anyBoolean(),
            nullable(String::class.java)
        )
    }

    @Test
    fun `test that folder creation does nothing when parent node is null`() = runTest {
        initViewModel()

        underTest.createFolder("New Folder", null)

        verify(createFolderNodeUseCase, never()).invoke(any(), any())
    }

    @Test
    fun `test that folder creation does nothing when getNodeByHandleUseCase returns null`() =
        runTest {
            val parentFolderId = NodeId(123L)
            val newFolderId = NodeId(456L)
            val parentFolder: FolderNode = mock {
                on { id } doReturn parentFolderId
            }

            whenever(createFolderNodeUseCase(name = "New Folder", parentNodeId = parentFolderId))
                .thenReturn(newFolderId)
            whenever(getNodeByHandleUseCase(newFolderId.longValue)).thenReturn(null)

            initViewModel()

            underTest.createFolder("New Folder", parentFolder)

            verifyNoInteractions(monitorMegaPickerFolderNodesUseCase)
        }

    @Test
    fun `test that disable battery optimization permission is shown when feature flag is enabled and permission not granted`() =
        runTest {
            initViewModel()
            whenever(getFeatureFlagValueUseCase.invoke(any())).thenReturn(true)

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = false
                )
            )

            underTest.state.test {
                assertThat(awaitItem().showDisableBatteryOptimizationsDialog).isEqualTo(true)
            }
        }

    @Test
    fun `test that disable battery optimization permission is not shown when feature flag is disabled`() =
        runTest {
            initViewModel()
            whenever(getFeatureFlagValueUseCase.invoke(any())).thenReturn(false)

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = false
                )
            )

            underTest.state.test {
                assertThat(awaitItem().showDisableBatteryOptimizationsDialog).isEqualTo(false)
            }
        }

    @Test
    fun `test that folder selection with all permissions granted triggers navigation`() = runTest {
        val currentFolderId = NodeId(2323L)
        val currentFolderName = "some secret folder"
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { name } doReturn currentFolderName
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(tryNodeSyncUseCase(currentFolderId)).thenReturn(Unit)

        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.navigateNextEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that folder selection in stop backup mode checks folder existence`() = runTest {
        val currentFolderId = NodeId(2323L)
        val currentFolderName = "some secret folder"
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { name } doReturn currentFolderName
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(nodeExistsInCurrentLocationUseCase(currentFolderId, "TestFolder")).thenReturn(true)

        initViewModel(isStopBackup = true, folderName = "TestFolder")

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        verify(nodeExistsInCurrentLocationUseCase, times(1)).invoke(currentFolderId, "TestFolder")
        verify(tryNodeSyncUseCase, never()).invoke(any())
    }

    @Test
    fun `test that folder selection in stop backup mode proceeds when folder does not exist`() =
        runTest {
            val currentFolderId = NodeId(2323L)
            val currentFolderName = "some secret folder"
            val currentFolder: TypedFolderNode = mock {
                on { id } doReturn currentFolderId
                on { name } doReturn currentFolderName
            }
            whenever(getRootNodeUseCase()).thenReturn(currentFolder)
            whenever(nodeExistsInCurrentLocationUseCase(currentFolderId, "TestFolder")).thenReturn(
                false
            )

            initViewModel(isStopBackup = true, folderName = "TestFolder")

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            verify(nodeExistsInCurrentLocationUseCase, times(1)).invoke(
                currentFolderId,
                "TestFolder"
            )
            verifyNoInteractions(tryNodeSyncUseCase)
        }

    @Test
    fun `test that folder selection in stop backup mode proceeds when folder name is null`() =
        runTest {
            val currentFolderId = NodeId(2323L)
            val currentFolderName = "some secret folder"
            val currentFolder: TypedFolderNode = mock {
                on { id } doReturn currentFolderId
                on { name } doReturn currentFolderName
            }
            whenever(getRootNodeUseCase()).thenReturn(currentFolder)
            initViewModel(isStopBackup = true, folderName = null)

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            verify(nodeExistsInCurrentLocationUseCase, never()).invoke(any(), any())
            verify(tryNodeSyncUseCase, never()).invoke(any())
        }

    @Test
    fun `test that error message is shown when deviceFolderUINodeErrorMessageMapper returns null`() =
        runTest {
            val currentFolderId = NodeId(2323L)
            val currentFolderName = "some secret folder"
            val currentFolder: TypedFolderNode = mock {
                on { id } doReturn currentFolderId
                on { name } doReturn currentFolderName
            }
            whenever(getRootNodeUseCase()).thenReturn(currentFolder)
            val errorCode = 18
            val syncError = SyncError.ACTIVE_SYNC_ABOVE_PATH
            val error = MegaSyncException(
                errorCode, "error", syncError = syncError
            )
            whenever(deviceFolderUINodeErrorMessageMapper(syncError)).thenReturn(null)
            whenever(deviceFolderUINodeErrorMessageMapper(SyncError.UNKNOWN_ERROR)).thenReturn(99999)
            doAnswer { throw error }.whenever(tryNodeSyncUseCase).invoke(currentFolderId)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            initViewModel()

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            val event = underTest.state.value.snackbarMessageId
            assertThat(event).isEqualTo(99999)
        }

    @Test
    fun `test that folder selection does nothing when current folder is null`() = runTest {
        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        verifyNoInteractions(tryNodeSyncUseCase, setSelectedMegaFolderUseCase)
    }

    @Test
    fun `test that saveSelectedFolder does nothing when current folder is null`() = runTest {
        // This test verifies that when the ViewModel is initialized but no folder is set,
        // folder selection doesn't perform any action
        whenever(getRootNodeUseCase()).thenReturn(null)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        verify(setSelectedMegaFolderUseCase, never()).invoke(any())
    }

    @Test
    fun `test that folders are excluded when getting handles fails`() = runTest {
        val rootFolderId = NodeId(123456L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        val childFolderId = NodeId(789L)
        val childFolder: TypedNode = mock {
            on { id } doReturn childFolderId
        }
        val childrenNodesWithFolder = listOf(childFolder)

        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel(
            folderNodesStub = { _, _, _, _ ->
                flowOf(
                    MegaPickerFolderResult(
                        currentFolder = rootFolder,
                        nodes = listOf(MegaPickerNodeInfo(node = childFolder, isDisabled = false)),
                        isSelectEnabled = false
                    )
                )
            }
        )

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.nodes).isNotNull()
            assertThat(state.nodes?.size).isEqualTo(1)
            assertThat(state.nodes?.first()?.isDisabled).isEqualTo(false)
        }
    }

    @Test
    fun `test that isSelectEnabled is true when not in stop backup mode and not root folder`() =
        runTest {
            val rootFolderId = NodeId(123456L)
            val currentFolderId = NodeId(789L)
            val rootFolder: FolderNode = mock {
                on { id } doReturn rootFolderId
            }
            val currentFolder: TypedFolderNode = mock {
                on { id } doReturn currentFolderId
            }

            whenever(getRootNodeUseCase()).thenReturn(rootFolder)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

            initViewModel()

            underTest.handleAction(MegaPickerAction.FolderClicked(currentFolder))

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isSelectEnabled).isEqualTo(true)
            }
        }

    @Test
    fun `test that isSelectEnabled is false when in root folder and not stop backup mode`() =
        runTest {
            val rootFolderId = NodeId(123456L)
            val rootFolder: FolderNode = mock {
                on { id } doReturn rootFolderId
            }

            whenever(getRootNodeUseCase()).thenReturn(rootFolder)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

            initViewModel()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isSelectEnabled).isEqualTo(false)
            }
        }

    @Test
    fun `test that folder creation handles success when getNodeByHandleUseCase returns valid node`() =
        runTest {
            val parentFolderId = NodeId(123L)
            val newFolderId = NodeId(456L)
            val parentFolder: FolderNode = mock {
                on { id } doReturn parentFolderId
            }
            val newFolder: FolderNode = mock {
                on { id } doReturn newFolderId
            }

            whenever(createFolderNodeUseCase(name = "New Folder", parentNodeId = parentFolderId))
                .thenReturn(newFolderId)
            whenever(getNodeByHandleUseCase(newFolderId.longValue)).thenReturn(newFolder)
            initViewModel()

            underTest.createFolder("New Folder", parentFolder)

            verify(createFolderNodeUseCase).invoke(
                name = "New Folder",
                parentNodeId = parentFolderId
            )
            verify(getNodeByHandleUseCase).invoke(newFolderId.longValue)
            verify(monitorMegaPickerFolderNodesUseCase).invoke(
                any(),
                nullable(NodeId::class.java),
                anyBoolean(),
                nullable(String::class.java)
            )
        }

    @Test
    fun `test that permission flow works correctly when all files permission is shown first`() =
        runTest {
            val currentFolderId = NodeId(2323L)
            val currentFolderName = "some secret folder"
            val currentFolder: TypedFolderNode = mock {
                on { id } doReturn currentFolderId
                on { name } doReturn currentFolderName
            }
            whenever(getRootNodeUseCase()).thenReturn(currentFolder)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            whenever(tryNodeSyncUseCase(currentFolderId)).thenReturn(Unit)

            initViewModel()

            // First, select folder without all files permission
            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = false,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.showAllFilesAccessDialog).isEqualTo(true)
                assertThat(state.showDisableBatteryOptimizationsDialog).isEqualTo(false)
            }

            // Then, dismiss the all files permission dialog
            underTest.handleAction(MegaPickerAction.AllFilesAccessPermissionDialogShown)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.showAllFilesAccessDialog).isEqualTo(false)
            }

            // Then, select folder again with all permissions
            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.navigateNextEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that error is shown when selected folder is used by camera uploads`() = runTest {
        val currentFolderId = NodeId(2323L)
        val currentFolderName = "some folder"
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { name } doReturn currentFolderName
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId = currentFolderId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.UsedByCameraUpload)

        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        val event = underTest.state.value.snackbarMessageId
        assertThat(event).isEqualTo(sharedR.string.error_folder_part_of_camera_uploads)
        verifyNoInteractions(tryNodeSyncUseCase)
    }

    @Test
    fun `test that error is shown when selected folder is used by media uploads`() = runTest {
        val currentFolderId = NodeId(2323L)
        val currentFolderName = "some folder"
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { name } doReturn currentFolderName
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId = currentFolderId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.UsedByMediaUpload)

        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        val event = underTest.state.value.snackbarMessageId
        assertThat(event).isEqualTo(sharedR.string.error_folder_part_of_media_uploads)
        verifyNoInteractions(tryNodeSyncUseCase)
    }

    @Test
    fun `test that error is shown when selected folder is used by sync or backup`() = runTest {
        val currentFolderId = NodeId(2323L)
        val currentFolderName = "some folder"
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { name } doReturn currentFolderName
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId = currentFolderId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.UsedBySyncOrBackup(deviceId = "device123"))

        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        val event = underTest.state.value.snackbarMessageId
        assertThat(event).isEqualTo(sharedR.string.error_folder_part_of_sync_or_backup)
        verifyNoInteractions(tryNodeSyncUseCase)
    }

    @Test
    fun `test that error is shown when selected folder is parent of camera uploads`() = runTest {
        val currentFolderId = NodeId(2323L)
        val currentFolderName = "some folder"
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { name } doReturn currentFolderName
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId = currentFolderId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.UsedByCameraUploadParent)

        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        val event = underTest.state.value.snackbarMessageId
        assertThat(event).isEqualTo(sharedR.string.error_folder_part_of_camera_uploads)
        verifyNoInteractions(tryNodeSyncUseCase)
    }

    @Test
    fun `test that folder selection proceeds when isFolderUsedBySyncOrBackupAcrossDevicesUseCase throws`() =
        runTest {
            val currentFolderId = NodeId(2323L)
            val currentFolderName = "some folder"
            val currentFolder: TypedFolderNode = mock {
                on { id } doReturn currentFolderId
                on { name } doReturn currentFolderName
            }
            whenever(getRootNodeUseCase()).thenReturn(currentFolder)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            whenever(tryNodeSyncUseCase(currentFolderId)).thenReturn(Unit)
            whenever(
                isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                    nodeId = currentFolderId,
                    shouldCheckCameraUploads = true,
                    shouldExcludeCurrentDevice = false
                )
            ).thenThrow(RuntimeException("API error"))

            initViewModel()

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.navigateNextEvent).isEqualTo(triggered)
                assertThat(state.snackbarMessageId).isNull()
            }
        }

    @Test
    fun `test that folder selection proceeds when folder is not used`() = runTest {
        val currentFolderId = NodeId(2323L)
        val currentFolderName = "some folder"
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
            on { name } doReturn currentFolderName
        }
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(tryNodeSyncUseCase(currentFolderId)).thenReturn(Unit)
        whenever(
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase(
                nodeId = currentFolderId,
                shouldCheckCameraUploads = true,
                shouldExcludeCurrentDevice = false
            )
        ).thenReturn(FolderUsageResult.NotUsed)

        initViewModel()

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.navigateNextEvent).isEqualTo(triggered)
            assertThat(state.snackbarMessageId).isNull()
        }
    }

    @Test
    fun `test that child nodes are disabled when they match backup or sync folders`() = runTest {
        val rootFolderId = NodeId(123456L)
        val clickedFolderId = NodeId(555L)
        val childFolderId = NodeId(789L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        val clickedFolder: TypedFolderNode = mock {
            on { id } doReturn clickedFolderId
        }
        val childFolder: TypedNode = mock {
            on { id } doReturn childFolderId
        }

        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel(
            folderNodesStub = { currentFolder, rootFolderId, _, _ ->
                if (currentFolder.id == clickedFolderId) {
                    flowOf(
                        MegaPickerFolderResult(
                            currentFolder = clickedFolder,
                            nodes = listOf(
                                MegaPickerNodeInfo(
                                    node = childFolder,
                                    isDisabled = true
                                )
                            ),
                            isSelectEnabled = true
                        )
                    )
                } else {
                    flowOf(
                        MegaPickerFolderResult(
                            currentFolder = currentFolder,
                            nodes = emptyList(),
                            isSelectEnabled = currentFolder.id != rootFolderId
                        )
                    )
                }
            }
        )

        underTest.handleAction(MegaPickerAction.FolderClicked(clickedFolder))

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.nodes).isNotNull()
            assertThat(state.nodes?.size).isEqualTo(1)
            assertThat(state.nodes?.first()?.isDisabled).isEqualTo(true)
        }
    }

    @Test
    fun `test that child nodes are not disabled when feature flag is disabled`() = runTest {
        val rootFolderId = NodeId(123456L)
        val childFolderId = NodeId(789L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        val childFolder: TypedNode = mock {
            on { id } doReturn childFolderId
        }

        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel(
            folderNodesStub = { _, _, _, _ ->
                flowOf(
                    MegaPickerFolderResult(
                        currentFolder = rootFolder,
                        nodes = listOf(MegaPickerNodeInfo(node = childFolder, isDisabled = false)),
                        isSelectEnabled = false
                    )
                )
            }
        )

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.nodes).isNotNull()
            assertThat(state.nodes?.size).isEqualTo(1)
            assertThat(state.nodes?.first()?.isDisabled).isEqualTo(false)
        }
    }

    @Test
    fun `test that child nodes are not disabled when getBackupInfoUseCase fails`() = runTest {
        val rootFolderId = NodeId(123456L)
        val clickedFolderId = NodeId(555L)
        val childFolderId = NodeId(789L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        val clickedFolder: TypedFolderNode = mock {
            on { id } doReturn clickedFolderId
        }
        val childFolder: TypedNode = mock {
            on { id } doReturn childFolderId
        }

        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel(
            folderNodesStub = { currentFolder, rootFolderId, _, _ ->
                if (currentFolder.id == clickedFolderId) {
                    flowOf(
                        MegaPickerFolderResult(
                            currentFolder = clickedFolder,
                            nodes = listOf(
                                MegaPickerNodeInfo(
                                    node = childFolder,
                                    isDisabled = false
                                )
                            ),
                            isSelectEnabled = true
                        )
                    )
                } else {
                    flowOf(
                        MegaPickerFolderResult(
                            currentFolder = currentFolder,
                            nodes = emptyList(),
                            isSelectEnabled = currentFolder.id != rootFolderId
                        )
                    )
                }
            }
        )

        underTest.handleAction(MegaPickerAction.FolderClicked(clickedFolder))

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.nodes).isNotNull()
            assertThat(state.nodes?.size).isEqualTo(1)
            assertThat(state.nodes?.first()?.isDisabled).isEqualTo(false)
        }
    }

    @Test
    fun `test that child nodes are not disabled when determineNodeRelationshipUseCase returns NoMatch`() =
        runTest {
            val rootFolderId = NodeId(123456L)
            val backupFolderId = NodeId(999L)
            val clickedFolderId = NodeId(555L)
            val childFolderId = NodeId(789L)
            val rootFolder: FolderNode = mock {
                on { id } doReturn rootFolderId
            }
            val clickedFolder: TypedFolderNode = mock {
                on { id } doReturn clickedFolderId
            }
            val childFolder: TypedNode = mock {
                on { id } doReturn childFolderId
            }

            whenever(getRootNodeUseCase()).thenReturn(rootFolder)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

            initViewModel(
                folderNodesStub = { currentFolder, rootFolderId, _, _ ->
                    if (currentFolder.id == clickedFolderId) {
                        flowOf(
                            MegaPickerFolderResult(
                                currentFolder = clickedFolder,
                                nodes = listOf(
                                    MegaPickerNodeInfo(
                                        node = childFolder,
                                        isDisabled = false
                                    )
                                ),
                                isSelectEnabled = true
                            )
                        )
                    } else {
                        flowOf(
                            MegaPickerFolderResult(
                                currentFolder = currentFolder,
                                nodes = emptyList(),
                                isSelectEnabled = currentFolder.id != rootFolderId
                            )
                        )
                    }
                }
            )

            underTest.handleAction(MegaPickerAction.FolderClicked(clickedFolder))

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.nodes).isNotNull()
                assertThat(state.nodes?.size).isEqualTo(1)
                assertThat(state.nodes?.first()?.isDisabled).isEqualTo(false)
            }
        }

    @Test
    fun `test that DisabledFolderClicked shows dialog when backupId is not null`() = runTest {
        val rootFolder: FolderNode = mock { on { id } doReturn NodeId(123456L) }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel()

        val node = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Folder"
        }
        val nodeUiModel = TypedNodeUiModel(
            node = node,
            isDisabled = true,
            backupId = 123L,
            deviceName = "My Laptop"
        )

        underTest.handleAction(MegaPickerAction.DisabledFolderClicked(nodeUiModel))

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showRemoveConnectionDialog).isTrue()
            assertThat(state.selectedDisabledFolder).isEqualTo(nodeUiModel)
        }
    }

    @Test
    fun `test that DisabledFolderClicked does nothing when backupId is null`() = runTest {
        val rootFolder: FolderNode = mock { on { id } doReturn NodeId(123456L) }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel()

        val node = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Folder"
        }
        val nodeUiModel = TypedNodeUiModel(
            node = node,
            isDisabled = true,
            backupId = null,
            deviceName = null
        )

        underTest.handleAction(MegaPickerAction.DisabledFolderClicked(nodeUiModel))

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showRemoveConnectionDialog).isFalse()
            assertThat(state.selectedDisabledFolder).isNull()
        }
    }

    @Test
    fun `test that RemoveConnectionConfirmed removes connection when backupId is not null`() =
        runTest {
            val rootFolder: FolderNode = mock { on { id } doReturn NodeId(123456L) }
            val currentFolder: TypedFolderNode = mock {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Folder"
            }
            whenever(getRootNodeUseCase()).thenReturn(rootFolder)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            whenever(removeDeviceFolderConnectionUseCase(any())).thenReturn(
                BackupRemovalStatus(
                    backupId = 123L,
                    isOutdated = false
                )
            )

            initViewModel()

            // First set up the state with a selected disabled folder
            val nodeUiModel = TypedNodeUiModel(
                node = currentFolder,
                isDisabled = true,
                backupId = 123L,
                deviceName = "My Laptop"
            )
            underTest.handleAction(MegaPickerAction.DisabledFolderClicked(nodeUiModel))

            // Then confirm removal
            underTest.handleAction(MegaPickerAction.RemoveConnectionConfirmed)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.showRemoveConnectionDialog).isFalse()
                assertThat(state.selectedDisabledFolder).isNull()
                assertThat(state.snackbarMessageId)
                    .isEqualTo(sharedR.string.device_center_snackbar_message_connection_removed)
            }
            verify(removeDeviceFolderConnectionUseCase).invoke(123L)
        }

    @Test
    fun `test that RemoveConnectionConfirmed dismisses dialog when backupId is null`() = runTest {
        val rootFolder: FolderNode = mock { on { id } doReturn NodeId(123456L) }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel()

        // Manually set state with null backupId (edge case)
        val node = mock<TypedFolderNode> {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Folder"
        }
        val nodeUiModel = TypedNodeUiModel(
            node = node,
            isDisabled = true,
            backupId = null,
            deviceName = null
        )

        underTest.handleAction(MegaPickerAction.DisabledFolderClicked(nodeUiModel))
        underTest.handleAction(MegaPickerAction.RemoveConnectionConfirmed)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showRemoveConnectionDialog).isFalse()
            assertThat(state.selectedDisabledFolder).isNull()
        }
        verify(removeDeviceFolderConnectionUseCase, never()).invoke(any())
    }

    @Test
    fun `test that RemoveConnectionConfirmed shows error when removal fails`() = runTest {
        val rootFolder: FolderNode = mock { on { id } doReturn NodeId(123456L) }
        val currentFolder: TypedFolderNode = mock {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "Test Folder"
        }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(removeDeviceFolderConnectionUseCase(any())).thenThrow(RuntimeException("Network error"))

        initViewModel()

        val nodeUiModel = TypedNodeUiModel(
            node = currentFolder,
            isDisabled = true,
            backupId = 123L,
            deviceName = "My Laptop"
        )
        underTest.handleAction(MegaPickerAction.DisabledFolderClicked(nodeUiModel))
        underTest.handleAction(MegaPickerAction.RemoveConnectionConfirmed)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showRemoveConnectionDialog).isFalse()
            assertThat(state.selectedDisabledFolder).isNull()
            assertThat(state.snackbarMessageId).isEqualTo(sharedR.string.general_text_error)
        }
    }

    @Test
    fun `test that RemoveConnectionDialogDismissed hides dialog and clears selected folder`() =
        runTest {
            val rootFolder: FolderNode = mock { on { id } doReturn NodeId(123456L) }
            whenever(getRootNodeUseCase()).thenReturn(rootFolder)
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

            initViewModel()

            val node = mock<TypedFolderNode> {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "Test Folder"
            }
            val nodeUiModel = TypedNodeUiModel(
                node = node,
                isDisabled = true,
                backupId = 123L,
                deviceName = "My Laptop"
            )

            underTest.handleAction(MegaPickerAction.DisabledFolderClicked(nodeUiModel))
            underTest.handleAction(MegaPickerAction.RemoveConnectionDialogDismissed)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.showRemoveConnectionDialog).isFalse()
                assertThat(state.selectedDisabledFolder).isNull()
            }
        }

    private fun initViewModel(
        isStopBackup: Boolean = false,
        folderName: String? = null,
        folderNodesStub: ((Node, NodeId?, Boolean, String?) -> kotlinx.coroutines.flow.Flow<MegaPickerFolderResult>)? = null,
    ) {
        wheneverBlocking { getFeatureFlagValueUseCase.invoke(any()) }.thenReturn(false)
        if (folderNodesStub != null) {
            whenever(
                monitorMegaPickerFolderNodesUseCase(
                    any(),
                    nullable(NodeId::class.java),
                    anyBoolean(),
                    nullable(String::class.java)
                )
            ).thenAnswer { invocation ->
                folderNodesStub(
                    invocation.getArgument(0),
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                    invocation.getArgument(3),
                )
            }
        } else {
            whenever(
                monitorMegaPickerFolderNodesUseCase(
                    any(),
                    nullable(NodeId::class.java),
                    anyBoolean(),
                    nullable(String::class.java)
                )
            ).thenAnswer { invocation ->
                val currentFolder = invocation.getArgument<Node>(0)
                val rootFolderId = invocation.getArgument<NodeId?>(1)
                flowOf(
                    MegaPickerFolderResult(
                        currentFolder = currentFolder,
                        nodes = emptyList(),
                        isSelectEnabled = currentFolder.id != rootFolderId
                    )
                )
            }
        }
        underTest = MegaPickerViewModel(
            isStopBackup = isStopBackup,
            folderName = folderName,
            setSelectedMegaFolderUseCase = setSelectedMegaFolderUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            tryNodeSyncUseCase = tryNodeSyncUseCase,
            deviceFolderUINodeErrorMessageMapper = deviceFolderUINodeErrorMessageMapper,
            createFolderNodeUseCase = createFolderNodeUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            isFolderUsedBySyncOrBackupAcrossDevicesUseCase = isFolderUsedBySyncOrBackupAcrossDevicesUseCase,
            removeDeviceFolderConnectionUseCase = removeDeviceFolderConnectionUseCase,
            monitorMegaPickerFolderNodesUseCase = monitorMegaPickerFolderNodesUseCase,
            nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
        )
    }
}
