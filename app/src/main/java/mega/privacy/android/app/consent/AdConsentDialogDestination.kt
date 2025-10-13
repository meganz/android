package mega.privacy.android.app.consent

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.google.android.ump.UserMessagingPlatform
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.Serializable
import mega.privacy.android.app.consent.model.AdsConsentState

@Serializable
data object AdConsentDialog : NavKey

fun EntryProviderBuilder<NavKey>.adConsentDialogDestination(
    navigateBack: () -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<AdConsentDialog>(
        metadata = DialogSceneStrategy.dialog()
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