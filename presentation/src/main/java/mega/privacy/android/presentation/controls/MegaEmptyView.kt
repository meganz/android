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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Reusable EmptyView with Icon & Text
 * Support for Icons in below formats:
 * 1. Vector (isVectorImage true): <vector>
 * 2. Bitmap (isBimapImage true): <bitmap>
 * 3. PNG (isVectorImage false, isBimapImage false)
 */
@Composable
fun MegaEmptyView(
    imageResId: Int,
    isVectorImage: Boolean,
    isBimapImage: Boolean,
    text: Spanned,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isVectorImage) {
            Icon(
                imageVector = ImageVector.vectorResource(id = imageResId),
                contentDescription = "Empty",
                modifier = Modifier.padding(bottom = 30.dp),
                tint = if (!MaterialTheme.colors.isLight) {
                    Color.Gray
                } else {
                    Color.Unspecified
                }
            )
        } else if (isBimapImage) {
            Icon(
                bitmap = ImageBitmap.imageResource(id = imageResId),
                contentDescription = "Empty",
                modifier = Modifier.padding(bottom = 30.dp),
                tint = if (!MaterialTheme.colors.isLight) {
                    Color.Gray
                } else {
                    Color.Unspecified
                }
            )
        } else {
            Icon(
                painter = painterResource(id = imageResId),
                contentDescription = "Empty",
                modifier = Modifier.padding(bottom = 30.dp),
                tint = if (!MaterialTheme.colors.isLight) {
                    Color.Gray
                } else {
                    Color.Unspecified
                }
            )
        }

        AndroidView(
            factory = { context -> TextView(context) },
            update = { it.text = text }
        )
    }
}
