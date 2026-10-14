package mega.privacy.android.navigation.contract.transparent

import androidx.navigation3.runtime.NavEntry
import mega.privacy.android.navigation.contract.metadata.NavEntryMetadataScope

object OverContentMetadata {
    const val KEY = "over_content_key"
}

/**
 * Registers over content metadata on this scope, which keeps the entries below this one in the
 * composition and draws them behind it. Use it for a screen that uncovers the screen below while
 * it moves, for example the audio player, which the user drags down to collapse.
 *
 * The entries below keep running for as long as this entry is displayed, so mark an entry with
 * this only when the screen below must stay visible.
 *
 * Use inside [mega.privacy.android.navigation.contract.metadata.buildMetadata]:
 * ```
 * metadata = buildMetadata {
 *     withOverContent()
 * }
 * ```
 */
fun NavEntryMetadataScope.withOverContent() {
    set(OverContentMetadata.KEY, true)
}

/**
 * Checks if this navigation entry is marked as drawn over the content below it
 */
internal fun NavEntry<*>.isOverContent() = this.metadata[OverContentMetadata.KEY] == true
