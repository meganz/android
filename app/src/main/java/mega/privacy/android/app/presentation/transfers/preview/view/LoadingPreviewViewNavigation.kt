package mega.privacy.android.app.presentation.transfers.preview.view

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.transfers.preview.model.LoadingPreviewViewModel

/**
 * LoadingPreviewInfo data class to hold navigation info for the Loading Preview screen.
 *
 * @param transferPath The path of the transfer where the file is being downloaded for preview.
 * @param transferUniqueId The unique ID of the transfer to preview.
 */
@Serializable
class LoadingPreviewInfo(
    val transferPath: String? = null,
    val transferUniqueId: Long? = null,
)

internal fun NavGraphBuilder.loadingPreviewScreen(
    onBackPress: () -> Unit,
    navigateToStorageSettings: () -> Unit,
) {
    composable<LoadingPreviewInfo> {
        val viewModel = hiltViewModel<LoadingPreviewViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LoadingPreviewView(
            onBackPress = onBackPress,
            uiState = uiState,
            consumeTransferEvent = viewModel::consumeTransferEvent,
            navigateToStorageSettings = navigateToStorageSettings,
        )
    }
}