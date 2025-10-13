package mega.privacy.android.feature.example.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.destination.ExampleLegacyResultScreen


fun EntryProviderBuilder<NavKey>.exampleLegacyResultScreen(
    returnResult: (String, Int?) -> Unit,
    onResultHandled: (String) -> Unit,
) {
    entry<ExampleLegacyResultScreen> {
        LaunchedEffect(Unit) {
            onResultHandled(ExampleLegacyResultScreen.RESULT_KEY)
        }
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