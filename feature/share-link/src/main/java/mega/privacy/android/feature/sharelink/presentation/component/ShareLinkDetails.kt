package mega.privacy.android.feature.sharelink.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * The read-only "Share link details" card group.
 *
 * Shows the link card and, when [key] is non-null (the link and key are shared separately), a
 * second card with the decryption key and its own copy action.
 *
 * @param link The public share link shown to the user.
 * @param onCopyLink Invoked when the link's copy icon is tapped.
 * @param modifier Modifier for the card group.
 * @param key The decryption key shown in a separate card, or null when the key is part of the link.
 * @param onCopyKey Invoked when the key's copy icon is tapped.
 */
@Composable
fun ShareLinkDetails(
    link: String,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier,
    key: String? = null,
    onCopyKey: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BoxSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
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
        if (key != null) {
            BoxSurface(
                surfaceColor = SurfaceColor.Surface1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .testTag(SHARE_LINK_KEY_DETAILS_TAG),
            ) {
                ShareLinkDetailRow(
                    modifier = Modifier.padding(16.dp),
                    label = stringResource(sharedR.string.album_get_link_decryption_key_section_title),
                    value = key,
                    onCopy = onCopyKey,
                    copyTestTag = SHARE_LINK_KEY_COPY_TAG,
                )
            }
        }
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

@CombinedThemePreviews
@Composable
private fun ShareLinkDetailsSeparateKeyPreview() {
    AndroidThemeForPreviews {
        ShareLinkDetails(
            link = "https://mega.nz/file/abc123",
            onCopyLink = {},
            key = "decryptionKey",
            onCopyKey = {},
        )
    }
}

internal const val SHARE_LINK_DETAILS_TAG = "share_link_details:card"
internal const val SHARE_LINK_KEY_DETAILS_TAG = "share_link_details:key_card"
internal const val SHARE_LINK_KEY_COPY_TAG = "share_link_details:key_copy"
