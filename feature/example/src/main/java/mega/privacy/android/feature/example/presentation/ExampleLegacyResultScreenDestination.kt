package mega.privacy.android.feature.example.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.navigation.destination.ExampleLegacyResultScreen


fun NavGraphBuilder.exampleLegacyResultScreen(returnResult: (String, Int?) -> Unit) {
    composable<ExampleLegacyResultScreen> {

        val legacyActivityLauncher = rememberLauncherForActivityResult(
            contract = ExampleLegacyResultActivity.Companion
        ) { result: Int? ->
            returnResult(ExampleLegacyResultScreen.RESULT_KEY, result)
        }

        LaunchedEffect(Unit) {
            legacyActivityLauncher.launch(Unit)
        }

    }
}