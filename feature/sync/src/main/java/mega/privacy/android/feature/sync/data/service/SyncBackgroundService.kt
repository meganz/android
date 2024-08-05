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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.collectChunked
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The service runs SDK in the background and synchronizes folders automatically
 */
@AndroidEntryPoint
internal class SyncBackgroundService : LifecycleService() {

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
    internal lateinit var pauseSyncUseCase: PauseSyncUseCase

    @Inject
    internal lateinit var resumeSyncUseCase: ResumeSyncUseCase

    @Inject
    internal lateinit var pauseResumeSyncsBasedOnBatteryAndWiFiUseCase: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase

    @Inject
    internal lateinit var monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase

    @Inject
    internal lateinit var monitorAccountDetailUseCase: MonitorAccountDetailUseCase

    @Inject
    internal lateinit var monitorTransferEventsUseCase: MonitorTransferEventsUseCase

    @Inject
    internal lateinit var handleTransferEventUseCase: HandleTransferEventUseCase

    override fun onCreate() {
        super.onCreate()
        Timber.d("SyncBackgroundService created")
        serviceInstance = this
        startForegroundOnAndroid14()
        monitorCompletedSyncTransfers()
        lifecycleScope.launch {
            combine(
                monitorConnectivityUseCase(),
                monitorSyncByWiFiUseCase(),
                monitorBatteryInfoUseCase(),
                monitorAccountDetailUseCase()
            ) { connectedToInternet: Boolean, syncByWifi: Boolean, batteryInfo: BatteryInfo, accountDetail: AccountDetail ->
                Triple(
                    batteryInfo,
                    Pair(
                        connectedToInternet,
                        syncByWifi,
                    ),
                    accountDetail.levelDetail?.accountType == AccountType.FREE
                )
            }.collect { (batteryInfo, connectionDetails, isFreeAccount) ->
                val (connectedToInternet, syncByWifi) = connectionDetails
                updateSyncState(connectedToInternet, syncByWifi, batteryInfo, isFreeAccount)
            }
        }
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
            .setColor(getColor(R.color.components_interactive))
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
        return START_STICKY
    }

    private fun monitorCompletedSyncTransfers() {
        lifecycleScope.launch {
            monitorTransferEventsUseCase()
                .catch { Timber.e(it) }
                .filter { it.transfer.isSyncTransfer }
                .collectChunked(
                    chunkDuration = 2.seconds,
                    flushOnIdleDuration = 200.milliseconds,
                ) { transferEvents ->
                    withContext(NonCancellable) {
                        launch {
                            handleTransferEventUseCase(events = transferEvents.toTypedArray())
                        }
                    }
                }
        }
    }

    private suspend fun updateSyncState(
        connectedToInternet: Boolean,
        syncOnlyByWifi: Boolean,
        batteryInfo: BatteryInfo,
        isFreeAccount: Boolean,
    ) {
        pauseResumeSyncsBasedOnBatteryAndWiFiUseCase(
            connectedToInternet = connectedToInternet,
            syncOnlyByWifi = syncOnlyByWifi,
            batteryInfo = batteryInfo,
            isFreeAccount = isFreeAccount
        )
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

        const val LOW_BATTERY_LEVEL = 20
    }
}