package mega.privacy.android.core.nodecomponents.action

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.dialog.delete.MoveToRubbishOrDeleteDialogArgs
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.mapper.RestoreNodeResultMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.VersionsMenuAction
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.chat.GetNodeToAttachUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.NavigationHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeActionTypeTest {
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val mockContext = mock<Context>()
    private val mockNavigationHandler = mock<NavigationHandler>()
    private val mockMegaNavigator = mock<MegaNavigator>()
    private val mockViewModel = mock<NodeOptionsActionViewModel>()
    private val mockNodeHandlesToJsonMapper = mock<NodeHandlesToJsonMapper>()
    private val mockCheckNodesNameCollisionUseCase = mock<CheckNodesNameCollisionUseCase>()
    private val mockRestoreNodesUseCase = mock<RestoreNodesUseCase>()
    private val mockRestoreNodeResultMapper = mock<RestoreNodeResultMapper>()
    private val mockIsHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase>()
    private val mockRemoveOfflineNodeUseCase = mock<RemoveOfflineNodeUseCase>()
    private val mockGetNodeToAttachUseCase = mock<GetNodeToAttachUseCase>()
    private val mockGetFileUriUseCase = mock<GetFileUriUseCase>()
    private val mockGetNodePreviewFileUseCase = mock<GetNodePreviewFileUseCase>()
    private val mockHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val mockHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val mockGetStreamingUriStringForNode = mock<GetStreamingUriStringForNode>()

    private val mockFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
    }

    private val mockFolderNode = mock<TypedFolderNode> {
        on { id } doReturn NodeId(456L)
    }

    private val mockVersionsLauncher = mock<ActivityResultLauncher<Long>>()
    private val mockMoveLauncher = mock<ActivityResultLauncher<LongArray>>()
    private val mockCopyLauncher = mock<ActivityResultLauncher<LongArray>>()
    private val mockShareFolderLauncher = mock<ActivityResultLauncher<LongArray>>()
    private val mockRestoreLauncher = mock<ActivityResultLauncher<LongArray>>()
    private val mockSendToChatLauncher = mock<ActivityResultLauncher<LongArray>>()
    private val mockHiddenNodesOnboardingLauncher = mock<ActivityResultLauncher<Boolean>>()

    private val mockSingleNodeActionProvider = SingleNodeActionProvider(
        viewModel = mockViewModel,
        context = mockContext,
        coroutineScope = testScope,
        postMessage = { },
        megaNavigator = mockMegaNavigator,
        navigationHandler = mockNavigationHandler,
        moveLauncher = mockMoveLauncher,
        copyLauncher = mockCopyLauncher,
        shareFolderLauncher = mockShareFolderLauncher,
        restoreLauncher = mockRestoreLauncher,
        sendToChatLauncher = mockSendToChatLauncher,
        hiddenNodesOnboardingLauncher = mockHiddenNodesOnboardingLauncher,
        versionsLauncher = mockVersionsLauncher
    )

    private val mockMultipleNodesActionProvider = MultipleNodesActionProvider(
        viewModel = mockViewModel,
        context = mockContext,
        coroutineScope = testScope,
        postMessage = { },
        megaNavigator = mockMegaNavigator,
        navigationHandler = mockNavigationHandler,
        moveLauncher = mockMoveLauncher,
        copyLauncher = mockCopyLauncher,
        shareFolderLauncher = mockShareFolderLauncher,
        restoreLauncher = mockRestoreLauncher,
        sendToChatLauncher = mockSendToChatLauncher,
        hiddenNodesOnboardingLauncher = mockHiddenNodesOnboardingLauncher
    )

    @BeforeEach
    fun setUp() {
        // Reset mocks
        whenever(mockNodeHandlesToJsonMapper(any<List<Long>>())).thenReturn("test-json")
        whenever(mockNodeHandlesToJsonMapper(any<String>())).thenReturn(listOf())
        mockCheckNodesNameCollisionUseCase.stub {
            onBlocking { invoke(any(), any()) } doReturn NodeNameCollisionsResult(
                noConflictNodes = emptyMap(),
                conflictNodes = emptyMap(),
                type = NodeNameCollisionType.RESTORE
            )
        }
        mockIsHiddenNodesOnboardedUseCase.stub {
            onBlocking { invoke() } doReturn false
        }
        mockViewModel.stub {
            onBlocking { isOnboarding() } doReturn false
        }
    }

    @AfterEach
    fun resetMocks() {
        reset(
            mockContext,
            mockNavigationHandler,
            mockMegaNavigator,
            mockViewModel,
            mockNodeHandlesToJsonMapper,
            mockCheckNodesNameCollisionUseCase,
            mockRestoreNodesUseCase,
            mockRestoreNodeResultMapper,
            mockIsHiddenNodesOnboardedUseCase,
            mockRemoveOfflineNodeUseCase,
            mockGetNodeToAttachUseCase,
            mockGetFileUriUseCase,
            mockGetNodePreviewFileUseCase,
            mockHttpServerStartUseCase,
            mockHttpServerIsRunningUseCase,
            mockGetStreamingUriStringForNode,
            mockVersionsLauncher,
            mockMoveLauncher,
            mockCopyLauncher,
            mockShareFolderLauncher,
            mockRestoreLauncher,
            mockSendToChatLauncher,
            mockHiddenNodesOnboardingLauncher
        )
    }

    // VersionsAction Tests
    @Test
    fun `test VersionsAction canHandle returns true for VersionsMenuAction`() {
        val action = VersionsAction()
        val menuAction = mock<VersionsMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test VersionsAction canHandle returns false for other actions`() {
        val action = VersionsAction()
        val otherAction = mock<CopyMenuAction>()

        assertThat(action.canHandle(otherAction)).isFalse()
    }

    @Test
    fun `test VersionsAction handle calls versionsLauncher with correct node id`() {
        val action = VersionsAction()
        val menuAction = mock<VersionsMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockVersionsLauncher).launch(123L)
    }

    // MoveAction Tests
    @Test
    fun `test MoveAction canHandle returns true for MoveMenuAction`() {
        val action = MoveAction()
        val menuAction = mock<MoveMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test MoveAction single node handle calls moveLauncher with correct node id`() {
        val action = MoveAction()
        val menuAction = mock<MoveMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockMoveLauncher).launch(longArrayOf(123L))
    }

    @Test
    fun `test MoveAction multiple nodes handle calls moveLauncher with correct node ids`() {
        val action = MoveAction()
        val menuAction = mock<MoveMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockMoveLauncher).launch(longArrayOf(123L, 456L))
    }

    // CopyAction Tests
    @Test
    fun `test CopyAction canHandle returns true for CopyMenuAction`() {
        val action = CopyAction()
        val menuAction = mock<CopyMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test CopyAction single node handle calls copyLauncher with correct node id`() {
        val action = CopyAction()
        val menuAction = mock<CopyMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockCopyLauncher).launch(longArrayOf(123L))
    }

    @Test
    fun `test CopyAction multiple nodes handle calls copyLauncher with correct node ids`() {
        val action = CopyAction()
        val menuAction = mock<CopyMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockCopyLauncher).launch(longArrayOf(123L, 456L))
    }

    // ShareFolderAction Tests
    @Test
    fun `test ShareFolderAction canHandle returns true for ShareFolderMenuAction`() {
        val action = ShareFolderAction()
        val menuAction = mock<ShareFolderMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test ShareFolderAction single node handle calls verifyShareFolderAction`() {
        val action = ShareFolderAction()
        val menuAction = mock<ShareFolderMenuAction>()

        action.handle(menuAction, mockFolderNode, mockSingleNodeActionProvider)

        verify(mockViewModel).verifyShareFolderAction(mockFolderNode)
    }

    @Test
    fun `test ShareFolderAction multiple nodes handle calls verifyShareFolderAction with nodes list`() {
        val action = ShareFolderAction()
        val menuAction = mock<ShareFolderMenuAction>()
        val nodes = listOf(mockFolderNode, mockFileNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockViewModel).verifyShareFolderAction(nodes)
    }

    // RestoreAction Tests
    @Test
    fun `test RestoreAction canHandle returns true for RestoreMenuAction`() = runTest {
        val action = RestoreAction(
            mockCheckNodesNameCollisionUseCase,
            mockRestoreNodesUseCase,
            mockRestoreNodeResultMapper
        )
        val menuAction = mock<RestoreMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test RestoreAction single node handle calls checkNodesNameCollisionUseCase`() = runTest {
        val action = RestoreAction(
            mockCheckNodesNameCollisionUseCase,
            mockRestoreNodesUseCase,
            mockRestoreNodeResultMapper
        )
        val menuAction = mock<RestoreMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockCheckNodesNameCollisionUseCase).invoke(
            mapOf(123L to -1L),
            NodeNameCollisionType.RESTORE
        )
    }

    @Test
    fun `test RestoreAction multiple nodes handle calls checkNodesNameCollisionUseCase with all nodes`() =
        runTest {
            val action = RestoreAction(
                mockCheckNodesNameCollisionUseCase,
                mockRestoreNodesUseCase,
                mockRestoreNodeResultMapper
            )
            val menuAction = mock<RestoreMenuAction>()
            val nodes = listOf(mockFileNode, mockFolderNode)

            action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

            verify(mockCheckNodesNameCollisionUseCase).invoke(
                mapOf(123L to -1L, 456L to -1L),
                NodeNameCollisionType.RESTORE
            )
        }

    // SendToChatAction Tests
    @Test
    fun `test SendToChatAction canHandle returns true for SendToChatMenuAction`() {
        val action = SendToChatAction(mockGetNodeToAttachUseCase)
        val menuAction = mock<SendToChatMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test SendToChatAction single node handle calls getNodeToAttachUseCase and sendToChatLauncher for TypedFileNode`() =
        runTest {
            val action = SendToChatAction(mockGetNodeToAttachUseCase)
            val menuAction = mock<SendToChatMenuAction>()
            val mockTypedNode = mock<TypedFileNode> {
                on { id } doReturn NodeId(123L)
            }

            whenever(mockGetNodeToAttachUseCase(mockTypedNode)).thenReturn(mockTypedNode)

            action.handle(menuAction, mockTypedNode, mockSingleNodeActionProvider)

            verify(mockGetNodeToAttachUseCase).invoke(mockTypedNode)
            verify(mockSingleNodeActionProvider.sendToChatLauncher).launch(longArrayOf(123L))
        }

    @Test
    fun `test SendToChatAction single node handle does not call sendToChatLauncher for non-TypedFileNode`() =
        runTest {
            val action = SendToChatAction(mockGetNodeToAttachUseCase)
            val menuAction = mock<SendToChatMenuAction>()

            action.handle(menuAction, mockFolderNode, mockSingleNodeActionProvider)

            verify(mockGetNodeToAttachUseCase, never()).invoke(any())
            verify(mockSingleNodeActionProvider.sendToChatLauncher, never()).launch(any())
        }

    @Test
    fun `test SendToChatAction single node handle does not call sendToChatLauncher when getNodeToAttachUseCase returns null`() =
        runTest {
            val action = SendToChatAction(mockGetNodeToAttachUseCase)
            val menuAction = mock<SendToChatMenuAction>()

            whenever(mockGetNodeToAttachUseCase(mockFileNode)).thenReturn(null)

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockGetNodeToAttachUseCase).invoke(mockFileNode)
            verify(mockSingleNodeActionProvider.sendToChatLauncher, never()).launch(any())
        }

    @Test
    fun `test SendToChatAction multiple nodes handle calls sendToChatLauncher with all node ids`() {
        val action = SendToChatAction(mockGetNodeToAttachUseCase)
        val menuAction = mock<SendToChatMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockMultipleNodesActionProvider.sendToChatLauncher).launch(longArrayOf(123L, 456L))
    }

    // OpenWithAction Tests
    @Test
    fun `test OpenWithAction canHandle returns true for OpenWithMenuAction`() {
        val action = OpenWithAction(
            mockGetFileUriUseCase,
            mockGetNodePreviewFileUseCase,
            mockHttpServerStartUseCase,
            mockHttpServerIsRunningUseCase,
            mockGetStreamingUriStringForNode
        )
        val menuAction = mock<OpenWithMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test OpenWithAction single node handle calls downloadNodeForPreview for non-TypedFileNode`() =
        runTest {
            val action = OpenWithAction(
                mockGetFileUriUseCase,
                mockGetNodePreviewFileUseCase,
                mockHttpServerStartUseCase,
                mockHttpServerIsRunningUseCase,
                mockGetStreamingUriStringForNode
            )
            val menuAction = mock<OpenWithMenuAction>()

            action.handle(menuAction, mockFolderNode, mockSingleNodeActionProvider)

            // For non-TypedFileNode, it should log an error but not call downloadNodeForPreview
            verify(mockViewModel, never()).downloadNodeForPreview(anyBoolean())
        }

    @Test
    fun `test OpenWithAction multiple nodes handle calls downloadNodeForPreview`() {
        val action = OpenWithAction(
            mockGetFileUriUseCase,
            mockGetNodePreviewFileUseCase,
            mockHttpServerStartUseCase,
            mockHttpServerIsRunningUseCase,
            mockGetStreamingUriStringForNode
        )
        val menuAction = mock<OpenWithMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockViewModel).downloadNodeForPreview(true)
    }

    // DownloadAction Tests
    @Test
    fun `test DownloadAction canHandle returns true for DownloadMenuAction`() {
        val action = DownloadAction()
        val menuAction = mock<DownloadMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test DownloadAction single node handle calls downloadNode`() {
        val action = DownloadAction()
        val menuAction = mock<DownloadMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockViewModel).downloadNode(withStartMessage = false)
    }

    @Test
    fun `test DownloadAction multiple nodes handle calls downloadNode`() {
        val action = DownloadAction()
        val menuAction = mock<DownloadMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockViewModel).downloadNode(withStartMessage = false)
    }

    // AvailableOfflineAction Tests
    @Test
    fun `test AvailableOfflineAction canHandle returns true for AvailableOfflineMenuAction`() {
        val action = AvailableOfflineAction(mockRemoveOfflineNodeUseCase)
        val menuAction = mock<AvailableOfflineMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test AvailableOfflineAction single node handle calls downloadNodeForOffline when node is not available offline`() =
        runTest {
            val action = AvailableOfflineAction(mockRemoveOfflineNodeUseCase)
            val menuAction = mock<AvailableOfflineMenuAction>()
            val nodeNotOffline = mock<TypedFileNode> {
                on { id } doReturn NodeId(123L)
                on { isAvailableOffline } doReturn false
            }

            action.handle(menuAction, nodeNotOffline, mockSingleNodeActionProvider)

            verify(mockViewModel).downloadNodeForOffline(withStartMessage = false)
        }

    @Test
    fun `test AvailableOfflineAction single node handle calls removeOfflineNodeUseCase when node is available offline`() =
        runTest {
            val action = AvailableOfflineAction(mockRemoveOfflineNodeUseCase)
            val menuAction = mock<AvailableOfflineMenuAction>()
            val nodeOffline = mock<TypedFileNode> {
                on { id } doReturn NodeId(123L)
                on { isAvailableOffline } doReturn true
            }

            action.handle(menuAction, nodeOffline, mockSingleNodeActionProvider)

            verify(mockRemoveOfflineNodeUseCase).invoke(NodeId(123L))
        }

    @Test
    fun `test AvailableOfflineAction multiple nodes handle calls downloadNodeForOffline`() {
        val action = AvailableOfflineAction(mockRemoveOfflineNodeUseCase)
        val menuAction = mock<AvailableOfflineMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockViewModel).downloadNodeForOffline(withStartMessage = false)
    }

    // HideAction Tests
    @Test
    fun `test HideAction canHandle returns true for HideMenuAction`() {
        val action = HideAction(mockIsHiddenNodesOnboardedUseCase)
        val menuAction = mock<HideMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test HideAction single node handle calls isHiddenNodesOnboardedUseCase and isOnboarding`() =
        runTest {
            val action = HideAction(mockIsHiddenNodesOnboardedUseCase)
            val menuAction = mock<HideMenuAction>()

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockIsHiddenNodesOnboardedUseCase).invoke()
            verify(mockViewModel).isOnboarding()
        }

    @Test
    fun `test HideAction multiple nodes handle calls isHiddenNodesOnboardedUseCase and isOnboarding`() =
        runTest {
            val action = HideAction(mockIsHiddenNodesOnboardedUseCase)
            val menuAction = mock<HideMenuAction>()
            val nodes = listOf(mockFileNode, mockFolderNode)

            action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

            verify(mockIsHiddenNodesOnboardedUseCase).invoke()
            verify(mockViewModel).isOnboarding()
        }

    // RenameNodeAction Tests
    @Test
    fun `test RenameNodeAction canHandle returns true for RenameMenuAction`() {
        val action = RenameNodeAction()
        val menuAction = mock<RenameMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test RenameNodeAction handle calls handleRenameNodeRequest`() {
        val action = RenameNodeAction()
        val menuAction = mock<RenameMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockViewModel).handleRenameNodeRequest(NodeId(123L))
    }

    // MoveToRubbishBinAction Tests
    @Test
    fun `test MoveToRubbishBinAction canHandle returns true for TrashMenuAction`() {
        val action = MoveToRubbishBinAction(mockNodeHandlesToJsonMapper)
        val menuAction = mock<TrashMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test MoveToRubbishBinAction single node handle calls nodeHandlesToJsonMapper and navigate`() =
        runTest {
            val action = MoveToRubbishBinAction(mockNodeHandlesToJsonMapper)
            val menuAction = mock<TrashMenuAction>()

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L))
            verify(mockNavigationHandler).navigate(
                MoveToRubbishOrDeleteDialogArgs(
                    isInRubbish = false,
                    nodeHandles = listOf(123L)
                )
            )
        }

    @Test
    fun `test MoveToRubbishBinAction multiple nodes handle calls nodeHandlesToJsonMapper and navigate`() =
        runTest {
            val action = MoveToRubbishBinAction(mockNodeHandlesToJsonMapper)
            val menuAction = mock<TrashMenuAction>()
            val nodes = listOf(mockFileNode, mockFolderNode)

            action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

            verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L, 456L))
            verify(mockNavigationHandler).navigate(
                MoveToRubbishOrDeleteDialogArgs(
                    isInRubbish = false,
                    nodeHandles = listOf(123L, 456L)
                )
            )
        }

    // ManageLinkAction Tests
    @Test
    fun `test ManageLinkAction canHandle returns true for ManageLinkMenuAction`() {
        val action = ManageLinkAction()
        val menuAction = mock<ManageLinkMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test ManageLinkAction single node handle calls openGetLinkActivity with single handle`() {
        val action = ManageLinkAction()
        val menuAction = mock<ManageLinkMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockMegaNavigator).openGetLinkActivity(
            context = mockContext,
            handle = 123L
        )
    }

    @Test
    fun `test ManageLinkAction multiple nodes handle calls openGetLinkActivity with handles array`() {
        val action = ManageLinkAction()
        val menuAction = mock<ManageLinkMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockMegaNavigator).openGetLinkActivity(
            context = mockContext,
            handles = longArrayOf(123L, 456L)
        )
    }

    // DeletePermanentAction Tests
    @Test
    fun `test DeletePermanentAction canHandle returns true for DeletePermanentlyMenuAction`() {
        val action = DeletePermanentAction(mockNodeHandlesToJsonMapper)
        val menuAction = mock<DeletePermanentlyMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test DeletePermanentAction single node handle calls nodeHandlesToJsonMapper and navigate`() =
        runTest {
            val action = DeletePermanentAction(mockNodeHandlesToJsonMapper)
            val menuAction = mock<DeletePermanentlyMenuAction>()

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L))
            verify(mockNavigationHandler).navigate(
                MoveToRubbishOrDeleteDialogArgs(
                    isInRubbish = true,
                    nodeHandles = listOf(123L)
                )
            )
        }

    @Test
    fun `test DeletePermanentAction multiple nodes handle calls nodeHandlesToJsonMapper and navigate`() =
        runTest {
            val action = DeletePermanentAction(mockNodeHandlesToJsonMapper)
            val menuAction = mock<DeletePermanentlyMenuAction>()
            val nodes = listOf(mockFileNode, mockFolderNode)

            action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

            verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L, 456L))
            verify(mockNavigationHandler).navigate(
                MoveToRubbishOrDeleteDialogArgs(
                    isInRubbish = true,
                    nodeHandles = listOf(123L, 456L)
                )
            )
        }

    // Test that actions don't handle wrong menu action types
    @Test
    fun `test actions return false for wrong menu action types`() {
        val wrongAction = mock<MenuAction>()

        assertThat(VersionsAction().canHandle(wrongAction)).isFalse()
        assertThat(MoveAction().canHandle(wrongAction)).isFalse()
        assertThat(CopyAction().canHandle(wrongAction)).isFalse()
        assertThat(ShareFolderAction().canHandle(wrongAction)).isFalse()
        assertThat(
            RestoreAction(
                mockCheckNodesNameCollisionUseCase,
                mockRestoreNodesUseCase,
                mockRestoreNodeResultMapper
            ).canHandle(wrongAction)
        ).isFalse()
        assertThat(SendToChatAction(mockGetNodeToAttachUseCase).canHandle(wrongAction)).isFalse()
        assertThat(
            OpenWithAction(
                mockGetFileUriUseCase,
                mockGetNodePreviewFileUseCase,
                mockHttpServerStartUseCase,
                mockHttpServerIsRunningUseCase,
                mockGetStreamingUriStringForNode
            ).canHandle(wrongAction)
        ).isFalse()
        assertThat(DownloadAction().canHandle(wrongAction)).isFalse()
        assertThat(AvailableOfflineAction(mockRemoveOfflineNodeUseCase).canHandle(wrongAction)).isFalse()
        assertThat(HideAction(mockIsHiddenNodesOnboardedUseCase).canHandle(wrongAction)).isFalse()
        assertThat(RenameNodeAction().canHandle(wrongAction)).isFalse()
        assertThat(MoveToRubbishBinAction(mockNodeHandlesToJsonMapper).canHandle(wrongAction)).isFalse()
        assertThat(ManageLinkAction().canHandle(wrongAction)).isFalse()
        assertThat(DeletePermanentAction(mockNodeHandlesToJsonMapper).canHandle(wrongAction)).isFalse()
    }
}
