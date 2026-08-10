package mega.privacy.android.app

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Loads the native "mega" library on a background thread to avoid blocking
 * the main thread during app startup (which causes ANR).
 */
object NativeLibraryLoader {
    private val latch = CountDownLatch(1)
    private val started = AtomicBoolean(false)

    /**
     * Start loading the native library on a background thread.
     * Safe to call multiple times — only the first call triggers loading.
     */
    fun startLoading() {
        if (!started.compareAndSet(false, true)) return
        Thread({
            try {
                System.loadLibrary("mega")
            } catch (_: UnsatisfiedLinkError) {
                // Library may already be loaded
            } finally {
                latch.countDown()
            }
        }, "mega-native-loader").start()
    }

    /**
     * Block until the native library is loaded.
     * If loading hasn't started yet, starts it first.
     */
    fun awaitLoaded() {
        startLoading()
        latch.await()
    }
}
