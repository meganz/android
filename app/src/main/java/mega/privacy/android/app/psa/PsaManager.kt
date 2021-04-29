package mega.privacy.android.app.psa

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.JobIntentService
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Constants.EVENT_PSA


/**
 * The ViewModel for PSA logic.
 */
object PsaManager/* : LifecycleObserver */{

    private const val LAST_PSA_CHECK_TIME_KEY = "last_psa_check_time"

    /**
     * The minimum interval in milliseconds that we should keep between two calls to
     * SDK to get PSA from server.
     */
//    private const val GET_PSA_INTERVAL_MS = 3600_000L
    const val GET_PSA_INTERVAL_MS = 5_000L

    @SuppressLint("StaticFieldLeak")
    private val application = MegaApplication.getInstance()
    private val megaApi = application.megaApi

    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private var psa: Psa? = null
//    private var getPsaDisposable: Disposable? = null
//    private var processLifecycleObserved = false

//    private var appInBackground = false

    /**
     * When we skipped a checking in background, to avoid redundant waiting,
     * we should check immediately and reschedule future checking,
     * rescheduleOnForeground is whether we should do that.
     */
//    private var rescheduleOnForeground = false

    /**
     * LiveData for PSA, mutable, used to emit value.
     */
    private val mutablePsa = MutableLiveData<Psa?>()

    /**
     * LiveData for PSA, not mutable, only used to observe value.
     */
//    val psa: LiveData<Psa?> = mutablePsa

//    private var alarmManager: AlarmManager? = null
//    private var pendingIntent: PendingIntent? = null
//
//    init {
//        val context = application.applicationContext
//        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
//        val intent = Intent(context, CheckPsaService::class.java)
//        pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
//    }

    /**
     * Start checking PSA periodically.
     */
    fun startChecking() {
        val timeSinceLastCheck =
            System.currentTimeMillis() - preferences.getLong(LAST_PSA_CHECK_TIME_KEY, 0L)
        var delay = GET_PSA_INTERVAL_MS - timeSinceLastCheck

        if (delay < 0) delay = 0
        AlarmReceiver.setAlarm(application.applicationContext, delay) {
//            mutablePsa.value = it
            psa = it
            LiveEventBus.get(EVENT_PSA, Psa::class.java).post(it)
        }
//        doStartChecking()

//        if (!processLifecycleObserved) {
//            processLifecycleObserved = true
//            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
//        }
    }

    /**
     * Stop checking PSA periodically.
     */
    fun stopChecking() {
//        rescheduleOnForeground = false
        doStopChecking()
//
//        // If user logout while there is a PSA displaying (not shown yet), if we don't
//        // reset mutablePsa, it will be displayed in LoginActivity again, which is not
//        // desired.
//        mutablePsa.value = null
        psa = null
        preferences.edit().remove(LAST_PSA_CHECK_TIME_KEY).apply()
    }

//    private fun doStartChecking() {
////        if (getPsaDisposable != null) {
////            return
////        }
//
//        val timeSinceLastCheck =
//            System.currentTimeMillis() - preferences.getLong(LAST_PSA_CHECK_TIME_KEY, 0L)
//
//        logDebug("doStartChecking timeSinceLastCheck $timeSinceLastCheck")
//
//        getPsaDisposable = Observable.interval(
//            // minus initial delay will be treated as 0
//            GET_PSA_INTERVAL_MS - timeSinceLastCheck,
//            GET_PSA_INTERVAL_MS, TimeUnit.MILLISECONDS
//        )
//            .subscribe(Consumer {
//                if (appInBackground) {
//                    logDebug("skip getPSAWithUrl")
//
//                    rescheduleOnForeground = true
//
//                    return@Consumer
//                }
//
//                preferences.edit()
//                    .putLong(LAST_PSA_CHECK_TIME_KEY, System.currentTimeMillis())
//                    .apply()
//
//                logDebug("getPSAWithUrl ${System.currentTimeMillis()}")
//
//                megaApi.getPSAWithUrl(object : BaseListener(application) {
//                    override fun onRequestFinish(
//                        api: MegaApiJava,
//                        request: MegaRequest,
//                        e: MegaError
//                    ) {
//                        super.onRequestFinish(api, request, e)
//
//                        // API response may arrive after stopChecking, in this case we shouldn't
//                        // emit PSA anymore.
//                        if (e.errorCode == MegaError.API_OK && getPsaDisposable != null) {
//                            mutablePsa.value = Psa(
//                                request.number.toInt(), request.name, request.text, request.file,
//                                request.password, request.link, request.email
//                            )
//                        }
//                    }
//                })
//            }, RxUtil.logErr("PsaManager getPSA"))
//    }

    private fun doStopChecking() {
//        getPsaDisposable?.dispose()
//        getPsaDisposable = null
        AlarmReceiver.cancelAlarm(application.applicationContext)
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
//        val value = mutablePsa.value
//        if (value != null) {
//            mutablePsa.postValue(value)
//        }
        if (psa != null) {
            Log.i("Alex", "display pending psa")
            LiveEventBus.get(EVENT_PSA, Psa::class.java).post(psa)
        }
    }

    /**
     * Dismiss the PSA.
     *
     * @param id the id of the PSA
     */
    fun dismissPsa(id: Int) {
        Log.i("Alex", "dismiss psa: $id")
        megaApi.setPSA(id)
//        mutablePsa.value = null
        psa = null
    }

//    @OnLifecycleEvent(Lifecycle.Event.ON_START)
//    fun onMoveToForeground() {
//        appInBackground = false
//
//        // When we skipped a checking in background, to avoid redundant waiting,
//        // we should check immediately and reschedule future checking.
//        if (rescheduleOnForeground) {
//            rescheduleOnForeground = false
//
//            doStopChecking()
//            doStartChecking()
//        }
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
//    fun onMoveToBackground() {
//        appInBackground = true
//    }
}
