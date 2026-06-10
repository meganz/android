package mega.privacy.android.feature.contact.group.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Shimmer skeleton shown while the contact groups are loading. Mirrors the layout of a
 * group row (a circular avatar placeholder and a title-bar placeholder).
 *
 * @param modifier
 */
@Composable
internal fun ContactGroupsLoadingView(modifier: Modifier = Modifier) {
    Column(modifier = modifier.testTag(CONTACT_GROUPS_LOADING_TAG)) {
        repeat(SKELETON_ROW_COUNT) { index ->
            ContactGroupLoadingRow(titleWidthFraction = SKELETON_TITLE_WIDTHS[index % SKELETON_TITLE_WIDTHS.size])
        }
    }
}

@Composable
private fun ContactGroupLoadingRow(titleWidthFraction: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .shimmerEffect(CircleShape),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(titleWidthFraction)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(RoundedCornerShape(4.dp)),
        )
    }
}

private const val SKELETON_ROW_COUNT = 8
private val SKELETON_TITLE_WIDTHS = listOf(0.6f, 0.4f, 0.5f, 0.35f)

internal const val CONTACT_GROUPS_LOADING_TAG = "contact_groups_screen:loading"

@CombinedThemePreviews
@Composable
private fun ContactGroupsLoadingViewPreview() {
    AndroidThemeForPreviews {
        ContactGroupsLoadingView()
    }
}
