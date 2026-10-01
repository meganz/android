package mega.privacy.android.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStopUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaApiHttpServerStopUseCaseTest {
    private lateinit var underTest: MegaApiHttpServerStopUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MegaApiHttpServerStopUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest()
            verify(mediaPlayerRepository).megaApiHttpServerStop()
        }
}