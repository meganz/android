package mega.privacy.android.app.presentation.achievements.invites

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsRoute

/**
 * Route for [InviteFriendsRoute]
 */
internal const val inviteFriendsRoute = "achievements/invites"
internal const val storageBonusInBytesArg = "storage_bonus_in_bytes"

internal class InviteFriendsArgs(val storageBonusInBytes: Long) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(checkNotNull(savedStateHandle[storageBonusInBytesArg]) as Long)
}

/**
 * Composable destination for [InviteFriendsRoute]
 */
fun NavGraphBuilder.inviteFriendsScreen() {
    composable(
        route = "$inviteFriendsRoute?storage_bonus={$storageBonusInBytesArg}",
        arguments = listOf(
            navArgument(name = storageBonusInBytesArg) {
                type = NavType.LongType
                defaultValue = 0
            },
        )
    ) {
        InviteFriendsRoute()
    }
}

/**
 * Navigation for [InviteFriendsRoute]
 */
fun NavController.navigateToInviteFriends(
    storageBonusInBytes: Long,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = "$inviteFriendsRoute?storage_bonus=$storageBonusInBytes",
        navOptions = navOptions
    )
}