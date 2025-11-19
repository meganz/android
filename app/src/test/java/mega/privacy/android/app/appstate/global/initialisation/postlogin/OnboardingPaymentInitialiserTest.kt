package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.account.ShouldShowUpgradeAccountUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OnboardingPaymentInitialiserTest {
    private lateinit var underTest: OnboardingPaymentInitialiser

    private val shouldShowUpgradeAccountUseCase = mock<ShouldShowUpgradeAccountUseCase>()
    private val getCurrentUserEmail = mock<GetCurrentUserEmail>()
    private val getLastRegisteredEmailUseCase = mock<GetLastRegisteredEmailUseCase>()
    private val navigationEventQueue = mock<NavigationEventQueue>()
    private val monitorFetchNodesFinishUseCase = mock<MonitorFetchNodesFinishUseCase> {
        on { invoke() } doReturn flowOf(true)
    }

    @BeforeAll
    fun setUp() {
        underTest = OnboardingPaymentInitialiser(
            shouldShowUpgradeAccountUseCase = shouldShowUpgradeAccountUseCase,
            getCurrentUserEmail = getCurrentUserEmail,
            getLastRegisteredEmailUseCase = getLastRegisteredEmailUseCase,
            navigationEventQueue = navigationEventQueue,
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            shouldShowUpgradeAccountUseCase,
            getCurrentUserEmail,
            getLastRegisteredEmailUseCase,
            navigationEventQueue
        )
    }

    @Test
    fun `test that no event is emitted when isFastLogin is true`() = runTest {
        underTest("session", true)

        verifyNoInteractions(shouldShowUpgradeAccountUseCase)
        verifyNoInteractions(getCurrentUserEmail)
        verifyNoInteractions(getLastRegisteredEmailUseCase)
        verifyNoInteractions(navigationEventQueue)
    }

    @Test
    fun `test that no event is emitted when shouldShowUpgradeAccountUseCase returns false`() =
        runTest {
            shouldShowUpgradeAccountUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

            underTest("session", false)

            verify(shouldShowUpgradeAccountUseCase).invoke()
            verifyNoInteractions(getCurrentUserEmail)
            verifyNoInteractions(getLastRegisteredEmailUseCase)
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that nav event is emitted with isNewAccount true when emails match`() = runTest {
        val email = "test@example.com"

        shouldShowUpgradeAccountUseCase.stub {
            onBlocking { invoke() }.thenReturn(true)
        }
        getCurrentUserEmail.stub {
            onBlocking { invoke() }.thenReturn(email)
        }
        getLastRegisteredEmailUseCase.stub {
            onBlocking { invoke() }.thenReturn(email)
        }

        underTest("session", false)

        verify(shouldShowUpgradeAccountUseCase).invoke()
        verify(getCurrentUserEmail).invoke()
        verify(getLastRegisteredEmailUseCase).invoke()
        verify(navigationEventQueue).emit(
            UpgradeAccountNavKey(
                isNewAccount = true,
                isUpgrade = false
            )
        )
    }

    @Test
    fun `test that nav event is emitted with isNewAccount false when emails do not match`() =
        runTest {
            val currentEmail = "current@example.com"
            val lastRegisteredEmail = "registered@example.com"

            shouldShowUpgradeAccountUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }
            getCurrentUserEmail.stub {
                onBlocking { invoke() }.thenReturn(currentEmail)
            }
            getLastRegisteredEmailUseCase.stub {
                onBlocking { invoke() }.thenReturn(lastRegisteredEmail)
            }

            underTest("session", false)

            verify(shouldShowUpgradeAccountUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verify(getLastRegisteredEmailUseCase).invoke()
            verify(navigationEventQueue).emit(
                UpgradeAccountNavKey(
                    isNewAccount = false,
                    isUpgrade = false
                )
            )
        }

    @Test
    fun `test that no event is emitted when shouldShowUpgradeAccountUseCase throws exception`() =
        runTest {
            shouldShowUpgradeAccountUseCase.stub {
                onBlocking { invoke() }.thenThrow(RuntimeException("Test error"))
            }

            underTest("session", false)

            verify(shouldShowUpgradeAccountUseCase).invoke()
            verifyNoInteractions(getCurrentUserEmail)
            verifyNoInteractions(getLastRegisteredEmailUseCase)
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that no event is emitted when getCurrentUserEmail throws exception`() = runTest {
        shouldShowUpgradeAccountUseCase.stub {
            onBlocking { invoke() }.thenReturn(true)
        }
        getCurrentUserEmail.stub {
            onBlocking { invoke() }.thenThrow(RuntimeException("Test error"))
        }

        underTest("session", false)

        verify(shouldShowUpgradeAccountUseCase).invoke()
        verify(getCurrentUserEmail).invoke()
        verifyNoInteractions(getLastRegisteredEmailUseCase)
        verifyNoInteractions(navigationEventQueue)
    }

    @Test
    fun `test that no event is emitted when getLastRegisteredEmailUseCase throws exception`() =
        runTest {
            shouldShowUpgradeAccountUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }
            getCurrentUserEmail.stub {
                onBlocking { invoke() }.thenReturn("test@example.com")
            }
            getLastRegisteredEmailUseCase.stub {
                onBlocking { invoke() }.thenThrow(RuntimeException("Test error"))
            }

            underTest("session", false)

            verify(shouldShowUpgradeAccountUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verify(getLastRegisteredEmailUseCase).invoke()
            verifyNoInteractions(navigationEventQueue)
        }
}

