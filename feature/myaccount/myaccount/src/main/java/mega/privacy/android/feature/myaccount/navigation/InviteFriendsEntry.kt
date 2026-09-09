package mega.privacy.android.feature.myaccount.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsView
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.view.InviteFriendsViewModel
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.InviteContactNavKey
import mega.privacy.android.navigation.destination.InviteFriendsNavKey

/**
 * Wires the [InviteFriendsView] to its [InviteFriendsViewModel] and routes the invite action to the
 * Compose invite-contact flow. The sent-invitation count is read back from [InviteContactNavKey] and
 * surfaced as the confirmation dialog, then cleared once dismissed so it does not re-show.
 */
internal fun EntryProviderScope<NavKey>.inviteFriends(navigationHandler: NavigationHandler) {
    entry<InviteFriendsNavKey> { navKey ->
        val viewModel = hiltViewModel<InviteFriendsViewModel, InviteFriendsViewModel.Factory>(
            creationCallback = { factory -> factory.create(navKey.grantStorageInBytes) }
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val sentInvitesCount by navigationHandler
            .monitorResult<Int>(InviteContactNavKey.KEY)
            .collectAsStateWithLifecycle(initialValue = null)

        InviteFriendsView(
            uiState = uiState,
            sentInvitesCount = sentInvitesCount,
            onInviteClick = {
                navigationHandler.navigate(InviteContactNavKey(isFromAchievement = true))
            },
            onConfirmationDismiss = {
                navigationHandler.clearResult(InviteContactNavKey.KEY)
            },
        )
    }
}
