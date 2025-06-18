package mega.privacy.android.feature.example.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object Secondary

fun NavGraphBuilder.exampleSecondaryScreen(
    onBack: () -> Unit,
) {
    composable<Secondary> {
        ExampleSecondaryScreen(
            onBack = onBack,
        )
    }
}