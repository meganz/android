package mega.privacy.android.app.presentation.login.logoutdialog

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.account.MonitorLoggedOutFromAnotherLocationUseCase
import mega.privacy.android.domain.usecase.account.SetLoggedOutFromAnotherLocationUseCase
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import timber.log.Timber
import javax.inject.Inject

class RemoteLogoutInitialiser @Inject constructor(
    private val monitorLoggedOutFromAnotherLocationUseCase: MonitorLoggedOutFromAnotherLocationUseCase,
    private val setLoggedOutFromAnotherLocationUseCase: SetLoggedOutFromAnotherLocationUseCase,
    private val appDialogEventQueue: AppDialogsEventQueue,
) : PostLoginInitialiser(
    action = { _, _ ->
        monitorLoggedOutFromAnotherLocationUseCase()
            .catch { Timber.e(it) }
            .collectLatest { loggedOut ->
                if (loggedOut) {
                    appDialogEventQueue.emit(AppDialogEvent(RemoteLogoutDialogNavKey))
                    setLoggedOutFromAnotherLocationUseCase(false)
                }
            }
    }
)