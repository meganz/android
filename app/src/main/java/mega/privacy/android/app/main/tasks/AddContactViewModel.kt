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
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
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
        getContactFeatureEnabled()
        getEnabledFeatures()
    }

    private fun getEnabledFeatures() {
        viewModelScope.launch {
            val enabledFeatures = setOfNotNull(
                AppFeatures.QRCodeCompose.takeIf { getFeatureFlagValueUseCase(it) }
            )
            _state.update { it.copy(enabledFeatureFlags = enabledFeatures) }
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
     * Check if given feature flag is enabled or not
     */
    fun isFeatureEnabled(feature: Feature) = state.value.enabledFeatureFlags.contains(feature)

}