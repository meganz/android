package mega.privacy.android.navigation.contract.bottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground

class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val sheetEntry: NavEntry<T>,
    private val onBack: () -> Unit,
    private val skipPartiallyExpanded: Boolean,
) : OverlayScene<T> {

    override val entries = listOf(sheetEntry)

    @OptIn(ExperimentalMaterial3Api::class)
    override val content = @Composable {
        val sheetState =
            rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

        BackHandler(enabled = sheetState.isVisible) {
            onBack()
        }

        MegaModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                onBack()
            },
            modifier = Modifier.statusBarsPadding(),
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        ) {
            sheetEntry.Content()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BottomSheetScene<*>

        if (key != other.key) return false
        if (previousEntries != other.previousEntries) return false
        if (overlaidEntries != other.overlaidEntries) return false
        if (sheetEntry != other.sheetEntry) return false
        if (skipPartiallyExpanded != other.skipPartiallyExpanded) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        result = 31 * result + sheetEntry.hashCode()
        result = 31 * result + skipPartiallyExpanded.hashCode()
        return result
    }

    override fun toString(): String {
        return "BottomSheetScene(key=$key, previousEntries=$previousEntries, overlaidEntries=$overlaidEntries, sheetEntry=$sheetEntry, skipPartiallyExpanded=$skipPartiallyExpanded)"
    }
}