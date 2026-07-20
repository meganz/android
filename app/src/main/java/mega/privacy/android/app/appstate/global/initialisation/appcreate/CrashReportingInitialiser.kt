package mega.privacy.android.app.appstate.global.initialisation.appcreate

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.jni.JniExceptionHandler
import mega.privacy.android.app.jni.JniExceptionReporter
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import timber.log.Timber
import javax.inject.Inject

/**
 * Installs the process-wide uncaught-exception handler and the JNI exception reporter.
 *
 * Critical: crash handlers must be in place before any other boot work or app code runs, so a
 * failure anywhere later in boot is still captured.
 */
internal class CrashReportingInitialiser @Inject constructor(
    private val crashReporter: CrashReporter,
) : SynchronousAppCreateInitialiser {
    override val name = "CrashReportingInitialiser"

    override operator fun invoke() {
        if (!BuildConfig.DEBUG) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                handleUncaughtException(throwable)
                if (isAppRelatedThrowable(throwable)) {
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }

            JniExceptionReporter.handler = object : JniExceptionHandler {
                override fun onJniException(location: String, message: String, stacktrace: String) {
                    try {
                        Timber.e("JNI exception at %s: %s\n%s", location, message, stacktrace)

                        Firebase.crashlytics.recordException(
                            RuntimeException("JNI exception at $location: $message\n$stacktrace")
                        )
                    } catch (t: Throwable) {
                        Log.e("Failed to log JNI exception: ${t.message}", t)
                    }
                }
            }

        } else {
            val isDebugBuildType = BuildConfig.BUILD_TYPE == "debug"
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(!isDebugBuildType)
            JniExceptionReporter.handler = object : JniExceptionHandler {
                override fun onJniException(location: String, message: String, stacktrace: String) {
                    try {
                        Timber.e("JNI exception at %s: %s\n%s", location, message, stacktrace)
                    } catch (t: Throwable) {
                        Log.e("Failed to log JNI exception: ${t.message}", t)
                    }
                }
            }
        }
    }

    private fun handleUncaughtException(throwable: Throwable) {
        Timber.e(throwable, "UNCAUGHT EXCEPTION")
        crashReporter.report(throwable)
    }

    /**
     * Returns true if [throwable] has at least one stack frame inside the app's package.
     * Pure third-party / system stacks return false so they can be reported as non-fatals
     */
    private fun isAppRelatedThrowable(throwable: Throwable): Boolean =
        throwable.stackTrace.any { it.className.startsWith(APP_PACKAGE_PREFIX) }

    companion object {
        private const val APP_PACKAGE_PREFIX = "mega.privacy.android."
    }
}
