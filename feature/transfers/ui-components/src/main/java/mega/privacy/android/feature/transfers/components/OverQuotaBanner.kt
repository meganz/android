package mega.privacy.android.feature.transfers.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Banner to be show in case of over quota
 * @param onUpgradeClick it should redirect the user to the upgrade account screen
 * @param onCancelButtonClick it should hide the banner
 * @param modifier
 */
@Composable
fun OverQuotaBanner(
    onUpgradeClick: () -> Unit,
    onCancelButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) =
    TopWarningBanner(
        modifier = modifier,
        body = null,
        title = stringResource(sharedR.string.transfers_over_quota_banner_title),
        actionButtonText = stringResource(sharedR.string.transfers_over_quota_banner_action_button),
        showCancelButton = true,
        onActionButtonClick = onUpgradeClick,
        onCancelButtonClick = onCancelButtonClick
    )

@CombinedThemePreviews
@Composable
private fun OverQuotaBannerPreview() {
    AndroidThemeForPreviews {
        OverQuotaBanner({}, {})
    }
}