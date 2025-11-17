package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.content.navigation.view.PermissionScreensNavKey
import mega.privacy.android.domain.entity.permission.OnboardingPermissionsCheckResult
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import mega.privacy.android.domain.usecase.notifications.ShouldShowNotificationReminderUseCase
import mega.privacy.android.domain.usecase.permisison.CheckOnboardingPermissionsUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class OnboardingPermissionInitialiserTest {
    private lateinit var underTest: OnboardingPermissionInitialiser

    private val checkOnboardingPermissionsUseCase = mock<CheckOnboardingPermissionsUseCase>()
    private val shouldShowNotificationReminderUseCase =
        mock<ShouldShowNotificationReminderUseCase>()
    private val isFirstLaunchUseCase = mock<IsFirstLaunchUseCase>()
    private val navigationEventQueue = mock<NavigationEventQueue>()

    @BeforeEach
    fun setUp() {
        underTest = OnboardingPermissionInitialiser(
            checkOnboardingPermissionsUseCase = checkOnboardingPermissionsUseCase,
            shouldShowNotificationReminderUseCase = shouldShowNotificationReminderUseCase,
            isFirstLaunchUseCase = isFirstLaunchUseCase,
            navigationEventQueue = navigationEventQueue,
        )
    }

    @Test
    fun `test that nav event is emitted if requestPermissionsOnFirstLaunch is true on first launch`() =
        runTest {
            val expectedPermissionsResult = OnboardingPermissionsCheckResult(
                requestPermissionsOnFirstLaunch = true,
                onlyShowNotificationPermission = false
            )

            isFirstLaunchUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }
            checkOnboardingPermissionsUseCase.stub {
                onBlocking { invoke() }.thenReturn(expectedPermissionsResult)
            }

            underTest("session", true)
            verify(navigationEventQueue).emit(PermissionScreensNavKey(false))
        }

    @Test
    fun `test that no event is emitted if requestPermissionsOnFirstLaunch is false on first launch`() =
        runTest {
            val expectedPermissionsResult = OnboardingPermissionsCheckResult(
                requestPermissionsOnFirstLaunch = false,
                onlyShowNotificationPermission = false
            )

            isFirstLaunchUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }
            checkOnboardingPermissionsUseCase.stub {
                onBlocking { invoke() }.thenReturn(expectedPermissionsResult)
            }

            underTest("session", true)
            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that nav event is emitted if shouldShowNotificationReminder is true on subsequent launch`() =
        runTest {
            isFirstLaunchUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }
            shouldShowNotificationReminderUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }

            underTest("session", true)
            verify(navigationEventQueue).emit(PermissionScreensNavKey(onlyShowNotificationPermission = true))
        }

    @Test
    fun `test that no event is emitted if shouldShowNotificationReminder is false on subsequent launch`() =
        runTest {
            isFirstLaunchUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }
            shouldShowNotificationReminderUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

            underTest("session", true)
            verifyNoInteractions(navigationEventQueue)
        }

}