package mega.privacy.android.feature.sync.ui.formatter

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Uses Robolectric so [Context.getString] loads real `sharedR` values. Expected strings are built
 * from the same string resources and placeholders as [FolderConflictMessageFormatter], so template
 * or label changes in XML surface as failures when output no longer matches.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderConflictMessageFormatterTest {

    private lateinit var context: Context
    private lateinit var underTest: FolderConflictMessageFormatter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        underTest = FolderConflictMessageFormatter(context)
    }

    /**
     * Mirrors [FolderConflictMessageFormatter.format]: same [sharedR.string.sync_error_folder_conflict]
     * call with resolved type label and device string.
     */
    private fun expectedSyncConflict(
        folderDisplayName: String,
        @StringRes folderTypeLabelRes: Int,
        featureLabel: String,
        deviceLabel: String,
    ): String = context.getString(
        sharedR.string.sync_error_folder_conflict,
        folderDisplayName,
        context.getString(folderTypeLabelRes),
        featureLabel,
        deviceLabel,
    )

    @Test
    fun `test that format builds conflict string with all provided values`() {
        val folderDisplayName = "Photos"
        val featureLabel = context.getString(sharedR.string.sync_label_camera_uploads)
        val deviceName = "My Phone"

        val result = underTest.format(
            folderDisplayName = folderDisplayName,
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            featureLabel = featureLabel,
            deviceName = deviceName,
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                folderDisplayName,
                sharedR.string.sync_label_cloud_folder,
                featureLabel,
                deviceName,
            )
        )
    }

    @Test
    fun `test that format falls back to sync_label_this_device when deviceName is null`() {
        val thisDevice = context.getString(sharedR.string.sync_label_this_device)
        val featureLabel = context.getString(sharedR.string.sync_label_a_sync_or_backup)

        val result = underTest.format(
            folderDisplayName = "DCIM",
            folderTypeLabelRes = sharedR.string.sync_label_device_folder,
            featureLabel = featureLabel,
            deviceName = null,
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "DCIM",
                sharedR.string.sync_label_device_folder,
                featureLabel,
                thisDevice,
            )
        )
    }

    @Test
    fun `test that formatDeviceFolderCameraUploadsConflict delegates with device folder label and camera uploads feature`() {
        val result = underTest.formatDeviceFolderCameraUploadsConflict("DCIM")

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "DCIM",
                sharedR.string.sync_label_device_folder,
                context.getString(sharedR.string.sync_label_camera_uploads),
                context.getString(sharedR.string.sync_label_this_device),
            )
        )
    }

    @Test
    fun `test that formatDeviceFolderMediaUploadsConflict delegates with device folder label and media uploads feature`() {
        val result = underTest.formatDeviceFolderMediaUploadsConflict("DCIM")

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "DCIM",
                sharedR.string.sync_label_device_folder,
                context.getString(sharedR.string.sync_label_media_uploads),
                context.getString(sharedR.string.sync_label_this_device),
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage returns null for NotUsed`() {
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.NotUsed,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `test that formatFromFolderUsage returns camera uploads message for UsedByCameraUpload`() {
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedByCameraUpload,
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                context.getString(sharedR.string.sync_label_camera_uploads),
                context.getString(sharedR.string.sync_label_this_device),
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage returns camera uploads message for UsedByCameraUploadParent`() {
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedByCameraUploadParent,
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                context.getString(sharedR.string.sync_label_camera_uploads),
                context.getString(sharedR.string.sync_label_this_device),
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage returns camera uploads message for UsedByCameraUploadChild`() {
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedByCameraUploadChild,
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                context.getString(sharedR.string.sync_label_camera_uploads),
                context.getString(sharedR.string.sync_label_this_device),
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage returns media uploads message for UsedByMediaUpload`() {
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedByMediaUpload,
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                context.getString(sharedR.string.sync_label_media_uploads),
                context.getString(sharedR.string.sync_label_this_device),
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage returns media uploads message for UsedByMediaUploadParent`() {
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedByMediaUploadParent,
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                context.getString(sharedR.string.sync_label_media_uploads),
                context.getString(sharedR.string.sync_label_this_device),
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage returns media uploads message for UsedByMediaUploadChild`() {
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedByMediaUploadChild,
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                context.getString(sharedR.string.sync_label_media_uploads),
                context.getString(sharedR.string.sync_label_this_device),
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage uses backup deviceName for UsedBySyncOrBackup`() {
        val syncOrBackup = context.getString(sharedR.string.sync_label_a_sync_or_backup)
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Backup",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedBySyncOrBackup(
                deviceId = "dev-1",
                deviceName = "Other Device"
            ),
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Backup",
                sharedR.string.sync_label_cloud_folder,
                syncOrBackup,
                "Other Device",
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage falls back to sync_label_this_device when UsedBySyncOrBackup has null deviceName`() {
        val thisDevice = context.getString(sharedR.string.sync_label_this_device)
        val syncOrBackup = context.getString(sharedR.string.sync_label_a_sync_or_backup)

        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Backup",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedBySyncOrBackup(deviceId = "dev-1", deviceName = null),
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Backup",
                sharedR.string.sync_label_cloud_folder,
                syncOrBackup,
                thisDevice,
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage uses backupName for UsedBySyncOrBackupParent`() {
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedBySyncOrBackupParent(
                deviceId = "dev-1",
                deviceName = "PC",
                backupName = "My Backup",
            ),
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                "My Backup",
                "PC",
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage falls back to a_sync_or_backup label when UsedBySyncOrBackupParent has null backupName`() {
        val syncOrBackup = context.getString(sharedR.string.sync_label_a_sync_or_backup)

        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedBySyncOrBackupParent(
                deviceId = "dev-1",
                deviceName = "PC",
                backupName = null,
            ),
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                syncOrBackup,
                "PC",
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage uses backupName for UsedBySyncOrBackupChild`() {
        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedBySyncOrBackupChild(
                deviceId = "dev-1",
                deviceName = "PC",
                backupName = "Child Backup",
            ),
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                "Child Backup",
                "PC",
            )
        )
    }

    @Test
    fun `test that formatFromFolderUsage falls back to a_sync_or_backup label when UsedBySyncOrBackupChild has null backupName`() {
        val syncOrBackup = context.getString(sharedR.string.sync_label_a_sync_or_backup)

        val result = underTest.formatFromFolderUsage(
            folderDisplayName = "Photos",
            folderTypeLabelRes = sharedR.string.sync_label_cloud_folder,
            result = FolderUsageResult.UsedBySyncOrBackupChild(
                deviceId = "dev-1",
                deviceName = "PC",
                backupName = null,
            ),
        )

        assertThat(result).isEqualTo(
            expectedSyncConflict(
                "Photos",
                sharedR.string.sync_label_cloud_folder,
                syncOrBackup,
                "PC",
            )
        )
    }
}
