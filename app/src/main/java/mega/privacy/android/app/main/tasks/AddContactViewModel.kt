package mega.privacy.android.app.main.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.model.AddContactState
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * AddContactView Model
 * @param getContactVerificationWarningUseCase [GetFeatureFlagValueUseCase]
 */
@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddContactState())

    val state = _state.asStateFlow()

    init {
        getEnabledFeatures()
        getContactFeatureEnabled()
    }

    private fun getEnabledFeatures() {
        viewModelScope.launch {
            getFeatureFlagValueUseCase(AppFeatures.CallUnlimitedProPlan).let { flag ->
                _state.update { state ->
                    state.copy(
                        isCallUnlimitedProPlanFeatureFlagEnabled = flag,
                    )
                }
            }
        }
    }

    /**
     * Gets contact enabled Value from [GetContactVerificationWarningUseCase]
     */
    fun getContactFeatureEnabled() {
        viewModelScope.launch {
            val contactVerificationWarningEnabled = getContactVerificationWarningUseCase()
            _state.update {
                it.copy(isContactVerificationWarningEnabled = contactVerificationWarningEnabled)
            }
        }
    }

    /**
     * Check if Call Unlimited Pro Plan feature flag is enabled or not
     */
    fun shouldShowParticipantsLimitWarning(shouldShow: Boolean) {
        viewModelScope.launch {
            runCatching {
                val shouldShowParticipantsLimitWarning =
                    getFeatureFlagValueUseCase(AppFeatures.CallUnlimitedProPlan) && shouldShow

                _state.update {
                    it.copy(shouldShowParticipantsLimitWarning = shouldShowParticipantsLimitWarning)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

}
