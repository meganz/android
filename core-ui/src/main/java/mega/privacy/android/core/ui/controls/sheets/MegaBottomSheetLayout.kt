package mega.privacy.android.core.ui.controls.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * BottomSheet used with compose navigation host
 *
 * @param bottomSheetNavigator the bottom sheet navigator used for the navigation controller
 * @param content screens with a navigation host in which the bottom sheet will show
 */
@ExperimentalMaterialNavigationApi
@Composable
fun MegaBottomSheetLayout(
    bottomSheetNavigator: BottomSheetNavigator,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        modifier = modifier,
        sheetBackgroundColor = MegaTheme.colors.background.pageBackground,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        content = content
    )
}
