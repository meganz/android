package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.app.appstate.content.navigation.view.PermissionScreensNavKey
import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import mega.privacy.android.domain.usecase.notifications.ShouldShowNotificationReminderUseCase
import mega.privacy.android.domain.usecase.permisison.CheckOnboardingPermissionsUseCase
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import timber.log.Timber
import javax.inject.Inject


class OnboardingPermissionInitialiser @Inject constructor(
    private val checkOnboardingPermissionsUseCase: CheckOnboardingPermissionsUseCase,
    private val shouldShowNotificationReminderUseCase: ShouldShowNotificationReminderUseCase,
    private val isFirstLaunchUseCase: IsFirstLaunchUseCase,
    private val navigationEventQueue: NavigationEventQueue,
) : PostLoginInitialiser(
    action = { _, _ ->
        runCatching {
            val isFirstLaunch = isFirstLaunchUseCase()

            if (isFirstLaunch) {
                checkOnboardingPermissionsUseCase().takeIf { it.requestPermissionsOnFirstLaunch }
                    ?.let { result ->
                        navigationEventQueue.emit(
                            PermissionScreensNavKey(
                                onlyShowNotificationPermission = result.onlyShowNotificationPermission
                            ),
                            priority = NavPriority.Priority(9)
                        )
                    }
            } else {
                val shouldShowNotificationReminder = shouldShowNotificationReminderUseCase()
                if (shouldShowNotificationReminder) {
                    navigationEventQueue.emit(
                        PermissionScreensNavKey(
                            onlyShowNotificationPermission = true
                        ),
                        priority = NavPriority.Priority(9)
                    )
                }
            }
        }.onFailure { e ->
            Timber.e(e, "Error checking onboarding permissions")
        }
    }
)