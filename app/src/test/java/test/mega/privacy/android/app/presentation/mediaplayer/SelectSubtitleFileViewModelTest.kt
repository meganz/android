package test.mega.privacy.android.app.presentation.mediaplayer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.mediaplayer.SelectSubtitleFileViewModel
import mega.privacy.android.app.mediaplayer.mapper.SubtitleFileInfoItemMapper
import mega.privacy.android.app.mediaplayer.model.SubtitleLoadState
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetSRTSubtitleFileListUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import test.mega.privacy.android.app.TimberJUnit5Extension

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TimberJUnit5Extension::class)
internal class SelectSubtitleFileViewModelTest {
    private lateinit var underTest: SelectSubtitleFileViewModel

    private val getSRTSubtitleFileListUseCase = mock<GetSRTSubtitleFileListUseCase>()
    private val subtitleFileInfoItemMapper = mock<SubtitleFileInfoItemMapper>()

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        reset(getSRTSubtitleFileListUseCase, subtitleFileInfoItemMapper)
        wheneverBlocking { getSRTSubtitleFileListUseCase() }.thenReturn(emptyList())
        underTest = SelectSubtitleFileViewModel(
            getSRTSubtitleFileListUseCase = getSRTSubtitleFileListUseCase,
            subtitleFileInfoItemMapper = subtitleFileInfoItemMapper,
            sendStatisticsMediaPlayerUseCase = mock(),
            savedStateHandle = SavedStateHandle()
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test getSubtitleFileInfoList return empty list`() = runTest {
        underTest.getSubtitleFileInfoList()
        underTest.state.test {
            assertThat(awaitItem() is SubtitleLoadState.Empty).isTrue()
        }
    }

    @Test
    fun `test getSubtitleFileInfoList return correctly`() = runTest {
        val expectedSubtitleFileInfoList: List<SubtitleFileInfo> = listOf(mock(), mock(), mock())

        getSRTSubtitleFileListUseCase.stub {
            onBlocking { invoke() }.thenReturn(expectedSubtitleFileInfoList)
        }

        whenever(subtitleFileInfoItemMapper(anyOrNull(), anyOrNull())).thenReturn(mock())

        underTest.state.test {
            underTest.getSubtitleFileInfoList()
            assertThat(awaitItem() is SubtitleLoadState.Empty).isTrue()
            val actual = awaitItem()
            assertThat(actual is SubtitleLoadState.Success).isTrue()
            assertThat((actual as SubtitleLoadState.Success).items.size).isEqualTo(3)
        }
    }
}