package mega.privacy.android.app.presentation.security


import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ReportFragment
import java.util.concurrent.atomic.AtomicBoolean

/**
 * When initialized, it hooks into the Activity callback of the Application and observes
 * Activities. It is responsible to hook in child-fragments to activities and fragments to report
 * their lifecycle events. Another responsibility of this class is to mark as stopped all lifecycle
 * providers related to an activity as soon it is not safe to run a fragment transaction in this
 * activity.
 */
internal object PasscodeLifecycleDispatcher {
    private val initialized = AtomicBoolean(false)

    @JvmStatic
    fun init(context: Context) {
        if (initialized.getAndSet(true)) {
            return
        }
        (context.applicationContext as Application)
            .registerActivityLifecycleCallbacks(DispatcherActivityCallback())
    }

    @VisibleForTesting
    internal class DispatcherActivityCallback : EmptyCallbacks() {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            ReportFragment.injectIfNeededIn(activity)
        }
    }
}