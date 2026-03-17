package mega.privacy.android.shared.original.core.ui.controls.sheets

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.navigation.ModalBottomSheetLayout
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * BottomSheet used with compose navigation host
 *
 * @param bottomSheetNavigator the bottom sheet navigator used for the navigation controller
 * @param content screens with a navigation host in which the bottom sheet will show
 */
@Composable
fun MegaBottomSheetLayout(
    bottomSheetNavigator: BottomSheetNavigator,
    modifier: Modifier = Modifier,
    sheetShape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        modifier = modifier,
        sheetShape = sheetShape,
        sheetBackgroundColor = DSTokens.colors.background.surface1,
        scrimColor = DSTokens.colors.background.blur,
        content = content
    )
}
