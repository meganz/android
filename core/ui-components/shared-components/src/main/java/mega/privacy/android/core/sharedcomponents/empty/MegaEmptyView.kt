package mega.privacy.android.core.sharedcomponents.empty

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.SpannedText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.theme.values.TextColor

/**
 * Material 3 Reusable EmptyView with Icon & Text
 * Pass imageVector using ImageVector.vectorResource(id = R.drawable.ic_xyz)
 * @param modifier
 * @param imageVector
 * @param text
 */
@Composable
fun MegaEmptyView(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    text: String,
) {
    MegaEmptyView(modifier, text) {
        Image(
            imageVector = imageVector,
            contentDescription = "Empty Icon",
            modifier = Modifier.width(120.dp)
        )
    }
}

/**
 * Material 3 Reusable EmptyView with Icon & Text
 * Pass imageBitmap using ImageBitmap.imageResource(id = R.drawable.ic_xyz)
 * @param modifier
 * @param imageBitmap
 * @param text
 */
@Composable
fun MegaEmptyView(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap,
    text: String,
) {
    MegaEmptyView(modifier, text) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Empty Icon",
            modifier = Modifier.width(120.dp)
        )
    }
}

/**
 * Material 3 Reusable EmptyView with Image & Text
 * Pass imagePainter using painterResource(id = R.drawable.ic_xyz)
 * @param modifier
 * @param imagePainter
 * @param text
 */
@Composable
fun MegaEmptyViewWithImage(
    modifier: Modifier = Modifier,
    imagePainter: Painter,
    text: String,
) {
    MegaEmptyView(modifier, text) {
        Image(
            painter = imagePainter,
            contentDescription = "Empty Image",
            modifier = Modifier.width(120.dp)
        )
    }
}

/**
 * Material 3 Reusable EmptyView with Icon & Text
 * Pass imageVector using ImageVector.vectorResource(id = R.drawable.ic_xyz)
 * @param modifier [Modifier]
 * @param text with string
 * @param imagePainter
 */
@Composable
fun MegaEmptyView(
    text: String,
    imagePainter: Painter,
    modifier: Modifier = Modifier,
) {
    MegaEmptyView(modifier, text) {
        Image(
            painter = imagePainter,
            contentDescription = "Empty Icon",
            modifier = Modifier.width(120.dp)
        )
    }
}

@Composable
private fun MegaEmptyView(modifier: Modifier, text: String, icon: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(modifier = Modifier.height(6.dp))
        val emptySpanStyle = MegaSpanStyle.DefaultColorStyle(SpanStyle())
        SpannedText(
            value = text,
            baseTextColor = TextColor.Secondary,
            baseStyle = MaterialTheme.typography.bodyLarge,
            spanStyles = mapOf(
                SpanIndicator('A') to emptySpanStyle,
                SpanIndicator('B') to emptySpanStyle
            ),
        )
        Spacer(modifier = Modifier.height(56.dp))
    }
}