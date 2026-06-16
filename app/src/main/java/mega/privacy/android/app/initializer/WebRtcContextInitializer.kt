package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import org.webrtc.ContextUtils

/**
 * AndroidX Startup initializer that sets the WebRTC application
 */
class WebRtcContextInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        ContextUtils.initialize(context.applicationContext)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
