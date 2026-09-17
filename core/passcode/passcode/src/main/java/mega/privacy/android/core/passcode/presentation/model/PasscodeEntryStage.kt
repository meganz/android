package mega.privacy.android.core.passcode.presentation.model

import androidx.compose.runtime.Stable

/**
 * The round the user is currently on while setting a passcode.
 *
 * The passcode typed in the first round is held inside [Confirmation] rather than beside the
 * stage, so leaving the confirmation round is the only way to reach it and doing so destroys it.
 */
@Stable
sealed interface PasscodeEntryStage {

    /**
     * Awaiting the first entry of a new passcode.
     */
    data object FirstEntry : PasscodeEntryStage

    /**
     * Awaiting the passcode to be typed a second time.
     *
     * @property firstEntry the passcode typed in the first round
     * @property showMismatchError whether the previous confirmation attempt failed to match
     */
    data class Confirmation(
        val firstEntry: String,
        val showMismatchError: Boolean,
    ) : PasscodeEntryStage

    /**
     * Both rounds matched and the passcode is being persisted.
     */
    data object Saving : PasscodeEntryStage
}
