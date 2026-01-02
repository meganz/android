package mega.privacy.android.app.presentation.account.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.user.UserCredentials

/**
 * UI state for QA account switching functionality
 *
 * @property cachedAccounts List of cached user accounts
 * @property isSwitchingAccount Whether an account switch is in progress
 * @property accountSwitchEvent Event triggered when account switch completes or fails
 */
data class QAAccountUiState(
    val cachedAccounts: List<UserCredentials> = emptyList(),
    val isSwitchingAccount: Boolean = false,
    val accountSwitchEvent: StateEventWithContent<QAAccountSwitchEvent> = consumed(),
)
