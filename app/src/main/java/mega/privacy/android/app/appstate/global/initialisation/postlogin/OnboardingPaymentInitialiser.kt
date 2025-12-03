package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.account.ShouldShowUpgradeAccountUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import timber.log.Timber
import javax.inject.Inject


class OnboardingPaymentInitialiser @Inject constructor(
    shouldShowUpgradeAccountUseCase: ShouldShowUpgradeAccountUseCase,
    monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    getCurrentUserEmail: GetCurrentUserEmail,
    getLastRegisteredEmailUseCase: GetLastRegisteredEmailUseCase,
    navigationEventQueue: NavigationEventQueue,
) : PostLoginInitialiser(
    action = { _, isFastLogin ->
        if (!isFastLogin) {
            runCatching {
                monitorFetchNodesFinishUseCase().take(1).collectLatest { isFinish ->
                    if (shouldShowUpgradeAccountUseCase() && isFinish) {
                        navigationEventQueue.emit(
                            UpgradeAccountNavKey(
                                isNewAccount = getCurrentUserEmail() == getLastRegisteredEmailUseCase(),
                                isUpgrade = false
                            )
                        )
                    }
                }
            }.onFailure { e ->
                Timber.e(e, "Error checking onboarding permissions")
            }
        }
    }
)