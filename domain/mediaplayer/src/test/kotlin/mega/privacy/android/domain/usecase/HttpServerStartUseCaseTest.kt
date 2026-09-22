package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.mediaplayer.HttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpServerStartUseCaseTest {
    private lateinit var underTest: HttpServerStartUseCase
    private val megaApiFolderHttpServerStartUseCase = mock<MegaApiFolderHttpServerStartUseCase>()
    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = HttpServerStartUseCase(
            megaApiFolderHttpServerStartUseCase = megaApiFolderHttpServerStartUseCase,
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            megaApiFolderHttpServerStartUseCase,
            megaApiHttpServerStartUseCase,
            hasCredentialsUseCase
        )
    }

    @ParameterizedTest(name = "when isFolderLink is {0}, and hasCredentialsUseCase return {1}")
    @MethodSource("provideParameters")
    fun `test that expected use case is invoked`(
        isFolderLink: Boolean,
        hasCredentials: Boolean,
        startUseCase: suspend () -> Boolean,
    ) = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(hasCredentials)
        whenever(startUseCase()).thenReturn(true)
        val actual = underTest(isFolderLink)
        assertThat(actual).isTrue()
    }

    private fun provideParameters() = listOf(
        Arguments.of(true, false, suspend { megaApiFolderHttpServerStartUseCase() }),
        Arguments.of(false, false, suspend { megaApiHttpServerStartUseCase() }),
        Arguments.of(true, true, suspend { megaApiHttpServerStartUseCase() }),
        Arguments.of(false, true, suspend { megaApiHttpServerStartUseCase() })
    )
}