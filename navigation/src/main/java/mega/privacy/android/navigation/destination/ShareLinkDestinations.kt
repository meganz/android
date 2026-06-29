package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation key for the revamped Share link screen (feature/share-link).
 *
 * Gated behind the `ShareLinkRevamp` flag: when the flag is disabled the destination
 * redirects to the legacy [GetLinkNavKey].
 *
 * @param handles List of node handles to share. A single handle is the single-node
 * "Share link" screen; multiple handles is the "Share links" multi-node screen.
 */
@Serializable
data class ShareLinkNavKey(
    val handles: List<Long> = emptyList(),
) : NavKey

/**
 * Navigation key for the revamped Link settings editor screen (feature/share-link),
 * opened from the gear action on the Share link screen.
 *
 * @param handles List of node handles whose link settings are being edited.
 */
@Serializable
data class LinkSettingsNavKey(
    val handles: List<Long> = emptyList(),
) : NavKey
