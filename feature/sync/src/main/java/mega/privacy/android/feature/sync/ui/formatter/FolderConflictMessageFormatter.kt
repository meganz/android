package mega.privacy.android.feature.sync.ui.formatter

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Builds unified folder conflict copy using [sharedR.string.sync_error_folder_conflict].
 */
class FolderConflictMessageFormatter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun format(
        folderDisplayName: String,
        @StringRes folderTypeLabelRes: Int,
        featureLabel: String,
        deviceName: String?,
    ): String {
        val device = deviceName ?: context.getString(sharedR.string.sync_label_this_device)
        return context.getString(
            sharedR.string.sync_error_folder_conflict,
            folderDisplayName,
            context.getString(folderTypeLabelRes),
            featureLabel,
            device,
        )
    }

    fun formatDeviceFolderCameraUploadsConflict(folderDisplayName: String): String = format(
        folderDisplayName = folderDisplayName,
        folderTypeLabelRes = sharedR.string.sync_label_device_folder,
        featureLabel = context.getString(sharedR.string.sync_label_camera_uploads),
        deviceName = null,
    )

    fun formatDeviceFolderMediaUploadsConflict(folderDisplayName: String): String = format(
        folderDisplayName = folderDisplayName,
        folderTypeLabelRes = sharedR.string.sync_label_device_folder,
        featureLabel = context.getString(sharedR.string.sync_label_media_uploads),
        deviceName = null,
    )

    fun formatFromFolderUsage(
        folderDisplayName: String,
        @StringRes folderTypeLabelRes: Int,
        result: FolderUsageResult,
    ): String? = when (result) {
        FolderUsageResult.NotUsed -> null
        is FolderUsageResult.UsedByCameraUpload,
        is FolderUsageResult.UsedByCameraUploadParent,
        is FolderUsageResult.UsedByCameraUploadChild,
            -> format(
            folderDisplayName,
            folderTypeLabelRes,
            context.getString(sharedR.string.sync_label_camera_uploads),
            null,
        )

        is FolderUsageResult.UsedByMediaUpload,
        is FolderUsageResult.UsedByMediaUploadParent,
        is FolderUsageResult.UsedByMediaUploadChild,
            -> format(
            folderDisplayName,
            folderTypeLabelRes,
            context.getString(sharedR.string.sync_label_media_uploads),
            null,
        )

        is FolderUsageResult.UsedBySyncOrBackup -> format(
            folderDisplayName,
            folderTypeLabelRes,
            context.getString(sharedR.string.sync_label_a_sync_or_backup),
            result.deviceName,
        )

        is FolderUsageResult.UsedBySyncOrBackupParent -> format(
            folderDisplayName,
            folderTypeLabelRes,
            result.backupName ?: context.getString(sharedR.string.sync_label_a_sync_or_backup),
            result.deviceName,
        )

        is FolderUsageResult.UsedBySyncOrBackupChild -> format(
            folderDisplayName,
            folderTypeLabelRes,
            result.backupName ?: context.getString(sharedR.string.sync_label_a_sync_or_backup),
            result.deviceName,
        )
    }
}
