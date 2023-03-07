package mega.privacy.android.app.presentation.verification

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.verification.model.SMSVerificationUIState
import mega.privacy.android.app.presentation.verification.model.mapper.SMSVerificationTextMapper
import mega.privacy.android.app.presentation.verification.model.mapper.SmsVerificationTextErrorMapper
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.usecase.AreAccountAchievementsEnabled
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.GetCountryCallingCodes
import mega.privacy.android.domain.usecase.Logout
import mega.privacy.android.domain.usecase.SetSMSVerificationShown
import mega.privacy.android.domain.usecase.verification.SendSMSVerificationCode
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for SMSVerification
 */
@HiltViewModel
class SMSVerificationViewModel @Inject constructor(
    private val setSMSVerificationShown: SetSMSVerificationShown,
    private val getCountryCallingCodes: GetCountryCallingCodes,
    private val logout: Logout,
    private val sendSMSVerificationCode: SendSMSVerificationCode,
    private val areAccountAchievementsEnabled: AreAccountAchievementsEnabled,
    private val getAccountAchievements: GetAccountAchievements,
    private val stringUtilWrapper: StringUtilWrapper,
    private val savedState: SavedStateHandle,
    private val smsVerificationTextMapper: SMSVerificationTextMapper,
    private val smsVerificationTextErrorMapper: SmsVerificationTextErrorMapper,
) : ViewModel() {

    private companion object {
        const val COUNTRY_NAME = "name"
        const val DIAL_CODE = "dial_code"
        const val COUNTRY_CODE = "code"
    }

    private val _uiState = MutableStateFlow(SMSVerificationUIState())

    /**
     * UI state flow
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            setSMSVerificationShown(true)
        }
        extractSavedState()
        getCountryCodes()
    }

    private fun extractSavedState() {
        val selectedCountryCode = savedState.get<String>(COUNTRY_CODE)
        val selectedCountryName = savedState.get<String>(COUNTRY_NAME)
        val selectedDialCode = savedState.get<String>(DIAL_CODE)
        _uiState.mapAndUpdate {
            it.copy(
                selectedCountryCode = selectedCountryCode ?: "",
                selectedCountryName = selectedCountryName ?: "",
                selectedDialCode = selectedDialCode ?: "",
            )
        }
    }

    /**
     * set whether user us locked or not
     */
    fun setIsUserLocked(isUserLocked: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUserLocked = isUserLocked)
            getBonusStorage()
        }
    }

    private suspend fun getBonusStorage() {
        if (!_uiState.value.isUserLocked) {
            val isAchievementUser = areAccountAchievementsEnabled()
            if (isAchievementUser) {
                val achievements =
                    getAccountAchievements(
                        AchievementType.MEGA_ACHIEVEMENT_ADD_PHONE,
                        awardIndex = 0,
                    )
                achievements?.let {
                    val bonusStorageSMS =
                        stringUtilWrapper.getSizeString(achievements.grantedStorage)
                    _uiState.mapAndUpdate {
                        it.copy(bonusStorageSMS = bonusStorageSMS)
                    }
                }
            }
        }
    }

    private fun getCountryCodes() {
        viewModelScope.launch {
            runCatching {
                val countryCallingCodes = getCountryCallingCodes()
                _uiState.mapAndUpdate {
                    it.copy(countryCallingCodes = countryCallingCodes)
                }
            }.onFailure {
                Timber.d("Error getCountryCallingCodes $it")
            }
        }
    }

    /**
     * on Logout
     */
    fun onLogout() {
        viewModelScope.launch {
            logout()
        }
    }

    /**
     * on send SMS verification Code
     */
    fun onSendSMSVerificationCode() {
        viewModelScope.launch {
            if (_uiState.value.phoneNumber.isNotEmpty()) {
                runCatching {
                    sendSMSVerificationCode(_uiState.value.phoneNumber)
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(phoneNumberErrorText = smsVerificationTextErrorMapper(error))
                    }
                }
            }
        }
    }

    private inline fun MutableStateFlow<SMSVerificationUIState>.mapAndUpdate(crossinline function: (SMSVerificationUIState) -> SMSVerificationUIState) {
        this.update {
            smsVerificationTextMapper(function(it))
        }
    }
}
