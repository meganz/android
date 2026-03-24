package mega.privacy.android.shared.original.core.ui.navigation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetDefaults
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
fun rememberExtendedBottomSheetNavigator(
    animationSpec: AnimationSpec<Float> = ModalBottomSheetDefaults.AnimationSpec
): BottomSheetNavigator {
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        animationSpec = animationSpec,
        skipHalfExpanded = true
    )
    return remember { BottomSheetNavigator(sheetState) }
}
