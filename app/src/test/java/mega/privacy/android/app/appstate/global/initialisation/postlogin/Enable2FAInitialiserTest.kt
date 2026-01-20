package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.Enable2FANavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Enable2FAInitialiserTest {
    private lateinit var underTest: Enable2FAInitialiser

    private val requireTwoFactorAuthenticationUseCase = mock<RequireTwoFactorAuthenticationUseCase>()
    private val getCurrentUserEmail = mock<GetCurrentUserEmail>()
    private val getLastRegisteredEmailUseCase = mock<GetLastRegisteredEmailUseCase>()
    private val navigationEventQueue = mock<NavigationEventQueue>()
    private val monitorUpdateUserDataUseCase = mock<MonitorUpdateUserDataUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = Enable2FAInitialiser(
            requireTwoFactorAuthenticationUseCase = requireTwoFactorAuthenticationUseCase,
            getCurrentUserEmail = getCurrentUserEmail,
            getLastRegisteredEmailUseCase = getLastRegisteredEmailUseCase,
            navigationEventQueue = navigationEventQueue,
            monitorUpdateUserDataUseCase = monitorUpdateUserDataUseCase,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            requireTwoFactorAuthenticationUseCase,
            getCurrentUserEmail,
            getLastRegisteredEmailUseCase,
            navigationEventQueue,
            monitorUpdateUserDataUseCase
        )
        // Default: monitorUpdateUserDataUseCase emits Unit
        whenever(monitorUpdateUserDataUseCase()).thenReturn(flowOf(Unit))
    }

    @Test
    fun `test that nav event is emitted when fast login and requireTwoFactorAuthenticationUseCase returns true`() =
        runTest {
            requireTwoFactorAuthenticationUseCase.stub {
                onBlocking { invoke(newAccount = false, firstLogin = false) }.thenReturn(true)
            }

            underTest("session", true)

            verify(monitorUpdateUserDataUseCase).invoke()
            verify(requireTwoFactorAuthenticationUseCase).invoke(
                newAccount = false,
                firstLogin = false
            )
            verify(navigationEventQueue).emit(Enable2FANavKey)
        }

    @Test
    fun `test that no nav event is emitted when fast login and requireTwoFactorAuthenticationUseCase returns false`() =
        runTest {
            requireTwoFactorAuthenticationUseCase.stub {
                onBlocking { invoke(newAccount = false, firstLogin = false) }.thenReturn(false)
            }

            underTest("session", true)

            verify(monitorUpdateUserDataUseCase).invoke()
            verify(requireTwoFactorAuthenticationUseCase).invoke(
                newAccount = false,
                firstLogin = false
            )
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that no nav event is emitted when fast login and requireTwoFactorAuthenticationUseCase throws exception`() =
        runTest {
            requireTwoFactorAuthenticationUseCase.stub {
                onBlocking { invoke(newAccount = false, firstLogin = false) }
                    .thenThrow(RuntimeException("Test error"))
            }

            underTest("session", true)

            verify(monitorUpdateUserDataUseCase).invoke()
            verify(requireTwoFactorAuthenticationUseCase).invoke(
                newAccount = false,
                firstLogin = false
            )
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that requireTwoFactorAuthenticationUseCase is called with newAccount true when emails match for non-fast login`() =
        runTest {
            val email = "test@example.com"

            getCurrentUserEmail.stub {
                onBlocking { invoke() }.thenReturn(email)
            }
            getLastRegisteredEmailUseCase.stub {
                onBlocking { invoke() }.thenReturn(email)
            }
            requireTwoFactorAuthenticationUseCase.stub {
                onBlocking { invoke(newAccount = true, firstLogin = true) }.thenReturn(false)
            }

            underTest("session", false)

            verify(monitorUpdateUserDataUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verify(getLastRegisteredEmailUseCase).invoke()
            verify(requireTwoFactorAuthenticationUseCase).invoke(
                newAccount = true,
                firstLogin = true
            )
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that requireTwoFactorAuthenticationUseCase is called with newAccount false when emails do not match for non-fast login`() =
        runTest {
            val currentEmail = "current@example.com"
            val lastRegisteredEmail = "registered@example.com"

            getCurrentUserEmail.stub {
                onBlocking { invoke() }.thenReturn(currentEmail)
            }
            getLastRegisteredEmailUseCase.stub {
                onBlocking { invoke() }.thenReturn(lastRegisteredEmail)
            }
            requireTwoFactorAuthenticationUseCase.stub {
                onBlocking { invoke(newAccount = false, firstLogin = true) }.thenReturn(false)
            }

            underTest("session", false)

            verify(monitorUpdateUserDataUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verify(getLastRegisteredEmailUseCase).invoke()
            verify(requireTwoFactorAuthenticationUseCase).invoke(
                newAccount = false,
                firstLogin = true
            )
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that no nav event is emitted when non-fast login and requireTwoFactorAuthenticationUseCase throws exception`() =
        runTest {
            val email = "test@example.com"

            getCurrentUserEmail.stub {
                onBlocking { invoke() }.thenReturn(email)
            }
            getLastRegisteredEmailUseCase.stub {
                onBlocking { invoke() }.thenReturn(email)
            }
            requireTwoFactorAuthenticationUseCase.stub {
                onBlocking { invoke(newAccount = true, firstLogin = true) }
                    .thenThrow(RuntimeException("Test error"))
            }

            underTest("session", false)

            verify(monitorUpdateUserDataUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verify(getLastRegisteredEmailUseCase).invoke()
            verify(requireTwoFactorAuthenticationUseCase).invoke(
                newAccount = true,
                firstLogin = true
            )
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that no nav event is emitted when non-fast login and getCurrentUserEmail throws exception`() =
        runTest {
            getCurrentUserEmail.stub {
                onBlocking { invoke() }.thenThrow(RuntimeException("Test error"))
            }

            underTest("session", false)

            verify(monitorUpdateUserDataUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verifyNoInteractions(getLastRegisteredEmailUseCase)
            verifyNoInteractions(requireTwoFactorAuthenticationUseCase)
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that no nav event is emitted when non-fast login and getLastRegisteredEmailUseCase throws exception`() =
        runTest {
            getCurrentUserEmail.stub {
                onBlocking { invoke() }.thenReturn("test@example.com")
            }
            getLastRegisteredEmailUseCase.stub {
                onBlocking { invoke() }.thenThrow(RuntimeException("Test error"))
            }

            underTest("session", false)

            verify(monitorUpdateUserDataUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verify(getLastRegisteredEmailUseCase).invoke()
            verifyNoInteractions(requireTwoFactorAuthenticationUseCase)
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that exception is handled gracefully when monitorUpdateUserDataUseCase throws exception`() =
        runTest {
            whenever(monitorUpdateUserDataUseCase()).thenReturn(
                flow { throw RuntimeException("Test error") }
            )

            // Should not throw exception
            underTest("session", false)

            verify(monitorUpdateUserDataUseCase).invoke()
            verifyNoInteractions(getCurrentUserEmail)
            verifyNoInteractions(getLastRegisteredEmailUseCase)
            verifyNoInteractions(requireTwoFactorAuthenticationUseCase)
            verifyNoInteractions(navigationEventQueue)
        }
}
