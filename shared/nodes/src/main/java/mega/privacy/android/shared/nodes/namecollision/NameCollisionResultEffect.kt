package mega.privacy.android.shared.nodes.namecollision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.navigation.destination.NameCollisionNavKey

/**
 * Listens for the name-collision screen result and, on each arrival, clears the result before
 * invoking [onOutcome] so back-navigation doesn't replay the previous resolution. Emissions are
 * safe-cast to [NameCollisionOutcome] to avoid a runtime `ClassCastException` on the untyped
 * `NavigationHandler.monitorResult` channel.
 */
@Composable
fun NameCollisionResultEffect(
    monitorResult: (String) -> Flow<Any?>,
    clearResult: (String) -> Unit,
    onOutcome: (NameCollisionOutcome) -> Unit,
) {
    val results = remember(monitorResult) { monitorResult(NameCollisionNavKey.RESULT) }
    val result by results.collectAsStateWithLifecycle(null)
    val outcome = result as? NameCollisionOutcome

    LaunchedEffect(outcome) {
        val current = outcome ?: return@LaunchedEffect
        clearResult(NameCollisionNavKey.RESULT)
        onOutcome(current)
    }
}
