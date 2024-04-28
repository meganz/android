@file:OptIn(ExperimentalCoroutinesApi::class)

package test.mega.privacy.android.app.presentation.photos.imagepreview

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth
import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.offline.SetNodeAvailableOffline
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewVideoLauncher
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.favourites.AddFavouritesUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.CheckFileUriUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageFromFileUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyTypedNodeUseCase
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ResetTotalDownloadsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
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
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase = mock()
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase = mock()
    private val checkNameCollision: CheckNameCollision = mock()
    private val copyNodeUseCase: CopyNodeUseCase = mock()
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase = mock()
    private val moveNodeUseCase: MoveNodeUseCase = mock()
    private val addFavouritesUseCase: AddFavouritesUseCase = mock()
    private val removeFavouritesUseCase: RemoveFavouritesUseCase = mock()
    private val setNodeAvailableOffline: SetNodeAvailableOffline = mock()
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
    private val resetTotalDownloadsUseCase: ResetTotalDownloadsUseCase = mock()
    private val deleteNodesUseCase: DeleteNodesUseCase = mock()
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase = mock()
    private val imagePreviewVideoLauncher: ImagePreviewVideoLauncher = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase = mock()
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase = mock()
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase = mock()
    private val getParentNodeUseCase: GetParentNodeUseCase = mock()

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
        areTransfersPausedUseCase,
        checkNameCollisionUseCase,
        checkNameCollision,
        copyNodeUseCase,
        copyTypedNodeUseCase,
        moveNodeUseCase,
        addFavouritesUseCase,
        removeFavouritesUseCase,
        setNodeAvailableOffline,
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
        resetTotalDownloadsUseCase,
        deleteNodesUseCase,
        updateNodeSensitiveUseCase,
        imagePreviewVideoLauncher,
        monitorAccountDetailUseCase,
        isHiddenNodesOnboardedUseCase,
        monitorShowHiddenItemsUseCase,
        getPrimarySyncHandleUseCase,
        getSecondarySyncHandleUseCase,
        getMyChatsFilesFolderIdUseCase,
        getParentNodeUseCase,
    )

    private fun initViewModel() {
        underTest = ImagePreviewViewModel(
            savedStateHandle = savedStateHandle,
            imageNodeFetchers = imageNodeFetchers,
            imagePreviewMenuMap = imagePreviewMenuMap,
            addImageTypeUseCase = addImageTypeUseCase,
            getImageUseCase = getImageUseCase,
            getImageFromFileUseCase = getImageFromFileUseCase,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            checkNameCollisionUseCase = checkNameCollisionUseCase,
            checkNameCollision = checkNameCollision,
            copyNodeUseCase = copyNodeUseCase,
            copyTypedNodeUseCase = copyTypedNodeUseCase,
            moveNodeUseCase = moveNodeUseCase,
            addFavouritesUseCase = addFavouritesUseCase,
            removeFavouritesUseCase = removeFavouritesUseCase,
            setNodeAvailableOffline = setNodeAvailableOffline,
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
            resetTotalDownloadsUseCase = resetTotalDownloadsUseCase,
            deleteNodesUseCase = deleteNodesUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            imagePreviewVideoLauncher = imagePreviewVideoLauncher,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            getMyChatsFilesFolderIdUseCase =  getMyChatsFilesFolderIdUseCase,
            getParentNodeUseCase = getParentNodeUseCase,
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