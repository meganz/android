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
 * @property simulateLastActiveDateResultEvent Event carrying the result of simulating the user
 *                                             last active date (success / invalid / failure)
 * @property prepareSimulateDateEvent Event to show the date picker, carrying the previous last
 *                                    active timestamp (epoch seconds, null if none) as its default
 * @property previousLastActiveTimestamp Previous simulated last active timestamp (epoch seconds,
 *                                       null if none); used to reject re-picking the same date
 */
data class QAAccountUiState(
    val cachedAccounts: List<UserCredentials> = emptyList(),
    val isSwitchingAccount: Boolean = false,
    val accountSwitchEvent: StateEventWithContent<QAAccountSwitchEvent> = consumed(),
    val simulateLastActiveDateResultEvent: StateEventWithContent<SimulateLastActiveDateResult> = consumed(),
    val prepareSimulateDateEvent: StateEventWithContent<Long?> = consumed(),
    val previousLastActiveTimestamp: Long? = null,
)
