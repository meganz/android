package mega.privacy.android.app.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import mega.privacy.android.app.consent.model.CookieConsentState
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.cookies.GetCookieUrlUseCase
import mega.privacy.android.shared.original.core.ui.utils.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CookieConsentViewModel @Inject constructor(
    private val getCookieUrlUseCase: GetCookieUrlUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
    private val adConsentWrapper: AdConsentWrapper,
) : ViewModel() {

    val state: StateFlow<CookieConsentState> by lazy {
        flow {
            emit(CookieConsentState.Data(cookiesUrl = getCookieUrlUseCase()))
        }.asUiStateFlow(viewModelScope, CookieConsentState.Loading)
    }

    fun acceptAllCookies() {
        updateCookieSettings(CookieType.entries.toSet())
        adConsentWrapper.refreshConsent()
    }

    fun acceptEssentialCookies() {
        updateCookieSettings(setOf(CookieType.ESSENTIAL))
        adConsentWrapper.refreshConsent()
    }

    private fun updateCookieSettings(types: Set<CookieType>) {
        viewModelScope.launch {
            runCatching { updateCookieSettingsUseCase(types) }.onFailure { Timber.e(it) }
        }
    }
}
