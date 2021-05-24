package mega.privacy.android.app.psa

import android.annotation.SuppressLint
import androidx.preference.PreferenceManager
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Constants.EVENT_PSA

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

    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private var psa: Psa? = null

    /**
     * Start checking PSA periodically.
     */
    fun startChecking() {
        val timeSinceLastCheck =
            System.currentTimeMillis() - preferences.getLong(LAST_PSA_CHECK_TIME_KEY, 0L)
        var delay = GET_PSA_INTERVAL_MS - timeSinceLastCheck

        if (delay < 0) delay = 0

        AlarmReceiver.setAlarm(application.applicationContext, delay) {
            preferences.edit()
                .putLong(LAST_PSA_CHECK_TIME_KEY, System.currentTimeMillis())
                .apply()
            psa = it
            LiveEventBus.get(EVENT_PSA, Psa::class.java).post(it)
        }
    }

    /**
     * Stop checking PSA periodically.
     */
    fun stopChecking() {
        doStopChecking()

        // If user logout while there is a PSA displaying (not shown yet), if we don't
        // reset psa, it will be displayed in LoginActivity again, which is not
        // desired.
        psa = null
        preferences.edit().remove(LAST_PSA_CHECK_TIME_KEY).apply()
    }

    private fun doStopChecking() {
        AlarmReceiver.cancelAlarm(application.applicationContext)
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
