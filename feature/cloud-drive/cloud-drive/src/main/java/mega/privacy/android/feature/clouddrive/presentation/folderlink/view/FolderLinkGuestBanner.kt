package mega.privacy.android.feature.clouddrive.presentation.folderlink.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.BrandFilledButtonM3XSmall
import mega.android.core.ui.components.button.PrimaryFilledButtonM3XSmall
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.SaveToMegaBannerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.SaveToMegaBannerCreateAccountButtonPressedEvent
import mega.privacy.mobile.analytics.event.SaveToMegaBannerLogInButtonPressedEvent

@Composable
internal fun FolderLinkGuestBanner(
    onCreateAccountClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        surfaceColor = SurfaceColor.Surface1,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(GUEST_BANNER_TAG),
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 8.dp
            )
        ) {
            Row(verticalAlignment = Alignment.Top) {
                MegaText(
                    text = stringResource(sharedR.string.folder_link_guest_banner_title),
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.W500
                    ),
                    modifier = Modifier
                        .weight(1f)
                )
                IconButton(
                    onClick = {
                        Analytics.tracker.trackEvent(SaveToMegaBannerCloseButtonPressedEvent)
                        onDismissClicked()
                    },
                    modifier = Modifier
                        .testTag(GUEST_BANNER_DISMISS_TAG)
                        .size(24.dp)
                        .wrapContentSize(unbounded = true, align = Alignment.Center)
                        .size(48.dp),
                ) {
                    MegaIcon(
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                        tint = IconColor.Primary,
                        contentDescription = stringResource(
                            sharedR.string.folder_link_guest_banner_dismiss_content_description
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MegaText(
                text = stringResource(sharedR.string.folder_link_guest_banner_description),
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandFilledButtonM3XSmall(
                    modifier = Modifier.testTag(GUEST_BANNER_CREATE_ACCOUNT_TAG),
                    text = stringResource(sharedR.string.folder_link_guest_banner_create_account),
                    onClick = {
                        Analytics.tracker.trackEvent(SaveToMegaBannerCreateAccountButtonPressedEvent)
                        onCreateAccountClicked()
                    },
                )
                PrimaryFilledButtonM3XSmall(
                    modifier = Modifier.testTag(GUEST_BANNER_LOGIN_TAG),
                    text = stringResource(sharedR.string.login_text),
                    onClick = {
                        Analytics.tracker.trackEvent(SaveToMegaBannerLogInButtonPressedEvent)
                        onLoginClicked()
                    },
                )
            }
        }
    }
}

internal const val GUEST_BANNER_TAG = "folder_link_screen:guest_banner"
internal const val GUEST_BANNER_DISMISS_TAG = "folder_link_screen:guest_banner_dismiss"
internal const val GUEST_BANNER_CREATE_ACCOUNT_TAG =
    "folder_link_screen:guest_banner_create_account"
internal const val GUEST_BANNER_LOGIN_TAG = "folder_link_screen:guest_banner_login"

@CombinedThemePreviews
@Composable
private fun FolderLinkGuestBannerPreview() {
    AndroidThemeForPreviews {
        FolderLinkGuestBanner(
            onCreateAccountClicked = {},
            onLoginClicked = {},
            onDismissClicked = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
