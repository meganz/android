package mega.privacy.android.app.appstate.global.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import mega.android.core.ui.components.LocalMegaScaffoldTopBanner
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerHost
import mega.privacy.android.navigation.contract.state.LocalMiniAudioPlayer
import mega.privacy.android.navigation.contract.state.MiniAudioPlayerSlot

/**
 * The single place the app-shell CompositionLocals are provided: the return-to-call banner through
 * [LocalMegaScaffoldTopBanner] and the audio mini player through [LocalMiniAudioPlayer].
 *
 * Every host that renders app destinations must wrap them in this, the single activity shell and
 * the standalone activities alike. A host that provides the locals itself, or not at all, leaves
 * its screens on the no-op defaults, so they silently lose the banner and the mini player.
 *
 * @param showTopBanner whether the return-to-call banner is offered to the content. False for the
 * immersive viewers, which never had a return-to-call affordance and whose chrome is meant to
 * stay out of the way.
 * @param content content that can read the shell locals.
 */
@Composable
fun ProvideAppShellLocals(
    showTopBanner: Boolean = true,
    content: @Composable () -> Unit,
) {
    ProvideAppShellLocals(
        topBanner = if (showTopBanner) rememberOngoingCallBanner() else null,
        miniAudioPlayer = { modifier, applyNavigationBarsPadding ->
            MiniAudioPlayerHost(
                modifier = modifier,
                applyNavigationBarsPadding = applyNavigationBarsPadding,
            )
        },
        content = content,
    )
}

@Composable
internal fun ProvideAppShellLocals(
    topBanner: (@Composable () -> Unit)?,
    miniAudioPlayer: MiniAudioPlayerSlot,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMegaScaffoldTopBanner provides topBanner,
        LocalMiniAudioPlayer provides miniAudioPlayer,
        content = content,
    )
}
