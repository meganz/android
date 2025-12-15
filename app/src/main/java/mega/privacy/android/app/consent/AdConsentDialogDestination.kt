package mega.privacy.android.app.consent

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.google.android.ump.UserMessagingPlatform
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.Serializable
import mega.privacy.android.app.consent.model.AdsConsentState
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata

@Serializable
data object AdConsentDialog : DialogNavKey

fun EntryProviderScope<DialogNavKey>.adConsentDialogDestination(
    remove: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<AdConsentDialog>(
        metadata = transparentMetadata()
    ) {
        val viewModel = hiltViewModel<AdsConsentViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val activity = LocalActivity.current

        DisposableEffect(activity) {
            if (activity != null) {
                viewModel.onLoaded(activity)
            }

            onDispose { viewModel.onUnLoaded() }
        }

        when (val state = uiState) {
            AdsConsentState.Loading -> {}
            is AdsConsentState.Data -> {
                val activity = LocalActivity.current
                EventEffect(
                    event = state.showConsentFormEvent,
                    onConsumed = viewModel::onConsentFormDisplayed,
                ) {
                    activity?.let {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(it) { error ->
                            viewModel.onConsentSelected(error)
                        }
                    }
                }

                EventEffect(
                    event = state.adConsentHandledEvent,
                    onConsumed = { remove(it) },
                ) {
                    viewModel.onAdConsentHandled()
                    onDialogHandled()
                }

                EventEffect(
                    event = state.adFeatureDisabled,
                    onConsumed = { remove(it) }
                ) {
                    onDialogHandled()
                }
            }
        }
    }
}