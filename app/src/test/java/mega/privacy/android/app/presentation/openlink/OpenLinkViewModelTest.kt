package mega.privacy.android.app.presentation.openlink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.link.DecodeLinkUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenLinkViewModelTest {

    private lateinit var underTest: OpenLinkViewModel

    private val localLogoutAppUseCase: LocalLogoutAppUseCase = mock()
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase = mock()
    private val logoutUseCase: LogoutUseCase = mock()
    private val querySignupLinkUseCase: QuerySignupLinkUseCase = mock()
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val decodeLinkUseCase: DecodeLinkUseCase = mock()
    private val applicationScope = CoroutineScope(extension.testDispatcher)

    @BeforeEach
    fun setup() {
        underTest = OpenLinkViewModel(
            localLogoutAppUseCase = localLogoutAppUseCase,
            clearEphemeralCredentialsUseCase = clearEphemeralCredentialsUseCase,
            logoutUseCase = logoutUseCase,
            querySignupLinkUseCase = querySignupLinkUseCase,
            getAccountCredentials = getAccountCredentialsUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            decodeLinkUseCase = decodeLinkUseCase,
            applicationScope = applicationScope
        )
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            UserCredentials(
                email = randomString(),
                session = randomString(),
                firstName = randomString(),
                lastName = randomString(),
                myHandle = randomString()
            ),
            true
        ),
        Arguments.of(
            null,
            false
        ),
    )

    @MethodSource("provideParameters")
    @ParameterizedTest(name = "when getAccountCredentialsUseCase returns {0} then isLoggedIn is {1}")
    fun `test that the isLoggedIn state is successfully updated when getAccountCredentialsUseCase returns the expected value`(
        userCredentials: UserCredentials?,
        expected: Boolean,
    ) =
        runTest {
            // Given
            val url = randomLink()
            val decodedLink = "decodedLink"

            whenever(decodeLinkUseCase(url)).thenReturn(decodedLink)
            whenever(getAccountCredentialsUseCase()).thenReturn(userCredentials)
            whenever(getRootNodeUseCase()).thenReturn(mock())
            // When
            underTest.decodeUrl(url)

            // Then
            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.isLoggedIn).isEqualTo(expected)
                assertThat(state.urlRedirectionEvent).isEqualTo(true)
            }
        }

    @Test
    fun `test that the decoded url state is updated when successfully decoding the url`() =
        runTest {
            // Given
            val url = randomLink()
            val decodedLink = "decodedLink"
            val userCredentials = UserCredentials(
                email = randomString(),
                session = randomString(),
                firstName = randomString(),
                lastName = randomString(),
                myHandle = randomString()
            )

            whenever(decodeLinkUseCase(url)).thenReturn(decodedLink)
            whenever(getAccountCredentialsUseCase()).thenReturn(userCredentials)
            whenever(getRootNodeUseCase()).thenReturn(mock())
            // When
            underTest.decodeUrl(url)

            // Then
            underTest.uiState.test {
                assertThat(expectMostRecentItem().decodedUrl).isEqualTo(decodedLink)
            }
        }

    @Test
    fun `test that account invitation email state is updated when successfully create account invitation email with a link`() =
        runTest {
            // Given
            val link = randomLink()
            val email = randomString()

            whenever(querySignupLinkUseCase(link)).thenReturn(email)

            // When
            underTest.getAccountInvitationEmail(link)

            // Then
            underTest.uiState.test {
                assertThat(expectMostRecentItem().accountInvitationEmail).isEqualTo(email)
            }
        }

    @Test
    fun `test that account invitation email state is not updated when it fails to create account invitation email with a link`() =
        runTest {
            // Given
            val link = randomLink()

            whenever(querySignupLinkUseCase(link)).thenThrow(RuntimeException())

            // When
            underTest.getAccountInvitationEmail(link)

            // Then
            underTest.uiState.test {
                assertThat(expectMostRecentItem().accountInvitationEmail).isEqualTo(null)
            }
        }

    @Test
    fun `test that the logout use case is executed when the user is logging out`() = runTest {
        // When
        underTest.logout()

        // Then
        verify(logoutUseCase).invoke()
    }

    @Test
    fun `test that the logout action is not confirmed when the user logged out without a confirmation link`() =
        runTest {
            // When
            underTest.logout()

            // Then
            verifyNoInteractions(clearEphemeralCredentialsUseCase, localLogoutAppUseCase)
        }

    @Test
    fun `test that the user logs out locally with cleared ephemeral credentials when the user successfully logged out from the given confirmation link`() =
        runTest {
            // Given
            MegaApplication.urlConfirmationLink = randomLink()

            // When
            underTest.logout()

            // Then
            verify(clearEphemeralCredentialsUseCase).invoke()
            verify(localLogoutAppUseCase).invoke()
        }

    @Test
    fun `test that the confirmation link is reset and the logged out state is updated when successfully confirming the logout action`() =
        runTest {
            // Given
            MegaApplication.urlConfirmationLink = randomLink()

            // When
            underTest.logout()

            // Then
            underTest.uiState.test {
                assertThat(expectMostRecentItem().logoutCompletedEvent).isTrue()
                assertThat(MegaApplication.urlConfirmationLink).isEqualTo(null)
            }
        }

    @Test
    fun `test that the confirmation link is not reset and the logged out state is not updated when failing to confirm the logout action`() =
        runTest {
            // Given
            val confirmationLink = randomLink()
            MegaApplication.urlConfirmationLink = confirmationLink

            whenever(clearEphemeralCredentialsUseCase()).thenThrow(RuntimeException())
            whenever(localLogoutAppUseCase()).thenThrow(RuntimeException())

            // When
            underTest.logout()

            // Then
            underTest.uiState.test {
                assertThat(expectMostRecentItem().logoutCompletedEvent).isFalse()
                assertThat(MegaApplication.urlConfirmationLink).isEqualTo(confirmationLink)
            }
        }

    @Test
    fun `test that urlRedirectionEvent is reset when consumed`() = runTest {
        underTest.onUrlRedirectionEventConsumed()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().urlRedirectionEvent).isFalse()
        }
    }

    @Test
    fun `test that logoutCompletedEvent is reset when consumed`() = runTest {
        underTest.onLogoutCompletedEventConsumed()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().logoutCompletedEvent).isFalse()
        }
    }

    @AfterEach
    fun resetMocks() {
        MegaApplication.urlConfirmationLink = null
        reset(
            localLogoutAppUseCase,
            clearEphemeralCredentialsUseCase,
            logoutUseCase,
            querySignupLinkUseCase,
            getAccountCredentialsUseCase,
            getRootNodeUseCase,
            decodeLinkUseCase
        )
    }

    private fun randomLink(totalPath: Int = 3) = buildString {
        append("https://")
        append(randomString())
        (0 until totalPath).forEach {
            append("/${randomString()}")
        }
    }

    private fun randomString(length: Int = 3): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }
}
