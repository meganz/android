package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import mega.privacy.android.navigation.megaNavigator
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType


internal fun NavGraphBuilder.overQuotaDialogNavigation(
    navHostController: NavHostController,
) {

    dialog(
        route = searchOverQuotaDialog,
    ) {
        LaunchedEffect(Unit) {
            navHostController.navigateUp()
            navHostController.context.also { context ->
                context.megaNavigator.openQuotaWarningUpsell(
                    context = context,
                    type = QuotaWarningType.Storage,
                    trigger = QuotaWarningTrigger.Upload,
                )
            }
        }
    }
}

internal const val searchOverQuotaDialog: String = "search/over_quota_dialog"
