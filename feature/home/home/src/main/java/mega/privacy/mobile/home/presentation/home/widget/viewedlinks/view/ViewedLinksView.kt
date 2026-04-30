package mega.privacy.mobile.home.presentation.home.widget.viewedlinks.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack

@Composable
internal fun ViewedLinkLoadingItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(VIEWED_LINK_LOADING_ITEM_TEST_TAG),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .shimmerEffect()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .shimmerEffect()
                .align(Alignment.CenterVertically)
        )

        MegaIcon(
            painter = rememberVectorPainter(
                IconPack.Medium.Thin.Outline.MoreVertical
            ),
            contentDescription = null,
            tint = IconColor.Primary,
        )
    }
}

internal const val VIEWED_LINK_LOADING_ITEM_TEST_TAG = "viewed_links:loading_item"