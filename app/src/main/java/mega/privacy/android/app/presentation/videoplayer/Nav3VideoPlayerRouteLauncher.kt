package mega.privacy.android.app.presentation.videoplayer

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Decides, for a built media-player [Intent], whether the revamped video player should open as the
 * Compose single-activity route instead of [VideoPlayerActivity].
 *
 * Centralises the [AppFeatures.VideoPlayerActivityRefactor] check so every entry point that would
 * otherwise `startActivity` a [VideoPlayerActivity] intent behaves consistently.
 */
class Nav3VideoPlayerRouteLauncher @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val launchSourceHolder: VideoPlayerLaunchSourceHolder,
) {
    /**
     * Always null while the revamped video player is being rebuilt: its Compose route has been
     * removed, so there is no destination to return and every caller starts [VideoPlayerActivity]
     * as before, whatever [AppFeatures.VideoPlayerActivityRefactor] is set to.
     *
     * The dependencies above are kept because the rebuilt route needs exactly the same ones: the
     * flag decides whether to take the route, and the launch payload is stashed in
     * [VideoPlayerLaunchSourceHolder] so the navigation key stays small enough for the back stack.
     */
    suspend fun routeOrNull(intent: Intent): NavKey? = null
}
