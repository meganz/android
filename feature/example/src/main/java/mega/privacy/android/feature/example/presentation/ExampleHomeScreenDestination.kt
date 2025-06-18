package mega.privacy.android.feature.example.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object HomeScreen

@Serializable
data object HomeScreen2

fun NavGraphBuilder.exampleHomeScreen() {
    composable<HomeScreen> {
        ExampleHomeScreen("1")
    }
}

fun NavGraphBuilder.otherExampleHomeScreen(onNavigate: (Any) -> Unit) {
    composable<HomeScreen2> {
        ExampleHomeScreen2(
            navigateToFeature = { onNavigate(Secondary) },
        )
    }
}

