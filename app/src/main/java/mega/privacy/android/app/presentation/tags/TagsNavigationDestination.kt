package mega.privacy.android.app.presentation.tags

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.destination.TagsNavKey

fun EntryProviderScope<NavKey>.tagsScreen(
    removeDestination: () -> Unit,
) {
    entry<TagsNavKey> { key ->
        TagsRoute(
            nodeHandle = key.nodeHandle,
            onBackPressed = removeDestination,
        )
    }
}
