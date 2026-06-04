package mega.privacy.android.feature.videoeditor.presentation.editor.tool.rotate

import androidx.compose.runtime.Immutable

/**
 * Cumulative rotation (in degrees, NOT reduced mod 360 — that lets a Compose
 * `animateFloatAsState` keep interpolating in the same direction across the
 * 270→360 wrap), plus a horizontal flip flag.
 */
@Immutable
data class RotateState(
    val degrees: Int = 0,
    val flipHorizontal: Boolean = false,
) {
    val isIdentity: Boolean get() = degrees % 360 == 0 && !flipHorizontal
}
