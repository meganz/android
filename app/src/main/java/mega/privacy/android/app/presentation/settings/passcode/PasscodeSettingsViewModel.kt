@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.settings.passcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.passcode.mapper.TimeoutOptionMapper
import mega.privacy.android.app.presentation.settings.passcode.model.PasscodeSettingsUIState
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.passcode.DisableBiometricPasscodeUseCase
import mega.privacy.android.domain.usecase.passcode.DisablePasscodeUseCase
import mega.privacy.android.domain.usecase.passcode.EnableBiometricsUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTimeOutUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTypeUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Passcode settings view model
 */
@HiltViewModel
class PasscodeSettingsViewModel @Inject constructor(
    private val monitorPasscodeLockPreferenceUseCase: MonitorPasscodeLockPreferenceUseCase,
    private val monitorPasscodeTypeUseCase: MonitorPasscodeTypeUseCase,
    private val monitorPasscodeTimeOutUseCase: MonitorPasscodeTimeOutUseCase,
    private val timeoutOptionMapper: TimeoutOptionMapper,
    private val disablePasscodeUseCase: DisablePasscodeUseCase,
    private val disableBiometricPasscodeUseCase: DisableBiometricPasscodeUseCase,
    private val enableBiometricsUseCase: EnableBiometricsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(PasscodeSettingsUIState.INITIAL)

    /**
     * State
     */
    val state: StateFlow<PasscodeSettingsUIState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                merge(monitorPasscodeLockPreferenceUseCase().mapLatest { enabled ->
                    { state: PasscodeSettingsUIState -> state.copy(isEnabled = enabled) }
                }, monitorPasscodeTypeUseCase().mapLatest { type ->
                    { state: PasscodeSettingsUIState -> state.copy(isBiometricsEnabled = type is PasscodeType.Biometric) }
                }, monitorPasscodeTimeOutUseCase().filterNotNull().mapLatest { timeout ->
                    { state: PasscodeSettingsUIState ->
                        state.copy(
                            timeout = timeoutOptionMapper(
                                timeout
                            )
                        )
                    }
                }).catch {
                    Timber.e(it, "An error was thrown in the passcode settings ui state flow")
                }.collect {
                    _state.update(it)
                }
            }.onFailure {
                Timber.e(
                    it,
                    "An error was thrown while collecting the passcode settings ui state flow"
                )
            }
        }
    }

    /**
     * Disable passcode
     */
    fun disablePasscode() {
        viewModelScope.launch {
            runCatching {
                disablePasscodeUseCase()
            }.onFailure {
                Timber.e(it, "An error occurred while trying to disable passcode")
            }
        }
    }

    /**
     * Disable biometrics
     */
    fun disableBiometrics() {
        viewModelScope.launch {
            runCatching {
                disableBiometricPasscodeUseCase()
            }.onFailure {
                Timber.e(it, "An error occurred while trying to disable biometrics for passcode")
            }
        }
    }

    /**
     * Enable biometrics
     */
    fun enableBiometrics() {
        viewModelScope.launch {
            runCatching {
                enableBiometricsUseCase()
            }.onFailure {
                Timber.e(it, "An error occurred while trying to enable biometrics for passcode")
            }
        }
    }

}