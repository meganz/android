package mega.privacy.android.feature.example.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.values.TextColor

@Composable
fun ExampleHomeScreen(content: String) {
    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ColumnSurface(
            surfaceColor = SurfaceColor.PageBackground,
        ) {
            MegaText("Example Home Screen", textColor = TextColor.Primary)

            MegaText(content, textColor = TextColor.Accent)
        }
    }
}

@Composable
fun ExampleHomeScreen2(navigateToFeature: () -> Unit) {
    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MegaOutlinedButton(
            onClick = navigateToFeature,
            modifier = Modifier.padding(16.dp),
            text = "Navigate to Feature",
        )
    }
}