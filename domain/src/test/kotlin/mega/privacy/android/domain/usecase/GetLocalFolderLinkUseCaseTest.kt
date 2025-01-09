package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.mediaplayer.GetLocalFolderLinkUseCase
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
class GetLocalFolderLinkUseCaseTest {
    private lateinit var underTest: GetLocalFolderLinkUseCase
    private val getLocalFolderLinkFromMegaApiFolderUseCase =
        mock<GetLocalFolderLinkFromMegaApiFolderUseCase>()
    private val getLocalFolderLinkFromMegaApiUseCase = mock<GetLocalFolderLinkFromMegaApiUseCase>()
    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()

    private val expectedHandle = 12345L
    private val expectedFolderLink = "expectedFolderLink"

    @BeforeAll
    fun setUp() {
        underTest = GetLocalFolderLinkUseCase(
            getLocalFolderLinkFromMegaApiFolderUseCase = getLocalFolderLinkFromMegaApiFolderUseCase,
            getLocalFolderLinkFromMegaApiUseCase = getLocalFolderLinkFromMegaApiUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            getLocalFolderLinkFromMegaApiFolderUseCase,
            getLocalFolderLinkFromMegaApiUseCase,
            hasCredentialsUseCase
        )
    }

    @ParameterizedTest(name = "when hasCredentialsUseCase return {1}")
    @MethodSource("provideParameters")
    fun `test that result is returned as expected`(
        hasCredentials: Boolean,
        getLocalFolderLink: suspend () -> String?,
    ) = runTest {
        whenever(hasCredentialsUseCase()).thenReturn(hasCredentials)
        whenever(getLocalFolderLink()).thenReturn(expectedFolderLink)
        val actual = underTest(expectedHandle)
        assertThat(actual).isEqualTo(expectedFolderLink)
    }

    private fun provideParameters() = listOf(
        Arguments.of(true, suspend { getLocalFolderLinkFromMegaApiUseCase(expectedHandle) }),
        Arguments.of(false, suspend { getLocalFolderLinkFromMegaApiFolderUseCase(expectedHandle) })
    )
}