package mega.privacy.android.app.presentation.recentactions.mapper

import mega.privacy.android.icon.pack.R as IconPackR
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.recentactions.model.RecentActionBucketUiEntity
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The mapper class to convert the RecentActionBucket to RecentActionBucketUiEntity
 */
class RecentActionBucketUiEntityMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileTypeIconMapper: FileTypeIconMapper,
) {

    /**
     * Convert to RecentActionBucket to RecentActionBucketUiEntity
     */
    operator fun invoke(
        item: RecentActionBucket,
    ): RecentActionBucketUiEntity {
        val node = item.nodes.first()
        val isSingleNode = item.nodes.size == 1

        return RecentActionBucketUiEntity(
            firstLineText = if (item.isKeyVerified) {
                if (isSingleNode) node.name else {
                    if (item.isMedia)
                        getMediaTitle(context, item.nodes)
                    else
                        context.getString(
                            R.string.title_bucket,
                            node.name,
                            item.nodes.size - 1
                        )
                }
            } else {
                context.resources.getQuantityString(
                    R.plurals.cloud_drive_undecrypted_file,
                    item.nodes.size
                )
            },
            updatedByText = if (!item.currentUserIsOwner) context.getString(
                if (item.isUpdate) R.string.update_action_bucket else R.string.create_action_bucket,
                item.userName
            ) else null,
            parentFolderName = if (item.isKeyVerified) {
                when (item.parentFolderName) {
                    CLOUD_DRIVE_FOLDER_NAME -> context.getString(R.string.section_cloud_drive)
                    else -> item.parentFolderName
                }
            } else {
                context.getString(R.string.shared_items_verify_credentials_undecrypted_folder)
            },
            time = TimeUtils.formatTime(item.timestamp),
            date = TimeUtils.formatBucketDate(item.timestamp, context),
            icon = if (!isSingleNode && item.isMedia)
                IconPackR.drawable.ic_image_stack_medium_solid
            else
                fileTypeIconMapper(node.type.extension),
            shareIcon = when (item.parentFolderSharesType) {
                RecentActionsSharesType.NONE -> null
                RecentActionsSharesType.INCOMING_SHARES -> IconPackR.drawable.ic_folder_incoming_medium_solid
                RecentActionsSharesType.OUTGOING_SHARES, RecentActionsSharesType.PENDING_OUTGOING_SHARES -> IconPackR.drawable.ic_folder_outgoing_medium_solid
            },
            actionIcon = if (item.isUpdate) R.drawable.ic_versions_small else R.drawable.ic_recents_up,
            showMenuButton = isSingleNode,
            isFavourite = isSingleNode && node.isFavourite,
            labelColorId = if (isSingleNode && node.label != MegaNode.NODE_LBL_UNKNOWN)
                MegaNodeUtil.getNodeLabelColor(
                    node.label
                )
            else null,
            bucket = item
        )
    }


    private fun getMediaTitle(
        context: Context,
        nodeList: List<TypedFileNode>,
    ): String {
        val numImages = nodeList.count { it.type.mimeType.startsWith("image/") }
        val numVideos = nodeList.size - numImages

        val mediaTitle = when {
            numImages > 0 && numVideos == 0 -> {
                context.resources.getQuantityString(
                    R.plurals.title_media_bucket_only_images,
                    numImages,
                    numImages
                )
            }

            numImages == 0 && numVideos > 0 -> {
                context.resources.getQuantityString(
                    R.plurals.title_media_bucket_only_videos,
                    numVideos,
                    numVideos
                )
            }

            else -> {
                context.resources.getQuantityString(
                    R.plurals.title_media_bucket_images_and_videos,
                    numImages,
                    numImages
                ) + context.resources.getQuantityString(
                    R.plurals.title_media_bucket_images_and_videos_2,
                    numVideos,
                    numVideos
                )
            }
        }
        return mediaTitle
    }
}

private const val CLOUD_DRIVE_FOLDER_NAME = "Cloud Drive"