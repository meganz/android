package mega.privacy.android.shared.original.core.ui.controls.lists

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * A list view that shows bullet points.
 *
 * @param items List of strings to show as bullet points
 * @param modifier Modifier
 * @param textStyle Text style
 * @param textColor Text color
 * @param spacing Spacing between bullet points
 */
@Composable
fun BulletListView(
    items: List<String>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.body1,
    textColor: TextColor = TextColor.Secondary,
    spacing: Dp = 16.dp,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        items.forEach { point ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing),
                verticalAlignment = Alignment.Top,
            ) {
                MegaText(
                    text = "•",
                    textColor = textColor,
                    style = textStyle.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
                MegaText(
                    text = point,
                    textColor = textColor,
                    style = textStyle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
internal fun BulletListViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        BulletListView(
            items = listOf("Item 1", "Item 2", "Item 3"),
            modifier = Modifier.padding(16.dp),
            textStyle = MaterialTheme.typography.body1,
            textColor = TextColor.Secondary,
            spacing = 8.dp
        )
    }
}