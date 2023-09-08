package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import mega.privacy.android.app.BuildConfig

/**
 * Karma initializer
 */
class KarmaInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // Karma.karmaPluginPort = BuildConfig.KARMA_PLUGIN_PORT
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
