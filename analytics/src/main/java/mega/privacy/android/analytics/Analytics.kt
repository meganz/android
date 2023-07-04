package mega.privacy.android.analytics

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.analytics.di.AnalyticsEntrypoint
import mega.privacy.android.analytics.tracker.AnalyticsTracker

/**
 * Analytics
 */
object Analytics {
    @Volatile
    private var instance: AnalyticsTracker? = null

    /**
     * Initialise
     *
     * @param context
     */
    fun initialise(context: Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    instance =
                        EntryPointAccessors.fromApplication(
                            context,
                            AnalyticsEntrypoint::class.java
                        ).provideAnalyticsTracker()
                }
            }
        }
    }

    /**
     * Initialise
     *
     * @param analyticsTracker
     */
    fun initialise(analyticsTracker: AnalyticsTracker?) {
        instance = analyticsTracker
    }

    /**
     * Tracker
     */
    val tracker: AnalyticsTracker
        get() = instance
            ?: throw IllegalStateException("Analytics need to be initialised before accessing the tracker")
}

