package test.mega.privacy.android.app.presentation.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.mapper.MediaQueueItemUiEntityMapper
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.app.mediaplayer.queue.video.VideoQueueViewModel
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

class VideoQueueViewModelTest {
    private lateinit var underTest: VideoQueueViewModel

    private val mediaQueueItemUiEntityMapper = mock<MediaQueueItemUiEntityMapper>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val isParticipatingInChatCallUseCase = mock<IsParticipatingInChatCallUseCase>()
    private val mediaPlayerGateway = mock<MediaPlayerGateway>()

    private val testName = "Video"
    private val testType = MediaQueueItemType.Playing
    private val testDuration = 10.minutes
    private val testPlayingPosition = testDuration.toLong(DurationUnit.SECONDS)
    private val durationString = "10:00"

    private fun getPlaylistItem(handle: Long, playlistType: Int = 2) = mock<PlaylistItem> {
        on { nodeHandle }.thenReturn(handle)
        on { thumbnail }.thenReturn(null)
        on { nodeName }.thenReturn(testName)
        on { type }.thenReturn(playlistType)
        on { duration }.thenReturn(testDuration)
    }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { isParticipatingInChatCallUseCase() }.thenReturn(false)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideoQueueViewModel(
            mediaQueueItemUiEntityMapper = mediaQueueItemUiEntityMapper,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            isParticipatingInChatCallUseCase = isParticipatingInChatCallUseCase,
            mediaPlayerGateway = mediaPlayerGateway
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            mediaQueueItemUiEntityMapper,
            durationInSecondsTextMapper,
            isParticipatingInChatCallUseCase,
            mediaPlayerGateway
        )
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.items).isEmpty()
            assertThat(initial.currentPlayingPosition).isEqualTo("00:00")
            assertThat(initial.indexOfCurrentPlayingItem).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that state is updated correctly after media queue items initialised`() = runTest {
        val list = (1..3).map {
            val handle = it.toLong()
            initMediaQueueItemMapperResult(
                handle = handle,
                itemType = when (it) {
                    1 -> MediaQueueItemType.Previous
                    2 -> MediaQueueItemType.Playing
                    else -> MediaQueueItemType.Next
                }
            )
            getPlaylistItem(handle, it)
        }

        whenever(mediaPlayerGateway.getCurrentPlayingPosition()).thenReturn(testPlayingPosition)
        whenever(durationInSecondsTextMapper(any())).thenReturn(durationString)

        initUnderTest()
        underTest.initMediaQueueItemList(list)
        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.items).isNotEmpty()
            assertThat(actual.indexOfCurrentPlayingItem).isEqualTo(1)
            assertThat(actual.currentPlayingPosition).isEqualTo(durationString)
        }
    }

    private fun initMediaQueueItemMapperResult(
        handle: Long,
        itemType: MediaQueueItemType = testType,
    ) {
        val nodeId = mock<NodeId> { on { longValue }.thenReturn(handle) }
        val mediaQueueItem = getMockedMediaQueueItem(nodeId, itemType)
        whenever(
            mediaQueueItemUiEntityMapper(
                icon = 0,
                thumbnailFile = null,
                id = NodeId(handle),
                name = testName,
                type = itemType,
                duration = testDuration
            )
        ).thenReturn(mediaQueueItem)
    }

    private fun getMockedMediaQueueItem(
        nodeId: NodeId,
        itemType: MediaQueueItemType = testType,
        name: String = testName,
        testIsSelected: Boolean = false,
    ) = mock<MediaQueueItemUiEntity> {
        on { id }.thenReturn(nodeId)
        on { type }.thenReturn(itemType)
        on { nodeName }.thenReturn(name)
        on { isSelected }.thenReturn(testIsSelected)
    }

    @Test
    fun `test that state is updated correctly when updateMediaQueueAfterReorder is called`() =
        runTest {
            val list = (1..3).map {
                initMediaQueueItemMapperResult(it.toLong())
                getPlaylistItem(it.toLong())
            }

            whenever(mediaPlayerGateway.getCurrentPlayingPosition()).thenReturn(testPlayingPosition)
            whenever(durationInSecondsTextMapper(any())).thenReturn(durationString)

            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.updateMediaQueueAfterReorder(1, 2)
            underTest.uiState.test {
                val actual = awaitItem().items
                assertThat(actual.size).isEqualTo(3)
                assertThat(actual[1].id).isEqualTo(NodeId(3L))
                assertThat(actual[2].id).isEqualTo(NodeId(2L))
            }
        }

    @Test
    fun `test that playerSeekTo function is invoked expected`() {
        val testIndex = 10
        underTest.seekTo(testIndex)
        verify(mediaPlayerGateway).playerSeekTo(testIndex)
    }
}