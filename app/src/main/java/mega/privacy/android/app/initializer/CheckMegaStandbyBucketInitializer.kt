package mega.privacy.android.app.initializer

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import timber.log.Timber

/**
 * Check mega standby bucket initializer
 *
 */
class CheckMegaStandbyBucketInitializer : Initializer<Unit> {
    /**
     * Get tombstone info initializer entry point
     *
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CheckMegaStandbyBucketInitializerEntryPoint {
        /**
         * App scope
         *
         */
        @ApplicationScope
        fun appScope(): CoroutineScope
    }

    /**
     * Get the current standby bucket of the app.
     * The system determines the standby state of the app based on app usage patterns.
     *
     * @return the current standby bucket of the app：
     * STANDBY_BUCKET_ACTIVE,
     * STANDBY_BUCKET_WORKING_SET,
     * STANDBY_BUCKET_FREQUENT,
     * STANDBY_BUCKET_RARE,
     * STANDBY_BUCKET_RESTRICTED,
     * STANDBY_BUCKET_NEVER
     */
    override fun create(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val entryPoint =
                EntryPointAccessors.fromApplication(context,
                    CheckMegaStandbyBucketInitializerEntryPoint::class.java)

            // make it run in background thread
            entryPoint.appScope().launch {
                (context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)
                    ?.let { usageStatsManager ->
                        Timber.d("getAppStandbyBucket(): ${usageStatsManager.appStandbyBucket}")
                    }
            }
        }
    }

    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(LoggerInitializer::class.java)
}