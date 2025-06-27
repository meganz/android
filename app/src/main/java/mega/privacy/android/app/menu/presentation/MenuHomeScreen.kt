package mega.privacy.android.app.menu.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.values.TextColor

@Composable
fun MenuHomeScreen(
    navigateToFeature: (Any) -> Unit,
) {
    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ColumnSurface(
            surfaceColor = SurfaceColor.PageBackground,
        ) {
            MegaText("Menu Home Screen", textColor = TextColor.Primary)
        }
    }
}