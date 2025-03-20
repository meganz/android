package mega.privacy.android.app.presentation.videoplayer.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerAddToAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerChatImportAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerCopyAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerDownloadAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerFileInfoAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerGetLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerHideAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerMoveAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRemoveAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRemoveLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRenameAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRubbishBinAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSaveForOfflineAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSendToChatAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerShareAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerUnhideAction
import mega.privacy.android.app.utils.Constants.FILE_BROWSER_ADAPTER
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetRootParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.math.exp

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LaunchSourceMapperTest {
    private lateinit var underTest: LaunchSourceMapper

    private val getNodeAccessUseCase = mock<GetNodeAccessUseCase>()
    private val getRubbishNodeUseCase = mock<GetRubbishNodeUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val hasSensitiveInheritedUseCase = mock<HasSensitiveInheritedUseCase>()
    private val getRootParentNodeUseCase = mock<GetRootParentNodeUseCase>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = LaunchSourceMapper(
            getNodeAccessUseCase = getNodeAccessUseCase,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            hasSensitiveInheritedUseCase = hasSensitiveInheritedUseCase,
            getRootParentNodeUseCase = getRootParentNodeUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeAccessUseCase,
            getRubbishNodeUseCase,
            getFeatureFlagValueUseCase,
            hasSensitiveInheritedUseCase,
            getRootParentNodeUseCase,
            isNodeInBackupsUseCase,
            isNodeInRubbishBinUseCase,
        )
    }

    @ParameterizedTest(name = "Offline source. Should show add: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that offline source returns correct actions`(showAdd: Boolean) = runTest {
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = OFFLINE_ADAPTER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(
            VideoPlayerFileInfoAction,
            VideoPlayerShareAction,
        )

        if (showAdd) {
            assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerAddToAction)
        } else {
            assertThat(actual).containsExactlyElementsIn(expected)
        }
    }

    @ParameterizedTest(name = "Rubbish bin source. Should show add: {0}, isNodeInBackup is: {1}")
    @MethodSource("provideRubbishBinSourceParams")
    fun `test that rubbish bin source returns correct actions`(
        showAdd: Boolean,
        isNodeInBackup: Boolean,
    ) = runTest {
        whenever(isNodeInBackupsUseCase(any())).thenReturn(isNodeInBackup)
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = RUBBISH_BIN_ADAPTER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = buildList<VideoPlayerMenuAction> {
            add(VideoPlayerFileInfoAction)
            if (showAdd) add(VideoPlayerAddToAction)
            if (!isNodeInBackup) add(VideoPlayerRemoveAction)
        }

        assertThat(actual).containsExactlyElementsIn(expected)
    }

    private fun provideRubbishBinSourceParams() = listOf(
        Arguments.of(true, true),
        Arguments.of(true, false),
        Arguments.of(false, true),
        Arguments.of(false, false)
    )

    @ParameterizedTest(name = "chat bin source. Should show add: {0}, canRemoveFromChat: {1}, isNodeInBackup is: {2}")
    @MethodSource("provideChatSourceParams")
    fun `test that chat source returns correct actions`(
        showAdd: Boolean,
        canRemoveFromChat: Boolean,
        isNodeInBackup: Boolean,
    ) = runTest {
        whenever(isNodeInBackupsUseCase(any())).thenReturn(isNodeInBackup)
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = FROM_CHAT,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { canRemoveFromChat },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(
            VideoPlayerDownloadAction,
            VideoPlayerChatImportAction,
            VideoPlayerSaveForOfflineAction
        )

        assertThat(actual).containsExactlyElementsIn(
            when {
                canRemoveFromChat && !isNodeInBackup && showAdd ->
                    expected + VideoPlayerRemoveAction + VideoPlayerAddToAction

                canRemoveFromChat && !isNodeInBackup -> expected + VideoPlayerRemoveAction
                showAdd -> expected + VideoPlayerAddToAction
                else -> expected
            }
        )
    }

    private fun provideChatSourceParams() = listOf(
        Arguments.of(true, true, true),
        Arguments.of(true, false, false),
        Arguments.of(false, true, false),
        Arguments.of(false, false, true),
        Arguments.of(true, true, false),
        Arguments.of(true, false, true),
        Arguments.of(false, true, true),
        Arguments.of(false, false, false)
    )

    @ParameterizedTest(name = "file link and zip source. launch source: {0}, Should show add: {1}")
    @MethodSource("provideFileLinkAndZipSourceParams")
    fun `test that file link and zip source return correct actions`(
        launchSource: Int,
        showAdd: Boolean,
    ) = runTest {
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = launchSource,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(VideoPlayerDownloadAction, VideoPlayerShareAction)

        assertThat(actual).containsExactlyElementsIn(
            if (showAdd)
                expected + VideoPlayerAddToAction
            else
                expected
        )
    }

    private fun provideFileLinkAndZipSourceParams() = listOf(
        Arguments.of(FILE_LINK_ADAPTER, true),
        Arguments.of(FILE_LINK_ADAPTER, false),
        Arguments.of(ZIP_ADAPTER, true),
        Arguments.of(ZIP_ADAPTER, false)
    )

    @ParameterizedTest(name = "folder link, album, version source. launch source: {0}, Should show add: {1}")
    @MethodSource("provideFolderLinkAlbumAndVersionSourceParams")
    fun `test that folder link, album, version source return correct actions`(
        launchSource: Int,
        showAdd: Boolean,
    ) = runTest {
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = launchSource,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(VideoPlayerDownloadAction)

        assertThat(actual).containsExactlyElementsIn(
            if (showAdd)
                expected + VideoPlayerAddToAction
            else
                expected
        )
    }

    private fun provideFolderLinkAlbumAndVersionSourceParams() = listOf(
        Arguments.of(FOLDER_LINK_ADAPTER, true),
        Arguments.of(FOLDER_LINK_ADAPTER, false),
        Arguments.of(FROM_ALBUM_SHARING, true),
        Arguments.of(FROM_ALBUM_SHARING, false),
        Arguments.of(VERSIONS_ADAPTER, true),
        Arguments.of(VERSIONS_ADAPTER, false)
    )

    @ParameterizedTest(name = "image viewer source. Should show add: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that image viewer source return correct actions`(
        showAdd: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(false)
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = FROM_IMAGE_VIEWER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(VideoPlayerDownloadAction)

        assertThat(actual).containsExactlyElementsIn(
            if (showAdd)
                expected + VideoPlayerAddToAction
            else
                expected
        )
    }

    @Test
    fun `test that image viewer source return correct actions with hide node action`() = runTest {
        val testRootNode = mock<FolderNode> {
            on { isIncomingShare }.thenReturn(false)
        }
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
        whenever(getRootParentNodeUseCase(any())).thenReturn(testRootNode)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = FROM_IMAGE_VIEWER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = false,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(VideoPlayerDownloadAction)

        assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerHideAction)
    }

    @Test
    fun `test that image viewer source return correct actions with unhide node action`() = runTest {
        val testRootNode = mock<FolderNode> {
            on { isIncomingShare }.thenReturn(false)
        }
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
        whenever(getRootParentNodeUseCase(any())).thenReturn(testRootNode)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(hasSensitiveInheritedUseCase(any())).thenReturn(false)
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = FROM_IMAGE_VIEWER,
            videoNode = mock<TypedVideoNode> {
                on { isMarkedSensitive }.thenReturn(true)
            },
            shouldShowAddTo = false,
            canRemoveFromChat = { false },
            isPaidUser = true,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(VideoPlayerDownloadAction)

        assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerUnhideAction)
    }

    @ParameterizedTest(name = "default source. Should show add: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that default source return correct actions`(
        showAdd: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(false)
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = FILE_BROWSER_ADAPTER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(
            VideoPlayerDownloadAction,
            VideoPlayerFileInfoAction,
            VideoPlayerSendToChatAction,
            VideoPlayerCopyAction
        )

        assertThat(actual).containsExactlyElementsIn(
            if (showAdd)
                expected + VideoPlayerAddToAction
            else
                expected
        )
    }

    @ParameterizedTest(name = "default source. isOwnerPermission: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that default source return correct actions regarding Owner permission`(
        isOwnerPermission: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(false)
        whenever(getNodeAccessUseCase(any())).thenReturn(
            if (isOwnerPermission) AccessPermission.OWNER else AccessPermission.FULL
        )
        val actual: List<VideoPlayerMenuAction> = underTest(
            launchSource = FILE_BROWSER_ADAPTER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = false,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(
            VideoPlayerDownloadAction,
            VideoPlayerFileInfoAction,
            VideoPlayerSendToChatAction,
            VideoPlayerCopyAction
        )

        val ownerExpected = listOf(
            VideoPlayerShareAction,
            VideoPlayerGetLinkAction
        )

        val fullAccessExpected = listOf(
            VideoPlayerRenameAction,
            VideoPlayerMoveAction,
            VideoPlayerRubbishBinAction
        )
        assertThat(actual).containsExactlyElementsIn(
            if (isOwnerPermission) {
                expected + ownerExpected + fullAccessExpected
            } else {
                expected + fullAccessExpected
            }
        )
    }

    @Test
    fun `test that default source return correct actions with remove link action`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(false)
            whenever(getNodeAccessUseCase(any())).thenReturn(AccessPermission.OWNER)
            val actual: List<VideoPlayerMenuAction> = underTest(
                launchSource = FILE_BROWSER_ADAPTER,
                videoNode = mock<TypedVideoNode> {
                    on { exportedData }.thenReturn(mock())
                },
                shouldShowAddTo = false,
                canRemoveFromChat = { false },
                isPaidUser = false,
                isExpiredBusinessUser = false,
            )

            val expected = listOf (
                VideoPlayerDownloadAction,
                VideoPlayerFileInfoAction,
                VideoPlayerSendToChatAction,
                VideoPlayerCopyAction,
                VideoPlayerShareAction,
                VideoPlayerRenameAction,
                VideoPlayerMoveAction,
                VideoPlayerRubbishBinAction,
            )

            assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerRemoveLinkAction)
        }

    @Test
    fun `test that default source return correct actions with hide node action`() =
        runTest {
            val testRootNode = mock<FolderNode> {
                on { isIncomingShare }.thenReturn(false)
            }
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
            whenever(getRootParentNodeUseCase(any())).thenReturn(testRootNode)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            val actual: List<VideoPlayerMenuAction> = underTest(
                launchSource = FILE_BROWSER_ADAPTER,
                videoNode = mock<TypedVideoNode>(),
                shouldShowAddTo = false,
                canRemoveFromChat = { false },
                isPaidUser = false,
                isExpiredBusinessUser = false,
            )

            val expected = listOf(
                VideoPlayerDownloadAction,
                VideoPlayerFileInfoAction,
                VideoPlayerSendToChatAction,
                VideoPlayerCopyAction
            )

            assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerHideAction)
        }

    @Test
    fun `test that default source return correct actions with unhide node action`() =
        runTest {
            val testRootNode = mock<FolderNode> {
                on { isIncomingShare }.thenReturn(false)
            }
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
            whenever(getRootParentNodeUseCase(any())).thenReturn(testRootNode)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(hasSensitiveInheritedUseCase(any())).thenReturn(false)
            val actual: List<VideoPlayerMenuAction> = underTest(
                launchSource = FILE_BROWSER_ADAPTER,
                videoNode = mock<TypedVideoNode> {
                    on { isMarkedSensitive }.thenReturn(true)
                },
                shouldShowAddTo = false,
                canRemoveFromChat = { false },
                isPaidUser = true,
                isExpiredBusinessUser = false,
            )

            val expected = listOf(
                VideoPlayerDownloadAction,
                VideoPlayerFileInfoAction,
                VideoPlayerSendToChatAction,
                VideoPlayerCopyAction
            )

            assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerUnhideAction)
        }
}