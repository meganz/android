package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetSRTSubtitleFileListUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSRTSubtitleFileListUseCaseTest {
    private lateinit var underTest: GetSRTSubtitleFileListUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val testFileSuffix = ".srt"

    @BeforeAll
    fun setUp() {
        underTest = GetSRTSubtitleFileListUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that result is empty`() =
        runTest {
            whenever(mediaPlayerRepository.getSubtitleFileInfoList(testFileSuffix)).thenReturn(
                emptyList()
            )
            assertThat(underTest()).isEmpty()
        }

    @Test
    fun `test that the result is not empty`() =
        runTest {
            val testSubTitleFileInfo = mock<SubtitleFileInfo>()
            whenever(mediaPlayerRepository.getSubtitleFileInfoList(testFileSuffix)).thenReturn(
                listOf(testSubTitleFileInfo)
            )
            assertThat(underTest()).isNotEmpty()
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest()
            verify(mediaPlayerRepository).getSubtitleFileInfoList(testFileSuffix)
        }
}