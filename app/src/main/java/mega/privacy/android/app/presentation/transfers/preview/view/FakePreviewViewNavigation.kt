package mega.privacy.android.app.presentation.transfers.preview.view

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.transfers.preview.model.FakePreviewViewModel

/**
 * FakePreviewInfo data class to hold navigation info for the Fake Preview screen.
 *
 * @param transferPath The path of the transfer where the file is being downloaded for preview.
 * @param transferUniqueId The unique ID of the transfer to preview.
 * @param transferTagToCancel The tag of the transfer to cancel if any.
 */
@Serializable
class FakePreviewInfo(
    val transferPath: String? = null,
    val transferUniqueId: Long? = null,
    val transferTagToCancel: Int? = null,
)

internal fun NavGraphBuilder.fakePreviewScreen(
    onBackPress: () -> Unit,
    navigateToStorageSettings: () -> Unit,
) {
    composable<FakePreviewInfo> {
        val viewModel = hiltViewModel<FakePreviewViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        FakePreviewView(
            onBackPress = onBackPress,
            uiState = uiState,
            consumeTransferEvent = viewModel::consumeTransferEvent,
            navigateToStorageSettings = navigateToStorageSettings,
        )
    }
}