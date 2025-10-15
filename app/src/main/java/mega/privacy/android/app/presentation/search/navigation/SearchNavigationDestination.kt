package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.SearchNodeNavKey
import mega.privacy.android.navigation.megaNavigator

fun EntryProviderBuilder<NavKey>.searchLegacyDestination(
    removeDestination: () -> Unit,
) {
    entry<SearchNodeNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openSearchActivity(
                context = context,
                nodeSourceType = key.nodeSourceType,
                parentHandle = key.parentHandle,
                isFirstNavigationLevel = key.isFirstNavigationLevel
            )
            removeDestination()
        }
    }
}

