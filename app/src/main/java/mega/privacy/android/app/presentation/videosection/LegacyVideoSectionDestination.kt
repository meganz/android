package mega.privacy.android.app.presentation.videosection

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.VideoSectionNavKey

fun EntryProviderScope<NavKey>.videoSectionLegacyDestination(
    removeDestination: () -> Unit,
) {
    entry<VideoSectionNavKey>(
        metadata = transparentMetadata()
    ) { key ->

        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, VideoSectionActivity::class.java)
            context.startActivity(intent)
        }

        removeDestination()
    }
}