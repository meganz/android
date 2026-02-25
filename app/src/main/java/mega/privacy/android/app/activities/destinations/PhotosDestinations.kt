package mega.privacy.android.app.activities.destinations

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.TimelineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyImagePreviewNavKey
import mega.privacy.android.navigation.destination.LegacySettingsCameraUploadsActivityNavKey
import mega.privacy.android.navigation.destination.MediaTimelinePhotoPreviewNavKey

fun EntryProviderScope<NavKey>.legacyMediaTimelinePhotoPreview(removeDestination: () -> Unit) {
    entry<MediaTimelinePhotoPreviewNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val intent = ImagePreviewActivity.createIntent(
                context = context,
                imageSource = ImagePreviewFetcherSource.TIMELINE,
                menuOptionsSource = ImagePreviewMenuSource.TIMELINE,
                anchorImageNodeId = NodeId(args.id),
                params = mapOf(
                    TimelineImageNodeFetcher.TIMELINE_SORT_TYPE to args.sortType,
                    TimelineImageNodeFetcher.TIMELINE_FILTER_TYPE to args.filterType,
                    TimelineImageNodeFetcher.TIMELINE_MEDIA_SOURCE to args.mediaSource,
                ),
                enableAddToAlbum = true,
            )
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

fun EntryProviderScope<NavKey>.legacySettingsCameraUploadsActivityNavKey(removeDestination: () -> Unit) {
    entry<LegacySettingsCameraUploadsActivityNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, SettingsCameraUploadsActivity::class.java).apply {
                putExtra(
                    INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT,
                    args.isShowHowToUploadPrompt
                )
            }
            context.startActivity(intent)
            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

fun EntryProviderScope<NavKey>.legacyImagePreview(
    removeDestination: () -> Unit,
) {
    entry<LegacyImagePreviewNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val intent = ImagePreviewActivity.createIntent(
                context = context,
                imageSource = ImagePreviewFetcherSource.DEFAULT,
                menuOptionsSource = ImagePreviewMenuSource.DEFAULT,
                anchorImageNodeId = NodeId(args.anchorImageId),
                params = mapOf(DefaultImageNodeFetcher.NODE_IDS to args.imageIds.toLongArray()),
            )

            context.startActivity(intent)
            removeDestination()
        }
    }
}
