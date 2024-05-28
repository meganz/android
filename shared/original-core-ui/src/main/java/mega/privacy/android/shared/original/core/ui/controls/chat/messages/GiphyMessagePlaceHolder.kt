package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme


/**
 * Place holder view when loading a Giphy message.
 *
 * @param width
 * @param height
 * @param content the content in the center of the placeholder.
 */
@Composable
fun GiphyMessagePlaceHolder(width: Int, height: Int, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = width.dp, height = height.dp)
            .border(
                width = 1.dp,
                color = MegaOriginalTheme.colors.border.subtle,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(color = MegaOriginalTheme.colors.background.surface2),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@CombinedThemePreviews
@Composable
private fun GiphyMessageLoadingPlaceHolderPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        GiphyMessagePlaceHolder(width = 256, height = 200) {
            MegaCircularProgressIndicator(modifier = Modifier.size(48.dp))
        }
    }
}

@CombinedThemePreviews
@Composable
private fun GiphyMessageStaticPlaceHolderPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        GiphyMessagePlaceHolder(width = 256, height = 200) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle),
                contentDescription = null
            )
        }
    }
}

