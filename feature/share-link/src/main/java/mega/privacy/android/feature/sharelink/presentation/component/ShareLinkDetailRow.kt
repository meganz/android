package mega.privacy.android.feature.sharelink.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * A single read-only share-link detail field: a [label] above its [value], with a trailing
 * copy action.
 *
 * @param label The field label shown above the value (e.g. "Link", "Decryption key").
 * @param value The read-only value shown to the user (truncated to a single line).
 * @param onCopy Invoked when the trailing copy icon is tapped.
 * @param modifier Modifier for the row.
 * @param copyContentDescription Accessibility description for the copy icon.
 * @param copyTestTag Test tag for the trailing copy icon, so callers rendering more than one row
 * can target each copy action distinctly.
 */
@Composable
fun ShareLinkDetailRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    copyContentDescription: String = stringResource(sharedR.string.general_copy),
    copyTestTag: String = SHARE_LINK_DETAIL_ROW_COPY_TAG,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MegaText(
            text = label,
            textColor = TextColor.Primary,
            style = AppTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MegaText(
                modifier = Modifier.weight(1f),
                text = value,
                textColor = TextColor.Primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = AppTheme.typography.bodyLarge,
            )
            MegaIcon(
                modifier = Modifier
                    .size(24.dp)
                    .testTag(copyTestTag)
                    .clickable(onClick = onCopy),
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Copy01),
                tint = IconColor.Primary,
                contentDescription = copyContentDescription,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ShareLinkDetailRowPreview() {
    AndroidThemeForPreviews {
        ShareLinkDetailRow(
            modifier = Modifier.padding(16.dp),
            label = "Link",
            value = "https://mega.nz/file/abc123#decryptionKey",
            onCopy = {},
        )
    }
}

internal const val SHARE_LINK_DETAIL_ROW_COPY_TAG = "share_link_detail_row:copy"
