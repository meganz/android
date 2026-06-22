package mega.privacy.android.app.presentation.videoplayer

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.videoplayer.navigation.ComposeVideoPlayerScreenNavKey
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import java.util.UUID
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
     * Returns a [ComposeVideoPlayerScreenNavKey] (after stashing the launch payload in
     * [VideoPlayerLaunchSourceHolder]) when [AppFeatures.VideoPlayerActivityRefactor] is on and
     * [intent] targets the revamped [VideoPlayerActivity]; otherwise null, meaning the caller should
     * `startActivity(intent)` as before.
     */
    suspend fun routeOrNull(intent: Intent): NavKey? {
        val refactorEnabled = runCatching {
            getFeatureFlagValueUseCase(AppFeatures.VideoPlayerActivityRefactor)
        }.getOrDefault(false)

        if (!refactorEnabled ||
            intent.component?.className != VideoPlayerActivity::class.java.name
        ) {
            return null
        }

        val launchId = UUID.randomUUID().toString()
        launchSourceHolder.put(launchId, intent.toVideoPlayerLaunchSource())
        return ComposeVideoPlayerScreenNavKey(launchId)
    }
}
