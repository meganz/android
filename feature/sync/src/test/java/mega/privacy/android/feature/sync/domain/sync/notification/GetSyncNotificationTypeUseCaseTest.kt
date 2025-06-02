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
            syncs = emptyList(),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(null)
    }

    @Test
    fun `test that it returns BATTERY_LOW when battery is low`() {
        val firstSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Sync",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
        )
        val secondSync = FolderPair(
            id = 6886L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Trip_to_NZ",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "NZ_trip"),
            syncStatus = SyncStatus.SYNCED,
        )
        val result = underTest(
            isBatteryLow = true,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            syncs = listOf(firstSync, secondSync),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.BATTERY_LOW)
    }

    @Test
    fun `test that it returns NOT_CONNECTED_TO_WIFI when network constraint is not respected`() {
        val firstSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Sync",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
        )
        val secondSync = FolderPair(
            id = 6886L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Trip_to_NZ",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "NZ_trip"),
            syncStatus = SyncStatus.SYNCED,
        )
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = false,
            isSyncOnlyByWifi = true,
            syncs = listOf(firstSync, secondSync),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.NOT_CONNECTED_TO_WIFI)
    }

    @Test
    fun `test that it returns ERROR when any sync has an error`() {
        val firstSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Sync",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
        )
        val secondSync = FolderPair(
            id = 6886L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Trip_to_NZ",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "NZ_trip"),
            syncStatus = SyncStatus.ERROR,
        )
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            syncs = listOf(firstSync, secondSync),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.ERROR)
    }

    @Test
    fun `test that it returns STALLED_ISSUE when there are stalled issues`() {
        val sync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Sync",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
            syncError = SyncError.NO_SYNC_ERROR
        )
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            syncs = listOf(sync),
            stalledIssues = listOf(mock(), mock())
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.STALLED_ISSUE)
    }

    @Test
    fun `test that it returns null when no conditions are met`() {
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            syncs = emptyList(),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isNull()
    }

    @Test
    fun `test that it returns CHANGE_SYNC_ROOT when local folder path is not an URI`() {
        val firstSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Sync",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
        )
        val secondSync = FolderPair(
            id = 6886L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Trip_to_NZ",
            localFolderPath = "/path/to/trip_to_nz",
            remoteFolder = RemoteFolder(NodeId(1244L), "NZ_trip"),
            syncStatus = SyncStatus.SYNCED,
        )
        val result = underTest(
            isBatteryLow = true,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            syncs = listOf(firstSync, secondSync),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.CHANGE_SYNC_ROOT)
    }

    @Test
    fun `test that it returns CHANGE_SYNC_ROOT when syncError is COULD_NOT_CREATE_IGNORE_FILE`() {
        val firstSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Sync",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
            syncError = SyncError.COULD_NOT_CREATE_IGNORE_FILE
        )
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            syncs = listOf(firstSync),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isEqualTo(SyncNotificationType.CHANGE_SYNC_ROOT)
    }

    @Test
    fun `test that it returns null when all conditions are met and no issues exist`() {
        val firstSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Sync",
            localFolderPath = LOCAL_FOLDER_PATH,
            remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
            syncError = SyncError.NO_SYNC_ERROR
        )
        val result = underTest(
            isBatteryLow = false,
            isUserOnWifi = true,
            isSyncOnlyByWifi = true,
            syncs = listOf(firstSync),
            stalledIssues = emptyList()
        )
        Truth.assertThat(result).isNull()
    }

    private companion object {
        const val LOCAL_FOLDER_PATH =
            "content://com.android.externalstorage.documents/document/primary%3APHOTOS"
    }
}
