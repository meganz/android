package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpServerIsRunningUseCaseTest {
    private lateinit var underTest: HttpServerIsRunningUseCase
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val megaApiFolderHttpServerIsRunningUseCase =
        mock<MegaApiFolderHttpServerIsRunningUseCase>()
    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()

    private val expectedPort = 123

    @BeforeAll
    fun setUp() {
        underTest = HttpServerIsRunningUseCase(
            megaApiFolderHttpServerIsRunningUseCase = megaApiFolderHttpServerIsRunningUseCase,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            megaApiHttpServerIsRunningUseCase,
            megaApiFolderHttpServerIsRunningUseCase,
            hasCredentialsUseCase
        )
    }

    @ParameterizedTest(name = "when isFolderLink is {0}, and hasCredentialsUseCase return {1}")
    @MethodSource("provideParameters")
    fun `test that result is returned as expected`(
        isFolderLink: Boolean,
        hasCredentials: Boolean,
        isRunningUseCase: suspend () -> Int,
    ) = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(hasCredentials)
        whenever(isRunningUseCase()).thenReturn(expectedPort)
        val actual = underTest(isFolderLink)
        assertThat(actual).isEqualTo(expectedPort)
    }

    private fun provideParameters() = listOf(
        Arguments.of(true, false, suspend { megaApiFolderHttpServerIsRunningUseCase() }),
        Arguments.of(false, false, suspend { megaApiHttpServerIsRunningUseCase() }),
        Arguments.of(true, true, suspend { megaApiHttpServerIsRunningUseCase() }),
        Arguments.of(false, true, suspend { megaApiHttpServerIsRunningUseCase() })
    )
}