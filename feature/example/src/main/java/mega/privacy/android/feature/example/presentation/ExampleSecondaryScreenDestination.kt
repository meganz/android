package mega.privacy.android.feature.example.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.navigation.destination.Secondary


fun NavGraphBuilder.exampleSecondaryScreen(
    onBack: () -> Unit,
) {
    composable<Secondary> {
        ExampleSecondaryScreen(
            onBack = onBack,
        )
    }
}