package mega.privacy.android.navigation.contract.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Slot rendering the audio mini player. The implementation lives in the app module, so feature
 * modules receive it through [LocalMiniAudioPlayer] instead of depending on it directly.
 *
 * `applyNavigationBarsPadding` asks the player to inset itself from the system navigation bar
 * while it is visible. Pass `true` when nothing below the player already does so.
 */
typealias MiniAudioPlayerSlot =
    @Composable (modifier: Modifier, applyNavigationBarsPadding: Boolean) -> Unit

/**
 * CompositionLocal providing the audio mini player slot.
 *
 * Defaults to a no-op so previews and screens hosted outside the app navigation graph keep working.
 */
val LocalMiniAudioPlayer = compositionLocalOf<MiniAudioPlayerSlot> { { _, _ -> } }
