package mega.privacy.android.app.appstate.content.destinations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import mega.privacy.android.app.appstate.global.model.RefreshEvent
import mega.privacy.android.app.presentation.login.FetchNodesViewModel
import mega.privacy.android.app.presentation.login.view.FetchNodesContent
import mega.privacy.android.navigation.contract.navkey.NoNodeNavKey

@Serializable
data class FetchingContentNavKey(
    val session: String,
    val isFromLogin: Boolean,
    val refreshEvent: RefreshEvent? = null,
) : NoNodeNavKey

fun EntryProviderScope<NavKey>.fetchingContentDestination() {
    entry<FetchingContentNavKey>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) {
        val viewModel = hiltViewModel<FetchNodesViewModel, FetchNodesViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(
                    FetchNodesViewModel.Args(
                        session = it.session,
                        isFromLogin = it.isFromLogin,
                        refreshEvent = it.refreshEvent
                    )
                )
            }
        )
        val state by viewModel.state.collectAsStateWithLifecycle()

        FetchNodesContent(
            isRequestStatusInProgress = state.isRequestStatusInProgress,
            currentProgress = state.currentProgress,
            currentStatusText = state.currentStatusText,
            startProgress = if (state.isFromLogin) 0.275f else 0f,
            requestStatusProgress = state.requestStatusProgress,
        )
    }
}