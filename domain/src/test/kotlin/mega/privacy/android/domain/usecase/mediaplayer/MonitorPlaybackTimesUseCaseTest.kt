package mega.privacy.android.domain.usecase.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.MonitorPlaybackTimesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorPlaybackTimesUseCaseTest {
    private lateinit var underTest: MonitorPlaybackTimesUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorPlaybackTimesUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that result is null`() =
        runTest {
            whenever(mediaPlayerRepository.monitorPlaybackTimes()).thenReturn(flowOf(null))
            underTest().test {
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that result is returned`() =
        runTest {
            val testMediaId = 1L
            val testDuration = 100L
            val testCurrentPosition = 1000L
            val playbackInfo = mock<PlaybackInformation> {
                on { mediaId }.thenReturn(testMediaId)
                on { totalDuration }.thenReturn(testDuration)
                on { currentPosition }.thenReturn(testCurrentPosition)
            }
            val testMap: Map<Long, PlaybackInformation> = mapOf(testMediaId to playbackInfo)
            whenever(mediaPlayerRepository.monitorPlaybackTimes()).thenReturn(flowOf(testMap))
            underTest().test {
                val actual = awaitItem()
                assertThat(actual?.values?.size).isEqualTo(1)
                assertThat(actual?.get(testMediaId)?.mediaId).isEqualTo(testMediaId)
                assertThat(actual?.get(testMediaId)?.totalDuration).isEqualTo(testDuration)
                assertThat(actual?.get(testMediaId)?.currentPosition).isEqualTo(testCurrentPosition)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest()
            verify(mediaPlayerRepository).monitorPlaybackTimes()
        }
}