package mega.privacy.android.app.presentation.videoplayer

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.annotation.VisibleForTesting
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Manages all Picture-in-Picture (PIP) logic for [VideoPlayerActivity].
 *
 * Extracted from [VideoPlayerActivity] to keep the activity lean. Owns cross-instance
 * tracking ([activePipInstance]), PIP params setup, aspect-ratio computation, and the
 * lifecycle hooks that drive PIP enter/exit behaviour.
 *
 * @param isPipEnabled Returns whether the PIP feature flag is currently enabled.
 * @param getVideoSize Returns the current playing video size, or null if unavailable.
 * @param onEnterPipMode Called to request the system to enter PIP mode with the given params.
 * @param isTaskRoot Returns whether the activity is the root of its task.
 * @param onLaunchMainApp Called when PIP is dismissed and the user should return to the main app.
 * @param onFinish Called to finish the owning activity.
 * @param mediaPlayerGateway Gateway used to stop/release the player on the old instance.
 * @param packageManager Used to check for PIP system feature support.
 */
internal class VideoPlayerPipManager(
    private val isPipEnabled: () -> Boolean,
    private val getVideoSize: () -> VideoSize?,
    private val onEnterPipMode: (PictureInPictureParams) -> Unit,
    private val isTaskRoot: () -> Boolean,
    private val onLaunchMainApp: () -> Unit,
    private val onFinish: () -> Unit,
    private val mediaPlayerGateway: MediaPlayerGateway,
    private val packageManager: PackageManager,
) {
    private var pipParams: PictureInPictureParams.Builder? = null
    private var isExitingPipMode = false
    private var isInPipMode = false

    /**
     * True when a new [VideoPlayerActivity] has taken over and called [finish] on this instance.
     * Checked in [VideoPlayerActivity.onDestroy] to skip player cleanup that the new instance
     * already performed.
     */
    var isBeingReplacedByNewInstance = false
        private set

    /**
     * Initialises PIP params, finishes any existing PIP instance, and registers this manager
     * as the new active PIP instance. Must be called from [VideoPlayerActivity.onCreate].
     */
    fun initialize() {
        finishExistingIfNeeded()
        activePipInstance = WeakReference(this)
        initializePipParams()
    }

    /**
     * Attempts to enter PIP mode using the current video size.
     * Does nothing if the PIP feature flag is off or PIP is not supported on the device.
     */
    fun enterPipModeIfPossible() {
        val params = pipParams ?: return
        if (!isPipEnabled()) return
        try {
            val videoSize = getVideoSize()
            val rational = if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
                computePipAspectRatio(videoSize.width, videoSize.height)
            } else {
                Rational(PIP_DEFAULT_WIDTH_RATIO, PIP_DEFAULT_HEIGHT_RATIO)
            }
            params.setAspectRatio(rational)
            onEnterPipMode(params.build())
        } catch (e: Exception) {
            Timber.w(e, "Failed to enter PIP mode")
        }
    }

    /**
     * Must be called from [VideoPlayerActivity.onPictureInPictureModeChanged].
     * Updates [isInPipMode] and sets [isExitingPipMode] when the PIP window is being dismissed.
     */
    fun onPipModeChanged(isInPipMode: Boolean) {
        this.isInPipMode = isInPipMode
        if (!isInPipMode) {
            isExitingPipMode = true
        }
    }

    /**
     * Must be called from [VideoPlayerActivity.onStart].
     * Resets [isExitingPipMode] — the activity came to the foreground, so PIP was expanded
     * rather than dismissed.
     */
    fun onStart() {
        isExitingPipMode = false
    }

    /**
     * Must be called from [VideoPlayerActivity.onStop].
     *
     * @return true if the PIP manager fully handled the stop event and the activity should
     * return immediately without further processing (e.g. pausing playback).
     */
    fun onStop(): Boolean {
        if (isExitingPipMode) {
            isExitingPipMode = false
            if (isTaskRoot()) {
                onLaunchMainApp()
            }
            onFinish()
            return true
        }
        if (isBeingReplacedByNewInstance) return true
        return false
    }

    /**
     * Must be called from [VideoPlayerActivity.onDestroy].
     * Clears [activePipInstance] only if it still points to this manager, so that a new
     * instance that has already registered itself is not accidentally cleared.
     */
    fun onDestroy() {
        if (activePipInstance?.get() === this) {
            activePipInstance = null
        }
    }

    private fun finishExistingIfNeeded() {
        activePipInstance?.get()?.let { existingManager ->
            if (existingManager !== this && existingManager.isInPipMode) {
                existingManager.isBeingReplacedByNewInstance = true
                // MediaPlayerGateway is a singleton shared across all VideoPlayerActivity instances,
                // so stopping/releasing here correctly affects the old instance's player.
                mediaPlayerGateway.playerStop()
                mediaPlayerGateway.playerRelease()
                existingManager.onFinish()
            }
        }
    }

    private fun initializePipParams() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            pipParams = PictureInPictureParams.Builder().also { builder ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setSeamlessResizeEnabled(false)
                    builder.setAutoEnterEnabled(false)
                }
            }
        }
    }

    private fun computePipAspectRatio(width: Int, height: Int): Rational {
        return when {
            width * PIP_MAX_ASPECT_RATIO_DENOM > height * PIP_MAX_ASPECT_RATIO_NUM ->
                Rational(PIP_MAX_ASPECT_RATIO_NUM, PIP_MAX_ASPECT_RATIO_DENOM)

            height * PIP_MAX_ASPECT_RATIO_DENOM > width * PIP_MAX_ASPECT_RATIO_NUM ->
                Rational(PIP_MAX_ASPECT_RATIO_DENOM, PIP_MAX_ASPECT_RATIO_NUM)

            else -> Rational(width, height)
        }
    }

    companion object {
        private const val PIP_DEFAULT_WIDTH_RATIO = 16
        private const val PIP_DEFAULT_HEIGHT_RATIO = 9
        private const val PIP_MAX_ASPECT_RATIO_NUM = 239
        private const val PIP_MAX_ASPECT_RATIO_DENOM = 100

        /**
         * Holds the [VideoPlayerPipManager] currently in PIP mode. Stored in the companion object
         * (not an instance field) because two activities can coexist briefly — when the user opens
         * a second video while one is in PIP, the new instance uses this reference to [finish] the
         * old one before taking over. [WeakReference] prevents blocking garbage collection.
         */
        @VisibleForTesting
        internal var activePipInstance: WeakReference<VideoPlayerPipManager>? = null
    }
}
