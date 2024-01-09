package mega.privacy.android.app.initializer

import android.app.ActivityManager
import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.globalmanagement.BackgroundRequestListener
import mega.privacy.android.app.listeners.GlobalListener
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.util.Locale

/**
 * Setup mega api initializer
 *
 */
class SetupMegaApiInitializer : Initializer<Unit> {

    /**
     * Setup mega api initializer entry point
     *
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SetupMegaApiInitializerEntryPoint {
        /**
         * Mega api
         *
         */
        @MegaApi
        fun megaApi(): MegaApiAndroid

        /**
         * Global listener
         *
         */
        fun globalListener(): GlobalListener

        /**
         * Request listener
         *
         */
        fun requestListener(): BackgroundRequestListener

        /**
         * App scope
         *
         */
        @ApplicationScope
        fun appScope(): CoroutineScope
    }

    /**
     * Create
     *
     */
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context,
                SetupMegaApiInitializerEntryPoint::class.java
            )
        val megaApi = entryPoint.megaApi()
        megaApi.retrySSLerrors(true)
        megaApi.downloadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
        megaApi.uploadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
        entryPoint.appScope().launch {
            addListeners(megaApi, entryPoint)
        }
        setStreamingBufferSize(megaApi, context)
        setSDKLanguage(megaApi)
        setResourceLimit(megaApi)
    }

    private fun addListeners(
        megaApiAndroid: MegaApiAndroid,
        setupMegaApiInitializerEntryPoint: SetupMegaApiInitializerEntryPoint,
    ) {
        Timber.d("ADD REQUEST LISTENER")
        megaApiAndroid.addRequestListener(setupMegaApiInitializerEntryPoint.requestListener())
        megaApiAndroid.addGlobalListener(setupMegaApiInitializerEntryPoint.globalListener())
    }

    private fun setStreamingBufferSize(megaApiAndroid: MegaApiAndroid, context: Context) {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        if (memoryInfo.totalMem > BUFFER_COMP) {
            Timber.d("Total mem: %d allocate 32 MB", memoryInfo.totalMem)
            megaApiAndroid.httpServerSetMaxBufferSize(MAX_BUFFER_32MB)
        } else {
            Timber.d("Total mem: %d allocate 16 MB", memoryInfo.totalMem)
            megaApiAndroid.httpServerSetMaxBufferSize(MAX_BUFFER_16MB)
        }
    }

    /**
     * Set the language code used by the app.
     * Language code is from current system setting.
     * Need to distinguish simplified and traditional Chinese.
     */
    private fun setSDKLanguage(megaApi: MegaApiAndroid) {
        val locale = Locale.getDefault()
        var langCode: String?

        // If it's Chinese
        langCode = if (Locale.CHINESE.toLanguageTag() == locale.language) {
            if (Util.isSimplifiedChinese()) Locale.SIMPLIFIED_CHINESE.toLanguageTag() else Locale.TRADITIONAL_CHINESE.toLanguageTag()
        } else {
            locale.toString()
        }
        var result = megaApi.setLanguage(langCode)
        if (!result) {
            langCode = locale.language
            result = megaApi.setLanguage(langCode)
        }
        Timber.d("Result: $result Language: $langCode")
    }

    private fun setResourceLimit(megaApi: MegaApiAndroid) {
        // Set the proper resource limit to try avoid issues when the number of parallel transfers is very big.
        val desirableRLimit = 20000 // SDK team recommended value
        val currentLimit = megaApi.platformGetRLimitNumFile()
        Timber.d("Current resource limit is set to %s", currentLimit)
        if (currentLimit < desirableRLimit) {
            Timber.d("Resource limit is under desirable value. Trying to increase the resource limit...")
            if (!megaApi.platformSetRLimitNumFile(desirableRLimit)) {
                Timber.w("Error setting resource limit.")
            }

            // Check new resource limit after set it in order to see if had been set successfully to the
            // desired value or maybe to a lower value limited by the system.
            Timber.d("Resource limit is set to ${megaApi.platformGetRLimitNumFile()}")
        }
    }

    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(LoggerInitializer::class.java, WorkManagerInitializer::class.java)

    companion object {
        private const val BUFFER_COMP: Long = 1073741824 // 1 GB
        private const val MAX_BUFFER_16MB = 16777216 // 16 MB
        private const val MAX_BUFFER_32MB = 33554432 // 32 MB
    }
}
