package mega.privacy.android.shared.account.inactivity.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.banner.InlineErrorBanner
import mega.android.core.ui.components.text.SpannableText
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.shared.account.inactivity.InactivityBannerViewModel
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.InactivityPurgeBannerLearnMoreButtonPressedEvent

/**
 * The account inactivity banner, shown while a purge event is pending and [enabled].
 *
 * @param onLearnMoreClicked opens the help article; receives [ACCOUNT_INACTIVITY_LEARN_MORE_URI]
 * @param enabled false hides the banner without dismissing it, for example in selection mode
 */
@Composable
fun AccountInactivityBanner(
    onLearnMoreClicked: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    viewModel: InactivityBannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val months = uiState.inactivityMonths

    if (enabled && months != null) {
        InlineErrorBanner(
            modifier = modifier.fillMaxWidth(),
            showCancelButton = true,
            title = SpannableText(
                text = stringResource(sharedR.string.cloud_drive_inactivity_banner_title)
            ),
            body = SpannableText(
                text = pluralStringResource(
                    sharedR.plurals.cloud_drive_inactivity_banner_message,
                    months,
                    months,
                )
            ),
            actionButtonText = stringResource(sharedR.string.general_learn_more),
            onActionButtonClick = {
                Analytics.tracker.trackEvent(InactivityPurgeBannerLearnMoreButtonPressedEvent)
                onLearnMoreClicked(ACCOUNT_INACTIVITY_LEARN_MORE_URI)
            },
            onCancelButtonClick = {
                uiState.purgeTimestamp?.let(viewModel::dismiss)
            },
        )
    }
}

/**
 * Help article explaining how data may be deleted by MEGA due to account inactivity
 */
const val ACCOUNT_INACTIVITY_LEARN_MORE_URI =
    "https://help.mega.io/files-folders/restore-delete/data-deleted-by-mega"
