package mega.privacy.android.app.sslverification.initialiser

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.app.appstate.global.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.app.sslverification.SSLErrorDialog
import mega.privacy.android.domain.usecase.network.MonitorSslVerificationFailedUseCase
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import timber.log.Timber
import javax.inject.Inject

class SSLErrorMonitorInitialiser @Inject constructor(
    private val monitorSslVerificationFailedUseCase: MonitorSslVerificationFailedUseCase,
    private val appDialogEventQueue: AppDialogsEventQueue,
) : AppStartInitialiser(
    action = {
        monitorSslVerificationFailedUseCase()
            .catch { Timber.e(it) }
            .collectLatest { appDialogEventQueue.emit(AppDialogEvent(SSLErrorDialog)) }
    }
)