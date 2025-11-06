package mega.privacy.android.core.nodecomponents.action

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.clickhandler.AvailableOfflineActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.CopyActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.DeletePermanentActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.DisputeTakeDownActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.DownloadActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.EditActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.FavouriteActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.GetLinkActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.HideActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.InfoActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.LabelActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.LeaveShareActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.ManageLinkActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.ManageShareFolderActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.MoveActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.MoveToRubbishBinActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.OpenWithActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RemoveFavouriteActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RemoveLinkActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RemoveOfflineActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RemoveShareActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RenameNodeActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.RestoreActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.SendToChatActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.ShareActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.ShareFolderActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.SyncActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.UnhideActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.VerifyActionClickHandler
import mega.privacy.android.core.nodecomponents.action.clickhandler.VersionsActionClickHandler
import mega.privacy.android.core.nodecomponents.dialog.delete.MoveToRubbishOrDeleteDialogArgs
import mega.privacy.android.core.nodecomponents.dialog.leaveshare.LeaveShareDialogNavKey
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.mapper.RestoreNodeResultMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DisputeTakeDownMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.EditMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.FavouriteMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.InfoMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.LabelMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.LeaveShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveFavouriteMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SyncMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.UnhideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.VerifyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.VersionsMenuAction
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeFavoriteUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.chat.GetNodeToAttachUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeShareDataUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import mega.privacy.android.navigation.destination.SyncNewFolderNavKey
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
class NodeActionClickHandlerTest {
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
    private val mockGetFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val mockGetLocalFilePathUseCase = mock<GetLocalFilePathUseCase>()
    private val mockExportNodeUseCase = mock<ExportNodeUseCase>()
    private val mockGetNodeShareDataUseCase = mock<GetNodeShareDataUseCase>()
    private val mockUpdateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val mockUpdateNodeFavoriteUseCase = mock<UpdateNodeFavoriteUseCase>()

    private val mockFileNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { name } doReturn "filename"
    }

    private val mockFolderNode = mock<TypedFolderNode> {
        on { id } doReturn NodeId(456L)
        on { name } doReturn "foldername"
    }

    private val mockVersionsLauncher = mock<ActivityResultLauncher<Long>>()
    private val mockMoveLauncher = mock<ActivityResultLauncher<LongArray>>()
    private val mockCopyLauncher = mock<ActivityResultLauncher<LongArray>>()
    private val mockShareFolderLauncher = mock<ActivityResultLauncher<LongArray>>()
    private val mockRestoreLauncher = mock<ActivityResultLauncher<ArrayList<NameCollision>>>()
    private val mockSendToChatLauncher = mock<ActivityResultLauncher<LongArray>>()
    private val mockHiddenNodesOnboardingLauncher = mock<ActivityResultLauncher<Boolean>>()
    private val isNodeDeletedFromBackupsUseCase = mock<IsNodeDeletedFromBackupsUseCase>()

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
            mockGetFeatureFlagValueUseCase,
            mockGetLocalFilePathUseCase,
            mockExportNodeUseCase,
            mockUpdateNodeSensitiveUseCase,
            mockUpdateNodeFavoriteUseCase,
            mockVersionsLauncher,
            mockMoveLauncher,
            mockCopyLauncher,
            mockShareFolderLauncher,
            mockRestoreLauncher,
            mockSendToChatLauncher,
            mockHiddenNodesOnboardingLauncher,
            mockGetNodeShareDataUseCase
        )
    }

    // VersionsAction Tests
    @Test
    fun `test VersionsAction canHandle returns true for VersionsMenuAction`() {
        val action = VersionsActionClickHandler()
        val menuAction = mock<VersionsMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test VersionsAction canHandle returns false for other actions`() {
        val action = VersionsActionClickHandler()
        val otherAction = mock<CopyMenuAction>()

        assertThat(action.canHandle(otherAction)).isFalse()
    }

    @Test
    fun `test VersionsAction handle calls versionsLauncher with correct node id`() {
        val action = VersionsActionClickHandler()
        val menuAction = mock<VersionsMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockVersionsLauncher).launch(123L)
    }

    // MoveAction Tests
    @Test
    fun `test MoveAction canHandle returns true for MoveMenuAction`() {
        val action = MoveActionClickHandler()
        val menuAction = mock<MoveMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test MoveAction single node handle calls moveLauncher with correct node id`() {
        val action = MoveActionClickHandler()
        val menuAction = mock<MoveMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockMoveLauncher).launch(longArrayOf(123L))
    }

    @Test
    fun `test MoveAction multiple nodes handle calls moveLauncher with correct node ids`() {
        val action = MoveActionClickHandler()
        val menuAction = mock<MoveMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockMoveLauncher).launch(longArrayOf(123L, 456L))
    }

    // CopyAction Tests
    @Test
    fun `test CopyAction canHandle returns true for CopyMenuAction`() {
        val action = CopyActionClickHandler()
        val menuAction = mock<CopyMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test CopyAction single node handle calls copyLauncher with correct node id`() {
        val action = CopyActionClickHandler()
        val menuAction = mock<CopyMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockCopyLauncher).launch(longArrayOf(123L))
    }

    @Test
    fun `test CopyAction multiple nodes handle calls copyLauncher with correct node ids`() {
        val action = CopyActionClickHandler()
        val menuAction = mock<CopyMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockCopyLauncher).launch(longArrayOf(123L, 456L))
    }

    // ShareFolderAction Tests
    @Test
    fun `test ShareFolderAction canHandle returns true for ShareFolderMenuAction`() {
        val action = ShareFolderActionClickHandler()
        val menuAction = mock<ShareFolderMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test ShareFolderAction single node handle calls verifyShareFolderAction`() {
        val action = ShareFolderActionClickHandler()
        val menuAction = mock<ShareFolderMenuAction>()

        action.handle(menuAction, mockFolderNode, mockSingleNodeActionProvider)

        verify(mockViewModel).verifyShareFolderAction(mockFolderNode)
    }

    @Test
    fun `test ShareFolderAction multiple nodes handle calls verifyShareFolderAction with nodes list`() {
        val action = ShareFolderActionClickHandler()
        val menuAction = mock<ShareFolderMenuAction>()
        val nodes = listOf(mockFolderNode, mockFileNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockViewModel).verifyShareFolderAction(nodes)
    }

    // RestoreAction Tests
    @Test
    fun `test RestoreAction canHandle returns true for RestoreMenuAction`() = runTest {
        val action = RestoreActionClickHandler(
            mockCheckNodesNameCollisionUseCase,
            mockRestoreNodesUseCase,
            mockRestoreNodeResultMapper,
            isNodeDeletedFromBackupsUseCase
        )
        val menuAction = mock<RestoreMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test RestoreAction single node handle calls checkNodesNameCollisionUseCase`() = runTest {
        val action = RestoreActionClickHandler(
            mockCheckNodesNameCollisionUseCase,
            mockRestoreNodesUseCase,
            mockRestoreNodeResultMapper,
            isNodeDeletedFromBackupsUseCase
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
            val action = RestoreActionClickHandler(
                mockCheckNodesNameCollisionUseCase,
                mockRestoreNodesUseCase,
                mockRestoreNodeResultMapper,
                isNodeDeletedFromBackupsUseCase
            )
            val menuAction = mock<RestoreMenuAction>()
            val nodes = listOf(mockFileNode, mockFolderNode)
            whenever(isNodeDeletedFromBackupsUseCase(any())).thenReturn(false)
            action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

            verify(mockCheckNodesNameCollisionUseCase).invoke(
                mapOf(123L to -1L, 456L to -1L),
                NodeNameCollisionType.RESTORE
            )
        }

    // SendToChatAction Tests
    @Test
    fun `test SendToChatAction canHandle returns true for SendToChatMenuAction`() {
        val action = SendToChatActionClickHandler(mockGetNodeToAttachUseCase)
        val menuAction = mock<SendToChatMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test SendToChatAction single node handle calls getNodeToAttachUseCase and sendToChatLauncher for TypedFileNode`() =
        runTest {
            val action = SendToChatActionClickHandler(mockGetNodeToAttachUseCase)
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
            val action = SendToChatActionClickHandler(mockGetNodeToAttachUseCase)
            val menuAction = mock<SendToChatMenuAction>()

            action.handle(menuAction, mockFolderNode, mockSingleNodeActionProvider)

            verify(mockGetNodeToAttachUseCase, never()).invoke(any())
            verify(mockSingleNodeActionProvider.sendToChatLauncher, never()).launch(any())
        }

    @Test
    fun `test SendToChatAction single node handle does not call sendToChatLauncher when getNodeToAttachUseCase returns null`() =
        runTest {
            val action = SendToChatActionClickHandler(mockGetNodeToAttachUseCase)
            val menuAction = mock<SendToChatMenuAction>()

            whenever(mockGetNodeToAttachUseCase(mockFileNode)).thenReturn(null)

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockGetNodeToAttachUseCase).invoke(mockFileNode)
            verify(mockSingleNodeActionProvider.sendToChatLauncher, never()).launch(any())
        }

    @Test
    fun `test SendToChatAction multiple nodes handle calls sendToChatLauncher with all node ids`() {
        val action = SendToChatActionClickHandler(mockGetNodeToAttachUseCase)
        val menuAction = mock<SendToChatMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockMultipleNodesActionProvider.sendToChatLauncher).launch(longArrayOf(123L, 456L))
    }

    // OpenWithAction Tests
    @Test
    fun `test OpenWithAction canHandle returns true for OpenWithMenuAction`() {
        val action = OpenWithActionClickHandler(
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
            val action = OpenWithActionClickHandler(
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
        val action = OpenWithActionClickHandler(
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
        val action = DownloadActionClickHandler()
        val menuAction = mock<DownloadMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test DownloadAction single node handle calls downloadNode`() {
        val action = DownloadActionClickHandler()
        val menuAction = mock<DownloadMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockViewModel).downloadNode(withStartMessage = false)
    }

    @Test
    fun `test DownloadAction multiple nodes handle calls downloadNode`() {
        val action = DownloadActionClickHandler()
        val menuAction = mock<DownloadMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockViewModel).downloadNode(withStartMessage = false)
    }

    // AvailableOfflineAction Tests
    @Test
    fun `test AvailableOfflineAction canHandle returns true for AvailableOfflineMenuAction`() {
        val action = AvailableOfflineActionClickHandler()
        val menuAction = mock<AvailableOfflineMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test AvailableOfflineAction single node handle calls downloadNodeForOffline when node is not available offline`() =
        runTest {
            val action = AvailableOfflineActionClickHandler()
            val menuAction = mock<AvailableOfflineMenuAction>()
            val nodeNotOffline = mock<TypedFileNode> {
                on { id } doReturn NodeId(123L)
                on { isAvailableOffline } doReturn false
            }

            action.handle(menuAction, nodeNotOffline, mockSingleNodeActionProvider)

            verify(mockViewModel).downloadNodeForOffline(withStartMessage = false)
        }

    @Test
    fun `test AvailableOfflineAction multiple nodes handle calls downloadNodeForOffline`() {
        val action = AvailableOfflineActionClickHandler()
        val menuAction = mock<AvailableOfflineMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockViewModel).downloadNodeForOffline(withStartMessage = false)
    }

    @Test
    fun `test RemoveOfflineAction single node handle calls removeOfflineNodeUseCase when node is available offline`() =
        runTest {
            val action = RemoveOfflineActionClickHandler(mockRemoveOfflineNodeUseCase)
            val menuAction = mock<AvailableOfflineMenuAction>()
            val nodeOffline = mock<TypedFileNode> {
                on { id } doReturn NodeId(123L)
                on { isAvailableOffline } doReturn true
            }

            action.handle(menuAction, nodeOffline, mockSingleNodeActionProvider)

            verify(mockRemoveOfflineNodeUseCase).invoke(NodeId(123L))
        }

    @Test
    fun `test RemoveOfflineAction multiple node handle calls removeOfflineNodeUseCase for all the nodes`() =
        runTest {
            val action = RemoveOfflineActionClickHandler(mockRemoveOfflineNodeUseCase)
            val menuAction = mock<AvailableOfflineMenuAction>()
            val node1 = mock<TypedFileNode> {
                on { id } doReturn NodeId(123L)
                on { isAvailableOffline } doReturn false
            }
            val node2 = mock<TypedFileNode> {
                on { id } doReturn NodeId(456L)
                on { isAvailableOffline } doReturn true
            }

            action.handle(menuAction, listOf(node1, node2), mockMultipleNodesActionProvider)

            verify(mockRemoveOfflineNodeUseCase).invoke(node1.id)
            verify(mockRemoveOfflineNodeUseCase).invoke(node2.id)
        }

    // HideAction Tests
    @Test
    fun `test HideAction canHandle returns true for HideMenuAction`() {
        val action = HideActionClickHandler(mockIsHiddenNodesOnboardedUseCase)
        val menuAction = mock<HideMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test HideAction single node handle calls isHiddenNodesOnboardedUseCase and isOnboarding`() =
        runTest {
            val action = HideActionClickHandler(mockIsHiddenNodesOnboardedUseCase)
            val menuAction = mock<HideMenuAction>()

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockIsHiddenNodesOnboardedUseCase).invoke()
            verify(mockViewModel).isOnboarding()
        }

    @Test
    fun `test HideAction multiple nodes handle calls isHiddenNodesOnboardedUseCase and isOnboarding`() =
        runTest {
            val action = HideActionClickHandler(mockIsHiddenNodesOnboardedUseCase)
            val menuAction = mock<HideMenuAction>()
            val nodes = listOf(mockFileNode, mockFolderNode)

            action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

            verify(mockIsHiddenNodesOnboardedUseCase).invoke()
            verify(mockViewModel).isOnboarding()
        }

    // RenameNodeAction Tests
    @Test
    fun `test RenameNodeAction canHandle returns true for RenameMenuAction`() {
        val action = RenameNodeActionClickHandler()
        val menuAction = mock<RenameMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test RenameNodeAction handle calls handleRenameNodeRequest`() {
        val action = RenameNodeActionClickHandler()
        val menuAction = mock<RenameMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockViewModel).handleRenameNodeRequest(NodeId(123L))
    }

    // MoveToRubbishBinAction Tests
    @Test
    fun `test MoveToRubbishBinAction canHandle returns true for TrashMenuAction`() {
        val action = MoveToRubbishBinActionClickHandler(mockNodeHandlesToJsonMapper)
        val menuAction = mock<TrashMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test MoveToRubbishBinAction single node handle calls nodeHandlesToJsonMapper and navigate`() =
        runTest {
            val action = MoveToRubbishBinActionClickHandler(mockNodeHandlesToJsonMapper)
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
            val action = MoveToRubbishBinActionClickHandler(mockNodeHandlesToJsonMapper)
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
        val action = ManageLinkActionClickHandler()
        val menuAction = mock<ManageLinkMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test ManageLinkAction single node handle calls openGetLinkActivity with single handle`() {
        val action = ManageLinkActionClickHandler()
        val menuAction = mock<ManageLinkMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockMegaNavigator).openGetLinkActivity(
            context = mockContext,
            handle = 123L
        )
    }

    @Test
    fun `test ManageLinkAction multiple nodes handle calls openGetLinkActivity with handles array`() {
        val action = ManageLinkActionClickHandler()
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
        val action = DeletePermanentActionClickHandler(mockNodeHandlesToJsonMapper)
        val menuAction = mock<DeletePermanentlyMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test DeletePermanentAction single node handle calls nodeHandlesToJsonMapper and navigate`() =
        runTest {
            val action = DeletePermanentActionClickHandler(mockNodeHandlesToJsonMapper)
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
            val action = DeletePermanentActionClickHandler(mockNodeHandlesToJsonMapper)
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
    // LabelAction Tests
    @Test
    fun `test LabelAction canHandle returns true for LabelMenuAction`() {
        val action = LabelActionClickHandler()
        val menuAction = mock<LabelMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test LabelAction single node handle calls navigationHandler`() {
        val action = LabelActionClickHandler()
        val menuAction = mock<LabelMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockNavigationHandler).navigate(any<NavKey>())
    }

    // ManageShareFolderAction Tests
    @Test
    fun `test ManageShareFolderAction canHandle returns true for ManageShareFolderMenuAction`() {
        val action =
            ManageShareFolderActionClickHandler(mockGetFeatureFlagValueUseCase, mockMegaNavigator)
        val menuAction = mock<ManageShareFolderMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test ManageShareFolderAction single node handle calls megaNavigator`() = runTest {
        val action =
            ManageShareFolderActionClickHandler(mockGetFeatureFlagValueUseCase, mockMegaNavigator)
        val menuAction = mock<ManageShareFolderMenuAction>()

        whenever(mockGetFeatureFlagValueUseCase(any())).thenReturn(true)


        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockNavigationHandler).navigate(any<FileContactInfoNavKey>())
    }

    // InfoAction Tests
    @Test
    fun `test InfoAction canHandle returns true for InfoMenuAction`() {
        val action = InfoActionClickHandler(mockMegaNavigator)
        val menuAction = mock<InfoMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test InfoAction single node handle calls megaNavigator`() {
        val action = InfoActionClickHandler(mockMegaNavigator)
        val menuAction = mock<InfoMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockMegaNavigator).openFileInfoActivity(any(), any())
    }

    // EditAction Tests
    @Test
    fun `test EditAction canHandle returns true for EditMenuAction`() {
        val action = EditActionClickHandler(mockMegaNavigator)
        val menuAction = mock<EditMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test EditAction single node handle calls megaNavigator`() {
        val action = EditActionClickHandler(mockMegaNavigator)
        val menuAction = mock<EditMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockMegaNavigator).openTextEditorActivity(any(), any(), any(), any(), anyOrNull())
    }

    // DisputeTakeDownAction Tests
    @Test
    fun `test DisputeTakeDownAction canHandle returns true for DisputeTakeDownMenuAction`() {
        val action = DisputeTakeDownActionClickHandler(mockMegaNavigator)
        val menuAction = mock<DisputeTakeDownMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test DisputeTakeDownAction single node handle calls megaNavigator`() {
        val action = DisputeTakeDownActionClickHandler(mockMegaNavigator)
        val menuAction = mock<DisputeTakeDownMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockMegaNavigator).launchUrl(any(), any())
    }

    // VerifyAction Tests
    @Test
    fun `test VerifyAction canHandle returns true for VerifyMenuAction`() {
        val action = VerifyActionClickHandler(
            mockGetNodeShareDataUseCase,
            mockMegaNavigator
        )
        val menuAction = mock<VerifyMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test VerifyAction single node handle calls getNodeShareDataUseCase`() = runTest {
        val action = VerifyActionClickHandler(
            mockGetNodeShareDataUseCase,
            mockMegaNavigator
        )
        val menuAction = mock<VerifyMenuAction>()
        val mockShareData = mock<ShareData> {
            on { user } doReturn "test@example.com"
            on { isVerified } doReturn false
            on { isPending } doReturn true
        }

        whenever(mockGetNodeShareDataUseCase(mockFileNode)).thenReturn(mockShareData)

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockGetNodeShareDataUseCase).invoke(mockFileNode)
    }

    @Test
    fun `test VerifyAction single node handle navigates to CannotVerifyContactDialog when not verified and pending`() =
        runTest {
            val action = VerifyActionClickHandler(
                mockGetNodeShareDataUseCase,
                mockMegaNavigator
            )
            val menuAction = mock<VerifyMenuAction>()
            val mockShareData = mock<ShareData> {
                on { user } doReturn "test@example.com"
                on { isVerified } doReturn false
                on { isPending } doReturn true
            }

            whenever(mockGetNodeShareDataUseCase(mockFileNode)).thenReturn(mockShareData)

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockNavigationHandler).navigate(any<NavKey>())
        }

    @Test
    fun `test VerifyAction single node handle opens AuthenticityCredentialsActivity when verified or not pending`() =
        runTest {
            val action = VerifyActionClickHandler(
                mockGetNodeShareDataUseCase,
                mockMegaNavigator
            )
            val menuAction = mock<VerifyMenuAction>()
            val mockShareData = mock<ShareData> {
                on { user } doReturn "test@example.com"
                on { isVerified } doReturn true
                on { isPending } doReturn false
            }

            whenever(mockGetNodeShareDataUseCase(mockFileNode)).thenReturn(mockShareData)

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockMegaNavigator).openAuthenticityCredentialsActivity(any(), any(), any())
        }

    @Test
    fun `test VerifyAction single node handle does nothing when no share data`() = runTest {
        val action = VerifyActionClickHandler(
            mockGetNodeShareDataUseCase,
            mockMegaNavigator
        )
        val menuAction = mock<VerifyMenuAction>()

        whenever(mockGetNodeShareDataUseCase(mockFileNode)).thenReturn(null)

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockGetNodeShareDataUseCase).invoke(mockFileNode)
        verify(mockNavigationHandler, never()).navigate(any<NavKey>())
        verify(mockMegaNavigator, never()).openAuthenticityCredentialsActivity(any(), any(), any())
    }

    // ShareAction Tests
    @Test
    fun `test ShareAction canHandle returns true for ShareMenuAction`() {
        val action = ShareActionClickHandler(
            mockGetLocalFilePathUseCase,
            mockExportNodeUseCase,
            mockGetFileUriUseCase
        )
        val menuAction = mock<ShareMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test ShareAction single node handle calls getLocalFilePathUseCase`() = runTest {
        val action = ShareActionClickHandler(
            mockGetLocalFilePathUseCase,
            mockExportNodeUseCase,
            mockGetFileUriUseCase
        )
        val menuAction = mock<ShareMenuAction>()

        whenever(mockGetLocalFilePathUseCase(any())).thenReturn("/test/path")

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockGetLocalFilePathUseCase).invoke(mockFileNode)
    }

    // RemoveShareAction Tests
    @Test
    fun `test RemoveShareAction canHandle returns true for RemoveShareMenuAction`() {
        val action = RemoveShareActionClickHandler(mockNodeHandlesToJsonMapper)
        val menuAction = mock<RemoveShareMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test RemoveShareAction single node handle calls nodeHandlesToJsonMapper`() {
        val action = RemoveShareActionClickHandler(mockNodeHandlesToJsonMapper)
        val menuAction = mock<RemoveShareMenuAction>()

        whenever(mockNodeHandlesToJsonMapper(anyList())).thenReturn("test")

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)
    }

    // RemoveLinkAction Tests
    @Test
    fun `test RemoveLinkAction canHandle returns true for RemoveLinkMenuAction`() {
        val action = RemoveLinkActionClickHandler(mockNodeHandlesToJsonMapper)
        val menuAction = mock<RemoveLinkMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test RemoveLinkAction single node handle calls nodeHandlesToJsonMapper`() {
        val action = RemoveLinkActionClickHandler(mockNodeHandlesToJsonMapper)
        val menuAction = mock<RemoveLinkMenuAction>()

        whenever(mockNodeHandlesToJsonMapper(anyList())).thenReturn("test")

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)
    }

    // GetLinkAction Tests
    @Test
    fun `test GetLinkAction canHandle returns true for GetLinkMenuAction`() {
        val action = GetLinkActionClickHandler(mockMegaNavigator)
        val menuAction = mock<GetLinkMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test GetLinkAction single node handle calls megaNavigator`() {
        val action = GetLinkActionClickHandler(mockMegaNavigator)
        val menuAction = mock<GetLinkMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockMegaNavigator).openGetLinkActivity(any(), anyLong())
    }

    // UnhideAction Tests
    @Test
    fun `test UnhideAction canHandle returns true for UnhideMenuAction`() {
        val action = UnhideActionClickHandler(mockIsHiddenNodesOnboardedUseCase)
        val menuAction = mock<UnhideMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test UnhideAction single node handle calls isHiddenNodesOnboardedUseCase`() = runTest {
        val action = UnhideActionClickHandler(mockIsHiddenNodesOnboardedUseCase)
        val menuAction = mock<UnhideMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockIsHiddenNodesOnboardedUseCase).invoke()
    }

    // RemoveFavouriteAction Tests
    @Test
    fun `test RemoveFavouriteAction canHandle returns true for RemoveFavouriteMenuAction`() {
        val action = RemoveFavouriteActionClickHandler(mockUpdateNodeFavoriteUseCase)
        val menuAction = mock<RemoveFavouriteMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test RemoveFavouriteAction single node handle calls updateNodeFavoriteUseCase`() =
        runTest {
            val action = RemoveFavouriteActionClickHandler(mockUpdateNodeFavoriteUseCase)
            val menuAction = mock<RemoveFavouriteMenuAction>()

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockUpdateNodeFavoriteUseCase).invoke(any(), any())
        }

    // FavouriteAction Tests
    @Test
    fun `test FavouriteAction canHandle returns true for FavouriteMenuAction`() {
        val action = FavouriteActionClickHandler(mockUpdateNodeFavoriteUseCase)
        val menuAction = mock<FavouriteMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test FavouriteAction single node handle calls updateNodeFavoriteUseCase`() = runTest {
        val action = FavouriteActionClickHandler(mockUpdateNodeFavoriteUseCase)
        val menuAction = mock<FavouriteMenuAction>()

        action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

        verify(mockUpdateNodeFavoriteUseCase).invoke(any(), any())
    }

    // LeaveShareAction Tests
    @Test
    fun `test LeaveShareAction canHandle returns true for LeaveShareMenuAction`() {
        val action = LeaveShareActionClickHandler(mockNodeHandlesToJsonMapper)
        val menuAction = mock<LeaveShareMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test LeaveShareAction canHandle returns false for other actions`() {
        val action = LeaveShareActionClickHandler(mockNodeHandlesToJsonMapper)
        val otherAction = mock<CopyMenuAction>()

        assertThat(action.canHandle(otherAction)).isFalse()
    }

    @Test
    fun `test LeaveShareAction single node handle calls nodeHandlesToJsonMapper and navigate`() =
        runTest {
            val action = LeaveShareActionClickHandler(mockNodeHandlesToJsonMapper)
            val menuAction = mock<LeaveShareMenuAction>()

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L))
            verify(mockNavigationHandler).navigate(
                LeaveShareDialogNavKey(handles = "test-json")
            )
        }

    @Test
    fun `test LeaveShareAction multiple nodes handle calls nodeHandlesToJsonMapper and navigate`() =
        runTest {
            val action = LeaveShareActionClickHandler(mockNodeHandlesToJsonMapper)
            val menuAction = mock<LeaveShareMenuAction>()
            val nodes = listOf(mockFileNode, mockFolderNode)

            action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

            verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L, 456L))
            verify(mockNavigationHandler).navigate(
                LeaveShareDialogNavKey(handles = "test-json")
            )
        }

    @Test
    fun `test LeaveShareAction single node handle does not navigate when nodeHandlesToJsonMapper fails`() =
        runTest {
            val action = LeaveShareActionClickHandler(mockNodeHandlesToJsonMapper)
            val menuAction = mock<LeaveShareMenuAction>()

            whenever(mockNodeHandlesToJsonMapper(any<List<Long>>())).thenThrow(RuntimeException("Mapper failed"))

            action.handle(menuAction, mockFileNode, mockSingleNodeActionProvider)

            verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L))
            verify(mockNavigationHandler, never()).navigate(any<LeaveShareDialogNavKey>())
        }

    @Test
    fun `test LeaveShareAction multiple nodes handle does not navigate when nodeHandlesToJsonMapper fails`() =
        runTest {
            val action = LeaveShareActionClickHandler(mockNodeHandlesToJsonMapper)
            val menuAction = mock<LeaveShareMenuAction>()
            val nodes = listOf(mockFileNode, mockFolderNode)

            whenever(mockNodeHandlesToJsonMapper(any<List<Long>>())).thenThrow(RuntimeException("Mapper failed"))

            action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

            verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L, 456L))
            verify(mockNavigationHandler, never()).navigate(any<LeaveShareDialogNavKey>())
        }

    // UnhideAction Tests (already exists, but adding for completeness)
    @Test
    fun `test UnhideAction multiple nodes handle calls isHiddenNodesOnboardedUseCase`() = runTest {
        val action = UnhideActionClickHandler(mockIsHiddenNodesOnboardedUseCase)
        val menuAction = mock<UnhideMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockIsHiddenNodesOnboardedUseCase).invoke()
    }

    // RenameNodeAction Tests (already exists, but adding multiple nodes test)
    @Test
    fun `test RenameNodeAction multiple nodes handle calls handleRenameNodeRequest for first node`() {
        val action = RenameNodeActionClickHandler()
        val menuAction = mock<RenameMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockViewModel).handleRenameNodeRequest(NodeId(123L))
    }

    // GetLinkAction Tests (already exists, but adding multiple nodes test)
    @Test
    fun `test GetLinkAction multiple nodes handle calls megaNavigator with handles array`() {
        val action = GetLinkActionClickHandler(mockMegaNavigator)
        val menuAction = mock<GetLinkMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockMegaNavigator).openGetLinkActivity(any<Context>(), any<LongArray>())
    }

    // RemoveLinkAction Tests (already exists, but adding multiple nodes test)
    @Test
    fun `test RemoveLinkAction multiple nodes handle calls nodeHandlesToJsonMapper`() {
        val action = RemoveLinkActionClickHandler(mockNodeHandlesToJsonMapper)
        val menuAction = mock<RemoveLinkMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        whenever(mockNodeHandlesToJsonMapper(anyList())).thenReturn("test")

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L, 456L))
    }

    // SendToChatAction Tests (already exists, but adding multiple nodes test for completeness)
    @Test
    fun `test SendToChatAction multiple nodes handle calls sendToChatLauncher with all node ids for multiple nodes`() {
        val action = SendToChatActionClickHandler(mockGetNodeToAttachUseCase)
        val menuAction = mock<SendToChatMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockMultipleNodesActionProvider.sendToChatLauncher).launch(longArrayOf(123L, 456L))
    }

    // ShareFolderAction Tests (already exists, but adding multiple nodes test for completeness)
    @Test
    fun `test ShareFolderAction multiple nodes handle calls verifyShareFolderAction with nodes list for multiple nodes`() {
        val action = ShareFolderActionClickHandler()
        val menuAction = mock<ShareFolderMenuAction>()
        val nodes = listOf(mockFolderNode, mockFileNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockViewModel).verifyShareFolderAction(nodes)
    }

    // RemoveShareAction Tests (already exists, but adding multiple nodes test)
    @Test
    fun `test RemoveShareAction multiple nodes handle calls nodeHandlesToJsonMapper`() {
        val action = RemoveShareActionClickHandler(mockNodeHandlesToJsonMapper)
        val menuAction = mock<RemoveShareMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        whenever(mockNodeHandlesToJsonMapper(anyList())).thenReturn("test")

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockNodeHandlesToJsonMapper).invoke(listOf(123L, 456L))
    }

    // DisputeTakeDownAction Tests (already exists, but adding multiple nodes test)
    @Test
    fun `test DisputeTakeDownAction multiple nodes handle calls megaNavigator`() {
        val action = DisputeTakeDownActionClickHandler(mockMegaNavigator)
        val menuAction = mock<DisputeTakeDownMenuAction>()
        val nodes = listOf(mockFileNode, mockFolderNode)

        action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

        verify(mockMegaNavigator).launchUrl(any<Context>(), any<String>())
    }

    // ShareAction Tests (already exists, but adding multiple nodes test)
    @Test
    fun `test ShareAction multiple nodes handle calls getLocalFilePathUseCase for first node`() =
        runTest {
            val action = ShareActionClickHandler(
                mockGetLocalFilePathUseCase,
                mockExportNodeUseCase,
                mockGetFileUriUseCase
            )
            val menuAction = mock<ShareMenuAction>()
            val nodes = listOf(mockFileNode, mockFolderNode)

            whenever(mockGetLocalFilePathUseCase(any())).thenReturn("/test/path")

            action.handle(menuAction, nodes, mockMultipleNodesActionProvider)

            verify(mockGetLocalFilePathUseCase).invoke(mockFileNode)
        }

    @Test
    fun `test actions return false for wrong menu action types`() {
        val wrongAction = mock<MenuAction>()

        assertThat(VersionsActionClickHandler().canHandle(wrongAction)).isFalse()
        assertThat(MoveActionClickHandler().canHandle(wrongAction)).isFalse()
        assertThat(CopyActionClickHandler().canHandle(wrongAction)).isFalse()
        assertThat(ShareFolderActionClickHandler().canHandle(wrongAction)).isFalse()
        assertThat(
            RestoreActionClickHandler(
                mockCheckNodesNameCollisionUseCase,
                mockRestoreNodesUseCase,
                mockRestoreNodeResultMapper,
                isNodeDeletedFromBackupsUseCase
            ).canHandle(wrongAction)
        ).isFalse()
        assertThat(SendToChatActionClickHandler(mockGetNodeToAttachUseCase).canHandle(wrongAction)).isFalse()
        assertThat(
            OpenWithActionClickHandler(
                mockGetFileUriUseCase,
                mockGetNodePreviewFileUseCase,
                mockHttpServerStartUseCase,
                mockHttpServerIsRunningUseCase,
                mockGetStreamingUriStringForNode
            ).canHandle(wrongAction)
        ).isFalse()
        assertThat(DownloadActionClickHandler().canHandle(wrongAction)).isFalse()
        assertThat(AvailableOfflineActionClickHandler().canHandle(wrongAction)).isFalse()
        assertThat(HideActionClickHandler(mockIsHiddenNodesOnboardedUseCase).canHandle(wrongAction)).isFalse()
        assertThat(RenameNodeActionClickHandler().canHandle(wrongAction)).isFalse()
        assertThat(
            MoveToRubbishBinActionClickHandler(mockNodeHandlesToJsonMapper).canHandle(
                wrongAction
            )
        ).isFalse()
        assertThat(ManageLinkActionClickHandler().canHandle(wrongAction)).isFalse()
        assertThat(
            DeletePermanentActionClickHandler(mockNodeHandlesToJsonMapper).canHandle(
                wrongAction
            )
        ).isFalse()
        assertThat(LabelActionClickHandler().canHandle(wrongAction)).isFalse()
        assertThat(
            ManageShareFolderActionClickHandler(
                mockGetFeatureFlagValueUseCase,
                mockMegaNavigator
            ).canHandle(wrongAction)
        ).isFalse()
        assertThat(InfoActionClickHandler(mockMegaNavigator).canHandle(wrongAction)).isFalse()
        assertThat(EditActionClickHandler(mockMegaNavigator).canHandle(wrongAction)).isFalse()
        assertThat(DisputeTakeDownActionClickHandler(mockMegaNavigator).canHandle(wrongAction)).isFalse()
        assertThat(
            VerifyActionClickHandler(mockGetNodeShareDataUseCase, mockMegaNavigator).canHandle(
                wrongAction
            )
        ).isFalse()
        assertThat(
            ShareActionClickHandler(
                mockGetLocalFilePathUseCase,
                mockExportNodeUseCase,
                mockGetFileUriUseCase
            ).canHandle(wrongAction)
        ).isFalse()
        assertThat(RemoveShareActionClickHandler(mockNodeHandlesToJsonMapper).canHandle(wrongAction)).isFalse()
        assertThat(RemoveLinkActionClickHandler(mockNodeHandlesToJsonMapper).canHandle(wrongAction)).isFalse()
        assertThat(GetLinkActionClickHandler(mockMegaNavigator).canHandle(wrongAction)).isFalse()
        assertThat(UnhideActionClickHandler(mockIsHiddenNodesOnboardedUseCase).canHandle(wrongAction)).isFalse()
        assertThat(
            RemoveFavouriteActionClickHandler(mockUpdateNodeFavoriteUseCase).canHandle(
                wrongAction
            )
        ).isFalse()
        assertThat(FavouriteActionClickHandler(mockUpdateNodeFavoriteUseCase).canHandle(wrongAction)).isFalse()
        assertThat(LeaveShareActionClickHandler(mockNodeHandlesToJsonMapper).canHandle(wrongAction)).isFalse()
        assertThat(SyncActionClickHandler().canHandle(wrongAction)).isFalse()
    }

    @Test
    fun `test SyncAction canHandle returns true for SyncMenuAction`() {
        val action = SyncActionClickHandler()
        val menuAction = mock<SyncMenuAction>()

        assertThat(action.canHandle(menuAction)).isTrue()
    }

    @Test
    fun `test SyncAction canHandle returns false for other actions`() {
        val action = SyncActionClickHandler()
        val otherAction = mock<CopyMenuAction>()

        assertThat(action.canHandle(otherAction)).isFalse()
    }

    @Test
    fun `test SyncAction single node handle calls navigate with correct parameters for folder`() {
        val action = SyncActionClickHandler()
        val menuAction = mock<SyncMenuAction>()

        action.handle(menuAction, mockFolderNode, mockSingleNodeActionProvider)

        verify(mockNavigationHandler).navigate(
            SyncNewFolderNavKey(
                remoteFolderHandle = mockFolderNode.id.longValue,
                remoteFolderName = mockFolderNode.name,
                isFromManagerActivity = true
            )
        )
    }
}
