package mega.privacy.android.feature.sync.data.service

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncsUseCase
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
    @LoginMutex
    internal lateinit var loginMutex: Mutex

    @Inject
    internal lateinit var monitorConnectivityUseCase: MonitorConnectivityUseCase

    @Inject
    internal lateinit var monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase

    @Inject
    internal lateinit var isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase

    @Inject
    internal lateinit var monitorSyncsUseCase: MonitorSyncsUseCase

    override fun onCreate() {
        super.onCreate()
        Timber.d("SyncBackgroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("SyncBackgroundService started")

        if (!loginMutex.isLocked) {
            lifecycleScope.launch {
                runCatching { backgroundFastLoginUseCase() }.getOrElse(Timber::e)
            }
        }

        lifecycleScope.launch {
            combine(
                monitorConnectivityUseCase(),
                monitorSyncByWiFiUseCase(),
                monitorSyncsUseCase()
            ) { connectedToInternet: Boolean, syncByWifi: Boolean, _ ->
                Pair(connectedToInternet, syncByWifi)
            }.collect { (connectedToInternet, syncByWifi) ->
                // pause syncs if not on wifi, will be implemented later
            }
        }
        return START_STICKY
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