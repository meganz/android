package mega.privacy.mobile.home.presentation.home.widget.recents.model

import androidx.annotation.DrawableRes
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.RecentActionBucket

/**
 * UI entity for a bucket in the recent actions list
 *
 * @property title RecentActionTitleText for the item that can be resolved to localized string in Composable
 * @property icon The icon to be displayed
 * @property shareIcon The icon to be displayed for sharing
 * @property parentFolderName Localized parent folder name
 * @property timestampText RecentActionTimestampText for formatting date/time in Composable
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
    val timestampText: RecentsTimestampText,
    val isMediaBucket: Boolean,
    val isUpdate: Boolean,
    val updatedByText: LocalizedText?,
    val userName: String?,
    val isFavourite: Boolean,
    val nodeLabel: NodeLabel?,
    val bucket: RecentActionBucket,
    val isSingleNode: Boolean,
)