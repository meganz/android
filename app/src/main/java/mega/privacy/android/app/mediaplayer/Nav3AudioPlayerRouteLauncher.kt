package mega.privacy.android.app.mediaplayer

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import mega.privacy.android.app.mediaplayer.navigation.AudioPlayerScreenNavKey
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase

/**
 * Decides, for a built media-player [Intent] targeting the audio player, whether it should open as
 * the Compose single-activity route instead of launching [AudioPlayerActivity] as a standalone
 * Activity.
 *
 * When [ApiFeatures.AudioPlayerRevamp] is enabled and the Intent targets [AudioPlayerActivity],
 * the launch payload is stashed in [AudioPlayerLaunchSourceHolder] and an [AudioPlayerScreenNavKey]
 * is returned. Otherwise null is returned and the caller falls back to startActivity.
 */
@Singleton
class Nav3AudioPlayerRouteLauncher @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val launchSourceHolder: AudioPlayerLaunchSourceHolder,
) {
    suspend fun routeOrNull(intent: Intent): NavKey? {
        val revampEnabled = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerRevamp)
        }.getOrDefault(false)

        if (!revampEnabled || !targetsAudioPlayer(intent)) {
            return null
        }

        val launchId = UUID.randomUUID().toString()
        launchSourceHolder.put(launchId, intent)
        return AudioPlayerScreenNavKey(launchId)
    }

    internal fun targetsAudioPlayer(intent: Intent): Boolean =
        intent.component?.className == AudioPlayerActivity::class.java.name
}
