package mega.privacy.android.core.passcode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.core.passcode.presentation.mapper.PasscodeTypeMapper
import mega.privacy.android.core.passcode.presentation.mapper.PasscodeUITypeToDomainMapper
import mega.privacy.android.core.passcode.presentation.model.PasscodeEntryStage
import mega.privacy.android.core.passcode.presentation.model.PasscodeTypeOption
import mega.privacy.android.core.passcode.presentation.model.PasscodeUIType
import mega.privacy.android.core.passcode.presentation.model.SetupPasscodeUiState
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.SetPasscodeRequest
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTimeOutUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTypeUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeTimeoutUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Drives the screen used to set a new passcode or change the existing one.
 *
 * Public because the navigation destination hosting the screen lives in the app module and
 * resolves this through `hiltViewModel`.
 */
@HiltViewModel
class SetupPasscodeViewModel @Inject constructor(
    private val monitorPasscodeTypeUseCase: MonitorPasscodeTypeUseCase,
    private val monitorPasscodeTimeOutUseCase: MonitorPasscodeTimeOutUseCase,
    private val setPasscodeUseCase: SetPasscodeUseCase,
    private val setPasscodeTimeoutUseCase: SetPasscodeTimeoutUseCase,
    private val passcodeTypeMapper: PasscodeTypeMapper,
    private val passcodeUITypeToDomainMapper: PasscodeUITypeToDomainMapper,
) : ViewModel() {

    private val entryStage = MutableStateFlow<PasscodeEntryStage>(PasscodeEntryStage.FirstEntry)
    private val selectedType = MutableStateFlow<PasscodeUIType?>(null)
    private val passcodeSavedEventChannel = Channel<StateEvent>(Channel.BUFFERED)

    /**
     * State
     */
    val uiState: StateFlow<SetupPasscodeUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorPasscodeTypeUseCase()
                .map { storedType ->
                    storedType?.let { passcodeTypeMapper(it) } ?: DEFAULT_PASSCODE_TYPE
                }
                .catch {
                    Timber.e(it, "Failed to monitor the stored passcode type")
                    emit(DEFAULT_PASSCODE_TYPE)
                },
            selectedType,
            entryStage,
            passcodeSavedEventChannel.receiveAsFlow().onStart { emit(consumed) },
        ) { storedType, userSelectedType, stage, savedEvent ->
            SetupPasscodeUiState.Data(
                passcodeType = userSelectedType ?: storedType,
                stage = stage,
                passcodeSavedEvent = savedEvent,
            )
        }.catch {
            Timber.e(it, "Failed to build the setup passcode state")
        }.asUiStateFlow(viewModelScope, SetupPasscodeUiState.Loading)
    }

    /**
     * Called once a complete passcode has been typed, whichever round the user is on.
     *
     * The stage transition is a single atomic update with [PasscodeEntryStage.Saving] as a fixed
     * point, so a repeated call for one entry cannot produce a second comparison or a second save.
     *
     * @param passcode
     */
    fun onPasscodeEntered(passcode: String) {
        val previousStage = entryStage.getAndUpdate { current ->
            when (current) {
                PasscodeEntryStage.FirstEntry -> PasscodeEntryStage.Confirmation(
                    firstEntry = passcode,
                    showMismatchError = false,
                )

                is PasscodeEntryStage.Confirmation ->
                    if (current.firstEntry == passcode) PasscodeEntryStage.Saving
                    else current.copy(showMismatchError = true)

                PasscodeEntryStage.Saving -> current
            }
        }

        if (previousStage is PasscodeEntryStage.Confirmation && previousStage.firstEntry == passcode) {
            savePasscode(passcode)
        }
    }

    /**
     * Called when the user picks a different passcode format.
     *
     * Resetting the stage is what discards any passcode already typed, since it is only reachable
     * through [PasscodeEntryStage.Confirmation].
     *
     * @param option
     */
    fun onPasscodeTypeSelected(option: PasscodeTypeOption) {
        val biometricEnabled = currentPasscodeType()?.biometricEnabled == true
        entryStage.value = PasscodeEntryStage.FirstEntry
        selectedType.value = when (option) {
            PasscodeTypeOption.FourDigits -> PasscodeUIType.Pin(biometricEnabled, FOUR_DIGITS)
            PasscodeTypeOption.SixDigits -> PasscodeUIType.Pin(biometricEnabled, SIX_DIGITS)
            PasscodeTypeOption.Alphanumeric -> PasscodeUIType.Alphanumeric(biometricEnabled)
        }
    }

    /**
     * Consume the passcode saved event
     */
    fun onPasscodeSavedEventConsumed() {
        passcodeSavedEventChannel.trySend(consumed)
    }

    private fun currentPasscodeType() =
        (uiState.value as? SetupPasscodeUiState.Data)?.passcodeType

    private fun savePasscode(passcode: String) {
        val uiType = currentPasscodeType()
        if (uiType == null) {
            Timber.e("Cannot save a passcode before the passcode type has loaded")
            entryStage.value = PasscodeEntryStage.FirstEntry
            return
        }

        viewModelScope.launch {
            runCatching {
                setPasscodeUseCase(
                    SetPasscodeRequest(
                        type = passcodeUITypeToDomainMapper(uiType),
                        passcode = passcode,
                    )
                )
                if (monitorPasscodeTimeOutUseCase().first() == null) {
                    setPasscodeTimeoutUseCase(PasscodeTimeout.DEFAULT)
                }
            }.onSuccess {
                passcodeSavedEventChannel.send(triggered)
            }.onFailure {
                Timber.e(it, "Failed to save the passcode")
                entryStage.value = PasscodeEntryStage.FirstEntry
            }
        }
    }

    private companion object {
        const val FOUR_DIGITS = 4
        const val SIX_DIGITS = 6
        val DEFAULT_PASSCODE_TYPE = PasscodeUIType.Pin(false, FOUR_DIGITS)
    }
}
