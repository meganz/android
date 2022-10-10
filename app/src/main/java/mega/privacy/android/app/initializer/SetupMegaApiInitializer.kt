package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.globalmanagement.BackgroundRequestListener
import mega.privacy.android.app.listeners.GlobalListener
import mega.privacy.android.app.utils.Util
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
         * App scope
         *
         */
        @ApplicationScope
        fun appScope(): CoroutineScope

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
    }

    /**
     * Create
     *
     */
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context,
                SetupMegaApiInitializerEntryPoint::class.java)
        with(entryPoint) {
            appScope().launch {
                megaApi().apply {
                    Timber.d("ADD REQUEST LISTENER")
                    retrySSLerrors(true)
                    downloadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
                    uploadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
                    addRequestListener(entryPoint.requestListener())
                    addGlobalListener(entryPoint.globalListener())
                }
                setSDKLanguage(megaApi())

                // Set the proper resource limit to try avoid issues when the number of parallel transfers is very big.
                val desirableRLimit = 20000 // SDK team recommended value
                val currentLimit = megaApi().platformGetRLimitNumFile()
                Timber.d("Current resource limit is set to %s", currentLimit)
                if (currentLimit < desirableRLimit) {
                    Timber.d("Resource limit is under desirable value. Trying to increase the resource limit...")
                    if (!megaApi().platformSetRLimitNumFile(desirableRLimit)) {
                        Timber.w("Error setting resource limit.")
                    }

                    // Check new resource limit after set it in order to see if had been set successfully to the
                    // desired value or maybe to a lower value limited by the system.
                    Timber.d("Resource limit is set to ${megaApi().platformGetRLimitNumFile()}")
                }
            }
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

    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(LoggerInitializer::class.java)

}