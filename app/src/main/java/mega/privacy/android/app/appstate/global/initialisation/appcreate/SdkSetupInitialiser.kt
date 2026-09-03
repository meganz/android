package mega.privacy.android.app.appstate.global.initialisation.appcreate

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.listeners.global.GlobalListener
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.global.InitialiseGlobalListenersUseCase
import mega.privacy.android.navigation.contract.initialisation.CriticalAsyncAppCreateInitialiser
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * Configures the core MEGA SDK instance and attaches its global listeners.
 *
 * Critical: SSL retry, transfer methods and the global listeners must be in place before any
 * other unit or app code talks to the SDK. Buffer size, SDK language and the resource limit stay
 * fire-and-forget in the application scope, exactly as they ran at androidx.startup provider
 * time.
 *
 * The SDK dependency is lazy so that constructing this initialiser never constructs the SDK
 * instance, whose creation blocks until the native library has finished loading.
 */
internal class SdkSetupInitialiser @Inject constructor(
    @MegaApi private val megaApiLazy: dagger.Lazy<MegaApiAndroid>,
    private val globalListener: GlobalListener,
    private val initialiseGlobalListenersUseCase: InitialiseGlobalListenersUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : CriticalAsyncAppCreateInitialiser {
    override val name = "SdkSetupInitialiser"

    private val megaApi: MegaApiAndroid
        get() = megaApiLazy.get()

    override suspend operator fun invoke() {
        megaApi.retrySSLerrors(true)
        megaApi.downloadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
        megaApi.uploadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
        addListeners()
        appScope.launch {
            setStreamingBufferSize()
            setSDKLanguage()
            setResourceLimit()
        }
    }

    private fun addListeners() {
        Timber.d("ADD REQUEST LISTENER")
        appScope.launch {
            initialiseGlobalListenersUseCase()
        }

        megaApi.addGlobalListener(globalListener)
    }

    private fun setStreamingBufferSize() {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        if (memoryInfo.totalMem > BUFFER_COMP) {
            Timber.d("Total mem: %d allocate 32 MB", memoryInfo.totalMem)
            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB)
        } else {
            Timber.d("Total mem: %d allocate 16 MB", memoryInfo.totalMem)
            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB)
        }
    }

    /**
     * Set the language code used by the app.
     * Language code is from current system setting.
     * Need to distinguish simplified and traditional Chinese.
     */
    private fun setSDKLanguage() {
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

    private fun setResourceLimit() {
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

    companion object {
        private const val BUFFER_COMP: Long = 1073741824 // 1 GB
        private const val MAX_BUFFER_16MB = 16777216 // 16 MB
        private const val MAX_BUFFER_32MB = 33554432 // 32 MB
    }
}
