package mega.privacy.android.app.presentation.videoplayer

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.mapper.SubtitleFileInfoItemMapper
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerSubtitleUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetSRTSubtitleFileListUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
internal class VideoPlayerSelectSubtitleViewModelTest {

    private lateinit var underTest: VideoPlayerSelectSubtitleViewModel

    private val getSRTSubtitleFileListUseCase = mock<GetSRTSubtitleFileListUseCase>()
    private val subtitleFileInfoItemMapper = mock<SubtitleFileInfoItemMapper>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()

    @BeforeEach
    fun setUp() {
        monitorHiddenNodesEnabledUseCase.stub {
            on { invoke() } doReturn flow { emit(false); awaitCancellation() }
        }
        monitorShowHiddenItemsUseCase.stub {
            on { invoke() } doReturn flow { emit(false); awaitCancellation() }
        }
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(emptyList())
        underTest = VideoPlayerSelectSubtitleViewModel(
            getSRTSubtitleFileListUseCase = getSRTSubtitleFileListUseCase,
            subtitleFileInfoItemMapper = subtitleFileInfoItemMapper,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getSRTSubtitleFileListUseCase,
            subtitleFileInfoItemMapper,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        assertThat(underTest.uiState.value.isLoading).isTrue()
        assertThat(underTest.uiState.value.items).isEmpty()
    }

    @Test
    fun `test that state emits empty items when subtitle list is empty`() = runTest {
        underTest.uiState.test {
            val state = awaitNonLoadingState()
            assertThat(state.items).isEmpty()
        }
    }

    @Test
    fun `test that state emits items when subtitle list is not empty`() = runTest {
        val subtitleInfoList = listOf(
            stubSubtitleFileInfo(1L, "subtitle1.srt"),
            stubSubtitleFileInfo(2L, "subtitle2.srt"),
        )
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(subtitleInfoList)
        subtitleInfoList.forEach { info ->
            whenever(subtitleFileInfoItemMapper(false, info)).thenReturn(mock())
        }

        underTest.getSubtitleFileInfoList()
        underTest.uiState.test {
            val state = awaitNonLoadingState()
            assertThat(state.items).hasSize(2)
        }
    }

    @Test
    fun `test that hiddenNodesEnabled is true when monitorHiddenNodesEnabledUseCase emits true`() =
        runTest {
            monitorHiddenNodesEnabledUseCase.stub {
                on { invoke() } doReturn flow { emit(true); awaitCancellation() }
            }
            underTest.uiState.test {
                val state = awaitNonLoadingState()
                assertThat(state.hiddenNodesEnabled).isTrue()
            }
        }

    @Test
    fun `test that hiddenNodesEnabled is false when monitorHiddenNodesEnabledUseCase emits false`() =
        runTest {
            underTest.uiState.test {
                val state = awaitNonLoadingState()
                assertThat(state.hiddenNodesEnabled).isFalse()
            }
        }

    @Test
    fun `test that sensitive items are filtered out when hiddenNodesEnabled is true and showHiddenItems is false`() =
        runTest {
            monitorHiddenNodesEnabledUseCase.stub {
                on { invoke() } doReturn flow { emit(true); awaitCancellation() }
            }
            val sensitiveItem =
                stubSubtitleFileInfo(1L, "sensitive.srt", isMarkedSensitive = true)
            val normalItem = stubSubtitleFileInfo(2L, "normal.srt")
            wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(
                listOf(sensitiveItem, normalItem)
            )
            whenever(subtitleFileInfoItemMapper(false, normalItem)).thenReturn(mock())

            underTest.getSubtitleFileInfoList()
            underTest.uiState.test {
                val state = awaitNonLoadingState()
                assertThat(state.items).hasSize(1)
            }
        }

    @Test
    fun `test that sensitive items are not filtered when hiddenNodesEnabled is false`() = runTest {
        val sensitiveItem = stubSubtitleFileInfo(1L, "sensitive.srt", isMarkedSensitive = true)
        val normalItem = stubSubtitleFileInfo(2L, "normal.srt")
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(
            listOf(sensitiveItem, normalItem)
        )
        whenever(subtitleFileInfoItemMapper(false, sensitiveItem)).thenReturn(mock())
        whenever(subtitleFileInfoItemMapper(false, normalItem)).thenReturn(mock())

        underTest.getSubtitleFileInfoList()
        underTest.uiState.test {
            val state = awaitNonLoadingState()
            assertThat(state.items).hasSize(2)
        }
    }

    @Test
    fun `test that sensitive items are not filtered when showHiddenItems is true`() = runTest {
        monitorHiddenNodesEnabledUseCase.stub {
            on { invoke() } doReturn flow { emit(true); awaitCancellation() }
        }
        monitorShowHiddenItemsUseCase.stub {
            on { invoke() } doReturn flow { emit(true); awaitCancellation() }
        }
        val sensitiveItem = stubSubtitleFileInfo(1L, "sensitive.srt", isMarkedSensitive = true)
        val normalItem = stubSubtitleFileInfo(2L, "normal.srt")
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(
            listOf(sensitiveItem, normalItem)
        )
        whenever(subtitleFileInfoItemMapper(false, sensitiveItem)).thenReturn(mock())
        whenever(subtitleFileInfoItemMapper(false, normalItem)).thenReturn(mock())

        underTest.getSubtitleFileInfoList()
        underTest.uiState.test {
            val state = awaitNonLoadingState()
            assertThat(state.items).hasSize(2)
        }
    }

    @Test
    fun `test that itemClickedUpdate selects item when it is not already selected`() = runTest {
        val item = stubSubtitleFileInfo(1L, "subtitle.srt")
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(listOf(item))
        whenever(subtitleFileInfoItemMapper(false, item)).thenReturn(
            SubtitleFileInfoItem(selected = false, subtitleFileInfo = item)
        )
        whenever(subtitleFileInfoItemMapper(true, item)).thenReturn(
            SubtitleFileInfoItem(selected = true, subtitleFileInfo = item)
        )

        underTest.getSubtitleFileInfoList()
        underTest.uiState.test {
            awaitNonLoadingState()
            underTest.itemClickedUpdate(item)
            val state = awaitItem()
            assertThat(state.selectedSubtitleFileInfo).isEqualTo(item)
        }
    }

    @Test
    fun `test that itemClickedUpdate deselects item when it is already selected`() = runTest {
        val item = stubSubtitleFileInfo(1L, "subtitle.srt")
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(listOf(item))
        whenever(subtitleFileInfoItemMapper(false, item)).thenReturn(
            SubtitleFileInfoItem(selected = false, subtitleFileInfo = item)
        )
        whenever(subtitleFileInfoItemMapper(true, item)).thenReturn(
            SubtitleFileInfoItem(selected = true, subtitleFileInfo = item)
        )

        underTest.getSubtitleFileInfoList()
        underTest.uiState.test {
            awaitNonLoadingState()
            underTest.itemClickedUpdate(item)
            awaitItem()
            underTest.itemClickedUpdate(item)
            val state = awaitItem()
            assertThat(state.selectedSubtitleFileInfo).isNull()
        }
    }

    @Test
    fun `test that clearSelectedItem clears the selection`() = runTest {
        val item = stubSubtitleFileInfo(1L, "subtitle.srt")
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(listOf(item))
        whenever(subtitleFileInfoItemMapper(false, item)).thenReturn(
            SubtitleFileInfoItem(selected = false, subtitleFileInfo = item)
        )
        whenever(subtitleFileInfoItemMapper(true, item)).thenReturn(
            SubtitleFileInfoItem(selected = true, subtitleFileInfo = item)
        )

        underTest.getSubtitleFileInfoList()
        underTest.uiState.test {
            awaitNonLoadingState()
            underTest.itemClickedUpdate(item)
            awaitItem()
            underTest.clearSelectedItem()
            val state = awaitItem()
            assertThat(state.selectedSubtitleFileInfo).isNull()
        }
    }

    @Test
    fun `test that searchQuery filters items by name`() = runTest {
        val matchingItem = stubSubtitleFileInfo(1L, "english.srt")
        val nonMatchingItem = stubSubtitleFileInfo(2L, "french.srt")
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(
            listOf(matchingItem, nonMatchingItem)
        )
        whenever(subtitleFileInfoItemMapper(false, matchingItem)).thenReturn(mock())
        whenever(subtitleFileInfoItemMapper(false, nonMatchingItem)).thenReturn(mock())

        underTest.getSubtitleFileInfoList()
        underTest.uiState.test {
            awaitNonLoadingState()
            underTest.searchQuery("english")
            val state = awaitItem()
            assertThat(state.items).hasSize(1)
        }
    }

    private fun stubSubtitleFileInfo(
        id: Long,
        name: String,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ) = SubtitleFileInfo(
        id = id,
        name = name,
        url = null,
        parentName = null,
        isMarkedSensitive = isMarkedSensitive,
        isSensitiveInherited = isSensitiveInherited,
    )

    private suspend fun ReceiveTurbine<VideoPlayerSubtitleUiState>.awaitNonLoadingState(): VideoPlayerSubtitleUiState {
        var item = awaitItem()
        while (item.isLoading) {
            item = awaitItem()
        }
        return item
    }
}
