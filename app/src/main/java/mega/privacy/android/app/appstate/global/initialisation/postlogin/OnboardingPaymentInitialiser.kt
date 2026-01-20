package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.account.ShouldShowUpgradeAccountUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import timber.log.Timber
import javax.inject.Inject


class OnboardingPaymentInitialiser @Inject constructor(
    shouldShowUpgradeAccountUseCase: ShouldShowUpgradeAccountUseCase,
    getCurrentUserEmail: GetCurrentUserEmail,
    getLastRegisteredEmailUseCase: GetLastRegisteredEmailUseCase,
    navigationEventQueue: NavigationEventQueue,
) : PostLoginInitialiser(
    action = { _, isFastLogin ->
        if (!isFastLogin) {
            runCatching {
                if (shouldShowUpgradeAccountUseCase()) {
                    navigationEventQueue.emit(
                        UpgradeAccountNavKey(
                            isNewAccount = getCurrentUserEmail() == getLastRegisteredEmailUseCase(),
                            isUpgrade = false
                        ),
                        priority = NavPriority.Priority(10)
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "Error checking onboarding permissions")
            }
        }
    }
)