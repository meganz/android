package mega.privacy.android.legacy.core.ui.controls.modifier

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.core.ui.theme.extensions.grey_050_grey_900

/**
 * Loading effect using compose
 */
fun Modifier.skeletonEffect() = composed {
    Modifier.placeholder(
        color = MaterialTheme.colors.grey_020_grey_800,
        shape = RoundedCornerShape(4.dp),
        highlight = PlaceholderHighlight.fade(MaterialTheme.colors.grey_050_grey_900),
        visible = true,
    )
}