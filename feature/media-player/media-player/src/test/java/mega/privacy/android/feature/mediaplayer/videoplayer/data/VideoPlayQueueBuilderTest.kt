package mega.privacy.android.feature.mediaplayer.videoplayer.data

import android.net.Uri
import androidx.media3.common.MediaItem
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodeByHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByEmailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByHandlesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByParentHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromInSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromOutSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromPublicLinksUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideosBySearchTypeUseCase
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateDownloadLocationUseCase
import mega.privacy.android.feature.mediaplayer.videoplayer.data.mapper.VideoNodeToMediaItemMapper
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSource
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSourceType
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for [VideoPlayQueueBuilder].
 *
 * The queue contents are asserted through a mocked [VideoNodeToMediaItemMapper] rather than real
 * media items: resolving a sibling's stream ends in `Uri.parse`, which is not available in a JVM
 * test, so the tests that reach that far stub it with `Mockito.mockStatic`. Same approach the audio
 * builder's tests take, for the same reason.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VideoPlayQueueBuilderTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase = mock()
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase = mock()
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase = mock()
    private val getLocalLinkFromMegaApiUseCase: GetLocalLinkFromMegaApiUseCase = mock()
    private val getVideoNodeByHandleUseCase: GetVideoNodeByHandleUseCase = mock()
    private val getVideoNodesUseCase: GetVideoNodesUseCase = mock()
    private val getVideoNodesByParentHandleUseCase: GetVideoNodesByParentHandleUseCase = mock()
    private val getVideoNodesByHandlesUseCase: GetVideoNodesByHandlesUseCase = mock()
    private val getVideoNodesByEmailUseCase: GetVideoNodesByEmailUseCase = mock()
    private val getVideoNodesFromPublicLinksUseCase: GetVideoNodesFromPublicLinksUseCase = mock()
    private val getVideoNodesFromInSharesUseCase: GetVideoNodesFromInSharesUseCase = mock()
    private val getVideoNodesFromOutSharesUseCase: GetVideoNodesFromOutSharesUseCase = mock()
    private val getVideosBySearchTypeUseCase: GetVideosBySearchTypeUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase = mock()
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase = mock()
    private val getFingerprintUseCase: GetFingerprintUseCase = mock()
    private val getOrCreateDownloadLocationUseCase: GetOrCreateDownloadLocationUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase =
        mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()
    private val videoNodeToMediaItemMapper: VideoNodeToMediaItemMapper = mock()

    private lateinit var underTest: VideoPlayQueueBuilder

    @BeforeEach
    fun setUp() = runTest(testDispatcher) {
        reset(
            megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase,
            getLocalFilePathUseCase,
            getLocalLinkFromMegaApiUseCase,
            getVideoNodeByHandleUseCase,
            getVideoNodesUseCase,
            getVideoNodesByParentHandleUseCase,
            getVideoNodesByHandlesUseCase,
            getVideoNodesByEmailUseCase,
            getVideoNodesFromPublicLinksUseCase,
            getVideoNodesFromInSharesUseCase,
            getVideoNodesFromOutSharesUseCase,
            getVideosBySearchTypeUseCase,
            getRootNodeUseCase,
            getRubbishNodeUseCase,
            getBackupsNodeUseCase,
            getFingerprintUseCase,
            getOrCreateDownloadLocationUseCase,
            monitorShowHiddenItemsUseCase,
            monitorSubFolderMediaDiscoverySettingsUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase,
            videoNodeToMediaItemMapper,
        )
        // The server is already up unless a test says otherwise, so nothing starts one by accident.
        whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(monitorSubFolderMediaDiscoverySettingsUseCase()).thenReturn(flowOf(true))
        val freeAccount = AccountDetail()
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(freeAccount))
        whenever(getVideoNodesUseCase(any())).thenReturn(emptyList())
        whenever(getLocalFilePathUseCase(any())).thenReturn(null)
        whenever(getLocalLinkFromMegaApiUseCase(any())).thenReturn("http://127.0.0.1/video")
        val defaultItem = mock<MediaItem>()
        val defaultNodeItem = mock<MediaItem>()
        whenever(videoNodeToMediaItemMapper(any<Long>(), any<Uri>(), anyOrNull()))
            .thenReturn(defaultItem)
        whenever(videoNodeToMediaItemMapper(any<TypedVideoNode>(), any<Uri>()))
            .thenReturn(defaultNodeItem)

        underTest = VideoPlayQueueBuilder(
            ioDispatcher = testDispatcher,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            getLocalFilePathUseCase = getLocalFilePathUseCase,
            getLocalLinkFromMegaApiUseCase = getLocalLinkFromMegaApiUseCase,
            getVideoNodeByHandleUseCase = getVideoNodeByHandleUseCase,
            getVideoNodesUseCase = getVideoNodesUseCase,
            getVideoNodesByParentHandleUseCase = getVideoNodesByParentHandleUseCase,
            getVideoNodesByHandlesUseCase = getVideoNodesByHandlesUseCase,
            getVideoNodesByEmailUseCase = getVideoNodesByEmailUseCase,
            getVideoNodesFromPublicLinksUseCase = getVideoNodesFromPublicLinksUseCase,
            getVideoNodesFromInSharesUseCase = getVideoNodesFromInSharesUseCase,
            getVideoNodesFromOutSharesUseCase = getVideoNodesFromOutSharesUseCase,
            getVideosBySearchTypeUseCase = getVideosBySearchTypeUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            getBackupsNodeUseCase = getBackupsNodeUseCase,
            getFingerprintUseCase = getFingerprintUseCase,
            getOrCreateDownloadLocationUseCase = getOrCreateDownloadLocationUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorSubFolderMediaDiscoverySettingsUseCase = monitorSubFolderMediaDiscoverySettingsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            videoNodeToMediaItemMapper = videoNodeToMediaItemMapper,
        )
    }

    private fun launchSource(
        contentUri: Uri? = mock(),
        handle: Long = TAPPED_HANDLE,
        sourceType: VideoPlayerLaunchSourceType = VideoPlayerLaunchSourceType.VideoBrowse,
        isPlaylist: Boolean = true,
        rebuildPlaylist: Boolean = true,
        parentHandle: Long = INVALID_HANDLE,
    ) = VideoPlayerLaunchSource(
        contentUri = contentUri,
        handle = handle,
        fileName = "video.mp4",
        sourceType = sourceType,
        isPlaylist = isPlaylist,
        rebuildPlaylist = rebuildPlaylist,
        parentHandle = parentHandle,
        sortOrder = SortOrder.ORDER_DEFAULT_ASC,
    )

    private fun videoNode(
        handle: Long,
        isTakenDown: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ) = mock<TypedVideoNode> {
        on { id }.thenReturn(NodeId(handle))
        on { name }.thenReturn("video-$handle.mp4")
        on { this.isTakenDown }.thenReturn(isTakenDown)
        on { this.isMarkedSensitive }.thenReturn(isMarkedSensitive)
        on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
    }

    private fun paidAccount() = AccountDetail(
        levelDetail = AccountLevelDetail(
            accountType = AccountType.PRO_I,
            subscriptionStatus = null,
            subscriptionRenewTime = 0L,
            accountSubscriptionCycle = mock(),
            proExpirationTime = 0L,
            accountPlanDetail = null,
            accountSubscriptionDetailList = emptyList(),
        )
    )

    @Test
    fun `test that invoke emits the tapped video on its own first`() = runTest {
        val firstItem = mock<MediaItem>()
        whenever(videoNodeToMediaItemMapper(any<Long>(), any<Uri>(), anyOrNull()))
            .thenReturn(firstItem)

        underTest(launchSource()).test {
            val emitted = awaitItem()

            assertThat(emitted.mediaItems).containsExactly(firstItem)
            assertThat(emitted.currentIndex).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits nothing when the launch has no content URI`() = runTest {
        underTest(launchSource(contentUri = null)).test {
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke stops after the tapped video when the launch is not a playlist`() =
        runTest {
            underTest(launchSource(isPlaylist = false)).test {
                awaitItem()
                awaitComplete()
            }

            verify(getVideoNodesUseCase, never()).invoke(any())
        }

    @Test
    fun `test that invoke stops after the tapped video when the caller wants no rebuild`() =
        runTest {
            underTest(launchSource(rebuildPlaylist = false)).test {
                awaitItem()
                awaitComplete()
            }

            verify(getVideoNodesUseCase, never()).invoke(any())
        }

    @Test
    fun `test that invoke gathers the siblings a VideoBrowse launch sits among`() = runTest {
        val source = launchSource()

        underTest(source).test { cancelAndIgnoreRemainingEvents() }

        verify(getVideoNodesUseCase).invoke(source.sortOrder)
    }

    @Test
    fun `test that invoke gathers the siblings a Search launch sits among`() = runTest {
        whenever(getVideoNodesByHandlesUseCase(any())).thenReturn(emptyList())
        val source = launchSource(sourceType = VideoPlayerLaunchSourceType.Search)
            .copy(searchedItems = listOf(1L, 2L))

        underTest(source).test { cancelAndIgnoreRemainingEvents() }

        verify(getVideoNodesByHandlesUseCase).invoke(listOf(1L, 2L))
    }

    @Test
    fun `test that invoke follows the media discovery setting into sub-folders`() = runTest {
        whenever(monitorSubFolderMediaDiscoverySettingsUseCase()).thenReturn(flowOf(false))
        whenever(
            getVideosBySearchTypeUseCase(any(), any(), any(), any())
        ).thenReturn(emptyList())
        val source = launchSource(
            sourceType = VideoPlayerLaunchSourceType.MediaDiscovery,
            parentHandle = 99L,
        )

        underTest(source).test { cancelAndIgnoreRemainingEvents() }

        // The queue has to cover the same ground the grid behind it showed.
        verify(getVideosBySearchTypeUseCase).invoke(
            handle = 99L,
            searchString = "*",
            recursive = false,
            order = source.sortOrder,
        )
    }

    @Test
    fun `test that invoke starts the streaming server when it is not running`() = runTest {
        whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
        val nodes = listOf(videoNode(TAPPED_HANDLE))
        whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

        Mockito.mockStatic(Uri::class.java).use {
            val parsedUri = mock<Uri>()
            whenever(Uri.parse(any())).thenReturn(parsedUri)

            underTest(launchSource()).test {
                awaitItem()
                // Ownership goes out before the siblings are resolved, not with the finished
                // queue: a player closed in between would otherwise never learn it owns the
                // server.
                assertThat(awaitItem().startedStreamingServer).isTrue()
                assertThat(awaitItem().startedStreamingServer).isTrue()
                awaitComplete()
            }
        }
        verify(megaApiHttpServerStartUseCase).invoke()
    }

    @Test
    fun `test that invoke hands the streaming server over before the siblings resolve`() = runTest {
        whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(0)
        // Every sibling is dropped, so the queue never reaches a second item. The hand-over still
        // has to arrive, or nothing is left to stop the server this build started.
        val nodes = listOf(videoNode(TAPPED_HANDLE, isTakenDown = true))
        whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

        underTest(launchSource()).test {
            awaitItem()
            val handOver = awaitItem()
            assertThat(handOver.startedStreamingServer).isTrue()
            assertThat(handOver.mediaItems).hasSize(1)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke leaves a streaming server someone else started alone`() = runTest {
        whenever(megaApiHttpServerIsRunningUseCase()).thenReturn(1)
        val nodes = listOf(videoNode(TAPPED_HANDLE))
        whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

        Mockito.mockStatic(Uri::class.java).use {
            val parsedUri = mock<Uri>()
            whenever(Uri.parse(any())).thenReturn(parsedUri)

            underTest(launchSource()).test {
                awaitItem()
                assertThat(awaitItem().startedStreamingServer).isFalse()
                awaitComplete()
            }
        }
        verify(megaApiHttpServerStartUseCase, never()).invoke()
    }

    @Test
    fun `test that the full queue keeps the item the tapped video started on`() = runTest {
        val firstItem = mock<MediaItem>()
        whenever(videoNodeToMediaItemMapper(any<Long>(), any<Uri>(), anyOrNull()))
            .thenReturn(firstItem)
        val nodes = listOf(videoNode(1L), videoNode(TAPPED_HANDLE), videoNode(3L))
        whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

        Mockito.mockStatic(Uri::class.java).use {
            val parsedUri = mock<Uri>()
            whenever(Uri.parse(any())).thenReturn(parsedUri)

            underTest(launchSource()).test {
                awaitItem()
                val full = awaitItem()

                // What the player needs is the media id it is already on, sitting at the index it
                // is told to start from; reusing the item the first emission was built with is
                // how the builder guarantees it.
                assertThat(full.mediaItems).hasSize(3)
                assertThat(full.mediaItems[1]).isSameInstanceAs(firstItem)
                assertThat(full.currentIndex).isEqualTo(1)
                awaitComplete()
            }
        }
    }

    @Test
    fun `test that the full queue puts the tapped video first when it is not among the siblings`() =
        runTest {
            val firstItem = mock<MediaItem>()
            whenever(videoNodeToMediaItemMapper(any<Long>(), any<Uri>(), anyOrNull()))
                .thenReturn(firstItem)
            val nodes = listOf(videoNode(1L), videoNode(3L))
            whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

            Mockito.mockStatic(Uri::class.java).use {
                val parsedUri = mock<Uri>()
                whenever(Uri.parse(any())).thenReturn(parsedUri)

                underTest(launchSource()).test {
                    awaitItem()
                    val full = awaitItem()

                    assertThat(full.mediaItems).hasSize(3)
                    assertThat(full.mediaItems[0]).isSameInstanceAs(firstItem)
                    assertThat(full.currentIndex).isEqualTo(0)
                    awaitComplete()
                }
            }
        }

    @Test
    fun `test that a taken down video is left out of the queue`() = runTest {
        val nodes = listOf(videoNode(TAPPED_HANDLE), videoNode(5L, isTakenDown = true))
        whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

        Mockito.mockStatic(Uri::class.java).use {
            val parsedUri = mock<Uri>()
            whenever(Uri.parse(any())).thenReturn(parsedUri)

            underTest(launchSource()).test {
                awaitItem()
                assertThat(awaitItem().mediaItems).hasSize(1)
                awaitComplete()
            }
        }
        verify(getLocalLinkFromMegaApiUseCase, never()).invoke(5L)
    }

    @Test
    fun `test that a sensitive video is left out when a paid account hides hidden items`() =
        runTest {
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            val paid = paidAccount()
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(paid))
            val nodes = listOf(videoNode(TAPPED_HANDLE), videoNode(5L, isMarkedSensitive = true))
            whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

            Mockito.mockStatic(Uri::class.java).use {
                val parsedUri = mock<Uri>()
                whenever(Uri.parse(any())).thenReturn(parsedUri)

                underTest(launchSource()).test {
                    awaitItem()
                    assertThat(awaitItem().mediaItems).hasSize(1)
                    awaitComplete()
                }
            }
        }

    @Test
    fun `test that a sensitive video is kept when the account cannot hide items`() = runTest {
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(AccountDetail()))
        val nodes = listOf(videoNode(TAPPED_HANDLE), videoNode(5L, isMarkedSensitive = true))
        whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

        Mockito.mockStatic(Uri::class.java).use {
            val parsedUri = mock<Uri>()
            whenever(Uri.parse(any())).thenReturn(parsedUri)

            underTest(launchSource()).test {
                awaitItem()
                // Hiding is a paid feature; a free account has nothing to filter by.
                assertThat(awaitItem().mediaItems).hasSize(2)
                awaitComplete()
            }
        }
    }

    @Test
    fun `test that a video whose stream cannot be resolved is left out of the queue`() = runTest {
        val nodes = listOf(videoNode(TAPPED_HANDLE), videoNode(5L))
        whenever(getVideoNodesUseCase(any())).thenReturn(nodes)
        whenever(getLocalLinkFromMegaApiUseCase(5L)).thenReturn(null)

        Mockito.mockStatic(Uri::class.java).use {
            val parsedUri = mock<Uri>()
            whenever(Uri.parse(any())).thenReturn(parsedUri)

            underTest(launchSource()).test {
                awaitItem()
                assertThat(awaitItem().mediaItems).hasSize(1)
                awaitComplete()
            }
        }
    }

    @Test
    fun `test that the tapped video keeps playing when every sibling is filtered out`() = runTest {
        val nodes = listOf(videoNode(TAPPED_HANDLE, isTakenDown = true))
        whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

        underTest(launchSource()).test {
            awaitItem()
            // Nothing follows: an empty sibling search says the video has no queue, not that it
            // should stop. The server was already running here, so there is no hand-over either.
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke gathers the siblings under the folder a launch names`() = runTest {
        val parent = mock<TypedVideoNode> { on { id }.thenReturn(NodeId(PARENT_HANDLE)) }
        whenever(getVideoNodeByHandleUseCase(PARENT_HANDLE)).thenReturn(parent)
        val nodes = listOf(videoNode(TAPPED_HANDLE))
        whenever(getVideoNodesByParentHandleUseCase(any(), any())).thenReturn(nodes)

        Mockito.mockStatic(Uri::class.java).use {
            val parsedUri = mock<Uri>()
            whenever(Uri.parse(any())).thenReturn(parsedUri)

            underTest(
                launchSource(
                    sourceType = VideoPlayerLaunchSourceType.FileBrowser,
                    parentHandle = PARENT_HANDLE,
                )
            ).test {
                awaitItem()
                assertThat(awaitItem().mediaItems).hasSize(1)
                awaitComplete()
            }
        }
        verify(getVideoNodesByParentHandleUseCase)(
            parentHandle = PARENT_HANDLE,
            order = SortOrder.ORDER_DEFAULT_ASC,
        )
    }

    @Test
    fun `test that invoke falls back to the root folder when the launch names no parent`() =
        runTest {
            val root = mock<TypedVideoNode> { on { id }.thenReturn(NodeId(ROOT_HANDLE)) }
            whenever(getRootNodeUseCase()).thenReturn(root)
            whenever(getVideoNodesByParentHandleUseCase(any(), any())).thenReturn(emptyList())

            underTest(launchSource(sourceType = VideoPlayerLaunchSourceType.FileBrowser)).test {
                awaitItem()
                awaitComplete()
            }

            verify(getVideoNodesByParentHandleUseCase)(
                parentHandle = ROOT_HANDLE,
                order = SortOrder.ORDER_DEFAULT_ASC,
            )
        }

    @Test
    fun `test that invoke hides sensitive videos when the hidden items setting cannot be read`() =
        runTest {
            whenever(monitorShowHiddenItemsUseCase())
                .thenReturn(flow { throw IllegalStateException() })
            // Hiding is a paid feature, so the account has to be able to hide before the failed
            // setting read decides anything.
            val paid = paidAccount()
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(paid))
            val nodes = listOf(videoNode(TAPPED_HANDLE), videoNode(3L, isMarkedSensitive = true))
            whenever(getVideoNodesUseCase(any())).thenReturn(nodes)

            Mockito.mockStatic(Uri::class.java).use {
                val parsedUri = mock<Uri>()
                whenever(Uri.parse(any())).thenReturn(parsedUri)

                underTest(launchSource()).test {
                    awaitItem()
                    // Failing open would be this filter putting back the very content it exists
                    // to keep out of sight.
                    assertThat(awaitItem().mediaItems).hasSize(1)
                    awaitComplete()
                }
            }
        }

    private companion object {
        const val TAPPED_HANDLE = 2L
        const val PARENT_HANDLE = 77L
        const val ROOT_HANDLE = 99L
    }
}
