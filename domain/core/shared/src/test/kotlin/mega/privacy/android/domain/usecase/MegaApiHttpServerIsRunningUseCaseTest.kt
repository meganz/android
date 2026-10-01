package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaApiHttpServerIsRunningUseCaseTest {
    private lateinit var underTest: MegaApiHttpServerIsRunningUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest =
            MegaApiHttpServerIsRunningUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that the result is returned`() =
        runTest {
            val result = 0
            whenever(mediaPlayerRepository.megaApiHttpServerIsRunning()).thenReturn(result)
            assertThat(underTest()).isEqualTo(result)
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest()
            verify(mediaPlayerRepository).megaApiHttpServerIsRunning()
        }
}