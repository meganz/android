package mega.privacy.mobile.home.presentation.recents.mapper

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Mapper to convert RecentActionBucket parent folder name to LocalizedText
 */
class RecentsParentFolderNameMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param item The RecentActionBucket to map
     * @return LocalizedText representing the parent folder name
     */
    operator fun invoke(item: RecentActionBucket): LocalizedText = when {
        !item.isNodeKeyDecrypted -> LocalizedText.StringRes(sharedR.string.shared_items_verify_credentials_undecrypted_folder)
        item.parentFolderName == CLOUD_DRIVE_FOLDER_NAME -> LocalizedText.StringRes(sharedR.string.video_section_videos_location_option_cloud_drive)
        else -> LocalizedText.Literal(item.parentFolderName)
    }
}

private const val CLOUD_DRIVE_FOLDER_NAME = "Cloud Drive"

