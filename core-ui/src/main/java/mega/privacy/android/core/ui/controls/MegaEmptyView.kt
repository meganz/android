package mega.privacy.android.core.ui.controls

import android.text.Spanned
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import mega.privacy.android.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.extensions.grey_300_grey_600
import mega.privacy.android.core.ui.theme.extensions.grey_300_white_alpha_087
import mega.privacy.android.core.ui.theme.extensions.grey_900_grey_100
import mega.privacy.android.core.ui.theme.extensions.light_grey_light_black

/**
 * Reusable EmptyView with Icon & Text
 * Pass imageVector using ImageVector.vectorResource(id = R.drawable.ic_xyz)
 * @param modifier
 * @param imageVector
 * @param text
 */
@Composable
fun MegaEmptyView(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    text: Spanned,
) {
    MegaEmptyView(modifier, text) {
        Icon(
            imageVector = imageVector,
            contentDescription = "Empty Icon",
            modifier = Modifier.padding(bottom = 30.dp),
            tint = getIconTint()
        )
    }
}

/**
 * Reusable EmptyView with Icon & Text
 * Pass imageBitmap using ImageBitmap.imageResource(id = R.drawable.ic_xyz)
 * @param modifier
 * @param imageBitmap
 * @param text
 */
@Composable
fun MegaEmptyView(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap,
    text: Spanned,
) {
    MegaEmptyView(modifier, text) {
        Icon(
            bitmap = imageBitmap,
            contentDescription = "Empty Icon",
            modifier = Modifier.padding(bottom = 30.dp),
            tint = getIconTint()
        )
    }
}

/**
 * Reusable EmptyView with Icon & Text
 * Pass imagePainter using painterResource(id = R.drawable.ic_xyz)
 * @param modifier
 * @param imagePainter
 * @param text
 */
@Composable
fun MegaEmptyView(
    modifier: Modifier = Modifier,
    imagePainter: Painter,
    text: Spanned,
) {
    MegaEmptyView(modifier, text) {
        Icon(
            painter = imagePainter,
            contentDescription = "Empty Icon",
            modifier = Modifier.padding(bottom = 30.dp),
            tint = getIconTint()
        )
    }
}

@Composable
private fun MegaEmptyView(modifier: Modifier, text: Spanned, Icon: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon()
        AndroidView(
            factory = { context ->
                TextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                }
            },
            update = { it.text = text }
        )
    }
}

@Composable
private fun getIconTint(): Color {
    return if (MaterialTheme.colors.isLight) {
        Color.Unspecified
    } else {
        Color.Gray
    }
}

/**
 * Reusable EmptyView with Icon & Text
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
        Icon(
            painter = imagePainter,
            contentDescription = "Empty Icon",
            modifier = Modifier.padding(bottom = 30.dp),
            tint = getIconTint()
        )
    }
}

@Composable
private fun MegaEmptyView(modifier: Modifier, text: String, Icon: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon()
        MegaSpannedText(
            value = text, baseStyle = MaterialTheme.typography.subtitle1.copy(
                fontSize = 16.sp,
            ),
            styles = mapOf(
                SpanIndicator('A') to SpanStyle(color = MaterialTheme.colors.grey_900_grey_100),
                SpanIndicator('B') to SpanStyle(color = MaterialTheme.colors.grey_300_grey_600)
            ),
            color = MaterialTheme.colors.grey_300_grey_600
        )
    }
}

/**
 * Empty view for search
 * @param text empty text to be displayed
 * @param imagePainter [Painter] resource id in form of painter
 * @param modifier [Modifier]
 */
@Composable
fun MegaEmptyViewForSearch(
    text: String,
    imagePainter: Painter,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = imagePainter,
            contentDescription = "Empty Icon",
            modifier = Modifier.padding(bottom = 30.dp),
            colorFilter = ColorFilter.tint(color = MaterialTheme.colors.light_grey_light_black)
        )
        MegaSpannedText(
            value = text, baseStyle = MaterialTheme.typography.subtitle1.copy(
                fontSize = 16.sp,
            ),
            styles = mapOf(
                SpanIndicator('A') to SpanStyle(color = MaterialTheme.colors.grey_900_grey_100),
                SpanIndicator('B') to SpanStyle(color = MaterialTheme.colors.grey_300_grey_600)
            ),
            color = MaterialTheme.colors.grey_300_white_alpha_087
        )
    }
}

