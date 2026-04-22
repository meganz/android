package mega.privacy.android.app.presentation.videoplayer.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerBottomSheetAction
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.FILE_BROWSER_ADAPTER
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetRootParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
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
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlayerBottomSheetActionsMapperTest {
    private lateinit var underTest: VideoPlayerBottomSheetActionsMapper

    private val getNodeAccessUseCase = mock<GetNodeAccessUseCase>()
    private val getRubbishNodeUseCase = mock<GetRubbishNodeUseCase>()
    private val hasSensitiveInheritedUseCase = mock<HasSensitiveInheritedUseCase>()
    private val getRootParentNodeUseCase = mock<GetRootParentNodeUseCase>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = VideoPlayerBottomSheetActionsMapper(
            getNodeAccessUseCase = getNodeAccessUseCase,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
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
            hasSensitiveInheritedUseCase,
            getRootParentNodeUseCase,
            isNodeInBackupsUseCase,
            isNodeInRubbishBinUseCase,
        )
    }

    @ParameterizedTest(name = "Offline source. Should show add: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that offline source returns correct actions`(showAdd: Boolean) = runTest {
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = OFFLINE_ADAPTER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(
            VideoPlayerBottomSheetAction.FileInfo,
            VideoPlayerBottomSheetAction.Share,
        )

        if (showAdd) {
            assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerBottomSheetAction.AddTo)
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
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = RUBBISH_BIN_ADAPTER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = buildList {
            add(VideoPlayerBottomSheetAction.FileInfo)
            if (showAdd) add(VideoPlayerBottomSheetAction.AddTo)
            if (!isNodeInBackup) add(VideoPlayerBottomSheetAction.Remove)
        }

        assertThat(actual).containsExactlyElementsIn(expected)
    }

    private fun provideRubbishBinSourceParams() = listOf(
        Arguments.of(true, true),
        Arguments.of(true, false),
        Arguments.of(false, true),
        Arguments.of(false, false)
    )

    @ParameterizedTest(name = "chat source. Should show add: {0}, canRemoveFromChat: {1}, isNodeInBackup is: {2}")
    @MethodSource("provideChatSourceParams")
    fun `test that chat source returns correct actions`(
        showAdd: Boolean,
        canRemoveFromChat: Boolean,
        isNodeInBackup: Boolean,
    ) = runTest {
        whenever(isNodeInBackupsUseCase(any())).thenReturn(isNodeInBackup)
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = FROM_CHAT,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { canRemoveFromChat },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(
            VideoPlayerBottomSheetAction.Download,
            VideoPlayerBottomSheetAction.ChatImport,
            VideoPlayerBottomSheetAction.SaveForOffline
        )

        assertThat(actual).containsExactlyElementsIn(
            when {
                canRemoveFromChat && !isNodeInBackup && showAdd ->
                    expected + VideoPlayerBottomSheetAction.Remove + VideoPlayerBottomSheetAction.AddTo

                canRemoveFromChat && !isNodeInBackup -> expected + VideoPlayerBottomSheetAction.Remove
                showAdd -> expected + VideoPlayerBottomSheetAction.AddTo
                else -> expected
            }
        )
    }

    @ParameterizedTest(name = "and should show add: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that chat source returns correct actions when node is null`(
        showAdd: Boolean,
    ) = runTest {
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = FROM_CHAT,
            videoNode = null,
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { true },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = buildList {
            add(VideoPlayerBottomSheetAction.Download)
            add(VideoPlayerBottomSheetAction.ChatImport)
            add(VideoPlayerBottomSheetAction.SaveForOffline)
            if (showAdd) {
                add(VideoPlayerBottomSheetAction.AddTo)
            }
        }

        assertThat(actual).containsExactlyElementsIn(expected)
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
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = launchSource,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected =
            listOf(VideoPlayerBottomSheetAction.Download, VideoPlayerBottomSheetAction.Share)

        assertThat(actual).containsExactlyElementsIn(
            if (showAdd)
                expected + VideoPlayerBottomSheetAction.AddTo
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
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = launchSource,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(VideoPlayerBottomSheetAction.Download)

        assertThat(actual).containsExactlyElementsIn(
            if (showAdd)
                expected + VideoPlayerBottomSheetAction.AddTo
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
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = FROM_IMAGE_VIEWER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected =
            listOf(VideoPlayerBottomSheetAction.Download, VideoPlayerBottomSheetAction.Hide)

        assertThat(actual).containsExactlyElementsIn(
            if (showAdd)
                expected + VideoPlayerBottomSheetAction.AddTo
            else
                expected
        )
    }

    @Test
    fun `test that image viewer source return correct actions with hide node action`() = runTest {
        val testRootNode = mock<FolderNode> {
            on { isIncomingShare }.thenReturn(false)
        }
        whenever(getRootParentNodeUseCase(any())).thenReturn(testRootNode)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = FROM_IMAGE_VIEWER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = false,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(VideoPlayerBottomSheetAction.Download)

        assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerBottomSheetAction.Hide)
    }

    @Test
    fun `test that image viewer source return correct actions with unhide node action`() = runTest {
        val testRootNode = mock<FolderNode> {
            on { isIncomingShare }.thenReturn(false)
        }
        whenever(getRootParentNodeUseCase(any())).thenReturn(testRootNode)
        whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
        whenever(hasSensitiveInheritedUseCase(any())).thenReturn(false)
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = FROM_IMAGE_VIEWER,
            videoNode = mock<TypedVideoNode> {
                on { isMarkedSensitive }.thenReturn(true)
            },
            shouldShowAddTo = false,
            canRemoveFromChat = { false },
            isPaidUser = true,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(VideoPlayerBottomSheetAction.Download)

        assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerBottomSheetAction.Unhide)
    }

    @ParameterizedTest(name = "default source. Should show add: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that default source return correct actions`(
        showAdd: Boolean,
    ) = runTest {
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = FILE_BROWSER_ADAPTER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = showAdd,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(
            VideoPlayerBottomSheetAction.Download,
            VideoPlayerBottomSheetAction.FileInfo,
            VideoPlayerBottomSheetAction.SendToChat,
            VideoPlayerBottomSheetAction.Copy,
            VideoPlayerBottomSheetAction.Hide
        )

        assertThat(actual).containsExactlyElementsIn(
            if (showAdd)
                expected + VideoPlayerBottomSheetAction.AddTo
            else
                expected
        )
    }

    @ParameterizedTest(name = "default source. isOwnerPermission: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that default source return correct actions regarding Owner permission`(
        isOwnerPermission: Boolean,
    ) = runTest {
        whenever(getNodeAccessUseCase(any())).thenReturn(
            if (isOwnerPermission) AccessPermission.OWNER else AccessPermission.FULL
        )
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = FILE_BROWSER_ADAPTER,
            videoNode = mock<TypedVideoNode>(),
            shouldShowAddTo = false,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        val expected = listOf(
            VideoPlayerBottomSheetAction.Download,
            VideoPlayerBottomSheetAction.FileInfo,
            VideoPlayerBottomSheetAction.SendToChat,
            VideoPlayerBottomSheetAction.Copy,
            VideoPlayerBottomSheetAction.Hide
        )

        val ownerExpected = listOf(
            VideoPlayerBottomSheetAction.Share,
            VideoPlayerBottomSheetAction.GetLink
        )

        val fullAccessExpected = listOf(
            VideoPlayerBottomSheetAction.Rename,
            VideoPlayerBottomSheetAction.Move,
            VideoPlayerBottomSheetAction.RubbishBin
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
            whenever(getNodeAccessUseCase(any())).thenReturn(AccessPermission.OWNER)
            val actual: List<VideoPlayerBottomSheetAction> = underTest(
                launchSource = FILE_BROWSER_ADAPTER,
                videoNode = mock<TypedVideoNode> {
                    on { exportedData }.thenReturn(mock())
                },
                shouldShowAddTo = false,
                canRemoveFromChat = { false },
                isPaidUser = false,
                isExpiredBusinessUser = false,
            )

            val expected = listOf(
                VideoPlayerBottomSheetAction.Download,
                VideoPlayerBottomSheetAction.FileInfo,
                VideoPlayerBottomSheetAction.SendToChat,
                VideoPlayerBottomSheetAction.Copy,
                VideoPlayerBottomSheetAction.Share,
                VideoPlayerBottomSheetAction.Rename,
                VideoPlayerBottomSheetAction.Move,
                VideoPlayerBottomSheetAction.RubbishBin,
                VideoPlayerBottomSheetAction.Hide
            )

            assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerBottomSheetAction.RemoveLink)
        }

    @Test
    fun `test that default source return correct actions with hide node action`() =
        runTest {
            val testRootNode = mock<FolderNode> {
                on { isIncomingShare }.thenReturn(false)
            }
            whenever(getRootParentNodeUseCase(any())).thenReturn(testRootNode)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            val actual: List<VideoPlayerBottomSheetAction> = underTest(
                launchSource = FILE_BROWSER_ADAPTER,
                videoNode = mock<TypedVideoNode>(),
                shouldShowAddTo = false,
                canRemoveFromChat = { false },
                isPaidUser = false,
                isExpiredBusinessUser = false,
            )

            val expected = listOf(
                VideoPlayerBottomSheetAction.Download,
                VideoPlayerBottomSheetAction.FileInfo,
                VideoPlayerBottomSheetAction.SendToChat,
                VideoPlayerBottomSheetAction.Copy
            )

            assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerBottomSheetAction.Hide)
        }

    @Test
    fun `test that default source return correct actions with unhide node action`() =
        runTest {
            val testRootNode = mock<FolderNode> {
                on { isIncomingShare }.thenReturn(false)
            }
            whenever(getRootParentNodeUseCase(any())).thenReturn(testRootNode)
            whenever(isNodeInBackupsUseCase(any())).thenReturn(false)
            whenever(hasSensitiveInheritedUseCase(any())).thenReturn(false)
            val actual: List<VideoPlayerBottomSheetAction> = underTest(
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
                VideoPlayerBottomSheetAction.Download,
                VideoPlayerBottomSheetAction.FileInfo,
                VideoPlayerBottomSheetAction.SendToChat,
                VideoPlayerBottomSheetAction.Copy
            )

            assertThat(actual).containsExactlyElementsIn(expected + VideoPlayerBottomSheetAction.Unhide)
        }

    @ParameterizedTest(name = "and launchSource is {0}")
    @ValueSource(ints = [FILE_BROWSER_ADAPTER, RUBBISH_BIN_ADAPTER, FROM_IMAGE_VIEWER])
    fun `test that action is empty when node is null`(
        launchSource: Int,
    ) = runTest {
        val actual: List<VideoPlayerBottomSheetAction> = underTest(
            launchSource = launchSource,
            videoNode = null,
            shouldShowAddTo = false,
            canRemoveFromChat = { false },
            isPaidUser = false,
            isExpiredBusinessUser = false,
        )

        assertThat(actual).isEmpty()
    }
}
