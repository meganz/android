package mega.privacy.android.navigation.contract.suppression

import androidx.navigation3.runtime.NavEntry
import mega.privacy.android.navigation.contract.metadata.NavEntryMetadataScope

/**
 * Metadata keys for overlay suppression.
 *
 * When a screen's entry is annotated with [withOverlaySuppression], any event
 * whose NavKey implements
 * [mega.privacy.android.navigation.contract.navkey.Suppressable]
 * will be deferred until the suppressing screen is no longer active.
 */
object OverlaySuppressionMetadata {
    const val KEY = "suppress_overlays"
}

/**
 * Marks this screen as suppressing overlay events (PSAs, non-critical dialogs).
 *
 * While this screen is the active (top) content screen, any event whose NavKey
 * implements [mega.privacy.android.navigation.contract.navkey.Suppressable]
 * will be deferred until this screen is no longer active.
 *
 * Use inside [mega.privacy.android.navigation.contract.metadata.buildMetadata]:
 * ```
 * metadata = buildMetadata {
 *     withOverlaySuppression()
 *     withScreenViewEvent(SomeScreenEvent)
 * }
 * ```
 */
fun NavEntryMetadataScope.withOverlaySuppression() {
    set(OverlaySuppressionMetadata.KEY, true)
}

/**
 * Returns `true` if this entry's metadata declares overlay suppression.
 */
fun NavEntry<*>.suppressesOverlays(): Boolean =
    metadata[OverlaySuppressionMetadata.KEY] == true
