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
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
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

        initCurrentPlayingPosition()

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

    private fun initCurrentPlayingPosition() {
        whenever(mediaPlayerGateway.getCurrentPlayingPosition()).thenReturn(testPlayingPosition)
        whenever(durationInSecondsTextMapper(any())).thenReturn(durationString)
    }

    @Test
    fun `test that state is updated correctly when updateMediaQueueAfterReorder is called`() =
        runTest {
            val list = (1..3).map {
                initMediaQueueItemMapperResult(it.toLong())
                getPlaylistItem(it.toLong())
            }

            initCurrentPlayingPosition()

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

    @Test
    fun `test that action mode is updated as expected`() = runTest {
        initUnderTest()
        underTest.uiState.test {
            assertThat(awaitItem().isActionMode).isFalse()
            underTest.updateActionMode(true)
            assertThat(awaitItem().isActionMode).isTrue()
            underTest.updateActionMode(false)
            assertThat(awaitItem().isActionMode).isFalse()
        }
    }

    @Test
    fun `test that search state is updated as expected`() = runTest {
        initUnderTest()
        underTest.uiState.test {
            assertThat(awaitItem().searchState).isEqualTo(SearchWidgetState.COLLAPSED)
            underTest.searchWidgetStateUpdate()
            assertThat(awaitItem().searchState).isEqualTo(SearchWidgetState.EXPANDED)
            underTest.searchWidgetStateUpdate()
            assertThat(awaitItem().searchState).isEqualTo(SearchWidgetState.COLLAPSED)
        }
    }

    @Test
    fun `test that states of the search feature are updated as expected`() = runTest {
        initUnderTest()
        underTest.searchWidgetStateUpdate()
        underTest.searchQuery("")
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.searchState).isEqualTo(SearchWidgetState.EXPANDED)
            assertThat(initial.query).isNotNull()
            underTest.closeSearch()
            val actual = awaitItem()
            assertThat(actual.searchState).isEqualTo(SearchWidgetState.COLLAPSED)
            assertThat(actual.query).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that state is updated correctly after the item is clicked`() =
        runTest {
            val item = getMockedMediaQueueItem(NodeId(1L))
            val testItem = getMockedMediaQueueItem(NodeId(1L), testIsSelected = true)
            val list = (1..3).map {
                if (it == 1) {
                    initMediaQueueItemMapperResult(it.toLong(), item)
                } else {
                    initMediaQueueItemMapperResult(it.toLong())
                }
                getPlaylistItem(it.toLong())
            }
            whenever(item.copy(isSelected = true)).thenReturn(testItem)
            initCurrentPlayingPosition()
            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.updateItemInSelectionState(0, item)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.items.size).isEqualTo(3)
                assertThat(actual.selectedItemHandles).isNotEmpty()
                assertThat(actual.selectedItemHandles.size).isEqualTo(1)
                assertThat(actual.selectedItemHandles[0]).isEqualTo(1)
                assertThat(actual.items[0].isSelected).isTrue()
                assertThat(actual.items[1].isSelected).isFalse()
                assertThat(actual.items[2].isSelected).isFalse()
            }
        }

    private fun initMediaQueueItemMapperResult(
        handle: Long,
        item: MediaQueueItemUiEntity,
        itemType: MediaQueueItemType = testType,
    ) {
        whenever(
            mediaQueueItemUiEntityMapper(
                icon = 0,
                thumbnailFile = null,
                id = NodeId(handle),
                name = testName,
                type = itemType,
                duration = testDuration
            )
        ).thenReturn(item)
    }

    @Test
    fun `test that state is updated correctly after clearing all selected items`() =
        runTest {
            val item1 = getMockedMediaQueueItem(NodeId(1L))
            val item2 = getMockedMediaQueueItem(NodeId(2L))
            val item3 = getMockedMediaQueueItem(NodeId(3L))
            initMediaQueueItemMapperResult(1, item1)
            initMediaQueueItemMapperResult(2, item2)
            initMediaQueueItemMapperResult(3, item3)
            val testSelectedItem = getMockedMediaQueueItem(NodeId(1L), testIsSelected = true)
            val testItem = getMockedMediaQueueItem(NodeId(1L), testIsSelected = false)
            val list = (1..3).map {
                getPlaylistItem(it.toLong())
            }
            whenever(item1.copy(isSelected = true)).thenReturn(testSelectedItem)
            whenever(item2.copy(isSelected = false)).thenReturn(testItem)
            whenever(item3.copy(isSelected = false)).thenReturn(testItem)
            whenever(testSelectedItem.copy(isSelected = false)).thenReturn(testItem)
            initCurrentPlayingPosition()
            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.updateItemInSelectionState(0, item1)
            underTest.uiState.test {
                assertThat(awaitItem().selectedItemHandles).isNotEmpty()
                underTest.clearAllSelected()
                assertThat(awaitItem().selectedItemHandles).isEmpty()
            }
        }

    @Test
    fun `test that state is updated correctly after removing selected items`() =
        runTest {
            val item = getMockedMediaQueueItem(NodeId(1L))
            val testItem = getMockedMediaQueueItem(NodeId(1L), testIsSelected = true)
            val list = (1..3).map {
                if (it == 1) {
                    initMediaQueueItemMapperResult(it.toLong(), item)
                } else {
                    initMediaQueueItemMapperResult(it.toLong())
                }
                getPlaylistItem(it.toLong())
            }
            whenever(item.copy(isSelected = true)).thenReturn(testItem)
            initCurrentPlayingPosition()
            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.updateItemInSelectionState(0, item)
            underTest.uiState.test {
                awaitItem().let {
                    assertThat(it.items.size).isEqualTo(3)
                    assertThat(it.selectedItemHandles).isNotEmpty()
                }
                underTest.removeSelectedItems()
                awaitItem().let {
                    assertThat(it.items.size).isEqualTo(2)
                    assertThat(it.selectedItemHandles).isEmpty()
                }
            }
        }

    @Test
    fun `test that state is updated correctly after searchQuery is called`() =
        runTest {
            val item = getMockedMediaQueueItem(NodeId(1L), name = "Test")
            val list = (1..3).map {
                if (it == 1) {
                    initMediaQueueItemMapperResult(it.toLong(), item)
                } else {
                    initMediaQueueItemMapperResult(it.toLong())
                }
                getPlaylistItem(it.toLong())
            }
            initCurrentPlayingPosition()
            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.uiState.test {
                awaitItem().let {
                    assertThat(it.items.size).isEqualTo(3)
                }
                underTest.searchQuery("Test")
                assertThat(awaitItem().query).isEqualTo("Test")
                awaitItem().let {
                    assertThat(it.items.size).isEqualTo(1)
                    assertThat(it.items[0].nodeName).isEqualTo("Test")
                }
                underTest.closeSearch()
                assertThat(awaitItem().query).isNull()
                assertThat(awaitItem().items.size).isEqualTo(3)
            }
        }
}