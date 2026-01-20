package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.app.fcm.FcmManager
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Initializer that monitors account detail and manages notification topics accordingly.
 */
class NotificationTopicsInitializer @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val fcmManager: FcmManager,
) : PostLoginInitialiser(
    action = { context, _ ->
        monitorAccountDetailUseCase()
            .catch { Timber.Forest.e(it) }
            .mapNotNull { it.levelDetail?.accountType }
            .distinctUntilChanged()
            .collect {
                fcmManager.subscribeToAccountTypeTopic(it)
                fcmManager.setAccountTypeUserProperty(it)
            }
    }
)