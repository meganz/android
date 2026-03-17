package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.compose.material.navigation.bottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.UpgradeProPlanBottomSheet

internal fun NavGraphBuilder.upgradeToProPlanModal(
    closeBottomSheets: () -> Unit,
) {
    bottomSheet(route = "upgradeToProPlan") {
        UpgradeProPlanBottomSheet(
            hideSheet = { closeBottomSheets() },
        )
    }
}

internal fun NavHostController.navigateToUpgradeToProPlanModal(navOptions: NavOptions? = null) {
    navigate("upgradeToProPlan", navOptions)
}