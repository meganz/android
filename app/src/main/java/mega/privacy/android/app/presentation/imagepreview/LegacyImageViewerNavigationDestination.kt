package mega.privacy.android.app.presentation.imagepreview

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyImageViewerNavKey

fun EntryProviderScope<NavKey>.legacyImageViewerScreen(
    removeDestination: () -> Unit,
) {
    entry<LegacyImageViewerNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = if (key.nodeSourceType == NodeSourceTypeInt.RECENTS_BUCKET_ADAPTER) {
                ImagePreviewActivity.createIntent(
                    context = context,
                    imageSource = ImagePreviewFetcherSource.DEFAULT,
                    menuOptionsSource = if (key.isInShare) ImagePreviewMenuSource.SHARED_ITEMS else ImagePreviewMenuSource.DEFAULT,
                    anchorImageNodeId = NodeId(key.nodeHandle),
                    params = key.nodeIds?.let { nodeIds ->
                        mapOf(DefaultImageNodeFetcher.NODE_IDS to nodeIds.toLongArray())
                    } ?: emptyMap(),
                    enableAddToAlbum = true,
                )
            } else {
                ImagePreviewActivity.createIntent(
                    context = context,
                    fileNodeId = key.nodeHandle,
                    parentNodeId = key.parentNodeHandle,
                    nodeSourceType = key.nodeSourceType
                )
            }
            intent?.let {
                context.startActivity(it)
            }

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

