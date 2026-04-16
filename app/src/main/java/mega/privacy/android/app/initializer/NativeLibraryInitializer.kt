package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import mega.privacy.android.app.NativeLibraryLoader

/**
 * AndroidX Startup initializer that kicks off native library loading
 * on a background thread as early as possible in the app lifecycle.
 */
class NativeLibraryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        NativeLibraryLoader.startLoading()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
