package mega.privacy.android.app.presentation.testpassword.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import java.io.File

/**
 * UI State for Test Password Feature
 * @param isCurrentPassword ui state to handle checks whether entered password is the same as the current password
 * @param userMessage ui state to handle showing message to the user
 * @param isUserLogout ui state to handle when user logs out
 * @param isFinishedCopyingRecoveryKey ui state to handle when recovery key has been copied
 * @param isUserExhaustedPasswordAttempts ui state to check whether user has exhausted all password attempts
 * @param isPasswordReminderBlocked ui state to handle password reminder block checkbox
 * @param isLoading ui state to handle screen loading
 * @param isUITestPasswordMode ui state to check whether screen is currently on test password mode layout
 * @param isLogoutMode ui state to check whether screen is in logout mode
 * @param wrongPasswordAttempts ui state to count number of wrong password attempts
 * @param printRecoveryKey Print the given recovery key file
 */
data class TestPasswordUIState(
    val isCurrentPassword: PasswordState = PasswordState.Initial,
    val userMessage: StateEventWithContent<Int> = consumed(),
    val isUserLogout: StateEventWithContent<Boolean> = consumed(),
    val isFinishedCopyingRecoveryKey: StateEventWithContent<Boolean> = consumed(),
    val isUserExhaustedPasswordAttempts: StateEvent = consumed,
    val isPasswordReminderBlocked: Boolean = false,
    val isLoading: Boolean = false,
    val isUITestPasswordMode: Boolean = false,
    val isLogoutMode: Boolean = false,
    val wrongPasswordAttempts: Int = 0,
    val printRecoveryKey: StateEventWithContent<File?> = consumed()
)

/**
 * Password State Holder to handle password related state with 3 types of state
 */
enum class PasswordState {
    /**
     * Initial state
     */
    Initial,

    /**
     * Success state and returns true
     */
    True,

    /**
     * Error state and returns false
     */
    False
}