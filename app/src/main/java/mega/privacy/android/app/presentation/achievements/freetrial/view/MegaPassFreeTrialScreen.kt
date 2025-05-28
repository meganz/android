package mega.privacy.android.app.presentation.achievements.freetrial.view

import androidx.compose.runtime.Composable
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun MegaPassFreeTrialScreen(
    isReceivedAward: Boolean = false,
    onInstallButtonClicked: () -> Unit = {}
) {
    FreeTrialView(
        icon = iconPackR.drawable.ic_mega_pass_free_trial,
        freeTrialText = sharedR.string.text_start_mega_pass_free_trial,
        installButtonText = sharedR.string.button_text_install_mega_pass,
        howItWorksText = if (isReceivedAward) {
            sharedR.string.text_received_mega_pass_free_trial
        } else {
            sharedR.string.text_how_it_works_mega_pass_free_trial
        },
        isReceivedAward = isReceivedAward,
        installButtonClicked = onInstallButtonClicked
    )
}