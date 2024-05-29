package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaApiHttpServerStartUseCaseTest {
    private lateinit var underTest: MegaApiHttpServerStartUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MegaApiHttpServerStartUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that the result returns true`() =
        runTest {
            whenever(mediaPlayerRepository.megaApiHttpServerStart()).thenReturn(true)
            assertThat(underTest()).isTrue()
        }

    @Test
    fun `test that the result returns false`() =
        runTest {
            whenever(mediaPlayerRepository.megaApiHttpServerStart()).thenReturn(false)
            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest()
            verify(mediaPlayerRepository).megaApiHttpServerStart()
        }
}