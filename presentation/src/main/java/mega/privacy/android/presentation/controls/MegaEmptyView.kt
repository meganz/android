package mega.privacy.android.presentation.controls

import android.text.Spanned
import android.widget.TextView
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Reusable EmptyView with Icon & Text
 * Pass imageVector using ImageVector.vectorResource(id = R.drawable.ic_xyz)
 * @param imageVector
 * @param text
 */
@Composable
fun MegaEmptyView(
    imageVector: ImageVector,
    text: Spanned,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = "Empty Icon",
            modifier = Modifier.padding(bottom = 30.dp),
            tint = if (!MaterialTheme.colors.isLight) {
                Color.Gray
            } else {
                Color.Unspecified
            }
        )

        AndroidView(
            factory = { context -> TextView(context) },
            update = { it.text = text }
        )
    }
}

/**
 * Reusable EmptyView with Icon & Text
 * Pass imageBitmap using ImageBitmap.imageResource(id = R.drawable.ic_xyz)
 * @param imageBitmap
 * @param text
 */
@Composable
fun MegaEmptyView(
    imageBitmap: ImageBitmap,
    text: Spanned,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            bitmap = imageBitmap,
            contentDescription = "Empty Icon",
            modifier = Modifier.padding(bottom = 30.dp),
            tint = if (!MaterialTheme.colors.isLight) {
                Color.Gray
            } else {
                Color.Unspecified
            }
        )

        AndroidView(
            factory = { context -> TextView(context) },
            update = { it.text = text }
        )
    }
}

/**
 * Reusable EmptyView with Icon & Text
 * Pass imagePainter using painterResource(id = R.drawable.ic_xyz)
 * @param imagePainter
 * @param text
 */
@Composable
fun MegaEmptyView(
    imagePainter: Painter,
    text: Spanned,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = imagePainter,
            contentDescription = "Empty Icon",
            modifier = Modifier.padding(bottom = 30.dp),
            tint = if (!MaterialTheme.colors.isLight) {
                Color.Gray
            } else {
                Color.Unspecified
            }
        )

        AndroidView(
            factory = { context -> TextView(context) },
            update = { it.text = text }
        )
    }
}

