package mega.privacy.android.app.mediaplayer.miniplayer

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.feature.mediaplayer.navigation.AudioPlayerScreenNavKey
import mega.privacy.android.feature.mediaplayer.presentation.miniplayer.AudioMiniPlayerRoute
import mega.privacy.android.navigation.contract.state.LocalNavigationRailVisible
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional

/**
 * The mini player a host should place: the Compose bar for the revamped player, the Media3
 * `PlayerView` bar for the legacy one.
 *
 * Only one of the two is ever composed. Both open their own `MediaController` against the same
 * session, so composing both would show two bars playing the same audio.
 *
 * @param modifier Modifier.
 * @param lifecycleOwner Owner the legacy bar observes; unused by the Compose bar, which follows
 * the composition.
 * @param onVisibilityChanged Called whenever the bar appears or disappears, so a host can adjust
 * the layout around it.
 * @param applyNavigationBarsPadding Inset the bar from the system navigation bar while visible,
 * for hosts where nothing below it already does so.
 */
@Composable
fun MiniAudioPlayerHost(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onVisibilityChanged: ((Boolean) -> Unit)? = null,
    applyNavigationBarsPadding: Boolean = false,
) {
    val activity = LocalActivity.current as? ComponentActivity
    if (activity == null) {
        // The Compose bar has to be scoped to an activity to keep one MediaController per host.
        // The Media3 bar needs no activity, so falling back to it keeps the mini player working
        // instead of dropping it silently.
        MiniAudioPlayerView(
            modifier = modifier,
            lifecycleOwner = lifecycleOwner,
            onVisibilityChanged = onVisibilityChanged,
            applyNavigationBarsPadding = applyNavigationBarsPadding,
        )
        return
    }

    val viewModel = hiltViewModel<MiniAudioPlayerHostViewModel>(activity)
    val isComposeBarEnabled by viewModel.isComposeMiniPlayerEnabled.collectAsStateWithLifecycle()

    when (isComposeBarEnabled) {
        null -> Unit

        true -> ComposeMiniAudioPlayer(
            activity = activity,
            modifier = modifier,
            onVisibilityChanged = onVisibilityChanged,
            applyNavigationBarsPadding = applyNavigationBarsPadding,
        )

        false -> MiniAudioPlayerView(
            modifier = modifier,
            lifecycleOwner = lifecycleOwner,
            onVisibilityChanged = onVisibilityChanged,
            applyNavigationBarsPadding = applyNavigationBarsPadding,
        )
    }
}

/**
 * Supplies the revamped bar with the behaviour that only the app module can reach: the
 * full-screen player destination and stopping the playback service.
 *
 * [AudioMiniPlayerRoute] resolves its ViewModel from the nearest owner and opens a
 * `MediaController` per instance, so the activity is provided as the owner here. A host inside a
 * navigation entry would otherwise reconnect the controller on every destination change.
 */
@Composable
private fun ComposeMiniAudioPlayer(
    activity: ComponentActivity,
    modifier: Modifier,
    onVisibilityChanged: ((Boolean) -> Unit)?,
    applyNavigationBarsPadding: Boolean,
) {
    val context = LocalContext.current
    val isNavigationRailVisible = LocalNavigationRailVisible.current

    CompositionLocalProvider(LocalViewModelStoreOwner provides activity) {
        AudioMiniPlayerRoute(
            onPlayerClicked = {
                if (!CallUtil.participatingInACall()) {
                    context.startActivity(
                        MegaActivity.getIntentWithExtraDestinations(
                            context,
                            listOf(AudioPlayerScreenNavKey(AudioPlayerScreenNavKey.RESUME_LAUNCH_ID)),
                            dropIfAlreadyShown = true,
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            )
                        }
                    )
                }
            },
            onCloseClicked = { AudioPlayerService.stopAudioPlayer(context) },
            onVisibilityChanged = onVisibilityChanged,
            modifier = modifier.conditional(
                isNavigationRailVisible || applyNavigationBarsPadding
            ) { navigationBarsPadding() },
        )
    }
}
