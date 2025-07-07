package mega.privacy.android.feature.sync.domain.sync.notification

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncNotificationTypeUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSyncNotificationTypeUseCaseTest {

    private lateinit var underTest: GetSyncNotificationTypeUseCase

    @BeforeEach
    fun setUp() {
        underTest = GetSyncNotificationTypeUseCase()
    }

    @Test
    fun `test that it returns null when syncs list is empty`() {
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            isCharging = true,
            isSyncOnlyWhenCharging = false,
            syncs = emptyList(),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isNull()
    }

    @Test
    fun `test that it returns CHANGE_SYNC_ROOT when sync has invalid local path`() {
        val folderPair = createFolderPair(isLocalPathUri = true)
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            isCharging = true,
            isSyncOnlyWhenCharging = false,
            syncs = listOf(folderPair),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.CHANGE_SYNC_ROOT)
    }

    @Test
    fun `test that it returns CHANGE_SYNC_ROOT when sync has ignore file error`() {
        val folderPair = createFolderPair(syncError = SyncError.COULD_NOT_CREATE_IGNORE_FILE)
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            isCharging = true,
            isSyncOnlyWhenCharging = false,
            syncs = listOf(folderPair),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.CHANGE_SYNC_ROOT)
    }

    @Test
    fun `test that it returns NOT_CHARGING when device is not charging and sync only when charging is enabled`() {
        val folderPair = createFolderPair()
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            isCharging = false,
            isSyncOnlyWhenCharging = true,
            syncs = listOf(folderPair),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.NOT_CHARGING)
    }

    @Test
    fun `test that it returns BATTERY_LOW when battery is low`() {
        val folderPair = createFolderPair()
        val result = underTest(
            isBatteryLow = true,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            isCharging = true,
            isSyncOnlyWhenCharging = false,
            syncs = listOf(folderPair),
            stalledIssues = listOf(mock(), mock())
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.BATTERY_LOW)
    }

    @Test
    fun `test that it returns NOT_CONNECTED_TO_WIFI when not on wifi and sync only by wifi is enabled`() {
        val folderPair = createFolderPair()
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = false,
            isSyncOnlyByWifi = true,
            isCharging = true,
            isSyncOnlyWhenCharging = false,
            syncs = listOf(folderPair),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.NOT_CONNECTED_TO_WIFI)
    }

    @Test
    fun `test that it returns ERROR when sync has error`() {
        val folderPair = createFolderPair(syncError = SyncError.UNKNOWN_ERROR)
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            isCharging = true,
            isSyncOnlyWhenCharging = false,
            syncs = listOf(folderPair),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.ERROR)
    }

    @Test
    fun `test that it returns STALLED_ISSUE when there are stalled issues`() {
        val folderPair = createFolderPair()
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            isCharging = true,
            isSyncOnlyWhenCharging = false,
            syncs = listOf(folderPair),
            stalledIssues = listOf(mock(), mock())
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.STALLED_ISSUE)
    }

    @Test
    fun `test that it returns null when all conditions are normal`() {
        val folderPair = createFolderPair()
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            isCharging = true,
            isSyncOnlyWhenCharging = false,
            syncs = listOf(folderPair),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isNull()
    }

    @Test
    fun `test that it returns null when sync only by wifi is disabled and user is not on wifi`() {
        val folderPair = createFolderPair()
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = false,
            isSyncOnlyByWifi = false,
            isCharging = true,
            isSyncOnlyWhenCharging = false,
            syncs = listOf(folderPair),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isNull()
    }

    @Test
    fun `test that it returns null when sync only when charging is disabled and device is not charging`() {
        val folderPair = createFolderPair()
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = false,
            isCharging = false,
            isSyncOnlyWhenCharging = false,
            syncs = listOf(folderPair),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isNull()
    }

    private fun createFolderPair(
        isLocalPathUri: Boolean = false,
        syncError: SyncError = SyncError.NO_SYNC_ERROR,
    ) = FolderPair(
        id = 1L,
        pairName = "Test Pair",
        localFolderPath = if (isLocalPathUri) "/test/path" else "content://com.android.externalstorage.documents/document/primary%3APHOTOS",
        remoteFolder = RemoteFolder(NodeId(1L), "Test Remote"),
        syncStatus = SyncStatus.SYNCING,
        syncType = SyncType.TYPE_TWOWAY,
        syncError = syncError
    )
}
