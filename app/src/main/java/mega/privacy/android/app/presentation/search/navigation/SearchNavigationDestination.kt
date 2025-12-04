package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.SearchNodeNavKey

fun EntryProviderScope<NavKey>.searchLegacyDestination(
    removeDestination: () -> Unit,
) {
    entry<SearchNodeNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(
                SearchActivity.getIntent(
                    context = context,
                    nodeSourceType = key.nodeSourceType,
                    parentHandle = key.parentHandle,
                )
            )
            removeDestination()
        }
    }
}

