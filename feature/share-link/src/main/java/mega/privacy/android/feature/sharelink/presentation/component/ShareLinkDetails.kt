package mega.privacy.android.feature.sharelink.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The read-only "Share link details" card
 *
 * @param link The public share link shown to the user.
 * @param onCopyLink Invoked when the link's copy icon is tapped.
 * @param modifier Modifier for the card.
 */
@Composable
fun ShareLinkDetails(
    link: String,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        surfaceColor = SurfaceColor.Surface1,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .testTag(SHARE_LINK_DETAILS_TAG),
    ) {
        ShareLinkDetailRow(
            modifier = Modifier.padding(16.dp),
            label = stringResource(sharedR.string.album_get_link_link_section_title),
            value = link,
            onCopy = onCopyLink,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ShareLinkDetailsPreview() {
    AndroidThemeForPreviews {
        ShareLinkDetails(
            link = "https://mega.nz/file/abc123#decryptionKey",
            onCopyLink = {},
        )
    }
}

internal const val SHARE_LINK_DETAILS_TAG = "share_link_details:card"
