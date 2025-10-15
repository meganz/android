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

    override val content = @Composable {
        transparentEntry.Content()
    }
}

