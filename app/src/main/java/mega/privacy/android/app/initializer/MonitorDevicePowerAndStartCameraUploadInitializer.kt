package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.environment.MonitorDevicePowerConnectionStateUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import timber.log.Timber

/**
 * Initialiser that monitors device power connection state and starts camera upload when connected.
 * This initialiser runs on app start and continuously monitors the power connection state.
 */
class MonitorDevicePowerAndStartCameraUploadInitializer : Initializer<Unit> {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MonitorDevicePowerAndStartCameraUploadInitialiserEntryPoint {

        fun monitorDevicePowerConnectionStateUseCase(): MonitorDevicePowerConnectionStateUseCase

        fun startCameraUploadUseCase(): StartCameraUploadUseCase

        @ApplicationScope
        fun appScope(): CoroutineScope
    }

    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context,
                MonitorDevicePowerAndStartCameraUploadInitialiserEntryPoint::class.java
            )
        entryPoint.appScope().launch {
            Timber.d("MonitorDevicePowerAndStartCameraUploadInitialiser launched")
            action(
                entryPoint.monitorDevicePowerConnectionStateUseCase(),
                entryPoint.startCameraUploadUseCase()
            )
        }
    }

    internal suspend fun action(
        monitorDevicePowerConnectionStateUseCase: MonitorDevicePowerConnectionStateUseCase,
        startCameraUploadUseCase: StartCameraUploadUseCase,
    ) {
        monitorDevicePowerConnectionStateUseCase().catch {
            Timber.e(
                "An error occurred while monitoring the Device Power Connection State $it"
            )
        }.collect { state ->
            Timber.d("The Device Power Connection State is $state")
            if (state == DevicePowerConnectionState.Connected) {
                startCameraUploadUseCase()
            }
        }
    }

    /**
     * Dependencies
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(mega.privacy.android.app.initializer.LoggerInitializer::class.java)
}
