package mega.privacy.android.shared.original.core.ui.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Create and remember a [BottomSheetNavigator] skipping half expanded state.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberExtendedBottomSheetNavigator(): BottomSheetNavigator {
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    return remember { BottomSheetNavigator(sheetState) }
}
