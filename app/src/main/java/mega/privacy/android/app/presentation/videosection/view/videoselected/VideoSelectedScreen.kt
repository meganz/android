package mega.privacy.android.app.presentation.videosection.view.videoselected

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.videosection.VideoSelectedViewModel
import mega.privacy.android.feature.sync.ui.mapper.FileTypeIconMapper

@Composable
internal fun VideoSelectedScreen(
    viewModel: VideoSelectedViewModel,
    onVideoSelected: (List<Long>) -> Unit,
    onBackPressed: () -> Unit,
    onSortOrderClick: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
) {
    val navHostController = rememberNavController()

    VideoSelectedNavHost(
        viewModel = viewModel,
        navHostController = navHostController,
        modifier = Modifier,
        onBackPressed = onBackPressed,
        onSortOrderClick = onSortOrderClick,
        onVideoSelected = onVideoSelected,
        fileTypeIconMapper = fileTypeIconMapper,
    )
}

@Composable
internal fun VideoSelectedNavHost(
    viewModel: VideoSelectedViewModel,
    navHostController: NavHostController,
    modifier: Modifier,
    onBackPressed: () -> Unit,
    onSortOrderClick: () -> Unit,
    onVideoSelected: (List<Long>) -> Unit,
    startDestination: String = videoSelectedRoute,
    fileTypeIconMapper: FileTypeIconMapper,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = startDestination
    ) {
        composable(
            route = videoSelectedRoute
        ) {
            VideoSelectedView(
                uiState = state,
                onSearchTextChange = viewModel::searchQuery,
                onCloseClicked = viewModel::closeSearch,
                onSearchClicked = viewModel::searchWidgetStateUpdate,
                onBackPressed = onBackPressed,
                onVideoSelected = onVideoSelected,
                onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                onSortOrderClick = onSortOrderClick,
                onItemClicked = viewModel::itemClicked,
                onMenuActionClick = { action ->
                    when (action) {
                        is FileInfoMenuAction.SelectionModeAction.SelectAll ->
                            viewModel.selectAllVideos()

                        is FileInfoMenuAction.SelectionModeAction.ClearSelection ->
                            viewModel.clearAllSelectedVideos()

                        else -> {}
                    }
                },
                fileTypeIconMapper = fileTypeIconMapper,
            )
        }
    }
}