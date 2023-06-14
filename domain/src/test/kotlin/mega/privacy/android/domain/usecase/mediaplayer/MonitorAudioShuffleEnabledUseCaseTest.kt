package mega.privacy.android.domain.usecase.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAudioShuffleEnabledUseCaseTest {
    private lateinit var underTest: MonitorAudioShuffleEnabledUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun initialise() {
        underTest = MonitorAudioShuffleEnabledUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that null values return a default of false`() = runTest {
        mediaPlayerRepository.stub {
            on { monitorAudioShuffleEnabled() }.thenReturn(flowOf(null))
        }

        underTest().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }
}