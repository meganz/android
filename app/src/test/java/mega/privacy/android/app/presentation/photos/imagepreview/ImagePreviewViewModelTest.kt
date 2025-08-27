@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.photos.imagepreview

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.core.nodecomponents.mapper.RemovePublicLinkResultMapper
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewVideoLauncher
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_NODE_FETCHER_SOURCE
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.PARAMS_CURRENT_IMAGE_NODE_ID_VALUE
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.OfflineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.triggeredContent
import mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.AddFavouritesUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.CheckFileUriUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.imagepreview.ClearImageResultUseCase
import mega.privacy.android.domain.usecase.imagepreview.GetImageFromFileUseCase
import mega.privacy.android.domain.usecase.imagepreview.GetImageUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImagePreviewViewModelTest {

    private lateinit var underTest: ImagePreviewViewModel

    private val savedStateHandle = mock<SavedStateHandle>()
    private val imageNodeFetchers = mutableMapOf<ImagePreviewFetcherSource, ImageNodeFetcher>()
    private val imagePreviewMenuMap = mapOf<ImagePreviewMenuSource, ImagePreviewMenu>()
    private val addImageTypeUseCase: AddImageTypeUseCase = mock()
    private val getImageUseCase: GetImageUseCase = mock()
    private val getImageFromFileUseCase: GetImageFromFileUseCase = mock()
    private val checkChatNodesNameCollisionAndCopyUseCase: CheckChatNodesNameCollisionAndCopyUseCase =
        mock()
    private val checkNodesNameCollisionWithActionUseCase: CheckNodesNameCollisionWithActionUseCase =
        mock()
    private val addFavouritesUseCase: AddFavouritesUseCase = mock()
    private val removeFavouritesUseCase: RemoveFavouritesUseCase = mock()
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase = mock()
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase = mock()
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase = mock()
    private val disableExportNodesUseCase: DisableExportNodesUseCase = mock()
    private val removePublicLinkResultMapper: RemovePublicLinkResultMapper = mock()
    private val checkUri: CheckFileUriUseCase = mock()
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase = mock()
    private val nodeMoveRequestMessageMapper: NodeMoveRequestMessageMapper = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getPublicChildNodeFromIdUseCase: GetPublicChildNodeFromIdUseCase = mock()
    private val getPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase =
        mock()
    private val deleteNodesUseCase: DeleteNodesUseCase = mock()
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase = mock()
    private val imagePreviewVideoLauncher: ImagePreviewVideoLauncher = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val clearImageResultUseCase: ClearImageResultUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()
    private val monitorConnectivityUseCase = Mockito.mock<MonitorConnectivityUseCase>()
    private val getNodeNameCollisionRenameNameUseCase: GetNodeNameCollisionRenameNameUseCase =
        mock()
    private val getNodeAccessPermission: GetNodeAccessPermission = mock()

    @BeforeAll
    fun setup() {
        commonStub()
        initViewModel()
    }

    @BeforeEach
    fun resetMocks() = reset(
        savedStateHandle,
        addImageTypeUseCase,
        getImageUseCase,
        getImageFromFileUseCase,
        checkNodesNameCollisionWithActionUseCase,
        checkChatNodesNameCollisionAndCopyUseCase,
        addFavouritesUseCase,
        removeFavouritesUseCase,
        removeOfflineNodeUseCase,
        monitorOfflineNodeUpdatesUseCase,
        isAvailableOfflineUseCase,
        disableExportNodesUseCase,
        removePublicLinkResultMapper,
        checkUri,
        moveNodesToRubbishUseCase,
        nodeMoveRequestMessageMapper,
        getFeatureFlagValueUseCase,
        getPublicChildNodeFromIdUseCase,
        getPublicNodeFromSerializedDataUseCase,
        deleteNodesUseCase,
        updateNodeSensitiveUseCase,
        imagePreviewVideoLauncher,
        monitorAccountDetailUseCase,
        isHiddenNodesOnboardedUseCase,
        monitorShowHiddenItemsUseCase,
        getBusinessStatusUseCase,
        monitorConnectivityUseCase,
        getNodeNameCollisionRenameNameUseCase,
        getNodeAccessPermission,
    ).also {
        imageNodeFetchers.clear()
        underTest.consumeTransferEvent()
    }

    private fun initViewModel() {
        underTest = ImagePreviewViewModel(
            savedStateHandle = savedStateHandle,
            imageNodeFetchers = imageNodeFetchers,
            imagePreviewMenuMap = imagePreviewMenuMap,
            addImageTypeUseCase = addImageTypeUseCase,
            getImageUseCase = getImageUseCase,
            getImageFromFileUseCase = getImageFromFileUseCase,
            checkChatNodesNameCollisionAndCopyUseCase = checkChatNodesNameCollisionAndCopyUseCase,
            checkNodesNameCollisionWithActionUseCase = checkNodesNameCollisionWithActionUseCase,
            addFavouritesUseCase = addFavouritesUseCase,
            removeFavouritesUseCase = removeFavouritesUseCase,
            removeOfflineNodeUseCase = removeOfflineNodeUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            disableExportNodesUseCase = disableExportNodesUseCase,
            removePublicLinkResultMapper = removePublicLinkResultMapper,
            checkUri = checkUri,
            moveNodesToRubbishUseCase = moveNodesToRubbishUseCase,
            nodeMoveRequestMessageMapper = nodeMoveRequestMessageMapper,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getPublicChildNodeFromIdUseCase = getPublicChildNodeFromIdUseCase,
            getPublicNodeFromSerializedDataUseCase = getPublicNodeFromSerializedDataUseCase,
            deleteNodesUseCase = deleteNodesUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            imagePreviewVideoLauncher = imagePreviewVideoLauncher,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            clearImageResultUseCase = clearImageResultUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getNodeNameCollisionRenameNameUseCase = getNodeNameCollisionRenameNameUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            context = mock()
        )
    }

    private fun commonStub() = runTest {
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the online status is updated correctly`(isOnline: Boolean) = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(isOnline)

        initViewModel()

        underTest.state.test {
            assertThat(expectMostRecentItem().isOnline).isEqualTo(isOnline)
        }
    }

    @Test
    fun `test that filterNonSensitiveNodes return nodes when showHiddenItems is null and isPaid`() =
        runTest {
            val imageNodes = listOf(
                createNonSensitiveNode(),
                createSensitiveNode(),
            )
            val expectedNodes = underTest.filterNonSensitiveNodes(
                imageNodes = imageNodes,
                showHiddenItems = null,
                isPaid = true,
                isBusinessAccountExpired = false,
            )
            assertThat(expectedNodes).isEqualTo(imageNodes)
        }

    @Test
    fun `test that filterNonSensitiveNodes return nodes when from Rubbish Bin regardless of params`() =
        runTest {
            commonStub()
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.RUBBISH_BIN)
            initViewModel()
            val imageNodes = listOf(
                createNonSensitiveNode(),
                createSensitiveNode(),
            )

            val expectedNodes = underTest.filterNonSensitiveNodes(
                imageNodes = imageNodes,
                showHiddenItems = false,
                isPaid = true,
                isBusinessAccountExpired = false,
            )
            assertThat(expectedNodes).isEqualTo(imageNodes)
        }

    @Test
    fun `test that filterNonSensitiveNodes return nodes when showHiddenItems is true and isPaid`() =
        runTest {
            val imageNodes = listOf(
                createNonSensitiveNode(),
                createSensitiveNode(),
            )
            val expectedNodes = underTest.filterNonSensitiveNodes(
                imageNodes = imageNodes,
                showHiddenItems = true,
                isPaid = true,
                isBusinessAccountExpired = false,
            )
            assertThat(expectedNodes).isEqualTo(imageNodes)
        }

    @Test
    fun `test that filterNonSensitiveNodes return nodes when showHiddenItems is true and isNotPaid`() =
        runTest {
            val imageNodes = listOf(
                createNonSensitiveNode(),
                createSensitiveNode(),
            )
            val expectedNodes = underTest.filterNonSensitiveNodes(
                imageNodes = imageNodes,
                showHiddenItems = true,
                isPaid = false,
                isBusinessAccountExpired = false,
            )
            assertThat(expectedNodes).isEqualTo(imageNodes)
        }

    @Test
    fun `test that filterNonSensitiveNodes return nodes when showHiddenItems is true and isPaid is null`() =
        runTest {
            val imageNodes = listOf(
                createNonSensitiveNode(),
                createSensitiveNode(),
            )
            val expectedNodes = underTest.filterNonSensitiveNodes(
                imageNodes = imageNodes,
                showHiddenItems = true,
                isPaid = null,
                isBusinessAccountExpired = false,
            )
            assertThat(expectedNodes).isEqualTo(imageNodes)
        }

    @Test
    fun `test that filterNonSensitiveNodes return non-sensitive nodes when showHiddenItems is false and isPaid`() =
        runTest {
            val nonSensitiveNode = createNonSensitiveNode()
            val imageNodes = listOf(
                nonSensitiveNode,
                createSensitiveNode(),
            )
            val expectedNodes = underTest.filterNonSensitiveNodes(
                imageNodes = imageNodes,
                showHiddenItems = false,
                isPaid = true,
                isBusinessAccountExpired = false,
            )
            assertThat(expectedNodes.size).isEqualTo(1)
            assertThat(expectedNodes).isEqualTo(listOf(nonSensitiveNode))
        }


    @Test
    internal fun `test that resultMessage is set when node is copied`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val successMessage = "Copy success"
            val context = mock<Context> {
                on { getString(R.string.context_correctly_copied) } doReturn successMessage
            }
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.Copy(
                    count = 1,
                    errorCount = 0
                )
            )

            underTest.copyNode(
                context = context,
                copyHandle = selectedNode,
                toHandle = newParentNode,
            )
            advanceUntilIdle()

            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.resultMessage).isEqualTo(successMessage)
            }
        }

    @Test
    internal fun `test that resultMessage is set when node copy is failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val successMessage = "Copy success"
            val context = mock<Context> {
                on { getString(R.string.context_no_copied) } doReturn successMessage
            }
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.Copy(
                    count = 1,
                    errorCount = 1
                )
            )

            underTest.copyNode(
                context = context,
                copyHandle = selectedNode,
                toHandle = newParentNode,
            )
            advanceUntilIdle()

            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.resultMessage).isEqualTo(successMessage)
            }
        }

    @Test
    internal fun `test that copyMoveException is set when copy is failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Copy node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ).thenThrow(runtimeException)
            underTest.copyNode(
                context = mock(),
                copyHandle = selectedNode,
                toHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.copyMoveException).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test that resultMessage is set when node is moved`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val successMessage = "Move success"
            val context = mock<Context> {
                on { getString(R.string.context_correctly_moved) } doReturn successMessage
            }
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )

            underTest.moveNode(
                context = context,
                moveHandle = selectedNode,
                toHandle = newParentNode,
            )
            advanceUntilIdle()

            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.resultMessage).isEqualTo(successMessage)
            }
        }

    @Test
    internal fun `test that resultMessage is set when node move is failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val successMessage = "Move failed"
            val context = mock<Context> {
                on { getString(R.string.context_no_moved) } doReturn successMessage
            }
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )

            underTest.moveNode(
                context = context,
                moveHandle = selectedNode,
                toHandle = newParentNode,
            )
            advanceUntilIdle()

            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.resultMessage).isEqualTo(successMessage)
            }
        }

    @Test
    internal fun `test that copyMoveException is set when move is failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ).thenThrow(runtimeException)
            underTest.moveNode(
                context = mock(),
                moveHandle = selectedNode,
                toHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.copyMoveException).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test that resultMessage is set when chat node is imported`() =
        runTest {
            val newParentNode = 158401030174851
            val chatId = 1000L
            val messageId = 2000L
            val successMessage = "Import success"
            val context = mock<Context> {
                on { getString(R.string.context_correctly_copied) } doReturn successMessage
            }
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = NodeId(newParentNode),
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.importChatNode(
                context = context,
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.resultMessage).isEqualTo(successMessage)
            }
        }

    @Test
    internal fun `test that resultMessage is set when chat node import is failed`() =
        runTest {
            val newParentNode = 158401030174851
            val chatId = 1000L
            val messageId = 2000L
            val successMessage = "Import failed"
            val context = mock<Context> {
                on { getString(R.string.context_no_copied) } doReturn successMessage
            }
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = NodeId(newParentNode),
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.importChatNode(
                context = context,
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.resultMessage).isEqualTo(successMessage)
            }
        }

    @Test
    internal fun `test that copyMoveException is set when import failed`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L

            val runtimeException = RuntimeException("Import node failed")
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ).thenThrow(runtimeException)

            underTest.importChatNode(
                context = mock(),
                chatId = chatId,
                messageId = messageId,
                newParentHandle = newParentNode.longValue,
            )
            advanceUntilIdle()

            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.copyMoveException).isEqualTo(runtimeException)
            }
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/bmp",
            "image/x-ms-bmp",
            "image/heif"
        ]
    )
    internal fun `test that isPhotoEditorMenuVisible returns true when feature flag is enabled and mime type is supported`(
        mimeType: String,
    ) =
        runTest {
            val imageNode = mock<ImageNode> {
                on { type } doReturn StaticImageFileTypeInfo(
                    mimeType,
                    mimeType.substringAfterLast("/")
                )
                on { id } doReturn NodeId(123L)
            }
            whenever(getFeatureFlagValueUseCase(AppFeatures.PhotoEditor)).thenReturn(true)
            whenever(getNodeAccessPermission(NodeId(123L))).thenReturn(AccessPermission.OWNER)
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.TIMELINE)

            val result = underTest.isPhotoEditorMenuVisible(imageNode)

            assertThat(result).isTrue()
        }

    @Test
    internal fun `test that isPhotoEditorMenuVisible returns false when feature flag is disabled`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { type } doReturn StaticImageFileTypeInfo("image/jpeg", "jpg")
                on { id } doReturn NodeId(123L)
            }
            whenever(getFeatureFlagValueUseCase(AppFeatures.PhotoEditor)).thenReturn(false)
            whenever(getNodeAccessPermission(NodeId(123L))).thenReturn(AccessPermission.OWNER)
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.TIMELINE)

            val result = underTest.isPhotoEditorMenuVisible(imageNode)

            assertThat(result).isFalse()
        }

    @Test
    internal fun `test that isPhotoEditorMenuVisible returns false when feature flag throws exception`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { type } doReturn StaticImageFileTypeInfo("image/jpeg", "jpg")
                on { id } doReturn NodeId(123L)
            }
            whenever(getFeatureFlagValueUseCase(AppFeatures.PhotoEditor)).thenThrow(
                RuntimeException(
                    "Feature flag error"
                )
            )
            whenever(getNodeAccessPermission(NodeId(123L))).thenReturn(AccessPermission.OWNER)
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.TIMELINE)

            val result = underTest.isPhotoEditorMenuVisible(imageNode)

            assertThat(result).isFalse()
        }

    @Test
    internal fun `test that isPhotoEditorMenuVisible returns false when user has no write permission`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { type } doReturn StaticImageFileTypeInfo("image/jpeg", "jpg")
                on { id } doReturn NodeId(123L)
            }
            whenever(getFeatureFlagValueUseCase(AppFeatures.PhotoEditor)).thenReturn(true)
            whenever(getNodeAccessPermission(NodeId(123L))).thenReturn(AccessPermission.READ)
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.TIMELINE)

            val result = underTest.isPhotoEditorMenuVisible(imageNode)

            assertThat(result).isFalse()
        }

    @Test
    internal fun `test that isPhotoEditorMenuVisible returns false when access permission throws exception`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { type } doReturn StaticImageFileTypeInfo("image/jpeg", "jpg")
                on { id } doReturn NodeId(123L)
            }
            whenever(getFeatureFlagValueUseCase(AppFeatures.PhotoEditor)).thenReturn(true)
            whenever(getNodeAccessPermission(NodeId(123L))).thenThrow(RuntimeException("Permission error"))
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.TIMELINE)

            val result = underTest.isPhotoEditorMenuVisible(imageNode)

            assertThat(result).isFalse()
        }

    @Test
    internal fun `test that isPhotoEditorMenuVisible returns false when source is not valid`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { type } doReturn StaticImageFileTypeInfo("image/jpeg", "jpg")
                on { id } doReturn NodeId(123L)
            }
            whenever(getFeatureFlagValueUseCase(AppFeatures.PhotoEditor)).thenReturn(true)
            whenever(getNodeAccessPermission(NodeId(123L))).thenReturn(AccessPermission.OWNER)
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.OFFLINE)

            val result = underTest.isPhotoEditorMenuVisible(imageNode)

            assertThat(result).isFalse()
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "image/webp",
            "image/gif",
            "image/raw",
            "image/svg+xml",
            "image/tiff",
            "image/tga"
        ]
    )
    internal fun `test that isPhotoEditorMenuVisible returns false when mime type is not supported`(
        mimeType: String,
    ) =
        runTest {
            val imageNode = mock<ImageNode> {
                on { type } doReturn StaticImageFileTypeInfo(
                    mimeType,
                    mimeType.substringAfterLast("/")
                )
                on { id } doReturn NodeId(123L)
            }
            whenever(getFeatureFlagValueUseCase(AppFeatures.PhotoEditor)).thenReturn(true)
            whenever(getNodeAccessPermission(NodeId(123L))).thenReturn(AccessPermission.OWNER)
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.TIMELINE)

            val result = underTest.isPhotoEditorMenuVisible(imageNode)

            assertThat(result).isFalse()
        }

    @Test
    internal fun `test that isPhotoEditorMenuVisible returns false when node type is video`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { type } doReturn VideoFileTypeInfo("video/mp4", "mp4", Duration.parse("10s"))
                on { id } doReturn NodeId(123L)
            }
            whenever(getFeatureFlagValueUseCase(AppFeatures.PhotoEditor)).thenReturn(true)
            whenever(getNodeAccessPermission(NodeId(123L))).thenReturn(AccessPermission.OWNER)
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.TIMELINE)

            val result = underTest.isPhotoEditorMenuVisible(imageNode)

            assertThat(result).isFalse()
        }

    @Test
    internal fun `test that CopyOfflineNode transfer event is triggered when executeTransfer is invoked in offline mode`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { id } doReturn NodeId(123L)
                on { isAvailableOffline } doReturn true
            }
            val offlineImageNodeFetcher = mock<OfflineImageNodeFetcher>()
            whenever(monitorConnectivityUseCase()) doReturn flowOf(false)
            whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
                .thenReturn(ImagePreviewFetcherSource.OFFLINE)
            whenever(savedStateHandle.get<Long>(PARAMS_CURRENT_IMAGE_NODE_ID_VALUE))
                .thenReturn(123L)
            imageNodeFetchers[ImagePreviewFetcherSource.OFFLINE] = offlineImageNodeFetcher
            whenever(offlineImageNodeFetcher.monitorImageNodes(any())) doReturn flowOf(
                listOf(imageNode)
            )
            initViewModel()

            underTest.executeTransfer(false)

            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.transferEvent.triggeredContent()).isInstanceOf(TransferTriggerEvent.CopyOfflineNode::class.java)
            }
        }

    @Test
    internal fun `test that uploadCurrentEditedImage triggers upload event when current image node exists`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { id } doReturn NodeId(123L)
                on { name } doReturn "test_image.jpg"
                on { size } doReturn 1024L
                on { modificationTime } doReturn 1234567890L
                on { parentId } doReturn NodeId(456L)
            }
            val uri = mock<Uri> {
                on { path } doReturn "/storage/emulated/0/edited_image.jpg"
            }
            val renameName = "test_image (1).jpg"

            whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn(renameName)

            // Set current image node in state
            underTest.setCurrentImageNode(imageNode)

            underTest.uploadCurrentEditedImage(uri)
            advanceUntilIdle()

            underTest.state.test {
                val state = expectMostRecentItem()
                val downloadEvent = state.transferEvent.triggeredContent()
                assertThat(downloadEvent).isInstanceOf(TransferTriggerEvent.StartUpload.Files::class.java)

                val uploadEvent = downloadEvent as TransferTriggerEvent.StartUpload.Files
                assertThat(uploadEvent.pathsAndNames).containsEntry(
                    "/storage/emulated/0/edited_image.jpg",
                    renameName
                )
                assertThat(uploadEvent.destinationId).isEqualTo(NodeId(456L))
            }
        }

    @Test
    internal fun `test that uploadCurrentEditedImage does nothing when uri path is null`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { id } doReturn NodeId(123L)
                on { name } doReturn "test_image.jpg"
                on { size } doReturn 1024L
                on { modificationTime } doReturn 1234567890L
                on { parentId } doReturn NodeId(456L)
            }
            val uri = mock<Uri> {
                on { path } doReturn null
            }

            // Set current image node in state
            underTest.setCurrentImageNode(imageNode)

            underTest.uploadCurrentEditedImage(uri)
            advanceUntilIdle()

            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.transferEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    @Test
    internal fun `test that uploadCurrentEditedImage handles exception gracefully`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { id } doReturn NodeId(123L)
                on { name } doReturn "test_image.jpg"
                on { size } doReturn 1024L
                on { modificationTime } doReturn 1234567890L
                on { parentId } doReturn NodeId(456L)
            }
            val uri = mock<Uri> {
                on { path } doReturn "/storage/emulated/0/edited_image.jpg"
            }

            whenever(getNodeNameCollisionRenameNameUseCase(any())).thenThrow(RuntimeException("Test exception"))

            // Set current image node in state
            underTest.setCurrentImageNode(imageNode)

            underTest.uploadCurrentEditedImage(uri)
            advanceUntilIdle()

            underTest.state.test {
                val state = expectMostRecentItem()
                assertThat(state.transferEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    @Test
    internal fun `test that uploadCurrentEditedImage creates correct FileNameCollision`() =
        runTest {
            val imageNode = mock<ImageNode> {
                on { id } doReturn NodeId(123L)
                on { name } doReturn "test_image.jpg"
                on { size } doReturn 1024L
                on { modificationTime } doReturn 1234567890L
                on { parentId } doReturn NodeId(456L)
            }
            val uri = mock<Uri> {
                on { path } doReturn "/storage/emulated/0/edited_image.jpg"
            }
            val renameName = "test_image (1).jpg"

            whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn(renameName)

            // Set current image node in state
            underTest.setCurrentImageNode(imageNode)

            underTest.uploadCurrentEditedImage(uri)
            advanceUntilIdle()

            // Verify that getNodeNameCollisionRenameNameUseCase was called with correct FileNameCollision
            verify(getNodeNameCollisionRenameNameUseCase).invoke(
                argThat { fileCollision ->
                    fileCollision.collisionHandle == 123L &&
                            fileCollision.name == "test_image.jpg" &&
                            fileCollision.size == 1024L &&
                            fileCollision.lastModified == 1234567890L &&
                            fileCollision.parentHandle == 456L
                }
            )
        }

    private fun createNonSensitiveNode(): ImageNode {
        return mock<ImageNode> {
            on { this.isMarkedSensitive } doReturn false
            on { this.isSensitiveInherited } doReturn false
        }
    }

    private fun createSensitiveNode(
        isMarkedSensitive: Boolean = true,
        isSensitiveInherited: Boolean = true,
    ): ImageNode {
        return mock<ImageNode> {
            on { this.isMarkedSensitive } doReturn isMarkedSensitive
            on { this.isSensitiveInherited } doReturn isSensitiveInherited
        }
    }
}