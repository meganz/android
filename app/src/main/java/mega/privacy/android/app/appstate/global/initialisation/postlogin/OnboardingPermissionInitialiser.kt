package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.app.appstate.content.navigation.view.PermissionScreensNavKey
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.permisison.CheckOnboardingPermissionsUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import timber.log.Timber
import javax.inject.Inject


class OnboardingPermissionInitialiser @Inject constructor(
    checkOnboardingPermissionsUseCase: CheckOnboardingPermissionsUseCase,
    navigationEventQueue: NavigationEventQueue,
) : PostLoginInitialiser(
    action = { _ ->
        runCatching {
            Timber.d("Checking onboarding permissions")
            checkOnboardingPermissionsUseCase().takeIf { it.requestPermissionsOnFirstLaunch }
                ?.let { result ->
                    navigationEventQueue.emit(
                        PermissionScreensNavKey(
                            onlyShowNotificationPermission = result.onlyShowNotificationPermission
                        )
                    )
                }
        }.onFailure { e ->
            Timber.e(e, "Error checking onboarding permissions")
        }
    }
)