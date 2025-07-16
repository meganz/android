package mega.privacy.android.feature.clouddrive.presentation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object CloudDriveScreen

fun NavGraphBuilder.cloudDriveScreen(
    onBack: () -> Unit,
) {
    composable<CloudDriveScreen> {
        val viewModel = hiltViewModel<CloudDriveViewModel>()
        CloudDriveScreen(
            onBack = onBack,
        )
    }
}