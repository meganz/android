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
