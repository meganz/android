package mega.privacy.android.app.presentation.achievements.freetrial.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.data.extensions.toUnitString
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun MegaVPNFreeTrialScreen(
    isReceivedAward: Boolean = false,
    storageAmount: Long = 0,
    awardStorageAmount: Long = 0,
    onInstallButtonClicked: () -> Unit = {},
) {
    FreeTrialView(
        icon = iconPackR.drawable.ic_mega_vpn_free_trial,
        freeTrialText = stringResource(
            sharedR.string.text_start_mega_vpn_free_trial,
            storageAmount.toUnitString(LocalContext.current)
        ),
        installButtonText = sharedR.string.button_text_install_mega_vpn,
        howItWorksText = if (isReceivedAward) {
            stringResource(
                sharedR.string.text_received_mega_vpn_free_trial,
                awardStorageAmount.toUnitString(LocalContext.current)
            )
        } else {
            stringResource(sharedR.string.text_how_it_works_mega_vpn_free_trial)
        },
        isReceivedAward = isReceivedAward,
        installButtonClicked = onInstallButtonClicked
    )
}