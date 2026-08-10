package mega.privacy.android.feature.documentscanner.presentation.analyzer

/**
 * Gates how often analysis frames are processed, so boundary detection runs at
 * a fixed cadence (≈5 Hz) instead of on every camera frame (~30 Hz). Detection
 * is expensive; throttling keeps CPU/GPU and battery use down without hurting
 * the stability signal, which only needs a few frames per second.
 *
 * Not thread-safe by design: it is driven from CameraX's single `ImageAnalysis`
 * executor thread, so no synchronisation is needed on [lastProcessedMs].
 *
 * @param intervalMs Minimum time between processed frames, in milliseconds.
 */
internal class FrameRateLimiter(private val intervalMs: Long) {

    private var lastProcessedMs: Long? = null

    /**
     * Returns true and records [timestampMs] as the last processed frame when at
     * least [intervalMs] has elapsed since the previous processed frame; returns
     * false otherwise (the caller should skip this frame). The first call always
     * returns true.
     */
    fun shouldProcess(timestampMs: Long): Boolean {
        val last = lastProcessedMs
        if (last != null && timestampMs - last < intervalMs) return false
        lastProcessedMs = timestampMs
        return true
    }

    /** Forget the last processed timestamp so the next frame is always processed. */
    fun reset() {
        lastProcessedMs = null
    }
}
