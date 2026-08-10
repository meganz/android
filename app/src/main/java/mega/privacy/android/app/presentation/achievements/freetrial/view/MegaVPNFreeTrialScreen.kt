package mega.privacy.android.app.presentation.achievements.freetrial.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.data.extensions.toUnitString
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.app.R

@Composable
internal fun MegaVPNFreeTrialScreen(
    isReceivedAward: Boolean = false,
    storageAmount: Long = 0,
    awardStorageAmount: Long = 0,
    durationInDays: Int,
    awardDaysLeft: Int?,
    onInstallButtonClicked: () -> Unit = {},
) {
    val isPermanent = if (isReceivedAward) awardDaysLeft == null else durationInDays == 0
    FreeTrialView(
        icon = iconPackR.drawable.ic_mega_vpn_free_trial,
        freeTrialText =
            when {
                isPermanent && isReceivedAward -> stringResource(
                    sharedR.string.figures_storage_achievements_awarded_text_permanent,
                    awardStorageAmount.toUnitString(LocalContext.current)
                )

                isPermanent && !isReceivedAward -> stringResource(
                    sharedR.string.mega_vpn_achievement_awarded_storage_text_permanent,
                    storageAmount.toUnitString(LocalContext.current)
                )

                isReceivedAward && awardDaysLeft != null && awardDaysLeft == 0 ->
                    stringResource(R.string.expired_label)

                isReceivedAward && awardDaysLeft != null && awardDaysLeft > 0 ->
                    LocalResources.current.getQuantityString(
                        sharedR.plurals.trial_awarded_achievement_days_left_detail_title,
                        awardDaysLeft,
                        awardDaysLeft
                    )

                else -> stringResource(
                    sharedR.string.figures_storage_achievements_text,
                    storageAmount.toUnitString(LocalContext.current),
                    durationInDays
                )
            },
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
        installButtonClicked = onInstallButtonClicked,
        isExpired = isReceivedAward && awardDaysLeft != null && awardDaysLeft == 0,
        isPermanent = isPermanent
    )
}