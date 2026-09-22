package mega.privacy.android.feature.sync.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Hands the exclusions list what the Add, Edit and size screens authored.
 *
 * Each result is cleared as it arrives and before it is reported: a result left in place is
 * replayed to the next collector, which would stage a change the user only cancelled.
 */
@Composable
internal fun SyncExclusionResultEffect(
    monitorResult: (String) -> Flow<Any?>,
    clearResult: (String) -> Unit,
    onExclusionAdded: (AddSyncExclusionResult) -> Unit,
    onRuleEdited: (EditSyncExclusionRuleResult) -> Unit,
    onSizeBoundsChanged: (EditSyncSizeExclusionResult) -> Unit,
) {
    NavigationResultEffect(
        key = AddSyncExclusionRuleNavKey.RESULT,
        monitorResult = monitorResult,
        clearResult = clearResult,
    ) { (it as? AddSyncExclusionResult)?.let(onExclusionAdded) }

    NavigationResultEffect(
        key = EditSyncExclusionRuleNavKey.RESULT,
        monitorResult = monitorResult,
        clearResult = clearResult,
    ) { (it as? EditSyncExclusionRuleResult)?.let(onRuleEdited) }

    NavigationResultEffect(
        key = EditSyncSizeExclusionNavKey.RESULT,
        monitorResult = monitorResult,
        clearResult = clearResult,
    ) { (it as? EditSyncSizeExclusionResult)?.let(onSizeBoundsChanged) }
}

/**
 * Reports each value arriving under [key], having cleared it first. The value is untyped, because
 * the result channel is, so [onResult] is left to reject anything it did not expect.
 */
@Composable
private fun NavigationResultEffect(
    key: String,
    monitorResult: (String) -> Flow<Any?>,
    clearResult: (String) -> Unit,
    onResult: (Any) -> Unit,
) {
    // remembered because the handler hands back a fresh wrapper each call, which would otherwise
    // restart the collection on every recomposition
    val results = remember(key, monitorResult) { monitorResult(key) }
    val result by results.collectAsStateWithLifecycle(null)

    LaunchedEffect(result) {
        val value = result ?: return@LaunchedEffect
        clearResult(key)
        onResult(value)
    }
}
