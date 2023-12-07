package mega.privacy.android.feature.sync.data.service

import mega.privacy.android.icon.pack.R as iconPackR
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
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
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.usecase.GetFolderPairsUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.ResumeSyncUseCase
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

    @Inject
    internal lateinit var pauseSyncUseCase: PauseSyncUseCase

    @Inject
    internal lateinit var resumeSyncUseCase: ResumeSyncUseCase

    @Inject
    internal lateinit var getFolderPairsUseCase: GetFolderPairsUseCase

    override fun onCreate() {
        super.onCreate()
        Timber.d("SyncBackgroundService created")
        serviceInstance = this
        startForegroundOnAndroid14()
    }

    /*
     * Background Service doesn't work on Android 14, so we have to start it as Foreground Service
     */
    private fun startForegroundOnAndroid14() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, getNotification())
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_SYNC_SERVICE_ID) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_SYNC_SERVICE_ID,
                NOTIFICATION_CHANNEL_SYNC_SERVICE_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setShowBadge(false)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        val builderCompat = NotificationCompat.Builder(
            applicationContext,
            NOTIFICATION_CHANNEL_SYNC_SERVICE_ID
        )

        return builderCompat
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setColor(getColor(R.color.red_600_red_300))
            .setOngoing(true)
            .setContentTitle(getString(R.string.sync_notification_monitoring_description))
            .build()
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
            getFolderPairsUseCase().forEach { pauseSyncUseCase(it.id) }
        } else {
            getFolderPairsUseCase().forEach { resumeSyncUseCase(it.id) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceInstance = null
        Timber.d("SyncBackgroundService destroyed")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    internal companion object {
        /**
         * Starts the service
         */
        fun start(context: Context) {
            val serviceIntent = Intent(context, SyncBackgroundService::class.java)
            context.startService(serviceIntent)
        }

        fun stop(context: Context) {
            val serviceIntent = Intent(context, SyncBackgroundService::class.java)
            context.stopService(serviceIntent)
        }

        fun isRunning() = serviceInstance != null

        private var serviceInstance: SyncBackgroundService? = null

        const val NOTIFICATION_ID = 80
        const val NOTIFICATION_CHANNEL_SYNC_SERVICE_ID = "SyncServiceNotification"
        const val NOTIFICATION_CHANNEL_SYNC_SERVICE_NAME = "Background Syncs"
    }
}