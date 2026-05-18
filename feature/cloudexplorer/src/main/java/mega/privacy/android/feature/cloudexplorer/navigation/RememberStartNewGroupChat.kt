package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey

/**
 * Returns an `onStartNewGroupChat` trigger: invoking it stores the per-launch handler and
 * navigates to [CreateGroupChatNavKey]; the result is dispatched once and then cleared.
 * Emissions are safe-cast via `as? NewGroupChatResult` to avoid runtime `ClassCastException`
 * on the untyped `NavigationHandler.monitorResult` channel.
 */
@Composable
internal fun rememberStartNewGroupChat(
    monitorResult: (String) -> Flow<Any?>,
    clearResult: (String) -> Unit,
    onNavigate: (NavKey) -> Unit,
): ((CreateGroupChatNavKey.NewGroupChatResult) -> Unit) -> Unit {
    var pendingCallback by remember {
        mutableStateOf<((CreateGroupChatNavKey.NewGroupChatResult) -> Unit)?>(null)
    }

    val selectionResult by monitorResult(CreateGroupChatNavKey.KEY)
        .collectAsStateWithLifecycle(initialValue = null)
    val selection = selectionResult as? CreateGroupChatNavKey.NewGroupChatResult

    LaunchedEffect(selection) {
        selection?.let { result ->
            clearResult(CreateGroupChatNavKey.KEY)
            pendingCallback?.invoke(result)
            pendingCallback = null
        }
    }

    return { callback ->
        if (pendingCallback == null) {
            pendingCallback = callback
            onNavigate(CreateGroupChatNavKey)
        }
    }
}
