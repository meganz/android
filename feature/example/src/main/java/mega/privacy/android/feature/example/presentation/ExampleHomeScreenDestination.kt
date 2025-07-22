package mega.privacy.android.feature.example.presentation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.destination.ExampleLegacyResultScreen
import mega.privacy.android.navigation.destination.Secondary

@Serializable
data object HomeScreen

@Serializable
data object HomeScreen2

fun NavGraphBuilder.exampleHomeScreen(
    setNavigationVisibility: (Boolean) -> Unit,
) {
    composable<HomeScreen> {
        ExampleHomeScreen(content = "1", setNavigationItemVisibility = setNavigationVisibility)
    }
}

fun NavGraphBuilder.otherExampleHomeScreen(
    onNavigate: (Any) -> Unit,
    resultFlow: (String) -> Flow<Int?>,
) {
    composable<HomeScreen2> {
        val viewModel = hiltViewModel<ExampleViewModel>()
        val result by resultFlow(ExampleLegacyResultScreen.RESULT_KEY).collectAsStateWithLifecycle(
            null
        )
        ExampleHomeScreen2(
            logout = { viewModel.logout() },
            navigateToFeature = { onNavigate(Secondary) },
            navigateToFeatureForResult = { onNavigate(ExampleLegacyResultScreen) },
            receivedResult = result
        )
    }
}

