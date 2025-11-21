package mega.privacy.mobile.home.presentation.home.widget.recents.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.feature.home.R

/**
 * Represents different types of recent action titles that can be resolved to localized strings
 * in Composable functions using Compose's context.
 */
sealed class RecentActionTitleText {
    /**
     * For single node: displays the node name
     */
    data class SingleNode(
        val nodeName: String,
    ) : RecentActionTitleText()

    /**
     * For media bucket with only images
     * @param numImages Number of images
     */
    data class MediaBucketImagesOnly(
        val numImages: Int,
    ) : RecentActionTitleText()

    /**
     * For media bucket with only videos
     * @param numVideos Number of videos
     */
    data class MediaBucketVideosOnly(
        val numVideos: Int,
    ) : RecentActionTitleText()

    /**
     * For media bucket with both images and videos
     * @param numImages Number of images
     * @param numVideos Number of videos
     */
    data class MediaBucketMixed(
        val numImages: Int,
        val numVideos: Int,
    ) : RecentActionTitleText()

    /**
     * For regular bucket: displays node name and count
     * @param nodeName Name of the first node
     * @param additionalCount Number of additional items
     */
    data class RegularBucket(
        val nodeName: String,
        val additionalCount: Int,
    ) : RecentActionTitleText()

    /**
     * For undecrypted files: displays undecrypted file count
     * @param count Number of undecrypted files
     */
    data class UndecryptedFiles(
        val count: Int,
    ) : RecentActionTitleText()
}

/**
 * Composable function that resolves RecentActionTitleText to a localized string
 * @return Localized string representation of the recent action title
 */
@Composable
fun RecentActionTitleText.text(): String {
    return when (this) {
        is RecentActionTitleText.SingleNode -> nodeName

        is RecentActionTitleText.MediaBucketImagesOnly -> {
            pluralStringResource(
                R.plurals.title_media_bucket_only_images,
                numImages,
                numImages
            )
        }

        is RecentActionTitleText.MediaBucketVideosOnly -> {
            pluralStringResource(
                R.plurals.title_media_bucket_only_videos,
                numVideos,
                numVideos
            )
        }

        is RecentActionTitleText.MediaBucketMixed -> {
            pluralStringResource(
                R.plurals.title_media_bucket_images_and_videos,
                numImages,
                numImages
            ) + pluralStringResource(
                R.plurals.title_media_bucket_images_and_videos_2,
                numVideos,
                numVideos
            )
        }

        is RecentActionTitleText.RegularBucket -> {
            stringResource(
                R.string.title_bucket,
                nodeName,
                additionalCount
            )
        }

        is RecentActionTitleText.UndecryptedFiles -> {
            pluralStringResource(
                R.plurals.cloud_drive_undecrypted_file,
                count
            )
        }
    }
}

