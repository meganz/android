package mega.privacy.android.app.presentation.meeting.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

/**
 * Navigation key for the free plan participants limit dialog
 *
 * @param callEndedDueToFreePlanLimits Whether the call ended due to free plan limits
 */
@Serializable
data class FreePlanParticipantsLimitNavKey(
    val callEndedDueToFreePlanLimits: Boolean = true,
) : DialogNavKey

/**
 * Navigation key for the upgrade to Pro plan bottom sheet
 */
@Serializable
data object UpgradeProPlanBottomSheetNavKey : NavKey

/**
 * Navigation key for the dialog prompting the user to update the app, shown when a call cannot be
 * joined because the installed app no longer supports the call protocol version
 */
@Serializable
data object ForceAppUpdateNavKey : DialogNavKey
