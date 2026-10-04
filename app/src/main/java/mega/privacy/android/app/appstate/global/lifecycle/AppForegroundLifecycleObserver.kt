package mega.privacy.android.app.appstate.global.lifecycle

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.logging.FlushLogsUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the app-lifecycle log-flush side effect: buffered log data is forced out
 * whenever the app leaves the foreground or the system reclaims memory, reducing
 * the amount lost if the process is killed while backgrounded.
 */
@Singleton
class AppForegroundLifecycleObserver @Inject constructor(
    private val flushLogsUseCase: FlushLogsUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : DefaultLifecycleObserver, ComponentCallbacks2 {

    override fun onStop(owner: LifecycleOwner) {
        flushLogs()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            flushLogs()
        }
    }

    private fun flushLogs() {
        applicationScope.launch {
            runCatching { flushLogsUseCase() }
                .onFailure { Timber.e(it, "Failed to flush logs") }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {}
}
