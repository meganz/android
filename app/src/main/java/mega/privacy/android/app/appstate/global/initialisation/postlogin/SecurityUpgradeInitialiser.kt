package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.account.MonitorSecurityUpgradeInAppUseCase
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import mega.privacy.android.navigation.destination.SecurityUpgradeDialogNavKey
import timber.log.Timber
import javax.inject.Inject

class SecurityUpgradeInitialiser @Inject constructor(
    private val monitorSecurityUpgradeInAppUseCase: MonitorSecurityUpgradeInAppUseCase,
    private val appDialogsEventQueue: AppDialogsEventQueue,
) : PostLoginInitialiser(
    action = { _, _ ->
        monitorSecurityUpgradeInAppUseCase()
            .catch { Timber.e(it, "Failed to monitor security upgrade event") }
            .collectLatest { shouldShow ->
                if (shouldShow) {
                    appDialogsEventQueue.emit(
                        event = AppDialogEvent(dialogDestination = SecurityUpgradeDialogNavKey),
                        priority = NavPriority.Priority(11)
                    )
                }
            }
    }
)

