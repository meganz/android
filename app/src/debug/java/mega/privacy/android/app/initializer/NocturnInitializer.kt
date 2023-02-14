package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.nocturn.NocturnImpl
import mega.privacy.android.nocturn.notification.NocturnNotificator

/**
 * Nocturn initializer
 */
class NocturnInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        if (!BuildConfig.ACTIVATE_NOCTURN) return

        val notificator = NocturnNotificator(context)
        val nocturn = NocturnImpl(notificator)
        nocturn.monitor(waitTimeout = BuildConfig.NOCTURN_TIMEOUT)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
