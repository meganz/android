package mega.privacy.android.app.presentation.passcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.passcode.mapper.PasscodeTypeMapper
import mega.privacy.android.app.presentation.passcode.model.PasscodeUnlockState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.passcode.UnlockPasscodeRequest
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeAttemptsUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTypeUseCase
import mega.privacy.android.domain.usecase.passcode.UnlockPasscodeUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Passcode unlock view model
 *
 * @property unlockPasscodeUseCase
 */
@HiltViewModel
internal class PasscodeUnlockViewModel @Inject constructor(
    private val monitorPasscodeAttemptsUseCase: MonitorPasscodeAttemptsUseCase,
    private val unlockPasscodeUseCase: UnlockPasscodeUseCase,
    private val monitorPasscodeTypeUseCase: MonitorPasscodeTypeUseCase,
    private val passcodeTypeMapper: PasscodeTypeMapper,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
) : ViewModel() {

    private val _state: MutableStateFlow<PasscodeUnlockState> = MutableStateFlow(
        PasscodeUnlockState.Loading(ThemeMode.System)
    )

    /**
     * State
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                combine(
                    monitorPasscodeTypeUseCase().mapNotNull { passcodeType ->
                        passcodeType?.let {
                            passcodeTypeMapper(it)
                        }
                    },
                    monitorPasscodeAttemptsUseCase(),
                    monitorThemeModeUseCase().catch {
                        Timber.e(it, "Error monitoring theme mode")
                        emit(ThemeMode.System)
                    }
                ) { type, attempts, themeMode ->
                    { state: PasscodeUnlockState ->
                        if (state is PasscodeUnlockState.Data) {
                            state.copy(
                                passcodeType = type,
                                failedAttempts = attempts,
                                logoutWarning = attempts >= 5,
                                themeMode = themeMode
                            )
                        } else {
                            PasscodeUnlockState.Data(
                                passcodeType = type,
                                failedAttempts = attempts,
                                logoutWarning = attempts >= 5,
                                themeMode = themeMode
                            )
                        }
                    }
                }.catch {
                    Timber.e(it)
                }.collect {
                    _state.update(it)
                }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Unlock with passcode
     *
     * @param passCode
     */
    fun unlockWithPasscode(passCode: String) =
        unlock(UnlockPasscodeRequest.PasscodeRequest(passCode))

    /**
     * Unlock with password
     *
     * @param password
     */
    fun unlockWithPassword(password: String) =
        unlock(UnlockPasscodeRequest.PasswordRequest(password))

    private fun unlock(request: UnlockPasscodeRequest) {
        viewModelScope.launch {
            runCatching {
                unlockPasscodeUseCase(request)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun unlockWithBiometrics() {
        unlock(UnlockPasscodeRequest.BiometricRequest)
    }

    fun onBiometricAuthFailed() {
        Timber.w("Biometric Authentication failed")
    }

}