package mega.privacy.android.core.nodecomponents.sheet.upload

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.theme.values.IconColor

/**
 * Reusable item for upload options bottom sheet.
 */
@Composable
internal fun UploadOptionItem(
    text: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OneLineListItem(
        modifier = modifier.testTag(testTag),
        text = text,
        leadingElement = {
            MegaIcon(
                painter = rememberVectorPainter(icon),
                contentDescription = text,
                tint = IconColor.Primary,
                modifier = Modifier.align(Alignment.Center)
            )
        },
        onClickListener = onClick,
    )
} 