package mega.privacy.android.shared.original.core.ui.controls.ads

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Item to show the benefits of the Ads Free.
 */
@Composable
fun AdsFreeItem(
    title: String,
    desc: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(id = icon),
            tint = MegaOriginalTheme.colors.icon.secondary,
            contentDescription = "Ads Free Icon"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MegaText(
                modifier = Modifier.testTag(ADS_FREE_ITEM_TITLE_TEST_TAG),
                text = title,
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.subtitle2medium,
            )

            MegaText(
                modifier = Modifier.testTag(ADS_FREE_ITEM_DESC_TEST_TAG),
                text = desc,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun AdsFreeItemPreview() {
    OriginalTempTheme(isSystemInDarkTheme()) {
        AdsFreeItem(
            title = "Enjoy an ad-free experience",
            desc = "Upgrade to a Pro plan for lots of storage and more. Our Pro plans start at just €4.99 a month.",
            icon = R.drawable.ic_camera_rotate,
        )
    }
}

/**
 * Test tag for the title of the Ads Free Item.
 */
const val ADS_FREE_ITEM_TITLE_TEST_TAG = "ads_free_item:title"

/**
 * Test tag for the description of the Ads Free Item.
 */
const val ADS_FREE_ITEM_DESC_TEST_TAG = "ads_free_item:desc"