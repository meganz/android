package mega.privacy.android.app.presentation.security

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ReportFragment
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import timber.log.Timber

/**
 * Class that provides lifecycle for the whole application process.
 *
 * You can consider this LifecycleOwner as the composite of all of your Activities, except that
 * [Lifecycle.Event.ON_CREATE] will be dispatched once and [Lifecycle.Event.ON_DESTROY]
 * will never be dispatched. Other lifecycle events will be dispatched with following rules:
 * ProcessLifecycleOwner will dispatch [Lifecycle.Event.ON_START],
 * [Lifecycle.Event.ON_RESUME] events, as a first activity moves through these events.
 * [Lifecycle.Event.ON_PAUSE], [Lifecycle.Event.ON_STOP], events will be dispatched with
 * a **delay** after a last activity
 * passed through them. This delay is long enough to guarantee that ProcessLifecycleOwner
 * won't send any events if activities are destroyed and recreated due to a
 * configuration change.
 *
 * It is useful for use cases where you would like to react on your app coming to the foreground or
 * going to the background and you don't need a milliseconds accuracy in receiving lifecycle
 * events.
 */
class PasscodeProcessLifecycleOwner private constructor() {


    // ground truth counters
    private var startedCounter = 0
    private var resumedCounter = 0

    var observer: PasscodeProcessLifeCycleObserver? = null

    inner class ActivityInitializationListener(private val activity: Activity) :
        ReportFragment.ActivityInitializationListener {
        override fun onCreate() {}

        override fun onStart() {
            activityStarted(activity)
        }

        override fun onResume() {
            activityResumed()
        }
    }

    companion object {
        private val newInstance = PasscodeProcessLifecycleOwner()

        /**
         * The LifecycleOwner for the whole application process. Note that if your application
         * has multiple processes, this provider does not know about other processes.
         *
         * @return [LifecycleOwner] for the whole application.
         */
        @JvmStatic
        fun get(): PasscodeProcessLifecycleOwner {
            return newInstance
        }

        @JvmStatic
        internal fun init(context: Context) {
            newInstance.attach(context)
        }
    }

    private fun getStateString(activity: Activity?) = buildString {
        activity?.let {
            append("Activity: ${activity.localClassName}\n")
            val orientation = it.resources.configuration.orientation
            append("Orientation: $orientation\n")
        }
        append("Started count: $startedCounter\n")
        append("Resumed count: $resumedCounter\n")
    }

    internal fun activityStarted(activity: Activity?) {
        startedCounter++
        if (startedCounter == 1) {
            Timber.d("Process lifecycle event: STARTED \n ${getStateString(activity)}")
            activity?.let {
                observer?.onStart(
                    PasscodeProcessLifeCycleEventData(it.resources.configuration.orientation)
                )
            }
        }
    }

    internal fun activityResumed() {
        resumedCounter++
    }

    internal fun activityPaused() {
        resumedCounter--
    }

    internal fun activityStopped(activity: Activity?) {
        startedCounter--
        if (startedCounter == 0) {
            Timber.d("Process lifecycle event: STOPPED \n ${getStateString(activity)}")
            activity?.let {
                observer?.onStop(
                    PasscodeProcessLifeCycleEventData(it.resources.configuration.orientation)
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    internal fun attach(context: Context) {
        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(object : EmptyCallbacks() {
            @RequiresApi(29)
            override fun onActivityPreCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) {
                // We need the ProcessLifecycleOwner to get ON_START and ON_RESUME precisely
                // before the first activity gets its LifecycleOwner started/resumed.
                // The activity's LifecycleOwner gets started/resumed via an activity registered
                // callback added in onCreate(). By adding our own activity registered callback in
                // onActivityPreCreated(), we get our callbacks first while still having the
                // right relative order compared to the Activity's onStart()/onResume() callbacks.
                Api29Impl.registerActivityLifecycleCallbacks(activity,
                    object : EmptyCallbacks() {
                        override fun onActivityPostStarted(activity: Activity) {
                            activityStarted(activity)
                        }

                        override fun onActivityPostResumed(activity: Activity) {
                            activityResumed()
                        }
                    })
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Only use ReportFragment pre API 29 - after that, we can use the
                // onActivityPostStarted and onActivityPostResumed callbacks registered in
                // onActivityPreCreated()
                if (Build.VERSION.SDK_INT < 29) {
                    activity.reportFragment.setProcessListener(
                        ActivityInitializationListener(
                            activity
                        )
                    )
                }
            }

            override fun onActivityPaused(activity: Activity) {
                activityPaused()
            }

            override fun onActivityStopped(activity: Activity) {
                activityStopped(activity)
            }
        })
    }


    @RequiresApi(29)
    internal object Api29Impl {
        @DoNotInline
        @JvmStatic
        fun registerActivityLifecycleCallbacks(
            activity: Activity,
            callback: Application.ActivityLifecycleCallbacks,
        ) {
            activity.registerActivityLifecycleCallbacks(callback)
        }
    }
}

interface PasscodeProcessLifeCycleObserver {
    fun onStart(data: PasscodeProcessLifeCycleEventData)
    fun onStop(data: PasscodeProcessLifeCycleEventData)
}

data class PasscodeProcessLifeCycleEventData(
    val orientation: Int,
)


