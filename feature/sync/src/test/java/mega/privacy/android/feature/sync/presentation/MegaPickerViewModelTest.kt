package mega.privacy.android.feature.sync.presentation

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
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerAction
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerState
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerViewModel
import mega.privacy.android.feature.sync.ui.megapicker.TypedNodeUiModel
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
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
            isSelectEnabled = false,
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
            isSelectEnabled = false,
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
