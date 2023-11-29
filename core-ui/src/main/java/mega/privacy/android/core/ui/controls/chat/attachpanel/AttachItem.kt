package mega.privacy.android.core.ui.controls.chat.attachpanel

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body4

/**
 * Attach item for attach panel in chat.
 *
 * @param iconId Icon resource id
 * @param itemName Item name
 * @param onItemClick Item click action
 * @param modifier [Modifier]
 */
@Composable
fun AttachItem(
    @DrawableRes iconId: Int,
    itemName: String,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .size(width = 64.dp, height = 66.dp)
        .clickable { onItemClick() },
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = MegaTheme.colors.button.secondary,
                shape = CircleShape
            )
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = itemName,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
                .testTag(TEST_TAG_ATTACH_ITEM_ICON),
            tint = MegaTheme.colors.icon.primary
        )
    }
    Text(
        text = itemName,
        style = MaterialTheme.typography.body4,
        modifier = Modifier.padding(top = 2.dp),
        color = MegaTheme.colors.text.primary
    )
}

@CombinedThemePreviews
@Composable
private fun AttachItemPreview() {
    AndroidThemeForPreviews {
        AttachItem(
            iconId = R.drawable.ic_menu,
            itemName = "Item",
            onItemClick = {}
        )
    }
}

internal const val TEST_TAG_ATTACH_ITEM_ICON = "chat_view:attach_panel:attach_icon"