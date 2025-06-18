package mega.privacy.android.feature.example.presentation

import androidx.compose.runtime.Composable
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor

@Composable
fun ExampleFeatureView() {
    MegaText("This is an example feature view", textColor = TextColor.Primary)
}