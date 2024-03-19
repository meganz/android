package mega.privacy.android.feature.devicecenter.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.devicecenter.ui.DeviceCenterInfoScreen
import mega.privacy.android.feature.devicecenter.ui.DeviceCenterInfoViewModel
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode

@Composable
internal fun DeviceCenterInfoScreenRoute(
    viewModel: DeviceCenterInfoViewModel,
    selectedItem: DeviceCenterUINode,
    onBackPressHandled: () -> Unit,
) {
    val uiState = viewModel.state.collectAsStateWithLifecycle()
    viewModel.setSelectedItem(selectedItem)

    DeviceCenterInfoScreen(uiState = uiState.value, onBackPressHandled = onBackPressHandled)
}