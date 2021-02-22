package mega.privacy.android.app.psa

import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RxUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import java.util.concurrent.TimeUnit

/**
 * The ViewModel for PSA logic.
 */
object PsaManager : LifecycleObserver {

    private const val LAST_PSA_CHECK_TIME_KEY = "last_psa_check_time"

    /**
     * The minimum interval in milliseconds that we should keep between two calls to
     * SDK to get PSA from server.
     */
    private const val GET_PSA_INTERVAL_MS = 3600_000L

    private val application = MegaApplication.getInstance()
    private val megaApi = application.megaApi

    private var getPsaDisposable: Disposable? = null
    private var processLifecycleObserved = false

    private var appInBackground = false

    /**
     * When we skipped a checking in background, to avoid redundant waiting,
     * we should check immediately and reschedule future checking,
     * rescheduleOnForeground is whether we should do that.
     */
    private var rescheduleOnForeground = false

    /**
     * LiveData for PSA, mutable, used to emit value.
     */
    private val mutablePsa = MutableLiveData<Psa?>()

    /**
     * LiveData for PSA, not mutable, only used to observe value.
     */
    val psa: LiveData<Psa?> = mutablePsa

    /**
     * Start checking PSA periodically.
     */
    fun startChecking() {
        doStartChecking()

        if (!processLifecycleObserved) {
            processLifecycleObserved = true
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }

    /**
     * Stop checking PSA periodically.
     */
    fun stopChecking() {
        rescheduleOnForeground = false
        doStopChecking()

        PreferenceManager.getDefaultSharedPreferences(MegaApplication.getInstance())
            .edit()
            .remove(LAST_PSA_CHECK_TIME_KEY)
            .apply()
    }

    private fun doStartChecking() {
        if (getPsaDisposable != null) {
            return
        }

        val preferences =
            PreferenceManager.getDefaultSharedPreferences(MegaApplication.getInstance())

        val timeSinceLastCheck =
            System.currentTimeMillis() - preferences.getLong(LAST_PSA_CHECK_TIME_KEY, 0L)

        logDebug("doStartChecking timeSinceLastCheck $timeSinceLastCheck")

        getPsaDisposable = Observable.interval(
            // minus initial delay will be treated as 0
            GET_PSA_INTERVAL_MS - timeSinceLastCheck,
            GET_PSA_INTERVAL_MS, TimeUnit.MILLISECONDS
        )
            .subscribe(Consumer {
                if (appInBackground) {
                    logDebug("skip getPSAWithUrl")

                    rescheduleOnForeground = true

                    return@Consumer
                }

                preferences.edit()
                    .putLong(LAST_PSA_CHECK_TIME_KEY, System.currentTimeMillis())
                    .apply()

                logDebug("getPSAWithUrl ${System.currentTimeMillis()}")

                megaApi.getPSAWithUrl(object : BaseListener(application) {
                    override fun onRequestFinish(
                        api: MegaApiJava,
                        request: MegaRequest,
                        e: MegaError
                    ) {
                        super.onRequestFinish(api, request, e)

                        if (e.errorCode == MegaError.API_OK) {
                            mutablePsa.value = Psa(
                                request.number.toInt(), request.name, request.text, request.file,
                                request.password, request.link, request.email
                            )
                        }
                    }
                })
            }, RxUtil.logErr("PsaManager getPSA"))
    }

    private fun doStopChecking() {
        getPsaDisposable?.dispose()
        getPsaDisposable = null
    }

    /**
     * Display the pending PSA (if exists) immediately.
     *
     * Activity will display PSA if it's resumed, but when switching activity, there will be a
     * time window that no activity is resumed, and if we get PSA result from API server in this
     * window, this PSA won't be displayed. So we need check if there is a PSA when activity is
     * resumed, if so, we'll display it immediately.
     *
     * And since activity's lifecycle state is updated after onResume return, we need post this
     * value.
     */
    fun displayPendingPsa() {
        val value = mutablePsa.value
        if (value != null) {
            mutablePsa.postValue(value)
        }
    }

    /**
     * Dismiss the PSA.
     *
     * @param id the id of the PSA
     */
    fun dismissPsa(id: Int) {
        megaApi.setPSA(id)
        mutablePsa.value = null
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        appInBackground = false

        // When we skipped a checking in background, to avoid redundant waiting,
        // we should check immediately and reschedule future checking.
        if (rescheduleOnForeground) {
            rescheduleOnForeground = false

            doStopChecking()
            doStartChecking()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        appInBackground = true
    }
}
