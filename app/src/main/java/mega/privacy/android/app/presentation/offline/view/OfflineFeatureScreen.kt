package mega.privacy.android.app.presentation.offline.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.app.presentation.offline.offlinecompose.OfflineComposeViewModel

/**
 * Scaffold for the Offline Flow Screen
 */
@Composable
fun OfflineFeatureScreen(viewModel: OfflineComposeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navHostController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
    ) { padding ->
        OfflineNavHostController(
            modifier = Modifier
                .padding(padding),
            navHostController = navHostController,
        )
    }
}

@Composable
internal fun OfflineNavHostController(
    modifier: Modifier,
    navHostController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = offlineRoute
    ) {
        offlineScreen()
    }
}