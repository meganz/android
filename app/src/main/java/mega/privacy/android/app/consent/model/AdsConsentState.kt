package mega.privacy.android.app.consent.model

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEvent

@Immutable
sealed interface AdsConsentState {
    data object Loading : AdsConsentState
    data class Data(
        val showConsentFormEvent: StateEvent,
        val adConsentHandledEvent: StateEvent,
        val adFeatureDisabled: StateEvent,
    ) : AdsConsentState
}
