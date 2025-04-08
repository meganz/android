package mega.privacy.android.app.presentation.achievements.invites

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsRoute

/**
 * Route for [InviteFriendsRoute]
 */
internal const val storageBonusInBytesArg = "storage_bonus_in_bytes"

/**
 * Route for [InviteFriendsRoute]
 * @param storageBonusInBytes Storage bonus in bytes
 */
@Serializable
data class InviteFriends(
    val storageBonusInBytes: Long,
)

/**
 * Composable destination for [InviteFriendsRoute]
 */
fun NavGraphBuilder.inviteFriendsScreen() {
    composable<InviteFriends> {
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
        InviteFriends(storageBonusInBytes),
        navOptions = navOptions
    )
}