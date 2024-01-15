package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.verifycontact.CannotVerifyContactDialog


internal fun NavGraphBuilder.cannotVerifyUserNavigation(
    navHostController: NavHostController,
) {
    dialog(
        route = cannotVerifyUserRoute.plus("/{${cannotVerifyUserRouteEmailArg}}"),
        arguments = listOf(
            navArgument(cannotVerifyUserRouteEmailArg) {
                type = NavType.StringType
            },
        )
    ) {
        CannotVerifyContactDialog(
            email = it.arguments?.getString(cannotVerifyUserRouteEmailArg).orEmpty()
        ) {
            navHostController.navigateUp()
        }
    }
}

internal const val cannotVerifyUserRoute =
    "search/node_bottom_sheet/cannot_verify_user_dialog"
internal const val cannotVerifyUserRouteEmailArg = "email"