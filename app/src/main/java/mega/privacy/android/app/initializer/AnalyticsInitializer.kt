package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import mega.privacy.android.analytics.Analytics

/**
 * Analytics initializer
 *
 * @constructor Create empty Analytics initializer
 */
class AnalyticsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Analytics.initialise(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> =
        mutableListOf(LoggerInitializer::class.java, SetupMegaApiInitializer::class.java)
}