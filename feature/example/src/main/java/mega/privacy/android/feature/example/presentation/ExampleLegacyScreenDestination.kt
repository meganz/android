package mega.privacy.android.feature.example.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.navigation.destination.ExampleLegacyScreen


fun NavGraphBuilder.exampleLegacyScreen(removeDestination: () -> Unit) {
    composable<ExampleLegacyScreen> {
        val content = it.toRoute<ExampleLegacyScreen>().content
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = ExampleLegacyActivity.getIntent(context, content)
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }

    }
}