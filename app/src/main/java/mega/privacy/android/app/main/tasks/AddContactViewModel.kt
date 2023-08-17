package mega.privacy.android.app.main.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetContactVerificationWarningUseCase
import mega.privacy.android.app.main.model.AddContactState
import mega.privacy.android.app.presentation.chat.model.ChatState
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * AddContactView Model
 * @param getContactVerificationWarningUseCase [GetFeatureFlagValueUseCase]
 */
@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddContactState())

    val state = _state.asStateFlow()

    init {
        getContactFeatureEnabled()
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

}