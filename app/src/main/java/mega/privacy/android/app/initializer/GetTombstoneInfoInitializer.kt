package mega.privacy.android.app.initializer

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.protobuf.TombstoneProtos
import mega.privacy.android.domain.qualifier.ApplicationScope
import timber.log.Timber
import java.io.IOException

class GetTombstoneInfoInitializer : Initializer<Unit> {

    /**
     * Get tombstone info initializer entry point
     *
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GetTombstoneInfoInitializerEntryPoint {
        /**
         * App scope
         *
         */
        @ApplicationScope
        fun appScope(): CoroutineScope
    }

    /**
     * Create
     *
     */
    override fun create(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val entryPoint =
                EntryPointAccessors.fromApplication(context,
                    GetTombstoneInfoInitializerEntryPoint::class.java)

            // make it run in background thread
            entryPoint.appScope().launch {
                Timber.d("getTombstoneInfo")
                (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.let { activityManager ->
                    val exitReasons = activityManager.getHistoricalProcessExitReasons(
                        /* packageName = */null,
                        /* pid = */0,
                        /* maxNum = */3
                    )
                    exitReasons.forEach { exitReason ->
                        if (exitReason.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                            // Get the tombstone input stream.
                            try {
                                exitReason.traceInputStream?.use {
                                    // The tombstone parser built with protoc uses the tombstone schema, then parses the trace.
                                    val tombstone =
                                        TombstoneProtos.Tombstone.parseFrom(it)
                                    Timber.e("Tombstone Info $tombstone")
                                }
                            } catch (e: IOException) {
                                Timber.e(e)
                            }
                        }
                    }
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