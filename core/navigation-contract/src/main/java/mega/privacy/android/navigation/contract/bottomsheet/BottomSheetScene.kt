package mega.privacy.android.navigation.contract.bottomsheet

import androidx.activity.compose.BackHandler
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
    private val onBack: (Int) -> Unit,
    private val skipPartiallyExpanded: Boolean,
) : OverlayScene<T> {

    override val entries = listOf(sheetEntry)

    @OptIn(ExperimentalMaterial3Api::class)
    override val content = @Composable {
        val sheetState =
            rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

        BackHandler(enabled = sheetState.isVisible) {
            onBack(1)
        }

        MegaModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                onBack(1)
            },
            modifier = Modifier.Companion,
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
            windowInsets = null,
        ) {
            sheetEntry.Content()
        }

    }
}