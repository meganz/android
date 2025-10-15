package mega.privacy.android.navigation.contract.transparent

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene

/**
 * A transparent scene that renders nothing, used for navigation entries that just launch
 * legacy activities and immediately remove themselves to avoid UI flashing.
 */
class TransparentScene<T : Any>(
    override val key: Any,
    private val transparentEntry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
) : OverlayScene<T> {

    override val entries = listOf(transparentEntry)

    override val content: @Composable (() -> Unit) = {
        transparentEntry.Content()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TransparentScene<*>

        if (key != other.key) return false
        if (transparentEntry != other.transparentEntry) return false
        if (previousEntries != other.previousEntries) return false
        if (overlaidEntries != other.overlaidEntries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + transparentEntry.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        return result
    }

    override fun toString(): String {
        return "TransparentScene(key=$key, transparentEntry=$transparentEntry, previousEntries=$previousEntries, overlaidEntries=$overlaidEntries)"
    }
}

