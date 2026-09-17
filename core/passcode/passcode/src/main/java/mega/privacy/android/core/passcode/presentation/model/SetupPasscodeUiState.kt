package mega.privacy.android.core.passcode.presentation.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent

/**
 * State of the screen used to set or change the passcode.
 */
@Stable
sealed interface SetupPasscodeUiState {

    /**
     * The stored passcode type has not been read yet.
     */
    data object Loading : SetupPasscodeUiState

    /**
     * @property passcodeType the format the passcode will be saved in
     * @property stage which entry round the user is on
     * @property passcodeSavedEvent triggered once the passcode has been persisted
     */
    data class Data(
        val passcodeType: PasscodeUIType,
        val stage: PasscodeEntryStage,
        val passcodeSavedEvent: StateEvent,
    ) : SetupPasscodeUiState
}
