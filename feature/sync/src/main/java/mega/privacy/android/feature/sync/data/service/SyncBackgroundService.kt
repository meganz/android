package mega.privacy.android.feature.sync.data.service

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.PauseAllSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.ResumeAllSyncsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The service runs SDK in the background and synchronizes folders automatically
 */
@AndroidEntryPoint
internal class SyncBackgroundService : LifecycleService() {

    @Inject
    @IoDispatcher
    internal lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    internal lateinit var backgroundFastLoginUseCase: BackgroundFastLoginUseCase

    @Inject
    internal lateinit var applicationLoggingInSetter: ApplicationLoggingInSetter

    @Inject
    internal lateinit var monitorConnectivityUseCase: MonitorConnectivityUseCase

    @Inject
    internal lateinit var monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase

    @Inject
    internal lateinit var isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase

    @Inject
    internal lateinit var pauseAllSyncsUseCase: PauseAllSyncsUseCase

    @Inject
    internal lateinit var resumeAllSyncsUseCase: ResumeAllSyncsUseCase

    @Inject
    internal lateinit var monitorSyncUseCase: MonitorSyncUseCase

    private lateinit var changedFolderPair: SharedFlow<FolderPair>

    override fun onCreate() {
        super.onCreate()
        Timber.d("SyncBackgroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("SyncBackgroundService started")
        changedFolderPair =
            monitorSyncUseCase().stateIn(lifecycleScope, SharingStarted.Eagerly, FolderPair.empty())


        if (!applicationLoggingInSetter.isLoggingIn()) {
            lifecycleScope.launch {
                applicationLoggingInSetter.setLoggingIn(true)
                runCatching { backgroundFastLoginUseCase() }.getOrElse(Timber::e)
                applicationLoggingInSetter.setLoggingIn(false)
            }
        }

        lifecycleScope.launch {
            combine(
                monitorConnectivityUseCase(),
                monitorSyncByWiFiUseCase(),
                changedFolderPair
            ) { connectedToInternet: Boolean, syncByWifi: Boolean, currentSync: FolderPair ->
                Pair(connectedToInternet, syncByWifi)
            }.collect { (connectedToInternet, syncByWifi) ->
                updateSyncState(connectedToInternet, syncByWifi)
            }
        }
        return START_STICKY
    }

    private suspend fun updateSyncState(
        connectedToInternet: Boolean,
        syncOnlyByWifi: Boolean,
    ) {
        val internetNotAvailable = !connectedToInternet
        val userNotOnWifi = !isOnWifiNetworkUseCase()
        if (internetNotAvailable || syncOnlyByWifi && userNotOnWifi) {
            pauseAllSyncsUseCase()
        } else {
            resumeAllSyncsUseCase()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("SyncBackgroundService destroyed")
    }

    internal companion object {
        /**
         * Starts the service
         */
        fun start(context: Context) {
            val serviceIntent = Intent(context, SyncBackgroundService::class.java)
            context.startService(serviceIntent)
        }
    }
}