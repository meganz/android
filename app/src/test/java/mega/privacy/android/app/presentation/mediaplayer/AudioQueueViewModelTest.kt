package mega.privacy.android.app.presentation.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.mapper.MediaQueueItemUiEntityMapper
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueViewModel
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import kotlin.time.Duration.Companion.minutes

class AudioQueueViewModelTest {
    private lateinit var underTest: AudioQueueViewModel

    private val mediaQueueItemUiEntityMapper = mock<MediaQueueItemUiEntityMapper>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val isParticipatingInChatCallUseCase = mock<IsParticipatingInChatCallUseCase>()

    private val testName = "Audio"
    private val testType = MediaQueueItemType.Playing
    private val testDuration = 10.minutes

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
        underTest = AudioQueueViewModel(
            mediaQueueItemUiEntityMapper = mediaQueueItemUiEntityMapper,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            isParticipatingInChatCallUseCase = isParticipatingInChatCallUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            mediaQueueItemUiEntityMapper,
            durationInSecondsTextMapper,
            isParticipatingInChatCallUseCase
        )
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.items).isEmpty()
            assertThat(initial.isPaused).isFalse()
            assertThat(initial.currentPlayingPosition).isEqualTo("00:00")
            assertThat(initial.indexOfCurrentPlayingItem).isEqualTo(-1)
            assertThat(initial.selectedItemHandles).isEmpty()
            assertThat(initial.isSearchMode).isFalse()
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

        initUnderTest()
        underTest.initMediaQueueItemList(list)
        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.items).isNotEmpty()
            assertThat(actual.indexOfCurrentPlayingItem).isEqualTo(1)
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
    fun `test that state is updated correctly when updatePlaybackState is called`() = runTest {
        initUnderTest()
        underTest.uiState.test {
            assertThat(awaitItem().isPaused).isFalse()
            underTest.updatePlaybackState(true)
            assertThat(awaitItem().isPaused).isTrue()
            underTest.updatePlaybackState(false)
            assertThat(awaitItem().isPaused).isFalse()
        }
    }

    @Test
    fun `test that state is updated correctly when updateCurrentPlayingPosition is called`() =
        runTest {
            val durationString = "10:00"
            whenever(durationInSecondsTextMapper(any())).thenReturn(durationString)
            initUnderTest()
            underTest.updateCurrentPlayingPosition(0)
            underTest.uiState.test {
                assertThat(awaitItem().currentPlayingPosition).isEqualTo(durationString)
            }
        }

    @Test
    fun `test that state is updated correctly when updateMediaQueueAfterReorder is called`() =
        runTest {
            val list = (1..3).map {
                initMediaQueueItemMapperResult(it.toLong())
                getPlaylistItem(it.toLong())
            }

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
    fun `test that state is updated correctly when updateMediaQueueAfterMediaItemTransition is called`() =
        runTest {
            val list = (1..3).map {
                val handle = it.toLong()
                initMediaQueueItemMapperResultWithCopy(
                    handle = handle,
                    itemType = when (it) {
                        1 -> MediaQueueItemType.Previous
                        2 -> MediaQueueItemType.Playing
                        else -> MediaQueueItemType.Next
                    },
                    parameterType = when (it) {
                        1 -> MediaQueueItemType.Playing
                        else -> MediaQueueItemType.Next
                    }
                )
                getPlaylistItem(handle)
            }

            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.updateMediaQueueAfterMediaItemTransition(0)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.indexOfCurrentPlayingItem).isEqualTo(1)
                assertThat(actual.items.size).isEqualTo(3)
                assertThat(actual.items[0].type.ordinal).isEqualTo(MediaQueueItemType.Previous.ordinal)
                assertThat(actual.items[1].type.ordinal).isEqualTo(MediaQueueItemType.Playing.ordinal)
                assertThat(actual.items[2].type.ordinal).isEqualTo(MediaQueueItemType.Next.ordinal)
            }
        }

    private fun initMediaQueueItemMapperResultWithCopy(
        handle: Long,
        itemType: MediaQueueItemType,
        parameterType: MediaQueueItemType,
    ) {
        val mediaQueueItem = getMockedMediaQueueItem(NodeId(handle), itemType)
        initMediaQueueItemMapperResultByItem(handle, mediaQueueItem)
        whenever(mediaQueueItem.copy(type = parameterType)).thenReturn(mediaQueueItem)
    }

    private fun initMediaQueueItemMapperResultByItem(
        handle: Long,
        mediaQueueItemUiEntity: MediaQueueItemUiEntity,
    ) {
        whenever(
            mediaQueueItemUiEntityMapper(
                icon = 0,
                thumbnailFile = null,
                id = NodeId(handle),
                name = testName,
                type = testType,
                duration = testDuration
            )
        ).thenReturn(mediaQueueItemUiEntity)
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
            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.onItemClicked(0, item)
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
            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.onItemClicked(0, item1)
            underTest.uiState.test {
                assertThat(awaitItem().selectedItemHandles).isNotEmpty()
                underTest.clearAllSelectedItems()
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
            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.onItemClicked(0, item)
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
    fun `test that state is updated correctly after updateSearchMode is called`() =
        runTest {
            initUnderTest()
            underTest.uiState.test {
                assertThat(awaitItem().isSearchMode).isFalse()
                underTest.updateSearchMode(true)
                assertThat(awaitItem().isSearchMode).isTrue()
                underTest.updateSearchMode(false)
                assertThat(awaitItem().isSearchMode).isFalse()
            }
        }

    @Test
    fun `test that state is updated correctly after searchQueryUpdate is called`() =
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
            initUnderTest()

            underTest.initMediaQueueItemList(list)
            underTest.uiState.test {
                awaitItem().let {
                    assertThat(it.items.size).isEqualTo(3)
                }
                underTest.searchQueryUpdate("Test")
                awaitItem().let {
                    assertThat(it.items.size).isEqualTo(1)
                    assertThat(it.items[0].nodeName).isEqualTo("Test")
                }
                underTest.updateSearchMode(false)
                assertThat(awaitItem().items.size).isEqualTo(3)
            }
        }
}