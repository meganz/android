package mega.privacy.android.app.consent.model

sealed interface CookieConsentState {
    data object Loading : CookieConsentState
    data class Data(val cookiesUrl: String) : CookieConsentState
}
