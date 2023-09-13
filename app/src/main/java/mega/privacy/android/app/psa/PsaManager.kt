package mega.privacy.android.app.psa

import android.annotation.SuppressLint
import androidx.preference.PreferenceManager
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.CoroutineScopesModule
import mega.privacy.android.app.di.CoroutinesDispatchersModule
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.Constants.EVENT_PSA
import mega.privacy.android.domain.entity.psa.Psa
import nz.mega.sdk.MegaError

/**
 * The ViewModel for PSA logic.
 */
object PsaManager {

    private const val LAST_PSA_CHECK_TIME_KEY = "last_psa_check_time"

    /**
     * The minimum interval in milliseconds that we should keep between two calls to
     * SDK to get PSA from server.
     */
    const val GET_PSA_INTERVAL_MS = 3600_000L

    @SuppressLint("StaticFieldLeak")
    private val application = MegaApplication.getInstance()
    private val megaApi = application.megaApi

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(application) }
    private var psa: Psa? = null
    private val coroutineScope = CoroutineScopesModule
        .provideCoroutineScope(CoroutinesDispatchersModule.providesIoDispatcher())

    /**
     * Check PSA from server
     */
    suspend fun checkPsa() = withContext(coroutineScope.coroutineContext) {
        val timeSinceLastCheck =
            System.currentTimeMillis() - preferences.getLong(LAST_PSA_CHECK_TIME_KEY, 0L)

        if (timeSinceLastCheck >= GET_PSA_INTERVAL_MS) {
            megaApi.getPSAWithUrl(OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                preferences.edit()
                    .putLong(LAST_PSA_CHECK_TIME_KEY, System.currentTimeMillis())
                    .apply()
                if (error.errorCode == MegaError.API_OK) {
                    psa = Psa(
                        request.number.toInt(), request.name, request.text, request.file,
                        request.password, request.link, request.email
                    )
                    LiveEventBus.get(EVENT_PSA, Psa::class.java).post(psa)
                }
            }))
        }
    }

    /**
     * Clean Psa Check timestamp from preferences
     */
    suspend fun clearPsa() = withContext(coroutineScope.coroutineContext) {
        // If user logout while there is a PSA displaying (not shown yet), if we don't
        // reset psa, it will be displayed in LoginActivity again, which is not
        // desired.
        psa = null
        preferences.edit().remove(LAST_PSA_CHECK_TIME_KEY).apply()
    }


    /**
     * Dismiss the PSA.
     *
     * @param id the id of the PSA
     */
    fun dismissPsa(id: Int) {
        megaApi.setPSA(id)
        psa = null
    }
}
