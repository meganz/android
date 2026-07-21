package mega.privacy.android.app.presentation.hidenode

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.HiddenNodesOnboardingNavKey

fun EntryProviderScope<NavKey>.hiddenNodesOnboardingScreen(
    navigationHandler: NavigationHandler,
) {
    entry<HiddenNodesOnboardingNavKey> {
        val onClose = { navigationHandler.remove(HiddenNodesOnboardingNavKey) }
        val viewModel = hiltViewModel<HiddenNodesOnboardingViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        HiddenNodesOnboardingScreen(
            state = state,
            isOnboarding = true,
            onClickBack = onClose,
            onClickContinue = onClose,
        )
    }
}
