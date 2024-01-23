package mega.privacy.android.app.presentation.search.navigation

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.shared.theme.MegaAppTheme


internal fun NavGraphBuilder.overQuotaDialogNavigation(
    navHostController: NavHostController,
) {

    dialog(
        route = searchOverQuotaDialog.plus("/{${searchOverQuotaDialogArgumentOverQuota}}"),
        arguments = listOf(
            navArgument(searchOverQuotaDialogArgumentOverQuota) {
                type = NavType.BoolType
            },
        ),
    ) {
        val overQuota = it.arguments?.getBoolean(searchOverQuotaDialogArgumentOverQuota) ?: false
        MegaAppTheme(isDark = isSystemInDarkTheme()) {
            StorageStatusDialogView(
                modifier = Modifier.padding(horizontal = 24.dp),
                usePlatformDefaultWidth = false,
                storageState = if (overQuota) StorageState.Red else StorageState.Orange,
                preWarning = overQuota.not(),
                overQuotaAlert = true,
                viewModel = hiltViewModel(),
                onUpgradeClick = {
                    navHostController.navigateUp()
                    navHostController.context.apply {
                        startActivity(Intent(this, UpgradeAccountActivity::class.java))
                    }
                },
                onCustomizedPlanClick = { email, accountType ->
                    AlertsAndWarnings.askForCustomizedPlan(
                        navHostController.context,
                        email,
                        accountType
                    )
                },
                onAchievementsClick = {
                    navHostController.navigateUp()
                    navHostController.context.apply {
                        startActivity(
                            Intent(this, MyAccountActivity::class.java)
                                .setAction(IntentConstants.ACTION_OPEN_ACHIEVEMENTS)
                        )
                    }
                },
                onClose = { navHostController.navigateUp() },
            )
        }
    }
}

internal const val searchOverQuotaDialog: String = "search/over_quota_dialog"
internal const val searchOverQuotaDialogArgumentOverQuota = "over_quota"