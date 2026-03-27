package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

/**
 * Floating search results toolbar.
 *
 * Shows match counter ("1 of 4") and prev/next navigation.
 * Position at bottom-center via Modifier.align(Alignment.BottomCenter) from parent Box.
 *
 * Uses `Card` with `CircleShape` matching `MegaFloatingToolbar` styling.
 * Includes `imePadding()` to stay above the keyboard when it's open.
 *
 * @param label formatted label, e.g. "1 of 4" or "No results"
 * @param onPrev Callback when previous match button is clicked
 * @param onNext Callback when next match button is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun PdfSearchResultsBar(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(bottom = 16.dp),
    ) {
        CardSurface(
            modifier = Modifier.align(Alignment.BottomCenter),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            surfaceColor = SurfaceColor.PageBackground
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MegaText(
                    text = label,
                    textColor = TextColor.Primary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(40.dp)
                ) {
                    MegaIcon(
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronLeft),
                        tint = IconColor.Primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(40.dp)
                ) {
                    MegaIcon(
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                        tint = IconColor.Primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PdfSearchResultsBarPreviewWithMatches() {
    AndroidThemeForPreviews {
        PdfSearchResultsBar(
            label = "1 of 4",
            onPrev = {},
            onNext = {},
        )
    }
}
