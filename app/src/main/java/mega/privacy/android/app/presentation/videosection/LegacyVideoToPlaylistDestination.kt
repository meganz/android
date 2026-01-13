package mega.privacy.android.app.presentation.videosection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.navigation3.runtime.EntryProviderScope
import mega.privacy.android.core.sharedcomponents.coroutine.LaunchedOnceEffect
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.activities.contract.VideoToPlaylistActivityContract
import mega.privacy.android.domain.entity.node.AddVideoToPlaylistResult
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyVideoToPlaylistNavKey

fun EntryProviderScope<NavKey>.legacyVideoToPlaylistDestination(
    removeDestination: () -> Unit,
    returnResult: (String, AddVideoToPlaylistResult?) -> Unit,
) {
    entry<LegacyVideoToPlaylistNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val launcher = rememberLauncherForActivityResult(
            VideoToPlaylistActivityContract()
        ) { result ->
            returnResult(LegacyVideoToPlaylistNavKey.ADD_VIDEO_TO_PLAYLIST_RESULT, result)
            removeDestination()
        }

        LaunchedOnceEffect {
            launcher.launch(key.nodeHandle)
        }
    }
}