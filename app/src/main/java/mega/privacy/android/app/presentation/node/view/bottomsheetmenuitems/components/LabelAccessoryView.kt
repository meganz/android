package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

/**
 * Label view
 */
@Composable
fun LabelAccessoryView(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = color)
        Spacer(modifier = Modifier.size(8.dp))
        Circle(color = color)
    }
}

@Composable
private fun Circle(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(8.dp),
        onDraw = {
            drawCircle(color = color)
        },
    )
}

@CombinedThemePreviews
@Composable
private fun LabelAccessoryViewPreview() {
    LabelAccessoryView(text = "Label", color = Color.Magenta)
}