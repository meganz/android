package mega.privacy.android.app

import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CompletableDeferred
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Loads the native "mega" library on a background thread to avoid blocking
 * the main thread during app startup (which causes ANR).
 */
object NativeLibraryLoader {
    private val latch = CountDownLatch(1)
    private val started = AtomicBoolean(false)
    private val loadFinished = CompletableDeferred<Unit>()

    @Volatile
    private var loadError: Throwable? = null

    /**
     * Start loading the native library on a background thread.
     * Safe to call multiple times — only the first call triggers loading.
     */
    fun startLoading() {
        if (!started.compareAndSet(false, true)) return
        Thread({
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY) }
            try {
                applyDebugLoadDelay()
                System.loadLibrary(LIBRARY_NAME)
            } catch (error: UnsatisfiedLinkError) {
                loadError = error
            } finally {
                latch.countDown()
                loadFinished.complete(Unit)
            }
        }, "mega-native-loader").start()
    }

    /**
     * Debug builds honour `adb shell setprop debug.mega.load.delay.ms <millis>` by delaying the
     * native library load, so slow-device boots (and the ANR guarantees around them) can be
     * reproduced on fast hardware. No-op in release builds or when the property is unset.
     */
    private fun applyDebugLoadDelay() {
        if (!BuildConfig.DEBUG) return
        val delayMillis = runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, DEBUG_LOAD_DELAY_PROPERTY) as? String
        }.getOrNull()?.toLongOrNull() ?: return
        if (delayMillis > 0) {
            Log.w(TAG, "Delaying native library load by $delayMillis ms ($DEBUG_LOAD_DELAY_PROPERTY)")
            runCatching { Thread.sleep(delayMillis) }
        }
    }

    /**
     * Block until the native library is loaded.
     *
     * Must never be called on the main thread while the library is still loading: on throttled
     * background process starts the load can take longer than the system ANR timeout.
     *
     * @throws IllegalStateException if the library cannot be loaded.
     */
    fun awaitLoaded() {
        startLoading()
        if (latch.count > 0) {
            reportMainThreadBlocking()
        }
        latch.await()
        ensureLoadedOrThrow()
    }

    /**
     * Suspend until the native library is loaded.
     *
     * @throws IllegalStateException if the library cannot be loaded.
     */
    suspend fun awaitLoadedSuspend() {
        startLoading()
        loadFinished.await()
        ensureLoadedOrThrow()
    }

    private fun reportMainThreadBlocking() {
        val isMainThread = runCatching {
            Looper.getMainLooper()?.isCurrentThread == true
        }.getOrDefault(false)
        if (!isMainThread) return
        val error = IllegalStateException(
            "Main thread is blocking on the native library load; this causes ANRs on slow devices"
        )
        if (BuildConfig.DEBUG) throw error
        Timber.e(error)
    }

    private fun ensureLoadedOrThrow() {
        if (loadError == null) return
        synchronized(this) {
            val error = loadError ?: return
            try {
                System.loadLibrary(LIBRARY_NAME)
                loadError = null
            } catch (retryError: UnsatisfiedLinkError) {
                retryError.addSuppressed(error)
                throw IllegalStateException(
                    "Native library $LIBRARY_NAME failed to load",
                    retryError,
                )
            }
        }
    }

    /**
     * Marks the library as loaded without touching [System.loadLibrary], so that JVM unit tests
     * of callers can pass the await calls.
     */
    @VisibleForTesting
    internal fun markLoadedForTest() {
        started.set(true)
        loadError = null
        latch.countDown()
        loadFinished.complete(Unit)
    }

    private const val LIBRARY_NAME = "mega"
    private const val TAG = "NativeLibraryLoader"
    private const val DEBUG_LOAD_DELAY_PROPERTY = "debug.mega.load.delay.ms"
}
