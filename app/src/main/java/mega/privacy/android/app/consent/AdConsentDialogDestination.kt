package mega.privacy.android.app.consent

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.google.android.ump.UserMessagingPlatform
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.consent.model.AdsConsentState
import mega.privacy.android.app.presentation.extensions.isAlive
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.AdConsentDialogNavKey


fun EntryProviderScope<in DialogNavKey>.adConsentDialogDestination(
    remove: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<AdConsentDialogNavKey>(
        metadata = transparentMetadata()
    ) { navKey ->
        val viewModel = hiltViewModel<AdsConsentViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val activity = LocalActivity.current

        DisposableEffect(activity) {
            activity?.takeIf { it.isAlive }?.let {
                viewModel.onLoaded(it)
            }

            onDispose { viewModel.onUnLoaded() }
        }

        when (val state = uiState) {
            AdsConsentState.Loading -> {}
            is AdsConsentState.Data -> {
                EventEffect(
                    event = state.showConsentFormEvent,
                    onConsumed = viewModel::onConsentFormDisplayed,
                ) {
                    activity?.takeIf { it.isAlive }?.let {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(it) { error ->
                            viewModel.onConsentSelected(error)
                        }
                    }
                }

                EventEffect(
                    event = state.adConsentHandledEvent,
                    onConsumed = { remove(navKey) },
                ) {
                    viewModel.onAdConsentHandled()
                    onDialogHandled()
                }

                EventEffect(
                    event = state.adFeatureDisabled,
                    onConsumed = { remove(navKey) }
                ) {
                    onDialogHandled()
                }
            }
        }
    }
}