package mega.privacy.android.analytics

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.analytics.di.AnalyticsEntrypoint
import mega.privacy.android.analytics.tracker.AnalyticsTracker

object Analytics {
    @Volatile
    private var instance: AnalyticsTracker? = null

    fun initialise(context: Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    instance =
                        EntryPointAccessors.fromApplication(
                            context,
                            AnalyticsEntrypoint::class.java
                        )
                            .provideAnalyticsTracker()
                }
            }
        }
    }

    val tracker: AnalyticsTracker
        get() = instance
            ?: throw IllegalStateException("Analytics need to be initialised before accessing the tracker")
}

