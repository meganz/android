package mega.privacy.android.navigation.contract.transparent

import androidx.navigation3.runtime.NavEntry

/**
 * Metadata keys for transparent scenes
 */
object TransparentMetadata {
    const val KEY = "transparent_key"
}

/**
 * Creates metadata map to mark a navigation entry as transparent.
 * Transparent entries render without showing a screen, useful for entries that just
 * launch legacy activities and immediately remove themselves.
 *
 * @return Map of metadata to mark entry as transparent
 */
fun transparentMetadata() = mapOf(
    TransparentMetadata.KEY to true
)

/**
 * Checks if this navigation entry is marked as transparent
 */
internal fun NavEntry<*>.isTransparent() = this.metadata[TransparentMetadata.KEY] == true

