package mega.privacy.mobile.home.presentation.configuration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.StartScreenPreferenceNavKey

@Serializable
data object HomeConfiguration : NavKey

fun EntryProviderScope<NavKey>.homeConfigurationScreen(
    navigationHandler: NavigationHandler,
) {
    entry<HomeConfiguration> {
        val viewmodel = hiltViewModel<HomeConfigurationViewModel>()
        val state by viewmodel.state.collectAsStateWithLifecycle()
        val snackbarEventQueue = rememberSnackBarQueue()
        val coroutineScope = rememberCoroutineScope()

        HomeConfigurationScreen(
            state = state,
            onWidgetEnabledChange = viewmodel::updateEnabledState,
            onWidgetOrderChange = viewmodel::updateWidgetOrder,
            onBack = navigationHandler::back,
            onResetToDefault = viewmodel::resetWidgetStateToDefault,
            showSnackbarMessage = { message ->
                coroutineScope.launch {
                    snackbarEventQueue.queueMessage(message)
                }
            },
            onChooseDefaultStartScreen = {
                navigationHandler.navigate(StartScreenPreferenceNavKey)
            }
        )
    }
}