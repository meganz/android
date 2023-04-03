package mega.privacy.android.app.presentation.verification

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.coroutineScope
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
import mega.privacy.android.domain.usecase.GetCurrentCountryCode
import mega.privacy.android.domain.usecase.SetSMSVerificationShown
import mega.privacy.android.domain.usecase.verification.FormatPhoneNumber
import mega.privacy.android.domain.usecase.verification.SendSMSVerificationCode
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * View Model for SMSVerification
 */
@HiltViewModel
class SMSVerificationViewModel @Inject constructor(
    private val setSMSVerificationShown: SetSMSVerificationShown,
    private val getCountryCallingCodes: GetCountryCallingCodes,
    private val sendSMSVerificationCode: SendSMSVerificationCode,
    private val areAccountAchievementsEnabled: AreAccountAchievementsEnabled,
    private val getAccountAchievements: GetAccountAchievements,
    private val stringUtilWrapper: StringUtilWrapper,
    private val getCurrentCountryCode: GetCurrentCountryCode,
    private val formatPhoneNumber: FormatPhoneNumber,
    private val savedState: SavedStateHandle,
    private val smsVerificationTextMapper: SMSVerificationTextMapper,
    private val smsVerificationTextErrorMapper: SmsVerificationTextErrorMapper,
) : ViewModel(), DefaultLifecycleObserver {

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
            extractSavedState()
            updateInferredCode()
            getCountryCodes()
        }
    }

    private suspend fun updateInferredCode() {
        runCatching {
            val inferredCountryCode = getCurrentCountryCode()
            inferredCountryCode?.let {
                _uiState.value = _uiState.value.copy(
                    inferredCountryCode = inferredCountryCode,
                )
            }
        }
    }

    private fun extractSavedState() {
        val selectedCountryCode = savedState.get<String>(COUNTRY_CODE)
        val selectedCountryName = savedState.get<String>(COUNTRY_NAME)
        val selectedDialCode = savedState.get<String>(DIAL_CODE)
        setSelectedCodes(
            selectedCountryCode,
            selectedCountryName,
            selectedDialCode,
            shouldSaveInHandle = false
        )
    }

    /**
     * set Selected Codes for country, name and dial code
     * @param selectedCountryCode [String]
     * @param selectedCountryName [String]
     * @param selectedDialCode [String]
     */
    fun setSelectedCodes(
        selectedCountryCode: String?,
        selectedCountryName: String?,
        selectedDialCode: String?,
        shouldSaveInHandle: Boolean = true,
    ) {
        Timber.d("Current Selection $selectedCountryCode $selectedCountryName $selectedDialCode")
        if (shouldSaveInHandle) {
            savedState[COUNTRY_CODE] = selectedCountryCode
            savedState[COUNTRY_NAME] = selectedCountryName
            savedState[DIAL_CODE] = selectedDialCode
        }
        _uiState.mapAndUpdate {
            it.copy(
                selectedCountryCode = selectedCountryCode?.uppercase().orEmpty(),
                selectedCountryName = selectedCountryName.orEmpty(),
                selectedDialCode = selectedDialCode.orEmpty(),
            )
        }
    }

    /**
     * set whether user is locked or not
     * @param isUserLocked [Boolean]
     */
    fun setIsUserLocked(isUserLocked: Boolean, context: Context) {
        Timber.d("is user locked $isUserLocked")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUserLocked = isUserLocked)
            getBonusStorage(context)
        }
    }

    /**
     * set phone number
     * @param phoneNumber [String]
     */
    fun setPhoneNumber(phoneNumber: String) {
        Timber.d("Current Phone Number $phoneNumber")
        _uiState.update {
            it.copy(
                phoneNumber = phoneNumber,
                isPhoneNumberValid = true,
                phoneNumberErrorText = ""
            )
        }
    }

    /**
     * set is phone number is valid or not
     * @param isValid [Boolean]
     */
    private fun setIsPhoneNumberValid(isValid: Boolean) {
        Timber.d("Current Phone Number is valid $isValid")
        _uiState.mapAndUpdate {
            it.copy(isPhoneNumberValid = isValid)
        }
    }

    private suspend fun getBonusStorage(context: Context) {
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
                        stringUtilWrapper.getSizeString(achievements.grantedStorage, context)
                    _uiState.mapAndUpdate {
                        it.copy(isAchievementsEnabled = true, bonusStorageSMS = bonusStorageSMS)
                    }
                }
            }
        }
    }

    private fun getCountryCodes() {
        viewModelScope.launch {
            runCatching {
                val countryCallingCodes = getCountryCallingCodes()
                resolveSelectedDialCode(countryCallingCodes)
            }.onFailure {
                Timber.d("Error getCountryCallingCodes $it")
            }
        }
    }

    /**
     * resolve Selected DialCode
     * this function will check to resolve selected country code,name and dial code from
     * [countryCallingCodes] where the format of [countryCallingCodes] is listOf("BD:880,", "AU:61,", "NZ:64,", "IN:91,")
     * if inferredCode is NZ the resolved selected country code,name and dial code  will be
     * respectively NZ, New Zealand and +64
     * @param countryCallingCodes
     */
    private fun resolveSelectedDialCode(countryCallingCodes: List<String>) {
        _uiState.value.inferredCountryCode.takeIf { it.isNotEmpty() }?.let { inferredCode ->
            countryCallingCodes.firstOrNull { it.startsWith(inferredCode, ignoreCase = true) }
                ?.split(":")
                ?.let {
                    val dialCode = it[1].split(",").firstOrNull()
                    val locale = Locale("", inferredCode)
                    _uiState.mapAndUpdate { state ->
                        state.copy(
                            selectedCountryName = locale.displayName,
                            selectedCountryCode = inferredCode.uppercase(),
                            selectedDialCode = "+$dialCode",
                            countryCallingCodes = countryCallingCodes
                        )
                    }
                }
        }
    }

    /**
     * on Consume SMSCode Sent FinishedEvent
     */
    fun onConsumeSMSCodeSentFinishedEvent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerificationCodeSent = false) }
        }
    }

    /**
     * on send SMS verification Code
     */
    private fun onSendSMSVerificationCode(phoneNumber: String) {
        viewModelScope.launch {
            if (phoneNumber.isNotEmpty()) {
                runCatching {
                    _uiState.update { state ->
                        state.copy(
                            isNextEnabled = false
                        )
                    }
                    sendSMSVerificationCode(phoneNumber)
                    _uiState.update { state ->
                        state.copy(
                            isVerificationCodeSent = true,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isNextEnabled = true,
                            phoneNumberErrorText = smsVerificationTextErrorMapper(error)
                        )
                    }
                }
            }
        }
    }

    /**
     * validate phone number
     */
    fun validatePhoneNumber() {
        viewModelScope.launch {
            (getFormattedPhoneNumber()?.takeIf { it.startsWith("+") }).let { phoneNumber ->
                setIsPhoneNumberValid(phoneNumber != null)
                phoneNumber?.let { onSendSMSVerificationCode(it) }
            }
        }
    }

    private suspend fun getFormattedPhoneNumber() = with(_uiState.value) {
        Timber.d("Selected Country Code $selectedCountryCode")
        runCatching {
            formatPhoneNumber(
                phoneNumber,
                selectedCountryCode
            )
        }.getOrNull().also { Timber.d("Formatted PhoneNumber $it") }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        owner.lifecycle.coroutineScope.launch { setSMSVerificationShown(true) }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.coroutineScope.launch { setSMSVerificationShown(false) }
        super.onDestroy(owner)
    }

    private inline fun MutableStateFlow<SMSVerificationUIState>.mapAndUpdate(crossinline function: (SMSVerificationUIState) -> SMSVerificationUIState) {
        this.update {
            smsVerificationTextMapper(function(it))
        }
    }
}
