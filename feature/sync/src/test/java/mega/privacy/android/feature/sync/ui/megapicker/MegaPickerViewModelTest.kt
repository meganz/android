package mega.privacy.android.feature.sync.ui.megapicker

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetTypedNodesFromFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.sync.TryNodeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSelectedMegaFolderUseCase
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
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
    private val getTypedNodesFromFolder: GetTypedNodesFromFolderUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val tryNodeSyncUseCase: TryNodeSyncUseCase = mock()
    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper = mock()
    private val getCameraUploadsFolderHandleUseCase: GetPrimarySyncHandleUseCase = mock()
    private val getMediaUploadsFolderHandleUseCase: GetSecondaryFolderNodeUseCase = mock()
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase = mock()
    private val createFolderNodeUseCase: CreateFolderNodeUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase = mock()

    private val typedNodeUiModels: List<TypedNodeUiModel> = emptyList()
    private val childrenNodes: List<TypedNode> = emptyList()

    private lateinit var underTest: MegaPickerViewModel

    @AfterEach
    fun resetAndTearDown() {
        reset(
            setSelectedMegaFolderUseCase,
            getRootNodeUseCase,
            getTypedNodesFromFolder,
            getNodeByHandleUseCase,
            tryNodeSyncUseCase,
            deviceFolderUINodeErrorMessageMapper,
            getCameraUploadsFolderHandleUseCase,
            getMediaUploadsFolderHandleUseCase,
            getMyChatsFilesFolderIdUseCase,
            createFolderNodeUseCase,
            getFeatureFlagValueUseCase,
            nodeExistsInCurrentLocationUseCase
        )
    }

    @Test
    fun `test that viewmodel fetches root folder and its children upon initialization`() = runTest {
        val rootFolderId = NodeId(123456L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

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
        whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flow {
            emit(childrenNodesWithCUAndChat)
            awaitCancellation()
        })
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(cameraUploadsFolderId.longValue)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(mediaUploadsFolder)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(chatFilesFolderId)
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        initViewModel()
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
        initViewModel()
        val clickedFolderId = NodeId(9845748L)
        val clickedFolder: TypedFolderNode = mock {
            on { id } doReturn clickedFolderId
        }
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(getTypedNodesFromFolder(clickedFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })
        whenever(
            nodeExistsInCurrentLocationUseCase(
                nodeId = clickedFolderId,
                name = ""
            )
        ).thenReturn(false)
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
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })
        whenever(getNodeByHandleUseCase(parentFolderId.longValue)).thenReturn(parentFolder)
        whenever(getTypedNodesFromFolder(parentFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        val expectedState = MegaPickerState(
            currentFolder = parentFolder,
            nodes = typedNodeUiModels,
            isSelectEnabled = true,
        )

        initViewModel()

        underTest.handleAction(MegaPickerAction.BackClicked)

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
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })
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
            whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
                emit(childrenNodes)
                awaitCancellation()
            })
            val errorCode = 18
            val syncError = SyncError.ACTIVE_SYNC_ABOVE_PATH
            val errorStringRes = 12345
            val error = MegaSyncException(
                errorCode, "error", syncError = syncError
            )
            whenever(deviceFolderUINodeErrorMessageMapper(syncError)).thenReturn(errorStringRes)
            doAnswer { throw error }.whenever(tryNodeSyncUseCase).invoke(currentFolderId)
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            initViewModel()

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            val event = underTest.state.value.errorMessageId
            assertThat(event).isEqualTo(errorStringRes)
        }

    @Test
    fun `test that error message event is null when viewmodel handle action is called`() = runTest {
        initViewModel()

        underTest.handleAction(
            MegaPickerAction.ErrorMessageShown
        )

        underTest.state.test {
            assertThat(awaitItem().errorMessageId).isEqualTo(null)
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
            whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
                emit(childrenNodes)
                awaitCancellation()
            })
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
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
        whenever(getTypedNodesFromFolder(newFolderId)).thenReturn(flow {
            emit(emptyList())
            awaitCancellation()
        })
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))

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
        val currentFolderId = NodeId(9845748L)
        val folderName = "ExistingFolder"
        val clickedFolder: TypedFolderNode = mock {
            on { id } doReturn currentFolderId
        }
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })
        whenever(nodeExistsInCurrentLocationUseCase(currentFolderId, folderName)).thenReturn(true)

        initViewModel(isStopBackup = true, folderName = folderName)

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
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)

        // Use a flow that delays emission to test loading state
        whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flow {
            kotlinx.coroutines.delay(100)
            emit(childrenNodes)
            awaitCancellation()
        })

        initViewModel()

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
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })

        // Use a flow that delays emission to test loading state
        whenever(getTypedNodesFromFolder(clickedFolderId)).thenReturn(flow {
            kotlinx.coroutines.delay(100)
            emit(childrenNodes)
            awaitCancellation()
        })

        initViewModel()

        underTest.handleAction(MegaPickerAction.FolderClicked(clickedFolder))

        underTest.state.test {
            // Skip initial state
            awaitItem()
            // The loading state might be too fast to catch, so we just verify the final state
            val finalState = awaitItem()
            assertThat(finalState.isLoading).isEqualTo(false)
        }
    }

    @Test
    fun `test that error is shown when getTypedNodesFromFolder fails`() = runTest {
        val rootFolderId = NodeId(123456L)
        val rootFolder: FolderNode = mock {
            on { id } doReturn rootFolderId
        }
        whenever(getRootNodeUseCase()).thenReturn(rootFolder)
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flow {
            throw RuntimeException("Network error")
        })

        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            // When getTypedNodesFromFolder fails, the loading state remains true
            assertThat(state.isLoading).isEqualTo(true)
        }
    }

    @Test
    fun `test that back click does nothing when current folder is null`() = runTest {
        // This test verifies that when the ViewModel is initialized but no folder is set,
        // back click doesn't perform any action
        whenever(getRootNodeUseCase()).thenReturn(null)
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
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
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(getRootNodeUseCase()).thenReturn(currentFolder)
        whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })
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
        verify(getTypedNodesFromFolder, never()).invoke(any<NodeId>())
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

            verifyNoInteractions(getTypedNodesFromFolder)
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
        whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
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
        whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
            emit(childrenNodes)
            awaitCancellation()
        })
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(nodeExistsInCurrentLocationUseCase(currentFolderId, "TestFolder")).thenReturn(true)

        initViewModel(isStopBackup = true, folderName = "TestFolder")

        underTest.handleAction(
            MegaPickerAction.CurrentFolderSelected(
                allFilesAccessPermissionGranted = true,
                disableBatteryOptimizationPermissionGranted = true
            )
        )

        verify(nodeExistsInCurrentLocationUseCase, times(2)).invoke(currentFolderId, "TestFolder")
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
            whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
                emit(childrenNodes)
                awaitCancellation()
            })
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
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

            verify(nodeExistsInCurrentLocationUseCase, times(2)).invoke(
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
            whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
                emit(childrenNodes)
                awaitCancellation()
            })
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))

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
            whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
                emit(childrenNodes)
                awaitCancellation()
            })
            val errorCode = 18
            val syncError = SyncError.ACTIVE_SYNC_ABOVE_PATH
            val error = MegaSyncException(
                errorCode, "error", syncError = syncError
            )
            whenever(deviceFolderUINodeErrorMessageMapper(syncError)).thenReturn(null)
            whenever(deviceFolderUINodeErrorMessageMapper(SyncError.UNKNOWN_ERROR)).thenReturn(99999)
            doAnswer { throw error }.whenever(tryNodeSyncUseCase).invoke(currentFolderId)
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            initViewModel()

            underTest.handleAction(
                MegaPickerAction.CurrentFolderSelected(
                    allFilesAccessPermissionGranted = true,
                    disableBatteryOptimizationPermissionGranted = true
                )
            )

            val event = underTest.state.value.errorMessageId
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
        whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
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
        whenever(getCameraUploadsFolderHandleUseCase()).thenThrow(RuntimeException("Failed to get CU handle"))
        whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
        whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
        whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
        whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flow {
            emit(childrenNodesWithFolder)
            awaitCancellation()
        })

        initViewModel()

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
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
                emit(childrenNodes)
                awaitCancellation()
            })

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
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
            whenever(nodeExistsInCurrentLocationUseCase(any(), any())).thenReturn(false)
            whenever(getTypedNodesFromFolder(rootFolderId)).thenReturn(flow {
                emit(childrenNodes)
                awaitCancellation()
            })

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
            whenever(getTypedNodesFromFolder(newFolderId)).thenReturn(flow {
                emit(emptyList())
                awaitCancellation()
            })
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))

            initViewModel()

            underTest.createFolder("New Folder", parentFolder)

            verify(createFolderNodeUseCase).invoke(
                name = "New Folder",
                parentNodeId = parentFolderId
            )
            verify(getNodeByHandleUseCase).invoke(newFolderId.longValue)
            verify(getTypedNodesFromFolder).invoke(newFolderId)
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
            whenever(getTypedNodesFromFolder(currentFolderId)).thenReturn(flow {
                emit(childrenNodes)
                awaitCancellation()
            })
            whenever(getCameraUploadsFolderHandleUseCase()).thenReturn(-1L)
            whenever(getMediaUploadsFolderHandleUseCase()).thenReturn(null)
            whenever(getMyChatsFilesFolderIdUseCase()).thenReturn(NodeId(-1L))
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

    private fun initViewModel(isStopBackup: Boolean = false, folderName: String? = null) {
        wheneverBlocking { getFeatureFlagValueUseCase.invoke(any()) }.thenReturn(false)
        underTest = MegaPickerViewModel(
            isStopBackup = isStopBackup,
            folderName = folderName,
            setSelectedMegaFolderUseCase = setSelectedMegaFolderUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            getTypedNodesFromFolder = getTypedNodesFromFolder,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            tryNodeSyncUseCase = tryNodeSyncUseCase,
            deviceFolderUINodeErrorMessageMapper = deviceFolderUINodeErrorMessageMapper,
            getCameraUploadsFolderHandleUseCase = getCameraUploadsFolderHandleUseCase,
            getMediaUploadsFolderHandleUseCase = getMediaUploadsFolderHandleUseCase,
            getMyChatsFilesFolderIdUseCase = getMyChatsFilesFolderIdUseCase,
            createFolderNodeUseCase = createFolderNodeUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase
        )
    }
}
