package mega.privacy.android.app.consent

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation3.runtime.NavKey
import com.google.android.ump.UserMessagingPlatform
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.Serializable
import mega.privacy.android.app.consent.model.AdsConsentState

@Serializable
data object AdConsentDialog : NavKey

fun NavGraphBuilder.adConsentDialogDestination(
    navigateBack: () -> Unit,
    onDialogHandled: () -> Unit,
) {
    dialog<AdConsentDialog> {
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
                    onConsumed = viewModel::onConsentFormEventHandled,
                ) {
                    activity?.let {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(it) { error ->
                            viewModel.onConsentFormShown(error)
                        }
                    }
                }

                EventEffect(
                    event = state.adConsentHandledEvent,
                    onConsumed = navigateBack,
                ) {
                    viewModel.onAdConsentHandled()
                    onDialogHandled()
                }

                EventEffect(
                    event = state.adFeatureDisabled,
                    onConsumed = navigateBack
                ) {
                    onDialogHandled()
                }
            }

        }
    }
}