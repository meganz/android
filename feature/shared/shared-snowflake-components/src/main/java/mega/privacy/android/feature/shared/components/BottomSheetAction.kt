package mega.privacy.android.feature.shared.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

/**
 * Composable function to display a card for the Pro plan.
 */
@Composable
fun BottomSheetAction(
    iconPainter: Painter,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .semantics { testTagsAsResourceId = true }
        .testTag(TEST_TAG_BOTTOM_SHEET_ACTION)
        .height(56.dp)
        .padding(horizontal = 20.dp)
        .clickable(onClick = onClick)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
) {
    MegaIcon(
        modifier = Modifier
            .testTag(TEST_TAG_ICON)
            .size(24.dp),
        painter = iconPainter,
        contentDescription = "",
        tint = IconColor.Secondary,
    )
    MegaText(
        modifier = Modifier
            .testTag(TEST_TAG_NAME)
            .padding(start = 20.dp),
        text = name,
        maxLines = 1,
        style = AppTheme.typography.bodyLarge,
        textColor = TextColor.Primary,
    )
}

@CombinedThemePreviews
@Composable
private fun BottomSheetActionPreview() {
    AndroidThemeForPreviews {
        BottomSheetAction(
            iconPainter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ExternalLink),
            name = "Action Name",
            onClick = {},
        )
    }
}

/**
 * Test tag for BottomSheetAction component.
 */
const val TEST_TAG_BOTTOM_SHEET_ACTION = "bottom_sheet_action"

/**
 * Test tag for icon in BottomSheetAction component.
 */
const val TEST_TAG_ICON = "$TEST_TAG_BOTTOM_SHEET_ACTION:icon"

/**
 * Test tag for name in BottomSheetAction component.
 */
const val TEST_TAG_NAME = "$TEST_TAG_BOTTOM_SHEET_ACTION:name"