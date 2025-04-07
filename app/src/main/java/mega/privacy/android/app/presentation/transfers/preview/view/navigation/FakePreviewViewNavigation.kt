package mega.privacy.android.app.presentation.transfers.preview.view.navigation

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.sharedViewModel
import mega.privacy.android.app.presentation.transfers.preview.model.FakePreviewViewModel
import mega.privacy.android.app.presentation.transfers.preview.view.FakePreviewView

internal const val fakePreviewRoute = "fakePreview"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.fakePreviewScreen(
    navHostController: NavHostController,
    scaffoldState: ScaffoldState,
    onBackPress: () -> Unit,
) {
    composable(route = fakePreviewRoute) { backStackEntry ->
        val viewModel =
            backStackEntry.sharedViewModel<FakePreviewViewModel>(navController = navHostController)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        FakePreviewView(
            scaffoldState = scaffoldState,
            onBackPress = onBackPress,
            uiState = uiState,
        )
    }
}