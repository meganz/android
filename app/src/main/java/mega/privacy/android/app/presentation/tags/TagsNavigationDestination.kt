package mega.privacy.android.app.presentation.tags

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.TagsNavKey

fun EntryProviderScope<NavKey>.tagsScreen(
    removeDestination: () -> Unit,
) {
    entry<TagsNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            context.startActivity(
                Intent(context, TagsActivity::class.java)
                    .putExtra(TagsActivity.NODE_ID, args.nodeHandle)
            )
            removeDestination()
        }
    }
}
