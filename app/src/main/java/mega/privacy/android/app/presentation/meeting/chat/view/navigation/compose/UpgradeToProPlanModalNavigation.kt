package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.UpgradeProPlanBottomSheet

@OptIn(ExperimentalMaterialNavigationApi::class)
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