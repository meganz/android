package mega.privacy.android.feature.devicecenter.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.devicecenter.ui.DeviceCenterInfoViewModel
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.view.DeviceCenterInfoScreenM3

/**
 * Material 3 Route for Device Center Info Screen
 *
 * This route uses DeviceCenterInfoScreenM3 which does NOT have its own AppBar,
 * relying instead on the parent's DeviceCenterAppBarM3.
 *
 * @param viewModel The DeviceCenterInfoViewModel
 * @param selectedItem The selected item (device or folder)
 * @param onBackPressHandled Lambda to handle back press
 * @param paddingValues Padding values from parent scaffold
 */
@Composable
internal fun DeviceCenterInfoScreenRouteM3(
    viewModel: DeviceCenterInfoViewModel,
    selectedItem: DeviceCenterUINode,
    onBackPressHandled: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(selectedItem) {
        viewModel.setSelectedItem(selectedItem)
    }

    DeviceCenterInfoScreenM3(
        uiState = uiState,
        onBackPressHandled = onBackPressHandled,
        paddingValues = paddingValues,
    )
}
