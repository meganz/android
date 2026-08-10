package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.domain.usecase.account.ShouldShowUpgradeAccountUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.feature_flags.FirebaseABTestFeatures
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import timber.log.Timber
import javax.inject.Inject


class OnboardingPaymentInitialiser @Inject constructor(
    shouldShowUpgradeAccountUseCase: ShouldShowUpgradeAccountUseCase,
    getCurrentUserEmail: GetCurrentUserEmail,
    getLastRegisteredEmailUseCase: GetLastRegisteredEmailUseCase,
    getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    navigationEventQueue: NavigationEventQueue,
) : PostLoginInitialiserAction(
    action = { _, isFastLogin ->
        if (!isFastLogin) {
            runCatching {
                if (shouldShowUpgradeAccountUseCase()) {
                    val isNewAccount = getCurrentUserEmail() == getLastRegisteredEmailUseCase()
                    // A/B test only applies to new accounts; regular login keeps current flow
                    val showUpgradeScreen = !isNewAccount ||
                            getFeatureFlagValueUseCase(FirebaseABTestFeatures.ShowPaywallAfterSignup)
                    if (showUpgradeScreen) {
                        navigationEventQueue.emit(
                            UpgradeAccountNavKey(
                                isNewAccount = isNewAccount,
                                isUpgrade = false
                            ),
                            priority = NavPriority.Priority(10)
                        )
                    }
                }
            }.onFailure { e ->
                Timber.e(e, "Error checking onboarding permissions")
            }
        }
    }
)