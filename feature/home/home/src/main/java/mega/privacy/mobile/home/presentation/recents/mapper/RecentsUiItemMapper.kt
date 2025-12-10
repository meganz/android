package mega.privacy.mobile.home.presentation.recents.mapper

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.feature.home.R
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.mobile.home.presentation.recents.model.RecentActionTitleText
import mega.privacy.mobile.home.presentation.recents.model.RecentsTimestampText
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiItem
import javax.inject.Inject

/**
 * The mapper class to convert the RecentActionBucket to RecentActionUiItem
 */
class RecentActionUiItemMapper @Inject constructor(
    private val fileTypeIconMapper: FileTypeIconMapper,
) {

    /**
     * Convert RecentActionBucket to RecentActionUiItem
     */
    operator fun invoke(
        item: RecentActionBucket,
    ): RecentsUiItem {
        val node = item.nodes.first()
        val isSingleNode = item.nodes.size == 1
        val isMediaBucket = !isSingleNode && item.isMedia
        val title = getTitle(
            item = item,
            node = node,
            isSingleNode = isSingleNode,
            isMediaBucket = isMediaBucket
        )

        return RecentsUiItem(
            title = title,
            icon = if (isMediaBucket) {
                IconPackR.drawable.ic_image_stack_medium_solid
            } else {
                fileTypeIconMapper(node.type.extension)
            },
            shareIcon = IconPackR.drawable.ic_folder_users_small_solid.takeIf {
                item.parentFolderSharesType != RecentActionsSharesType.NONE
            },
            parentFolderName = getParentFolderName(item),
            timestampText = RecentsTimestampText(item.timestamp),
            isMediaBucket = isMediaBucket,
            isUpdate = item.isUpdate,
            updatedByText = getUpdatedByText(item),
            userName = if (!item.currentUserIsOwner) item.userName else null,
            isFavourite = isSingleNode && node.isFavourite,
            nodeLabel = if (isSingleNode) node.nodeLabel else null,
            isSingleNode = isSingleNode,
            isSensitive = isSingleNode && (node.isMarkedSensitive || node.isSensitiveInherited),
            bucket = item
        )
    }

    private fun getTitle(
        item: RecentActionBucket,
        node: TypedFileNode,
        isSingleNode: Boolean,
        isMediaBucket: Boolean,
    ) = if (item.isNodeKeyDecrypted) {
        when {
            isSingleNode -> RecentActionTitleText.SingleNode(node.name)
            isMediaBucket -> {
                val numImages = item.nodes.count { it.type.mimeType.startsWith("image/") }
                val numVideos = item.nodes.size - numImages
                when {
                    numImages > 0 && numVideos == 0 -> {
                        RecentActionTitleText.MediaBucketImagesOnly(numImages)
                    }

                    numImages == 0 && numVideos > 0 -> {
                        RecentActionTitleText.MediaBucketVideosOnly(numVideos)
                    }

                    else -> {
                        RecentActionTitleText.MediaBucketMixed(numImages, numVideos)
                    }
                }
            }

            else -> {
                RecentActionTitleText.RegularBucket(
                    nodeName = node.name,
                    additionalCount = item.nodes.size - 1
                )
            }
        }
    } else {
        RecentActionTitleText.UndecryptedFiles(item.nodes.size)
    }


    private fun getParentFolderName(item: RecentActionBucket) = if (item.isNodeKeyDecrypted) {
        when (item.parentFolderName) {
            CLOUD_DRIVE_FOLDER_NAME -> LocalizedText.StringRes(R.string.section_cloud_drive)
            else -> LocalizedText.Literal(item.parentFolderName)
        }
    } else {
        LocalizedText.StringRes(R.string.shared_items_verify_credentials_undecrypted_folder)
    }

    private fun getUpdatedByText(item: RecentActionBucket) = if (!item.currentUserIsOwner) {
        val stringResId = if (item.isUpdate) {
            R.string.update_action_bucket
        } else {
            R.string.create_action_bucket
        }
        LocalizedText.StringRes(stringResId, listOf(item.userName))
    } else {
        null
    }
}

private const val CLOUD_DRIVE_FOLDER_NAME = "Cloud Drive"