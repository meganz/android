package mega.privacy.android.app.initializer

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import mega.privacy.android.core.coroutine.logAndSwallowExceptions
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import mega.privacy.android.domain.usecase.environment.MonitorDevicePowerConnectionStateUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Post login initialiser that starts camera upload when the device begins charging or regains
 * connectivity. Runs after login via
 * [mega.privacy.android.app.appstate.global.initialisation.GlobalInitialiser.onPostLogin].
 */
class CameraUploadAutoStartInitializer @Inject constructor(
    monitorDevicePowerConnectionStateUseCase: MonitorDevicePowerConnectionStateUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    startCameraUploadUseCase: StartCameraUploadUseCase,
) : PostLoginInitialiserAction(
    action = { _, _ ->
        val powerTrigger = monitorDevicePowerConnectionStateUseCase()
            .filter { it == DevicePowerConnectionState.Connected }
        val connectivityTrigger = monitorConnectivityUseCase()
            .filter { it }
        merge(powerTrigger, connectivityTrigger)
            .catch { Timber.e(it, "An error occurred while monitoring camera upload triggers") }
            .collect {
                runCatching {
                    startCameraUploadUseCase()
                }.logAndSwallowExceptions()
            }
    }
)
