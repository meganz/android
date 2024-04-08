package mega.privacy.android.app.presentation.recentactions.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.RecentActionBucket

/**
 * UI entity for a bucket in the recent actions list
 *
 * @property firstLineText The first line of text to be displayed
 * @property icon The icon to be displayed
 * @property shareIcon The icon to be displayed for sharing
 * @property actionIcon The icon to be displayed for the action
 * @property parentFolderName The name of the parent folder
 * @property showMenuButton Whether the menu button should be displayed
 * @property date The date to be displayed
 * @property time The time to be displayed
 * @property updatedByText The text to be displayed for the updated by field
 * @property isFavourite Whether the item is a favourite
 * @property labelColor The color of the label
 * @property bucket The recent action bucket
 */
data class RecentActionBucketUiEntity(
    val firstLineText: String,
    @DrawableRes val icon: Int,
    @DrawableRes val shareIcon: Int?,
    @DrawableRes val actionIcon: Int,
    val parentFolderName: String,
    val showMenuButton: Boolean,
    val date: String,
    val time: String,
    val updatedByText: String?,
    val isFavourite: Boolean,
    @ColorRes val labelColor: Int?,
    val bucket: RecentActionBucket,
)