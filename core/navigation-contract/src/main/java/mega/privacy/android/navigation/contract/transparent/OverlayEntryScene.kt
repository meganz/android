package mega.privacy.android.navigation.contract.transparent

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene

/**
 * An [OverlayScene] that draws one entry over the entries below it, which stay in the
 * composition.
 */
class OverlayEntryScene<T : Any>(
    override val key: Any,
    private val overlayEntry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
) : OverlayScene<T> {

    override val entries = listOf(overlayEntry)

    override val content: @Composable (() -> Unit) = {
        overlayEntry.Content()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as OverlayEntryScene<*>

        if (key != other.key) return false
        if (overlayEntry != other.overlayEntry) return false
        if (previousEntries != other.previousEntries) return false
        if (overlaidEntries != other.overlaidEntries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + overlayEntry.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        return result
    }

    override fun toString(): String {
        return "OverlayEntryScene(key=$key, overlayEntry=$overlayEntry, previousEntries=$previousEntries, overlaidEntries=$overlaidEntries)"
    }
}

/**
 * Returns an [OverlayEntryScene] that draws the last entry of [entries] over the entries below it,
 * or null when that entry is not marked by [isMarked].
 *
 * It returns null for a back stack of a single entry, because the overlaid entries of an
 * [OverlayScene] must not be empty.
 */
internal fun <T : Any> overlaySceneOrNull(
    entries: List<NavEntry<T>>,
    isMarked: NavEntry<T>.() -> Boolean,
): Scene<T>? {
    if (entries.size < 2) return null

    val current = entries.last()
    if (current.isMarked().not()) return null

    val below = entries.dropLast(1)
    return OverlayEntryScene(
        key = current.contentKey,
        overlayEntry = current,
        previousEntries = below,
        overlaidEntries = below,
    )
}
