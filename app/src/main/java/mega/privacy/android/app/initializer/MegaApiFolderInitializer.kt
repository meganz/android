package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import timber.log.Timber

/**
 * Mega api folder initializer
 *
 */
class MegaApiFolderInitializer : Initializer<Unit> {

    /**
     * Mega api folder initializer entry point
     *
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MegaApiFolderInitializerEntryPoint {
        /**
         * Mega api
         *
         */
        @MegaApi
        fun megaApi(): MegaApiAndroid

        /**
         * Mega chat api
         *
         */
        @MegaApiFolder
        fun megaApiFolder(): MegaApiAndroid
    }

    /**
     * Create
     *
     */
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context,
                MegaApiFolderInitializerEntryPoint::class.java)
        entryPoint.megaApiFolder().apply {
            val megaApi = entryPoint.megaApi()
            if (isLoggedIn(megaApi)) {
                Timber.d("Logged in. Setting account auth token for folder links.")
                accountAuth = megaApi.accountAuth
            }
            retrySSLerrors(true)
            downloadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
            uploadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
        }
    }

    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(LoggerInitializer::class.java, SetupMegaApiInitializer::class.java)

    private fun isLoggedIn(megaApi: MegaApiAndroid): Boolean = megaApi.isLoggedIn != 0
}