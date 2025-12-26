package mega.privacy.mobile.home.presentation.recents.model

import androidx.annotation.DrawableRes
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.node.NodeSourceType

/**
 * UI entity for a bucket in the recent actions list
 *
 * @property title RecentActionTitleText for the item that can be resolved to localized string in Composable
 * @property icon The icon to be displayed
 * @property shareIcon The icon to be displayed for sharing
 * @property parentFolderName Localized parent folder name
 * @property isMediaBucket Whether the item is a media bucket
 * @property isUpdate Whether this is an update action
 * @property updatedByText Localized text indicating who updated/created the item
 * @property userName User name for updatedByText formatting
 * @property isFavourite Whether the item is a favourite
 * @property nodeLabel The node label
 * @property bucket The recent action bucket
 */
data class RecentsUiItem(
    val title: RecentActionTitleText,
    @DrawableRes val icon: Int,
    @DrawableRes val shareIcon: Int?,
    val parentFolderName: LocalizedText,
    val isMediaBucket: Boolean,
    val isUpdate: Boolean,
    val updatedByText: LocalizedText?,
    val userName: String?,
    val isFavourite: Boolean,
    val nodeLabel: NodeLabel?,
    val bucket: RecentActionBucket,
    val isSingleNode: Boolean,
    val isSensitive: Boolean,
) {

    /**
     * Get the first node in the bucket
     */
    val firstNode
        get() = bucket.nodes.firstOrNull()

    /**
     * Get the node source type based on the parent folder shares type
     */
    val nodeSourceType
        get() = if (bucket.parentFolderSharesType == RecentActionsSharesType.INCOMING_SHARES) {
            NodeSourceType.INCOMING_SHARES
        } else {
            NodeSourceType.CLOUD_DRIVE
        }

    /**
     * Unique key for the item in a list
     * Adjust for duplicate single node buckets edge case
     */
    val key = buildString {
        append(bucket.identifier)
        if (isSingleNode) {
            append("|N:${firstNode?.id?.longValue}")
        }
    }
}