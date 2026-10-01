package mega.privacy.android.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStopUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpServerStopUseCaseTest {
    private lateinit var underTest: HttpServerStopUseCase
    private val megaApiFolderHttpServerStopUseCase = mock<MegaApiFolderHttpServerStopUseCase>()
    private val megaApiHttpServerStopUseCase = mock<MegaApiHttpServerStopUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = HttpServerStopUseCase(
            megaApiFolderHttpServerStopUseCase = megaApiFolderHttpServerStopUseCase,
            megaApiHttpServerStopUseCase = megaApiHttpServerStopUseCase
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            megaApiFolderHttpServerStopUseCase,
            megaApiHttpServerStopUseCase,
        )
    }

    @Test
    fun `test that the use cases are invoked as expected`() = runTest {
        underTest()
        verify(megaApiFolderHttpServerStopUseCase).invoke()
        verify(megaApiHttpServerStopUseCase).invoke()
    }
}