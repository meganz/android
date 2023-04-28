package test.mega.privacy.android.app.presentation.mediaplayer

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.mediaplayer.SelectSubtitleFileViewModel
import mega.privacy.android.app.mediaplayer.mapper.SubtitleFileInfoItemMapper
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.mediaplayer.model.SubtitleLoadState
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.usecase.mediaplayer.GetSRTSubtitleFileListUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
internal class SelectSubtitleFileViewModelTest {
    private lateinit var underTest: SelectSubtitleFileViewModel

    private val getSRTSubtitleFileListUseCase = mock<GetSRTSubtitleFileListUseCase>()
    private val subtitleFileInfoItemMapper = mock<SubtitleFileInfoItemMapper>()
    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = SelectSubtitleFileViewModel(
            getSRTSubtitleFileListUseCase = getSRTSubtitleFileListUseCase,
            subtitleFileInfoItemMapper = subtitleFileInfoItemMapper,
            sendStatisticsMediaPlayerUseCase = mock(),
            savedStateHandle = SavedStateHandle()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test getSubtitleFileInfoList return empty list`() = runTest {
        whenever(getSRTSubtitleFileListUseCase()).thenReturn(
            emptyList()
        )
        underTest.getSubtitleFileInfoList()
        scheduler.advanceUntilIdle()
        assertThat(
            underTest.state is SubtitleLoadState.Empty
        ).isTrue()
    }

    @Test
    fun `test getSubtitleFileInfoList return correctly`() = runTest {
        val expectedSubtitleFileInfo1 = SubtitleFileInfo(
            id = 1234567,
            name = "test1.srt",
            url = "test1@url",
            parentName = "Folder1"
        )
        val expectedSubtitleFileInfo2 = SubtitleFileInfo(
            id = 1234568,
            name = "test2.srt",
            url = "test2@url",
            parentName = "Folder2"
        )
        val expectedSubtitleFileInfo3 = SubtitleFileInfo(
            id = 1234569,
            name = "test3.srt",
            url = "test3@url",
            parentName = "Folder3"
        )
        val expectedSubtitleFileInfoList = listOf(
            expectedSubtitleFileInfo1,
            expectedSubtitleFileInfo2,
            expectedSubtitleFileInfo3
        )

        val expectedSubtitleFileInfoItemList = listOf(
            SubtitleFileInfoItem(
                false, expectedSubtitleFileInfo1
            ),
            SubtitleFileInfoItem(
                false, expectedSubtitleFileInfo2
            ),
            SubtitleFileInfoItem(
                false, expectedSubtitleFileInfo3
            )
        )

        whenever(getSRTSubtitleFileListUseCase()).thenReturn(
            expectedSubtitleFileInfoList
        )
        whenever(subtitleFileInfoItemMapper(false, expectedSubtitleFileInfo1)).thenReturn(
            SubtitleFileInfoItem(false, expectedSubtitleFileInfo1)
        )
        whenever(subtitleFileInfoItemMapper(false, expectedSubtitleFileInfo2)).thenReturn(
            SubtitleFileInfoItem(false, expectedSubtitleFileInfo2)
        )
        whenever(subtitleFileInfoItemMapper(false, expectedSubtitleFileInfo3)).thenReturn(
            SubtitleFileInfoItem(false, expectedSubtitleFileInfo3)
        )

        underTest.getSubtitleFileInfoList()
        scheduler.advanceUntilIdle()
        assertThat(
            (underTest.state as SubtitleLoadState.Success).items
        ).isEqualTo(
            expectedSubtitleFileInfoItemList
        )
    }
}