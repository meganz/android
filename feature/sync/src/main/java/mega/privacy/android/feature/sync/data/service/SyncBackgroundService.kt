package mega.privacy.android.feature.sync.data.service

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
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

    override fun onCreate() {
        super.onCreate()
        Timber.d("SyncBackgroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("SyncBackgroundService started")
        if (!applicationLoggingInSetter.isLoggingIn()) {
            lifecycleScope.launch {
                applicationLoggingInSetter.setLoggingIn(true)
                runCatching { backgroundFastLoginUseCase() }.getOrElse(Timber::e)
                applicationLoggingInSetter.setLoggingIn(false)
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