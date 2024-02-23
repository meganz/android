package mega.privacy.android.app.presentation.openlink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenLinkViewModelTest {

    private lateinit var underTest: OpenLinkViewModel

    private val localLogoutAppUseCase: LocalLogoutAppUseCase = mock()
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase = mock()
    private val logoutUseCase: LogoutUseCase = mock()
    private val querySignupLinkUseCase: QuerySignupLinkUseCase = mock()
    private val applicationScope = CoroutineScope(extension.testDispatcher)

    @BeforeEach
    fun setup() {
        underTest = OpenLinkViewModel(
            localLogoutAppUseCase = localLogoutAppUseCase,
            clearEphemeralCredentialsUseCase = clearEphemeralCredentialsUseCase,
            logoutUseCase = logoutUseCase,
            querySignupLinkUseCase = querySignupLinkUseCase,
            applicationScope = applicationScope
        )
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
            underTest.state.test {
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
            underTest.state.test {
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
            underTest.state.test {
                assertThat(expectMostRecentItem().isLoggedOut).isTrue()
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
            underTest.state.test {
                assertThat(expectMostRecentItem().isLoggedOut).isFalse()
                assertThat(MegaApplication.urlConfirmationLink).isEqualTo(confirmationLink)
            }
        }

    @AfterEach
    fun resetMocks() {
        MegaApplication.urlConfirmationLink = null
        reset(
            localLogoutAppUseCase,
            clearEphemeralCredentialsUseCase,
            logoutUseCase,
            querySignupLinkUseCase
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
