package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAudioBackgroundPlayEnabledUseCaseTest {
    private lateinit var underTest: MonitorAudioBackgroundPlayEnabledUseCase

    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun initialise() {
        underTest =
            MonitorAudioBackgroundPlayEnabledUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that null values return a default of false`() = runTest {
        mediaPlayerRepository.stub {
            on { monitorAudioBackgroundPlayEnabled() }.thenReturn(flowOf(null))
        }

        underTest().test {
            Truth.assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }
}