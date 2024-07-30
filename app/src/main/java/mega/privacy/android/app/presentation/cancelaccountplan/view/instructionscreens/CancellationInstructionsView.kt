package mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.resources.R

/**
 * Composable function to display the instructions to cancel the subscription
 */
@Composable
internal fun CancellationInstructionsView(
    instructionsType: CancellationInstructionsType?,
    onMegaUrlClicked: (url: String) -> Unit,
    isAccountReactivationNeeded: Boolean,
    onCancelSubsFromOtherDeviceClicked: (url: String) -> Unit,
    onBackPressed: () -> Unit,
) {

    MegaScaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                onNavigationPressed = onBackPressed,
                elevation = 0.dp,
                title = stringResource(id = if (isAccountReactivationNeeded) R.string.account_cancellation_instructions_reactivate_subscription_title else R.string.account_cancellation_instructions_cancel_subscription_title),
            )
        }
    ) {
        when (instructionsType) {
            CancellationInstructionsType.AppStore -> AppleInstructionsView(
                onCancelSubsFromOtherDeviceClicked
            )

            CancellationInstructionsType.WebClient -> {
                if (isAccountReactivationNeeded) {
                    WebReactivationInstructionsView(onMegaUrlClicked = onMegaUrlClicked)
                } else {
                    WebCancellationInstructionsView(onMegaUrlClicked = onMegaUrlClicked)
                }
            }

            else -> {
                //do nothing
            }
        }
    }

}


@CombinedThemePreviews
@Composable
private fun CancelSubscriptionViewViaAppStore(
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CancellationInstructionsView(
            instructionsType = CancellationInstructionsType.AppStore,
            onMegaUrlClicked = {},
            onCancelSubsFromOtherDeviceClicked = {},
            onBackPressed = {},
            isAccountReactivationNeeded = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CancelSubscriptionViewViaWebclientPreview(
    @PreviewParameter(BooleanProvider::class) isAccountExpired: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CancellationInstructionsView(
            instructionsType = CancellationInstructionsType.WebClient,
            onMegaUrlClicked = {},
            onCancelSubsFromOtherDeviceClicked = {},
            onBackPressed = {},
            isAccountReactivationNeeded = isAccountExpired
        )
    }
}

