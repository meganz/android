package mega.privacy.android.feature.example.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

@OptIn(ExperimentalMaterial3Api::class)
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

            var openBottomSheet by remember { mutableStateOf(false) }
            MegaText("Example Home Screen", textColor = TextColor.Primary)

            MegaText(content, textColor = TextColor.Accent)

            MegaOutlinedButton(
                onClick = { openBottomSheet = openBottomSheet.not() },
                modifier = Modifier.padding(16.dp),
                text = "Toggle bottom sheet",
            )

            if (openBottomSheet) {
                MegaModalBottomSheet(
                    sheetState = rememberModalBottomSheetState(),
                    bottomSheetBackground = MegaModalBottomSheetBackground.PageBackground,
                    onDismissRequest = { openBottomSheet = false },
                ) {
                    (1..15).forEach { index ->
                        Row() {
                            MegaIcon(
                                modifier = Modifier.padding(16.dp),
                                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Camera),
                                contentDescription = "Icon $index",
                                tint = IconColor.Primary
                            )
                            MegaText(
                                text = "Item $index",
                                modifier = Modifier.padding(16.dp),
                                textColor = TextColor.Primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExampleHomeScreen2(
    logout: () -> Unit,
    navigateToFeature: () -> Unit,
    navigateToFeatureForResult: () -> Unit,
    receivedResult: Int?,
) {
    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MegaOutlinedButton(
            onClick = logout,
            modifier = Modifier.padding(16.dp),
            text = "Logout",
        )
        MegaOutlinedButton(
            onClick = navigateToFeature,
            modifier = Modifier.padding(16.dp),
            text = "Navigate to Feature",
        )
        MegaOutlinedButton(
            onClick = navigateToFeatureForResult,
            modifier = Modifier.padding(16.dp),
            text = "Navigate to Feature for result",
        )
        receivedResult?.let {
            MegaText(
                text = "Received result: $it",
                textColor = TextColor.Primary
            )
        }
    }
}