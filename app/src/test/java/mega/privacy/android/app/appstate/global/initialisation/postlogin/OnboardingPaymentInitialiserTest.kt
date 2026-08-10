package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.account.ShouldShowUpgradeAccountUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.feature_flags.FirebaseABTestFeatures
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = OnboardingPaymentInitialiser(
            shouldShowUpgradeAccountUseCase = shouldShowUpgradeAccountUseCase,
            getCurrentUserEmail = getCurrentUserEmail,
            getLastRegisteredEmailUseCase = getLastRegisteredEmailUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            navigationEventQueue = navigationEventQueue,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            shouldShowUpgradeAccountUseCase,
            getCurrentUserEmail,
            getLastRegisteredEmailUseCase,
            getFeatureFlagValueUseCase,
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
                on { invoke() }.thenReturn(false)
            }

            underTest("session", false)

            verify(shouldShowUpgradeAccountUseCase).invoke()
            verifyNoInteractions(getCurrentUserEmail)
            verifyNoInteractions(getLastRegisteredEmailUseCase)
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that nav event is emitted with isNewAccount true when emails match and ab test flag is enabled`() =
        runTest {
            val email = "test@example.com"

            shouldShowUpgradeAccountUseCase.stub {
                on { invoke() }.thenReturn(true)
            }
            getCurrentUserEmail.stub {
                on { invoke() }.thenReturn(email)
            }
            getLastRegisteredEmailUseCase.stub {
                on { invoke() }.thenReturn(email)
            }
            getFeatureFlagValueUseCase.stub {
                on { invoke(FirebaseABTestFeatures.ShowPaywallAfterSignup) }.thenReturn(true)
            }

            underTest("session", false)

            verify(shouldShowUpgradeAccountUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verify(getLastRegisteredEmailUseCase).invoke()
            verify(getFeatureFlagValueUseCase).invoke(FirebaseABTestFeatures.ShowPaywallAfterSignup)
            verify(navigationEventQueue).emit(
                UpgradeAccountNavKey(
                    isNewAccount = true,
                    isUpgrade = false
                ),
                priority = NavPriority.Priority(10)
            )
        }

    @Test
    fun `test that no event is emitted when emails match and ab test flag is disabled`() =
        runTest {
            val email = "test@example.com"

            shouldShowUpgradeAccountUseCase.stub {
                on { invoke() }.thenReturn(true)
            }
            getCurrentUserEmail.stub {
                on { invoke() }.thenReturn(email)
            }
            getLastRegisteredEmailUseCase.stub {
                on { invoke() }.thenReturn(email)
            }
            getFeatureFlagValueUseCase.stub {
                on { invoke(FirebaseABTestFeatures.ShowPaywallAfterSignup) }.thenReturn(false)
            }

            underTest("session", false)

            verify(getFeatureFlagValueUseCase).invoke(FirebaseABTestFeatures.ShowPaywallAfterSignup)
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that nav event is emitted with isNewAccount false when emails do not match`() =
        runTest {
            val currentEmail = "current@example.com"
            val lastRegisteredEmail = "registered@example.com"

            shouldShowUpgradeAccountUseCase.stub {
                on { invoke() }.thenReturn(true)
            }
            getCurrentUserEmail.stub {
                on { invoke() }.thenReturn(currentEmail)
            }
            getLastRegisteredEmailUseCase.stub {
                on { invoke() }.thenReturn(lastRegisteredEmail)
            }

            underTest("session", false)

            verify(shouldShowUpgradeAccountUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verify(getLastRegisteredEmailUseCase).invoke()
            verifyNoInteractions(getFeatureFlagValueUseCase)
            verify(navigationEventQueue).emit(
                UpgradeAccountNavKey(
                    isNewAccount = false,
                    isUpgrade = false
                ),
                priority = NavPriority.Priority(10)
            )
        }

    @Test
    fun `test that no event is emitted when getFeatureFlagValueUseCase throws exception`() =
        runTest {
            val email = "test@example.com"

            shouldShowUpgradeAccountUseCase.stub {
                on { invoke() }.thenReturn(true)
            }
            getCurrentUserEmail.stub {
                on { invoke() }.thenReturn(email)
            }
            getLastRegisteredEmailUseCase.stub {
                on { invoke() }.thenReturn(email)
            }
            getFeatureFlagValueUseCase.stub {
                on { invoke(FirebaseABTestFeatures.ShowPaywallAfterSignup) }
                    .thenThrow(RuntimeException("Test error"))
            }

            underTest("session", false)

            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that no event is emitted when shouldShowUpgradeAccountUseCase throws exception`() =
        runTest {
            shouldShowUpgradeAccountUseCase.stub {
                on { invoke() }.thenThrow(RuntimeException("Test error"))
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
            on { invoke() }.thenReturn(true)
        }
        getCurrentUserEmail.stub {
            on { invoke() }.thenThrow(RuntimeException("Test error"))
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
                on { invoke() }.thenReturn(true)
            }
            getCurrentUserEmail.stub {
                on { invoke() }.thenReturn("test@example.com")
            }
            getLastRegisteredEmailUseCase.stub {
                on { invoke() }.thenThrow(RuntimeException("Test error"))
            }

            underTest("session", false)

            verify(shouldShowUpgradeAccountUseCase).invoke()
            verify(getCurrentUserEmail).invoke()
            verify(getLastRegisteredEmailUseCase).invoke()
            verifyNoInteractions(navigationEventQueue)
        }
}

