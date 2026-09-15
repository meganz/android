package mega.privacy.android.navigation.contract.retain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.LocalRetainedValuesStoreProvider
import androidx.compose.runtime.retain.RetainedValuesStoreRegistry
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.navigation3.runtime.NavEntryDecorator

/**
 * Returns a [RetainedValuesStoreNavEntryDecorator] that is remembered across recompositions.
 *
 * @param registry The registry holding one retained-values store per entry. Defaults to a registry
 *   that is itself retained, so entry stores survive configuration changes.
 */
@Composable
fun <T : Any> rememberRetainedValuesStoreNavEntryDecorator(
    registry: RetainedValuesStoreRegistry = retainRetainedValuesStoreRegistry(),
): RetainedValuesStoreNavEntryDecorator<T> = remember(registry) {
    RetainedValuesStoreNavEntryDecorator(registry)
}

/**
 * Gives each [androidx.navigation3.runtime.NavEntry] its own retained-values store, so values
 * created with [androidx.compose.runtime.retain.retain] inside an entry are kept while the entry
 * stays on the back stack, including while it is covered by another entry or the Activity is
 * recreated, and are discarded when the entry is popped.
 *
 * Retained values are not written to saved instance state, so they do not survive process death.
 *
 * @param registry The registry holding one retained-values store per entry
 */
class RetainedValuesStoreNavEntryDecorator<T : Any>(
    registry: RetainedValuesStoreRegistry,
) : NavEntryDecorator<T>(
    onPop = { key -> registry.clearChild(key) },
    decorate = { entry ->
        registry.LocalRetainedValuesStoreProvider(entry.contentKey) { entry.Content() }
    },
)
