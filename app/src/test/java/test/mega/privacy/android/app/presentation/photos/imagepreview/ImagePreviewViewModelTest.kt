@file:OptIn(ExperimentalCoroutinesApi::class)

package test.mega.privacy.android.app.presentation.photos.imagepreview

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewVideoLauncher
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
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
import mega.privacy.android.domain.usecase.imagepreview.GetImageFromFileUseCase
import mega.privacy.android.domain.usecase.imagepreview.GetImageUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImagePreviewViewModelTest {

    private lateinit var underTest: ImagePreviewViewModel

    private val savedStateHandle = mock<SavedStateHandle>()
    private val imageNodeFetchers =
        mapOf<@JvmSuppressWildcards ImagePreviewFetcherSource, @JvmSuppressWildcards ImageNodeFetcher>()
    private val imagePreviewMenuMap =
        mapOf<@JvmSuppressWildcards ImagePreviewMenuSource, @JvmSuppressWildcards ImagePreviewMenu>()
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
    private val moveRequestMessageMapper: MoveRequestMessageMapper = mock()
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
        moveRequestMessageMapper,
        getFeatureFlagValueUseCase,
        getPublicChildNodeFromIdUseCase,
        getPublicNodeFromSerializedDataUseCase,
        deleteNodesUseCase,
        updateNodeSensitiveUseCase,
        imagePreviewVideoLauncher,
        monitorAccountDetailUseCase,
        isHiddenNodesOnboardedUseCase,
        monitorShowHiddenItemsUseCase,
    )

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
            moveRequestMessageMapper = moveRequestMessageMapper,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getPublicChildNodeFromIdUseCase = getPublicChildNodeFromIdUseCase,
            getPublicNodeFromSerializedDataUseCase = getPublicNodeFromSerializedDataUseCase,
            deleteNodesUseCase = deleteNodesUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            imagePreviewVideoLauncher = imagePreviewVideoLauncher,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun commonStub() = runTest {
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
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
                isPaid = true
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
                isPaid = true
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
                isPaid = false
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
                isPaid = null
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
                isPaid = true
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