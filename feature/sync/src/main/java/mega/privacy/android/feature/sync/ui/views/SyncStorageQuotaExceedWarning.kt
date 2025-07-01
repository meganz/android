package mega.privacy.android.feature.sync.ui.views


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePhoneLandscapePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R

/**
 * Permission banner shown on top of sync screens
 */
@Composable
internal fun SyncStorageQuotaExceedWarning(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopWarningBanner(
        title = stringResource(id = R.string.sync_error_storage_over_quota_banner_title),
        actionButtonText = stringResource(id = R.string.account_storage_over_quota_inline_error_banner_upgrade_link),
        onActionButtonClick = onUpgradeClick,
        showCancelButton = false,
        body = null,
        modifier = modifier,
    )
}


@CombinedThemePreviews
@CombinedThemePhoneLandscapePreviews
@Composable
private fun SyncPromotionBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncStorageQuotaExceedWarning(
            onUpgradeClick = {},
        )
    }
}
