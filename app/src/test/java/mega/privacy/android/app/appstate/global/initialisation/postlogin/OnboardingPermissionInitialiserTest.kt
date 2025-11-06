package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.content.navigation.view.PermissionScreensNavKey
import mega.privacy.android.domain.entity.permission.OnboardingPermissionsCheckResult
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
    private val navigationEventQueue = mock<NavigationEventQueue>()

    @BeforeEach
    fun setUp() {
        underTest = OnboardingPermissionInitialiser(
            checkOnboardingPermissionsUseCase = checkOnboardingPermissionsUseCase,
            navigationEventQueue = navigationEventQueue,
        )
    }

    @Test
    fun `test that nav event is emitted if requestPermissionsOnFirstLaunch is true`() =
        runTest {
            val expectedPermissionsResult = OnboardingPermissionsCheckResult(
                requestPermissionsOnFirstLaunch = true,
                onlyShowNotificationPermission = false
            )


            checkOnboardingPermissionsUseCase.stub {
                onBlocking { invoke() }.thenReturn(expectedPermissionsResult)
            }

            underTest("session")
            verify(navigationEventQueue).emit(PermissionScreensNavKey(false))
        }

    @Test
    fun `test that no event is emitted if requestPermissionsOnFirstLaunch is false`() = runTest {
        val expectedPermissionsResult = OnboardingPermissionsCheckResult(
            requestPermissionsOnFirstLaunch = false,
            onlyShowNotificationPermission = false
        )


        checkOnboardingPermissionsUseCase.stub {
            onBlocking { invoke() }.thenReturn(expectedPermissionsResult)
        }

        underTest("session")
        verifyNoInteractions(navigationEventQueue)
    }

}